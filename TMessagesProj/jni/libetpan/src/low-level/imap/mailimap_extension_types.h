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
#ifndef MAILIMAP_EXTENSION_TYPES_H

#define MAILIMAP_EXTENSION_TYPES_H

#include <libetpan/mailstream.h>

struct mailimap_extension_data;

/*
  this is the list of known extensions with the purpose to
  get integer identifers for the extensions.
*/

enum {
  MAILIMAP_EXTENSION_ANNOTATEMORE,  /* the annotatemore-draft */
  MAILIMAP_EXTENSION_ACL,           /* the acl capability */
  MAILIMAP_EXTENSION_UIDPLUS,       /* UIDPLUS */
  MAILIMAP_EXTENSION_QUOTA,         /* quota */
  MAILIMAP_EXTENSION_NAMESPACE,     /* namespace */
  MAILIMAP_EXTENSION_XLIST,         /* XLIST (Gmail and Zimbra have this) */
  MAILIMAP_EXTENSION_XGMLABELS,     /* X-GM-LABELS (Gmail) */
  MAILIMAP_EXTENSION_XGMMSGID,      /* X-GM-MSGID (Gmail) */
  MAILIMAP_EXTENSION_XGMTHRID,      /* X-GM-THRID (Gmail) */
  MAILIMAP_EXTENSION_ID,            /* ID */
  MAILIMAP_EXTENSION_ENABLE,        /* ENABLE */
  MAILIMAP_EXTENSION_CONDSTORE,     /* CONDSTORE */
  MAILIMAP_EXTENSION_QRESYNC,       /* QRESYNC */
  MAILIMAP_EXTENSION_SORT           /* SORT */
};


/*
  this is a list of extended parser functions. The extended parser
  passes its identifier to the extension parser.
*/

enum {
  MAILIMAP_EXTENDED_PARSER_RESPONSE_DATA,
  MAILIMAP_EXTENDED_PARSER_RESP_TEXT_CODE,
  MAILIMAP_EXTENDED_PARSER_MAILBOX_DATA,
  MAILIMAP_EXTENDED_PARSER_FETCH_DATA,
  MAILIMAP_EXTENDED_PARSER_STATUS_ATT
};

/*
  this is the extension interface. each extension consists
  of a initial parser and an initial free. the parser is
  passed the calling parser's identifier. based on this
  identifier the initial parser can then decide which
  actual parser to call. free has mailimap_extension_data
  as parameter. if you look at mailimap_extension_data
  you'll see that it contains "type" as one of its
  elements. thus an extension's initial free can call
  the correct actual free to free its data.
*/
struct mailimap_extension_api {
  char * ext_name;
  int ext_id; /* use -1 if this is an extension outside libetpan */

  int (* ext_parser)(int calling_parser, mailstream * fd,
            MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx,
            struct mailimap_extension_data ** result,
            size_t progr_rate,
            progress_function * progr_fun);

  void (* ext_free)(struct mailimap_extension_data * ext_data);
};

/*
  mailimap_extension_data is a wrapper for values parsed by extensions

  - extension is an identifier for the extension that parsed the value.

  - type is an identifier for the real type of the data.

  - data is a pointer to the real data.
*/
struct mailimap_extension_data {
  struct mailimap_extension_api * ext_extension;
  int ext_type;
  void * ext_data;
};

#endif
