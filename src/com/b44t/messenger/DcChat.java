package com.b44t.messenger;

public class DcChat {

    public static final int DC_CHAT_TYPE_UNDEFINED   = 0;
    public static final int DC_CHAT_TYPE_SINGLE      = 100;
    public static final int DC_CHAT_TYPE_GROUP       = 120;
    public static final int DC_CHAT_TYPE_MAILINGLIST = 140;
    public static final int DC_CHAT_TYPE_BROADCAST   = 160;

    public static final int DC_CHAT_NO_CHAT          = 0;
    public final static int DC_CHAT_ID_ARCHIVED_LINK = 6;
    public final static int DC_CHAT_ID_ALLDONE_HINT  = 7;
    public final static int DC_CHAT_ID_LAST_SPECIAL  = 9;

    public final static int DC_CHAT_VISIBILITY_NORMAL   = 0;
    public final static int DC_CHAT_VISIBILITY_ARCHIVED = 1;
    public final static int DC_CHAT_VISIBILITY_PINNED   = 2;

    public DcChat(long chatCPtr) {
        this.chatCPtr = chatCPtr;
    }

  @Override protected void finalize() throws Throwable {
        super.finalize();
        unrefChatCPtr();
        chatCPtr = 0;
    }

    public native int     getId             ();
    public native int     getType           ();
    public native int     getVisibility     ();
    public native String  getName           ();
    public native String  getProfileImage   ();
    public native int     getColor          ();
    public native boolean isUnpromoted      ();
    public native boolean isSelfTalk        ();
    public native boolean isDeviceTalk      ();
    public native boolean canSend           ();
    public native boolean isProtected       ();
    public native boolean isSendingLocations();
    public native boolean isMuted           ();
    public native boolean isContactRequest  ();


    // aliases and higher-level tools

    public boolean isGroup() {
      // isMultiUser() might fit better,
      // however, would result in lots of code changes, so we leave this as is for now.
      int type = getType();
      return type == DC_CHAT_TYPE_GROUP || type == DC_CHAT_TYPE_MAILINGLIST;
    }

    public boolean isMailingList() {
        return getType() == DC_CHAT_TYPE_MAILINGLIST;
    }

    public boolean canVideochat() {
        return canSend() && !isSelfTalk();
    }


    // working with raw c-data

    private long        chatCPtr;    // CAVE: the name is referenced in the JNI
    private native void unrefChatCPtr();
    public long         getChatCPtr  () { return chatCPtr; }

}
