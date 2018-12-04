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

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import net.sqlcipher.database.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.thoughtcrime.securesms.attachments.AttachmentId;
import org.thoughtcrime.securesms.attachments.DatabaseAttachment;
import org.thoughtcrime.securesms.crypto.AttachmentSecret;
import org.thoughtcrime.securesms.crypto.ClassicDecryptingPartInputStream;
import org.thoughtcrime.securesms.crypto.ModernDecryptingPartInputStream;
import org.thoughtcrime.securesms.crypto.ModernEncryptingPartOutputStream;
import org.thoughtcrime.securesms.database.helpers.SQLCipherOpenHelper;
import org.thoughtcrime.securesms.mms.MmsException;
import org.thoughtcrime.securesms.util.JsonUtils;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.MediaUtil.ThumbnailData;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.video.EncryptedMediaDataSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class AttachmentDatabase extends Database {
  
  private static final String TAG = AttachmentDatabase.class.getSimpleName();

  public  static final String TABLE_NAME             = "part";
  public  static final String ROW_ID                 = "_id";
          static final String ATTACHMENT_JSON_ALIAS  = "attachment_json";
  public  static final String MMS_ID                 = "mid";
          static final String CONTENT_TYPE           = "ct";
          static final String NAME                   = "name";
          static final String CONTENT_DISPOSITION    = "cd";
          static final String CONTENT_LOCATION       = "cl";
  public  static final String DATA                   = "_data";
          static final String TRANSFER_STATE         = "pending_push";
  public  static final String SIZE                   = "data_size";
          static final String FILE_NAME              = "file_name";
  public  static final String THUMBNAIL              = "thumbnail";
          static final String THUMBNAIL_ASPECT_RATIO = "aspect_ratio";
  public  static final String UNIQUE_ID              = "unique_id";
          static final String DIGEST                 = "digest";
          static final String VOICE_NOTE             = "voice_note";
          static final String QUOTE                  = "quote";
          static final String FAST_PREFLIGHT_ID      = "fast_preflight_id";
  public  static final String DATA_RANDOM            = "data_random";
  private static final String THUMBNAIL_RANDOM       = "thumbnail_random";
          static final String WIDTH                  = "width";
          static final String HEIGHT                 = "height";

  public  static final String DIRECTORY              = "parts";

  public static final int TRANSFER_PROGRESS_DONE    = 0;
  public static final int TRANSFER_PROGRESS_STARTED = 1;
  public static final int TRANSFER_PROGRESS_PENDING = 2;
  public static final int TRANSFER_PROGRESS_FAILED  = 3;

  private static final String PART_ID_WHERE = ROW_ID + " = ? AND " + UNIQUE_ID + " = ?";

  private static final String[] PROJECTION = new String[] {ROW_ID,
                                                           MMS_ID, CONTENT_TYPE, NAME, CONTENT_DISPOSITION,
                                                           CONTENT_LOCATION, DATA, THUMBNAIL, TRANSFER_STATE,
                                                           SIZE, FILE_NAME, THUMBNAIL, THUMBNAIL_ASPECT_RATIO,
                                                           UNIQUE_ID, DIGEST, FAST_PREFLIGHT_ID, VOICE_NOTE,
                                                           QUOTE, DATA_RANDOM, THUMBNAIL_RANDOM, WIDTH, HEIGHT};

  public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ROW_ID + " INTEGER PRIMARY KEY, " +
    MMS_ID + " INTEGER, " + "seq" + " INTEGER DEFAULT 0, "                        +
    CONTENT_TYPE + " TEXT, " + NAME + " TEXT, " + "chset" + " INTEGER, "             +
    CONTENT_DISPOSITION + " TEXT, " + "fn" + " TEXT, " + "cid" + " TEXT, "  +
    CONTENT_LOCATION + " TEXT, " + "ctt_s" + " INTEGER, "                 +
    "ctt_t" + " TEXT, " + "encrypted" + " INTEGER, "                         +
    TRANSFER_STATE + " INTEGER, "+ DATA + " TEXT, " + SIZE + " INTEGER, "   +
    FILE_NAME + " TEXT, " + THUMBNAIL + " TEXT, " + THUMBNAIL_ASPECT_RATIO + " REAL, " +
    UNIQUE_ID + " INTEGER NOT NULL, " + DIGEST + " BLOB, " + FAST_PREFLIGHT_ID + " TEXT, " +
    VOICE_NOTE + " INTEGER DEFAULT 0, " + DATA_RANDOM + " BLOB, " + THUMBNAIL_RANDOM + " BLOB, " +
    QUOTE + " INTEGER DEFAULT 0, " + WIDTH + " INTEGER DEFAULT 0, " + HEIGHT + " INTEGER DEFAULT 0);";

  public static final String[] CREATE_INDEXS = {
    "CREATE INDEX IF NOT EXISTS part_mms_id_index ON " + TABLE_NAME + " (" + MMS_ID + ");",
    "CREATE INDEX IF NOT EXISTS pending_push_index ON " + TABLE_NAME + " (" + TRANSFER_STATE + ");",
  };

  private final ExecutorService thumbnailExecutor = Util.newSingleThreadedLifoExecutor();

  private final AttachmentSecret attachmentSecret;

  public AttachmentDatabase(Context context, SQLCipherOpenHelper databaseHelper, AttachmentSecret attachmentSecret) {
    super(context, databaseHelper);
    this.attachmentSecret = attachmentSecret;
  }

  public @NonNull InputStream getAttachmentStream(AttachmentId attachmentId, long offset)
      throws IOException
  {
    InputStream dataStream = getDataStream(attachmentId, DATA, offset);

    if (dataStream == null) throw new IOException("No stream for: " + attachmentId);
    else                    return dataStream;
  }

  public @NonNull InputStream getThumbnailStream(@NonNull AttachmentId attachmentId)
      throws IOException
  {
    Log.w(TAG, "getThumbnailStream(" + attachmentId + ")");
    InputStream dataStream = getDataStream(attachmentId, THUMBNAIL, 0);

    if (dataStream != null) {
      return dataStream;
    }

    try {
      InputStream generatedStream = thumbnailExecutor.submit(new ThumbnailFetchCallable(attachmentId)).get();

      if (generatedStream == null) throw new FileNotFoundException("No thumbnail stream available: " + attachmentId);
      else                         return generatedStream;
    } catch (InterruptedException ie) {
      throw new AssertionError("interrupted");
    } catch (ExecutionException ee) {
      Log.w(TAG, ee);
      throw new IOException(ee);
    }
  }

  public @Nullable DatabaseAttachment getAttachment(@NonNull AttachmentId attachmentId)
  {
    SQLiteDatabase database = databaseHelper.getReadableDatabase();
    Cursor cursor           = null;

    try {
      cursor = database.query(TABLE_NAME, PROJECTION, PART_ID_WHERE, attachmentId.toStrings(), null, null, null);

      if (cursor != null && cursor.moveToFirst()) {
        List<DatabaseAttachment> list = getAttachment(cursor);

        if (list != null && list.size() > 0) {
          return list.get(0);
        }
      }

      return null;
    } finally {
      if (cursor != null)
        cursor.close();
    }
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
    SQLiteDatabase database = databaseHelper.getReadableDatabase();
    Cursor         cursor   = null;

    String randomColumn;

    switch (dataType) {
      case DATA:      randomColumn = DATA_RANDOM;      break;
      case THUMBNAIL: randomColumn = THUMBNAIL_RANDOM; break;
      default:throw   new AssertionError("Unknown data type: " + dataType);
    }

    try {
      cursor = database.query(TABLE_NAME, new String[]{dataType, SIZE, randomColumn}, PART_ID_WHERE, attachmentId.toStrings(),
                              null, null, null);

      if (cursor != null && cursor.moveToFirst()) {
        if (cursor.isNull(0)) {
          return null;
        }

        return new DataInfo(new File(cursor.getString(0)),
                            cursor.getLong(1),
                            cursor.getBlob(2));
      } else {
        return null;
      }
    } finally {
      if (cursor != null)
        cursor.close();
    }

  }

  private @NonNull DataInfo setAttachmentData(@NonNull InputStream in)
      throws MmsException
  {
    try {
      File partsDirectory = context.getDir(DIRECTORY, Context.MODE_PRIVATE);
      File dataFile       = File.createTempFile("part", ".mms", partsDirectory);
      return setAttachmentData(dataFile, in);
    } catch (IOException e) {
      throw new MmsException(e);
    }
  }

  private @NonNull DataInfo setAttachmentData(@NonNull File destination, @NonNull InputStream in)
      throws MmsException
  {
    try {
      Pair<byte[], OutputStream> out    = ModernEncryptingPartOutputStream.createFor(attachmentSecret, destination, false);
      long                       length = Util.copy(in, out.second);

      return new DataInfo(destination, length, out.first);
    } catch (IOException e) {
      throw new MmsException(e);
    }
  }

  public List<DatabaseAttachment> getAttachment(@NonNull Cursor cursor) {
    try {
      if (cursor.getColumnIndex(AttachmentDatabase.ATTACHMENT_JSON_ALIAS) != -1) {
        if (cursor.isNull(cursor.getColumnIndexOrThrow(ATTACHMENT_JSON_ALIAS))) {
          return new LinkedList<>();
        }

        List<DatabaseAttachment> result = new LinkedList<>();
        JSONArray                array  = new JSONArray(cursor.getString(cursor.getColumnIndexOrThrow(ATTACHMENT_JSON_ALIAS)));

        for (int i=0;i<array.length();i++) {
          JsonUtils.SaneJSONObject object = new JsonUtils.SaneJSONObject(array.getJSONObject(i));

          if (!object.isNull(ROW_ID)) {
            result.add(new DatabaseAttachment(new AttachmentId(object.getLong(ROW_ID), object.getLong(UNIQUE_ID)),
                                              object.getLong(MMS_ID),
                                              !TextUtils.isEmpty(object.getString(DATA)),
                                              !TextUtils.isEmpty(object.getString(THUMBNAIL)),
                                              object.getString(CONTENT_TYPE),
                                              object.getInt(TRANSFER_STATE),
                                              object.getLong(SIZE),
                                              object.getString(FILE_NAME),
                                              object.getString(CONTENT_LOCATION),
                                              object.getString(CONTENT_DISPOSITION),
                                              object.getString(NAME),
                                              null,
                                              object.getString(FAST_PREFLIGHT_ID),
                                              object.getInt(VOICE_NOTE) == 1,
                                              object.getInt(WIDTH),
                                              object.getInt(HEIGHT),
                                              object.getInt(QUOTE) == 1));
          }
        }

        return result;
      } else {
        return Collections.singletonList(new DatabaseAttachment(new AttachmentId(cursor.getLong(cursor.getColumnIndexOrThrow(ROW_ID)),
                                                                                 cursor.getLong(cursor.getColumnIndexOrThrow(UNIQUE_ID))),
                                                                cursor.getLong(cursor.getColumnIndexOrThrow(MMS_ID)),
                                                                !cursor.isNull(cursor.getColumnIndexOrThrow(DATA)),
                                                                !cursor.isNull(cursor.getColumnIndexOrThrow(THUMBNAIL)),
                                                                cursor.getString(cursor.getColumnIndexOrThrow(CONTENT_TYPE)),
                                                                cursor.getInt(cursor.getColumnIndexOrThrow(TRANSFER_STATE)),
                                                                cursor.getLong(cursor.getColumnIndexOrThrow(SIZE)),
                                                                cursor.getString(cursor.getColumnIndexOrThrow(FILE_NAME)),
                                                                cursor.getString(cursor.getColumnIndexOrThrow(CONTENT_LOCATION)),
                                                                cursor.getString(cursor.getColumnIndexOrThrow(CONTENT_DISPOSITION)),
                                                                cursor.getString(cursor.getColumnIndexOrThrow(NAME)),
                                                                cursor.getBlob(cursor.getColumnIndexOrThrow(DIGEST)),
                                                                cursor.getString(cursor.getColumnIndexOrThrow(FAST_PREFLIGHT_ID)),
                                                                cursor.getInt(cursor.getColumnIndexOrThrow(VOICE_NOTE)) == 1,
                                                                cursor.getInt(cursor.getColumnIndexOrThrow(WIDTH)),
                                                                cursor.getInt(cursor.getColumnIndexOrThrow(HEIGHT)),
                                                                cursor.getInt(cursor.getColumnIndexOrThrow(QUOTE)) == 1));
      }
    } catch (JSONException e) {
      throw new AssertionError(e);
    }
  }


  @SuppressWarnings("WeakerAccess")
  @VisibleForTesting
  protected void updateAttachmentThumbnail(AttachmentId attachmentId, InputStream in, float aspectRatio)
      throws MmsException
  {
    Log.w(TAG, "updating part thumbnail for #" + attachmentId);

    DataInfo thumbnailFile = setAttachmentData(in);

    SQLiteDatabase database = databaseHelper.getWritableDatabase();
    ContentValues  values   = new ContentValues(2);

    values.put(THUMBNAIL, thumbnailFile.file.getAbsolutePath());
    values.put(THUMBNAIL_ASPECT_RATIO, aspectRatio);
    values.put(THUMBNAIL_RANDOM, thumbnailFile.random);

    database.update(TABLE_NAME, values, PART_ID_WHERE, attachmentId.toStrings());

    Cursor cursor = database.query(TABLE_NAME, new String[] {MMS_ID}, PART_ID_WHERE, attachmentId.toStrings(), null, null, null);

    try {
      if (cursor != null && cursor.moveToFirst()) {
        notifyConversationListeners(DatabaseFactory.getMmsDatabase(context).getThreadIdForMessage(cursor.getLong(cursor.getColumnIndexOrThrow(MMS_ID))));
      }
    } finally {
      if (cursor != null) cursor.close();
    }
  }


  @VisibleForTesting
  class ThumbnailFetchCallable implements Callable<InputStream> {

    private final AttachmentId attachmentId;

    ThumbnailFetchCallable(AttachmentId attachmentId) {
      this.attachmentId = attachmentId;
    }

    @Override
    public @Nullable InputStream call() throws Exception {
      Log.w(TAG, "Executing thumbnail job...");
      final InputStream stream = getDataStream(attachmentId, THUMBNAIL, 0);

      if (stream != null) {
        return stream;
      }

      DatabaseAttachment attachment = getAttachment(attachmentId);

      if (attachment == null || !attachment.hasData()) {
        return null;
      }

      ThumbnailData data = null;

      if (MediaUtil.isVideoType(attachment.getContentType())) {
        data = generateVideoThumbnail(attachmentId);
      }

      if (data == null) {
        return null;
      }

      updateAttachmentThumbnail(attachmentId, data.toDataStream(), data.getAspectRatio());

      return getDataStream(attachmentId, THUMBNAIL, 0);
    }

    @SuppressLint("NewApi")
    private ThumbnailData generateVideoThumbnail(AttachmentId attachmentId) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        Log.w(TAG, "Video thumbnails not supported...");
        return null;
      }

      DataInfo dataInfo = getAttachmentDataFileInfo(attachmentId, DATA);

      if (dataInfo == null) {
        Log.w(TAG, "No data file found for video thumbnail...");
        return null;
      }

      EncryptedMediaDataSource dataSource = new EncryptedMediaDataSource(attachmentSecret, dataInfo.file, dataInfo.random, dataInfo.length);
      MediaMetadataRetriever   retriever  = new MediaMetadataRetriever();
      retriever.setDataSource(dataSource);

      Bitmap bitmap = retriever.getFrameAtTime(1000);

      Log.w(TAG, "Generated video thumbnail...");
      return new ThumbnailData(bitmap);
    }
  }

  private static class DataInfo {
    private final File   file;
    private final long   length;
    private final byte[] random;

    private DataInfo(File file, long length, byte[] random) {
      this.file = file;
      this.length = length;
      this.random = random;
    }
  }
}
