# R15 performance work — active

User authorization 2026-11-02: sustained work without a budget ceiling; reverse engineering permitted. Android target is3x native rendering, with texture packs/upscaling permitted. PC target is3x native rendering plus4x atlas pack,2x/4x MSAA and modest2x/4x filtering, targeting stable60 real frames. Compare60/70/uncapped actual game progression; do not change the game clock or report synthetic frames as performance.

This commit removes two avoidable presenter costs while keeping fallback controls:

- CPU compatibility transfer retains its RGBA scratch allocation and texture storage. Same-size frames use TexSubImage2D; first-use/resize still defines storage. Pixel format/order, readback, filtering and explicit CPU-path Finish are unchanged.
- GPU framebuffer copy and later sampling remain in the same GL command stream, so they no longer force CPU completion before every copy. `TOUCHHLE_DS_PRESENT_COPY_FINISH=1` restores the prior wait for matched driver testing. Cross-context texture upload and Android GLES/Vulkan/AHB synchronization remain unchanged.
- Android adopts the PC absolute-present-deadline contract: composition/readback/swap time is deducted before sleeping. The old Android path computed a relative delay before presentation and slept that obsolete full duration afterwards. No clock multiplier is introduced.
- PC launcher/control inbox/runner and Android launcher expose70 FPS alongside30/60/uncapped. Existing saved user choices remain respected.

Primary synchronization references: [OpenGL2 specification](https://registry.khronos.org/OpenGL/specs/gl/glspec20.pdf), [Khronos synchronization](https://wikis.khronos.org/opengl/Synchronization). This same-context argument does not license removing cross-context synchronization.

Six pure deadline/storage/wait-policy tests pass. Full PC DS regression suite173passed/1optional ignored at the first build checkpoint. Further native build/device/image/performance checks are **pending**; do not treat this commit or a successful build as final acceptance. GPU-copy and GPU-text production defaults are not promoted by this commit.

Current machine evidence: `E:/CodexProjects/DeadSpaceLegacy/report/performance-push-20261102`. The initial GPU-copy ABBA observation was ~14.0ms CPU-copy versus8.44ms GPU-copy at4x/MSAA2 in the same Store room. Captures were coherent, but a dependency build overlapped; classify as exploratory and repeat without build contention at the requested3x/4x-atlas target. Native timer/run-loop disassembly is recorded there; no guest timing patch applied.

OriginalIPA, accepted releases, UI/text/input/save contracts remain protected. Further work includes controlled70/uncapped game-time validation, target3x+4x-atlas/memory/performance, Android3x/device validation and optional textures, heavy/cross-level correctness, shader/copy fallback and remaining original acceptance gates.

## Opt-in original game-time observer

`TOUCHHLE_DS_GAME_CLOCK_TRACE=1` verifies the original ARM instructions at
M3GApplication::OnUpdate (0x530a84) and Timer::Tick before arming a process-local
breakpoint observer. The original STR r0,[sp] is replayed with the same value;
registers/flags and all game-clock values remain unchanged. Every two seconds
it reports native accumulated milliseconds versus wall time, update count,
min/max delta and invalid negative deltas. First/owner-change deltas are
excluded because they predate the observation window. Unknown binary words
disable the observer. No original IPA is changed. This diagnostic incurs
breakpoint overhead: use it for game-speed comparisons, and disable it for
final throughput samples. Two pure metrics tests cover half-speed detection,
negative values and independent windows.

The first Android build caught a PC-only telemetry call accidentally included
in the present-local port; it was removed. That failed build is not a release
candidate. Full Android rebuild and native observer validation remain required.

## Actual survival baseline and current stress target

The normal Android release from implementation6dc55da2 built successfully;
APK6c9b75bc592b14dc5cf717a022b42813b366603fd8e1b9429441d20b468cf59a.
The private SaveLab wrapper carries byte-identical native libraries; its main
libtouchHLE hash is713f86c41ea81213587ecf4c1cc9af48ee1d4c8b9207ce32bc4da9cb3cc3a9be.
This is separate from installed public mainv6; the performance APK has not
been installed as the main application.

The user clarified actual play as native **5 Minutes to Kill / 夺命5分钟**,
random running until death. The mining arena was selected through its real
menu. Native3x960x1440,MSAA0,queued GPU copy,fast present,uncapped,clock observer
off: actual04:57 arena,04:45 grapple and red native death/results images were
reviewed. Continuous UID-targeted touch plans reached native state26. They
changed neither health nor game time and used no attacks/healing. The alive
window retained450 actual present intervals:35.807FPS,mean27.927ms,p95 33.081ms,
p99 43.961ms,max83.987ms. This short baseline includes probe/capture overhead;
it does **not** meet the3x60FPS target or certify all scenes. No crash observed.

All320 original files across14 scopes, original Lab APK/preferences and both
IPAs were restored/verified byte-exact; main user progress was never seeded.
Device is asleep. Evidence: `report/performance-push-20261102/android-timed-combat-v2`.
The first attempt failed in the diagnostic input-plan wrapper (missing Python
quote helper), restored correctly and is not counted as gameplay. Earlier four
clock samples on a loading tip remain rejected: an objective event/player
pointer can precede actual scene entry. Readiness needs actual world/timer and
native state progression together.

Local PC baseline used3x/4x textures andMSAA4/AF4, with native random running
through enemy grapple to death. The user now requests local **4K output, ultra
native and8x**: next PC arm requests3840x2160 output,8x internal rendering and8x
textures,MSAA4/AF4,without FSR/frame generation. Effective settings still need
runtime verification; these are pending stress targets. Android remains3x.

## Resolution diagnosis and opt-in execution attribution

New private native timed-mode running samples at1x/3x measured31.67/31.99FPS
in the first finite running block. Both actual04:57 world/player images were
reviewed. The9x pixel-count difference did not materially change throughput.
The emulator SDLThread used about96% of one core over each complete
entry-to-observed-death window; both big-core frequency readbacks were2.4GHz.
These short dynamic samples support investigating CPU/call costs; they do not
prove a particular function is slow or justify lowering the3x target. Raw
frames/resources and exact windows are in
`report/performance-push-20261102/android-resolution-diagnostic`.
OriginalAPK,320files/14scopes and bothIPAs were restored byte-exact, screenAsleep.

`TOUCHHLE_DS_EXECUTION_PROFILE=1` adds a bounded diagnostic on the emulator
thread. It times individual JIT runs and completed named host SVC calls,
including a separate texture-upload glFinish row. At actual guest presents,
at most12 host rows are logged per two-second interval; at most513 symbol
buckets are retained. It is off by default and reads no clocks when off.
Host measurements are **inclusive completed-call wall time**, may contain
nested callbacks/waits, and must not be summed or described as CPU time.
Long calls can span reporting windows. JIT rows are disjoint run intervals;
neither row is GPU execution time. Profiling itself has overhead, so final
throughput samples must disable it. Each report includes the actual native
frame counter so loading, active play and death/results can be separated
against the frame CSV. Resource queries must not leave the player standing
between entry and the finite running plan; collect them outside the measured
block. No game clock, state, quality, affinity
or power setting is changed. Two accumulator tests cover independent totals,
reset, maximum duration and bounded unexpected symbol growth.

The full-frequency diagnostic itself was intrusive: frame-tagged actual3x
first running blocks were23.20FPS with it versus33.39FPS with it disabled.
These short dynamic samples are not a controlled percentage-overhead result,
but clearly require lower observation cost before diagnosing real throughput.
It now selects general JIT/SVC calls randomly at1/64 by default, scales totals
by the inverse probability, and explicitly logs estimates, sample counts and
sample-only maxima. `TOUCHHLE_DS_EXECUTION_PROFILE_SAMPLE_SHIFT=0` restores
full-frequency timing; allowed shift0..10, default6. Rare texture-upload
synchronisation and the low-frequency whole EAGL present are timed exactly.
Four pure tests cover weighted totals, independent host/guest/reset semantics,
bounded buckets, observed maxima and random-selection density/exact mode.
Final throughput still disables profiling entirely.

Saved per-thread stat readbacks put the busy SDLThread on cores4..6, nice-4,
with roughly96% of one core over entry-to-death and big-policy2.4GHz readbacks.
This is not evidence of continuous core residency or exact GPU time, but it
does rule out treating low total multi-core utilisation as proof of idle CPU.

## First multicore resource stage

The user explicitly asks for multicore optimisation. Pure alpha-aware RGBA
mip reduction now splits independent output rows across scoped worker threads
for output levels >=1024x1024. Default up to8 workers onPC,4 onAndroid, bounded
by available CPUs; TOUCHHLE_DS_TEXTURE_WORKERS=1 retains serial execution.
Small levels remain serial. Workers borrow immutable pixels and disjoint
output slices; they perform no GL calls, guest access, game clock/state or
input mutation. All workers join before upload. Failed thread creation falls
back to recomputing the level serially after successful workers have joined.
Integer reduction/transparent RGB/alpha order remains bit-exact. Five pure
tests cover original golden chains, malformed input, thin axes, odd edges
and serial-versus0/2/4/8-worker byte parity, including a2053x1027 texture.

On thisPC, an8192x8192 full mip-chain benchmark(warm median3 samples, warmup
separate) measured243.116ms with1 worker,76.060ms with4,46.062ms with8.
Windows process CPU/wall ratios were0.97/3.39/5.77 active cores, respectively.
The final pixel matched; full-byte parity is independently tested. This proves
parallel resource computation, **not** parallel ARM gameplay or a combat FPS
increase. Native upload/actual texture visuals andAndroid runtime validation
remain required before promoting the next candidate. Profile/build evidence
is in report/performance-push-20261102/parallel-mip-*.json.

Next multicore work: PNG/PVRTC predecode/preload with bounded memory and
owned results; assess rendering preparation/submission separation without
sharing mutable Environment or violating ordered GL/readback semantics.
Actual native game execution still runs on the emulator owner thread.
