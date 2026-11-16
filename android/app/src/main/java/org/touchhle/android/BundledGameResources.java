package org.touchhle.android;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Shared launcher/native-Activity preparation so failures can stay in the launcher. */
final class BundledGameResources {
    private BundledGameResources() { }
    static File ipa(Context context) throws IOException {
        AssetManager assets=context.getAssets();String expected;
        try(InputStream in=assets.open("touchHLE_bundled/bundle.id")) {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();int b;
            while((b=in.read())!=-1){if(bytes.size()>=128)throw new IOException("Invalid bundled IPA fingerprint");bytes.write(b);}
            expected=new String(bytes.toByteArray(),StandardCharsets.UTF_8).trim();
        }
        return BundledIpaStore.ensure(RuntimeStorage.requireCompatible(context),expected,
                ()->assets.open("touchHLE_bundled/DeadSpace.ipa"));
    }
    static void textures(Context context,boolean selected) throws IOException {
        if(selected&&BuildConfig.BUNDLED_TEXTURES)BundledTextureStore.ensure(
                RuntimeStorage.requireCompatible(context),context.getAssets(),Math.max(1,Math.min(4,BuildConfig.BUNDLED_TEXTURE_SCALE)));
    }
}
