/*******************************************************************************
 *
 *                           Delta Chat Java Adapter
 *                           (C) 2017 Björn Petersen
 *                    Contact: r10s@b44t.com, http://b44t.com
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
 ******************************************************************************/


// Purpose: The C part of the Java<->C Wrapper, see also DcContext.java


#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include "deltachat-core-rust/deltachat-ffi/deltachat.h"


static dc_msg_t* get_dc_msg(JNIEnv *env, jobject obj);


// passing a NULL-jstring results in a NULL-ptr - this is needed by functions using eg. NULL for "delete"
#define CHAR_REF(a) \
	char* a##Ptr = char_ref__(env, (a));
static char* char_ref__(JNIEnv* env, jstring a) {
    if (a==NULL) {
        return NULL;
    }

    /* we do not use the JNI functions GetStringUTFChars()/ReleaseStringUTFChars()
    as they do not work on some older systems for code points >0xffff, eg. emojos.
    as a workaround, we're calling back to java-land's String.getBytes() which works as expected */
    static jclass    s_strCls    = NULL;
    static jmethodID s_getBytes  = NULL;
    static jclass    s_strEncode = NULL;
    if (s_getBytes==NULL) {
        s_strCls    = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/String"));
        s_getBytes  = (*env)->GetMethodID(env, s_strCls, "getBytes", "(Ljava/lang/String;)[B");
        s_strEncode = (*env)->NewGlobalRef(env, (*env)->NewStringUTF(env, "UTF-8"));
    }

    const jbyteArray stringJbytes = (jbyteArray)(*env)->CallObjectMethod(env, a, s_getBytes, s_strEncode);
    const jsize length = (*env)->GetArrayLength(env, stringJbytes);
    jbyte* pBytes = (*env)->GetByteArrayElements(env, stringJbytes, NULL);
    if (pBytes==NULL) {
        return NULL;
    }

    char* cstr = strndup((const char*)pBytes, length);

    (*env)->ReleaseByteArrayElements(env, stringJbytes, pBytes, JNI_ABORT);
    (*env)->DeleteLocalRef(env, stringJbytes);

    return cstr;
}

#define CHAR_UNREF(a) \
	free(a##Ptr);

#define JSTRING_NEW(a) jstring_new__(env, (a))
static jstring jstring_new__(JNIEnv* env, const char* a)
{
	if (a==NULL || a[0]==0) {
		return (*env)->NewStringUTF(env, "");
	}

	/* for non-empty strings, do not use NewStringUTF() as this is buggy on some Android versions.
	Instead, create the string using `new String(ByteArray, "UTF-8);` which seems to be programmed more properly.
	(eg. on KitKat a simple "SMILING FACE WITH SMILING EYES" (U+1F60A, UTF-8 F0 9F 98 8A) will let the app crash, reporting 0xF0 is a bad UTF-8 start,
	see http://stackoverflow.com/questions/12127817/android-ics-4-0-ndk-newstringutf-is-crashing-down-the-app ) */
	static jclass    s_strCls    = NULL;
	static jmethodID s_strCtor   = NULL;
	static jclass    s_strEncode = NULL;
	if (s_strCtor==NULL) {
		s_strCls    = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/String"));
		s_strCtor   = (*env)->GetMethodID(env, s_strCls, "<init>", "([BLjava/lang/String;)V");
		s_strEncode = (*env)->NewGlobalRef(env, (*env)->NewStringUTF(env, "UTF-8"));
	}

	int a_bytes = strlen(a);
	jbyteArray array = (*env)->NewByteArray(env, a_bytes);
		(*env)->SetByteArrayRegion(env, array, 0, a_bytes, (const jbyte*)a);
		jstring ret = (jstring) (*env)->NewObject(env, s_strCls, s_strCtor, array, s_strEncode);
	(*env)->DeleteLocalRef(env, array); /* we have to delete the reference as it is not returned to Java, AFAIK */

	return ret;
}


// convert c-timestamp to java-timestamp
#define JTIMESTAMP(a) (((jlong)a)*((jlong)1000))


// convert java-timestamp to c-timestamp
#define CTIMESTAMP(a) (((jlong)a)/((jlong)1000))


static jintArray dc_array2jintArray_n_unref(JNIEnv *env, dc_array_t* ca)
{
	/* takes a C-array of type dc_array_t and converts it it a Java-Array.
	then the C-array is freed and the Java-Array is returned. */
	int i, icnt = ca? dc_array_get_cnt(ca) : 0;
	jintArray ret = (*env)->NewIntArray(env, icnt); if (ret==NULL) { return NULL; }

	if (ca) {
		if (icnt) {
			const uint32_t* ca_data = dc_array_get_raw(ca);
			if (sizeof(uint32_t)==sizeof(jint)) {
				(*env)->SetIntArrayRegion(env, ret, 0, icnt, (jint*)ca_data);
			}
			else {
				jint* temp = calloc(icnt, sizeof(jint));
					for (i = 0; i < icnt; i++) {
						temp[i] = (jint)ca_data[i];
					}
					(*env)->SetIntArrayRegion(env, ret, 0, icnt, temp);
				free(temp);
			}
		}
		dc_array_unref(ca);
	}

	return ret;
}


static uint32_t* jintArray2uint32Pointer(JNIEnv* env, jintArray ja, int* ret_icnt)
{
	/* takes a Java-Array and converts it to a C-Array. */
	uint32_t* ret = NULL;
	if (ret_icnt) { *ret_icnt = 0; }

	if (env && ja && ret_icnt)
	{
		int i, icnt  = (*env)->GetArrayLength(env, ja);
		if (icnt > 0)
		{
			jint* temp = (*env)->GetIntArrayElements(env, ja, NULL);
			if (temp)
			{
				ret = calloc(icnt, sizeof(uint32_t));
				if (ret)
				{
					for (i = 0; i < icnt; i++) {
						ret[i] = (uint32_t)temp[i];
					}
					*ret_icnt = icnt;
				}
				(*env)->ReleaseIntArrayElements(env, ja, temp, 0);
			}
		}
	}

	return ret;
}


/*******************************************************************************
 * DcContext
 ******************************************************************************/


typedef struct dc_jnicontext_t {
	JavaVM*   jvm; // JNIEnv cannot be shared between threads, so we share the JavaVM object
	jclass    cls;
	jobject   obj;
	jmethodID methodId;
} dc_jnicontext_t;


static dc_context_t* get_dc_context(JNIEnv *env, jobject obj)
{
	static jfieldID fid = 0;
	if (fid==0) {
		jclass cls = (*env)->GetObjectClass(env, obj);
		fid = (*env)->GetFieldID(env, cls, "contextCPtr", "J" /*Signature, J=long*/);
	}
	if (fid) {
		return (dc_context_t*)(*env)->GetLongField(env, obj, fid);
	}
	return NULL;
}


static uintptr_t s_context_callback_(dc_context_t* context, int event, uintptr_t data1, uintptr_t data2)
{
	jlong   l;
	JNIEnv* env;
	dc_jnicontext_t* jnicontext = dc_get_userdata(context);

	if (jnicontext==NULL || jnicontext->jvm==NULL || jnicontext->cls==NULL ||  jnicontext->obj==NULL || jnicontext->methodId==NULL) {
		return 0; /* may happen on startup */
	}

	(*jnicontext->jvm)->GetEnv(jnicontext->jvm, (void**)&env, JNI_VERSION_1_6); // as this function may be called from _any_ thread, we cannot use a static pointer to JNIEnv
	if (env==NULL) {
		return 0; /* may happen on startup */
	}

	l = (*env)->CallLongMethod(env, jnicontext->obj, jnicontext->methodId, (jint)event, (jlong)data1, (jlong)data2);
	return (uintptr_t)l;
}


JNIEXPORT jlong Java_com_b44t_messenger_DcContext_createContextCPtr(JNIEnv *env, jobject obj, jstring osname)
{
	jclass cls = (*env)->GetObjectClass(env, obj);

	dc_jnicontext_t* jnicontext = calloc(1, sizeof(dc_jnicontext_t));
	if (cls==NULL || jnicontext==NULL) {
		return 0;
	}

	(*env)->GetJavaVM(env, &jnicontext->jvm);
	jnicontext->cls = (*env)->NewGlobalRef(env, cls);
	jnicontext->obj = (*env)->NewGlobalRef(env, obj);
	jnicontext->methodId = (*env)->GetMethodID(env, jnicontext->cls, "handleEvent","(IJJ)J" /*signature as "(param)ret" with I=int, J=long*/);

	CHAR_REF(osname);
		jlong contextCPtr = (jlong)dc_context_new(s_context_callback_, jnicontext, osnamePtr);
	CHAR_UNREF(osname);
	return contextCPtr;
}


/* DcContext - open/configure/connect/fetch */


JNIEXPORT jint Java_com_b44t_messenger_DcContext_open(JNIEnv *env, jobject obj, jstring dbfile)
{
	CHAR_REF(dbfile);
		jint ret = dc_open(get_dc_context(env, obj), dbfilePtr, NULL);
	CHAR_UNREF(dbfile)
	return ret;
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_close(JNIEnv *env, jobject obj)
{
	dc_close(get_dc_context(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_setStockTranslation(JNIEnv *env, jobject obj, jint stock_id, jstring translation)
{
	CHAR_REF(translation);
		dc_set_stock_translation(get_dc_context(env, obj), stock_id, translationPtr);
	CHAR_UNREF(translation)
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcContext_setConfigFromQr(JNIEnv *env, jobject obj, jstring qr)
{
	CHAR_REF(qr);
		jboolean ret = dc_set_config_from_qr(get_dc_context(env, obj), qrPtr);
	CHAR_UNREF(qr);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContext_getBlobdir(JNIEnv *env, jobject obj)
{
	char* temp = dc_get_blobdir(get_dc_context(env, obj));
		jstring ret = JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_configure(JNIEnv *env, jobject obj)
{
	dc_configure(get_dc_context(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_stopOngoingProcess(JNIEnv *env, jobject obj)
{
	dc_stop_ongoing_process(get_dc_context(env, obj));
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_isConfigured(JNIEnv *env, jobject obj)
{
	return (jint)dc_is_configured(get_dc_context(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_performImapJobs(JNIEnv *env, jobject obj)
{
	dc_perform_imap_jobs(get_dc_context(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_performImapIdle(JNIEnv *env, jobject obj)
{
	dc_perform_imap_idle(get_dc_context(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_performImapFetch(JNIEnv *env, jobject obj)
{
	dc_perform_imap_fetch(get_dc_context(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_interruptImapIdle(JNIEnv *env, jobject obj)
{
	dc_interrupt_imap_idle(get_dc_context(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_performSentboxJobs(JNIEnv *env, jobject obj)
{
	dc_perform_sentbox_jobs(get_dc_context(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_performSentboxFetch(JNIEnv *env, jobject obj)
{
	dc_perform_sentbox_fetch(get_dc_context(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_performSentboxIdle(JNIEnv *env, jobject obj)
{
	dc_perform_sentbox_idle(get_dc_context(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_interruptSentboxIdle(JNIEnv *env, jobject obj)
{
	dc_interrupt_sentbox_idle(get_dc_context(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_performMvboxJobs(JNIEnv *env, jobject obj)
{
	dc_perform_mvbox_jobs(get_dc_context(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_performMvboxFetch(JNIEnv *env, jobject obj)
{
	dc_perform_mvbox_fetch(get_dc_context(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_performMvboxIdle(JNIEnv *env, jobject obj)
{
	dc_perform_mvbox_idle(get_dc_context(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_interruptMvboxIdle(JNIEnv *env, jobject obj)
{
	dc_interrupt_mvbox_idle(get_dc_context(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_performSmtpJobs(JNIEnv *env, jobject obj)
{
	dc_perform_smtp_jobs(get_dc_context(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_performSmtpIdle(JNIEnv *env, jobject obj)
{
	dc_perform_smtp_idle(get_dc_context(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_maybeNetwork(JNIEnv *env, jobject obj)
{
	dc_maybe_network(get_dc_context(env, obj));
}


/* DcContext - handle contacts */

JNIEXPORT jboolean Java_com_b44t_messenger_DcContext_mayBeValidAddr(JNIEnv *env, jobject obj, jstring addr)
{
	CHAR_REF(addr);
		jboolean ret = dc_may_be_valid_addr(addrPtr);
	CHAR_UNREF(addr);
	return ret;
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_lookupContactIdByAddr(JNIEnv *env, jobject obj, jstring addr)
{
	CHAR_REF(addr);
		jint ret = dc_lookup_contact_id_by_addr(get_dc_context(env, obj), addrPtr);
	CHAR_UNREF(addr);
	return ret;
}


JNIEXPORT jintArray Java_com_b44t_messenger_DcContext_getContacts(JNIEnv *env, jobject obj, jint flags, jstring query)
{
	CHAR_REF(query);
	    dc_array_t* ca = dc_get_contacts(get_dc_context(env, obj), flags, queryPtr);
	CHAR_UNREF(query);
	return dc_array2jintArray_n_unref(env, ca);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_getBlockedCount(JNIEnv *env, jobject obj)
{
	return dc_get_blocked_cnt(get_dc_context(env, obj));
}


JNIEXPORT jintArray Java_com_b44t_messenger_DcContext_getBlockedContacts(JNIEnv *env, jobject obj)
{
	dc_array_t* ca = dc_get_blocked_contacts(get_dc_context(env, obj));
	return dc_array2jintArray_n_unref(env, ca);
}


JNIEXPORT jlong Java_com_b44t_messenger_DcContext_getContactCPtr(JNIEnv *env, jobject obj, jint contact_id)
{
	return (jlong)dc_get_contact(get_dc_context(env, obj), contact_id);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_createContact(JNIEnv *env, jobject obj, jstring name, jstring addr)
{
	CHAR_REF(name);
	CHAR_REF(addr);
		jint ret = (jint)dc_create_contact(get_dc_context(env, obj), namePtr, addrPtr);
	CHAR_UNREF(addr);
	CHAR_UNREF(name);
	return ret;
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_blockContact(JNIEnv *env, jobject obj, jint contact_id, jint block)
{
	dc_block_contact(get_dc_context(env, obj), contact_id, block);
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcContext_deleteContact(JNIEnv *env, jobject obj, jint contact_id)
{
	return (jboolean)dc_delete_contact(get_dc_context(env, obj), contact_id);
}


/* DcContext - handle chats */

JNIEXPORT jlong Java_com_b44t_messenger_DcContext_getChatlistCPtr(JNIEnv *env, jobject obj, jint listflags, jstring query, jint queryId)
{
	jlong ret;
	if (query) {
		CHAR_REF(query);
			ret = (jlong)dc_get_chatlist(get_dc_context(env, obj), listflags, queryPtr, queryId);
		CHAR_UNREF(query);
	}
	else {
		ret = (jlong)dc_get_chatlist(get_dc_context(env, obj), listflags, NULL, queryId);
	}
	return ret;
}


JNIEXPORT jlong Java_com_b44t_messenger_DcContext_getChatCPtr(JNIEnv *env, jobject obj, jint chat_id)
{
	return (jlong)dc_get_chat(get_dc_context(env, obj), chat_id);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_getChatIdByContactId(JNIEnv *env, jobject obj, jint contact_id)
{
	return (jint)dc_get_chat_id_by_contact_id(get_dc_context(env, obj), contact_id);
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_markseenMsgs(JNIEnv *env, jobject obj, jintArray msg_ids)
{
	int msg_ids_cnt = 0;
	uint32_t* msg_ids_ptr = jintArray2uint32Pointer(env, msg_ids, &msg_ids_cnt);
		dc_markseen_msgs(get_dc_context(env, obj), msg_ids_ptr, msg_ids_cnt);
	free(msg_ids_ptr);
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_marknoticedChat(JNIEnv *env, jobject obj, jint chat_id)
{
	dc_marknoticed_chat(get_dc_context(env, obj), chat_id);
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_marknoticedAllChats(JNIEnv *env, jobject obj)
{
	dc_marknoticed_all_chats(get_dc_context(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_marknoticedContact(JNIEnv *env, jobject obj, jint contact_id)
{
	dc_marknoticed_contact(get_dc_context(env, obj), contact_id);
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_setChatVisibility(JNIEnv *env, jobject obj, jint chat_id, jint visibility)
{
	dc_set_chat_visibility(get_dc_context(env, obj), chat_id, visibility);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_createChatByContactId(JNIEnv *env, jobject obj, jint contact_id)
{
	return (jint)dc_create_chat_by_contact_id(get_dc_context(env, obj), contact_id);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_createChatByMsgId(JNIEnv *env, jobject obj, jint msg_id)
{
	return (jint)dc_create_chat_by_msg_id(get_dc_context(env, obj), msg_id);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_createGroupChat(JNIEnv *env, jobject obj, jboolean verified, jstring name)
{
	CHAR_REF(name);
		jint ret = (jint)dc_create_group_chat(get_dc_context(env, obj), verified, namePtr);
	CHAR_UNREF(name);
	return ret;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcContext_isContactInChat(JNIEnv *env, jobject obj, jint chat_id, jint contact_id)
{
	return (jboolean)dc_is_contact_in_chat(get_dc_context(env, obj), chat_id, contact_id);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_addContactToChat(JNIEnv *env, jobject obj, jint chat_id, jint contact_id)
{
	return (jint)dc_add_contact_to_chat(get_dc_context(env, obj), chat_id, contact_id);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_removeContactFromChat(JNIEnv *env, jobject obj, jint chat_id, jint contact_id)
{
	return (jint)dc_remove_contact_from_chat(get_dc_context(env, obj), chat_id, contact_id);
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_setDraft(JNIEnv *env, jobject obj, jint chat_id, jobject msg /* NULL=delete */)
{
	dc_set_draft(get_dc_context(env, obj), chat_id, get_dc_msg(env, msg));
}


JNIEXPORT jlong Java_com_b44t_messenger_DcContext_getDraftCPtr(JNIEnv *env, jobject obj, jint chat_id)
{
	return (jlong)dc_get_draft(get_dc_context(env, obj), chat_id);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_setChatName(JNIEnv *env, jobject obj, jint chat_id, jstring name)
{
	CHAR_REF(name);
		jint ret = (jint)dc_set_chat_name(get_dc_context(env, obj), chat_id, namePtr);
	CHAR_UNREF(name);
	return ret;
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_setChatProfileImage(JNIEnv *env, jobject obj, jint chat_id, jstring image/*NULL=delete*/)
{
	CHAR_REF(image);
		jint ret = (jint)dc_set_chat_profile_image(get_dc_context(env, obj), chat_id, imagePtr/*CHAR_REF() preserves NULL*/);
	CHAR_UNREF(image);
	return ret;
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_deleteChat(JNIEnv *env, jobject obj, jint chat_id)
{
	dc_delete_chat(get_dc_context(env, obj), chat_id);
}


/* DcContext - handle messages */


JNIEXPORT jint Java_com_b44t_messenger_DcContext_getFreshMsgCount(JNIEnv *env, jobject obj, jint chat_id)
{
	return dc_get_fresh_msg_cnt(get_dc_context(env, obj), chat_id);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_estimateDeletionCount(JNIEnv *env, jobject obj, jboolean from_server, jlong seconds)
{
	return dc_estimate_deletion_cnt(get_dc_context(env, obj), from_server, seconds);
}



JNIEXPORT jlong Java_com_b44t_messenger_DcContext_getMsgCPtr(JNIEnv *env, jobject obj, jint id)
{
	return (jlong)dc_get_msg(get_dc_context(env, obj), id);
}


JNIEXPORT jlong Java_com_b44t_messenger_DcContext_createMsgCPtr(JNIEnv *env, jobject obj, jint viewtype)
{
	return (jlong)dc_msg_new(get_dc_context(env, obj), viewtype);
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContext_getMsgInfo(JNIEnv *env, jobject obj, jint msg_id)
{
	char* temp = dc_get_msg_info(get_dc_context(env, obj), msg_id);
		jstring ret = JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_deleteMsgs(JNIEnv *env, jobject obj, jintArray msg_ids)
{
	int msg_ids_cnt = 0;
	uint32_t* msg_ids_ptr = jintArray2uint32Pointer(env, msg_ids, &msg_ids_cnt);
		dc_delete_msgs(get_dc_context(env, obj), msg_ids_ptr, msg_ids_cnt);
	free(msg_ids_ptr);
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_forwardMsgs(JNIEnv *env, jobject obj, jintArray msg_ids, jint chat_id)
{
	int msg_ids_cnt = 0;
	uint32_t* msg_ids_ptr = jintArray2uint32Pointer(env, msg_ids, &msg_ids_cnt);
		dc_forward_msgs(get_dc_context(env, obj), msg_ids_ptr, msg_ids_cnt, chat_id);
	free(msg_ids_ptr);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_prepareMsg(JNIEnv *env, jobject obj, jint chat_id, jobject msg)
{
	return dc_prepare_msg(get_dc_context(env, obj), chat_id, get_dc_msg(env, msg));
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_sendMsg(JNIEnv *env, jobject obj, jint chat_id, jobject msg)
{
	return dc_send_msg(get_dc_context(env, obj), chat_id, get_dc_msg(env, msg));
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_sendTextMsg(JNIEnv *env, jobject obj, jint chat_id, jstring text)
{
	CHAR_REF(text);
		jint msg_id = dc_send_text_msg(get_dc_context(env, obj), chat_id, textPtr);
	CHAR_UNREF(text);
	return msg_id;
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_addDeviceMsg(JNIEnv *env, jobject obj, jstring label, jobject msg)
{
	CHAR_REF(label);
		int msg_id = dc_add_device_msg(get_dc_context(env, obj), labelPtr, get_dc_msg(env, msg));
	CHAR_UNREF(label);
	return msg_id;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcContext_wasDeviceMsgEverAdded(JNIEnv *env, jobject obj, jstring label)
{
	CHAR_REF(label);
		jboolean ret = dc_was_device_msg_ever_added(get_dc_context(env, obj), labelPtr) != 0;
	CHAR_UNREF(label);
	return ret;
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_updateDeviceChats(JNIEnv *env, jobject obj)
{
	dc_update_device_chats(get_dc_context(env, obj));
}


/* DcContext - handle config */

JNIEXPORT void Java_com_b44t_messenger_DcContext_setConfig(JNIEnv *env, jobject obj, jstring key, jstring value /*may be NULL*/)
{
	CHAR_REF(key);
	CHAR_REF(value);
		dc_set_config(get_dc_context(env, obj), keyPtr, valuePtr /*is NULL if value is NULL, CHAR_REF() handles this*/);
	CHAR_UNREF(key);
	CHAR_UNREF(value);
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContext_getConfig(JNIEnv *env, jobject obj, jstring key)
{
	CHAR_REF(key);
		char* temp = dc_get_config(get_dc_context(env, obj), keyPtr);
			jstring ret = NULL;
			if (temp) {
				ret = JSTRING_NEW(temp);
			}
		dc_str_unref(temp);
	CHAR_UNREF(key);
	return ret; /* returns NULL only if key is unset and "def" is NULL */
}


/* DcContext - out-of-band verification */

JNIEXPORT jlong Java_com_b44t_messenger_DcContext_checkQrCPtr(JNIEnv *env, jobject obj, jstring qr)
{
	CHAR_REF(qr);
		jlong ret = (jlong)dc_check_qr(get_dc_context(env, obj), qrPtr);
	CHAR_UNREF(qr);
	return ret;
}

JNIEXPORT jstring Java_com_b44t_messenger_DcContext_getSecurejoinQr(JNIEnv *env, jobject obj, jint chat_id)
{
	char* temp = dc_get_securejoin_qr(get_dc_context(env, obj), chat_id);
		jstring ret = JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}

JNIEXPORT jint Java_com_b44t_messenger_DcContext_joinSecurejoin(JNIEnv *env, jobject obj, jstring qr)
{
	CHAR_REF(qr);
		jint ret = dc_join_securejoin(get_dc_context(env, obj), qrPtr);
	CHAR_UNREF(qr);
	return ret;
}


/* DcContext - misc. */

JNIEXPORT jstring Java_com_b44t_messenger_DcContext_getInfo(JNIEnv *env, jobject obj)
{
	char* temp = dc_get_info(get_dc_context(env, obj));
		jstring ret = JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContext_getOauth2Url(JNIEnv *env, jobject obj, jstring addr, jstring redirectUrl)
{
    CHAR_REF(addr);
    CHAR_REF(redirectUrl);
	char* temp = dc_get_oauth2_url(get_dc_context(env, obj), addrPtr, redirectUrlPtr);
		jstring ret = JSTRING_NEW(temp);
	dc_str_unref(temp);
	CHAR_UNREF(redirectUrl);
	CHAR_UNREF(addr);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContext_getContactEncrInfo(JNIEnv *env, jobject obj, jint contact_id)
{
	char* temp = dc_get_contact_encrinfo(get_dc_context(env, obj), contact_id);
		jstring ret = JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContext_initiateKeyTransfer(JNIEnv *env, jobject obj)
{
	jstring setup_code = NULL;
	char* temp = dc_initiate_key_transfer(get_dc_context(env, obj));
	if (temp) {
		setup_code = JSTRING_NEW(temp);
		dc_str_unref(temp);
	}
	return setup_code;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcContext_continueKeyTransfer(JNIEnv *env, jobject obj, jint msg_id, jstring setupCode)
{
	CHAR_REF(setupCode);
		jboolean ret = dc_continue_key_transfer(get_dc_context(env, obj), msg_id, setupCodePtr);
	CHAR_UNREF(setupCode);
	return ret;
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_imex(JNIEnv *env, jobject obj, jint what, jstring dir)
{
	CHAR_REF(dir);
		dc_imex(get_dc_context(env, obj), what, dirPtr, "");
	CHAR_UNREF(dir);
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContext_imexHasBackup(JNIEnv *env, jobject obj, jstring dir)
{
	CHAR_REF(dir);
		jstring ret = NULL;
		char* temp = dc_imex_has_backup(get_dc_context(env, obj),  dirPtr);
		if (temp) {
			ret = JSTRING_NEW(temp);
			dc_str_unref(temp);
		}
	CHAR_UNREF(dir);
	return ret; /* may be NULL! */
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_emptyServer(JNIEnv *env, jobject obj, jint flags)
{
	dc_empty_server(get_dc_context(env, obj), flags);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_addAddressBook(JNIEnv *env, jobject obj, jstring adrbook)
{
	CHAR_REF(adrbook);
		int modify_count = dc_add_address_book(get_dc_context(env, obj), adrbookPtr);
	CHAR_UNREF(adrbook);
	return modify_count;
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_sendLocationsToChat(JNIEnv *env, jobject obj, jint chat_id, jint seconds)
{
	dc_send_locations_to_chat(get_dc_context(env, obj), chat_id, seconds);
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcContext_isSendingLocationsToChat(JNIEnv *env, jobject obj, jint chat_id)
{
	return (dc_is_sending_locations_to_chat(get_dc_context(env, obj), chat_id)!=0);
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcContext_setLocation(JNIEnv *env, jobject obj, jfloat latitude, jfloat longitude, jfloat accuracy)
{
	return (dc_set_location(get_dc_context(env, obj), latitude, longitude, accuracy)!=0);
}


JNIEXPORT jlong Java_com_b44t_messenger_DcContext_getLocationsCPtr(JNIEnv *env, jobject obj, jint chat_id, jint contact_id, jlong timestamp_start, jlong timestamp_end)
{
	return (jlong)dc_get_locations(get_dc_context(env, obj), chat_id, contact_id, CTIMESTAMP(timestamp_start), CTIMESTAMP(timestamp_end));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_deleteAllLocations(JNIEnv *env, jobject obj)
{
	dc_delete_all_locations(get_dc_context(env, obj));
}


JNIEXPORT jlong Java_com_b44t_messenger_DcContext_getProviderFromEmailCPtr(JNIEnv *env, jobject obj, jstring email)
{
	CHAR_REF(email);
		jlong ret = (jlong)dc_provider_new_from_email(get_dc_context(env, obj), emailPtr);
	CHAR_UNREF(email);
	return ret;
}


/*******************************************************************************
 * DcArray
 ******************************************************************************/


static dc_array_t* get_dc_array(JNIEnv *env, jobject obj)
{
	static jfieldID fid = 0;
	if (fid==0) {
		jclass cls = (*env)->GetObjectClass(env, obj);
		fid = (*env)->GetFieldID(env, cls, "arrayCPtr", "J" /*Signature, J=long*/);
	}
	if (fid) {
		return (dc_array_t*)(*env)->GetLongField(env, obj, fid);
	}
	return NULL;
}


JNIEXPORT void Java_com_b44t_messenger_DcArray_unrefArrayCPtr(JNIEnv *env, jobject obj)
{
	dc_array_unref(get_dc_array(env, obj));
}


JNIEXPORT jint Java_com_b44t_messenger_DcArray_getCnt(JNIEnv *env, jobject obj)
{
	return dc_array_get_cnt(get_dc_array(env, obj));
}


JNIEXPORT jfloat Java_com_b44t_messenger_DcArray_getLatitude(JNIEnv *env, jobject obj, jint index)
{
	return (jfloat)dc_array_get_latitude(get_dc_array(env, obj), index);
}


JNIEXPORT jfloat Java_com_b44t_messenger_DcArray_getLongitude(JNIEnv *env, jobject obj, jint index)
{
	return (jfloat)dc_array_get_longitude(get_dc_array(env, obj), index);
}


JNIEXPORT jfloat Java_com_b44t_messenger_DcArray_getAccuracy(JNIEnv *env, jobject obj, jint index)
{
	return (jfloat)dc_array_get_accuracy(get_dc_array(env, obj), index);
}


JNIEXPORT jlong Java_com_b44t_messenger_DcArray_getTimestamp(JNIEnv *env, jobject obj, jint index)
{
	return JTIMESTAMP(dc_array_get_timestamp(get_dc_array(env, obj), index));
}


JNIEXPORT jint Java_com_b44t_messenger_DcArray_getMsgId(JNIEnv *env, jobject obj, jint index)
{
	return dc_array_get_msg_id(get_dc_array(env, obj), index);
}


JNIEXPORT jint Java_com_b44t_messenger_DcArray_getLocationId(JNIEnv *env, jobject obj, jint index)
{
	return dc_array_get_id(get_dc_array(env, obj), index);
}


JNIEXPORT jstring Java_com_b44t_messenger_DcArray_getMarker(JNIEnv *env, jobject obj, jint index)
{
	char* temp = dc_array_get_marker(get_dc_array(env, obj), index);
		jstring ret = NULL;
		if (temp) {
			ret = JSTRING_NEW(temp);
		}
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcArray_isIndependent(JNIEnv *env, jobject obj, jint index)
{
	return (dc_array_is_independent(get_dc_array(env, obj), index)!=0);
}


/*******************************************************************************
 * DcChatlist
 ******************************************************************************/


static dc_chatlist_t* get_dc_chatlist(JNIEnv *env, jobject obj)
{
	static jfieldID fid = 0;
	if (fid==0) {
		jclass cls = (*env)->GetObjectClass(env, obj);
		fid = (*env)->GetFieldID(env, cls, "chatlistCPtr", "J" /*Signature, J=long*/);
	}
	if (fid) {
		return (dc_chatlist_t*)(*env)->GetLongField(env, obj, fid);
	}
	return NULL;
}


JNIEXPORT void Java_com_b44t_messenger_DcChatlist_unrefChatlistCPtr(JNIEnv *env, jobject obj)
{
	dc_chatlist_unref(get_dc_chatlist(env, obj));
}


JNIEXPORT jint Java_com_b44t_messenger_DcChatlist_getCnt(JNIEnv *env, jobject obj)
{
	return dc_chatlist_get_cnt(get_dc_chatlist(env, obj));
}


JNIEXPORT jint Java_com_b44t_messenger_DcChatlist_getChatId(JNIEnv *env, jobject obj, jint index)
{
	return dc_chatlist_get_chat_id(get_dc_chatlist(env, obj), index);
}


JNIEXPORT jlong Java_com_b44t_messenger_DcChatlist_getChatCPtr(JNIEnv *env, jobject obj, jint index)
{
	dc_chatlist_t* chatlist = get_dc_chatlist(env, obj);
	return (jlong)dc_get_chat(dc_chatlist_get_context(chatlist), dc_chatlist_get_chat_id(chatlist, index));
}


JNIEXPORT jint Java_com_b44t_messenger_DcChatlist_getMsgId(JNIEnv *env, jobject obj, jint index)
{
	return dc_chatlist_get_msg_id(get_dc_chatlist(env, obj), index);
}


JNIEXPORT jlong Java_com_b44t_messenger_DcChatlist_getMsgCPtr(JNIEnv *env, jobject obj, jint index)
{
	dc_chatlist_t* chatlist = get_dc_chatlist(env, obj);
	return (jlong)dc_get_msg(dc_chatlist_get_context(chatlist), dc_chatlist_get_msg_id(chatlist, index));
}


JNIEXPORT jlong Java_com_b44t_messenger_DcChatlist_getSummaryCPtr(JNIEnv *env, jobject obj, jint index, jlong chatCPtr)
{
	return (jlong)dc_chatlist_get_summary(get_dc_chatlist(env, obj), index, (dc_chat_t*)chatCPtr);
}


/*******************************************************************************
 * DcChat
 ******************************************************************************/


static dc_chat_t* get_dc_chat(JNIEnv *env, jobject obj)
{
	static jfieldID fid = 0;
	if (fid==0) {
		jclass cls = (*env)->GetObjectClass(env, obj);
		fid = (*env)->GetFieldID(env, cls, "chatCPtr", "J" /*Signature, J=long*/);
	}
	if (fid) {
		return (dc_chat_t*)(*env)->GetLongField(env, obj, fid);
	}
	return NULL;
}


JNIEXPORT void Java_com_b44t_messenger_DcChat_unrefChatCPtr(JNIEnv *env, jobject obj)
{
	dc_chat_unref(get_dc_chat(env, obj));
}


JNIEXPORT jint Java_com_b44t_messenger_DcChat_getId(JNIEnv *env, jobject obj)
{
	return dc_chat_get_id(get_dc_chat(env, obj));
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcChat_isGroup(JNIEnv *env, jobject obj)
{
	int chat_type = dc_chat_get_type(get_dc_chat(env, obj));
	return (chat_type==DC_CHAT_TYPE_GROUP || chat_type==DC_CHAT_TYPE_VERIFIED_GROUP);
}


JNIEXPORT jint Java_com_b44t_messenger_DcChat_getVisibility(JNIEnv *env, jobject obj)
{
	return dc_chat_get_visibility(get_dc_chat(env, obj));
}


JNIEXPORT jstring Java_com_b44t_messenger_DcChat_getName(JNIEnv *env, jobject obj)
{
	char* temp = dc_chat_get_name(get_dc_chat(env, obj));
		jstring ret = JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcChat_getProfileImage(JNIEnv *env, jobject obj)
{
	char* temp = dc_chat_get_profile_image(get_dc_chat(env, obj));
		jstring ret = JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT jint Java_com_b44t_messenger_DcChat_getColor(JNIEnv *env, jobject obj)
{
	return dc_chat_get_color(get_dc_chat(env, obj));
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcChat_isUnpromoted(JNIEnv *env, jobject obj)
{
	return dc_chat_is_unpromoted(get_dc_chat(env, obj))!=0;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcChat_isSelfTalk(JNIEnv *env, jobject obj)
{
	return dc_chat_is_self_talk(get_dc_chat(env, obj))!=0;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcChat_isDeviceTalk(JNIEnv *env, jobject obj)
{
	return dc_chat_is_device_talk(get_dc_chat(env, obj))!=0;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcChat_canSend(JNIEnv *env, jobject obj)
{
	return dc_chat_can_send(get_dc_chat(env, obj))!=0;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcChat_isVerified(JNIEnv *env, jobject obj)
{
	return dc_chat_is_verified(get_dc_chat(env, obj))!=0;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcChat_isSendingLocations(JNIEnv *env, jobject obj)
{
	return dc_chat_is_sending_locations(get_dc_chat(env, obj))!=0;
}


JNIEXPORT jintArray Java_com_b44t_messenger_DcContext_getChatMedia(JNIEnv *env, jobject obj, jint chat_id, jint type1, jint type2, jint type3)
{
	dc_array_t* ca = dc_get_chat_media(get_dc_context(env, obj), chat_id, type1, type2, type3);
	return dc_array2jintArray_n_unref(env, ca);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_getNextMedia(JNIEnv *env, jobject obj, jint msg_id, jint dir, jint type1, jint type2, jint type3)
{
	return dc_get_next_media(get_dc_context(env, obj), msg_id, dir, type1, type2, type3);
}


JNIEXPORT jintArray Java_com_b44t_messenger_DcContext_getChatMsgs(JNIEnv *env, jobject obj, jint chat_id, jint flags, jint marker1before)
{
	dc_array_t* ca = dc_get_chat_msgs(get_dc_context(env, obj), chat_id, flags, marker1before);
	return dc_array2jintArray_n_unref(env, ca);
}


JNIEXPORT jintArray Java_com_b44t_messenger_DcContext_searchMsgs(JNIEnv *env, jobject obj, jint chat_id, jstring query)
{
	CHAR_REF(query);
		dc_array_t* ca = dc_search_msgs(get_dc_context(env, obj), chat_id, queryPtr);
	CHAR_UNREF(query);
	return dc_array2jintArray_n_unref(env, ca);
}


JNIEXPORT jintArray Java_com_b44t_messenger_DcContext_getFreshMsgs(JNIEnv *env, jobject obj)
{
	dc_array_t* ca = dc_get_fresh_msgs(get_dc_context(env, obj));
	return dc_array2jintArray_n_unref(env, ca);
}


JNIEXPORT jintArray Java_com_b44t_messenger_DcContext_getChatContacts(JNIEnv *env, jobject obj, jint chat_id)
{
	dc_array_t* ca = dc_get_chat_contacts(get_dc_context(env, obj), chat_id);
	return dc_array2jintArray_n_unref(env, ca);
}


/*******************************************************************************
 * DcMsg
 ******************************************************************************/


static dc_msg_t* get_dc_msg(JNIEnv *env, jobject obj)
{
	static jfieldID fid = 0;
	if (env && obj) {
		if (fid==0) {
			jclass cls = (*env)->GetObjectClass(env, obj);
			fid = (*env)->GetFieldID(env, cls, "msgCPtr", "J" /*Signature, J=long*/);
		}
		if (fid) {
			return (dc_msg_t*)(*env)->GetLongField(env, obj, fid);
		}
	}
	return NULL;
}


JNIEXPORT void Java_com_b44t_messenger_DcMsg_unrefMsgCPtr(JNIEnv *env, jobject obj)
{
	dc_msg_unref(get_dc_msg(env, obj));
}


JNIEXPORT jint Java_com_b44t_messenger_DcMsg_getId(JNIEnv *env, jobject obj)
{
	return dc_msg_get_id(get_dc_msg(env, obj));
}


JNIEXPORT jstring Java_com_b44t_messenger_DcMsg_getText(JNIEnv *env, jobject obj)
{
	char* temp = dc_msg_get_text(get_dc_msg(env, obj));
		jstring ret = JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT jlong Java_com_b44t_messenger_DcMsg_getTimestamp(JNIEnv *env, jobject obj)
{
	return JTIMESTAMP(dc_msg_get_timestamp(get_dc_msg(env, obj)));
}


JNIEXPORT jlong Java_com_b44t_messenger_DcMsg_getSortTimestamp(JNIEnv *env, jobject obj)
{
	return JTIMESTAMP(dc_msg_get_sort_timestamp(get_dc_msg(env, obj)));
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcMsg_hasDeviatingTimestamp(JNIEnv *env, jobject obj)
{
	return dc_msg_has_deviating_timestamp(get_dc_msg(env, obj))!=0;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcMsg_hasLocation(JNIEnv *env, jobject obj)
{
	return dc_msg_has_location(get_dc_msg(env, obj))!=0;
}


JNIEXPORT jint Java_com_b44t_messenger_DcMsg_getType(JNIEnv *env, jobject obj)
{
	return dc_msg_get_viewtype(get_dc_msg(env, obj));
}


JNIEXPORT jint Java_com_b44t_messenger_DcMsg_getState(JNIEnv *env, jobject obj)
{
	return dc_msg_get_state(get_dc_msg(env, obj));
}


JNIEXPORT jint Java_com_b44t_messenger_DcMsg_getChatId(JNIEnv *env, jobject obj)
{
	return dc_msg_get_chat_id(get_dc_msg(env, obj));
}


JNIEXPORT jint Java_com_b44t_messenger_DcMsg_getFromId(JNIEnv *env, jobject obj)
{
	return dc_msg_get_from_id(get_dc_msg(env, obj));
}


JNIEXPORT jint Java_com_b44t_messenger_DcMsg_getWidth(JNIEnv *env, jobject obj, jint def)
{
	jint ret = (jint)dc_msg_get_width(get_dc_msg(env, obj));
	return ret? ret : def;
}


JNIEXPORT jint Java_com_b44t_messenger_DcMsg_getHeight(JNIEnv *env, jobject obj, jint def)
{
	jint ret = (jint)dc_msg_get_height(get_dc_msg(env, obj));
	return ret? ret : def;
}


JNIEXPORT jint Java_com_b44t_messenger_DcMsg_getDuration(JNIEnv *env, jobject obj)
{
	return dc_msg_get_duration(get_dc_msg(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcMsg_lateFilingMediaSize(JNIEnv *env, jobject obj, jint width, jint height, jint duration)
{
	dc_msg_latefiling_mediasize(get_dc_msg(env, obj), width, height, duration);
}


JNIEXPORT jlong Java_com_b44t_messenger_DcMsg_getFilebytes(JNIEnv *env, jobject obj)
{
	return (jlong)dc_msg_get_filebytes(get_dc_msg(env, obj));
}


JNIEXPORT jlong Java_com_b44t_messenger_DcMsg_getSummaryCPtr(JNIEnv *env, jobject obj, jlong chatCPtr)
{
	return (jlong)dc_msg_get_summary(get_dc_msg(env, obj), (dc_chat_t*)chatCPtr);
}


JNIEXPORT jstring Java_com_b44t_messenger_DcMsg_getSummarytext(JNIEnv *env, jobject obj, jint approx_characters)
{
	char* temp = dc_msg_get_summarytext(get_dc_msg(env, obj), approx_characters);
		jstring ret = JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT jint Java_com_b44t_messenger_DcMsg_showPadlock(JNIEnv *env, jobject obj)
{
	return dc_msg_get_showpadlock(get_dc_msg(env, obj));
}


JNIEXPORT jstring Java_com_b44t_messenger_DcMsg_getFile(JNIEnv *env, jobject obj)
{
	char* temp = dc_msg_get_file(get_dc_msg(env, obj));
		jstring ret =  JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcMsg_getFilemime(JNIEnv *env, jobject obj)
{
	char* temp = dc_msg_get_filemime(get_dc_msg(env, obj));
		jstring ret =  JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcMsg_getFilename(JNIEnv *env, jobject obj)
{
	char* temp = dc_msg_get_filename(get_dc_msg(env, obj));
		jstring ret =  JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcMsg_isForwarded(JNIEnv *env, jobject obj)
{
    return dc_msg_is_forwarded(get_dc_msg(env, obj))!=0;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcMsg_isIncreation(JNIEnv *env, jobject obj)
{
    return dc_msg_is_increation(get_dc_msg(env, obj))!=0;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcMsg_isInfo(JNIEnv *env, jobject obj)
{
    return dc_msg_is_info(get_dc_msg(env, obj))!=0;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcMsg_isSetupMessage(JNIEnv *env, jobject obj)
{
    return dc_msg_is_setupmessage(get_dc_msg(env, obj));
}


JNIEXPORT jstring Java_com_b44t_messenger_DcMsg_getSetupCodeBegin(JNIEnv *env, jobject obj)
{
	char* temp = dc_msg_get_setupcodebegin(get_dc_msg(env, obj));
		jstring ret =  JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT void Java_com_b44t_messenger_DcMsg_setText(JNIEnv *env, jobject obj, jstring text)
{
	CHAR_REF(text);
		dc_msg_set_text(get_dc_msg(env, obj), textPtr);
	CHAR_UNREF(text);
}


JNIEXPORT void Java_com_b44t_messenger_DcMsg_setFile(JNIEnv *env, jobject obj, jstring file, jstring filemime)
{
	CHAR_REF(file);
	CHAR_REF(filemime);
		dc_msg_set_file(get_dc_msg(env, obj), filePtr, filemimePtr);
	CHAR_UNREF(filemime);
	CHAR_UNREF(file);
}


JNIEXPORT void Java_com_b44t_messenger_DcMsg_setDimension(JNIEnv *env, jobject obj, int width, int height)
{
    dc_msg_set_dimension(get_dc_msg(env, obj), width, height);
}


JNIEXPORT void Java_com_b44t_messenger_DcMsg_setDuration(JNIEnv *env, jobject obj, int duration)
{
    dc_msg_set_duration(get_dc_msg(env, obj), duration);
}


JNIEXPORT void Java_com_b44t_messenger_DcMsg_setLocation(JNIEnv *env, jobject obj, jfloat latitude, jfloat longitude)
{
    dc_msg_set_location(get_dc_msg(env, obj), latitude, longitude);
}



/*******************************************************************************
 * DcContact
 ******************************************************************************/


static dc_contact_t* get_dc_contact(JNIEnv *env, jobject obj)
{
	static jfieldID fid = 0;
	if (fid==0) {
		jclass cls = (*env)->GetObjectClass(env, obj);
		fid = (*env)->GetFieldID(env, cls, "contactCPtr", "J" /*Signature, J=long*/);
	}
	if (fid) {
		return (dc_contact_t*)(*env)->GetLongField(env, obj, fid);
	}
	return NULL;
}


JNIEXPORT void Java_com_b44t_messenger_DcContact_unrefContactCPtr(JNIEnv *env, jobject obj)
{
	dc_contact_unref(get_dc_contact(env, obj));
}


JNIEXPORT jint Java_com_b44t_messenger_DcContact_getId(JNIEnv *env, jobject obj)
{
	return dc_contact_get_id(get_dc_contact(env, obj));
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContact_getName(JNIEnv *env, jobject obj)
{
	char* temp = dc_contact_get_name(get_dc_contact(env, obj));
		jstring ret = JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContact_getDisplayName(JNIEnv *env, jobject obj)
{
	char* temp = dc_contact_get_display_name(get_dc_contact(env, obj));
		jstring ret = JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContact_getFirstName(JNIEnv *env, jobject obj)
{
	char* temp = dc_contact_get_first_name(get_dc_contact(env, obj));
		jstring ret = JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContact_getAddr(JNIEnv *env, jobject obj)
{
	char* temp = dc_contact_get_addr(get_dc_contact(env, obj));
		jstring ret = JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContact_getNameNAddr(JNIEnv *env, jobject obj)
{
	char* temp = dc_contact_get_name_n_addr(get_dc_contact(env, obj));
		jstring ret = JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContact_getProfileImage(JNIEnv *env, jobject obj)
{
	char* temp = dc_contact_get_profile_image(get_dc_contact(env, obj));
		jstring ret = JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT jint Java_com_b44t_messenger_DcContact_getColor(JNIEnv *env, jobject obj)
{
	return dc_contact_get_color(get_dc_contact(env, obj));
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcContact_isBlocked(JNIEnv *env, jobject obj)
{
	return (jboolean)(dc_contact_is_blocked(get_dc_contact(env, obj))!=0);
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcContact_isVerified(JNIEnv *env, jobject obj)
{
	return dc_contact_is_verified(get_dc_contact(env, obj))==2;
}


/*******************************************************************************
 * DcLot
 ******************************************************************************/


static dc_lot_t* get_dc_lot(JNIEnv *env, jobject obj)
{
	static jfieldID fid = 0;
	if (fid==0) {
		jclass cls = (*env)->GetObjectClass(env, obj);
		fid = (*env)->GetFieldID(env, cls, "lotCPtr", "J" /*Signature, J=long*/);
	}
	if (fid) {
		return (dc_lot_t*)(*env)->GetLongField(env, obj, fid);
	}
	return NULL;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcLot_getText1(JNIEnv *env, jobject obj)
{
	char* temp = dc_lot_get_text1(get_dc_lot(env, obj));
		jstring ret = JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT jint Java_com_b44t_messenger_DcLot_getText1Meaning(JNIEnv *env, jobject obj)
{
	return dc_lot_get_text1_meaning(get_dc_lot(env, obj));
}


JNIEXPORT jstring Java_com_b44t_messenger_DcLot_getText2(JNIEnv *env, jobject obj)
{
	char* temp = dc_lot_get_text2(get_dc_lot(env, obj));
		jstring ret = JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT jlong Java_com_b44t_messenger_DcLot_getTimestamp(JNIEnv *env, jobject obj)
{
	return JTIMESTAMP(dc_lot_get_timestamp(get_dc_lot(env, obj)));
}


JNIEXPORT jint Java_com_b44t_messenger_DcLot_getState(JNIEnv *env, jobject obj)
{
	return dc_lot_get_state(get_dc_lot(env, obj));
}


JNIEXPORT jint Java_com_b44t_messenger_DcLot_getId(JNIEnv *env, jobject obj)
{
	return dc_lot_get_id(get_dc_lot(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcLot_unrefLotCPtr(JNIEnv *env, jobject obj)
{
	dc_lot_unref(get_dc_lot(env, obj));
}


/*******************************************************************************
 * DcProvider
 ******************************************************************************/


static dc_provider_t* get_dc_provider(JNIEnv *env, jobject obj)
{
	static jfieldID fid = 0;
	if (fid==0) {
		jclass cls = (*env)->GetObjectClass(env, obj);
		fid = (*env)->GetFieldID(env, cls, "providerCPtr", "J" /*Signature, J=long*/);
	}
	if (fid) {
		return (dc_provider_t*)(*env)->GetLongField(env, obj, fid);
	}
	return NULL;
}


JNIEXPORT void Java_com_b44t_messenger_DcProvider_unrefProviderCPtr(JNIEnv *env, jobject obj)
{
	dc_provider_unref(get_dc_provider(env, obj));
}


JNIEXPORT jint Java_com_b44t_messenger_DcProvider_getStatus(JNIEnv *env, jobject obj)
{
	return (jint)dc_provider_get_status(get_dc_provider(env, obj));
}


JNIEXPORT jstring Java_com_b44t_messenger_DcProvider_getBeforeLoginHint(JNIEnv *env, jobject obj)
{
	char* temp = dc_provider_get_before_login_hint(get_dc_provider(env, obj));
		jstring ret = JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcProvider_getOverviewPage(JNIEnv *env, jobject obj)
{
	char* temp = dc_provider_get_overview_page(get_dc_provider(env, obj));
		jstring ret = JSTRING_NEW(temp);
	dc_str_unref(temp);
	return ret;
}

/*******************************************************************************
 * Tools
 ******************************************************************************/


JNIEXPORT jboolean Java_com_b44t_messenger_DcContext_data1IsString(JNIEnv *env, jclass cls, jint event)
{
	return DC_EVENT_DATA1_IS_STRING(event);
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcContext_data2IsString(JNIEnv *env, jclass cls, jint event)
{
	return DC_EVENT_DATA2_IS_STRING(event);
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContext_dataToString(JNIEnv *env, jclass cls, jlong data)
{
	/* the callback may return a long that represents a pointer to a C-String; this function creates a Java-string from such values. */
	if (data==0) {
		return NULL;
	}
	const char* cstring = (const char*)data;
	return JSTRING_NEW(cstring);
}

