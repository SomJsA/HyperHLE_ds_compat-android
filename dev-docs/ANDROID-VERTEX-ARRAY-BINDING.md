# Native ES1 vertex array buffer query

The fixed-function ES1 vertex array buffer binding is
`GL_VERTEX_ARRAY_BUFFER_BINDING` (0x8896), queried by `glGetIntegerv`.
The ES2 generic attribute binding pname0x889F belongs to
`glGetVertexAttribiv`; it is not an ES1 `glGetIntegerv` pname. Use the generated
`gles11::VERTEX_ARRAY_BUFFER_BINDING` constant in native bounds and scaled-blood
queries. `GL_ARRAY_BUFFER_BINDING` is also not a substitute: the currently bound
buffer may differ from the VBO captured by the previous `glVertexPointer`.

The previous native local constant was incorrectly0x889F. The screen-span
query in `native_texnodepth_begin` runs during ordinary indexed draws, so this
introduced a driver error even when the indexed draw itself was valid. A failed
query also left the VBO test at zero, defeating the distinction between a host
client pointer and a VBO offset. Correcting the query preserves that distinction;
no errors are suppressed, no rendering default or material correction changes.

Fresh current-root d0f game tracing reproduced18150 wrapper0x500 reports from
the DrawElements closure. This trace used the actual private runtime root and
confirmed that its options file loaded. Earlier d0f technical runs used the
perf-lab root while writing trace options under font-context-private, so their
absence of wrapper trace errors is not a trace-enabled zero-error certification.
Their recorded resource uploads and package/data restore evidence remain scoped.

Real Adreno610/OpenGL ES-CM1.1 component evidence is in
`report/android-native-array-binding-20261105/RESULT.json`:32 client/VBO cases,
old query alwaysINVALID_ENUM, correct captured-array binding and unrelated
current-buffer distinction, valid draws and exact red pixels. No game/package
data was changed by the offscreen fixture. Game regression and lights/white-wall
acceptance are separate; do not infer a full visual fix from a query component.

Official definitions:

- https://raw.githubusercontent.com/KhronosGroup/OpenGL-Registry/main/api/GLES/gl.h
- https://raw.githubusercontent.com/KhronosGroup/OpenGL-Registry/main/api/GLES2/gl2.h
