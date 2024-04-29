package org.thoughtcrime.securesms.components.emoji;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.annimon.stream.Stream;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

public class RecentEmojiPageModel implements EmojiPageModel {
  private static final String TAG            = RecentEmojiPageModel.class.getSimpleName();
  private static final int    EMOJI_LRU_SIZE = 50;

  private final SharedPreferences     prefs;
  private final String                preferenceName;
  private final LinkedHashSet<String> recentlyUsed;

  public RecentEmojiPageModel(Context context, @NonNull String preferenceName) {
    this.prefs          = PreferenceManager.getDefaultSharedPreferences(context);
    this.preferenceName = preferenceName;
    this.recentlyUsed   = getPersistedCache();
  }

  private LinkedHashSet<String> getPersistedCache() {
    String serialized = prefs.getString(preferenceName, "[]");
    try {
      CollectionType collectionType = TypeFactory.defaultInstance()
                                                 .constructCollectionType(LinkedHashSet.class, String.class);
      return JsonUtils.getMapper().readValue(serialized, collectionType);
    } catch (IOException e) {
      Log.w(TAG, e);
      return new LinkedHashSet<>();
    }
  }

  @Override public int getIconAttr() {
    return R.attr.emoji_category_recent;
  }

  @Override public List<String> getEmoji() {
    List<String> emoji = new ArrayList<>(recentlyUsed);
    Collections.reverse(emoji);
    return emoji;
  }

  @Override public List<Emoji> getDisplayEmoji() {
    return Stream.of(getEmoji()).map(Emoji::new).toList();
  }

  @Override public boolean hasSpriteMap() {
    return false;
  }

  @Override public String getSprite() {
    return null;
  }

  @Override public boolean isDynamic() {
    return true;
  }

  public void onCodePointSelected(String emoji) {
    recentlyUsed.remove(emoji);
    recentlyUsed.add(emoji);

    if (recentlyUsed.size() > EMOJI_LRU_SIZE) {
      Iterator<String> iterator = recentlyUsed.iterator();
      iterator.next();
      iterator.remove();
    }

    final LinkedHashSet<String> latestRecentlyUsed = new LinkedHashSet<>(recentlyUsed);
    new AsyncTask<Void, Void, Void>() {

      @Override
      protected Void doInBackground(Void... params) {
        try {
          String serialized = JsonUtils.toJson(latestRecentlyUsed);
          prefs.edit()
               .putString(preferenceName, serialized)
               .apply();
        } catch (IOException e) {
          Log.w(TAG, e);
        }

        return null;
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }
}
