package org.thoughtcrime.securesms.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.ColorInt;
import androidx.appcompat.app.AlertDialog;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.Toast;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ConversationActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;

public class LongClickCopySpan extends URLSpan {
  private static final String PREFIX_MAILTO = "mailto:";
  private static final String PREFIX_TEL = "tel:";

  private boolean isHighlighted;
  @ColorInt
  private int highlightColor;

  public LongClickCopySpan(String url) {
    super(url);
  }

  @Override
  public void onClick(View widget) {
    String url = getURL();
    if (url.startsWith(PREFIX_MAILTO)) {
      try {
        String addr = prepareUrl(url);
        Activity activity = (Activity) widget.getContext();
        DcContext dcContext = DcHelper.getContext(activity);
        DcContact contact = dcContext.getContact(dcContext.createContact(null, addr));
        new AlertDialog.Builder(activity)
            .setMessage(activity.getString(R.string.ask_start_chat_with, contact.getNameNAddr()))
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
              int chatId = dcContext.createChatByContactId(contact.getId());
              if (chatId != 0) {
                Intent intent = new Intent(activity, ConversationActivity.class);
                intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId);
                activity.startActivity(intent);
              }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
      }
      catch(Exception e) {
        e.printStackTrace();
      }
    }
    else {
      super.onClick(widget);
    }
  }

  void onLongClick(View widget) {
    Context context = widget.getContext();
    String preparedUrl = prepareUrl(getURL());

    new AlertDialog.Builder(context)
        .setTitle(preparedUrl)
        .setItems(new CharSequence[]{
                context.getString(R.string.menu_copy_to_clipboard)
            },
            (dialogInterface, i) -> {
              copyUrl(context, preparedUrl);
              Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
            })
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  @Override
  public void updateDrawState(TextPaint ds) {
    super.updateDrawState(ds);
    ds.bgColor = highlightColor;
    ds.setUnderlineText(!isHighlighted);
  }

  void setHighlighted(boolean highlighted, @ColorInt int highlightColor) {
    this.isHighlighted = highlighted;
    this.highlightColor = highlightColor;
  }

  private void copyUrl(Context context, String url) {
    int sdk = android.os.Build.VERSION.SDK_INT;
    if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
      @SuppressWarnings("deprecation") android.text.ClipboardManager clipboard =
              (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
      clipboard.setText(url);
    } else {
      copyUriSdk11(context, url);
    }
  }

  @TargetApi(android.os.Build.VERSION_CODES.HONEYCOMB)
  private void copyUriSdk11(Context context, String url) {
    android.content.ClipboardManager clipboard =
            (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    ClipData clip = ClipData.newPlainText(context.getString(R.string.app_name), url);
    clipboard.setPrimaryClip(clip);
  }

  private String prepareUrl(String url) {
    if (url.startsWith(PREFIX_MAILTO)) {
      return url.substring(PREFIX_MAILTO.length());
    } else if (url.startsWith(PREFIX_TEL)) {
      return url.substring(PREFIX_TEL.length());
    }
    return url;
  }
}
