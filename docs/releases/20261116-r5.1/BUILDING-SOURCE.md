# Android R5.1 release source

This source snapshot preserves the validated candidates' runtime input bytes. APKs were built on 9a733936 with the local snapshot recorded in BUILD-INPUTS.json. This publication commit freezes that same source and adds release metadata. No runtime was rebuilt for publication.

```
git clone --branch 20261116-r5.1 --recurse-submodules https://github.com/SomJsA/HyperHLE_ds_compat-android.git
cd HyperHLE_ds_compat-android
git -C vendor/SDL apply ../../tools/patches/sdl-android-thread-stack.patch
```

Then follow the normal Android build instructions. Public upstream SDL 2.26.4 (07d0f51f) plus the included Java thread-stack patch reconstructs the exact tested 446c02be tree. This was checked using an isolated Git index. The local custom commit need not exist in the upstream repository. Other submodule pins are unchanged. Four inherited regular-file Git modes are corrected without changing their bytes.

Prepared assets must include overlay/native-languages.json from this source. The source does not include the original IPA, user saves, release APKs or signing credentials. Use your trusted resource input; never repackage a derived IPA as part of the build. Published APK bytes remain identical to the validated candidates.
