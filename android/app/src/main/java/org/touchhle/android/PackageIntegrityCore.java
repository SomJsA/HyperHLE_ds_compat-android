package org.touchhle.android;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.*;

/** Streaming checks of the installed APK. No extraction or game-state writes. API 21. */
final class PackageIntegrityCore {
    private static final String IPA = "assets/touchHLE_bundled/DeadSpace.ipa";
    private static final String ID = "assets/touchHLE_bundled/bundle.id";
    private static final long LIMIT = 8L * 1024 * 1024 * 1024;
    private static final String[] REQUIRED = {"AndroidManifest.xml", "classes.dex", IPA, ID,
            "lib/arm64-v8a/libtouchHLE.so", "lib/arm64-v8a/libSDL2.so",
            "assets/overlay/TitleChinese.ttf", "assets/overlay/StoryChinese.ttf",
            "assets/overlay/ChapterEnglish.ttf", "assets/overlay/subtitles-original.json"};
    static final class Result {
        final String apkSha256; final int files; final long bytes;
        Result(String sha, int files, long bytes) { this.apkSha256=sha; this.files=files; this.bytes=bytes; }
    }
    static final class Failure extends IOException {
        final String apkSha256, member;
        Failure(String sha,String member,String reason) { super(reason); this.apkSha256=sha;this.member=member; }
    }
    private PackageIntegrityCore() { }
    private static void interrupted() throws InterruptedIOException {
        if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Check cancelled");
    }
    private static MessageDigest sha() throws IOException {
        try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new IOException(e);}
    }
    private static String hex(byte[] bytes) {
        char[] chars=new char[bytes.length*2];String alphabet="0123456789abcdef";
        for(int i=0;i<bytes.length;i++){chars[2*i]=alphabet.charAt((bytes[i]&255)>>>4);chars[2*i+1]=alphabet.charAt(bytes[i]&15);}
        return new String(chars);
    }
    static String hash(File file) throws IOException {
        MessageDigest digest=sha();byte[] buffer=new byte[65536];
        try(InputStream in=new FileInputStream(file)) {
            int count;while((count=in.read(buffer))!=-1){interrupted();digest.update(buffer,0,count);}
        }
        return hex(digest.digest());
    }
    static Result verify(File apk) throws IOException {
        String apkSha=hash(apk);Set<String> seen=new HashSet<>();int files=0;long total=0;
        try(ZipFile zip=new ZipFile(apk)) {
            ZipEntry id=zip.getEntry(ID);
            if(id==null||id.getSize()<1||id.getSize()>128)throw new Failure(apkSha,ID,"Missing or invalid bundled resource fingerprint");
            ByteArrayOutputStream marker=new ByteArrayOutputStream();
            try(InputStream in=zip.getInputStream(id)){int b;while((b=in.read())!=-1){if(marker.size()>=128)throw new Failure(apkSha,ID,"Oversized resource fingerprint");marker.write(b);}}
            String expected=new String(marker.toByteArray(),StandardCharsets.UTF_8).trim().toLowerCase(Locale.ROOT);
            if(!expected.matches("[0-9a-f]{64}"))throw new Failure(apkSha,ID,"Invalid bundled resource SHA-256");
            Enumeration<? extends ZipEntry> entries=zip.entries();byte[] buffer=new byte[65536];
            while(entries.hasMoreElements()) {
                interrupted();ZipEntry entry=entries.nextElement();String name=entry.getName();
                if(!seen.add(name)||seen.size()>50000)throw new Failure(apkSha,name,"Duplicate or excessive archive entries");
                if(entry.isDirectory())continue;
                if(entry.getSize()<0||entry.getSize()>LIMIT||total>LIMIT-entry.getSize())throw new Failure(apkSha,name,"Invalid archive member size");
                CRC32 crc=new CRC32();MessageDigest ipa=IPA.equals(name)?sha():null;long size=0;
                try(InputStream in=zip.getInputStream(entry)) {
                    int count;while((count=in.read(buffer))!=-1) {
                        interrupted();size+=count;
                        if(size>entry.getSize())throw new Failure(apkSha,name,"Archive member exceeds its declared size");
                        crc.update(buffer,0,count);if(ipa!=null)ipa.update(buffer,0,count);
                    }
                } catch(ZipException e){throw new Failure(apkSha,name,"Archive data cannot be decoded: "+e.getMessage());}
                if(size!=entry.getSize()||entry.getCrc()<0||crc.getValue()!=entry.getCrc())
                    throw new Failure(apkSha,name,"CRC/length mismatch: expected "+entry.getCrc()+" / "+entry.getSize()+", read "+crc.getValue()+" / "+size);
                if(ipa!=null&&!expected.equals(hex(ipa.digest())))throw new Failure(apkSha,name,"Bundled IPA SHA-256 mismatch");
                files++;total+=size;
            }
            for(String required:REQUIRED)if(!seen.contains(required))throw new Failure(apkSha,required,"Required package resource is missing");
        }catch(ZipException e){throw new Failure(apkSha,"APK","Cannot read archive: "+e.getMessage());}
        return new Result(apkSha,files,total);
    }
}
