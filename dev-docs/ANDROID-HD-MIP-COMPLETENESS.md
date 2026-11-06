# Android HD replacement mip completeness

After replacing level0 with an HD PNG, the runtime ignores the guest's old
PVRTC mip levels. Forcing LINEAR at upload time is insufficient: the guest can
later request mip filtering. On the real Adreno610 ES1 driver, that incomplete
texture samples pure white without a GL error. The original-texture room was
intact while the same10a/HD4x scene lost most walls/floor to pure white.

The actual-device fixture in `report/android-hd-white-sampling-20261105` covers
RGBA8/RGBA4444 and64/4096 textures. All four missing-chain/mip-filter cases
sampled255/255/255/255 with GLerror0; completing the chain restored the expected
17/85/153/255. A same-binary game diagnostic with only explicit HD mip generation
and a bounded color observer restored the room. The user confirmed that displayed
4x scene looked normal; the internal rendering scale remained the recommended3x.
This is scoped image evidence, not full story/lighting/performance acceptance.

Android HD uploads now create complete replacement chains by default. Existing
alpha-aware CPU mip generation for selected RGBA8 atlases remains intact;
packed large RGBA4444 textures use the existing driver generation path. Original
textures, PNG/IPA bytes, formats and platform rendering/thread defaults are not
rewritten. All GL work stays on the current owning context.

`TOUCHHLE_DS_HD_COMPLETE_MIPS=0` is an explicit diagnostic rollback to the old
incomplete-chain behavior. Desktop retains the existing `TOUCHHLE_DS_TEX_MIP`
opt-in; the new Android override does not promote desktop defaults. Normal
Android callers need no launcher/diagnostic flag to obtain complete HD textures.

Mip storage adds roughly one third to base-level texture storage. That estimate
is not a measured process/GPU peak. Normal main/chaptertest builds, actual
2x/4x/6x upload/visual/memory checks and context/streaming/long-play regression
remain required. Do not count pixel restoration or uploadGL0 as FPS or full
R15 acceptance. Preserve the earlier pure-white failure and its zero-error logs.
