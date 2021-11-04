package org.thoughtcrime.securesms.util;

import android.Manifest;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.database.NoExternalStorageException;
import org.thoughtcrime.securesms.permissions.Permissions;

import java.io.File;

public class StorageUtil {

  private static File getStorageDir() throws NoExternalStorageException {
    final File storage = Environment.getExternalStorageDirectory();

    if (!storage.canWrite()) {
      throw new NoExternalStorageException();
    }

    return storage;
  }

  public static boolean canWriteInSignalStorageDir() {
    File storage;

    try {
      storage = getStorageDir();
    } catch (NoExternalStorageException e) {
      return false;
    }

    return storage.canWrite();
  }

  public static boolean canWriteToMediaStore(Context context) {
    return Build.VERSION.SDK_INT > 28 ||
            Permissions.hasAll(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
  }

  public static @NonNull Uri getVideoUri() {
    if (Build.VERSION.SDK_INT < 21) {
      return getLegacyUri(Environment.DIRECTORY_MOVIES);
    } else {
      return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    }
  }

  public static @NonNull
  Uri getAudioUri() {
    if (Build.VERSION.SDK_INT < 21) {
      return getLegacyUri(Environment.DIRECTORY_MUSIC);
    } else {
      return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    }
  }

  public static @NonNull Uri getImageUri() {
    if (Build.VERSION.SDK_INT < 21) {
      return getLegacyUri(Environment.DIRECTORY_PICTURES);
    } else {
      return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    }
  }

  public static @NonNull Uri getDownloadUri() {
    if (Build.VERSION.SDK_INT < 29) {
      return getLegacyUri(Environment.DIRECTORY_DOWNLOADS);
    } else {
      return MediaStore.Downloads.EXTERNAL_CONTENT_URI;
    }
  }

  public static @NonNull Uri getLegacyUri(@NonNull String directory) {
    return Uri.fromFile(Environment.getExternalStoragePublicDirectory(directory));
  }

  public static @Nullable String getCleanFileName(@Nullable String fileName) {
    if (fileName == null) return null;

    fileName = fileName.replace('\u202D', '\uFFFD');
    fileName = fileName.replace('\u202E', '\uFFFD');

    return fileName;
  }
}
