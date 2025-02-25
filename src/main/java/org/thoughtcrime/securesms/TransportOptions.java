package org.thoughtcrime.securesms;

import static org.thoughtcrime.securesms.TransportOption.Type;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.util.guava.Optional;

import java.util.LinkedList;
import java.util.List;

public class TransportOptions {
  private final List<OnTransportChangedListener> listeners = new LinkedList<>();
  private final Context                          context;
  private final List<TransportOption>            enabledTransports;

  private Type                      defaultTransportType  = Type.NORMAL_MAIL;
  private Optional<TransportOption> selectedOption        = Optional.absent();

  public TransportOptions(Context context) {
    this.context               = context;
    this.enabledTransports     = initializeAvailableTransports();
  }

  public void reset() {
    List<TransportOption> transportOptions = initializeAvailableTransports();

    this.enabledTransports.clear();
    this.enabledTransports.addAll(transportOptions);

    if (selectedOption.isPresent() && !isEnabled(selectedOption.get())) {
      setSelectedTransport(null);
    } else {
      this.defaultTransportType = Type.NORMAL_MAIL;

      notifyTransportChangeListeners();
    }
  }

  public void setDefaultTransport(Type type) {
    this.defaultTransportType = type;

    if (!selectedOption.isPresent()) {
      notifyTransportChangeListeners();
    }
  }

  public void setSelectedTransport(@Nullable  TransportOption transportOption) {
    this.selectedOption = Optional.fromNullable(transportOption);
    notifyTransportChangeListeners();
  }

  public @NonNull TransportOption getSelectedTransport() {
    if (selectedOption.isPresent()) return selectedOption.get();

    for (TransportOption transportOption : enabledTransports) {
      if (transportOption.getType() == defaultTransportType) {
        return transportOption;
      }
    }

    throw new AssertionError("No options of default type!");
  }

  public List<TransportOption> getEnabledTransports() {
    return enabledTransports;
  }

  public void addOnTransportChangedListener(OnTransportChangedListener listener) {
    this.listeners.add(listener);
  }

  private List<TransportOption> initializeAvailableTransports() {
    List<TransportOption> results = new LinkedList<>();

    results.add(new TransportOption(Type.NORMAL_MAIL, R.drawable.ic_send_sms_white_24dp,
                                    context.getString(R.string.menu_send),
                                    context.getString(R.string.chat_input_placeholder)));

    return results;
  }

  private void notifyTransportChangeListeners() {
    for (OnTransportChangedListener listener : listeners) {
      listener.onChange(getSelectedTransport(), selectedOption.isPresent());
    }
  }

  private boolean isEnabled(TransportOption transportOption) {
    for (TransportOption option : enabledTransports) {
      if (option.equals(transportOption)) return true;
    }

    return false;
  }

  public interface OnTransportChangedListener {
    public void onChange(TransportOption newTransport, boolean manuallySelected);
  }
}
