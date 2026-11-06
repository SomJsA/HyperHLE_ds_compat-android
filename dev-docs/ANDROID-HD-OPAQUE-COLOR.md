# Android opaque HD color diagnostic

The actual167e6x scene has substantially darker, chromatic posterized shadows
than4x. The same normal binary, resources and render3x with onlyHD_RGBA8=1
restores those shadows. Both traced scenes upload11textures withGL0 and report
no wrapperGLerror. FullRGBA8 has a much larger actual process/graphics snapshot;
it is not promoted to default. These short scenes do not certify longplay.

Native Adreno fixtures cover POT/6144NPOT and complete driver mip generation.
The existing truncated4444 conversion mapsRGB7/11/15 to0/0/0; roundedRGBA5551
samples8/8/16 while retainingRGBA alpha semantics. UnlikeRGB565,RGBA5551 keeps
the RGBA base format, including texture-environment REPLACE alpha behavior.

TOUCHHLE_DS_HD_OPAQUE_5551=1 is an explicit Android-only diagnostic for the
existing >4096 sixteen-bit path. All input alpha bytes must be255; otherwise
the originalRGBA4444 path remains. HD_RGBA8=1 takes precedence. Smaller textures,
desktop, all default format choices, replacement PNG/IPA bytes, ambient policy,
alpha-aware mip selection and owning-context/thread rules remain unchanged.
Only pure pixel conversion uses the new module; GL stays on the owner thread.

The source tests verify rejection of nonopaque/malformed inputs, canonical
packed RGBA layout and bounded error for every channel byte. The30-case actual
driver fixture is component evidence. A normally built APK, actual same-binary
format-on/off scenes, memory and alpha/streaming/longplay checks are still needed
before promoting this candidate or claiming the6x defect is fixed.
