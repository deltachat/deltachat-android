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

#ifndef QRESYNC_H

#define QRESYNC_H

#include <libetpan/mailimap_extension_types.h>
#include <libetpan/mailimap_types.h>
#include <libetpan/clist.h>
#include <libetpan/qresync_types.h>

LIBETPAN_EXPORT
extern struct mailimap_extension_api mailimap_extension_qresync;

/*
mailimap_select_qresync()
known_uids can be NULL
seq_match_data_sequences can be NULL
seq_match_data_uids can be NULL
*/

LIBETPAN_EXPORT
  int mailimap_select_qresync(mailimap * session, const char * mb,
  uint32_t uidvalidity, uint64_t modseq_value,
  struct mailimap_set * known_uids,
  struct mailimap_set * seq_match_data_sequences,
  struct mailimap_set * seq_match_data_uids,
  clist ** fetch_result, struct mailimap_qresync_vanished ** p_vanished,
  uint64_t * p_mod_sequence_value);

LIBETPAN_EXPORT
int mailimap_fetch_qresync(mailimap * session,
	struct mailimap_set * set,
	struct mailimap_fetch_type * fetch_type, uint64_t mod_sequence_value,
  clist ** fetch_result, struct mailimap_qresync_vanished ** p_vanished);

LIBETPAN_EXPORT
int mailimap_uid_fetch_qresync(mailimap * session,
	struct mailimap_set * set,
	struct mailimap_fetch_type * fetch_type, uint64_t mod_sequence_value,
  clist ** fetch_result, struct mailimap_qresync_vanished ** p_vanished);

LIBETPAN_EXPORT
int mailimap_has_qresync(mailimap * session);

#endif
