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

#ifdef HAVE_CONFIG_H
#	include <config.h>
#endif

#include "mailimap.h"
#include "mailimap_extension.h"
#include "quota.h"
#include "quota_types.h"
#include "quota_parser.h"
#include "quota_sender.h"

#include <stdlib.h>

LIBETPAN_EXPORT
struct mailimap_extension_api mailimap_extension_quota = {
  /* name */          "QUOTA",
  /* extension_id */  MAILIMAP_EXTENSION_QUOTA,
  /* parser */        mailimap_quota_parse,
  /* free */          mailimap_quota_free
};

/*
  this is one of the imap commands quota adds.
*/
LIBETPAN_EXPORT
int mailimap_quota_getquotaroot(mailimap * session,
    const char * list_mb,
    struct mailimap_quota_complete_data ** result)
{
  struct mailimap_response * response;
  struct mailimap_extension_data * ext_data;
  clistiter * cur;
  int r;
  int error_code;
  struct mailimap_quota_quotaroot_data * quotaroot_data = NULL;
  clist * quota_list = NULL;

  if ((session->imap_state != MAILIMAP_STATE_AUTHENTICATED) && (session->imap_state != MAILIMAP_STATE_SELECTED))
    return MAILIMAP_ERROR_BAD_STATE;

  r = mailimap_send_current_tag(session);
  if (r != MAILIMAP_NO_ERROR)
    return r;

  r = mailimap_quota_getquotaroot_send(session->imap_stream,
    list_mb);
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

  quota_list = clist_new();
  if (quota_list == NULL)
    return MAILIMAP_ERROR_MEMORY;

  for (cur = clist_begin(session->imap_response_info->rsp_extension_list);
    cur != NULL; cur = clist_next(cur)) {
      ext_data = (struct mailimap_extension_data *) clist_content(cur);
      if (ext_data->ext_extension->ext_id == MAILIMAP_EXTENSION_QUOTA) {
	if (ext_data->ext_type == MAILIMAP_QUOTA_TYPE_QUOTA_DATA) {
	  r = clist_append(quota_list, ext_data->ext_data);
	  if (r != 0) {
	    clist_foreach(quota_list,
		(clist_func) &mailimap_quota_quota_data_free, NULL);
	    clist_free(quota_list);
	    if (quotaroot_data)
	      mailimap_quota_quotaroot_data_free(quotaroot_data);
	    clist_foreach(session->imap_response_info->rsp_extension_list,
		  (clist_func) mailimap_extension_data_free, NULL);
	    clist_free(session->imap_response_info->rsp_extension_list);
	    session->imap_response_info->rsp_extension_list = NULL;
	    mailimap_response_free(response);
	    return MAILIMAP_ERROR_MEMORY;
	  }
	  ext_data->ext_data = NULL;
	  ext_data->ext_type = -1;
	} else if (ext_data->ext_type == MAILIMAP_QUOTA_TYPE_QUOTAROOT_DATA) {
	  if (!quotaroot_data) {
	    quotaroot_data = ext_data->ext_data;
	    ext_data->ext_data = NULL;
	    ext_data->ext_type = -1;
	  }
	}
      }
  }

  clist_foreach(session->imap_response_info->rsp_extension_list,
        (clist_func) mailimap_extension_data_free, NULL);
  clist_free(session->imap_response_info->rsp_extension_list);
  session->imap_response_info->rsp_extension_list = NULL;

  error_code = response->rsp_resp_done->rsp_data.rsp_tagged->rsp_cond_state->rsp_type;

  mailimap_response_free(response);

  if (!quotaroot_data) {
    clist_foreach(quota_list,
	(clist_func) &mailimap_quota_quota_data_free, NULL);
    clist_free(quota_list);
    return MAILIMAP_ERROR_EXTENSION;
  }

  * result = mailimap_quota_complete_data_new(quotaroot_data, quota_list);
  if (!*result) {
    clist_foreach(quota_list,
	(clist_func) &mailimap_quota_quota_data_free, NULL);
    clist_free(quota_list);
    mailimap_quota_quotaroot_data_free(quotaroot_data);
    return MAILIMAP_ERROR_MEMORY;
  }

  switch (error_code) {
  case MAILIMAP_RESP_COND_STATE_OK:
    return MAILIMAP_NO_ERROR;

  default:
    return MAILIMAP_ERROR_EXTENSION;
  }
}

void
mailimap_quota_free(struct mailimap_extension_data * ext_data)
{
  switch (ext_data->ext_type) {
    case MAILIMAP_QUOTA_TYPE_QUOTA_DATA:
      mailimap_quota_quota_data_free(ext_data->ext_data);
      break;

    case MAILIMAP_QUOTA_TYPE_QUOTAROOT_DATA:
      mailimap_quota_quotaroot_data_free(ext_data->ext_data);
      break;
  }

  free(ext_data);
}

LIBETPAN_EXPORT
int mailimap_has_quota(mailimap * session)
{
  return mailimap_has_extension(session, "QUOTA");
}

