/*
 * libEtPan! -- a mail stuff library
 *
 * Copyright (C) 2001, 2011 - DINH Viet Hoa
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

#include "enable.h"

#include <stdlib.h>

#include "mailimap_parser.h"
#include "mailimap_sender.h"
#include "mailimap.h"
#include "mailimap_keywords.h"

enum {
    MAILIMAP_ENABLE_TYPE_ENABLE
};

static int
mailimap_enable_extension_parse(int calling_parser, mailstream * fd,
                               MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
                               struct mailimap_extension_data ** result,
                               size_t progr_rate, progress_function * progr_fun);

static void
mailimap_enable_extension_data_free(struct mailimap_extension_data * ext_data);

LIBETPAN_EXPORT
struct mailimap_extension_api mailimap_extension_enable = {
  /* name */          "ENABLE",
  /* extension_id */  MAILIMAP_EXTENSION_ENABLE,
  /* parser */        mailimap_enable_extension_parse,
  /* free */          mailimap_enable_extension_data_free
};

/*
"ENABLE" 1*(SP capability)
*/

static int mailimap_capability_info_send(mailstream * fd, struct mailimap_capability * cap)
{
	int r;
	
	switch (cap->cap_type) {
		case MAILIMAP_CAPABILITY_AUTH_TYPE:
      r = mailimap_token_send(fd, "AUTH=");
	    if (r != MAILIMAP_NO_ERROR)
	      return r;
	    r = mailimap_token_send(fd, cap->cap_data.cap_auth_type);
		  if (r != MAILIMAP_NO_ERROR)
			  return r;
			return MAILIMAP_NO_ERROR;
			
		case MAILIMAP_CAPABILITY_NAME:
      r = mailimap_token_send(fd, cap->cap_data.cap_name);
      if (r != MAILIMAP_NO_ERROR)
        return r;
			return MAILIMAP_NO_ERROR;

    default:
	    return MAILIMAP_ERROR_INVAL;
	}
}

static int mailimap_capability_data_send(mailstream * fd, struct mailimap_capability_data * capabilities)
{
	return mailimap_struct_spaced_list_send(fd, capabilities->cap_list,
		(mailimap_struct_sender *) mailimap_capability_info_send);
}

static int mailimap_enable_send(mailstream * fd, struct mailimap_capability_data * capabilities)
{
  int r;
  
  r = mailimap_token_send(fd, "ENABLE");
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_space_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_capability_data_send(fd, capabilities);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  return MAILIMAP_NO_ERROR;
}

LIBETPAN_EXPORT
int mailimap_enable(mailimap * session, struct mailimap_capability_data * capabilities,
    struct mailimap_capability_data ** result)
{
  struct mailimap_response * response;
  int r;
  int error_code;
  clistiter * cur;
	struct mailimap_capability_data * cap_data;
  
  if (session->imap_state != MAILIMAP_STATE_AUTHENTICATED)
    return MAILIMAP_ERROR_BAD_STATE;
  
  r = mailimap_send_current_tag(session);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_enable_send(session->imap_stream, capabilities);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_crlf_send(session->imap_stream);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  if (mailstream_flush(session->imap_stream) == -1)
    return MAILIMAP_ERROR_STREAM;
  
  if (mailimap_read_line(session) == NULL)
    return MAILIMAP_ERROR_STREAM;
  
  r = mailimap_parse_response(session, &response);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  

  cap_data = NULL;
  for(cur = clist_begin(session->imap_response_info->rsp_extension_list) ; cur != NULL ; cur = clist_next(cur)) {
    struct mailimap_extension_data * ext_data;
    
    ext_data = clist_content(cur);
    if (ext_data->ext_extension->ext_id != MAILIMAP_EXTENSION_ENABLE) {
      continue;
    }
    if (ext_data->ext_type != MAILIMAP_ENABLE_TYPE_ENABLE) {
      continue;
    }
    
    if (cap_data != NULL) {
      mailimap_capability_data_free(cap_data);
    }
    cap_data = ext_data->ext_data;
    ext_data->ext_data = NULL;
		break;
  }
  if (cap_data == NULL) {
    clist * list;
    
    list = clist_new();
    if (list == NULL) {
      return MAILIMAP_ERROR_MEMORY;
    }
    cap_data = mailimap_capability_data_new(list);
    if (cap_data == NULL) {
      clist_free(list);
      return MAILIMAP_ERROR_MEMORY;
    }
  }
  
  error_code = response->rsp_resp_done->rsp_data.rsp_tagged->rsp_cond_state->rsp_type;
  
  mailimap_response_free(response);
  
  switch (error_code) {
    case MAILIMAP_RESP_COND_STATE_OK:
  	  * result = cap_data;
      return MAILIMAP_NO_ERROR;
      
    default:
  		mailimap_capability_data_free(cap_data);
      return MAILIMAP_ERROR_EXTENSION;
  }
}


LIBETPAN_EXPORT
int mailimap_has_enable(mailimap * session)
{
  return mailimap_has_extension(session, "ENABLE");
}

static void
mailimap_enable_extension_data_free(struct mailimap_extension_data * ext_data)
{
  if (ext_data->ext_data != NULL) {
    mailimap_capability_data_free((struct mailimap_capability_data *) ext_data->ext_data);
  }
  free(ext_data);
}

static int mailimap_enable_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
	size_t * indx,
	struct mailimap_capability_data ** result,
	size_t progr_rate,
	progress_function * progr_fun)
{
  size_t cur_token;
  int r;
  int res;
	struct mailimap_capability_data * capabilities;
  clist * cap_list;
  
  cur_token = * indx;
  
  r = mailimap_token_case_insensitive_parse(fd, buffer, &cur_token, "ENABLED");
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }
  
  r = mailimap_capability_list_parse(fd, buffer, parser_ctx, &cur_token,
                                     &cap_list,
                                     progr_rate, progr_fun);
  if (r == MAILIMAP_ERROR_PARSE) {
    cap_list = clist_new();
    if (cap_list == NULL) {
      res = MAILIMAP_ERROR_MEMORY;
      goto err;
    }
    r = MAILIMAP_NO_ERROR;
  }
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }
  
  capabilities = mailimap_capability_data_new(cap_list);
  if (capabilities == NULL) {
    res = MAILIMAP_ERROR_MEMORY;
    goto free_list;
  }
  
  * result = capabilities;
  * indx = cur_token;
  
  return MAILIMAP_NO_ERROR;
  
free_list:
  if (cap_list) {
    clist_foreach(cap_list, (clist_func) mailimap_capability_free, NULL);
    clist_free(cap_list);
  }
err:
  return res;
}

static int
mailimap_enable_extension_parse(int calling_parser, mailstream * fd,
                                MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
                                struct mailimap_extension_data ** result,
                                size_t progr_rate, progress_function * progr_fun)
{
  int r;
  struct mailimap_capability_data * capabilities = NULL;
  struct mailimap_extension_data * ext_data;
  void * data;
  int type;
  size_t cur_token;
  
  cur_token = * indx;
  
  switch (calling_parser)
  {
    case MAILIMAP_EXTENDED_PARSER_RESPONSE_DATA:
			r = mailimap_enable_parse(fd, buffer, parser_ctx, &cur_token,
		    &capabilities, progr_rate, progr_fun);
      if (r == MAILIMAP_NO_ERROR) {
        type = MAILIMAP_ENABLE_TYPE_ENABLE;
        data = capabilities;
      }
      
      if (r != MAILIMAP_NO_ERROR) {
        return r;
      }
      
      ext_data = mailimap_extension_data_new(&mailimap_extension_enable,
                                             type, data);
      if (ext_data == NULL) {
        if (capabilities != NULL)
          mailimap_capability_data_free(capabilities);
        return MAILIMAP_ERROR_MEMORY;
      }
      
      * result = ext_data;
      * indx = cur_token;
      
      return MAILIMAP_NO_ERROR;
      
    default:
      /* return a MAILIMAP_ERROR_PARSE if the extension
       doesn't extend calling_parser. */
      return MAILIMAP_ERROR_PARSE;
  }
}

