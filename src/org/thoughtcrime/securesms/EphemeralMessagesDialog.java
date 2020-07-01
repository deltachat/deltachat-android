package org.thoughtcrime.securesms;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.util.concurrent.TimeUnit;

public class EphemeralMessagesDialog {

    private final static String TAG = EphemeralMessagesDialog.class.getSimpleName();

    public static void show(final Context context, int currentSelectedTime, final @NonNull EphemeralMessagesInterface listener) {
        CharSequence[] choices = context.getResources().getStringArray(R.array.ephemeral_message_durations);
        int preselected = getPreselection(currentSelectedTime);
        final int[] selectedChoice = new int[]{preselected};

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.pref_ephemeral_messages)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    final long burnAfter;
                    switch (selectedChoice[0]) {
                        case 1:  burnAfter = TimeUnit.SECONDS.toSeconds(30); break;
                        case 2:  burnAfter = TimeUnit.HOURS.toSeconds(1); break;
                        case 3:  burnAfter = TimeUnit.DAYS.toSeconds(1);  break;
                        case 4:  burnAfter = TimeUnit.DAYS.toSeconds(7);  break;
                        case 5:  burnAfter = TimeUnit.DAYS.toSeconds(28); break;
                        default: burnAfter = 0; break;
                    }
                    listener.onTimeSelected(burnAfter);
                })
                .setSingleChoiceItems(choices, preselected, ((dialog, which) -> {
                    selectedChoice[0] = which;
                }));
        builder.show();
    }

    public interface EphemeralMessagesInterface {
        void onTimeSelected(long duration);
    }

    private static int getPreselection(int timespan) {
        if (timespan ==  TimeUnit.DAYS.toSeconds(28)) {
            return 5;
        } else if (timespan == TimeUnit.DAYS.toSeconds(7)) {
            return 4;
        } else if (timespan == TimeUnit.DAYS.toSeconds(1)) {
            return 3;
        } else if (timespan == TimeUnit.HOURS.toSeconds(1)) {
            return 2;
        } else if (timespan == TimeUnit.SECONDS.toSeconds(30)) {
            return 1;
        } else {
            if (timespan != 0) {
                Log.e(TAG, "Invalid ephemeral messages timespan, falling back to OFF");
            }
            return 0;
        }

    }

}
