package org.thoughtcrime.securesms;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

public class ShareLocationDialog {

  public static void show(final Context context, final @NonNull ShareLocationDurationSelectionListener listener) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(R.string.title_share_location);
    builder.setItems(R.array.share_location_durations, (dialog, which) -> {
      final int shareLocationUnit;

      switch (which) {
        default:
        case 0:  shareLocationUnit =      5 * 60; break;
        case 1:  shareLocationUnit =     30 * 60; break;
        case 2:  shareLocationUnit =     60 * 60; break;
        case 3:  shareLocationUnit = 2 * 60 * 60; break;
        case 4:  shareLocationUnit = 6 * 60 * 60; break;
      }

      listener.onSelected(shareLocationUnit);
    });
    builder.setNegativeButton(R.string.cancel, null);

    builder.show();
  }

  public interface ShareLocationDurationSelectionListener {
    void onSelected(int durationInSeconds);
  }
}
