//! Accept native AF only when the requested value actually survives set/get.
pub(super) fn verified_limit(requested: f32, reported: f32, actual: f32, errors: [u32; 3]) -> Option<f32> {
    if !requested.is_finite() || requested <= 1.0 || !reported.is_finite()
        || reported < 1.0 || !actual.is_finite() || errors != [0; 3] {
        return None;
    }
    let wanted = requested.min(reported);
    ((actual - wanted).abs() <= 0.01).then_some(reported)
}

#[cfg(test)]
mod tests {
    use super::verified_limit;
    #[test] fn advertised_but_rejected_is_not_support() {
        assert_eq!(verified_limit(4.0, 16.0, 0.0, [0, 0x500, 0x500]), None);
    }
    #[test] fn successful_query_does_not_hide_a_set_error() {
        assert_eq!(verified_limit(4.0, 16.0, 4.0, [0, 0x500, 0]), None);
    }
    #[test] fn silent_clamp_must_not_claim_requested_filtering() {
        assert_eq!(verified_limit(4.0, 16.0, 1.0, [0; 3]), None);
    }
    #[test] fn accepts_readback_and_driver_limit_clamping() {
        assert_eq!(verified_limit(4.0, 16.0, 4.0, [0; 3]), Some(16.0));
        assert_eq!(verified_limit(8.0, 4.0, 4.0, [0; 3]), Some(4.0));
    }
    #[test] fn invalid_floats_and_query_errors_are_rejected() {
        assert_eq!(verified_limit(4.0, f32::NAN, 4.0, [0; 3]), None);
        assert_eq!(verified_limit(4.0, 16.0, f32::INFINITY, [0; 3]), None);
        assert_eq!(verified_limit(4.0, 16.0, 4.0, [0x500, 0, 0]), None);
    }
}
