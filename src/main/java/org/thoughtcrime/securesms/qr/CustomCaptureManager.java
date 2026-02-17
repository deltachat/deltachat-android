package org.thoughtcrime.securesms.qr;

import android.app.Activity;

import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class CustomCaptureManager extends CaptureManager {

  private OnResultInterceptor interceptor;

  public CustomCaptureManager(Activity activity, DecoratedBarcodeView barcodeView) {
    super(activity, barcodeView);
  }

  public void setResultInterceptor(OnResultInterceptor interceptor) {
    this.interceptor = interceptor;
  }

  @Override
  protected void returnResult(BarcodeResult rawResult) {
    if (interceptor != null) {
      interceptor.onResult(rawResult, () -> {
        super.returnResult(rawResult);
      });
    } else {
      super.returnResult(rawResult);
    }
  }

  public interface OnResultInterceptor {
    void onResult(BarcodeResult result, Runnable finishCallback);
  }
}
