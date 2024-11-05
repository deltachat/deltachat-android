package org.thoughtcrime.securesms.components.emoji;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import net.fellbaum.jemoji.Emoji;
import net.fellbaum.jemoji.EmojiManager;

import java.util.List;


public class AutoScaledEmojiTextView extends AppCompatTextView {

  private float originalFontSize;

  public AutoScaledEmojiTextView(Context context) {
    this(context, null);
  }

  public AutoScaledEmojiTextView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public AutoScaledEmojiTextView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    TypedArray typedArray = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.textSize});
    originalFontSize = typedArray.getDimensionPixelSize(0, 0);
    typedArray.recycle();
  }

  @Override
  public void setText(@Nullable CharSequence text, BufferType type) {
    float scale = getTextScale(text);
    super.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalFontSize * scale);
    super.setText(text, type);
  }

  private float getTextScale(CharSequence text) {
    if (text.length() > 16) {
      return 1;
    }
    List<Emoji> emojis = EmojiManager.extractEmojisInOrder(text.toString());
    int emojiCount = emojis.size();
    if (emojiCount <= 8) {
      int charCount = text.length();
      for (Emoji emoji : emojis) {
        charCount -= emoji.getEmoji().length();
      }
      if (charCount == 0) { // only emojis in text
        float scale = 1.25f;
        if (emojiCount <= 6) scale += 0.25f;
        if (emojiCount <= 4) scale += 0.25f;
        if (emojiCount <= 2) scale += 0.25f;
        return scale;
      }
    }
    return 1;
  }

}
