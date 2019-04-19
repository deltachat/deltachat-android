package org.thoughtcrime.securesms;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import java.util.concurrent.TimeUnit;

/**
 * Created by cyberta on 06.03.19.
 */

public class ShareLocationDialog extends AlertDialog {


  protected ShareLocationDialog(Context context) {
    super(context);
  }

  protected ShareLocationDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
    super(context, cancelable, cancelListener);
  }

  protected ShareLocationDialog(Context context, int theme) {
    super(context, theme);
  }

  public static void show(final Context context, final @NonNull ShareLocationDurationSelectionListener listener) {
    Builder builder = new Builder(context);
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

    builder.show();

  }

  public interface ShareLocationDurationSelectionListener {
    void onSelected(int durationInSeconds);
  }

}
