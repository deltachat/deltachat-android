package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageButton;
import org.thoughtcrime.securesms.TransportOption;
import org.thoughtcrime.securesms.TransportOptions;
import org.thoughtcrime.securesms.TransportOptions.OnTransportChangedListener;
import org.thoughtcrime.securesms.util.ViewUtil;

public class SendButton extends AppCompatImageButton
    implements TransportOptions.OnTransportChangedListener {

  private final TransportOptions transportOptions;

  public SendButton(Context context) {
    super(context);
    this.transportOptions = initializeTransportOptions();
    ViewUtil.mirrorIfRtl(this, getContext());
  }

  public SendButton(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.transportOptions = initializeTransportOptions();
    ViewUtil.mirrorIfRtl(this, getContext());
  }

  public SendButton(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    this.transportOptions = initializeTransportOptions();
    ViewUtil.mirrorIfRtl(this, getContext());
  }

  private TransportOptions initializeTransportOptions() {
    TransportOptions transportOptions = new TransportOptions(getContext());
    transportOptions.addOnTransportChangedListener(this);
    return transportOptions;
  }

  public void addOnTransportChangedListener(OnTransportChangedListener listener) {
    transportOptions.addOnTransportChangedListener(listener);
  }

  public TransportOption getSelectedTransport() {
    return transportOptions.getSelectedTransport();
  }

  public void resetAvailableTransports() {
    transportOptions.reset();
  }

  public void setDefaultTransport(TransportOption.Type type) {
    transportOptions.setDefaultTransport(type);
  }

  @Override
  public void onChange(TransportOption newTransport, boolean isManualSelection) {
    setImageResource(newTransport.getDrawable());
    setContentDescription(newTransport.getDescription());
  }
}
