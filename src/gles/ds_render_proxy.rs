// Generated from GLES1Native signatures; pointer calls synchronously finish before return.
use super::gles11_raw::types::*;
use super::gles_generic::GLchar;
impl GLES for Proxy {
    fn has_native_matrix_palette(&self) -> bool {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); g.has_native_matrix_palette() })
    }
    fn native_anisotropy_limit(&self) -> Option<GLfloat> {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); g.native_anisotropy_limit() })
    }
    fn is_native_es1(&self) -> bool {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); g.is_native_es1() })
    }
    unsafe fn driver_description(&self) -> String {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.driver_description() } })
    }
    unsafe fn GetError(&mut self) -> GLenum {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetError() } })
    }
    unsafe fn Enable(&mut self, cap: GLenum) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Enable(cap) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Enable(cap) } }); }
    }
    unsafe fn IsEnabled(&mut self, cap: GLenum) -> GLboolean {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.IsEnabled(cap) } })
    }
    unsafe fn Disable(&mut self, cap: GLenum) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Disable(cap) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Disable(cap) } }); }
    }
    unsafe fn ClientActiveTexture(&mut self, texture: GLenum) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ClientActiveTexture(texture) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ClientActiveTexture(texture) } }); }
    }
    unsafe fn EnableClientState(&mut self, array: GLenum) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.EnableClientState(array) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.EnableClientState(array) } }); }
    }
    unsafe fn DisableClientState(&mut self, array: GLenum) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DisableClientState(array) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DisableClientState(array) } }); }
    }
    unsafe fn GetBooleanv(&mut self, pname: GLenum, params: *mut GLboolean) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetBooleanv(pname, params as *mut GLboolean) } })
    }
    unsafe fn GetFloatv(&mut self, pname: GLenum, params: *mut GLfloat) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetFloatv(pname, params as *mut GLfloat) } })
    }
    unsafe fn GetIntegerv(&mut self, pname: GLenum, params: *mut GLint) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetIntegerv(pname, params as *mut GLint) } })
    }
    unsafe fn GetFixedv(&mut self, pname: GLenum, params: *mut GLfixed) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetFixedv(pname, params as *mut GLfixed) } })
    }
    unsafe fn GetTexEnviv(&mut self, target: GLenum, pname: GLenum, params: *mut GLint) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetTexEnviv(target, pname, params as *mut GLint) } })
    }
    unsafe fn GetTexEnvfv(&mut self, target: GLenum, pname: GLenum, params: *mut GLfloat) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetTexEnvfv(target, pname, params as *mut GLfloat) } })
    }
    unsafe fn GetTexEnvxv(&mut self, target: GLenum, pname: GLenum, params: *mut GLfixed) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetTexEnvxv(target, pname, params as *mut GLfixed) } })
    }
    unsafe fn GetTexParameteriv(&mut self, target: GLenum, pname: GLenum, params: *mut GLint) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetTexParameteriv(target, pname, params as *mut GLint) } })
    }
    unsafe fn GetTexParameterfv(&mut self, target: GLenum, pname: GLenum, params: *mut GLfloat) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetTexParameterfv(target, pname, params as *mut GLfloat) } })
    }
    unsafe fn GetTexParameterxv(&mut self, target: GLenum, pname: GLenum, params: *mut GLfixed) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetTexParameterxv(target, pname, params as *mut GLfixed) } })
    }
    unsafe fn GetClipPlanef(&mut self, plane: GLenum, equation: *mut GLfloat) {
        let equation = equation as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetClipPlanef(plane, equation as *mut GLfloat) } })
    }
    unsafe fn GetClipPlanex(&mut self, plane: GLenum, equation: *mut GLfixed) {
        let equation = equation as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetClipPlanex(plane, equation as *mut GLfixed) } })
    }
    unsafe fn GetLightfv(&mut self, light: GLenum, pname: GLenum, params: *mut GLfloat) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetLightfv(light, pname, params as *mut GLfloat) } })
    }
    unsafe fn GetLightxv(&mut self, light: GLenum, pname: GLenum, params: *mut GLfixed) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetLightxv(light, pname, params as *mut GLfixed) } })
    }
    unsafe fn GetMaterialfv(&mut self, face: GLenum, pname: GLenum, params: *mut GLfloat) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetMaterialfv(face, pname, params as *mut GLfloat) } })
    }
    unsafe fn GetMaterialxv(&mut self, face: GLenum, pname: GLenum, params: *mut GLfixed) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetMaterialxv(face, pname, params as *mut GLfixed) } })
    }
    unsafe fn GetPointerv(&mut self, pname: GLenum, params: *mut *const GLvoid) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetPointerv(pname, params as *mut *const GLvoid) } })
    }
    unsafe fn Hint(&mut self, target: GLenum, mode: GLenum) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Hint(target, mode) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Hint(target, mode) } }); }
    }
    unsafe fn Finish(&mut self) {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Finish() } })
    }
    unsafe fn Flush(&mut self) {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Flush() } })
    }
    unsafe fn GetString(&mut self, name: GLenum) -> *const GLubyte {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); (unsafe { g.GetString(name) }) as usize }) as *const GLubyte
    }
    unsafe fn CurrentPaletteMatrixOES(&mut self, index: GLuint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.CurrentPaletteMatrixOES(index) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.CurrentPaletteMatrixOES(index) } }); }
    }
    unsafe fn LoadPaletteFromModelViewMatrixOES(&mut self) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.LoadPaletteFromModelViewMatrixOES() } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.LoadPaletteFromModelViewMatrixOES() } }); }
    }
    unsafe fn MatrixIndexPointerOES(
        &mut self,
        size: GLint,
        type_: GLenum,
        stride: GLsizei,
        pointer: *const GLvoid,
    ) {
        let pointer = pointer as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.MatrixIndexPointerOES(size, type_, stride, pointer as *const GLvoid) } })
    }
    unsafe fn WeightPointerOES(
        &mut self,
        size: GLint,
        type_: GLenum,
        stride: GLsizei,
        pointer: *const GLvoid,
    ) {
        let pointer = pointer as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.WeightPointerOES(size, type_, stride, pointer as *const GLvoid) } })
    }
    unsafe fn AlphaFunc(&mut self, func: GLenum, ref_: GLclampf) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.AlphaFunc(func, ref_) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.AlphaFunc(func, ref_) } }); }
    }
    unsafe fn AlphaFuncx(&mut self, func: GLenum, ref_: GLclampx) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.AlphaFuncx(func, ref_) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.AlphaFuncx(func, ref_) } }); }
    }
    unsafe fn BlendFunc(&mut self, sfactor: GLenum, dfactor: GLenum) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BlendFunc(sfactor, dfactor) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BlendFunc(sfactor, dfactor) } }); }
    }
    unsafe fn BlendEquationOES(&mut self, mode: GLenum) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BlendEquationOES(mode) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BlendEquationOES(mode) } }); }
    }
    unsafe fn ColorMask(
        &mut self,
        red: GLboolean,
        green: GLboolean,
        blue: GLboolean,
        alpha: GLboolean,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ColorMask(red, green, blue, alpha) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ColorMask(red, green, blue, alpha) } }); }
    }
    unsafe fn ClipPlanef(&mut self, plane: GLenum, equation: *const GLfloat) {
        let equation = equation as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ClipPlanef(plane, equation as *const GLfloat) } })
    }
    unsafe fn ClipPlanex(&mut self, plane: GLenum, equation: *const GLfixed) {
        let equation = equation as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ClipPlanex(plane, equation as *const GLfixed) } })
    }
    unsafe fn CullFace(&mut self, mode: GLenum) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.CullFace(mode) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.CullFace(mode) } }); }
    }
    unsafe fn DepthFunc(&mut self, func: GLenum) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DepthFunc(func) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DepthFunc(func) } }); }
    }
    unsafe fn DepthMask(&mut self, flag: GLboolean) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DepthMask(flag) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DepthMask(flag) } }); }
    }
    unsafe fn FrontFace(&mut self, mode: GLenum) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.FrontFace(mode) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.FrontFace(mode) } }); }
    }
    unsafe fn DepthRangef(&mut self, near: GLclampf, far: GLclampf) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DepthRangef(near, far) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DepthRangef(near, far) } }); }
    }
    unsafe fn DepthRangex(&mut self, near: GLclampx, far: GLclampx) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DepthRangex(near, far) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DepthRangex(near, far) } }); }
    }
    unsafe fn PolygonOffset(&mut self, factor: GLfloat, units: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PolygonOffset(factor, units) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PolygonOffset(factor, units) } }); }
    }
    unsafe fn PolygonOffsetx(&mut self, factor: GLfixed, units: GLfixed) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PolygonOffsetx(factor, units) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PolygonOffsetx(factor, units) } }); }
    }
    unsafe fn SampleCoverage(&mut self, value: GLclampf, invert: GLboolean) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.SampleCoverage(value, invert) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.SampleCoverage(value, invert) } }); }
    }
    unsafe fn SampleCoveragex(&mut self, value: GLclampx, invert: GLboolean) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.SampleCoveragex(value, invert) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.SampleCoveragex(value, invert) } }); }
    }
    unsafe fn ShadeModel(&mut self, mode: GLenum) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ShadeModel(mode) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ShadeModel(mode) } }); }
    }
    unsafe fn Scissor(&mut self, x: GLint, y: GLint, width: GLsizei, height: GLsizei) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Scissor(x, y, width, height) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Scissor(x, y, width, height) } }); }
    }
    unsafe fn Viewport(&mut self, x: GLint, y: GLint, width: GLsizei, height: GLsizei) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Viewport(x, y, width, height) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Viewport(x, y, width, height) } }); }
    }
    unsafe fn LineWidth(&mut self, val: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.LineWidth(val) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.LineWidth(val) } }); }
    }
    unsafe fn LineWidthx(&mut self, val: GLfixed) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.LineWidthx(val) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.LineWidthx(val) } }); }
    }
    unsafe fn StencilFunc(&mut self, func: GLenum, ref_: GLint, mask: GLuint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.StencilFunc(func, ref_, mask) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.StencilFunc(func, ref_, mask) } }); }
    }
    unsafe fn StencilOp(&mut self, sfail: GLenum, dpfail: GLenum, dppass: GLenum) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.StencilOp(sfail, dpfail, dppass) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.StencilOp(sfail, dpfail, dppass) } }); }
    }
    unsafe fn StencilMask(&mut self, mask: GLuint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.StencilMask(mask) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.StencilMask(mask) } }); }
    }
    unsafe fn LogicOp(&mut self, opcode: GLenum) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.LogicOp(opcode) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.LogicOp(opcode) } }); }
    }
    unsafe fn PointSize(&mut self, size: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PointSize(size) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PointSize(size) } }); }
    }
    unsafe fn PointSizex(&mut self, size: GLfixed) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PointSizex(size) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PointSizex(size) } }); }
    }
    unsafe fn PointParameterf(&mut self, pname: GLenum, param: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PointParameterf(pname, param) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PointParameterf(pname, param) } }); }
    }
    unsafe fn PointParameterx(&mut self, pname: GLenum, param: GLfixed) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PointParameterx(pname, param) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PointParameterx(pname, param) } }); }
    }
    unsafe fn PointParameterfv(&mut self, pname: GLenum, params: *const GLfloat) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PointParameterfv(pname, params as *const GLfloat) } })
    }
    unsafe fn PointParameterxv(&mut self, pname: GLenum, params: *const GLfixed) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PointParameterxv(pname, params as *const GLfixed) } })
    }
    unsafe fn Fogf(&mut self, pname: GLenum, param: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Fogf(pname, param) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Fogf(pname, param) } }); }
    }
    unsafe fn Fogx(&mut self, pname: GLenum, param: GLfixed) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Fogx(pname, param) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Fogx(pname, param) } }); }
    }
    unsafe fn Fogfv(&mut self, pname: GLenum, params: *const GLfloat) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Fogfv(pname, params as *const GLfloat) } })
    }
    unsafe fn Fogxv(&mut self, pname: GLenum, params: *const GLfixed) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Fogxv(pname, params as *const GLfixed) } })
    }
    unsafe fn Lightf(&mut self, light: GLenum, pname: GLenum, param: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Lightf(light, pname, param) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Lightf(light, pname, param) } }); }
    }
    unsafe fn Lightx(&mut self, light: GLenum, pname: GLenum, param: GLfixed) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Lightx(light, pname, param) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Lightx(light, pname, param) } }); }
    }
    unsafe fn Lightfv(&mut self, light: GLenum, pname: GLenum, params: *const GLfloat) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Lightfv(light, pname, params as *const GLfloat) } })
    }
    unsafe fn Lightxv(&mut self, light: GLenum, pname: GLenum, params: *const GLfixed) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Lightxv(light, pname, params as *const GLfixed) } })
    }
    unsafe fn LightModelf(&mut self, pname: GLenum, param: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.LightModelf(pname, param) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.LightModelf(pname, param) } }); }
    }
    unsafe fn LightModelx(&mut self, pname: GLenum, param: GLfixed) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.LightModelx(pname, param) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.LightModelx(pname, param) } }); }
    }
    unsafe fn LightModelfv(&mut self, pname: GLenum, params: *const GLfloat) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.LightModelfv(pname, params as *const GLfloat) } })
    }
    unsafe fn LightModelxv(&mut self, pname: GLenum, params: *const GLfixed) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.LightModelxv(pname, params as *const GLfixed) } })
    }
    unsafe fn Materialf(&mut self, face: GLenum, pname: GLenum, param: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Materialf(face, pname, param) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Materialf(face, pname, param) } }); }
    }
    unsafe fn Materialx(&mut self, face: GLenum, pname: GLenum, param: GLfixed) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Materialx(face, pname, param) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Materialx(face, pname, param) } }); }
    }
    unsafe fn Materialfv(&mut self, face: GLenum, pname: GLenum, params: *const GLfloat) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Materialfv(face, pname, params as *const GLfloat) } })
    }
    unsafe fn Materialxv(&mut self, face: GLenum, pname: GLenum, params: *const GLfixed) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Materialxv(face, pname, params as *const GLfixed) } })
    }
    unsafe fn IsBuffer(&mut self, buffer: GLuint) -> GLboolean {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.IsBuffer(buffer) } })
    }
    unsafe fn GenBuffers(&mut self, n: GLsizei, buffers: *mut GLuint) {
        let buffers = buffers as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GenBuffers(n, buffers as *mut GLuint) } })
    }
    unsafe fn DeleteBuffers(&mut self, n: GLsizei, buffers: *const GLuint) {
        let buffers = buffers as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DeleteBuffers(n, buffers as *const GLuint) } })
    }
    unsafe fn BindBuffer(&mut self, target: GLenum, buffer: GLuint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BindBuffer(target, buffer) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BindBuffer(target, buffer) } }); }
    }
    unsafe fn BufferData(
        &mut self,
        target: GLenum,
        size: GLsizeiptr,
        data: *const GLvoid,
        usage: GLenum,
    ) {
        let data = data as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BufferData(target, size, data as *const GLvoid, usage) } })
    }
    unsafe fn BufferSubData(
        &mut self,
        target: GLenum,
        offset: GLintptr,
        size: GLsizeiptr,
        data: *const GLvoid,
    ) {
        let data = data as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BufferSubData(target, offset, size, data as *const GLvoid) } })
    }
    unsafe fn Color4f(&mut self, red: GLfloat, green: GLfloat, blue: GLfloat, alpha: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Color4f(red, green, blue, alpha) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Color4f(red, green, blue, alpha) } }); }
    }
    unsafe fn Color4x(&mut self, red: GLfixed, green: GLfixed, blue: GLfixed, alpha: GLfixed) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Color4x(red, green, blue, alpha) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Color4x(red, green, blue, alpha) } }); }
    }
    unsafe fn Color4ub(&mut self, red: GLubyte, green: GLubyte, blue: GLubyte, alpha: GLubyte) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Color4ub(red, green, blue, alpha) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Color4ub(red, green, blue, alpha) } }); }
    }
    unsafe fn Normal3f(&mut self, nx: GLfloat, ny: GLfloat, nz: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Normal3f(nx, ny, nz) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Normal3f(nx, ny, nz) } }); }
    }
    unsafe fn Normal3x(&mut self, nx: GLfixed, ny: GLfixed, nz: GLfixed) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Normal3x(nx, ny, nz) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Normal3x(nx, ny, nz) } }); }
    }
    unsafe fn ColorPointer(
        &mut self,
        size: GLint,
        type_: GLenum,
        stride: GLsizei,
        pointer: *const GLvoid,
    ) {
        let pointer = pointer as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ColorPointer(size, type_, stride, pointer as *const GLvoid) } })
    }
    unsafe fn NormalPointer(&mut self, type_: GLenum, stride: GLsizei, pointer: *const GLvoid) {
        let pointer = pointer as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.NormalPointer(type_, stride, pointer as *const GLvoid) } })
    }
    unsafe fn TexCoordPointer(
        &mut self,
        size: GLint,
        type_: GLenum,
        stride: GLsizei,
        pointer: *const GLvoid,
    ) {
        let pointer = pointer as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexCoordPointer(size, type_, stride, pointer as *const GLvoid) } })
    }
    unsafe fn VertexPointer(
        &mut self,
        size: GLint,
        type_: GLenum,
        stride: GLsizei,
        pointer: *const GLvoid,
    ) {
        let pointer = pointer as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.VertexPointer(size, type_, stride, pointer as *const GLvoid) } })
    }
    unsafe fn PointSizePointerOES(
        &mut self,
        type_: GLenum,
        stride: GLsizei,
        pointer: *const GLvoid,
    ) {
        let pointer = pointer as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PointSizePointerOES(type_, stride, pointer as *const GLvoid) } })
    }
    unsafe fn DrawArrays(&mut self, mode: GLenum, first: GLint, count: GLsizei) {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DrawArrays(mode, first, count) } })
    }
    unsafe fn DrawElements(
        &mut self,
        mode: GLenum,
        count: GLsizei,
        type_: GLenum,
        indices: *const GLvoid,
    ) {
        let indices = indices as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DrawElements(mode, count, type_, indices as *const GLvoid) } })
    }
    unsafe fn DrawTexsOES(&mut self, x: i16, y: i16, z: i16, width: i16, height: i16) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DrawTexsOES(x, y, z, width, height) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DrawTexsOES(x, y, z, width, height) } }); }
    }
    unsafe fn DrawTexiOES(&mut self, x: GLint, y: GLint, z: GLint, width: GLint, height: GLint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DrawTexiOES(x, y, z, width, height) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DrawTexiOES(x, y, z, width, height) } }); }
    }
    unsafe fn DrawTexxOES(
        &mut self,
        x: GLfixed,
        y: GLfixed,
        z: GLfixed,
        width: GLfixed,
        height: GLfixed,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DrawTexxOES(x, y, z, width, height) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DrawTexxOES(x, y, z, width, height) } }); }
    }
    unsafe fn DrawTexfOES(
        &mut self,
        x: GLfloat,
        y: GLfloat,
        z: GLfloat,
        width: GLfloat,
        height: GLfloat,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DrawTexfOES(x, y, z, width, height) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DrawTexfOES(x, y, z, width, height) } }); }
    }
    unsafe fn DrawTexsvOES(&mut self, coords: *const i16) {
        let coords = coords as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DrawTexsvOES(coords as *const i16) } })
    }
    unsafe fn DrawTexivOES(&mut self, coords: *const GLint) {
        let coords = coords as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DrawTexivOES(coords as *const GLint) } })
    }
    unsafe fn DrawTexxvOES(&mut self, coords: *const GLfixed) {
        let coords = coords as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DrawTexxvOES(coords as *const GLfixed) } })
    }
    unsafe fn DrawTexfvOES(&mut self, coords: *const GLfloat) {
        let coords = coords as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DrawTexfvOES(coords as *const GLfloat) } })
    }
    unsafe fn Clear(&mut self, mask: GLbitfield) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Clear(mask) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Clear(mask) } }); }
    }
    unsafe fn ClearColor(
        &mut self,
        red: GLclampf,
        green: GLclampf,
        blue: GLclampf,
        alpha: GLclampf,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ClearColor(red, green, blue, alpha) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ClearColor(red, green, blue, alpha) } }); }
    }
    unsafe fn ClearColorx(
        &mut self,
        red: GLclampx,
        green: GLclampx,
        blue: GLclampx,
        alpha: GLclampx,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ClearColorx(red, green, blue, alpha) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ClearColorx(red, green, blue, alpha) } }); }
    }
    unsafe fn ClearDepthf(&mut self, depth: GLclampf) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ClearDepthf(depth) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ClearDepthf(depth) } }); }
    }
    unsafe fn ClearDepthx(&mut self, depth: GLclampx) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ClearDepthx(depth) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ClearDepthx(depth) } }); }
    }
    unsafe fn ClearStencil(&mut self, s: GLint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ClearStencil(s) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ClearStencil(s) } }); }
    }
    unsafe fn PixelStorei(&mut self, pname: GLenum, param: GLint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PixelStorei(pname, param) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PixelStorei(pname, param) } }); }
    }
    unsafe fn ReadPixels(
        &mut self,
        x: GLint,
        y: GLint,
        width: GLsizei,
        height: GLsizei,
        format: GLenum,
        type_: GLenum,
        pixels: *mut GLvoid,
    ) {
        let pixels = pixels as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ReadPixels(x, y, width, height, format, type_, pixels as *mut GLvoid) } })
    }
    unsafe fn GenTextures(&mut self, n: GLsizei, textures: *mut GLuint) {
        let textures = textures as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GenTextures(n, textures as *mut GLuint) } })
    }
    unsafe fn DeleteTextures(&mut self, n: GLsizei, textures: *const GLuint) {
        let textures = textures as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DeleteTextures(n, textures as *const GLuint) } })
    }
    unsafe fn ActiveTexture(&mut self, texture: GLenum) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ActiveTexture(texture) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ActiveTexture(texture) } }); }
    }
    unsafe fn IsTexture(&mut self, texture: GLuint) -> GLboolean {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.IsTexture(texture) } })
    }
    unsafe fn BindTexture(&mut self, target: GLenum, texture: GLuint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BindTexture(target, texture) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BindTexture(target, texture) } }); }
    }
    unsafe fn TexParameteri(&mut self, target: GLenum, pname: GLenum, param: GLint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexParameteri(target, pname, param) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexParameteri(target, pname, param) } }); }
    }
    unsafe fn TexParameterf(&mut self, target: GLenum, pname: GLenum, param: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexParameterf(target, pname, param) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexParameterf(target, pname, param) } }); }
    }
    unsafe fn TexParameterx(&mut self, target: GLenum, pname: GLenum, param: GLfixed) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexParameterx(target, pname, param) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexParameterx(target, pname, param) } }); }
    }
    unsafe fn TexParameteriv(&mut self, target: GLenum, pname: GLenum, params: *const GLint) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexParameteriv(target, pname, params as *const GLint) } })
    }
    unsafe fn TexParameterfv(&mut self, target: GLenum, pname: GLenum, params: *const GLfloat) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexParameterfv(target, pname, params as *const GLfloat) } })
    }
    unsafe fn TexParameterxv(&mut self, target: GLenum, pname: GLenum, params: *const GLfixed) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexParameterxv(target, pname, params as *const GLfixed) } })
    }
    unsafe fn TexImage2D(
        &mut self,
        target: GLenum,
        level: GLint,
        mut internalformat: GLint,
        width: GLsizei,
        height: GLsizei,
        border: GLint,
        format: GLenum,
        type_: GLenum,
        pixels: *const GLvoid,
    ) {
        let pixels = pixels as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexImage2D(target, level, internalformat, width, height, border, format, type_, pixels as *const GLvoid) } })
    }
    unsafe fn TexSubImage2D(
        &mut self,
        target: GLenum,
        level: GLint,
        xoffset: GLint,
        yoffset: GLint,
        width: GLsizei,
        height: GLsizei,
        format: GLenum,
        type_: GLenum,
        pixels: *const GLvoid,
    ) {
        let pixels = pixels as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexSubImage2D(target, level, xoffset, yoffset, width, height, format, type_, pixels as *const GLvoid) } })
    }
    unsafe fn CompressedTexImage2D(
        &mut self,
        target: GLenum,
        level: GLint,
        internalformat: GLenum,
        width: GLsizei,
        height: GLsizei,
        border: GLint,
        image_size: GLsizei,
        data: *const GLvoid,
    ) {
        let data = data as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.CompressedTexImage2D(target, level, internalformat, width, height, border, image_size, data as *const GLvoid) } })
    }
    unsafe fn CompressedTexSubImage2D(
        &mut self,
        target: GLenum,
        level: GLint,
        xoffset: GLint,
        yoffset: GLint,
        width: GLsizei,
        height: GLsizei,
        format: GLenum,
        image_size: GLsizei,
        data: *const GLvoid,
    ) {
        let data = data as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.CompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, image_size, data as *const GLvoid) } })
    }
    unsafe fn CopyTexImage2D(
        &mut self,
        target: GLenum,
        level: GLint,
        internalformat: GLenum,
        x: GLint,
        y: GLint,
        width: GLsizei,
        height: GLsizei,
        border: GLint,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.CopyTexImage2D(target, level, internalformat, x, y, width, height, border) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.CopyTexImage2D(target, level, internalformat, x, y, width, height, border) } }); }
    }
    unsafe fn CopyTexSubImage2D(
        &mut self,
        target: GLenum,
        level: GLint,
        xoffset: GLint,
        yoffset: GLint,
        x: GLint,
        y: GLint,
        width: GLsizei,
        height: GLsizei,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.CopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.CopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height) } }); }
    }
    unsafe fn TexEnvf(&mut self, target: GLenum, pname: GLenum, param: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexEnvf(target, pname, param) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexEnvf(target, pname, param) } }); }
    }
    unsafe fn TexEnvx(&mut self, target: GLenum, pname: GLenum, param: GLfixed) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexEnvx(target, pname, param) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexEnvx(target, pname, param) } }); }
    }
    unsafe fn TexEnvi(&mut self, target: GLenum, pname: GLenum, param: GLint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexEnvi(target, pname, param) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexEnvi(target, pname, param) } }); }
    }
    unsafe fn TexEnvfv(&mut self, target: GLenum, pname: GLenum, params: *const GLfloat) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexEnvfv(target, pname, params as *const GLfloat) } })
    }
    unsafe fn TexEnvxv(&mut self, target: GLenum, pname: GLenum, params: *const GLfixed) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexEnvxv(target, pname, params as *const GLfixed) } })
    }
    unsafe fn TexEnviv(&mut self, target: GLenum, pname: GLenum, params: *const GLint) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.TexEnviv(target, pname, params as *const GLint) } })
    }
    unsafe fn MultiTexCoord4f(
        &mut self,
        target: GLenum,
        s: GLfloat,
        t: GLfloat,
        r: GLfloat,
        q: GLfloat,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.MultiTexCoord4f(target, s, t, r, q) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.MultiTexCoord4f(target, s, t, r, q) } }); }
    }
    unsafe fn MultiTexCoord4x(
        &mut self,
        target: GLenum,
        s: GLfixed,
        t: GLfixed,
        r: GLfixed,
        q: GLfixed,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.MultiTexCoord4x(target, s, t, r, q) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.MultiTexCoord4x(target, s, t, r, q) } }); }
    }
    unsafe fn MatrixMode(&mut self, mode: GLenum) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.MatrixMode(mode) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.MatrixMode(mode) } }); }
    }
    unsafe fn LoadIdentity(&mut self) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.LoadIdentity() } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.LoadIdentity() } }); }
    }
    unsafe fn LoadMatrixf(&mut self, m: *const GLfloat) {
        let m = m as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.LoadMatrixf(m as *const GLfloat) } })
    }
    unsafe fn LoadMatrixx(&mut self, m: *const GLfixed) {
        let m = m as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.LoadMatrixx(m as *const GLfixed) } })
    }
    unsafe fn MultMatrixf(&mut self, m: *const GLfloat) {
        let m = m as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.MultMatrixf(m as *const GLfloat) } })
    }
    unsafe fn MultMatrixx(&mut self, m: *const GLfixed) {
        let m = m as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.MultMatrixx(m as *const GLfixed) } })
    }
    unsafe fn PushMatrix(&mut self) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PushMatrix() } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PushMatrix() } }); }
    }
    unsafe fn PopMatrix(&mut self) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PopMatrix() } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.PopMatrix() } }); }
    }
    unsafe fn Orthof(
        &mut self,
        left: GLfloat,
        right: GLfloat,
        bottom: GLfloat,
        top: GLfloat,
        near: GLfloat,
        far: GLfloat,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Orthof(left, right, bottom, top, near, far) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Orthof(left, right, bottom, top, near, far) } }); }
    }
    unsafe fn Orthox(
        &mut self,
        left: GLfixed,
        right: GLfixed,
        bottom: GLfixed,
        top: GLfixed,
        near: GLfixed,
        far: GLfixed,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Orthox(left, right, bottom, top, near, far) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Orthox(left, right, bottom, top, near, far) } }); }
    }
    unsafe fn Frustumf(
        &mut self,
        left: GLfloat,
        right: GLfloat,
        bottom: GLfloat,
        top: GLfloat,
        near: GLfloat,
        far: GLfloat,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Frustumf(left, right, bottom, top, near, far) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Frustumf(left, right, bottom, top, near, far) } }); }
    }
    unsafe fn Frustumx(
        &mut self,
        left: GLfixed,
        right: GLfixed,
        bottom: GLfixed,
        top: GLfixed,
        near: GLfixed,
        far: GLfixed,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Frustumx(left, right, bottom, top, near, far) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Frustumx(left, right, bottom, top, near, far) } }); }
    }
    unsafe fn Rotatef(&mut self, angle: GLfloat, x: GLfloat, y: GLfloat, z: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Rotatef(angle, x, y, z) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Rotatef(angle, x, y, z) } }); }
    }
    unsafe fn Rotatex(&mut self, angle: GLfixed, x: GLfixed, y: GLfixed, z: GLfixed) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Rotatex(angle, x, y, z) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Rotatex(angle, x, y, z) } }); }
    }
    unsafe fn Scalef(&mut self, x: GLfloat, y: GLfloat, z: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Scalef(x, y, z) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Scalef(x, y, z) } }); }
    }
    unsafe fn Scalex(&mut self, x: GLfixed, y: GLfixed, z: GLfixed) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Scalex(x, y, z) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Scalex(x, y, z) } }); }
    }
    unsafe fn Translatef(&mut self, x: GLfloat, y: GLfloat, z: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Translatef(x, y, z) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Translatef(x, y, z) } }); }
    }
    unsafe fn Translatex(&mut self, x: GLfixed, y: GLfixed, z: GLfixed) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Translatex(x, y, z) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Translatex(x, y, z) } }); }
    }
    unsafe fn GenFramebuffersOES(&mut self, n: GLsizei, framebuffers: *mut GLuint) {
        let framebuffers = framebuffers as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GenFramebuffersOES(n, framebuffers as *mut GLuint) } })
    }
    unsafe fn GenRenderbuffersOES(&mut self, n: GLsizei, renderbuffers: *mut GLuint) {
        let renderbuffers = renderbuffers as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GenRenderbuffersOES(n, renderbuffers as *mut GLuint) } })
    }
    unsafe fn IsFramebufferOES(&mut self, renderbuffer: GLuint) -> GLboolean {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.IsFramebufferOES(renderbuffer) } })
    }
    unsafe fn IsRenderbufferOES(&mut self, renderbuffer: GLuint) -> GLboolean {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.IsRenderbufferOES(renderbuffer) } })
    }
    unsafe fn BindFramebufferOES(&mut self, target: GLenum, framebuffer: GLuint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BindFramebufferOES(target, framebuffer) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BindFramebufferOES(target, framebuffer) } }); }
    }
    unsafe fn BindRenderbufferOES(&mut self, target: GLenum, renderbuffer: GLuint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BindRenderbufferOES(target, renderbuffer) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BindRenderbufferOES(target, renderbuffer) } }); }
    }
    unsafe fn RenderbufferStorageOES(
        &mut self,
        target: GLenum,
        internalformat: GLenum,
        width: GLsizei,
        height: GLsizei,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.RenderbufferStorageOES(target, internalformat, width, height) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.RenderbufferStorageOES(target, internalformat, width, height) } }); }
    }
    unsafe fn FramebufferRenderbufferOES(
        &mut self,
        target: GLenum,
        attachment: GLenum,
        renderbuffertarget: GLenum,
        renderbuffer: GLuint,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.FramebufferRenderbufferOES(target, attachment, renderbuffertarget, renderbuffer) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.FramebufferRenderbufferOES(target, attachment, renderbuffertarget, renderbuffer) } }); }
    }
    unsafe fn FramebufferTexture2DOES(
        &mut self,
        target: GLenum,
        attachment: GLenum,
        textarget: GLenum,
        texture: GLuint,
        level: i32,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.FramebufferTexture2DOES(target, attachment, textarget, texture, level) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.FramebufferTexture2DOES(target, attachment, textarget, texture, level) } }); }
    }
    unsafe fn GetFramebufferAttachmentParameterivOES(
        &mut self,
        target: GLenum,
        attachment: GLenum,
        pname: GLenum,
        params: *mut GLint,
    ) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetFramebufferAttachmentParameterivOES(target, attachment, pname, params as *mut GLint) } })
    }
    unsafe fn GetRenderbufferParameterivOES(
        &mut self,
        target: GLenum,
        pname: GLenum,
        params: *mut GLint,
    ) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetRenderbufferParameterivOES(target, pname, params as *mut GLint) } })
    }
    unsafe fn CheckFramebufferStatusOES(&mut self, target: GLenum) -> GLenum {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.CheckFramebufferStatusOES(target) } })
    }
    unsafe fn DeleteFramebuffersOES(&mut self, n: GLsizei, framebuffers: *const GLuint) {
        let framebuffers = framebuffers as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DeleteFramebuffersOES(n, framebuffers as *const GLuint) } })
    }
    unsafe fn DeleteRenderbuffersOES(&mut self, n: GLsizei, renderbuffers: *const GLuint) {
        let renderbuffers = renderbuffers as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DeleteRenderbuffersOES(n, renderbuffers as *const GLuint) } })
    }
    unsafe fn GenerateMipmapOES(&mut self, target: GLenum) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GenerateMipmapOES(target) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GenerateMipmapOES(target) } }); }
    }
    unsafe fn GenFramebuffers(&mut self, n: GLsizei, framebuffers: *mut GLuint) {
        let framebuffers = framebuffers as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GenFramebuffers(n, framebuffers as *mut GLuint) } })
    }
    unsafe fn GenRenderbuffers(&mut self, n: GLsizei, renderbuffers: *mut GLuint) {
        let renderbuffers = renderbuffers as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GenRenderbuffers(n, renderbuffers as *mut GLuint) } })
    }
    unsafe fn IsFramebuffer(&mut self, framebuffer: GLuint) -> GLboolean {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.IsFramebuffer(framebuffer) } })
    }
    unsafe fn IsRenderbuffer(&mut self, renderbuffer: GLuint) -> GLboolean {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.IsRenderbuffer(renderbuffer) } })
    }
    unsafe fn BindFramebuffer(&mut self, target: GLenum, framebuffer: GLuint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BindFramebuffer(target, framebuffer) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BindFramebuffer(target, framebuffer) } }); }
    }
    unsafe fn BindRenderbuffer(&mut self, target: GLenum, renderbuffer: GLuint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BindRenderbuffer(target, renderbuffer) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BindRenderbuffer(target, renderbuffer) } }); }
    }
    unsafe fn RenderbufferStorage(
        &mut self,
        target: GLenum,
        internalformat: GLenum,
        width: GLsizei,
        height: GLsizei,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.RenderbufferStorage(target, internalformat, width, height) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.RenderbufferStorage(target, internalformat, width, height) } }); }
    }
    unsafe fn FramebufferRenderbuffer(
        &mut self,
        target: GLenum,
        attachment: GLenum,
        renderbuffertarget: GLenum,
        renderbuffer: GLuint,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.FramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.FramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer) } }); }
    }
    unsafe fn FramebufferTexture2D(
        &mut self,
        target: GLenum,
        attachment: GLenum,
        textarget: GLenum,
        texture: GLuint,
        level: i32,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.FramebufferTexture2D(target, attachment, textarget, texture, level) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.FramebufferTexture2D(target, attachment, textarget, texture, level) } }); }
    }
    unsafe fn CheckFramebufferStatus(&mut self, target: GLenum) -> GLenum {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.CheckFramebufferStatus(target) } })
    }
    unsafe fn DeleteFramebuffers(&mut self, n: GLsizei, framebuffers: *const GLuint) {
        let framebuffers = framebuffers as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DeleteFramebuffers(n, framebuffers as *const GLuint) } })
    }
    unsafe fn DeleteRenderbuffers(&mut self, n: GLsizei, renderbuffers: *const GLuint) {
        let renderbuffers = renderbuffers as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DeleteRenderbuffers(n, renderbuffers as *const GLuint) } })
    }
    unsafe fn GenerateMipmap(&mut self, target: GLenum) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GenerateMipmap(target) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GenerateMipmap(target) } }); }
    }
    unsafe fn GetFramebufferAttachmentParameteriv(
        &mut self,
        target: GLenum,
        attachment: GLenum,
        pname: GLenum,
        params: *mut GLint,
    ) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetFramebufferAttachmentParameteriv(target, attachment, pname, params as *mut GLint) } })
    }
    unsafe fn GetRenderbufferParameteriv(
        &mut self,
        target: GLenum,
        pname: GLenum,
        params: *mut GLint,
    ) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetRenderbufferParameteriv(target, pname, params as *mut GLint) } })
    }
    unsafe fn GetBufferParameteriv(&mut self, target: GLenum, pname: GLenum, params: *mut GLint) {
        let params = params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetBufferParameteriv(target, pname, params as *mut GLint) } })
    }
    unsafe fn MapBufferOES(&mut self, target: GLenum, access: GLenum) -> *mut GLvoid {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); (unsafe { g.MapBufferOES(target, access) }) as usize }) as *mut GLvoid
    }
    unsafe fn UnmapBufferOES(&mut self, target: GLenum) -> GLboolean {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.UnmapBufferOES(target) } })
    }
    unsafe fn CreateShader(&mut self, _type_: GLenum) -> GLuint {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.CreateShader(_type_) } })
    }
    unsafe fn DeleteShader(&mut self, _shader: GLuint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DeleteShader(_shader) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DeleteShader(_shader) } }); }
    }
    unsafe fn ShaderSource(
        &mut self,
        _shader: GLuint,
        _count: GLsizei,
        _string: *const *const GLchar,
        _length: *const GLint,
    ) {
        let _string = _string as usize;
        let _length = _length as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ShaderSource(_shader, _count, _string as *const *const GLchar, _length as *const GLint) } })
    }
    unsafe fn CompileShader(&mut self, _shader: GLuint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.CompileShader(_shader) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.CompileShader(_shader) } }); }
    }
    unsafe fn GetShaderiv(&mut self, _shader: GLuint, _pname: GLenum, _params: *mut GLint) {
        let _params = _params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetShaderiv(_shader, _pname, _params as *mut GLint) } })
    }
    unsafe fn GetShaderInfoLog(
        &mut self,
        _shader: GLuint,
        _maxLength: GLsizei,
        _length: *mut GLsizei,
        _infoLog: *mut GLchar,
    ) {
        let _length = _length as usize;
        let _infoLog = _infoLog as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetShaderInfoLog(_shader, _maxLength, _length as *mut GLsizei, _infoLog as *mut GLchar) } })
    }
    unsafe fn IsShader(&mut self, _shader: GLuint) -> GLboolean {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.IsShader(_shader) } })
    }
    unsafe fn CreateProgram(&mut self) -> GLuint {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.CreateProgram() } })
    }
    unsafe fn DeleteProgram(&mut self, _program: GLuint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DeleteProgram(_program) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DeleteProgram(_program) } }); }
    }
    unsafe fn AttachShader(&mut self, _program: GLuint, _shader: GLuint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.AttachShader(_program, _shader) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.AttachShader(_program, _shader) } }); }
    }
    unsafe fn DetachShader(&mut self, _program: GLuint, _shader: GLuint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DetachShader(_program, _shader) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DetachShader(_program, _shader) } }); }
    }
    unsafe fn LinkProgram(&mut self, _program: GLuint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.LinkProgram(_program) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.LinkProgram(_program) } }); }
    }
    unsafe fn UseProgram(&mut self, _program: GLuint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.UseProgram(_program) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.UseProgram(_program) } }); }
    }
    unsafe fn GetProgramiv(&mut self, _program: GLuint, _pname: GLenum, _params: *mut GLint) {
        let _params = _params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetProgramiv(_program, _pname, _params as *mut GLint) } })
    }
    unsafe fn GetProgramInfoLog(
        &mut self,
        _program: GLuint,
        _maxLength: GLsizei,
        _length: *mut GLsizei,
        _infoLog: *mut GLchar,
    ) {
        let _length = _length as usize;
        let _infoLog = _infoLog as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetProgramInfoLog(_program, _maxLength, _length as *mut GLsizei, _infoLog as *mut GLchar) } })
    }
    unsafe fn IsProgram(&mut self, _program: GLuint) -> GLboolean {
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.IsProgram(_program) } })
    }
    unsafe fn ValidateProgram(&mut self, _program: GLuint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ValidateProgram(_program) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ValidateProgram(_program) } }); }
    }
    unsafe fn BindAttribLocation(
        &mut self,
        _program: GLuint,
        _index: GLuint,
        _name: *const GLchar,
    ) {
        let _name = _name as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BindAttribLocation(_program, _index, _name as *const GLchar) } })
    }
    unsafe fn GetAttribLocation(&mut self, _program: GLuint, _name: *const GLchar) -> GLint {
        let _name = _name as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetAttribLocation(_program, _name as *const GLchar) } })
    }
    unsafe fn GetUniformLocation(&mut self, _program: GLuint, _name: *const GLchar) -> GLint {
        let _name = _name as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetUniformLocation(_program, _name as *const GLchar) } })
    }
    unsafe fn GetActiveAttrib(
        &mut self,
        _program: GLuint,
        _index: GLuint,
        _bufSize: GLsizei,
        _length: *mut GLsizei,
        _size: *mut GLint,
        _type_: *mut GLenum,
        _name: *mut GLchar,
    ) {
        let _length = _length as usize;
        let _size = _size as usize;
        let _type_ = _type_ as usize;
        let _name = _name as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetActiveAttrib(_program, _index, _bufSize, _length as *mut GLsizei, _size as *mut GLint, _type_ as *mut GLenum, _name as *mut GLchar) } })
    }
    unsafe fn GetActiveUniform(
        &mut self,
        _program: GLuint,
        _index: GLuint,
        _bufSize: GLsizei,
        _length: *mut GLsizei,
        _size: *mut GLint,
        _type_: *mut GLenum,
        _name: *mut GLchar,
    ) {
        let _length = _length as usize;
        let _size = _size as usize;
        let _type_ = _type_ as usize;
        let _name = _name as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetActiveUniform(_program, _index, _bufSize, _length as *mut GLsizei, _size as *mut GLint, _type_ as *mut GLenum, _name as *mut GLchar) } })
    }
    unsafe fn EnableVertexAttribArray(&mut self, _index: GLuint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.EnableVertexAttribArray(_index) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.EnableVertexAttribArray(_index) } }); }
    }
    unsafe fn DisableVertexAttribArray(&mut self, _index: GLuint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DisableVertexAttribArray(_index) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.DisableVertexAttribArray(_index) } }); }
    }
    unsafe fn VertexAttribPointer(
        &mut self,
        _index: GLuint,
        _size: GLint,
        _type_: GLenum,
        _normalized: GLboolean,
        _stride: GLsizei,
        _pointer: *const GLvoid,
    ) {
        let _pointer = _pointer as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.VertexAttribPointer(_index, _size, _type_, _normalized, _stride, _pointer as *const GLvoid) } })
    }
    unsafe fn VertexAttrib1f(&mut self, _index: GLuint, _x: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.VertexAttrib1f(_index, _x) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.VertexAttrib1f(_index, _x) } }); }
    }
    unsafe fn VertexAttrib2f(&mut self, _index: GLuint, _x: GLfloat, _y: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.VertexAttrib2f(_index, _x, _y) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.VertexAttrib2f(_index, _x, _y) } }); }
    }
    unsafe fn VertexAttrib3f(&mut self, _index: GLuint, _x: GLfloat, _y: GLfloat, _z: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.VertexAttrib3f(_index, _x, _y, _z) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.VertexAttrib3f(_index, _x, _y, _z) } }); }
    }
    unsafe fn VertexAttrib4f(
        &mut self,
        _index: GLuint,
        _x: GLfloat,
        _y: GLfloat,
        _z: GLfloat,
        _w: GLfloat,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.VertexAttrib4f(_index, _x, _y, _z, _w) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.VertexAttrib4f(_index, _x, _y, _z, _w) } }); }
    }
    unsafe fn VertexAttrib1fv(&mut self, _index: GLuint, _v: *const GLfloat) {
        let _v = _v as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.VertexAttrib1fv(_index, _v as *const GLfloat) } })
    }
    unsafe fn VertexAttrib2fv(&mut self, _index: GLuint, _v: *const GLfloat) {
        let _v = _v as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.VertexAttrib2fv(_index, _v as *const GLfloat) } })
    }
    unsafe fn VertexAttrib3fv(&mut self, _index: GLuint, _v: *const GLfloat) {
        let _v = _v as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.VertexAttrib3fv(_index, _v as *const GLfloat) } })
    }
    unsafe fn VertexAttrib4fv(&mut self, _index: GLuint, _v: *const GLfloat) {
        let _v = _v as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.VertexAttrib4fv(_index, _v as *const GLfloat) } })
    }
    unsafe fn Uniform1f(&mut self, _location: GLint, _v0: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform1f(_location, _v0) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform1f(_location, _v0) } }); }
    }
    unsafe fn Uniform2f(&mut self, _location: GLint, _v0: GLfloat, _v1: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform2f(_location, _v0, _v1) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform2f(_location, _v0, _v1) } }); }
    }
    unsafe fn Uniform3f(&mut self, _location: GLint, _v0: GLfloat, _v1: GLfloat, _v2: GLfloat) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform3f(_location, _v0, _v1, _v2) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform3f(_location, _v0, _v1, _v2) } }); }
    }
    unsafe fn Uniform4f(
        &mut self,
        _location: GLint,
        _v0: GLfloat,
        _v1: GLfloat,
        _v2: GLfloat,
        _v3: GLfloat,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform4f(_location, _v0, _v1, _v2, _v3) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform4f(_location, _v0, _v1, _v2, _v3) } }); }
    }
    unsafe fn Uniform1i(&mut self, _location: GLint, _v0: GLint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform1i(_location, _v0) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform1i(_location, _v0) } }); }
    }
    unsafe fn Uniform2i(&mut self, _location: GLint, _v0: GLint, _v1: GLint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform2i(_location, _v0, _v1) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform2i(_location, _v0, _v1) } }); }
    }
    unsafe fn Uniform3i(&mut self, _location: GLint, _v0: GLint, _v1: GLint, _v2: GLint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform3i(_location, _v0, _v1, _v2) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform3i(_location, _v0, _v1, _v2) } }); }
    }
    unsafe fn Uniform4i(
        &mut self,
        _location: GLint,
        _v0: GLint,
        _v1: GLint,
        _v2: GLint,
        _v3: GLint,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform4i(_location, _v0, _v1, _v2, _v3) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform4i(_location, _v0, _v1, _v2, _v3) } }); }
    }
    unsafe fn Uniform1fv(&mut self, _location: GLint, _count: GLsizei, _value: *const GLfloat) {
        let _value = _value as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform1fv(_location, _count, _value as *const GLfloat) } })
    }
    unsafe fn Uniform2fv(&mut self, _location: GLint, _count: GLsizei, _value: *const GLfloat) {
        let _value = _value as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform2fv(_location, _count, _value as *const GLfloat) } })
    }
    unsafe fn Uniform3fv(&mut self, _location: GLint, _count: GLsizei, _value: *const GLfloat) {
        let _value = _value as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform3fv(_location, _count, _value as *const GLfloat) } })
    }
    unsafe fn Uniform4fv(&mut self, _location: GLint, _count: GLsizei, _value: *const GLfloat) {
        let _value = _value as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform4fv(_location, _count, _value as *const GLfloat) } })
    }
    unsafe fn Uniform1iv(&mut self, _location: GLint, _count: GLsizei, _value: *const GLint) {
        let _value = _value as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform1iv(_location, _count, _value as *const GLint) } })
    }
    unsafe fn Uniform2iv(&mut self, _location: GLint, _count: GLsizei, _value: *const GLint) {
        let _value = _value as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform2iv(_location, _count, _value as *const GLint) } })
    }
    unsafe fn Uniform3iv(&mut self, _location: GLint, _count: GLsizei, _value: *const GLint) {
        let _value = _value as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform3iv(_location, _count, _value as *const GLint) } })
    }
    unsafe fn Uniform4iv(&mut self, _location: GLint, _count: GLsizei, _value: *const GLint) {
        let _value = _value as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.Uniform4iv(_location, _count, _value as *const GLint) } })
    }
    unsafe fn UniformMatrix2fv(
        &mut self,
        _location: GLint,
        _count: GLsizei,
        _transpose: GLboolean,
        _value: *const GLfloat,
    ) {
        let _value = _value as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.UniformMatrix2fv(_location, _count, _transpose, _value as *const GLfloat) } })
    }
    unsafe fn UniformMatrix3fv(
        &mut self,
        _location: GLint,
        _count: GLsizei,
        _transpose: GLboolean,
        _value: *const GLfloat,
    ) {
        let _value = _value as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.UniformMatrix3fv(_location, _count, _transpose, _value as *const GLfloat) } })
    }
    unsafe fn UniformMatrix4fv(
        &mut self,
        _location: GLint,
        _count: GLsizei,
        _transpose: GLboolean,
        _value: *const GLfloat,
    ) {
        let _value = _value as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.UniformMatrix4fv(_location, _count, _transpose, _value as *const GLfloat) } })
    }
    unsafe fn BlendColor(&mut self, _r: GLclampf, _g: GLclampf, _b: GLclampf, _a: GLclampf) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BlendColor(_r, _g, _b, _a) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BlendColor(_r, _g, _b, _a) } }); }
    }
    unsafe fn BlendEquation(&mut self, _mode: GLenum) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BlendEquation(_mode) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BlendEquation(_mode) } }); }
    }
    unsafe fn BlendEquationSeparate(&mut self, _modeRGB: GLenum, _modeAlpha: GLenum) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BlendEquationSeparate(_modeRGB, _modeAlpha) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BlendEquationSeparate(_modeRGB, _modeAlpha) } }); }
    }
    unsafe fn BlendFuncSeparate(
        &mut self,
        _srcRGB: GLenum,
        _dstRGB: GLenum,
        _srcAlpha: GLenum,
        _dstAlpha: GLenum,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BlendFuncSeparate(_srcRGB, _dstRGB, _srcAlpha, _dstAlpha) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.BlendFuncSeparate(_srcRGB, _dstRGB, _srcAlpha, _dstAlpha) } }); }
    }
    unsafe fn StencilFuncSeparate(
        &mut self,
        _face: GLenum,
        _func: GLenum,
        _ref_: GLint,
        _mask: GLuint,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.StencilFuncSeparate(_face, _func, _ref_, _mask) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.StencilFuncSeparate(_face, _func, _ref_, _mask) } }); }
    }
    unsafe fn StencilOpSeparate(
        &mut self,
        _face: GLenum,
        _sfail: GLenum,
        _dpfail: GLenum,
        _dppass: GLenum,
    ) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.StencilOpSeparate(_face, _sfail, _dpfail, _dppass) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.StencilOpSeparate(_face, _sfail, _dpfail, _dppass) } }); }
    }
    unsafe fn StencilMaskSeparate(&mut self, _face: GLenum, _mask: GLuint) {
        if self.actor.queued { self.actor.submit(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.StencilMaskSeparate(_face, _mask) } }); }
        else { self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.StencilMaskSeparate(_face, _mask) } }); }
    }
    unsafe fn GetVertexAttribiv(&mut self, _index: GLuint, _pname: GLenum, _params: *mut GLint) {
        let _params = _params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetVertexAttribiv(_index, _pname, _params as *mut GLint) } })
    }
    unsafe fn GetVertexAttribfv(&mut self, _index: GLuint, _pname: GLenum, _params: *mut GLfloat) {
        let _params = _params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetVertexAttribfv(_index, _pname, _params as *mut GLfloat) } })
    }
    unsafe fn GetVertexAttribPointerv(
        &mut self,
        _index: GLuint,
        _pname: GLenum,
        _pointer: *mut *mut GLvoid,
    ) {
        let _pointer = _pointer as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetVertexAttribPointerv(_index, _pname, _pointer as *mut *mut GLvoid) } })
    }
    unsafe fn GetUniformiv(&mut self, _program: GLuint, _location: GLint, _params: *mut GLint) {
        let _params = _params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetUniformiv(_program, _location, _params as *mut GLint) } })
    }
    unsafe fn GetUniformfv(&mut self, _program: GLuint, _location: GLint, _params: *mut GLfloat) {
        let _params = _params as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetUniformfv(_program, _location, _params as *mut GLfloat) } })
    }
    unsafe fn GetAttachedShaders(
        &mut self,
        _program: GLuint,
        _maxCount: GLsizei,
        _count: *mut GLsizei,
        _shaders: *mut GLuint,
    ) {
        let _count = _count as usize;
        let _shaders = _shaders as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetAttachedShaders(_program, _maxCount, _count as *mut GLsizei, _shaders as *mut GLuint) } })
    }
    unsafe fn GetShaderSource(
        &mut self,
        _shader: GLuint,
        _bufSize: GLsizei,
        _length: *mut GLsizei,
        _source: *mut GLchar,
    ) {
        let _length = _length as usize;
        let _source = _source as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetShaderSource(_shader, _bufSize, _length as *mut GLsizei, _source as *mut GLchar) } })
    }
    unsafe fn GetShaderPrecisionFormat(
        &mut self,
        _shadertype: GLenum,
        _precisiontype: GLenum,
        _range: *mut GLint,
        _precision: *mut GLint,
    ) {
        let _range = _range as usize;
        let _precision = _precision as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.GetShaderPrecisionFormat(_shadertype, _precisiontype, _range as *mut GLint, _precision as *mut GLint) } })
    }
    unsafe fn ShaderBinary(
        &mut self,
        _count: GLsizei,
        _shaders: *const GLuint,
        _binaryformat: GLenum,
        _binary: *const GLvoid,
        _length: GLsizei,
    ) {
        let _shaders = _shaders as usize;
        let _binary = _binary as usize;
        self.actor.call(self.context, move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); unsafe { g.ShaderBinary(_count, _shaders as *const GLuint, _binaryformat, _binary as *const GLvoid, _length) } })
    }
}
