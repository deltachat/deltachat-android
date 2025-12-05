/*
 * Copyright (C) 2014 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.thoughtcrime.securesms;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.Fragment;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.notifications.FcmReceiveService;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;

public class LogViewFragment extends Fragment {
  private EditText logPreview;

  public LogViewFragment() {
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_view_log, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    logPreview   = (EditText) getView().findViewById(R.id.log_preview);

    // add padding to avoid content hidden behind system bars
    ViewUtil.applyWindowInsets(getView().findViewById(R.id.content_container), true, false, true, true);

    new PopulateLogcatAsyncTask(this).execute();
  }

  public String getLogText() {
    return logPreview==null? "null" : logPreview.getText().toString();
  }

  public Float getLogTextSize() { return logPreview.getTextSize(); }

  public void setLogTextSize(Float textSize) {
    logPreview.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
  }

  public void scrollDownLog() {
    logPreview.requestFocus();
    logPreview.setSelection(logPreview.getText().length());
  }

  public void scrollUpLog() {
    logPreview.requestFocus();
    logPreview.setSelection(0);
  }

  public File saveLogFile(File outputDir) {
    File             logFile     = null;
    SimpleDateFormat dateFormat  = new SimpleDateFormat("yyyyMMdd-HHmmss");
    Date             now         = new Date();
    String           logFileName = "deltachat-log-" + dateFormat.format(now) + ".txt";

    try {
      String logText =  logPreview.getText().toString();
      if(!logText.trim().equals("")){
        logFile = new File(outputDir + "/" + logFileName);
        if(!logFile.exists()) logFile.createNewFile();

        FileWriter logFileWriter = new FileWriter(logFile, false);
        BufferedWriter logFileBufferWriter = new BufferedWriter(logFileWriter);
        logFileBufferWriter.write(logText);
        logFileBufferWriter.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return logFile;
  }

  private static String grabLogcat(LogViewFragment fragment) {
    String command = "logcat -v threadtime -d -t 10000 *:I";
    try {
      final Process         process        = Runtime.getRuntime().exec(command);
      final BufferedReader  bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      final StringBuilder   log            = new StringBuilder();
      final String          separator      = System.getProperty("line.separator");

      String line;
      while ((line = bufferedReader.readLine()) != null) {
        line = line.replaceFirst(" (\\d+) E ", " $1 \uD83D\uDD34 ");
        line = line.replaceFirst(" (\\d+) W ", " $1 \uD83D\uDFE0 ");
        line = line.replaceFirst(" (\\d+) I ", " $1 \uD83D\uDD35 ");
        line = line.replaceFirst(" (\\d+) D ", " $1 \uD83D\uDFE2 ");
        log.append(line);
        log.append(separator);
      }
      return log.toString();
    } catch (Exception e) {
      return "Error grabbing log: " + e;
    }
  }

  private class PopulateLogcatAsyncTask extends AsyncTask<Void,Void,String> {
    private final WeakReference<LogViewFragment> weakFragment;

    public PopulateLogcatAsyncTask(LogViewFragment fragment) {
      this.weakFragment = new WeakReference<>(fragment);
    }

    @Override
    protected String doInBackground(Void... voids) {
      LogViewFragment fragment = weakFragment.get();
      if (fragment == null) return null;

      return "**This log may contain sensitive information. If you want to post it publicly you may examine and edit it beforehand.**\n\n" +
          buildDescription(fragment) + "\n" + grabLogcat(fragment);
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      logPreview.setText(R.string.one_moment);
    }

    @Override
    protected void onPostExecute(String logcat) {
      super.onPostExecute(logcat);
      if (TextUtils.isEmpty(logcat)) {
        // the log is in english, so it is fine if some of explaining strings are in english as well
        logPreview.setText("Could not read the log on your device. You can still use ADB to get a debug log instead.");
        return;
      }
      logPreview.setText(logcat);
    }
  }

  private static long asMegs(long bytes) {
    return bytes / 1048576L;
  }

  public static String getMemoryUsage(Context context) {
    Runtime info = Runtime.getRuntime();
    return String.format(Locale.ENGLISH, "%dM (%.2f%% free, %dM max)",
                         asMegs(info.totalMemory()),
                         (float)info.freeMemory() / info.totalMemory() * 100f,
                         asMegs(info.maxMemory()));
  }

  public static String getMemoryClass(Context context) {
    ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    String          lowMem          = "";

    if (activityManager.isLowRamDevice()) {
      lowMem = ", low-mem device";
    }
    return activityManager.getMemoryClass() + lowMem;
  }

  private static String buildDescription(LogViewFragment fragment) {
    Context context = fragment.getActivity();

    PowerManager powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
    final PackageManager pm      = context.getPackageManager();
    final StringBuilder  builder = new StringBuilder();

    builder.append("device=")
           .append(Build.MANUFACTURER).append(" ")
           .append(Build.MODEL).append(" (")
           .append(Build.PRODUCT).append(")\n");
    builder.append("android=").append(VERSION.RELEASE).append(" (")
                               .append(VERSION.INCREMENTAL).append(", ")
                               .append(Build.DISPLAY).append(")\n");
    builder.append("sdk=").append(Build.VERSION.SDK_INT).append("\n");
    builder.append("memory=").append(getMemoryUsage(context)).append("\n");
    builder.append("memoryClass=").append(getMemoryClass(context)).append("\n");
    builder.append("host=").append(Build.HOST).append("\n");
    builder.append("applicationId=").append(BuildConfig.APPLICATION_ID).append("\n");
    builder.append("app=");
    try {
      builder.append(pm.getApplicationLabel(pm.getApplicationInfo(context.getPackageName(), 0)))
             .append(" ")
             .append(pm.getPackageInfo(context.getPackageName(), 0).versionName)
             .append("-")
             .append(BuildConfig.FLAVOR)
             .append(BuildConfig.DEBUG? "-debug" : "")
             .append("\n");
      builder.append("versionCode=")
             .append(pm.getPackageInfo(context.getPackageName(), 0).versionCode)
             .append("\n");
      builder.append("installer=")
             .append(pm.getInstallerPackageName(context.getPackageName()))
             .append("\n");
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        builder.append("ignoreBatteryOptimizations=").append(
            powerManager.isIgnoringBatteryOptimizations(context.getPackageName())).append("\n");
      }
      builder.append("reliableService=").append(
              Prefs.reliableService(context)).append("\n");

      Locale locale = Util.getLocale();
      builder.append("lang=").append(locale.toString()).append("\n");
      boolean isRtl = Util.getLayoutDirection(context) == View.LAYOUT_DIRECTION_RTL;
      builder.append("rtl=").append(isRtl).append("\n");

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        boolean notifPermGranted = PermissionChecker.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PermissionChecker.PERMISSION_GRANTED;
        builder.append("post-notifications-granted=").append(notifPermGranted).append("\n");
      } else {
        builder.append("post-notifications-granted=<not needed>").append("\n");
      }

      final String token = FcmReceiveService.getToken();
      builder.append("push-enabled=").append(Prefs.isPushEnabled(context)).append("\n");
      builder.append("push-token=").append(token == null ? "<empty>" : token).append("\n");
    } catch (Exception e) {
      builder.append("Unknown\n");
    }

    final Rpc rpc = DcHelper.getRpc(context);
    final int accId = DcHelper.getContext(context).getAccountId();

    builder.append("\n");
    try {
      builder.append(rpc.getStorageUsageReportString(accId));
    } catch (RpcException e) {
      builder.append(e);
    }
    builder.append(DcHelper.getContext(context).getInfo());

    return builder.toString();
  }
}
