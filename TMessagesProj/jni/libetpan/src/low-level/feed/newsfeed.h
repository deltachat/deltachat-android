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

#ifndef NEWSFEED_H
#define NEWSFEED_H

#include <libetpan/newsfeed_types.h>
#include <libetpan/newsfeed_item.h>
#include <libetpan/newsfeed_item_enclosure.h>

struct newsfeed * newsfeed_new(void);
void newsfeed_free(struct newsfeed * feed);

int newsfeed_get_response_code(struct newsfeed * feed);

int newsfeed_set_url(struct newsfeed * feed, const char * url);
const char * newsfeed_get_url(struct newsfeed * feed);

int newsfeed_set_title(struct newsfeed * feed, const char * title);
const char * newsfeed_get_title(struct newsfeed * feed);

int newsfeed_set_description(struct newsfeed * feed, const char * description);
const char * newsfeed_get_description(struct newsfeed * feed);

int newsfeed_set_language(struct newsfeed * feed, const char * language);
const char * newsfeed_get_language(struct newsfeed * feed);

int newsfeed_set_author(struct newsfeed * feed, const char * author);
const char * newsfeed_get_author(struct newsfeed * feed);

int newsfeed_set_generator(struct newsfeed * feed, const char * generator);
const char * newsfeed_get_generator(struct newsfeed * feed);

unsigned int newsfeed_item_list_get_count(struct newsfeed * feed);
struct newsfeed_item * newsfeed_get_item(struct newsfeed * feed, unsigned int n);

void newsfeed_set_date(struct newsfeed * feed, time_t date);
time_t newsfeed_get_date(struct newsfeed * feed);

void newsfeed_set_timeout(struct newsfeed * feed, unsigned int timeout);
unsigned int newsfeed_get_timeout(struct newsfeed * feed);

int newsfeed_add_item(struct newsfeed * feed, struct newsfeed_item * item);

int newsfeed_update(struct newsfeed * feed, time_t last_update);

#endif /* NEWSFEED_H */
