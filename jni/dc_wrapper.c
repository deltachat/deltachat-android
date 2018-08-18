/*******************************************************************************
 *
 *                              Delta Chat Android
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
#include <android/log.h>
#include "messenger-backend/src/deltachat.h"
#include "messenger-backend/cmdline/cmdline.h"


#define CHAR_REF(a) \
	const char* a##Ptr = (a)? (*env)->GetStringUTFChars(env, (a), 0) : NULL; // passing a NULL-jstring results in a NULL-ptr - this is needed by functions using eg. NULL for "delete"

#define CHAR_UNREF(a) \
	if(a) { (*env)->ReleaseStringUTFChars(env, (a), a##Ptr); }

#define JSTRING_NEW(a) jstring_new__(env, (a))
static jstring jstring_new__(JNIEnv* env, const char* a)
{
	if( a==NULL || a[0]==0 ) {
		return (*env)->NewStringUTF(env, "");
	}

	/* for non-empty strings, do not use NewStringUTF() as this is buggy on some Android versions.
	Instead, create the string using `new String(ByteArray, "UTF-8);` which seems to be programmed more properly.
	(eg. on KitKat a simple "SMILING FACE WITH SMILING EYES" (U+1F60A, UTF-8 F0 9F 98 8A) will let the app crash, reporting 0xF0 is a bad UTF-8 start,
	see http://stackoverflow.com/questions/12127817/android-ics-4-0-ndk-newstringutf-is-crashing-down-the-app ) */
	static jclass    s_strCls    = NULL;
	static jmethodID s_strCtor   = NULL;
	static jclass    s_strEncode = NULL;
	if( s_strCls==NULL ) {
		s_strCls    = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/String"));
		s_strCtor   = (*env)->GetMethodID(env, s_strCls, "<init>", "([BLjava/lang/String;)V");
		s_strEncode = (*env)->NewGlobalRef(env, (*env)->NewStringUTF(env, "UTF-8"));
	}

	int a_bytes = strlen(a);
	jbyteArray array = (*env)->NewByteArray(env, a_bytes);
		(*env)->SetByteArrayRegion(env, array, 0, a_bytes, a);
		jstring ret = (jstring) (*env)->NewObject(env, s_strCls, s_strCtor, array, s_strEncode);
	(*env)->DeleteLocalRef(env, array); /* we have to delete the reference as it is not returned to Java, AFAIK */

	return ret;
}


/* global stuff */

static JavaVM*   s_jvm = NULL;
static jclass    s_DcContext_class = NULL;
static jmethodID s_DcCallback_methodID = NULL;
static int       s_global_init_done = 0;


static void s_init_globals(JNIEnv *env, jclass DcContext_class)
{
	/* make sure, the intialisation is done only once */
	if( s_global_init_done ) { return; }
	s_global_init_done = 1;

	/* prepare calling back a Java function */
	__android_log_print(ANDROID_LOG_INFO, "DeltaChat", "JNI: s_init_globals()..."); /* low-level logging, dc_log_*() may not be yet available. However, please note that __android_log_print() may not work (eg. on LG X Cam) */

	(*env)->GetJavaVM(env, &s_jvm); /* JNIEnv cannot be shared between threads, so we share the JavaVM object */
	s_DcContext_class =  (*env)->NewGlobalRef(env, DcContext_class);
	s_DcCallback_methodID = (*env)->GetStaticMethodID(env, DcContext_class, "DcCallback","(IJJ)J" /*signature as "(param)ret" with I=int, J=long*/ );
}


/* tools */

static jintArray dc_array2jintArray_n_unref(JNIEnv *env, dc_array_t* ca)
{
	/* takes a C-array of type dc_array_t and converts it it a Java-Array.
	then the C-array is freed and the Java-Array is returned. */
	int i, icnt = ca? dc_array_get_cnt(ca) : 0;
	jintArray ret = (*env)->NewIntArray(env, icnt); if (ret == NULL) { return NULL; }

	if( ca ) {
		if( icnt ) {
			uintptr_t* ca_data = dc_array_get_raw(ca);
			if( sizeof(uintptr_t)==sizeof(jint) ) {
				(*env)->SetIntArrayRegion(env, ret, 0, icnt, (jint*)ca_data);
			}
			else {
				jint* temp = calloc(icnt, sizeof(jint));
					for( i = 0; i < icnt; i++ ) {
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
	if( ret_icnt ) { *ret_icnt = 0; }

	if( env && ja && ret_icnt )
	{
		int i, icnt  = (*env)->GetArrayLength(env, ja);
		if( icnt > 0 )
		{
			const jint* temp = (*env)->GetIntArrayElements(env, ja, NULL);
			if( temp )
			{
				ret = calloc(icnt, sizeof(uint32_t));
				if( ret )
				{
					for( i = 0; i < icnt; i++ ) {
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


static dc_context_t* get_dc_context(JNIEnv *env, jclass cls)
{
	static jfieldID fid = 0;
	if( fid == 0 ) {
		fid = (*env)->GetStaticFieldID(env, cls, "m_hContext", "J" /*Signature, J=long*/);
	}
	if( fid ) {
		return (dc_context_t*)(*env)->GetStaticLongField(env, cls, fid);
	}
	return NULL;
}


/* DcContext - new/delete */

static uintptr_t s_context_callback_(dc_context_t* context, int event, uintptr_t data1, uintptr_t data2)
{
	jlong   l;
	JNIEnv* env;

	#if 0 /* -- __android_log_print() does not log eg. on LG X Cam - but Javas Log.i() etc. do. So, we do not optimize these calls and just use the Java logging. */
	if( event==DC_EVENT_INFO || event==DC_EVENT_WARNING ) {
	    __android_log_print(event==DC_EVENT_INFO? ANDROID_LOG_INFO : ANDROID_LOG_WARN, "DeltaChat", "%s", (char*)data2); /* on problems, add `-llog` to `Android.mk` */
		return 0; /* speed up things for info/warning */
	}
	else if( event == DC_EVENT_ERROR ) {
	    __android_log_print(ANDROID_LOG_ERROR, "DeltaChat", "%s", (char*)data2);
	    /* errors are also forwarded to Java to show them in a bubble or so */
	}
	#endif

	if( s_jvm==NULL || s_DcContext_class==NULL || s_DcCallback_methodID==NULL ) {
		return 0; /* may happen on startup */
	}

	(*s_jvm)->GetEnv(s_jvm, &env, JNI_VERSION_1_6); /* as this function may be called from _any_ thread, we cannot use a static pointer to JNIEnv */
	if( env==NULL ) {
		return 0; /* may happen on startup */
	}

	l = (*env)->CallStaticLongMethod(env, s_DcContext_class, s_DcCallback_methodID, (jint)event, (jlong)data1, (jlong)data2);
	return (uintptr_t)l;
}


JNIEXPORT jlong Java_com_b44t_messenger_DcContext_DcContextNew(JNIEnv *env, jclass c)
{
	s_init_globals(env, c);
	return (jlong)dc_context_new(s_context_callback_, NULL, "Android");
}


/* DcContext - open/configure/connect/fetch */

JNIEXPORT jint Java_com_b44t_messenger_DcContext_open(JNIEnv *env, jclass cls, jstring dbfile)
{
	CHAR_REF(dbfile);
		jint ret = dc_open(get_dc_context(env, cls), dbfilePtr, NULL);
	CHAR_UNREF(dbfile)
	return ret;
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_close(JNIEnv *env, jclass cls)
{
	dc_close(get_dc_context(env, cls));
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContext_getBlobdir(JNIEnv *env, jclass cls)
{
	char* temp = dc_get_blobdir(get_dc_context(env, cls));
		jstring ret = JSTRING_NEW(temp);
	free(temp);
	return ret;
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_configure(JNIEnv *env, jclass cls)
{
	dc_configure(get_dc_context(env, cls));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_stopOngoingProcess(JNIEnv *env, jclass cls)
{
	dc_stop_ongoing_process(get_dc_context(env, cls));
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_isConfigured(JNIEnv *env, jclass cls)
{
	return (jint)dc_is_configured(get_dc_context(env, cls));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_performJobs(JNIEnv *env, jclass cls)
{
	dc_perform_imap_jobs(get_dc_context(env, cls));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_idle(JNIEnv *env, jclass cls)
{
	dc_perform_imap_idle(get_dc_context(env, cls));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_interruptIdle(JNIEnv *env, jclass cls)
{
	dc_interrupt_imap_idle(get_dc_context(env, cls));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_fetch(JNIEnv *env, jclass cls)
{
	dc_perform_imap_fetch(get_dc_context(env, cls));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_performSmtpJobs(JNIEnv *env, jclass cls)
{
	dc_perform_smtp_jobs(get_dc_context(env, cls));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_performSmtpIdle(JNIEnv *env, jclass cls)
{
	dc_perform_smtp_idle(get_dc_context(env, cls));
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_interruptSmtpIdle(JNIEnv *env, jclass cls)
{
	dc_interrupt_smtp_idle(get_dc_context(env, cls));
}


/* DcContext - handle contacts */

JNIEXPORT jintArray Java_com_b44t_messenger_DcContext_getContacts(JNIEnv *env, jclass cls, jint flags, jstring query)
{
	CHAR_REF(query);
	    dc_array_t* ca = dc_get_contacts(get_dc_context(env, cls), flags, queryPtr);
	CHAR_UNREF(query);
	return dc_array2jintArray_n_unref(env, ca);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_getBlockedCount(JNIEnv *env, jclass cls)
{
	return dc_get_blocked_cnt(get_dc_context(env, cls));
}


JNIEXPORT jintArray Java_com_b44t_messenger_DcContext_getBlockedContacts(JNIEnv *env, jclass cls)
{
	dc_array_t* ca = dc_get_blocked_contacts(get_dc_context(env, cls));
	return dc_array2jintArray_n_unref(env, ca);
}


JNIEXPORT jlong Java_com_b44t_messenger_DcContext_DcContextGetContact(JNIEnv *env, jclass c, jlong hContext, jint contact_id)
{
	return (jlong)dc_get_contact((dc_context_t*)hContext, contact_id);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_createContact(JNIEnv *env, jclass cls, jstring name, jstring addr)
{
	CHAR_REF(name);
	CHAR_REF(addr);
		jint ret = (jint)dc_create_contact(get_dc_context(env, cls), namePtr, addrPtr);
	CHAR_UNREF(addr);
	CHAR_UNREF(name);
	return ret;
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_blockContact(JNIEnv *env, jclass cls, jint contact_id, jint block)
{
	dc_block_contact(get_dc_context(env, cls), contact_id, block);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_deleteContact(JNIEnv *env, jclass cls, jint contact_id)
{
	return (jint)dc_delete_contact(get_dc_context(env, cls), contact_id);
}


/* DcContext - handle chats */

JNIEXPORT jlong Java_com_b44t_messenger_DcContext_DcContextGetChatlist(JNIEnv *env, jclass c, jlong hContext, jint listflags, jstring query, jint queryId)
{
	jlong ret;
	if( query ) {
		CHAR_REF(query);
			ret = (jlong)dc_get_chatlist((dc_context_t*)hContext, listflags, queryPtr, queryId);
		CHAR_UNREF(query);
	}
	else {
		ret = (jlong)dc_get_chatlist((dc_context_t*)hContext, listflags, NULL, queryId);
	}
	return ret;
}


JNIEXPORT jlong Java_com_b44t_messenger_DcContext_DcContextGetChat(JNIEnv *env, jclass c, jlong hContext, jint chat_id)
{
	return (jlong)dc_get_chat((dc_context_t*)hContext, chat_id);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_getChatIdByContactId(JNIEnv *env, jclass cls, jint contact_id)
{
	return (jint)dc_get_chat_id_by_contact_id(get_dc_context(env, cls), contact_id);
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_markseenMsgs(JNIEnv *env, jclass cls, jintArray msg_ids)
{
	int msg_ids_cnt = 0;
	const uint32_t* msg_ids_ptr = jintArray2uint32Pointer(env, msg_ids, &msg_ids_cnt);
		dc_markseen_msgs(get_dc_context(env, cls), msg_ids_ptr, msg_ids_cnt);
	free(msg_ids_ptr);
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_marknoticedChat(JNIEnv *env, jclass cls, jint chat_id)
{
	dc_marknoticed_chat(get_dc_context(env, cls), chat_id);
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_marknoticedContact(JNIEnv *env, jclass cls, jint contact_id)
{
	dc_marknoticed_contact(get_dc_context(env, cls), contact_id);
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_archiveChat(JNIEnv *env, jclass cls, jint chat_id, jint archive)
{
	dc_archive_chat(get_dc_context(env, cls), chat_id, archive);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_createChatByContactId(JNIEnv *env, jclass cls, jint contact_id)
{
	return (jint)dc_create_chat_by_contact_id(get_dc_context(env, cls), contact_id);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_createChatByMsgId(JNIEnv *env, jclass cls, jint msg_id)
{
	return (jint)dc_create_chat_by_msg_id(get_dc_context(env, cls), msg_id);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_createGroupChat(JNIEnv *env, jclass cls, jboolean verified, jstring name)
{
	CHAR_REF(name);
		jint ret = (jint)dc_create_group_chat(get_dc_context(env, cls), verified, namePtr);
	CHAR_UNREF(name);
	return ret;
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_isContactInChat(JNIEnv *env, jclass cls, jint chat_id, jint contact_id)
{
	return (jint)dc_is_contact_in_chat(get_dc_context(env, cls), chat_id, contact_id);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_addContactToChat(JNIEnv *env, jclass cls, jint chat_id, jint contact_id)
{
	return (jint)dc_add_contact_to_chat(get_dc_context(env, cls), chat_id, contact_id);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_removeContactFromChat(JNIEnv *env, jclass cls, jint chat_id, jint contact_id)
{
	return (jint)dc_remove_contact_from_chat(get_dc_context(env, cls), chat_id, contact_id);
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_setDraft(JNIEnv *env, jclass cls, jint chat_id, jstring draft /* NULL=delete */)
{
	CHAR_REF(draft);
		dc_set_text_draft(get_dc_context(env, cls), chat_id, draftPtr /* NULL=delete */);
	CHAR_UNREF(draft);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_setChatName(JNIEnv *env, jclass cls, jint chat_id, jstring name)
{
	CHAR_REF(name);
		jint ret = (jint)dc_set_chat_name(get_dc_context(env, cls), chat_id, namePtr);
	CHAR_UNREF(name);
	return ret;
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_setChatProfileImage(JNIEnv *env, jclass cls, jint chat_id, jstring image/*NULL=delete*/)
{
	CHAR_REF(image);
		jint ret = (jint)dc_set_chat_profile_image(get_dc_context(env, cls), chat_id, imagePtr/*CHAR_REF() preserves NULL*/);
	CHAR_UNREF(image);
	return ret;
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_deleteChat(JNIEnv *env, jclass cls, jint chat_id)
{
	dc_delete_chat(get_dc_context(env, cls), chat_id);
}


/* DcContext - handle messages */


JNIEXPORT jint Java_com_b44t_messenger_DcContext_getFreshMsgCount(JNIEnv *env, jclass cls, jint chat_id)
{
	return dc_get_fresh_msg_cnt(get_dc_context(env, cls), chat_id);
}


JNIEXPORT jlong Java_com_b44t_messenger_DcContext_DcContextGetMsg(JNIEnv *env, jclass c, jlong hContext, jint id)
{
	return (jlong)dc_get_msg((dc_context_t*)hContext, id);
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContext_getMsgInfo(JNIEnv *env, jclass cls, jint msg_id)
{
	char* temp = dc_get_msg_info(get_dc_context(env, cls), msg_id);
		jstring ret = JSTRING_NEW(temp);
	free(temp);
	return ret;
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_deleteMsgs(JNIEnv *env, jclass cls, jintArray msg_ids)
{
	int msg_ids_cnt = 0;
	const uint32_t* msg_ids_ptr = jintArray2uint32Pointer(env, msg_ids, &msg_ids_cnt);
		dc_delete_msgs(get_dc_context(env, cls), msg_ids_ptr, msg_ids_cnt);
	free(msg_ids_ptr);
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_forwardMsgs(JNIEnv *env, jclass cls, jintArray msg_ids, jint chat_id)
{
	int msg_ids_cnt = 0;
	const uint32_t* msg_ids_ptr = jintArray2uint32Pointer(env, msg_ids, &msg_ids_cnt);
		dc_forward_msgs(get_dc_context(env, cls), msg_ids_ptr, msg_ids_cnt, chat_id);
	free(msg_ids_ptr);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_sendTextMsg(JNIEnv *env, jclass cls, jint chat_id, jstring text)
{
	CHAR_REF(text);
		jint msg_id = dc_send_text_msg(get_dc_context(env, cls), chat_id, textPtr);
	CHAR_UNREF(text);
	return msg_id;
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_sendVcardMsg(JNIEnv *env, jobject obj, jint chat_id, jint contact_id)
{
	return dc_send_vcard_msg(get_dc_context(env, obj), chat_id, contact_id);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_sendMediaMsg(JNIEnv *env, jclass cls, jint chat_id, jint type, jstring file, jstring mime, jint w, jint h, jint ms, jstring author, jstring trackname)
{
	jint msg_id = 0;
	CHAR_REF(file);
	CHAR_REF(mime);
	CHAR_REF(author);
	CHAR_REF(trackname);
	switch( type ) {
		case DC_MSG_IMAGE: msg_id = (jint)dc_send_image_msg(get_dc_context(env, cls), chat_id, filePtr, mimePtr, w, h); break;
		case DC_MSG_VIDEO: msg_id = (jint)dc_send_video_msg(get_dc_context(env, cls), chat_id, filePtr, mimePtr, w, h, ms); break;
		case DC_MSG_VOICE: msg_id = (jint)dc_send_voice_msg(get_dc_context(env, cls), chat_id, filePtr, mimePtr, ms); break;
		case DC_MSG_AUDIO: msg_id = (jint)dc_send_audio_msg(get_dc_context(env, cls), chat_id, filePtr, mimePtr, ms, authorPtr, tracknamePtr); break;
		default:           msg_id = (jint)dc_send_file_msg (get_dc_context(env, cls), chat_id, filePtr, mimePtr); break;
	}
	CHAR_UNREF(trackname);
	CHAR_UNREF(author);
	CHAR_UNREF(mime);
	CHAR_UNREF(file);
	return msg_id;
}


/* DcContext - handle config */

JNIEXPORT void Java_com_b44t_messenger_DcContext_setConfig(JNIEnv *env, jclass cls, jstring key, jstring value /*may be NULL*/)
{
	CHAR_REF(key);
	CHAR_REF(value);
		dc_set_config(get_dc_context(env, cls), keyPtr, valuePtr /*is NULL if value is NULL, CHAR_REF() handles this*/);
	CHAR_UNREF(key);
	CHAR_UNREF(value);
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_setConfigInt(JNIEnv *env, jclass cls, jstring key, jint value)
{
	CHAR_REF(key);
		dc_set_config_int(get_dc_context(env, cls), keyPtr, value);
	CHAR_UNREF(key);
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContext_getConfig(JNIEnv *env, jclass cls, jstring key, jstring def/*may be NULL*/)
{
	CHAR_REF(key);
	CHAR_REF(def);
		char* temp = dc_get_config(get_dc_context(env, cls), keyPtr, defPtr /*is NULL if value is NULL, CHAR_REF() handles this*/);
			jstring ret = NULL;
			if( temp ) {
				ret = JSTRING_NEW(temp);
			}
		free(temp);
	CHAR_UNREF(key);
	CHAR_UNREF(def);
	return ret; /* returns NULL only if key is unset and "def" is NULL */
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_getConfigInt(JNIEnv *env, jclass cls, jstring key, jint def)
{
	CHAR_REF(key);
		jint ret = dc_get_config_int(get_dc_context(env, cls), keyPtr, def);
	CHAR_UNREF(key);
	return ret;
}


/* DcContext - out-of-band verification */

JNIEXPORT jlong Java_com_b44t_messenger_DcContext_checkQrCPtr(JNIEnv *env, jclass cls, jstring qr)
{
	CHAR_REF(qr);
		jlong ret = (jlong)dc_check_qr(get_dc_context(env, cls), qrPtr);
	CHAR_UNREF(qr);
	return ret;
}

JNIEXPORT jstring Java_com_b44t_messenger_DcContext_getSecurejoinQr(JNIEnv *env, jclass cls, jint chat_id)
{
	char* temp = dc_get_securejoin_qr(get_dc_context(env, cls), chat_id);
		jstring ret = JSTRING_NEW(temp);
	free(temp);
	return ret;
}

JNIEXPORT jint Java_com_b44t_messenger_DcContext_joinSecurejoin(JNIEnv *env, jclass cls, jstring qr)
{
	CHAR_REF(qr);
		jint ret = dc_join_securejoin(get_dc_context(env, cls), qrPtr);
	CHAR_UNREF(qr);
	return ret;
}


/* DcContext - misc. */

JNIEXPORT jstring Java_com_b44t_messenger_DcContext_getInfo(JNIEnv *env, jclass cls)
{
	char* temp = dc_get_info(get_dc_context(env, cls));
		jstring ret = JSTRING_NEW(temp);
	free(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContext_getContactEncrInfo(JNIEnv *env, jclass cls, jint contact_id)
{
	char* temp = dc_get_contact_encrinfo(get_dc_context(env, cls), contact_id);
		jstring ret = JSTRING_NEW(temp);
	free(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContext_cmdline(JNIEnv *env, jclass cls, jstring cmd)
{
	CHAR_REF(cmd);
		char* temp = dc_cmdline(get_dc_context(env, cls), cmdPtr);
			jstring ret = JSTRING_NEW(temp);
		free(temp);
	CHAR_UNREF(cmd);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContext_initiateKeyTransfer(JNIEnv *env, jclass cls)
{
	jstring setup_code = NULL;
	char* temp = dc_initiate_key_transfer(get_dc_context(env, cls));
	if( temp ) {
		setup_code = JSTRING_NEW(temp);
		free(temp);
	}
	return setup_code;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcContext_continueKeyTransfer(JNIEnv *env, jclass cls, jint msg_id, jstring setupCode)
{
	CHAR_REF(setupCode);
		jboolean ret = dc_continue_key_transfer(get_dc_context(env, cls), msg_id, setupCodePtr);
	CHAR_UNREF(setupCode);
	return ret;
}


JNIEXPORT void Java_com_b44t_messenger_DcContext_imex(JNIEnv *env, jclass cls, jint what, jstring dir)
{
	CHAR_REF(dir);
		dc_imex(get_dc_context(env, cls), what, dirPtr, "");
	CHAR_UNREF(dir);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_checkPassword(JNIEnv *env, jclass cls, jstring pw)
{
	CHAR_REF(pw);
		jint r = dc_check_password(get_dc_context(env, cls),  pwPtr);
	CHAR_UNREF(pw);
	return r;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContext_imexHasBackup(JNIEnv *env, jclass cls, jstring dir)
{
	CHAR_REF(dir);
		jstring ret = NULL;
		char* temp = dc_imex_has_backup(get_dc_context(env, cls),  dirPtr);
		if( temp ) {
			ret = JSTRING_NEW(temp);
			free(temp);
		}
	CHAR_UNREF(dir);
	return ret; /* may be NULL! */
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_addAddressBook(JNIEnv *env, jclass cls, jstring adrbook)
{
	CHAR_REF(adrbook);
		int modify_count = dc_add_address_book(get_dc_context(env, cls), adrbookPtr);
	CHAR_UNREF(adrbook);
	return modify_count;
}



/*******************************************************************************
 * DcChatlist
 ******************************************************************************/


static dc_chatlist_t* get_dc_chatlist(JNIEnv *env, jobject obj)
{
	static jfieldID fid = 0;
	if( fid == 0 ) {
		jclass cls = (*env)->GetObjectClass(env, obj);
		fid = (*env)->GetFieldID(env, cls, "m_hChatlist", "J" /*Signature, J=long*/);
	}
	if( fid ) {
		return (dc_chatlist_t*)(*env)->GetLongField(env, obj, fid);
	}
	return NULL;
}


JNIEXPORT void Java_com_b44t_messenger_DcChatlist_DcChatlistUnref(JNIEnv *env, jclass c, jlong hChatlist)
{
	dc_chatlist_unref((dc_chatlist_t*)hChatlist);
}


JNIEXPORT jint Java_com_b44t_messenger_DcChatlist_getCnt(JNIEnv *env, jobject obj, jlong hChatlist)
{
	return dc_chatlist_get_cnt(get_dc_chatlist(env, obj));
}


JNIEXPORT jlong Java_com_b44t_messenger_DcChatlist_DcChatlistGetChatByIndex(JNIEnv *env, jclass c, jlong hChatlist, jint index)
{
	dc_chatlist_t* chatlist = (dc_chatlist_t*)hChatlist;
	return (jlong)dc_get_chat(dc_chatlist_get_context(chatlist), dc_chatlist_get_chat_id(chatlist, index));
}


JNIEXPORT jlong Java_com_b44t_messenger_DcChatlist_DcChatlistGetMsgByIndex(JNIEnv *env, jclass c, jlong hChatlist, jint index)
{
	dc_chatlist_t* chatlist = (dc_chatlist_t*)hChatlist;
	return (jlong)dc_get_msg(dc_chatlist_get_context(chatlist), dc_chatlist_get_msg_id(chatlist, index));
}


JNIEXPORT jlong Java_com_b44t_messenger_DcChatlist_DcChatlistGetSummaryByIndex(JNIEnv *env, jclass c, jlong hChatlist, jint index, jlong hChat)
{
	return (jlong)dc_chatlist_get_summary((dc_chatlist_t*)hChatlist, index, (dc_chat_t*)hChat);
}


/*******************************************************************************
 * DcChat
 ******************************************************************************/


static dc_chat_t* get_dc_chat(JNIEnv *env, jobject obj)
{
	static jfieldID fid = 0;
	if( fid == 0 ) {
		jclass cls = (*env)->GetObjectClass(env, obj);
		fid = (*env)->GetFieldID(env, cls, "m_hChat", "J" /*Signature, J=long*/);
	}
	if( fid ) {
		return (dc_chat_t*)(*env)->GetLongField(env, obj, fid);
	}
	return NULL;
}


JNIEXPORT void Java_com_b44t_messenger_DcChat_DcChatUnref(JNIEnv *env, jclass c, jlong hChat)
{
	dc_chat_unref((dc_chat_t*)hChat);
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


JNIEXPORT jint Java_com_b44t_messenger_DcChat_getArchived(JNIEnv *env, jobject obj)
{
	return dc_chat_get_archived(get_dc_chat(env, obj));
}


JNIEXPORT jstring Java_com_b44t_messenger_DcChat_getName(JNIEnv *env, jobject obj)
{
	const char* temp = dc_chat_get_name(get_dc_chat(env, obj));
		jstring ret = JSTRING_NEW(temp);
	free(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcChat_getSubtitle(JNIEnv *env, jobject obj)
{
	const char* temp = dc_chat_get_subtitle(get_dc_chat(env, obj));
		jstring ret = JSTRING_NEW(temp);
	free(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcChat_getProfileImage(JNIEnv *env, jobject obj)
{
	const char* temp = dc_chat_get_profile_image(get_dc_chat(env, obj));
		jstring ret = JSTRING_NEW(temp);
	free(temp);
	return ret;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcChat_isUnpromoted(JNIEnv *env, jobject obj)
{
	return dc_chat_is_unpromoted(get_dc_chat(env, obj)) != 0;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcChat_isSelfTalk(JNIEnv *env, jobject obj)
{
	return dc_chat_is_self_talk(get_dc_chat(env, obj)) != 0;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcChat_isVerified(JNIEnv *env, jobject obj)
{
	return dc_chat_is_verified(get_dc_chat(env, obj)) != 0;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcChat_getDraft(JNIEnv *env, jobject obj) /* returns NULL for "no draft" */
{
	const char* temp = dc_chat_get_text_draft(get_dc_chat(env, obj));
		jstring ret = temp? JSTRING_NEW(temp) : NULL;
	free(temp);
	return ret;
}


JNIEXPORT jlong Java_com_b44t_messenger_DcChat_getDraftTimestamp(JNIEnv *env, jobject obj)
{
	return (jlong)dc_chat_get_draft_timestamp(get_dc_chat(env, obj));
}


JNIEXPORT jintArray Java_com_b44t_messenger_DcContext_getChatMedia(JNIEnv *env, jclass cls, jint chat_id, jint msg_type, jint or_msg_type)
{
	dc_array_t* ca = dc_get_chat_media(get_dc_context(env, cls), chat_id, msg_type, or_msg_type);
	return dc_array2jintArray_n_unref(env, ca);
}


JNIEXPORT jint Java_com_b44t_messenger_DcContext_getNextMedia(JNIEnv *env, jclass cls, jint msg_id, jint dir)
{
	return dc_get_next_media(get_dc_context(env, cls), msg_id, dir);
}


JNIEXPORT jintArray Java_com_b44t_messenger_DcContext_getChatMsgs(JNIEnv *env, jclass cls, jint chat_id, jint flags, jint marker1before)
{
	dc_array_t* ca = dc_get_chat_msgs(get_dc_context(env, cls), chat_id, flags, marker1before);
	return dc_array2jintArray_n_unref(env, ca);
}


JNIEXPORT jintArray Java_com_b44t_messenger_DcContext_searchMsgs(JNIEnv *env, jclass cls, jint chat_id, jstring query)
{
	CHAR_REF(query);
		dc_array_t* ca = dc_search_msgs(get_dc_context(env, cls), chat_id, queryPtr);
	CHAR_UNREF(query);
	return dc_array2jintArray_n_unref(env, ca);
}


JNIEXPORT jintArray Java_com_b44t_messenger_DcContext_getFreshMsgs(JNIEnv *env, jclass cls)
{
	dc_array_t* ca = dc_get_fresh_msgs(get_dc_context(env, cls));
	return dc_array2jintArray_n_unref(env, ca);
}


JNIEXPORT jintArray Java_com_b44t_messenger_DcContext_getChatContacts(JNIEnv *env, jclass cls, jint chat_id)
{
	dc_array_t* ca = dc_get_chat_contacts(get_dc_context(env, cls), chat_id);
	return dc_array2jintArray_n_unref(env, ca);
}


/*******************************************************************************
 * DcMsg
 ******************************************************************************/


static dc_msg_t* get_dc_msg(JNIEnv *env, jobject obj)
{
	static jfieldID fid = 0;
	if( fid == 0 ) {
		jclass cls = (*env)->GetObjectClass(env, obj);
		fid = (*env)->GetFieldID(env, cls, "m_hMsg", "J" /*Signature, J=long*/);
	}
	if( fid ) {
		return (dc_msg_t*)(*env)->GetLongField(env, obj, fid);
	}
	return NULL;
}


JNIEXPORT void Java_com_b44t_messenger_DcMsg_DcMsgUnref(JNIEnv *env, jclass c, jlong hMsg)
{
	dc_msg_unref((dc_msg_t*)hMsg);
}


JNIEXPORT jint Java_com_b44t_messenger_DcMsg_getId(JNIEnv *env, jobject obj)
{
	return dc_msg_get_id(get_dc_msg(env, obj));
}


JNIEXPORT jstring Java_com_b44t_messenger_DcMsg_getText(JNIEnv *env, jobject obj)
{
	char* temp = dc_msg_get_text(get_dc_msg(env, obj));
		jstring ret = JSTRING_NEW(temp);
	free(temp);
	return ret;
}


JNIEXPORT jlong Java_com_b44t_messenger_DcMsg_getTimestamp(JNIEnv *env, jobject obj)
{
	return (jlong)dc_msg_get_timestamp(get_dc_msg(env, obj));
}


JNIEXPORT jint Java_com_b44t_messenger_DcMsg_getType(JNIEnv *env, jobject obj)
{
	return dc_msg_get_type(get_dc_msg(env, obj));
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


JNIEXPORT jint Java_com_b44t_messenger_DcMsg_getBytes(JNIEnv *env, jobject obj)
{
	return (jint)dc_msg_get_filebytes(get_dc_msg(env, obj));
}


JNIEXPORT jlong Java_com_b44t_messenger_DcMsg_getSummaryCPtr(JNIEnv *env, jobject obj, jlong hChat)
{
	return (jlong)dc_msg_get_summary(get_dc_msg(env, obj), (dc_chat_t*)hChat);
}


JNIEXPORT jstring Java_com_b44t_messenger_DcMsg_getSummarytext(JNIEnv *env, jobject obj, jint approx_characters)
{
	char* temp = dc_msg_get_summarytext(get_dc_msg(env, obj), approx_characters);
		jstring ret = JSTRING_NEW(temp);
	free(temp);
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
	free(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcMsg_getFilemime(JNIEnv *env, jobject obj)
{
	char* temp = dc_msg_get_filemime(get_dc_msg(env, obj));
		jstring ret =  JSTRING_NEW(temp);
	free(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcMsg_getFilename(JNIEnv *env, jobject obj)
{
	char* temp = dc_msg_get_filename(get_dc_msg(env, obj));
		jstring ret =  JSTRING_NEW(temp);
	free(temp);
	return ret;
}


JNIEXPORT jlong Java_com_b44t_messenger_DcMsg_getMediainfoCPtr(JNIEnv *env, jobject obj)
{
	return (jlong)dc_msg_get_mediainfo(get_dc_msg(env, obj));
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcMsg_isForwarded(JNIEnv *env, jobject obj)
{
    return dc_msg_is_forwarded(get_dc_msg(env, obj)) != 0;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcMsg_isIncreation(JNIEnv *env, jobject obj)
{
    return dc_msg_is_increation(get_dc_msg(env, obj)) != 0;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcMsg_isInfo(JNIEnv *env, jobject obj)
{
    return dc_msg_is_info(get_dc_msg(env, obj)) != 0;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcMsg_isSetupMessage(JNIEnv *env, jobject obj)
{
    return dc_msg_is_setupmessage(get_dc_msg(env, obj));
}


JNIEXPORT jstring Java_com_b44t_messenger_DcMsg_getSetupCodeBegin(JNIEnv *env, jobject obj)
{
	char* temp = dc_msg_get_setupcodebegin(get_dc_msg(env, obj));
		jstring ret =  JSTRING_NEW(temp);
	free(temp);
	return ret;
}



/*******************************************************************************
 * DcContact
 ******************************************************************************/


static dc_contact_t* get_dc_contact(JNIEnv *env, jobject obj)
{
	static jfieldID fid = 0;
	if( fid == 0 ) {
		jclass cls = (*env)->GetObjectClass(env, obj);
		fid = (*env)->GetFieldID(env, cls, "m_hContact", "J" /*Signature, J=long*/);
	}
	if( fid ) {
		return (dc_contact_t*)(*env)->GetLongField(env, obj, fid);
	}
	return NULL;
}


JNIEXPORT void Java_com_b44t_messenger_DcContact_DcContactUnref(JNIEnv *env, jclass c, jlong hContact)
{
	dc_contact_unref((dc_contact_t*)hContact);
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContact_getName(JNIEnv *env, jobject obj)
{
	const char* temp = dc_contact_get_name(get_dc_contact(env, obj));
		jstring ret = JSTRING_NEW(temp);
	free(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContact_getDisplayName(JNIEnv *env, jobject obj)
{
	const char* temp = dc_contact_get_display_name(get_dc_contact(env, obj));
		jstring ret = JSTRING_NEW(temp);
	free(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContact_getFirstName(JNIEnv *env, jobject obj)
{
	const char* temp = dc_contact_get_first_name(get_dc_contact(env, obj));
		jstring ret = JSTRING_NEW(temp);
	free(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContact_getAddr(JNIEnv *env, jobject obj)
{
	const char* temp = dc_contact_get_addr(get_dc_contact(env, obj));
		jstring ret = JSTRING_NEW(temp);
	free(temp);
	return ret;
}


JNIEXPORT jstring Java_com_b44t_messenger_DcContact_getNameNAddr(JNIEnv *env, jobject obj)
{
	const char* temp = dc_contact_get_name_n_addr(get_dc_contact(env, obj));
		jstring ret = JSTRING_NEW(temp);
	free(temp);
	return ret;
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcContact_isBlocked(JNIEnv *env, jobject obj)
{
	return (jboolean)( dc_contact_is_blocked(get_dc_contact(env, obj)) != 0 );
}


JNIEXPORT jboolean Java_com_b44t_messenger_DcContact_isVerified(JNIEnv *env, jobject obj)
{
	return dc_contact_is_verified(get_dc_contact(env, obj)) == 2;
}


/*******************************************************************************
 * DcLot
 ******************************************************************************/


static dc_lot_t* get_dc_lot(JNIEnv *env, jobject obj)
{
	static jfieldID fid = 0;
	if( fid == 0 ) {
		jclass cls = (*env)->GetObjectClass(env, obj);
		fid = (*env)->GetFieldID(env, cls, "m_hLot", "J" /*Signature, J=long*/);
	}
	if( fid ) {
		return (dc_lot_t*)(*env)->GetLongField(env, obj, fid);
	}
	return NULL;
}


JNIEXPORT void Java_com_b44t_messenger_DcLot_unref(JNIEnv *env, jclass cls, jlong hLot)
{
	dc_lot_unref((dc_lot_t*)hLot);
}


JNIEXPORT jstring Java_com_b44t_messenger_DcLot_getText1(JNIEnv *env, jobject obj)
{
	char* temp = dc_lot_get_text1(get_dc_lot(env, obj));
		jstring ret = JSTRING_NEW(temp);
	free(temp);
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
	free(temp);
	return ret;
}


JNIEXPORT jlong Java_com_b44t_messenger_DcLot_getTimestamp(JNIEnv *env, jobject obj)
{
	return dc_lot_get_timestamp(get_dc_lot(env, obj));
}


JNIEXPORT jint Java_com_b44t_messenger_DcLot_getState(JNIEnv *env, jobject obj)
{
	return dc_lot_get_state(get_dc_lot(env, obj));
}


JNIEXPORT jint Java_com_b44t_messenger_DcLot_getId(JNIEnv *env, jobject obj)
{
	return dc_lot_get_id(get_dc_lot(env, obj));
}


JNIEXPORT void Java_com_b44t_messenger_DcLot_DcLotUnref(JNIEnv *env, jclass c, jlong hLot)
{
	dc_lot_unref((dc_lot_t*)hLot);
}


/*******************************************************************************
 * Tools
 ******************************************************************************/


JNIEXPORT jstring Java_com_b44t_messenger_DcContext_CPtr2String(JNIEnv *env, jclass c, jlong hStr)
{
	/* the callback may return a long that represents a pointer to a C-String; this function creates a Java-string from such values. */
	if( hStr == 0 ) {
		return NULL;
	}
	const char* ptr = (const char*)hStr;
	return JSTRING_NEW(ptr);
}


JNIEXPORT jlong Java_com_b44t_messenger_DcContext_String2CPtr(JNIEnv *env, jclass c, jstring str)
{
    char* hStr = NULL;
    if( str ) {
        CHAR_REF(str);
            hStr = strdup(strPtr);
        CHAR_UNREF(str);
    }
    return (jlong)hStr;
}



