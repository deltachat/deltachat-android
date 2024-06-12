package com.b44t.messenger;

public class DcLot {

    public final static int DC_TEXT1_DRAFT    = 1;
    public final static int DC_TEXT1_USERNAME = 2;
    public final static int DC_TEXT1_SELF     = 3;

    public DcLot(long lotCPtr) {
        this.lotCPtr = lotCPtr;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        unrefLotCPtr();
        lotCPtr = 0;
    }

    public native String getText1       ();
    public native int    getText1Meaning();
    public native String getText2       ();
    public native long   getTimestamp   ();
    public native int    getState       ();
    public native int    getId          ();

    // working with raw c-data
    private long        lotCPtr;    // CAVE: the name is referenced in the JNI
    private native void unrefLotCPtr();
}
