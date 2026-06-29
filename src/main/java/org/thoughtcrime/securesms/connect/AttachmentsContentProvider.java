package org.thoughtcrime.securesms.connect;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.b44t.messenger.DcContext;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Objects;
import org.thoughtcrime.securesms.util.MediaUtil;

public class AttachmentsContentProvider extends ContentProvider {

  private static final String[] ALL_COLUMNS = {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};

  /* We save all attachments in our private files-directory
  that cannot be read by other apps.

  When starting an Intent for viewing, we cannot use the paths.
  Instead, we give a content://-url that results in calls to this class.

  (An alternative would be to copy files to view to a public directory, however, this would
  lead to duplicate data.
  Another alternative would be to write all attachments to a public directory, however, this
  may lead to security problems as files are system-wide-readable and would also cause problems
  if the user or another app deletes these files) */

  @Override
  public ParcelFileDescriptor openFile(Uri uri, @NonNull String mode) throws FileNotFoundException {
    DcContext dcContext = DcHelper.getContext(Objects.requireNonNull(getContext()));

    // `uri` originally comes from DcHelper.openForViewOrShare() and
    // looks like `content://chat.delta.attachments/ef39a39/text.txt`
    // where ef39a39 is the file in the blob directory
    // and text.txt is the original name of the file, as returned by `msg.getFilename()`.
    // `uri.getPathSegments()` returns ["ef39a39", "text.txt"] in this example.
    String file = uri.getPathSegments().get(0);
    if (!DcHelper.sharedFiles.containsKey(file)) {
      throw new FileNotFoundException("File was not shared before.");
    }

    File privateFile = new File(dcContext.getBlobdir(), file);
    return ParcelFileDescriptor.open(privateFile, ParcelFileDescriptor.MODE_READ_ONLY);
  }

  @Override
  public int delete(
      @NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
    return 0;
  }

  @Override
  public String getType(Uri uri) {
    String file = uri.getPathSegments().get(0);
    String mimeType = DcHelper.sharedFiles.get(file);

    return DcHelper.checkMime(uri.getPathSegments().get(1), mimeType);
  }

  @Override
  public String getTypeAnonymous(Uri uri) {
    String ext = MediaUtil.getFileExtensionFromUrl(uri.toString());
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
  }

  @Override
  public Uri insert(@NonNull Uri arg0, ContentValues values) {
    return null;
  }

  @Override
  public boolean onCreate() {
    return true;
  }

  @Override
  public Cursor query(
      @NonNull Uri uri,
      @Nullable String[] projection,
      @Nullable String selection,
      @Nullable String[] selectionArgs,
      @Nullable String sortOrder) {
    // Segment [0] is the blob file key ("ef39a39"), same as in openFile() and getType()
    // Segment [1] is the original display name ("text.txt")
    String file = uri.getPathSegments().get(0);

    // Same guard used in openFile()
    if (!DcHelper.sharedFiles.containsKey(file)) {
      return null;
    }

    // Resolve the actual File
    DcContext dcContext = DcHelper.getContext(Objects.requireNonNull(getContext()));
    File privateFile = new File(dcContext.getBlobdir(), file);

    // Default to all supported columns
    if (projection == null) {
      projection = ALL_COLUMNS;
    }

    // Build column-name and value arrays
    String[] cols = new String[projection.length];
    Object[] values = new Object[projection.length];
    int i = 0;
    for (String col : projection) {
      if (OpenableColumns.DISPLAY_NAME.equals(col)) {
        cols[i] = OpenableColumns.DISPLAY_NAME;
        values[i++] = uri.getPathSegments().get(1);
      } else if (OpenableColumns.SIZE.equals(col)) {
        cols[i] = OpenableColumns.SIZE;
        values[i++] = privateFile.length();
      }
    }

    // Set arrays to only matched columns.
    cols = Arrays.copyOf(cols, i);
    values = Arrays.copyOf(values, i);

    MatrixCursor cursor = new MatrixCursor(cols, 1);
    cursor.addRow(values);
    return cursor;
  }

  @Override
  public int update(
      @NonNull Uri uri,
      @Nullable ContentValues values,
      @Nullable String selection,
      @Nullable String[] selectionArgs) {
    return 0;
  }
}
