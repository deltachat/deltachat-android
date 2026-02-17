package org.thoughtcrime.securesms.components.emoji;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.emoji2.emojipicker.EmojiPickerView;
import androidx.emoji2.emojipicker.EmojiViewItem;

import com.google.android.material.tabs.TabLayout;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.InputAwareLayout.InputView;
import org.thoughtcrime.securesms.util.ResUtil;

import java.io.File;

public class MediaKeyboard extends LinearLayout implements InputView, Consumer<EmojiViewItem>, StickerPickerView.StickerPickerListener {

  private static final String TAG = MediaKeyboard.class.getSimpleName();

  @Nullable private MediaKeyboardListener keyboardListener;
  private EmojiPickerView emojiPicker;
  private StickerPickerView stickerPicker;
  private View stickerPickerContainer;
  private View stickerPickerEmpty;

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
      setupViews();
    }
    setVisibility(VISIBLE);
    if (keyboardListener != null) keyboardListener.onShown();
  }

  private void setupViews() {
    emojiPicker = findViewById(R.id.emoji_picker);
    stickerPicker = findViewById(R.id.sticker_picker);
    stickerPickerContainer = findViewById(R.id.sticker_picker_container);
    stickerPickerEmpty = findViewById(R.id.sticker_picker_empty);
    TabLayout tabLayout = findViewById(R.id.media_keyboard_tabs);

    if (emojiPicker != null) {
      emojiPicker.setOnEmojiPickedListener(this);
    }

    if (stickerPicker != null) {
      stickerPicker.setStickerPickerListener(this);
    }

    if (tabLayout != null) {
      Drawable emojiIcon = ResUtil.getDrawable(getContext(), R.attr.conversation_emoji_toggle);

      tabLayout.addTab(tabLayout.newTab().setIcon(emojiIcon));
      tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_sticker_24));

      tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
          if (tab.getPosition() == 0) {
            showEmojiPicker();
          } else {
            showStickerPicker();
          }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
        }
      });
    }
  }

  private void showEmojiPicker() {
    if (emojiPicker != null) {
      emojiPicker.setVisibility(View.VISIBLE);
    }
    if (stickerPickerContainer != null) {
      stickerPickerContainer.setVisibility(View.GONE);
    }
  }

  private void showStickerPicker() {
    if (emojiPicker != null) {
      emojiPicker.setVisibility(View.GONE);
    }
    if (stickerPickerContainer != null) {
      stickerPickerContainer.setVisibility(View.VISIBLE);
    }
    if (stickerPicker != null) {
      stickerPicker.loadStickers();
      updateStickerEmptyState();
    }
  }

  private void updateStickerEmptyState() {
    if (stickerPicker != null && stickerPickerEmpty != null) {
      boolean hasStickers = stickerPicker.getAdapter() != null && stickerPicker.getAdapter().getItemCount() > 0;
      stickerPickerEmpty.setVisibility(hasStickers ? View.GONE : View.VISIBLE);
    }
  }

  public void refreshStickerPicker() {
    if (stickerPicker != null) {
      stickerPicker.loadStickers();
      updateStickerEmptyState();
    }
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

  @Override
  public void onStickerSelected(@NonNull File stickerFile) {
    if (keyboardListener != null) {
      keyboardListener.onStickerPicked(Uri.fromFile(stickerFile));
    }
  }

  @Override
  public void onStickerDeleted(@NonNull File stickerFile) {
    updateStickerEmptyState();
  }

  public interface MediaKeyboardListener {
    void onShown();
    void onHidden();
    void onEmojiPicked(String emoji);
    void onStickerPicked(Uri stickerUri);
  }
}
