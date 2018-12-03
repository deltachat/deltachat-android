package org.thoughtcrime.securesms.database;


import android.content.ContentValues;
import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;

import org.thoughtcrime.securesms.database.helpers.SQLCipherOpenHelper;

import java.util.List;

public class GroupReceiptDatabase extends Database {

  public  static final String TABLE_NAME = "group_receipts";

  private static final String ID        = "_id";
  public  static final String MMS_ID    = "mms_id";
  private static final String ADDRESS   = "address";
  private static final String STATUS    = "status";
  private static final String TIMESTAMP = "timestamp";

  public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY, "                          +
      MMS_ID + " INTEGER, " + ADDRESS + " TEXT, " + STATUS + " INTEGER, " + TIMESTAMP + " INTEGER);";

  public static final String[] CREATE_INDEXES = {
      "CREATE INDEX IF NOT EXISTS group_receipt_mms_id_index ON " + TABLE_NAME + " (" + MMS_ID + ");",
  };

  public GroupReceiptDatabase(Context context, SQLCipherOpenHelper databaseHelper) {
    super(context, databaseHelper);
  }

  public void insert(List<Address> addresses, long mmsId, int status, long timestamp) {
    SQLiteDatabase db = databaseHelper.getWritableDatabase();

    for (Address address : addresses) {
      ContentValues values = new ContentValues(4);
      values.put(MMS_ID, mmsId);
      values.put(ADDRESS, address.serialize());
      values.put(STATUS, status);
      values.put(TIMESTAMP, timestamp);

      db.insert(TABLE_NAME, null, values);
    }
  }

  public void update(Address address, long mmsId, int status, long timestamp) {
    SQLiteDatabase db     = databaseHelper.getWritableDatabase();
    ContentValues  values = new ContentValues(2);
    values.put(STATUS, status);
    values.put(TIMESTAMP, timestamp);

    db.update(TABLE_NAME, values, MMS_ID + " = ? AND " + ADDRESS + " = ? AND " + STATUS + " < ?",
              new String[] {String.valueOf(mmsId), address.serialize(), String.valueOf(status)});
  }

  void deleteRowsForMessage(long mmsId) {
    SQLiteDatabase db = databaseHelper.getWritableDatabase();
    db.delete(TABLE_NAME, MMS_ID + " = ?", new String[] {String.valueOf(mmsId)});
  }
}
