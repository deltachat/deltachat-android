/*
 * libEtPan! -- a mail stuff library
 *
 * Copyright (C) 2001, 2005 - DINH Viet Hoa
 * Copyright (C) 2006 Andrej Kacian <andrej@kacian.sk>
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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "newsfeed.h"

#include <string.h>
#include <stdlib.h>

#include "newsfeed_private.h"
#include "mailstream.h"
#include "newsfeed_item.h"
#include "parser.h"

#ifdef HAVE_CURL
#include <curl/curl.h>
#endif

#ifdef LIBETPAN_REENTRANT
#	ifndef WIN32
#		include <pthread.h>
#	endif
#endif

#ifdef HAVE_CURL
static int curl_error_convert(int curl_res);
#endif

/* feed_new()
 * Initializes new Feed struct, setting its url and a default timeout. */
struct newsfeed * newsfeed_new(void)
{
  struct newsfeed * feed;
  
  feed = malloc(sizeof(* feed));
  if (feed == NULL)
    goto err;
  
  feed->feed_url = NULL;
  feed->feed_title = NULL;
  feed->feed_description = NULL;
  feed->feed_language = NULL;
  feed->feed_author = NULL;
  feed->feed_generator = NULL;
  feed->feed_item_list = carray_new(16);
  if (feed->feed_item_list == NULL)
    goto free;
  feed->feed_response_code = 0;
  feed->feed_timeout = 0;
  
  return feed;
  
 free:
  free(feed);
 err:
  return NULL;
}

void newsfeed_free(struct newsfeed * feed)
{
  unsigned int i;
  
  free(feed->feed_url);
  free(feed->feed_title);
  free(feed->feed_description);
  free(feed->feed_language);
  free(feed->feed_author);
  free(feed->feed_generator);
  
  for(i = 0 ; i < carray_count(feed->feed_item_list) ; i ++) {
    struct newsfeed_item * item;
    
    item = carray_get(feed->feed_item_list, i);
    newsfeed_item_free(item);
  }
  
  free(feed);
}

int newsfeed_get_response_code(struct newsfeed * feed)
{
  return feed->feed_response_code;
}

/* URL */
int newsfeed_set_url(struct newsfeed * feed, const char * url)
{
  if (url != feed->feed_url) {
    char * dup_url;
    
    if (url == NULL) {
      dup_url = NULL;
    }
    else {
      dup_url = strdup(url);
      if (dup_url == NULL)
        return -1;
    }
    
    free(feed->feed_url);
    feed->feed_url = dup_url;
  }
  
  return 0;
}

const char * newsfeed_get_url(struct newsfeed * feed)
{
  return feed->feed_url;
}

/* Title */
int newsfeed_set_title(struct newsfeed * feed, const char * title)
{
  if (title != feed->feed_title) {
    char * dup_title;
    
    if (title == NULL) {
      dup_title = NULL;
    }
    else {
      dup_title = strdup(title);
      if (dup_title == NULL)
        return -1;
    }
    
    free(feed->feed_title);
    feed->feed_title = dup_title;
  }
  
  return 0;
}

const char * newsfeed_get_title(struct newsfeed *feed)
{
  return feed->feed_title;
}

/* Description */
int newsfeed_set_description(struct newsfeed * feed, const char * description)
{
  if (description != feed->feed_description) {
    char * dup_description;
    
    if (description == NULL) {
      dup_description = NULL;
    }
    else {
      dup_description = strdup(description);
      if (dup_description == NULL)
        return -1;
    }
    
    free(feed->feed_description);
    feed->feed_description = dup_description;
  }
  
  return 0;
}

const char * newsfeed_get_description(struct newsfeed * feed)
{
  return feed->feed_description;
}

/* Language */
int newsfeed_set_language(struct newsfeed * feed, const char * language)
{
  if (language != feed->feed_language) {
    char * dup_language;
    
    if (language == NULL) {
      dup_language = NULL;
    }
    else {
      dup_language = strdup(language);
      if (dup_language == NULL)
        return -1;
    }
    
    free(feed->feed_language);
    feed->feed_language = dup_language;
  }
  
  return 0;
}

const char * newsfeed_get_language(struct newsfeed * feed)
{
  return feed->feed_language;
}

/* Author */
int newsfeed_set_author(struct newsfeed * feed, const char * author)
{
  if (author != feed->feed_author) {
    char * dup_author;
    
    if (author == NULL) {
      dup_author = NULL;
    }
    else {
      dup_author = strdup(author);
      if (dup_author == NULL)
        return -1;
    }
    
    free(feed->feed_author);
    feed->feed_author = dup_author;
  }
  
  return 0;
}

const char * newsfeed_get_author(struct newsfeed * feed)
{
  return feed->feed_author;
}

/* Generator */
int newsfeed_set_generator(struct newsfeed * feed, const char * generator)
{
  if (generator != feed->feed_generator) {
    char * dup_generator;
    
    if (generator == NULL) {
      dup_generator = NULL;
    }
    else {
      dup_generator = strdup(generator);
      if (dup_generator == NULL)
        return -1;
    }
    
    free(feed->feed_generator);
    feed->feed_generator = dup_generator;
  }
  
  return 0;
}

const char * newsfeed_get_generator(struct newsfeed * feed)
{
  return feed->feed_generator;
}

void newsfeed_set_date(struct newsfeed * feed, time_t date)
{
  feed->feed_date = date;
}

time_t newsfeed_get_date(struct newsfeed * feed)
{
  return feed->feed_date;
}

/* Returns nth item from feed. */
unsigned int newsfeed_item_list_get_count(struct newsfeed * feed)
{
  return carray_count(feed->feed_item_list);
}

struct newsfeed_item * newsfeed_get_item(struct newsfeed * feed, unsigned int n)
{
  return carray_get(feed->feed_item_list, n);
}

/* feed_update()
 * Takes initialized feed with url set, fetches the feed from this url,
 * updates rest of Feed struct members and returns HTTP response code
 * we got from url's server. */
int newsfeed_update(struct newsfeed * feed, time_t last_update)
{
#if (defined(HAVE_CURL) && defined(HAVE_EXPAT))
  CURL * eh;
  CURLcode curl_res;
  struct newsfeed_parser_context * feed_ctx;
  unsigned int res;
  unsigned int timeout_value;
  long response_code;
  
  if (feed->feed_url == NULL) {
    res = NEWSFEED_ERROR_BADURL;
    goto err;
  }
  
  /* Init curl before anything else. */
  eh = curl_easy_init();
  if (eh == NULL) {
    res = NEWSFEED_ERROR_MEMORY;
    goto err;
  }
  
  /* Curl initialized, create parser context now. */
  feed_ctx = malloc(sizeof(* feed_ctx));
  if (feed_ctx == NULL) {
    res = NEWSFEED_ERROR_MEMORY;
    goto free_eh;
  }
  
  feed_ctx->parser = XML_ParserCreate(NULL);
  if (feed_ctx->parser == NULL) {
    res = NEWSFEED_ERROR_MEMORY;
    goto free_ctx;
  }
  feed_ctx->depth = 0;
  feed_ctx->str = mmap_string_sized_new(256);
  if (feed_ctx->str == NULL) {
    res = NEWSFEED_ERROR_MEMORY;
    goto free_praser;
  }
  feed_ctx->feed = feed;
  feed_ctx->location = 0;
  feed_ctx->curitem = NULL;
  feed_ctx->error = NEWSFEED_NO_ERROR;
  
  /* Set initial expat handlers, which will take care of choosing
   * correct parser later. */
  newsfeed_parser_set_expat_handlers(feed_ctx);
  
  if (feed->feed_timeout != 0)
    timeout_value = feed->feed_timeout;
  else
    timeout_value = mailstream_network_delay.tv_sec;
  
  curl_easy_setopt(eh, CURLOPT_URL, feed->feed_url);
  curl_easy_setopt(eh, CURLOPT_NOPROGRESS, 1);
#ifdef CURLOPT_MUTE
  curl_easy_setopt(eh, CURLOPT_MUTE, 1);
#endif
  curl_easy_setopt(eh, CURLOPT_WRITEFUNCTION, newsfeed_writefunc);
  curl_easy_setopt(eh, CURLOPT_WRITEDATA, feed_ctx);
  curl_easy_setopt(eh, CURLOPT_FOLLOWLOCATION, 1);
  curl_easy_setopt(eh, CURLOPT_MAXREDIRS, 3);
  curl_easy_setopt(eh, CURLOPT_TIMEOUT, timeout_value);
  curl_easy_setopt(eh, CURLOPT_NOSIGNAL, 1);
  curl_easy_setopt(eh, CURLOPT_USERAGENT, "libEtPan!");
  
  /* Use HTTP's If-Modified-Since feature, if application provided
   * the timestamp of last update. */
  if (last_update != -1) {
    curl_easy_setopt(eh, CURLOPT_TIMECONDITION,
        CURL_TIMECOND_IFMODSINCE);
    curl_easy_setopt(eh, CURLOPT_TIMEVALUE, last_update);
  }
        
#if LIBCURL_VERSION_NUM >= 0x070a00
  curl_easy_setopt(eh, CURLOPT_SSL_VERIFYPEER, 0);
  curl_easy_setopt(eh, CURLOPT_SSL_VERIFYHOST, 0);
#endif

  curl_res = curl_easy_perform(eh);
  if (curl_res != 0) {
    res = curl_error_convert(curl_res);
    goto free_str;
  }
  
  curl_easy_getinfo(eh, CURLINFO_RESPONSE_CODE, &response_code);
  
  curl_easy_cleanup(eh);
  
  if (feed_ctx->error != NEWSFEED_NO_ERROR) {
    res = feed_ctx->error;
    goto free_str;
  }
  
  /* Cleanup, we should be done. */
  mmap_string_free(feed_ctx->str);
  XML_ParserFree(feed_ctx->parser);
  free(feed_ctx);
  
  feed->feed_response_code = (int) response_code;
  
  return NEWSFEED_NO_ERROR;;
  
 free_str:
  mmap_string_free(feed_ctx->str);
 free_praser:
  XML_ParserFree(feed_ctx->parser);
 free_ctx:
  free(feed_ctx);
 free_eh:
  curl_easy_cleanup(eh);
 err:
  return res;
#else
  return NEWSFEED_ERROR_INTERNAL;
#endif
}

int newsfeed_add_item(struct newsfeed * feed, struct newsfeed_item * item)
{
  return carray_add(feed->feed_item_list, item, NULL);
}

#ifdef HAVE_CURL
static int curl_error_convert(int curl_res)
{
  switch (curl_res) {
  case CURLE_OK:
    return NEWSFEED_NO_ERROR;
    
  case CURLE_UNSUPPORTED_PROTOCOL:
    return NEWSFEED_ERROR_UNSUPPORTED_PROTOCOL;
    
  case CURLE_FAILED_INIT:
  case CURLE_LIBRARY_NOT_FOUND:
  case CURLE_FUNCTION_NOT_FOUND:
  case CURLE_BAD_FUNCTION_ARGUMENT:
  case CURLE_BAD_CALLING_ORDER:
  case CURLE_UNKNOWN_TELNET_OPTION:
  case CURLE_TELNET_OPTION_SYNTAX:
  case CURLE_OBSOLETE:
  case CURLE_GOT_NOTHING:
  case CURLE_INTERFACE_FAILED:
  case CURLE_SHARE_IN_USE:
  case CURL_LAST:
    return NEWSFEED_ERROR_INTERNAL;
    
  case CURLE_URL_MALFORMAT:
  case CURLE_URL_MALFORMAT_USER:
  case CURLE_MALFORMAT_USER:
    return NEWSFEED_ERROR_BADURL;
    
  case CURLE_COULDNT_RESOLVE_PROXY:
    return NEWSFEED_ERROR_RESOLVE_PROXY;
    
  case CURLE_COULDNT_RESOLVE_HOST:
    return NEWSFEED_ERROR_RESOLVE_HOST;
    
  case CURLE_COULDNT_CONNECT:
    return NEWSFEED_ERROR_CONNECT;
    
  case CURLE_FTP_WEIRD_SERVER_REPLY:
  case CURLE_FTP_WEIRD_PASS_REPLY:
  case CURLE_FTP_WEIRD_USER_REPLY:
  case CURLE_FTP_WEIRD_PASV_REPLY:
  case CURLE_FTP_WEIRD_227_FORMAT:
    return NEWSFEED_ERROR_PROTOCOL;
    
  case CURLE_FTP_ACCESS_DENIED:
    return NEWSFEED_ERROR_ACCESS;
    
  case CURLE_FTP_USER_PASSWORD_INCORRECT:
  case CURLE_BAD_PASSWORD_ENTERED:
  case CURLE_LOGIN_DENIED:
    return NEWSFEED_ERROR_AUTHENTICATION;
    
  case CURLE_FTP_CANT_GET_HOST:
  case CURLE_FTP_CANT_RECONNECT:
  case CURLE_FTP_COULDNT_SET_BINARY:
  case CURLE_FTP_QUOTE_ERROR:
  case CURLE_FTP_COULDNT_SET_ASCII:
  case CURLE_FTP_PORT_FAILED:
  case CURLE_FTP_COULDNT_USE_REST:
  case CURLE_FTP_COULDNT_GET_SIZE:
    return NEWSFEED_ERROR_FTP;
    
  case CURLE_PARTIAL_FILE:
    return NEWSFEED_ERROR_PARTIAL_FILE;
    
  case CURLE_FTP_COULDNT_RETR_FILE:
  case CURLE_FILE_COULDNT_READ_FILE:
  case CURLE_BAD_DOWNLOAD_RESUME:
  case CURLE_FILESIZE_EXCEEDED:
    return NEWSFEED_ERROR_FETCH;
    
  case CURLE_FTP_COULDNT_STOR_FILE:
  case CURLE_HTTP_POST_ERROR:
    return NEWSFEED_ERROR_PUT;
    
  case CURLE_OUT_OF_MEMORY:
    return NEWSFEED_ERROR_MEMORY;
    
  case CURLE_OPERATION_TIMEOUTED:
    return NEWSFEED_ERROR_STREAM;
    
  case CURLE_HTTP_RANGE_ERROR:
  case CURLE_HTTP_RETURNED_ERROR:
  case CURLE_TOO_MANY_REDIRECTS:
  case CURLE_BAD_CONTENT_ENCODING:
    return NEWSFEED_ERROR_HTTP;
    
  case CURLE_LDAP_CANNOT_BIND:
  case CURLE_LDAP_SEARCH_FAILED:
  case CURLE_LDAP_INVALID_URL:
    return NEWSFEED_ERROR_LDAP;

  case CURLE_ABORTED_BY_CALLBACK:
    return NEWSFEED_ERROR_CANCELLED;
    
  case CURLE_FTP_WRITE_ERROR:
  case CURLE_SEND_ERROR:
  case CURLE_RECV_ERROR:
  case CURLE_READ_ERROR:
  case CURLE_WRITE_ERROR:
  case CURLE_SEND_FAIL_REWIND:
    return NEWSFEED_ERROR_STREAM;
    
  case CURLE_SSL_CONNECT_ERROR:
  case CURLE_SSL_PEER_CERTIFICATE:
  case CURLE_SSL_ENGINE_NOTFOUND:
  case CURLE_SSL_ENGINE_SETFAILED:
  case CURLE_SSL_CERTPROBLEM:
  case CURLE_SSL_CIPHER:
  case CURLE_SSL_CACERT:
  case CURLE_FTP_SSL_FAILED:
  case CURLE_SSL_ENGINE_INITFAILED:
    return NEWSFEED_ERROR_SSL;
    
  default:
    return NEWSFEED_ERROR_INTERNAL;
  }
}
#endif

void newsfeed_set_timeout(struct newsfeed * feed, unsigned int timeout)
{
  feed->feed_timeout = timeout;
}

unsigned int newsfeed_get_timeout(struct newsfeed * feed)
{
  return feed->feed_timeout;
}
