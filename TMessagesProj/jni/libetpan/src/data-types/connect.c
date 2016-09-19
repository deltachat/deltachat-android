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
 * $Id: connect.c,v 1.29 2011/02/27 01:11:50 hoa Exp $
 */

#ifdef HAVE_CONFIG_H
#	include <config.h>
#endif

#include "connect.h"

#include "mailstream.h"

#include <sys/types.h>
#include <string.h>
#include <stdio.h>
#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif
#include <errno.h>
#include <fcntl.h>

#ifdef WIN32
#	include "win_etpan.h"
#else
#	include <netdb.h>
#	include <netinet/in.h>
#	include <sys/socket.h>
#	ifdef HAVE_SYS_POLL_H
#		include <sys/poll.h>
#	endif
#	include <unistd.h>
#	include <arpa/inet.h>
#endif

uint16_t mail_get_service_port(const char * name, char * protocol)
{
  struct servent * service;

  service = getservbyname(name, protocol);

  if (service == NULL)
    return 0;

  return ntohs(service->s_port);
}

static int prepare_fd(int fd)
{
#ifndef WIN32
  int fd_flags;
  int r;
  
  fd_flags = fcntl(fd, F_GETFL, 0);
  fd_flags |= O_NDELAY;
  r = fcntl(fd, F_SETFL, fd_flags);
  if (r < 0)
    return -1;
#endif
  
  return 0;
}

#ifdef HAVE_IPV6
static int verify_sock_errors(int s)
{
  socklen_t len;
  int val;
  len = sizeof(val);
  if (getsockopt(s, SOL_SOCKET, SO_ERROR, &val, &len) < 0) {
    return -1;
  } else if (val != 0) {
    return -1;
  }
  return 0;
}
#endif

static int wait_connect(int s, int r, time_t timeout_seconds)
{
#if defined(WIN32) || !USE_POLL
  fd_set fds;
#else
  struct pollfd pfd;
#endif // WIN32
  struct timeval timeout;
  
  if (r == 0) {
    /* connected immediately */
    return 0;
  }
  else if (r == -1) {
    if (errno == EINPROGRESS) {
      /* select */
    }
    else {
      return -1;
    }
  }
  
  if (timeout_seconds == 0) {
		timeout = mailstream_network_delay;
	}
	else {
		timeout.tv_sec = timeout_seconds;
		timeout.tv_usec = 0;
	}
  
#if defined(WIN32) || !USE_POLL
  FD_ZERO(&fds);
  FD_SET(s, &fds);
  /* TODO: how to cancel this ? -> could be cancelled using a cancel fd */
  r = select(s + 1, NULL, &fds, NULL, &timeout);
  if (r <= 0) {
    return -1;
  }
  
  if (!FD_ISSET(s, &fds)) {
    /* though, it's strange */
    return -1;
  }
#else
  pfd.fd = s;
  pfd.events = POLLOUT;
  pfd.revents = 0;
  
  r = poll(&pfd, 1, timeout.tv_sec * 1000 + timeout.tv_usec / 1000);
  if (r <= 0) {
    return -1;
  }
  
  if (pfd.revents & POLLOUT) {
    return -1;
  }
#endif

  return 0;
}

int mail_tcp_connect(const char * server, uint16_t port)
{
  return mail_tcp_connect_with_local_address(server, port, NULL, 0);
}

int mail_tcp_connect_timeout(const char * server, uint16_t port, time_t timeout)
{
  return mail_tcp_connect_with_local_address_timeout(server, port, NULL, 0, timeout);
}

int mail_tcp_connect_with_local_address(const char * server, uint16_t port,
    const char * local_address, uint16_t local_port)
{
	return mail_tcp_connect_with_local_address_timeout(server, port, local_address, local_port, 0);
}

int mail_tcp_connect_with_local_address_timeout(const char * server, uint16_t port,
    const char * local_address, uint16_t local_port, time_t timeout)
{
#ifndef HAVE_IPV6
  struct hostent * remotehost;
  struct sockaddr_in sa;
#else /* HAVE_IPV6 */
  struct addrinfo hints, *res, *ai;
  struct addrinfo la_hints;
  char port_str[6];
#endif
#ifdef WIN32
  SOCKET s;
  long r;
#else
  int s;
  int r;
#endif

#ifndef HAVE_IPV6
  s = socket(PF_INET, SOCK_STREAM, 0);
  if (s == -1)
    goto err;

  if ((local_address != NULL) || (local_port != 0)) {
    struct sockaddr_in la;
    
    la.sin_family = AF_INET;
    la.sin_port = htons(local_port);
    if (local_address == NULL) {
      la.sin_addr.s_addr = htonl(INADDR_ANY);
    }
    r = inet_aton(local_address, &la.sin_addr);
    if (r == 0)
      goto close_socket;
    r = bind(s, (struct sockaddr *) &la, sizeof(struct sockaddr_in));
    if (r == -1)
      goto close_socket;
  }
  
  remotehost = gethostbyname(server);
  if (remotehost == NULL)
    goto close_socket;

  sa.sin_family = AF_INET;
  sa.sin_port = htons(port);
  memcpy(&sa.sin_addr, remotehost->h_addr, remotehost->h_length);
  
  r = prepare_fd(s);
  if (r == -1) {
    goto close_socket;
  }
  
  r = connect(s, (struct sockaddr *) &sa, sizeof(struct sockaddr_in));
  r = wait_connect(s, r, timeout);
  if (r == -1) {
    goto close_socket;
  }
#else /* HAVE_IPV6 */
  memset(&hints, 0, sizeof(hints));
  hints.ai_family = AF_UNSPEC;
  hints.ai_socktype = SOCK_STREAM;
  hints.ai_protocol = IPPROTO_TCP;
  
  memset(&la_hints, 0, sizeof(la_hints));
  la_hints.ai_family = AF_UNSPEC;
  la_hints.ai_socktype = SOCK_STREAM;
  la_hints.ai_flags = AI_PASSIVE;
  
  /* convert port from integer to string. */
  snprintf(port_str, sizeof(port_str), "%d", port);
  
  res = NULL;
  if (getaddrinfo(server, port_str, &hints, &res) != 0)
    goto err;

  s = -1;
  for (ai = res; ai != NULL; ai = ai->ai_next) {
    s = socket(ai->ai_family, ai->ai_socktype, ai->ai_protocol);

    if (s == -1)
      continue;
    
    // Christopher Lyon Anderson - prevent SigPipe
#ifdef SO_NOSIGPIPE
    int kOne = 1;
    int err = setsockopt(s, SOL_SOCKET, SO_NOSIGPIPE, &kOne, sizeof(kOne));
    if (err != 0)
        continue;
#endif

    if ((local_address != NULL) || (local_port != 0)) {
      char local_port_str[6];
      char * p_local_port_str;
      struct addrinfo * la_res;
      
      if (local_port != 0) {
        snprintf(local_port_str, sizeof(local_port_str), "%d", local_port);
        p_local_port_str = local_port_str;
      }
      else {
        p_local_port_str = NULL;
      }
      la_res = NULL;
      r = getaddrinfo(local_address, p_local_port_str, &la_hints, &la_res);
      if (r != 0)
        goto close_socket;
      r = bind(s, (struct sockaddr *) la_res->ai_addr, la_res->ai_addrlen);
      if (la_res != NULL)
        freeaddrinfo(la_res);
      if (r == -1)
        goto close_socket;
    }
    
    r = prepare_fd(s);
    if (r == -1) {
      goto close_socket;
    }
    
    r = connect(s, ai->ai_addr, ai->ai_addrlen);
    r = wait_connect(s, r, timeout);
    
    if (r != -1) {
      r = verify_sock_errors(s);
    }

    if (r == -1) {
      if (ai->ai_next) {
#ifdef WIN32
	  closesocket(s);
#else
	  close(s);
#endif
	  continue;
      } else {
        goto close_socket;
      }
    }
    /* if we're here, we're good */
    break;
  }
  
  if (res != NULL)
    freeaddrinfo(res);

  if (ai == NULL)
    goto err;
#endif
  return s;
  
 close_socket:
#ifdef HAVE_IPV6
  if (res != NULL)
    freeaddrinfo(res);
#endif
#ifdef WIN32
  closesocket(s);
#else
  close(s);
#endif
 err:
  return -1;
}
