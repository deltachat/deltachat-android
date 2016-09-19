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
#ifndef NEWSFEED_TYPES_H

#define NEWSFEED_TYPES_H

#include <libetpan/carray.h>
#include <sys/types.h>

enum {
  NEWSFEED_NO_ERROR = 0,
  NEWSFEED_ERROR_CANCELLED,
  NEWSFEED_ERROR_INTERNAL,
  NEWSFEED_ERROR_BADURL,
  NEWSFEED_ERROR_RESOLVE_PROXY,
  NEWSFEED_ERROR_RESOLVE_HOST,
  NEWSFEED_ERROR_CONNECT,
  NEWSFEED_ERROR_STREAM,
  NEWSFEED_ERROR_PROTOCOL,
  NEWSFEED_ERROR_PARSE,
  NEWSFEED_ERROR_ACCESS,
  NEWSFEED_ERROR_AUTHENTICATION,
  NEWSFEED_ERROR_FTP,
  NEWSFEED_ERROR_PARTIAL_FILE,
  NEWSFEED_ERROR_FETCH,
  NEWSFEED_ERROR_HTTP,
  NEWSFEED_ERROR_FILE,
  NEWSFEED_ERROR_PUT,
  NEWSFEED_ERROR_MEMORY,
  NEWSFEED_ERROR_SSL,
  NEWSFEED_ERROR_LDAP,
  NEWSFEED_ERROR_UNSUPPORTED_PROTOCOL
};

struct newsfeed {
  char * feed_url;
  char * feed_title;
  char * feed_description;
  char * feed_language;
  char * feed_author;
  char * feed_generator;
  time_t feed_date;
  carray * feed_item_list;
  int feed_response_code;
  
  unsigned int feed_timeout;
};

struct newsfeed_item {
  char * fi_url;
  char * fi_title;
  char * fi_summary;
  char * fi_text;
  char * fi_author;
  char * fi_id;
  time_t fi_date_published;
  time_t fi_date_modified;
  struct newsfeed * fi_feed; /* owner */
  struct newsfeed_item_enclosure * fi_enclosure;
};

struct newsfeed_item_enclosure {
  char * fie_url;
  char * fie_type;
  size_t fie_size;
};

#endif
