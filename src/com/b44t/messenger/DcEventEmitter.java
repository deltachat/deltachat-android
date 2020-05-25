package com.b44t.messenger;

public class DcEventEmitter {

    public DcEventEmitter(long eventEmitterCPtr) {
        this.eventEmitterCPtr = eventEmitterCPtr;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        unrefEventEmitterCPtr();;
        eventEmitterCPtr = 0;
    }

    public DcEvent getNextEvent () { return new DcEvent(getNextEventCPtr()); }

    // working with raw c-data
    private long        eventEmitterCPtr;    // CAVE: the name is referenced in the JNI
    private native long getNextEventCPtr     ();
    private native void unrefEventEmitterCPtr();
}
