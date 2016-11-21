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

#include "condstore_types.h"

#include <stdlib.h>

LIBETPAN_EXPORT
struct mailimap_condstore_fetch_mod_resp * mailimap_condstore_fetch_mod_resp_new(uint64_t cs_modseq_value)
{
  struct mailimap_condstore_fetch_mod_resp * fetch_data;
  
  fetch_data = malloc(sizeof(* fetch_data));
  if (fetch_data == NULL)
    return NULL;
  
  fetch_data->cs_modseq_value = cs_modseq_value;
  
  return fetch_data;
}

LIBETPAN_EXPORT
void mailimap_condstore_fetch_mod_resp_free(struct mailimap_condstore_fetch_mod_resp * fetch_data)
{
  free(fetch_data);
}

LIBETPAN_EXPORT
struct mailimap_condstore_resptextcode * mailimap_condstore_resptextcode_new(int cs_type,
  uint64_t cs_modseq_value, struct mailimap_set * cs_modified_set)
{
  struct mailimap_condstore_resptextcode * resptextcode;
  
  resptextcode = malloc(sizeof(* resptextcode));
  if (resptextcode == NULL)
    return NULL;
  
  resptextcode->cs_type = cs_type;
  switch (cs_type) {
  case MAILIMAP_CONDSTORE_RESPTEXTCODE_HIGHESTMODSEQ:
    resptextcode->cs_data.cs_modseq_value = cs_modseq_value;
    break;
  case MAILIMAP_CONDSTORE_RESPTEXTCODE_NOMODSEQ:
    break;
  case MAILIMAP_CONDSTORE_RESPTEXTCODE_MODIFIED:
    resptextcode->cs_data.cs_modified_set = cs_modified_set;
    break;
  }
  
  return resptextcode;
}

LIBETPAN_EXPORT
void mailimap_condstore_resptextcode_free(struct mailimap_condstore_resptextcode * resptextcode)
{
  switch (resptextcode->cs_type) {
    case MAILIMAP_CONDSTORE_RESPTEXTCODE_HIGHESTMODSEQ:
      break;
    case MAILIMAP_CONDSTORE_RESPTEXTCODE_NOMODSEQ:
      break;
    case MAILIMAP_CONDSTORE_RESPTEXTCODE_MODIFIED:
      mailimap_set_free(resptextcode->cs_data.cs_modified_set);
      break;
  }
  free(resptextcode);
}

LIBETPAN_EXPORT
struct mailimap_condstore_search * mailimap_condstore_search_new(clist * cs_search_result, uint64_t cs_modseq_value)
{
  struct mailimap_condstore_search * search_data;
  
  search_data = malloc(sizeof(* search_data));
  if (search_data == NULL)
    return NULL;
    
  search_data->cs_search_result = cs_search_result;
  search_data->cs_modseq_value = cs_modseq_value;
  
  return search_data;
}

LIBETPAN_EXPORT
void mailimap_condstore_search_free(struct mailimap_condstore_search * search_data)
{
  if (search_data->cs_search_result != NULL) {
    clist_foreach(search_data->cs_search_result, (clist_func) free, NULL);
    clist_free(search_data->cs_search_result);
  }
  free(search_data);
}

LIBETPAN_EXPORT
struct mailimap_condstore_status_info * mailimap_condstore_status_info_new(uint64_t cs_highestmodseq_value)
{
  struct mailimap_condstore_status_info * status_info;
  
  status_info = malloc(sizeof(* status_info));
  if (status_info == NULL)
    return NULL;
  
  status_info->cs_highestmodseq_value = cs_highestmodseq_value;
  
  return status_info;
}

LIBETPAN_EXPORT
void mailimap_condstore_status_info_free(struct mailimap_condstore_status_info * status_info)
{
  free(status_info);
}

