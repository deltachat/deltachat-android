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

#ifdef HAVE_CONFIG_H
#	include <config.h>
#endif

#include "namespace.h"
#include "mailimap.h"
#include "mailimap_extension.h"
#include "namespace_types.h"
#include "namespace_parser.h"
#include "namespace_sender.h"

#include <stdlib.h>

static void
mailimap_namespace_extension_data_free(struct mailimap_extension_data * ext_data);

LIBETPAN_EXPORT
struct mailimap_extension_api mailimap_extension_namespace = {
  /* name */          "NAMESPACE",
  /* extension_id */  MAILIMAP_EXTENSION_NAMESPACE,
  /* parser */        mailimap_namespace_extension_parse,
  /* free */          mailimap_namespace_extension_data_free
};

int mailimap_namespace(mailimap * session, struct mailimap_namespace_data ** result)
{
  struct mailimap_namespace_data * namespace_data;
  struct mailimap_response * response;
  clistiter * cur;
  int r;
  int error_code;
  
  if ((session->imap_state != MAILIMAP_STATE_AUTHENTICATED) && (session->imap_state != MAILIMAP_STATE_SELECTED))
    return MAILIMAP_ERROR_BAD_STATE;
  
  r = mailimap_send_current_tag(session);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_namespace_send(session->imap_stream);
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
  
  namespace_data = NULL;
  for (cur = clist_begin(session->imap_response_info->rsp_extension_list);
       cur != NULL; cur = clist_next(cur)) {
    struct mailimap_extension_data * ext_data;
    
    ext_data = (struct mailimap_extension_data *) clist_content(cur);
    if (ext_data->ext_extension->ext_id == MAILIMAP_EXTENSION_NAMESPACE) {
      if (namespace_data == NULL) {
        namespace_data = ext_data->ext_data;
        ext_data->ext_data = NULL;
        ext_data->ext_type = -1;
      }
    }
  }
  
  clist_foreach(session->imap_response_info->rsp_extension_list,
                (clist_func) mailimap_extension_data_free, NULL);
  clist_free(session->imap_response_info->rsp_extension_list);
  session->imap_response_info->rsp_extension_list = NULL;
  
  if (namespace_data == NULL) {
    return MAILIMAP_ERROR_EXTENSION;
  }
  
  error_code = response->rsp_resp_done->rsp_data.rsp_tagged->rsp_cond_state->rsp_type;
  switch (error_code) {
    case MAILIMAP_RESP_COND_STATE_OK:
      break;
      
    default:
      mailimap_namespace_data_free(namespace_data);
      return MAILIMAP_ERROR_EXTENSION;
  }
  
  mailimap_response_free(response);
  
  * result = namespace_data;
  
  return MAILIMAP_NO_ERROR;
}

static void
mailimap_namespace_extension_data_free(struct mailimap_extension_data * ext_data)
{
  if (ext_data->ext_data != NULL) {
    mailimap_namespace_data_free((struct mailimap_namespace_data *) ext_data->ext_data);
  }
  free(ext_data);
}

LIBETPAN_EXPORT
int mailimap_has_namespace(mailimap * session)
{
  return mailimap_has_extension(session, "NAMESPACE");
}

