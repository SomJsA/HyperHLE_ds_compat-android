use std::cell::Cell;
use std::time::{Duration, Instant};

const MIN_SAMPLE: Duration = Duration::from_millis(4);
const MAX_SAMPLE: Duration = Duration::from_millis(120);
const DEFAULT_MIN_DEFLECTION_G: f32 = 0.128;
const MIN_ANGULAR_RATE_G_PER_S: f32 = 1.6;
const SHAKE_BYPASS_G: f32 = 2.0;
const PULSE_DURATION: Duration = Duration::from_millis(120);
const DEFAULT_PULSE_COOLDOWN: Duration = Duration::from_millis(380);
const REARM_DEFLECTION_G: f32 = 0.10;
const REARM_DURATION: Duration = Duration::from_millis(120);
const PULSE_X: f32 = 0.92;
const PULSE_Z: f32 = -0.39;

#[derive(Clone, Copy)]
struct Sample {
    at: Instant,
    x: f32,
}

#[derive(Clone, Copy)]
struct Pulse {
    until: Instant,
    x: f32,
}

#[derive(Clone, Copy, Default)]
struct State {
    previous: Option<Sample>,
    pulse: Option<Pulse>,
    cooldown_until: Option<Instant>,
    armed: bool,
    centered_since: Option<Instant>,
}

/// Converts a vigorous sideways flick into one short lateral pulse. A held
/// tilt does not retrigger until the device returns near neutral and cools down.
pub struct TiltPulse {
    enabled: bool,
    min_deflection_g: f32,
    pulse_cooldown: Duration,
    state: Cell<State>,
}

impl Default for TiltPulse {
    fn default() -> Self {
        Self {
            enabled: std::env::var("TOUCHHLE_DS_TILT_PULSE").as_deref() == Ok("1"),
            min_deflection_g: std::env::var("TOUCHHLE_DS_TILT_THRESHOLD_G")
                .ok()
                .and_then(|value| value.parse::<f32>().ok())
                .filter(|value| value.is_finite() && (0.08..=0.80).contains(value))
                .unwrap_or(DEFAULT_MIN_DEFLECTION_G),
            pulse_cooldown: std::env::var("TOUCHHLE_DS_TILT_COOLDOWN_MS")
                .ok()
                .and_then(|value| value.parse::<u64>().ok())
                .map(Duration::from_millis)
                .filter(|value| (100..=1000).contains(&value.as_millis()))
                .unwrap_or(DEFAULT_PULSE_COOLDOWN),
            state: Cell::new(State {
                armed: true,
                ..State::default()
            }),
        }
    }
}

impl TiltPulse {
    pub fn apply(&self, raw: (f32, f32, f32), now: Instant) -> (f32, f32, f32) {
        if !self.enabled {
            return raw;
        }
        let mut state = self.state.get();
        let magnitude = (raw.0 * raw.0 + raw.1 * raw.1 + raw.2 * raw.2).sqrt();

        // A zero-g shake must reach the guest untouched. Do not interpret its
        // large lateral component as a fire-mode gesture.
        if magnitude >= SHAKE_BYPASS_G {
            state.previous = Some(Sample { at: now, x: raw.0 });
            state.centered_since = None;
            self.state.set(state);
            return raw;
        }

        if let Some(pulse) = state.pulse {
            if now < pulse.until {
                state.previous = Some(Sample { at: now, x: raw.0 });
                self.state.set(state);
                return (pulse.x, 0.0, PULSE_Z);
            }
            state.pulse = None;
            state.cooldown_until = Some(pulse.until + self.pulse_cooldown);
            state.armed = false;
            state.centered_since = None;
        }

        if state.cooldown_until.is_some_and(|until| now < until) {
            state.previous = Some(Sample { at: now, x: raw.0 });
            self.state.set(state);
            return (0.0, 0.0, -1.0);
        }
        state.cooldown_until = None;

        if !state.armed {
            if raw.0.abs() <= REARM_DEFLECTION_G {
                let started = *state.centered_since.get_or_insert(now);
                if now.duration_since(started) >= REARM_DURATION {
                    state.armed = true;
                    state.centered_since = None;
                    echo!("[DStiltPulse] rearmed after neutral hold");
                }
            } else {
                state.centered_since = None;
            }
            state.previous = Some(Sample { at: now, x: raw.0 });
            self.state.set(state);
            return (0.0, 0.0, -1.0);
        }

        if let Some(previous) = state.previous {
            let elapsed = now.duration_since(previous.at);
            if elapsed >= MIN_SAMPLE && elapsed <= MAX_SAMPLE {
                let seconds = elapsed.as_secs_f32();
                let rate = (raw.0 - previous.x) / seconds;
                if raw.0.abs() >= self.min_deflection_g && rate.abs() >= MIN_ANGULAR_RATE_G_PER_S {
                    let direction = if rate < 0.0 { -1.0 } else { 1.0 };
                    let pulse = Pulse {
                        until: now + PULSE_DURATION,
                        x: direction * PULSE_X,
                    };
                    state.pulse = Some(pulse);
                    state.armed = false;
                    state.centered_since = None;
                    state.previous = Some(Sample { at: now, x: raw.0 });
                    self.state.set(state);
                    echo!(
                        "[DStiltPulse] pulse direction={} x={:.3} rate_g_per_s={:.3}",
                        if direction < 0.0 { "left" } else { "right" },
                        raw.0,
                        rate
                    );
                    return (pulse.x, 0.0, PULSE_Z);
                }
            }
        }

        state.previous = Some(Sample { at: now, x: raw.0 });
        self.state.set(state);
        raw
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn enabled_pulse() -> TiltPulse {
        let pulse = TiltPulse::default();
        TiltPulse { enabled: true, ..pulse }
    }

    #[test]
    fn default_is_off_and_leaves_native_sensor_input_unchanged() {
        let pulse = TiltPulse::default();
        assert!(!pulse.enabled);
        let now = Instant::now();
        pulse.apply((0.0, 0.0, -1.0), now);
        let input = (-0.64, 0.0, -0.76);
        assert_eq!(pulse.apply(input, now + Duration::from_millis(16)), input);
    }

    fn at(base: Instant, ms: u64) -> Instant {
        base + Duration::from_millis(ms)
    }

    #[test]
    fn gentle_tilt_does_not_switch() {
        let base = Instant::now();
        let pulse = enabled_pulse();
        for (ms, x) in [(0, 0.0), (100, -0.05), (200, -0.12), (300, -0.20)] {
            let output = pulse.apply((x, 0.0, -1.0), at(base, ms));
            assert!((output.0 - x).abs() < 0.001);
        }
    }

    #[test]
    fn strong_flick_emits_one_pulse_and_held_tilt_cannot_repeat() {
        let base = Instant::now();
        let pulse = enabled_pulse();
        pulse.apply((0.0, 0.0, -1.0), at(base, 0));
        let triggered = pulse.apply((-0.64, 0.0, -0.76), at(base, 16));
        assert!(triggered.0 < -0.9);

        let held = pulse.apply((-0.80, 0.0, -0.60), at(base, 80));
        assert_eq!(held, triggered);
        let still_held = pulse.apply((-0.80, 0.0, -0.60), at(base, 700));
        assert_eq!(still_held.0, 0.0);
    }

    #[test]
    fn returning_to_center_rearms_for_one_opposite_pulse() {
        let base = Instant::now();
        let pulse = enabled_pulse();
        pulse.apply((0.0, 0.0, -1.0), at(base, 0));
        assert!(pulse.apply((-0.70, 0.0, -0.71), at(base, 16)).0 < 0.0);
        pulse.apply((0.0, 0.0, -1.0), at(base, 600));
        pulse.apply((0.0, 0.0, -1.0), at(base, 740));
        let opposite = pulse.apply((0.70, 0.0, -0.71), at(base, 756));
        assert!(opposite.0 > 0.9);
    }

    #[test]
    fn zero_g_shake_bypasses_fire_mode_filter() {
        let base = Instant::now();
        let pulse = enabled_pulse();
        let raw = (3.2, 0.4, -5.0);
        assert_eq!(pulse.apply(raw, at(base, 16)), raw);
    }

    #[test]
    fn reduced_default_accepts_a_flick_at_forty_percent_of_old_strength() {
        let base=Instant::now();let pulse=enabled_pulse();
        assert_eq!(pulse.min_deflection_g,0.32*0.4);
        assert_eq!(pulse.pulse_cooldown,Duration::from_millis(380));
        pulse.apply((0.0,0.0,-1.0),at(base,0));
        assert!(pulse.apply((-0.128,0.0,-0.99),at(base,40)).0 < -0.9);
        assert_eq!(pulse.apply((0.0,0.0,-1.0),at(base,539)).0,0.0);
        pulse.apply((0.0,0.0,-1.0),at(base,540));
        pulse.apply((0.0,0.0,-1.0),at(base,660));
        assert!(pulse.apply((0.128,0.0,-0.99),at(base,700)).0 > 0.9);
    }
}
