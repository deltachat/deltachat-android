package com.b44t.messenger;

public class DcEvent {

    public DcEvent(long eventCPtr) {
        this.eventCPtr = eventCPtr;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        unrefEventCPtr();
        eventCPtr = 0;
    }

    public native int    getId       ();
    public native int    getData1Int ();
    public native int    getData2Int ();
    public native String getData2Str ();
    public native byte[] getData2Blob();
    public native int    getAccountId();

    // working with raw c-data
    private long        eventCPtr;    // CAVE: the name is referenced in the JNI
    private native void unrefEventCPtr();
}
