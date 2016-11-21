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

#include "quota_parser.h"
#include "mailimap_keywords.h"
#include "mailimap_extension.h"
#include "quota.h"
#include "quota_parser.h"

#include <stdlib.h>

int
mailimap_quota_quota_resource_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
    size_t * indx, void * result_ptr,
    size_t progr_rate, progress_function * progr_fun)
{
  struct mailimap_quota_quota_resource ** result = result_ptr;
  size_t cur_token = * indx;
  int r, res;
  char * resource_name;
  uint32_t usage, limit;
  struct mailimap_quota_quota_resource * resource;

  r = mailimap_atom_parse(fd, buffer, parser_ctx, &cur_token, &resource_name,
      progr_rate, progr_fun);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }

  r = mailimap_space_parse(fd, buffer, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto resource_name_free;
  }

  r = mailimap_number_parse(fd, buffer, &cur_token, &usage);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto resource_name_free;
  }

  r = mailimap_space_parse(fd, buffer, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto resource_name_free;
  }

  r = mailimap_number_parse(fd, buffer, &cur_token, &limit);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto resource_name_free;
  }

  resource = mailimap_quota_quota_resource_new(resource_name,
      usage, limit);
  if (!resource) {
    res = r;
    goto resource_name_free;
  }

  * result = resource;
  * indx = cur_token;

  return MAILIMAP_NO_ERROR;

 resource_name_free:
  mailimap_atom_free(resource_name);
 err:
  return res;
}

static int
mailimap_quota_quota_list_nonempty_parse(mailstream * fd, MMAPString * buffer,
    size_t * indx, clist ** result,
    size_t progr_rate, progress_function * progr_fun)
{
  size_t cur_token;
  int r;
  int res;
  clist * quota_resource_list;

  cur_token = * indx;

  r = mailimap_oparenth_parse(fd, buffer, NULL, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }

  r = mailimap_struct_spaced_list_parse(fd, buffer, NULL,
      &cur_token, &quota_resource_list,
      &mailimap_quota_quota_resource_parse,
      (mailimap_struct_destructor *)
      &mailimap_quota_quota_resource_free,
      progr_rate, progr_fun);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }

  r = mailimap_cparenth_parse(fd, buffer, NULL, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto quota_list_free;
  }

  * result = quota_resource_list;
  * indx = cur_token;

  return MAILIMAP_NO_ERROR;

 quota_list_free:
  clist_foreach(quota_resource_list,
      (clist_func) &mailimap_quota_quota_resource_free, NULL);
  clist_free(quota_resource_list);
 err:
  return res;
}

static int
mailimap_quota_quota_list_empty_parse(mailstream * fd, MMAPString * buffer,
    size_t * indx, clist ** result,
    size_t progr_rate, progress_function * progr_fun)
{
  size_t cur_token;
  int r;
  clist * quota_resource_list;

  cur_token = * indx;

  r = mailimap_oparenth_parse(fd, buffer, NULL, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    return r;
  }

  r = mailimap_cparenth_parse(fd, buffer, NULL, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    return r;
  }

  quota_resource_list = clist_new();
  if (!quota_resource_list) {
    return MAILIMAP_ERROR_MEMORY;
  }

  * result = quota_resource_list;
  * indx = cur_token;
  
  return MAILIMAP_NO_ERROR;
}

static int
mailimap_quota_quota_list_parse(mailstream * fd, MMAPString * buffer,
    size_t * indx, clist ** result,
    size_t progr_rate, progress_function * progr_fun)
{
  int r;

  r = mailimap_quota_quota_list_empty_parse(fd, buffer, indx, result,
      progr_rate, progr_fun);
  if (r == MAILIMAP_NO_ERROR) {
    return r;
  }

  return mailimap_quota_quota_list_nonempty_parse(fd, buffer, indx, result,
      progr_rate, progr_fun);
}

static int
mailimap_quota_quota_response_parse(mailstream * fd, MMAPString * buffer,
    size_t * indx, struct mailimap_quota_quota_data ** result,
    size_t progr_rate, progress_function * progr_fun)
{
  size_t cur_token;
  char * quotaroot;
  clist * quota_list;
  struct mailimap_quota_quota_data * quota_data;
  int r;
  int res;

  cur_token = * indx;

  r = mailimap_token_case_insensitive_parse(fd, buffer,
					    &cur_token, "QUOTA");
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }

  r = mailimap_space_parse(fd, buffer, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }

  r = mailimap_astring_parse(fd, buffer, NULL, &cur_token, &quotaroot,
          progr_rate, progr_fun);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }

  r = mailimap_space_parse(fd, buffer, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto quotaroot_free;
  }

  r = mailimap_quota_quota_list_parse(fd, buffer, &cur_token,
      &quota_list, progr_rate, progr_fun);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto quotaroot_free;
  }

  quota_data = mailimap_quota_quota_data_new(quotaroot, quota_list);
  if (quota_data == NULL) {
    res = MAILIMAP_ERROR_MEMORY;
    goto quota_list_free;
  }

  * result = quota_data;
  * indx = cur_token;

  return MAILIMAP_NO_ERROR;

 quota_list_free:
  clist_foreach(quota_list,
      (clist_func) &mailimap_quota_quota_resource_free, NULL);
  clist_free(quota_list);
 quotaroot_free:
  mailimap_astring_free(quotaroot);
 err:
  return res;
}

static int
mailimap_quota_quotaroot_response_parse(mailstream * fd, MMAPString * buffer,
    size_t * indx, struct mailimap_quota_quotaroot_data ** result,
    size_t progr_rate, progress_function * progr_fun)
{
  size_t cur_token;
  char * mailbox;
  char * quotaroot;
  clist * quotaroot_list;
  struct mailimap_quota_quotaroot_data * quotaroot_data;
  int r;
  int res;

  cur_token = * indx;

  r = mailimap_token_case_insensitive_parse(fd, buffer,
					    &cur_token, "QUOTAROOT");
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }

  r = mailimap_space_parse(fd, buffer, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }

  r = mailimap_mailbox_parse(fd, buffer, NULL, &cur_token, &mailbox,
          progr_rate, progr_fun);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }

  quotaroot_list = clist_new();
  if (!quotaroot_list) {
    res = MAILIMAP_ERROR_MEMORY;
    goto mailbox_free;
  }

  for (;;) {
    r = mailimap_space_parse(fd, buffer, &cur_token);
    if (r == MAILIMAP_ERROR_PARSE) {
      break;
    } else if (r != MAILIMAP_NO_ERROR) {
      res = r;
      goto quotaroot_list_free;
    }

    r = mailimap_astring_parse(fd, buffer, NULL, &cur_token, &quotaroot,
        progr_rate, progr_fun);
    if (r != MAILIMAP_NO_ERROR) {
      res = r;
      goto quotaroot_list_free;
    }

    if (clist_append(quotaroot_list, quotaroot) < 0) {
      mailimap_astring_free(quotaroot);
      res = MAILIMAP_ERROR_MEMORY;
      goto quotaroot_list_free;
    }
  }

  quotaroot_data = mailimap_quota_quotaroot_data_new(mailbox,
      quotaroot_list);
  if (quotaroot_data == NULL) {
    res = MAILIMAP_ERROR_MEMORY;
    goto quotaroot_list_free;
  }

  * result = quotaroot_data;
  * indx = cur_token;

  return MAILIMAP_NO_ERROR;

 quotaroot_list_free:
  clist_foreach(quotaroot_list,
      (clist_func) &mailimap_astring_free, NULL);
  clist_free(quotaroot_list);
 mailbox_free:
  mailimap_mailbox_free(mailbox);
 err:
  return res;
}

/*
  this is the extension's initial parser. it switches on calling_parser
  and calls the corresponding actual parser. quota extends
  imap as follows:
       mailbox-data       /= "*" SP quota_response CRLF /
                             "*" SP quotaroot_response CRLF

       quota_response     ::= "QUOTA" SP astring SP quota_list

       quotaroot_response ::= "QUOTAROOT" SP astring *(SP astring)

       quota_list         ::= "(" #quota_resource ")"

  note that RFC2087 doesn't actually specify whether the responses augment
  mailbox-data or augment the cases in response-data; I have chosen to
  place them in mailbox-data (the difference is academic as the byte stream
  is identical in either case)
*/
int mailimap_quota_parse(int calling_parser, mailstream * fd,
    MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
    struct mailimap_extension_data ** result,
    size_t progr_rate,
    progress_function * progr_fun)
{
  int r;
  struct mailimap_quota_quota_data * quota_data = 0;
  struct mailimap_quota_quotaroot_data * quotaroot_data = 0;
  void * data;
  int type;

  switch (calling_parser)
  {
    case MAILIMAP_EXTENDED_PARSER_MAILBOX_DATA:
      r = mailimap_quota_quota_response_parse(fd, buffer, indx,
        &quota_data, progr_rate, progr_fun);
      if (r == MAILIMAP_NO_ERROR) {
	type = MAILIMAP_QUOTA_TYPE_QUOTA_DATA;
	data = quota_data;
      }

      if (r == MAILIMAP_ERROR_PARSE) {
	r = mailimap_quota_quotaroot_response_parse(fd, buffer, indx,
          &quotaroot_data, progr_rate, progr_fun);
        if (r == MAILIMAP_NO_ERROR) {
          type = MAILIMAP_QUOTA_TYPE_QUOTAROOT_DATA;
          data = quotaroot_data;
        }
      }

      if (r != MAILIMAP_NO_ERROR) {
        return r;
      }

      * result = mailimap_extension_data_new(&mailimap_extension_quota,
                type, data);
      if (*result == NULL) {
        if (quota_data)
          mailimap_quota_quota_data_free(quota_data);
        if (quotaroot_data)
          mailimap_quota_quotaroot_data_free(quotaroot_data);
        return MAILIMAP_ERROR_MEMORY;
      }
      break;
    default:
      /* return a MAILIMAP_ERROR_PARSE if the extension
         doesn't extend calling_parser. */
      return MAILIMAP_ERROR_PARSE;
      break;
  }

  return MAILIMAP_NO_ERROR;
}

