package com.b44t.messenger;

public class DcProvider {

    public final static int DC_PROVIDER_STATUS_OK          = 1;
    public final static int DC_PROVIDER_STATUS_PREPARATION = 2;
    public final static int DC_PROVIDER_STATUS_BROKEN      = 3;

    public DcProvider(long providerCPtr) {
        this.providerCPtr = providerCPtr;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        unrefProviderCPtr();
        providerCPtr = 0;
    }

    public native String    getName        ();
    public native int       getStatus      ();
    public native String    getStatusDate  ();
    public native String    getMarkdown    ();
    public native String    getOverviewPage();

    // working with raw c-data
    private long        providerCPtr;    // CAVE: the name is referenced in the JNI
    private native void unrefProviderCPtr();
}
