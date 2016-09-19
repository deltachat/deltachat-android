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

#include "mailstream_cfstream.h"

#if HAVE_CFNETWORK
#include <CoreFoundation/CoreFoundation.h>
#include <TargetConditionals.h>
#if TARGET_OS_IPHONE || TARGET_IPHONE_SIMULATOR
#include <CFNetwork/CFNetwork.h>
#include <Security/Security.h>
#else
#include <CoreServices/CoreServices.h>
#endif
#endif

#ifndef WIN32
#include <pthread.h>
#endif

#if LIBETPAN_IOS_DISABLE_SSL && HAVE_CFNETWORK
#define CFSTREAM_ENABLED_DEFAULT 1
#else
#define CFSTREAM_ENABLED_DEFAULT 0
#endif

LIBETPAN_EXPORT
int mailstream_cfstream_enabled = CFSTREAM_ENABLED_DEFAULT;

LIBETPAN_EXPORT
int mailstream_cfstream_voip_enabled = 0;

enum {
  STATE_NONE,
  STATE_WAIT_OPEN,
  STATE_OPEN_READ_DONE,
  STATE_OPEN_WRITE_DONE,
  STATE_OPEN_READ_WRITE_DONE,
  STATE_OPEN_WRITE_READ_DONE,
  STATE_WAIT_READ,
  STATE_READ_DONE,
  STATE_WAIT_WRITE,
  STATE_WRITE_DONE,
  STATE_WAIT_IDLE,
  STATE_IDLE_DONE,
  STATE_WAIT_SSL,
  STATE_SSL_READ_DONE,
  STATE_SSL_WRITE_DONE,
  STATE_SSL_READ_WRITE_DONE,
  STATE_SSL_WRITE_READ_DONE
};

#if HAVE_CFNETWORK
struct mailstream_cfstream_data {
  int state;
  CFStreamClientContext streamContext;
  
  CFReadStreamRef readStream;
  void * readBuffer;
  size_t readBufferSize;
  ssize_t readResult;
  int readOpenResult;
  int readSSLResult;
  
  CFWriteStreamRef writeStream;
  const void * writeBuffer;
  size_t writeBufferSize;
  ssize_t writeResult;
  int writeOpenResult;
  int writeSSLResult;
  
  Boolean cancelled;
  CFRunLoopSourceRef cancelSource;
  CFRunLoopSourceContext cancelContext;
  
  Boolean idleInterrupted;
  CFRunLoopSourceRef idleInterruptedSource;
  CFRunLoopSourceContext idleInterruptedContext;
  int idleMaxDelay;
  
  CFRunLoopRef runloop;
  pthread_mutex_t runloop_lock;
  
  int ssl_enabled;
  int ssl_level;
  int ssl_is_server;
  char * ssl_peer_name;
  int ssl_certificate_verification_mask;
};
#endif

/* data */

#if HAVE_CFNETWORK
static int low_open(mailstream_low * s);
static void cfstream_data_close(struct mailstream_cfstream_data * socket_data);
#endif

/* mailstream_low, socket */

static int mailstream_low_cfstream_close(mailstream_low * s);
static ssize_t mailstream_low_cfstream_read(mailstream_low * s,
                                            void * buf, size_t count);
static ssize_t mailstream_low_cfstream_write(mailstream_low * s,
                                             const void * buf, size_t count);
static void mailstream_low_cfstream_free(mailstream_low * s);
static int mailstream_low_cfstream_get_fd(mailstream_low * s);
static void mailstream_low_cfstream_cancel(mailstream_low * s);
static carray * mailstream_low_cfstream_get_certificate_chain(mailstream_low * s);

static int mailstream_low_cfstream_setup_idle(mailstream_low * s);
static int mailstream_low_cfstream_unsetup_idle(mailstream_low * s);
static int mailstream_low_cfstream_interrupt_idle(mailstream_low * s);

static mailstream_low_driver local_mailstream_cfstream_driver = {
  /* mailstream_read */ mailstream_low_cfstream_read,
  /* mailstream_write */ mailstream_low_cfstream_write,
  /* mailstream_close */ mailstream_low_cfstream_close,
  /* mailstream_get_fd */ mailstream_low_cfstream_get_fd,
  /* mailstream_free */ mailstream_low_cfstream_free,
  /* mailstream_cancel */ mailstream_low_cfstream_cancel,
  /* mailstream_get_cancel */ NULL,
  /* mailstream_get_certificate_chain */ mailstream_low_cfstream_get_certificate_chain,
  /* mailstream_setup_idle */ mailstream_low_cfstream_setup_idle,
  /* mailstream_unsetup_idle */ mailstream_low_cfstream_unsetup_idle,
  /* mailstream_interrupt_idle */ mailstream_low_cfstream_interrupt_idle,
};

mailstream_low_driver * mailstream_cfstream_driver =
&local_mailstream_cfstream_driver;

#if HAVE_CFNETWORK
static struct mailstream_cfstream_data * cfstream_data_new(CFReadStreamRef readStream, CFWriteStreamRef writeStream)
{
  struct mailstream_cfstream_data * cfstream_data;
  
  cfstream_data = (struct mailstream_cfstream_data * ) malloc(sizeof(* cfstream_data));
  memset(cfstream_data, 0, sizeof(* cfstream_data));
  cfstream_data->readStream = (CFReadStreamRef) CFRetain(readStream);
  cfstream_data->writeStream = (CFWriteStreamRef) CFRetain(writeStream);
  cfstream_data->ssl_level = MAILSTREAM_CFSTREAM_SSL_LEVEL_NEGOCIATED_SSL;
  pthread_mutex_init(&cfstream_data->runloop_lock, NULL);
  
  return cfstream_data;
}

static void cfstream_data_free(struct mailstream_cfstream_data * cfstream_data)
{
  cfstream_data_close(cfstream_data);
  pthread_mutex_destroy(&cfstream_data->runloop_lock);
  free(cfstream_data->ssl_peer_name);
  free(cfstream_data);
}

static void cfstream_data_close(struct mailstream_cfstream_data * cfstream_data)
{
  if (cfstream_data->writeStream != NULL) {
    CFWriteStreamSetClient(cfstream_data->writeStream, kCFStreamEventNone, NULL, NULL);
    CFWriteStreamClose(cfstream_data->writeStream);
    CFRelease(cfstream_data->writeStream);
    cfstream_data->writeStream = NULL;
  }
  if (cfstream_data->readStream != NULL) {
    CFReadStreamSetClient(cfstream_data->readStream, kCFStreamEventNone, NULL, NULL);
    CFReadStreamClose(cfstream_data->readStream);
    CFRelease(cfstream_data->readStream);
    cfstream_data->readStream = NULL;
  }
}
#endif

mailstream * mailstream_cfstream_open(const char * hostname, int16_t port)
{
	return mailstream_cfstream_open_voip_timeout(hostname, port, 0, 0);
}

mailstream * mailstream_cfstream_open_timeout(const char * hostname, int16_t port, time_t timeout)
{
	return mailstream_cfstream_open_voip_timeout(hostname, port, 0, timeout);
}

mailstream * mailstream_cfstream_open_voip(const char * hostname, int16_t port, int voip_enabled)
{
	return mailstream_cfstream_open_voip_timeout(hostname, port, voip_enabled, 0);
}

mailstream * mailstream_cfstream_open_voip_timeout(const char * hostname, int16_t port, int voip_enabled,
  time_t timeout)
{
#if HAVE_CFNETWORK
  mailstream_low * low;
  mailstream * s;
  
  low = mailstream_low_cfstream_open_voip_timeout(hostname, port, voip_enabled, timeout);
  if (low == NULL) {
    return NULL;
  }
  s = mailstream_new(low, 8192);
  return s;
#else
  return NULL;
#endif
}

#if HAVE_CFNETWORK
static void cancelPerform(void *info)
{
  struct mailstream_cfstream_data * cfstream_data;
  mailstream_low * s;
  
  //fprintf(stderr, "cancelled\n");
  
  s = info;
  cfstream_data = (struct mailstream_cfstream_data *) s->data;
  cfstream_data->cancelled = true;
}

static void readDataFromStream(mailstream_low * s)
{
  struct mailstream_cfstream_data * cfstream_data;
  
  cfstream_data = (struct mailstream_cfstream_data *) s->data;
  
  cfstream_data->readResult = CFReadStreamRead(cfstream_data->readStream,
                                               cfstream_data->readBuffer,
                                               cfstream_data->readBufferSize);
  //fprintf(stderr, "data read %i\n", (int) cfstream_data->readResult);
}

static void writeDataToStream(mailstream_low * s)
{
  struct mailstream_cfstream_data * cfstream_data;
  
  cfstream_data = (struct mailstream_cfstream_data *) s->data;
  
  cfstream_data->writeResult = CFWriteStreamWrite(cfstream_data->writeStream,
                                                  cfstream_data->writeBuffer,
                                                  cfstream_data->writeBufferSize);
  //fprintf(stderr, "data written %i\n", (int) cfstream_data->writeResult);
}

static void readStreamCallback(CFReadStreamRef stream, CFStreamEventType eventType, void *clientCallBackInfo)
{
  mailstream_low * s;
  struct mailstream_cfstream_data * cfstream_data;
  
  s = (mailstream_low *) clientCallBackInfo;
  cfstream_data = (struct mailstream_cfstream_data *) s->data;
  
  switch (eventType) {
    case kCFStreamEventNone:
      break;
    case kCFStreamEventOpenCompleted:
      cfstream_data->readResult = 0;
      cfstream_data->readOpenResult = 0;
      switch (cfstream_data->state) {
        case STATE_WAIT_OPEN:
          cfstream_data->state = STATE_OPEN_READ_DONE;
          break;
        case STATE_OPEN_WRITE_DONE:
          cfstream_data->state = STATE_OPEN_WRITE_READ_DONE;
          break;
      }
      break;
    case kCFStreamEventHasBytesAvailable:
      cfstream_data->readSSLResult = 0;
      switch (cfstream_data->state) {
        case STATE_WAIT_READ:
          //fprintf(stderr, "has data\n");
          readDataFromStream(s);
          cfstream_data->state = STATE_READ_DONE;
          break;
        case STATE_WAIT_IDLE:
          cfstream_data->state = STATE_IDLE_DONE;
          break;
        case STATE_WAIT_SSL:
          cfstream_data->state = STATE_SSL_READ_DONE;
          break;
        case STATE_SSL_WRITE_DONE:
          cfstream_data->state = STATE_SSL_WRITE_READ_DONE;
          break;
      }
      break;
    case kCFStreamEventCanAcceptBytes:
      break;
    case kCFStreamEventErrorOccurred:
      cfstream_data->readResult = -1;
      cfstream_data->readOpenResult = -1;
      cfstream_data->readSSLResult = -1;
      switch (cfstream_data->state) {
        case STATE_WAIT_OPEN:
          cfstream_data->state = STATE_OPEN_READ_DONE;
          break;
        case STATE_OPEN_WRITE_DONE:
          cfstream_data->state = STATE_OPEN_WRITE_READ_DONE;
          break;
        case STATE_WAIT_READ:
          //fprintf(stderr, "error read\n");
          cfstream_data->state = STATE_READ_DONE;
          break;
        case STATE_WAIT_IDLE:
          cfstream_data->state = STATE_IDLE_DONE;
          break;
        case STATE_WAIT_SSL:
          cfstream_data->state = STATE_SSL_READ_DONE;
          break;
        case STATE_SSL_WRITE_DONE:
          cfstream_data->state = STATE_SSL_WRITE_READ_DONE;
          break;
      }
      break;
    case kCFStreamEventEndEncountered:
      cfstream_data->readResult = 0;
      cfstream_data->readOpenResult = 0;
      cfstream_data->readSSLResult = 0;
      switch (cfstream_data->state) {
        case STATE_WAIT_OPEN:
          cfstream_data->state = STATE_OPEN_READ_DONE;
          break;
        case STATE_OPEN_WRITE_DONE:
          cfstream_data->state = STATE_OPEN_WRITE_READ_DONE;
          break;
        case STATE_WAIT_READ:
          //fprintf(stderr, "end read\n");
          cfstream_data->state = STATE_READ_DONE;
          break;
        case STATE_WAIT_IDLE:
          cfstream_data->state = STATE_IDLE_DONE;
          break;
      }
      break;
  }
}

static void writeStreamCallback(CFWriteStreamRef stream, CFStreamEventType eventType, void *clientCallBackInfo)
{
  mailstream_low * s;
  struct mailstream_cfstream_data * cfstream_data;
  
  s = (mailstream_low *) clientCallBackInfo;
  cfstream_data = (struct mailstream_cfstream_data *) s->data;
  
  switch (eventType) {
    case kCFStreamEventNone:
      break;
    case kCFStreamEventOpenCompleted:
      cfstream_data->writeResult = 0;
      cfstream_data->writeOpenResult = 0;
      switch (cfstream_data->state) {
        case STATE_WAIT_OPEN:
          cfstream_data->state = STATE_OPEN_WRITE_DONE;
          break;
        case STATE_OPEN_READ_DONE:
          cfstream_data->state = STATE_OPEN_READ_WRITE_DONE;
          break;
      }
      break;
    case kCFStreamEventHasBytesAvailable:
      break;
    case kCFStreamEventCanAcceptBytes:
      //fprintf(stderr, "can accept\n");
      cfstream_data->writeSSLResult = 0;
      switch (cfstream_data->state) {
        case STATE_WAIT_WRITE:
          writeDataToStream(s);
          cfstream_data->state = STATE_WRITE_DONE;
          break;
        case STATE_WAIT_SSL:
          cfstream_data->state = STATE_SSL_WRITE_DONE;
          break;
        case STATE_SSL_READ_DONE:
          cfstream_data->state = STATE_SSL_READ_WRITE_DONE;
          break;
      }
      break;
    case kCFStreamEventErrorOccurred:
      cfstream_data->writeResult = -1;
      cfstream_data->writeOpenResult = -1;
      cfstream_data->writeSSLResult = -1;
      switch (cfstream_data->state) {
        case STATE_WAIT_OPEN:
          cfstream_data->state = STATE_OPEN_WRITE_DONE;
          break;
        case STATE_OPEN_READ_DONE:
          cfstream_data->state = STATE_OPEN_READ_WRITE_DONE;
          break;
        case STATE_WAIT_WRITE:
          cfstream_data->state = STATE_OPEN_WRITE_DONE;
          break;
        case STATE_WAIT_SSL:
          cfstream_data->state = STATE_SSL_WRITE_DONE;
          break;
        case STATE_SSL_READ_DONE:
          cfstream_data->state = STATE_SSL_READ_WRITE_DONE;
          break;
      }
      break;
    case kCFStreamEventEndEncountered:
      cfstream_data->writeResult = -1;
      cfstream_data->writeOpenResult = -1;
      cfstream_data->writeSSLResult = -1;
      switch (cfstream_data->state) {
        case STATE_WAIT_OPEN:
          cfstream_data->state = STATE_OPEN_WRITE_DONE;
          break;
        case STATE_OPEN_READ_DONE:
          cfstream_data->state = STATE_OPEN_READ_WRITE_DONE;
          break;
        case STATE_WAIT_WRITE:
          cfstream_data->state = STATE_OPEN_WRITE_DONE;
          break;
      }
      break;
  }
}
#endif

mailstream_low * mailstream_low_cfstream_open(const char * hostname, int16_t port)
{
    return mailstream_low_cfstream_open_voip_timeout(hostname, port, mailstream_cfstream_voip_enabled, 0);
}

mailstream_low * mailstream_low_cfstream_open_timeout(const char * hostname, int16_t port,
  time_t timeout)
{
	return mailstream_low_cfstream_open_voip_timeout(hostname, port,
	  mailstream_cfstream_voip_enabled, timeout);
}

mailstream_low * mailstream_low_cfstream_open_voip(const char * hostname, int16_t port, int voip_enabled)
{
	return mailstream_low_cfstream_open_voip_timeout(hostname, port, voip_enabled, 0);
}

#if HAVE_CFNETWORK
static int numberIntValue(CFNumberRef nb)
{
  if (nb == NULL) {
    return 0;
  }

  int result;
  CFNumberGetValue(nb, kCFNumberIntType, &result);
  return result;
}
#endif

mailstream_low * mailstream_low_cfstream_open_voip_timeout(const char * hostname, int16_t port,
  int voip_enabled, time_t timeout)
{
#if HAVE_CFNETWORK
  mailstream_low * s;
  struct mailstream_cfstream_data * cfstream_data;
  CFReadStreamRef readStream;
  CFWriteStreamRef writeStream;
  CFStringRef hostString;
  CFOptionFlags readFlags;
  CFOptionFlags writeFlags;
  int r;
  
  hostString = CFStringCreateWithCString(NULL, hostname, kCFStringEncodingUTF8);
  CFStreamCreatePairWithSocketToHost(NULL, hostString, port, &readStream, &writeStream);
  CFRelease(hostString);

#if TARGET_OS_IPHONE && !TARGET_IPHONE_SIMULATOR  
  if (voip_enabled) {
    CFReadStreamSetProperty(readStream, kCFStreamNetworkServiceType, kCFStreamNetworkServiceTypeVoIP);
    CFWriteStreamSetProperty(writeStream, kCFStreamNetworkServiceType, kCFStreamNetworkServiceTypeVoIP);
  }
#endif

#if !TARGET_OS_IPHONE && !TARGET_IPHONE_SIMULATOR
  CFDictionaryRef proxySettings = CFNetworkCopySystemProxySettings();
  CFNumberRef nbEnabled = CFDictionaryGetValue(proxySettings, kCFNetworkProxiesSOCKSEnable);
  if (numberIntValue(nbEnabled)) {
    CFReadStreamSetProperty(readStream, kCFStreamPropertySOCKSProxy, proxySettings);
    CFWriteStreamSetProperty(writeStream, kCFStreamPropertySOCKSProxy, proxySettings);
  }
  CFRelease(proxySettings);
#endif

  cfstream_data = cfstream_data_new(readStream, writeStream);
  s = mailstream_low_new(cfstream_data, mailstream_cfstream_driver);
	mailstream_low_set_timeout(s, timeout);  

  //fprintf(stderr, "open %s %i -> %p\n", hostname, port, s);
  
  /* setup streams */
  cfstream_data->streamContext.info = s;
  
  readFlags = kCFStreamEventOpenCompleted |
  kCFStreamEventHasBytesAvailable |
  kCFStreamEventErrorOccurred |
  kCFStreamEventEndEncountered;
  
  writeFlags = kCFStreamEventOpenCompleted |
  kCFStreamEventCanAcceptBytes |
  kCFStreamEventErrorOccurred |
  kCFStreamEventEndEncountered;
  
  CFReadStreamSetClient(cfstream_data->readStream, readFlags, readStreamCallback, &cfstream_data->streamContext);
  CFWriteStreamSetClient(cfstream_data->writeStream, writeFlags, writeStreamCallback, &cfstream_data->streamContext);
  
  CFRelease(readStream);
  CFRelease(writeStream);
  readStream = NULL;
  writeStream = NULL;
  
  /* setup cancel */
  cfstream_data->cancelContext.info = s;
  cfstream_data->cancelContext.perform = cancelPerform;
  cfstream_data->cancelSource = CFRunLoopSourceCreate(NULL, 0, &cfstream_data->cancelContext);
  
  r = low_open(s);
  if (r < 0) {
    mailstream_low_cfstream_close(s);
    return NULL;
  }
  
  return s;
#else
  return NULL;
#endif
}


static int mailstream_low_cfstream_close(mailstream_low * s)
{
#if HAVE_CFNETWORK
  struct mailstream_cfstream_data * cfstream_data;
  
  cfstream_data = (struct mailstream_cfstream_data *) s->data;
  
  if (cfstream_data->cancelSource != NULL) {
    CFRelease(cfstream_data->cancelSource);
    cfstream_data->cancelSource = NULL;
  }
  
  cfstream_data_close(cfstream_data);
  
  return 0;
#else
  return 0;
#endif
}

static void mailstream_low_cfstream_free(mailstream_low * s)
{
#if HAVE_CFNETWORK
  struct mailstream_cfstream_data * cfstream_data;
  
  cfstream_data = (struct mailstream_cfstream_data *) s->data;
  cfstream_data_free(cfstream_data);
  s->data = NULL;
  
  free(s);
#endif
}

static int mailstream_low_cfstream_get_fd(mailstream_low * s)
{
#if HAVE_CFNETWORK
  struct mailstream_cfstream_data * cfstream_data = NULL;
  CFDataRef native_handle_data = NULL;
  CFSocketNativeHandle native_handle_value = -1;
  CFIndex native_data_len  = 0;
  CFIndex native_value_len = 0;

  if (!s)
    return -1;

  cfstream_data = (struct mailstream_cfstream_data *) s->data;

  if (!cfstream_data->readStream)
    return -1;

  native_handle_data = (CFDataRef)CFReadStreamCopyProperty(cfstream_data->readStream, kCFStreamPropertySocketNativeHandle);
  if (!native_handle_data)
    return -1;

  native_data_len  = CFDataGetLength(native_handle_data);
  native_value_len = (CFIndex)sizeof(native_handle_value);

  if (native_data_len != native_value_len) {
    CFRelease(native_handle_data);
    return -1;
  }

  CFDataGetBytes(native_handle_data, CFRangeMake(0, MIN(native_data_len, native_value_len)), (UInt8 *)&native_handle_value);
  CFRelease(native_handle_data);

  return native_handle_value;
#else
  return -1;
#endif
}

#if HAVE_CFNETWORK
static void setup_runloop(mailstream_low * s)
{
  struct mailstream_cfstream_data * cfstream_data;
  
  cfstream_data = (struct mailstream_cfstream_data *) s->data;
  
  pthread_mutex_lock(&cfstream_data->runloop_lock);
  
  cfstream_data->runloop = (CFRunLoopRef) CFRetain(CFRunLoopGetCurrent());
  if (cfstream_data->cancelSource != NULL) {
    CFRunLoopAddSource(cfstream_data->runloop, cfstream_data->cancelSource, kCFRunLoopDefaultMode);
    //fprintf(stderr, "add cancel source %p\n", cfstream_data->cancelSource);
  }
  if (cfstream_data->idleInterruptedSource != NULL) {
    CFRunLoopAddSource(cfstream_data->runloop, cfstream_data->idleInterruptedSource, kCFRunLoopDefaultMode);
    //fprintf(stderr, "add idle source %p\n", cfstream_data->idleInterruptedSource);
  }
  
  pthread_mutex_unlock(&cfstream_data->runloop_lock);
}

static void unsetup_runloop(mailstream_low * s)
{
  struct mailstream_cfstream_data * cfstream_data;
  
  cfstream_data = (struct mailstream_cfstream_data *) s->data;
  
  pthread_mutex_lock(&cfstream_data->runloop_lock);
  
  if (cfstream_data->idleInterruptedSource != NULL) {
    CFRunLoopRemoveSource(cfstream_data->runloop, cfstream_data->idleInterruptedSource, kCFRunLoopDefaultMode);
  }
  if (cfstream_data->cancelSource != NULL) {
    CFRunLoopRemoveSource(cfstream_data->runloop, cfstream_data->cancelSource, kCFRunLoopDefaultMode);
  }
  if (cfstream_data->runloop != NULL) {
    CFRelease(cfstream_data->runloop);
    cfstream_data->runloop = NULL;
  }
  
  
  pthread_mutex_unlock(&cfstream_data->runloop_lock);
}

enum {
  WAIT_RUNLOOP_EXIT_NO_ERROR,
  WAIT_RUNLOOP_EXIT_INTERRUPTED,
  WAIT_RUNLOOP_EXIT_CANCELLED,
  WAIT_RUNLOOP_EXIT_TIMEOUT,
};

static int wait_runloop(mailstream_low * s, int wait_state)
{
  struct mailstream_cfstream_data * cfstream_data;
  int read_scheduled;
  int write_scheduled;
  int error;
  
  setup_runloop(s);
  
  cfstream_data = (struct mailstream_cfstream_data *) s->data;
  cfstream_data->state = wait_state;
  
  read_scheduled = 0;
  write_scheduled = 0;
  error = WAIT_RUNLOOP_EXIT_NO_ERROR;
  
  switch (wait_state) {
    case STATE_WAIT_OPEN:
      //fprintf(stderr, "wait open\n");
      CFReadStreamScheduleWithRunLoop(cfstream_data->readStream, cfstream_data->runloop, kCFRunLoopDefaultMode);
      CFWriteStreamScheduleWithRunLoop(cfstream_data->writeStream, cfstream_data->runloop, kCFRunLoopDefaultMode);
      read_scheduled = 1;
      write_scheduled = 1;
      break;
    case STATE_WAIT_READ:
      //fprintf(stderr, "wait read\n");
      CFReadStreamScheduleWithRunLoop(cfstream_data->readStream, cfstream_data->runloop, kCFRunLoopDefaultMode);
      read_scheduled = 1;
      break;
    case STATE_WAIT_WRITE:
      //fprintf(stderr, "wait write\n");
      CFWriteStreamScheduleWithRunLoop(cfstream_data->writeStream, cfstream_data->runloop, kCFRunLoopDefaultMode);
      write_scheduled = 1;
      break;
    case STATE_WAIT_IDLE:
      //fprintf(stderr, "wait idle\n");
      CFReadStreamScheduleWithRunLoop(cfstream_data->readStream, cfstream_data->runloop, kCFRunLoopDefaultMode);
      read_scheduled = 1;
      break;
    case STATE_WAIT_SSL:
      //fprintf(stderr, "wait ssl\n");
      CFReadStreamScheduleWithRunLoop(cfstream_data->readStream, cfstream_data->runloop, kCFRunLoopDefaultMode);
      CFWriteStreamScheduleWithRunLoop(cfstream_data->writeStream, cfstream_data->runloop, kCFRunLoopDefaultMode);
      read_scheduled = 1;
      write_scheduled = 1;
      break;
  }
  
  if (read_scheduled) {
    if (CFReadStreamHasBytesAvailable(cfstream_data->readStream)) {
      readStreamCallback(cfstream_data->readStream, kCFStreamEventHasBytesAvailable, s);
    }
  }
  if (write_scheduled) {
    if (CFWriteStreamCanAcceptBytes(cfstream_data->writeStream)) {
      writeStreamCallback(cfstream_data->writeStream, kCFStreamEventCanAcceptBytes, s);
    }
  }
  
  while (1) {
    struct timeval timeout;
    CFTimeInterval delay;
    int r;
    int done;
    
    if (cfstream_data->cancelled) {
      error = WAIT_RUNLOOP_EXIT_CANCELLED;
      break;
    }
    if (cfstream_data->state == STATE_WAIT_IDLE) {
      if (cfstream_data->idleInterrupted) {
        error = WAIT_RUNLOOP_EXIT_INTERRUPTED;
        break;
      }
    }

    done = 0;
    switch (cfstream_data->state) {
      case STATE_OPEN_READ_DONE:
        CFReadStreamUnscheduleFromRunLoop(cfstream_data->readStream, cfstream_data->runloop, kCFRunLoopDefaultMode);
        read_scheduled = 0;
        break;
      case STATE_OPEN_WRITE_DONE:
        CFWriteStreamUnscheduleFromRunLoop(cfstream_data->writeStream, cfstream_data->runloop, kCFRunLoopDefaultMode);
        write_scheduled = 0;
        break;
      case STATE_OPEN_READ_WRITE_DONE:
        done = 1;
        break;
      case STATE_OPEN_WRITE_READ_DONE:
        done = 1;
        break;
      case STATE_READ_DONE:
        done = 1;
        break;
      case STATE_WRITE_DONE:
        done = 1;
        break;
      case STATE_IDLE_DONE:
        done = 1;
        break;
      case STATE_SSL_READ_DONE:
        done = 1;
        break;
      case STATE_SSL_WRITE_DONE:
        done = 1;
        break;
      case STATE_SSL_READ_WRITE_DONE:
        done = 1;
        break;
      case STATE_SSL_WRITE_READ_DONE:
        done = 1;
        break;
    }
    
    if (done) {
      break;
    }
    
    if (wait_state == STATE_WAIT_IDLE) {
      timeout.tv_sec = cfstream_data->idleMaxDelay;
      timeout.tv_usec = 0;
    }
    else {
	    if (s->timeout == 0) {
				timeout = mailstream_network_delay;
			}
			else {
	      timeout.tv_sec = s->timeout;
	      timeout.tv_usec = 0;
			}
    }
    delay = (CFTimeInterval) timeout.tv_sec + (CFTimeInterval) timeout.tv_usec / (CFTimeInterval) 1e6;
    
    r = CFRunLoopRunInMode(kCFRunLoopDefaultMode, delay, true);
    if (r == kCFRunLoopRunTimedOut) {
      error = WAIT_RUNLOOP_EXIT_TIMEOUT;
      break;
    }
  }
  
  if (read_scheduled) {
    CFReadStreamUnscheduleFromRunLoop(cfstream_data->readStream, cfstream_data->runloop, kCFRunLoopDefaultMode);
  }
  if (write_scheduled) {
    CFWriteStreamUnscheduleFromRunLoop(cfstream_data->writeStream, cfstream_data->runloop, kCFRunLoopDefaultMode);
  }
  
  unsetup_runloop(s);
  
  if (error != WAIT_RUNLOOP_EXIT_NO_ERROR)
    return error;
  
  return WAIT_RUNLOOP_EXIT_NO_ERROR;
}
#endif

static ssize_t mailstream_low_cfstream_read(mailstream_low * s,
                                            void * buf, size_t count)
{
#if HAVE_CFNETWORK
  struct mailstream_cfstream_data * cfstream_data;
  int r;
  
  cfstream_data = (struct mailstream_cfstream_data *) s->data;
  cfstream_data->readBuffer = buf;
  cfstream_data->readBufferSize = count;
  
  if (cfstream_data->cancelled) {
    return -1;
  }
 
  if (CFReadStreamGetStatus(cfstream_data->readStream) == kCFStreamStatusError) {
    return -1;
  }
  
  if (CFReadStreamHasBytesAvailable(cfstream_data->readStream)) {
    readDataFromStream(s);
    return cfstream_data->readResult;
  }
  
  r = wait_runloop(s, STATE_WAIT_READ);
  if (r != WAIT_RUNLOOP_EXIT_NO_ERROR) {
    return -1;
  }
  
  return cfstream_data->readResult;
#else
  return -1;
#endif
}

static ssize_t mailstream_low_cfstream_write(mailstream_low * s,
                                             const void * buf, size_t count)
{
#if HAVE_CFNETWORK
  struct mailstream_cfstream_data * cfstream_data;
  int r;
  
  cfstream_data = (struct mailstream_cfstream_data *) s->data;
  cfstream_data->writeBuffer = buf;
  cfstream_data->writeBufferSize = count;
  
  if (cfstream_data->cancelled)
    return -1;
  
  if (CFWriteStreamGetStatus(cfstream_data->writeStream) == kCFStreamStatusError) {
    return -1;
  }
  
  if (CFWriteStreamCanAcceptBytes(cfstream_data->writeStream)) {
    writeDataToStream(s);
    return cfstream_data->writeResult;
  }
  
  r = wait_runloop(s, STATE_WAIT_WRITE);
  if (r != WAIT_RUNLOOP_EXIT_NO_ERROR) {
    return -1;
  }
  
  return cfstream_data->writeResult;
#else
  return -1;
#endif
}

#if HAVE_CFNETWORK
static int low_open(mailstream_low * s)
{
  struct mailstream_cfstream_data * cfstream_data;
  int r;
  
  cfstream_data = (struct mailstream_cfstream_data *) s->data;
  
  CFReadStreamOpen(cfstream_data->readStream);
  CFWriteStreamOpen(cfstream_data->writeStream);
  
  r = wait_runloop(s, STATE_WAIT_OPEN);
  if (r != WAIT_RUNLOOP_EXIT_NO_ERROR) {
    return -1;
  }
  
  if (cfstream_data->writeOpenResult < 0)
    return -1;
  if (cfstream_data->readOpenResult < 0)
    return -1;
  
  return 0;
}
#endif

static void mailstream_low_cfstream_cancel(mailstream_low * s)
{
#if HAVE_CFNETWORK
  struct mailstream_cfstream_data * cfstream_data;
  
  cfstream_data = (struct mailstream_cfstream_data *) s->data;
  
  pthread_mutex_lock(&cfstream_data->runloop_lock);
  
  if (cfstream_data->cancelSource != NULL) {
    CFRunLoopSourceSignal(cfstream_data->cancelSource);
  }
  if (cfstream_data->runloop != NULL) {
    CFRunLoopWakeUp(cfstream_data->runloop);
  }
  
  pthread_mutex_unlock(&cfstream_data->runloop_lock);
#endif
}

int mailstream_cfstream_set_ssl_enabled(mailstream * s, int ssl_enabled)
{
#if HAVE_CFNETWORK
  struct mailstream_cfstream_data * cfstream_data;
  int r;
  CFIndex count;
  
  cfstream_data = (struct mailstream_cfstream_data *) s->low->data;
  cfstream_data->ssl_enabled = ssl_enabled;
  if (ssl_enabled) {
    CFMutableDictionaryRef settings;
    
    settings = CFDictionaryCreateMutable(NULL, 0, &kCFTypeDictionaryKeyCallBacks, &kCFTypeDictionaryValueCallBacks);
    switch (cfstream_data->ssl_level) {
      case MAILSTREAM_CFSTREAM_SSL_LEVEL_NONE:
        CFDictionarySetValue(settings, kCFStreamSSLLevel, kCFStreamSocketSecurityLevelNone);
        break;
      case MAILSTREAM_CFSTREAM_SSL_LEVEL_SSLv2:
        CFDictionarySetValue(settings, kCFStreamSSLLevel, kCFStreamSocketSecurityLevelSSLv2);
        break;
      case MAILSTREAM_CFSTREAM_SSL_LEVEL_SSLv3:
        CFDictionarySetValue(settings, kCFStreamSSLLevel, kCFStreamSocketSecurityLevelSSLv3);
        break;
      case MAILSTREAM_CFSTREAM_SSL_LEVEL_TLSv1:
        CFDictionarySetValue(settings, kCFStreamSSLLevel, kCFStreamSocketSecurityLevelTLSv1);
        break;
      case MAILSTREAM_CFSTREAM_SSL_LEVEL_NEGOCIATED_SSL:
        CFDictionarySetValue(settings, kCFStreamSSLLevel, kCFStreamSocketSecurityLevelNegotiatedSSL);
        break;
    }
    
    if ((cfstream_data->ssl_certificate_verification_mask & MAILSTREAM_CFSTREAM_SSL_ALLOWS_EXPIRED_CERTIFICATES) != 0) {
      CFDictionarySetValue(settings, kCFStreamSSLAllowsExpiredCertificates, kCFBooleanTrue);
    }
    if ((cfstream_data->ssl_certificate_verification_mask & MAILSTREAM_CFSTREAM_SSL_ALLOWS_EXPIRED_ROOTS) != 0) {
      CFDictionarySetValue(settings, kCFStreamSSLAllowsExpiredRoots, kCFBooleanTrue);
    }
    if ((cfstream_data->ssl_certificate_verification_mask & MAILSTREAM_CFSTREAM_SSL_ALLOWS_ANY_ROOT) != 0) {
      CFDictionarySetValue(settings, kCFStreamSSLAllowsAnyRoot, kCFBooleanTrue);
    }
    if ((cfstream_data->ssl_certificate_verification_mask & MAILSTREAM_CFSTREAM_SSL_DISABLE_VALIDATES_CERTIFICATE_CHAIN) != 0) {
      CFDictionarySetValue(settings, kCFStreamSSLValidatesCertificateChain, kCFBooleanFalse);
    }
    
    CFReadStreamSetProperty(cfstream_data->readStream, kCFStreamPropertySSLSettings, settings);
    CFWriteStreamSetProperty(cfstream_data->writeStream, kCFStreamPropertySSLSettings, settings);
    CFRelease(settings);
  }
  else {
    CFMutableDictionaryRef settings;
    
    settings = CFDictionaryCreateMutable(NULL, 0, &kCFTypeDictionaryKeyCallBacks, &kCFTypeDictionaryValueCallBacks);
    CFDictionarySetValue(settings, kCFStreamSSLLevel, kCFStreamSocketSecurityLevelNone);
		CFReadStreamSetProperty(cfstream_data->readStream, kCFStreamPropertySSLSettings, settings);
		CFWriteStreamSetProperty(cfstream_data->writeStream, kCFStreamPropertySSLSettings, settings);
    CFRelease(settings);
  }
  
  // We need to investigate more about how to establish a STARTTLS connection.
  // For now, wait until we get the certificate chain.
  
  CFArrayRef certs;
  SecTrustRef secTrust;
  while (1) {
    r = wait_runloop(s->low, STATE_WAIT_SSL);
    if (r != WAIT_RUNLOOP_EXIT_NO_ERROR) {
      return -1;
    }
    if (cfstream_data->writeSSLResult < 0)
      return -1;
    if (cfstream_data->readSSLResult < 0)
      return -1;
    
    secTrust = (SecTrustRef)CFReadStreamCopyProperty(cfstream_data->readStream, kCFStreamPropertySSLPeerTrust);
    if (secTrust) {
        // SecTrustEvaluate() needs to be called before SecTrustGetCertificateCount() in Mac OS X <= 10.8
        SecTrustEvaluate(secTrust, NULL);
        count = SecTrustGetCertificateCount(secTrust);
        CFRelease(secTrust);
    }
    else {
        certs = CFReadStreamCopyProperty(cfstream_data->readStream, kCFStreamPropertySSLPeerCertificates);
        if (certs) {
            count = CFArrayGetCount(certs);
            CFRelease(certs);
        }
        else {
            // No trust and no certs, wait more.
            continue;
        }
    }
      
    if (count == 0) {
      // No certificates, wait more.
      continue;
    }
    
    break;
  }
  
  return 0;
#else
  return -1;
#endif
}

int mailstream_cfstream_is_ssl_enabled(mailstream * s)
{
#if HAVE_CFNETWORK
  struct mailstream_cfstream_data * cfstream_data;
  cfstream_data = (struct mailstream_cfstream_data *) s->low->data;
  return cfstream_data->ssl_enabled;
#else
  return 0;
#endif
}

void mailstream_cfstream_set_ssl_verification_mask(mailstream * s, int verification_mask)
{
#if HAVE_CFNETWORK
  struct mailstream_cfstream_data * cfstream_data;
  cfstream_data = (struct mailstream_cfstream_data *) s->low->data;
  cfstream_data->ssl_certificate_verification_mask = verification_mask;
#endif
}

void mailstream_cfstream_set_ssl_peer_name(mailstream * s, const char * peer_name)
{
#if HAVE_CFNETWORK
  struct mailstream_cfstream_data * cfstream_data;
  cfstream_data = (struct mailstream_cfstream_data *) s->low->data;
  
  if (cfstream_data->ssl_peer_name != peer_name) {
    free(cfstream_data->ssl_peer_name);
    cfstream_data->ssl_peer_name = NULL;
    if (peer_name != NULL) {
      cfstream_data->ssl_peer_name = strdup(peer_name);
    }
  }
#endif
}

void mailstream_cfstream_set_ssl_is_server(mailstream * s, int is_server)
{
#if HAVE_CFNETWORK
  struct mailstream_cfstream_data * cfstream_data;
  cfstream_data = (struct mailstream_cfstream_data *) s->low->data;
  cfstream_data->ssl_is_server = is_server;
#endif
}

void mailstream_cfstream_set_ssl_level(mailstream * s, int ssl_level)
{
#if HAVE_CFNETWORK
  struct mailstream_cfstream_data * cfstream_data;
  cfstream_data = (struct mailstream_cfstream_data *) s->low->data;
  cfstream_data->ssl_level = ssl_level;
#endif
}

int mailstream_cfstream_wait_idle(mailstream * s, int max_idle_delay)
{
  return mailstream_low_cfstream_wait_idle(s->low, max_idle_delay);
}

int mailstream_low_cfstream_wait_idle(mailstream_low * low, int max_idle_delay)
{
#if HAVE_CFNETWORK
  struct mailstream_cfstream_data * cfstream_data;
  int r;
  
  cfstream_data = (struct mailstream_cfstream_data *) low->data;
  cfstream_data->idleMaxDelay = max_idle_delay;
  
  r = wait_runloop(low, STATE_WAIT_IDLE);
  switch (r) {
    case WAIT_RUNLOOP_EXIT_TIMEOUT:
      return MAILSTREAM_IDLE_TIMEOUT;
    case WAIT_RUNLOOP_EXIT_INTERRUPTED:
      return MAILSTREAM_IDLE_INTERRUPTED;
    case WAIT_RUNLOOP_EXIT_CANCELLED:
      return MAILSTREAM_IDLE_CANCELLED;
  }
  return MAILSTREAM_IDLE_HASDATA;
#else
  return MAILSTREAM_IDLE_ERROR;
#endif
}

#if HAVE_CFNETWORK
static void idleInterruptedPerform(void *info)
{
  struct mailstream_cfstream_data * cfstream_data;
  mailstream_low * s;
  
  s = info;
  
  cfstream_data = (struct mailstream_cfstream_data *) s->data;
  cfstream_data->idleInterrupted = true;
}
#endif

static int mailstream_low_cfstream_setup_idle(mailstream_low * s)
{
#if HAVE_CFNETWORK
  struct mailstream_cfstream_data * cfstream_data;
  
  cfstream_data = (struct mailstream_cfstream_data *) s->data;
  cfstream_data->idleInterrupted = false;
  cfstream_data->idleInterruptedContext.info = s;
  cfstream_data->idleInterruptedContext.perform = idleInterruptedPerform;
  cfstream_data->idleInterruptedSource = CFRunLoopSourceCreate(NULL, 0, &cfstream_data->idleInterruptedContext);
  return 0;
#else
  return -1;
#endif
}

static int mailstream_low_cfstream_unsetup_idle(mailstream_low * s)
{
#if HAVE_CFNETWORK
  struct mailstream_cfstream_data * cfstream_data;
  
  cfstream_data = (struct mailstream_cfstream_data *) s->data;
  if (cfstream_data->idleInterruptedSource != NULL) {
    CFRelease(cfstream_data->idleInterruptedSource);
    cfstream_data->idleInterruptedSource = NULL;
  }
  return 0;
#else
  return -1;
#endif
}

static int mailstream_low_cfstream_interrupt_idle(mailstream_low * s)
{
#if HAVE_CFNETWORK
  struct mailstream_cfstream_data * cfstream_data;
  
  cfstream_data = (struct mailstream_cfstream_data *) s->data;
  
  pthread_mutex_lock(&cfstream_data->runloop_lock);
  
  if (cfstream_data->idleInterruptedSource != NULL) {
    CFRunLoopSourceSignal(cfstream_data->idleInterruptedSource);
  }
  if (cfstream_data->runloop != NULL) {
    CFRunLoopWakeUp(cfstream_data->runloop);
  }
  
  pthread_mutex_unlock(&cfstream_data->runloop_lock);
  return 0;
#else
  return -1;
#endif
}

static carray * mailstream_low_cfstream_get_certificate_chain(mailstream_low * s)
{
#if HAVE_CFNETWORK
  struct mailstream_cfstream_data * cfstream_data;
  unsigned int i;
  carray * result;
  CFArrayRef certs;
  CFIndex count;
  
  cfstream_data = (struct mailstream_cfstream_data *) s->data;
    
  SecTrustRef secTrust = (SecTrustRef)CFReadStreamCopyProperty(cfstream_data->readStream, kCFStreamPropertySSLPeerTrust);
  if (secTrust) {
      // SecTrustEvaluate() needs to be called before SecTrustGetCertificateCount() in Mac OS X <= 10.8
      SecTrustEvaluate(secTrust, NULL);
      count = SecTrustGetCertificateCount(secTrust);
      result = carray_new(4);
      for(i = 0 ; i < count ; i ++) {
          SecCertificateRef cert = (SecCertificateRef) SecTrustGetCertificateAtIndex(secTrust, i);
          CFDataRef data = SecCertificateCopyData(cert);
          if (data == NULL) {
            carray_free(result);
            CFRelease(secTrust);
            return NULL;
          }
          CFIndex length = CFDataGetLength(data);
          const UInt8 * bytes = CFDataGetBytePtr(data);
          MMAPString * str = mmap_string_sized_new(length);
          mmap_string_append_len(str, (char*) bytes, length);
          carray_add(result, str, NULL);
          CFRelease(data);
      }
      CFRelease(secTrust);
  }
  else {
      certs = CFReadStreamCopyProperty(cfstream_data->readStream, kCFStreamPropertySSLPeerCertificates);
      if (certs) {
          count = CFArrayGetCount(certs);
          result = carray_new(4);
          for(i = 0 ; i < count ; i ++) {
              SecCertificateRef cert = (SecCertificateRef) CFArrayGetValueAtIndex(certs, i);
              CFDataRef data = SecCertificateCopyData(cert);
              if (data == NULL) {
                carray_free(result);
                CFRelease(certs);
                return NULL;
              }
              CFIndex length = CFDataGetLength(data);
              const UInt8 * bytes = CFDataGetBytePtr(data);
              MMAPString * str = mmap_string_sized_new(length);
              mmap_string_append_len(str, (char*) bytes, length);
              carray_add(result, str, NULL);
              CFRelease(data);
          }
          CFRelease(certs);
      }
      else {
          return NULL;
      }
  }
    
  return result;
#else
  return NULL;
#endif
}
