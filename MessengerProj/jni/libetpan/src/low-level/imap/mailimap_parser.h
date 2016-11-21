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
 * $Id: mailimap_parser.h,v 1.17 2011/03/30 13:29:49 hoa Exp $
 */

#ifndef MAILIMAP_PARSER_H

#define MAILIMAP_PARSER_H

#ifdef __cplusplus
extern "C" {
#endif

#include "mailimap_types.h"

int mailimap_greeting_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
			    size_t * indx,
			    struct mailimap_greeting ** result,
			    size_t progr_rate,
			    progress_function * progr_fun);

int
mailimap_response_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
			size_t * indx, struct mailimap_response ** result,
			size_t progr_rate,
			progress_function * progr_fun);

int
mailimap_response_parse_with_context(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
                                     size_t * indx, struct mailimap_response ** result,
                                     mailprogress_function * body_progr_fun,
                                     mailprogress_function * items_progr_fun,
                                     void * context,
                                     mailimap_msg_att_handler * msg_att_handler,
                                     void * msg_att_context);
  
int
mailimap_continue_req_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
			    size_t * indx,
			    struct mailimap_continue_req ** result,
			    size_t progr_rate,
			    progress_function * progr_fun);

int
mailimap_response_data_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
			     size_t * indx,
			     struct mailimap_response_data ** result,
			     size_t progr_rate,
			     progress_function * progr_fun);

typedef int mailimap_struct_parser(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
				   size_t * indx, void * result,
				   size_t progr_rate,
				   progress_function * progr_fun);

typedef void mailimap_struct_destructor(void * result);

int
mailimap_mailbox_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
		       size_t * indx, char ** result,
		       size_t progr_rate,
		       progress_function * progr_fun);
int
mailimap_mailbox_list_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
                            size_t * indx,
                            struct mailimap_mailbox_list ** result,
                            size_t progr_rate,
                            progress_function * progr_fun);
  
int mailimap_nstring_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
				  size_t * indx, char ** result,
				  size_t * result_len,
				  size_t progr_rate,
				  progress_function * progr_fun);

int
mailimap_string_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
		      size_t * indx, char ** result,
		      size_t * result_len,
		      size_t progr_rate,
		      progress_function * progr_fun);

int
mailimap_struct_spaced_list_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
				  size_t * indx, clist ** result,
				  mailimap_struct_parser * parser,
				  mailimap_struct_destructor * destructor,
				  size_t progr_rate,
				  progress_function * progr_fun);

int mailimap_oparenth_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
				   size_t * indx);

int mailimap_cparenth_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
				   size_t * indx);

int mailimap_atom_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
			size_t * indx, char ** result,
			size_t progr_rate,
			progress_function * progr_fun);

int
mailimap_astring_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
		       size_t * indx,
		       char ** result,
		       size_t progr_rate,
		       progress_function * progr_fun);

int
mailimap_number_parse(mailstream * fd, MMAPString * buffer,
		      size_t * indx, uint32_t * result);

int
mailimap_nz_number_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
			 size_t * indx, uint32_t * result);

int
mailimap_struct_list_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
    size_t * indx, clist ** result,
    char symbol,
    mailimap_struct_parser * parser,
    mailimap_struct_destructor * destructor,
    size_t progr_rate,
    progress_function * progr_fun);

int mailimap_uniqueid_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
    size_t * indx, uint32_t * result);

int mailimap_colon_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
    size_t * indx);

int mailimap_dquote_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
                          size_t * indx);

int
mailimap_quoted_char_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
                           size_t * indx, char * result);
  
int mailimap_nil_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
                       size_t * indx);

int
mailimap_struct_multiple_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
			       size_t * indx, clist ** result,
			       mailimap_struct_parser * parser,
			       mailimap_struct_destructor * destructor,
			       size_t progr_rate,
			       progress_function * progr_fun);

int mailimap_capability_data_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
	size_t * indx,
	struct mailimap_capability_data ** result,
	size_t progr_rate,
	progress_function * progr_fun);

int mailimap_capability_list_parse(mailstream * fd,
  MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
  size_t * indx,
  clist ** result,
  size_t progr_rate,
  progress_function * progr_fun);

int mailimap_status_att_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
  size_t * indx, int * result);

int mailimap_nz_number_alloc_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
			       size_t * indx,
			       uint32_t ** result,
			       size_t progr_rate,
             progress_function * progr_fun);

int mailimap_mod_sequence_value_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
  size_t * indx, uint64_t * result);

int mailimap_uint64_parse(mailstream * fd, MMAPString * buffer, struct mailimap_parser_context * parser_ctx,
  size_t * indx, uint64_t * result);

int mailimap_set_parse(mailstream * fd,
  MMAPString * buffer, struct mailimap_parser_context * parser_ctx, size_t * indx, struct mailimap_set ** result);

LIBETPAN_EXPORT
int mailimap_hack_date_time_parse(char * str,
                                  struct mailimap_date_time ** result,
                                  size_t progr_rate,
                                  progress_function * progr_fun);

#ifdef __cplusplus
}
#endif

#endif
