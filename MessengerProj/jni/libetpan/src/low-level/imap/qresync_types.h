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

#ifndef QRESYNC_TYPES_H

#define QRESYNC_TYPES_H

#include <libetpan/libetpan-config.h>

enum {
  MAILIMAP_QRESYNC_TYPE_VANISHED,
  MAILIMAP_QRESYNC_TYPE_RESP_TEXT_CODE
};

struct mailimap_qresync_vanished {
  int qr_earlier;
  struct mailimap_set * qr_known_uids;
};

enum {
  MAILIMAP_QRESYNC_RESPTEXTCODE_CLOSED
};

struct mailimap_qresync_resptextcode {
  int qr_type;
};

LIBETPAN_EXPORT
struct mailimap_qresync_vanished * mailimap_qresync_vanished_new(int qr_earlier, struct mailimap_set * qr_known_uids);

LIBETPAN_EXPORT
void mailimap_qresync_vanished_free(struct mailimap_qresync_vanished * vanished);

LIBETPAN_EXPORT
struct mailimap_qresync_resptextcode * mailimap_qresync_resptextcode_new(int qr_type);

LIBETPAN_EXPORT
void mailimap_qresync_resptextcode_free(struct mailimap_qresync_resptextcode * resptextcode);

#endif
