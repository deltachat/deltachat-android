package com.b44t.messenger;

public class DcReactions {

    public DcReactions(long reactionsCPtr) {
        this.reactionsCPtr = reactionsCPtr;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        unrefReactionsCPtr();
        reactionsCPtr = 0;
    }

    public native int[]     getContacts     ();
    public native String    getByContactId  (int contact_id);

    // working with raw c-data
    private long        reactionsCPtr;    // CAVE: the name is referenced in the JNI
    private native void unrefReactionsCPtr();
};
