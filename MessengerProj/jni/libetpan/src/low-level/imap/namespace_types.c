/*
 * libEtPan! -- a mail stuff library
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

#include "namespace_types.h"

#include "mailimap_types.h"
#include <stdlib.h>

LIBETPAN_EXPORT
struct mailimap_namespace_response_extension *
mailimap_namespace_response_extension_new(char * name,
                                          clist * values)
{
  struct mailimap_namespace_response_extension * ext;
  
  ext = malloc(sizeof(* ext));
  if (ext == NULL)
    return NULL;
  
  ext->ns_name = name;
  ext->ns_values = values;
  
  return ext;
}

LIBETPAN_EXPORT
void mailimap_namespace_response_extension_free(struct mailimap_namespace_response_extension * ext)
{
  clistiter * cur;
  
  for(cur = clist_begin(ext->ns_values) ; cur != NULL ; cur = clist_next(cur)) {
    char * value;
    
    value = clist_content(cur);
    mailimap_string_free(value);
  }
  clist_free(ext->ns_values);
  mailimap_string_free(ext->ns_name);
  free(ext);
}

LIBETPAN_EXPORT
struct mailimap_namespace_info * mailimap_namespace_info_new(char * prefix, char delimiter,
                                                             clist * extensions)
{
  struct mailimap_namespace_info * info;
  
  info = malloc(sizeof(* info));
  if (info == NULL)
    return NULL;
  
  info->ns_prefix = prefix;
  info->ns_delimiter = delimiter;
  info->ns_extensions = extensions;
  
  return info;
}

LIBETPAN_EXPORT
void mailimap_namespace_info_free(struct mailimap_namespace_info * info)
{
  clistiter * cur;
  
  if (info->ns_extensions != NULL) {
    for(cur = clist_begin(info->ns_extensions) ; cur != NULL ; cur = clist_next(cur)) {
      struct mailimap_namespace_response_extension * ext;
      
      ext = clist_content(cur);
      mailimap_namespace_response_extension_free(ext);
    }
    clist_free(info->ns_extensions);
  }
  mailimap_string_free(info->ns_prefix);
  free(info);
}

LIBETPAN_EXPORT
struct mailimap_namespace_item * mailimap_namespace_item_new(clist * data_list)
{
  struct mailimap_namespace_item * item;
  
  item = malloc(sizeof(* item));
  if (item == NULL)
    return NULL;
  
  item->ns_data_list = data_list;
  
  return item;
}

LIBETPAN_EXPORT
void mailimap_namespace_item_free(struct mailimap_namespace_item * item)
{
  clistiter * cur;
  
  for(cur = clist_begin(item->ns_data_list) ; cur != NULL ; cur = clist_next(cur)) {
    struct mailimap_namespace_info * info;
    
    info = clist_content(cur);
    mailimap_namespace_info_free(info);
  }
  clist_free(item->ns_data_list);
  free(item);
}

LIBETPAN_EXPORT
struct mailimap_namespace_data * mailimap_namespace_data_new(struct mailimap_namespace_item * personal,
                                                             struct mailimap_namespace_item * other,
                                                             struct mailimap_namespace_item * shared)
{
  struct mailimap_namespace_data * ns;
  
  ns = malloc(sizeof(* ns));
  if (ns == NULL)
    return NULL;
  
  ns->ns_personal = personal;
  ns->ns_other = other;
  ns->ns_shared = shared;
  
  return ns;
}

LIBETPAN_EXPORT
void mailimap_namespace_data_free(struct mailimap_namespace_data * ns)
{
  if (ns->ns_personal != NULL) {
    mailimap_namespace_item_free(ns->ns_personal);
  }
  if (ns->ns_other != NULL) {
    mailimap_namespace_item_free(ns->ns_other);
  }
  if (ns->ns_shared != NULL) {
    mailimap_namespace_item_free(ns->ns_shared);
  }
  free(ns);
}
