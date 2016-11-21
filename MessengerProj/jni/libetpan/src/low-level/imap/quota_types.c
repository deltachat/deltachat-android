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

#ifdef HAVE_CONFIG_H
#	include <config.h>
#endif

#include "mailimap_types.h"
#include "quota_types.h"
#include "mailimap_extension.h"

#include <stdlib.h>
#include <string.h>

LIBETPAN_EXPORT
struct mailimap_quota_quota_resource *
mailimap_quota_quota_resource_new(char * resource_name,
    uint32_t usage, uint32_t limit)
{
  struct mailimap_quota_quota_resource * res;

  res = malloc(sizeof(* res));
  if (!res)
    return NULL;

  res->resource_name = resource_name;
  res->usage = usage;
  res->limit = limit;

  return res;
}

LIBETPAN_EXPORT
void
mailimap_quota_quota_resource_free(
    struct mailimap_quota_quota_resource * res) {
  mailimap_atom_free(res->resource_name);
  free(res);
}

LIBETPAN_EXPORT
struct mailimap_quota_quota_data *
mailimap_quota_quota_data_new(char * quotaroot, clist * quota_list)
{
  struct mailimap_quota_quota_data * data;

  data = malloc(sizeof(* data));
  if (!data)
    return NULL;

  data->quotaroot = quotaroot;
  data->quota_list = quota_list;

  return data;
}

LIBETPAN_EXPORT
void
mailimap_quota_quota_data_free(struct mailimap_quota_quota_data * data)
{
  mailimap_astring_free(data->quotaroot);
  clist_foreach(data->quota_list,
      (clist_func) &mailimap_quota_quota_resource_free, NULL);
  clist_free(data->quota_list);
  free(data);
}

LIBETPAN_EXPORT
struct mailimap_quota_quotaroot_data *
mailimap_quota_quotaroot_data_new(char * mailbox, clist * quotaroot_list)
{
  struct mailimap_quota_quotaroot_data * data;

  data = malloc(sizeof(* data));
  if (!data)
    return NULL;

  data->mailbox = mailbox;
  data->quotaroot_list = quotaroot_list;

  return data;
}

LIBETPAN_EXPORT
void
mailimap_quota_quotaroot_data_free(struct mailimap_quota_quotaroot_data * data)
{
  mailimap_mailbox_free(data->mailbox);
  clist_foreach(data->quotaroot_list,
      (clist_func) &mailimap_astring_free, NULL);
  clist_free(data->quotaroot_list);
  free(data);
}

LIBETPAN_EXPORT
struct mailimap_quota_complete_data *
mailimap_quota_complete_data_new(
    struct mailimap_quota_quotaroot_data * quotaroot_data,
    clist * quota_list)
{
  struct mailimap_quota_complete_data * data;

  data = malloc(sizeof(* data));
  if (!data)
    return NULL;

  data->quotaroot_data = quotaroot_data;
  data->quota_list = quota_list;

  return data;
}

LIBETPAN_EXPORT
void
mailimap_quota_complete_data_free(struct mailimap_quota_complete_data * data)
{
  mailimap_quota_quotaroot_data_free(data->quotaroot_data);
  clist_foreach(data->quota_list,
      (clist_func) &mailimap_quota_quota_data_free, NULL);
  clist_free(data->quota_list);
  free(data);
}

