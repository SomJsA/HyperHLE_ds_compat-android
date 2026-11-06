# Android HD rounded premultiplied RGBA diagnostic

The clean043fa6x same-binary off/on pair showed partial improvement: four
opaque textures usedRGBA5551 while seven nonopaque textures retainedRGBA4444.
Each arm had11successful uploads,no wrapperGLerrors,6moves and12views. Large
wall/floor shadows recovered,but residual colorstepping remained versusRGBA8.
The candidate remains opt-in;partial image improvement is not full acceptance.

The legacy4444 conversion drops the low nibble,then the driver expands0..15
to0..255. This stretches contrast: low intensities tend darker,high intensities
brighter. Nearest representable RGB has at most8levels error rather than15;
over source0..63,the tested mean dark bias drops from-6 to about-0.16levels.

The first de66RGB-only prototype kept legacy alpha truncation. An additional
test exposed84premultiplied-storage violations over32896valid grayscale pixels:
[15,15,15,15] packed to0x1110,soRGB exceededalpha. The original seven tests missed
that cross-channel property. That candidate is rejected even when a GL/upload
test or a scoped image looks fine;its normal builds/trial remain negative evidence.

TOUCHHLE_DS_HD_ROUND_4444=1 now rounds **all RGBA channels together** in the
existing Android >4096sixteen-bit path. A common monotonic quantizer preserves
RGB<=alpha for premultiplied PNG input. Source[15,15,15,15] becomes0x1111.
Each decoded channel differs from its source by at most8levels. Alpha precision
is still four bits;its quantization changes to nearest along with RGB.

All PNG/IPA bytes,the image premultiplication algorithm,texture dimensions,
formats,mip rules,context ownership and defaults remain. HD_RGBA8 takes
precedence. HD_OPAQUE_5551 independently selects5551 for fully opaque inputs;
this flag rounds remaining4444inputs. GL stays on its owning thread.

Eight pure resource tests now include the cross-channel invariant and zeroalpha
RGB rule. Actual GPU sampling and premultiplied ONE/ONE_MINUS_SRC_ALPHA blending,
normal APKs and same-binary6x image/memory regressions are required before
promotion. Four-bit precision is not RGBA8equivalence,performance improvement,
streaming/longplay or fullR15acceptance. Both experimental format flags stayOFF.
