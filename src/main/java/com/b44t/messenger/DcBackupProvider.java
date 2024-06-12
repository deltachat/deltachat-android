package com.b44t.messenger;

public class DcBackupProvider {

    public DcBackupProvider(long backupProviderCPtr) {
        this.backupProviderCPtr = backupProviderCPtr;
    }

    public boolean isOk() {
      return backupProviderCPtr != 0;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        unref();
    }

    public void unref() {
        if (backupProviderCPtr != 0) {
            unrefBackupProviderCPtr();
            backupProviderCPtr = 0;
        }
    }

    public native String    getQr           ();
    public native String    getQrSvg        ();
    public native void      waitForReceiver ();

    // working with raw c-data
    private long        backupProviderCPtr;    // CAVE: the name is referenced in the JNI
    private native void unrefBackupProviderCPtr();
}
