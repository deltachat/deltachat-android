package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.DateUtils;

import java.util.Locale;

public class ConversationItemFooter extends LinearLayout {

  private TextView            dateView;
  private ImageView           secureIndicatorView;
  private ImageView           locationIndicatorView;
  private DeliveryStatusView  deliveryStatusView;

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
    secureIndicatorView   = findViewById(R.id.footer_secure_indicator);
    locationIndicatorView = findViewById(R.id.footer_location_indicator);
    deliveryStatusView    = new DeliveryStatusView(findViewById(R.id.delivery_indicator));

    if (attrs != null) {
      TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.ConversationItemFooter, 0, 0);
      setTextColor(typedArray.getInt(R.styleable.ConversationItemFooter_footer_text_color, getResources().getColor(R.color.core_white)));
      typedArray.recycle();
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
  }

  public void setMessageRecord(@NonNull DcMsg messageRecord, @NonNull Locale locale) {
    presentDate(messageRecord, locale);
    secureIndicatorView.setVisibility(messageRecord.isSecure() ? View.VISIBLE : View.GONE);
    locationIndicatorView.setVisibility(messageRecord.hasLocation() ? View.VISIBLE : View.GONE);
    presentDeliveryStatus(messageRecord);
  }

  public void setTextColor(int color) {
    dateView.setTextColor(color);
    secureIndicatorView.setColorFilter(color);
    locationIndicatorView.setColorFilter(color);
    deliveryStatusView.setTint(color);
  }

  private void presentDate(@NonNull DcMsg messageRecord, @NonNull Locale locale) {
    dateView.forceLayout();
//    if(messageRecord.hasDeviatingTimestamp()) {
      dateView.setText(DateUtils.getExtendedRelativeTimeSpanString(getContext(), locale, messageRecord.getTimestamp()));
//    }
//    else {
//      dateView.setText(DateUtils.getTimeOfDayTimeSpanString(getContext(), locale, messageRecord.getTimestamp()));
//    }
  }

  private void presentDeliveryStatus(@NonNull DcMsg messageRecord) {
    if      (!messageRecord.isOutgoing())  deliveryStatusView.setNone();
    else if (messageRecord.isRemoteRead()) deliveryStatusView.setRead();
    else if (messageRecord.isDelivered())  deliveryStatusView.setSent();
    else if (messageRecord.isFailed())     deliveryStatusView.setFailed();
    else if (messageRecord.isPreparing())  deliveryStatusView.setPreparing();
    else                                   deliveryStatusView.setPending();
  }
}
