//! Experimental single GL owner per SDL window. Default off.
//! Pointer calls wait for completion; no Environment/Window is sent to worker.
use super::{gles11_raw as gl,gles1_native::NativeDriverState,gles_generic::GLES};
use std::{collections::HashMap,ffi::{CStr,CString},sync::{Arc,Mutex,atomic::{AtomicBool,AtomicUsize,Ordering},mpsc::{self,SyncSender}},thread::JoinHandle};

type Job=Box<dyn FnOnce(&mut Owner)+Send+'static>;
enum Message { Run(Job),Stop }
pub(crate) struct Actor {
    sender:SyncSender<Message>,thread:Mutex<Option<JoinHandle<()>>>,
    selected:AtomicUsize,closed:AtomicBool,pub(crate) queued:bool,
}
#[link(name="EGL")]
extern "C" {
 fn eglGetCurrentDisplay()->*mut std::ffi::c_void;
 fn eglQueryContext(d:*mut std::ffi::c_void,c:*mut std::ffi::c_void,a:i32,v:*mut i32)->u32;
 fn eglChooseConfig(d:*mut std::ffi::c_void,a:*const i32,c:*mut *mut std::ffi::c_void,n:i32,count:*mut i32)->u32;
 fn eglCreatePbufferSurface(d:*mut std::ffi::c_void,c:*mut std::ffi::c_void,a:*const i32)->*mut std::ffi::c_void;
 fn eglDestroySurface(d:*mut std::ffi::c_void,s:*mut std::ffi::c_void)->u32;
 fn eglMakeCurrent(d:*mut std::ffi::c_void,draw:*mut std::ffi::c_void,read:*mut std::ffi::c_void,c:*mut std::ffi::c_void)->u32;
 fn eglGetError()->i32;
}
struct Owner {window:usize,current:usize,drivers:HashMap<usize,NativeDriverState>,swaps:u64,
 display:usize,parking:HashMap<usize,usize>,background:bool,in_parking:bool}
fn error()->String {unsafe{CStr::from_ptr(sdl2_sys::SDL_GetError()).to_string_lossy().into_owned()}}
impl Owner {
    fn bind(&mut self,context:usize)->Result<(),String>{
        if self.current==context && self.in_parking==(self.background && context!=0){return Ok(());}
        if self.in_parking {
            if unsafe{eglMakeCurrent(self.display as *mut _,std::ptr::null_mut(),std::ptr::null_mut(),std::ptr::null_mut())}==0 {
                return Err(format!("Parking detach EGL=0x{:x}",unsafe{eglGetError()}));
            }
            self.current=0;self.in_parking=false;
        }
        if self.background && context!=0 {
            let surface=self.parking_surface(context)?;
            if unsafe{eglMakeCurrent(self.display as *mut _,surface as *mut _,surface as *mut _,context as *mut _)}==0 {
                return Err(format!("Parking bind EGL=0x{:x}",unsafe{eglGetError()}));
            }
            self.current=context;self.in_parking=true;return Ok(());
        }
        let window=if context==0 {0}else{self.window};
        if unsafe{sdl2_sys::SDL_GL_MakeCurrent(window as *mut _,context as *mut _)}!=0 {return Err(error());}
        self.current=context;Ok(())
    }
    fn parking_surface(&mut self,context:usize)->Result<usize,String>{
        if let Some(surface)=self.parking.get(&context){return Ok(*surface);}
        let mut id=0;let mut count=0;let mut config=std::ptr::null_mut();
        let display=self.display as *mut _;
        if unsafe{eglQueryContext(display,context as *mut _,0x3028,&mut id)}==0{return Err("EGL context config query".into());}
        let attrs=[0x3028,id,0x3033,1,0x3038];
        if unsafe{eglChooseConfig(display,attrs.as_ptr(),&mut config,1,&mut count)}==0||count!=1{return Err("Context config has no parking pbuffer".into());}
        let size=[0x3057,1,0x3056,1,0x3038];let surface=unsafe{eglCreatePbufferSurface(display,config,size.as_ptr())}as usize;
        if surface==0{return Err(format!("Parking surface EGL=0x{:x}",unsafe{eglGetError()}));}
        self.parking.insert(context,surface);Ok(surface)
    }
    fn set_background(&mut self,background:bool,selected:usize)->Result<(),String>{
        if self.background==background{return Ok(());}
        self.bind(0)?;
        self.background=background;
        if background && selected!=0 {self.bind(selected)?;unsafe{gl::Finish();}}
        log!("[DSrender-thread] background={} window_surface_released={} parking_context={:#x}",background,background,if background {selected}else{0});
        Ok(())
    }
    fn remove(&mut self,context:usize){
        if self.drivers.remove(&context).is_none(){return;}
        if self.current==context{self.bind(0).expect("Render owner detach before delete");}
        if let Some(surface)=self.parking.remove(&context){unsafe{eglDestroySurface(self.display as *mut _,surface as *mut _);}}
        unsafe{sdl2_sys::SDL_GL_DeleteContext(context as *mut _);}
    }
}
impl Drop for Owner {
    fn drop(&mut self){
        let _=self.bind(0);
        for(_,surface)in self.parking.drain(){unsafe{eglDestroySurface(self.display as *mut _,surface as *mut _);}}
        for(context,_)in self.drivers.drain(){unsafe{sdl2_sys::SDL_GL_DeleteContext(context as *mut _);}}
    }
}
impl Actor {
    pub(crate) fn requested()->Option<bool>{match std::env::var("TOUCHHLE_DS_RENDER_THREAD").as_deref(){Ok("sync")=>Some(false),Ok("queue")=>Some(true),_=>None}}
    pub(crate) fn new(window:usize,queued:bool)->Result<Arc<Self>,String>{
        let(tx,rx)=mpsc::sync_channel(256);
        let thread=std::thread::Builder::new().name("DS-render-owner".into()).spawn(move||{
            gl::load_with(|name|{let name=CString::new(name).unwrap();unsafe{sdl2_sys::SDL_GL_GetProcAddress(name.as_ptr())as *const _}});
            let mut owner=Owner {window,current:0,drivers:HashMap::new(),swaps:0,display:0,parking:HashMap::new(),background:false,in_parking:false};
            while let Ok(message)=rx.recv(){match message{Message::Run(job)=>job(&mut owner),Message::Stop=>break}}
        }).map_err(|e|e.to_string())?;
        Ok(Arc::new(Self {sender:tx,thread:Mutex::new(Some(thread)),selected:AtomicUsize::new(0),closed:AtomicBool::new(false),queued}))
    }
    fn exchange<R:Send+'static>(&self,f:impl FnOnce(&mut Owner)->R+Send+'static)->R{
        let(tx,rx)=mpsc::sync_channel(1);
        self.sender.send(Message::Run(Box::new(move|owner|{let value=f(owner);let _=tx.send(value);}))).expect("Render owner unavailable");
        match rx.recv(){Ok(value)=>value,Err(_)=>{self.join();panic!("Render owner stopped before completing a synchronous borrow");}}
    }
    pub(crate) fn register(&self,context:usize)->Result<(),String>{
        let result=self.exchange(move|owner|{
            owner.bind(context)?;
            owner.display=unsafe{eglGetCurrentDisplay()}as usize;
            let mut driver=NativeDriverState::for_owner();unsafe{driver.initialize_current();}
            owner.drivers.insert(context,driver);Ok(())
        });
        if result.is_ok(){self.select(context);log!("[DSrender-thread] registered context={:#x} queued_scalar={} worker_owns_all_GL=true; draws/pointers synchronous until owned snapshots are linked",context,self.queued);}
        result
    }
    pub(crate) fn select(&self,context:usize){self.selected.store(context,Ordering::Release);}
    pub(crate) fn selected_context(&self)->usize{self.selected.load(Ordering::Acquire)}
    pub(crate) fn is_selected(&self,context:usize)->bool{self.selected.load(Ordering::Acquire)==context}
    pub(crate) fn quiesce(&self)->Result<(),String>{self.exchange(|owner|owner.bind(0))}
    pub(crate) fn suspend(&self)->Result<(),String>{let selected=self.selected_context();self.exchange(move|owner|owner.set_background(true,selected))}
    pub(crate) fn resume(&self)->Result<(),String>{let selected=self.selected_context();self.exchange(move|owner|owner.set_background(false,selected))}
    pub(crate) fn remove(&self,context:usize){
        if self.closed.load(Ordering::Acquire){return;}
        self.exchange(move|owner|owner.remove(context));
        let _=self.selected.compare_exchange(context,0,Ordering::AcqRel,Ordering::Acquire);
    }
    pub(crate) fn call<R:Send+'static>(&self,context:usize,f:impl FnOnce(&mut NativeDriverState)->R+Send+'static)->R{
        self.exchange(move|owner|{owner.bind(context).expect("Render owner context bind");f(owner.drivers.get_mut(&context).expect("Unknown render context"))})
    }
    pub(crate) fn submit(&self,context:usize,f:impl FnOnce(&mut NativeDriverState)+Send+'static){
        self.sender.send(Message::Run(Box::new(move|owner|{owner.bind(context).expect("Render owner context bind");f(owner.drivers.get_mut(&context).expect("Unknown render context"));}))).expect("Render queue stopped");
    }
    pub(crate) fn swap(&self){
        let context=self.selected.load(Ordering::Acquire);
        self.exchange(move|owner|{
            owner.bind(context).expect("Render owner present context");
            if owner.background {unsafe{gl::Flush();}return;}
            unsafe{sdl2_sys::SDL_GL_SwapWindow(owner.window as *mut _);}
            owner.swaps+=1;
            if owner.swaps<=4||owner.swaps%120==0{log!("[DSrender-thread] actual_SDL_swap={} context={:#x} worker={:?}",owner.swaps,context,std::thread::current().id());}
        });
    }
    fn join(&self){if let Some(thread)=self.thread.lock().unwrap().take(){let _=thread.join();}}
    pub(crate) fn shutdown(&self){if !self.closed.swap(true,Ordering::AcqRel){let _=self.sender.send(Message::Stop);}self.join();}
}
impl Drop for Actor {fn drop(&mut self){self.shutdown();}}
pub(crate) struct Proxy {pub(crate) actor:Arc<Actor>,pub(crate) context:usize}
include!("ds_render_proxy.rs");
