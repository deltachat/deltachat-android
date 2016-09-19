/*
 * libEtPan! -- a mail stuff library
 *
 * Copyright (C) 2001, 2011 - DINH Viet Hoa
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

#ifndef XGMLABELS_H
#define XGMLABELS_H

#ifdef __cplusplus
extern "C" {
#endif
  
#include <libetpan/libetpan-config.h>
#include <libetpan/mailimap_extension.h>
  
  struct mailimap_msg_att_xgmlabels {
    clist * att_labels; /* != NULL */
  };
  
  LIBETPAN_EXPORT
  extern struct mailimap_extension_api mailimap_extension_xgmlabels;
  
  LIBETPAN_EXPORT
  struct mailimap_fetch_att * mailimap_fetch_att_new_xgmlabels(void);
  
  LIBETPAN_EXPORT
  int mailimap_has_xgmlabels(mailimap * session);
  
  LIBETPAN_EXPORT
  struct mailimap_msg_att_xgmlabels * mailimap_msg_att_xgmlabels_new(clist * att_labels);

  LIBETPAN_EXPORT
  struct mailimap_msg_att_xgmlabels * mailimap_msg_att_xgmlabels_new_empty(void);

  LIBETPAN_EXPORT
  int mailimap_msg_att_xgmlabels_add(struct mailimap_msg_att_xgmlabels * att, char * label);
  
  LIBETPAN_EXPORT void mailimap_msg_att_xgmlabels_free(struct mailimap_msg_att_xgmlabels * att);
  
  LIBETPAN_EXPORT
  int
  mailimap_store_xgmlabels(mailimap * session,
                           struct mailimap_set * set,
                           int fl_sign, int fl_silent,
                           struct mailimap_msg_att_xgmlabels * labels);
  
  LIBETPAN_EXPORT
  int
  mailimap_uid_store_xgmlabels(mailimap * session,
                               struct mailimap_set * set,
                               int fl_sign, int fl_silent,
                               struct mailimap_msg_att_xgmlabels * labels);
  
#ifdef __cplusplus
}
#endif


#endif
