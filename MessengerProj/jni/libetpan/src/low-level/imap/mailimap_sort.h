/*
 * libEtPan! -- a mail stuff library
 *
 * Copyright (C) 2001, 2011 - DINH Viet Hoa
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

#ifndef libetpan_mailimap_sort_h
#define libetpan_mailimap_sort_h

#ifdef __cplusplus
extern "C" {
#endif
  
#include <libetpan/libetpan-config.h>
#include <libetpan/mailimap_extension.h>
#include <libetpan/mailimap_sort_types.h>
  
  LIBETPAN_EXPORT
  extern struct mailimap_extension_api mailimap_extension_sort;
  
  
  /*
   mailimap_sort()
   
   All mails that match the given criteria will be returned
   their numbers sorted by the given sorting criteria in the result list.
   
   @param session  IMAP session
   @param charset  This indicates the charset of the strings that appears
   in the searching criteria
   @param key      This is the searching criteria
   @param result   The result is a clist of (uint32_t *) and will be
   stored in (* result).
   
   @return the return code is one of MAILIMAP_ERROR_XXX or
   MAILIMAP_NO_ERROR codes
   */
  
  LIBETPAN_EXPORT
  int
  mailimap_sort(mailimap * session, const char * charset,
                struct mailimap_sort_key * key, struct mailimap_search_key * searchkey,
                clist ** result);
  
  /*
   mailimap_uid_sort()
   
   
   All mails that match the given criteria will be returned
   their unique identifiers sorted by the given sorting criteria in the result list.
   
   @param session    IMAP session
   @param charset    This indicates the charset of the strings that appears
   in the searching criteria
   @param key        This is the sorting criteria
   @param searchkey  This is the searching criteria
   @param result     The result is a clist of (uint32_t *) and will be
   stored in (* result).
   
   @return the return code is one of MAILIMAP_ERROR_XXX or
   MAILIMAP_NO_ERROR codes
   */
  
  LIBETPAN_EXPORT
  int
  mailimap_uid_sort(mailimap * session, const char * charset,
                    struct mailimap_sort_key * key, struct mailimap_search_key * searchkey,
                    clist ** result);

  LIBETPAN_EXPORT
  void mailimap_sort_result_free(clist * search_result);
  
#ifdef __cplusplus
}
#endif

#endif
