//! Semantic PC bindings. Host keys are translated before both Down and Up.
use std::collections::HashMap;

pub const ACTIONS: [(&str, &str); 15] = [
    ("forward", "W"), ("left", "A"), ("back", "S"), ("right", "D"),
    ("interact", "E"), ("kinesis", "F"), ("stasis", "C"), ("reload", "R"),
    ("melee", "Q"), ("rotate", "X"), ("weapon1", "1"), ("weapon2", "2"),
    ("weapon3", "3"), ("weapon4", "4"), ("weapon5", "5"),
];

pub fn parse(text: &str) -> Result<HashMap<String, String>, String> {
    let mut by_action: HashMap<&str, String> = ACTIONS.iter().map(|(a, k)| (*a, k.to_string())).collect();
    let mut seen = std::collections::HashSet::new();
    for (line_no, raw) in text.lines().enumerate() {
        let line = raw.trim();
        if line.is_empty() || line.starts_with('#') { continue; }
        let (action, key) = line.split_once('=').ok_or_else(|| format!("line {}: expected action=key", line_no + 1))?;
        let action = action.trim();
        if !by_action.contains_key(action) || !seen.insert(action.to_string()) {
            return Err(format!("unknown or repeated action: {action}"));
        }
        let key = key.trim().to_ascii_uppercase();
        // M is a legacy frame marker. Keep it and function/modifier keys reserved.
        if key.len() != 1 || !key.as_bytes()[0].is_ascii_alphanumeric() || ["G", "M", "N", "P", "V"].contains(&key.as_str()) {
            return Err(format!("unsupported/reserved key: {key}"));
        }
        *by_action.get_mut(action).unwrap() = key;
    }
    let mut result = HashMap::new();
    for (action, semantic) in ACTIONS {
        let key = by_action.remove(action).unwrap();
        if result.insert(key.clone(), semantic.to_string()).is_some() {
            return Err(format!("key assigned to multiple actions: {key}"));
        }
    }
    Ok(result)
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test] fn defaults_and_swap() {
        assert_eq!(parse("").unwrap()["E"], "E");
        let map = parse("interact=R\nreload=E").unwrap();
        assert_eq!(map["R"], "E"); assert_eq!(map["E"], "R");
    }
    #[test] fn conflicts_and_reserved() {
        for text in ["interact=R", "interact=F5", "interact=M", "wat=K", "interact=K\ninteract=J", "interact=é"] {
            assert!(parse(text).is_err(), "{text}");
        }
    }
    #[test] fn unused_key_replaces_original() {
        let map = parse("# custom\ninteract=k").unwrap();
        assert_eq!(map["K"], "E"); assert!(!map.contains_key("E"));
    }
}
