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

#include "qresync_types.h"

#include <stdlib.h>

#include "mailimap_types.h"

LIBETPAN_EXPORT
struct mailimap_qresync_vanished * mailimap_qresync_vanished_new(int qr_earlier, struct mailimap_set * qr_known_uids)
{
  struct mailimap_qresync_vanished * vanished;
  
  vanished = malloc(sizeof(* vanished));
  if (vanished == NULL)
    return vanished;
  
  vanished->qr_earlier = qr_earlier;
  vanished->qr_known_uids = qr_known_uids;
  
  return vanished;
}

LIBETPAN_EXPORT
void mailimap_qresync_vanished_free(struct mailimap_qresync_vanished * vanished)
{
  mailimap_set_free(vanished->qr_known_uids);
  free(vanished);
}

LIBETPAN_EXPORT
struct mailimap_qresync_resptextcode * mailimap_qresync_resptextcode_new(int qr_type)
{
  struct mailimap_qresync_resptextcode * resptextcode;
  
  resptextcode = malloc(sizeof(* resptextcode));
  if (resptextcode == NULL)
    return resptextcode;
    
  resptextcode->qr_type = qr_type;
  
  return resptextcode;
}

LIBETPAN_EXPORT
void mailimap_qresync_resptextcode_free(struct mailimap_qresync_resptextcode * resptextcode)
{
  free(resptextcode);
}
