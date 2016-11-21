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
#ifndef IMAPDRIVER_TOOLS_PRIVATE_H

#define IMAPDRIVER_TOOLS_PRIVATE_H

#include "mail_cache_db.h"

int
imapdriver_get_cached_envelope(struct mail_cache_db * cache_db,
    MMAPString * mmapstr,
    mailsession * session, mailmessage * msg,
    struct mailimf_fields ** result);

int
imapdriver_write_cached_envelope(struct mail_cache_db * cache_db,
    MMAPString * mmapstr,
    mailsession * session, mailmessage * msg,
    struct mailimf_fields * fields);

int imap_error_to_mail_error(int error);

int imap_store_flags(mailimap * imap, uint32_t first, uint32_t last,
    struct mail_flags * flags);

int imap_fetch_flags(mailimap * imap,
    uint32_t indx, struct mail_flags ** result);

int imap_get_messages_list(mailimap * imap,
    mailsession * session, mailmessage_driver * driver,
    uint32_t first_index,
    struct mailmessage_list ** result);

#endif
