# Android JIT flat-memory experiment

`TOUCHHLE_DS_CPU_FASTMEM=1` opts into Dynarmic fastmem on Android. The default remains the original page table. This changes address translation only; no unsafe optimization flags, guest timings, instruction results, exclusive accesses or GL ownership change. PC implementation is untouched.

The existing 4 GiB guest allocation is retained. The null segment must be nonempty and aligned to the host page size. Its host pages become `PROT_NONE`, so null reads and writes fault into Dynarmic's existing recompilation/page-table fallback and touchHLE's compatibility callbacks. Null writes must not contaminate subsequent zero reads. Unknown/incompatible page layout or failed protection leaves the original page table active. In particular a 4 KiB guest null segment on a 16 KiB host must fall back. The destructor destroys the JIT before restoring page protection. Never enable this merely by setting a pointer over accessible null memory.

Log `[DSjit-fastmem]` records requested/effective policy, host page size and guard size. A requested flag does not establish activation. This is an opt-in experiment until the normal release and real native gameplay are checked; a shell executable is not an Android app/ART integration test.

## Repeatable component check

Build `tools/tests/AndroidFastmemLoop.S` for ARMv7 as an object, extract its `.text` bytes with NDK `llvm-objcopy`, then compile `tools/tests/AndroidFastmemBench.cpp` with NDK API21 arm64, `-O3 -DNDEBUG -std=c++20`, the current wrapper directory and the normal locked Dynarmic build's `out/include`. Link the same `libdynarmic.a`, `libfmt.a`, `libmcl.a`, static libc++, `-ldl -llog`. The wrapper must be heap allocated: it contains an 8 MiB page table and does not fit the shell main-thread stack.

Pass the extracted loop file as the only executable argument. Six ABBAAB rounds compare every integer register, CPSR and all 1024 output words after 204,800,000 memory operations per round. Each round also checks zero null reads, absorbed null writes, and readable/restored null memory after the real wrapper destructor. Mock memory callbacks mirror the null contract; native application validation must still exercise the actual Rust shim.

2026-11-03 tablet component evidence is under `R15-ANDROID-OPT-20261103-A/fastmem-wrapper-device-02` in the session workspace: page-table median730.023 ms, fastmem601.289 ms, 17.63% elapsed reduction in this memory-loop component. This is neither a frame-rate result nor a full-game correctness certificate. The earlier standalone Dynarmic result agrees. The first wrapper harness failed by putting the page table on its stack; the corrected heap harness passed without changing the implementation.

Real-game frame CSVs retain all intervals, the complete death-observation tail and low-frequency probe overhead. Dynamic combat runs differ in enemy behavior and are not paired static performance proof. Keep MSAA/AF fallbacks and actual texture-upload GL errors separate from requested settings.
