package org.thoughtcrime.securesms.components;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.view.ContentInfoCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import androidx.core.os.BuildCompat;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.TransportOption;
import org.thoughtcrime.securesms.util.Prefs;

public class ComposeText extends AppCompatEditText {

  private CharSequence    hint;
  private SpannableString subHint;

  @Nullable private InputPanel.MediaListener mediaListener;

  public ComposeText(Context context) {
    super(context);
    initialize();
  }

  public ComposeText(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public ComposeText(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
  }

  @Override
  public boolean onTextContextMenuItem(int id) {
    if (id == android.R.id.paste) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        id = android.R.id.pasteAsPlainText;
      } else if (ViewCompat.getOnReceiveContentMimeTypes(this) != null) {
        // older device, manually paste as plain text
        ClipboardManager cm = (ClipboardManager) getContext().getSystemService(
          Context.CLIPBOARD_SERVICE);
        ClipData clip = (cm == null) ? null : cm.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
          ContentInfoCompat payload = new ContentInfoCompat.Builder(clip, ContentInfoCompat.SOURCE_CLIPBOARD)
            .setFlags(ContentInfoCompat.FLAG_CONVERT_TO_PLAIN_TEXT)
            .build();
          ViewCompat.performReceiveContent(this, payload);
        }
        return true;
      }
    }
    return super.onTextContextMenuItem(id);
  }

  public String getTextTrimmed(){
    return getText().toString().trim();
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);

    if (!TextUtils.isEmpty(hint)) {
      if (!TextUtils.isEmpty(subHint)) {
        setHint(new SpannableStringBuilder().append(ellipsizeToWidth(hint))
                                            .append("\n")
                                            .append(ellipsizeToWidth(subHint)));
      } else {
        setHint(ellipsizeToWidth(hint));
      }
    }
  }

  private CharSequence ellipsizeToWidth(CharSequence text) {
    return TextUtils.ellipsize(text,
                               getPaint(),
                               getWidth() - getPaddingLeft() - getPaddingRight(),
                               TruncateAt.END);
  }

  public void setHint(@NonNull String hint, @Nullable CharSequence subHint) {
    this.hint = hint;

    if (subHint != null) {
      this.subHint = new SpannableString(subHint);
      this.subHint.setSpan(new RelativeSizeSpan(0.5f), 0, subHint.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
    } else {
      this.subHint = null;
    }

    if (this.subHint != null) {
      super.setHint(new SpannableStringBuilder().append(ellipsizeToWidth(this.hint))
                                                .append("\n")
                                                .append(ellipsizeToWidth(this.subHint)));
    } else {
      super.setHint(ellipsizeToWidth(this.hint));
    }
  }

  private boolean isLandscape() {
    return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
  }

  public void setTransport(TransportOption transport) {
    // do not add InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE
    // as this removes the ability to compose multi-line messages.

    int imeOptions = (getImeOptions() & ~EditorInfo.IME_MASK_ACTION) | EditorInfo.IME_ACTION_SEND;
    int inputType  = getInputType();

    if (isLandscape()) setImeActionLabel(getContext().getString(R.string.menu_send), EditorInfo.IME_ACTION_SEND);
    else               setImeActionLabel(null, 0);

    setInputType(inputType);
    setImeOptions(imeOptions);
    setHint(transport.getComposeHint(),null);
  }

  @Override
  public InputConnection onCreateInputConnection(@NonNull EditorInfo editorInfo) {
    InputConnection inputConnection = super.onCreateInputConnection(editorInfo);

    if(Prefs.isEnterSendsEnabled(getContext())) {
      editorInfo.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
    }

    if (mediaListener == null)      return inputConnection;
    if (inputConnection == null)    return null;

    // media with mime-types defined by setContentMimeTypes() may be selected in the system keyboard
    // and are passed to onCommitContent() then;
    // from there we use them as stickers.
    EditorInfoCompat.setContentMimeTypes(editorInfo, new String[] {"image/jpeg", "image/png", "image/gif", "image/webp"});

    return InputConnectionCompat.createWrapper(inputConnection, editorInfo, new CommitContentListener(mediaListener));
  }

  public void setMediaListener(@Nullable InputPanel.MediaListener mediaListener) {
    this.mediaListener = mediaListener;
  }

  private void initialize() {
    if (Prefs.isIncognitoKeyboardEnabled(getContext())) {
      setImeOptions(getImeOptions() | EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING);
    }
  }

  private static class CommitContentListener implements InputConnectionCompat.OnCommitContentListener {

    private static final String TAG = CommitContentListener.class.getName();

    private final InputPanel.MediaListener mediaListener;

    private CommitContentListener(@NonNull InputPanel.MediaListener mediaListener) {
      this.mediaListener = mediaListener;
    }

    @Override
    public boolean onCommitContent(@NonNull InputContentInfoCompat inputContentInfo, int flags, Bundle opts) {
      if (BuildCompat.isAtLeastNMR1() && (flags & InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
        try {
          inputContentInfo.requestPermission();
        } catch (Exception e) {
          Log.w(TAG, e);
          return false;
        }
      }

      if (inputContentInfo.getDescription().getMimeTypeCount() > 0) {
        mediaListener.onMediaSelected(inputContentInfo.getContentUri(),
                                      inputContentInfo.getDescription().getMimeType(0));

        return true;
      }

      return false;
    }
  }

}
