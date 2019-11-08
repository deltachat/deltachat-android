package org.thoughtcrime.securesms;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.util.concurrent.TimeUnit;

public class MuteDialog {

  public static void show(final Context context, final @NonNull MuteSelectionListener listener) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(R.string.menu_mute);
    builder.setNegativeButton(R.string.cancel, null);
    builder.setItems(R.array.mute_durations, (dialog, which) -> {
      final long muteUntil;

      switch (which) {
        case 0:  muteUntil = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1);    break;
        case 1:  muteUntil = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2);    break;
        case 2:  muteUntil = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1);     break;
        case 3:  muteUntil = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7);     break;
        case 4:  muteUntil = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(36500); break;
        default: muteUntil = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1);    break;
      }

      listener.onMuted(muteUntil);
    });

    builder.show();

  }

  public interface MuteSelectionListener {
    void onMuted(long until);
  }

}
