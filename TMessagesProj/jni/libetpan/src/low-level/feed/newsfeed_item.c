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

#include "newsfeed_item.h"

#include "newsfeed.h"
#include "newsfeed_item_enclosure.h"

#include <stdlib.h>
#include <string.h>

/* newsfeed_item_new()
 * Initializes a new empty newsfeed_item struct, setting its parent feed,
 * if supplied. */
struct newsfeed_item * newsfeed_item_new(struct newsfeed * feed)
{
  struct newsfeed_item * item;
  
  item = malloc(sizeof(* item));
  if (item == NULL)
    return NULL;
  
  item->fi_url = NULL;
  item->fi_title = NULL;
  item->fi_summary = NULL;
  item->fi_text = NULL;
  item->fi_author = NULL;
  item->fi_id = NULL;
  
  item->fi_date_published = 0;
  item->fi_date_modified = 0;
  item->fi_enclosure = NULL;
  
  item->fi_feed = feed;
  
  return item;
}

void newsfeed_item_free(struct newsfeed_item * item)
{
  if (item->fi_enclosure != NULL)
    newsfeed_item_enclosure_free(item->fi_enclosure);
  free(item->fi_id);
  free(item->fi_author);
  free(item->fi_text);
  free(item->fi_summary);
  free(item->fi_title);
  free(item->fi_url);
  
  free(item);
}

struct newsfeed * newsfeed_item_get_feed(struct newsfeed_item * item)
{
  return item->fi_feed;
}

const char * newsfeed_item_get_url(struct newsfeed_item * item)
{
  return item->fi_url;
}

int newsfeed_item_set_url(struct newsfeed_item *item, const char * url)
{
  if (url != item->fi_url) {
    char * dup_url;
    
    if (url == NULL) {
      dup_url = NULL;
    }
    else {
      dup_url = strdup(url);
      if (dup_url == NULL)
        return -1;
    }
    
    free(item->fi_url);
    item->fi_url = dup_url;
  }
  
  return 0;
}

const char * newsfeed_item_get_title(struct newsfeed_item * item)
{
  return item->fi_title;
}

int newsfeed_item_set_title(struct newsfeed_item * item, const char * title)
{
  if (title != item->fi_title) {
    char * dup_title;
    
    if (title == NULL) {
      dup_title = NULL;
    }
    else {
      dup_title = strdup(title);
      if (dup_title == NULL)
        return -1;
    }
    
    free(item->fi_title);
    item->fi_title = dup_title;
  }
  
  return 0;
}

const char * newsfeed_item_get_summary(struct newsfeed_item * item)
{
  return item->fi_summary;
}

int newsfeed_item_set_summary(struct newsfeed_item * item, const char * summary)
{
  if (summary != item->fi_summary) {
    char * dup_summary;
    
    if (summary == NULL) {
      dup_summary = NULL;
    }
    else {
      dup_summary = strdup(summary);
      if (dup_summary == NULL)
        return -1;
    }
    
    free(item->fi_summary);
    item->fi_summary = dup_summary;
  }
  
  return 0;
}

const char * newsfeed_item_get_text(struct newsfeed_item * item)
{
  return item->fi_text;
}

int newsfeed_item_set_text(struct newsfeed_item * item, const char * text)
{
  if (text != item->fi_text) {
    char * dup_text;
    
    if (text == NULL) {
      dup_text = NULL;
    }
    else {
      dup_text = strdup(text);
      if (dup_text == NULL)
        return -1;
    }
    
    free(item->fi_text);
    item->fi_text = dup_text;
  }
  
  return 0;
}

const char * newsfeed_item_get_author(struct newsfeed_item * item)
{
  return item->fi_author;
}

int newsfeed_item_set_author(struct newsfeed_item * item, const char * author)
{
  if (author != item->fi_author) {
    char * dup_author;
    
    if (author == NULL) {
      dup_author = NULL;
    }
    else {
      dup_author = strdup(author);
      if (dup_author == NULL)
        return -1;
    }
    
    free(item->fi_author);
    item->fi_author = dup_author;
  }
  
  return 0;
}

const char * newsfeed_item_get_id(struct newsfeed_item * item)
{
  return item->fi_id;
}

int newsfeed_item_set_id(struct newsfeed_item * item, const char * id)
{
  if (id != item->fi_id) {
    char * dup_id;
    
    if (id == NULL) {
      dup_id = NULL;
    }
    else {
      dup_id = strdup(id);
      if (dup_id == NULL)
        return -1;
    }
    
    free(item->fi_id);
    item->fi_id = dup_id;
  }
  
  return 0;
}

time_t newsfeed_item_get_date_published(struct newsfeed_item * item)
{
  return item->fi_date_published;
}

void newsfeed_item_set_date_published(struct newsfeed_item * item, time_t date)
{
  item->fi_date_published = date;
}

time_t newsfeed_item_get_date_modified(struct newsfeed_item * item)
{
  return item->fi_date_modified;
}

void newsfeed_item_set_date_modified(struct newsfeed_item * item, time_t date)
{
  item->fi_date_modified = date;
}

struct newsfeed_item_enclosure * newsfeed_item_get_enclosure(struct newsfeed_item * item)
{
  return item->fi_enclosure;
}

void newsfeed_item_set_enclosure(struct newsfeed_item * item,
    struct newsfeed_item_enclosure * enclosure)
{
  item->fi_enclosure = enclosure;
}
