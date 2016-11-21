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
 * $Id: mailmime_content.h,v 1.16 2008/02/20 22:15:52 hoa Exp $
 */

#ifndef MAILMIME_CONTENT_H

#define MAILMIME_CONTENT_H

#ifdef __cplusplus
extern "C" {
#endif

#include <libetpan/mailmime_types.h>

LIBETPAN_EXPORT
char * mailmime_content_charset_get(struct mailmime_content * content);

LIBETPAN_EXPORT
char * mailmime_content_param_get(struct mailmime_content * content,
				  char * name);

LIBETPAN_EXPORT
int mailmime_parse(const char * message, size_t length,
		   size_t * indx, struct mailmime ** result);

LIBETPAN_EXPORT
int mailmime_get_section(struct mailmime * mime,
			 struct mailmime_section * section,
			 struct mailmime ** result);


LIBETPAN_EXPORT
char * mailmime_extract_boundary(struct mailmime_content * content_type);


/* decode */

LIBETPAN_EXPORT
int mailmime_base64_body_parse(const char * message, size_t length,
			       size_t * indx, char ** result,
			       size_t * result_len);

LIBETPAN_EXPORT
int mailmime_quoted_printable_body_parse(const char * message, size_t length,
					 size_t * indx, char ** result,
					 size_t * result_len, int in_header);


LIBETPAN_EXPORT
int mailmime_binary_body_parse(const char * message, size_t length,
			       size_t * indx, char ** result,
			       size_t * result_len);

/*
 mailmime_part_parse()

 This function gets full MIME part for parsing at once.
 It is not suitable, if we want parse incomplete message in a stream mode.
 
 @return the return code is one of MAILIMF_ERROR_XXX or
   MAILIMF_NO_ERROR codes
 */
LIBETPAN_EXPORT
int mailmime_part_parse(const char * message, size_t length,
			size_t * indx,
			int encoding, char ** result, size_t * result_len);


/*
 mailmime_part_parse_partial()

 This function may parse incomplete MIME part (i.e. in streaming mode).
 It stops when detect incomplete encoding unit at the end of data.
 Position of the first unparsed byte will be returned in (*indx) value.

 For parsing last portion of data must be used mailmime_part_parse() version.

 @param message    Message for unparsed data.
 @param length     Length of the unparsed data.
 @param INOUT indx Index of first unparsed symbol in the message.
 @param encoding   Encoding of the input data.
 @param result     Parsed MIME part content. Must be freed with mmap_string_unref().
 @param result_len Length of parsed data.

 @return the return code is one of MAILIMF_ERROR_XXX or
   MAILIMF_NO_ERROR codes

 Example Usage:
 @code
 uint32_t received = 0;
 uint32_t partLength = bodystructure[partId]->length;
 for (;;) {
   bool isThisRangeLast;
   struct imap_range_t range = { received, 1024*1024 };
   char *result;
   size_t result_len;
   int error = imap_fetch_part_range(uid, partId, range, &result, &result_len);
   if (error != NoError) {
     // handle network error
     break;
   }

   if (result_len == 0) {
     // requested range is empty. part is completely fetched
     break;
   }

   isThisRangeLast = (received + result_len >= partLength); // determine that the received data is the last,
                                                            // may be more difficult (in case of invalid metadata on the server).

   char *decoded;
   size_t decoded_len;
   if (isThisRangeLast) {
     uint32_t index = 0;
     mailmime_part_parse(result, result_len, encoding, &index, &decoded, &decoded_len);
     break;
   }
   else {
     uint32_t index = 0;
     mailmime_part_parse_partial(result, result_len, encoding, &index, &decoded, &decoded_len);
     // we may have some non-decoded bytes at the end of chunk.
     // in this case we just request it in the next chunk 
     received += index;
   }
 }
 @endcode
 */
LIBETPAN_EXPORT
int mailmime_part_parse_partial(const char * message, size_t length,
                                size_t * indx,
                                int encoding, char ** result, size_t * result_len);


LIBETPAN_EXPORT
int mailmime_get_section_id(struct mailmime * mime,
			    struct mailmime_section ** result);

#ifdef __cplusplus
}
#endif

#endif
