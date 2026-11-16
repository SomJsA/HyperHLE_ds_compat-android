# Android R2: opening captions, rotation and launcher language

User feedback on 2026-11-11 is the authority for these three cue offsets:
`Oper_02` and `Miner_02` +1500ms, `Miner_01` -2500ms. `Anchor_02` and every
other ID retain their original timing. The original opening track contains
fourteen entries, including silent entries. Preserve those entries when
building the absolute guest-clock timeline. The early cue must be selected
from the upcoming native segment, rather than waiting for that segment's event.
Never use Android wall time to advance captions during a native pause.

With the replacement layer ready, both checked native RenderSubtitle branches
remain suppressed across silent gaps and segment transitions. Disabling the
layer restores the exact original instructions. Unsupported binaries retain
their original path. The original subtitle resource has 193 unique nonempty
cue IDs; all are present in the replacement map. The selected IPA stays intact.

`LandscapeRotation` interprets sensor angles relative to the device's natural
orientation, separately from Surface rotation. Only the two landscape sectors
(±25°) are eligible. For a naturally portrait device, sensor270° is primary
landscape and sensor90° is reverse. Natural-landscape devices use 0°/180°.
Require 700ms stability, then show a button; only a click changes orientation.
Cancel pending observations on pause, unknown readings and portrait/diagonal
readings. Do not change native tilt-pulse, shake or input semantics. See the
[Android orientation API](https://developer.android.com/reference/android/view/OrientationEventListener#onOrientationChanged(int)).

Launcher language is app-specific: first run selects Chinese for a Chinese
system locale and English otherwise; an explicit upper-right choice persists.
It is independent of storage location and portable game configuration. Existing
unknown config keys were already ignored by the importer; the new exclusion
and removal of `ui_language` make that separation explicit. Test two consecutive
cold launches, rather than relying only on the currently displayed text.
Keep the Chinese and English Easter eggs as separate complete pairs.

The corresponding PC installer and launcher share the explicit language choice
in LocalAppData/DeadSpaceLegacy/ui-language.txt. Keep the existing test override
TOUCHHLE_UI_LANGUAGE. The original game language and saves are independent.

The user-authorized Vill_37 correction is present in both native SBIN resources
and display text. Native text fingerprints are now explicit per platform;
the PC input tutorial is intentionally different from Android. The shared
text guard permits only that declared tutorial difference and rejects shared
text drift or incomplete overrides. Do not refresh unrelated ambient or
submodule baselines to hide their existing failures.

Evidence: E:/CodexProjects/DeadSpaceLegacy/report/android-locale-caption-20261111.
The isolated caption test's native library SHA matches both final delivery
libraries. Actual opening screenshots show only the replacement dialogue;
Miner_02 appeared 1.492s after its native event and Miner_01 appeared 2.488s
before its native event. Anchor_02 display and native event were simultaneous.
Oper_02 was tested across pause/resume, retaining the guest-clock adjustment.

Engineering checks do not close hand-held rotation acceptance, subjective audio
alignment, the older chapter-list/editor investigations, or full R15 playtesting.
