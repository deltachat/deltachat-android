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

#include <stddef.h>

#include "mailimap_compress.h"

#include "mailimap.h"
#include "mailimap_sender.h"
#include "mailstream_compress.h"

#include <stdio.h>

LIBETPAN_EXPORT
int mailimap_compress(mailimap * session)
{
  struct mailimap_response * response;
  int r;
  int res;
  int error_code;
  mailstream_low * compressed_stream;
  mailstream_low * low;

  r = mailimap_send_current_tag(session);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }

  r = mailimap_token_send(session->imap_stream, "COMPRESS DEFLATE");
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }

  r = mailimap_crlf_send(session->imap_stream);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }

  if (mailstream_flush(session->imap_stream) == -1) {
    res = MAILIMAP_ERROR_STREAM;
    goto err;
  }

  if (mailimap_read_line(session) == NULL) {
    res = MAILIMAP_ERROR_STREAM;
    goto err;
  }

  r = mailimap_parse_response(session, &response);
  if (r != MAILIMAP_NO_ERROR) {
    res = r;
    goto err;
  }

  error_code = response->rsp_resp_done->rsp_data.rsp_tagged->rsp_cond_state->rsp_type;

  mailimap_response_free(response);

  if (error_code != MAILIMAP_RESP_COND_STATE_OK) {
    res = MAILIMAP_ERROR_EXTENSION;
    goto err;
  }

  low = mailstream_get_low(session->imap_stream);
  compressed_stream = mailstream_low_compress_open(low);
  if (compressed_stream == NULL) {
    res = MAILIMAP_ERROR_STREAM;
    goto err;
  }
  mailstream_low_set_timeout(compressed_stream, session->imap_timeout);
  mailstream_set_low(session->imap_stream, compressed_stream);
  
  return MAILIMAP_NO_ERROR;
  
err:
  return res;
}

LIBETPAN_EXPORT
int mailimap_has_compress_deflate(mailimap * session)
{
  return mailimap_has_extension(session, "COMPRESS=DEFLATE");
}
