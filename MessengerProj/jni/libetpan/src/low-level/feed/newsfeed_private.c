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
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "newsfeed_private.h"

#include <string.h>

#include "mailimf.h"
#include "timeutils.h"

static inline time_t get_date(struct mailimf_date_time * date_time)
{
  struct tm tmval;
  time_t timeval;
  
  tmval.tm_sec  = date_time->dt_sec;
  tmval.tm_min  = date_time->dt_min;
  tmval.tm_hour = date_time->dt_hour;
  tmval.tm_sec  = date_time->dt_sec;
  tmval.tm_mday = date_time->dt_day;
  tmval.tm_mon  = date_time->dt_month - 1;
  tmval.tm_year = date_time->dt_year - 1900;
  
  timeval = mail_mkgmtime(&tmval);
  
  timeval -= date_time->dt_zone * 36;
  
  return timeval;
}

time_t newsfeed_rfc822_date_parse(char * text)
{
  time_t date;
  struct mailimf_date_time * date_time;
  size_t current_pos;
  int r;
  
  date = (time_t) -1;
  current_pos = 0;
  r = mailimf_date_time_parse(text, strlen(text),
      &current_pos, &date_time);
  if (r == MAILIMF_NO_ERROR) {
    date = get_date(date_time);
    mailimf_date_time_free(date_time);
  }
  
  return date;
}
