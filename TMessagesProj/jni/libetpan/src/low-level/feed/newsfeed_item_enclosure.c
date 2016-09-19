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
#include "newsfeed_item_enclosure.h"

#include <string.h>
#include <stdlib.h>

struct newsfeed_item_enclosure * newsfeed_item_enclosure_new(void)
{
  struct newsfeed_item_enclosure * enclosure;
  
  enclosure = malloc(sizeof(* enclosure));
  if (enclosure == NULL)
    goto err;
  
  enclosure->fie_url = NULL;
  enclosure->fie_type = NULL;
  enclosure->fie_size = 0;
  
  return enclosure;
  
 err:
  return NULL;
}

void newsfeed_item_enclosure_free(struct newsfeed_item_enclosure * enclosure)
{
  free(enclosure->fie_url);
  free(enclosure->fie_type);
  free(enclosure);
}

char * newsfeed_item_enclosure_get_url(struct newsfeed_item_enclosure * enclosure)
{
  return enclosure->fie_url;
}

int newsfeed_item_enclosure_set_url(struct newsfeed_item_enclosure * enclosure,
    const char * url)
{
  if (url != enclosure->fie_url) {
    char * dup_url;
    
    if (url == NULL) {
      dup_url = NULL;
    }
    else {
      dup_url = strdup(url);
      if (dup_url == NULL)
        return -1;
    }
    
    free(enclosure->fie_url);
    enclosure->fie_url = dup_url;
  }
  
  return 0;
}

char * newsfeed_item_enclosure_get_type(struct newsfeed_item_enclosure * enclosure)
{
  return enclosure->fie_type;
}

int newsfeed_item_enclosure_set_type(struct newsfeed_item_enclosure * enclosure,
    const char * type)
{
  if (type != enclosure->fie_type) {
    char * dup_type;
    
    if (type == NULL) {
      dup_type = NULL;
    }
    else {
      dup_type = strdup(type);
      if (dup_type == NULL)
        return -1;
    }
    
    free(enclosure->fie_type);
    enclosure->fie_type = dup_type;
  }
  
  return 0;
}

size_t newsfeed_item_enclosure_get_size(struct newsfeed_item_enclosure * enclosure)
{
  return enclosure->fie_size;
}

void newsfeed_item_enclosure_set_size(struct newsfeed_item_enclosure * enclosure,
    size_t size)
{
  enclosure->fie_size = size;
}
