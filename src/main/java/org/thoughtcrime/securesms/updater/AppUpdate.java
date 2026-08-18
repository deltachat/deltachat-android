package org.thoughtcrime.securesms.updater;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import androidx.fragment.app.FragmentActivity;
import chat.delta.rpc.Rpc;
import chat.delta.rpc.types.AppSource;
import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import java.io.File;
import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Prefs;

public class AppUpdate {
  private static final String TAG = "AppUpdate";

  private static final String CLIENT_ID = "deltachat";
  private static final String SOURCE_ID = "gplay";
  private static final String PLAY_STORE_PACKAGE = "com.android.vending";

  private static final long CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L;

  private static final String SEPARATOR = "\n";

  static final String APK_FILENAME = "deltachat-update.apk";

  private static class StoredUpdate {
    final int version;
    final int msgId;
    final int accountId;
    final String versionString;
    final String downloadUrl;

    StoredUpdate(int version, int msgId, int accountId, String versionString, String downloadUrl) {
      this.version = version;
      this.msgId = msgId;
      this.accountId = accountId;
      this.versionString = versionString;
      this.downloadUrl = downloadUrl;
    }

    static StoredUpdate load(Context context) {
      String raw = Prefs.getUpdateMsg(context);
      if (TextUtils.isEmpty(raw)) return null;
      String[] parts = raw.split(SEPARATOR, -1);
      if (parts.length < 5) return null;
      try {
        return new StoredUpdate(
            Integer.parseInt(parts[0]),
            Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2]),
            parts[3],
            parts[4]);
      } catch (NumberFormatException e) {
        Log.w(TAG, "could not parse stored update info", e);
        return null;
      }
    }

    void save(Context context) {
      Prefs.setUpdateMsg(
          context,
          version
              + SEPARATOR
              + msgId
              + SEPARATOR
              + accountId
              + SEPARATOR
              + versionString
              + SEPARATOR
              + downloadUrl);
    }

    static void clear(Context context) {
      Prefs.setUpdateMsg(context, "");
    }
  }

  public static boolean isSelfUpdateEnabled(Context context) {
    if (!SOURCE_ID.equals(BuildConfig.FLAVOR)) return false;
    try {
      PackageManager pm = context.getPackageManager();
      String installer;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        installer = pm.getInstallSourceInfo(context.getPackageName()).getInstallingPackageName();
      } else {
        installer = pm.getInstallerPackageName(context.getPackageName());
      }
      return !PLAY_STORE_PACKAGE.equals(installer);
    } catch (Exception e) {
      Log.w(TAG, "cannot determine install source", e);
      return false;
    }
  }

  public static synchronized void cleanupUpdated(Context context) {
    try {
      StoredUpdate stored = StoredUpdate.load(context);
      if (stored == null || stored.version > BuildConfig.VERSION_CODE) return;
      deleteStoredMsg(context, stored);
      deleteDownloadedApk(context);
      StoredUpdate.clear(context);
    } catch (Exception e) {
      Log.e(TAG, "cleanupUpdated() failed", e);
    }
  }

  public static synchronized void maybeCheckUpdate(Context context) {
    try {
      if (!isSelfUpdateEnabled(context)) return;

      long now = System.currentTimeMillis();
      long lastCheck = Prefs.getUpdateLastCheck(context);
      if (lastCheck > now) {
        lastCheck = 0;
      }
      if (lastCheck != 0 && now - lastCheck < CHECK_INTERVAL_MS) return;

      Rpc rpc = DcHelper.getRpc(context);
      AppSource source = rpc.getAppVersion(CLIENT_ID, SOURCE_ID);
      if (source == null || source.versionInteger == null) return;

      Prefs.setUpdateLastCheck(context, now);

      int version = source.versionInteger;
      StoredUpdate stored = StoredUpdate.load(context);
      if (stored != null && stored.version == version) return;

      if (stored != null) {
        deleteStoredMsg(context, stored);
        StoredUpdate.clear(context);
      }

      if (version <= BuildConfig.VERSION_CODE) {
        deleteDownloadedApk(context);
        return;
      }

      if (TextUtils.isEmpty(source.versionString) || TextUtils.isEmpty(source.downloadUrl)) {
        Log.w(TAG, "incomplete version info");
        return;
      }

      DcContext dcContext = DcHelper.getContext(context);
      DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
      msg.setText(context.getString(R.string.update_available_msg));
      int msgId = dcContext.addDeviceMsg(null, msg);
      new StoredUpdate(
              version, msgId, dcContext.getAccountId(), source.versionString, source.downloadUrl)
          .save(context);
    } catch (Exception e) {
      Log.e(TAG, "maybeCheckUpdate() failed", e);
    }
  }

  private static void deleteStoredMsg(Context context, StoredUpdate stored) {
    if (stored.msgId == 0) return;
    try {
      DcContext dcContext = DcHelper.getAccounts(context).getAccount(stored.accountId);
      if (!dcContext.isOk()) return;
      DcMsg msg = dcContext.getMsg(stored.msgId);
      if (msg.isOk() && msg.getFromId() == DcContact.DC_CONTACT_ID_DEVICE) {
        dcContext.deleteMsgs(new int[] {stored.msgId});
      }
    } catch (Exception e) {
      Log.w(TAG, "could not delete update device message", e);
    }
  }

  static File getApkDir(Context context) {
    return context.getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
  }

  private static void deleteDownloadedApk(Context context) {
    File dir = getApkDir(context);
    if (dir == null) return;
    File apk = new File(dir, APK_FILENAME);
    if (apk.exists() && !apk.delete()) {
      Log.w(TAG, "could not delete " + apk);
    }
  }

  public static boolean isUpdateDeviceMsg(Context context, DcChat dcChat, DcMsg msg) {
    if (msg == null) return false;
    StoredUpdate stored = StoredUpdate.load(context);
    return stored != null
        && stored.msgId != 0
        && stored.version > BuildConfig.VERSION_CODE
        && dcChat.getAccountId() == stored.accountId
        && msg.getId() == stored.msgId
        && msg.getFromId() == DcContact.DC_CONTACT_ID_DEVICE;
  }

  public static void updateDeviceMsgTapped(FragmentActivity activity) {
    if (activity == null) return;
    StoredUpdate stored = StoredUpdate.load(activity);
    if (stored == null) return;
    AppUpdateDialogFragment.show(activity, stored.versionString, stored.downloadUrl);
  }
}
