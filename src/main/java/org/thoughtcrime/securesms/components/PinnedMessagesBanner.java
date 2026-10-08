package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import chat.delta.rpc.RpcException;
import chat.delta.rpc.types.VcardContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.DecryptableStreamUriLoader;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.mms.Slide;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.ViewUtil;

public class PinnedMessagesBanner extends FrameLayout {
  private static final String TAG = "PinnedMessagesBanner";

  private LinearLayout positionIndicatorContainer;
  private TextView textView;
  private ViewGroup thumbnailContainer;
  private ImageView thumbnailView;
  private View videoOverlayView;
  private ImageView fileIconView;
  private Button openAppBtn;

  private PinnedMessagesBannerListener listener;
  private @NonNull Integer currentIndex = 0;
  private @NonNull List<Integer> msgIds = new ArrayList<>();

  public interface PinnedMessagesBannerListener {
    void onAppButtonClicked(int msgId);

    void onMessageClicked(int msgId);
  }

  public PinnedMessagesBanner(Context context) {
    super(context);
    initialize();
  }

  public PinnedMessagesBanner(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public PinnedMessagesBanner(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
  }

  private void initialize() {
    inflate(getContext(), R.layout.pinned_messages_banner, this);

    positionIndicatorContainer = findViewById(R.id.position_indicator_container);
    textView = findViewById(R.id.msg_text);
    thumbnailContainer = findViewById(R.id.thumbnail_container);
    thumbnailView = findViewById(R.id.thumbnail);
    videoOverlayView = findViewById(R.id.video_overlay);
    fileIconView = findViewById(R.id.file_icon);
    openAppBtn = findViewById(R.id.open_app_btn);

    openAppBtn.setOnClickListener(
        view -> {
          if (listener != null) {
            listener.onAppButtonClicked(msgIds.get(currentIndex));
          }
        });

    findViewById(R.id.container)
        .setOnClickListener(
            v -> {
              if (listener != null) {
                listener.onMessageClicked(msgIds.get(currentIndex));
              }
            });
  }

  private void updatePositionIndicator() {
    positionIndicatorContainer.removeAllViews();
    int totalCount = msgIds.size();
    final int maxBarsCount = 4;

    if (totalCount <= 1) {
      positionIndicatorContainer.setVisibility(GONE);
      return;
    }

    positionIndicatorContainer.setVisibility(VISIBLE);

    int activeColor = ContextCompat.getColor(getContext(), R.color.delta_accent);
    int inactiveColor = ColorUtils.setAlphaComponent(activeColor, 102);
    int marginPx = ViewUtil.dpToPx(getContext(), 2);

    int activeIndex = totalCount - 1 - currentIndex;
    if (totalCount > maxBarsCount) {
      float ratio = (float) activeIndex / (totalCount - 1);
      activeIndex = Math.round(ratio * (maxBarsCount - 1));
      totalCount = maxBarsCount;
    }

    for (int i = 0; i < totalCount; i++) {
      View bar = new View(getContext());

      LinearLayout.LayoutParams params =
          new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);

      if (i > 0) {
        params.setMargins(0, marginPx, 0, 0);
      }

      bar.setLayoutParams(params);
      bar.setBackgroundColor(i == activeIndex ? activeColor : inactiveColor);

      positionIndicatorContainer.addView(bar);
    }
  }

  public void goToStart(@NonNull GlideRequests glideRequests) {
    if (currentIndex != 0 && !msgIds.isEmpty()) {
      currentIndex = 0;
      renderCurrentPinnedMessage(glideRequests);
      updatePositionIndicator();
    }
  }

  public void goToNextMessage(@NonNull GlideRequests glideRequests) {
    if (!msgIds.isEmpty()) {
      currentIndex = (currentIndex + 1) % msgIds.size();
      renderCurrentPinnedMessage(glideRequests);
      updatePositionIndicator();
    }
  }

  public void setMessages(@NonNull GlideRequests glideRequests, @NonNull List<Integer> msgIds) {
    if (currentIndex > 0 && !msgIds.isEmpty()) {
      int index = msgIds.indexOf(this.msgIds.get(currentIndex));
      currentIndex = index >= 0 ? index : currentIndex % msgIds.size();
    } else {
      currentIndex = 0;
    }
    this.msgIds = msgIds;
    if (msgIds.isEmpty()) {
      dismiss();
    } else {
      renderCurrentPinnedMessage(glideRequests);
      updatePositionIndicator();
      setVisibility(VISIBLE);
    }
  }

  private void renderCurrentPinnedMessage(@NonNull GlideRequests glideRequests) {
    DcContext dcContext = DcHelper.getContext(getContext());
    DcMsg pinnedMsg = dcContext.getMsg(msgIds.get(currentIndex));
    if (pinnedMsg == null) return;

    textView.setText(pinnedMsg.getSummarytext(500));

    Slide slide = null;
    if (pinnedMsg.getType() != DcMsg.DC_MSG_TEXT) {
      slide = MediaUtil.getSlideForMsg(getContext(), pinnedMsg);
    }

    videoOverlayView.setVisibility(GONE);
    openAppBtn.setVisibility(GONE);

    if (slide != null && slide.hasQuoteThumbnail()) {
      thumbnailContainer.setVisibility(VISIBLE);
      fileIconView.setVisibility(GONE);

      if (slide.isWebxdcDocument()) {
        openAppBtn.setVisibility(VISIBLE);
        try {
          JSONObject info = pinnedMsg.getWebxdcInfo();
          byte[] blob = pinnedMsg.getWebxdcBlob(info.getString("icon"));
          glideRequests
              .load(blob)
              .centerCrop()
              .override(getContext().getResources().getDimensionPixelSize(R.dimen.quote_thumb_size))
              .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
              .into(thumbnailView);
        } catch (Exception e) {
          Log.e(TAG, "failed to get webxdc icon", e);
          thumbnailContainer.setVisibility(GONE);
        }
      } else if (slide.isVcard()) {
        try {
          VcardContact vcardContact =
              DcHelper.getRpc(getContext()).parseVcard(pinnedMsg.getFile()).get(0);
          Recipient recipient = new Recipient(getContext(), vcardContact);
          glideRequests
              .load(recipient.getContactPhoto(getContext()))
              .error(recipient.getFallbackAvatarDrawable(getContext()))
              .circleCrop()
              .override(getContext().getResources().getDimensionPixelSize(R.dimen.quote_thumb_size))
              .diskCacheStrategy(DiskCacheStrategy.NONE)
              .into(thumbnailView);
        } catch (RpcException | IndexOutOfBoundsException e) {
          Log.e(TAG, "failed to parse vCard", e);
          thumbnailContainer.setVisibility(GONE);
        }
      } else {
        Uri thumbnailUri = slide.getUri();
        if (slide.hasVideo()) {
          videoOverlayView.setVisibility(VISIBLE);
          MediaUtil.createVideoThumbnailIfNeeded(
              getContext(), slide.getUri(), slide.getThumbnailUri(), null);
          thumbnailUri = slide.getThumbnailUri();
        }
        if (thumbnailUri != null) {
          glideRequests
              .load(new DecryptableStreamUriLoader.DecryptableUri(thumbnailUri))
              .centerCrop()
              .override(getContext().getResources().getDimensionPixelSize(R.dimen.quote_thumb_size))
              .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
              .into(thumbnailView);
        }
      }
    } else if (slide != null && slide.hasAudio()) {
      thumbnailContainer.setVisibility(GONE);
      fileIconView.setVisibility(GONE);
    } else if (slide != null && slide.hasDocument()) {
      thumbnailContainer.setVisibility(GONE);
      fileIconView.setVisibility(VISIBLE);
    } else {
      thumbnailContainer.setVisibility(GONE);
      fileIconView.setVisibility(GONE);
    }
  }

  public void dismiss() {
    this.msgIds = new ArrayList<>();
    this.currentIndex = 0;
    setVisibility(GONE);
  }

  public void setListener(PinnedMessagesBannerListener listener) {
    this.listener = listener;
  }
}
