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
 * $Id: feedstorage.c,v 1.1 2007/01/18 09:15:01 hoa Exp $
 */

#ifdef HAVE_CONFIG_H
#	include <config.h>
#endif

#include "feedstorage.h"

#include <stdlib.h>
#include <string.h>

#include "maildriver.h"
#include "feeddriver.h"
#include "mailstorage_tools.h"
#include "mail.h"

/* feed storage */

#define FEED_DEFAULT_PORT  119
#define FEEDS_DEFAULT_PORT 563

static int feed_mailstorage_connect(struct mailstorage * storage);
static int feed_mailstorage_get_folder_session(struct mailstorage * storage,
    char * pathname, mailsession ** result);
static void feed_mailstorage_uninitialize(struct mailstorage * storage);

static mailstorage_driver feed_mailstorage_driver = {
  /* sto_name               */ "feed",
  /* sto_connect            */ feed_mailstorage_connect,
  /* sto_get_folder_session */ feed_mailstorage_get_folder_session,
  /* sto_uninitialize       */ feed_mailstorage_uninitialize,
};

int feed_mailstorage_init(struct mailstorage * storage,
    const char * feed_url,
    int feed_cached, const char * feed_cache_directory,
    const char * feed_flags_directory)
{
  struct feed_mailstorage * feed_storage;
  int res;
  
  feed_storage = malloc(sizeof(* feed_storage));
  if (feed_storage == NULL) {
    res = MAIL_ERROR_MEMORY;
    goto err;
  }

  feed_storage->feed_url = strdup(feed_url);
  if (feed_storage->feed_url == NULL) {
    res = MAIL_ERROR_MEMORY;
    goto free;
  }

  feed_storage->feed_cached = feed_cached;

  if (feed_cached && (feed_cache_directory != NULL) &&
      (feed_flags_directory != NULL)) {
    feed_storage->feed_cache_directory = strdup(feed_cache_directory);
    if (feed_storage->feed_cache_directory == NULL) {
      res = MAIL_ERROR_MEMORY;
      goto free_url;
    }
    feed_storage->feed_flags_directory = strdup(feed_flags_directory);
    if (feed_storage->feed_flags_directory == NULL) {
      res = MAIL_ERROR_MEMORY;
      goto free_cache_directory;
    }
  }
  else {
    feed_storage->feed_cached = FALSE;
    feed_storage->feed_cache_directory = NULL;
    feed_storage->feed_flags_directory = NULL;
  }

  storage->sto_data = feed_storage;
  storage->sto_driver = &feed_mailstorage_driver;

  return MAIL_NO_ERROR;

 free_cache_directory:
  free(feed_storage->feed_cache_directory);
 free_url:
  free(feed_storage->feed_url);
 free:
  free(feed_storage);
 err:
  return res;
}

static void feed_mailstorage_uninitialize(struct mailstorage * storage)
{
  struct feed_mailstorage * feed_storage;

  feed_storage = storage->sto_data;

  if (feed_storage->feed_flags_directory != NULL)
    free(feed_storage->feed_flags_directory);
  if (feed_storage->feed_cache_directory != NULL)
    free(feed_storage->feed_cache_directory);
  free(feed_storage->feed_url);
  free(feed_storage);
  
  storage->sto_data = NULL;
}

static int feed_mailstorage_connect(struct mailstorage * storage)
{
  struct feed_mailstorage * feed_storage;
  mailsession_driver * driver;
  int r;
  int res;
  mailsession * session;

  feed_storage = storage->sto_data;
  driver = feed_session_driver;
  
  session = mailsession_new(driver);
  if (session == NULL) {
    res = MAIL_ERROR_MEMORY;
    goto err;
  }
  
  r = mailsession_connect_path(session, feed_storage->feed_url);
  switch (r) {
  case MAIL_NO_ERROR_NON_AUTHENTICATED:
  case MAIL_NO_ERROR_AUTHENTICATED:
  case MAIL_NO_ERROR:
    break;
  default:
    res = r;
    goto free;
  }
  
  storage->sto_session = session;

  return MAIL_NO_ERROR;

 free:
  mailsession_free(session);
 err:
  return res;
}

static int feed_mailstorage_get_folder_session(struct mailstorage * storage,
    char * pathname, mailsession ** result)
{
  * result = storage->sto_session;
  
  return MAIL_NO_ERROR;
}
