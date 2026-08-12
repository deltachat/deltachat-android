package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;
import com.b44t.messenger.DcMsg;
import java.util.Locale;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.AccessibilityUtil;
import org.thoughtcrime.securesms.util.DateUtils;

public class ConversationItemFooter extends LinearLayout {

  private static final String TAG = "ConversationItemFooter";

  private TextView dateView;
  private TextView editedView;
  private TextView viewsLabel;
  private ImageView viewsIcon;
  private ImageView bookmarkIndicatorView;
  private ImageView emailIndicatorView;
  private ImageView locationIndicatorView;
  private DeliveryStatusView deliveryStatusView;
  private Integer textColor = null;
  private Context context;
  private Rpc rpc;

  public ConversationItemFooter(Context context) {
    super(context);
    init(context, null);
  }

  public ConversationItemFooter(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs);
  }

  public ConversationItemFooter(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  private void init(Context context, @Nullable AttributeSet attrs) {
    this.context = context;
    this.rpc = DcHelper.getRpc(context);
    inflate(getContext(), R.layout.conversation_item_footer, this);

    dateView = findViewById(R.id.footer_date);
    editedView = findViewById(R.id.footer_edited);
    viewsLabel = findViewById(R.id.footer_views);
    viewsIcon = findViewById(R.id.footer_views_icon);
    bookmarkIndicatorView = findViewById(R.id.footer_bookmark_indicator);
    emailIndicatorView = findViewById(R.id.footer_email_indicator);
    locationIndicatorView = findViewById(R.id.footer_location_indicator);
    deliveryStatusView = new DeliveryStatusView(findViewById(R.id.delivery_indicator));

    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    for (int i = 0; i < getChildCount(); i++) {
      getChildAt(i).setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    if (attrs != null) {
      try (TypedArray typedArray =
          getContext()
              .getTheme()
              .obtainStyledAttributes(attrs, R.styleable.ConversationItemFooter, 0, 0)) {
        setTextColor(
            typedArray.getInt(
                R.styleable.ConversationItemFooter_footer_text_color,
                getResources().getColor(R.color.core_white)));
      }
    }
  }

  public void setMessageRecord(
      @NonNull DcMsg messageRecord, boolean isOutChannel, @NonNull MessageState state) {
    presentDate(messageRecord);

    boolean bookmark = messageRecord.getOriginalMsgId() != 0 || messageRecord.getSavedMsgId() != 0;
    bookmarkIndicatorView.setVisibility(bookmark ? View.VISIBLE : View.GONE);

    boolean edited = messageRecord.isEdited();
    editedView.setVisibility(edited ? View.VISIBLE : View.GONE);

    int downloadState = messageRecord.getDownloadState();
    boolean notEncrypted =
        !(messageRecord.isSecure()
            || downloadState == DcMsg.DC_DOWNLOAD_AVAILABLE
            || downloadState == DcMsg.DC_DOWNLOAD_FAILURE
            || downloadState == DcMsg.DC_DOWNLOAD_IN_PROGRESS);
    emailIndicatorView.setVisibility(notEncrypted ? View.VISIBLE : View.GONE);

    boolean hasLocation = messageRecord.hasLocation();
    locationIndicatorView.setVisibility(hasLocation ? View.VISIBLE : View.GONE);

    int readCount = -1;
    if (isOutChannel && messageRecord.isOutgoing()) {
      try {
        readCount =
            rpc.getMessageReadReceiptCount(rpc.getSelectedAccountId(), messageRecord.getId());
        viewsLabel.setText(String.format(Locale.getDefault(), "%d", readCount));
        viewsLabel.setVisibility(View.VISIBLE);
        viewsIcon.setVisibility(View.VISIBLE);
      } catch (RpcException e) {
        Log.w(TAG, "failed to get read receipt count", e);
        viewsLabel.setText("");
        viewsLabel.setVisibility(View.GONE);
        viewsIcon.setVisibility(View.GONE);
      }
    } else {
      viewsLabel.setText("");
      viewsLabel.setVisibility(View.GONE);
      viewsIcon.setVisibility(View.GONE);
    }

    deliveryStatusView.setState(state);

    StringBuilder desc = new StringBuilder();
    if (edited) AccessibilityUtil.append(desc, getContext().getString(R.string.edited));
    if (bookmark) AccessibilityUtil.append(desc, getContext().getString(R.string.saved));
    if (notEncrypted)
      AccessibilityUtil.append(desc, getContext().getString(R.string.a11y_msg_not_encrypted));
    if (hasLocation) AccessibilityUtil.append(desc, getContext().getString(R.string.location));
    if (readCount >= 0) {
      AccessibilityUtil.append(
          desc, getResources().getQuantityString(R.plurals.a11y_msg_read_by, readCount, readCount));
    }
    AccessibilityUtil.append(desc, dateView.getText());
    setContentDescription(AccessibilityUtil.toDescription(desc));
  }

  private void setTextColor(int color) {
    textColor = color;
    dateView.setTextColor(color);
    editedView.setTextColor(color);
    viewsLabel.setTextColor(color);
    viewsIcon.setColorFilter(color);
    bookmarkIndicatorView.setColorFilter(color);
    emailIndicatorView.setColorFilter(color);
    locationIndicatorView.setColorFilter(color);
    deliveryStatusView.setTint(color);
  }

  private void presentDate(@NonNull DcMsg dcMsg) {
    dateView.forceLayout();
    dateView.setText(
        DateUtils.getExtendedRelativeTimeSpanString(getContext(), dcMsg.getTimestamp()));
  }

  private void presentDeliveryStatus(@NonNull DcMsg messageRecord, boolean isOutChannel) {
    // isDownloading is temporary and should be checked first.
    boolean isDownloading = messageRecord.getDownloadState() == DcMsg.DC_DOWNLOAD_IN_PROGRESS;

    if (isDownloading) deliveryStatusView.setDownloading();
    else if (messageRecord.isPending()) deliveryStatusView.setPending();
    else if (messageRecord.isFailed()) deliveryStatusView.setFailed();
    else if (!messageRecord.isOutgoing() || isOutChannel) deliveryStatusView.setNone();
    else if (messageRecord.isRemoteRead()) deliveryStatusView.setRead();
    else if (messageRecord.isDelivered()) deliveryStatusView.setSent();
    else if (messageRecord.isPreparing()) deliveryStatusView.setPreparing();
    else deliveryStatusView.setPending();
  }
}
