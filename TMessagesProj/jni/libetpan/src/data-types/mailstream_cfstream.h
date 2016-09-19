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

#ifndef MAILSTREAM_CFSTREAM_H

#define MAILSTREAM_CFSTREAM_H

#include <libetpan/libetpan-config.h>
#include <libetpan/mailstream.h>

#ifdef __cplusplus
extern "C" {
#endif
  
  LIBETPAN_EXPORT
  extern int mailstream_cfstream_enabled;
  
  LIBETPAN_EXPORT
  extern int mailstream_cfstream_voip_enabled;
  
  enum {
    MAILSTREAM_CFSTREAM_SSL_ALLOWS_EXPIRED_CERTIFICATES = 1 << 0,
    MAILSTREAM_CFSTREAM_SSL_ALLOWS_EXPIRED_ROOTS = 1 << 1,
    MAILSTREAM_CFSTREAM_SSL_ALLOWS_ANY_ROOT = 1 << 2,
    MAILSTREAM_CFSTREAM_SSL_DISABLE_VALIDATES_CERTIFICATE_CHAIN = 1 << 3,
    MAILSTREAM_CFSTREAM_SSL_NO_VERIFICATION = MAILSTREAM_CFSTREAM_SSL_ALLOWS_EXPIRED_CERTIFICATES | 
       MAILSTREAM_CFSTREAM_SSL_ALLOWS_EXPIRED_ROOTS |
       MAILSTREAM_CFSTREAM_SSL_ALLOWS_ANY_ROOT |
       MAILSTREAM_CFSTREAM_SSL_DISABLE_VALIDATES_CERTIFICATE_CHAIN
  };
  
  enum {
    MAILSTREAM_CFSTREAM_SSL_LEVEL_NONE,
    MAILSTREAM_CFSTREAM_SSL_LEVEL_SSLv2,
    MAILSTREAM_CFSTREAM_SSL_LEVEL_SSLv3,
    MAILSTREAM_CFSTREAM_SSL_LEVEL_TLSv1,
    MAILSTREAM_CFSTREAM_SSL_LEVEL_NEGOCIATED_SSL
  };
  
  /* socket */
  
  extern mailstream_low_driver * mailstream_cfstream_driver;
  
  mailstream * mailstream_cfstream_open(const char * hostname, int16_t port);
  mailstream * mailstream_cfstream_open_timeout(const char * hostname, int16_t port, time_t timeout);
  mailstream * mailstream_cfstream_open_voip(const char * hostname, int16_t port, int voip_enabled);
  mailstream * mailstream_cfstream_open_voip_timeout(const char * hostname, int16_t port, int voip_enabled,
		time_t timeout);
	
  mailstream_low * mailstream_low_cfstream_open(const char * hostname, int16_t port);
	mailstream_low * mailstream_low_cfstream_open_timeout(const char * hostname, int16_t port,
		time_t timeout);
  mailstream_low * mailstream_low_cfstream_open_voip(const char * hostname, int16_t port, int voip_enabled);
  mailstream_low * mailstream_low_cfstream_open_voip_timeout(const char * hostname, int16_t port,
    int voip_enabled, time_t timeout);
  
  /* first, set these settings */
  void mailstream_cfstream_set_ssl_verification_mask(mailstream * s, int verification_mask);
  void mailstream_cfstream_set_ssl_peer_name(mailstream * s, const char * peer_name);
  void mailstream_cfstream_set_ssl_is_server(mailstream * s, int is_server);
  void mailstream_cfstream_set_ssl_level(mailstream * s, int ssl_level);
  /* missing setting certificate */
  
  /* then, enable SSL */
  int mailstream_cfstream_set_ssl_enabled(mailstream * s, int ssl_enabled);
  int mailstream_cfstream_is_ssl_enabled(mailstream * s);
  
  /* support for IMAP IDLE */
  
  /* can be run in thread */
  int mailstream_cfstream_wait_idle(mailstream * s, int max_idle_delay);
  int mailstream_low_cfstream_wait_idle(mailstream_low * low, int max_idle_delay);
  
  /* in main thread */
  /*
  void mailstream_low_cfstream_setup_idle(mailstream_low * s);
  void mailstream_low_cfstream_interrupt_idle(mailstream_low * s);
  void mailstream_low_cfstream_unsetup_idle(mailstream_low * s);
   */
  /* SSL certificate */
  
#ifdef __cplusplus
}
#endif

#endif
