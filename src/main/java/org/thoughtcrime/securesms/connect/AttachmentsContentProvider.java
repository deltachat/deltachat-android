package org.thoughtcrime.securesms.connect;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.util.MediaUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;

public class AttachmentsContentProvider extends ContentProvider {

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
    public int delete(@NonNull Uri arg0, String arg1, String[] arg2) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        String file = uri.getPathSegments().get(0);
        String mimeType = DcHelper.sharedFiles.get(file);

        return DcHelper.checkMime(uri.toString(), mimeType);
    }

    @Override
    public String getTypeAnonymous(Uri uri) {
        String ext = MediaUtil.getFileExtensionFromUrl(uri.toString());
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
    }

    @Override
    public Uri insert(@NonNull Uri arg0, ContentValues arg1) {
        return null;
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(@NonNull Uri arg0, String[] arg1, String arg2, String[] arg3,
                        String arg4) {
        return null;
    }

    @Override
    public int update(@NonNull Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        return 0;
    }
}
