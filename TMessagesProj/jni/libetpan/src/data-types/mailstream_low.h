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
 * $Id: mailstream_low.h,v 1.15 2011/05/04 16:01:10 hoa Exp $
 */

#ifndef MAILSTREAM_LOW_H

#define MAILSTREAM_LOW_H

#include <sys/types.h>
#include <libetpan/mailstream_types.h>

#ifdef __cplusplus
extern "C" {
#endif

/* general functions */

LIBETPAN_EXPORT
mailstream_low * mailstream_low_new(void * data,
				    mailstream_low_driver * driver);

ssize_t mailstream_low_write(mailstream_low * s,
    const void * buf, size_t count);

ssize_t mailstream_low_read(mailstream_low * s, void * buf, size_t count);

LIBETPAN_EXPORT
int mailstream_low_close(mailstream_low * s);

LIBETPAN_EXPORT
int mailstream_low_get_fd(mailstream_low * s);

LIBETPAN_EXPORT
struct mailstream_cancel * mailstream_low_get_cancel(mailstream_low * s);

LIBETPAN_EXPORT
void mailstream_low_free(mailstream_low * s);

LIBETPAN_EXPORT
void mailstream_low_cancel(mailstream_low * s);

LIBETPAN_EXPORT
void mailstream_low_log_error(mailstream_low * s,
	const void * buf, size_t count);

LIBETPAN_EXPORT
void mailstream_low_set_privacy(mailstream_low * s, int can_be_public);

LIBETPAN_EXPORT
int mailstream_low_set_identifier(mailstream_low * s,
  char * identifier);

LIBETPAN_EXPORT
const char * mailstream_low_get_identifier(mailstream_low * s);

LIBETPAN_EXPORT
void mailstream_low_set_timeout(mailstream_low * s,
  time_t timeout);

LIBETPAN_EXPORT
time_t mailstream_low_get_timeout(mailstream_low * s);

LIBETPAN_EXPORT
void mailstream_low_set_logger(mailstream_low * s, void (* logger)(mailstream_low * s, int log_type,
  const char * str, size_t size, void * context), void * logger_context);

/* Get certificate chain. Returns an array of MMAPString containing DER data or NULL if it's not a SSL connection */
LIBETPAN_EXPORT
carray * mailstream_low_get_certificate_chain(mailstream_low * s);

LIBETPAN_EXPORT
int mailstream_low_wait_idle(mailstream_low * low, struct mailstream_cancel * cancel,
                             int max_idle_delay);

/*
  All those functions returns -1 if interrupt idle is not implemented and
  should be based on select().
*/
LIBETPAN_EXPORT
int mailstream_low_setup_idle(mailstream_low * low);

LIBETPAN_EXPORT
int mailstream_low_unsetup_idle(mailstream_low * low);

LIBETPAN_EXPORT
int mailstream_low_interrupt_idle(mailstream_low * low);
  
#ifdef __cplusplus
}
#endif

#endif
