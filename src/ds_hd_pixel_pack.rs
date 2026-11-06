//! Pure resource conversion for the opt-in opaque HD color diagnostic.
//! Keeping RGBA storage preserves fixed-function alpha semantics that RGB565
//! would change for texture-environment modes such as REPLACE.

pub fn opaque_rgba5551(pixels: &[u8]) -> Option<Vec<u16>> {
    if pixels.is_empty() || pixels.len() % 4 != 0
        || !pixels.chunks_exact(4).all(|pixel| pixel[3] == 255)
    {
        return None;
    }
    let mut packed = Vec::with_capacity(pixels.len() / 4);
    for pixel in pixels.chunks_exact(4) {
        let red = (u16::from(pixel[0]) * 31 + 127) / 255;
        let green = (u16::from(pixel[1]) * 31 + 127) / 255;
        let blue = (u16::from(pixel[2]) * 31 + 127) / 255;
        packed.push((red << 11) | (green << 6) | (blue << 1) | 1);
    }
    Some(packed)
}

/// Round all RGBA channels together to preserve premultiplied RGB <= alpha.
pub fn rgba4444_nearest(pixels: &[u8]) -> Option<Vec<u16>> {
    if pixels.is_empty() || pixels.len() % 4 != 0 {
        return None;
    }
    let mut packed = Vec::with_capacity(pixels.len() / 4);
    for pixel in pixels.chunks_exact(4) {
        let red = (u16::from(pixel[0]) * 15 + 127) / 255;
        let green = (u16::from(pixel[1]) * 15 + 127) / 255;
        let blue = (u16::from(pixel[2]) * 15 + 127) / 255;
        let alpha = (u16::from(pixel[3]) * 15 + 127) / 255;
        packed.push((red << 12) | (green << 8) | (blue << 4) | alpha);
    }
    Some(packed)
}

#[cfg(test)]
mod tests {
    use super::{opaque_rgba5551, rgba4444_nearest};

    #[test]
    fn rejects_transparency_and_malformed_input_without_output() {
        for pixels in [vec![], vec![1, 2, 3], vec![1, 2, 3, 254],
            vec![1, 2, 3, 255, 4, 5, 6, 0]]
        {
            assert!(opaque_rgba5551(&pixels).is_none());
        }
    }

    #[test]
    fn canonical_colors_keep_rgba_layout_and_opaque_alpha() {
        let pixels = [0, 0, 0, 255, 255, 0, 0, 255, 0, 255, 0, 255,
            0, 0, 255, 255, 255, 255, 255, 255, 7, 11, 15, 255];
        assert_eq!(opaque_rgba5551(&pixels).unwrap(),
            [0x0001, 0xf801, 0x07c1, 0x003f, 0xffff, 0x0845]);
    }

    #[test]
    fn every_byte_value_has_bounded_color_error_and_preserves_alpha() {
        for value in 0..=255u8 {
            let pixels = [value, 255 - value, value.wrapping_mul(73), 255];
            let word = opaque_rgba5551(&pixels).unwrap()[0];
            assert_eq!(word & 1, 1);
            for (shift, source) in [11, 6, 1].into_iter().zip(pixels) {
                let decoded = (((word >> shift) & 31) * 255 + 15) / 31;
                assert!((i32::from(decoded) - i32::from(source)).abs() <= 4);
            }
        }
    }

    #[test]
    fn rounded4444_rejects_incomplete_pixels() {
        assert!(rgba4444_nearest(&[]).is_none());
        assert!(rgba4444_nearest(&[1, 2, 3]).is_none());
    }

    #[test]
    fn rounded4444_canonical_rgba_colors() {
        let pixels = [7, 11, 15, 255, 255, 0, 0, 128, 0, 255, 0, 7,
            0, 0, 255, 32, 240, 240, 240, 240];
        assert_eq!(rgba4444_nearest(&pixels).unwrap(),
            [0x011f, 0xf008, 0x0f00, 0x00f2, 0xeeee]);
    }

    #[test]
    fn rounded4444_bounds_every_color_and_alpha_byte() {
        for value in 0..=255u8 {
            for alpha in 0..=255u8 {
                let pixels = [value, 255 - value, value.wrapping_mul(73), alpha];
                let word = rgba4444_nearest(&pixels).unwrap()[0];
                assert!((i32::from((word & 15) * 17) - i32::from(alpha)).abs() <= 8);
                for (shift, source) in [12, 8, 4].into_iter().zip(pixels) {
                    let decoded = ((word >> shift) & 15) * 17;
                    assert!((i32::from(decoded) - i32::from(source)).abs() <= 8);
                }
            }
        }
    }

    #[test]
    fn rounded4444_removes_large_dark_range_bias() {
        let mut legacy_error = 0i32;
        let mut rounded_error = 0i32;
        for value in 0..64u8 {
            let word = rgba4444_nearest(&[value, value, value, 255]).unwrap()[0];
            rounded_error += i32::from((word >> 12) * 17) - i32::from(value);
            legacy_error += i32::from(value >> 4) * 17 - i32::from(value);
        }
        assert!(rounded_error.abs() < 64);
        assert!(legacy_error <= -6 * 64);
    }

    #[test]
    fn rounded4444_preserves_premultiplied_storage_for_every_alpha_and_channel() {
        assert_eq!(rgba4444_nearest(&[15,15,15,15]).unwrap(),[0x1111]);
        for alpha in 0..=255u8 {
            for red in 0..=alpha {
                let pixels=[red,alpha-red,red.wrapping_mul(73).min(alpha),alpha];
                let word=rgba4444_nearest(&pixels).unwrap()[0];
                for shift in [12,8,4] {assert!(((word>>shift)&15)<=(word&15));}
                if (word&15)==0 {assert_eq!(word,0);}
            }
        }
    }
}
