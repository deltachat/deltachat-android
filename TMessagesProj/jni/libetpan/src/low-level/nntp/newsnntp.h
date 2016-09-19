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
 * $Id: newsnntp.h,v 1.21 2008/02/20 22:15:53 hoa Exp $
 */

#ifndef NEWSNNTP_H

#define NEWSNNTP_H

#ifdef __cplusplus
extern "C" {
#endif

#ifdef HAVE_INTTYPES_H
#	include <inttypes.h>
#endif
#include <sys/types.h>
#include <time.h>

#include <libetpan/clist.h>
#include <libetpan/mailstream.h>
#include <libetpan/newsnntp_socket.h>
#include <libetpan/newsnntp_ssl.h>
#include <libetpan/newsnntp_types.h>

/*
   newsnntp_new()
   
   This function returns a new NNTP session.
   
   @param progr_rate  When downloading messages, a function will be called
   each time the amount of bytes downloaded reaches a multiple of this
   value, this can be 0.
   @param progr_fun   This is the function to call to notify the progress,
   this can be NULL.
   
   @return an NNTP session is returned.
*/

LIBETPAN_EXPORT
newsnntp * newsnntp_new(size_t nntp_progr_rate,
    progress_function * nntp_progr_fun);

/*
   newsnntp_free()
   
   This function will free the data structures associated with
   the NNTP session.
   
   @param session   NNTP session
*/

LIBETPAN_EXPORT
void newsnntp_free(newsnntp * session);

/*
   newsnntp_set_logger() set a logger for the connection.
   
   @param session         NNTP session
   @param logger          logger function. See mailstream_types.h to know possible log_type values.
   str is the log, data received or data sent.
   @param logger_context  parameter that is passed to the logger function.
   @return the value of the timeout in seconds.
*/

LIBETPAN_EXPORT
void newsnntp_set_logger(newsnntp * session, void (* logger)(newsnntp * session, int log_type,
                                                             const char * str, size_t size, void * context), void * logger_context);

/*
   newsnntp_set_progress_callback() set NNTP progression callbacks.
   
   @param session           NNTP session
   @param progr_fun         callback function.
*/

LIBETPAN_EXPORT
void newsnntp_set_progress_callback(newsnntp * f, mailprogress_function * progr_fun, void * context);

/*
   newsnntp_set_timeout() set the network timeout of the NNTP session.
   
   @param session    NNTP session
   @param timeout    value of the timeout in seconds.
*/

LIBETPAN_EXPORT
void newsnntp_set_timeout(newsnntp * session, time_t timeout);

/*
   newsnntp_get_timeout() get the network timeout of the NNTP session.
   
   @param session    NNTP session
   
   @return           value of the timeout in seconds.
*/

LIBETPAN_EXPORT
time_t newsnntp_get_timeout(newsnntp * session);

/*
   newsnntp_connect()
   
   This function will connect the NNTP session with the given stream.
   
   @param session  the NNTP session
   @param s        stream to use
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_connect(newsnntp * session, mailstream * s);

/*
   newsnntp_quit() disconnect the NNTP session.
   
   @param session    NNTP session
 */

LIBETPAN_EXPORT
int newsnntp_quit(newsnntp * session);

/*
   newsnntp_head() fetch the headers of an article.
   
   @param session     NNTP session
   @param indx        index of the article to fetch.
   @param result      the header data.
   @param result_len  the length of the result data.
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_head(newsnntp * session, uint32_t indx,
    char ** result, size_t * result_len);

/*
   newsnntp_head_free()
   
   This function will free the data associated with an NNTP
   article header.
   
   @param str    header data.
*/

LIBETPAN_EXPORT
void newsnntp_head_free(char * str);

/*
   newsnntp_article() fetch the header and contents of an article.
   
   @param session     NNTP session
   @param indx        index of the article to fetch.
   @param result      the article data.
   @param result_len  the length of the result data.
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_article(newsnntp * session, uint32_t indx,
    char ** result, size_t * result_len);

/*
   newsnntp_article_by_message_id() fetch the header and contents 
   of an article by message id.
   
   @param session     NNTP session
   @param msg_id      ID of the article to fetch.
   @param result      The article data.
   @param result_len  The length of the result data.
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_article_by_message_id(newsnntp * session, char * msg_id,
    char ** result, size_t * result_len);

/*
   newsnntp_article_free()
   
   This function will free the data associated with an NNTP
   article.
   
   @param str    article data.
*/

LIBETPAN_EXPORT
void newsnntp_article_free(char * str);

/*
   newsnntp_body() fetch the contents of an article.
   
   @param session     NNTP session
   @param indx        index of the article to fetch.
   @param result      body data.
   @param result_len  length of the result data.
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_body(newsnntp * session, uint32_t indx,
    char ** result, size_t * result_len);

/*
   newsnntp_body_free()
   
   This function will free the data associated with an NNTP
   article body.
   
   @param str    article body data.
*/

LIBETPAN_EXPORT
void newsnntp_body_free(char * str);

/*
   newsnntp_mode_reader() switch the mode of the server to
   reader mode.
   
   @param session    NNTP session
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_mode_reader(newsnntp * session);

/*
   newsnntp_date() fetches the current Coordinated Universal
   Time from the server's perspective.
   
   @param session    NNTP session
   @param tm         server's current time.
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_date(newsnntp * session, struct tm * tm);

/*
   newsnntp_authinfo_username() sets the session's username.
   
   @param session    NNTP session
   @param username   username.
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_authinfo_username(newsnntp * session, const char * username);

/*
   newsnntp_authinfo_password() sets the session's password.
   
   @param session    NNTP session
   @param username   password.
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_authinfo_password(newsnntp * session, const char * password);

/*
   newsnntp_post() posts a message to a newsgroup.
   
   @param session    NNTP session
   @param message    the message data.
   @param size       the size of the message.
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_post(newsnntp * session, const char * message, size_t size);



/******************* requests ******************************/

/*
   newsnntp_group() select a newsgroup and fetch info about it.
   
   @param session    NNTP session
   @param groupname  name of the newsgroup.
   @param info       information about the group.
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_group(newsnntp * session, const char * groupname,
		    struct newsnntp_group_info ** info);

/*
   newsnntp_group_free()
   
   This function will free the data associated with NNTP group
   data.
   
   @param info    group info.
*/

LIBETPAN_EXPORT
void newsnntp_group_free(struct newsnntp_group_info * info);

/*
   newsnntp_list() select a newsgroup and fetch info about it.
   
   @param session    NNTP session
   @param result     list of struct newsnntp_group_info *.
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_list(newsnntp * session, clist ** result);

/*
   newsnntp_list_free()
   
   This function will free the data associated with a list
   of newsnntp_group_info *.
   
   @param l    the list of group info.
*/

LIBETPAN_EXPORT
void newsnntp_list_free(clist * l);

/*
   newsnntp_list_overview_fmt() fetch the server's format
   overview.
   
   @param session    NNTP session
   @param result     string describing the server's format
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_list_overview_fmt(newsnntp * session, clist ** result);

/*
   newsnntp_list_overview_fmt_free()
   
   This function will free the data associated with a list
   obtained from newsnntp_list_overview_fmt()
   
   @param l    the list of format data.
*/

LIBETPAN_EXPORT
void newsnntp_list_overview_fmt_free(clist * l);

/*
   newsnntp_list_active() fetch groups matching a wildmat string.
   
   @param session    NNTP session
   @param wildmat    an optional wildmat pattern string
   @param result     a list of struct newsnntp_group_info *
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_list_active(newsnntp * session, const char * wildmat, clist ** result);

/*
   newsnntp_list_active_free()
   
   This function will free the data associated with a list
   obtained from newsnntp_list_active()
   
   @param l    the list of group data.
*/

LIBETPAN_EXPORT
void newsnntp_list_active_free(clist * l);

/*
   newsnntp_list_active_times() fetches when the selected newsgroup
   was created.
   
   @param session    NNTP session
   @param result     a list of struct newsnntp_group_time *
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_list_active_times(newsnntp * session, clist ** result);

/*
   newsnntp_list_active_times_free()
   
   This function will free the data associated with a list
   obtained from newsnntp_list_active_times()
   
   @param l    the list of group time data.
*/

LIBETPAN_EXPORT
void newsnntp_list_active_times_free(clist * l);

/*
   newsnntp_list_distribution() fetches a list of descriptions of
   distributions known to the server.
   
   @param session    NNTP session
   @param result     a list of struct newsnntp_distrib_value_meaning *
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_list_distribution(newsnntp * session, clist ** result);

/*
   newsnntp_list_distribution_free()
   
   This function will free the data associated with a list
   obtained from newsnntp_list_distribution()
   
   @param l    the list of distribution data.
*/

LIBETPAN_EXPORT
void newsnntp_list_distribution_free(clist * l);

/*
   newsnntp_list_distrib_pats() fetches a list of canonical
   distribution values.  Good for figuring out what to put in the
   Distribution header of an article being posted.
   
   @param session    NNTP session
   @param result     a list of struct newsnntp_distrib_default_value *
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_list_distrib_pats(newsnntp * session, clist ** result);

/*
   newsnntp_list_distribution_free()
   
   This function will free the data associated with a list
   obtained from newsnntp_list_distrib_pats()
   
   @param l    the list of distribution data.
*/

LIBETPAN_EXPORT
void newsnntp_list_distrib_pats_free(clist * l);

/*
   newsnntp_list_newsgroups() fetches a list of newsgroups and
   their descriptions.
   
   @param session    NNTP session
   @param pattern    an optional wildmat pattern
   @param result     a list of struct newsnntp_group_description *
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_list_newsgroups(newsnntp * session, const char * pattern,
			      clist ** result);

/*
   newsnntp_list_newsgroups_free()
   
   This function will free the data associated with a list
   obtained from newsnntp_list_newsgroups()
   
   @param l    the list of newsgroup data.
*/

LIBETPAN_EXPORT
void newsnntp_list_newsgroups_free(clist * l);

/*
   newsnntp_list_subscriptions() fetches a default list of
   subscriptions for new users of the server.
   
   @param session    NNTP session
   @param result     a list of newsgroup name strings
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_list_subscriptions(newsnntp * session, clist ** result);

/*
   newsnntp_list_subscriptions_free()
   
   This function will free the data associated with a list of
   subscriptions.
   
   @param l    the list of newsgroup data.
*/

LIBETPAN_EXPORT
void newsnntp_list_subscriptions_free(clist * l);

/*
   newsnntp_listgroup() fetches a list of all article numbers
   for a particular group.
   
   @param session     NNTP session
   @param group_name  the group name
   @param result      a list of uint32_t article numbers
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_listgroup(newsnntp * session, const char * group_name,
		       clist ** result);

/*
   newsnntp_listgroup_free()
   
   This function will free the data associated with a list of
   subscriptions.
   
   @param l    the list of article numbers.
*/

LIBETPAN_EXPORT
void newsnntp_listgroup_free(clist * l);

/*
   newsnntp_xhdr_single() retrieves specific header fields from a
   specific article.
   
   @param session    NNTP session
   @param header     the field name
   @param result     a list of struct newsnntp_xhdr_resp_item *.
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_xhdr_single(newsnntp * session, const char * header, uint32_t article,
			  clist ** result);

/*
   newsnntp_xhdr_range() retrieves specific header fields from
   a range of specific articles.
   
   @param session   NNTP session
   @param header    the field name
   @param rangeinf  the lower bound of the range
   @param rangesup  the upper bound of the range
   @param result    a list of struct newsnntp_xhdr_resp_item *.
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_xhdr_range(newsnntp * session, const char * header,
			 uint32_t rangeinf, uint32_t rangesup,
			 clist ** result);

/*
   newsnntp_xhdr_free()
   
   This function will free the data associated with a list of
   struct newsnntp_xhdr_resp_item *.
   
   @param l    the list of article header responses.
*/

LIBETPAN_EXPORT
void newsnntp_xhdr_free(clist * l);

/*
   newsnntp_xover_single() retrieves overview data for a specific
   article.
   
   @param session    NNTP session
   @param header     the field name
   @param result     a list of struct newsnntp_xhdr_resp_item *.
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_xover_single(newsnntp * session, uint32_t article,
			   struct newsnntp_xover_resp_item ** result);

/*
   newsnntp_xover_range() retrieves overview information for a
   particular range of articles.
   
   @param session   NNTP session
   @param header    the field name
   @param rangeinf  the lower bound of the range
   @param rangesup  the upper bound of the range
   @param result    a list of struct newsnntp_xover_resp_item *.
   
   @return the return code is one of NEWSNNTP_ERROR_XXX or
   NEWSNNTP_NO_ERROR codes
*/

LIBETPAN_EXPORT
int newsnntp_xover_range(newsnntp * session, uint32_t rangeinf, uint32_t rangesup,
			  clist ** result);
void xover_resp_item_free(struct newsnntp_xover_resp_item * n);

/*
   newsnntp_xover_resp_list_free()
   
   This function will free the data associated with a list of
   struct newsnntp_xover_resp_item *.
   
   @param l    the list of overview responses.
*/

LIBETPAN_EXPORT
void newsnntp_xover_resp_list_free(clist * l);

/* deprecated */
LIBETPAN_EXPORT
int newsnntp_authinfo_generic(newsnntp * session, const char * authentificator,
                              const char * arguments);

#ifdef __cplusplus
}
#endif

#endif
