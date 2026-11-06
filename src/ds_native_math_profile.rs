//! Optional original native-math call timings. Inclusive elapsed, not CPU or additive rows.
use crate::{cpu::Cpu, mem::ConstVoidPtr, Environment};
use std::time::{Duration, Instant};
struct Target {
    at: u32,
    len: usize,
    hash: u64,
    push: u32,
    name: &'static str,
}
const BKPT: u32 = 0xe120_0070;
const TARGETS: [Target; 6] = [
    Target {
        at: 0x470e10,
        len: 352,
        hash: 15108973501790637656,
        push: 0xe92d40f0,
        name: "m3g::SkinnedMesh::GetBoneTransforms",
    },
    Target {
        at: 0x54acdc,
        len: 2784,
        hash: 8864950408480463809,
        push: 0xe92d40f0,
        name: "m3g::OpenGLESRenderer::ProcessVertexBufferNormalsSoftwareSkinnedRange",
    },
    Target {
        at: 0x54b7bc,
        len: 2044,
        hash: 17521011096042093999,
        push: 0xe92d40f0,
        name: "m3g::OpenGLESRenderer::ProcessVertexBufferPositionsSoftwareSkinnedRange",
    },
    Target {
        at: 0x561ab4,
        len: 2784,
        hash: 6495204319146205461,
        push: 0xe92d40f0,
        name: "m3g::OpenGLES11Renderer::ProcessVertexBufferNormalsSoftwareSkinnedRange",
    },
    Target {
        at: 0x562594,
        len: 2044,
        hash: 14346199516750005908,
        push: 0xe92d40f0,
        name: "m3g::OpenGLES11Renderer::ProcessVertexBufferPositionsSoftwareSkinnedRange",
    },
    Target {
        at: 0x4ea124,
        len: 80,
        hash: 4286019995833382226,
        push: 0xe92d4080,
        name: "m3g::Transform::TransformVector",
    },
];
#[derive(Default)]
pub(crate) struct State {
    armed: [bool; 6],
    calls: [u64; 6],
    elapsed_ns: [u128; 6],
    last: Option<Instant>,
}
pub(crate) fn prime(env: &mut Environment) {
    if std::env::var("TOUCHHLE_DS_NATIVE_MATH_PROFILE").as_deref() != Ok("1")
        || env.bundle.bundle_identifier() != "com.ea.deadspace.bv"
    {
        return;
    }
    let mut count = 0;
    for (i, t) in TARGETS.iter().enumerate() {
        let Some(data) = env
            .mem
            .get_bytes_fallible(ConstVoidPtr::from_bits(t.at), t.len as u32)
        else {
            continue;
        };
        let hash = data.iter().fold(0xcbf29ce484222325u64, |h, b| {
            (h ^ u64::from(*b)).wrapping_mul(0x100000001b3)
        });
        if data.len() != t.len
            || hash != t.hash
            || u32::from_le_bytes(data[..4].try_into().unwrap()) != t.push
        {
            echo!("[DSmath] unknown original {}; not armed", t.name);
            continue;
        }
        env.mem
            .write(crate::mem::MutPtr::<u32>::from_bits(t.at), BKPT);
        env.cpu.invalidate_cache_range(t.at, 4);
        env.framework_state.opengles.ds_native_math_profile.armed[i] = true;
        count += 1;
    }
    echo!("[DSmath] original math profile armed={} inclusive elapsed includes nested calls/bridge/scheduling, never sum parent and child or call this CPU time",count);
}
pub(crate) fn handle_breakpoint(env: &mut Environment) -> bool {
    let pc = env.cpu.regs()[Cpu::PC];
    let Some(i) = TARGETS.iter().position(|t| t.at == pc) else {
        return false;
    };
    if !env.framework_state.opengles.ds_native_math_profile.armed[i] {
        return false;
    }
    let start = Instant::now();
    crate::ds_native_call::original_after_push(env, pc, TARGETS[i].push);
    let elapsed = start.elapsed().as_nanos();
    let state = &mut env.framework_state.opengles.ds_native_math_profile;
    state.calls[i] += 1;
    state.elapsed_ns[i] += elapsed;
    let now = Instant::now();
    if state
        .last
        .is_none_or(|last| now.duration_since(last) >= Duration::from_secs(2))
    {
        state.last = Some(now);
        for (i, t) in TARGETS.iter().enumerate() {
            if state.calls[i] != 0 {
                echo!(
                    "[DSmath] name={} calls={} inclusive_ns={} not_exclusive_cpu",
                    t.name,
                    state.calls[i],
                    state.elapsed_ns[i]
                );
            }
        }
    }
    true
}
