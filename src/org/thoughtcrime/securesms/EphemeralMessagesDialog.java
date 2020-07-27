package org.thoughtcrime.securesms;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.TextViewCompat;

import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.concurrent.TimeUnit;

public class EphemeralMessagesDialog {

    private final static String TAG = EphemeralMessagesDialog.class.getSimpleName();

    public static void show(final Context context, int currentSelectedTime, final @NonNull EphemeralMessagesInterface listener) {
        CharSequence[] choices = context.getResources().getStringArray(R.array.ephemeral_message_durations);
        int preselected = getPreselection(currentSelectedTime);
        final int[] selectedChoice = new int[]{preselected};

        View dialogView = View.inflate(context, R.layout.dialog_extended_options, null);
        RadioGroup container = dialogView.findViewById(R.id.optionsContainer);
        for (CharSequence choice : choices) {

            RadioButton radioButton = new RadioButton(context);
            radioButton.setText(choice);
            TextViewCompat.setTextAppearance(radioButton, android.R.style.TextAppearance_Medium);

            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, ViewUtil.dpToPx(context, 8));
            radioButton.setLayoutParams(params);
            container.addView(radioButton);
        }

        container.setOnCheckedChangeListener((group, checkedId) -> {
            int childCount = group.getChildCount();
            for (int x = 0; x < childCount; x++) {
                RadioButton btn = (RadioButton) group.getChildAt(x);
                if (btn.getId() == checkedId) {
                    selectedChoice[0] = x;
                }
            }
        });
        container.check(container.getChildAt(preselected).getId());

        TextView messageView = dialogView.findViewById(R.id.description);
        messageView.setText(context.getString(R.string.ephemeral_messages_hint));

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.ephemeral_messages)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    final long burnAfter;
                    switch (selectedChoice[0]) {
                        case 1:  burnAfter = TimeUnit.SECONDS.toSeconds(30); break;
                        case 2:  burnAfter = TimeUnit.MINUTES.toSeconds(1); break;
                        case 3:  burnAfter = TimeUnit.HOURS.toSeconds(1); break;
                        case 4:  burnAfter = TimeUnit.DAYS.toSeconds(1);  break;
                        case 5:  burnAfter = TimeUnit.DAYS.toSeconds(7);  break;
                        case 6:  burnAfter = TimeUnit.DAYS.toSeconds(28); break;
                        default: burnAfter = 0; break;
                    }
                    listener.onTimeSelected(burnAfter);
                });
        builder.show();
    }

    public interface EphemeralMessagesInterface {
        void onTimeSelected(long duration);
    }

    private static int getPreselection(int timespan) {
        if (timespan == TimeUnit.DAYS.toSeconds(28)) {
            return 6;
        } else if (timespan == TimeUnit.DAYS.toSeconds(7)) {
            return 5;
        } else if (timespan == TimeUnit.DAYS.toSeconds(1)) {
            return 4;
        } else if (timespan == TimeUnit.HOURS.toSeconds(1)) {
            return 3;
        } else if (timespan == TimeUnit.MINUTES.toSeconds(1)) {
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
