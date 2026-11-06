# Native palette experiment

`TOUCHHLE_DS_NATIVE_MATRIX_PALETTE=1` opts a native GLES1 context into the
driver's `GL_OES_matrix_palette`. Default is off. Require the exact driver
extension token, all four loaded entry points, and positive real palette/vertex
limits. Unsupported drivers keep the previous route. No fake limits or context
version change. The four previously inherited no-op methods forward only when
this gate succeeds. First guest use of each method is logged separately from
capability detection; enabling a route is not evidence that the game uses it.

Host present temporarily disables the palette and its weight/index client
arrays, then restores their enable bits. It does not mutate palette matrices,
weight/index pointers or buffer bindings. GL/guest memory remain on their owner
thread. Ambient/light fixes are unchanged.

Important existing boundary: guest `glGetString` supplies a fixed iOS extension
list, independent of native backend `GetString`. That list already advertises
palette support. The old backend-only extension filter therefore cannot by
itself establish that DeadSpace selected CPU skinning. This experiment does not
change the fixed guest list. Actual guest calls, coherent animation/UI, and
native gameplay timings are required before drawing a performance conclusion.

The 2026-11-02 Adreno610 raw-driver offscreen probe found 20 palette matrices,
4 vertex units, all four entry points, and zero differences over 422 nonblack
reference pixels in a tiny weighted-triangle comparison. This establishes driver
capability only; it does not establish wrapper/game use or speed.

Gate/token negative tests can run without GL/ADB:
`rustc --edition 2021 --test src/gles/ds_native_palette.rs -o <temporary-test.exe>`.
Build and isolate a normal Android release before real-device experiments.
Do not enable this flag in distributed defaults without actual scene evidence.
