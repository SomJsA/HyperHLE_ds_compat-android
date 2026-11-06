//! Replacement textures must survive a later guest mip-filter request.

pub(crate) fn enabled_for(android: bool, complete_override: Option<&str>, legacy: bool) -> bool {
    if android {
        // Explicit diagnostic rollback reproduces the old incomplete-chain path.
        complete_override != Some("0")
    } else {
        // This Android correction must not promote desktop rendering defaults.
        legacy
    }
}

pub(crate) fn enabled() -> bool {
    let complete_override = std::env::var("TOUCHHLE_DS_HD_COMPLETE_MIPS").ok();
    enabled_for(
        cfg!(target_os = "android"),
        complete_override.as_deref(),
        std::env::var_os("TOUCHHLE_DS_TEX_MIP").is_some(),
    )
}

#[cfg(test)]
mod tests {
    use super::enabled_for;

    #[test]
    fn android_default_preserves_completeness_without_a_launcher_flag() {
        assert!(enabled_for(true, None, false));
        assert!(enabled_for(true, Some("1"), false));
        assert!(enabled_for(true, Some("unknown"), false));
    }

    #[test]
    fn explicit_android_rollback_survives_an_inherited_legacy_flag() {
        assert!(!enabled_for(true, Some("0"), false));
        assert!(!enabled_for(true, Some("0"), true));
    }

    #[test]
    fn desktop_defaults_and_legacy_opt_in_remain_independent() {
        for value in [None, Some("0"), Some("1")] {
            assert!(!enabled_for(false, value, false));
            assert!(enabled_for(false, value, true));
        }
    }
}
