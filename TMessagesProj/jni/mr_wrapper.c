/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *     Copyright (C) 2016 Björn Petersen Software Design and Development
 *                   Contact: r10s@b44t.com, http://b44t.com
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see http://www.gnu.org/licenses/ .
 *
 *******************************************************************************
 *
 * File:    mr_wrapper.c
 * Authors: Björn Petersen
 * Purpose: The C part of the Java<->C Wrapper, see also MrMailbox.java
 *
 ******************************************************************************/
 
 
#include <jni.h>
#include <android/log.h>
#include "../../../messenger-backend/src/mrmailbox.h"


#define CHAR_REF(a) \
	const char* a##Ptr = (a)? (*env)->GetStringUTFChars(env, (a), 0) : NULL; /* passing a NULL-jstring results in a NULL-ptr */

#define CHAR_UNREF(a) \
	if(a) { (*env)->ReleaseStringUTFChars(env, (a), a##Ptr); }

#define JSTRING_NEW(a) \
	(*env)->NewStringUTF(env, a? a : "") /*should handle NULL arguments!*/


/* our log handler */

static void s_log_callback_(int type, const char* msg)
{
	int prio;
	
	switch( type ) {
		case 'd': prio = ANDROID_LOG_DEBUG; break;
		case 'i': prio = ANDROID_LOG_INFO;  break;
		case 'w': prio = ANDROID_LOG_WARN;  break;
		default:  prio = ANDROID_LOG_ERROR; break;
	}
	__android_log_print(prio, "LibreChat", "%s\n", msg); /* on problems, add `-llog` to `Android.mk` */
}


/* globl stuff */

static JNIEnv*   s_env = NULL;
static jclass    s_MrMailbox_class = NULL;
static jmethodID s_MrCallback_methodID = NULL;

static void s_init_globals(JNIEnv *env, jclass MrMailbox_class)
{
	/* make sure, the intialisation is done only once */
	static bool s_global_init_done = 0;
	if( s_global_init_done ) { return; }
	s_global_init_done = 1;
	
	/* init global callback */
	mrlog_set_handler(s_log_callback_);

	/* get a global pointer to the MrMailbox class and to the MrCallback method; note that this may _not_ work from other-than-java-main threads */
	s_env = env;
	s_MrMailbox_class =  (*env)->NewGlobalRef(env, MrMailbox_class);
	s_MrCallback_methodID = (*env)->GetStaticMethodID(env, MrMailbox_class, "MrCallback","(IJJ)J" /*signature as "(param)ret" with I=int, J=long*/ );
	
	/* system-specific backend initialisations */
	mrosnative_init_android(env); /*this should be called before any other "important" routine is called*/
}


/*******************************************************************************
 * MrMailbox
 ******************************************************************************/
 

/* MrMailbox - new/delete */

static uintptr_t s_mailbox_callback_(mrmailbox_t* mailbox, int event, uintptr_t data1, uintptr_t data2)
{
	jlong l;

	if( s_env==NULL || s_MrMailbox_class==NULL || s_MrCallback_methodID==NULL ) {
		s_log_callback_('e', "Callback called but JNI not ready.");
		return 0;
	}

	l = (*s_env)->CallStaticLongMethod(s_env, s_MrMailbox_class, s_MrCallback_methodID, (jint)event, (jlong)data1, (jlong)data2);
	return (uintptr_t)l;
}


JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrMailboxNew(JNIEnv *env, jclass c)
{
	s_init_globals(env, c);
	return (jlong)mrmailbox_new(s_mailbox_callback_, NULL);
}


/* MrMailbox - open/configure/connect/fetch */

JNIEXPORT jint Java_org_telegram_messenger_MrMailbox_MrMailboxOpen(JNIEnv *env, jclass c, jlong hMailbox, jstring dbfile, jstring blobdir)
{
	CHAR_REF(dbfile);
	CHAR_REF(blobdir);
		jint ret = mrmailbox_open((mrmailbox_t*)hMailbox, dbfilePtr, blobdirPtr);
	CHAR_UNREF(blobdir);
	CHAR_UNREF(dbfile)
	return ret;
}


JNIEXPORT void Java_org_telegram_messenger_MrMailbox_MrMailboxClose(JNIEnv *env, jclass c, jlong hMailbox)
{
	mrmailbox_close((mrmailbox_t*)hMailbox);
}


JNIEXPORT jint Java_org_telegram_messenger_MrMailbox_MrMailboxConfigure(JNIEnv *env, jclass c, jlong hMailbox)
{
	return mrmailbox_configure((mrmailbox_t*)hMailbox);
}


JNIEXPORT jint Java_org_telegram_messenger_MrMailbox_MrMailboxIsConfigured(JNIEnv *env, jclass c, jlong hMailbox)
{
	return (jint)mrmailbox_is_configured((mrmailbox_t*)hMailbox);
}


JNIEXPORT jint Java_org_telegram_messenger_MrMailbox_MrMailboxConnect(JNIEnv *env, jclass c, jlong hMailbox)
{
	return mrmailbox_connect((mrmailbox_t*)hMailbox);
}


JNIEXPORT void Java_org_telegram_messenger_MrMailbox_MrMailboxDisconnect(JNIEnv *env, jclass c, jlong hMailbox)
{
	mrmailbox_disconnect((mrmailbox_t*)hMailbox);
}


JNIEXPORT jint Java_org_telegram_messenger_MrMailbox_MrMailboxFetch(JNIEnv *env, jclass c, jlong hMailbox)
{
	return mrmailbox_fetch((mrmailbox_t*)hMailbox);
}


/* MrMailbox - handle contacts */

JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrMailboxGetContactById(JNIEnv *env, jclass c, long hMailbox, jint id)
{
	return (jlong)mrmailbox_get_contact_by_id((mrmailbox_t*)hMailbox, id);
}


/* MrMailbox - handle chats */

JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrMailboxGetChatlist(JNIEnv *env, jclass c, jlong hMailbox)
{
	return (jlong)mrmailbox_get_chatlist((mrmailbox_t*)hMailbox);
}


JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrMailboxGetChatById(JNIEnv *env, jclass c, jlong hMailbox, jint id)
{
	return (jlong)mrmailbox_get_chat_by_id((mrmailbox_t*)hMailbox, id);
}


/* MrMailbox - handle config */

JNIEXPORT jint Java_org_telegram_messenger_MrMailbox_MrMailboxSetConfig(JNIEnv *env, jclass c, jlong hMailbox, jstring key, jstring value)
{
	CHAR_REF(key);
	CHAR_REF(value);
		jint ret = (jlong)mrmailbox_set_config((mrmailbox_t*)hMailbox, keyPtr, valuePtr);
	CHAR_UNREF(key);
	CHAR_UNREF(value);
	return ret;
}


JNIEXPORT jstring Java_org_telegram_messenger_MrMailbox_MrMailboxGetConfig(JNIEnv *env, jclass c, jlong hMailbox, jstring key, jstring def)
{
	CHAR_REF(key);
	CHAR_REF(def);
		char* temp = mrmailbox_get_config((mrmailbox_t*)hMailbox, keyPtr, defPtr);
			jstring ret = JSTRING_NEW(temp);
		free(temp);
	CHAR_UNREF(key);
	CHAR_UNREF(def);
	return ret;
}


/* MrMailbox - misc. */

JNIEXPORT jstring Java_org_telegram_messenger_MrMailbox_MrMailboxGetInfo(JNIEnv *env, jclass c, jlong hMailbox)
{
	char* temp = mrmailbox_get_info((mrmailbox_t*)hMailbox);
		jstring ret = JSTRING_NEW(temp);
	free(temp);
	return ret;
}


JNIEXPORT jstring Java_org_telegram_messenger_MrMailbox_MrMailboxExecute(JNIEnv *env, jclass c, jlong hMailbox, jstring cmd)
{
	CHAR_REF(cmd);
		char* temp = mrmailbox_execute((mrmailbox_t*)hMailbox, cmdPtr);
			jstring ret = JSTRING_NEW(temp);
		free(temp);
	CHAR_UNREF(cmd);
	return ret;
}


/*******************************************************************************
 * MrChatlist
 ******************************************************************************/


JNIEXPORT void Java_org_telegram_messenger_MrMailbox_MrChatlistUnref(JNIEnv *env, jclass c, jlong hChatlist)
{
	mrchatlist_unref((mrchatlist_t*)hChatlist);
}


JNIEXPORT jint Java_org_telegram_messenger_MrMailbox_MrChatlistGetCnt(JNIEnv *env, jclass c, jlong hChatlist)
{
	return mrchatlist_get_cnt((mrchatlist_t*)hChatlist);
}


JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrChatlistGetChatByIndex(JNIEnv *env, jclass c, jlong hChatlist, jint index)
{
	return (jlong)mrchatlist_get_chat_by_index((mrchatlist_t*)hChatlist, index);
}


/*******************************************************************************
 * MrChat
 ******************************************************************************/


JNIEXPORT void Java_org_telegram_messenger_MrMailbox_MrChatUnref(JNIEnv *env, jclass c, jlong hChat)
{
	mrchat_unref((mrchat_t*)hChat);
}


JNIEXPORT jint Java_org_telegram_messenger_MrMailbox_MrChatGetId(JNIEnv *env, jclass c, jlong hChat)
{
	mrchat_t* ths = (mrchat_t*)hChat; if( ths == NULL ) { return 0; }
	return ths->m_id;
}


JNIEXPORT jint Java_org_telegram_messenger_MrMailbox_MrChatGetType(JNIEnv *env, jclass c, jlong hChat)
{
	mrchat_t* ths = (mrchat_t*)hChat; if( ths == NULL ) { return 0; }
	return ths->m_type;
}


JNIEXPORT jstring Java_org_telegram_messenger_MrMailbox_MrChatGetName(JNIEnv *env, jclass c, jlong hChat)
{
	mrchat_t* ths = (mrchat_t*)hChat; if( ths == NULL ) { return JSTRING_NEW(NULL); }
	return JSTRING_NEW(ths->m_name);
}


JNIEXPORT jstring Java_org_telegram_messenger_MrMailbox_MrChatGetSubtitle(JNIEnv *env, jclass c, jlong hChat)
{
	const char* temp = mrchat_get_subtitle((mrchat_t*)hChat); /* mrchat_get_subtitle() checks for nullpointers */
		jstring ret = JSTRING_NEW(temp);
	free(temp);
	return ret;
}


JNIEXPORT jint Java_org_telegram_messenger_MrMailbox_MrChatGetUnreadCount(JNIEnv *env, jclass c, jlong hChat)
{
	return mrchat_get_unread_count((mrchat_t*)hChat); /* mrchat_get_unread_count() checks for nullpointers */
}


JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrChatGetSummary(JNIEnv *env, jclass c, jlong hChat)
{
	return (jlong)mrchat_get_summary((mrchat_t*)hChat);
}


JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrChatGetMsglist(JNIEnv *env, jclass c, jlong hChat, jint offset, jint amount)
{
	return (jlong)mrchat_get_msglist((mrchat_t*)hChat, offset, amount);
}


/*******************************************************************************
 * MrMsglist
 ******************************************************************************/


JNIEXPORT void Java_org_telegram_messenger_MrMailbox_MrMsglistUnref(JNIEnv *env, jclass c, jlong hMsglist)
{
	mrmsglist_unref((mrmsglist_t*)hMsglist); /* mrmsglist_unref() checks for nullpointers */
}


JNIEXPORT jint Java_org_telegram_messenger_MrMailbox_MrMsglistGetCnt(JNIEnv *env, jclass c, jlong hMsglist)
{
	return mrmsglist_get_cnt((mrmsglist_t*)hMsglist);
}


JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrMsglistGetMsgByIndex(JNIEnv *env, jclass c, jlong hMsglist, jint index)
{
	return (jlong)mrmsglist_get_msg_by_index((mrmsglist_t*)hMsglist, index);
}


/*******************************************************************************
 * MrMsg
 ******************************************************************************/


JNIEXPORT void Java_org_telegram_messenger_MrMailbox_MrMsgUnref(JNIEnv *env, jclass c, jlong hMsg)
{
	mrmsg_unref((mrmsg_t*)hMsg);
}


JNIEXPORT jstring Java_org_telegram_messenger_MrMailbox_MrMsgGetText(JNIEnv *env, jclass c, jlong hMsg)
{
	mrmsg_t* ths = (mrmsg_t*)hMsg; if( ths == NULL ) { return JSTRING_NEW(NULL); }
	return JSTRING_NEW(ths->m_text);
}


JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrMsgGetTimestamp(JNIEnv *env, jclass c, jlong hMsg)
{
	mrmsg_t* ths = (mrmsg_t*)hMsg; if( ths == NULL ) { return 0; }
	return ths->m_timestamp;
}


JNIEXPORT jint Java_org_telegram_messenger_MrMailbox_MrMsgGetType(JNIEnv *env, jclass c, jlong hMsg)
{
	mrmsg_t* ths = (mrmsg_t*)hMsg; if( ths == NULL ) { return 0; }
	return ths->m_type;
}


JNIEXPORT jint Java_org_telegram_messenger_MrMailbox_MrMsgGetState(JNIEnv *env, jclass c, jlong hMsg)
{
	mrmsg_t* ths = (mrmsg_t*)hMsg; if( ths == NULL ) { return 0; }
	return ths->m_state;
}


JNIEXPORT jint Java_org_telegram_messenger_MrMailbox_MrMsgGetFromId(JNIEnv *env, jclass c, jlong hMsg)
{
	mrmsg_t* ths = (mrmsg_t*)hMsg; if( ths == NULL ) { return 0; }
	return ths->m_from_id;
}


/*******************************************************************************
 * MrPoortext
 ******************************************************************************/


JNIEXPORT void Java_org_telegram_messenger_MrMailbox_MrPoortextUnref(JNIEnv *env, jclass c, jlong hPoortext)
{
	mrpoortext_unref((mrpoortext_t*)hPoortext);
}


JNIEXPORT jstring Java_org_telegram_messenger_MrMailbox_MrPoortextGetTitle(JNIEnv *env, jclass c, jlong hPoortext)
{
	mrpoortext_t* ths = (mrpoortext_t*)hPoortext; if( ths == NULL ) { return JSTRING_NEW(NULL); }
	return JSTRING_NEW(ths->m_title);
}


JNIEXPORT jint Java_org_telegram_messenger_MrMailbox_MrPoortextGetTitleMeaning(JNIEnv *env, jclass c, jlong hPoortext)
{
	mrpoortext_t* ths = (mrpoortext_t*)hPoortext; if( ths == NULL ) { return 0; }
	return ths->m_title_meaning;
}


JNIEXPORT jstring Java_org_telegram_messenger_MrMailbox_MrPoortextGetText(JNIEnv *env, jclass c, jlong hPoortext)
{
	mrpoortext_t* ths = (mrpoortext_t*)hPoortext; if( ths == NULL ) { return JSTRING_NEW(NULL); }
	return JSTRING_NEW(ths->m_text);
}


JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrPoortextGetTimestamp(JNIEnv *env, jclass c, jlong hPoortext)
{
	mrpoortext_t* ths = (mrpoortext_t*)hPoortext; if( ths == NULL ) { return 0; }
	return ths->m_timestamp;
}


JNIEXPORT jint Java_org_telegram_messenger_MrMailbox_MrPoortextGetState(JNIEnv *env, jclass c, jlong hPoortext)
{
	mrpoortext_t* ths = (mrpoortext_t*)hPoortext; if( ths == NULL ) { return 0; }
	return ths->m_state;
}


/*******************************************************************************
 * Tools
 ******************************************************************************/


JNIEXPORT void Java_org_telegram_messenger_MrMailbox_MrStockAddStr(JNIEnv* env, jclass c, jint id, jstring str)
{
	CHAR_REF(str);
		mrstock_add_str(id, strPtr);
	CHAR_UNREF(str)
}


JNIEXPORT jstring Java_org_telegram_messenger_MrMailbox_MrGetVersionStr(JNIEnv *env, jclass c)
{
	s_init_globals(env, c);
	const char* temp = mr_get_version_str();
		jstring ret = JSTRING_NEW(temp);
	free(temp);
	return ret;
}


