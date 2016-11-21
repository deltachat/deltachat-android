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
 * $Id: mailstream_ssl.h,v 1.18 2008/07/03 19:42:27 colinleroy Exp $
 */

#ifndef MAILSTREAM_SSL_H

#define MAILSTREAM_SSL_H

#include <libetpan/mailstream.h>

#ifdef __cplusplus
extern "C" {
#endif

/* socket */

#if LIBETPAN_IOS_DISABLE_SSL
#undef USE_SSL
#endif

#ifdef USE_SSL
extern mailstream_low_driver * mailstream_ssl_driver;
#endif

struct mailstream_ssl_context;

LIBETPAN_EXPORT
mailstream_low * mailstream_low_ssl_open(int fd);

LIBETPAN_EXPORT
mailstream_low * mailstream_low_ssl_open_timeout(int fd, time_t timeout);

LIBETPAN_EXPORT
mailstream_low * mailstream_low_tls_open(int fd);

LIBETPAN_EXPORT
mailstream_low * mailstream_low_tls_open_timeout(int fd, time_t timeout);

LIBETPAN_EXPORT
mailstream * mailstream_ssl_open(int fd);

LIBETPAN_EXPORT
mailstream * mailstream_ssl_open_timeout(int fd, time_t timeout);

LIBETPAN_EXPORT
mailstream * mailstream_ssl_open_with_callback(int fd,
    void (* callback)(struct mailstream_ssl_context * ssl_context, void * data), void * data);

LIBETPAN_EXPORT
mailstream * mailstream_ssl_open_with_callback_timeout(int fd, time_t timeout,
    void (* callback)(struct mailstream_ssl_context * ssl_context, void * data), void * data);

LIBETPAN_EXPORT
void mailstream_gnutls_init_not_required(void);

LIBETPAN_EXPORT
void mailstream_openssl_init_not_required(void);

LIBETPAN_EXPORT
void mailstream_ssl_init_not_required(void);

LIBETPAN_EXPORT
ssize_t mailstream_ssl_get_certificate(mailstream *stream, unsigned char **cert_DER);

LIBETPAN_EXPORT
mailstream_low * mailstream_low_ssl_open_with_callback(int fd,
    void (* callback)(struct mailstream_ssl_context * ssl_context, void * data), void * data);

LIBETPAN_EXPORT
mailstream_low * mailstream_low_ssl_open_with_callback_timeout(int fd, time_t timeout,
    void (* callback)(struct mailstream_ssl_context * ssl_context, void * data), void * data);

LIBETPAN_EXPORT
mailstream_low * mailstream_low_tls_open_with_callback(int fd,
    void (* callback)(struct mailstream_ssl_context * ssl_context, void * data), void * data);

LIBETPAN_EXPORT
mailstream_low * mailstream_low_tls_open_with_callback_timeout(int fd, time_t timeout,
    void (* callback)(struct mailstream_ssl_context * ssl_context, void * data), void * data);

LIBETPAN_EXPORT
int mailstream_ssl_set_client_certicate(struct mailstream_ssl_context * ssl_context,
    char * file_name);

LIBETPAN_EXPORT
int mailstream_ssl_set_client_certificate_data(struct mailstream_ssl_context * ssl_context,
    unsigned char *x509_der, size_t len);
int mailstream_ssl_set_client_private_key_data(struct mailstream_ssl_context * ssl_context,
    unsigned char *pkey_der, size_t len);

LIBETPAN_EXPORT
int mailstream_ssl_set_server_certicate(struct mailstream_ssl_context * ssl_context, 
    char * CAfile, char * CApath);

LIBETPAN_EXPORT
void * mailstream_ssl_get_openssl_ssl_ctx(struct mailstream_ssl_context * ssl_context);

LIBETPAN_EXPORT
int mailstream_ssl_get_fd(struct mailstream_ssl_context * ssl_context);

#ifdef __cplusplus
}
#endif

#endif
