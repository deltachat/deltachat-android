package org.thoughtcrime.securesms.map;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import org.thoughtcrime.securesms.components.emoji.EmojiProvider;
import org.thoughtcrime.securesms.util.Pair;

import java.lang.ref.WeakReference;

public class EmojiBitmapGenerationTask extends AsyncTask<String, Pair<String, Bitmap>, Void> {

  public interface EmojiBitmapGenerationCallback {
    void onEmojiBitmapCreated(String codePoint, Bitmap emoji);
  }
  WeakReference<EmojiBitmapGenerationCallback> callbackWeakReference;
  WeakReference<EmojiProvider> emojiProviderWeakReference;

  public EmojiBitmapGenerationTask(EmojiBitmapGenerationCallback callback, EmojiProvider emojiProvider) {
    callbackWeakReference = new WeakReference<>(callback);
    emojiProviderWeakReference = new WeakReference<>(emojiProvider);
  }

  @Override
  protected Void doInBackground(String... codePoints) {
    for (String codePoint : codePoints) {
      EmojiProvider emojiProvider = emojiProviderWeakReference.get();
      if (emojiProvider == null) {
        return null;
      }
      Bitmap emoji = emojiProvider.getEmojiBitmap(codePoint, true);
      publishProgress(new Pair<>(codePoint, emoji));
    }

    return null;
  }

  @Override
  protected void onProgressUpdate(Pair<String, Bitmap>... values) {
    super.onProgressUpdate(values);
    EmojiBitmapGenerationCallback callback =  callbackWeakReference.get();
    if (callback != null) {
      callback.onEmojiBitmapCreated(values[0].first(), values[0].second());
    }
  }

  @Override
  protected void onPostExecute(Void aVoid) {
    super.onPostExecute(aVoid);
  }

  @Override
  protected void onCancelled() {
    super.onCancelled();
  }
}
