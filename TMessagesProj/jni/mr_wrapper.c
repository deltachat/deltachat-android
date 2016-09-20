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


/*******************************************************************************
 * MrMailbox
 ******************************************************************************/
 

JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrMailboxNew(JNIEnv *env, jclass c)
{
	return (jlong)mrmailbox_new();
}


JNIEXPORT jlong Java_org_telegram_messenger_MrMailbox_MrMailboxUnref(JNIEnv *env, jclass c, jlong hMailbox)
{
	mrmailbox_unref((mrmailbox_t*)hMailbox);
}


JNIEXPORT int Java_org_telegram_messenger_MrMailbox_MrMailboxOpen(JNIEnv *env, jclass c, jlong hMailbox, jstring dbfile)
{
	const char* dbfilePtr = (*env)->GetStringUTFChars(env, dbfile, 0);
	if( dbfilePtr == NULL ) { return 0; }
		int ret = mrmailbox_open((mrmailbox_t*)hMailbox, dbfilePtr);
	(*env)->ReleaseStringUTFChars(env, dbfile, dbfilePtr);
	return ret;
}


JNIEXPORT void Java_org_telegram_messenger_MrMailbox_MrMailboxClose(JNIEnv *env, jclass c, jlong hMailbox)
{
	mrmailbox_close((mrmailbox_t*)hMailbox);
}


/*******************************************************************************
 * MrChat
 ******************************************************************************/


JNIEXPORT jstring Java_org_telegram_messenger_MrMailbox_MrChatGetName(JNIEnv *env, jclass c, jlong hChat)
{
	return (*env)->NewStringUTF(env, "foobar test");
}


/*******************************************************************************
 * Tools
 ******************************************************************************/


JNIEXPORT jstring Java_org_telegram_messenger_MrMailbox_MrGetVersionStr(JNIEnv *env, jclass c)
{
	char* temp = mr_get_version_str();
	jstring ret = (*env)->NewStringUTF(env, temp);
	free(temp);
	return ret;
}

