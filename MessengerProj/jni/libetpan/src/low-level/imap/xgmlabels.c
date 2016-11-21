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

#include "xgmlabels.h"

#include <string.h>
#include <stdlib.h>
#include <stdio.h>

#include "clist.h"
#include "mailimap_types_helper.h"
#include "mailimap_extension.h"
#include "mailimap_keywords.h"
#include "mailimap_parser.h"
#include "mailimap_sender.h"
#include "mailimap.h"

struct mailimap_fetch_att * mailimap_fetch_att_new_xgmlabels(void)
{
  char * keyword;
  struct mailimap_fetch_att * att;
  
  keyword = strdup("X-GM-LABELS");
  if (keyword == NULL)
    return NULL;
  
  att = mailimap_fetch_att_new_extension(keyword);
  if (att == NULL) {
    free(keyword);
    return NULL;
  }
  
  return att;
}

int mailimap_has_xgmlabels(mailimap * session)
{
  return mailimap_has_extension(session, "X-GM-EXT-1");
}

struct mailimap_msg_att_xgmlabels * mailimap_msg_att_xgmlabels_new(clist * att_labels)
{
  struct mailimap_msg_att_xgmlabels * att;
  
  att = malloc(sizeof(* att));
  if (att == NULL)
    return NULL;
  
  att->att_labels = att_labels;
  
  return att;
}

void mailimap_msg_att_xgmlabels_free(struct mailimap_msg_att_xgmlabels * att)
{
  clist_foreach(att->att_labels, (clist_func) mailimap_astring_free, NULL);
  clist_free(att->att_labels);
  free(att);
}

struct mailimap_msg_att_xgmlabels * mailimap_msg_att_xgmlabels_new_empty(void)
{
  clist * list;
  struct mailimap_msg_att_xgmlabels * att;
  
  list = clist_new();
  if (list == NULL)
    return NULL;
  
  att = mailimap_msg_att_xgmlabels_new(list);
  if (att == NULL) {
    clist_free(list);
    free(att);
    return NULL;
  }
  
  return att;
}

int mailimap_msg_att_xgmlabels_add(struct mailimap_msg_att_xgmlabels * att, char * label)
{
  return clist_append(att->att_labels, label);
}

enum {
  MAILIMAP_XGMLABELS_TYPE_XGMLABELS
};

/* extension data structure */

static int
mailimap_xgmlabels_extension_parse(int calling_parser, mailstream * fd,
                                   MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
                                   struct mailimap_extension_data ** result,
                                   size_t progr_rate, progress_function * progr_fun);

static void
mailimap_xgmlabels_extension_data_free(struct mailimap_extension_data * ext_data);

LIBETPAN_EXPORT
struct mailimap_extension_api mailimap_extension_xgmlabels = {
  /* name */          "X-GM-lABELS",
  /* extension_id */  MAILIMAP_EXTENSION_XGMLABELS,
  /* parser */        mailimap_xgmlabels_extension_parse,
  /* free */          mailimap_xgmlabels_extension_data_free
};

static int mailimap_xgmlabels_parse(mailstream * fd,
                                    MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
                                    clist ** result)
{
    size_t cur_token;
    clist * list;
    int r;
    int res;
    
    cur_token = * indx;
    
    r = mailimap_oparenth_parse(fd, buffer, parser_ctx, &cur_token);
    if (r != MAILIMAP_NO_ERROR) {
      res = r;
      goto err;
    }
    
    r = mailimap_struct_spaced_list_parse(fd, buffer, parser_ctx,
                                          &cur_token, &list,
                                          (mailimap_struct_parser * ) mailimap_astring_parse,
                                          (mailimap_struct_destructor * ) mailimap_astring_free,
                                          0, NULL);
    if (r == MAILIMAP_ERROR_PARSE) {
      list = clist_new();
      if (list == NULL) {
        res = MAILIMAP_ERROR_MEMORY;
        goto err;
      }
    }
    else if (r != MAILIMAP_NO_ERROR) {
      res = r;
      goto err;
    }
    
    r = mailimap_cparenth_parse(fd, buffer, parser_ctx, &cur_token);
    if (r != MAILIMAP_NO_ERROR) {
      res = r;
      goto free_list;
    }
    
    * indx = cur_token;
    * result = list;
    
    return MAILIMAP_NO_ERROR;
    
free_list:
    clist_foreach(list, (clist_func) mailimap_astring_free, NULL);
    clist_free(list);
err:
    return res;
}

static int fetch_data_xgmlabels_parse(mailstream * fd,
                                      MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
                                      struct mailimap_msg_att_xgmlabels ** result)
{
  size_t cur_token;
  struct mailimap_msg_att_xgmlabels * att;
  clist * label_list;
  int r;
  
  cur_token = * indx;
  
  r = mailimap_token_case_insensitive_parse(fd, buffer,
                                            &cur_token, "X-GM-LABELS");
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_space_parse(fd, buffer, &cur_token);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_xgmlabels_parse(fd, buffer, parser_ctx, &cur_token, &label_list);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  att = mailimap_msg_att_xgmlabels_new(label_list);
  if (att == NULL) {
    clist_foreach(label_list, (clist_func) mailimap_astring_free, NULL);
    clist_free(label_list);
    return MAILIMAP_ERROR_MEMORY;
  }
  
  * indx = cur_token;
  * result = att;
  
  return MAILIMAP_NO_ERROR;
}

static int
mailimap_xgmlabels_extension_parse(int calling_parser, mailstream * fd,
                                   MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
                                   struct mailimap_extension_data ** result,
                                   size_t progr_rate, progress_function * progr_fun)
{
  size_t cur_token;
  int type;
  struct mailimap_msg_att_xgmlabels * att;
  void * data;
  struct mailimap_extension_data * ext_data;
  int r;
  
  cur_token = * indx;
  
  switch (calling_parser)
  {
    case MAILIMAP_EXTENDED_PARSER_FETCH_DATA:
      att = NULL;
      r = fetch_data_xgmlabels_parse(fd, buffer, parser_ctx, &cur_token, &att);
      if (r != MAILIMAP_NO_ERROR)
        return r;
      
      type = MAILIMAP_XGMLABELS_TYPE_XGMLABELS;
      data = att;
      
      ext_data = mailimap_extension_data_new(&mailimap_extension_xgmlabels,
                                             type, data);
      if (ext_data == NULL) {
        if (att != NULL)
          mailimap_msg_att_xgmlabels_free(att);
        return MAILIMAP_ERROR_MEMORY;
      }
      
      * result = ext_data;
      * indx = cur_token;
      
      return MAILIMAP_NO_ERROR;
    
    default:
      return MAILIMAP_ERROR_PARSE;
  }
}

static void
mailimap_xgmlabels_extension_data_free(struct mailimap_extension_data * ext_data)
{
  if (ext_data->ext_data != NULL) {
    if (ext_data->ext_type == MAILIMAP_XGMLABELS_TYPE_XGMLABELS) {
      mailimap_msg_att_xgmlabels_free((struct mailimap_msg_att_xgmlabels *) ext_data->ext_data);
    }
  }
  free(ext_data);
}

static int mailimap_msg_att_xgmlabels_send(mailstream * fd, struct mailimap_msg_att_xgmlabels * labels)
{
  int r;
  
  r = mailimap_oparenth_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  if (labels->att_labels != NULL) {
    r = mailimap_struct_spaced_list_send(fd, labels->att_labels,
                                         (mailimap_struct_sender *) mailimap_astring_send);
    if (r != MAILIMAP_NO_ERROR)
      return r;
  }
  
  r = mailimap_cparenth_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  return MAILIMAP_NO_ERROR;
}

static int mailimap_store_xgmlabels_send(mailstream * fd, struct mailimap_set * set,
                                         int fl_sign, int fl_silent,
                                         struct mailimap_msg_att_xgmlabels * labels)
{
  int r;
  
  r = mailimap_token_send(fd, "STORE");
  if (r != MAILIMAP_NO_ERROR)
    return r;
  r = mailimap_space_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  r = mailimap_set_send(fd, set);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  r = mailimap_space_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  switch (fl_sign) {
    case 1:
      r = mailimap_char_send(fd, '+');
      if (r != MAILIMAP_NO_ERROR)
        return r;
      break;
    case -1:
      r = mailimap_char_send(fd, '-');
      if (r != MAILIMAP_NO_ERROR)
        return r;
      break;
  }
  
  r = mailimap_token_send(fd, "X-GM-LABELS");
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  if (fl_silent) {
    r = mailimap_token_send(fd, ".SILENT");
    if (r != MAILIMAP_NO_ERROR)
      return r;
  }
  
  r = mailimap_space_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_msg_att_xgmlabels_send(fd, labels);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  return MAILIMAP_NO_ERROR;
}

static int mailimap_uid_store_xgmlabels_send(mailstream * fd, struct mailimap_set * set,
                                             int fl_sign, int fl_silent,
                                             struct mailimap_msg_att_xgmlabels * labels)
{
  int r;
  
  r = mailimap_token_send(fd, "UID");
  if (r != MAILIMAP_NO_ERROR)
    return r;
  r = mailimap_space_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  return mailimap_store_xgmlabels_send(fd, set, fl_sign, fl_silent, labels);
}

int
mailimap_store_xgmlabels(mailimap * session,
                         struct mailimap_set * set,
                         int fl_sign, int fl_silent,
                         struct mailimap_msg_att_xgmlabels * labels)
{
  struct mailimap_response * response;
  int r;
  int error_code;
  
  if (session->imap_state != MAILIMAP_STATE_SELECTED)
    return MAILIMAP_ERROR_BAD_STATE;
  
  r = mailimap_send_current_tag(session);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_store_xgmlabels_send(session->imap_stream, set, fl_sign, fl_silent, labels);
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
  
  error_code = response->rsp_resp_done->rsp_data.rsp_tagged->rsp_cond_state->rsp_type;
  
  mailimap_response_free(response);
  
  switch (error_code) {
    case MAILIMAP_RESP_COND_STATE_OK:
      return MAILIMAP_NO_ERROR;
      
    default:
      return MAILIMAP_ERROR_STORE;
  }
}

int
mailimap_uid_store_xgmlabels(mailimap * session,
                             struct mailimap_set * set,
                             int fl_sign, int fl_silent,
                             struct mailimap_msg_att_xgmlabels * labels)
{
  struct mailimap_response * response;
  int r;
  int error_code;
  
  if (session->imap_state != MAILIMAP_STATE_SELECTED)
    return MAILIMAP_ERROR_BAD_STATE;
  
  r = mailimap_send_current_tag(session);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_uid_store_xgmlabels_send(session->imap_stream, set, fl_sign, fl_silent, labels);
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
  
  error_code = response->rsp_resp_done->rsp_data.rsp_tagged->rsp_cond_state->rsp_type;
  
  mailimap_response_free(response);
  
  switch (error_code) {
    case MAILIMAP_RESP_COND_STATE_OK:
      return MAILIMAP_NO_ERROR;
      
    default:
      return MAILIMAP_ERROR_UID_STORE;
  }
}
