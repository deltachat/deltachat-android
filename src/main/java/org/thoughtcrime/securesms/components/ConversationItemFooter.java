package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.DateUtils;

public class ConversationItemFooter extends LinearLayout {

  private TextView            dateView;
  private TextView            editedView;
  private ImageView           bookmarkIndicatorView;
  private ImageView           emailIndicatorView;
  private ImageView           locationIndicatorView;
  private DeliveryStatusView  deliveryStatusView;
  private Integer             textColor = null;
  private int                 callDuration = 0;

  public ConversationItemFooter(Context context) {
    super(context);
    init(null);
  }

  public ConversationItemFooter(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(attrs);
  }

  public ConversationItemFooter(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(attrs);
  }

  private void init(@Nullable AttributeSet attrs) {
    inflate(getContext(), R.layout.conversation_item_footer, this);

    dateView              = findViewById(R.id.footer_date);
    editedView            = findViewById(R.id.footer_edited);
    bookmarkIndicatorView = findViewById(R.id.footer_bookmark_indicator);
    emailIndicatorView   = findViewById(R.id.footer_email_indicator);
    locationIndicatorView = findViewById(R.id.footer_location_indicator);
    deliveryStatusView    = new DeliveryStatusView(findViewById(R.id.delivery_indicator));

    if (attrs != null) {
      try (TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.ConversationItemFooter, 0, 0)) {
        setTextColor(typedArray.getInt(R.styleable.ConversationItemFooter_footer_text_color, getResources().getColor(R.color.core_white)));
      }
    }
  }

  /* Call duration in seconds. Only >0 if this is a call message */
  public void setCallDuration(int duration) {
    callDuration = duration;
  }

  public void setMessageRecord(@NonNull DcMsg messageRecord) {
    presentDate(messageRecord);
    boolean bookmark = messageRecord.getOriginalMsgId() != 0 || messageRecord.getSavedMsgId() != 0;
    bookmarkIndicatorView.setVisibility(bookmark ? View.VISIBLE : View.GONE);
    editedView.setVisibility(messageRecord.isEdited() ? View.VISIBLE : View.GONE);

    int downloadState = messageRecord.getDownloadState();
    if (messageRecord.isSecure() || downloadState == DcMsg.DC_DOWNLOAD_AVAILABLE || downloadState == DcMsg.DC_DOWNLOAD_FAILURE || downloadState == DcMsg.DC_DOWNLOAD_IN_PROGRESS) {
      emailIndicatorView.setVisibility(View.GONE);
    } else {
      emailIndicatorView.setVisibility(View.VISIBLE);
    }

    locationIndicatorView.setVisibility(messageRecord.hasLocation() ? View.VISIBLE : View.GONE);
    presentDeliveryStatus(messageRecord);
  }

  public void setTextColor(int color) {
    textColor = color;
    dateView.setTextColor(color);
    editedView.setTextColor(color);
    bookmarkIndicatorView.setColorFilter(color);
    emailIndicatorView.setColorFilter(color);
    locationIndicatorView.setColorFilter(color);
    deliveryStatusView.setTint(color);
  }

  private void presentDate(@NonNull DcMsg dcMsg) {
    dateView.forceLayout();
    Context context = getContext();
    String date = dcMsg.getType() == DcMsg.DC_MSG_CALL?
      DateUtils.getExtendedTimeSpanString(context, dcMsg.getTimestamp())
      : DateUtils.getExtendedRelativeTimeSpanString(context, dcMsg.getTimestamp());
    if (callDuration > 0) {
      String duration = DateUtils.getFormattedCallDuration(context, callDuration);
      dateView.setText(context.getString(R.string.call_date_and_duration, date, duration));
    } else {
      dateView.setText(date);
    }
  }

  private void presentDeliveryStatus(@NonNull DcMsg messageRecord) {
    // isDownloading is temporary and should be checked first.
    boolean isDownloading = messageRecord.getDownloadState() == DcMsg.DC_DOWNLOAD_IN_PROGRESS;
    boolean isCall = messageRecord.getType() == DcMsg.DC_MSG_CALL;

         if (isDownloading)                deliveryStatusView.setDownloading();
    else if (messageRecord.isPending())    deliveryStatusView.setPending();
    else if (messageRecord.isFailed())     deliveryStatusView.setFailed();
    else if (!messageRecord.isOutgoing() || isCall)  deliveryStatusView.setNone();
    else if (messageRecord.isRemoteRead()) deliveryStatusView.setRead();
    else if (messageRecord.isDelivered())  deliveryStatusView.setSent();
    else if (messageRecord.isPreparing())  deliveryStatusView.setPreparing();
    else                                   deliveryStatusView.setPending();

    if (messageRecord.isFailed()) {
      deliveryStatusView.setTint(Color.RED);
    } else {
      deliveryStatusView.setTint(textColor); // Reset the color to the standard color (because the footer is re-used in a RecyclerView)
    }
  }

  public String getDescription() {
      String desc = dateView.getText().toString();
      String deliveryDesc = deliveryStatusView.getDescription();
      if (!"".equals(deliveryDesc)) {
          desc += "\n" + deliveryDesc;
      }
      return desc;
  }
}
