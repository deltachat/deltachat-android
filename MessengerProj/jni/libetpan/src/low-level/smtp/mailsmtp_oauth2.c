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
#include "mailsmtp_oauth2.h"

#include <string.h>
#include <stdlib.h>
#include <stdio.h>

#include "base64.h"
#include "mailsmtp_private.h"

#define SMTP_STRING_SIZE 513

enum {
  XOAUTH2_TYPE_GMAIL,
  XOAUTH2_TYPE_OUTLOOK_COM,
};

static int oauth2_authenticate(mailsmtp * session, int type, const char * auth_user,
    const char * access_token);

LIBETPAN_EXPORT
int mailsmtp_oauth2_authenticate(mailsmtp * session, const char * auth_user,
    const char * access_token)
{
  return oauth2_authenticate(session, XOAUTH2_TYPE_GMAIL, auth_user, access_token);
}

LIBETPAN_EXPORT
int mailsmtp_oauth2_outlook_authenticate(mailsmtp * session, const char * auth_user,
    const char * access_token)
{
  return oauth2_authenticate(session, XOAUTH2_TYPE_OUTLOOK_COM, auth_user, access_token);
}

static int oauth2_authenticate(mailsmtp * session, int type, const char * auth_user,
    const char * access_token)
{
  int r;
  char command[SMTP_STRING_SIZE];
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
    res = MAILSMTP_ERROR_MEMORY;
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
    res = MAILSMTP_ERROR_MEMORY;
    goto free;
  }
  
  switch (type) {
    case XOAUTH2_TYPE_GMAIL:
    default:
    {
      snprintf(command, SMTP_STRING_SIZE, "AUTH XOAUTH2 ");
      r = mailsmtp_send_command_private(session, command);
      if (r == -1) {
        res = MAILSMTP_ERROR_STREAM;
        goto free;
      }
      r = mailsmtp_send_command_private(session, full_auth_string_b64);
      if (r == -1) {
        res = MAILSMTP_ERROR_STREAM;
        goto free;
      }
      snprintf(command, SMTP_STRING_SIZE, "\r\n");
      r = mailsmtp_send_command_private(session, command);
      if (r == -1) {
        res = MAILSMTP_ERROR_STREAM;
        goto free;
      }
      break;
    }
    case XOAUTH2_TYPE_OUTLOOK_COM:
      snprintf(command, SMTP_STRING_SIZE, "AUTH XOAUTH2\r\n");
      r = mailsmtp_send_command_private(session, command);
      if (r == -1) {
        res = MAILSMTP_ERROR_STREAM;
        goto free;
      }
      break;
  }
  
  r = mailsmtp_read_response(session);
  switch (r) {
    case 220:
    case 235:
      res = MAILSMTP_NO_ERROR;
      goto free;
      
    case 334:
      /* AUTH in progress */
      
      switch (type) {
        case XOAUTH2_TYPE_GMAIL:
        default:
        {
          /* There's probably an error, send an empty line as acknowledgement. */
          snprintf(command, SMTP_STRING_SIZE, "\r\n");
          r = mailsmtp_send_command_private(session, command);
          if (r == -1) {
            res = MAILSMTP_ERROR_STREAM;
            goto free;
          }
          break;
        }
        case XOAUTH2_TYPE_OUTLOOK_COM:
        {
          r = mailsmtp_send_command_private(session, full_auth_string_b64);
          if (r == -1) {
            res = MAILSMTP_ERROR_STREAM;
            goto free;
          }
          snprintf(command, SMTP_STRING_SIZE, "\r\n");
          r = mailsmtp_send_command_private(session, command);
          if (r == -1) {
            res = MAILSMTP_ERROR_STREAM;
            goto free;
          }
          break;
        }
      }
      
      r = mailsmtp_read_response(session);
      switch (r) {
        case 535:
          res = MAILSMTP_ERROR_AUTH_LOGIN;
          goto free;
          
        case 220:
        case 235:
          res = MAILSMTP_NO_ERROR;
          goto free;
          
        case 0:
          res = MAILSMTP_ERROR_STREAM;
          goto free;

        default:
          res = MAILSMTP_ERROR_UNEXPECTED_CODE;
          goto free;
      }
      break;

    case 0:
      res = MAILSMTP_ERROR_STREAM;
      goto free;

    default:
      res = MAILSMTP_ERROR_UNEXPECTED_CODE;
      goto free;
  }
  
free:
  free(full_auth_string);
  free(full_auth_string_b64);
  return res;
}
