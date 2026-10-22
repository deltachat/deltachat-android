package org.thoughtcrime.securesms.webxdc;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.preference.PreferenceManager;
import chat.delta.rpc.Rpc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.thoughtcrime.securesms.util.Prefs;

/** Per-webxdc-app permission, following the browser model where each app is a "site". */
public class WebxdcPermissions {
  private static final String TAG = "WebxdcPermissions";
  private static final String KEY_PREFIX = "webxdc_permissions_";

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
    Prefs.removePreference(context, key(accountId, msgId));
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
}
