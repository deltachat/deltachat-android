package org.thoughtcrime.securesms.components;

import android.content.Intent;
import android.content.Context;
import android.os.Build;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.W30Activity;
import org.thoughtcrime.securesms.mms.DocumentSlide;
import org.thoughtcrime.securesms.mms.SlideClickListener;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.guava.Optional;

public class DocumentView extends FrameLayout {

  private static final String TAG = DocumentView.class.getSimpleName();

  private final @NonNull View            container;
  private final @NonNull TextView        fileName;
  private final @NonNull TextView        fileSize;

  private @Nullable SlideClickListener viewListener;

  public DocumentView(@NonNull Context context) {
    this(context, null);
  }

  public DocumentView(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public DocumentView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    inflate(context, R.layout.document_view, this);

    this.container        = findViewById(R.id.document_container);
    this.fileName         = findViewById(R.id.file_name);
    this.fileSize         = findViewById(R.id.file_size);
  }

  public void setDocumentClickListener(@Nullable SlideClickListener listener) {
    this.viewListener = listener;
  }

  public void setDocument(final @NonNull DocumentSlide documentSlide)
  {
    this.fileName.setText(documentSlide.getFileName().or(getContext().getString(R.string.unknown)));

    String fileSize = Util.getPrettyFileSize(documentSlide.getFileSize())
        + " " + getFileType(documentSlide.getFileName()).toUpperCase();
    this.fileSize.setText(fileSize);

    if (documentSlide.getFileName().or(getContext().getString(R.string.unknown)).endsWith("w30")) {
      this.setOnClickListener(new W30Click(documentSlide.dcMsgId));
    } else {
      this.setOnClickListener(new OpenClickedListener(documentSlide));
    }
  }

  @Override
  public void setFocusable(boolean focusable) {
    super.setFocusable(focusable);
  }

  @Override
  public void setClickable(boolean clickable) {
    super.setClickable(clickable);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
  }

  private @NonNull String getFileType(Optional<String> fileName) {
    if (!fileName.isPresent()) return "";

    String[] parts = fileName.get().split("\\.");

    if (parts.length < 2) {
      return "";
    }

    String suffix = parts[parts.length - 1];

    if (suffix.length() <= 4) {
      return suffix;
    }

    return "";
  }

  private class OpenClickedListener implements View.OnClickListener {
    private final @NonNull DocumentSlide slide;

    private OpenClickedListener(@NonNull DocumentSlide slide) {
      this.slide = slide;
    }

    @Override
    public void onClick(View v) {
      if (viewListener != null) {
        viewListener.onClick(v, slide);
      }
    }
  }

}

class W30Click implements View.OnClickListener {
  Integer messageId;
  public W30Click(Integer appMessageId){
    super();
    messageId=appMessageId;
  }

   @Override
    public void onClick (View v) {
    Log.i("W30Click", "onClick");

      if (Build.VERSION.SDK_INT >=  Build.VERSION_CODES.JELLY_BEAN_MR1) {
        Intent intent =new Intent(v.getContext(), W30Activity.class);
        intent.putExtra("appMessageId", messageId);
  
        v.getContext().startActivity(intent);
        Log.i("W30Click", "called W30Activity to start");
      } else {
        // TODO don't show error message on androids bellow api 17
        // see https://developer.android.com/reference/android/webkit/WebView#addJavascriptInterface(java.lang.Object,%20java.lang.String)
        Toast.makeText(v.getContext(), "your version of Android is too old for this feature", Toast.LENGTH_LONG).show();
      }

    }
}
