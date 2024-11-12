package org.thoughtcrime.securesms.components.emoji;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageButton;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.ResUtil;

public class EmojiToggle extends AppCompatImageButton {

  private Drawable emojiToggle;
//  private Drawable stickerToggle;

  private Drawable mediaToggle;
  private Drawable imeToggle;


  public EmojiToggle(Context context) {
    super(context);
    initialize();
  }

  public EmojiToggle(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public EmojiToggle(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    initialize();
  }

  public void setToMedia() {
    setImageDrawable(mediaToggle);
  }

  public void setToIme() {
    setImageDrawable(imeToggle);
  }

  private void initialize() {
    this.emojiToggle   = ResUtil.getDrawable(getContext(), R.attr.conversation_emoji_toggle);
//    this.stickerToggle = ResUtil.getDrawable(getContext(), R.attr.conversation_sticker_toggle);
    this.imeToggle     = ResUtil.getDrawable(getContext(), R.attr.conversation_keyboard_toggle);
    this.mediaToggle   = emojiToggle;

    setToMedia();
  }

  public void setStickerMode(boolean stickerMode) {
    this.mediaToggle = /*stickerMode ? stickerToggle :*/ emojiToggle;

    if (getDrawable() != imeToggle) {
      setToMedia();
    }
  }

  public boolean isStickerMode() {
    //return this.mediaToggle == stickerToggle;
    return false;
  }
}
