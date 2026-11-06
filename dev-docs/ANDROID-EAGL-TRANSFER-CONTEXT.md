# Android EAGL transfer cache ownership

The legacy presenter kept six GL caches in host-thread storage: ES2 program,
objects, source-renderbuffer attachment and texture dimensions; ES1 transfer
texture/storage and source FBO. Multiple backing contexts may run on the same
host thread. Cached dimensions do not prove texture storage exists in the
current context. A sharegroup also does not make private attachment and storage
history interchangeable.

The same texture-cache pattern produced GL_INVALID_VALUE0x501 in an actual
two-independent-context PC test. Android retained that pattern after its earlier
PresentState fix. This port changes Android's own presenter in place; it does
not copy PC input, GPU-card, cursor or synchronization implementations.

Each EAGLContextHostObject now holds an Rc<RefCell<TransferState>> for its six
private caches. Transfer and postprocess state use the same effective backing
owner as get_thread_context, including its existing worker/main fallback.
Reusable storage is retained within its owner. The CPU-only RGBA scratch buffer
remains thread-local. No guest/GL/Environment state crosses a new worker.

EAGL deallocation first makes its owner current and releases its private transfer
and postprocess resources, then restores the existing thread/main binding when
another owner remains. No GL calls occur from Rust Drop. Android's render-owner
proxy, pause/surface behavior, FSR/FG policy, font roles, sync defaults and
application IDs are unchanged by this port.

EXT FBO sharing and core/ARB FBO sharing have different rules; do not describe
all framebuffer objects as universally unshareable. This implementation generates
independent helper objects and tests actual attachments rather than assuming
numeric names overlap between contexts. Background component tests use a
test-build-only hidden desktop SDL window and actual production helper code.
They establish host component behavior, not Adreno/Mali or Android lifecycle
acceptance. The structural guard rejects known GL caches in thread_local macros
while allowing CPU scratch; it is not a full ownership proof.

Required evidence remains separate: actual unshared red/green texture copy and
reuse; shared helper/guest attachment isolation; ES2 program/object validity and
owner-current release; fallback resolution; normal complete main/isolated APKs
with exact fonts/original IPA; then current-device backup/install/readback and
pause/resume/surface-recreation validation. Device access is pending. Preserve
the previous font APKs and installed-release anchors. No exact10h rotation,
performance, light/box, full-story or final R15 acceptance follows from this fix.

Current results are recorded at the project root in
report/android-eagl-transfer-context-20261105/STATUS.json.
