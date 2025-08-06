package org.thoughtcrime.securesms.util;


import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.R;

import java.util.List;

public class IntentUtils {

  public static boolean isResolvable(@NonNull Context context, @NonNull Intent intent) {
    List<ResolveInfo> resolveInfoList = context.getPackageManager().queryIntentActivities(intent, 0);
    return resolveInfoList != null && resolveInfoList.size() > 1;
  }

  public static void showInBrowser(Context context, String url) {
    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    try {
      context.startActivity(browserIntent);
    } catch (ActivityNotFoundException e) {
      Toast.makeText(context, R.string.no_browser_installed, Toast.LENGTH_LONG).show();
    }
  }

  public static int FLAG_MUTABLE() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      return PendingIntent.FLAG_MUTABLE;
    } else {
      return 0;
    }
  }
}
