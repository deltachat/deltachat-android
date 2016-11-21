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
#include "mailimap_id_parser.h"

#include "mailimap_parser.h"
#include "mailimap_id_types.h"
#include "mailimap_keywords.h"
#include "mailimap_id.h"

#include <stdio.h>

static int mailimap_id_response_parse(mailstream * fd,
    MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
    struct mailimap_extension_data ** result);

static int mailimap_id_params_list_parse(mailstream * fd,
    MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
    struct mailimap_id_params_list ** result);

int mailimap_id_parse(int calling_parser, mailstream * fd,
    MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
    struct mailimap_extension_data ** result,
    size_t progr_rate,
    progress_function * progr_fun)
{
  if (calling_parser != MAILIMAP_EXTENDED_PARSER_RESPONSE_DATA)
    return MAILIMAP_ERROR_PARSE;

  return mailimap_id_response_parse(fd, buffer, parser_ctx, indx, result);
}

/* id_response ::= "ID" SPACE id_params_list */

static int mailimap_id_response_parse(mailstream * fd,
    MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
    struct mailimap_extension_data ** result)
{
  struct mailimap_id_params_list * params_list;
  struct mailimap_extension_data * ext_data;
  size_t cur_token;
  int r;

  cur_token = * indx;

  r = mailimap_token_case_insensitive_parse(fd, buffer, &cur_token, "ID");
  if (r != MAILIMAP_NO_ERROR)
    return r;

  r = mailimap_space_parse(fd, buffer, &cur_token);
  /* ignore result */
  
  r = mailimap_id_params_list_parse(fd, buffer, parser_ctx, &cur_token, &params_list);
  if (r != MAILIMAP_NO_ERROR)
    return r;

  ext_data = mailimap_extension_data_new(&mailimap_extension_id,
    0, params_list);
  if (ext_data == NULL) {
    mailimap_id_params_list_free(params_list);
    return MAILIMAP_ERROR_MEMORY;
  }

  * indx = cur_token;
  * result = ext_data;

  return MAILIMAP_NO_ERROR;
}

/*
  string SPACE nstring
*/

static int mailimap_id_param_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
				   size_t * indx, struct mailimap_id_param ** result,
				   size_t progr_rate,
				   progress_function * progr_fun)
{
  int r;
  char * name;
  char * value;
  size_t len;
  size_t cur_token;
  struct mailimap_id_param * param;
  
  cur_token = * indx;
  
  r = mailimap_string_parse(fd, buffer, parser_ctx, &cur_token, &name, &len, 0, NULL);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_space_parse(fd, buffer, &cur_token);
  /* ignore result */
  
  r = mailimap_nstring_parse(fd, buffer, parser_ctx, &cur_token, &value, &len, 0, NULL);
  if (r != MAILIMAP_NO_ERROR) {
    mailimap_string_free(name);
    return r;
  }
  
  param = mailimap_id_param_new(name, value);
  if (param == NULL) {
    mailimap_nstring_free(value);
    mailimap_string_free(name);
    return MAILIMAP_ERROR_MEMORY;
  }
  
  * indx = cur_token;
  * result = param;
  
  return MAILIMAP_NO_ERROR;
}

/*
id_params_list ::= "(" #(string SPACE nstring) ")" / nil
         ;; list of field value pairs
*/

static int mailimap_id_params_list_parse(mailstream * fd,
  MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
  struct mailimap_id_params_list ** result)
{
  struct mailimap_id_params_list * params_list;
  clist * items;
  size_t cur_token;
  int r;
  
  cur_token = * indx;

  r = mailimap_nil_parse(fd, buffer, parser_ctx, &cur_token);
  if (r == MAILIMAP_NO_ERROR) {
    * indx = cur_token;
    * result = NULL;
    return MAILIMAP_NO_ERROR;
  }
  
  r = mailimap_oparenth_parse(fd, buffer, parser_ctx, &cur_token);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_struct_spaced_list_parse(fd, buffer, parser_ctx,
  				  &cur_token, &items,
  				  (mailimap_struct_parser *) mailimap_id_param_parse,
  				  (mailimap_struct_destructor *) mailimap_id_param_free,
  				  0, NULL);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  params_list = mailimap_id_params_list_new(items);
  if (params_list == NULL) {
    clist_foreach(items, (clist_func) mailimap_id_param_free, NULL);
    clist_free(items);
    return MAILIMAP_ERROR_MEMORY;
  }

  r = mailimap_cparenth_parse(fd, buffer, parser_ctx, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    mailimap_id_params_list_free(params_list);
    return r;
  }
  
  * indx = cur_token;
  * result = params_list;
  
  return MAILIMAP_NO_ERROR;
}
