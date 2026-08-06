package org.thoughtcrime.securesms.updater;

import android.content.Context;
import androidx.fragment.app.FragmentActivity;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import org.json.JSONException;
import org.json.JSONObject;
import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;

public class AppUpdate {
  private static final String TAG = "AppUpdate";

  private static final String APP_KEY = "deltachat";

  private static final long ONE_WEEK_SECONDS = 7L * 24 * 60 * 60;

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
    try {
      return new JSONObject(MOCK_LATEST_VERSIONS_JSON);
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

  public static boolean isUpdateAvailable() {
    LatestVersion latest = getLatestForThisBuild();
    return latest != null && latest.versionInteger > BuildConfig.VERSION_CODE;
  }

  public static String buildUpdateText(Context context) {
    return context.getString(R.string.update_available_msg);
  }

  public static boolean isEligible(Context context) {
    LatestVersion latest = getLatestForThisBuild();
    if (latest == null) return false;
    if (latest.versionInteger <= BuildConfig.VERSION_CODE) return false;
    long nowSeconds = System.currentTimeMillis() / 1000L;
    return latest.releaseTimestamp <= nowSeconds - ONE_WEEK_SECONDS;
  }

  public static void addUpdateDeviceMsg(Context context) {
    LatestVersion latest = getLatestForThisBuild();
    if (latest == null) return;
    DcContext dcContext = DcHelper.getContext(context);
    DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
    msg.setText(buildUpdateText(context));
    dcContext.addDeviceMsg("update_" + latest.versionInteger, msg);
  }

  public static boolean isUpdateDeviceMsg(Context context, DcMsg msg) {
    return msg != null
        && msg.getFromId() == DcContact.DC_CONTACT_ID_DEVICE
        && isUpdateAvailable()
        && buildUpdateText(context).equals(msg.getText());
  }

  public static void updateDeviceMsgTapped(FragmentActivity activity) {
    LatestVersion latest = getLatestForThisBuild();
    if (latest == null || activity == null) return;
    AppUpdateDialogFragment.show(activity, latest);
  }
}
