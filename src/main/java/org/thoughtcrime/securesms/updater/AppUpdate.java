package org.thoughtcrime.securesms.updater;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import androidx.fragment.app.FragmentActivity;
import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import java.io.File;
import org.json.JSONException;
import org.json.JSONObject;
import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Prefs;

public class AppUpdate {
  private static final String TAG = "AppUpdate";

  private static final String APP_KEY = "deltachat";

  static final String APK_FILENAME = "deltachat-update.apk";

  private static final String MOCK_LATEST_VERSIONS_JSON =
      "{"
          + "  \"deltachat\": {"
          + "    \"gplay\": {"
          + "      \"version_integer\": 755,"
          + "      \"version_string\": \"2.57.0\","
          + "      \"release_timestamp\": 1782499168,"
          + "      \"download_url\": \"https://github.com/deltachat/deltachat-android/releases/download/v2.57.0/deltachat-gplay-release-2.57.0.apk\""
          + "    }"
          + "  }"
          + "}";

  private static volatile JSONObject cachedLatestVersions;

  public static class LatestVersion {
    public final int versionInteger;
    public final String versionString;
    public final long releaseTimestamp;
    public final String downloadUrl;

    LatestVersion(
        int versionInteger, String versionString, long releaseTimestamp, String downloadUrl) {
      this.versionInteger = versionInteger;
      this.versionString = versionString;
      this.releaseTimestamp = releaseTimestamp;
      this.downloadUrl = downloadUrl;
    }
  }

  /** Mock function. */
  public static JSONObject getLatestVersions() {
    JSONObject cached = cachedLatestVersions;
    if (cached != null) return cached;
    try {
      cached = new JSONObject(MOCK_LATEST_VERSIONS_JSON);
      cachedLatestVersions = cached;
      return cached;
    } catch (JSONException e) {
      return null;
    }
  }

  public static LatestVersion getLatestForThisBuild() {
    JSONObject all = getLatestVersions();
    if (all == null) return null;
    JSONObject app = all.optJSONObject(APP_KEY);
    if (app == null) return null;
    JSONObject flavor = app.optJSONObject(BuildConfig.FLAVOR);
    if (flavor == null) return null;
    try {
      return new LatestVersion(
          flavor.getInt("version_integer"),
          flavor.getString("version_string"),
          flavor.getLong("release_timestamp"),
          flavor.getString("download_url"));
    } catch (JSONException e) {
      return null;
    }
  }

  private static int getTargetVersion() {
    LatestVersion latest = getLatestForThisBuild();
    if (latest == null || latest.versionInteger <= BuildConfig.VERSION_CODE) return 0;
    return latest.versionInteger;
  }

  public static synchronized void reconcile(Context context) {
    try {
      int target = getTargetVersion();
      if (Prefs.getUpdateMsgVersion(context) == target) return;

      deleteStoredMsg(context);

      if (target == 0) {
        deleteDownloadedApk(context);
        Prefs.setUpdateDeviceMsg(context, 0, 0, 0);
        return;
      }

      DcContext dcContext = DcHelper.getContext(context);
      DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
      msg.setText(context.getString(R.string.update_available_msg));
      int msgId = dcContext.addDeviceMsg(null, msg);
      Prefs.setUpdateDeviceMsg(context, target, dcContext.getAccountId(), msgId);
    } catch (Exception e) {
      Log.e(TAG, "reconcile() failed", e);
    }
  }

  private static void deleteStoredMsg(Context context) {
    int msgId = Prefs.getUpdateMsgId(context);
    if (msgId == 0) return;
    try {
      DcContext dcContext =
          DcHelper.getAccounts(context).getAccount(Prefs.getUpdateMsgAccountId(context));
      if (!dcContext.isOk()) return;
      DcMsg msg = dcContext.getMsg(msgId);
      if (msg.isOk() && msg.getFromId() == DcContact.DC_CONTACT_ID_DEVICE) {
        dcContext.deleteMsgs(new int[] {msgId});
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
    if (msg == null || dcChat == null) return false;
    int msgId = Prefs.getUpdateMsgId(context);
    if (msgId == 0) return false;
    if (Prefs.getUpdateMsgVersion(context) <= BuildConfig.VERSION_CODE) return false;
    if (dcChat.getAccountId() != Prefs.getUpdateMsgAccountId(context)) return false;
    return msg.getId() == msgId && msg.getFromId() == DcContact.DC_CONTACT_ID_DEVICE;
  }

  public static void updateDeviceMsgTapped(FragmentActivity activity) {
    LatestVersion latest = getLatestForThisBuild();
    if (latest == null || activity == null) return;
    AppUpdateDialogFragment.show(activity, latest);
  }
}
