/*
 * libEtPan! -- a mail library
 *
 * Copyright (C) 2001, 2005 - DINH Viet Hoa
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

/*
 * $Id: mailprivacy_tools.h,v 1.7 2007/12/10 21:32:59 hoa Exp $
 */

#ifndef MAIL_PRIVACY_TOOLS_H

#define MAIL_PRIVACY_TOOLS_H

#include <libetpan/mailmessage.h>
#include <libetpan/mailprivacy_types.h>

LIBETPAN_EXPORT
void mailprivacy_mime_clear(struct mailmime * mime);

LIBETPAN_EXPORT
FILE * mailprivacy_get_tmp_file(struct mailprivacy * privacy,
    char * filename, size_t size);

LIBETPAN_EXPORT
int mailprivacy_get_tmp_filename(struct mailprivacy * privacy,
    char * filename, size_t size);

LIBETPAN_EXPORT
struct mailmime *
mailprivacy_new_file_part(struct mailprivacy * privacy,
    char * filename,
    char * default_content_type, int default_encoding);

LIBETPAN_EXPORT
int mailmime_substitute(struct mailmime * old_mime,
    struct mailmime * new_mime);

LIBETPAN_EXPORT
int mailprivacy_fetch_mime_body_to_file(struct mailprivacy * privacy,
    char * filename, size_t size,
    mailmessage * msg, struct mailmime * mime);

LIBETPAN_EXPORT
int mailprivacy_get_part_from_file(struct mailprivacy * privacy,
    int check_privacy, int reencode,
    char * filename,
    struct mailmime ** result_mime);

LIBETPAN_EXPORT
int mail_quote_filename(char * result, size_t size, char * path);

LIBETPAN_EXPORT
void mailprivacy_prepare_mime(struct mailmime * mime);

LIBETPAN_EXPORT
char * mailprivacy_dup_imf_file(struct mailprivacy * privacy,
    char * source_filename);

LIBETPAN_EXPORT
struct mailmime_fields *
mailprivacy_mime_fields_dup(struct mailprivacy * privacy,
    struct mailmime_fields * mime_fields);

LIBETPAN_EXPORT
struct mailmime_parameter *
mailmime_parameter_dup(struct mailmime_parameter * param);

LIBETPAN_EXPORT
struct mailmime_composite_type *
mailmime_composite_type_dup(struct mailmime_composite_type * composite_type);

LIBETPAN_EXPORT
struct mailmime_discrete_type *
mailmime_discrete_type_dup(struct mailmime_discrete_type * discrete_type);

LIBETPAN_EXPORT
struct mailmime_type * mailmime_type_dup(struct mailmime_type * type);

LIBETPAN_EXPORT
struct mailmime_content *
mailmime_content_dup(struct mailmime_content * content);

LIBETPAN_EXPORT
int mailprivacy_fetch_decoded_to_file(struct mailprivacy * privacy,
    char * filename, size_t size,
    mailmessage * msg, struct mailmime * mime);

LIBETPAN_EXPORT
int mailprivacy_get_mime(struct mailprivacy * privacy,
    int check_privacy, int reencode,
    char * content, size_t content_len,
    struct mailmime ** result_mime);

#endif
