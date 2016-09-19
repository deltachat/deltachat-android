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
#include "mailimap_sort.h"

#include <stdlib.h>
#include "mailimap.h"
#include "mailimap_extension.h"
#include "mailimap_extension_types.h"
#include "mailimap_sender.h"
#include "mailimap_parser.h"
#include "mailimap_keywords.h"
#include "mailimap_sender.h"

enum {
  MAILIMAP_SORT_TYPE_SORT
};


int
mailimap_sort_send(mailstream * fd, const char * charset,
                       struct mailimap_sort_key * key, struct mailimap_search_key * searchkey);

int
mailimap_uid_sort_send(mailstream * fd, const char * charset,
                       struct mailimap_sort_key * key, struct mailimap_search_key * searchkey);

int mailimap_sort_key_send(mailstream * fd,
                           struct mailimap_sort_key * key);

static int
mailimap_sort_extension_parse(int calling_parser, mailstream * fd,
                               MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
                               struct mailimap_extension_data ** result,
                               size_t progr_rate, progress_function * progr_fun);


static void
mailimap_sort_extension_data_free(struct mailimap_extension_data * ext_data);

LIBETPAN_EXPORT
struct mailimap_extension_api mailimap_extension_sort = {
  /* name */          "SORT",
  /* extension_id */  MAILIMAP_EXTENSION_SORT,
  /* parser */        mailimap_sort_extension_parse,
  /* free */          mailimap_sort_extension_data_free
};


LIBETPAN_EXPORT
int
mailimap_sort(mailimap * session, const char * charset,
              struct mailimap_sort_key * key, struct mailimap_search_key * searchkey,
              clist ** result)
{
  struct mailimap_response * response;
  int r;
  int error_code;
  clist * sort_result = NULL;
  clistiter * cur = NULL;
  
  if (session->imap_state != MAILIMAP_STATE_SELECTED)
    return MAILIMAP_ERROR_BAD_STATE;
  
  r = mailimap_send_current_tag(session);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_sort_send(session->imap_stream, charset, key, searchkey);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_crlf_send(session->imap_stream);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  if (mailstream_flush(session->imap_stream) == -1)
    return MAILIMAP_ERROR_STREAM;
  
  if (mailimap_read_line(session) == NULL)
    return MAILIMAP_ERROR_STREAM;
  
  r = mailimap_parse_response(session, &response);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  
  for (cur = clist_begin(session->imap_response_info->rsp_extension_list);
       cur != NULL; cur = clist_next(cur)) {
    struct mailimap_extension_data * ext_data;
    
    ext_data = (struct mailimap_extension_data *) clist_content(cur);
    if (ext_data->ext_extension->ext_id == MAILIMAP_EXTENSION_SORT) {
      if (sort_result == NULL) {
        sort_result = ext_data->ext_data;
        ext_data->ext_data = NULL;
        ext_data->ext_type = -1;
      }
    }
  }
  
  clist_foreach(session->imap_response_info->rsp_extension_list,
                (clist_func) mailimap_extension_data_free, NULL);
  clist_free(session->imap_response_info->rsp_extension_list);
  session->imap_response_info->rsp_extension_list = NULL;
  
  if (sort_result == NULL) {
    return MAILIMAP_ERROR_EXTENSION;
  }
  
  error_code = response->rsp_resp_done->rsp_data.rsp_tagged->rsp_cond_state->rsp_type;
  switch (error_code) {
    case MAILIMAP_RESP_COND_STATE_OK:
      break;
      
    default:
      mailimap_search_result_free(sort_result);
      return MAILIMAP_ERROR_EXTENSION;
  }
  
  mailimap_response_free(response);
  
  * result = sort_result;
  
  return MAILIMAP_NO_ERROR;
}

LIBETPAN_EXPORT
int
mailimap_uid_sort(mailimap * session, const char * charset,
                  struct mailimap_sort_key * key, struct mailimap_search_key * searchkey,
                  clist ** result)
{
  struct mailimap_response * response;
  int r;
  int error_code;
  clistiter * cur = NULL;
  clist * sort_result = NULL;
  
  if (session->imap_state != MAILIMAP_STATE_SELECTED)
    return MAILIMAP_ERROR_BAD_STATE;
  
  r = mailimap_send_current_tag(session);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_uid_sort_send(session->imap_stream, charset, key, searchkey);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_crlf_send(session->imap_stream);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  if (mailstream_flush(session->imap_stream) == -1)
    return MAILIMAP_ERROR_STREAM;
  
  if (mailimap_read_line(session) == NULL)
    return MAILIMAP_ERROR_STREAM;
  
  r = mailimap_parse_response(session, &response);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  
  for (cur = clist_begin(session->imap_response_info->rsp_extension_list);
       cur != NULL; cur = clist_next(cur)) {
    struct mailimap_extension_data * ext_data;
    
    ext_data = (struct mailimap_extension_data *) clist_content(cur);
    if (ext_data->ext_extension->ext_id == MAILIMAP_EXTENSION_SORT) {
      if (sort_result == NULL) {
        sort_result = ext_data->ext_data;
        ext_data->ext_data = NULL;
        ext_data->ext_type = -1;
      }
    }
  }
  
  clist_foreach(session->imap_response_info->rsp_extension_list,
                (clist_func) mailimap_extension_data_free, NULL);
  clist_free(session->imap_response_info->rsp_extension_list);
  session->imap_response_info->rsp_extension_list = NULL;
  
  if (sort_result == NULL) {
    return MAILIMAP_ERROR_EXTENSION;
  }
  
  error_code = response->rsp_resp_done->rsp_data.rsp_tagged->rsp_cond_state->rsp_type;
  switch (error_code) {
    case MAILIMAP_RESP_COND_STATE_OK:
      break;
      
    default:
      mailimap_search_result_free(sort_result);
      return MAILIMAP_ERROR_EXTENSION;
  }
  
  mailimap_response_free(response);
  
  * result = sort_result;
  
  return MAILIMAP_NO_ERROR;
}

LIBETPAN_EXPORT
void mailimap_sort_result_free(clist * search_result)
{
  clist_foreach(search_result, (clist_func) free, NULL);
  clist_free(search_result);
}


int
mailimap_sort_send(mailstream * fd, const char * charset,
                   struct mailimap_sort_key * key, struct mailimap_search_key * searchkey)
{
  int r;
  
  r = mailimap_token_send(fd, "SORT");
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_space_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_oparenth_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_sort_key_send(fd, key);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_cparenth_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  if (charset != NULL) {
    r = mailimap_space_send(fd);
    if (r != MAILIMAP_NO_ERROR)
      return r;
    r = mailimap_astring_send(fd, charset);
    if (r != MAILIMAP_NO_ERROR)
      return r;
  }
  
  r = mailimap_space_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  if (searchkey != NULL) {
    r = mailimap_search_key_send(fd, searchkey);
    if (r != MAILIMAP_NO_ERROR)
      return r;
  }
  
  
  return MAILIMAP_NO_ERROR;
}

int
mailimap_uid_sort_send(mailstream * fd, const char * charset,
                       struct mailimap_sort_key * key, struct mailimap_search_key * searchkey)

{
  int r;
  
  r = mailimap_token_send(fd, "UID");
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_space_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  return mailimap_sort_send(fd, charset, key, searchkey);
}

int mailimap_sort_key_send(mailstream * fd,
                                  struct mailimap_sort_key * key)
{
  int r;
  
  if (key->sortk_is_reverse) {
    r = mailimap_token_send(fd, "REVERSE");
    if (r != MAILIMAP_NO_ERROR)
      return r;
    r = mailimap_space_send(fd);
    if (r != MAILIMAP_NO_ERROR)
      return r;
  }
  
  switch (key->sortk_type) {
      
    case MAILIMAP_SORT_KEY_ARRIVAL:
      return mailimap_token_send(fd, "ARRIVAL");
    case MAILIMAP_SORT_KEY_CC:
      return mailimap_token_send(fd, "CC");
    case MAILIMAP_SORT_KEY_DATE:
      return mailimap_token_send(fd, "DATE");
    case MAILIMAP_SORT_KEY_FROM:
      return mailimap_token_send(fd, "FROM");
    case MAILIMAP_SORT_KEY_SIZE:
      return mailimap_token_send(fd, "SIZE");
    case MAILIMAP_SORT_KEY_SUBJECT:
      return mailimap_token_send(fd, "SUBJECT");
    case MAILIMAP_SORT_KEY_TO:
      return mailimap_token_send(fd, "TO");
      
    case MAILIMAP_SORT_KEY_MULTIPLE:
      r = mailimap_struct_spaced_list_send(fd, key->sortk_multiple,
                                           (mailimap_struct_sender *)
                                           mailimap_sort_key_send);
      
      return MAILIMAP_NO_ERROR;
    default:
      /* should not happend */
      return MAILIMAP_ERROR_INVAL;
  }
}

static int
mailimap_number_list_data_sort_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
                                     size_t * indx,
                                     clist ** result,
                                     size_t progr_rate,
                                     progress_function * progr_fun)
{
  size_t cur_token;
  clist * number_list;
  int r;
  size_t final_token;
  
  cur_token = * indx;
  
  r = mailimap_token_case_insensitive_parse(fd, buffer, &cur_token, "SORT");
  if (r != MAILIMAP_NO_ERROR) {
    return r;
  }
  
  final_token = cur_token;
  number_list = NULL;
  
  r = mailimap_space_parse(fd, buffer, &cur_token);
  if (r == MAILIMAP_NO_ERROR) {
    r = mailimap_struct_spaced_list_parse(fd, buffer, parser_ctx, &cur_token, &number_list,
                                          (mailimap_struct_parser *)
                                          mailimap_nz_number_alloc_parse,
                                          (mailimap_struct_destructor *)
                                          mailimap_number_alloc_free,
                                          progr_rate, progr_fun);
    if (r == MAILIMAP_NO_ERROR) {
      final_token = cur_token;
    }
  }
  
  * result = number_list;
  * indx = final_token;
  
  return MAILIMAP_NO_ERROR;
}

static int
mailimap_sort_extension_parse(int calling_parser, mailstream * fd,
                              MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
                              struct mailimap_extension_data ** result,
                              size_t progr_rate, progress_function * progr_fun)
{
  int r;
  clist * number_list = NULL;
  struct mailimap_extension_data * ext_data;
  void * data = NULL;
  size_t cur_token;
  
  cur_token = * indx;
  
  switch (calling_parser)
  {
    case MAILIMAP_EXTENDED_PARSER_RESPONSE_DATA:
    case MAILIMAP_EXTENDED_PARSER_MAILBOX_DATA:
      r = mailimap_number_list_data_sort_parse(fd, buffer, NULL, &cur_token,
                                               &number_list, progr_rate, progr_fun);
      if (r == MAILIMAP_NO_ERROR) {
        data = number_list;
      }
      
      if (r != MAILIMAP_NO_ERROR) {
        return r;
      }
      
      ext_data = mailimap_extension_data_new(&mailimap_extension_sort,
                                             MAILIMAP_SORT_TYPE_SORT, data);
      if (ext_data == NULL) {
        if (number_list != NULL)
          mailimap_mailbox_data_search_free(number_list);
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

static void
mailimap_sort_extension_data_free(struct mailimap_extension_data * ext_data)
{
  if (ext_data->ext_data != NULL) {
    mailimap_mailbox_data_search_free((clist *) ext_data->ext_data);
  }
  free(ext_data);
}

