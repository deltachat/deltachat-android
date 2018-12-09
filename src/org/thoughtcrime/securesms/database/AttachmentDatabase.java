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
package org.thoughtcrime.securesms.database;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import org.thoughtcrime.securesms.attachments.AttachmentId;
import org.thoughtcrime.securesms.crypto.AttachmentSecret;
import org.thoughtcrime.securesms.crypto.ClassicDecryptingPartInputStream;
import org.thoughtcrime.securesms.crypto.ModernDecryptingPartInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class AttachmentDatabase extends Database {
  
  private static final String TAG = AttachmentDatabase.class.getSimpleName();

  public  static final String DATA                   = "_data";
  public  static final String SIZE                   = "data_size";

  public static final int TRANSFER_PROGRESS_DONE    = 0;
  public static final int TRANSFER_PROGRESS_STARTED = 1;
  public static final int TRANSFER_PROGRESS_FAILED  = 3;

  private final AttachmentSecret attachmentSecret;

  public AttachmentDatabase(Context context, AttachmentSecret attachmentSecret) {
    super(context);
    this.attachmentSecret = attachmentSecret;
  }

  public @NonNull InputStream getAttachmentStream(AttachmentId attachmentId, long offset)
      throws IOException
  {
    InputStream dataStream = getDataStream(attachmentId, DATA, offset);

    if (dataStream == null) throw new IOException("No stream for: " + attachmentId);
    else                    return dataStream;
  }

  @SuppressWarnings("WeakerAccess")
  @VisibleForTesting
  protected @Nullable InputStream getDataStream(AttachmentId attachmentId, String dataType, long offset)
  {
    DataInfo dataInfo = getAttachmentDataFileInfo(attachmentId, dataType);

    if (dataInfo == null) {
      return null;
    }

    try {
      if (dataInfo.random != null && dataInfo.random.length == 32) {
        return ModernDecryptingPartInputStream.createFor(attachmentSecret, dataInfo.random, dataInfo.file, offset);
      } else {
        InputStream stream  = ClassicDecryptingPartInputStream.createFor(attachmentSecret, dataInfo.file);
        long        skipped = stream.skip(offset);

        if (skipped != offset) {
          Log.w(TAG, "Skip failed: " + skipped + " vs " + offset);
          return null;
        }

        return stream;
      }
    } catch (IOException e) {
      Log.w(TAG, e);
      return null;
    }
  }

  private @Nullable DataInfo getAttachmentDataFileInfo(@NonNull AttachmentId attachmentId, @NonNull String dataType)
  {
    return null;
  }

  private static class DataInfo {
    private final File   file;
    private final byte[] random;

    private DataInfo(File file, long length, byte[] random) {
      this.file = file;
      this.random = random;
    }
  }
}
