/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

#define STB_IMAGE_IMPLEMENTATION
#define STB_IMAGE_STATIC
#define STBI_ONLY_ZLIB
#define STBI_SUPPORT_ZLIB
#define STBI_NO_STDIO
#include "../../../vendor/stb/stb_image.h"
char *touchHLE_stbi_legacy_inflate(const char *buffer,int len,int initial,int *outlen,int header) {
    return stbi_zlib_decode_malloc_guesssize_headerflag(buffer,len,initial,outlen,header);
}
const char *touchHLE_stbi_legacy_reason(void) { return stbi_failure_reason(); }
