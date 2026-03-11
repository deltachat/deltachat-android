package com.b44t.messenger;

public class DcProvider {

  public static final int DC_PROVIDER_STATUS_OK = 1;
  public static final int DC_PROVIDER_STATUS_PREPARATION = 2;
  public static final int DC_PROVIDER_STATUS_BROKEN = 3;

  public DcProvider(long providerCPtr) {
    this.providerCPtr = providerCPtr;
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    unrefProviderCPtr();
    providerCPtr = 0;
  }

  public native int getStatus();

  public native String getBeforeLoginHint();

  public native String getOverviewPage();

  // working with raw c-data
  private long providerCPtr; // CAVE: the name is referenced in the JNI

  private native void unrefProviderCPtr();
}
