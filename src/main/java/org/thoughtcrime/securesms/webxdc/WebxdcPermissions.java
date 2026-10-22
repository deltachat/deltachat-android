package org.thoughtcrime.securesms.webxdc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import androidx.preference.PreferenceManager;
import chat.delta.rpc.Rpc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.thoughtcrime.securesms.util.Prefs;

/**
 * Per-webxdc-app permission, following the browser model where each app is a "site". Also stores
 * folder grants the user shared with apps.
 */
public class WebxdcPermissions {
  private static final String TAG = "WebxdcPermissions";
  private static final String KEY_PREFIX = "webxdc_permissions_";
  private static final String KEY_FOLDERS = "folders";

  private static boolean pruneDone = false;

  public enum State {
    ASK,
    ALLOWED,
    DENIED
  }

  public enum Capability {
    MICROPHONE,
    CAMERA,
    LOCATION,
    MOTION_SENSORS
  }

  public static class FolderGrant {
    public final String treeUri;
    public final String name;

    FolderGrant(String treeUri, String name) {
      this.treeUri = treeUri;
      this.name = name;
    }
  }

  private static String key(int accountId, int msgId) {
    return KEY_PREFIX + accountId + "." + msgId;
  }

  private static SharedPreferences prefs(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context);
  }

  private static JSONObject read(Context context, int accountId, int msgId) {
    String json = Prefs.getStringPreference(context, key(accountId, msgId), null);
    try {
      return json == null ? new JSONObject() : new JSONObject(json);
    } catch (JSONException e) {
      Log.w(TAG, "Webxdc permissions record corrupted, resetting", e);
      return new JSONObject();
    }
  }

  private static void write(Context context, int accountId, int msgId, JSONObject record) {
    if (record.length() == 0) {
      Prefs.removePreference(context, key(accountId, msgId));
    } else {
      Prefs.setStringPreference(context, key(accountId, msgId), record.toString());
    }
  }

  private static String capKey(Capability capability) {
    return capability.name().toLowerCase(Locale.US);
  }

  public static State get(Context context, int accountId, int msgId, Capability capability) {
    switch (read(context, accountId, msgId).optString(capKey(capability))) {
      case "allowed":
        return State.ALLOWED;
      case "denied":
        return State.DENIED;
      default:
        return State.ASK;
    }
  }

  public static void set(
      Context context, int accountId, int msgId, Capability capability, State state) {
    try {
      JSONObject record = read(context, accountId, msgId);
      if (state == State.ASK) {
        record.remove(capKey(capability));
      } else {
        record.put(capKey(capability), state == State.ALLOWED ? "allowed" : "denied");
      }
      write(context, accountId, msgId, record);
    } catch (JSONException e) {
      // in-memory kv are valid JSON by construction
    }
  }

  public static void clear(Context context, int accountId, int msgId) {
    List<FolderGrant> folders = getFolders(context, accountId, msgId);
    Prefs.removePreference(context, key(accountId, msgId));
    for (FolderGrant folder : folders) {
      releaseTreeIfUnused(context, folder.treeUri);
    }
  }

  public static synchronized void pruneStaleOnce(Context context, Rpc rpc) {
    if (pruneDone) {
      return;
    }
    pruneDone = true;

    // do batched rpc call per accountId
    Map<Integer, Map<Integer, String>> records = new HashMap<>();
    for (String prefKey : new ArrayList<>(prefs(context).getAll().keySet())) {
      if (!prefKey.startsWith(KEY_PREFIX)) {
        continue;
      }
      String ids = prefKey.substring(KEY_PREFIX.length());
      int dot = ids.indexOf('.');
      if (dot <= 0) {
        continue;
      }
      try {
        int accountId = Integer.parseInt(ids.substring(0, dot));
        int msgId = Integer.parseInt(ids.substring(dot + 1));
        records.computeIfAbsent(accountId, k -> new HashMap<>()).put(msgId, prefKey);
      } catch (NumberFormatException e) {
        Log.w(TAG, "Cannot parse webxdc permissions key: " + prefKey);
      }
    }

    for (Map.Entry<Integer, Map<Integer, String>> accountEntry : records.entrySet()) {
      int accountId = accountEntry.getKey();
      Set<Integer> existing;
      try {
        existing =
            new HashSet<>(
                rpc.getExistingMsgIds(
                    accountId, new ArrayList<>(accountEntry.getValue().keySet())));
      } catch (Exception e) {
        Log.w(TAG, "Cannot check webxdc messages for account " + accountId, e);
        continue;
      }
      for (Map.Entry<Integer, String> record : accountEntry.getValue().entrySet()) {
        if (!existing.contains(record.getKey())) {
          Log.i(TAG, "Pruning webxdc permissions for " + record.getValue());
          clear(context, accountId, record.getKey());
        }
      }
    }
  }

  public static List<FolderGrant> getFolders(Context context, int accountId, int msgId) {
    List<FolderGrant> result = new ArrayList<>();
    JSONArray folders = read(context, accountId, msgId).optJSONArray(KEY_FOLDERS);
    if (folders == null) {
      return result;
    }
    for (int i = 0; i < folders.length(); i++) {
      JSONObject folder = folders.optJSONObject(i);
      if (folder != null) {
        result.add(new FolderGrant(folder.optString("uri"), folder.optString("name")));
      }
    }
    return result;
  }

  public static int addFolder(
      Context context, int accountId, int msgId, String treeUri, String name) {
    try {
      JSONObject record = read(context, accountId, msgId);
      JSONArray folders = record.optJSONArray(KEY_FOLDERS);
      if (folders == null) {
        folders = new JSONArray();
        record.put(KEY_FOLDERS, folders);
      }
      for (int i = 0; i < folders.length(); i++) {
        JSONObject folder = folders.optJSONObject(i);
        if (folder != null && treeUri.equals(folder.optString("uri"))) {
          return i;
        }
      }
      JSONObject folder = new JSONObject();
      folder.put("uri", treeUri);
      folder.put("name", name);
      folders.put(folder);
      write(context, accountId, msgId, record);
      return folders.length() - 1;
    } catch (JSONException e) {
      // in-memory kv are valid JSON by construction
      return -1;
    }
  }

  public static List<FolderGrant> getReusableFolders(Context context, int accountId, int msgId) {
    Map<String, String> reusable = new LinkedHashMap<>();
    for (Map.Entry<String, ?> entry : prefs(context).getAll().entrySet()) {
      String prefKey = entry.getKey();
      if (!prefKey.startsWith(KEY_PREFIX) || prefKey.equals(key(accountId, msgId))) {
        continue;
      }
      try {
        JSONArray folders = new JSONObject((String) entry.getValue()).optJSONArray(KEY_FOLDERS);
        if (folders == null) {
          continue;
        }
        for (int i = 0; i < folders.length(); i++) {
          JSONObject folder = folders.optJSONObject(i);
          if (folder != null) {
            reusable.put(folder.optString("uri"), folder.optString("name"));
          }
        }
      } catch (Exception e) {
        Log.d(TAG, "Ignoring malformed webxdc permissions record: " + entry.getKey());
      }
    }
    List<FolderGrant> result = new ArrayList<>();
    for (Map.Entry<String, String> e : reusable.entrySet()) {
      result.add(new FolderGrant(e.getKey(), e.getValue()));
    }
    return result;
  }

  private static void releaseTreeIfUnused(Context context, String treeUri) {
    for (Map.Entry<String, ?> entry : prefs(context).getAll().entrySet()) {
      // a tree uri is a long unique document id
      if (entry.getKey().startsWith(KEY_PREFIX)
          && entry.getValue() instanceof String
          && ((String) entry.getValue()).contains(treeUri)) {
        return;
      }
    }
    try {
      context
          .getContentResolver()
          .releasePersistableUriPermission(
              Uri.parse(treeUri), Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } catch (Exception e) {
      Log.d(TAG, "URI read permission already gone");
    }
  }
}
