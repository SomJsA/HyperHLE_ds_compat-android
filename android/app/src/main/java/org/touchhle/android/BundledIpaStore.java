package org.touchhle.android;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;

/** Exact bundled-input copies; an existing user IPA is never replaced. API 21. */
final class BundledIpaStore {
    interface Source { InputStream open() throws IOException; }
    private BundledIpaStore() {}

    static synchronized File ensure(File base,String expected,Source source) throws IOException {
        if(expected==null||!expected.matches("[a-fA-F0-9]{64}"))throw new IOException("Invalid bundled IPA SHA-256");
        expected=expected.toLowerCase(Locale.ROOT);
        File apps=new File(base,"touchHLE_apps"),legacy=new File(apps,"DeadSpace.ipa");
        File marker=new File(apps,".DeadSpace.ipa.bundle-id");
        // Retain the previous installer's explicitly marked input, including a
        // user-customized copy. A missing/stale marker never licenses deletion.
        if(legacy.isFile()&&marker.isFile()&&expected.equals(readMarker(marker)))return legacy;
        File cache=new File(base,"touchHLE_bundled_cache");
        if(!cache.getCanonicalFile().equals(new File(base.getCanonicalFile(),cache.getName())))throw new IOException("Linked bundled cache path");
        if(!cache.isDirectory()&&!cache.mkdirs())throw new IOException("Could not create bundled cache");
        File published=new File(cache,expected),destination=new File(published,"DeadSpace.ipa");
        if(published.exists())return checked(published,destination,expected);
        File staging=new File(cache,".pending-"+UUID.randomUUID());
        if(!staging.mkdir())throw new IOException("Could not create bundled staging directory");
        File temporary=new File(staging,"DeadSpace.ipa");boolean moved=false;
        try {
            MessageDigest digest=digest();
            try(InputStream input=source.open();FileOutputStream output=new FileOutputStream(temporary)) {
                if(input==null)throw new IOException("Bundled IPA asset is unavailable");
                byte[] buffer=new byte[1024*1024];int count;
                while((count=input.read(buffer))!=-1){output.write(buffer,0,count);digest.update(buffer,0,count);}
                output.getFD().sync();
            }
            if(!expected.equals(hex(digest.digest())))throw new IOException("Bundled IPA content hash mismatch");
            // Publish a whole new directory. Existing nonempty directories and
            // their IPA files cannot be replaced by a POSIX directory rename.
            if(published.exists())return checked(published,destination,expected);
            if(!staging.renameTo(published)) {
                if(published.exists())return checked(published,destination,expected);
                throw new IOException("Could not publish bundled IPA cache");
            }
            moved=true;return destination;
        } finally {
            // Only these two paths were created by this invocation. No recurse.
            if(!moved){temporary.delete();staging.delete();}
        }
    }
    private static File checked(File folder,File file,String expected)throws IOException {
        if(!folder.getCanonicalFile().equals(new File(folder.getParentFile().getCanonicalFile(),folder.getName()))
                ||!file.getCanonicalFile().equals(new File(folder.getCanonicalFile(),file.getName()))
                ||!file.isFile()||!expected.equals(hash(file)))throw new IOException("Existing bundled cache differs; existing files were retained");
        return file;
    }
    private static String readMarker(File file)throws IOException {
        if(file.length()>128)return "";
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(InputStream input=new FileInputStream(file)){int value;while((value=input.read())!=-1){if(bytes.size()>=128)return "";bytes.write(value);}}
        return new String(bytes.toByteArray(),StandardCharsets.UTF_8).trim().toLowerCase(Locale.ROOT);
    }
    private static MessageDigest digest()throws IOException {
        try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException error){throw new IOException("SHA-256 unavailable",error);}
    }
    static String hash(File file)throws IOException {
        MessageDigest digest=digest();byte[] buffer=new byte[65536];int count;
        try(InputStream input=new FileInputStream(file)){while((count=input.read(buffer))!=-1)digest.update(buffer,0,count);}
        return hex(digest.digest());
    }
    private static String hex(byte[] data){StringBuilder value=new StringBuilder();for(byte b:data){value.append(Character.forDigit((b>>4)&15,16));value.append(Character.forDigit(b&15,16));}return value.toString();}
}
