/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
//! Shared utilities.

use super::gles11_raw as gles11; // constants only
use super::gles11_raw::types::{GLenum, GLfixed, GLfloat, GLint, GLsizei};
use super::GLES;

/// Dead Space's opaque PVRTC environment atlases preserve far less highlight
/// detail here than they do on the original iOS renderer. Keep the affected
/// materials on a gentler exposure without touching UI textures, alpha
/// atlases, or texture content belonging to other applications. Most affected
/// materials use 0.84; the dominant silver-white wall atlas uses 0.50.
const DS_AMBIENT_DIM_KEYS: &[u32] = &[
    0x043a_2b1c,
    0x05a7_6ada,
    0x073b_0ab8,
    0x133a_85ee,
    0x15f5_4e17,
    0x1aa4_e365,
    0x2cf8_acf0,
    0x335d_1a0d,
    0x4cde_6c8d,
    0x53a7_9ba3,
    0x5fe3_1f94,
    0x62bf_6ba7,
    0x6c25_d924,
    0x718f_bb64,
    0x760d_5f8a,
    0x8881_3bba,
    0x96e7_0555,
    0xa7fa_e2de,
    0xa8ed_3568,
    0xac0e_62d8,
    0xb040_1841,
    0xb197_7ef1,
    0xb30d_e681,
    0xbb80_5dcf,
    0xc5a1_61fa,
    0xdd43_19a1,
    0xe13b_2985,
    0xe30f_c790,
    0xf49c_0f81,
    0xf4d1_78fa,
];

fn ds_dim_ambient_pixels(pixels: &mut [u32], scale: u32) {
    // Preserve hue and the relative separation between dark panels and
    // silver highlights while applying the material-specific exposure.
    for pixel in pixels {
        let channels = *pixel;
        let r = channels & 0xff;
        let g = (channels >> 8) & 0xff;
        let b = (channels >> 16) & 0xff;
        let a = channels & 0xff00_0000;
        let dim = |value: u32| (value * scale + 127) / 255;
        *pixel = a | dim(r) | (dim(g) << 8) | (dim(b) << 16);
    }
}

pub(crate) fn ds_ambient_dim_scale(content_key: u32) -> Option<u32> {
    DS_AMBIENT_DIM_KEYS.contains(&content_key).then_some(
        if content_key == 0x62bf_6ba7 { 128 } else { 214 },
    )
}

/// Apply the accepted ambient exposure to an opaque RGBA8 HD replacement.
/// PNG replacements bypass `try_decode_pvrtc`, so they must use the same
/// material-keyed scale as the original PVRTC path.
pub(crate) fn ds_dim_ambient_rgba8(pixels: &mut [u8], scale: u32) {
    debug_assert_eq!(pixels.len() % 4, 0);
    for pixel in pixels.chunks_exact_mut(4) {
        let dim = |value: u8| ((value as u32 * scale + 127) / 255) as u8;
        pixel[0] = dim(pixel[0]);
        pixel[1] = dim(pixel[1]);
        pixel[2] = dim(pixel[2]);
    }
}

/// PERF OPTIMIZATION (loading): cache of recently decoded PVRTC textures.
/// Unity apps commonly destroy and re-create the same compressed textures
/// across scene transitions; decoding PVRTC is one of the biggest chunks of
/// load time. Entries are keyed by (content hash, params), so a re-upload of
/// identical payload skips the expensive software decode entirely.
///
/// The cache is bounded by a total-decoded-bytes budget to keep host memory
/// usage predictable (decoded RGBA8 is 4-8x the compressed size).
const PVRTC_CACHE_MAX_ENTRIES: usize = 12;
const PVRTC_CACHE_MAX_BYTES: usize = 48 * 1024 * 1024;

struct PvrtcCacheEntry {
    key: u64,
    is_2bit: bool,
    width: u32,
    height: u32,
    /// Decoded RGBA8 (one `u32` per texel, as returned by `decode_pvrtc`).
    pixels: std::rc::Rc<[u32]>,
}

use std::cell::RefCell;
thread_local! {
    static PVRTC_DECODE_CACHE: RefCell<Vec<PvrtcCacheEntry>> = const { RefCell::new(Vec::new()) };
}

/// FNV-1a 64-bit hash of the compressed payload.
fn fnv1a_hash(data: &[u8]) -> u64 {
    let mut hash: u64 = 0xcbf29ce484222325;
    for &byte in data {
        hash ^= byte as u64;
        hash = hash.wrapping_mul(0x100000001b3);
    }
    hash
}

/// Host-side directory for the on-disk PVRTC decode cache. Decoding PVRTC on
/// the CPU is one of the dominant startup costs for games that ship most of
/// their textures compressed (e.g. Unity titles); persisting the decoded RGBA
/// makes every subsequent launch of the same app skip the decode entirely.
fn pvrtc_disk_cache_dir() -> Option<std::path::PathBuf> {
    static CACHE_DIR: std::sync::OnceLock<Option<std::path::PathBuf>> = std::sync::OnceLock::new();
    CACHE_DIR
        .get_or_init(|| {
            let dir = crate::paths::user_data_base_path().join("touchHLE_pvrtc_cache");
            match std::fs::create_dir_all(&dir) {
                Ok(()) => Some(dir),
                Err(e) => {
                    log!(
                        "Warning: PVRTC disk cache disabled, can't create {}: {e}",
                        dir.display()
                    );
                    None
                }
            }
        })
        .clone()
}

fn pvrtc_disk_cache_path(
    key: u64,
    is_2bit: bool,
    is_opaque: bool,
    width: u32,
    height: u32,
) -> Option<std::path::PathBuf> {
    pvrtc_disk_cache_dir().map(|dir| {
        dir.join(format!(
            "{:016x}-{}{}-{}x{}.rgba",
            key,
            if is_2bit { 2 } else { 4 },
            if is_opaque { "o" } else { "a" },
            width,
            height
        ))
    })
}

fn pvrtc_disk_cache_load(
    key: u64,
    is_2bit: bool,
    is_opaque: bool,
    width: u32,
    height: u32,
) -> Option<Vec<u32>> {
    let path = pvrtc_disk_cache_path(key, is_2bit, is_opaque, width, height)?;
    let data = std::fs::read(&path).ok()?;
    let expected = (width as usize) * (height as usize) * std::mem::size_of::<u32>();
    if data.len() != expected {
        // Corrupt or stale entry from an older format — ignore it.
        return None;
    }
    let mut pixels = Vec::with_capacity(expected / std::mem::size_of::<u32>());
    for chunk in data.chunks_exact(std::mem::size_of::<u32>()) {
        pixels.push(u32::from_le_bytes([chunk[0], chunk[1], chunk[2], chunk[3]]));
    }
    Some(pixels)
}

fn pvrtc_disk_cache_store(
    key: u64,
    is_2bit: bool,
    is_opaque: bool,
    width: u32,
    height: u32,
    pixels: &[u32],
) {
    let Some(path) = pvrtc_disk_cache_path(key, is_2bit, is_opaque, width, height) else {
        return;
    };
    let mut bytes = Vec::with_capacity(pixels.len() * std::mem::size_of::<u32>());
    for px in pixels {
        bytes.extend_from_slice(&px.to_le_bytes());
    }
    // Write atomically (tmp + rename) so a crash mid-write can't leave a
    // half-written entry that later fails the size check anyway.
    let tmp = path.with_extension("tmp");
    if std::fs::write(&tmp, &bytes).is_ok() && std::fs::rename(&tmp, &path).is_err() {
        let _ = std::fs::remove_file(&tmp);
    }
}

/// Convert a fixed-point scalar to a floating-point scalar.
///
/// Beware: Rust's type checker won't complain if you mix up [GLfixed] with
/// [GLint], but they have very different meanings.
pub fn fixed_to_float(fixed: GLfixed) -> GLfloat {
    ((fixed as f64) / ((1 << 16) as f64)) as f32
}

/// Convert a floating-point scalar to a fixed-point (16.16) scalar, saturating
/// on overflow. Used to implement `glGetFixedv` on top of a backend that only
/// exposes floating-point state.
pub fn float_to_fixed(float: GLfloat) -> GLfixed {
    let scaled = (float as f64) * ((1 << 16) as f64);
    if scaled >= GLfixed::MAX as f64 {
        GLfixed::MAX
    } else if scaled <= GLfixed::MIN as f64 {
        GLfixed::MIN
    } else if scaled.is_nan() {
        0
    } else {
        scaled.round() as GLfixed
    }
}

/// Convert a fixed-point 4-by-4 matrix to floating-point.
pub unsafe fn matrix_fixed_to_float(m: *const GLfixed) -> [GLfloat; 16] {
    let mut matrix = [0f32; 16];
    for (i, cell) in matrix.iter_mut().enumerate() {
        *cell = fixed_to_float(m.add(i).read_unaligned());
    }
    matrix
}

/// Type of a parameter, used in [ParamTable].
#[derive(Copy, Clone, Eq, PartialEq, Debug)]
pub enum ParamType {
    /// `GLboolean`
    Boolean,
    /// `GLfloat`
    Float,
    /// `GLint`
    Int,
    /// Placeholder type for things like colors which are floating-point
    /// but don't have the usual conversion behavior to/from integers etc.
    /// [ParamTable] will accept it for floating-point inputs only.
    /// TODO: Remove this and add proper types for colors etc.
    FloatSpecial,
    /// Hack to achieve `#[non_exhaustive]`-like behavior within this crate,
    /// since more types might be added in future
    _NonExhaustive,
}

/// Table of parameter names, component types and component counts.
///
/// This is a helper for implementing the common pattern in OpenGL where a set
/// of parameters named by [GLenum] values can be accessed via functions with
/// suffixes like `f`, `fv`, `i`, `iv`, etc.
pub struct ParamTable(pub &'static [(GLenum, ParamType, u8)]);

impl ParamTable {
    /// Look up the component type and count for a parameter. Returns a safe
    /// default (`ParamType::Float`, 1) for unknown names rather than panicking
    /// the host, so misbehaving guest code only loses correctness for the
    /// specific call rather than tearing the whole emulator down.
    pub fn get_type_info(&self, pname: GLenum) -> (ParamType, u8) {
        match self.0.iter().find(|&&(pname2, _, _)| pname == pname2) {
            Some(&(_, type_, count)) => (type_, count),
            None => {
                log!(
                    "Warning: ParamTable::get_type_info: unhandled parameter name {pname:#x}; defaulting to (Float, 1)."
                );
                (ParamType::Float, 1)
            }
        }
    }

    /// Assert that a parameter name is recognized.
    pub fn assert_known_param(&self, pname: GLenum) {
        self.get_type_info(pname);
    }

    pub fn contains(&self, pname: GLenum) -> bool {
        self.0.iter().any(|(pname2, _, _)| pname == *pname2)
    }

    /// Check that a parameter name is recognized and that the parameter has a
    /// particular component count. Logs a warning instead of panicking the
    /// host on mismatch, so the worst case is just a malformed GL call.
    pub fn assert_component_count(&self, pname: GLenum, provided_count: u8) {
        let (_type, actual_count) = self.get_type_info(pname);
        if actual_count != provided_count {
            log!(
                "Warning: ParamTable::assert_component_count: parameter {pname:#x} has component count {actual_count}, {provided_count} given; continuing anyway."
            );
        }
    }

    /// Implements a fixed-point scalar (`x`) setter by calling a provided
    /// floating-point scalar (`f`) or integer scalar (`i`) setter as
    /// as appropriate.
    ///
    /// This will panic if the name is not recognized or the parameter is not
    /// a scalar.
    pub unsafe fn setx<FF, FI>(&self, setf: FF, seti: FI, pname: GLenum, param: GLfixed)
    where
        FF: FnOnce(GLfloat),
        FI: FnOnce(GLint),
    {
        let (type_, component_count) = self.get_type_info(pname);
        assert!(component_count == 1);
        // Yes, the OpenGL standard lets you mismatch types. Yes, it requires
        // an implicit conversion. Yes, it requires no scaling of fixed-point
        // values when converting to integer. :(
        // On the other hand, fixed-to-float/float-to-fixed conversion is always
        // the same even for the weird float-ish values.
        match type_ {
            ParamType::Float | ParamType::FloatSpecial => setf(fixed_to_float(param)),
            _ => seti(param),
        }
    }

    /// Implements a fixed-point vector (`xv`) setter by calling a provided
    /// floating-point vector (`fv`) or integer vector (`iv`) setter as
    /// as appropriate.
    ///
    /// This will panic if the name is not recognized.
    pub unsafe fn setxv<FFV, FIV>(
        &self,
        setfv: FFV,
        setiv: FIV,
        pname: GLenum,
        params: *const GLfixed,
    ) where
        FFV: FnOnce(*const GLfloat),
        FIV: FnOnce(*const GLint),
    {
        let (type_, count) = self.get_type_info(pname);
        // Yes, the OpenGL standard is like this (see above).
        match type_ {
            // Fixed-to-float/float-to-fixed conversion is always the same even
            // for the weird float-ish values.
            ParamType::Float | ParamType::FloatSpecial => {
                let mut params_float = [0.0; 16]; // probably the max?
                let params_float = &mut params_float[..usize::from(count)];
                for (i, param_float) in params_float.iter_mut().enumerate() {
                    *param_float = fixed_to_float(params.add(i).read())
                }
                setfv(params_float.as_ptr())
            }
            _ => setiv(params),
        }
    }
}

/// Helper for implementing `glCompressedTexImage2D`: if `internalformat` is
/// one of the `IMG_texture_compression_pvrtc` formats, decode it via the
/// software PVRTC decoder and call `glTexImage2D` to upload the resulting
/// RGBA8 texture. Returns:
/// * `true` if the format was a PVRTC variant and the upload was attempted
///   (regardless of whether the upload itself succeeded — that's between the
///   driver and the caller).
/// * `false` if `internalformat` is not a PVRTC variant; the caller is then
///   responsible for handling the format some other way (passthrough,
///   paletted-texture decode, ...).
///
/// Previously this helper used `assert!` for `border == 0` and for the
/// payload-size check inside `decode_pvrtc`. Real-world iPhone OS games
/// occasionally pass in slightly truncated payloads (broken builds, in-app
/// procedural textures with off-by-one mip sizes, third-party loaders that
/// trim trailing zeros), and a panic in the present pipeline brings down the
/// whole host process. Treat malformed input as a soft failure: log once and
/// return `true` *without* uploading anything, so the caller doesn't
/// re-attempt (the data is unusable either way) and the rest of the frame
/// can still draw.
#[allow(clippy::too_many_arguments)]
pub fn try_decode_pvrtc(
    gles: &mut dyn GLES,
    target: GLenum,
    level: GLint,
    internalformat: GLenum,
    width: GLsizei,
    height: GLsizei,
    border: GLint,
    pvrtc_data: &[u8],
) -> bool {
    let is_2bit = match internalformat {
        gles11::COMPRESSED_RGB_PVRTC_4BPPV1_IMG | gles11::COMPRESSED_RGBA_PVRTC_4BPPV1_IMG => false,
        gles11::COMPRESSED_RGB_PVRTC_2BPPV1_IMG | gles11::COMPRESSED_RGBA_PVRTC_2BPPV1_IMG => true,
        _ => return false,
    };

    if border != 0 {
        log!(
            "Warning: try_decode_pvrtc: invalid non-zero border ({border}) for PVRTC \
             upload {width}x{height} (level {level}, format {internalformat:#x}); \
             skipping upload."
        );
        return true;
    }

    let Ok(width_u) = u32::try_from(width) else {
        log!(
            "Warning: try_decode_pvrtc: invalid width {width} for PVRTC upload \
             (level {level}, format {internalformat:#x}); skipping upload."
        );
        return true;
    };
    let Ok(height_u) = u32::try_from(height) else {
        log!(
            "Warning: try_decode_pvrtc: invalid height {height} for PVRTC upload \
             (level {level}, format {internalformat:#x}); skipping upload."
        );
        return true;
    };

    // The IMG_texture_compression_pvrtc spec specifies a fixed payload size
    // for any given (is_2bit, width, height) tuple. Validate it here so that
    // a short / oversized input from the guest doesn't panic the host
    // PVRTC decoder. (`decode_pvrtc` itself still asserts internally as
    // a defence-in-depth measure, but we shouldn't rely on that — see the
    // function-level doc comment.)
    let expected_size = if is_2bit {
        (width_u.max(16) as usize * height_u.max(8) as usize * 2).div_ceil(8)
    } else {
        (width_u.max(8) as usize * height_u.max(8) as usize * 4).div_ceil(8)
    };
    if pvrtc_data.len() != expected_size {
        log!(
            "Warning: try_decode_pvrtc: PVRTC payload size mismatch for \
             {width}x{height} (level {level}, format {internalformat:#x}, \
             is_2bit={is_2bit}): got {} bytes, expected {expected_size}; \
             skipping upload.",
            pvrtc_data.len(),
        );
        return true;
    }

    // Look up the decoded result in the cache before doing the (expensive)
    // software decode.
    let key = fnv1a_hash(pvrtc_data);
    let content_key =
        crate::ds_texid::content_key(width_u, height_u, level, internalformat, pvrtc_data);
    if level == 0 && std::env::var_os("TOUCHHLE_DS_TEXTURE_TRACE").is_some() {
        let mut texture: GLint = 0;
        unsafe {
            gles.GetIntegerv(gles11::TEXTURE_BINDING_2D, &mut texture);
        }
        let is_opaque = matches!(
            internalformat,
            gles11::COMPRESSED_RGB_PVRTC_4BPPV1_IMG | gles11::COMPRESSED_RGB_PVRTC_2BPPV1_IMG
        );
        log!(
            "[DSTEX pvrtc-key] tex={} key={:016x} opaque={} size={}x{} is_2bit={} internal=0x{:x}",
            texture,
            key,
            is_opaque,
            width,
            height,
            is_2bit,
            internalformat
        );
    }
    let cached = PVRTC_DECODE_CACHE.with(|cache| {
        let mut cache = cache.borrow_mut();
        if let Some(pos) = cache.iter().position(|e| {
            e.key == key && e.is_2bit == is_2bit && e.width == width_u && e.height == height_u
        }) {
            // Move-to-front for simple LRU behaviour.
            let entry = cache.remove(pos);
            cache.insert(0, entry);
            let entry = &cache[0];
            Some(entry.pixels.clone())
        } else {
            None
        }
    });

    let pixels: std::rc::Rc<[u32]> = cached.unwrap_or_else(|| {
        // Second-tier lookup: on-disk decode cache (survives restarts).
        // Cache-key discriminator: RGB vs RGBA PVRTC of identical payloads
        // decode differently (alpha channel), so distinguish them on disk.
        let is_opaque = matches!(
            internalformat,
            gles11::COMPRESSED_RGB_PVRTC_4BPPV1_IMG | gles11::COMPRESSED_RGB_PVRTC_2BPPV1_IMG
        );
        let decoded: Vec<u32> = pvrtc_disk_cache_load(key, is_2bit, is_opaque, width_u, height_u)
            .unwrap_or_else(|| {
                let decoded: Vec<u32> =
                    crate::image::decode_pvrtc(pvrtc_data, is_2bit, width_u, height_u);
                pvrtc_disk_cache_store(key, is_2bit, is_opaque, width_u, height_u, &decoded);
                decoded
            });
        let decoded: std::rc::Rc<[u32]> = decoded.into();
        PVRTC_DECODE_CACHE.with(|cache| {
            let mut cache = cache.borrow_mut();
            // Evict least-recently-used entries until the new entry fits
            // within the entry-count and byte-budget limits.
            let decoded_bytes = decoded.len() * std::mem::size_of::<u32>();
            let mut total_bytes: usize = cache
                .iter()
                .map(|e| e.pixels.len() * std::mem::size_of::<u32>())
                .sum();
            while cache.len() >= PVRTC_CACHE_MAX_ENTRIES
                || total_bytes + decoded_bytes > PVRTC_CACHE_MAX_BYTES
            {
                let Some(evicted) = cache.pop() else { break };
                total_bytes -= evicted.pixels.len() * std::mem::size_of::<u32>();
            }
            cache.insert(
                0,
                PvrtcCacheEntry {
                    key,
                    is_2bit,
                    width: width_u,
                    height: height_u,
                    pixels: decoded.clone(),
                },
            );
        });
        decoded
    });
    let ambient_key = if level == 0 {
        DS_AMBIENT_DIM_KEYS
            .contains(&content_key)
            .then_some(content_key)
    } else {
        let mut texture: GLint = 0;
        unsafe { gles.GetIntegerv(gles11::TEXTURE_BINDING_2D, &mut texture) };
        crate::ds_texid::lookup(texture as u32)
            .map(|ident| ident.key)
            .filter(|key| DS_AMBIENT_DIM_KEYS.contains(key))
    };
    let pixels = if internalformat == gles11::COMPRESSED_RGB_PVRTC_4BPPV1_IMG
        && ambient_key.is_some()
        && std::env::var("TOUCHHLE_DS_PVRTC_AMBIENT_DIM").as_deref() != Ok("0")
    {
        let mut dimmed = pixels.as_ref().to_vec();
        let scale = ambient_key.and_then(ds_ambient_dim_scale).unwrap();
        ds_dim_ambient_pixels(&mut dimmed, scale);
        dimmed.into()
    } else {
        pixels
    };
    unsafe {
        gles.TexImage2D(
            target,
            level,
            gles11::RGBA as _,
            width,
            height,
            border,
            gles11::RGBA,
            gles11::UNSIGNED_BYTE,
            pixels.as_ptr() as *const _,
        )
    };
    true
}

pub struct PalettedTextureFormat {
    /// * `true` for 4-bit (nibble) index, 16-color palette.
    /// * `false` for 8-bit (byte) index, 256-color palette.
    pub index_is_nibble: bool,
    /// `glTexImage2D`-style `format` for palette entries: `GL_RGB` or `GL_RGBA`
    pub palette_entry_format: GLenum,
    /// `glTexImage2D`-style `type` for palette entries: `GL_UNSIGNED_BYTE` or
    /// some `GL_UNSIGNED_SHORT_` value
    pub palette_entry_type: GLenum,
}
impl PalettedTextureFormat {
    /// If the provided format is from `OES_compressed_paletted_texture`,
    /// return [Some] with information about it, or [None] otherwise.
    pub fn get_info(internalformat: GLenum) -> Option<Self> {
        match internalformat {
            gles11::PALETTE4_RGB8_OES => Some(Self {
                index_is_nibble: true,
                palette_entry_format: gles11::RGB,
                palette_entry_type: gles11::UNSIGNED_BYTE,
            }),
            gles11::PALETTE4_RGBA8_OES => Some(Self {
                index_is_nibble: true,
                palette_entry_format: gles11::RGBA,
                palette_entry_type: gles11::UNSIGNED_BYTE,
            }),
            gles11::PALETTE4_R5_G6_B5_OES => Some(Self {
                index_is_nibble: true,
                palette_entry_format: gles11::RGB,
                palette_entry_type: gles11::UNSIGNED_SHORT_5_6_5,
            }),
            gles11::PALETTE4_RGBA4_OES => Some(Self {
                index_is_nibble: true,
                palette_entry_format: gles11::RGBA,
                palette_entry_type: gles11::UNSIGNED_SHORT_4_4_4_4,
            }),
            gles11::PALETTE4_RGB5_A1_OES => Some(Self {
                index_is_nibble: true,
                palette_entry_format: gles11::RGBA,
                palette_entry_type: gles11::UNSIGNED_SHORT_5_5_5_1,
            }),
            gles11::PALETTE8_RGB8_OES => Some(Self {
                index_is_nibble: false,
                palette_entry_format: gles11::RGB,
                palette_entry_type: gles11::UNSIGNED_BYTE,
            }),
            gles11::PALETTE8_RGBA8_OES => Some(Self {
                index_is_nibble: false,
                palette_entry_format: gles11::RGBA,
                palette_entry_type: gles11::UNSIGNED_BYTE,
            }),
            gles11::PALETTE8_R5_G6_B5_OES => Some(Self {
                index_is_nibble: false,
                palette_entry_format: gles11::RGB,
                palette_entry_type: gles11::UNSIGNED_SHORT_5_6_5,
            }),
            gles11::PALETTE8_RGBA4_OES => Some(Self {
                index_is_nibble: false,
                palette_entry_format: gles11::RGBA,
                palette_entry_type: gles11::UNSIGNED_SHORT_4_4_4_4,
            }),
            gles11::PALETTE8_RGB5_A1_OES => Some(Self {
                index_is_nibble: false,
                palette_entry_format: gles11::RGBA,
                palette_entry_type: gles11::UNSIGNED_SHORT_5_5_5_1,
            }),
            _ => None,
        }
    }
}

#[cfg(test)]
mod ambient_dim_tests {
    use super::{ds_ambient_dim_scale, ds_dim_ambient_rgba8};

    #[test]
    fn ambient_scale_is_limited_to_known_materials() {
        assert_eq!(ds_ambient_dim_scale(0x62bf_6ba7), Some(128));
        assert_eq!(ds_ambient_dim_scale(0x043a_2b1c), Some(214));
        assert_eq!(ds_ambient_dim_scale(0x1234_5678), None);
    }

    #[test]
    fn rgba8_dim_preserves_alpha_and_uses_accepted_rounding() {
        let mut pixels = [255, 128, 1, 77, 64, 32, 16, 0];
        ds_dim_ambient_rgba8(&mut pixels[..4], 128);
        ds_dim_ambient_rgba8(&mut pixels[4..], 214);
        assert_eq!(pixels, [128, 64, 1, 77, 54, 27, 13, 0]);
    }
}
