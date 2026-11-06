# Android color diagnostics handoff — 2026-10-26

This branch is an isolated diagnostic follow-up to the accepted depth24 repair.
It does not contain a new visual-correction claim and must not replace the
delivered APK.

## Accepted baseline

- Fix commit: `515bf53d` (`fix(android): retain 24-bit scene depth for station geometry`)
- Delivered APK:
  `DeadSpace-Android-arm64-20261026-Depth24-Fix.apk`
- APK SHA-256:
  `5177aa3afa8a2a5ede9ee9bdf65e259ec7d97f7512b96f05c33c90a9668c36e5`
- Delivery directory:
  `E:/BaiduSyncdisk/DeadSpace/Android-Depth24-Fix-20261026/`
- The six files listed by `SHA256SUMS.txt` were rechecked and matched.
  No active Baidu upload sidecar remained after the check.

## Diagnostic scope

- Branch: `codex/android-color-diagnostics-20261026`
- Worktree:
  `E:/CodexProjects/DeadSpaceLegacy/work/HyperHLE_ds_compat-android-color-20261026`
- Target: determine whether pale/white surfaces originate in guest material
  state, Android color attachment precision, texture environment state, or the
  final presentation path.
- Probe package suffix: `.colordiag`, separate from the delivered application.
- The probe logs requested and actual renderbuffer formats, channel bits,
  framebuffer status, draw state, texture environment state, and opt-in paired
  lossless PPM captures.
- The probe is opt-in through `TOUCHHLE_DS_COLOR_PROBE=1`; it applies no gamma,
  exposure, or other visual correction.

## Build and test state

- The diagnostic APK built successfully.
- Diagnostic APK SHA-256:
  `ad9d1651dbb84a8d049afbb7f30e2840fe8ed21f76974abe099f1930120ce02d`
- The target scene is the user's completed save in the five-minute
  challenge / zero-gravity area, where white walls are the concern.
- The tablet was disconnected when this handoff was consolidated. No conclusion
  about the white-wall cause has been established, and no diagnostic recording
  has yet been validated from that scene.
- The official native Android comparison available locally covers only the
  opening. Do not claim an exact native zero-gravity comparison without a new
  matching recording.

## Recovery and safety notes

- Keep the PC worktree and its unrelated dirty files untouched.
- The isolated Android worktree contains build-materialized assets and vendored
  dependency line-ending changes. Neither belongs in the diagnostic source
  commit.
- The original delivered application sandbox was backed up before the separate
  diagnostic package was installed.
- Resume only after reconnecting the tablet over ADB. Install and run the
  `.colordiag` package separately; do not overwrite or relabel the accepted APK.
