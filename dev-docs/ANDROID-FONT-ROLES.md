# Android requested font roles

The exact user MStiffHei title face (SHA256 c14c692b526830a48bfa7d946afb3beb350f078abad3cd67d245877fed52b751)
and SweiShearSansCJKsc-Regular story face (df140c929d10307ca270de1161134d20bf1cc3b12aaee7b6df50ebfb4b8ceb2d)
are added as TitleChinese.ttf and StoryChinese.ttf in native res/overlay and
Java assets/overlay. Existing fonts remain for historical tools and fallback.
No IPA is rebuilt.

The Android scene observer is environment-owned and independent of PC input
or Java overlay readiness. Fingerprinted LayerMainMenu state/draw entries select
Frontend; LayerGameWorld::OnDraw selects Story when its embedded HUD references
a verified native player. Temporary absence keeps the last role, including pause.
The three scene entry PUSH instructions replay their original stack/register
effects without guest calls. All35 code words, including complete GetHud offset
evidence, match the original IPA before hooks can arm. Unknown code retains
original font behavior.

Both CGFont(+8) and UIFont(+16) GetID implementations use the verified three
GlyphBuffer consumers and native ID counter. Environment-owned role keys are
bounded512 and reserve IDs through that original allocator. Original font IDs,
cmap, Latin provider, input and saves remain intact. UIKit CJK measurement,
wrapping and drawing share the chosen face. Diagnostic logging is opt-in/bounded.

Java chapter Chinese/English/numbers and objective labels use MStiffHei;
captions and objective bodies use Swei Regular. RoleFonts normalizes logical
heights using each actual Paint ascent/descent. The obsolete global Noto
1000/1448 factor does not apply to the new faces. Chapter fit, label and body
metrics remain separate; cache keys include face identity. Existing style
defaults and explicit fontSizeSp preferences are preserved. RoleFonts APIs are
available at minSdk21; actual Skia metrics/layout still require device acceptance.

Five host tests pass: native cache allocator/domain/capacity,8 scene PUSH replay
cases and24 getter return cases against production JIT, all GPRs/CPSR/FPSCR and
stack, plus scene persistence. The Android-only benchmark call is cfg-gated for
host builds; existing FPSCR accessors are exposed under cfg(test), without an
Android production change.18 guard tests and Java compilation pass. Host tests
are not Android device acceptance.

Artifact guard pins native-source and Java-asset bytes, then checks each exact
font in libtouchHLE.so and APK assets. Negative tests reject stale assets/native
fonts or changed source. Full release, API21 lint, installation, all-chapter
typography, shutdown stress, performance and full R15 acceptance remain separate.
Evidence: project report android-font-roles-20261104/STATUS.json.
