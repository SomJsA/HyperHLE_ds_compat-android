//! Binary-specific subtitle indices and all-or-nothing draw gate preflight.
pub(crate) const SITES: [(u32, u32, u32); 2] = [
    (0x0018_9d64, 0x0a00_0008, 0xea00_0008),
    (0x0018_a1f8, 0x0a00_0033, 0xea00_0033),
];

pub(crate) fn entry(id: &str) -> Option<u32> {
    include_str!("ds_subtitle_ids.txt")
        .lines()
        .position(|s| s == id)
        .map(|index| index as u32 + 1)
}

pub(crate) fn preflight(actual: [Option<u32>; 2], suppressed: bool) -> bool {
    SITES.iter().enumerate().all(|(i, (_, original, skip))| {
        actual[i] == Some(if suppressed { *skip } else { *original })
    })
}

pub(crate) fn valid_segment(begin: u32, end: u32, segment: u32) -> Option<u32> {
    let size = end.checked_sub(begin)?;
    if begin < 0x1000 || size == 0 || size % 20 != 0 || size > 20 * 512 || segment >= size / 20 {
        return None;
    }
    begin.checked_add(segment * 20)
}

/// Resume only the exact live world that entered our Android settings menu.
pub(crate) fn resume_menu(saved:u64,current:u64,hud_state:u32)->bool {
    saved!=0&&saved==current&&matches!(hud_state,2|3)
}
/// RenderSubtitle checks the finished byte before drawing its active track.
pub(crate) fn visible_value(serial:u32,index:Option<u32>,finished:Option<u8>)->u64 {
    if finished!=Some(0){return 0;}
    index.map(|i|(u64::from(serial)<<32)|u64::from(i)).unwrap_or(0)
}
/// SubtitleTrack stores remaining milliseconds at +8 and per-cue durations
/// in its +0x24 vector (verified against SetTimes/OnUpdate/Reset).
pub(crate) fn timing_value(total:u32,remaining:i32)->u64 {
    if !(100..=600_000).contains(&total){return 0;}
    let elapsed=(i64::from(total)-i64::from(remaining)).clamp(0,i64::from(total)) as u32;
    (u64::from(total)<<32)|u64::from(elapsed)
}

pub(crate) fn opening_offset(id: &str) -> i64 {
    match id {
        "Oper_02" | "Miner_02" => 1500,
        "Miner_01" => -2500,
        _ => 0,
    }
}

/// Absolute cue windows use the original track clock. Looking ahead is
/// necessary for an early cue; waiting for its native event would be too late.
pub(crate) fn adjusted_cue(cues: &[(u32, u32, i64)], segment: u32, remaining: i32)
    -> Option<(u32, u32, u64)> {
    let current = cues.get(segment as usize)?;
    let mut clock = i64::from(current.1) - i64::from(remaining);
    for cue in &cues[..segment as usize] { clock += i64::from(cue.1); }
    let mut start = 0i64;
    let mut chosen = None;
    let mut latest_start = i64::MIN;
    for (position, &(index, duration, offset)) in cues.iter().enumerate() {
        if !(100..=600_000).contains(&duration) { return None; }
        let shifted = start + offset;
        if index != 0 && clock >= shifted && clock < shifted + i64::from(duration) && shifted >= latest_start {
            latest_start = shifted;
            chosen = Some((position as u32, index,
                (u64::from(duration) << 32) | (clock - shifted) as u64));
        }
        start += i64::from(duration);
    }
    chosen
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn opening_adjustments_use_guest_time_and_do_not_shift_sector_six() {
        assert_eq!(opening_offset("Anchor_02"), 0);
        assert_eq!(opening_offset("Oper_02"), 1500);
        assert_eq!(opening_offset("Miner_02"), 1500);
        assert_eq!(opening_offset("Miner_01"), -2500);
        let cues = [(1, 5000, 0), (2, 5000, 1500), (3, 5000, 0), (4, 5000, -2500)];
        assert_eq!(adjusted_cue(&cues, 1, 5000), None);
        assert_eq!(adjusted_cue(&cues, 1, 3500), Some((1, 2, 5000u64 << 32)));
        assert_eq!(adjusted_cue(&cues, 2, 5000), Some((2, 3, 5000u64 << 32)));
        assert_eq!(adjusted_cue(&cues, 2, 2500), Some((3, 4, 5000u64 << 32)));
        assert_eq!(adjusted_cue(&cues, 2, 2500), adjusted_cue(&cues, 2, 2500));
    }
    #[test]
    fn original_opening_track_keeps_silent_gaps_and_supports_early_lookahead() {
        // Original published/data/subtitles.sb: the timed opening track has
        // fourteen entries, including empty IDs for silence.
        let cues = [(0,4500,0),(1,4500,0),(0,3500,0),(2,4500,1500),
            (0,1000,0),(3,2000,1500),(0,3500,0),(4,6500,0),
            (0,3000,0),(5,3000,-2500),(0,5000,0),(6,3000,0),
            (0,1500,0),(7,5000,0)];
        assert_eq!(adjusted_cue(&cues,3,4500),None);
        assert_eq!(adjusted_cue(&cues,3,3000),Some((3,2,4500u64<<32)));
        assert_eq!(adjusted_cue(&cues,4,500),Some((3,2,(4500u64<<32)|3500)));
        assert_eq!(adjusted_cue(&cues,5,2000),Some((3,2,(4500u64<<32)|4000)));
        assert_eq!(adjusted_cue(&cues,5,500),Some((5,3,2000u64<<32)));
        assert_eq!(adjusted_cue(&cues,7,6500),Some((7,4,6500u64<<32)));
        assert_eq!(adjusted_cue(&cues,8,2500),Some((9,5,3000u64<<32)));
        assert_eq!(adjusted_cue(&cues,10,5000),None);
        assert_eq!(adjusted_cue(&cues,11,3000),Some((11,6,3000u64<<32)));
    }
    #[test]
    fn settings_resume_never_targets_a_new_or_already_running_world(){
        assert!(resume_menu(42,42,2));assert!(resume_menu(42,42,3));
        assert!(!resume_menu(0,0,2));assert!(!resume_menu(42,43,2));assert!(!resume_menu(42,42,0));
    }
    #[test]
    fn finished_or_unreadable_track_cannot_leave_the_last_caption_visible(){
        assert_eq!(visible_value(7,Some(10),Some(0)),(7u64<<32)|10);
        assert_eq!(visible_value(7,Some(10),Some(1)),0);
        assert_eq!(visible_value(7,Some(10),None),0);
    }
    #[test]
    fn native_remaining_time_is_bounded_and_retains_pause_progress(){
        assert_eq!(timing_value(10_000,7500),(10_000u64<<32)|2500);
        assert_eq!(timing_value(10_000,7500),timing_value(10_000,7500));
        assert_eq!(timing_value(10_000,-100),(10_000u64<<32)|10_000);
        assert_eq!(timing_value(0,0),0);assert_eq!(timing_value(u32::MAX,1),0);
    }
    #[test]
    fn known_dialogue_indices_and_missing_fallback() {
        assert!(entry("Van_47").is_some());
        assert!(entry("Anchor_01").is_some());
        assert_ne!(entry("Van_47"), entry("Anchor_01"));
        assert_eq!(entry("Anchor_01\0"), None);
        assert_eq!(entry("unknown dialogue"), None);
    }
    #[test]
    fn both_draw_sites_must_match_before_any_patch() {
        let originals = [Some(SITES[0].1), Some(SITES[1].1)];
        let skips = [Some(SITES[0].2), Some(SITES[1].2)];
        assert!(preflight(originals, false));
        assert!(preflight(skips, true));
        assert!(!preflight([originals[0], skips[1]], false));
        assert!(!preflight([skips[0], originals[1]], true));
        assert!(!preflight([originals[0], None], false));
    }
    #[test]
    fn track_boundaries_reject_sentinels_and_bad_segments() {
        assert_eq!(valid_segment(0x2000, 0x2028, 1), Some(0x2014));
        for (a, b, i) in [
            (0, 0, 0),
            (1, 21, 0),
            (0x2000, 0x2000, 0),
            (0x2000, 0x2001, 0),
            (0x2000, 0x2028, 2),
            (0x2028, 0x2000, 0),
            (u32::MAX - 5, u32::MAX, 1),
        ] {
            assert_eq!(valid_segment(a, b, i), None);
        }
    }
}
