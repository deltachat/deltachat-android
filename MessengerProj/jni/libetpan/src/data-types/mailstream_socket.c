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
 * $Id: mailstream_socket.c,v 1.33 2011/04/15 10:43:31 hoa Exp $
 */

#ifdef HAVE_CONFIG_H
#	include <config.h>
#endif

#include "mailstream_socket.h"

#ifdef HAVE_UNISTD_H
#	include <unistd.h>
#endif
#include <stdlib.h>
#include <fcntl.h>
#ifdef HAVE_SYS_SOCKET_H
#	include <sys/socket.h>
#endif

#ifdef HAVE_STRING_H
#  include <string.h>
#endif

/*
  these 3 headers MUST be included before <sys/select.h>
  to insure compatibility with Mac OS X (this is true for 10.2)
*/

#ifdef WIN32
#	include <win_etpan.h>
#else
#	include <sys/time.h>
#	include <sys/types.h>
#   if USE_POLL
#       ifdef HAVE_SYS_POLL_H
#           include <sys/poll.h>
#       endif
#   else
#       ifdef HAVE_SYS_SELECT_H
#           include <sys/select.h>
#       endif
#   endif
#endif

#include "mailstream_cancel.h"

struct mailstream_socket_data {
  int fd;
  struct mailstream_cancel * cancel;
  int use_read;
};

/* mailstream_low, socket */

static int mailstream_low_socket_close(mailstream_low * s);
static ssize_t mailstream_low_socket_read(mailstream_low * s,
					  void * buf, size_t count);
static ssize_t mailstream_low_socket_write(mailstream_low * s,
					   const void * buf, size_t count);
static void mailstream_low_socket_free(mailstream_low * s);
static int mailstream_low_socket_get_fd(mailstream_low * s);
static void mailstream_low_socket_cancel(mailstream_low * s);
static struct mailstream_cancel * mailstream_low_socket_get_cancel(mailstream_low * s);

static mailstream_low_driver local_mailstream_socket_driver = {
  /* mailstream_read */ mailstream_low_socket_read,
  /* mailstream_write */ mailstream_low_socket_write,
  /* mailstream_close */ mailstream_low_socket_close,
  /* mailstream_get_fd */ mailstream_low_socket_get_fd,
  /* mailstream_free */ mailstream_low_socket_free,
  /* mailstream_cancel */ mailstream_low_socket_cancel,
  /* mailstream_get_cancel */ mailstream_low_socket_get_cancel,
  /* mailstream_get_certificate_chain */ NULL,
  /* mailstream_setup_idle */ NULL,
  /* mailstream_unsetup_idle */ NULL,
  /* mailstream_interrupt_idle */ NULL,
};

mailstream_low_driver * mailstream_socket_driver =
&local_mailstream_socket_driver;

/* file descriptor must be given in (default) blocking-mode */

static struct mailstream_socket_data * socket_data_new(int fd)
{
  struct mailstream_socket_data * socket_data;

  socket_data = (struct mailstream_socket_data * ) malloc(sizeof(* socket_data));
  if (socket_data == NULL)
    goto err;
  
  socket_data->fd = fd;
  socket_data->use_read = 0;
  socket_data->cancel = mailstream_cancel_new();
  if (socket_data->cancel == NULL)
    goto free;
  
  return socket_data;
  
 free:
  free(socket_data);
 err:
  return NULL;
}

static void socket_data_free(struct mailstream_socket_data * socket_data)
{
  mailstream_cancel_free(socket_data->cancel);
  free(socket_data);
}

static void socket_data_close(struct mailstream_socket_data * socket_data)
{
#ifdef WIN32
/* SEB */ 
  closesocket(socket_data->fd);
#else
  close(socket_data->fd);
#endif
  socket_data->fd = -1;
}

mailstream_low * mailstream_low_socket_open(int fd)
{
  mailstream_low * s;
  struct mailstream_socket_data * socket_data;

  socket_data = socket_data_new(fd);
  if (socket_data == NULL)
    goto err;

  s = mailstream_low_new(socket_data, mailstream_socket_driver);
  if (s == NULL)
    goto free_socket_data;

  return s;

 free_socket_data:
  socket_data_free(socket_data);
 err:
  return NULL;
}

static int mailstream_low_socket_close(mailstream_low * s)
{
  struct mailstream_socket_data * socket_data;

  socket_data = (struct mailstream_socket_data *) s->data;
  socket_data_close(socket_data);

  return 0;
}

static void mailstream_low_socket_free(mailstream_low * s)
{
  struct mailstream_socket_data * socket_data;

  socket_data = (struct mailstream_socket_data *) s->data;
  socket_data_free(socket_data);
  s->data = NULL;

  free(s);
}

static int mailstream_low_socket_get_fd(mailstream_low * s)
{
  struct mailstream_socket_data * socket_data;

  socket_data = (struct mailstream_socket_data *) s->data;
  return socket_data->fd;
}


static ssize_t mailstream_low_socket_read(mailstream_low * s,
					  void * buf, size_t count)
{
  struct mailstream_socket_data * socket_data;
  
  socket_data = (struct mailstream_socket_data *) s->data;

  if (mailstream_cancel_cancelled(socket_data->cancel))
    return -1;
  
  /* timeout */
  {
    struct timeval timeout;
    int r;
    int fd;
    int cancelled;
    int got_data;
#if defined(WIN32)
    fd_set fds_read;
    HANDLE event;
#elif USE_POLL
    struct pollfd pfd[2];
#else
    fd_set fds_read;
    int max_fd;
#endif
    
    if (s->timeout == 0) {
      timeout = mailstream_network_delay;
    }
    else {
      timeout.tv_sec = s->timeout;
      timeout.tv_usec = 0;
    }
    
    fd = mailstream_cancel_get_fd(socket_data->cancel);
    
#if defined(WIN32)
    FD_ZERO(&fds_read);
    FD_SET(fd, &fds_read);

    event = CreateEvent(NULL, TRUE, FALSE, NULL);
    WSAEventSelect(socket_data->fd, event, FD_READ | FD_CLOSE);
    FD_SET(event, &fds_read);
    r = WaitForMultipleObjects(fds_read.fd_count, fds_read.fd_array, FALSE, timeout.tv_sec * 1000 + timeout.tv_usec / 1000);
    if (WAIT_TIMEOUT == r) {
      WSAEventSelect(socket_data->fd, event, 0);
      CloseHandle(event);
      return -1;
    }
    
    cancelled = (fds_read.fd_array[r - WAIT_OBJECT_0] == fd);
    got_data = (fds_read.fd_array[r - WAIT_OBJECT_0] == event);
    WSAEventSelect(socket_data->fd, event, 0);
    CloseHandle(event);
#elif USE_POLL
    pfd[0].fd = socket_data->fd;
    pfd[0].events = POLLIN;
    pfd[0].revents = 0;

    pfd[1].fd = fd;
    pfd[1].events = POLLIN;
    pfd[1].revents = 0;

    r = poll(&pfd[0], 2, timeout.tv_sec * 1000 + timeout.tv_usec / 1000);
    if (r <= 0)
        return -1;

    cancelled = pfd[1].revents & POLLIN;
    got_data = pfd[0].revents & POLLIN;
#else
    FD_ZERO(&fds_read);
    FD_SET(fd, &fds_read);
    FD_SET(socket_data->fd, &fds_read);
    max_fd = fd > socket_data->fd ? fd : socket_data->fd;
    r = select(max_fd + 1, &fds_read, NULL,/* &fds_excp*/ NULL, &timeout);
    if (r <= 0)
      return -1;
    
    cancelled = FD_ISSET(fd, &fds_read);
    got_data = FD_ISSET(socket_data->fd, &fds_read);
#endif
    
    if (cancelled) {
      /* cancelled */
      mailstream_cancel_ack(socket_data->cancel);
      return -1;
    }
    
    if (!got_data)
      return 0;
  }
  
  if (socket_data->use_read) {
    return read(socket_data->fd, buf, count);
  }
  else {
    return recv(socket_data->fd, buf, count, 0);
  }
}

static ssize_t mailstream_low_socket_write(mailstream_low * s,
					   const void * buf, size_t count)
{
  struct mailstream_socket_data * socket_data;

  socket_data = (struct mailstream_socket_data *) s->data;
  
  if (mailstream_cancel_cancelled(socket_data->cancel))
    return -1;
  
  /* timeout */
  {
    struct timeval timeout;
    int r;
    int fd;
    int cancelled;
    int write_enabled;
#if defined(WIN32)
    fd_set fds_read;
    fd_set fds_write;
    HANDLE event;
#elif USE_POLL
    struct pollfd pfd[2];
#else
    fd_set fds_read;
    fd_set fds_write;
    int max_fd;
#endif
    
    if (s->timeout == 0) {
      timeout = mailstream_network_delay;
    }
    else {
      timeout.tv_sec = s->timeout;
      timeout.tv_usec = 0;
    }
    
    fd = mailstream_cancel_get_fd(socket_data->cancel);
#if defined(WIN32)
    FD_ZERO(&fds_read);
    FD_SET(fd, &fds_read);
    FD_ZERO(&fds_write);

    event = CreateEvent(NULL, TRUE, FALSE, NULL);
    WSAEventSelect(socket_data->fd, event, FD_WRITE | FD_CLOSE);
    FD_SET(event, &fds_read);
    r = WaitForMultipleObjects(fds_read.fd_count, fds_read.fd_array, FALSE, timeout.tv_sec * 1000 + timeout.tv_usec / 1000);
    if (r < 0) {
      WSAEventSelect(socket_data->fd, event, 0);
      CloseHandle(event);
      return -1;
    }
    
    cancelled = (fds_read.fd_array[r - WAIT_OBJECT_0] == fd);
    write_enabled = (fds_read.fd_array[r - WAIT_OBJECT_0] == event);
    WSAEventSelect(socket_data->fd, event, 0);
    CloseHandle(event);
#elif USE_POLL
    pfd[0].fd = socket_data->fd;
    pfd[0].events = POLLOUT;
    pfd[0].revents = 0;

    pfd[1].fd = fd;
    pfd[1].events = POLLIN;
    pfd[1].revents = 0;

    r = poll(&pfd[0], 2, timeout.tv_sec * 1000 + timeout.tv_usec / 1000);
    if (r <= 0)
      return -1;

    cancelled = pfd[1].revents & POLLIN;
    write_enabled = pfd[0].revents & POLLOUT;
#else
    FD_ZERO(&fds_read);
    FD_SET(fd, &fds_read);
    FD_ZERO(&fds_write);
    FD_SET(socket_data->fd, &fds_write);
    max_fd = fd > socket_data->fd ? fd : socket_data->fd;
    r = select(max_fd + 1, &fds_read, &fds_write, /*&fds_excp */ NULL, &timeout);
    if (r <= 0)
        return -1;

    cancelled = FD_ISSET(fd, &fds_read);
    write_enabled = FD_ISSET(socket_data->fd, &fds_write);
#endif
    
    if (cancelled) {
      /* cancelled */
      mailstream_cancel_ack(socket_data->cancel);
      return -1;
    }
    
    if (!write_enabled)
      return 0;
  }
  
  return send(socket_data->fd, buf, count, 0);
}


/* mailstream */

mailstream * mailstream_socket_open(int fd)
{
	return mailstream_socket_open_timeout(fd, 0);
}

mailstream * mailstream_socket_open_timeout(int fd, time_t timeout)
{
  mailstream_low * low;
  mailstream * s;

  low = mailstream_low_socket_open(fd);
  if (low == NULL)
    goto err;
	mailstream_low_set_timeout(low, timeout);

  s = mailstream_new(low, 8192);
  if (s == NULL)
    goto free_low;

  return s;

 free_low:
  mailstream_low_close(low);
 err:
  return NULL;
}

static void mailstream_low_socket_cancel(mailstream_low * s)
{
  struct mailstream_socket_data * socket_data;

  socket_data = (struct mailstream_socket_data *) s->data;
  mailstream_cancel_notify(socket_data->cancel);
}

void mailstream_socket_set_use_read(mailstream * stream, int use_read)
{
  struct mailstream_socket_data * socket_data;
  mailstream_low * low;
  
  low = mailstream_get_low(stream);
  socket_data = (struct mailstream_socket_data *) low->data;
  socket_data->use_read = use_read;
}

static struct mailstream_cancel * mailstream_low_socket_get_cancel(mailstream_low * s)
{
  struct mailstream_socket_data * socket_data;
  
  socket_data = (struct mailstream_socket_data *) s->data;
  return socket_data->cancel;
}
