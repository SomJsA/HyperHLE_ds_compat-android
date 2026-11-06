/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

// Included after stb_image: use its exact allocator and error state.
#include <zlib.h>
extern char *touchHLE_stbi_legacy_inflate(const char*,int,int,int*,int);
extern const char *touchHLE_stbi_legacy_reason(void);
static __thread unsigned long long fast_calls,legacy_calls;
void touchHLE_png_inflate_counters(unsigned long long *fast,unsigned long long *legacy) {
    *fast=fast_calls;*legacy=legacy_calls;
}
static char *legacy(const char *buffer,int len,int initial,int *outlen,int header) {
    legacy_calls++;
    char *result=touchHLE_stbi_legacy_inflate(buffer,len,initial,outlen,header);
    if(!result){const char *reason=touchHLE_stbi_legacy_reason();stbi__err(reason?reason:"zlib",reason?reason:"zlib");}
    return result;
}
char *stbi_zlib_decode_malloc_guesssize_headerflag(const char *buffer,int len,int initial,int *outlen,int header) {
    const char *mode=getenv("TOUCHHLE_DS_PNG_INFLATE");
    if(mode && (!strcmp(mode,"legacy") || !strcmp(mode,"0"))) return legacy(buffer,len,initial,outlen,header);
    if(len<0 || initial<0) {stbi__err("bad zlib size","bad zlib size");return NULL;}
    size_t capacity=initial>0?(size_t)initial:16384;
    char *pixels=(char*)STBI_MALLOC(capacity);
    if(!pixels) return legacy(buffer,len,initial,outlen,header);
    z_stream stream;memset(&stream,0,sizeof(stream));
    if(inflateInit2(&stream,header?MAX_WBITS:-MAX_WBITS)!=Z_OK){STBI_FREE(pixels);return legacy(buffer,len,initial,outlen,header);}
    stream.next_in=(Bytef*)buffer;stream.avail_in=(uInt)len;
    for(;;) {
        stream.next_out=(Bytef*)pixels+stream.total_out;
        stream.avail_out=(uInt)(capacity-stream.total_out);
        int status=inflate(&stream,Z_NO_FLUSH);
        if(status==Z_STREAM_END) {
            int produced=(int)stream.total_out;inflateEnd(&stream);
            if(outlen)*outlen=produced;
            fast_calls++;return pixels;
        }
        if((status!=Z_OK && status!=Z_BUF_ERROR) || stream.avail_out!=0)break;
        if(capacity>=INT_MAX)break;
        size_t next=capacity>(size_t)INT_MAX/2?(size_t)INT_MAX:capacity*2;
        char *grown=(char*)STBI_REALLOC_SIZED(pixels,capacity,next);
        if(!grown)break;
        pixels=grown;capacity=next;
    }
    inflateEnd(&stream);STBI_FREE(pixels);
    // Preserve stb's acceptance of existing nonstandard/truncated streams.
    // In particular CgBI uses raw deflate. Failed fast work is freed first.
    return legacy(buffer,len,initial,outlen,header);
}
