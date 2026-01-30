package com.b44t.messenger;

public class DcEventChannel {

  public DcEventChannel() {
    eventChannelCPtr = createEventChannelCPtr();
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    if (eventChannelCPtr != 0) {
      unrefEventChannelCPtr();
      eventChannelCPtr = 0;
    }
  }

  public DcEventEmitter getEventEmitter() {
    return new DcEventEmitter(getEventEmitterCPtr());
  }

  // working with raw c-data
  private long         eventChannelCPtr;      // CAVE: the name is referenced in the JNI
  private native long  createEventChannelCPtr ();
  private native void  unrefEventChannelCPtr  ();
  private native long  getEventEmitterCPtr    ();
}
