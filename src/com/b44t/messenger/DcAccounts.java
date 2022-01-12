package com.b44t.messenger;

public class DcAccounts {

    public DcAccounts(String osName, String dir) {
        accountsCPtr = createAccountsCPtr(osName, dir);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        unref();
    }

    public void unref() {
        if (accountsCPtr != 0) {
            unrefAccountsCPtr();
            accountsCPtr = 0;
        }
    }

    public DcAccountsEventEmitter getEventEmitter      () { return new DcAccountsEventEmitter(getEventEmitterCPtr()); }
    public native void            startIo              ();
    public native void            stopIo               ();
    public native void            maybeNetwork         ();

    public native int             addAccount           ();
    public native int             addClosedAccount     ();
    public native int             migrateAccount       (String dbfile);
    public native boolean         removeAccount        (int accountId);
    public native int[]           getAll               ();
    public DcContext              getAccount           (int accountId) { return new DcContext(getAccountCPtr(accountId)); }
    public DcContext              getSelectedAccount   () { return new DcContext(getSelectedAccountCPtr()); }
    public native boolean         selectAccount        (int accountId);

    // working with raw c-data
    private long         accountsCPtr;          // CAVE: the name is referenced in the JNI
    private native long  createAccountsCPtr     (String osName, String dir);
    private native void  unrefAccountsCPtr      ();
    private native long  getEventEmitterCPtr    ();
    private native long  getAccountCPtr         (int accountId);
    private native long  getSelectedAccountCPtr ();
}
