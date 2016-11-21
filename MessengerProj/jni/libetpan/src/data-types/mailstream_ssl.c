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
 * $Id: mailstream_ssl.c,v 1.77 2011/08/30 19:42:16 colinleroy Exp $
 */

/*
  NOTE :

  The user has to call himself SSL_library_init() if he wants to
  use SSL.
*/

#ifdef HAVE_CONFIG_H
#	include <config.h>
#endif

#include "mailstream_ssl.h"
#include "mailstream_ssl_private.h"

#ifdef HAVE_UNISTD_H
#	include <unistd.h>
#endif
#ifdef HAVE_STDLIB_H
#	include <stdlib.h>
#endif
#ifdef HAVE_STRING_H
#	include <string.h>
#endif
#include <fcntl.h>

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
#	        include <sys/poll.h>
#       endif
#   else 
#       ifdef HAVE_SELECT_H
#	        include <sys/select.h>
#       endif
#   endif
#endif

#if LIBETPAN_IOS_DISABLE_SSL
#undef USE_SSL
#endif

/* mailstream_low, ssl */

#ifdef USE_SSL
# ifndef USE_GNUTLS
#  include <openssl/ssl.h>
# else
#  include <errno.h>
#  include <gnutls/gnutls.h>
#  include <gnutls/x509.h>
# endif
# ifdef LIBETPAN_REENTRANT
#	 if HAVE_PTHREAD_H
#	  include <pthread.h>
#  elif defined(WIN32)
    void mailprivacy_gnupg_init_lock();
    void mailprivacy_smime_init_lock();
#  endif
#	endif
#endif

#include "mmapstring.h"
#include "mailstream_cancel.h"

struct mailstream_ssl_context
{
  int fd;
#ifdef USE_SSL
#ifndef USE_GNUTLS
  SSL_CTX * openssl_ssl_ctx;
  X509* client_x509;
  EVP_PKEY *client_pkey;
#else
  gnutls_session session;
  gnutls_x509_crt client_x509;
  gnutls_x509_privkey client_pkey;
  gnutls_certificate_credentials_t gnutls_credentials;
#endif
#endif
};

#ifdef USE_SSL
#ifndef USE_GNUTLS
struct mailstream_ssl_data {
  int fd;
  SSL * ssl_conn;
  SSL_CTX * ssl_ctx;
  struct mailstream_cancel * cancel;
};

#else
struct mailstream_ssl_data {
  int fd;
  gnutls_session session;
  gnutls_certificate_credentials_t xcred;
  struct mailstream_cancel * cancel;
};
#endif
#endif

#ifdef USE_SSL
#ifdef LIBETPAN_REENTRANT
#	if HAVE_PTHREAD_H
#		define MUTEX_LOCK(x) pthread_mutex_lock(x)
#		define MUTEX_UNLOCK(x) pthread_mutex_unlock(x)
		static pthread_mutex_t ssl_lock = PTHREAD_MUTEX_INITIALIZER;
#	elif (defined WIN32)
#		define MUTEX_LOCK(x) EnterCriticalSection(x);
#		define MUTEX_UNLOCK(x) LeaveCriticalSection(x);
		static CRITICAL_SECTION ssl_lock;
#	else
#		error "What are your threads?"
#	endif
#else
#	define MUTEX_LOCK(x)
#	define MUTEX_UNLOCK(x)
#endif
static int openssl_init_done = 0;
#endif

// Used to make OpenSSL thread safe
#ifndef USE_GNUTLS
#if defined (HAVE_PTHREAD_H) && !defined (WIN32) && defined (USE_SSL) && defined (LIBETPAN_REENTRANT)
  struct CRYPTO_dynlock_value
  {
      pthread_mutex_t mutex;
  };

  static pthread_mutex_t * s_mutex_buf = NULL;

  static void locking_function(int mode, int n, const char * file, int line)
  {
    if (mode & CRYPTO_LOCK)
      MUTEX_LOCK(&s_mutex_buf[n]);
    else
      MUTEX_UNLOCK(&s_mutex_buf[n]);
  }

  static unsigned long id_function(void)
  {
    return ((unsigned long) pthread_self());
  }

  static struct CRYPTO_dynlock_value *dyn_create_function(const char *file, int line)
  {
    struct CRYPTO_dynlock_value *value;
    
    value = (struct CRYPTO_dynlock_value *) malloc(sizeof(struct CRYPTO_dynlock_value));
    if (!value) {
      goto err;
    }
    pthread_mutex_init(&value->mutex, NULL);
    
    return value;
    
  err:
    return (NULL);
  }

  static void dyn_lock_function(int mode, struct CRYPTO_dynlock_value *l, const char *file, int line)
  {
    if (mode & CRYPTO_LOCK) {
      MUTEX_LOCK(&l->mutex);
    }
    else {
      MUTEX_UNLOCK(&l->mutex);
    }
  }

  static void dyn_destroy_function(struct CRYPTO_dynlock_value *l,const char *file, int line)
  {
    pthread_mutex_destroy(&l->mutex);
    free(l);
  }

  static void mailstream_openssl_reentrant_setup(void)
  {
		int i;
	
    s_mutex_buf = (pthread_mutex_t *) malloc(CRYPTO_num_locks() * sizeof(* s_mutex_buf));
    if(s_mutex_buf == NULL)
      return;
    
    for(i = 0 ; i < CRYPTO_num_locks() ; i++)
      pthread_mutex_init(&s_mutex_buf[i], NULL);
    CRYPTO_set_id_callback(id_function);
    CRYPTO_set_locking_callback(locking_function);
    CRYPTO_set_dynlock_create_callback(dyn_create_function);
    CRYPTO_set_dynlock_lock_callback(dyn_lock_function);
    CRYPTO_set_dynlock_destroy_callback(dyn_destroy_function);
  }
#endif
#endif

void mailstream_ssl_init_lock(void)
{
#if !defined (HAVE_PTHREAD_H) && defined (WIN32) && defined (USE_SSL)
  static long volatile mailstream_ssl_init_lock_done = 0;
  if (InterlockedExchange(&mailstream_ssl_init_lock_done, 1) == 0) {
    InitializeCriticalSection(&ssl_lock);
  }
#endif
}

void mailstream_ssl_uninit_lock(void)
{
#if !defined (HAVE_PTHREAD_H) && defined (WIN32) && defined (USE_SSL)
	static long volatile mailstream_ssl_init_lock_done = 0;
	if (InterlockedExchange(&mailstream_ssl_init_lock_done, 1) == 0) {
		DeleteCriticalSection(&ssl_lock);
	}
#endif
}

void mailstream_gnutls_init_not_required(void)
{
}

void mailstream_openssl_init_not_required(void)
{
#ifdef USE_SSL
  MUTEX_LOCK(&ssl_lock);
  openssl_init_done = 1;
  MUTEX_UNLOCK(&ssl_lock);
#endif
}

void mailstream_ssl_init_not_required(void)
{
  mailstream_gnutls_init_not_required();
  mailstream_openssl_init_not_required();
}

static inline void mailstream_ssl_init(void)
{
#ifdef USE_SSL
  mailstream_ssl_init_lock();
  MUTEX_LOCK(&ssl_lock);
#ifndef USE_GNUTLS
  if (!openssl_init_done) {
    #if defined (HAVE_PTHREAD_H) && !defined (WIN32) && defined (USE_SSL) && defined (LIBETPAN_REENTRANT)
      mailstream_openssl_reentrant_setup();
    #endif
    
    SSL_load_error_strings();
    SSL_library_init();
    OpenSSL_add_all_algorithms();
    
    openssl_init_done = 1;
  }
#else
  gnutls_global_init();
#endif
  MUTEX_UNLOCK(&ssl_lock);
#endif
}

#ifdef USE_SSL
static inline int mailstream_prepare_fd(int fd)
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
#endif

static int wait_SSL_connect(int s, int want_read, time_t timeout_seconds)
{
  struct timeval timeout;
  int r;
#if defined(WIN32) || !USE_POLL
  fd_set fds;
#else
  struct pollfd pfd;
#endif // WIN32

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
  /* TODO: how to cancel this ? */
  if (want_read)
    r = select(s + 1, &fds, NULL, NULL, &timeout);
  else
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
  if (want_read) {
    pfd.events = POLLIN;
  }
  else {
    pfd.events = POLLOUT;
  }
  r = poll(&pfd, 1, timeout.tv_sec * 1000 + timeout.tv_usec / 1000);
  if (r <= 0) {
    return -1;
  }

  if (pfd.revents & pfd.events) {
    return -1;
  }
#endif
  
  return 0;
}

#ifdef USE_SSL
static int mailstream_low_ssl_close(mailstream_low * s);
static ssize_t mailstream_low_ssl_read(mailstream_low * s,
				       void * buf, size_t count);
static ssize_t mailstream_low_ssl_write(mailstream_low * s,
					const void * buf, size_t count);
static void mailstream_low_ssl_free(mailstream_low * s);
static int mailstream_low_ssl_get_fd(mailstream_low * s);
static void mailstream_low_ssl_cancel(mailstream_low * s);
static struct mailstream_cancel * mailstream_low_ssl_get_cancel(mailstream_low * s);
static carray * mailstream_low_ssl_get_certificate_chain(mailstream_low * s);

static mailstream_low_driver local_mailstream_ssl_driver = {
  /* mailstream_read */ mailstream_low_ssl_read,
  /* mailstream_write */ mailstream_low_ssl_write,
  /* mailstream_close */ mailstream_low_ssl_close,
  /* mailstream_get_fd */ mailstream_low_ssl_get_fd,
  /* mailstream_free */ mailstream_low_ssl_free,
  /* mailstream_cancel */ mailstream_low_ssl_cancel,
  /* mailstream_get_cancel */ mailstream_low_ssl_get_cancel,
  /* mailstream_get_certificate_chain */ mailstream_low_ssl_get_certificate_chain,
  /* mailstream_setup_idle */ NULL,
  /* mailstream_unsetup_idle */ NULL,
  /* mailstream_interrupt_idle */ NULL,
};

mailstream_low_driver * mailstream_ssl_driver = &local_mailstream_ssl_driver;
#endif

/* file descriptor must be given in (default) blocking-mode */

#ifdef USE_SSL
#ifndef USE_GNUTLS

static struct mailstream_ssl_context * mailstream_ssl_context_new(SSL_CTX * open_ssl_ctx, int fd);
static void mailstream_ssl_context_free(struct mailstream_ssl_context * ssl_ctx);

static int mailstream_openssl_client_cert_cb(SSL *ssl, X509 **x509, EVP_PKEY **pkey)
{
	struct mailstream_ssl_context * ssl_context = (struct mailstream_ssl_context *)SSL_CTX_get_app_data(ssl->ctx);
	
	if (x509 == NULL || pkey == NULL) {
		return 0;
	}

	if (ssl_context == NULL)
		return 0;

	*x509 = ssl_context->client_x509;
	*pkey = ssl_context->client_pkey;

	if (*x509 && *pkey)
		return 1;
	else
		return 0;
}

static struct mailstream_ssl_data * ssl_data_new_full(int fd, time_t timeout,
	SSL_METHOD * method, void (* callback)(struct mailstream_ssl_context * ssl_context, void * cb_data),
	void * cb_data)
{
  struct mailstream_ssl_data * ssl_data;
  SSL * ssl_conn;
  int r;
  SSL_CTX * tmp_ctx;
  struct mailstream_cancel * cancel;
  struct mailstream_ssl_context * ssl_context = NULL;
#ifdef SSL_MODE_RELEASE_BUFFERS
  long mode = 0;
#endif
  
  mailstream_ssl_init();
  
  tmp_ctx = SSL_CTX_new(method);
  if (tmp_ctx == NULL)
    goto err;
  
  if (callback != NULL) {
    ssl_context = mailstream_ssl_context_new(tmp_ctx, fd);
    callback(ssl_context, cb_data);
  }
  
  SSL_CTX_set_app_data(tmp_ctx, ssl_context);
  SSL_CTX_set_client_cert_cb(tmp_ctx, mailstream_openssl_client_cert_cb);
  ssl_conn = (SSL *) SSL_new(tmp_ctx);
  
#ifdef SSL_MODE_RELEASE_BUFFERS
  mode = SSL_get_mode(ssl_conn);
  SSL_set_mode(ssl_conn, mode | SSL_MODE_RELEASE_BUFFERS);
#endif
  
  if (ssl_conn == NULL)
    goto free_ctx;
  
  if (SSL_set_fd(ssl_conn, fd) == 0)
    goto free_ssl_conn;
  
again:
  r = SSL_connect(ssl_conn);

  switch(SSL_get_error(ssl_conn, r)) {
  	case SSL_ERROR_WANT_READ:
          r = wait_SSL_connect(fd, 1, timeout);
          if (r < 0)
            goto free_ssl_conn;
	  else
	    goto again;
	break;
	case SSL_ERROR_WANT_WRITE:
          r = wait_SSL_connect(fd, 0, timeout);
          if (r < 0)
            goto free_ssl_conn;
	  else
	    goto again;
	break;
  }
  if (r <= 0)
    goto free_ssl_conn;
  
  cancel = mailstream_cancel_new();
  if (cancel == NULL)
    goto free_ssl_conn;
  
  r = mailstream_prepare_fd(fd);
  if (r < 0)
    goto free_cancel;
  
  ssl_data = malloc(sizeof(* ssl_data));
  if (ssl_data == NULL)
    goto free_cancel;
  
  ssl_data->fd = fd;
  ssl_data->ssl_conn = ssl_conn;
  ssl_data->ssl_ctx = tmp_ctx;
  ssl_data->cancel = cancel;
  mailstream_ssl_context_free(ssl_context);

  return ssl_data;

 free_cancel:
  mailstream_cancel_free(cancel);
 free_ssl_conn:
  SSL_free(ssl_conn);
 free_ctx:
  SSL_CTX_free(tmp_ctx);
  mailstream_ssl_context_free(ssl_context);
 err:
  return NULL;
}

static struct mailstream_ssl_data * ssl_data_new(int fd, time_t timeout,
	void (* callback)(struct mailstream_ssl_context * ssl_context, void * cb_data), void * cb_data)
{
  return ssl_data_new_full(fd, timeout,
#if (OPENSSL_VERSION_NUMBER >= 0x10100000L)
		TLS_client_method(),
#else
	/* Despite their name the SSLv23_*method() functions have nothing to do
	 * with the availability of SSLv2 or SSLv3. What these functions do is
	 * negotiate with the peer the highest available SSL/TLS protocol version
	 * available. The name is as it is for historic reasons. This is a very
	 * common confusion and is the main reason why these names have been
	 * deprecated in the latest dev version of OpenSSL. */
		SSLv23_client_method(),
#endif
		callback, cb_data);
}

#else

static struct mailstream_ssl_context * mailstream_ssl_context_new(gnutls_session session, int fd);
static void mailstream_ssl_context_free(struct mailstream_ssl_context * ssl_ctx);

#if GNUTLS_VERSION_NUMBER <= 0x020c00
static int mailstream_gnutls_client_cert_cb(gnutls_session session,
                               const gnutls_datum *req_ca_rdn, int nreqs,
                               const gnutls_pk_algorithm *sign_algos,
                               int sign_algos_length, gnutls_retr_st *st)
#else
static int mailstream_gnutls_client_cert_cb(gnutls_session session,
                               const gnutls_datum *req_ca_rdn, int nreqs,
                               const gnutls_pk_algorithm *sign_algos,
                               int sign_algos_length, gnutls_retr2_st *st)
#endif
{
	struct mailstream_ssl_context * ssl_context = (struct mailstream_ssl_context *)gnutls_session_get_ptr(session);
	gnutls_certificate_type type = gnutls_certificate_type_get(session);

	st->ncerts = 0;

	if (ssl_context == NULL)
		return 0;

	if (type == GNUTLS_CRT_X509 && ssl_context->client_x509 && ssl_context->client_pkey) {
		st->ncerts = 1;
#if GNUTLS_VERSION_NUMBER <= 0x020c00
		st->type = type;
#else
		st->key_type = type;
#endif
		st->cert.x509 = &(ssl_context->client_x509);
		st->key.x509 = ssl_context->client_pkey;
		st->deinit_all = 0;
	}
	return 0;
}

static struct mailstream_ssl_data * ssl_data_new(int fd, time_t timeout,
  void (* callback)(struct mailstream_ssl_context * ssl_context, void * cb_data), void * cb_data)
{
  struct mailstream_ssl_data * ssl_data;
  gnutls_session session;
  struct mailstream_cancel * cancel;
  gnutls_certificate_credentials_t xcred;
  int r;
  struct mailstream_ssl_context * ssl_context = NULL;
  unsigned int timeout_value;
  
  mailstream_ssl_init();
  
  if (gnutls_certificate_allocate_credentials (&xcred) != 0)
    return NULL;

  r = gnutls_init(&session, GNUTLS_CLIENT);
  if (session == NULL || r != 0)
    return NULL;
  
  if (callback != NULL) {
    ssl_context = mailstream_ssl_context_new(session, fd);
    callback(ssl_context, cb_data);
  }
  
  gnutls_session_set_ptr(session, ssl_context);
  gnutls_credentials_set(session, GNUTLS_CRD_CERTIFICATE, xcred);
#if GNUTLS_VERSION_NUMBER <= 0x020c00
  gnutls_certificate_client_set_retrieve_function(xcred, mailstream_gnutls_client_cert_cb);
#else
  gnutls_certificate_set_retrieve_function(xcred, mailstream_gnutls_client_cert_cb);
#endif
  gnutls_set_default_priority(session);
  gnutls_priority_set_direct(session, "NORMAL", NULL);

  gnutls_record_disable_padding(session);
  gnutls_dh_set_prime_bits(session, 512);

  gnutls_transport_set_ptr(session, (gnutls_transport_ptr) fd);

  /* lower limits on server key length restriction */
  gnutls_dh_set_prime_bits(session, 512);
  
  if (timeout == 0) {
		timeout_value = mailstream_network_delay.tv_sec * 1000 + mailstream_network_delay.tv_usec / 1000;
  }
  else {
		timeout_value = timeout;
  }
#if GNUTLS_VERSION_NUMBER >= 0x030100
	gnutls_handshake_set_timeout(session, timeout_value);
#endif

  do {
    r = gnutls_handshake(session);
  } while (r == GNUTLS_E_AGAIN || r == GNUTLS_E_INTERRUPTED);

  if (r < 0) {
    gnutls_perror(r);
    goto free_ssl_conn;
  }
  
  cancel = mailstream_cancel_new();
  if (cancel == NULL)
    goto free_ssl_conn;
  
  r = mailstream_prepare_fd(fd);
  if (r < 0)
    goto free_cancel;
  
  ssl_data = malloc(sizeof(* ssl_data));
  if (ssl_data == NULL)
    goto err;
  
  ssl_data->fd = fd;
  ssl_data->session = session;
  ssl_data->xcred = xcred;
  ssl_data->cancel = cancel;
  
  mailstream_ssl_context_free(ssl_context);

  return ssl_data;
  
 free_cancel:
  mailstream_cancel_free(cancel);
 free_ssl_conn:
  gnutls_certificate_free_credentials(xcred);
  mailstream_ssl_context_free(ssl_context);
  gnutls_deinit(session);
 err:
  return NULL;
}
#endif

static void  ssl_data_free(struct mailstream_ssl_data * ssl_data)
{
  mailstream_cancel_free(ssl_data->cancel);
  free(ssl_data);
}

#ifndef USE_GNUTLS
static void  ssl_data_close(struct mailstream_ssl_data * ssl_data)
{
  SSL_free(ssl_data->ssl_conn);
  ssl_data->ssl_conn = NULL;
  SSL_CTX_free(ssl_data->ssl_ctx);
  ssl_data->ssl_ctx  = NULL;
#ifdef WIN32
  closesocket(ssl_data->fd);
#else
  close(ssl_data->fd);
#endif
  ssl_data->fd = -1;
}
#else
static void  ssl_data_close(struct mailstream_ssl_data * ssl_data)
{
  gnutls_certificate_free_credentials(ssl_data->xcred);
  gnutls_deinit(ssl_data->session);

  MUTEX_LOCK(&ssl_lock);
  gnutls_global_deinit();
  MUTEX_UNLOCK(&ssl_lock);

  ssl_data->session = NULL;
#ifdef WIN32
  closesocket(ssl_data->fd);
#else
  close(ssl_data->fd);
#endif
  ssl_data->fd = -1;
}
#endif

#endif

static mailstream_low * mailstream_low_ssl_open_full(int fd, int starttls, time_t timeout,
  void (* callback)(struct mailstream_ssl_context * ssl_context, void * cb_data), void * cb_data)
{
#ifdef USE_SSL
  mailstream_low * s;
  struct mailstream_ssl_data * ssl_data;

  ssl_data = ssl_data_new(fd, timeout, callback, cb_data);

  if (ssl_data == NULL)
    goto err;

  s = mailstream_low_new(ssl_data, mailstream_ssl_driver);
  if (s == NULL)
    goto free_ssl_data;
	mailstream_low_set_timeout(s, timeout);

  return s;

 free_ssl_data:
  ssl_data_free(ssl_data);
 err:
  return NULL;
#else
  return NULL;
#endif
}

mailstream_low * mailstream_low_ssl_open(int fd)
{
	return mailstream_low_ssl_open_timeout(fd, 0);
}

mailstream_low * mailstream_low_tls_open(int fd)
{
	return mailstream_low_tls_open_timeout(fd, 0);
}

mailstream_low * mailstream_low_ssl_open_timeout(int fd, time_t timeout)
{
  return mailstream_low_ssl_open_full(fd, 0, timeout, NULL, NULL);
}

mailstream_low * mailstream_low_tls_open_timeout(int fd, time_t timeout)
{
  return mailstream_low_ssl_open_full(fd, 1, timeout, NULL, NULL);
}

#ifdef USE_SSL
static int mailstream_low_ssl_close(mailstream_low * s)
{
  struct mailstream_ssl_data * ssl_data;

  ssl_data = (struct mailstream_ssl_data *) s->data;
  ssl_data_close(ssl_data);

  return 0;
}

static void mailstream_low_ssl_free(mailstream_low * s)
{
  struct mailstream_ssl_data * ssl_data;

  ssl_data = (struct mailstream_ssl_data *) s->data;
  ssl_data_free(ssl_data);
  s->data = NULL;

  free(s);
}

static int mailstream_low_ssl_get_fd(mailstream_low * s)
{
  struct mailstream_ssl_data * ssl_data;

  ssl_data = (struct mailstream_ssl_data *) s->data;
  return ssl_data->fd;
}

static int wait_read(mailstream_low * s)
{
  struct timeval timeout;
  int fd;
  struct mailstream_ssl_data * ssl_data;
  int r;
  int cancelled;
#if defined(WIN32)
  fd_set fds_read;
  HANDLE event;
#elif USE_POLL
  struct pollfd pfd[2];
#else
  fd_set fds_read;
  int max_fd;
#endif

  ssl_data = (struct mailstream_ssl_data *) s->data;
  if (s->timeout == 0) {
    timeout = mailstream_network_delay;
  }
  else {
		timeout.tv_sec = s->timeout;
    timeout.tv_usec = 0;
  }
  
#ifdef USE_GNUTLS
  if (gnutls_record_check_pending(ssl_data->session) != 0)
    return 0;
#endif

  fd = mailstream_cancel_get_fd(ssl_data->cancel);
#if defined(WIN32)
  FD_ZERO(&fds_read);
  FD_SET(fd, &fds_read);
  event = CreateEvent(NULL, TRUE, FALSE, NULL);
  WSAEventSelect(ssl_data->fd, event, FD_READ | FD_CLOSE);
  FD_SET(event, &fds_read);
  r = WaitForMultipleObjects(fds_read.fd_count, fds_read.fd_array, FALSE, timeout.tv_sec * 1000 + timeout.tv_usec / 1000);
  if (WAIT_TIMEOUT == r) {
    WSAEventSelect(ssl_data->fd, event, 0);
    CloseHandle(event);
    return -1;
  }
  
  cancelled = (fds_read.fd_array[r - WAIT_OBJECT_0] == fd);
  WSAEventSelect(ssl_data->fd, event, 0);
  CloseHandle(event);
#elif USE_POLL
  pfd[0].fd = ssl_data->fd;
  pfd[0].events = POLLIN;
  pfd[0].revents = 0;

  pfd[1].fd = fd;
  pfd[1].events = POLLIN;
  pfd[1].revents = 0;

  r = poll(&pfd[0], 2, timeout.tv_sec * 1000 + timeout.tv_usec / 1000);
  if (r <= 0)
    return -1;
  
  cancelled = pfd[1].revents & POLLIN;
#else
  FD_ZERO(&fds_read);
  FD_SET(fd, &fds_read);
  FD_SET(ssl_data->fd, &fds_read);
  max_fd = fd > ssl_data->fd ? fd : ssl_data->fd;
  r = select(max_fd + 1, &fds_read, NULL, NULL, &timeout);
  if (r <= 0)
      return -1;
  cancelled = FD_ISSET(fd, &fds_read);
#endif
  if (cancelled) {
    /* cancelled */
    mailstream_cancel_ack(ssl_data->cancel);
    return -1;
  }
  
  return 0;
}

#ifndef USE_GNUTLS
static ssize_t mailstream_low_ssl_read(mailstream_low * s,
				       void * buf, size_t count)
{
  struct mailstream_ssl_data * ssl_data;
  int r;

  ssl_data = (struct mailstream_ssl_data *) s->data;
  
  if (mailstream_cancel_cancelled(ssl_data->cancel))
    return -1;
  
  while (1) {
    int ssl_r;
    
    r = SSL_read(ssl_data->ssl_conn, buf, (int) count);
    if (r > 0)
      return r;
    
    ssl_r = SSL_get_error(ssl_data->ssl_conn, r);
    switch (ssl_r) {
    case SSL_ERROR_NONE:
      return r;
      
    case SSL_ERROR_ZERO_RETURN:
      return r;
      
    case SSL_ERROR_WANT_READ:
      r = wait_read(s);
      if (r < 0)
        return r;
      break;
      
    default:
      return -1;
    }
  }
}
#else
static ssize_t mailstream_low_ssl_read(mailstream_low * s,
				       void * buf, size_t count)
{
  struct mailstream_ssl_data * ssl_data;
  int r;

  ssl_data = (struct mailstream_ssl_data *) s->data;
  if (mailstream_cancel_cancelled(ssl_data->cancel))
    return -1;
  
  while (1) {
    r = gnutls_record_recv(ssl_data->session, buf, count);
    if (r > 0)
      return r;
    
    switch (r) {
    case 0: /* closed connection */
      return -1;
    
    case GNUTLS_E_REHANDSHAKE:
      do {
         r = gnutls_handshake(ssl_data->session); 
      } while (r == GNUTLS_E_AGAIN || r == GNUTLS_E_INTERRUPTED);
      break; /* re-receive */
    case GNUTLS_E_AGAIN:
    case GNUTLS_E_INTERRUPTED:
      r = wait_read(s);
      if (r < 0)
        return r;
      break;
      
    default:
      return -1;
    }
  }
}
#endif

static int wait_write(mailstream_low * s)
{
  struct timeval timeout;
  int r;
  int fd;
  struct mailstream_ssl_data * ssl_data;
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
  
  ssl_data = (struct mailstream_ssl_data *) s->data;
  if (mailstream_cancel_cancelled(ssl_data->cancel))
    return -1;
 
  if (s->timeout == 0) {
    timeout = mailstream_network_delay;
  }
  else {
		timeout.tv_sec = s->timeout;
    timeout.tv_usec = 0;
  }
  
  fd = mailstream_cancel_get_fd(ssl_data->cancel);
#if defined(WIN32)
  FD_ZERO(&fds_read);
  FD_ZERO(&fds_write);
  FD_SET(fd, &fds_read);
  event = CreateEvent(NULL, TRUE, FALSE, NULL);
  WSAEventSelect(ssl_data->fd, event, FD_WRITE | FD_CLOSE);
  FD_SET(event, &fds_read);
  r = WaitForMultipleObjects(fds_read.fd_count, fds_read.fd_array, FALSE, timeout.tv_sec * 1000 + timeout.tv_usec / 1000);
  if (r < 0) {
		WSAEventSelect(ssl_data->fd, event, 0);
		CloseHandle(event);
    return -1;
	}
  
  cancelled = (fds_read.fd_array[r - WAIT_OBJECT_0] == fd) /* SEB 20070709 */;
  write_enabled = (fds_read.fd_array[r - WAIT_OBJECT_0] == event);
	WSAEventSelect(ssl_data->fd, event, 0);
	CloseHandle(event);
#elif USE_POLL
  pfd[0].fd = ssl_data->fd;
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
  FD_ZERO(&fds_write);
  FD_SET(fd, &fds_read);
  FD_SET(ssl_data->fd, &fds_write);
  
  max_fd = fd > ssl_data->fd ? fd : ssl_data->fd;
  r = select(max_fd + 1, &fds_read, &fds_write, NULL, &timeout);
  if (r <= 0)
    return -1;

  cancelled = FD_ISSET(fd, &fds_read);
  write_enabled = FD_ISSET(ssl_data->fd, &fds_write);
#endif
  
  if (cancelled) {
    /* cancelled */
    mailstream_cancel_ack(ssl_data->cancel);
    return -1;
  }
  
  if (!write_enabled)
    return 0;
  
  return 1;
}

#ifndef USE_GNUTLS
static ssize_t mailstream_low_ssl_write(mailstream_low * s,
					const void * buf, size_t count)
{
  struct mailstream_ssl_data * ssl_data;
  int ssl_r;
  int r;
  
  ssl_data = (struct mailstream_ssl_data *) s->data;
  r = wait_write(s);
  if (r <= 0)
    return r;
  
  r = SSL_write(ssl_data->ssl_conn, buf, (int) count);
  if (r > 0)
    return r;
  
  ssl_r = SSL_get_error(ssl_data->ssl_conn, r);
  switch (ssl_r) {
  case SSL_ERROR_NONE:
    return r;
    
  case SSL_ERROR_ZERO_RETURN:
    return -1;
    
  case SSL_ERROR_WANT_WRITE:
    return 0;
    
  default:
    return r;
  }
}
#else
static ssize_t mailstream_low_ssl_write(mailstream_low * s,
					const void * buf, size_t count)
{
  struct mailstream_ssl_data * ssl_data;
  int r;
  
  ssl_data = (struct mailstream_ssl_data *) s->data;
  r = wait_write(s);
  if (r <= 0)
    return r;
  
  r = gnutls_record_send(ssl_data->session, buf, count);
  if (r > 0)
    return r;
  
  switch (r) {
  case 0:
    return -1;
    
  case GNUTLS_E_AGAIN:
  case GNUTLS_E_INTERRUPTED:
    return 0;
    
  default:
    return r;
  }
}
#endif
#endif

/* mailstream */

mailstream * mailstream_ssl_open(int fd)
{
  return mailstream_ssl_open_timeout(fd, 0);
}

mailstream * mailstream_ssl_open_timeout(int fd, time_t timeout)
{
  return mailstream_ssl_open_with_callback_timeout(fd, timeout, NULL, NULL);
}

mailstream * mailstream_ssl_open_with_callback(int fd,
    void (* callback)(struct mailstream_ssl_context * ssl_context, void * data), void * data)
{
	return mailstream_ssl_open_with_callback_timeout(fd, 0, callback, data);
}

mailstream * mailstream_ssl_open_with_callback_timeout(int fd, time_t timeout,
    void (* callback)(struct mailstream_ssl_context * ssl_context, void * data), void * data)
{
#ifdef USE_SSL
  mailstream_low * low;
  mailstream * s;

  low = mailstream_low_ssl_open_with_callback_timeout(fd, timeout, callback, data);
  if (low == NULL)
    goto err;

  s = mailstream_new(low, 8192);
  if (s == NULL)
    goto free_low;

  return s;

 free_low:
  mailstream_low_close(low);
 err:
  return NULL;
#else
  return NULL;
#endif
}

ssize_t mailstream_ssl_get_certificate(mailstream *stream, unsigned char **cert_DER)
{
#ifdef USE_SSL
  struct mailstream_ssl_data *data = NULL;
  ssize_t len = 0;
#ifndef USE_GNUTLS
  SSL *ssl_conn = NULL;
  X509 *cert = NULL;
#else
  gnutls_session session = NULL;
  const gnutls_datum *raw_cert_list;
  unsigned int raw_cert_list_length;
  gnutls_x509_crt cert = NULL;
  size_t cert_size;
#endif

  if (cert_DER == NULL || stream == NULL || stream->low == NULL)
    return -1;

  data = stream->low->data;
  if (data == NULL)
    return -1;

#ifndef USE_GNUTLS
  ssl_conn = data->ssl_conn;
  if (ssl_conn == NULL)
    return -1;
  
  cert = SSL_get_peer_certificate(ssl_conn);
  if (cert == NULL)
    return -1;
  
  len = i2d_X509(cert, NULL);
  * cert_DER = malloc(len);
  if (* cert_DER == NULL)
    return -1;

  i2d_X509(cert, cert_DER);

	X509_free(cert);

  return len;
#else
  session = data->session;
  raw_cert_list = gnutls_certificate_get_peers(session, &raw_cert_list_length);

  if (raw_cert_list 
  && gnutls_certificate_type_get(session) == GNUTLS_CRT_X509
  &&  gnutls_x509_crt_init(&cert) >= 0
  &&  gnutls_x509_crt_import(cert, &raw_cert_list[0], GNUTLS_X509_FMT_DER) >= 0) {
    cert_size = 0;
    if (gnutls_x509_crt_export(cert, GNUTLS_X509_FMT_DER, NULL, &cert_size) 
        != GNUTLS_E_SHORT_MEMORY_BUFFER)
      return -1;

    *cert_DER = malloc (cert_size);
    if (*cert_DER == NULL)
      return -1;

    if (gnutls_x509_crt_export(cert, GNUTLS_X509_FMT_DER, *cert_DER, &cert_size) < 0)
      return -1;

    len = (ssize_t)cert_size;
    gnutls_x509_crt_deinit(cert);
    
    return len;
  }
#endif
#endif
  return -1;
}

static void mailstream_low_ssl_cancel(mailstream_low * s)
{
#ifdef USE_SSL
  struct mailstream_ssl_data * data;
  
  data = s->data;
  mailstream_cancel_notify(data->cancel);
#endif
}

mailstream_low * mailstream_low_ssl_open_with_callback(int fd,
    void (* callback)(struct mailstream_ssl_context * ssl_context, void * data), void * data)
{
	return mailstream_low_ssl_open_with_callback_timeout(fd, 0, callback, data);
}

mailstream_low * mailstream_low_ssl_open_with_callback_timeout(int fd, time_t timeout,
    void (* callback)(struct mailstream_ssl_context * ssl_context, void * data), void * data)
{
  return mailstream_low_ssl_open_full(fd, 0, timeout, callback, data);
}

mailstream_low * mailstream_low_tls_open_with_callback(int fd,
    void (* callback)(struct mailstream_ssl_context * ssl_context, void * data), void * data)
{
  return mailstream_low_tls_open_with_callback_timeout(fd, 0, callback, data);
}

mailstream_low * mailstream_low_tls_open_with_callback_timeout(int fd, time_t timeout,
    void (* callback)(struct mailstream_ssl_context * ssl_context, void * data), void * data)
{
  return mailstream_low_ssl_open_full(fd, 1, timeout, callback, data);
}

int mailstream_ssl_set_client_certicate(struct mailstream_ssl_context * ssl_context,
    char * filename)
{
#ifdef USE_SSL
#ifdef USE_GNUTLS
  /* not implemented */
  return -1;
#else
  SSL_CTX * ctx = (SSL_CTX *)ssl_context->openssl_ssl_ctx;
  STACK_OF(X509_NAME) *cert_names;
  
  cert_names = SSL_load_client_CA_file(filename);
  if (cert_names != NULL) {
    SSL_CTX_set_client_CA_list(ctx, cert_names);
    return 0;
  }
  else {
    return -1;
  }
#endif /* USE_GNUTLS */
#else
  return -1;
#endif /* USE_SSL */
}

LIBETPAN_EXPORT
int mailstream_ssl_set_client_certificate_data(struct mailstream_ssl_context * ssl_context,
    unsigned char *x509_der, size_t len)
{
#ifdef USE_SSL
#ifndef USE_GNUTLS
  X509 *x509 = NULL;
  if (x509_der != NULL && len > 0)
    x509 = d2i_X509(NULL, (const unsigned char **)&x509_der, len);
  ssl_context->client_x509 = (X509 *)x509;
  return 0;
#else
  gnutls_datum tmp;
  int r;
  ssl_context->client_x509 = NULL;
  if (len == 0)
    return 0;
  gnutls_x509_crt_init(&(ssl_context->client_x509));
  tmp.data = x509_der;
  tmp.size = len;
  if ((r = gnutls_x509_crt_import(ssl_context->client_x509, &tmp, GNUTLS_X509_FMT_DER)) < 0) {
    gnutls_x509_crt_deinit(ssl_context->client_x509);
    ssl_context->client_x509 = NULL;
    return -1;
  }
  return 0;
#endif
#endif
  return -1;
}
int mailstream_ssl_set_client_private_key_data(struct mailstream_ssl_context * ssl_context,
    unsigned char *pkey_der, size_t len)
{
#ifdef USE_SSL
#ifndef USE_GNUTLS
  EVP_PKEY *pkey = NULL;
  if (pkey_der != NULL && len > 0)
    pkey = d2i_AutoPrivateKey(NULL, (const unsigned char **)&pkey_der, len);
  ssl_context->client_pkey = (EVP_PKEY *)pkey;
  return 0;
#else
  gnutls_datum tmp;
  int r;
  ssl_context->client_pkey = NULL;
  if (len == 0)
    return 0;
  gnutls_x509_privkey_init(&(ssl_context->client_pkey));
  tmp.data = pkey_der;
  tmp.size = len;
  if ((r = gnutls_x509_privkey_import(ssl_context->client_pkey, &tmp, GNUTLS_X509_FMT_DER)) < 0) {
    gnutls_x509_privkey_deinit(ssl_context->client_pkey);
    ssl_context->client_pkey = NULL;
    return -1;
  }
  return 0;
#endif
#endif
  return -1;
}

int mailstream_ssl_set_server_certicate(struct mailstream_ssl_context * ssl_context, 
    char * CAfile, char * CApath)
{
#ifdef USE_SSL
#ifdef USE_GNUTLS
  /* not implemented */
  return -1;
#else
  SSL_CTX * ctx = (SSL_CTX *)ssl_context->openssl_ssl_ctx;
  SSL_CTX_set_verify(ctx, SSL_VERIFY_PEER, 0);
  if (!SSL_CTX_load_verify_locations(ctx, CAfile, CApath))
    return -1;
  else
    return 0;
#endif /* USE_GNUTLS */
#else
  return -1;
#endif /* USE_SSL */
}

#ifdef USE_SSL
#ifndef USE_GNUTLS
static struct mailstream_ssl_context * mailstream_ssl_context_new(SSL_CTX * open_ssl_ctx, int fd)
{
  struct mailstream_ssl_context * ssl_ctx;
  
  ssl_ctx = malloc(sizeof(* ssl_ctx));
  if (ssl_ctx == NULL)
    return NULL;
  
  ssl_ctx->openssl_ssl_ctx = open_ssl_ctx;
  ssl_ctx->client_x509 = NULL;
  ssl_ctx->client_pkey = NULL;
  ssl_ctx->fd = fd;
  
  return ssl_ctx;
}

static void mailstream_ssl_context_free(struct mailstream_ssl_context * ssl_ctx)
{
  if (ssl_ctx)
    free(ssl_ctx);
}
#else
static struct mailstream_ssl_context * mailstream_ssl_context_new(gnutls_session session, int fd)
{
  struct mailstream_ssl_context * ssl_ctx;
  
  ssl_ctx = malloc(sizeof(* ssl_ctx));
  if (ssl_ctx == NULL)
    return NULL;
  
  ssl_ctx->session = session;
  ssl_ctx->client_x509 = NULL;
  ssl_ctx->client_pkey = NULL;
  ssl_ctx->fd = fd;
  
  return ssl_ctx;
}

static void mailstream_ssl_context_free(struct mailstream_ssl_context * ssl_ctx)
{
  if (ssl_ctx) {
    if (ssl_ctx->client_x509)
      gnutls_x509_crt_deinit(ssl_ctx->client_x509);
    if (ssl_ctx->client_pkey)
      gnutls_x509_privkey_deinit(ssl_ctx->client_pkey);
    free(ssl_ctx);
  }
}
#endif
#endif

void * mailstream_ssl_get_openssl_ssl_ctx(struct mailstream_ssl_context * ssl_context)
{
#ifdef USE_SSL
#ifndef USE_GNUTLS
  return ssl_context->openssl_ssl_ctx;
#endif
#endif /* USE_SSL */
  return 0;
}

int mailstream_ssl_get_fd(struct mailstream_ssl_context * ssl_context)
{
  return ssl_context->fd;
}

static struct mailstream_cancel * mailstream_low_ssl_get_cancel(mailstream_low * s)
{
#ifdef USE_SSL
  struct mailstream_ssl_data * data;
  
  data = s->data;
  return data->cancel;
#else
  return NULL;
#endif
}

carray * mailstream_low_ssl_get_certificate_chain(mailstream_low * s)
{
#ifdef USE_SSL
  struct mailstream_ssl_data * ssl_data;
  carray * result;
  int skpos;
#ifndef USE_GNUTLS
  STACK_OF(X509) * skx;
  
  ssl_data = (struct mailstream_ssl_data *) s->data;
  if (!(skx = SSL_get_peer_cert_chain(ssl_data->ssl_conn))) {
    return NULL;
  }
  
  result = carray_new(4);
  for(skpos = 0 ; skpos < sk_num(skx) ; skpos ++) {
    X509 * x = (X509 *) sk_value(skx, skpos);
    unsigned char * p;
    MMAPString * str;
    int length = i2d_X509(x, NULL);
    str = mmap_string_sized_new(length);
    p = (unsigned char *) str->str;
    str->len = length;
    i2d_X509(x, &p);
    carray_add(result, str, NULL);
  }
  
  return result;
#else
  gnutls_session session = NULL;
  const gnutls_datum *raw_cert_list;
  unsigned int raw_cert_list_length;

  ssl_data = (struct mailstream_ssl_data *) s->data;

  session = ssl_data->session;
  raw_cert_list = gnutls_certificate_get_peers(session, &raw_cert_list_length);

  if (raw_cert_list && gnutls_certificate_type_get(session) == GNUTLS_CRT_X509) {
    result = carray_new(4);
    for(skpos = 0 ; skpos < raw_cert_list_length ; skpos ++) {
      gnutls_x509_crt cert = NULL;
      if (gnutls_x509_crt_init(&cert) >= 0
       && gnutls_x509_crt_import(cert, &raw_cert_list[skpos], GNUTLS_X509_FMT_DER) >= 0) {
         size_t cert_size = 0;
         MMAPString * str = NULL;
         unsigned char * p;

         if (gnutls_x509_crt_export(cert, GNUTLS_X509_FMT_DER, NULL, &cert_size)
	     == GNUTLS_E_SHORT_MEMORY_BUFFER) {
           str = mmap_string_sized_new(cert_size);
           p = (unsigned char *) str->str;
           str->len = cert_size;
	 }
	 if (str != NULL &&
             gnutls_x509_crt_export(cert, GNUTLS_X509_FMT_DER, p, &cert_size) >= 0) {
           carray_add(result, str, NULL);
	 } else {
	   return NULL;
	 }
         gnutls_x509_crt_deinit(cert);
       }
    }
  }

  return result;

  return NULL;
#endif
#else
  return NULL;
#endif
}
