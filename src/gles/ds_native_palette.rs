/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
//! Gate the experimental native-driver palette route; never invent support.

pub fn eligible(
    requested: bool,
    extensions: &str,
    entrypoints: [bool; 4],
    matrices: i32,
    units: i32,
) -> bool {
    requested
        && extensions
            .split_ascii_whitespace()
            .any(|s| s == "GL_OES_matrix_palette")
        && entrypoints.into_iter().all(|loaded| loaded)
        && matrices > 0
        && units > 0
}

pub fn filtered_extensions(extensions: &str, enabled: bool) -> String {
    extensions
        .split_ascii_whitespace()
        .filter(|s| enabled || *s != "GL_OES_matrix_palette")
        .collect::<Vec<_>>()
        .join(" ")
}

#[cfg(test)]
mod tests {
    use super::*;
    const EXT: &str = "GL_A GL_OES_matrix_palette GL_OES_matrix_palette_extra";

    #[test]
    fn gate_requires_request_and_real_complete_support() {
        assert!(eligible(true, EXT, [true; 4], 20, 4));
        assert!(!eligible(false, EXT, [true; 4], 20, 4));
        assert!(!eligible(
            true,
            "GL_OES_matrix_palette_extra",
            [true; 4],
            20,
            4
        ));
        for missing in 0..4 {
            let mut loaded = [true; 4];
            loaded[missing] = false;
            assert!(!eligible(true, EXT, loaded, 20, 4));
        }
        for (matrices, units) in [(0, 4), (20, 0), (-1, 4), (20, -1)] {
            assert!(!eligible(true, EXT, [true; 4], matrices, units));
        }
    }

    #[test]
    fn filtering_is_token_exact_and_reversible() {
        assert_eq!(
            filtered_extensions(EXT, false),
            "GL_A GL_OES_matrix_palette_extra"
        );
        assert_eq!(filtered_extensions(EXT, true), EXT);
        assert_eq!(filtered_extensions("", false), "");
    }
}
