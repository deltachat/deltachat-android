/*
 * libEtPan! -- a mail stuff library
 *
 * Copyright (C) 2001, 2013 - DINH Viet Hoa
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

#ifndef CONDSTORE_TYPE_H

#define CONDSTORE_TYPE_H

#include <libetpan/mailimap_types.h>

enum {
  MAILIMAP_CONDSTORE_TYPE_FETCH_DATA,
  MAILIMAP_CONDSTORE_TYPE_RESP_TEXT_CODE,
  MAILIMAP_CONDSTORE_TYPE_SEARCH_DATA,
  MAILIMAP_CONDSTORE_TYPE_STATUS_INFO
};

struct mailimap_condstore_fetch_mod_resp {
  uint64_t cs_modseq_value;
};

enum {
  MAILIMAP_CONDSTORE_RESPTEXTCODE_HIGHESTMODSEQ,
  MAILIMAP_CONDSTORE_RESPTEXTCODE_NOMODSEQ,
  MAILIMAP_CONDSTORE_RESPTEXTCODE_MODIFIED
};

struct mailimap_condstore_resptextcode {
  int cs_type;
  union {
    uint64_t cs_modseq_value;
    struct mailimap_set * cs_modified_set;
  } cs_data;
};

struct mailimap_condstore_search {
  clist * cs_search_result; /* uint32_t */
  uint64_t cs_modseq_value;
};

struct mailimap_condstore_status_info {
  uint64_t cs_highestmodseq_value;
};

LIBETPAN_EXPORT
struct mailimap_condstore_fetch_mod_resp * mailimap_condstore_fetch_mod_resp_new(uint64_t cs_modseq_value);

LIBETPAN_EXPORT
void mailimap_condstore_fetch_mod_resp_free(struct mailimap_condstore_fetch_mod_resp * fetch_data);

LIBETPAN_EXPORT
struct mailimap_condstore_resptextcode * mailimap_condstore_resptextcode_new(int cs_type,
  uint64_t cs_modseq_value, struct mailimap_set * cs_modified_set);

LIBETPAN_EXPORT
void mailimap_condstore_resptextcode_free(struct mailimap_condstore_resptextcode * resptextcode);

LIBETPAN_EXPORT
struct mailimap_condstore_search * mailimap_condstore_search_new(clist * cs_search_result, uint64_t cs_modseq_value);

LIBETPAN_EXPORT
void mailimap_condstore_search_free(struct mailimap_condstore_search * search_data);

LIBETPAN_EXPORT
struct mailimap_condstore_status_info * mailimap_condstore_status_info_new(uint64_t cs_highestmodseq_value);

LIBETPAN_EXPORT
void mailimap_condstore_status_info_free(struct mailimap_condstore_status_info * status_info);

#endif
