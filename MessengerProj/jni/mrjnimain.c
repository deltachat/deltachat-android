/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *                        (C) 2013-2016 Nikolai Kudashov
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


#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <sys/types.h>
#include <inttypes.h>
#include <stdlib.h>
#include <openssl/aes.h>
#include <unistd.h>
#include "mrjnimain.h"
#include "image.h"
#include "gifvideo.h"


jint JNI_OnLoad(JavaVM *vm, void *reserved) { /* this function is called automatically by the JNI when the library gets loaded */
	JNIEnv *env = 0;
    srand(time(NULL));

	__android_log_print(ANDROID_LOG_INFO, "DeltaChat", "JNI_OnLoad() ..."); /* please note, that __android_log_print() may not work (eg. on LG X Cam), however, we don't have an option here. */
    
	if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
		return -1;
	}
    
    if (imageOnJNILoad(vm, reserved, env) == -1) {
        return -1;
    }
    
    if (gifvideoOnJNILoad(vm, env) == -1) {
        return -1;
    }

	__android_log_print(ANDROID_LOG_INFO, "DeltaChat", "JNI_OnLoad() succeeded.");

	return JNI_VERSION_1_6;
}


void JNI_OnUnload(JavaVM *vm, void *reserved) {
}


JNIEXPORT jstring Java_com_b44t_messenger_Utilities_readlink(JNIEnv *env, jclass class, jstring path) {
    static char buf[1000];
    char *fileName = (*env)->GetStringUTFChars(env, path, NULL);
    int result = readlink(fileName, buf, 999);
    jstring value = 0;
    if (result != -1) {
        buf[result] = '\0';
        value = (*env)->NewStringUTF(env, buf);
    }
    (*env)->ReleaseStringUTFChars(env, path, fileName);
    return value;
}

