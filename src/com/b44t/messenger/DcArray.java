package com.b44t.messenger;

public class DcArray {

    public DcArray(long arrayCPtr) {
        this.arrayCPtr = arrayCPtr;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        unrefArrayCPtr();
        arrayCPtr = 0;
    }

    public native int       getCnt       ();
    public native float     getLatitude  (int index);
    public native float     getLongitude (int index);
    public native float     getAccuracy  (int index);
    public native long      getTimestamp (int index);
    public native int       getMsgId     (int index);
    public native int       getLocationId(int index);
    public native String    getMarker    (int index);

    // working with raw c-data
    private long        arrayCPtr;    // CAVE: the name is referenced in the JNI
    private native void unrefArrayCPtr();
}
