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
 * $Id: mailimap_socket.c,v 1.18 2009/12/19 00:57:31 hoa Exp $
 */

#ifdef HAVE_CONFIG_H
#	include <config.h>
#endif

#include "mailimap_socket.h"

#include "mailimap.h"

#include "connect.h"

#include <stdlib.h>
#ifdef HAVE_NETINET_IN_H
#	include <netinet/in.h>
#endif
#ifdef HAVE_UNISTD_H
#	include <unistd.h>
#endif

#include "mailstream_cfstream.h"

#define DEFAULT_IMAP_PORT 143
#define SERVICE_NAME_IMAP "imap2"
#define SERVICE_TYPE_TCP "tcp"

#if HAVE_CFNETWORK
static int mailimap_cfsocket_connect_voip(mailimap * f, const char * server, uint16_t port, int voip_enabled);
#endif

LIBETPAN_EXPORT
int mailimap_socket_connect_voip(mailimap * f, const char * server, uint16_t port, int voip_enabled)
{
    int s;
    mailstream * stream;

  #if HAVE_CFNETWORK
    if (mailstream_cfstream_enabled) {
      return mailimap_cfsocket_connect_voip(f, server, port, voip_enabled);
    }
  #endif

    if (port == 0) {
      port = mail_get_service_port(SERVICE_NAME_IMAP, SERVICE_TYPE_TCP);
      if (port == 0)
        port = DEFAULT_IMAP_PORT;
    }

    /* Connection */

    s = mail_tcp_connect_timeout(server, port, f->imap_timeout);
    if (s == -1)
      return MAILIMAP_ERROR_CONNECTION_REFUSED;

    stream = mailstream_socket_open_timeout(s, f->imap_timeout);
    if (stream == NULL) {
  #ifdef WIN32
  	closesocket(s);
  #else
      close(s);
  #endif
      return MAILIMAP_ERROR_MEMORY;
    }

    return mailimap_connect(f, stream);
}

LIBETPAN_EXPORT
int mailimap_socket_connect(mailimap * f, const char * server, uint16_t port)
{
  return mailimap_socket_connect_voip(f, server, port, mailstream_cfstream_voip_enabled);
}


int mailimap_socket_starttls(mailimap * f)
{
  return mailimap_socket_starttls_with_callback(f, NULL, NULL);
}

static int mailimap_cfsocket_starttls(mailimap * f);

int mailimap_socket_starttls_with_callback(mailimap * f,
    void (* callback)(struct mailstream_ssl_context * ssl_context, void * data), void * data)
{
  mailstream_low * low;
  mailstream_low * new_low;
  int r;
  int fd;
  
  low = mailstream_get_low(f->imap_stream);
  if (low->driver == mailstream_cfstream_driver) {
    // won't use callback
    return mailimap_cfsocket_starttls(f);
  }
  
  r = mailimap_starttls(f);
  
  switch (r) {
  case MAILIMAP_NO_ERROR:
    break;
  default:
    return r;
  }

  fd = mailstream_low_get_fd(low);
  if (fd == -1)
    return MAILIMAP_ERROR_STREAM;
  
  new_low = mailstream_low_tls_open_with_callback_timeout(fd, f->imap_timeout,
    callback, data);
  if (new_low == NULL)
    return MAILIMAP_ERROR_STREAM;
  
  mailstream_low_free(low);
  mailstream_set_low(f->imap_stream, new_low);
  
  return MAILIMAP_NO_ERROR;
}

#if HAVE_CFNETWORK
static int mailimap_cfsocket_connect_voip(mailimap * f, const char * server, uint16_t port, int voip_enabled)
{
  mailstream * stream;
  
  stream = mailstream_cfstream_open_voip_timeout(server, port, voip_enabled, f->imap_timeout);
  if (stream == NULL) {
    return MAILIMAP_ERROR_CONNECTION_REFUSED;
  }
  
  return mailimap_connect(f, stream);
}
#endif

static int mailimap_cfsocket_starttls(mailimap * f)
{
  int r;
  
  r = mailimap_starttls(f);
  switch (r) {
    case MAILIMAP_NO_ERROR:
      break;
    default:
      return r;
  }
  
  mailstream_cfstream_set_ssl_verification_mask(f->imap_stream, MAILSTREAM_CFSTREAM_SSL_NO_VERIFICATION);
  r = mailstream_cfstream_set_ssl_enabled(f->imap_stream, 1);
  if (r < 0) {
    return MAILIMAP_ERROR_SSL;
  }
  
  return MAILIMAP_NO_ERROR;
}
