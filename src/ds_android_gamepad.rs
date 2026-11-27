// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

//! Digital Android gamepad actions, using the same names as the PC launcher.

const ACTIONS: [&str; 8] = [
    "interact", "kinesis", "reload", "stasis", "melee", "primary", "cursor", "pause",
];
pub const BUTTONS: [&str; 10] = ["A", "B", "X", "Y", "LB", "RB", "START", "BACK", "LS", "RS"];
const DEFAULTS: [usize; ACTIONS.len()] = [0, 1, 2, 3, 4, 5, 6, 7];

pub struct Bindings([usize; ACTIONS.len()]);

impl Bindings {
    pub fn parse(text: &str) -> Option<Self> {
        let mut buttons = DEFAULTS;
        let mut actions_seen = [false; ACTIONS.len()];
        for line in text.lines().map(str::trim) {
            if line.is_empty() || line.starts_with('#') {
                continue;
            }
            let (action, button) = line.split_once('=')?;
            let action = ACTIONS.iter().position(|&name| name == action.trim())?;
            if actions_seen[action] {
                return None;
            }
            actions_seen[action] = true;
            buttons[action] = BUTTONS.iter().position(|name| name.eq_ignore_ascii_case(button.trim()))?;
        }
        let mut buttons_seen = [false; BUTTONS.len()];
        for &button in &buttons {
            if buttons_seen[button] {
                return None;
            }
            buttons_seen[button] = true;
        }
        Some(Self(buttons))
    }

    pub fn action_for(&self, button: usize) -> Option<usize> {
        self.0.iter().position(|&mapped| mapped == button)
    }

    pub fn interact_button(&self) -> usize {
        self.0[0]
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn swaps_and_unassigned_buttons() {
        let map = Bindings::parse("interact=START\ncursor=A\nreload=LS").unwrap();
        assert_eq!(map.action_for(6), Some(0));
        assert_eq!(map.action_for(0), Some(6));
        assert_eq!(map.action_for(8), Some(2));
        assert_eq!(map.action_for(2), None);
        assert_eq!(map.interact_button(), 6);
    }

    #[test]
    fn conflicts_and_unknown_tokens_are_invalid() {
        for invalid in ["reload=A", "reload=RT", "reload=X\nreload=LS", "locator=RS", "reload"] {
            assert!(Bindings::parse(invalid).is_none(), "{invalid}");
        }
    }

    #[test]
    fn default_matches_launcher() {
        let defaults = Bindings::parse("").unwrap();
        for action in 0..ACTIONS.len() {
            assert_eq!(defaults.action_for(action), Some(action));
        }
        assert_eq!(defaults.action_for(9), None);
    }
}
