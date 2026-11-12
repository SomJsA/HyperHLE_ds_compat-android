//! Observe original dialogue timing; replace only mapped subtitle drawing.
#[path = "ds_subtitle_policy.rs"]
mod policy;
use crate::abi::{CallFromHost, GuestFunction};
use crate::ds_chapter_overlay::{patch, wide, word};
use crate::Environment;
use std::sync::atomic::{AtomicBool, AtomicU32, AtomicU64, Ordering};

static READY: AtomicBool = AtomicBool::new(false);
static SNAPSHOT: AtomicU64 = AtomicU64::new(0);
static TIMING:AtomicU64=AtomicU64::new(0);
static TIMING_SERIAL:AtomicU32=AtomicU32::new(0);
static MENU_CONTEXT:AtomicU64=AtomicU64::new(0);
static MENU_HUD_STATE:AtomicU32=AtomicU32::new(u32::MAX);
static MENU_SAVED_CONTEXT:AtomicU64=AtomicU64::new(0);
static MENU_RESUME:AtomicBool=AtomicBool::new(false);


pub(crate) fn text_entry(id: &str) -> Option<u32> { policy::entry(id) }

#[derive(Default)]
pub(crate) struct State {
    checked: bool,
    supported: bool,
    suppressed: bool,
    last: Option<(u32, u32, u32, u32)>,
    index: Option<u32>,
    serial: u32,
    timing_owner: (u32, u32),
    cues: Vec<(u32, u32, i64)>,
    display_key: Option<(u32, u32, u32)>,
    display_serial: u32,
}

fn suppress(env: &mut Environment, desired: bool) -> bool {
    let current = env.framework_state.opengles.ds_subtitle.suppressed;
    if current == desired {
        return true;
    }
    if !policy::preflight(policy::SITES.map(|(a, _, _)| word(env, a)), current) {
        echo!("[DSsubtitle-native] preflight failed; custom display withheld");
        return false;
    }
    for (address, original, skip) in policy::SITES {
        patch(env, address, if desired { skip } else { original });
    }
    env.framework_state.opengles.ds_subtitle.suppressed = desired;
    echo!(
        "[DSsubtitle-native] {}",
        if desired { "hidden" } else { "restored" }
    );
    true
}

pub(crate) fn observe(env: &mut Environment) {
    if std::env::var_os("TOUCHHLE_DS_SUBTITLE_OVERLAY").is_none() {
        return;
    }
    if !env.framework_state.opengles.ds_subtitle.checked {
        if !env
            .bundle
            .bundle_identifier()
            .to_ascii_lowercase()
            .contains("deadspace")
        {
            return;
        }
        let supported = policy::preflight(policy::SITES.map(|(a, _, _)| word(env, a)), false);
        let state = &mut env.framework_state.opengles.ds_subtitle;
        state.checked = true;
        state.supported = supported;
        echo!(
            "[DSsubtitle-hook] supported={} entries=original-unreviewed",
            supported
        );
    }
    if !env.framework_state.opengles.ds_subtitle.supported {
        return;
    }
    let hud: u32 =
        GuestFunction::from_addr_and_thumb_flag(0x0003_0474, false).call_from_host(env, ());
    let world = if hud >= 0x1000 {
        word(env, hud.saturating_add(4)).unwrap_or(0)
    } else {
        0
    };
    let context=if hud>=0x1000&&world>=0x1000{(u64::from(hud)<<32)|u64::from(world)}else{0};
    let hud_state=word(env,hud.saturating_add(0x234)).unwrap_or(u32::MAX);
    MENU_CONTEXT.store(context,Ordering::Release);MENU_HUD_STATE.store(hud_state,Ordering::Release);
    if MENU_RESUME.swap(false,Ordering::AcqRel)&&policy::resume_menu(MENU_SAVED_CONTEXT.swap(0,Ordering::AcqRel),context,hud_state){
        // Same borrowed StringIdEvent view already verified by the PC native
        // HUD path. Never write HUD state or invoke a generic owning handler.
        let expected:Vec<u8>="BTN_PLAY_PAUSE".chars().flat_map(|c|(c as u32).to_le_bytes()).collect();
        let begin=0x008e_e3a0u32;let end=begin+expected.len() as u32;
        if env.mem.get_bytes_fallible(crate::mem::ConstVoidPtr::from_bits(begin),expected.len() as u32)==Some(expected.as_slice()){
            let event=env.mem.alloc(24);
            for (i,value) in [0,0,begin,end,end,0].into_iter().enumerate(){env.mem.write(crate::mem::MutPtr::<u32>::from_bits(event.to_bits()+i as u32*4),value);}
            let _:u32=GuestFunction::from_addr_and_thumb_flag(0x0019_4158,false).call_from_host(env,(hud,event.to_bits()));
            env.mem.free(event);
            echo!("[DScaption-menu] continue native HUD {}->{}",hud_state,word(env,hud.saturating_add(0x234)).unwrap_or(u32::MAX));
        }
    }
    let track = if world >= 0x1000 {
        word(env, hud.saturating_add(0x1220)).unwrap_or(0)
    } else {
        0
    };
    let segment = if track >= 0x1000 {
        word(env, track.saturating_add(4)).unwrap_or(u32::MAX)
    } else {
        u32::MAX
    };
    // Include world ownership so restarting a level cannot reuse an old caption.
    let key = (hud, world, track, segment);
    if env.framework_state.opengles.ds_subtitle.last != Some(key) {
        let id = if track >= 0x1000 {
            word(env, track.saturating_add(0x10))
                .zip(word(env, track.saturating_add(0x14)))
                .and_then(|(begin, end)| policy::valid_segment(begin, end, segment))
                .and_then(|address| wide(env, address))
        } else {
            None
        };
        let index = id.as_deref().and_then(policy::entry);
        let state = &mut env.framework_state.opengles.ds_subtitle;
        state.index = index;
        state.last = Some(key);
        state.serial = state.serial.wrapping_add(1);
        echo!(
            "[DSsubtitle-event] serial={} track={:#x} segment={} id={:?} mapped={}",
            state.serial,
            track,
            segment,
            id,
            index.is_some()
        );
    }
    if env.framework_state.opengles.ds_subtitle.timing_owner != (world, track) {
        let mut cues = Vec::new();
        if let Some((begin, end, times)) = word(env, track.saturating_add(0x10))
            .zip(word(env, track.saturating_add(0x14)))
            .zip(word(env, track.saturating_add(0x24)))
            .and_then(|((a,b),t)| policy::valid_segment(a,b,0).map(|_|(a,b,t))) {
            let count = (end - begin) / 20;
            if times >= 0x1000 && word(env,track.saturating_add(0x28))
                .and_then(|end|end.checked_sub(times)) == Some(count * 4) {
                for i in 0..count {
                    let address = begin + i * 20;
                    // The opening track includes empty IDs for intentional
                    // silence. Keep their durations in the absolute timeline.
                    let id = if word(env,address).zip(word(env,address+4))
                        .map(|(a,b)|a==b).unwrap_or(false) { Some(String::new()) }
                        else { wide(env,address) };
                    let cue = id.zip(word(env, times + i * 4));
                    if let Some((id, duration)) = cue {
                        if let Some(index) = if id.is_empty() { Some(0) } else { policy::entry(&id) } {
                            if (100..=600_000).contains(&duration) {
                                cues.push((index, duration, policy::opening_offset(&id)));
                                continue;
                            }
                        }
                    }
                    cues.clear(); break;
                }
            }
        }
        let state = &mut env.framework_state.opengles.ds_subtitle;
        state.timing_owner = (world, track);
        state.cues = cues;
    }
    let state = &env.framework_state.opengles.ds_subtitle;
    let finished=env.mem.get_bytes_fallible(crate::mem::ConstVoidPtr::from_bits(track),1).and_then(|v|v.first()).copied();
    let adjusted = if finished == Some(0) && !state.cues.is_empty() {
        word(env, track.saturating_add(8)).and_then(|left|
            policy::adjusted_cue(&state.cues, segment, left as i32))
    } else { None };
    let uses_timeline = !state.cues.is_empty();
    let desired = READY.load(Ordering::Acquire);
    let patched = suppress(env, desired);
    let state = &env.framework_state.opengles.ds_subtitle;
    let (display_segment, index) = if uses_timeline {
        adjusted.map(|(position,index,_)|(position,Some(index))).unwrap_or((u32::MAX,None))
    } else { (segment, state.index.filter(|_|finished==Some(0))) };
    let native_timing=if desired&&patched&&index.is_some() {
        let caption_bytes=word(env,track.saturating_add(0x10)).zip(word(env,track.saturating_add(0x14)))
            .and_then(|(a,b)|b.checked_sub(a)).filter(|n|*n>0&&*n%20==0&&*n<=20*512).map(|n|n/20*4);
        let duration=word(env,track.saturating_add(0x24)).zip(word(env,track.saturating_add(0x28)))
            .filter(|(a,b)|*a>=0x1000&&caption_bytes.is_some()&&b.checked_sub(*a)==caption_bytes)
            .and_then(|(a,_)|a.checked_add(segment*4)).and_then(|a|word(env,a));
        duration.zip(word(env,track.saturating_add(8))).map(|(total,left)|policy::timing_value(total,left as i32)).unwrap_or(0)
    }else{0};
    let state = &mut env.framework_state.opengles.ds_subtitle;
    let key = index.map(|_|(world,track,display_segment));
    if key != state.display_key {
        state.display_key = key;
        state.display_serial = state.display_serial.wrapping_add(1).max(1);
        if let Some(index) = index {
            echo!("[DSsubtitle-display] serial={} index={} cue={} timing_adjusted={}",
                state.display_serial,index,display_segment,uses_timeline);
        }
    }
    let value = policy::visible_value(state.display_serial,index,finished);
    let timing = adjusted.map(|(_,_,timing)|timing).unwrap_or(native_timing);
    TIMING_SERIAL.store(0,Ordering::Release);
    TIMING.store(timing,Ordering::Release);
    TIMING_SERIAL.store((value>>32) as u32,Ordering::Release);
    SNAPSHOT.store(
        if desired && patched { value } else { 0 },
        Ordering::Release,
    );
}

#[no_mangle]
pub extern "system" fn Java_org_touchhle_android_SubtitleOverlay_nativeSnapshot(
    _env: *mut std::ffi::c_void,
    _class: *mut std::ffi::c_void,
) -> i64 {
    SNAPSHOT.load(Ordering::Acquire) as i64
}

#[no_mangle]
pub extern "system" fn Java_org_touchhle_android_SubtitleOverlay_nativeSetReady(
    _env: *mut std::ffi::c_void,
    _class: *mut std::ffi::c_void,
    ready: u8,
) {
    READY.store(ready != 0, Ordering::Release);
    if ready == 0 {
        SNAPSHOT.store(0, Ordering::Release);
    }
}

/// Timing is accepted only for the exact captured subtitle serial.
pub(crate) fn host_timing(snapshot:u64)->u64 {
    let serial=(snapshot>>32) as u32;
    if snapshot==0||TIMING_SERIAL.load(Ordering::Acquire)!=serial{return 0;}
    let value=TIMING.load(Ordering::Acquire);
    if TIMING_SERIAL.load(Ordering::Acquire)==serial{value}else{0}
}
#[no_mangle]
pub extern "system" fn Java_org_touchhle_android_SubtitleOverlay_nativeTiming(
    _env:*mut std::ffi::c_void,_class:*mut std::ffi::c_void,snapshot:i64,
)->i64 {host_timing(snapshot as u64) as i64}

#[no_mangle]
pub extern "system" fn Java_org_touchhle_android_MainActivity_nativePrepareCaptionMenu(_env:*mut std::ffi::c_void,_class:*mut std::ffi::c_void){
    let state=MENU_HUD_STATE.load(Ordering::Acquire);
    MENU_SAVED_CONTEXT.store(if matches!(state,0|1){MENU_CONTEXT.load(Ordering::Acquire)}else{0},Ordering::Release);
}
#[no_mangle]
pub extern "system" fn Java_org_touchhle_android_MainActivity_nativeResumeCaptionMenu(_env:*mut std::ffi::c_void,_class:*mut std::ffi::c_void){
    if MENU_SAVED_CONTEXT.load(Ordering::Acquire)!=0{MENU_RESUME.store(true,Ordering::Release);}
}
