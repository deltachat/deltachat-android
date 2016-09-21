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
	return (jlong)mrmailbox_new();
}


/* MrMailbox - open/close/connect/fetch */

JNIEXPORT int Java_org_telegram_messenger_MrMailbox_MrMailboxOpen(JNIEnv *env, jclass c, jlong hMailbox, jstring dbfile)
{
	CHAR_REF(dbfile);
	if( dbfilePtr == NULL ) { return 0; }
		int ret = mrmailbox_open((mrmailbox_t*)hMailbox, dbfilePtr);
	CHAR_UNREF(dbfile)
	return ret;
}


JNIEXPORT void Java_org_telegram_messenger_MrMailbox_MrMailboxClose(JNIEnv *env, jclass c, jlong hMailbox)
{
	mrmailbox_close((mrmailbox_t*)hMailbox);
}


JNIEXPORT int Java_org_telegram_messenger_MrMailbox_MrMailboxConnect(JNIEnv *env, jclass c, jlong hMailbox)
{
	return mrmailbox_connect((mrmailbox_t*)hMailbox);
}


JNIEXPORT void Java_org_telegram_messenger_MrMailbox_MrMailboxDisconnect(JNIEnv *env, jclass c, jlong hMailbox)
{
	mrmailbox_disconnect((mrmailbox_t*)hMailbox);
}


JNIEXPORT int Java_org_telegram_messenger_MrMailbox_MrMailboxFetch(JNIEnv *env, jclass c, jlong hMailbox)
{
	return mrmailbox_fetch((mrmailbox_t*)hMailbox);
}


/* MrMailbox - handle contacts */

JNIEXPORT int Java_org_telegram_messenger_MrMailbox_MrMailboxGetContactCnt(JNIEnv *env, jclass c, jlong hMailbox)
{
	return (int)mrmailbox_get_contact_cnt((mrmailbox_t*)hMailbox);
}


JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrMailboxGetContactByIndex(JNIEnv *env, jclass c, long hMailbox, int i)
{
	return (jlong)mrmailbox_get_contact_by_index((mrmailbox_t*)hMailbox, i);
}


/* MrMailbox - handle chats */

JNIEXPORT int Java_org_telegram_messenger_MrMailbox_MrMailboxGetChatCnt(JNIEnv *env, jclass c, jlong hMailbox)
{
	return (int)mrmailbox_get_chat_cnt((mrmailbox_t*)hMailbox);
}


JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrMailboxGetChats(JNIEnv *env, jclass c, jlong hMailbox)
{
	return (jlong)mrmailbox_get_chats((mrmailbox_t*)hMailbox);
}


/* MrMailbox - handle config */

JNIEXPORT int Java_org_telegram_messenger_MrMailbox_MrMailboxSetConfig(JNIEnv *env, jclass c, jlong hMailbox, jstring key, jstring value)
{
	CHAR_REF(key);
	CHAR_REF(value);
		int ret = (jlong)mrmailbox_set_config((mrmailbox_t*)hMailbox, keyPtr, valuePtr);
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


JNIEXPORT int Java_org_telegram_messenger_MrMailbox_MrChatlistGetCnt(JNIEnv *env, jclass c, jlong hChatlist)
{
	return mrchatlist_get_cnt((mrchatlist_t*)hChatlist);
}


JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrChatlistGetChat(JNIEnv *env, jclass c, jlong hChatlist, int index)
{
	return (jlong)mrchatlist_get_chat((mrchatlist_t*)hChatlist, index);
}


/*******************************************************************************
 * MrChat
 ******************************************************************************/


JNIEXPORT int Java_org_telegram_messenger_MrMailbox_MrChatGetId(JNIEnv *env, jclass c, jlong hChat)
{
	mrchat_t* ths = (mrchat_t*)hChat;
	return ths->m_id;
}


JNIEXPORT int Java_org_telegram_messenger_MrMailbox_MrChatGetType(JNIEnv *env, jclass c, jlong hChat)
{
	mrchat_t* ths = (mrchat_t*)hChat;
	return ths->m_type;
}


JNIEXPORT jstring Java_org_telegram_messenger_MrMailbox_MrChatGetName(JNIEnv *env, jclass c, jlong hChat)
{
	mrchat_t* ths = (mrchat_t*)hChat;
	return JSTRING_NEW(ths->m_name);
}


JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrChatGetLastMsg(JNIEnv *env, jclass c, jlong hChat)
{
	mrchat_t* ths = (mrchat_t*)hChat;
	return mrchat_get_last_msg(ths);
}


/*******************************************************************************
 * Tools
 ******************************************************************************/


JNIEXPORT jstring Java_org_telegram_messenger_MrMailbox_MrGetVersionStr(JNIEnv *env, jclass c)
{
	const char* temp = mr_get_version_str();
		jstring ret = JSTRING_NEW(temp);
	free(temp);
	return ret;
}

