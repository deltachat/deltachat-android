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

#include "mailimap_id.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "mailimap.h"
#include "mailimap_sender.h"
#include "mailimap_id_sender.h"
#include "mailimap_id_parser.h"
#include "mailimap_id_types.h"

static void mailimap_id_ext_data_free(struct mailimap_extension_data * ext_data);

LIBETPAN_EXPORT
struct mailimap_extension_api mailimap_extension_id = {
  /* name */          "ID",
  /* extension_id */  MAILIMAP_EXTENSION_ID,
  /* parser */        mailimap_id_parse,
  /* free */          mailimap_id_ext_data_free
};

int mailimap_id(mailimap * session, struct mailimap_id_params_list * client_identification,
                struct mailimap_id_params_list ** result)
{
  struct mailimap_response * response;
  int r;
  int error_code;
  clistiter * cur;
  struct mailimap_id_params_list * params_list;

  if ((session->imap_state == MAILIMAP_STATE_DISCONNECTED) || (session->imap_state == MAILIMAP_STATE_LOGOUT))
    return MAILIMAP_ERROR_BAD_STATE;
  
  r = mailimap_send_current_tag(session);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_id_send(session->imap_stream, client_identification);
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
  
  error_code = response->rsp_resp_done->rsp_data.rsp_tagged->rsp_cond_state->rsp_type;
  
  params_list = NULL;
  for (cur = clist_begin(session->imap_response_info->rsp_extension_list);
    cur != NULL; cur = clist_next(cur)) {
    struct mailimap_extension_data * ext_data;

    ext_data = (struct mailimap_extension_data *) clist_content(cur);
    if (ext_data->ext_extension->ext_id == MAILIMAP_EXTENSION_ID) {
      params_list = ext_data->ext_data;
  	  ext_data->ext_data = NULL;
  	  ext_data->ext_type = -1;
    }
  }

  if (params_list == NULL) {
    params_list = mailimap_id_params_list_new_empty();
  }

  mailimap_response_free(response);
  
  switch (error_code) {
  case MAILIMAP_RESP_COND_STATE_OK:
    * result = params_list;
    return MAILIMAP_NO_ERROR;

  default:
    mailimap_id_params_list_free(params_list);
    return MAILIMAP_ERROR_EXTENSION;
  }
}

static void mailimap_id_ext_data_free(struct mailimap_extension_data * ext_data)
{
  if (ext_data->ext_data != NULL) {
    mailimap_id_params_list_free(ext_data->ext_data);
  }
  free(ext_data);
}

LIBETPAN_EXPORT
int mailimap_id_basic(mailimap * session, const char * name, const char * version,
                      char ** p_server_name, char ** p_server_version)
{
  struct mailimap_id_params_list * client_identification;
  struct mailimap_id_params_list * server_identification;
  int r;
  char * server_name;
  char * server_version;
  char * dup_name;
  char * dup_value;
  clistiter * cur;
  
  client_identification = mailimap_id_params_list_new_empty();
  if (client_identification == NULL)
    return MAILIMAP_ERROR_MEMORY;
  
  if (name != NULL) {
    dup_name = strdup("name");
    if (dup_name == NULL) {
      mailimap_id_params_list_free(client_identification);
      return MAILIMAP_ERROR_MEMORY;
    }

    dup_value = strdup(name);
    if (dup_value == NULL) {
      free(dup_name);
      mailimap_id_params_list_free(client_identification);
      return MAILIMAP_ERROR_MEMORY;
    }

    r = mailimap_id_params_list_add_name_value(client_identification, dup_name, dup_value);
    if (r != MAILIMAP_NO_ERROR) {
      free(dup_value);
      free(dup_name);
      mailimap_id_params_list_free(client_identification);
      return MAILIMAP_ERROR_MEMORY;
    }
  }
  
  if (version != NULL) {
    dup_name = strdup("version");
    if (dup_name == NULL) {
      mailimap_id_params_list_free(client_identification);
      return MAILIMAP_ERROR_MEMORY;
    }

    dup_value = strdup(version);
    if (dup_value == NULL) {
      free(dup_name);
      mailimap_id_params_list_free(client_identification);
      return MAILIMAP_ERROR_MEMORY;
    }

    r = mailimap_id_params_list_add_name_value(client_identification, dup_name, dup_value);
    if (r != MAILIMAP_NO_ERROR) {
      free(dup_value);
      free(dup_name);
      mailimap_id_params_list_free(client_identification);
      return MAILIMAP_ERROR_MEMORY;
    }
  }
  
  r = mailimap_id(session, client_identification, &server_identification);
  if (r != MAILIMAP_NO_ERROR) {
    mailimap_id_params_list_free(client_identification);
    return r;
  }
  
  server_name = NULL;
  server_version = NULL;
  for(cur = clist_begin(server_identification->idpa_list) ; cur != NULL ; cur = clist_next(cur)) {
    struct mailimap_id_param * param;
    
    param = clist_content(cur);
    if (strcasecmp(param->idpa_name, "name") == 0) {
      if (server_name != NULL) {
        free(server_name);
      }
      server_name = strdup(param->idpa_value);
    }
    else if (strcasecmp(param->idpa_name, "version") == 0) {
      if (server_version != NULL) {
        free(server_version);
      }
      server_version = strdup(param->idpa_value);
    }
  }
  
  mailimap_id_params_list_free(client_identification);
  mailimap_id_params_list_free(server_identification);
  
  * p_server_name = server_name;
  * p_server_version = server_version;
  
  return MAILIMAP_NO_ERROR;
}

LIBETPAN_EXPORT
int mailimap_has_id(mailimap * session)
{
  return mailimap_has_extension(session, "ID");
}

