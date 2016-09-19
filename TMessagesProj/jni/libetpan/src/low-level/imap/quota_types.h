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

#ifndef QUOTA_TYPES_H

#define QUOTA_TYPES_H

#ifdef __cplusplus
extern "C" {
#endif

#include <libetpan/libetpan-config.h>
#include <libetpan/mailstream.h>
#include <libetpan/clist.h>

struct mailimap_quota_quota_resource {
	char * resource_name;
	uint32_t usage;
	uint32_t limit;
};

LIBETPAN_EXPORT
struct mailimap_quota_quota_resource *
mailimap_quota_quota_resource_new(char * resource_name,
		uint32_t usage, uint32_t limit);

LIBETPAN_EXPORT
void
mailimap_quota_quota_resource_free(struct mailimap_quota_quota_resource * res);



struct mailimap_quota_quota_data {
  char * quotaroot;
  clist * quota_list;
  /* list of (struct mailimap_quota_quota_resource *) */
};

LIBETPAN_EXPORT
struct mailimap_quota_quota_data *
mailimap_quota_quota_data_new(char * quotaroot, clist * quota_list);

LIBETPAN_EXPORT
void
mailimap_quota_quota_data_free(struct mailimap_quota_quota_data * data);



struct mailimap_quota_quotaroot_data {
  char * mailbox;
  clist * quotaroot_list;
  /* list of (char *) */
};

LIBETPAN_EXPORT
struct mailimap_quota_quotaroot_data *
mailimap_quota_quotaroot_data_new(char * mailbox, clist * quotaroot_list);

LIBETPAN_EXPORT
void
mailimap_quota_quotaroot_data_free(
    struct mailimap_quota_quotaroot_data * data);



enum {
  MAILIMAP_QUOTA_TYPE_QUOTA_DATA,       /* child of mailbox-data */
  MAILIMAP_QUOTA_TYPE_QUOTAROOT_DATA    /* child of mailbox-data */
};



struct mailimap_quota_complete_data {
  struct mailimap_quota_quotaroot_data * quotaroot_data;
  clist * quota_list;
  /* list of (struct mailimap_quota_quota_data *) */
};

LIBETPAN_EXPORT
struct mailimap_quota_complete_data *
mailimap_quota_complete_data_new(
    struct mailimap_quota_quotaroot_data * quotaroot_data,
    clist * quota_list);

LIBETPAN_EXPORT
void
mailimap_quota_complete_data_free(struct mailimap_quota_complete_data * data);

#ifdef __cplusplus
}
#endif

#endif
