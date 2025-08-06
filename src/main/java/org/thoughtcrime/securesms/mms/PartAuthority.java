package org.thoughtcrime.securesms.mms;

import android.content.ContentUris;
import android.content.Context;
import android.content.UriMatcher;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.providers.SingleUseBlobProvider;

import java.io.IOException;
import java.io.InputStream;

public class PartAuthority {

  private static final int PERSISTENT_ROW = 3;
  private static final int SINGLE_USE_ROW = 4;

  private static final UriMatcher uriMatcher;

  static {
    uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    uriMatcher.addURI(PersistentBlobProvider.AUTHORITY, PersistentBlobProvider.EXPECTED_PATH_OLD, PERSISTENT_ROW);
    uriMatcher.addURI(PersistentBlobProvider.AUTHORITY, PersistentBlobProvider.EXPECTED_PATH_NEW, PERSISTENT_ROW);
    uriMatcher.addURI(SingleUseBlobProvider.AUTHORITY, SingleUseBlobProvider.PATH, SINGLE_USE_ROW);
  }

  public static InputStream getAttachmentStream(@NonNull Context context, @NonNull Uri uri)
      throws IOException
  {
    int match = uriMatcher.match(uri);
    try {
      switch (match) {
      case PERSISTENT_ROW: return PersistentBlobProvider.getInstance().getStream(context, ContentUris.parseId(uri));
      case SINGLE_USE_ROW: return SingleUseBlobProvider.getInstance().getStream(ContentUris.parseId(uri));
      default:             return context.getContentResolver().openInputStream(uri);
      }
    } catch (SecurityException se) {
      throw new IOException(se);
    }
  }

  public static @Nullable String getAttachmentFileName(@NonNull Context context, @NonNull Uri uri) {
    int match = uriMatcher.match(uri);

    switch (match) {
    case PERSISTENT_ROW:
      return PersistentBlobProvider.getFileName(context, uri);
    case SINGLE_USE_ROW:
    default:
      return null;
    }
  }

  public static @Nullable Long getAttachmentSize(@NonNull Context context, @NonNull Uri uri) {
    int match = uriMatcher.match(uri);

    switch (match) {
      case PERSISTENT_ROW:
        return PersistentBlobProvider.getFileSize(context, uri);
      case SINGLE_USE_ROW:
      default:
        return null;
    }
  }

  public static @Nullable String getAttachmentContentType(@NonNull Context context, @NonNull Uri uri) {
    int match = uriMatcher.match(uri);

    switch (match) {
      case PERSISTENT_ROW:
        return PersistentBlobProvider.getMimeType(context, uri);
      case SINGLE_USE_ROW:
      default:
        return null;
    }
  }

  public static boolean isLocalUri(final @NonNull Uri uri) {
    int match = uriMatcher.match(uri);
    switch (match) {
    case PERSISTENT_ROW:
    case SINGLE_USE_ROW:
      return true;
    }
    return false;
  }
}
