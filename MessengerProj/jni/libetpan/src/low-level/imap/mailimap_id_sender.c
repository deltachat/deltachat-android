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
#include "mailimap_id_sender.h"

#include <stdlib.h>

#include "mailimap_sender.h"

static int mailimap_id_param_send(mailstream * fd, struct mailimap_id_param * param);
static int mailimap_id_params_list_send(mailstream * fd, struct mailimap_id_params_list * list);

int mailimap_id_send(mailstream * fd, struct mailimap_id_params_list * client_identification)
{
  int r;

  r = mailimap_token_send(fd, "ID");
  if (r != MAILIMAP_NO_ERROR)
    return r;

  r = mailimap_space_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;

  r = mailimap_id_params_list_send(fd, client_identification);
  if (r != MAILIMAP_NO_ERROR)
    return r;

  return MAILIMAP_NO_ERROR;
}

static int mailimap_id_params_list_send(mailstream * fd, struct mailimap_id_params_list * list)
{
  int r;

  if ((list == NULL) || (clist_count(list->idpa_list) == 0)) {
    r = mailimap_token_send(fd, "NIL");
    if (r != MAILIMAP_NO_ERROR)
      return r;
    return MAILIMAP_NO_ERROR;
  }

  r = mailimap_oparenth_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_struct_spaced_list_send(fd, list->idpa_list,
  				  (mailimap_struct_sender *)
				  mailimap_id_param_send);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_cparenth_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  return MAILIMAP_NO_ERROR;
}

static int mailimap_id_param_send(mailstream * fd, struct mailimap_id_param * param)
{
  int r;
  
  // Sends quoted string instead since Yahoo IMAP server will break when sending atoms.
  r = mailimap_quoted_send(fd, param->idpa_name);
  if (r != MAILIMAP_NO_ERROR)
    return r;
  
  r = mailimap_space_send(fd);
  if (r != MAILIMAP_NO_ERROR)
    return r;

  if (param->idpa_value == NULL) {
    r = mailimap_token_send(fd, "NIL");
    if (r != MAILIMAP_NO_ERROR)
      return r;
  }
  else {
    // Sends quoted string instead since Yahoo IMAP server will break when sending atoms.
    r = mailimap_quoted_send(fd, param->idpa_value);
    if (r != MAILIMAP_NO_ERROR)
      return r;
  }
  
  return MAILIMAP_NO_ERROR;
}
