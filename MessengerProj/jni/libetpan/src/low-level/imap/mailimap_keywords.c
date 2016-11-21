/*
 * libEtPan! -- a mail stuff library
 *
 * Copyright (C) 2001, 2005 - DINH Viet Hoa
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

/*
 * $Id: mailimap_keywords.c,v 1.14 2011/05/28 23:21:45 hoa Exp $
 */

#ifdef HAVE_CONFIG_H
#	include <config.h>
#endif

#include "mailimap_keywords.h"
#include "mailimap_types.h"
#include <string.h>
#include <stdio.h>

#ifndef UNSTRICT_SYNTAX
#define UNSTRICT_SYNTAX
#endif

struct mailimap_token_value {
  int value;
  const char * str;
};

int mailimap_token_case_insensitive_parse(mailstream * fd,
					  MMAPString * buffer,
					  size_t * indx,
					  const char * token)
{
  size_t len;
  size_t cur_token;
  int r;

  cur_token = * indx;
  len = strlen(token);

#ifdef UNSTRICT_SYNTAX
  r = mailimap_space_parse(fd, buffer, &cur_token);
  if ((r != MAILIMAP_NO_ERROR) && (r != MAILIMAP_ERROR_PARSE))
    return r;
#endif

  if (strncasecmp(buffer->str + cur_token, token, len) == 0) {
    cur_token += len;
    * indx = cur_token;
    return MAILIMAP_NO_ERROR;
  }
  else
    return MAILIMAP_ERROR_PARSE;
}


static int is_space_or_tab(char ch)
{
  return (ch == ' ') || (ch == '\t');
}

int mailimap_char_parse(mailstream * fd, MMAPString * buffer,
			size_t * indx, char token)
{
  size_t cur_token;

  cur_token = * indx;

  if (buffer->str[cur_token] == token) {
    cur_token ++;
    * indx = cur_token;
    return MAILIMAP_NO_ERROR;
  }
  else
    return MAILIMAP_ERROR_PARSE;
}

int mailimap_space_parse(mailstream * fd, MMAPString * buffer,
			 size_t * indx)
{
#ifdef UNSTRICT_SYNTAX

  /* can accept unstrict syntax */
  size_t cur_token;

  cur_token = * indx;

  while (is_space_or_tab(* (buffer->str + cur_token)))
    cur_token ++;

  if (cur_token == * indx)
    return MAILIMAP_ERROR_PARSE;

  * indx = cur_token;

  return MAILIMAP_NO_ERROR;

#else
  return mailimap_char_parse(fd, buffer, indx, ' ');
#endif
}



#define mailimap_get_token_str(indx, tab) \
           mailimap_get_token_str_size(indx, tab, \
           sizeof(tab) / sizeof(struct mailimap_token_value))

#define mailimap_get_token_value(fd, buffer, indx, tab) \
           mailimap_get_token_value_size(fd, buffer, indx, tab, \
           sizeof(tab) / sizeof(struct mailimap_token_value))


static const char * mailimap_get_token_str_size(int indx,
					  struct mailimap_token_value * tab,
					  size_t size)
{
  size_t i;

  for(i = 0 ; i < size ; i++)
    if (indx == tab[i].value)
      return tab[i].str;
  
  return NULL;
}



static int mailimap_get_token_value_size(mailstream * fd, MMAPString * buffer,
					 size_t * indx,
					 struct mailimap_token_value * tab,
					 size_t size)
{
  size_t i;
  int r;

#ifdef UNSTRICT_SYNTAX
  /* can accept unstrict syntax */
  r = mailimap_space_parse(fd, buffer, indx);
  if ((r != MAILIMAP_NO_ERROR) && (r != MAILIMAP_ERROR_PARSE))
    return r;
#endif

  for(i = 0 ; i < size ; i++) {
    r = mailimap_token_case_insensitive_parse(fd, buffer, indx, tab[i].str);
    if (r == MAILIMAP_NO_ERROR)
      return tab[i].value;
  }
  
  return -1;
}


static struct mailimap_token_value status_att_tab[] = {
  {MAILIMAP_STATUS_ATT_MESSAGES,      "MESSAGES"},
  {MAILIMAP_STATUS_ATT_RECENT,        "RECENT"},
  {MAILIMAP_STATUS_ATT_UIDNEXT,       "UIDNEXT"},
  {MAILIMAP_STATUS_ATT_UIDVALIDITY,   "UIDVALIDITY"},
  {MAILIMAP_STATUS_ATT_UNSEEN,        "UNSEEN"},
  {MAILIMAP_STATUS_ATT_HIGHESTMODSEQ, "HIGHESTMODSEQ"},
};

int mailimap_status_att_get_token_value(mailstream * fd, MMAPString * buffer,
					size_t * indx)
{
  int r;

#ifdef UNSTRICT_SYNTAX
  /* can accept unstrict syntax */
  r = mailimap_space_parse(fd, buffer, indx);
  if ((r != MAILIMAP_NO_ERROR) && (r != MAILIMAP_ERROR_PARSE))
    return r;
#endif
  return mailimap_get_token_value(fd, buffer, indx,
				  status_att_tab);
}


const char * mailimap_status_att_get_token_str(int indx)
{
  return mailimap_get_token_str(indx, status_att_tab);
}

static struct mailimap_token_value month_tab[] = {
  {1,  "Jan"},
  {2,  "Feb"},
  {3,  "Mar"},
  {4,  "Apr"},
  {5,  "May"},
  {6,  "Jun"},
  {7,  "Jul"},
  {8,  "Aug"},
  {9,  "Sep"},
  {10, "Oct"},
  {11, "Nov"},
  {12, "Dec"}
};

int mailimap_month_get_token_value(mailstream * fd, MMAPString * buffer,
				   size_t * indx)
{
  return mailimap_get_token_value(fd, buffer, indx, month_tab);
}


const char * mailimap_month_get_token_str(int indx)
{
  return mailimap_get_token_str(indx, month_tab);
}





static struct mailimap_token_value mailimap_flag_tab[] = {
  {MAILIMAP_FLAG_ANSWERED, "\\Answered"},
  {MAILIMAP_FLAG_FLAGGED,  "\\Flagged"},
  {MAILIMAP_FLAG_DELETED,  "\\Deleted"},
  {MAILIMAP_FLAG_SEEN,     "\\Seen"},
  {MAILIMAP_FLAG_DRAFT,    "\\Draft"}
};

int mailimap_flag_get_token_value(mailstream * fd, MMAPString * buffer,
				  size_t * indx)
{
  return mailimap_get_token_value(fd, buffer, indx,
				  mailimap_flag_tab);
}


const char * mailimap_flag_get_token_str(int indx)
{
  return mailimap_get_token_str(indx, mailimap_flag_tab);
}




static struct mailimap_token_value encoding_tab[] = {
  {MAILIMAP_BODY_FLD_ENC_7BIT,             "7BIT"},
  {MAILIMAP_BODY_FLD_ENC_8BIT,             "8BIT"},
  {MAILIMAP_BODY_FLD_ENC_BINARY,           "BINARY"},
  {MAILIMAP_BODY_FLD_ENC_BASE64,           "BASE64"},
  {MAILIMAP_BODY_FLD_ENC_QUOTED_PRINTABLE, "QUOTED-PRINTABLE"}
};

int mailimap_encoding_get_token_value(mailstream * fd, MMAPString * buffer,
				      size_t * indx)
{
  return mailimap_get_token_value(fd, buffer, indx, encoding_tab);
}

static struct mailimap_token_value mbx_list_sflag_tab[] = {
  {MAILIMAP_MBX_LIST_SFLAG_MARKED,      "\\Marked"},
  {MAILIMAP_MBX_LIST_SFLAG_NOSELECT,    "\\Noselect"},
  {MAILIMAP_MBX_LIST_SFLAG_UNMARKED,    "\\Unmarked"},
  {MAILIMAP_MBX_LIST_SFLAG_MARKED,      "/Marked"},
  {MAILIMAP_MBX_LIST_SFLAG_NOSELECT,    "/Noselect"},
	{MAILIMAP_MBX_LIST_SFLAG_UNMARKED,    "/Unmarked"}
};

int mailimap_mbx_list_sflag_get_token_value(mailstream * fd,
					    MMAPString * buffer,
					    size_t * indx)
{
  return mailimap_get_token_value(fd, buffer, indx, mbx_list_sflag_tab);
}

static struct mailimap_token_value media_basic_tab[] = {
  {MAILIMAP_MEDIA_BASIC_APPLICATION, "APPLICATION"},
  {MAILIMAP_MEDIA_BASIC_AUDIO,       "AUDIO"},
  {MAILIMAP_MEDIA_BASIC_IMAGE,       "IMAGE"},
  {MAILIMAP_MEDIA_BASIC_MESSAGE,     "MESSAGE"},
  {MAILIMAP_MEDIA_BASIC_VIDEO,       "VIDEO"}
};

int mailimap_media_basic_get_token_value(mailstream * fd, MMAPString * buffer,
					 size_t * indx)
{
  return mailimap_get_token_value(fd, buffer, indx, media_basic_tab);
}

static struct mailimap_token_value resp_cond_state_tab[] = {
  {MAILIMAP_RESP_COND_STATE_OK,    "OK"},
  {MAILIMAP_RESP_COND_STATE_NO,    "NO"},
  {MAILIMAP_RESP_COND_STATE_BAD,   "BAD"}
};

int mailimap_resp_cond_state_get_token_value(mailstream * fd,
					     MMAPString * buffer,
					     size_t * indx)
{
  return mailimap_get_token_value(fd, buffer, indx, resp_cond_state_tab);
}

static struct mailimap_token_value resp_text_code_1_tab[] = {
  {MAILIMAP_RESP_TEXT_CODE_ALERT,      "ALERT"},
  {MAILIMAP_RESP_TEXT_CODE_PARSE,      "PARSE"},
  {MAILIMAP_RESP_TEXT_CODE_READ_ONLY,  "READ-ONLY"},
  {MAILIMAP_RESP_TEXT_CODE_READ_WRITE, "READ-WRITE"},
  {MAILIMAP_RESP_TEXT_CODE_TRY_CREATE, "TRYCREATE"}
};

int mailimap_resp_text_code_1_get_token_value(mailstream * fd,
					      MMAPString * buffer,
					      size_t * indx)
{
  return mailimap_get_token_value(fd, buffer, indx, resp_text_code_1_tab);
}

static struct mailimap_token_value resp_text_code_2_tab[] = {
  {MAILIMAP_RESP_TEXT_CODE_UIDNEXT,      "UIDNEXT"},
  {MAILIMAP_RESP_TEXT_CODE_UIDVALIDITY,  "UIDVALIDITY"},
  {MAILIMAP_RESP_TEXT_CODE_UNSEEN,       "UNSEEN"},
};

int mailimap_resp_text_code_2_get_token_value(mailstream * fd,
					      MMAPString * buffer,
					      size_t * indx)
{
  return mailimap_get_token_value(fd, buffer, indx, resp_text_code_2_tab);
}

static struct mailimap_token_value section_msgtext_tab[] = {
  {MAILIMAP_SECTION_MSGTEXT_HEADER_FIELDS_NOT, "HEADER.FIELDS.NOT"},
  {MAILIMAP_SECTION_MSGTEXT_HEADER_FIELDS,     "HEADER.FIELDS"},
  {MAILIMAP_SECTION_MSGTEXT_HEADER,            "HEADER"},
  {MAILIMAP_SECTION_MSGTEXT_TEXT,              "TEXT"}
};

int mailimap_section_msgtext_get_token_value(mailstream * fd,
					     MMAPString * buffer,
					     size_t * indx)
{
  return mailimap_get_token_value(fd, buffer, indx, section_msgtext_tab);
}
