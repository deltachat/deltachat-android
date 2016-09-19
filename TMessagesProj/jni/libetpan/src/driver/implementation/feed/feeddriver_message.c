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

/*
 * $Id: feeddriver_message.c,v 1.2 2007/04/07 16:12:14 hoa Exp $
 */

#ifdef HAVE_CONFIG_H
#	include <config.h>
#endif

#include "feeddriver_message.h"

#include "mailmessage_tools.h"
#include "feeddriver.h"
#include "newsfeed.h"
#include <string.h>
#include <stdlib.h>

static int feed_prefetch(mailmessage * msg_info);

static void feed_prefetch_free(struct generic_message_t * msg);

static int feed_initialize(mailmessage * msg_info);

static int feed_fetch_size(mailmessage * msg_info,
			   size_t * result);

static mailmessage_driver local_feed_message_driver = {
  /* msg_name */ "feed",

  /* msg_initialize */ feed_initialize,
  /* msg_uninitialize */ mailmessage_generic_uninitialize,

  /* msg_flush */ mailmessage_generic_flush,
  /* msg_check */ NULL,

  /* msg_fetch_result_free */ mailmessage_generic_fetch_result_free,

  /* msg_fetch */ mailmessage_generic_fetch,
  /* msg_fetch_header */ mailmessage_generic_fetch_header,
  /* msg_fetch_body */ mailmessage_generic_fetch_body,
  /* msg_fetch_size */ feed_fetch_size,
  /* msg_get_bodystructure */ mailmessage_generic_get_bodystructure,
  /* msg_fetch_section */ mailmessage_generic_fetch_section,
  /* msg_fetch_section_header */ mailmessage_generic_fetch_section_header,
  /* msg_fetch_section_mime */ mailmessage_generic_fetch_section_mime,
  /* msg_fetch_section_body */ mailmessage_generic_fetch_section_body,
  /* msg_fetch_envelope */ mailmessage_generic_fetch_envelope,

  /* msg_get_flags */ NULL,
};

mailmessage_driver * feed_message_driver = &local_feed_message_driver;

static inline struct feed_session_state_data *
get_data(mailmessage * msg_info)
{
  return msg_info->msg_session->sess_data;
}

static inline struct newsfeed * get_feed_session(mailmessage * msg_info)
{
  return get_data(msg_info)->feed_session;
}

static int feed_prefetch(mailmessage * msg_info)
{
  struct generic_message_t * msg;
  int r;
  MMAPString * str;
  const char * text;
  int col;
  struct newsfeed * feed;
  struct newsfeed_item * item;
  int res;
  
  feed = get_feed_session(msg_info);
  item = newsfeed_get_item(feed, msg_info->msg_index);
  
  str = mmap_string_new("");
  if (str == NULL) {
    res = MAIL_ERROR_MEMORY;
    goto err;
  }
  
  col = 0;
  r = mailimf_fields_write_mem(str, &col,
      msg_info->msg_fields);
  if (r != MAILIMF_NO_ERROR) {
    res = MAIL_ERROR_MEMORY;
    goto free_str;
  }
  
  if (mmap_string_append(str, "\r\n") == NULL) {
    res = MAIL_ERROR_MEMORY;
    goto free_str;
  }
  
  text = newsfeed_item_get_text(item);
  if (text == NULL) {
    /* if no content, fallback on summary */
    text = newsfeed_item_get_summary(item);
  }
  if (mmap_string_append(str, text) == NULL) {
    res = MAIL_ERROR_MEMORY;
    goto free_str;
  }
  
  msg = msg_info->msg_data;
  msg->msg_message = str->str;
  msg->msg_length = str->len;
  
  mmap_string_ref(str);
  
  return MAIL_NO_ERROR;
  
 free_str:
  mmap_string_free(str);
 err:
  return res;
}

static void feed_prefetch_free(struct generic_message_t * msg)
{
  if (msg->msg_message != NULL) {
    mmap_string_unref(msg->msg_message);
    msg->msg_message = NULL;
  }
}

static int feed_initialize(mailmessage * msg_info)
{
  struct generic_message_t * msg;
  int r;
  char * uid;
  char static_uid[20];

  snprintf(static_uid, 20, "%u", msg_info->msg_index);
  uid = strdup(static_uid);
  if (uid == NULL)
    return MAIL_ERROR_MEMORY;
  
  r = mailmessage_generic_initialize(msg_info);
  if (r != MAIL_NO_ERROR) {
    free(uid);
    return r;
  }

  msg = msg_info->msg_data;
  msg->msg_prefetch = feed_prefetch;
  msg->msg_prefetch_free = feed_prefetch_free;
  msg_info->msg_uid = uid;

  return MAIL_NO_ERROR;
}

static int feed_fetch_size(mailmessage * msg_info,
    size_t * result)
{
  int r;
  struct generic_message_t * msg;
  struct mailmime * mime;
  
  r = mailmessage_generic_get_bodystructure(msg_info, &mime);
  if (r != MAIL_NO_ERROR) {
    return r;
  }
  
  msg = msg_info->msg_data;
  return (int) msg->msg_length;
}
