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

      // See https://c.delta.chat/classdc__context__t.html#a6460395925d49d2053bc95224bf5ce37.
      switch (which) {
        case 0:  muteUntil = TimeUnit.HOURS.toSeconds(1); break;
        case 1:  muteUntil = TimeUnit.HOURS.toSeconds(8); break;
        case 2:  muteUntil = TimeUnit.DAYS.toSeconds(1);  break;
        case 3:  muteUntil = TimeUnit.DAYS.toSeconds(7);  break;
        case 4:  muteUntil = -1; break; // mute forever
        default: muteUntil = 0; break;
      }

      listener.onMuted(muteUntil);
    });

    builder.show();
  }

  public interface MuteSelectionListener {
    void onMuted(long duration);
  }

}
