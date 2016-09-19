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

#include "parser.h"

#include <string.h>
#include <ctype.h>
#include <stdio.h>
#include <errno.h>

#ifdef HAVE_ICONV
#include <iconv.h>
#endif

#include "newsfeed.h"

#include "newsfeed_private.h"
#include "parser_rss20.h"
#include "parser_rdf.h"
#include "parser_atom10.h"
#include "parser_atom03.h"

enum {
  FEED_TYPE_NONE,
  FEED_TYPE_RDF,
  FEED_TYPE_RSS_20,
  FEED_TYPE_ATOM_03,
  FEED_TYPE_ATOM_10
};

#ifdef HAVE_EXPAT
static void handler_set(XML_Parser parser, unsigned int type)
{
  if (parser == NULL)
    return;
  
  switch(type) {
  case FEED_TYPE_RSS_20:
    XML_SetElementHandler(parser,
        newsfeed_parser_rss20_start,
        newsfeed_parser_rss20_end);
    break;
    
  case FEED_TYPE_RDF:
    XML_SetElementHandler(parser,
        newsfeed_parser_rdf_start,
        newsfeed_parser_rdf_end);
    break;
    
  case FEED_TYPE_ATOM_10:
    XML_SetElementHandler(parser,
        newsfeed_parser_atom10_start,
        newsfeed_parser_atom10_end);
    break;

  case FEED_TYPE_ATOM_03:
    XML_SetElementHandler(parser,
        newsfeed_parser_atom03_start,
        newsfeed_parser_atom03_end);
    break;
  }
}

static void elparse_start_chooser(void * data,
    const char * el, const char ** attr)
{
  struct newsfeed_parser_context * ctx;
  unsigned int feedtype;
  XML_Parser parser;
  
  ctx = (struct newsfeed_parser_context *) data;
  feedtype = FEED_TYPE_NONE;
  
  if (ctx->depth == 0) {
    /* RSS 2.0 detected */
    if (strcasecmp(el, "rss") == 0) {
      feedtype = FEED_TYPE_RSS_20;
    }
    else if (strcasecmp(el, "rdf:RDF") == 0) {
      feedtype = FEED_TYPE_RDF;
    }
    else if (strcasecmp(el, "feed") == 0) {
      const char * version;
      
      /* ATOM feed detected, let's check version */
      version = newsfeed_parser_get_attribute_value(attr, "xmlns");
      if (version != NULL) {
        if (strcmp(version, "http://www.w3.org/2005/Atom") == 0)
          feedtype = FEED_TYPE_ATOM_10;
        else
          feedtype = FEED_TYPE_ATOM_03;
      }
    }
  }
  
  parser = ctx->parser;
  handler_set(parser, feedtype);
  
  ctx->depth ++;
}

static void elparse_end_dummy(void * data, const char * el)
{
  struct newsfeed_parser_context * ctx;
  
  ctx = (struct newsfeed_parser_context *) data;
  
  mmap_string_truncate(ctx->str, 0);
  
  ctx->depth --;
}

static void chparse(void * data, const char * s, int len)
{
  struct newsfeed_parser_context * ctx;
  char * pt;
  int i;
  int blank;
  
  blank = 1;
  ctx = (struct newsfeed_parser_context *) data;
  
  /* check if the string is blank, ... */
  for(i = 0, pt = (XML_Char *) s ; i < len ; i ++) {
    if ((* pt != ' ') && (* pt != '\t'))
      blank = 0;
    pt ++;
  }
  
  /* ... because we do not want to deal with blank strings */
  if (blank)
    return;
  
  for(i = 0, pt = (XML_Char *) s ; i < len ; i ++) {
    /* do not append newline as first char of our string */
    if ((* pt != '\n') || (ctx->str->len != 0)) {
      if (mmap_string_append_c(ctx->str, * pt) == NULL) {
        ctx->error = NEWSFEED_ERROR_MEMORY;
        return;
      }
      pt ++;
    }
  }
}

#define CHARSIZEUTF32 4

enum {
  LEP_ICONV_OK,
  LEP_ICONV_FAILED,
  LEP_ICONV_ILSEQ,
  LEP_ICONV_INVAL,
  LEP_ICONV_UNKNOWN
};

static int iconv_utf32_char(iconv_t cd, const char * inbuf, size_t insize,
     uint32_t * p_value)
{
#ifdef HAVE_ICONV
  size_t outsize;
  unsigned char outbuf[CHARSIZEUTF32];
  char * outbufp;
  int r;
  
  outsize = sizeof(outbuf);
  outbufp = (char *) outbuf;
#ifdef HAVE_ICONV_PROTO_CONST
  r = iconv(cd, (const char **) &inbuf, &insize,
      &outbufp, &outsize);
#else
  r = iconv(cd, (char **) &inbuf, &insize, &outbufp, &outsize);
#endif
  if (r == -1) {
    iconv (cd, 0, 0, 0, 0);
    switch (errno) {
    case EILSEQ:
      return LEP_ICONV_ILSEQ;
    case EINVAL:
      return LEP_ICONV_INVAL;
    default:
      return LEP_ICONV_UNKNOWN;
    }
  }
  else {
    uint32_t value;
    unsigned int i;
    
    if ((insize > 0) || (outsize > 0))
      return LEP_ICONV_FAILED;
    
    value = 0;
    for(i = 0 ; i < sizeof(outbuf) ; i ++) {
      value = (value << 8) + outbuf[i];
    }
    
    * p_value = value;
    return LEP_ICONV_OK;
  }
#else
  return LEP_ICONV_FAILED;
#endif
}

/* return 1 if conversion function is needed */
static int setup_unknown_encoding(const char * charset, XML_Encoding * info)
{
  iconv_t cd;
  int flag;
  char buf[4];
  unsigned int i;
  int r;

  cd = iconv_open("UTF-32BE", charset);
  if (cd == (iconv_t) (-1)) {
    return -1;
  }
  
  flag = 0;
  for (i = 0; i < 256; i++) {
    /* *** first char *** */
    uint32_t value;
    
    buf[0] = i;
    info->map[i] = 0;
    r = iconv_utf32_char(cd, buf, 1, &value);
    if (r == LEP_ICONV_OK) {
      info->map[i] = value;
    }
    else if (r != LEP_ICONV_INVAL) {
      /* do nothing */
    }
    else /* r == LEP_ICONV_INVAL */ {
      unsigned int j;
      
      for (j = 0; j < 256; j++) {
        /* *** second char *** */
        buf[1] = j;
        r = iconv_utf32_char(cd, buf, 2, &value);
        if (r == LEP_ICONV_OK) {
          flag = 1;
          info->map[i] = -2;
          break;
        }
        else if (r != LEP_ICONV_INVAL) {
          /* do nothing */
        }
        else /* r == LEP_ICONV_INVAL */ {
          unsigned int k;
          
          for (k = 0; k < 256; k++) {
            /* *** third char *** */
            buf[2] = k;
            r = iconv_utf32_char(cd, buf, 3, &value);
            if (r == LEP_ICONV_OK) {
              flag = 1;
              info->map[i] = -3;
              break;
            }
          }
        }
      }
    }
  }
  
  iconv_close(cd);
  
  return flag;
}

struct unknown_encoding_data {
  char * charset;
  iconv_t cd;
  char map[256];
};

static int unknown_encoding_convert(void * data, const char * s)
{
  int r;
  struct unknown_encoding_data * enc_data;
  size_t insize;
  uint32_t value;
  
  enc_data = data;
  
  if (s == NULL)
    goto err;
  
  insize = -enc_data->map[(unsigned char) s[0]];
  r = iconv_utf32_char(enc_data->cd, s, insize, &value);
  if (r != LEP_ICONV_OK)
    return -1;
  
  return value;
  
 err:
  return -1;
}

static void unknown_encoding_data_free(void * data)
{
  struct unknown_encoding_data * enc_data;
  
  enc_data = data;
  free(enc_data->charset);
  iconv_close(enc_data->cd);
  free(enc_data);
}

static int unknown_encoding_handler(void * encdata, const XML_Char * name,
    XML_Encoding * info)
{
  iconv_t cd;
  struct unknown_encoding_data * data;
  int result;
  unsigned int i;

  result = setup_unknown_encoding(name, info);
  if (result == 0) {
    info->data = NULL;
    info->convert = NULL;
    info->release = NULL;
    return XML_STATUS_OK;
  }
  
  cd = iconv_open("UTF-32BE", name);
  if (cd == (iconv_t) -1) {
    goto err;
  }
  
  data = malloc(sizeof(* data));
  if (data == NULL)
    goto close_iconv;
  
  data->charset = strdup(name);
  if (data->charset == NULL)
    goto free_data;
  
  data->cd = cd;
  for(i = 0 ; i < 256 ; i ++) {
    data->map[i] = info->map[i];
  }
  info->data = data;
  info->convert = unknown_encoding_convert;
  info->release = unknown_encoding_data_free;
  
  return XML_STATUS_OK;
  
 free_data:
  free(data);
 close_iconv:
  iconv_close(cd);
 err:
  return XML_STATUS_ERROR;
}
#endif

void newsfeed_parser_set_expat_handlers(struct newsfeed_parser_context * ctx)
{
#ifdef HAVE_EXPAT
  XML_Parser parser;
  
  parser = ctx->parser;
  
  XML_SetUserData(parser, (void *) ctx);
  
  XML_SetElementHandler(parser,
      elparse_start_chooser,
      elparse_end_dummy);
  
  XML_SetCharacterDataHandler(parser,
      chparse);
  
  XML_SetUnknownEncodingHandler(parser, unknown_encoding_handler, NULL);
#endif
}

size_t newsfeed_writefunc(void * ptr, size_t size, size_t nmemb, void * data)
{
#ifdef HAVE_EXPAT
  unsigned int len;
  struct newsfeed_parser_context * ctx;
  XML_Parser parser;
  
  ctx = data;
  len = size * nmemb;
  
  if (ctx->error != NEWSFEED_NO_ERROR) {
    return 0;
  }
  
  parser = ctx->parser;
  XML_Parse(parser, ptr, len, 0);
  
  if (ctx->error != NEWSFEED_NO_ERROR) {
    return 0;
  }
  
  return len;
#endif
  return 0;
}

const char * newsfeed_parser_get_attribute_value(const char ** attr,
    const char * name)
{
  unsigned int i;
  
  if ((attr == NULL) || (name == NULL))
    return NULL;
  
  for(i = 0 ; attr[i] != NULL && attr[i + 1] != NULL ; i += 2 ) {
    if (strcmp(attr[i], name) == 0)
      return attr[i + 1];
  }
  
  /* We haven't found anything. */
  return NULL;
}
