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
#include "mailimap_id_types.h"

#include <stdlib.h>

#include "mailimap_types.h"

LIBETPAN_EXPORT
struct mailimap_id_params_list * mailimap_id_params_list_new(clist * items)
{
  struct mailimap_id_params_list * list;
  
  list = malloc(sizeof(* list));
  if (list == NULL)
    return NULL;
  
  list->idpa_list = items;
  
  return list;
}

LIBETPAN_EXPORT
void mailimap_id_params_list_free(struct mailimap_id_params_list * list)
{
  clist_foreach(list->idpa_list, (clist_func) mailimap_id_param_free, NULL);
  clist_free(list->idpa_list);
  free(list);
}

LIBETPAN_EXPORT
struct mailimap_id_param * mailimap_id_param_new(char * name, char * value)
{
  struct mailimap_id_param * param;
  
  param = malloc(sizeof(* param));
  if (param == NULL)
    return NULL;
  
  param->idpa_name = name;
  param->idpa_value = value;
  
  return param;
}

LIBETPAN_EXPORT
void mailimap_id_param_free(struct mailimap_id_param * param)
{
  mailimap_string_free(param->idpa_name);
  mailimap_nstring_free(param->idpa_value);
  free(param);
}

LIBETPAN_EXPORT
struct mailimap_id_params_list * mailimap_id_params_list_new_empty(void)
{
  clist * items;
  struct mailimap_id_params_list * list;
  
  items = clist_new();
  if (items == NULL)
    return NULL;
  
  list = mailimap_id_params_list_new(items);
  if (list == NULL) {
    clist_free(items);
    return NULL;
  }
  
  return list;
}

LIBETPAN_EXPORT
int mailimap_id_params_list_add_name_value(struct mailimap_id_params_list * list, char * name, char * value)
{
  struct mailimap_id_param * param;
  int r;
  
  param = mailimap_id_param_new(name, value);
  if (param == NULL)
    return -1;
  
  r = clist_append(list->idpa_list, param);
  if (r < 0) {
    mailimap_id_param_free(param);
    return -1;
  }
  
  return 0;
}

