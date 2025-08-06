package org.thoughtcrime.securesms.components;


import android.content.Context;
import android.content.res.TypedArray;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.b44t.messenger.DcMsg;

import org.json.JSONObject;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.mms.DocumentSlide;
import org.thoughtcrime.securesms.mms.SlideClickListener;
import org.thoughtcrime.securesms.util.JsonUtils;

import java.io.ByteArrayInputStream;

public class WebxdcView extends FrameLayout {

  private final @NonNull AppCompatImageView icon;
  private final @NonNull TextView           appName;
  private final @NonNull TextView           appSubtitle;

  private @Nullable SlideClickListener viewListener;

  public WebxdcView(@NonNull Context context) {
    this(context, null);
  }

  public WebxdcView(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public WebxdcView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    boolean compact;
    try (TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.WebxdcView, 0, 0)) {
      compact = a.getBoolean(R.styleable.WebxdcView_compact, false);
    }
    if (compact) {
      inflate(context, R.layout.webxdc_compact_view, this);
    } else {
      inflate(context, R.layout.webxdc_view, this);
    }

    this.icon        = findViewById(R.id.webxdc_icon);
    this.appName     = findViewById(R.id.webxdc_app_name);
    this.appSubtitle = findViewById(R.id.webxdc_subtitle);
  }

  public void setWebxdcClickListener(@Nullable SlideClickListener listener) {
    this.viewListener = listener;
  }

  public void setWebxdc(final @NonNull DcMsg dcMsg, String defaultSummary)
  {
    JSONObject info = dcMsg.getWebxdcInfo();
    setOnClickListener(new OpenClickedListener(getContext(), dcMsg));

    // icon
    byte[] blob = dcMsg.getWebxdcBlob(JsonUtils.optString(info, "icon"));
    if (blob != null) {
      ByteArrayInputStream is = new ByteArrayInputStream(blob);
      Drawable drawable = Drawable.createFromStream(is, "icon");
      icon.setImageDrawable(drawable);
    }

    // name
    String docName = JsonUtils.optString(info, "document");
    String xdcName = JsonUtils.optString(info, "name");
    appName.setText(docName.isEmpty() ? xdcName : (docName + " – " + xdcName));

    // subtitle
    String summary = info.optString("summary");
    if (summary.isEmpty()) {
      summary = defaultSummary;
    }
    if (summary.isEmpty()) {
      appSubtitle.setVisibility(View.GONE);
    } else {
      appSubtitle.setVisibility(View.VISIBLE);
      appSubtitle.setText(summary);
    }
  }

  public String getDescription() {
    String desc = getContext().getString(R.string.webxdc_app);
    desc += "\n" + appName.getText();
    if (appSubtitle.getText() != null && !appSubtitle.getText().toString().isEmpty() && !appSubtitle.getText().toString().equals(getContext().getString(R.string.webxdc_app))) {
      desc += "\n" + appSubtitle.getText();
    }
    return desc;
  }

  private class OpenClickedListener implements View.OnClickListener {
    private final @NonNull DocumentSlide slide;

    private OpenClickedListener(Context context, @NonNull DcMsg dcMsg) {
      this.slide = new DocumentSlide(context, dcMsg);
    }

    @Override
    public void onClick(View v) {
      if (viewListener != null) {
        viewListener.onClick(v, slide);
      }
    }
  }

}
