package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageButton;

import org.thoughtcrime.securesms.TransportOption;
import org.thoughtcrime.securesms.TransportOptions;
import org.thoughtcrime.securesms.TransportOptions.OnTransportChangedListener;
import org.thoughtcrime.securesms.TransportOptionsPopup;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.guava.Optional;

public class SendButton extends AppCompatImageButton
    implements TransportOptions.OnTransportChangedListener,
               TransportOptionsPopup.SelectedListener,
               View.OnLongClickListener
{

  private final TransportOptions transportOptions;

  private Optional<TransportOptionsPopup> transportOptionsPopup = Optional.absent();

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

    setOnLongClickListener(this);

    return transportOptions;
  }

  private TransportOptionsPopup getTransportOptionsPopup() {
    if (!transportOptionsPopup.isPresent()) {
      transportOptionsPopup = Optional.of(new TransportOptionsPopup(getContext(), this, this));
    }
    return transportOptionsPopup.get();
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
  public void onSelected(TransportOption option) {
    transportOptions.setSelectedTransport(option);
    getTransportOptionsPopup().dismiss();
  }

  @Override
  public void onChange(TransportOption newTransport, boolean isManualSelection) {
    setImageResource(newTransport.getDrawable());
    setContentDescription(newTransport.getDescription());
  }

  @Override
  public boolean onLongClick(View v) {
    if (transportOptions.getEnabledTransports().size() > 1) {
      getTransportOptionsPopup().display(transportOptions.getEnabledTransports());
      return true;
    }

    return false;
  }
}
