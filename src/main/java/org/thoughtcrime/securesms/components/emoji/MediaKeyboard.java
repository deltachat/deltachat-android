package org.thoughtcrime.securesms.components.emoji;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.emoji2.emojipicker.EmojiPickerView;
import androidx.emoji2.emojipicker.EmojiViewItem;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.InputAwareLayout.InputView;

public class MediaKeyboard extends FrameLayout implements InputView, Consumer<EmojiViewItem> {

  private static final String TAG = MediaKeyboard.class.getSimpleName();

  @Nullable private MediaKeyboardListener   keyboardListener;
  private EmojiPickerView emojiPicker;

  public MediaKeyboard(@NonNull Context context) {
    super(context);
  }

  public MediaKeyboard(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public void setKeyboardListener(@Nullable MediaKeyboardListener listener) {
    this.keyboardListener = listener;
  }

  @Override
  public boolean isShowing() {
    return getVisibility() == VISIBLE;
  }

  @Override
  public void show(int height, boolean immediate) {
    ViewGroup.LayoutParams params = getLayoutParams();
    params.height = height;
    Log.i(TAG, "showing emoji drawer with height " + params.height);
    setLayoutParams(params);

    show();
  }

  public void show() {
    if (emojiPicker == null) {
      emojiPicker = findViewById(R.id.emoji_picker);
      emojiPicker.setOnEmojiPickedListener(this);
    }
    setVisibility(VISIBLE);
    if (keyboardListener != null) keyboardListener.onShown();
  }

  @Override
  public void hide(boolean immediate) {
    setVisibility(GONE);
    if (keyboardListener != null) keyboardListener.onHidden();
    Log.i(TAG, "hide()");
  }

  @Override
  public void accept(EmojiViewItem emojiViewItem) {
    if (keyboardListener != null) keyboardListener.onEmojiPicked(emojiViewItem.getEmoji());
  }

  public interface MediaKeyboardListener {
    void onShown();
    void onHidden();
    void onEmojiPicked(String emoji);
  }
}
