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

#include "condstore.h"
#include "condstore_private.h"

#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#include "mailimap_sender.h"
#include "mailimap.h"
#include "condstore_types.h"
#include "mailimap_keywords.h"
#include "mailimap_parser.h"
#include "qresync.h"
#include "qresync_private.h"

/*
   capability          =/ "CONDSTORE"

   status-att          =/ "HIGHESTMODSEQ"
                          ;; extends non-terminal defined in RFC 3501.

   status-att-val      =/ "HIGHESTMODSEQ" SP mod-sequence-valzer
                          ;; extends non-terminal defined in [IMAPABNF].
                          ;; Value 0 denotes that the mailbox doesn't
                          ;; support persistent mod-sequences
                          ;; as described in Section 3.1.2

   store-modifier      =/ "UNCHANGEDSINCE" SP mod-sequence-valzer
                          ;; Only a single "UNCHANGEDSINCE" may be
                          ;; specified in a STORE operation

   fetch-modifier      =/ chgsince-fetch-mod
                          ;; conforms to the generic "fetch-modifier"
                          ;; syntax defined in [IMAPABNF].

   chgsince-fetch-mod  = "CHANGEDSINCE" SP mod-sequence-value
                          ;; CHANGEDSINCE FETCH modifier conforms to
                          ;; the fetch-modifier syntax

   fetch-att           =/ fetch-mod-sequence
                          ;; modifies original IMAP4 fetch-att

   fetch-mod-sequence  = "MODSEQ"

   fetch-mod-resp      = "MODSEQ" SP "(" permsg-modsequence ")"

   msg-att-dynamic     =/ fetch-mod-resp

   search-key          =/ search-modsequence
                          ;; modifies original IMAP4 search-key
                          ;;
                          ;; This change applies to all commands
                          ;; referencing this non-terminal, in
                          ;; particular SEARCH.

   search-modsequence  = "MODSEQ" [search-modseq-ext] SP
                         mod-sequence-valzer

   search-modseq-ext   = SP entry-name SP entry-type-req

   resp-text-code      =/ "HIGHESTMODSEQ" SP mod-sequence-value /
                          "NOMODSEQ" /
                          "MODIFIED" SP set

   entry-name          = entry-flag-name

   entry-flag-name     = DQUOTE "/flags/" attr-flag DQUOTE
                          ;; each system or user defined flag <flag>
                          ;; is mapped to "/flags/<flag>".
                          ;;
                          ;; <entry-flag-name> follows the escape rules
                          ;; used by "quoted" string as described in
                          ;; Section 4.3 of [IMAP4], e.g., for the flag
                          ;; \Seen the corresponding <entry-name> is
                          ;; "/flags/\\seen", and for the flag
                          ;; $MDNSent, the corresponding <entry-name>
                          ;; is "/flags/$mdnsent".

   entry-type-resp     = "priv" / "shared"
                          ;; metadata item type

   entry-type-req      = entry-type-resp / "all"
                          ;; perform SEARCH operation on private
                          ;; metadata item, shared metadata item or both

   permsg-modsequence  = mod-sequence-value
                          ;; per message mod-sequence

   mod-sequence-value  = 1*DIGIT
                          ;; Positive unsigned 64-bit integer
                          ;; (mod-sequence)
                          ;; (1 <= n < 18,446,744,073,709,551,615)

   mod-sequence-valzer = "0" / mod-sequence-value

   search-sort-mod-seq = "(" "MODSEQ" SP mod-sequence-value ")"

   select-param        =/ condstore-param
                          ;; conforms to the generic "select-param"
                          ;; non-terminal syntax defined in [IMAPABNF].

   condstore-param     = "CONDSTORE"

   mailbox-data        =/ "SEARCH" [1*(SP nz-number) SP
                          search-sort-mod-seq]

   attr-flag           = "\\Answered" / "\\Flagged" / "\\Deleted" /
                         "\\Seen" / "\\Draft" / attr-flag-keyword /
                         attr-flag-extension
                          ;; Does not include "\\Recent"

   attr-flag-extension = "\\" atom
                          ;; Future expansion.  Client implementations
                          ;; MUST accept flag-extension flags.  Server
                          ;; implementations MUST NOT generate
                          ;; flag-extension flags except as defined by
                          ;; future standard or standards-track
                          ;; revisions of [IMAP4].

   attr-flag-keyword   = atom
*/

static void
	mailimap_condstore_extension_data_free(struct mailimap_extension_data * ext_data);

static int
	mailimap_condstore_extension_parse(int calling_parser, mailstream * fd,
	MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
	struct mailimap_extension_data ** result,
	size_t progr_rate, progress_function * progr_fun);

struct mailimap_extension_api mailimap_extension_condstore = {
  /* name */          "CONDSTORE",
  /* extension_id */  MAILIMAP_EXTENSION_CONDSTORE,
  /* parser */        mailimap_condstore_extension_parse,
  /* free */          mailimap_condstore_extension_data_free
};

int mailimap_store_unchangedsince_optional(mailimap * session,
	struct mailimap_set * set, int use_unchangedsince, uint64_t mod_sequence_valzer,
	struct mailimap_store_att_flags * store_att_flags)
{
  struct mailimap_response * response;
  int r;
  int error_code;

  if (session->imap_state != MAILIMAP_STATE_SELECTED)
    return MAILIMAP_ERROR_BAD_STATE;

  r = mailimap_send_current_tag(session);
  if (r != MAILIMAP_NO_ERROR)
    return r;

  r = mailimap_store_send(session->imap_stream, set,
		use_unchangedsince, mod_sequence_valzer, store_att_flags);
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

int mailimap_uid_store_unchangedsince_optional(mailimap * session,
	struct mailimap_set * set, int use_unchangedsince, uint64_t mod_sequence_valzer,
	struct mailimap_store_att_flags * store_att_flags)
{
	struct mailimap_response * response;
	int r;
	int error_code;

	if (session->imap_state != MAILIMAP_STATE_SELECTED)
		return MAILIMAP_ERROR_BAD_STATE;

	r = mailimap_send_current_tag(session);
	if (r != MAILIMAP_NO_ERROR)
		return r;

	r = mailimap_uid_store_send(session->imap_stream, set,
		use_unchangedsince, mod_sequence_valzer, store_att_flags);
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

int mailimap_store_unchangedsince(mailimap * session,
	struct mailimap_set * set, uint64_t mod_sequence_valzer,
	struct mailimap_store_att_flags * store_att_flags)
{
	return mailimap_store_unchangedsince_optional(session, set, 1, mod_sequence_valzer,
		store_att_flags);
}

int mailimap_uid_store_unchangedsince(mailimap * session,
	struct mailimap_set * set, uint64_t mod_sequence_valzer,
	struct mailimap_store_att_flags * store_att_flags)
{
	return mailimap_uid_store_unchangedsince_optional(session, set, 1, mod_sequence_valzer,
		store_att_flags);
}

int mailimap_fetch_changedsince(mailimap * session,
	struct mailimap_set * set,
	struct mailimap_fetch_type * fetch_type, uint64_t mod_sequence_value,
	clist ** result)
{
  return mailimap_fetch_qresync_vanished(session, set, fetch_type, mod_sequence_value, 0,
                                         result, NULL);
}

int mailimap_uid_fetch_changedsince(mailimap * session,
	struct mailimap_set * set,
	struct mailimap_fetch_type * fetch_type, uint64_t mod_sequence_value,
	clist ** result)
{
  return mailimap_uid_fetch_qresync_vanished(session, set, fetch_type, mod_sequence_value, 0,
                                             result, NULL);
}

struct mailimap_fetch_att * mailimap_fetch_att_new_modseq(void)
{
  char * keyword;
  struct mailimap_fetch_att * att;
  
  keyword = strdup("MODSEQ");
  if (keyword == NULL)
    return NULL;
  
  att = mailimap_fetch_att_new_extension(keyword);
  if (att == NULL) {
    free(keyword);
    return NULL;
  }
  
  return att;
}

struct mailimap_search_key * mailimap_search_key_new_modseq(struct mailimap_flag * entry_name,
	int entry_type_req,
  uint64_t modseq_valzer)
{
  struct mailimap_search_key * key;

  key = malloc(sizeof(* key));
  if (key == NULL)
    return NULL;
  
	key->sk_type = MAILIMAP_SEARCH_KEY_MODSEQ;
	key->sk_data.sk_modseq.sk_entry_name = entry_name;
	key->sk_data.sk_modseq.sk_entry_type_req = entry_type_req;
	key->sk_data.sk_modseq.sk_modseq_valzer = modseq_valzer;
	
	return key;
}

static int search_modseq(mailimap * session, const char * charset,
                         struct mailimap_search_key * key,
                         int uid_enabled, int literalplus_enabled,
                         clist ** result, uint64_t * p_mod_sequence_value)
{
  struct mailimap_response * response;
  int r;
  int error_code;
  struct mailimap_condstore_search * search_data;
  clistiter * cur;
  
  if (session->imap_state != MAILIMAP_STATE_SELECTED)
    return MAILIMAP_ERROR_BAD_STATE;
  
  r = mailimap_send_current_tag(session);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  if (literalplus_enabled) {
    if (uid_enabled) {
      r = mailimap_uid_search_literalplus_send(session->imap_stream, charset, key);
    }
    else {
      r = mailimap_search_literalplus_send(session->imap_stream, charset, key);
    }
  }
  else {
    if (uid_enabled) {
      r = mailimap_uid_search_send(session->imap_stream, charset, key);
    }
    else {
      r = mailimap_search_send(session->imap_stream, charset, key);
    }
  }
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
  
  search_data = NULL;
  for(cur = clist_begin(session->imap_response_info->rsp_extension_list) ; cur != NULL ; cur = clist_next(cur)) {
    struct mailimap_extension_data * ext_data;
    
    ext_data = clist_content(cur);
    if (ext_data->ext_extension->ext_id != MAILIMAP_EXTENSION_CONDSTORE) {
      continue;
    }
    if (ext_data->ext_type != MAILIMAP_CONDSTORE_TYPE_SEARCH_DATA) {
      continue;
    }
    
    search_data = ext_data->ext_data;
    ext_data->ext_data = NULL;
    break;
  }
  
  if (search_data == NULL) {
    * result = session->imap_response_info->rsp_search_result;
    if (p_mod_sequence_value != NULL) {
      * p_mod_sequence_value = 0;
    }
    session->imap_response_info->rsp_search_result = NULL;
  }
  else {
    * result = search_data->cs_search_result;
    * p_mod_sequence_value = search_data->cs_modseq_value;
    search_data->cs_search_result = NULL;
    mailimap_condstore_search_free(search_data);
  }
  
  error_code = response->rsp_resp_done->rsp_data.rsp_tagged->rsp_cond_state->rsp_type;
  
  mailimap_response_free(response);
  
  switch (error_code) {
    case MAILIMAP_RESP_COND_STATE_OK:
      return MAILIMAP_NO_ERROR;
      
    default:
      if (uid_enabled) {
        return MAILIMAP_ERROR_UID_SEARCH;
      }
      else {
        return MAILIMAP_ERROR_SEARCH;
      }
  }
}

int mailimap_search_modseq(mailimap * session, const char * charset,
	struct mailimap_search_key * key, clist ** result, uint64_t * p_mod_sequence_value)
{
  return search_modseq(session, charset, key, 0, 0, result, p_mod_sequence_value);
}

int mailimap_uid_search_modseq(mailimap * session, const char * charset,
	struct mailimap_search_key * key, clist ** result, uint64_t * p_mod_sequence_value)
{
  return search_modseq(session, charset, key, 1, 0, result, p_mod_sequence_value);
}

LIBETPAN_EXPORT
int mailimap_search_literalplus_modseq(mailimap * session, const char * charset,
                                       struct mailimap_search_key * key, clist ** result, uint64_t * p_mod_sequence_value)
{
  return search_modseq(session, charset, key, 0, 1, result, p_mod_sequence_value);
}

LIBETPAN_EXPORT
int mailimap_uid_search_literalplus_modseq(mailimap * session, const char * charset,
                                           struct mailimap_search_key * key, clist ** result, uint64_t * p_mod_sequence_value)
{
  return search_modseq(session, charset, key, 1, 1, result, p_mod_sequence_value);
}

int mailimap_select_condstore(mailimap * session, const char * mb, uint64_t * p_mod_sequence_value)
{
	return mailimap_select_condstore_optional(session, mb, 1, p_mod_sequence_value);
}

int mailimap_select_condstore_optional(mailimap * session, const char * mb,
  int condstore, uint64_t * p_mod_sequence_value)
{
  struct mailimap_response * response;
  int r;
  int error_code;
  uint64_t mod_sequence_value;
  clistiter * cur;

  if ((session->imap_state != MAILIMAP_STATE_AUTHENTICATED) &&
      (session->imap_state != MAILIMAP_STATE_SELECTED))
    return MAILIMAP_ERROR_BAD_STATE;

  r = mailimap_send_current_tag(session);
  if (r != MAILIMAP_NO_ERROR)
    return r;

  r = mailimap_select_send(session->imap_stream, mb, condstore);
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

  mod_sequence_value = 0;
  error_code = response->rsp_resp_done->rsp_data.rsp_tagged->rsp_cond_state->rsp_type;
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
    return MAILIMAP_ERROR_SELECT;
  }
}

int mailimap_examine_condstore(mailimap * session, const char * mb, uint64_t * p_mod_sequence_value)
{
	return mailimap_examine_condstore_optional(session, mb, 1, p_mod_sequence_value);
}

int mailimap_examine_condstore_optional(mailimap * session, const char * mb,
  int condstore, uint64_t * p_mod_sequence_value)
{
  struct mailimap_response * response;
  int r;
  int error_code;
  uint64_t mod_sequence_value;
  clistiter * cur;

  if ((session->imap_state != MAILIMAP_STATE_AUTHENTICATED) &&
      (session->imap_state != MAILIMAP_STATE_SELECTED))
    return MAILIMAP_ERROR_BAD_STATE;

  r = mailimap_send_current_tag(session);
  if (r != MAILIMAP_NO_ERROR)
    return r;

  r = mailimap_examine_send(session->imap_stream, mb, condstore);
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

  mod_sequence_value = 0;
  error_code = response->rsp_resp_done->rsp_data.rsp_tagged->rsp_cond_state->rsp_type;
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
    return MAILIMAP_ERROR_EXAMINE;
  }
}

int mailimap_has_condstore(mailimap * session)
{
  return mailimap_has_extension(session, "CONDSTORE");
}

static int fetch_data_parse(mailstream * fd,
  MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx, struct mailimap_condstore_fetch_mod_resp ** result)
{
  int r;
  size_t cur_token;
  struct mailimap_condstore_fetch_mod_resp * fetch_data;
  uint64_t value;
  
  /*
   fetch-mod-resp      = "MODSEQ" SP "(" permsg-modsequence ")"
   permsg-modsequence  = mod-sequence-value
   */
  
  cur_token = * indx;
  
  r = mailimap_token_case_insensitive_parse(fd, buffer, &cur_token, "MODSEQ");
  if (r != MAILIMAP_NO_ERROR) {
    return r;
  }
  r = mailimap_space_parse(fd, buffer, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    return r;
  }
  r = mailimap_oparenth_parse(fd, buffer, parser_ctx, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    return r;
  }
  r = mailimap_mod_sequence_value_parse(fd, buffer, parser_ctx, &cur_token, &value);
  if (r != MAILIMAP_NO_ERROR) {
    return r;
  }
  r = mailimap_cparenth_parse(fd, buffer, parser_ctx, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    return r;
  }
  
  fetch_data = mailimap_condstore_fetch_mod_resp_new(value);
  if (fetch_data == NULL) {
    return MAILIMAP_ERROR_MEMORY;
  }
  
  * indx = cur_token;
  * result = fetch_data;
  
  return MAILIMAP_NO_ERROR;
}

static int resp_text_code_parse(mailstream * fd,
  MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx, struct mailimap_condstore_resptextcode ** result)
{
  size_t cur_token;
  struct mailimap_condstore_resptextcode * resptextcode;
  int r;
  
  cur_token = * indx;
  
  /*
    resp-text-code      =/ "HIGHESTMODSEQ" SP mod-sequence-value /
    "NOMODSEQ" /
    "MODIFIED" SP set
  */
  
  r = mailimap_token_case_insensitive_parse(fd, buffer, &cur_token, "HIGHESTMODSEQ");
  if (r == MAILIMAP_NO_ERROR) {
    uint64_t value;
    
    r = mailimap_space_parse(fd, buffer, &cur_token);
    if (r != MAILIMAP_NO_ERROR) {
      return r;
    }
    r = mailimap_mod_sequence_value_parse(fd, buffer, parser_ctx, &cur_token, &value);
    if (r != MAILIMAP_NO_ERROR) {
      return r;
    }
    
    resptextcode = mailimap_condstore_resptextcode_new(MAILIMAP_CONDSTORE_RESPTEXTCODE_HIGHESTMODSEQ, value, NULL);
    if (resptextcode == NULL)
      return MAILIMAP_ERROR_MEMORY;
    
    * indx = cur_token;
    * result = resptextcode;
    
    return MAILIMAP_NO_ERROR;
  }
  r = mailimap_token_case_insensitive_parse(fd, buffer, &cur_token, "NOMODSEQ");
  if (r == MAILIMAP_NO_ERROR) {
    resptextcode = mailimap_condstore_resptextcode_new(MAILIMAP_CONDSTORE_RESPTEXTCODE_NOMODSEQ, 0, NULL);
    if (resptextcode == NULL)
      return MAILIMAP_ERROR_MEMORY;
    
    * indx = cur_token;
    * result = resptextcode;
    
    return MAILIMAP_NO_ERROR;
  }
  r = mailimap_token_case_insensitive_parse(fd, buffer, &cur_token, "MODIFIED");
  if (r == MAILIMAP_NO_ERROR) {
    struct mailimap_set * set;
    
    r = mailimap_space_parse(fd, buffer, &cur_token);
    if (r != MAILIMAP_NO_ERROR) {
      return r;
    }
    
    r = mailimap_set_parse(fd, buffer, parser_ctx, &cur_token, &set);
    if (r != MAILIMAP_NO_ERROR) {
      return r;
    }
    
    resptextcode = mailimap_condstore_resptextcode_new(MAILIMAP_CONDSTORE_RESPTEXTCODE_MODIFIED, 0, set);
    if (resptextcode == NULL) {
      mailimap_set_free(set);
      return MAILIMAP_ERROR_MEMORY;
    }
    
    * indx = cur_token;
    * result = resptextcode;
    
    return MAILIMAP_NO_ERROR;
  }
  return MAILIMAP_ERROR_PARSE;
}

static int search_data_parse(mailstream * fd,
  MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx, struct mailimap_condstore_search ** result)
{
  int r;
  clist * number_list;
  struct mailimap_condstore_search * search_data;
  size_t cur_token;
  int res;
  uint64_t value;
  
  cur_token = * indx;
  
  /*
  mailbox-data        =/ "SEARCH" [1*(SP nz-number) SP
                         search-sort-mod-seq]
                         search-sort-mod-seq = "(" "MODSEQ" SP mod-sequence-value ")"
  */
  
  r = mailimap_token_case_insensitive_parse(fd, buffer, &cur_token, "SEARCH");
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
    return r;
  }
  
  r = mailimap_space_parse(fd, buffer, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }

  r = mailimap_struct_spaced_list_parse(fd, buffer, parser_ctx, &cur_token, &number_list,
    (mailimap_struct_parser *)
    mailimap_nz_number_alloc_parse,
    (mailimap_struct_destructor *)
    mailimap_number_alloc_free,
    0, NULL);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }
  
  r = mailimap_space_parse(fd, buffer, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto free_number_list;
  }
  r = mailimap_oparenth_parse(fd, buffer, parser_ctx, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto free_number_list;
  }
  r = mailimap_mod_sequence_value_parse(fd, buffer, parser_ctx, &cur_token, &value);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto free_number_list;
  }
  r = mailimap_cparenth_parse(fd, buffer, parser_ctx, &cur_token);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto free_number_list;
  }
  
  search_data = mailimap_condstore_search_new(number_list, value);
  if (search_data == NULL) {
    res = r;
    goto free_number_list;
  }
  
  * indx = cur_token;
  * result = search_data;
  
  return MAILIMAP_NO_ERROR;
  
free_number_list:
  clist_foreach(number_list, (clist_func) free, NULL);
  clist_free(number_list);
err:
  return res;
}

static int status_info_parse(mailstream * fd,
  MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx, struct mailimap_condstore_status_info ** result)
{
  int r;
  struct mailimap_condstore_status_info * status_info;
  size_t cur_token;
  int status_att;
  uint64_t value;
  
  cur_token = * indx;
  
  r = mailimap_status_att_parse(fd, buffer, parser_ctx, &cur_token, &status_att);
  if (r != MAILIMAP_NO_ERROR) {
    return r;
  }
  if (status_att != MAILIMAP_STATUS_ATT_HIGHESTMODSEQ) {
    return MAILIMAP_ERROR_PARSE;
  }
  r = mailimap_space_parse(fd, buffer, &cur_token);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  r = mailimap_mod_sequence_value_parse(fd, buffer, parser_ctx, &cur_token, &value);
  if (r != MAILIMAP_NO_ERROR)
    return r;
    
  status_info = mailimap_condstore_status_info_new(value);
  if (status_info == NULL)
    return MAILIMAP_ERROR_MEMORY;
  
  * result = status_info;
  * indx = cur_token;
  
  return MAILIMAP_NO_ERROR;
}

static int
	mailimap_condstore_extension_parse(int calling_parser, mailstream * fd,
	MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
	struct mailimap_extension_data ** result,
	size_t progr_rate, progress_function * progr_fun)
{
  size_t cur_token;
  int r;
  
  cur_token = * indx;
  
  switch (calling_parser) {
    case MAILIMAP_EXTENDED_PARSER_FETCH_DATA: {
      struct mailimap_condstore_fetch_mod_resp * fetch_data;
      struct mailimap_extension_data * ext_data;

      r = fetch_data_parse(fd, buffer, parser_ctx, &cur_token, &fetch_data);
      if (r != MAILIMAP_NO_ERROR)
        return r;
      ext_data = mailimap_extension_data_new(&mailimap_extension_condstore,
        MAILIMAP_CONDSTORE_TYPE_FETCH_DATA, fetch_data);
      if (ext_data == NULL) {
        mailimap_condstore_fetch_mod_resp_free(fetch_data);
        return MAILIMAP_ERROR_MEMORY;
      }
      * indx = cur_token;
      * result = ext_data;
      return MAILIMAP_NO_ERROR;
    }
      
    case MAILIMAP_EXTENDED_PARSER_RESP_TEXT_CODE: {
      struct mailimap_condstore_resptextcode * resptextcode;
      struct mailimap_extension_data * ext_data;
      
      r = resp_text_code_parse(fd, buffer, parser_ctx, &cur_token, &resptextcode);
      if (r != MAILIMAP_NO_ERROR)
        return r;
      ext_data = mailimap_extension_data_new(&mailimap_extension_condstore,
        MAILIMAP_CONDSTORE_TYPE_RESP_TEXT_CODE, resptextcode);
      if (ext_data == NULL) {
        mailimap_condstore_resptextcode_free(resptextcode);
        return MAILIMAP_ERROR_MEMORY;
      }
      * indx = cur_token;
      * result = ext_data;
      return MAILIMAP_NO_ERROR;
    }
      
    case MAILIMAP_EXTENDED_PARSER_MAILBOX_DATA: {
      struct mailimap_condstore_search * search_data;
      struct mailimap_extension_data * ext_data;
      
      search_data = NULL;
      r = search_data_parse(fd, buffer, parser_ctx, &cur_token, &search_data);
      if (r != MAILIMAP_NO_ERROR)
        return r;
      ext_data = mailimap_extension_data_new(&mailimap_extension_condstore,
        MAILIMAP_CONDSTORE_TYPE_SEARCH_DATA, search_data);
      if (ext_data == NULL) {
        mailimap_condstore_search_free(search_data);
        return MAILIMAP_ERROR_MEMORY;
      }
      * indx = cur_token;
      * result = ext_data;
      return MAILIMAP_NO_ERROR;
    }
    
    case MAILIMAP_EXTENDED_PARSER_STATUS_ATT: {
      struct mailimap_condstore_status_info * status_info;
      struct mailimap_extension_data * ext_data;
      
      r = status_info_parse(fd, buffer, parser_ctx, &cur_token, &status_info);
      if (r != MAILIMAP_NO_ERROR)
        return r;
      ext_data = mailimap_extension_data_new(&mailimap_extension_condstore,
        MAILIMAP_CONDSTORE_TYPE_STATUS_INFO, status_info);
      if (ext_data == NULL) {
        mailimap_condstore_status_info_free(status_info);
        return MAILIMAP_ERROR_MEMORY;
      }
      * indx = cur_token;
      * result = ext_data;
      return MAILIMAP_NO_ERROR;
    }
  }
  
  return MAILIMAP_ERROR_PARSE;
}

static void
	mailimap_condstore_extension_data_free(struct mailimap_extension_data * ext_data)
{
  switch (ext_data->ext_type) {
    case MAILIMAP_CONDSTORE_TYPE_FETCH_DATA:
      mailimap_condstore_fetch_mod_resp_free(ext_data->ext_data);
      break;
    case MAILIMAP_CONDSTORE_TYPE_RESP_TEXT_CODE:
      mailimap_condstore_resptextcode_free(ext_data->ext_data);
      break;
    case MAILIMAP_CONDSTORE_TYPE_SEARCH_DATA:
      mailimap_condstore_search_free(ext_data->ext_data);
      break;
    case MAILIMAP_CONDSTORE_TYPE_STATUS_INFO:
      mailimap_condstore_status_info_free(ext_data->ext_data);
      break;
  }

  free(ext_data);
}
