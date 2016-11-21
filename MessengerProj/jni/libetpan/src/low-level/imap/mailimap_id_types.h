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
#ifndef MAILIMAP_ID_TYPES_H

#define MAILIMAP_ID_TYPES_H

#include <libetpan/clist.h>

#ifdef __cplusplus
extern "C" {
#endif

struct mailimap_id_params_list {
  clist * /* struct mailimap_id_param */ idpa_list;
};

LIBETPAN_EXPORT
struct mailimap_id_params_list * mailimap_id_params_list_new(clist * items);

LIBETPAN_EXPORT
void mailimap_id_params_list_free(struct mailimap_id_params_list * list);

struct mailimap_id_param {
  char * idpa_name;
  char * idpa_value;
};

LIBETPAN_EXPORT
struct mailimap_id_param * mailimap_id_param_new(char * name, char * value);

LIBETPAN_EXPORT
void mailimap_id_param_free(struct mailimap_id_param * param);

LIBETPAN_EXPORT
struct mailimap_id_params_list * mailimap_id_params_list_new_empty(void);

LIBETPAN_EXPORT
int mailimap_id_params_list_add_name_value(struct mailimap_id_params_list * list, char * name, char * value);

#ifdef __cplusplus
}
#endif

#endif
