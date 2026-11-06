/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <time.h>
#include <zlib.h>
#include "stb_image.h"
extern void touchHLE_png_inflate_counters(unsigned long long*,unsigned long long*);
static uint64_t now(void){struct timespec t;clock_gettime(CLOCK_MONOTONIC,&t);return (uint64_t)t.tv_sec*1000000+t.tv_nsec/1000;}
int main(int argc,char **argv) {
    if(argc<2)return 2;
    for(int file=1;file<argc;file++) {
        FILE *input=fopen(argv[file],"rb");if(!input)return 3;
        fseek(input,0,SEEK_END);long length=ftell(input);rewind(input);
        if(length<=0 || length>INT32_MAX){fclose(input);return 4;}
        unsigned char *data=malloc((size_t)length);if(!data)return 5;
        if(fread(data,1,(size_t)length,input)!=(size_t)length){fclose(input);free(data);return 6;}fclose(input);
        stbi_convert_iphone_png_to_rgb(1);stbi_set_unpremultiply_on_load(1);
        unsigned char *reference=NULL;size_t reference_bytes=0;int rw=0,rh=0;
        for(int round=0;round<6;round++) {
            // ABBAAB order, including one initial legacy reference decode.
            int fast=round==1 || round==2 || round==5;
            setenv("TOUCHHLE_DS_PNG_INFLATE",fast?"system":"legacy",1);
            unsigned long long f0,l0,f1,l1;touchHLE_png_inflate_counters(&f0,&l0);
            int w=0,h=0,c=0;uint64_t begin=now();unsigned char *rgba=stbi_load_from_memory(data,(int)length,&w,&h,&c,4);uint64_t elapsed=now()-begin;
            touchHLE_png_inflate_counters(&f1,&l1);
            if(!rgba){fprintf(stderr,"decode failed %s: %s\n",argv[file],stbi_failure_reason());return 7;}
            size_t bytes=(size_t)w*(size_t)h*4;
            if(!reference){reference=rgba;reference_bytes=bytes;rw=w;rh=h;}
            else {if(w!=rw || h!=rh || bytes!=reference_bytes || memcmp(reference,rgba,bytes)){fprintf(stderr,"pixel mismatch %s\n",argv[file]);return 8;}stbi_image_free(rgba);}
            printf("{\"file\":\"%s\",\"round\":%d,\"mode\":\"%s\",\"width\":%d,\"height\":%d,\"decode_us\":%llu,\"fast_inflate_calls\":%llu,\"legacy_inflate_calls\":%llu,\"byte_equal\":true}\n",argv[file],round,fast?"system":"legacy",w,h,(unsigned long long)elapsed,f1-f0,l1-l0);fflush(stdout);
        }
        stbi_image_free(reference);free(data);
    }
    return 0;
}
