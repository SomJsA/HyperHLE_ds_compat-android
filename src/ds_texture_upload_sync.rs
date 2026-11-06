//! Explicit Android texture-upload synchronization policy.
#[derive(Debug, Copy, Clone, PartialEq, Eq)]
pub(crate) enum Mode { Off, Flush, Finish }
impl Mode {
    pub(crate) fn parse(value: Option<&str>) -> Self {
        match value.map(str::trim) {
            None | Some("0" | "off" | "false") => Self::Off,
            Some("flush") => Self::Flush,
            // Preserve the previous presence-enabled policy for old/unknown values.
            Some(_) => Self::Finish,
        }
    }
}
#[cfg(test)] mod tests {
    use super::*;
    #[test] fn explicit_off_is_not_treated_as_presence_enabled_finish() {
        for value in [None,Some("0"),Some("off"),Some("false"),Some(" 0 ")] {
            assert_eq!(Mode::parse(value),Mode::Off);
        }
    }
    #[test] fn flush_is_a_distinct_non_finish_request() {assert_eq!(Mode::parse(Some("flush")),Mode::Flush);}
    #[test] fn existing_or_unknown_presence_preserves_finish() {
        for value in ["1","true","finish","","unknown"] {assert_eq!(Mode::parse(Some(value)),Mode::Finish);}
    }
}
