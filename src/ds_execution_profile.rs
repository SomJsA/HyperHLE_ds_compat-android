/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
//! Opt-in, sampled emulator wall-time attribution. Guest/JIT intervals are
//! disjoint; inclusive completed host calls may contain callbacks/waits, so
//! never sum host rows or describe them as CPU/GPU execution time.

use std::cell::{Cell, RefCell};
use std::collections::HashMap;
use std::sync::OnceLock;
use std::time::{Duration, Instant};
const MAX_SYMBOLS: usize = 512;

#[derive(Default)]
struct Metric {
    samples: u64,
    calls: u64,
    elapsed: Duration,
    max: Duration,
}
impl Metric {
    fn add(&mut self, elapsed: Duration, weight: u32) {
        self.samples += 1;
        self.calls += u64::from(weight);
        self.elapsed += elapsed * weight;
        self.max = self.max.max(elapsed);
    }
}
#[derive(Default)]
struct Profile {
    since: Option<Instant>,
    guest: Metric,
    host: HashMap<&'static str, Metric>,
}
impl Profile {
    fn host_call(&mut self, symbol: &'static str, elapsed: Duration, weight: u32) {
        let symbol = if self.host.contains_key(symbol) || self.host.len() < MAX_SYMBOLS {
            symbol
        } else {
            "<other-host-symbols>"
        };
        self.host.entry(symbol).or_default().add(elapsed, weight);
    }
    fn reset(&mut self, now: Instant) {
        self.since = Some(now);
        self.guest = Metric::default();
        self.host.clear();
    }
}
thread_local! {
    static PROFILE: RefCell<Profile> = RefCell::new(Profile::default());
    static SAMPLE_STATE: Cell<u64> = const { Cell::new(0x7e3a_2139_84d1_bf65) };
}
pub struct Sample {
    start: Instant,
    weight: u32,
}
#[inline]
pub fn enabled() -> bool {
    static ENABLED: OnceLock<bool> = OnceLock::new();
    *ENABLED.get_or_init(|| std::env::var("TOUCHHLE_DS_EXECUTION_PROFILE").as_deref() == Ok("1"))
}
fn sample_shift() -> u32 {
    static SHIFT: OnceLock<u32> = OnceLock::new();
    *SHIFT.get_or_init(|| {
        std::env::var("TOUCHHLE_DS_EXECUTION_PROFILE_SAMPLE_SHIFT")
            .ok()
            .and_then(|s| s.parse::<u32>().ok())
            .unwrap_or(6)
            .min(10)
    })
}
fn next_sample(state: &mut u64, shift: u32) -> bool {
    // Avoid synchronising the selection with repeating native call sequences.
    *state ^= *state << 13;
    *state ^= *state >> 7;
    *state ^= *state << 17;
    (*state & ((1u64 << shift) - 1)) == 0
}
#[inline]
pub fn begin() -> Option<Sample> {
    if !enabled() {
        return None;
    }
    let shift = sample_shift();
    let selected = SAMPLE_STATE.with(|cell| {
        let mut state = cell.get();
        let selected = next_sample(&mut state, shift);
        cell.set(state);
        selected
    });
    selected.then(|| Sample {
        start: Instant::now(),
        weight: 1 << shift,
    })
}
/// Rare upload synchronisation is measured exactly, never extrapolated from
/// a few randomly selected slow operations.
pub fn begin_exact() -> Option<Sample> {
    enabled().then(|| Sample {
        start: Instant::now(),
        weight: 1,
    })
}
pub fn end_guest(sample: Option<Sample>) {
    if let Some(sample) = sample {
        let elapsed = sample.start.elapsed();
        PROFILE.with(|p| p.borrow_mut().guest.add(elapsed, sample.weight));
    }
}
pub fn end_host(symbol: &'static str, sample: Option<Sample>) {
    if let Some(sample) = sample {
        let elapsed = sample.start.elapsed();
        PROFILE.with(|p| p.borrow_mut().host_call(symbol, elapsed, sample.weight));
    }
}

pub struct HostScope {
    symbol: &'static str,
    sample: Option<Sample>,
}
impl Drop for HostScope {
    fn drop(&mut self) {
        end_host(self.symbol, self.sample.take());
    }
}
/// Low-frequency frame boundary: retain exact timing including early returns.
pub fn exact_host_scope(symbol: &'static str) -> HostScope {
    HostScope {
        symbol,
        sample: begin_exact(),
    }
}
/// Actual native-present boundary. Bounded reports; disabled runs read no clock.
pub fn present(frame: u64) {
    if !enabled() {
        return;
    }
    let now = Instant::now();
    let report = PROFILE.with(|p| {
        let mut p = p.borrow_mut();
        let Some(since) = p.since else {
            p.reset(now);
            return None;
        };
        let window = now.saturating_duration_since(since);
        if window < Duration::from_secs(2) {
            return None;
        }
        let mut rows: Vec<_> = p
            .host
            .iter()
            .map(|(&s, m)| (s, m.samples, m.calls, m.elapsed, m.max))
            .collect();
        rows.sort_unstable_by(|a, b| b.3.cmp(&a.3).then_with(|| a.0.cmp(b.0)));
        rows.truncate(12);
        let result = (
            window,
            p.guest.samples,
            p.guest.calls,
            p.guest.elapsed,
            rows,
        );
        p.reset(now);
        Some(result)
    });
    if let Some((window, samples, runs, guest, rows)) = report {
        log!("[DSexecution] frame={} sample_shift={} window_ms={:.3} estimated_guest_jit_ms={:.3} estimated_guest_runs={} guest_samples={} host_rows=estimated_inclusive_completed_wall_not_cpu", frame,sample_shift(),window.as_secs_f64()*1000.0,guest.as_secs_f64()*1000.0,runs,samples);
        for (symbol, samples, calls, elapsed, max) in rows {
            log!("[DSexecution] host symbol={} samples={} estimated_calls={} estimated_inclusive_ms={:.3} sample_max_ms={:.3}",symbol,samples,calls,elapsed.as_secs_f64()*1000.0,max.as_secs_f64()*1000.0);
        }
    }
}
#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn inclusive_host_and_guest_are_separate_and_reset() {
        let mut p = Profile::default();
        p.host_call("callback-owner", Duration::from_millis(20), 1);
        p.host_call("callback-owner", Duration::from_millis(7), 1);
        p.guest.add(Duration::from_millis(12), 1);
        assert_eq!(p.host["callback-owner"].elapsed, Duration::from_millis(27));
        assert_eq!(p.host["callback-owner"].max, Duration::from_millis(20));
        assert_eq!(p.guest.elapsed, Duration::from_millis(12));
        p.reset(Instant::now());
        assert!(p.host.is_empty());
        assert_eq!(p.guest.calls, 0);
    }
    #[test]
    fn symbol_growth_is_bounded_without_losing_weighted_time() {
        let mut p = Profile::default();
        for _ in 0..MAX_SYMBOLS {
            let s: &'static str = Box::leak(format!("symbol-{}", p.host.len()).into_boxed_str());
            p.host_call(s, Duration::from_micros(1), 64);
        }
        p.host_call("new-a", Duration::from_millis(3), 64);
        p.host_call("new-b", Duration::from_millis(2), 64);
        assert_eq!(p.host.len(), MAX_SYMBOLS + 1);
        assert_eq!(
            p.host["<other-host-symbols>"].elapsed,
            Duration::from_millis(320)
        );
    }
    #[test]
    fn sampling_weights_preserve_observed_max_and_sample_count() {
        let mut m = Metric::default();
        m.add(Duration::from_micros(10), 64);
        assert_eq!(m.samples, 1);
        assert_eq!(m.calls, 64);
        assert_eq!(m.elapsed, Duration::from_micros(640));
        assert_eq!(m.max, Duration::from_micros(10));
    }
    #[test]
    fn random_selection_has_expected_density_and_exact_mode_selects_all() {
        let mut state = 0x7e3a_2139_84d1_bf65;
        let mut selected = 0;
        for _ in 0..65536 {
            if next_sample(&mut state, 6) {
                selected += 1;
            }
        }
        assert!((800..1250).contains(&selected));
        for _ in 0..100 {
            assert!(next_sample(&mut state, 0));
        }
    }
}
