/**
 * Copyright (C) 2013 Open Whisper Systems
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

package org.thoughtcrime.securesms;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.notifications.MessageNotifier;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.VersionTracker;

import java.util.SortedSet;
import java.util.TreeSet;

public class DatabaseUpgradeActivity extends BaseActivity {

  public static final int NO_MORE_KEY_EXCHANGE_PREFIX_VERSION  = 46;
  public static final int MMS_BODY_VERSION                     = 46;
  public static final int TOFU_IDENTITIES_VERSION              = 50;
  public static final int CURVE25519_VERSION                   = 63;
  public static final int ASYMMETRIC_MASTER_SECRET_FIX_VERSION = 73;
  public static final int NO_V1_VERSION                        = 83;
  public static final int SIGNED_PREKEY_VERSION                = 83;
  public static final int NO_DECRYPT_QUEUE_VERSION             = 113;
  public static final int PUSH_DECRYPT_SERIAL_ID_VERSION       = 131;
  public static final int MIGRATE_SESSION_PLAINTEXT            = 136;
  public static final int CONTACTS_ACCOUNT_VERSION             = 136;
  public static final int MEDIA_DOWNLOAD_CONTROLS_VERSION      = 151;
  public static final int REDPHONE_SUPPORT_VERSION             = 157;
  public static final int NO_MORE_CANONICAL_DB_VERSION         = 276;
  public static final int PROFILES                             = 289;
  public static final int SCREENSHOTS                          = 300;
  public static final int PERSISTENT_BLOBS                     = 317;
  public static final int INTERNALIZE_CONTACTS                 = 317;
  public static final int SQLCIPHER                            = 334;
  public static final int SQLCIPHER_COMPLETE                   = 352;
  public static final int REMOVE_JOURNAL                       = 353;
  public static final int REMOVE_CACHE                         = 354;
  public static final int FULL_TEXT_SEARCH                     = 358;
  public static final int BAD_IMPORT_CLEANUP                   = 373;

  private static final SortedSet<Integer> UPGRADE_VERSIONS = new TreeSet<Integer>() {{
    add(NO_MORE_KEY_EXCHANGE_PREFIX_VERSION);
    add(TOFU_IDENTITIES_VERSION);
    add(CURVE25519_VERSION);
    add(ASYMMETRIC_MASTER_SECRET_FIX_VERSION);
    add(NO_V1_VERSION);
    add(SIGNED_PREKEY_VERSION);
    add(NO_DECRYPT_QUEUE_VERSION);
    add(PUSH_DECRYPT_SERIAL_ID_VERSION);
    add(MIGRATE_SESSION_PLAINTEXT);
    add(MEDIA_DOWNLOAD_CONTROLS_VERSION);
    add(REDPHONE_SUPPORT_VERSION);
    add(NO_MORE_CANONICAL_DB_VERSION);
    add(SCREENSHOTS);
    add(INTERNALIZE_CONTACTS);
    add(PERSISTENT_BLOBS);
    add(SQLCIPHER);
    add(SQLCIPHER_COMPLETE);
    add(REMOVE_CACHE);
    add(FULL_TEXT_SEARCH);
    add(BAD_IMPORT_CLEANUP);
  }};

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    if (needsUpgradeTask()) {
      Log.w("DatabaseUpgradeActivity", "Upgrading...");
      setContentView(R.layout.database_upgrade_activity);

    } else {
      VersionTracker.updateLastSeenVersion(this);
      updateNotifications(this);
      startActivity((Intent)getIntent().getParcelableExtra("next_intent"));
      finish();
    }
  }

  private boolean needsUpgradeTask() {
    int currentVersionCode = Util.getCurrentApkReleaseVersion(this);
    int lastSeenVersion    = VersionTracker.getLastSeenVersion(this);

    Log.w("DatabaseUpgradeActivity", "LastSeenVersion: " + lastSeenVersion);

    if (lastSeenVersion >= currentVersionCode)
      return false;

    for (int version : UPGRADE_VERSIONS) {
      Log.w("DatabaseUpgradeActivity", "Comparing: " + version);
      if (lastSeenVersion < version)
        return true;
    }

    return false;
  }

  public static boolean isUpdate(Context context) {
    try {
      int currentVersionCode  = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
      int previousVersionCode = VersionTracker.getLastSeenVersion(context);

      return previousVersionCode < currentVersionCode;
    } catch (PackageManager.NameNotFoundException e) {
      throw new AssertionError(e);
    }
  }

  @SuppressLint("StaticFieldLeak")
  private void updateNotifications(final Context context) {
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        MessageNotifier.updateNotification(context);
        return null;
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  public interface DatabaseUpgradeListener {
    public void setProgress(int progress, int total);
  }

}
