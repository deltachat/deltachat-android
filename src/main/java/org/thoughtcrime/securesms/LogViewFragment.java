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

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.notifications.FcmReceiveService;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.Prefs;

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

public class LogViewFragment extends Fragment {
  private static final String TAG = LogViewFragment.class.getSimpleName();

  private EditText logPreview;
  private final @NonNull DynamicLanguage dynamicLanguage;

  public LogViewFragment(DynamicLanguage dynamicLanguage) {
    this.dynamicLanguage = dynamicLanguage;
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
    try {
      final Process         process        = Runtime.getRuntime().exec("logcat -v threadtime -d -t 10000");
      final BufferedReader  bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      final StringBuilder   log            = new StringBuilder();
      final String          separator      = System.getProperty("line.separator");
      final boolean devMode = Prefs.isDeveloperModeEnabled(fragment.getActivity());

      String line;
      while ((line = bufferedReader.readLine()) != null) {
        line = line.replaceFirst(" (\\d+) E ", " $1 \uD83D\uDD34 ");
        line = line.replaceFirst(" (\\d+) W ", " $1 \uD83D\uDFE0 ");
        line = line.replaceFirst(" (\\d+) I ", " $1 \uD83D\uDD35 ");
        String debugLine = line.replaceFirst(" (\\d+) D ", " $1 \uD83D\uDFE2 ");
        if (debugLine.equals(line)) { // not a debug entry
          log.append(line);
          log.append(separator);
        } else if (devMode) {
          log.append(debugLine);
          log.append(separator);
        }
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

  @TargetApi(VERSION_CODES.KITKAT)
  public static String getMemoryClass(Context context) {
    ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    String          lowMem          = "";

    if (VERSION.SDK_INT >= VERSION_CODES.KITKAT && activityManager.isLowRamDevice()) {
      lowMem = ", low-mem device";
    }
    return activityManager.getMemoryClass() + lowMem;
  }

  private static String buildDescription(LogViewFragment fragment) {
    Context context = fragment.getActivity();

    PowerManager powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
    final PackageManager pm      = context.getPackageManager();
    final StringBuilder  builder = new StringBuilder();
    final DcContext dcContext = DcHelper.getContext(context);

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

      Locale locale = fragment.dynamicLanguage.getCurrentLocale();
      builder.append("lang=").append(locale.toString()).append("\n");
      if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
        boolean isRtl = DynamicLanguage.getLayoutDirection(context) == View.LAYOUT_DIRECTION_RTL;
        builder.append("rtl=").append(isRtl).append("\n");
      }

      final String token = FcmReceiveService.getToken();
      builder.append("push-enabled=").append(Prefs.isPushEnabled(context)).append("\n");
      builder.append("push-token=").append(token == null ? "<empty>" : token).append("\n");
    } catch (Exception e) {
      builder.append("Unknown\n");
    }

    builder.append("\n");
    builder.append(dcContext.getInfo());

    return builder.toString();
  }
}
