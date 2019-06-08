package org.thoughtcrime.securesms.components.reminder;


import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.util.Linkify;
import android.widget.TextView;

import org.thoughtcrime.securesms.R;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class OutdatedReminder extends Reminder {

    private static final long OUTDATED_THRESHOLD_IN_DAYS = 90;

    public OutdatedReminder(@NonNull final Context context) {
        super(context.getString(R.string.information_outdated_app_title),
                context.getString(R.string.information_outdated_app_text));

        setOkListener(v -> {
            AlertDialog sourceDialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.information_outdated_app_dialog_title)
                    .setMessage(R.string.information_outdated_app_dialog_text)
                    .setPositiveButton(R.string.ok, (dialog, which) -> dialog.cancel())
                    .create();
            sourceDialog.show();
            Linkify.addLinks((TextView) Objects.requireNonNull(sourceDialog.findViewById(android.R.id.message)), Linkify.WEB_URLS);
        });
    }

    @Override
    public boolean isDismissable() {
        return false;
    }

    @NonNull
    @Override
    public Importance getImportance() {
        return Importance.ERROR;
    }

    public static boolean isEligible(Context context) {
        if (context == null) {
            return false;
        }
        try {
            final PackageManager packageManager = context.getPackageManager();
            String packageName = context.getPackageName();
            if (packageManager.getInstallerPackageName(packageName) == null) {
                long lastUpdateTime = packageManager
                        .getPackageInfo(packageName, 0)
                        .lastUpdateTime;
                long nowTime = System.currentTimeMillis();
                long diff = nowTime - lastUpdateTime;
                long diffInDays = TimeUnit.MILLISECONDS.toDays(diff);
                return diffInDays >= OUTDATED_THRESHOLD_IN_DAYS;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
