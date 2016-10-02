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
#include "../../../messenger-backend/src/mrmailbox.h"


#define CHAR_REF(a) \
	const char* a##Ptr = (*env)->GetStringUTFChars(env, (a), 0); \
	if( a##Ptr == NULL ) { return 0; }

#define CHAR_UNREF(a) \
	(*env)->ReleaseStringUTFChars(env, (a), a##Ptr);

#define JSTRING_NEW(a) \
	(*env)->NewStringUTF(env, a? a : "") /*should handle NULL arguments!*/


/*******************************************************************************
 * MrMailbox
 ******************************************************************************/
 

/* MrMailbox - new/delete */

JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrMailboxNew(JNIEnv *env, jclass c)
{
	mrosnative_init_android(env); /*this should be called before any other "important" routine is called*/
	return (jlong)mrmailbox_new();
}


/* MrMailbox - open/close/connect/fetch */

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

JNIEXPORT jint Java_org_telegram_messenger_MrMailbox_MrMailboxGetContactCnt(JNIEnv *env, jclass c, jlong hMailbox)
{
	return (jint)mrmailbox_get_contact_cnt((mrmailbox_t*)hMailbox);
}


JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrMailboxGetContactByIndex(JNIEnv *env, jclass c, long hMailbox, jint i)
{
	return (jlong)mrmailbox_get_contact_by_index((mrmailbox_t*)hMailbox, i);
}


/* MrMailbox - handle chats */

JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrMailboxGetChats(JNIEnv *env, jclass c, jlong hMailbox)
{
	return (jlong)mrmailbox_get_chats((mrmailbox_t*)hMailbox);
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
		const char* temp = (jlong)mrmailbox_get_config((mrmailbox_t*)hMailbox, keyPtr, def);
			jstring ret = JSTRING_NEW(temp);
		free(temp);
	CHAR_UNREF(key);
	CHAR_UNREF(def);
	return ret;
}


/*******************************************************************************
 * MrChatlist
 ******************************************************************************/


JNIEXPORT void Java_org_telegram_messenger_MrMailbox_MrChatlistUnref(JNIEnv *env, jclass c, jlong hChatlist)
{
	return mrchatlist_unref((mrchatlist_t*)hChatlist);
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
	return mrchat_unref((mrchat_t*)hChat);
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


JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrChatGetLastSummary(JNIEnv *env, jclass c, jlong hChat)
{
	return (jlong)mrchat_get_last_summary((mrchat_t*)hChat);
}


JNIEXPORT jint Java_org_telegram_messenger_MrMailbox_MrChatGetLastState(JNIEnv *env, jclass c, jlong hChat)
{
	return mrchat_get_last_state((mrchat_t*)hChat);
}


JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrChatGetLastTimestamp(JNIEnv *env, jclass c, jlong hChat)
{
	return (jlong)mrchat_get_last_timestamp((mrchat_t*)hChat); /* mrchat_get_last_timestamp() checks for nullpointers */
}


JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrChatGetMsgs(JNIEnv *env, jclass c, jlong hChat, jint offset, jint amount)
{
	return (jlong)mrchat_get_msgs((mrchat_t*)hChat, offset, amount);
}


/*******************************************************************************
 * MrMsglist
 ******************************************************************************/


JNIEXPORT void Java_org_telegram_messenger_MrMailbox_MrMsglistUnref(JNIEnv *env, jclass c, jlong hMsglist)
{
	return mrmsglist_unref((mrmsglist_t*)hMsglist); /* mrmsglist_unref() checks for nullpointers */
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
	return mrmsg_unref((mrmsg_t*)hMsg);
}


/*******************************************************************************
 * MrPoortext
 ******************************************************************************/


JNIEXPORT void Java_org_telegram_messenger_MrMailbox_MrPoortextUnref(JNIEnv *env, jclass c, jlong hPoortext)
{
	return mrpoortext_unref((mrpoortext_t*)hPoortext);
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
	mrosnative_init_android(env); /*this should be called before any other "important" routine is called*/
	const char* temp = mr_get_version_str();
		jstring ret = JSTRING_NEW(temp);
	free(temp);
	return ret;
}


