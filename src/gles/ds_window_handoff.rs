//! Explicit boot-only SDL2 context handoff probe. No Environment or game data.
use super::gles11_raw as gl;
use std::ffi::{CStr, CString};

fn sdl_error() -> String {
    unsafe { CStr::from_ptr(sdl2_sys::SDL_GetError()).to_string_lossy().into_owned() }
}

pub(crate) fn run(window: usize) -> Result<(), String> {
    let context = unsafe { sdl2_sys::SDL_GL_GetCurrentContext() } as usize;
    if window == 0 || context == 0 { return Err("No current SDL window/context".into()); }
    gl::load_with(|name| {
        let name = CString::new(name).unwrap();
        unsafe { sdl2_sys::SDL_GL_GetProcAddress(name.as_ptr()) as *const _ }
    });
    if unsafe { sdl2_sys::SDL_GL_MakeCurrent(std::ptr::null_mut(), std::ptr::null_mut()) } != 0 {
        return Err(format!("Main context detach: {}",sdl_error()));
    }
    // Only numeric handle ownership tokens cross the thread. The SDL objects
    // remain alive in the blocked caller until join and explicit rebind.
    let task = std::thread::Builder::new().name("DS-GL-handoff".into()).spawn(move || unsafe {
        if sdl2_sys::SDL_GL_MakeCurrent(window as *mut _, context as *mut _) != 0 {
            return Err(format!("Worker context bind: {}",sdl_error()));
        }
        struct Detach;
        impl Drop for Detach {
            fn drop(&mut self) { unsafe { sdl2_sys::SDL_GL_MakeCurrent(std::ptr::null_mut(),std::ptr::null_mut()); } }
        }
        let _detach = Detach;
        let mut texture = 0;
        let mut framebuffer = 0;
        let mut old_texture = 0;
        let mut old_framebuffer = 0;
        let mut clear = [0.0;4];
        gl::GetIntegerv(gl::TEXTURE_BINDING_2D,&mut old_texture);
        gl::GetIntegerv(gl::FRAMEBUFFER_BINDING_OES,&mut old_framebuffer);
        gl::GetFloatv(gl::COLOR_CLEAR_VALUE,clear.as_mut_ptr());
        gl::GenTextures(1,&mut texture);
        gl::BindTexture(gl::TEXTURE_2D,texture);
        gl::TexParameteri(gl::TEXTURE_2D,gl::TEXTURE_MIN_FILTER,gl::NEAREST as _);
        gl::TexParameteri(gl::TEXTURE_2D,gl::TEXTURE_MAG_FILTER,gl::NEAREST as _);
        gl::TexImage2D(gl::TEXTURE_2D,0,gl::RGBA as _,16,16,0,gl::RGBA,gl::UNSIGNED_BYTE,std::ptr::null());
        gl::GenFramebuffersOES(1,&mut framebuffer);
        gl::BindFramebufferOES(gl::FRAMEBUFFER_OES,framebuffer);
        gl::FramebufferTexture2DOES(gl::FRAMEBUFFER_OES,gl::COLOR_ATTACHMENT0_OES,gl::TEXTURE_2D,texture,0);
        let complete = gl::CheckFramebufferStatusOES(gl::FRAMEBUFFER_OES)==gl::FRAMEBUFFER_COMPLETE_OES;
        let mut equal = complete;
        let colors = [[0u8,0,0,255],[255,0,0,255],[0,255,0,255],[0,0,255,255]];
        let mut tested = 0;
        if complete {
            for expected in colors.iter().cycle().take(8) {
                gl::ClearColor(expected[0]as f32/255.0,expected[1]as f32/255.0,expected[2]as f32/255.0,1.0);
                gl::Clear(gl::COLOR_BUFFER_BIT);
                // One pixel of a new self-drawn 16x16 fixture, before guest boot.
                // No game framebuffer, screenshot file or FPS window is read.
                let mut pixel = [0u8;4];
                gl::ReadPixels(0,0,1,1,gl::RGBA,gl::UNSIGNED_BYTE,pixel.as_mut_ptr().cast());
                equal &= pixel==*expected;
                tested+=1;
            }
        }
        gl::BindFramebufferOES(gl::FRAMEBUFFER_OES,old_framebuffer as _);
        gl::BindTexture(gl::TEXTURE_2D,old_texture as _);
        gl::DeleteFramebuffersOES(1,&framebuffer);
        gl::DeleteTextures(1,&texture);
        gl::ClearColor(0.03,0.06,0.07,1.0);
        gl::Clear(gl::COLOR_BUFFER_BIT);
        sdl2_sys::SDL_ClearError();
        sdl2_sys::SDL_GL_SwapWindow(window as *mut _);
        let swap_error=sdl_error();
        gl::ClearColor(clear[0],clear[1],clear[2],clear[3]);
        let error=gl::GetError();
        if !equal || error!=gl::NO_ERROR || !swap_error.is_empty() {
            return Err(format!("Worker fixture/swap failed: complete={} equal={} gl=0x{:x} SDL={}",complete,equal,error,swap_error));
        }
        Ok(tested)
    });
    let mut hash=0x2026_1103u64;
    let result=match task {
        Ok(task)=>{
            for i in 0..4_000_000u64 {hash=std::hint::black_box(hash.wrapping_mul(6364136223846793005).wrapping_add(i));}
            task.join().map_err(|_|"Worker panicked".to_owned()).and_then(|v|v)
        },
        Err(error)=>Err(format!("Worker spawn: {}",error)),
    };
    // Join happens before rebind even on worker failure. No two threads ever
    // make this context current simultaneously; no unsafe Send/Sync is used.
    if unsafe {sdl2_sys::SDL_GL_MakeCurrent(window as *mut _,context as *mut _)}!=0 {
        return Err(format!("Main context rebind: {}",sdl_error()));
    }
    if unsafe {sdl2_sys::SDL_GL_GetCurrentContext()}as usize!=context {
        return Err("SDL TLS current context was not restored".into());
    }
    let tested=result?;
    log!("[DSgl-handoff] passed worker_owned_window_context=true fixture_cases={} SDL_swap=true main_restored=true concurrent_cpu_hash={:#x}; boot component only, not threaded game/FPS",tested,hash);
    Ok(())
}
