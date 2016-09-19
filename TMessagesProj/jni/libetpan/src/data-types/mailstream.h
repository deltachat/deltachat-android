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
 * $Id: mailstream.h,v 1.21 2011/03/11 21:49:36 hoa Exp $
 */

#ifndef MAILSTREAM_H

#define MAILSTREAM_H

#ifndef _MSC_VER
#	include <sys/time.h>
#endif

#include <libetpan/mailstream_low.h>
#include <libetpan/mailstream_helper.h>
#include <libetpan/mailstream_socket.h>
#include <libetpan/mailstream_ssl.h>
#include <libetpan/mailstream_cfstream.h>
#include <libetpan/mailstream_types.h>

#ifdef __cplusplus
extern "C" {
#endif

LIBETPAN_EXPORT
mailstream * mailstream_new(mailstream_low * low, size_t buffer_size);

LIBETPAN_EXPORT
ssize_t mailstream_write(mailstream * s, const void * buf, size_t count);

LIBETPAN_EXPORT
ssize_t mailstream_read(mailstream * s, void * buf, size_t count);

LIBETPAN_EXPORT
int mailstream_close(mailstream * s);

LIBETPAN_EXPORT
int mailstream_flush(mailstream * s);

LIBETPAN_EXPORT
ssize_t mailstream_feed_read_buffer(mailstream * s);

LIBETPAN_EXPORT
void mailstream_log_error(mailstream * s, char * buf, size_t count);

LIBETPAN_EXPORT
mailstream_low * mailstream_get_low(mailstream * s);

LIBETPAN_EXPORT
void mailstream_set_low(mailstream * s, mailstream_low * low);

LIBETPAN_EXPORT
void mailstream_cancel(mailstream * s);

LIBETPAN_EXPORT
void mailstream_set_privacy(mailstream * s, int can_be_public);

#ifdef LIBETPAN_MAILSTREAM_DEBUG
LIBETPAN_EXPORT
extern int mailstream_debug;

/* direction is 1 for send, 0 for receive, -1 when it does not apply */
LIBETPAN_EXPORT
extern void (* mailstream_logger)(int direction,
    const char * str, size_t size);
LIBETPAN_EXPORT
extern void (* mailstream_logger_id)(mailstream_low * s, int is_stream_data, int direction,
	const char * str, size_t size);
#endif

LIBETPAN_EXPORT
void mailstream_set_logger(mailstream * s, void (* logger)(mailstream * s, int log_type,
  const char * str, size_t size, void * context), void * logger_context);

/* can be run in thread */
LIBETPAN_EXPORT
int mailstream_wait_idle(mailstream * s, int max_idle_delay);

/* in main thread */
LIBETPAN_EXPORT
int mailstream_setup_idle(mailstream * s);

LIBETPAN_EXPORT
void mailstream_unsetup_idle(mailstream * s);

LIBETPAN_EXPORT
void mailstream_interrupt_idle(mailstream * s);

/* Get certificate chain. Returns an array of MMAPString containing DER data or NULL if it's not a SSL connection */
LIBETPAN_EXPORT
carray * mailstream_get_certificate_chain(mailstream * s);

LIBETPAN_EXPORT
void mailstream_certificate_chain_free(carray * certificate_chain);

#define LIBETPAN_MAILSTREAM_NETWORK_DELAY
LIBETPAN_EXPORT
extern struct timeval mailstream_network_delay;

#ifdef __cplusplus
}
#endif

#endif

