package org.thoughtcrime.securesms.connect;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.b44t.messenger.DcContext;

import java.io.File;
import java.io.FileNotFoundException;

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
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        DcContext dcContext = DcHelper.getContext(getContext());

        String path = uri.getPath();
        if (!DcHelper.sharedFiles.containsKey(path)) {
            throw new FileNotFoundException("File was not shared before.");
        }

        File privateFile = new File(dcContext.getBlobdir(), path);
        return ParcelFileDescriptor.open(privateFile, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        return 0;
    }

    @Override
    public String getType(Uri arg0) {
        return null;
    }

    @Override
    public Uri insert(Uri arg0, ContentValues arg1) {
        return null;
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri arg0, String[] arg1, String arg2, String[] arg3,
                        String arg4) {
        return null;
    }

    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        return 0;
    }
}
