/*
 * libEtPan! -- a mail stuff library
 *
 * Copyright (C) 2001, 2014 - DINH Viet Hoa
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the libEtPan! project nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHORS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHORS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
#ifndef UIDPLUS_TYPES_H

#define UIDPLUS_TYPES_H

#ifdef __cplusplus
extern "C" {
#endif

#ifndef WIN32 
#include <inttypes.h>
#endif

#include "mailimap_types.h"

enum {
  MAILIMAP_UIDPLUS_RESP_CODE_APND,
  MAILIMAP_UIDPLUS_RESP_CODE_COPY,
  MAILIMAP_UIDPLUS_RESP_CODE_UIDNOTSTICKY
};

struct mailimap_uidplus_resp_code_apnd {
  uint32_t uid_uidvalidity;
  struct mailimap_set * uid_set;
};

struct mailimap_uidplus_resp_code_copy {
  uint32_t uid_uidvalidity;
  struct mailimap_set * uid_source_set;
  struct mailimap_set * uid_dest_set;
};

LIBETPAN_EXPORT
struct mailimap_uidplus_resp_code_apnd *
mailimap_uidplus_resp_code_apnd_new(uint32_t uid_uidvalidity, struct mailimap_set * uid_set);

LIBETPAN_EXPORT
void mailimap_uidplus_resp_code_apnd_free(struct mailimap_uidplus_resp_code_apnd * resp_code_apnd);

LIBETPAN_EXPORT
struct mailimap_uidplus_resp_code_copy *
mailimap_uidplus_resp_code_copy_new(uint32_t uid_uidvalidity, struct mailimap_set * uid_source_set, struct mailimap_set * uid_dest_set);

LIBETPAN_EXPORT
void mailimap_uidplus_resp_code_copy_free(struct mailimap_uidplus_resp_code_copy * resp_code_copy);

LIBETPAN_EXPORT
void mailimap_uidplus_free(struct mailimap_extension_data * ext_data);

#ifdef __cplusplus
}
#endif

#endif
