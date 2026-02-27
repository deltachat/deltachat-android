package org.thoughtcrime.securesms.calls;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.telecom.CallEndpointCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.thoughtcrime.securesms.R;

import java.util.List;

/**
 * Bottom sheet dialog for selecting audio output device
 */
@RequiresApi(Build.VERSION_CODES.O)
public class AudioDevicePickerDialog extends BottomSheetDialog {
  private static final String TAG = AudioDevicePickerDialog.class.getSimpleName();

  public interface OnDeviceSelectedListener {
    void onDeviceSelected(CallEndpointCompat endpoint);
  }

  public AudioDevicePickerDialog(@NonNull Context context,
                                 @NonNull List<CallEndpointCompat> endpoints,
                                 CallEndpointCompat currentEndpoint,
                                 @NonNull OnDeviceSelectedListener listener) {
    super(context);

    // Inflate layout
    setContentView(R.layout.dialog_audio_device_picker);

    // Setup RecyclerView
    RecyclerView recyclerView = findViewById(R.id.audio_device_list);
    if (recyclerView == null) {
      Log.e(TAG, "RecyclerView not found in layout");
      return;
    }

    recyclerView.setLayoutManager(new LinearLayoutManager(context));

    AudioDeviceAdapter adapter = new AudioDeviceAdapter(
      endpoints,
      currentEndpoint,
      endpoint -> {
        listener.onDeviceSelected(endpoint);
        dismiss();
      }
    );

    recyclerView.setAdapter(adapter);
  }

  /**
   * RecyclerView adapter for audio devices
   */
  private static class AudioDeviceAdapter extends RecyclerView.Adapter<AudioDeviceAdapter.ViewHolder> {

    private final List<CallEndpointCompat> endpoints;
    private final CallEndpointCompat currentEndpoint;
    private final OnDeviceSelectedListener listener;

    AudioDeviceAdapter(List<CallEndpointCompat> endpoints,
                       CallEndpointCompat currentEndpoint,
                       OnDeviceSelectedListener listener) {
      this.endpoints = endpoints;
      this.currentEndpoint = currentEndpoint;
      this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(parent.getContext()).inflate(
        R.layout.item_audio_device,
        parent,
        false
      );
      return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
      CallEndpointCompat endpoint = endpoints.get(position);
      boolean isCurrent = currentEndpoint != null &&
        endpoint.getIdentifier().equals(currentEndpoint.getIdentifier());

      holder.bind(endpoint, isCurrent, listener);
    }

    @Override
    public int getItemCount() {
      return endpoints.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
      private final ImageView iconView;
      private final TextView nameView;
      private final ImageView checkView;

      ViewHolder(@NonNull View itemView) {
        super(itemView);
        iconView = itemView.findViewById(R.id.device_icon);
        nameView = itemView.findViewById(R.id.device_name);
        checkView = itemView.findViewById(R.id.device_check);
      }

      void bind(CallEndpointCompat endpoint, boolean isCurrent, OnDeviceSelectedListener listener) {
        nameView.setText(endpoint.getName());

        int iconRes = CallUtil.getIconResByCallEndpoint(endpoint);
        iconView.setImageResource(iconRes);

        // Show checkmark for current device
        checkView.setVisibility(isCurrent ? View.VISIBLE : View.GONE);

        // Set click listener
        itemView.setOnClickListener(v -> listener.onDeviceSelected(endpoint));
      }
    }
  }
}
