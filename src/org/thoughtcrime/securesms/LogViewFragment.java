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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.NoExternalStorageException;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Scrubber;
import org.thoughtcrime.securesms.util.StorageUtil;

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
  private @NonNull DynamicLanguage dynamicLanguage;

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
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_view_log, container, false);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
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

  public void scrollDownLog() { logPreview.setSelection(logPreview.getText().length()); }

  public void scrollUpLog() { logPreview.setSelection(0); }

  public boolean saveLogFile() {

    File             outputDir   = null;
    SimpleDateFormat dateFormat  = new SimpleDateFormat("yyyyMMdd-HHmmss");
    Date             now         = new Date();
    String           logFileName = "deltachat-log-" + dateFormat.format(now) + ".txt";

    try {
      outputDir = StorageUtil.getDownloadDir();
      String logText =  logPreview.getText().toString();
      if(!logText.trim().equals("")){
        File logFile = new File(outputDir + "/" + logFileName);

        if(!logFile.exists()) logFile.createNewFile();

        FileWriter logFileWriter = new FileWriter(logFile, false);
        BufferedWriter logFileBufferWriter = new BufferedWriter(logFileWriter);
        logFileBufferWriter.write(logText);
        logFileBufferWriter.close();
      }
    } catch (IOException | NoExternalStorageException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  private static String grabLogcat() {
    try {
      final Process         process        = Runtime.getRuntime().exec("logcat -v threadtime -d");
      final BufferedReader  bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      final StringBuilder   log            = new StringBuilder();
      final String          separator      = System.getProperty("line.separator");

      String line;
      while ((line = bufferedReader.readLine()) != null) {
        log.append(line);
        log.append(separator);
      }
      return log.toString();
    } catch (IOException ioe) {
      Log.w(TAG, "IOException when trying to read logcat.", ioe);
      return null;
    }
  }

  private class PopulateLogcatAsyncTask extends AsyncTask<Void,Void,String> {
    private WeakReference<LogViewFragment> weakFragment;

    public PopulateLogcatAsyncTask(LogViewFragment fragment) {
      this.weakFragment = new WeakReference<>(fragment);
    }

    @Override
    protected String doInBackground(Void... voids) {
      LogViewFragment fragment = weakFragment.get();
      if (fragment == null) return null;

      return "**This log may contain sensitive information. If you want to post it publicly you may examine and edit it beforehand.**\n\n" +
          buildDescription(fragment) + "\n" + new Scrubber().scrub(grabLogcat());
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
      builder.append("installer=")
             .append(pm.getInstallerPackageName(context.getPackageName()))
             .append("\n");
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        builder.append("ignoreBatteryOptimizations=").append(
            powerManager.isIgnoringBatteryOptimizations(context.getPackageName())).append("\n");
      }
      builder.append("notifications=").append(
              Prefs.isNotificationsEnabled(context)).append("\n");
      builder.append("reliableService=").append(
              Prefs.reliableService(context)).append("\n");

      Locale locale = fragment.dynamicLanguage.getCurrentLocale();
      builder.append("lang=").append(locale.getLanguage()).append("\n");
      if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
        boolean isRtl = DynamicLanguage.getLayoutDirection(context) == View.LAYOUT_DIRECTION_RTL;
        builder.append("rtl=").append(isRtl).append("\n");
      }

    } catch (Exception e) {
      builder.append("Unknown\n");
    }

    builder.append("\n");
    DcContext dcContext = DcHelper.getContext(context);
    builder.append(dcContext.getInfo());

    return builder.toString();
  }
}
