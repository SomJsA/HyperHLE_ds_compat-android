//! Bounded parameter/error diagnostics. No framebuffer, scene recognition or correction.
use std::{collections::HashSet,sync::{Mutex,OnceLock}};
pub(super) fn enabled() -> bool {
    static ENABLED: OnceLock<bool> = OnceLock::new();
    *ENABLED.get_or_init(|| std::env::var("TOUCHHLE_DS_TEXENV_TRACE").as_deref()==Ok("1"))
}
pub(super) fn record(api: &'static str,target:u32,pname:u32,first:u32,prior:u32,error:u32) {
    type Key=(&'static str,u32,u32,u32);
    static SEEN: OnceLock<Mutex<HashSet<Key>>> = OnceLock::new();
    let mut seen=SEEN.get_or_init(Default::default).lock().unwrap();
    if seen.len()<192 && seen.insert((api,target,pname,error)) {
        log!("[DStexenv] api={} target=0x{:x} pname=0x{:x} first_bits=0x{:08x} float_view={} prior=0x{:x} error=0x{:x}; parameters unchanged, consumed errors preserved",api,target,pname,first,f32::from_bits(first),prior,error);
    }
}
