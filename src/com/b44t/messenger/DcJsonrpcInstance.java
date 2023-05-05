package com.b44t.messenger;

public class DcJsonrpcInstance {

    public DcJsonrpcInstance(long jsonrpcInstanceCPtr) {
        this.jsonrpcInstanceCPtr = jsonrpcInstanceCPtr;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        unrefJsonrpcInstanceCPtr();
        jsonrpcInstanceCPtr = 0;
    }

    public native void request(String request);
    public native String getNextResponse();

    // working with raw c-data
    private long        jsonrpcInstanceCPtr;    // CAVE: the name is referenced in the JNI
    private native void unrefJsonrpcInstanceCPtr();
}
