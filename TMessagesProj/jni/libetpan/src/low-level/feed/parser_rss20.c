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

#include "parser_rss20.h"

#include <string.h>
#include <stdlib.h>

#include "newsfeed.h"
#include "newsfeed_item.h"
#include "newsfeed_item_enclosure.h"
#include "date.h"
#include "mailimf.h"
#include "parser.h"
#include "newsfeed_private.h"

void newsfeed_parser_rss20_start(void *data, const char * el, const char ** attr)
{
  struct newsfeed_parser_context * ctx;
  int r;
  
  ctx = data;
  
  if (ctx->depth == 2) {
    if (strcasecmp(el, "item") == 0) {
      if (ctx->curitem != NULL)
        newsfeed_item_free(ctx->curitem);
      
      ctx->curitem = newsfeed_item_new(ctx->feed);
      if (ctx->curitem == NULL) {
        ctx->error = NEWSFEED_ERROR_MEMORY;
      }
    }
    else {
      ctx->location = 0;
    }
  }
  else if (ctx->depth == 3) {
    if (strcasecmp(el, "enclosure") == 0) {  /* Media enclosure */
      const char * strsize;
      const char * url;
      const char * type;
      struct newsfeed_item_enclosure * enclosure;
      size_t size;
      
      url = newsfeed_parser_get_attribute_value(attr, "url");
      type = newsfeed_parser_get_attribute_value(attr, "type");
      strsize = newsfeed_parser_get_attribute_value(attr, "length");
      size = 0;
      if (strsize != NULL )
        size = strtoul(strsize, NULL, 10);
      
      enclosure = newsfeed_item_enclosure_new();
      r = newsfeed_item_enclosure_set_url(enclosure, url);
      if (r < 0) {
        ctx->error = NEWSFEED_ERROR_MEMORY;
        return;
      }
      r = newsfeed_item_enclosure_set_type(enclosure, type);
      if (r < 0) {
        ctx->error = NEWSFEED_ERROR_MEMORY;
        return;
      }
      newsfeed_item_enclosure_set_size(enclosure, size);
      
      newsfeed_item_set_enclosure(ctx->curitem, enclosure);
    }
  }
  else {
    ctx->location = 0;
  }
  
  ctx->depth ++;
}

void newsfeed_parser_rss20_end(void * data, const char * el)
{
  struct newsfeed_parser_context * ctx;
  struct newsfeed * feed;
  char * text;
  int r;
  
  ctx = data;
  feed = ctx->feed;
  text = ctx->str->str;
  
  ctx->depth--;

  switch (ctx->depth) {
  case 0:
    if (strcasecmp(el, "rss") == 0) {
      /* we finished parsing the feed */
    }
    break;
    
  case 2:
    /* decide if we just received </item>, so we can
     * add a complete item to feed */
    if (strcasecmp(el, "item") == 0) {
      /* append the complete feed item */
      r = newsfeed_add_item(ctx->feed, ctx->curitem);
      if (r < 0) {
        ctx->error = NEWSFEED_ERROR_MEMORY;
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
    else if (strcasecmp(el, "admin:generatorAgent") == 0) {
      r = newsfeed_set_generator(feed, text);
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
    
  case 3:
    if ( ctx->curitem == NULL) {
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
    else if (strcasecmp(el, "author") == 0) {
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
    else if (strcasecmp(el, "guid") == 0) {
      r = newsfeed_item_set_id(ctx->curitem, text);
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
  
  mmap_string_truncate(ctx->str, 0);
}
