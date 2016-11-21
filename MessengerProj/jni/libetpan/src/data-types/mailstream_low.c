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
 * $Id: mailstream_low.c,v 1.27 2011/05/04 16:09:54 hoa Exp $
 */

#ifdef HAVE_CONFIG_H
#	include <config.h>
#endif

#include "mailstream_low.h"
#include <stdlib.h>

#ifdef LIBETPAN_MAILSTREAM_DEBUG

#define STREAM_DEBUG

#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <string.h>
#ifdef HAVE_UNISTD_H
#	include <unistd.h>
#endif
#include "maillock.h"
#ifdef WIN32
#	include "win_etpan.h"
#endif
#if defined(ANDROID) || defined(__ANDROID__)
#include <sys/select.h>
#endif

#if USE_POLL && defined(HAVE_SYS_POLL_H)
#include <sys/poll.h>
#endif

#include "mailstream_cfstream.h"
#include "mailstream_compress.h"
#include "mailstream_cancel.h"

#define LOG_FILE "libetpan-stream-debug.log"

LIBETPAN_EXPORT
int mailstream_debug = 0;

LIBETPAN_EXPORT
void (* mailstream_logger)(int direction,
    const char * str, size_t size) = NULL;
LIBETPAN_EXPORT
void (* mailstream_logger_id)(mailstream_low * s, int is_stream_data, int direction,
    const char * str, size_t size) = NULL;

static inline void mailstream_logger_internal(mailstream_low * s, int is_stream_data, int direction,
    const char * buffer, size_t size);

// Will log a buffer.
#define STREAM_LOG_ERROR(low, direction, buf, size) \
  mailstream_logger_internal(low, 2, direction, buf, size); \
  if (mailstream_debug) { \
	  if (mailstream_logger_id != NULL) { \
	    mailstream_logger_id(low, 2, direction, buf, size); \
	  } \
    else if (mailstream_logger != NULL) { \
      mailstream_logger(direction, buf, size); \
    } \
    else { \
      FILE * f; \
      mode_t old_mask; \
      \
      old_mask = umask(0077); \
      f = fopen(LOG_FILE, "a"); \
      umask(old_mask); \
      if (f != NULL) { \
        maillock_write_lock(LOG_FILE, fileno(f)); \
        fwrite((buf), 1, (size), f); \
        maillock_write_unlock(LOG_FILE, fileno(f)); \
        fclose(f); \
      } \
    } \
  }

// Will log a buffer.
#define STREAM_LOG_BUF(low, direction, buf, size) \
  mailstream_logger_internal(low, 1, direction, buf, size); \
  if (mailstream_debug) { \
  	if (mailstream_logger_id != NULL) { \
  	  mailstream_logger_id(low, 1, direction, buf, size); \
  	} \
    else if (mailstream_logger != NULL) { \
      mailstream_logger(direction, buf, size); \
    } \
    else { \
      FILE * f; \
      mode_t old_mask; \
      \
      old_mask = umask(0077); \
      f = fopen(LOG_FILE, "a"); \
      umask(old_mask); \
      if (f != NULL) { \
        maillock_write_lock(LOG_FILE, fileno(f)); \
        fwrite((buf), 1, (size), f); \
        maillock_write_unlock(LOG_FILE, fileno(f)); \
        fclose(f); \
      } \
    } \
  }

// Will log some log text string.
#define STREAM_LOG(low, direction, str) \
  mailstream_logger_internal(low, 0, direction, str, strlen(str)); \
  if (mailstream_debug) { \
  	if (mailstream_logger_id != NULL) { \
  	  mailstream_logger_id(low, 0, direction, str, strlen(str)); \
  	} \
    else if (mailstream_logger != NULL) { \
      mailstream_logger(direction, str, strlen(str)); \
    } \
    else { \
      FILE * f; \
      mode_t old_mask; \
      \
      old_mask = umask(0077); \
      f = fopen(LOG_FILE, "a"); \
      umask(old_mask); \
      if (f != NULL) { \
        maillock_write_lock(LOG_FILE, fileno(f)); \
        fputs((str), f); \
        maillock_write_unlock(LOG_FILE, fileno(f)); \
        fclose(f); \
      } \
    } \
  }

#else

#define STREAM_LOG_ERROR(low, direction, buf, size) do { } while (0)
#define STREAM_LOG_BUF(low, direction, buf, size) do { } while (0)
#define STREAM_LOG(low, direction, buf) do { } while (0)

#endif


/* general functions */

mailstream_low * mailstream_low_new(void * data,
				    mailstream_low_driver * driver)
{
  mailstream_low * s;

  s = malloc(sizeof(* s));
  if (s == NULL)
    return NULL;

  s->data = data;
  s->driver = driver;
  s->privacy = 1;
	s->identifier = NULL;
	s->timeout = 0;
  s->logger = NULL;
  s->logger_context = NULL;
  
  return s;
}

int mailstream_low_close(mailstream_low * s)
{
  if (s == NULL)
    return -1;
  s->driver->mailstream_close(s);

  return 0;
}

int mailstream_low_get_fd(mailstream_low * s)
{
  if (s == NULL)
    return -1;
  return s->driver->mailstream_get_fd(s);
}

struct mailstream_cancel * mailstream_low_get_cancel(mailstream_low * s)
{
  if (s == NULL)
    return NULL;
  if (s->driver->mailstream_get_cancel == NULL)
    return NULL;
  return s->driver->mailstream_get_cancel(s);
}

void mailstream_low_free(mailstream_low * s)
{
	free(s->identifier);
	s->identifier = NULL;
  s->driver->mailstream_free(s);
}

ssize_t mailstream_low_read(mailstream_low * s, void * buf, size_t count)
{
  ssize_t r;
  
  if (s == NULL)
    return -1;
  r = s->driver->mailstream_read(s, buf, count);
  
#ifdef STREAM_DEBUG
  if (r > 0) {
    STREAM_LOG(s, 0, "<<<<<<< read <<<<<<\n");
    STREAM_LOG_BUF(s, 0, buf, r);
    STREAM_LOG(s, 0, "\n");
    STREAM_LOG(s, 0, "<<<<<<< end read <<<<<<\n");
  }
#endif
  
  if (r < 0) {
    STREAM_LOG_ERROR(s, 4, buf, 0);
  }
  
  return r;
}

ssize_t mailstream_low_write(mailstream_low * s,
    const void * buf, size_t count)
{
  ssize_t r;
  
  if (s == NULL)
    return -1;

#ifdef STREAM_DEBUG
  STREAM_LOG(s, 1, ">>>>>>> send >>>>>>\n");
  if (s->privacy) {
    STREAM_LOG_BUF(s, 1, buf, count);
  }
  else {
    STREAM_LOG_BUF(s, 2, buf, count);
  }
  STREAM_LOG(s, 1, "\n");
  STREAM_LOG(s, 1, ">>>>>>> end send >>>>>>\n");
#endif

  r = s->driver->mailstream_write(s, buf, count);
  
  if (r < 0) {
    STREAM_LOG_ERROR(s, 4 | 1, buf, 0);
  }
  
  return r;
}

void mailstream_low_cancel(mailstream_low * s)
{
  if (s == NULL)
    return;
  
  if (s->driver->mailstream_cancel == NULL)
    return;
  
  s->driver->mailstream_cancel(s);
}

void mailstream_low_log_error(mailstream_low * s,
    const void * buf, size_t count)
{
	STREAM_LOG_ERROR(s, 0, buf, count);
}

void mailstream_low_set_privacy(mailstream_low * s, int can_be_public)
{
  s->privacy = can_be_public;
}

int mailstream_low_set_identifier(mailstream_low * s,
    char * identifier)
{
	free(s->identifier);
	s->identifier = NULL;
	
	if (identifier != NULL) {
		s->identifier = identifier;
  }

	return 0;
}

const char * mailstream_low_get_identifier(mailstream_low * s)
{
	return s->identifier;
}

void mailstream_low_set_timeout(mailstream_low * s,
  time_t timeout)
{
	s->timeout = timeout;
}

time_t mailstream_low_get_timeout(mailstream_low * s)
{
	return s->timeout;
}

void mailstream_low_set_logger(mailstream_low * s, void (* logger)(mailstream_low * s, int log_type,
  const char * str, size_t size, void * context), void * logger_context)
{
  s->logger = logger;
  s->logger_context = logger_context;
}

static inline void mailstream_logger_internal(mailstream_low * s, int is_stream_data, int direction,
  const char * buffer, size_t size)
{
  int log_type = -1;
  
  if (s->logger == NULL)
    return;
  
  /*
   stream data:
  0: log
  1: buffer
  2: error
  
  direction:
  4|1: send error
  4: receive error
  2: sent private data
  1: sent data
  0: received data
  */
  
  switch (is_stream_data) {
    case 0: {
      switch (direction) {
        case 1:
        case 2:
        case 4|1:
          log_type = MAILSTREAM_LOG_TYPE_INFO_SENT;
          break;
        case 0:
        case 4:
          log_type = MAILSTREAM_LOG_TYPE_INFO_RECEIVED;
          break;
      }
      break;
    }
    case 1: {
      switch (direction) {
        case 2:
          log_type = MAILSTREAM_LOG_TYPE_DATA_SENT_PRIVATE;
          break;
        case 1:
        case 4|1:
          log_type = MAILSTREAM_LOG_TYPE_DATA_SENT;
          break;
        case 0:
        case 4:
        default:
          log_type = MAILSTREAM_LOG_TYPE_DATA_RECEIVED;
          break;
      }
      break;
    }
    case 2: {
      switch (direction) {
        case 2:
        case 1:
        case 4|1:
          log_type = MAILSTREAM_LOG_TYPE_ERROR_SENT;
          break;
        case 4:
          log_type = MAILSTREAM_LOG_TYPE_ERROR_RECEIVED;
          break;
        case 0:
          log_type = MAILSTREAM_LOG_TYPE_ERROR_PARSE;
          break;
      }
      break;
    }
  }
  
  if (log_type == -1)
    return;
  
  s->logger(s, log_type, buffer, size, s->logger_context);
}

carray * mailstream_low_get_certificate_chain(mailstream_low * s)
{
  if (s == NULL)
    return NULL;

  if (s->driver->mailstream_get_certificate_chain == NULL)
    return NULL;

  return s->driver->mailstream_get_certificate_chain(s);
}

int mailstream_low_wait_idle(mailstream_low * low, struct mailstream_cancel * idle,
                             int max_idle_delay)
{
  int fd;
  int idle_fd;
  int cancel_fd;
  struct timeval delay;
  int r;
#if defined(WIN32)
  fd_set readfds;
#elif USE_POLL
  struct pollfd pfd[3];
#else
  int maxfd;
  fd_set readfds;
#endif
  
  if (low->driver == mailstream_cfstream_driver) {
    return mailstream_low_cfstream_wait_idle(low, max_idle_delay);
  }
  else if (low->driver == mailstream_compress_driver) {
    return mailstream_low_compress_wait_idle(low, idle, max_idle_delay);
  }
  
  if (idle == NULL) {
		return MAILSTREAM_IDLE_ERROR;
	}
  if (mailstream_low_get_cancel(low) == NULL) {
		return MAILSTREAM_IDLE_ERROR;
	}
  fd = mailstream_low_get_fd(low);
  idle_fd = mailstream_cancel_get_fd(idle);
  cancel_fd = mailstream_cancel_get_fd(mailstream_low_get_cancel(low));
  
#if defined(WIN32)
  FD_ZERO(&readfds);
  HANDLE event = CreateEvent(NULL, TRUE, FALSE, NULL);
  WSAEventSelect(fd, event, FD_READ | FD_CLOSE);
  FD_SET(event, &readfds);
  FD_SET(idle_fd, &readfds);
  FD_SET(cancel_fd, &readfds);
  r = WaitForMultipleObjects(readfds.fd_count, readfds.fd_array, FALSE, max_idle_delay * 1000);
  WSAEventSelect(fd, event, 0);
  CloseHandle(event);
  if (r == WAIT_TIMEOUT) {
	  return MAILSTREAM_IDLE_TIMEOUT;
  }
  else if (r == WAIT_OBJECT_0){
	  return MAILSTREAM_IDLE_HASDATA;
  }
  else if (r == WAIT_OBJECT_0 + 1){
	  return MAILSTREAM_IDLE_INTERRUPTED;
  }
  else if (r == WAIT_OBJECT_0 + 2){
	  return MAILSTREAM_IDLE_CANCELLED;
  }
  DWORD i = GetLastError();
  return MAILSTREAM_IDLE_ERROR;
#elif USE_POLL
  pfd[0].fd = fd;
  pfd[0].events = POLLIN;
  pfd[0].revents = 0;

  pfd[1].fd = idle_fd;
  pfd[1].events = POLLIN;
  pfd[1].revents = 0;

  pfd[2].fd = cancel_fd;
  pfd[2].events = POLLIN;
  pfd[2].revents = 0;

  r = poll(&pfd[0], 3, max_idle_delay * 1000);

  if (r == 0){
    // timeout
    return MAILSTREAM_IDLE_TIMEOUT;
  }
  else if (r == -1) {
    // do nothing
    return MAILSTREAM_IDLE_ERROR;
  }
  else {
    if (pfd[0].revents & POLLIN) {
      // has something on socket
      return MAILSTREAM_IDLE_HASDATA;
    }
    if (pfd[1].revents & POLLIN) {
      // idle interrupted
      mailstream_cancel_ack(idle);
      return MAILSTREAM_IDLE_INTERRUPTED;
    }
    if (pfd[2].revents & POLLIN) {
      // idle cancelled
      mailstream_cancel_ack(mailstream_low_get_cancel(low));
      return MAILSTREAM_IDLE_CANCELLED;
    }
    return MAILSTREAM_IDLE_ERROR;
  }
#else
  FD_ZERO(&readfds);
  FD_SET(fd, &readfds);
  FD_SET(idle_fd, &readfds);
  FD_SET(cancel_fd, &readfds);
  maxfd = fd;
  if (idle_fd > maxfd) {
    maxfd = idle_fd;
  }
  if (cancel_fd > maxfd) {
    maxfd = cancel_fd;
  }
  delay.tv_sec = max_idle_delay;
  delay.tv_usec = 0;
  
  r = select(maxfd + 1, &readfds, NULL, NULL, &delay);
  if (r == 0) {
    // timeout
    return MAILSTREAM_IDLE_TIMEOUT;
  }
  else if (r == -1) {
    // do nothing
    return MAILSTREAM_IDLE_ERROR;
  }
  else {
    if (FD_ISSET(fd, &readfds)) {
      // has something on socket
      return MAILSTREAM_IDLE_HASDATA;
    }
    if (FD_ISSET(idle_fd, &readfds)) {
      // idle interrupted
      mailstream_cancel_ack(idle);
      return MAILSTREAM_IDLE_INTERRUPTED;
    }
    if (FD_ISSET(cancel_fd, &readfds)) {
      // idle cancelled
      mailstream_cancel_ack(mailstream_low_get_cancel(low));
      return MAILSTREAM_IDLE_CANCELLED;
    }
    return MAILSTREAM_IDLE_ERROR;
  }
#endif
}

int mailstream_low_setup_idle(mailstream_low * low)
{
  if (low->driver->mailstream_setup_idle == NULL)
    return -1;
  
  return low->driver->mailstream_setup_idle(low);
}

int mailstream_low_unsetup_idle(mailstream_low * low)
{
  if (low->driver->mailstream_unsetup_idle == NULL)
    return -1;
  
  return low->driver->mailstream_unsetup_idle(low);
}

int mailstream_low_interrupt_idle(mailstream_low * low)
{
  if (low->driver->mailstream_interrupt_idle == NULL)
    return -1;
  
  return low->driver->mailstream_interrupt_idle(low);
}

