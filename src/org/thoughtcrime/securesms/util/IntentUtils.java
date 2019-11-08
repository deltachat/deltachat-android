package org.thoughtcrime.securesms.util;


import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import androidx.annotation.NonNull;

import java.util.List;

public class IntentUtils {

  public static boolean isResolvable(@NonNull Context context, @NonNull Intent intent) {
    List<ResolveInfo> resolveInfoList = context.getPackageManager().queryIntentActivities(intent, 0);
    return resolveInfoList != null && resolveInfoList.size() > 1;
  }

  public static void showBrowserIntent(Context context, String url) {
    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    context.startActivity(browserIntent);
  }

}
