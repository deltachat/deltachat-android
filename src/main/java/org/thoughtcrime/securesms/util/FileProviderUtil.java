package org.thoughtcrime.securesms.util;


import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import org.thoughtcrime.securesms.BuildConfig;

import java.io.File;

public class FileProviderUtil {

  private static final String AUTHORITY = BuildConfig.APPLICATION_ID+".fileprovider";

  public static Uri getUriFor(@NonNull Context context, @NonNull File file) throws IllegalStateException, NullPointerException {
    return FileProvider.getUriForFile(context, AUTHORITY, file);
  }

}
