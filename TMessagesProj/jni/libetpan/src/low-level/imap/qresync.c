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

#include "qresync.h"
#include "qresync_private.h"

#include <string.h>
#include <stdlib.h>
#include <stdio.h>

#include "mailimap_sender.h"
#include "mailimap.h"
#include "condstore.h"
#include "condstore_private.h"
#include "mailimap_keywords.h"
#include "mailimap_parser.h"

/*
capability          =/ "QRESYNC"

select-param        =  "QRESYNC" SP "(" uidvalidity SP
mod-sequence-value [SP known-uids]
[SP seq-match-data] ")"
;; conforms to the generic select-param
;; syntax defined in [IMAPABNF]

seq-match-data      =  "(" known-sequence-set SP known-uid-set ")"

uidvalidity         =  nz-number

known-uids          =  sequence-set
;; sequence of UIDs, "*" is not allowed

known-sequence-set  =  sequence-set
;; set of message numbers corresponding to
;; the UIDs in known-uid-set, in ascending order.
;; * is not allowed.

known-uid-set       =  sequence-set
;; set of UIDs corresponding to the messages in
;; known-sequence-set, in ascending order.
;; * is not allowed.

message-data        =/ expunged-resp

expunged-resp       =  "VANISHED" [SP "(EARLIER)"] SP known-uids

rexpunges-fetch-mod =  "VANISHED"
;; VANISHED UID FETCH modifier conforms
;; to the fetch-modifier syntax
;; defined in [IMAPABNF].  It is only
;; allowed in the UID FETCH command.

resp-text-code      =/ "CLOSED"
*/

static void
	mailimap_qresync_extension_data_free(struct mailimap_extension_data * ext_data);

static int
	mailimap_qresync_extension_parse(int calling_parser, mailstream * fd,
	MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
	struct mailimap_extension_data ** result,
	size_t progr_rate, progress_function * progr_fun);

struct mailimap_extension_api mailimap_extension_qresync = {
  /* name */          "QRESYNC",
  /* extension_id */  MAILIMAP_EXTENSION_QRESYNC,
  /* parser */        mailimap_qresync_extension_parse,
  /* free */          mailimap_qresync_extension_data_free
};

int mailimap_select_qresync_send(mailstream * fd, const char * mb,
  uint32_t uidvalidity, uint64_t modseq_value,
  struct mailimap_set * known_uids,
  struct mailimap_set * seq_match_data_sequences,
  struct mailimap_set * seq_match_data_uids)
{
  int r;
  
  /*
  select-param        =  "QRESYNC" SP "(" uidvalidity SP
  mod-sequence-value [SP known-uids]
  [SP seq-match-data] ")"
  */
  
  r = mailimap_token_send(fd, "SELECT");
  if (r != MAILIMAP_NO_ERROR)
    return r;
  r = mailimap_space_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  r = mailimap_mailbox_send(fd, mb);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  r = mailimap_space_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
	r = mailimap_token_send(fd, "QRESYNC");
	if (r != MAILIMAP_NO_ERROR)
		return r;
  r = mailimap_space_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
	r = mailimap_oparenth_send(fd);
	if (r != MAILIMAP_NO_ERROR)
		return r;
  r = mailimap_number_send(fd, uidvalidity);
	if (r != MAILIMAP_NO_ERROR)
		return r;
  r = mailimap_space_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  r = mailimap_mod_sequence_value_send(fd, modseq_value);
	if (r != MAILIMAP_NO_ERROR)
		return r;
	if (known_uids != NULL) {
    r = mailimap_space_send(fd);
    if (r != MAILIMAP_NO_ERROR)
      return r;
    r = mailimap_set_send(fd, known_uids);
    if (r != MAILIMAP_NO_ERROR)
      return r;
  }
  if ((seq_match_data_sequences != NULL) && (seq_match_data_uids != NULL)) {
    r = mailimap_space_send(fd);
    if (r != MAILIMAP_NO_ERROR)
      return r;
    /* seq-match-data      =  "(" known-sequence-set SP known-uid-set ")" */
  	r = mailimap_oparenth_send(fd);
  	if (r != MAILIMAP_NO_ERROR)
  		return r;
    r = mailimap_set_send(fd, seq_match_data_sequences);
    if (r != MAILIMAP_NO_ERROR)
      return r;
    r = mailimap_space_send(fd);
    if (r != MAILIMAP_NO_ERROR)
      return r;
    r = mailimap_set_send(fd, seq_match_data_uids);
    if (r != MAILIMAP_NO_ERROR)
      return r;
  	r = mailimap_cparenth_send(fd);
  	if (r != MAILIMAP_NO_ERROR)
  		return r;
  }
	r = mailimap_cparenth_send(fd);
	if (r != MAILIMAP_NO_ERROR)
		return r;
	
  return MAILIMAP_NO_ERROR;
}

static struct mailimap_qresync_vanished * get_vanished(mailimap * session)
{
  struct mailimap_qresync_vanished * vanished;
  clistiter * cur;

  vanished = NULL;
  for(cur = clist_begin(session->imap_response_info->rsp_extension_list) ; cur != NULL ; cur = clist_next(cur)) {
    struct mailimap_extension_data * ext_data;

    ext_data = clist_content(cur);
    if (ext_data->ext_extension->ext_id != MAILIMAP_EXTENSION_QRESYNC) {
      continue;
    }
    if (ext_data->ext_type != MAILIMAP_QRESYNC_TYPE_VANISHED) {
      continue;
    }
    
    vanished = ext_data->ext_data;
    ext_data->ext_data = NULL;
    break;
  }
  
  return vanished;
}

static uint64_t get_mod_sequence_value(mailimap * session)
{
  uint64_t mod_sequence_value;
  clistiter * cur;
  
  mod_sequence_value = 0;
  for(cur = clist_begin(session->imap_response_info->rsp_extension_list) ; cur != NULL ; cur = clist_next(cur)) {
    struct mailimap_extension_data * ext_data;
    struct mailimap_condstore_resptextcode * resptextcode;
    
    ext_data = clist_content(cur);
    if (ext_data->ext_extension->ext_id != MAILIMAP_EXTENSION_CONDSTORE) {
      continue;
    }
    if (ext_data->ext_type != MAILIMAP_CONDSTORE_TYPE_RESP_TEXT_CODE) {
      continue;
    }
    
    resptextcode = ext_data->ext_data;
    switch (resptextcode->cs_type) {
      case MAILIMAP_CONDSTORE_RESPTEXTCODE_HIGHESTMODSEQ:
      mod_sequence_value = resptextcode->cs_data.cs_modseq_value;
      break;
      case MAILIMAP_CONDSTORE_RESPTEXTCODE_NOMODSEQ:
      mod_sequence_value = 0;
      break;
    }
  }
  
  return mod_sequence_value;
}

int mailimap_select_qresync(mailimap * session, const char * mb,
  uint32_t uidvalidity, uint64_t modseq_value,
  struct mailimap_set * known_uids,
  struct mailimap_set * seq_match_data_sequences,
  struct mailimap_set * seq_match_data_uids,
  clist ** fetch_result, struct mailimap_qresync_vanished ** p_vanished,
  uint64_t * p_mod_sequence_value)
{
  struct mailimap_response * response;
  int r;
  int error_code;
  uint64_t mod_sequence_value;

  if ((session->imap_state != MAILIMAP_STATE_AUTHENTICATED) &&
      (session->imap_state != MAILIMAP_STATE_SELECTED))
    return MAILIMAP_ERROR_BAD_STATE;

  r = mailimap_send_current_tag(session);
  if (r != MAILIMAP_NO_ERROR)
    return r;

  r = mailimap_select_qresync_send(session->imap_stream, mb, uidvalidity, modseq_value,
    known_uids, seq_match_data_sequences, seq_match_data_uids);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_crlf_send(session->imap_stream);
  if (r != MAILIMAP_NO_ERROR)
    return r;

  if (mailstream_flush(session->imap_stream) == -1)
    return MAILIMAP_ERROR_STREAM;

  if (mailimap_read_line(session) == NULL)
    return MAILIMAP_ERROR_STREAM;

  if (session->imap_selection_info != NULL)
    mailimap_selection_info_free(session->imap_selection_info);
  session->imap_selection_info = mailimap_selection_info_new();

  r = mailimap_parse_response(session, &response);
  if (r != MAILIMAP_NO_ERROR)
    return r;

  * fetch_result = session->imap_response_info->rsp_fetch_list;
  session->imap_response_info->rsp_fetch_list = NULL;
  if (p_vanished != NULL) {
    * p_vanished = get_vanished(session);
  }

  mod_sequence_value = get_mod_sequence_value(session);
  error_code = response->rsp_resp_done->rsp_data.rsp_tagged->rsp_cond_state->rsp_type;

  mailimap_response_free(response);

  switch (error_code) {
  case MAILIMAP_RESP_COND_STATE_OK:
    session->imap_state = MAILIMAP_STATE_SELECTED;
    * p_mod_sequence_value = mod_sequence_value;
    return MAILIMAP_NO_ERROR;

  default:
    mailimap_selection_info_free(session->imap_selection_info);
    session->imap_selection_info = NULL;
    session->imap_state = MAILIMAP_STATE_AUTHENTICATED;
    * p_mod_sequence_value = mod_sequence_value;
    if (* fetch_result != NULL) {
      mailimap_fetch_list_free(* fetch_result);
    }
    if (p_vanished != NULL) {
      if (* p_vanished != NULL) {
        mailimap_qresync_vanished_free(* p_vanished);
      }
    }
    return MAILIMAP_ERROR_SELECT;
  }
}

int mailimap_fetch_qresync(mailimap * session,
                           struct mailimap_set * set,
                           struct mailimap_fetch_type * fetch_type, uint64_t mod_sequence_value,
                           clist ** fetch_result, struct mailimap_qresync_vanished ** p_vanished)
{
  return mailimap_fetch_qresync_vanished(session, set, fetch_type, mod_sequence_value, 1,
                                         fetch_result, p_vanished);
}

static int send_fetch_param(mailstream * fd, uint64_t mod_sequence_value, int vanished)
{
  int r;
  
  if (mod_sequence_value == 0)
    return MAILIMAP_NO_ERROR;
  
  r = mailimap_space_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_oparenth_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_token_send(fd, "CHANGEDSINCE");
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_space_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_mod_sequence_value_send(fd, mod_sequence_value);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  if (vanished) {
    r = mailimap_space_send(fd);
    if (r != MAILIMAP_NO_ERROR)
      return r;
    r = mailimap_token_send(fd, "VANISHED");
    if (r != MAILIMAP_NO_ERROR)
      return r;
  }
  
  r = mailimap_cparenth_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  return MAILIMAP_NO_ERROR;
}

int mailimap_fetch_qresync_vanished(mailimap * session,
  struct mailimap_set * set,
  struct mailimap_fetch_type * fetch_type, uint64_t mod_sequence_value, int vanished,
  clist ** fetch_result, struct mailimap_qresync_vanished ** p_vanished)
{
  struct mailimap_response * response;
  int r;
  int error_code;

  if (session->imap_state != MAILIMAP_STATE_SELECTED)
    return MAILIMAP_ERROR_BAD_STATE;

  r = mailimap_send_current_tag(session);
  if (r != MAILIMAP_NO_ERROR)
    return r;

  r = mailimap_fetch_send(session->imap_stream, set, fetch_type);
  if (r != MAILIMAP_NO_ERROR)
    return r;

  r = send_fetch_param(session->imap_stream, mod_sequence_value, vanished);
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

  * fetch_result = session->imap_response_info->rsp_fetch_list;
  session->imap_response_info->rsp_fetch_list = NULL;
  if (p_vanished != NULL) {
    * p_vanished = get_vanished(session);
  }

  if (clist_count(* fetch_result) == 0) {
    error_code = response->rsp_resp_done->rsp_data.rsp_tagged->rsp_cond_state->rsp_type;
  }
  else {
    error_code = MAILIMAP_RESP_COND_STATE_OK;
  }

  mailimap_response_free(response);

  switch (error_code) {
  case MAILIMAP_RESP_COND_STATE_OK:
    return MAILIMAP_NO_ERROR;

  default:
    if (* fetch_result != NULL) {
      mailimap_fetch_list_free(* fetch_result);
    }
    if (p_vanished != NULL && * p_vanished != NULL) {
      mailimap_qresync_vanished_free(* p_vanished);
    }
    return MAILIMAP_ERROR_FETCH;
  }
}

int mailimap_uid_fetch_qresync(mailimap * session,
                               struct mailimap_set * set,
                               struct mailimap_fetch_type * fetch_type, uint64_t mod_sequence_value,
                               clist ** fetch_result, struct mailimap_qresync_vanished ** p_vanished)
{
  return mailimap_uid_fetch_qresync_vanished(session, set, fetch_type, mod_sequence_value, 1,
                                             fetch_result, p_vanished);
}

int mailimap_uid_fetch_qresync_vanished(mailimap * session,
  struct mailimap_set * set,
  struct mailimap_fetch_type * fetch_type, uint64_t mod_sequence_value, int vanished,
  clist ** fetch_result, struct mailimap_qresync_vanished ** p_vanished)
{
  struct mailimap_response * response;
  int r;
  int error_code;

  if (session->imap_state != MAILIMAP_STATE_SELECTED)
    return MAILIMAP_ERROR_BAD_STATE;

  r = mailimap_send_current_tag(session);
  if (r != MAILIMAP_NO_ERROR)
    return r;

  r = mailimap_uid_fetch_send(session->imap_stream, set, fetch_type);
  if (r != MAILIMAP_NO_ERROR)
    return r;

  r = send_fetch_param(session->imap_stream, mod_sequence_value, vanished);
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
  if (r != MAILIMAP_NO_ERROR) {
    return r;
  }
  
  * fetch_result = session->imap_response_info->rsp_fetch_list;
  session->imap_response_info->rsp_fetch_list = NULL;
  if (p_vanished != NULL) {
    * p_vanished = get_vanished(session);
  }

  if (clist_count(* fetch_result) == 0) {
    error_code = response->rsp_resp_done->rsp_data.rsp_tagged->rsp_cond_state->rsp_type;
  }
  else {
    error_code = MAILIMAP_RESP_COND_STATE_OK;
  }
  
  mailimap_response_free(response);

  switch (error_code) {
  case MAILIMAP_RESP_COND_STATE_OK:
    return MAILIMAP_NO_ERROR;

  default:
    if (* fetch_result != NULL) {
      mailimap_fetch_list_free(* fetch_result);
    }
    if (p_vanished != NULL) {
      if (* p_vanished != NULL) {
        mailimap_qresync_vanished_free(* p_vanished);
      }
    }
    return MAILIMAP_ERROR_UID_FETCH;
  }
}

int mailimap_has_qresync(mailimap * session)
{
  return mailimap_has_extension(session, "QRESYNC");
}

static void
	mailimap_qresync_extension_data_free(struct mailimap_extension_data * ext_data)
{
  switch (ext_data->ext_type) {
    case MAILIMAP_QRESYNC_TYPE_VANISHED:
      if (ext_data->ext_data != NULL) {
        mailimap_qresync_vanished_free(ext_data->ext_data);
      }
      break;
    case MAILIMAP_QRESYNC_TYPE_RESP_TEXT_CODE:
      if (ext_data->ext_data != NULL) {
        mailimap_qresync_resptextcode_free(ext_data->ext_data);
      }
      break;
  }

  free(ext_data);
}

static int resp_text_code_parse(mailstream * fd,
  MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
  struct mailimap_qresync_resptextcode ** result);

static int vanished_parse(mailstream * fd,
  MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
  struct mailimap_qresync_vanished ** result);

static int
	mailimap_qresync_extension_parse(int calling_parser, mailstream * fd,
	MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
	struct mailimap_extension_data ** result,
	size_t progr_rate, progress_function * progr_fun)
{
  size_t cur_token;
  int r;
  
  cur_token = * indx;
  
  switch (calling_parser) {
    case MAILIMAP_EXTENDED_PARSER_RESP_TEXT_CODE: {
      struct mailimap_qresync_resptextcode * resptextcode;
      struct mailimap_extension_data * ext_data;
      
      r = resp_text_code_parse(fd, buffer, parser_ctx, &cur_token, &resptextcode);
      if (r != MAILIMAP_NO_ERROR)
        return r;
      ext_data = mailimap_extension_data_new(&mailimap_extension_qresync,
        MAILIMAP_QRESYNC_TYPE_RESP_TEXT_CODE, resptextcode);
      if (ext_data == NULL) {
        mailimap_qresync_resptextcode_free(resptextcode);
        return MAILIMAP_ERROR_MEMORY;
      }
      * indx = cur_token;
      * result = ext_data;
      return MAILIMAP_NO_ERROR;
    }
      
    case MAILIMAP_EXTENDED_PARSER_RESPONSE_DATA: {
      struct mailimap_qresync_vanished * vanished;
      struct mailimap_extension_data * ext_data;
      
      r = vanished_parse(fd, buffer, parser_ctx, &cur_token, &vanished);
      if (r != MAILIMAP_NO_ERROR)
        return r;
      ext_data = mailimap_extension_data_new(&mailimap_extension_qresync,
        MAILIMAP_QRESYNC_TYPE_VANISHED, vanished);
      if (ext_data == NULL) {
        mailimap_qresync_vanished_free(vanished);
        return MAILIMAP_ERROR_MEMORY;
      }
      * indx = cur_token;
      * result = ext_data;
      return MAILIMAP_NO_ERROR;
    }
  }
  
  return MAILIMAP_ERROR_PARSE;
}

static int resp_text_code_parse(mailstream * fd,
  MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
  struct mailimap_qresync_resptextcode ** result)
{
  int r;
  size_t cur_token;
  
  cur_token = * indx;
  
  r = mailimap_token_case_insensitive_parse(fd, buffer, &cur_token, "CLOSED");
  if (r == MAILIMAP_NO_ERROR) {
    struct mailimap_qresync_resptextcode * resptextcode;
    
    resptextcode = mailimap_qresync_resptextcode_new(MAILIMAP_QRESYNC_RESPTEXTCODE_CLOSED);
    if (resptextcode == NULL)
      return MAILIMAP_ERROR_MEMORY;
    
    * indx = cur_token;
    * result = resptextcode;
    
    return MAILIMAP_NO_ERROR;
  }
  
  return MAILIMAP_ERROR_PARSE;
}

/*
expunged-resp       =  "VANISHED" [SP "(EARLIER)"] SP known-uids
*/

static int vanished_parse(mailstream * fd,
  MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
  struct mailimap_qresync_vanished ** result)
{
  int r;
  struct mailimap_set * set;
  int earlier;
  struct mailimap_qresync_vanished * vanished;
  size_t cur_token;
  int res;
  
  cur_token = * indx;
  
  r = mailimap_token_case_insensitive_parse(fd, buffer, &cur_token, "VANISHED");
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }
  
  r = mailimap_space_parse(fd, buffer, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }
  
  earlier = 0;
  r = mailimap_token_case_insensitive_parse(fd, buffer, &cur_token, "(EARLIER)");
  if (r == MAILIMAP_NO_ERROR) {
    earlier = 1;
    
    r = mailimap_space_parse(fd, buffer, &cur_token);
    if (r != MAILIMAP_NO_ERROR) {
      res = r;
      goto err;
    }
  }
  
  r = mailimap_set_parse(fd, buffer, parser_ctx, &cur_token, &set);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }
  
  vanished = mailimap_qresync_vanished_new(earlier, set);
  if (vanished == NULL) {
    res = MAILIMAP_ERROR_MEMORY;
    goto free_set;
  }
  
  * indx = cur_token;
  * result = vanished;
  
  return MAILIMAP_NO_ERROR;
  
  free_set:
  mailimap_set_free(set);
  err:
  return res;
}
