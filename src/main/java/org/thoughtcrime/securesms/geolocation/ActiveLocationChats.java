package org.thoughtcrime.securesms.geolocation;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class ActiveLocationChats {

  private static final String PREFS_NAME = "location_streaming";
  private static final String KEY_ACTIVE = "active_chat_ids";
  private static final String ID_SEPARATOR = ":";

  private ActiveLocationChats() {}

  private static SharedPreferences prefs(Context context) {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
  }

  private static String key(int accountId, int chatId) {
    return accountId + ID_SEPARATOR + chatId;
  }

  /**
   * Add a chat for a specific account. Uses commit() to guarantee the write reaches disk before the
   * process can die to preserve the superset invariant.
   */
  static void add(Context context, int accountId, int chatId) {
    Set<String> current = new HashSet<>(getAll(context));
    current.add(key(accountId, chatId));
    prefs(context).edit().putStringSet(KEY_ACTIVE, current).commit();
  }

  public static void remove(Context context, int accountId, int chatId) {
    Set<String> current = new HashSet<>(getAll(context));
    current.remove(key(accountId, chatId));
    prefs(context).edit().putStringSet(KEY_ACTIVE, current).apply();
  }

  static void removeAccount(Context context, int accountId) {
    String prefix = accountId + ID_SEPARATOR;
    Set<String> current = new HashSet<>(getAll(context));
    Iterator<String> it = current.iterator();
    while (it.hasNext()) {
      if (it.next().startsWith(prefix)) {
        it.remove();
      }
    }
    prefs(context).edit().putStringSet(KEY_ACTIVE, current).apply();
  }

  static void clear(Context context) {
    prefs(context).edit().remove(KEY_ACTIVE).apply();
  }

  static boolean isEmpty(Context context) {
    return getAll(context).isEmpty();
  }

  /** Returns the set of account IDs with at least 1 active chat. */
  static Set<Integer> getAccountIds(Context context) {
    Set<Integer> ids = new HashSet<>();
    for (String entry : getAll(context)) {
      int sep = entry.indexOf(ID_SEPARATOR);
      if (sep > 0) {
        try {
          ids.add(Integer.parseInt(entry.substring(0, sep)));
        } catch (NumberFormatException ignored) {
        }
      }
    }
    return ids;
  }

  /** Returns the chat IDs that are active for an account. */
  static Set<Integer> getChatIds(Context context, int accountId) {
    Set<Integer> ids = new HashSet<>();
    String prefix = accountId + ID_SEPARATOR;
    for (String entry : getAll(context)) {
      if (entry.startsWith(prefix)) {
        try {
          ids.add(Integer.parseInt(entry.substring(prefix.length())));
        } catch (NumberFormatException ignored) {
        }
      }
    }
    return ids;
  }

  private static Set<String> getAll(Context context) {
    return prefs(context).getStringSet(KEY_ACTIVE, new HashSet<>());
  }
}
