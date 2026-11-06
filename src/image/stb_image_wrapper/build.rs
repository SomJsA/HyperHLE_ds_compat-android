/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
use std::path::Path;

fn rerun_if_changed(path: &Path) {
    println!("cargo:rerun-if-changed={}", path.to_str().unwrap());
}

fn main() {
    let package_root = Path::new(env!("CARGO_MANIFEST_DIR"));
    let workspace_root = package_root.join("../../..");

    let mut build = cc::Build::new();
    build.file(package_root.join("lib.c"));
    if std::env::var("CARGO_CFG_TARGET_OS").as_deref() == Ok("android") {
        build.file(package_root.join("android_png_legacy.c"));
    }
    build.compile("stb_image_wrapper");
    if std::env::var("CARGO_CFG_TARGET_OS").as_deref() == Ok("android") {
        println!("cargo:rustc-link-lib=z");
    }
    rerun_if_changed(&package_root.join("android_png_legacy.c"));
    rerun_if_changed(&package_root.join("android_png_inflate.inc"));
    rerun_if_changed(&package_root.join("lib.c"));
    rerun_if_changed(&workspace_root.join("vendor/stb/stb_image.h"));
    rerun_if_changed(&workspace_root.join("vendor/stb/stb_image_write.h"));
}
