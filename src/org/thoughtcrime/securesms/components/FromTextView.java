package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.graphics.Typeface;
import androidx.core.view.ViewCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.AttributeSet;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.emoji.EmojiTextView;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.ResUtil;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.spans.CenterAlignedRelativeSizeSpan;

public class FromTextView extends EmojiTextView {

  private static final String TAG = FromTextView.class.getSimpleName();

  public FromTextView(Context context) {
    super(context);
  }

  public FromTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void setText(Recipient recipient) {
    setText(recipient, true);
  }

  public void setText(Recipient recipient, boolean read) {
    String fromString = recipient.toShortString();

    int typeface;

    if (!read) {
      typeface = Typeface.BOLD;
    } else {
      typeface = Typeface.NORMAL;
    }

    SpannableStringBuilder builder = new SpannableStringBuilder();

    SpannableString fromSpan = new SpannableString(fromString);
    fromSpan.setSpan(new StyleSpan(typeface), 0, builder.length(),
                     Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

    if (recipient.getName() == null && !TextUtils.isEmpty(recipient.getProfileName())) {
      SpannableString profileName = new SpannableString(" (~" + recipient.getProfileName() + ") ");
      profileName.setSpan(new CenterAlignedRelativeSizeSpan(0.75f), 0, profileName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      profileName.setSpan(new TypefaceSpan("sans-serif-light"), 0, profileName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      profileName.setSpan(new ForegroundColorSpan(ResUtil.getColor(getContext(), R.attr.conversation_list_item_subject_color)), 0, profileName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

      if (ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL){
        builder.append(profileName);
        builder.append(fromSpan);
      } else {
        builder.append(fromSpan);
        builder.append(profileName);
      }
    } else {
      builder.append(fromSpan);
    }

    setText(builder);

    int chatId = recipient.getAddress().isDcChat()? recipient.getAddress().getDcChatId() : 0;

    if (Prefs.isChatMuted(getContext(), chatId)) {
      setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_volume_off_grey600_18dp, 0, 0, 0);
    }
    else {
      setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    }
  }


}
