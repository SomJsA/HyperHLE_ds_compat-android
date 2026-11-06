//! Opt-in native math with per-function calibration, periodic shadow and native fallback.
//! No screenshots, GL calls or guest state workers.
use crate::{
    abi::GuestFunction,
    cpu::Cpu,
    mem::{ConstVoidPtr, MutPtr},
    Environment,
};
use std::time::{Duration, Instant};

#[path = "ds_skin_math_bounds.rs"]
mod bounds;
use bounds::{overlap, span};
#[path = "ds_skin_math_policy.rs"]
mod policy;
use policy::{Action, Mode};

const ENTRIES: [u32; 2] = [0x0037_f814, 0x0037_fac8];
const BKPT: u32 = 0xe120_0070;
const PUSH: u32 = 0xe92d_40f0;
const FP_FLAGS: u32 = 0x9f;
const MAX_VERTICES: u32 = 32_768;

#[derive(Default)]
pub(crate) struct State {
    armed: bool,
    mode: Mode,
    disabled: bool,
    force_every: u64,
    accelerated: [u64; 2],
    native_calls: [u64; 2],
    forced_fallback: [u64; 2],
    capture_ns: [u128; 2],
    handler_ns: [u128; 2],
    calls: [u64; 2],
    vertices: [u64; 2],
    compared: [u64; 2],
    mismatches: [u64; 2],
    unsupported: [u64; 2],
    original_ns: [u128; 2],
    kernel_ns: [u128; 2],
    last_report: Option<Instant>,
}

fn bytes(env: &Environment, at: u32, len: usize) -> Option<&[u8]> {
    if at < 0x1000 || len == 0 || u64::from(at).checked_add(len as u64)? > 1u64 << 32 {
        return None;
    }
    let slice = env
        .mem
        .get_bytes_fallible(ConstVoidPtr::from_bits(at), len.try_into().ok()?)?;
    (slice.len() == len).then_some(slice)
}
fn word(env: &Environment, at: u32) -> Option<u32> {
    Some(u32::from_le_bytes(bytes(env, at, 4)?.try_into().ok()?))
}
fn fingerprint(env: &Environment, at: u32, len: usize) -> Option<u64> {
    Some(
        bytes(env, at, len)?
            .iter()
            .fold(0xcbf2_9ce4_8422_2325u64, |h, b| {
                (h ^ u64::from(*b)).wrapping_mul(0x100_0000_01b3)
            }),
    )
}
pub(crate) fn prime(env: &mut Environment) {
    let requested = std::env::var("TOUCHHLE_DS_SKIN_MATH").ok();
    let mode = Mode::parse(requested.as_deref());
    if !cfg!(target_arch = "aarch64") || mode == Mode::Off {
        return;
    }
    let known = env.bundle.bundle_identifier() == "com.ea.deadspace.bv"
        && fingerprint(env, ENTRIES[0], 692) == Some(55_936_553_603_488_786)
        && fingerprint(env, ENTRIES[1], 716) == Some(10_538_268_167_844_965_500)
        && fingerprint(env, 0x0053_d1a4, 16) == Some(5_222_870_166_602_214_087)
        && ENTRIES.iter().all(|at| word(env, *at) == Some(PUSH));
    if !known {
        echo!("[DSskin] unknown original skin/GetData instructions; shadow not armed");
        return;
    }
    for at in ENTRIES {
        env.mem.write(MutPtr::<u32>::from_bits(at), BKPT);
        env.cpu.invalidate_cache_range(at, 4);
    }
    env.framework_state.opengles.ds_skin_math.armed = true;
    env.framework_state.opengles.ds_skin_math.mode = mode;
    env.framework_state.opengles.ds_skin_math.force_every =
        std::env::var("TOUCHHLE_DS_SKIN_MATH_FORCE_FALLBACK_EVERY")
            .ok()
            .and_then(|v| v.parse().ok())
            .unwrap_or(0);

    echo!("[DSskin] armed mode={:?};64 native output/FP matches required per function, periodic shadow256; mismatch disables host for process; original timings include bridge/scheduling",mode);
}

struct Array {
    at: u32,
    stride: usize,
}
fn array(
    env: &Environment,
    global: u32,
    components: u32,
    size: usize,
    max_stride: usize,
) -> Result<Array, &'static str> {
    let obj = word(env, global).ok_or("array-global")?;
    let actual =
        word(env, obj.checked_add(68).ok_or("object-overflow")?).ok_or("array-components")?;
    let base = word(env, obj.checked_add(76).ok_or("object-overflow")?).ok_or("array-base")?;
    let offset = word(env, obj.checked_add(84).ok_or("object-overflow")?).ok_or("array-offset")?;
    let stride =
        word(env, obj.checked_add(88).ok_or("object-overflow")?).ok_or("array-stride")? as usize;
    if actual != components || stride < size || stride > max_stride || (size > 4 && stride % 4 != 0)
    {
        return Err("unsupported-array-layout");
    }
    Ok(Array {
        at: span(base, u64::from(offset), size)?,
        stride,
    })
}
struct Snapshot {
    dest: u32,
    count: u32,
    positions: Vec<f32>,
    weights: Vec<f32>,
    indices: Vec<i8>,
    matrices: Vec<f32>,
}
fn floats(
    slice: &[u8],
    stride: usize,
    components: usize,
    count: usize,
) -> Result<Vec<f32>, &'static str> {
    let mut result = Vec::with_capacity(count * components);
    for i in 0..count {
        for c in 0..components {
            let at = i * stride + c * 4;
            result.push(f32::from_le_bytes(slice[at..at + 4].try_into().unwrap()));
        }
    }
    Ok(result)
}
fn capture(env: &Environment, args: [u32; 4]) -> Result<Snapshot, &'static str> {
    let [dest, first, count, _] = args;
    if count == 0 || count > MAX_VERTICES || first > 1_048_576 {
        return Err("vertex-range");
    }
    let pos = array(env, 0x00ab_b844, 3, 12, 64)?;
    let idx = array(env, 0x00ab_b848, 4, 4, 16)?;
    let wgt = array(env, 0x00ab_b84c, 4, 16, 64)?;
    let range = |a: &Array, size: usize| -> Result<(u32, usize), &'static str> {
        let len = (count as usize - 1) * a.stride + size;
        span(a.at, u64::from(first) * a.stride as u64, len).map(|at| (at, len))
    };
    let pr = range(&pos, 12)?;
    let ir = range(&idx, 4)?;
    let wr = range(&wgt, 16)?;
    let destination = (
        span(dest, u64::from(first) * 12, count as usize * 12)?,
        count as usize * 12,
    );
    if [pr, ir, wr].iter().any(|r| overlap(destination, *r)) {
        return Err("aliased-output");
    }
    let positions = floats(
        bytes(env, pr.0, pr.1).ok_or("positions-unreadable")?,
        pos.stride,
        3,
        count as usize,
    )?;
    let weights = floats(
        bytes(env, wr.0, wr.1).ok_or("weights-unreadable")?,
        wgt.stride,
        4,
        count as usize,
    )?;
    let raw_indices = bytes(env, ir.0, ir.1).ok_or("indices-unreadable")?;
    let mut indices = Vec::with_capacity(count as usize * 4);
    let mut max_index = 0i8;
    for i in 0..count as usize {
        let mut active = true;
        for b in 0..4 {
            let w = weights[i * 4 + b];
            if b != 0 && w.to_bits() & 0x7000_0000 == 0 {
                active = false;
            }
            let index = raw_indices[i * idx.stride + b] as i8;
            if active {
                if index < 0 || !w.is_finite() {
                    return Err("unsupported-weight-index");
                }
                max_index = max_index.max(index);
            }
            indices.push(index);
        }
    }
    if positions.iter().any(|v| !v.is_finite()) {
        return Err("nonfinite-position");
    }
    let holder = word(env, 0x00ab_b83c).ok_or("palette-holder")?;
    let matrix_base =
        word(env, holder.checked_add(8).ok_or("palette-overflow")?).ok_or("palette-data")?;
    let mr = (
        span(matrix_base, 0, (max_index as usize + 1) * 64)?,
        (max_index as usize + 1) * 64,
    );
    if overlap(destination, mr) {
        return Err("aliased-palette");
    }
    let matrices = floats(
        bytes(env, mr.0, mr.1).ok_or("palette-unreadable")?,
        64,
        16,
        max_index as usize + 1,
    )?;
    if matrices.iter().any(|v| !v.is_finite()) {
        return Err("nonfinite-palette");
    }
    bytes(env, destination.0, destination.1).ok_or("destination-unreadable")?;
    Ok(Snapshot {
        dest: destination.0,
        count,
        positions,
        weights,
        indices,
        matrices,
    })
}

fn native_original(env: &mut Environment, entry: u32) {
    crate::ds_native_call::original_after_push(env, entry, PUSH);
}
fn calculate(snapshot: &Snapshot, kind: usize) -> (Vec<u8>, u32, u128) {
    let mut output = vec![0.0f32; snapshot.count as usize * 3];
    let start = Instant::now();
    let flags = unsafe {
        touchHLE_dynarmic_wrapper::touchHLE_skin_math(
            output.as_mut_ptr(),
            snapshot.positions.as_ptr(),
            snapshot.weights.as_ptr(),
            snapshot.indices.as_ptr(),
            snapshot.matrices.as_ptr(),
            snapshot.count,
            (kind == 0) as u32,
        )
    };
    let kernel_ns = start.elapsed().as_nanos();
    let bytes = output.into_iter().flat_map(f32::to_le_bytes).collect();
    (bytes, flags, kernel_ns)
}
pub(crate) fn handle_breakpoint(env: &mut Environment) -> bool {
    if !env.framework_state.opengles.ds_skin_math.armed {
        return false;
    }
    let Some(kind) = ENTRIES.iter().position(|at| *at == env.cpu.regs()[Cpu::PC]) else {
        return false;
    };
    let handler_start = Instant::now();
    let args = <[u32; 4]>::try_from(&env.cpu.regs()[..4]).unwrap();
    let before_fp = env.cpu.fpscr();
    let start = Instant::now();
    let snapshot = capture(env, args);
    let capture_ns = start.elapsed().as_nanos();
    let state = &env.framework_state.opengles.ds_skin_math;
    let forced = state.force_every != 0 && state.calls[kind] % state.force_every == 0;
    let action = policy::choose(
        state.mode,
        state.disabled,
        state.compared[kind],
        state.calls[kind],
        snapshot.is_ok(),
        state.force_every,
    );
    let kernel = if action != Action::Native {
        snapshot.as_ref().ok().map(|s| calculate(s, kind))
    } else {
        None
    };
    let mut original_ns = 0;
    let kernel_ns = kernel.as_ref().map_or(0, |k| k.2);
    let mut equal = None;
    let mut accelerated = false;
    if action == Action::Host {
        let s = snapshot.as_ref().unwrap();
        let (candidate, flags, _) = kernel.as_ref().unwrap();
        if let Some(dest) = env
            .mem
            .get_bytes_fallible_mut(ConstVoidPtr::from_bits(s.dest), candidate.len() as u32)
        {
            if dest.len() == candidate.len() {
                dest.copy_from_slice(candidate);
                accelerated = true;
            }
        }
        if accelerated {
            env.cpu.set_fpscr(before_fp | (*flags & FP_FLAGS));
            let return_to = env.cpu.regs()[Cpu::LR];
            env.cpu.branch(GuestFunction::from_addr_with_thumb_bit(return_to));
        }
    }
    if !accelerated {
        let start = Instant::now();
        native_original(env, ENTRIES[kind]);
        original_ns = start.elapsed().as_nanos();
        if action == Action::Compare {
            let s = snapshot.as_ref().unwrap();
            let (candidate, flags, _) = kernel.as_ref().unwrap();
            let actual = bytes(env, s.dest, candidate.len())
                .expect("Original skin output became unreadable");
            let after_fp = env.cpu.fpscr();
            equal = Some(
                actual == candidate.as_slice()
                    && (after_fp & FP_FLAGS) == ((before_fp | *flags) & FP_FLAGS),
            );
            if equal == Some(false)
                && env.framework_state.opengles.ds_skin_math.mismatches[kind] < 4
            {
                echo!("[DSskin] SHADOW MISMATCH kind={} first={} count={} byte={:?} fp_before={:#x} original={:#x} kernel={:#x}; host disabled, native output retained",kind,args[1],args[2],actual.iter().zip(candidate).position(|(a,b)|a!=b),before_fp,after_fp,flags);
            }
        }
    }
    let state = &mut env.framework_state.opengles.ds_skin_math;
    state.calls[kind] += 1;
    state.vertices[kind] += u64::from(args[2]);
    state.original_ns[kind] += original_ns;
    state.kernel_ns[kind] += kernel_ns;
    state.capture_ns[kind] += capture_ns;
    state.handler_ns[kind] += handler_start.elapsed().as_nanos();
    if accelerated {
        state.accelerated[kind] += 1;
    } else {
        state.native_calls[kind] += 1;
    }
    if forced {
        state.forced_fallback[kind] += 1;
    }
    match equal {
        Some(true) => state.compared[kind] += 1,
        Some(false) => {
            state.mismatches[kind] += 1;
            state.disabled = true;
        }
        None => {
            if snapshot.is_err() {
                state.unsupported[kind] += 1;
            }
        }
    }
    if state.calls[kind] <= 4 {
        echo!("[DSskin] sample mode={:?} kind={} first={} count={} eligibility={:?} action={:?} equal={:?} native_ns={} kernel_ns={}",state.mode,kind,args[1],args[2],snapshot.as_ref().err(),action,equal,original_ns,kernel_ns);
    }
    let now = Instant::now();
    if state
        .last_report
        .is_none_or(|last| now.duration_since(last) >= Duration::from_secs(2))
    {
        state.last_report = Some(now);
        echo!("[DSskin] mode={:?} calls={:?} vertices={:?} compared={:?} mismatches={:?} unsupported={:?} accelerated={:?} native_calls={:?} forced={:?} disabled={} original_ns={:?} kernel_ns={:?} capture_ns={:?} handler_ns={:?}; hook/observer overhead included",state.mode,state.calls,state.vertices,state.compared,state.mismatches,state.unsupported,state.accelerated,state.native_calls,state.forced_fallback,state.disabled,state.original_ns,state.kernel_ns,state.capture_ns,state.handler_ns);
    }
    true
}
