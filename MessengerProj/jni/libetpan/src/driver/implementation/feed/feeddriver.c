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
 * $Id: feeddriver.c,v 1.3 2008/04/11 07:33:08 hoa Exp $
 */

#ifdef HAVE_CONFIG_H
#	include <config.h>
#endif

#include "feeddriver.h"

#include <string.h>
#include <stdlib.h>
#include <time.h>

#include "mailimf_types_helper.h"
#include "newsfeed.h"
#include "mail.h"
#include "mailmessage.h"
#include "maildriver_tools.h"
#include "feeddriver_message.h"
#include "feeddriver_types.h"

#define MIN_DELAY 5

static int feeddriver_initialize(mailsession * session);

static void feeddriver_uninitialize(mailsession * session);

static int feeddriver_connect_path(mailsession * session, const char * path);

static int feeddriver_status_folder(mailsession * session, const char * mb,
    uint32_t * result_messages,
    uint32_t * result_recent,
    uint32_t * result_unseen);

static int feeddriver_messages_number(mailsession * session, const char * mb,
				      uint32_t * result);

static int
feeddriver_get_envelopes_list(mailsession * session,
			      struct mailmessage_list * env_list);


static int feeddriver_get_messages_list(mailsession * session,
					struct mailmessage_list ** result);

static int feeddriver_get_message(mailsession * session,
				  uint32_t num, mailmessage ** result);

static int feeddriver_get_message_by_uid(mailsession * session,
    const char * uid,
    mailmessage ** result);

static mailsession_driver local_feed_session_driver = {
  /* sess_name */ "feed",

  /* sess_initialize */ feeddriver_initialize,
  /* sess_uninitialize */ feeddriver_uninitialize,

  /* sess_parameters */ NULL,

  /* sess_connect_stream */ NULL,
  /* sess_connect_path */ feeddriver_connect_path,
  /* sess_starttls */ NULL,
  /* sess_login */ NULL,
  /* sess_logout */ NULL,
  /* sess_noop */ NULL,

  /* sess_build_folder_name */ NULL,
  /* sess_create_folder */ NULL,
  /* sess_delete_folder */ NULL,
  /* sess_rename_folder */ NULL,
  /* sess_check_folder */ NULL,
  /* sess_examine_folder */ NULL,
  /* sess_select_folder */ NULL,
  /* sess_expunge_folder */ NULL,
  /* sess_status_folder */ feeddriver_status_folder,
  /* sess_messages_number */ feeddriver_messages_number,
  /* sess_recent_number */ feeddriver_messages_number,
  /* sess_unseen_number */ feeddriver_messages_number,
  /* sess_list_folders */ NULL,
  /* sess_lsub_folders */ NULL,
  /* sess_subscribe_folder */ NULL,
  /* sess_unsubscribe_folder */ NULL,

  /* sess_append_message */ NULL,
  /* sess_append_message_flags */ NULL,
  /* sess_copy_message */ NULL,
  /* sess_move_message */ NULL,

  /* sess_get_message */ feeddriver_get_message,
  /* sess_get_message_by_uid */ feeddriver_get_message_by_uid,

  /* sess_get_messages_list */ feeddriver_get_messages_list,
  /* sess_get_envelopes_list */ feeddriver_get_envelopes_list,
  /* sess_remove_message */ NULL,

  /* sess_login_sasl */ NULL,
};


mailsession_driver * feed_session_driver = &local_feed_session_driver;

static void update(mailsession * session);

static int feeddriver_feed_error_to_mail_error(int error)
{
  switch (error) {
  case NEWSFEED_NO_ERROR:
    return MAIL_NO_ERROR;
    
  case NEWSFEED_ERROR_CANCELLED:
    return MAIL_ERROR_STREAM;
    
  case NEWSFEED_ERROR_INTERNAL:
    return MAIL_ERROR_UNKNOWN;
  
  case NEWSFEED_ERROR_BADURL:
    return MAIL_ERROR_INVAL;
    
  case NEWSFEED_ERROR_RESOLVE_PROXY:
  case NEWSFEED_ERROR_RESOLVE_HOST:
    return MAIL_ERROR_CONNECT;
    
  case NEWSFEED_ERROR_CONNECT:
    return MAIL_ERROR_CONNECT;
    
  case NEWSFEED_ERROR_STREAM:
    return MAIL_ERROR_STREAM;
    
  case NEWSFEED_ERROR_PROTOCOL:
  case NEWSFEED_ERROR_PARSE:
    return MAIL_ERROR_PARSE;
    
  case NEWSFEED_ERROR_ACCESS:
    return MAIL_ERROR_NO_PERMISSION;
    
  case NEWSFEED_ERROR_AUTHENTICATION:
    return MAIL_ERROR_LOGIN;
    
  case NEWSFEED_ERROR_FTP:
    return MAIL_ERROR_UNKNOWN;
    
  case NEWSFEED_ERROR_PARTIAL_FILE:
  case NEWSFEED_ERROR_FETCH:
    return MAIL_ERROR_FETCH;
    
  case NEWSFEED_ERROR_HTTP:
    return MAIL_ERROR_UNKNOWN;
    
  case NEWSFEED_ERROR_FILE:
    return MAIL_ERROR_FILE;
    
  case NEWSFEED_ERROR_PUT:
    return MAIL_ERROR_APPEND;
    
  case NEWSFEED_ERROR_MEMORY:
    return MAIL_ERROR_MEMORY;
    
  case NEWSFEED_ERROR_SSL:
    return MAIL_ERROR_SSL;
    
  case NEWSFEED_ERROR_LDAP:
    return MAIL_ERROR_UNKNOWN;
    
  case NEWSFEED_ERROR_UNSUPPORTED_PROTOCOL:
    return MAIL_ERROR_INVAL;
  }
  
  return MAIL_ERROR_UNKNOWN;
}

static inline struct feed_session_state_data *
get_data(mailsession * session)
{
  return session->sess_data;
}

static inline struct newsfeed * get_feed_session(mailsession * session)
{
  return get_data(session)->feed_session;
}

static int feeddriver_initialize(mailsession * session)
{
  struct feed_session_state_data * data;
  struct newsfeed * feed;

  feed = newsfeed_new();
  if (feed == NULL)
    goto err;

  data = malloc(sizeof(* data));
  if (data == NULL)
    goto free;

  data->feed_session = feed;
  data->feed_error = MAIL_NO_ERROR;
  session->sess_data = data;

  return MAIL_NO_ERROR;

 free:
  newsfeed_free(feed);
 err:
  return MAIL_ERROR_MEMORY;
}

static void feeddriver_uninitialize(mailsession * session)
{
  struct feed_session_state_data * data;

  data = get_data(session);
  
  newsfeed_free(data->feed_session);
  free(data);
  
  session->sess_data = NULL;
}

static int feeddriver_connect_path(mailsession * session, const char * path)
{
  struct feed_session_state_data * data;
  int r;
  
  data = get_data(session);
  r = newsfeed_set_url(data->feed_session, path);
  return feeddriver_feed_error_to_mail_error(r);
}

static int feeddriver_status_folder(mailsession * session, const char * mb,
    uint32_t * result_messages,
    uint32_t * result_recent,
    uint32_t * result_unseen)
{
  uint32_t count;
  int r;
  
  r = feeddriver_messages_number(session, mb, &count);
  if (r != MAIL_NO_ERROR)
    return r;
          
  * result_messages = count;
  * result_recent = count;
  * result_unseen = count;
  
  return MAIL_NO_ERROR;
}

static int feeddriver_messages_number(mailsession * session, const char * mb,
    uint32_t * result)
{
  struct feed_session_state_data * data;
  unsigned int count;
  int res;
  
  update(session);
  data = get_data(session);
  if (data->feed_error != MAIL_NO_ERROR) {
    res = data->feed_error;
    goto err;
  }
  
  count = newsfeed_item_list_get_count(data->feed_session);
  
  * result = count;
  
  return MAIL_NO_ERROR;
  
 err:
  return res;
}

static void update(mailsession * session)
{
  int r;
  struct feed_session_state_data * data;
  time_t value;
  
  data = get_data(session);
  
  value = time(NULL);
  if (data->feed_last_update != (time_t) -1) {
    if (value - data->feed_last_update < MIN_DELAY)
      return;
  }
  
  r = newsfeed_update(data->feed_session, -1);
  data->feed_error = feeddriver_feed_error_to_mail_error(r);
  if (data->feed_error == MAIL_NO_ERROR) {
    value = time(NULL);
    data->feed_last_update = value;
  }
}

static int
feeddriver_get_envelopes_list(mailsession * session,
			      struct mailmessage_list * env_list)
{
  return MAIL_NO_ERROR;
}

static inline int to_be_quoted(const char * word, size_t size)
{
  int do_quote;
  const char * cur;
  size_t i;

  do_quote = 0;
  cur = word;
  for(i = 0 ; i < size ; i ++) {
    switch (* cur) {
    case ',':
    case ':':
    case '!':
    case '"':
    case '#':
    case '$':
    case '@':
    case '[':
    case '\\':
    case ']':
    case '^':
    case '`':
    case '{':
    case '|':
    case '}':
    case '~':
    case '=':
    case '?':
    case '_':
      do_quote = 1;
      break;
    default:
      if (((unsigned char) * cur) >= 128)
        do_quote = 1;
      break;
    }
    cur ++;
  }

  return do_quote;
}

#define MAX_IMF_LINE 72

static inline int quote_word(const char * display_charset,
    MMAPString * mmapstr, const char * word, size_t size)
{
  const char * cur;
  size_t i;
  char hex[4];
  int col;
  
  if (mmap_string_append(mmapstr, "=?") == NULL)
    return -1;
  if (mmap_string_append(mmapstr, display_charset) == NULL)
    return -1;
  if (mmap_string_append(mmapstr, "?Q?") == NULL)
    return -1;
  
  col = (int) mmapstr->len;
  
  cur = word;
  for(i = 0 ; i < size ; i ++) {
    int do_quote_char;

    if (col + 2 /* size of "?=" */
        + 3 /* max size of newly added character */
        + 1 /* minimum column of string in a
               folded header */ >= MAX_IMF_LINE) {
      int old_pos;
      /* adds a concatened encoded word */
      
      if (mmap_string_append(mmapstr, "?=") == NULL)
        return -1;
      
      if (mmap_string_append(mmapstr, " ") == NULL)
        return -1;
      
      old_pos = (int) mmapstr->len;
      
      if (mmap_string_append(mmapstr, "=?") == NULL)
        return -1;
      if (mmap_string_append(mmapstr, display_charset) == NULL)
        return -1;
      if (mmap_string_append(mmapstr, "?Q?") == NULL)
        return -1;
      
      col = (int) mmapstr->len - old_pos;
    }
    
    do_quote_char = 0;
    switch (* cur) {
    case ',':
    case ':':
    case '!':
    case '"':
    case '#':
    case '$':
    case '@':
    case '[':
    case '\\':
    case ']':
    case '^':
    case '`':
    case '{':
    case '|':
    case '}':
    case '~':
    case '=':
    case '?':
    case '_':
      do_quote_char = 1;
      break;

    default:
      if (((unsigned char) * cur) >= 128)
        do_quote_char = 1;
      break;
    }

    if (do_quote_char) {
      snprintf(hex, 4, "=%2.2X", (unsigned char) * cur);
      if (mmap_string_append(mmapstr, hex) == NULL)
        return -1;
      col += 3;
    }
    else {
      if (* cur == ' ') {
        if (mmap_string_append_c(mmapstr, '_') == NULL)
          return -1;
      }
      else {
        if (mmap_string_append_c(mmapstr, * cur) == NULL)
          return -1;
      }
      col += 3;
    }
    cur ++;
  }

  if (mmap_string_append(mmapstr, "?=") == NULL)
    return -1;
  
  return 0;
}

static inline void get_word(const char * begin,
    const char ** pend, int * pto_be_quoted)
{
  const char * cur;
  
  cur = begin;

  while ((* cur != ' ') && (* cur != '\t') && (* cur != '\0')) {
    cur ++;
  }

  if (cur - begin +
      1  /* minimum column of string in a
            folded header */ > MAX_IMF_LINE)
    * pto_be_quoted = 1;
  else
    * pto_be_quoted = to_be_quoted(begin, cur - begin);
  
  * pend = cur;
}

static char * make_quoted_printable(const char * display_charset,
    const char * phrase)
{
  char * str;
  const char * cur;
  MMAPString * mmapstr;
  int r;

  mmapstr = mmap_string_new("");
  if (mmapstr == NULL)
    return NULL;
  
  cur = phrase;
  while (* cur != '\0') {
    const char * begin;
    const char * end;
    int do_quote;
    int quote_words;

    begin = cur;
    end = begin;
    quote_words = 0;
    do_quote = 1;

    while (* cur != '\0') {
      get_word(cur, &cur, &do_quote);
      if (do_quote) {
        quote_words = 1;
        end = cur;
      }
      else
        break;
      if (* cur != '\0')
        cur ++;
    }

    if (quote_words) {
      r = quote_word(display_charset, mmapstr, begin, end - begin);
      if (r < 0) {
        mmap_string_free(mmapstr);
        return NULL;
      }
      
      if ((* end == ' ') || (* end == '\t')) {
        if (mmap_string_append_c(mmapstr, * end) == NULL) {
          mmap_string_free(mmapstr);
          return NULL;
        }
        end ++;
      }

      if (* end != '\0') {
        if (mmap_string_append_len(mmapstr, end, cur - end) == NULL) {
          mmap_string_free(mmapstr);
          return NULL;
        }
      }
    }
    else {
      if (mmap_string_append_len(mmapstr, begin, cur - begin) == NULL) {
        mmap_string_free(mmapstr);
        return NULL;
      }
    }

    if ((* cur == ' ') || (* cur == '\t')) {
      if (mmap_string_append_c(mmapstr, * cur) == 0) {
        mmap_string_free(mmapstr);
        return NULL;
      }
      cur ++;
    }
  }

  str = strdup(mmapstr->str);
  if (str == NULL) {
    mmap_string_free(mmapstr);
    return NULL;
  }

  mmap_string_free(mmapstr);
  
  return str;
}

static mailmessage * feed_item_to_message(mailsession * session,
    unsigned int num,
    struct newsfeed_item * item)
{
  struct mailimf_fields * fields;
  struct mailimf_date_time * date_time;
  time_t time_modified;
  struct mailimf_mailbox_list * from;
  mailmessage * msg;
  char * subject;
  const char * subject_const;
  char * msg_id;
  int r;
  const char * author_const;
  
  from = NULL;
  author_const = newsfeed_item_get_author(item);
  if (author_const != NULL) {
    char * author;
    char * addr_spec;
    struct mailimf_mailbox * mb;
    
    author = strdup(author_const);
    if (author == NULL) {
      goto err;
    }
    
    from = mailimf_mailbox_list_new_empty();
    if (from == NULL) {
      free(author);
      goto err;
    }
    addr_spec = strdup("invalid@localhost.local");
    if (addr_spec == NULL) {
      free(author);
      goto free_from;
    }
      
    /* XXX - encode author with MIME */
    mb = mailimf_mailbox_new(author, addr_spec);
    if (mb == NULL) {
      free(addr_spec);
      free(author);
      goto free_from;
    }
    
    r = mailimf_mailbox_list_add(from, mb);
    if (r != MAILIMF_NO_ERROR) {
      mailimf_mailbox_free(mb);
      goto free_from;
    }
  }
  
  date_time = NULL;
  time_modified = newsfeed_item_get_date_modified(item);
  if (time_modified != (time_t) -1) {
    date_time = mailimf_get_date(time_modified);
    if (date_time == NULL) {
      goto free_from;
    }
  }
  
  subject = NULL;
  subject_const = newsfeed_item_get_title(item);
  if (subject_const != NULL) {
    subject = make_quoted_printable("utf-8", subject_const);
    if (subject == NULL) {
      goto free_date;
    }
  }
  
  msg_id = mailimf_get_message_id();
  if (msg_id == NULL) {
    goto free_subject;
  }
  
  fields = mailimf_fields_new_with_data_all(date_time,
      from,
      NULL,
      NULL,
      NULL,
      NULL,
      NULL,
      msg_id,
      NULL,
      NULL,
      subject);
  
  msg = mailmessage_new();
  r = mailmessage_init(msg, session, feed_message_driver, num, 0);
  if (r != MAIL_NO_ERROR) {
    goto free_fields;
  }
  msg->msg_fields = fields;
  
  return msg;
  
 free_fields:
  mailimf_fields_free(fields);
  goto err;
 free_subject:
  free(subject);
 free_date:
  mailimf_date_time_free(date_time);
 free_from:
  mailimf_mailbox_list_free(from);
 err:
  return NULL;
}

static int feeddriver_get_messages_list(mailsession * session,
    struct mailmessage_list ** result)
{
  unsigned int i;
  struct feed_session_state_data * data;
  unsigned int count;
  struct mailmessage_list * msg_list;
  carray * tab;
  int res;
  int r;
  
  update(session);
  data = get_data(session);
  if (data->feed_error != MAIL_NO_ERROR) {
    res = data->feed_error;
    goto err;
  }
  
  count = newsfeed_item_list_get_count(data->feed_session);
  
  tab = carray_new(count);
  if (tab == NULL) {
    res = MAIL_ERROR_MEMORY;
    goto err;
  }
  fprintf(stderr, "count: %i\n", count);
  
  for(i = 0 ; i < count ; i ++) {
    struct newsfeed_item * item;
    mailmessage * msg;
    
    item = newsfeed_get_item(data->feed_session, i);
    msg = feed_item_to_message(session, i, item);
    r = carray_add(tab, msg, NULL);
    if (r < 0) {
      res = MAIL_ERROR_MEMORY;
      goto free_tab;
    }
  }
  
  msg_list = mailmessage_list_new(tab);
  if (msg_list == NULL) {
    res = MAIL_ERROR_MEMORY;
    goto free_tab;
  }
  
  * result = msg_list;
  
  return MAIL_NO_ERROR;
  
 free_tab:
  for(i = 0 ; i < carray_count(tab) ; i ++) {
    mailmessage * msg;
    
    msg = carray_get(tab, i);
    mailmessage_free(msg);
  }
 err:
  return res;
}

static int feeddriver_get_message(mailsession * session,
    uint32_t num, mailmessage ** result)
{
  mailmessage * msg_info;
  int r;
  
  msg_info = mailmessage_new();
  if (msg_info == NULL)
    return MAIL_ERROR_MEMORY;
  
  r = mailmessage_init(msg_info, session, feed_message_driver, num, 0);
  if (r != MAIL_NO_ERROR) {
    mailmessage_free(msg_info);
    return r;
  }
  
  * result = msg_info;
  
  return MAIL_NO_ERROR;
}

static int feeddriver_get_message_by_uid(mailsession * session,
    const char * uid,
    mailmessage ** result)
{
#if 0
  uint32_t num;
  char * p;
  
  if (uid == NULL)
    return MAIL_ERROR_INVAL;
  
  num = strtoul(uid, &p, 10);
  if ((p == uid) || (* p != '\0'))
    return MAIL_ERROR_INVAL;
  
  return feeddriver_get_message(session, num, result);
#endif
  return MAIL_ERROR_INVAL;
 }
