/* windlopen.c--Windows dynamic loader interface
 * Ryan Troll
 * $Id: windlopen.c,v 1.17 2009/01/25 20:20:57 mel Exp $
 */
/* 
 * Copyright (c) 1998-2003 Carnegie Mellon University.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The name "Carnegie Mellon University" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For permission or any other legal
 *    details, please contact  
 *      Office of Technology Transfer
 *      Carnegie Mellon University
 *      5000 Forbes Avenue
 *      Pittsburgh, PA  15213-3890
 *      (412) 268-4387, fax: (412) 268-7395
 *      tech-transfer@andrew.cmu.edu
 *
 * 4. Redistributions of any form whatsoever must retain the following
 *    acknowledgment:
 *    "This product includes software developed by Computing Services
 *     at Carnegie Mellon University (http://www.cmu.edu/computing/)."
 *
 * CARNEGIE MELLON UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO
 * THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS, IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY BE LIABLE
 * FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN
 * AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING
 * OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

#include <stdio.h>
#include <io.h>
#include <sys/stat.h>

#include <config.h>
#include <sasl.h>
#include "saslint.h"

#define DLL_SUFFIX	".dll"
#define DLL_MASK	"*" DLL_SUFFIX
#define DLL_MASK_LEN	5

const int _is_sasl_server_static = 0;

/* : inefficient representation, but works */
typedef struct lib_list 
{
    struct lib_list *next;
    HMODULE library;
} lib_list_t;

static lib_list_t *lib_list_head = NULL;

int _sasl_locate_entry(void *library,
		       const char *entryname,
		       void **entry_point) 
{
    if(entryname == NULL) {
	_sasl_log(NULL, SASL_LOG_ERR,
		  "no entryname in _sasl_locate_entry");
	return SASL_BADPARAM;
    }

    if(library == NULL) {
	_sasl_log(NULL, SASL_LOG_ERR,
		  "no library in _sasl_locate_entry");
	return SASL_BADPARAM;
    }

    if(entry_point == NULL) {
	_sasl_log(NULL, SASL_LOG_ERR,
		  "no entrypoint output pointer in _sasl_locate_entry");
	return SASL_BADPARAM;
    }

    *entry_point = GetProcAddress(library, entryname);

    if (*entry_point == NULL) {
#if 0 /* This message appears to confuse people */
	_sasl_log(NULL, SASL_LOG_DEBUG,
		  "unable to get entry point %s: %s", entryname,
		  GetLastError());
#endif
	return SASL_FAIL;
    }

    return SASL_OK;
}

static int _sasl_plugin_load(char *plugin, void *library,
			     const char *entryname,
			     int (*add_plugin)(const char *, void *)) 
{
    void *entry_point;
    int result;
    
    result = _sasl_locate_entry(library, entryname, &entry_point);
    if(result == SASL_OK) {
	result = add_plugin(plugin, entry_point);
	if(result != SASL_OK)
	    _sasl_log(NULL, SASL_LOG_DEBUG,
		      "_sasl_plugin_load failed on %s for plugin: %s\n",
		      entryname, plugin);
    }

    return result;
}

/* loads a plugin library */
int _sasl_get_plugin(const char *file,
		     const sasl_callback_t *verifyfile_cb,
		     void **libraryptr)
{
    int r = 0;
    HINSTANCE library;
    lib_list_t *newhead;
    
    r = ((sasl_verifyfile_t *)(verifyfile_cb->proc))
		    (verifyfile_cb->context, file, SASL_VRFY_PLUGIN);
    if (r != SASL_OK) return r;

    newhead = sasl_ALLOC(sizeof(lib_list_t));
    if (!newhead) return SASL_NOMEM;

    if (!(library = LoadLibrary (file))) {
	_sasl_log(NULL, SASL_LOG_ERR,
		  "unable to LoadLibrary %s: %s", file, GetLastError());
	sasl_FREE(newhead);
	return SASL_FAIL;
    }

    newhead->library = library;
    newhead->next = lib_list_head;
    lib_list_head = newhead;

    *libraryptr = library;
    return SASL_OK;
}

/* undoes actions done by _sasl_get_plugin */
void _sasl_remove_last_plugin()
{
    lib_list_t *last_plugin = lib_list_head;
    lib_list_head = lib_list_head->next;
    if (last_plugin->library) {
	FreeLibrary(last_plugin->library);
    }
    sasl_FREE(last_plugin);
}

/* gets the list of mechanisms */
int _sasl_load_plugins(const add_plugin_list_t *entrypoints,
		       const sasl_callback_t *getpath_cb,
		       const sasl_callback_t *verifyfile_cb)
{
    int result;
    char cur_dir[PATH_MAX], full_name[PATH_MAX+2], prefix[PATH_MAX+2];
				/* 1 for '\\' 1 for trailing '\0' */
    char * pattern;
    char c;
    int pos;
    const char *path=NULL;
    int position;
    const add_plugin_list_t *cur_ep;
    struct stat statbuf;		/* filesystem entry information */
    intptr_t fhandle;			/* file handle for _findnext function */
    struct _finddata_t finddata;	/* data returned by _findnext() */
    size_t prefix_len;

    if (! entrypoints
	|| ! getpath_cb
	|| getpath_cb->id != SASL_CB_GETPATH
	|| ! getpath_cb->proc
	|| ! verifyfile_cb
	|| verifyfile_cb->id != SASL_CB_VERIFYFILE
	|| ! verifyfile_cb->proc)
	return SASL_BADPARAM;

    /* get the path to the plugins */
    result = ((sasl_getpath_t *)(getpath_cb->proc))(getpath_cb->context,
						    &path);
    if (result != SASL_OK) return result;
    if (! path) return SASL_FAIL;

    if (strlen(path) >= PATH_MAX) { /* no you can't buffer overrun */
	return SASL_FAIL;
    }

    position=0;
    do {
	pos=0;
	do {
	    c=path[position];
	    position++;
	    cur_dir[pos]=c;
	    pos++;
	} while ((c!=PATHS_DELIMITER) && (c!=0));
	cur_dir[pos-1]='\0';


/* : check to make sure that a valid directory name was passed in */
	if (stat (cur_dir, &statbuf) < 0) {
	    continue;
	}
	if ((statbuf.st_mode & S_IFDIR) == 0) {
	    continue;
	}

	strcpy (prefix, cur_dir);
	prefix_len = strlen (prefix);

/* : Don't append trailing \ unless required */
	if (prefix[prefix_len-1] != '\\') {
	    strcat (prefix,"\\");
	    prefix_len++;
	}

	pattern = prefix;

/* : Check that we have enough space for "*.dll" */
	if ((prefix_len + DLL_MASK_LEN) > (sizeof(prefix) - 1)) {
	    _sasl_log(NULL, SASL_LOG_WARN, "plugin search mask is too big");
            continue;
	}

	strcat (prefix + prefix_len, "*" DLL_SUFFIX);

        fhandle = _findfirst (pattern, &finddata);
        if (fhandle == -1) {	/* no matching files */
            continue;
        }

/* : Truncate "*.dll" */
	prefix[prefix_len] = '\0';

	do {
	    size_t length;
	    void *library;
	    char *c;
	    char plugname[PATH_MAX];
	    int entries;

	    length = strlen(finddata.name);
	    if (length < 5) { /* At least <Ch>.dll */
		continue; /* can not possibly be what we're looking for */
	    }

/* : Check for overflow */
	    if (length + prefix_len >= PATH_MAX) continue; /* too big */

	    if (stricmp(finddata.name + (length - strlen(DLL_SUFFIX)), DLL_SUFFIX) != 0) {
		continue;
	    }

/* : Check that it is not a directory */
	    if ((finddata.attrib & _A_SUBDIR) == _A_SUBDIR) {
		continue;
	    }

/* : Construct full name from prefix and name */

	    strcpy (full_name, prefix);
	    strcat (full_name, finddata.name);
		
/* cut off .dll suffix -- this only need be approximate */
	    strcpy (plugname, finddata.name);
	    c = strrchr(plugname, '.');
	    if (c != NULL) *c = '\0';

	    result = _sasl_get_plugin (full_name, verifyfile_cb, &library);

	    if (result != SASL_OK) {
		continue;
	    }

	    entries = 0;
	    for (cur_ep = entrypoints; cur_ep->entryname; cur_ep++) {
		result = _sasl_plugin_load(plugname,
					   library,
					   cur_ep->entryname,
					   cur_ep->add_plugin);
		if (result == SASL_OK) {
		    ++entries;
		}
		/* If this fails, it's not the end of the world */
	    }
	    if (entries == 0) {
		_sasl_remove_last_plugin();
	    }

	} while (_findnext (fhandle, &finddata) == 0);
	
	_findclose (fhandle);

    } while ((c!='=') && (c!=0));

    return SASL_OK;
}

int
_sasl_done_with_plugins(void)
{
    lib_list_t *libptr, *libptr_next;
    
    for(libptr = lib_list_head; libptr; libptr = libptr_next) {
	libptr_next = libptr->next;
	if (libptr->library != NULL) {
	    FreeLibrary(libptr->library);
	}
	sasl_FREE(libptr);
    }

    lib_list_head = NULL;

    return SASL_OK;
}
