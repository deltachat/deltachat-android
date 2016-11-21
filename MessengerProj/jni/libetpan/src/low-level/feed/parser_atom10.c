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

#include "parser_atom10.h"

#include <string.h>

#include "newsfeed.h"
#include "newsfeed_item.h"
#include "newsfeed_private.h"
#include "date.h"
#include "parser.h"

enum {
  FEED_LOC_ATOM10_NONE,
  FEED_LOC_ATOM10_ENTRY,
  FEED_LOC_ATOM10_AUTHOR
};

void newsfeed_parser_atom10_start(void * data, const char * el, const char ** attr)
{
  struct newsfeed_parser_context * ctx;
  int r;
  
  ctx = data;
  
  if (ctx->depth == 1) {
    if (strcasecmp(el, "entry") == 0) {
      /* Start of new feed item found.
       * Create a new FeedItem, freeing the one we already have, if any. */
      if( ctx->curitem != NULL )
        newsfeed_item_free(ctx->curitem);
      
      ctx->curitem = newsfeed_item_new(ctx->feed);
      if (ctx->curitem == NULL) {
        ctx->error = NEWSFEED_ERROR_MEMORY;
        return;
      }
      
      ctx->location = FEED_LOC_ATOM10_ENTRY;
    }
    else if (strcasecmp(el, "author") == 0) {
      /* Start of author info for the feed found.
       * Set correct location. */
      ctx->location = FEED_LOC_ATOM10_AUTHOR;
    }
    else {
      ctx->location = FEED_LOC_ATOM10_NONE;
    }
  }
  else if (ctx->depth == 2) {
    if (strcasecmp(el, "author") == 0) {
      /* Start of author info for current feed item.
       * Set correct location. */
      ctx->location = FEED_LOC_ATOM10_AUTHOR;
    }
    else if (strcasecmp(el, "link") == 0) {
      const char * url;
      
      /* Capture item URL, from the "url" XML attribute. */
      url = newsfeed_parser_get_attribute_value(attr, "href");
      r = newsfeed_item_set_url(ctx->curitem, url);
      if (r < 0) {
        ctx->error = NEWSFEED_ERROR_MEMORY;
        return;
      }
      ctx->location = FEED_LOC_ATOM10_ENTRY;
    }
    else {
      ctx->location = FEED_LOC_ATOM10_ENTRY;
    }
  }
  
  ctx->depth ++;
}

void newsfeed_parser_atom10_end(void *data, const char * el)
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
    if (strcasecmp(el, "feed") == 0) {
      /* We have finished parsing the feed. */
    }
    break;
    
  case 1:
    /* decide if we just received </entry>, so we can
     * add a complete item to feed */
    if (strcasecmp(el, "entry") == 0) {
      /* append the complete feed item */
      r = newsfeed_add_item(ctx->feed, ctx->curitem);
      if (r < 0) {
        ctx->error = NEWSFEED_ERROR_MEMORY;
        return;
      }
      
      /* since it's in the linked list, lose this pointer */
      ctx->curitem = NULL;
    }
    else if (strcasecmp(el, "title") == 0) {
      /* so it wasn't end of item */
      r = newsfeed_set_title(feed, text);
      if (r < 0) {
        ctx->error = NEWSFEED_ERROR_MEMORY;
        return;
      }
    }
    else if (strcasecmp(el, "summary") == 0) {
      r = newsfeed_set_description(feed, text);
      if (r < 0) {
        ctx->error = NEWSFEED_ERROR_MEMORY;
        return;
      }
    }
    else if (strcasecmp(el, "updated") == 0) {
      time_t date;
      
      date = newsfeed_iso8601_date_parse(text);
      newsfeed_set_date(feed, date);
    }
    /* TODO: add more later */
    break;

  case 2:
    if( ctx->curitem == NULL )
      break;

    switch(ctx->location) {
    case FEED_LOC_ATOM10_ENTRY:
      /* We're in feed/entry */
      if (strcasecmp(el, "title") == 0) {
        r = newsfeed_item_set_title(ctx->curitem, text);
        if (r < 0) {
          ctx->error = NEWSFEED_ERROR_MEMORY;
          return;
        }
      }
      else if (strcasecmp(el, "summary") == 0) {
        r = newsfeed_item_set_summary(ctx->curitem, text);
        if (r < 0) {
          ctx->error = NEWSFEED_ERROR_MEMORY;
          return;
        }
      }
      else if (strcasecmp(el, "content") == 0) {
        r = newsfeed_item_set_text(ctx->curitem, text);
        if (r < 0) {
          ctx->error = NEWSFEED_ERROR_MEMORY;
          return;
        }
      }
      else if (strcasecmp(el, "id") == 0) {
        r = newsfeed_item_set_id(ctx->curitem, text);
        if (r < 0) {
          ctx->error = NEWSFEED_ERROR_MEMORY;
          return;
        }
      }
      else if (strcasecmp(el, "issued") == 0) {
        time_t date;
        
        date = newsfeed_iso8601_date_parse(text);
        newsfeed_item_set_date_published(ctx->curitem, date);
      }
      else if (strcasecmp(el, "updated") == 0) {
        time_t date;
        
        date = newsfeed_iso8601_date_parse(text);
        newsfeed_item_set_date_modified(ctx->curitem, date);
      }
      break;
      
    case FEED_LOC_ATOM10_AUTHOR:
      /* We're in feed/author */
      if (strcasecmp(el, "name") == 0) {
        r = newsfeed_item_set_author(ctx->curitem, text);
        if (r < 0) {
          ctx->error = NEWSFEED_ERROR_MEMORY;
          return;
        }
      }
      /* TODO: construct a "Na Me <add@dre.ss>" string
       * from available tags */
      
      break;
    }
    
    break;
    
  case 3:
    if (ctx->curitem == NULL)
      break;
    
    if (ctx->location == FEED_LOC_ATOM10_AUTHOR) {
      /* We're in feed/entry/author */
      if (strcasecmp(el, "name") == 0) {
        r = newsfeed_item_set_author(ctx->curitem, text);
        if (r < 0) {
          ctx->error = NEWSFEED_ERROR_MEMORY;
          return;
        }
      }
    }
    
    break;
  }
  
  mmap_string_truncate(ctx->str, 0);
}
