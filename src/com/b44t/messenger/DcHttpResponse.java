package com.b44t.messenger;

public class DcHttpResponse {

    public DcHttpResponse(long httpResponseCPtr) {
        this.httpResponseCPtr = httpResponseCPtr;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        unrefHttpResponseCPtr();
        httpResponseCPtr = 0;
    }

    public native String    getMimetype     ();
    public native String    getEncoding     ();
    public native byte[]    getBlob         ();

    // working with raw c-data
    private long        httpResponseCPtr;    // CAVE: the name is referenced in the JNI
    private native void unrefHttpResponseCPtr();
}
