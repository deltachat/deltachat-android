package com.b44t.messenger;

public class DcAccountsEventEmitter {

    public DcAccountsEventEmitter(long accountsEventEmitterCPtr) {
        this.accountsEventEmitterCPtr = accountsEventEmitterCPtr;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        unrefAccountsEventEmitterCPtr();
        accountsEventEmitterCPtr = 0;
    }

    public DcEvent getNextEvent () {
        long eventCPtr = getNextEventCPtr();
        return eventCPtr == 0 ? null : new DcEvent(eventCPtr);
    }

    // working with raw c-data
    private long        accountsEventEmitterCPtr;     // CAVE: the name is referenced in the JNI
    private native long getNextEventCPtr              ();
    private native void unrefAccountsEventEmitterCPtr ();
}
