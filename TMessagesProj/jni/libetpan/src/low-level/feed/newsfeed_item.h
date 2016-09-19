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

#ifndef NEWSFEED_ITEM_H
#define NEWSFEED_ITEM_H

#include <libetpan/newsfeed_types.h>

struct newsfeed_item * newsfeed_item_new(struct newsfeed * feed);
void newsfeed_item_free(struct newsfeed_item * item);

struct newsfeed * newsfeed_item_get_feed(struct newsfeed_item * item);

const char * newsfeed_item_get_url(struct newsfeed_item * item);
int newsfeed_item_set_url(struct newsfeed_item * item, const char * url);

const char * newsfeed_item_get_title(struct newsfeed_item * item);
int newsfeed_item_set_title(struct newsfeed_item * item, const char * title);

const char * newsfeed_item_get_summary(struct newsfeed_item * item);
int newsfeed_item_set_summary(struct newsfeed_item * item, const char * summary);

const char * newsfeed_item_get_text(struct newsfeed_item * item);
int newsfeed_item_set_text(struct newsfeed_item * item, const char * text);

const char * newsfeed_item_get_author(struct newsfeed_item * item);
int newsfeed_item_set_author(struct newsfeed_item * item, const char * author);

const char * newsfeed_item_get_id(struct newsfeed_item * item);
int newsfeed_item_set_id(struct newsfeed_item * item, const char * id);

time_t newsfeed_item_get_date_published(struct newsfeed_item * item);
void newsfeed_item_set_date_published(struct newsfeed_item * item, time_t date);

time_t newsfeed_item_get_date_modified(struct newsfeed_item * item);
void newsfeed_item_set_date_modified(struct newsfeed_item * item, time_t date);

struct newsfeed_item_enclosure * newsfeed_item_get_enclosure(struct newsfeed_item * item);
void newsfeed_item_set_enclosure(struct newsfeed_item * item,
    struct newsfeed_item_enclosure * enclosure);

#endif /* __FEEDITEM_H */
