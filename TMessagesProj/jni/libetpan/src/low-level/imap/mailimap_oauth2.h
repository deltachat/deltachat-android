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
#ifndef MAILIMAP_OAUTH2_H

#define MAILIMAP_OAUTH2_H

#include <libetpan/mailimap_types.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
   mailimap_oauth2_authenticate()
   Authenticates the client using using an oauth2 token.
   To gather a deeper understanding of the OAuth2 aunthentication
   process refer to: https://developers.google.com/gmail/xoauth2_protocol
   For a quick start you may follow this brief set of steps:
   1. Set up a profile for your app in the Google 
      API Console: https://code.google.com/apis/console
   2. With your recently obtained client_id and secret 
      load the following URL (everything goes ina single line):
      https://accounts.google.com/o/oauth2/auth?client_id=[YOUR_CLIENT_ID]&
          redirect_uri=urn%3Aietf%3Awg%3Aoauth%3A2.0%3Aoob&
          response_type=code&scope=https%3A%2F%2Fmail.google.com%2F%20email&
          &access_type=offline
   3. The user most follow instructions to authorize application access
      to Gmail.
   4. After the user hits the "Accept" button it will be redirected to another
      page where the access token will be issued.
   5. Now from the app we need and authorization token, to get one we issue a POST request
      the following URL: https://accounts.google.com/o/oauth2/token using these parameters:
      client_id: This is the client id we got from step 1
      client_secret: Client secret as we got it from step 1
      code: This is the code we received in step 4
      redirect_uri: This is a redirect URI where the access token will be sent, for non 
       web applications this is usually urn:ietf:wg:oauth:2.0:oob (as we got from step 1)
      grant_type: Always use the authorization_code parameter to retrieve an access and refresh tokens
   6. After step 5 completes we receive a JSON object similar to:
       {
         "access_token":"1/fFAGRNJru1FTz70BzhT3Zg",
         "refresh_token":"1/fFAGRNJrufoiWEGIWEFJFJF",
         "expires_in":3920,
         "token_type":"Bearer"
       }
      The above output gives us the access_token, now we need to also retrieve the user's e-mail, 
      to do that we need to perform an HTTP GET request to Google's UserInfo API using this URL:
       https://www.googleapis.com/oauth2/v1/userinfo?access_token=[YOUR_ACCESS_TOKEN]
      this will return the following JSON output:
       {
        "id": "00000000000002222220000000",
        "email": "email@example.com",
        "verified_email": true
       }
   @param session       IMAP session
   @param auth_user     Authentication user (tipically an e-mail address, depends on server)
   @param access_token  OAuth2 access token
   @return the return code is one of MAILIMAP_ERROR_XXX or
   MAILIMAP_NO_ERROR codes
  */

LIBETPAN_EXPORT
int mailimap_oauth2_authenticate(mailimap * session, const char * auth_user,
    const char * access_token);

LIBETPAN_EXPORT
int mailimap_has_xoauth2(mailimap * session);
  
#ifdef __cplusplus
}
#endif

#endif
		
