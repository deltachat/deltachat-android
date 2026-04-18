package org.thoughtcrime.securesms.geolocation;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public final class ActiveLocationChats {

  private static final String PREFS_NAME = "location_streaming";
  private static final String KEY_ACTIVE = "active_chat_ids";

  private ActiveLocationChats() {}

  private static SharedPreferences prefs(Context context) {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
  }

  /**
   * Add a chat. Uses commit() to guarantee the write reaches disk before the process can die to
   * preserve the superset invariant.
   */
  static void add(Context context, int chatId) {
    Set<String> current = new HashSet<>(getAll(context));
    current.add(String.valueOf(chatId));
    prefs(context).edit().putStringSet(KEY_ACTIVE, current).commit();
  }

  public static void remove(Context context, int chatId) {
    Set<String> current = new HashSet<>(getAll(context));
    current.remove(String.valueOf(chatId));
    prefs(context).edit().putStringSet(KEY_ACTIVE, current).apply();
  }

  static void clear(Context context) {
    prefs(context).edit().remove(KEY_ACTIVE).apply();
  }

  static Set<Integer> getAllIds(Context context) {
    Set<Integer> ids = new HashSet<>();
    for (String s : getAll(context)) {
      try {
        ids.add(Integer.parseInt(s));
      } catch (NumberFormatException ignored) {
      }
    }
    return ids;
  }

  private static Set<String> getAll(Context context) {
    return prefs(context).getStringSet(KEY_ACTIVE, new HashSet<>());
  }
}
