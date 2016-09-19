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

#ifdef HAVE_CONFIG_H
#	include <config.h>
#endif

#include "namespace_parser.h"

#include "namespace_types.h"
#include "mailimap_keywords.h"
#include "mailimap_extension.h"
#include "namespace.h"

#include <stdlib.h>
#include <string.h>

static int mailimap_namespace_data_parse(mailstream * fd,
                                         MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
                                         struct mailimap_namespace_data ** result,
                                         size_t progr_rate, progress_function * progr_fun);

static int mailimap_namespace_response_extension_parse(mailstream * fd,
                                                       MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
                                                       struct mailimap_namespace_response_extension ** result,
                                                       size_t progr_rate, progress_function * progr_fun);

int
mailimap_namespace_extension_parse(int calling_parser, mailstream * fd,
                                   MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
                                   struct mailimap_extension_data ** result,
                                   size_t progr_rate, progress_function * progr_fun)
{
  int r;
  struct mailimap_namespace_data * namespace_data = NULL;
  struct mailimap_extension_data * ext_data;
  void * data;
  int type;
  size_t cur_token;
  
  cur_token = * indx;
  
  switch (calling_parser)
  {
    case MAILIMAP_EXTENDED_PARSER_RESPONSE_DATA:
      r = mailimap_namespace_data_parse(fd, buffer, parser_ctx, &cur_token,
                                        &namespace_data, progr_rate, progr_fun);
      if (r == MAILIMAP_NO_ERROR) {
        type = MAILIMAP_NAMESPACE_TYPE_NAMESPACE;
        data = namespace_data;
      }
      
      if (r != MAILIMAP_NO_ERROR) {
        return r;
      }
      
      ext_data = mailimap_extension_data_new(&mailimap_extension_namespace,
                                             type, data);
      if (ext_data == NULL) {
        if (namespace_data != NULL)
          mailimap_namespace_data_free(namespace_data);
        return MAILIMAP_ERROR_MEMORY;
      }
      
      * result = ext_data;
      * indx = cur_token;
      
      return MAILIMAP_NO_ERROR;
      
    default:
      /* return a MAILIMAP_ERROR_PARSE if the extension
       doesn't extend calling_parser. */
      return MAILIMAP_ERROR_PARSE;
  }
}

/*
 "(" string SP  (<"> QUOTED_CHAR <"> /
 nil) *(Namespace_Response_Extension) ")"
 */

static int mailimap_namespace_info_parse(mailstream * fd,
                                         MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
                                         struct mailimap_namespace_info ** result,
                                         size_t progr_rate, progress_function * progr_fun)
{
  size_t cur_token = * indx;
  int r;
  int res;
  char * prefix;
  size_t prefix_len;
  clistiter * cur;
  clist * ext_list;
  char delimiter;
  struct mailimap_namespace_info * info;
  
  r = mailimap_oparenth_parse(fd, buffer, parser_ctx, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }
  
  r = mailimap_string_parse(fd, buffer, parser_ctx, &cur_token, &prefix, &prefix_len, progr_rate, progr_fun);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }
  
  r = mailimap_space_parse(fd, buffer, &cur_token);
  if (r == MAILIMAP_ERROR_PARSE) {
    /* do nothing */
  }
  
  r = mailimap_nil_parse(fd, buffer, parser_ctx, &cur_token);
  if (r == MAILIMAP_NO_ERROR) {
	delimiter = 0;
  }
  else {
    r = mailimap_dquote_parse(fd, buffer, parser_ctx, &cur_token);
    if (r == MAILIMAP_ERROR_PARSE) {
      res = r;
      goto free_prefix;
    }
    
    r = mailimap_quoted_char_parse(fd, buffer, parser_ctx, &cur_token, &delimiter);
    if (r == MAILIMAP_ERROR_PARSE) {
      // could not parse, use delimiter as fallback
      // this is an issue on Courier-IMAP
      delimiter = prefix[strlen(prefix) - 1];
    }
    
    r = mailimap_dquote_parse(fd, buffer, parser_ctx, &cur_token);
    if (r == MAILIMAP_ERROR_PARSE) {
      res = r;
      goto free_prefix;
    }
  }

  r = mailimap_cparenth_parse(fd, buffer, parser_ctx, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto free_prefix;
  }
  
  r = mailimap_struct_multiple_parse(fd, buffer, parser_ctx, &cur_token,
                                     &ext_list,
                                     (mailimap_struct_parser *)
                                     mailimap_namespace_response_extension_parse,
                                     (mailimap_struct_destructor *)
                                     mailimap_namespace_response_extension_free,
                                     progr_rate, progr_fun);
  if (r == MAILIMAP_ERROR_PARSE) {
    ext_list = NULL;
  }
  else if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto free_prefix;
  }
  
  info = mailimap_namespace_info_new(prefix, delimiter, ext_list);
  if (info == NULL) {
    res = MAILIMAP_ERROR_MEMORY;
    goto free_ext;
  }
  
  * result = info;
  * indx = cur_token;
  
  return MAILIMAP_NO_ERROR;
  
free_ext:
  if (ext_list != NULL) {
    for(cur = clist_begin(ext_list) ; cur != NULL ; cur = clist_next(cur)) {
      struct mailimap_namespace_response_extension * ext;
      
      ext = clist_content(cur);
      mailimap_namespace_response_extension_free(ext);
    }
    clist_free(ext_list);
  }
free_prefix:
  mailimap_string_free(prefix);
err:
  return res;
}

/*
 Namespace = nil / "(" 1*( "(" string SP  (<"> QUOTED_CHAR <"> /
 nil) *(Namespace_Response_Extension) ")" ) ")"

 Namespace = nil / "(" 1*( info ) ")"
*/


static int mailimap_namespace_item_parse(mailstream * fd,
                                         MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
                                         struct mailimap_namespace_item ** result,
                                         size_t progr_rate, progress_function * progr_fun)
{
  size_t cur_token = * indx;
  int r;
  int res;
  clist * info_list;
  clistiter * cur;
  struct mailimap_namespace_item * item;
  
  r = mailimap_nil_parse(fd, buffer, parser_ctx, &cur_token);
  if (r == MAILIMAP_NO_ERROR) {
    * indx = cur_token;
    * result = NULL;
    return MAILIMAP_NO_ERROR;
  }
  
  r = mailimap_oparenth_parse(fd, buffer, parser_ctx, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    return r;
  }
  
  r = mailimap_struct_multiple_parse(fd, buffer, parser_ctx, &cur_token,
                                     &info_list,
                                     (mailimap_struct_parser *)
                                     mailimap_namespace_info_parse,
                                     (mailimap_struct_destructor *)
                                     mailimap_namespace_info_free,
                                     progr_rate, progr_fun);
  if (r == MAILIMAP_ERROR_PARSE) {
    res = r;
    goto err;
  }
  
  r = mailimap_cparenth_parse(fd, buffer, parser_ctx, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
	res = r;
    goto free_info_list;
  }

  item = mailimap_namespace_item_new(info_list);
  if (item == NULL) {
    res = MAILIMAP_ERROR_MEMORY;
    goto free_info_list;
  }
  
  * indx = cur_token;
  * result = item;
  
  return MAILIMAP_NO_ERROR;
  
free_info_list:
  for(cur = clist_begin(info_list) ; cur != NULL ; cur = clist_next(cur)) {
    struct mailimap_namespace_info * info;
    
    info = clist_content(cur);
    mailimap_namespace_info_free(info);
  }
  clist_free(info_list);
err:
  return res;
}

/*
 Namespace_Command = "NAMESPACE"
 */

/*
 Namespace_Response_Extension = SP string SP "(" string *(SP string) ")"
 */

static int namespace_extension_value_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
                                           size_t * indx, char ** result,
                                           size_t progr_rate,
                                           progress_function * progr_fun)
{
  size_t result_len;
  
  return mailimap_string_parse(fd, buffer, parser_ctx, indx, result, &result_len, progr_rate, progr_fun);
}

static int mailimap_namespace_response_extension_parse(mailstream * fd,
                                                       MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
                                                       struct mailimap_namespace_response_extension ** result,
                                                       size_t progr_rate, progress_function * progr_fun)
{
  int r;
  int res;
  size_t cur_token;
  char * name;
  size_t name_len;
  clist * value_list;
  struct mailimap_namespace_response_extension * ext;
  clistiter * cur;
  
  cur_token = * indx;
  
  r = mailimap_space_parse(fd, buffer, &cur_token);
  if (r == MAILIMAP_ERROR_PARSE) {
    /* do nothing */
  }
  
  r = mailimap_string_parse(fd, buffer, parser_ctx, &cur_token, &name, &name_len, progr_rate, progr_fun);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }
  
  r = mailimap_space_parse(fd, buffer, &cur_token);
  if (r == MAILIMAP_ERROR_PARSE) {
    /* do nothing */
  }
  
  r = mailimap_oparenth_parse(fd, buffer, parser_ctx, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }
  
  r = mailimap_struct_spaced_list_parse(fd, buffer, parser_ctx, &cur_token, &value_list,
                                        (mailimap_struct_parser *)  namespace_extension_value_parse,
                                        (mailimap_struct_destructor *) mailimap_string_free,
                                        progr_rate, progr_fun);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto free_name;
  }
  
  r = mailimap_cparenth_parse(fd, buffer, parser_ctx, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto free_value_list;
  }
  
  ext = mailimap_namespace_response_extension_new(name, value_list);
  if (ext == NULL) {
    res = MAILIMAP_ERROR_MEMORY;
    goto free_value_list;
  }
  
  * indx = cur_token;
  * result = ext;
  
  return MAILIMAP_NO_ERROR;
  
free_value_list:
  for(cur = clist_begin(value_list) ; cur != NULL ; cur = clist_next(cur)) {
    char * value;
    
    value = clist_content(cur);
    mailimap_string_free(value);
  }
  clist_free(value_list);
free_name:
  mailimap_string_free(name);
err:
  return res;
}

 /*
 Namespace_Response = "*" SP "NAMESPACE" SP Namespace SP Namespace SP
 Namespace
 
 ; The first Namespace is the Personal Namespace(s)
 ; The second Namespace is the Other Users' Namespace(s)
 ; The third Namespace is the Shared Namespace(s)
*/

static int mailimap_namespace_data_parse(mailstream * fd,
                                         MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
                                         struct mailimap_namespace_data ** result,
                                         size_t progr_rate, progress_function * progr_fun)
{
  size_t cur_token;
  int r;
  int res;
  struct mailimap_namespace_item * personal_namespace;
  struct mailimap_namespace_item * other_namespace;
  struct mailimap_namespace_item * shared_namespace;
  struct mailimap_namespace_data * ns;
  
  cur_token = * indx;
  
  r = mailimap_token_case_insensitive_parse(fd, buffer,
                                            &cur_token, "NAMESPACE");
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }
  
  r = mailimap_space_parse(fd, buffer, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }
  
  r = mailimap_namespace_item_parse(fd, buffer, parser_ctx, &cur_token, &personal_namespace, progr_rate, progr_fun);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }
  
  r = mailimap_namespace_item_parse(fd, buffer, parser_ctx, &cur_token, &other_namespace, progr_rate, progr_fun);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto free_personal;
  }
  
  r = mailimap_namespace_item_parse(fd, buffer, parser_ctx, &cur_token, &shared_namespace, progr_rate, progr_fun);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto free_other;
  }
  
  ns = mailimap_namespace_data_new(personal_namespace, other_namespace, shared_namespace);
  if (ns == NULL) {
    res = MAILIMAP_ERROR_MEMORY;
    goto free_shared;
  }
  
  * result = ns;
  * indx = cur_token;
  
  return MAILIMAP_NO_ERROR;
  
free_shared:
  if (shared_namespace != NULL) {
    mailimap_namespace_item_free(shared_namespace);
  }
free_other:
  if (other_namespace != NULL) {
    mailimap_namespace_item_free(other_namespace);
  }
free_personal:
  if (personal_namespace != NULL) {
    mailimap_namespace_item_free(personal_namespace);
  }
err:
  return res;
}
