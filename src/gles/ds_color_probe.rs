//! Opt-in Android color diagnostics; no correction is applied.
use super::gles11_raw as gl;
use super::gles_generic::GLES;
use std::collections::HashSet;
use std::sync::{
    atomic::{AtomicBool, Ordering},
    Mutex, OnceLock,
};

pub(crate) fn enabled() -> bool {
    std::env::var("TOUCHHLE_DS_COLOR_PROBE").as_deref() == Ok("1")
}

fn once(label: &str) -> bool {
    static SEEN: OnceLock<Mutex<HashSet<String>>> = OnceLock::new();
    let mut seen = SEEN.get_or_init(Default::default).lock().unwrap();
    seen.len() < 160 && seen.insert(label.to_owned())
}

fn target_once() -> bool {
    static LOGGED: AtomicBool = AtomicBool::new(false);
    !LOGGED.swap(true, Ordering::Relaxed)
}

fn trigger() -> Option<(String, u64)> {
    let path = std::env::var("TOUCHHLE_DS_COLOR_TRIGGER_FILE").ok()?;
    let frame = crate::window::DS_FRAME_COUNTER.load(std::sync::atomic::Ordering::Relaxed);
    static STATE: OnceLock<Mutex<(u64, String, u64)>> = OnceLock::new();
    let mut state = STATE
        .get_or_init(|| Mutex::new((u64::MAX, String::new(), 0)))
        .lock()
        .unwrap();
    if state.0 != frame {
        state.0 = frame;
        let token = std::fs::read_to_string(path)
            .unwrap_or_default()
            .trim()
            .to_owned();
        if !token.is_empty() && token != state.1 {
            state.1 = token;
            state.2 = frame;
            log!("[DScolor trigger] token={} frame={}", state.1, frame);
        }
    }
    if state.1.is_empty() {
        None
    } else {
        Some((state.1.clone(), state.2))
    }
}

pub(crate) unsafe fn storage(g: &mut dyn GLES, target: u32, requested: u32, w: i32, h: i32) {
    if !enabled() || target != gl::RENDERBUFFER_OES {
        return;
    }
    let mut values = [0; 7];
    for (p, v) in [0x8d44, 0x8d50, 0x8d51, 0x8d52, 0x8d53, 0x8d54, 0x8d55]
        .into_iter()
        .zip(&mut values)
    {
        g.GetRenderbufferParameterivOES(target, p, v);
    }
    log!("[DScolor storage] requested=0x{:x} size={}x{} actual_format=0x{:x} RGBA={:?} depth={} stencil={}", requested, w, h, values[0], &values[1..5], values[5], values[6]);
}

pub(crate) unsafe fn attachment(g: &mut dyn GLES) {
    if !enabled() {
        return;
    }
    let mut values = [0; 6];
    for (p, v) in [
        gl::FRAMEBUFFER_BINDING_OES,
        0x0d52,
        0x0d53,
        0x0d54,
        0x0d55,
        gl::DEPTH_BITS,
    ]
    .into_iter()
    .zip(&mut values)
    {
        g.GetIntegerv(p, v);
    }
    log!(
        "[DScolor attachment] fbo={} RGBA={:?} depth={} status=0x{:x}",
        values[0],
        &values[1..5],
        values[5],
        g.CheckFramebufferStatusOES(gl::FRAMEBUFFER_OES)
    );
}

pub(crate) unsafe fn draw(g: &mut dyn GLES, count: i32) {
    if !enabled() {
        return;
    }
    let frame = crate::window::DS_FRAME_COUNTER.load(std::sync::atomic::Ordering::Relaxed);
    let mut tex = 0;
    g.GetIntegerv(gl::TEXTURE_BINDING_2D, &mut tex);
    let key = crate::ds_texid::lookup(tex as u32)
        .map(|v| v.key)
        .unwrap_or(0);
    let target_draw = key == 0x62bf_6ba7 && count == 2430;
    let armed = if std::env::var_os("TOUCHHLE_DS_COLOR_TRIGGER_FILE").is_some() {
        trigger().is_some_and(|(_, start)| frame.saturating_sub(start) <= 600)
    } else {
        (1000..=5500).contains(&frame)
    };
    if !armed && !target_draw {
        return;
    }
    if target_draw {
        if !target_once() {
            return;
        }
    } else if !once(&format!("draw:{:x}:{}", key, count)) {
        return;
    }
    let mut mode = 0;
    g.GetTexEnviv(gl::TEXTURE_ENV, gl::TEXTURE_ENV_MODE, &mut mode);
    let mut rgb_scale = 0.;
    g.GetTexEnvfv(gl::TEXTURE_ENV, 0x8573, &mut rgb_scale);
    let mut alpha_scale = 0.;
    g.GetTexEnvfv(gl::TEXTURE_ENV, 0x0d1c, &mut alpha_scale);
    let mut current = [0.; 4];
    g.GetFloatv(gl::CURRENT_COLOR, current.as_mut_ptr());
    let mut env_color = [0.; 4];
    g.GetTexEnvfv(
        gl::TEXTURE_ENV,
        gl::TEXTURE_ENV_COLOR,
        env_color.as_mut_ptr(),
    );
    let mut factors = [0; 4];
    for (p, v) in [
        gl::BLEND_SRC,
        gl::BLEND_DST,
        gl::ACTIVE_TEXTURE,
        gl::TEXTURE_BINDING_2D,
    ]
    .into_iter()
    .zip(&mut factors)
    {
        g.GetIntegerv(p, v);
    }
    log!("[DScolor draw] f={} key={:08x} count={} mode=0x{:x} rgb_scale={} alpha_scale={} current={:?} env={:?} blend={} factors={:?} lighting={} color_array={}", frame, key, count, mode, rgb_scale, alpha_scale, current, env_color, g.IsEnabled(gl::BLEND), factors, g.IsEnabled(gl::LIGHTING), g.IsEnabled(gl::COLOR_ARRAY));
    let mut tex_matrix = [0.0; 16];
    g.GetFloatv(gl::TEXTURE_MATRIX, tex_matrix.as_mut_ptr());
    let mut wrap = [0; 2];
    for (p, v) in [gl::TEXTURE_WRAP_S, gl::TEXTURE_WRAP_T]
        .into_iter()
        .zip(&mut wrap)
    {
        g.GetTexParameteriv(gl::TEXTURE_2D, p, v);
    }
    let mut filters = [0; 2];
    for (p, v) in [gl::TEXTURE_MIN_FILTER, gl::TEXTURE_MAG_FILTER]
        .into_iter()
        .zip(&mut filters)
    {
        g.GetTexParameteriv(gl::TEXTURE_2D, p, v);
    }
    let mut texcoord = [0; 3];
    for (p, v) in [
        gl::TEXTURE_COORD_ARRAY_SIZE,
        gl::TEXTURE_COORD_ARRAY_TYPE,
        gl::TEXTURE_COORD_ARRAY_STRIDE,
    ]
    .into_iter()
    .zip(&mut texcoord)
    {
        g.GetIntegerv(p, v);
    }
    log!(
        "[DScolor state] key={:08x} count={} tex_matrix={:?} wrap={:?} filters=[min=0x{:x},mag=0x{:x}] texcoord=[size={},type=0x{:x},stride={}]",
        key,
        count,
        tex_matrix,
        wrap,
        filters[0],
        filters[1],
        texcoord[0],
        texcoord[1],
        texcoord[2]
    );
    if key == 0x62bf_6ba7 && count == 2430 {
        let mut pointer: *const std::ffi::c_void = std::ptr::null();
        g.GetPointerv(gl::TEXTURE_COORD_ARRAY_POINTER, &mut pointer);
        if !pointer.is_null() && texcoord[0] > 0 {
            let size = texcoord[0] as usize;
            let stride = if texcoord[2] == 0 {
                size * 4
            } else {
                texcoord[2] as usize
            };
            let mut first = Vec::with_capacity(8);
            for index in 0..8 {
                let base = pointer.cast::<u8>().add(index * stride);
                let coords: Vec<f32> = match texcoord[1] as u32 {
                    gl::FLOAT => (0..size)
                        .map(|component| base.cast::<f32>().add(component).read_unaligned())
                        .collect(),
                    gl::FIXED => (0..size)
                        .map(|component| {
                            base.cast::<i32>().add(component).read_unaligned() as f32 / 65536.0
                        })
                        .collect(),
                    gl::SHORT => (0..size)
                        .map(|component| {
                            base.cast::<i16>().add(component).read_unaligned() as f32 / 32768.0
                        })
                        .collect(),
                    _ => break,
                };
                first.push(coords);
            }
            log!("[DScolor texcoord] key={:08x} first={:?}", key, first);
        }
    }
    let mut color_info = [0; 3];
    for (p, v) in [0x8081, 0x8082, 0x8898].into_iter().zip(&mut color_info) {
        g.GetIntegerv(p, v);
    }
    if key == 0x62bf_6ba7
        && count == 2430
        && g.IsEnabled(gl::COLOR_ARRAY) != 0
        && (3..=4).contains(&color_info[0])
    {
        let mut ptr: *const std::ffi::c_void = std::ptr::null();
        g.GetPointerv(0x8090, &mut ptr);
        if !ptr.is_null() {
            let size = color_info[0] as usize;
            let component_bytes = match color_info[1] as u32 {
                gl::BYTE | gl::UNSIGNED_BYTE => 1,
                gl::SHORT | gl::UNSIGNED_SHORT => 2,
                gl::FLOAT | gl::FIXED => 4,
                _ => 0,
            };
            if component_bytes != 0 {
                let stride = if color_info[2] == 0 {
                    size * component_bytes
                } else {
                    color_info[2] as usize
                };
                if stride >= size * component_bytes {
                    let read = |base: *const u8, component: usize| -> f32 {
                        let offset = component * component_bytes;
                        match color_info[1] as u32 {
                            gl::UNSIGNED_BYTE => base.add(offset).read() as f32 / 255.0,
                            gl::BYTE => base.add(offset).cast::<i8>().read() as f32 / 127.0,
                            gl::UNSIGNED_SHORT => {
                                base.add(offset).cast::<u16>().read_unaligned() as f32 / 65535.0
                            }
                            gl::SHORT => {
                                base.add(offset).cast::<i16>().read_unaligned() as f32 / 32767.0
                            }
                            gl::FIXED => {
                                base.add(offset).cast::<i32>().read_unaligned() as f32 / 65536.0
                            }
                            gl::FLOAT => base.add(offset).cast::<f32>().read_unaligned(),
                            _ => unreachable!(),
                        }
                    };
                    let mut first = [[1.0; 4]; 8];
                    let mut min = [f32::INFINITY; 4];
                    let mut max = [f32::NEG_INFINITY; 4];
                    let mut sum = [0.0; 4];
                    let vertices = count.max(0) as usize;
                    for index in 0..vertices {
                        let base = ptr.cast::<u8>().add(index * stride);
                        for component in 0..size {
                            let value = read(base, component);
                            if index < first.len() {
                                first[index][component] = value;
                            }
                            min[component] = min[component].min(value);
                            max[component] = max[component].max(value);
                            sum[component] += value;
                        }
                    }
                    let mean = sum.map(|value| value / vertices.max(1) as f32);
                    log!(
                        "[DScolor vertex] key={:08x} count={} info={:?} first={:?} min={:?} max={:?} mean={:?}",
                        key,
                        count,
                        color_info,
                        first,
                        min,
                        max,
                        mean
                    );
                }
            }
        }
    }
}

pub(crate) fn texenv(target: u32, pname: u32, value: f32, prior: u32, error: u32) {
    if enabled() && once(&format!("texenv:{:x}:{:x}:{:x}", target, pname, error)) {
        log!(
            "[DScolor texenv] target=0x{:x} pname=0x{:x} first={} prior=0x{:x} error=0x{:x}",
            target,
            pname,
            value,
            prior,
            error
        );
    }
}

pub(crate) unsafe fn capture(g: &mut dyn GLES, label: &str, x: i32, y: i32, w: i32, h: i32) {
    if !enabled() || w <= 0 || h <= 0 || w as i64 * h as i64 > 16_000_000 {
        return;
    }
    let frame = crate::window::DS_FRAME_COUNTER.load(std::sync::atomic::Ordering::Relaxed);
    let wanted = if std::env::var_os("TOUCHHLE_DS_COLOR_TRIGGER_FILE").is_some() {
        trigger().is_some_and(|(_, start)| {
            let elapsed = frame.saturating_sub(start);
            elapsed <= 600 && elapsed % 120 == 0
        })
    } else {
        std::env::var("TOUCHHLE_DS_COLOR_FRAMES")
            .unwrap_or_else(|_| "1500,1800,2400,4000".into())
            .split(',')
            .any(|v| v.parse::<u64>().ok() == Some(frame))
    };
    if !wanted || !once(&format!("capture:{}:{}", label, frame)) {
        return;
    }
    let Ok(dir) = std::env::var("TOUCHHLE_DS_COLOR_DIR") else {
        return;
    };
    let mut data = vec![0u8; w as usize * h as usize * 4];
    g.ReadPixels(
        x,
        y,
        w,
        h,
        gl::RGBA,
        gl::UNSIGNED_BYTE,
        data.as_mut_ptr().cast(),
    );
    let mut ppm = format!("P6\n{} {}\n255\n", w, h).into_bytes();
    let row = w as usize * 4;
    for line in data.chunks_exact(row).rev() {
        for rgba in line.chunks_exact(4) {
            ppm.extend_from_slice(&rgba[..3]);
        }
    }
    let path = std::path::Path::new(&dir).join(format!("{}-f{}.ppm", label, frame));
    let _ = std::fs::create_dir_all(&dir);
    match std::fs::write(&path, ppm) {
        Ok(()) => log!(
            "[DScolor capture] label={} f={} {}x{} path={}",
            label,
            frame,
            w,
            h,
            path.display()
        ),
        Err(e) => log!("[DScolor capture] failed: {}", e),
    }
}
