package org.thoughtcrime.securesms.database;


import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Stream;

import net.sqlcipher.database.SQLiteDatabase;

import org.thoughtcrime.securesms.database.helpers.SQLCipherOpenHelper;
import org.thoughtcrime.securesms.util.GroupUtil;
import org.whispersystems.libsignal.util.guava.Optional;

import java.io.Closeable;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;

public class GroupDatabase extends Database {

  @SuppressWarnings("unused")
  private static final String TAG = GroupDatabase.class.getSimpleName();

          static final String TABLE_NAME          = "groups";
  private static final String ID                  = "_id";
          static final String GROUP_ID            = "group_id";
  private static final String TITLE               = "title";
  private static final String MEMBERS             = "members";
  private static final String AVATAR              = "avatar";
  private static final String AVATAR_ID           = "avatar_id";
  private static final String AVATAR_KEY          = "avatar_key";
  private static final String AVATAR_CONTENT_TYPE = "avatar_content_type";
  private static final String AVATAR_RELAY        = "avatar_relay";
  private static final String AVATAR_DIGEST       = "avatar_digest";
  private static final String TIMESTAMP           = "timestamp";
  private static final String ACTIVE              = "active";
  private static final String MMS                 = "mms";

  public static final String CREATE_TABLE =
      "CREATE TABLE " + TABLE_NAME +
          " (" + ID + " INTEGER PRIMARY KEY, " +
          GROUP_ID + " TEXT, " +
          TITLE + " TEXT, " +
          MEMBERS + " TEXT, " +
          AVATAR + " BLOB, " +
          AVATAR_ID + " INTEGER, " +
          AVATAR_KEY + " BLOB, " +
          AVATAR_CONTENT_TYPE + " TEXT, " +
          AVATAR_RELAY + " TEXT, " +
          TIMESTAMP + " INTEGER, " +
          ACTIVE + " INTEGER DEFAULT 1, " +
          AVATAR_DIGEST + " BLOB, " +
          MMS + " INTEGER DEFAULT 0);";

  public static final String[] CREATE_INDEXS = {
      "CREATE UNIQUE INDEX IF NOT EXISTS group_id_index ON " + TABLE_NAME + " (" + GROUP_ID + ");",
  };

  private static final String[] GROUP_PROJECTION = {
      GROUP_ID, TITLE, MEMBERS, AVATAR, AVATAR_ID, AVATAR_KEY, AVATAR_CONTENT_TYPE, AVATAR_RELAY, AVATAR_DIGEST,
      TIMESTAMP, ACTIVE, MMS
  };

  static final List<String> TYPED_GROUP_PROJECTION = Stream.of(GROUP_PROJECTION).map(columnName -> TABLE_NAME + "." + columnName).toList();

  public GroupDatabase(Context context, SQLCipherOpenHelper databaseHelper) {
    super(context, databaseHelper);
  }

  public Optional<GroupRecord> getGroup(String groupId) {
    try (Cursor cursor = databaseHelper.getReadableDatabase().query(TABLE_NAME, null, GROUP_ID + " = ?",
                                                                    new String[] {groupId},
                                                                    null, null, null))
    {
      if (cursor != null && cursor.moveToNext()) {
        return getGroup(cursor);
      }

      return Optional.absent();
    }
  }

  Optional<GroupRecord> getGroup(Cursor cursor) {
    Reader reader = new Reader(cursor);
    return Optional.fromNullable(reader.getCurrent());
  }

  public Reader getGroupsFilteredByTitle(String constraint) {
    @SuppressLint("Recycle")
    Cursor cursor = databaseHelper.getReadableDatabase().query(TABLE_NAME, null, TITLE + " LIKE ?",
                                                                                        new String[]{"%" + constraint + "%"},
                                                                                        null, null, null);

    return new Reader(cursor);
  }

  public Reader getGroups() {
    @SuppressLint("Recycle")
    Cursor cursor = databaseHelper.getReadableDatabase().query(TABLE_NAME, null, null, null, null, null, null);
    return new Reader(cursor);
  }

  public void remove(String groupId, Address source) {
    List<Address> currentMembers = getCurrentMembers(groupId);
    currentMembers.remove(source);

    ContentValues contents = new ContentValues();
    contents.put(MEMBERS, Address.toSerializedList(currentMembers, ','));

    databaseHelper.getWritableDatabase().update(TABLE_NAME, contents, GROUP_ID + " = ?",
                                                new String[] {groupId});
  }

  private List<Address> getCurrentMembers(String groupId) {
    Cursor cursor = null;

    try {
      cursor = databaseHelper.getReadableDatabase().query(TABLE_NAME, new String[] {MEMBERS},
                                                          GROUP_ID + " = ?",
                                                          new String[] {groupId},
                                                          null, null, null);

      if (cursor != null && cursor.moveToFirst()) {
        String serializedMembers = cursor.getString(cursor.getColumnIndexOrThrow(MEMBERS));
        return Address.fromSerializedList(serializedMembers, ',');
      }

      return new LinkedList<>();
    } finally {
      if (cursor != null)
        cursor.close();
    }
  }

  public static class Reader implements Closeable {

    private final Cursor cursor;

    public Reader(Cursor cursor) {
      this.cursor = cursor;
    }

    public @Nullable GroupRecord getNext() {
      if (cursor == null || !cursor.moveToNext()) {
        return null;
      }

      return getCurrent();
    }

    public @Nullable GroupRecord getCurrent() {
      if (cursor == null || cursor.getString(cursor.getColumnIndexOrThrow(GROUP_ID)) == null) {
        return null;
      }

      return new GroupRecord(cursor.getString(cursor.getColumnIndexOrThrow(GROUP_ID)),
                             cursor.getString(cursor.getColumnIndexOrThrow(TITLE)),
                             cursor.getString(cursor.getColumnIndexOrThrow(MEMBERS)),
                             cursor.getBlob(cursor.getColumnIndexOrThrow(AVATAR)),
                             cursor.getLong(cursor.getColumnIndexOrThrow(AVATAR_ID)),
                             cursor.getBlob(cursor.getColumnIndexOrThrow(AVATAR_KEY)),
                             cursor.getString(cursor.getColumnIndexOrThrow(AVATAR_CONTENT_TYPE)),
                             cursor.getString(cursor.getColumnIndexOrThrow(AVATAR_RELAY)),
                             cursor.getInt(cursor.getColumnIndexOrThrow(ACTIVE)) == 1,
                             cursor.getBlob(cursor.getColumnIndexOrThrow(AVATAR_DIGEST)),
                             cursor.getInt(cursor.getColumnIndexOrThrow(MMS)) == 1);
    }

    @Override
    public void close() {
      if (this.cursor != null)
        this.cursor.close();
    }
  }

  public static class GroupRecord {

    private final String        id;
    private final String        title;
    private final List<Address> members;
    private final byte[]        avatar;
    private final long          avatarId;
    private final byte[]        avatarKey;
    private final byte[]        avatarDigest;
    private final String        avatarContentType;
    private final String        relay;
    private final boolean       active;
    private final boolean       mms;

    public GroupRecord(String id, String title, String members, byte[] avatar,
                       long avatarId, byte[] avatarKey, String avatarContentType,
                       String relay, boolean active, byte[] avatarDigest, boolean mms)
    {
      this.id                = id;
      this.title             = title;
      this.avatar            = avatar;
      this.avatarId          = avatarId;
      this.avatarKey         = avatarKey;
      this.avatarDigest      = avatarDigest;
      this.avatarContentType = avatarContentType;
      this.relay             = relay;
      this.active            = active;
      this.mms               = mms;

      if (!TextUtils.isEmpty(members)) this.members = Address.fromSerializedList(members, ',');
      else                             this.members = new LinkedList<>();
    }

    public byte[] getId() {
      try {
        return GroupUtil.getDecodedId(id);
      } catch (IOException ioe) {
        throw new AssertionError(ioe);
      }
    }

    public String getEncodedId() {
      return id;
    }

    public String getTitle() {
      return title;
    }

    public List<Address> getMembers() {
      return members;
    }

    public byte[] getAvatar() {
      return avatar;
    }

    public long getAvatarId() {
      return avatarId;
    }

    public String getRelay() {
      return relay;
    }

    public boolean isActive() {
      return active;
    }

    public boolean isMms() {
      return mms;
    }
  }
}
