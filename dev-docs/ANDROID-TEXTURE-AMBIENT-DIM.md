# Android HD texture ambient exposure

The accepted Android ambient-dim repair originally applies while decoding the
opaque RGB PVRTC environment atlases listed in `src/gles/util.rs`. HD PNG
replacements take the `glCompressedTexImage2D` override path and therefore
bypassed the repair. This caused a texture-tier-dependent visual difference:
the original PVRTC atlas was dimmed, while its 2x/4x/6x replacement was uploaded
with the PNG's unmodified exposure.

`ds_ambient_dim_scale()` remains the single material-key whitelist. PNG
replacements are eligible only when:

- their stable source texture content key is on that whitelist;
- every RGBA texel is opaque (`alpha == 255`);
- `TOUCHHLE_DS_PVRTC_AMBIENT_DIM` is not `0`.

The conversion uses the same integer rounding as the PVRTC path (`0.50` for the
silver-white primary atlas and `0.84` for the other accepted environment
atlases). UI, text, transparent atlases, unknown content keys and applications
outside Dead Space are not modified. `TOUCHHLE_DS_PVRTC_AMBIENT_DIM=0` restores
the unmodified PNG pixels for comparison and rollback.

This is a texture-load operation only. It does not add per-frame work, change
presentation gamma, or replace same-scene visual acceptance. A 4x/6x device
capture must still prove the affected scene, and a clean A/B with the rollback
switch must be retained.
