# Android native test mailbox

Opt in with `TOUCHHLE_DS_CONTROL_INBOX=<private application file>`. Without it,
normal input is unchanged and no mailbox filesystem polling occurs. The parser
is byte-identical to PC `ds_control_inbox.rs`: bounded one-line requests,
monotonic sequence numbers, stale startup request ignored, 200ms read interval.
Five shared protocol tests cover malformed batches, replay and numeric bounds.

Android currently accepts these commands, prefixed by a fresh sequence number:

- `tap/down/move/up x y` and `swipe x y x2 y2`: existing native script touch
  queue, landscape iPhone points480x320, diagnostic finger only.
- `probe`: existing native gauge reader, **no frame capture and no empty-clip
  mutation**, including when legacy `TOUCHHLE_DS_EMPTY_CLIP` is set.
- `checkpoint`: log native frame cursor, not a saved-game checkpoint.
- `release/cancel`: lift the diagnostic finger at its last point.
- `mark`: explicit existing screenshot diagnostic; keep outside FPS windows.

Other PC diagnostic commands are explicitly rejected on this Android backend;
this is not a claim of desktop keyboard/controller feature parity. Commands
stay on the owning native thread and do not synthesize Android or host input.
The old manual N key keeps its original gauge-plus-frame-capture behaviour.

The controller must verify the owned package's foreground state, wait for the
ready log and each sequence acknowledgement, publish requests atomically, and
gate gameplay on native state plus reviewed actual entry/death evidence.
Do not send native input behind unknown system dialogs. Record and clean up
only a positively identified warning belonging to the isolated test package,
outside measurement windows. First verify a real menu click before batching.

Use a private seeded copy and fresh backup/isolation journal, restore original
data/APK with byte verification, release the diagnostic finger and sleep the
device in finally. Resource/memory estimates and loading-page FPS are never
substitutes for actual applied uploads and native gameplay samples.
