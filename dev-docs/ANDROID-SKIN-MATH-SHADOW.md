# Android native skin math shadow

This phase is diagnostic only. `TOUCHHLE_DS_SKIN_MATH=shadow` opts Android arm64 into full-fingerprinted hooks of the original `OptimizedSkinningEngine::ProcessSkin` and `ProcessSkinNormal`. Other architectures and ordinary launches remain off. Unknown original bytes never get patched. The original IPA is unchanged; only the known in-process entries are hooked.

Every call retains the original ARM result. A checked, bounded snapshot gathers vec3 float positions, four signed-byte bone indices, four float weights and already-transposed matrix data. Unsupported layouts, null/wrapped/oversized ranges, output aliases and nonfinite relevant data skip comparison and run the original. Empty or unsupported calls are not reinterpreted as successful acceleration. GL and guest memory stay on the owner thread; the pure kernel receives only owned host vectors.

The original first PUSH of r4-r7/LR is replayed with a host-return trampoline, then the original body runs at entry+4 through the existing no-extra-stack guest-call bridge. No entry unpatch/recompile loop is used. Stack restoration is asserted. Original elapsed time includes this hook/bridge and scheduling; it is not exclusive CPU time. The shadow comparison adds copying and math overhead, so these frames are diagnostic, not a release FPS result.

The host kernel uses nonfused AArch64 NEON multiply/add, the original four-weight early-stop bit test, and the same position/normal translation rule. It saves/restores host FPCR/FPSR, uses standard NEON rounding/FZ/DN, and returns sticky exception flags. Both output bytes and guest FPSCR exception bits0x9F are compared. No computed result is written to guest memory. Logs are bounded first-call examples and periodic cumulative counts/timings. A future acceleration phase must require actual game-input comparisons and preserve original fallback; component speed is not a game-frame gain.

## Repeatable original-code component

`python tools/tests/AndroidSkinMathImage.py --ipa <accepted-original-base.ipa> --output <new-skin-image.bin>` validates immutable base SHA and all three original function fingerprints, then extracts only memory-test TEXT/DATA segments. It does not make an IPA. Do not commit this copyrighted game-code fixture or substitute it into release assets.

Compile `tools/tests/AndroidSkinMathBench.cpp` together with the actual `src/cpu/dynarmic_wrapper/skin_math.cpp`, NDK API21 arm64, `-O3 -DNDEBUG -std=c++20 -ffp-contract=off -fno-fast-math`, includes for the wrapper and locked Dynarmic `out/include`, and the same `libdynarmic.a`, `libfmt.a`, `libmcl.a`, static libc++, `-ldl -llog`. Pass the extracted image file. Use a fresh owned device temp directory, byte canary/hash verification and finally sleep.

The component covers240 ordinary 1/2/3/4-weight cases and128 signed-zero/subnormal/extreme/Inf/NaN cases for both original ARM routines, comparing all output bytes and FP exception flags. It also verifies the entry-PUSH replay, then reports ABBAAB component timings. The first three-weight prototype failed and was corrected to the actual four-weight layout; that failure is preserved in session evidence. Two pure checked-span/alias tests run independently with `rustc --edition 2021 --test src/ds_skin_math_bounds.rs`.

Phase1 synthetic component measured roughly16.7x for1,048,576 vertices per round; actual renderer invocation count, supported real layouts and per-frame share remain to be established by the normal APK. Keep all3x/real60, texture/clock/streaming, native UI/input/text, lights/boxes, save/audio/weapons/hardware and final human acceptance gates open.


The optional calibrated host phase is documented separately in `ANDROID-SKIN-MATH-HOST.md`; ordinary launches still remain off. Real phase2 shadow recorded14,526 position calls/7,125,944 vertices with zero byte/FP mismatches; no actual normal calls were observed. Those diagnostic timings are not a release frame-rate gain.
