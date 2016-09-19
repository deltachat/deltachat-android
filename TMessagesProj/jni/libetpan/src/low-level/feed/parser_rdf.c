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

#include "parser_rdf.h"

#include <string.h>
#include <time.h>

#include "mailimf.h"
#include "newsfeed.h"
#include "newsfeed_item.h"
#include "newsfeed_private.h"
#include "date.h"

void newsfeed_parser_rdf_start(void * data, const char * el, const char ** attr)
{
  struct newsfeed_parser_context * ctx;
  
  ctx = data;
  if (ctx->depth == 1) {
    if (strcasecmp(el, "channel") == 0) {
      ctx->location = FEED_LOC_RDF_CHANNEL;
    }
    else if (strcasecmp(el, "item") == 0) {
      if (ctx->curitem != NULL)
        newsfeed_item_free(ctx->curitem);
      
      ctx->curitem = newsfeed_item_new(ctx->feed);
      if (ctx->curitem == NULL) {
        ctx->error = NEWSFEED_ERROR_MEMORY;
      }
      ctx->location = FEED_LOC_RDF_ITEM;
    }
    else {
      ctx->location = 0;
    }
  }
  
  ctx->depth ++;
}

void newsfeed_parser_rdf_end(void *data, const char *el)
{
  struct newsfeed_parser_context * ctx;
  struct newsfeed * feed;
  char * text;
  int r;
  
  ctx = data;
  feed = ctx->feed;
  text = ctx->str->str;
  
  ctx->depth --;
  
  switch (ctx->depth) {
  case 0:
    if (strcasecmp(el, "rdf") == 0) {
      /* we finished parsing the feed */
    }
    break;
    
  case 1:
    /* <item></item> block just ended, so ... */
    if (strcasecmp(el, "item") == 0) {
      /* add the complete feed item to our feed struct */
      r = newsfeed_add_item(feed, ctx->curitem);
      if (r < 0) {
        ctx->error = NEWSFEED_ERROR_MEMORY;
        return;
      }
      
      /* since it's in the linked list, lose this pointer */
      ctx->curitem = NULL;
    }
    break;
    
  case 2:
    switch(ctx->location) {
    case FEED_LOC_RDF_CHANNEL:
      /* We're inside introductory <channel></channel> */
      if (strcasecmp(el, "title") == 0) {
        r = newsfeed_set_title(feed, text);
        if (r < 0) {
          ctx->error = NEWSFEED_ERROR_MEMORY;
          return;
        }
      }
      else if (strcasecmp(el, "description") == 0) {
        r = newsfeed_set_description(feed, text);
        if (r < 0) {
          ctx->error = NEWSFEED_ERROR_MEMORY;
          return;
        }
      }
      else if (strcasecmp(el, "dc:language") == 0) {
        r = newsfeed_set_language(feed, text);
        if (r < 0) {
          ctx->error = NEWSFEED_ERROR_MEMORY;
          return;
        }
      }
      else if (strcasecmp(el, "dc:creator") == 0) {
        r = newsfeed_set_author(feed, text);
        if (r < 0) {
          ctx->error = NEWSFEED_ERROR_MEMORY;
          return;
        }
      }
      else if (strcasecmp(el, "dc:date") == 0) {
        time_t date;
        
        date = newsfeed_iso8601_date_parse(text);
        newsfeed_set_date(feed, date);
      }
      else if (strcasecmp(el, "pubDate") == 0) {
        time_t date;
      
        date = newsfeed_rfc822_date_parse(text);
        newsfeed_set_date(feed, date);
      }
      break;
      
    case FEED_LOC_RDF_ITEM:
      /* We're inside an <item></item> */
      if (ctx->curitem == NULL) {
        break;
      }
      
      /* decide which field did we just get */
      if (strcasecmp(el, "title") == 0) {
        r = newsfeed_item_set_title(ctx->curitem, text);
        if (r < 0) {
          ctx->error = NEWSFEED_ERROR_MEMORY;
          return;
        }
      }
      else if (strcasecmp(el, "dc:creator") == 0) {
        r = newsfeed_item_set_author(ctx->curitem, text);
        if (r < 0) {
          ctx->error = NEWSFEED_ERROR_MEMORY;
          return;
        }
      }
      else if (strcasecmp(el, "description") == 0) {
        r = newsfeed_item_set_text(ctx->curitem, text);
        if (r < 0) {
          ctx->error = NEWSFEED_ERROR_MEMORY;
          return;
        }
      }
      else if (strcasecmp(el, "content:encoded") == 0) {
        r = newsfeed_item_set_text(ctx->curitem, text);
        if (r < 0) {
          ctx->error = NEWSFEED_ERROR_MEMORY;
          return;
        }
      }
      else if (strcasecmp(el, "link") == 0) {
        r = newsfeed_item_set_url(ctx->curitem, text);
        if (r < 0) {
          ctx->error = NEWSFEED_ERROR_MEMORY;
          return;
        }
      }
      else if (strcasecmp(el, "dc:date") == 0) {
        time_t date;
        
        date = newsfeed_iso8601_date_parse(text);
        newsfeed_item_set_date_modified(ctx->curitem, date);
      }
      else if (strcasecmp(el, "pubDate") == 0) {
        time_t date;
      
        date = newsfeed_rfc822_date_parse(text);
        newsfeed_item_set_date_modified(ctx->curitem, date);
      }
      break;
    }
    
    break;
    
  }
  
  mmap_string_truncate(ctx->str, 0);
}
