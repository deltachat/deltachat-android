/*
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.util;

import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.os.ConfigurationCompat;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.ComposeText;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class Util {
  private static final String TAG = Util.class.getSimpleName();
  public static final String INVITE_DOMAIN = "i.delta.chat";

  public static final Handler handler = new Handler(Looper.getMainLooper());

  public static boolean isEmpty(ComposeText value) {
    return value == null || value.getText() == null || TextUtils.isEmpty(value.getTextTrimmed());
  }

  public static boolean isInviteURL(Uri uri) {
    return INVITE_DOMAIN.equals(uri.getHost()) && uri.getEncodedFragment() != null;
  }

  public static boolean isInviteURL(String url) {
    try {
      return isInviteURL(Uri.parse(url));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  public static CharSequence getBoldedString(String value) {
    SpannableString spanned = new SpannableString(value);
    spanned.setSpan(new StyleSpan(Typeface.BOLD), 0,
                    spanned.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

    return spanned;
  }

  private static final int redDestructiveColor = 0xffff0c16; // typical "destructive red" for light/dark mode

  public static void redMenuItem(Menu menu, int id) {
    MenuItem item = menu.findItem(id);
    SpannableString s = new SpannableString(item.getTitle());
    s.setSpan(new ForegroundColorSpan(redDestructiveColor), 0, s.length(), 0);
    item.setTitle(s);
  }

  public static void redPositiveButton(AlertDialog dialog) {
    redButton(dialog, AlertDialog.BUTTON_POSITIVE);
  }

  public static void redButton(AlertDialog dialog, int whichButton) {
    try {
      dialog.getButton(whichButton).setTextColor(redDestructiveColor);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static @NonNull int[] appendInt(@Nullable int[] cur, int val) {
    if (cur == null) {
      return new int[] { val };
    }
    final int N = cur.length;
    int[] ret = new int[N + 1];
    System.arraycopy(cur, 0, ret, 0, N);
    ret[N] = val;
    return ret;
  }

  public static boolean contains(@Nullable int[] array, int val) {
    if (array == null) {
      return false;
    }
    for (int element : array) {
      if (element == val) {
        return true;
      }
    }
    return false;
  }

  public static void wait(Object lock, long timeout) {
    try {
      lock.wait(timeout);
    } catch (InterruptedException ie) {
      throw new AssertionError(ie);
    }
  }

  public static void close(OutputStream out) {
    try {
      out.close();
    } catch (IOException e) {
      Log.w(TAG, e);
    }
  }

  public static long copy(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[8192];
    int read;
    long total = 0;

    while ((read = in.read(buffer)) != -1) {
      out.write(buffer, 0, read);
      total += read;
    }

    in.close();
    out.flush();
    out.close();

    return total;
  }

  public static boolean moveFile(String fromPath, String toPath) {
    boolean success = false;

    // 1st try: a simple rename
    try {
      File fromFile = new File(fromPath);
      File toFile = new File(toPath);
      toFile.delete();
      if(fromFile.renameTo(toFile)) {
        success = true;
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    // 2nd try: copy file
    if (!success) {
      try {
        InputStream fromStream = new FileInputStream(fromPath);
        OutputStream toStream = new FileOutputStream(toPath);
        if(Util.copy(fromStream, toStream)>0) {
          success = true;
        }
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }

    return success;
  }

  public static boolean isMainThread() {
    return Looper.myLooper() == Looper.getMainLooper();
  }

  public static void assertMainThread() {
    if (!isMainThread()) {
      throw new AssertionError("Main-thread assertion failed.");
    }
  }

  public static void runOnMain(final @NonNull Runnable runnable) {
    if (isMainThread()) runnable.run();
    else                handler.post(runnable);
  }

  public static void runOnMainDelayed(final @NonNull Runnable runnable, long delayMillis) {
    handler.postDelayed(runnable, delayMillis);
  }

  public static void runOnMainSync(final @NonNull Runnable runnable) {
    if (isMainThread()) {
      runnable.run();
    } else {
      final CountDownLatch sync = new CountDownLatch(1);
      runOnMain(() -> {
        try {
          runnable.run();
        } finally {
          sync.countDown();
        }
      });
      try {
        sync.await();
      } catch (InterruptedException ie) {
        throw new AssertionError(ie);
      }
    }
  }

  public static void runOnBackground(final @NonNull Runnable runnable) {
    AsyncTask.THREAD_POOL_EXECUTOR.execute(runnable);
  }

  public static void runOnAnyBackgroundThread(final @NonNull Runnable runnable) {
    if (Util.isMainThread()) {
      Util.runOnBackground(runnable);
    } else {
      runnable.run();
    }
  }

  public static void runOnBackgroundDelayed(final @NonNull Runnable runnable, long delayMillis) {
    handler.postDelayed(() -> {
      AsyncTask.THREAD_POOL_EXECUTOR.execute(runnable);
    }, delayMillis);
  }

  public static boolean equals(@Nullable Object a, @Nullable Object b) {
    return Objects.equals(a, b);
  }

  public static int hashCode(@Nullable Object... objects) {
    return Arrays.hashCode(objects);
  }

  public static boolean isLowMemory(Context context) {
    ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

    return activityManager.isLowRamDevice() || activityManager.getLargeMemoryClass() <= 64;
  }

  public static int clamp(int value, int min, int max) {
    return Math.min(Math.max(value, min), max);
  }

  public static float clamp(float value, float min, float max) {
    return Math.min(Math.max(value, min), max);
  }

  public static void writeTextToClipboard(@NonNull Context context, @NonNull String text) {
    android.content.ClipboardManager clipboard =
            (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    ClipData clip = ClipData.newPlainText(context.getString(R.string.app_name), text);
    clipboard.setPrimaryClip(clip);
  }

  public static String getTextFromClipboard(@NonNull Context context) {
    android.content.ClipboardManager clipboard =
            (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
      ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
      return item.getText().toString();
    }
    return "";
  }

  public static int toIntExact(long value) {
    if ((int)value != value) {
      throw new ArithmeticException("integer overflow");
    }
    return (int)value;
  }

  public static int objectToInt(Object value) {
    try {
      if(value instanceof String) {
          return Integer.parseInt((String)value);
      }
      else if (value instanceof Boolean) {
        return (Boolean)value? 1 : 0;
      }
      else if (value instanceof Integer) {
        return (Integer)value;
      }
      else if (value instanceof Long) {
        return toIntExact((Long)value);
      }
    } catch (Exception e) {
    }
    return 0;
  }

  public static String getPrettyFileSize(long sizeBytes) {
    if (sizeBytes <= 0) return "0";

    String[] units       = new String[]{"B", "kB", "MB", "GB", "TB"};
    int      digitGroups = (int) (Math.log10(sizeBytes) / Math.log10(1024));

    return new DecimalFormat("#,##0.#").format(sizeBytes/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
  }

  public static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  }

  /// Converts a rgb-color as returned eg. by DcContact.getColor()
  /// to argb-color as used by Android.
  public static int rgbToArgbColor(int rgb) {
    return Color.argb(0xFF, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
  }

  private static long lastClickTime = 0;
  public static boolean isClickedRecently() {
    long now = System.currentTimeMillis();
    if (now - lastClickTime < 500) {
      Log.i(TAG, "tap discarded");
      return true;
    }
    lastClickTime = now;
    return false;
  }

  private static AccessibilityManager accessibilityManager;
  public static boolean isTouchExplorationEnabled(Context context) {
    try {
      if (accessibilityManager == null) {
        Context applicationContext = context.getApplicationContext();
        accessibilityManager = ((AccessibilityManager) applicationContext.getSystemService(Context.ACCESSIBILITY_SERVICE));
      }
      return accessibilityManager.isTouchExplorationEnabled();
    } catch (Exception e) {
      return false;
    }
  }

  private static Locale lastLocale = null;

  public synchronized static Locale getLocale() {
    if (lastLocale == null) {
      try {
        lastLocale = ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration()).get(0);
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (lastLocale == null) {
        // Locale.getDefault() returns the locale the App was STARTED in, not the locale of the system.
        // It just happens to be the same for the majority of use cases.
        lastLocale = Locale.getDefault();
      }
    }
    return lastLocale;
  }

  public synchronized static void localeChanged() {
    lastLocale = null;
  }

  public static int getLayoutDirection(Context context) {
    Configuration configuration = context.getResources().getConfiguration();
    return configuration.getLayoutDirection();
  }
}
