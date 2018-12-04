package org.thoughtcrime.securesms.database;

import android.content.Context;

import org.thoughtcrime.securesms.database.helpers.SQLCipherOpenHelper;

public abstract class MessagingDatabase extends Database implements MmsSmsColumns {

  private static final String TAG = MessagingDatabase.class.getSimpleName();

  public MessagingDatabase(Context context, SQLCipherOpenHelper databaseHelper) {
    super(context, databaseHelper);
  }

}
