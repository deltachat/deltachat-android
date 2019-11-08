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

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.ComposeText;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Util {
  private static final String TAG = Util.class.getSimpleName();

  public static Handler handler = new Handler(Looper.getMainLooper());

  public static String join(String[] list, String delimiter) {
    return join(Arrays.asList(list), delimiter);
  }

  public static String join(Collection<String> list, String delimiter) {
    StringBuilder result = new StringBuilder();
    int i = 0;

    for (String item : list) {
      result.append(item);

      if (++i < list.size())
        result.append(delimiter);
    }

    return result.toString();
  }

  public static String join(long[] list, String delimeter) {
    StringBuilder sb = new StringBuilder();

    for (int j=0;j<list.length;j++) {
      if (j != 0) sb.append(delimeter);
      sb.append(list[j]);
    }

    return sb.toString();
  }

  public static boolean isEmpty(ComposeText value) {
    return value == null || value.getText() == null || TextUtils.isEmpty(value.getTextTrimmed());
  }

  public static CharSequence getBoldedString(String value) {
    SpannableString spanned = new SpannableString(value);
    spanned.setSpan(new StyleSpan(Typeface.BOLD), 0,
                    spanned.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

    return spanned;
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

  public static void wait(Object lock, long timeout) {
    try {
      lock.wait(timeout);
    } catch (InterruptedException ie) {
      throw new AssertionError(ie);
    }
  }

  public static void close(InputStream in) {
    try {
      in.close();
    } catch (IOException e) {
      Log.w(TAG, e);
    }
  }

  public static void close(OutputStream out) {
    try {
      out.close();
    } catch (IOException e) {
      Log.w(TAG, e);
    }
  }

  public static void readFully(InputStream in, byte[] buffer) throws IOException {
    int offset = 0;

    for (;;) {
      int read = in.read(buffer, offset, buffer.length - offset);
      if (read == -1) throw new IOException("Stream ended early");

      if (read + offset < buffer.length) offset += read;
      else                		           return;
    }
  }

  public static byte[] readFully(InputStream in) throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    byte[] buffer              = new byte[4096];
    int read;

    while ((read = in.read(buffer)) != -1) {
      bout.write(buffer, 0, read);
    }

    in.close();

    return bout.toByteArray();
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

  public static List<String> split(String source, String delimiter) {
    List<String> results = new LinkedList<>();

    if (TextUtils.isEmpty(source)) {
      return results;
    }

    String[] elements = source.split(delimiter);
    Collections.addAll(results, elements);

    return results;
  }

  public static byte[][] split(byte[] input, int firstLength, int secondLength) {
    byte[][] parts = new byte[2][];

    parts[0] = new byte[firstLength];
    System.arraycopy(input, 0, parts[0], 0, firstLength);

    parts[1] = new byte[secondLength];
    System.arraycopy(input, firstLength, parts[1], 0, secondLength);

    return parts;
  }

  public static byte[] combine(byte[]... elements) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      for (byte[] element : elements) {
        baos.write(element);
      }

      return baos.toByteArray();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public static byte[] trim(byte[] input, int length) {
    byte[] result = new byte[length];
    System.arraycopy(input, 0, result, 0, result.length);

    return result;
  }

  public static int getCurrentApkReleaseVersion(Context context) {
    try {
      return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
    } catch (PackageManager.NameNotFoundException e) {
      throw new AssertionError(e);
    }
  }

  public static byte[] getSecretBytes(int size) {
    byte[] secret = new byte[size];
    getSecureRandom().nextBytes(secret);
    return secret;
  }

  public static SecureRandom getSecureRandom() {
    return new SecureRandom();
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
    return a == b || (a != null && a.equals(b));
  }

  public static int hashCode(@Nullable Object... objects) {
    return Arrays.hashCode(objects);
  }

  public static @Nullable Uri uri(@Nullable String uri) {
    if (uri == null) return null;
    else             return Uri.parse(uri);
  }

  @TargetApi(VERSION_CODES.KITKAT)
  public static boolean isLowMemory(Context context) {
    ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

    return (VERSION.SDK_INT >= VERSION_CODES.KITKAT && activityManager.isLowRamDevice()) ||
           activityManager.getLargeMemoryClass() <= 64;
  }

  public static int clamp(int value, int min, int max) {
    return Math.min(Math.max(value, min), max);
  }

  public static float clamp(float value, float min, float max) {
    return Math.min(Math.max(value, min), max);
  }

  public static void writeTextToClipboard(@NonNull Context context, @NonNull String text) {
    try {
      ClipboardManager clipboardManager = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
      clipboardManager.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_name), text));
    }
    catch(Exception e) {
      e.printStackTrace();
    }
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
}
