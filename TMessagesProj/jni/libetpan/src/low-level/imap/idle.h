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
#ifndef MAILIMAP_IDLE_H

#define MAILIMAP_IDLE_H

#ifdef __cplusplus
extern "C" {
#endif

#include "mailimap_types.h"

LIBETPAN_EXPORT
int mailimap_idle(mailimap * session);

LIBETPAN_EXPORT
int mailimap_idle_done(mailimap * session);

LIBETPAN_EXPORT
int mailimap_idle_get_fd(mailimap * session);

/* delay in seconds */
LIBETPAN_EXPORT
void mailimap_idle_set_delay(mailimap * session, long delay);

LIBETPAN_EXPORT
long mailimap_idle_get_done_delay(mailimap * session);

LIBETPAN_EXPORT
int mailimap_has_idle(mailimap * session);

#ifdef __cplusplus
}
#endif

#endif
