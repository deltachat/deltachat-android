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
#include "mailstream_cancel.h"

#ifdef HAVE_CONFIG_H
#	include <config.h>
#endif

#ifdef WIN32
#	include <win_etpan.h>
#endif

#ifdef LIBETPAN_REENTRANT
#	ifndef WIN32
#		include <pthread.h>
#	endif
#endif

#include <stdlib.h>
#ifdef HAVE_UNISTD_H
#	include <unistd.h>
#endif

#ifdef WIN32
#	include <io.h>
#	include <fcntl.h>
#endif

#ifdef LIBETPAN_REENTRANT
#	ifdef WIN32
#		define MUTEX_KEY	CRITICAL_SECTION
		static int MUTEX_INIT(CRITICAL_SECTION* mutex) {
			InitializeCriticalSection( mutex);
			return 0;
		}
#		define MUTEX_LOCK(x) EnterCriticalSection(x)
#		define MUTEX_UNLOCK(x) LeaveCriticalSection(x)
#		define MUTEX_DESTROY(x) DeleteCriticalSection(x);
#	else
#		define MUTEX_KEY	 pthread_mutex_t
#		define MUTEX_INIT(x) pthread_mutex_init(x, NULL)
#		define MUTEX_DESTROY(x) pthread_mutex_destroy(x);
#		define MUTEX_LOCK(x) pthread_mutex_lock(x)
#		define MUTEX_UNLOCK(x) pthread_mutex_unlock(x)
#	endif
#else
#	define MUTEX_INIT(x)
#	define MUTEX_DESTROY(x)
#	define MUTEX_LOCK(x)
#	define MUTEX_UNLOCK(x)
#endif

struct mailstream_cancel_internal {
#ifdef LIBETPAN_REENTRANT
  MUTEX_KEY ms_lock;
#endif
#ifdef WIN32
  HANDLE event;
#endif
};

struct mailstream_cancel * mailstream_cancel_new(void)
{
  int r;
  struct mailstream_cancel * cancel;
  struct mailstream_cancel_internal * ms_internal;
  
  cancel = malloc(sizeof(struct mailstream_cancel));
  if (cancel == NULL)
    goto err;
  
  cancel->ms_cancelled = 0;
  
  ms_internal = malloc(sizeof(* ms_internal));
  if (ms_internal == NULL)
    goto free;
  cancel->ms_internal = ms_internal;
  if (cancel->ms_internal == NULL)
    goto free_internal;
  
#ifndef WIN32  
  r = pipe(cancel->ms_fds);
  if (r < 0)
    goto free_internal;
#else
  ms_internal->event = CreateEvent(NULL, TRUE, FALSE, NULL);
  if (ms_internal->event == NULL)
    goto free_internal;
#endif
  
#ifdef LIBETPAN_REENTRANT
  r = MUTEX_INIT(&ms_internal->ms_lock);
  if (r != 0)
    goto close_pipe;
#endif
  
  return cancel;
  
 close_pipe:
#ifndef WIN32  
  close(cancel->ms_fds[0]);
  close(cancel->ms_fds[1]);
#else
  CloseHandle(ms_internal->event);
#endif
 free_internal:
  free(cancel->ms_internal);
 free:
  free(cancel);
 err:
  return NULL;
}

void mailstream_cancel_free(struct mailstream_cancel * cancel)
{
  struct mailstream_cancel_internal * ms_internal;
  
  ms_internal = cancel->ms_internal;

  MUTEX_DESTROY(&ms_internal->ms_lock);

#ifndef WIN32  
  close(cancel->ms_fds[0]);
  close(cancel->ms_fds[1]);
#else
  CloseHandle(ms_internal->event);
#endif
  free(cancel->ms_internal);
  free(cancel);
}

void mailstream_cancel_notify(struct mailstream_cancel * cancel)
{
  char ch;
  struct mailstream_cancel_internal * ms_internal;
  
  ms_internal = cancel->ms_internal;
  MUTEX_LOCK(&ms_internal->ms_lock);

  cancel->ms_cancelled = 1;

  MUTEX_UNLOCK(&ms_internal->ms_lock);
  
  ch = 0;
#ifndef WIN32
  write(cancel->ms_fds[1], &ch, 1);
#else
  SetEvent(ms_internal->event);
#endif
}

void mailstream_cancel_ack(struct mailstream_cancel * cancel)
{
#ifndef WIN32
  char ch;
  read(cancel->ms_fds[0], &ch, 1);
#endif
}

int mailstream_cancel_cancelled(struct mailstream_cancel * cancel)
{
  int cancelled;
  struct mailstream_cancel_internal * ms_internal;
  
  ms_internal = cancel->ms_internal;

  MUTEX_LOCK(&ms_internal->ms_lock);

  cancelled = cancel->ms_cancelled;

  MUTEX_UNLOCK(&ms_internal->ms_lock);


  return cancelled;
}

int mailstream_cancel_get_fd(struct mailstream_cancel * cancel)
{
#ifndef WIN32
  return cancel->ms_fds[0];
#else
  struct mailstream_cancel_internal * ms_internal;
  
  ms_internal = cancel->ms_internal;

  return ms_internal->event;
#endif
}
