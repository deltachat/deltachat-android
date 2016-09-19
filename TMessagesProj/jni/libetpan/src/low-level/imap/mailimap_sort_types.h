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

#ifndef MAILIMAP_SORT_TYPES_H

#define MAILIMAP_SORT_TYPES_H

#ifdef __cplusplus
extern "C" {
#endif
  
#ifndef WIN32
#include <inttypes.h>
#endif
  
#include "mailimap_types.h"
  
  /* this is the condition of the SORT operation */
  
  enum {
    MAILIMAP_SORT_KEY_ARRIVAL,
    MAILIMAP_SORT_KEY_CC,
    MAILIMAP_SORT_KEY_DATE,
    MAILIMAP_SORT_KEY_FROM,
    MAILIMAP_SORT_KEY_SIZE,
    MAILIMAP_SORT_KEY_SUBJECT,
    MAILIMAP_SORT_KEY_TO,
    MAILIMAP_SORT_KEY_MULTIPLE
  };
  
  struct mailimap_sort_key {
    int sortk_type;
    int sortk_is_reverse;
    clist * sortk_multiple; /* list of (struct mailimap_sort_key *) */
  };
  
  
  LIBETPAN_EXPORT
  struct mailimap_sort_key *
  mailimap_sort_key_new(int sortk_type,
                        int is_reverse,
                        clist * sortk_multiple);
  
  LIBETPAN_EXPORT
  void mailimap_sort_key_free(struct mailimap_sort_key * key);
  
  LIBETPAN_EXPORT
  struct mailimap_sort_key *
  mailimap_sort_key_new_arrival(int is_reverse);
  
  LIBETPAN_EXPORT
  struct mailimap_sort_key *
  mailimap_sort_key_new_cc(int is_reverse);
  
  LIBETPAN_EXPORT
  struct mailimap_sort_key *
  mailimap_sort_key_new_date(int is_reverse);
  
  LIBETPAN_EXPORT
  struct mailimap_sort_key *
  mailimap_sort_key_new_from(int is_reverse);
  
  LIBETPAN_EXPORT
  struct mailimap_sort_key *
  mailimap_sort_key_new_size(int is_reverse);
  
  LIBETPAN_EXPORT
  struct mailimap_sort_key *
  mailimap_sort_key_new_subject(int is_reverse);
  
  LIBETPAN_EXPORT
  struct mailimap_sort_key *
  mailimap_sort_key_new_to(int is_reverse);
  
  LIBETPAN_EXPORT
  struct mailimap_sort_key *
  mailimap_sort_key_new_multiple(clist * keys);
  
  LIBETPAN_EXPORT
  struct mailimap_sort_key *
  mailimap_sort_key_new_multiple_empty(void);
  
  LIBETPAN_EXPORT
  int
  mailimap_sort_key_multiple_add(struct mailimap_sort_key * keys,
                                 struct mailimap_sort_key * key_item);

  
#ifdef __cplusplus
}
#endif

#endif