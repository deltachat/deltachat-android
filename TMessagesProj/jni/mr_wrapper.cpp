
#include <jni.h>
#include "../../../messenger-backend/src/mrmailbox.h"


JNIEXPORT int Java_org_telegram_messenger_MrMailbox_MrMailboxNew(JNIEnv *env, jclass c)
{
	return (int)mrmailbox_new();
}


JNIEXPORT int Java_org_telegram_messenger_MrMailbox_MrMailboxDelete(JNIEnv *env, jclass c, int hMailbox)
{
	mrmailbox_delete((mrmailbox_t*)hMailbox);
}


JNIEXPORT int Java_org_telegram_messenger_MrMailbox_MrMailboxOpen(JNIEnv *env, jclass c, int hMailbox, jstring dbfile)
{
	const char* dbfilePtr = env->GetStringUTFChars(dbfile, 0);
	if( dbfilePtr == NULL ) { return 0; }
		int ret = mrmailbox_open((mrmailbox_t*)hMailbox, dbfilePtr);
	env->ReleaseStringUTFChars(dbfile, dbfilePtr);
	return ret;
}


JNIEXPORT void Java_org_telegram_messenger_MrMailbox_MrMailboxClose(JNIEnv *env, jclass c, int hMailbox)
{
	mrmailbox_close((mrmailbox_t*)hMailbox);
}

