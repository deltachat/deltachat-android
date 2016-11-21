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
//
//  Created by Pitiphong Phongpattranont on 28/3/56 BE.
//

#ifdef HAVE_CONFIG_H
#	include <config.h>
#endif

#include "mailimap_sort_types.h"
#include "mmapstring.h"
#include "mail.h"
#include "mailimap_extension.h"

#include <stdlib.h>
#include <stdio.h>
#include <stdbool.h>


struct mailimap_sort_key *
mailimap_sort_key_new(int sortk_type,
                      int is_reverse,
                      clist * sortk_multiple) {
  struct mailimap_sort_key * key;
  
  key = malloc(sizeof(* key));
  if (key == NULL)
    return NULL;
  
  key->sortk_type = sortk_type;
  key->sortk_is_reverse = is_reverse;
  
  if (sortk_type == MAILIMAP_SORT_KEY_MULTIPLE) {
    key->sortk_multiple = sortk_multiple;
  }
  
  return key;
}


void mailimap_sort_key_free(struct mailimap_sort_key * key) {
  if (key->sortk_type == MAILIMAP_SORT_KEY_MULTIPLE) {
    clist_foreach(key->sortk_multiple,
                  (clist_func) mailimap_sort_key_free, NULL);
    clist_free(key->sortk_multiple);
  }
  
  free(key);
}


struct mailimap_sort_key *
mailimap_sort_key_new_arrival(int is_reverse) {
  return mailimap_sort_key_new(MAILIMAP_SORT_KEY_ARRIVAL, is_reverse, NULL);
}

struct mailimap_sort_key *
mailimap_sort_key_new_cc(int is_reverse) {
  return mailimap_sort_key_new(MAILIMAP_SORT_KEY_CC, is_reverse, NULL);
}

struct mailimap_sort_key *
mailimap_sort_key_new_date(int is_reverse) {
  return mailimap_sort_key_new(MAILIMAP_SORT_KEY_DATE, is_reverse, NULL);
}

struct mailimap_sort_key *
mailimap_sort_key_new_from(int is_reverse) {
  return mailimap_sort_key_new(MAILIMAP_SORT_KEY_FROM, is_reverse, NULL);
}

struct mailimap_sort_key *
mailimap_sort_key_new_size(int is_reverse) {
  return mailimap_sort_key_new(MAILIMAP_SORT_KEY_SIZE, is_reverse, NULL);
}

struct mailimap_sort_key *
mailimap_sort_key_new_subject(int is_reverse) {
  return mailimap_sort_key_new(MAILIMAP_SORT_KEY_SUBJECT, is_reverse, NULL);
}

struct mailimap_sort_key *
mailimap_sort_key_new_to(int is_reverse) {
  return mailimap_sort_key_new(MAILIMAP_SORT_KEY_TO, is_reverse, NULL);
}

struct mailimap_sort_key *
mailimap_sort_key_new_multiple(clist * keys) {
  return mailimap_sort_key_new(MAILIMAP_SORT_KEY_MULTIPLE, false, keys);
}


struct mailimap_sort_key *
mailimap_sort_key_new_multiple_empty(void)
{
  clist * list;
  
  list = clist_new();
  if (list == NULL)
    return NULL;
  
  return mailimap_sort_key_new_multiple(list);
}

int
mailimap_sort_key_multiple_add(struct mailimap_sort_key * keys,
                               struct mailimap_sort_key * key_item)
{
  int r;
	
  r = clist_append(keys->sortk_multiple, key_item);
  if (r < 0)
    return MAILIMAP_ERROR_MEMORY;
  
  return MAILIMAP_NO_ERROR;
}


