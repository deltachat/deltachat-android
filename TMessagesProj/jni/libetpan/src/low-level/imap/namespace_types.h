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

#ifndef NAMESPACE_TYPES_H

#define NAMESPACE_TYPES_H

#include <libetpan/clist.h>

enum {
  MAILIMAP_NAMESPACE_TYPE_NAMESPACE
};

struct mailimap_namespace_response_extension {
  char * ns_name; /* != NULL */
  clist * ns_values; /* != NULL, list of char * */
};

LIBETPAN_EXPORT
struct mailimap_namespace_response_extension *
mailimap_namespace_response_extension_new(char * name,
                                          clist * values);

LIBETPAN_EXPORT
void mailimap_namespace_response_extension_free(struct mailimap_namespace_response_extension * ext);

struct mailimap_namespace_info {
  char * ns_prefix; /* != NULL */
  char ns_delimiter;
  clist * ns_extensions; /* can be NULL, list of mailimap_namespace_response_extension */
};

LIBETPAN_EXPORT
struct mailimap_namespace_info * mailimap_namespace_info_new(char * prefix, char delimiter,
                                                             clist * extensions);

LIBETPAN_EXPORT
void mailimap_namespace_info_free(struct mailimap_namespace_info * info);

struct mailimap_namespace_item {
  clist * ns_data_list; /* != NULL, list of mailimap_namespace_info */
};

LIBETPAN_EXPORT
struct mailimap_namespace_item * mailimap_namespace_item_new(clist * data_list);

LIBETPAN_EXPORT
void mailimap_namespace_item_free(struct mailimap_namespace_item * item);

struct mailimap_namespace_data {
  struct mailimap_namespace_item * ns_personal; /* can be NULL */
  struct mailimap_namespace_item * ns_other; /* can be NULL */
  struct mailimap_namespace_item * ns_shared; /* can be NULL */
};

LIBETPAN_EXPORT
struct mailimap_namespace_data *
mailimap_namespace_data_new(struct mailimap_namespace_item * personal,
                       struct mailimap_namespace_item * other,
                       struct mailimap_namespace_item * shared);

LIBETPAN_EXPORT
void mailimap_namespace_data_free(struct mailimap_namespace_data * ns);

#endif
