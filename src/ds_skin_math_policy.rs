//! Pure opt-in policy. Unsupported data and mismatches retain original execution.
#[derive(Copy, Clone, Debug, Default, PartialEq)]
pub(crate) enum Mode {
    #[default]
    Off,
    Shadow,
    Host,
}
impl Mode {
    pub(crate) fn parse(value: Option<&str>) -> Self {
        match value {
            Some("shadow") => Self::Shadow,
            Some("host") => Self::Host,
            _ => Self::Off,
        }
    }
}
#[derive(Copy, Clone, Debug, PartialEq)]
pub(crate) enum Action {
    Native,
    Compare,
    Host,
}
pub(crate) fn choose(
    mode: Mode,
    disabled: bool,
    matched: u64,
    calls: u64,
    eligible: bool,
    force_every: u64,
) -> Action {
    if mode == Mode::Off
        || !eligible
        || (mode == Mode::Host && disabled)
        || (force_every != 0 && calls % force_every == 0)
    {
        return Action::Native;
    }
    if mode == Mode::Shadow || matched < 64 || calls % 256 == 0 {
        Action::Compare
    } else {
        Action::Host
    }
}
#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn no_host_output_before_actual_calibration_or_after_any_mismatch() {
        for matched in 0..64 {
            assert_eq!(
                choose(Mode::Host, false, matched, 100, true, 0),
                Action::Compare
            );
        }
        assert_eq!(choose(Mode::Host, false, 64, 100, true, 0), Action::Host);
        assert_eq!(choose(Mode::Host, true, 1000, 100, true, 0), Action::Native);
        assert_eq!(
            choose(Mode::Host, false, 1000, 100, false, 0),
            Action::Native
        );
    }
    #[test]
    fn periodic_and_forced_native_paths_cannot_be_skipped() {
        assert_eq!(
            choose(Mode::Host, false, 1000, 256, true, 0),
            Action::Compare
        );
        assert_eq!(
            choose(Mode::Host, false, 1000, 34, true, 17),
            Action::Native
        );
        assert_eq!(choose(Mode::Host, false, 1000, 35, true, 17), Action::Host);
        assert_eq!(
            choose(Mode::Shadow, false, 1000, 35, true, 0),
            Action::Compare
        );
        for text in [None, Some(""), Some("1"), Some("unknown")] {
            assert_eq!(Mode::parse(text), Mode::Off);
        }
    }
}
