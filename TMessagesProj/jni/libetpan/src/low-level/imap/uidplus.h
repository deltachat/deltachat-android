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
#ifndef UIDPLUS_H

#define UIDPLUS_H

#ifdef __cplusplus
extern "C" {
#endif

#include <libetpan/libetpan-config.h>
#include <libetpan/mailimap_types.h>
#include <libetpan/uidplus_types.h>

LIBETPAN_EXPORT
extern struct mailimap_extension_api mailimap_extension_uidplus;

LIBETPAN_EXPORT
int mailimap_uid_expunge(mailimap * session, struct mailimap_set * set);

LIBETPAN_EXPORT
int mailimap_uidplus_copy(mailimap * session, struct mailimap_set * set,
    const char * mb,
    uint32_t * uidvalidity_result,
    struct mailimap_set ** source_result,
    struct mailimap_set ** dest_result);

LIBETPAN_EXPORT
int mailimap_uidplus_uid_copy(mailimap * session, struct mailimap_set * set,
    const char * mb,
    uint32_t * uidvalidity_result,
    struct mailimap_set ** source_result,
    struct mailimap_set ** dest_result);

LIBETPAN_EXPORT
int mailimap_uidplus_move(mailimap * session, struct mailimap_set * set,
    const char * mb,
    uint32_t * uidvalidity_result,
    struct mailimap_set ** source_result,
    struct mailimap_set ** dest_result);

LIBETPAN_EXPORT
int mailimap_uidplus_uid_move(mailimap * session, struct mailimap_set * set,
    const char * mb,
    uint32_t * uidvalidity_result,
    struct mailimap_set ** source_result,
    struct mailimap_set ** dest_result);

LIBETPAN_EXPORT
int mailimap_uidplus_append(mailimap * session, const char * mailbox,
    struct mailimap_flag_list * flag_list,
    struct mailimap_date_time * date_time,
    const char * literal, size_t literal_size,
    uint32_t * uidvalidity_result,
    uint32_t * uid_result);

LIBETPAN_EXPORT
int mailimap_uidplus_append_simple(mailimap * session, const char * mailbox,
    const char * content, size_t size,
    uint32_t * uidvalidity_result,
    uint32_t * uid_result);

LIBETPAN_EXPORT
int mailimap_has_uidplus(mailimap * session);

#ifdef __cplusplus
}
#endif

#endif
