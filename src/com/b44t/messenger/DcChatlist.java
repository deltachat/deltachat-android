package com.b44t.messenger;

public class DcChatlist {

    private int accountId;

    public DcChatlist(int accountId, long chatlistCPtr) {
        this.accountId = accountId;
        this.chatlistCPtr = chatlistCPtr;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        unrefChatlistCPtr();
        chatlistCPtr = 0;
    }

    public int              getAccountId() { return accountId; }
    public native int       getCnt    ();
    public native int       getChatId (int index);
    public DcChat           getChat   (int index) { return new DcChat(accountId, getChatCPtr(index)); }
    public native int       getMsgId  (int index);
    public DcMsg            getMsg    (int index) { return new DcMsg(getMsgCPtr(index)); }
    public DcLot            getSummary(int index, DcChat chat) { return new DcLot(getSummaryCPtr(index, chat==null? 0 : chat.getChatCPtr())); }

    public class Item {
        public DcLot summary;
        public int   msgId;
        public int   chatId;
    }

    public Item getItem(int index) {
        Item item = new Item();
        item.summary = getSummary(index, null);
        item.msgId   = getMsgId(index);
        item.chatId  = getChatId(index);
        return item;
    }

    // working with raw c-data
    private long        chatlistCPtr;    // CAVE: the name is referenced in the JNI
    private native void unrefChatlistCPtr();
    private native long getChatCPtr      (int index);
    private native long getMsgCPtr       (int index);
    private native long getSummaryCPtr   (int index, long chatCPtr);
}
