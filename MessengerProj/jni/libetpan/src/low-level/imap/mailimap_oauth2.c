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
#include "mailimap_oauth2.h"

#include <string.h>
#include <stdlib.h>

#include "base64.h"
#include "mailimap_sender.h"
#include "mailimap_parser.h"
#include "mailimap.h"

static int mailimap_oauth2_authenticate_send(mailimap * session,
                                             const char * auth_user,
                                             const char * access_token);

LIBETPAN_EXPORT
int mailimap_oauth2_authenticate(mailimap * session, const char *auth_user, const char * access_token)
{
  struct mailimap_response * response;
  int r;
  int error_code;
  size_t indx;
  struct mailimap_continue_req * cont_req;
  
  if (session->imap_state != MAILIMAP_STATE_NON_AUTHENTICATED)
    return MAILIMAP_ERROR_BAD_STATE;
  
  mailstream_set_privacy(session->imap_stream, 0);
  r = mailimap_send_current_tag(session);
  if (r != MAILIMAP_NO_ERROR) {
    mailstream_set_privacy(session->imap_stream, 1);
    return r;
  }
  
  r = mailimap_oauth2_authenticate_send(session, auth_user, access_token);
  if (r != MAILIMAP_NO_ERROR) {
    mailstream_set_privacy(session->imap_stream, 1);
    return r;
  }
  
  r = mailimap_crlf_send(session->imap_stream);
  if (r != MAILIMAP_NO_ERROR) {
    mailstream_set_privacy(session->imap_stream, 1);
    return r;
  }
  
  if (mailstream_flush(session->imap_stream) == -1) {
    mailstream_set_privacy(session->imap_stream, 1);
    return MAILIMAP_ERROR_STREAM;
  }
  mailstream_set_privacy(session->imap_stream, 1);
  
  if (mailimap_read_line(session) == NULL)
    return MAILIMAP_ERROR_STREAM;

  indx = 0;
  r = mailimap_continue_req_parse(session->imap_stream,
      session->imap_stream_buffer, NULL,
      &indx, &cont_req,
      session->imap_progr_rate, session->imap_progr_fun);
  if (r == MAILIMAP_NO_ERROR) {
    mailimap_continue_req_free(cont_req);
    
    /* There's probably an error, send an empty line as acknowledgement. */
    r = mailimap_crlf_send(session->imap_stream);
    if (r != MAILIMAP_NO_ERROR) {
      return r;
    }
  
    if (mailstream_flush(session->imap_stream) == -1) {
      return MAILIMAP_ERROR_STREAM;
    }
  }
  else if (r == MAILIMAP_ERROR_PARSE) {
    r = MAILIMAP_NO_ERROR;
  }

  if (r != MAILIMAP_NO_ERROR) {
    return r;
  }

  r = mailimap_parse_response(session, &response);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  error_code = response->rsp_resp_done->rsp_data.rsp_tagged->rsp_cond_state->rsp_type;
  
  mailimap_response_free(response);
  
  switch (error_code) {
    case MAILIMAP_RESP_COND_STATE_OK:
      session->imap_state = MAILIMAP_STATE_AUTHENTICATED;
      return MAILIMAP_NO_ERROR;
      
    default:
      return MAILIMAP_ERROR_LOGIN;
  }
}

static int mailimap_oauth2_authenticate_send(mailimap * session,
                                             const char * auth_user,
                                             const char * access_token)
{
  int r;
  char * ptr;
  char * full_auth_string;
  char * full_auth_string_b64;
  size_t auth_user_len;
  size_t access_token_len;
  size_t full_auth_string_len;
  int res;
  
  full_auth_string = NULL;
  full_auth_string_b64 = NULL;
  
  /* Build client response string */
  auth_user_len = strlen(auth_user);
  access_token_len = strlen(access_token);
  full_auth_string_len = 5 + auth_user_len + 1 + 12 + access_token_len + 2;
  full_auth_string = malloc(full_auth_string_len + 1);
  if (full_auth_string == NULL) {
    res = MAILIMAP_ERROR_MEMORY;
    goto free;
  }
  
  ptr = memcpy(full_auth_string, "user=", 5);
  ptr = memcpy(ptr + 5, auth_user, auth_user_len);
  ptr = memcpy(ptr + auth_user_len, "\1auth=Bearer ", 13);
  ptr = memcpy(ptr + 13, access_token, access_token_len);
  ptr = memcpy(ptr + access_token_len, "\1\1\0", 3);
  
  /* Convert to base64 */
  full_auth_string_b64 = encode_base64(full_auth_string, (int) full_auth_string_len);
  if (full_auth_string_b64 == NULL) {
    res = MAILIMAP_ERROR_MEMORY;
    goto free;
  }
  
  r = mailimap_token_send(session->imap_stream, "AUTHENTICATE");
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto free;
  }
  r = mailimap_space_send(session->imap_stream);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto free;
  }
  r = mailimap_token_send(session->imap_stream, "XOAUTH2");
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto free;
  }
  r = mailimap_space_send(session->imap_stream);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto free;
  }
  r = mailimap_token_send(session->imap_stream, full_auth_string_b64);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto free;
  }
  
  res = MAILIMAP_NO_ERROR;
  
 free:
  free(full_auth_string);
  free(full_auth_string_b64);
  return res;
}

LIBETPAN_EXPORT
int mailimap_has_xoauth2(mailimap * session)
{
  return mailimap_has_authentication(session, "XOAUTH2");
}
