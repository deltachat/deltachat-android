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
import android.database.Cursor;

import net.sqlcipher.database.SQLiteDatabase;

import org.thoughtcrime.securesms.database.helpers.SQLCipherOpenHelper;
import org.whispersystems.libsignal.IdentityKey;

public class IdentityDatabase extends Database {

  @SuppressWarnings("unused")
  private static final String TAG = IdentityDatabase.class.getSimpleName();

  private static final String TABLE_NAME           = "identities";
  private static final String ID                   = "_id";
  private static final String ADDRESS              = "address";
  private static final String IDENTITY_KEY         = "key";
  private static final String TIMESTAMP            = "timestamp";
  private static final String FIRST_USE            = "first_use";
  private static final String NONBLOCKING_APPROVAL = "nonblocking_approval";
  private static final String VERIFIED             = "verified";

  public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME +
      " (" + ID + " INTEGER PRIMARY KEY, " +
      ADDRESS + " TEXT UNIQUE, " +
      IDENTITY_KEY + " TEXT, " +
      FIRST_USE + " INTEGER DEFAULT 0, " +
      TIMESTAMP + " INTEGER DEFAULT 0, " +
      VERIFIED + " INTEGER DEFAULT 0, " +
      NONBLOCKING_APPROVAL + " INTEGER DEFAULT 0);";

  public enum VerifiedStatus {
    DEFAULT, VERIFIED, UNVERIFIED;

  }

  IdentityDatabase(Context context, SQLCipherOpenHelper databaseHelper) {
    super(context, databaseHelper);
  }

  public Cursor getIdentities() {
    SQLiteDatabase database = databaseHelper.getReadableDatabase();
    return database.query(TABLE_NAME, null, null, null, null, null, null);
  }

  public static class IdentityRecord {

    private final Address        address;
    private final IdentityKey    identitykey;
    private final VerifiedStatus verifiedStatus;
    private final boolean        firstUse;
    private final long           timestamp;

    private IdentityRecord(Address address,
                           IdentityKey identitykey, VerifiedStatus verifiedStatus,
                           boolean firstUse, long timestamp, boolean nonblockingApproval)
    {
      this.address             = address;
      this.identitykey         = identitykey;
      this.verifiedStatus      = verifiedStatus;
      this.firstUse            = firstUse;
      this.timestamp           = timestamp;
    }

    public Address getAddress() {
      return address;
    }

    public long getTimestamp() {
      return timestamp;
    }

    @Override
    public String toString() {
      return "{address: " + address + ", identityKey: " + identitykey + ", verifiedStatus: " + verifiedStatus + ", firstUse: " + firstUse + "}";
    }

  }
}
