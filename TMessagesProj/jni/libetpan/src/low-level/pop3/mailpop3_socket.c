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
 * $Id: mailpop3_socket.c,v 1.16 2009/12/19 00:57:31 hoa Exp $
 */

#ifdef HAVE_CONFIG_H
#	include <config.h>
#endif

#include "mailpop3_socket.h"

#include "mailpop3.h"
#include "mailstream_cfstream.h"

#include "connect.h"
#ifdef HAVE_NETINET_IN_H
#	include <netinet/in.h>
#endif
#ifdef HAVE_UNISTD_H
#	include <unistd.h>
#endif
#include <stdlib.h>

#define DEFAULT_POP3_PORT 110
#define SERVICE_NAME_POP3 "pop3"
#define SERVICE_TYPE_TCP "tcp"

#if HAVE_CFNETWORK
static int mailpop3_cfsocket_connect(mailpop3 * f, const char * server, uint16_t port);
#endif

int mailpop3_socket_connect(mailpop3 * f, const char * server, uint16_t port)
{
  int s;
  mailstream * stream;

#if HAVE_CFNETWORK
  if (mailstream_cfstream_enabled) {
    return mailpop3_cfsocket_connect(f, server, port);
  }
#endif
  
  if (port == 0) {
    port = mail_get_service_port(SERVICE_NAME_POP3, SERVICE_TYPE_TCP);
    if (port == 0)
      port = DEFAULT_POP3_PORT;
  }
  
  /* Connection */

  s = mail_tcp_connect_timeout(server, port, f->pop3_timeout);
  if (s == -1)
    return MAILPOP3_ERROR_CONNECTION_REFUSED;

  stream = mailstream_socket_open_timeout(s, f->pop3_timeout);
  if (stream == NULL) {
#ifdef WIN32
	closesocket(s);
#else
    close(s);
#endif
    return MAILPOP3_ERROR_MEMORY;
  }

  return mailpop3_connect(f, stream);
}

static int mailpop3_cfsocket_starttls(mailpop3 * f);

LIBETPAN_EXPORT
int mailpop3_socket_starttls(mailpop3 * f)
{
  return mailpop3_socket_starttls_with_callback(f, NULL, NULL);
}

LIBETPAN_EXPORT
int mailpop3_socket_starttls_with_callback(mailpop3 * f,
    void (* callback)(struct mailstream_ssl_context * ssl_context, void * data), void * data)
{
  int r;
  int fd;
  mailstream_low * low;
  mailstream_low * new_low;
  
  low = mailstream_get_low(f->pop3_stream);
  if (low->driver == mailstream_cfstream_driver) {
    // won't use callback
    return mailpop3_cfsocket_starttls(f);
  }
  
  r = mailpop3_stls(f);

  switch (r) {
  case MAILPOP3_NO_ERROR:
    break;
  default:
    return r;
  }

  fd = mailstream_low_get_fd(low);
  if (fd == -1)
    return MAILPOP3_ERROR_STREAM;
  
  new_low = mailstream_low_tls_open_with_callback_timeout(fd,
      f->pop3_timeout, callback, data);
  if (new_low == NULL)
    return MAILPOP3_ERROR_SSL;
  
  mailstream_low_free(low);
  mailstream_set_low(f->pop3_stream, new_low);
  
  return MAILPOP3_NO_ERROR;
}

#if HAVE_CFNETWORK
static int mailpop3_cfsocket_connect(mailpop3 * f, const char * server, uint16_t port)
{
  mailstream * stream;
  
  stream = mailstream_cfstream_open_timeout(server, port, f->pop3_timeout);
  if (stream == NULL) {
    return MAILPOP3_ERROR_CONNECTION_REFUSED;
  }
  
  return mailpop3_connect(f, stream);
}
#endif

static int mailpop3_cfsocket_starttls(mailpop3 * f)
{
  int r;
  
  r = mailpop3_stls(f);
  switch (r) {
    case MAILPOP3_NO_ERROR:
      break;
    default:
      return r;
  }
  
  mailstream_cfstream_set_ssl_verification_mask(f->pop3_stream, MAILSTREAM_CFSTREAM_SSL_NO_VERIFICATION);
  r = mailstream_cfstream_set_ssl_enabled(f->pop3_stream, 1);
  if (r < 0) {
    return MAILPOP3_ERROR_SSL;
  }
  
  return MAILPOP3_NO_ERROR;
}
