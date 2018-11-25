package org.thoughtcrime.securesms;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientModifiedListener;
import org.thoughtcrime.securesms.util.ExpirationUtil;
import org.thoughtcrime.securesms.util.GroupUtil;
import org.thoughtcrime.securesms.util.Util;

import java.util.Locale;
import java.util.Set;

public class ConversationUpdateItem extends LinearLayout
    implements RecipientModifiedListener, BindableConversationItem
{
  private static final String TAG = ConversationUpdateItem.class.getSimpleName();

  private Set<DcMsg>    batchSelected;

  private ImageView     icon;
  private TextView      title;
  private TextView      body;
  private TextView      date;
  private Recipient     sender;
  private DcMsg         messageRecord;
  private Locale        locale;

  public ConversationUpdateItem(Context context) {
    super(context);
  }

  public ConversationUpdateItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void onFinishInflate() {
    super.onFinishInflate();

    this.icon  = findViewById(R.id.conversation_update_icon);
    this.title = findViewById(R.id.conversation_update_title);
    this.body  = findViewById(R.id.conversation_update_body);
    this.date  = findViewById(R.id.conversation_update_date);
  }

  @Override
  public void bind(@NonNull DcMsg                   messageRecord,
                   @NonNull DcChat                  dcChat,
                   @NonNull GlideRequests           glideRequests,
                   @NonNull Locale                  locale,
                   @NonNull Set<DcMsg>              batchSelected,
                   @NonNull Recipient               conversationRecipient,
                            boolean                 pulseUpdate)
  {
    this.batchSelected = batchSelected;

    bind(messageRecord, locale);
  }

  @Override
  public void setEventListener(@Nullable EventListener listener) {
    // No events to report yet
  }

  @Override
  public DcMsg getMessageRecord() {
    return messageRecord;
  }

  private void bind(@NonNull DcMsg messageRecord, @NonNull Locale locale) {
    this.messageRecord = messageRecord;
    this.sender        = Recipient.fromMsg(DcHelper.getContext(getContext()), messageRecord.getId());
    this.locale        = locale;

    this.sender.addListener(this);

    if      (messageRecord.isGroupAction())           setGroupRecord(messageRecord);
    else if (messageRecord.isJoined())                setJoinedRecord(messageRecord);
    else if (messageRecord.isExpirationTimerUpdate()) setTimerRecord(messageRecord);
    else if (messageRecord.isEndSession())            setEndSessionRecord(messageRecord);
    else if (messageRecord.isIdentityUpdate())        setIdentityRecord(messageRecord);
    else if (messageRecord.isIdentityVerified() ||
             messageRecord.isIdentityDefault())       setIdentityVerifyUpdate(messageRecord);
    else                                              setGenericInfoRecord(messageRecord);

    if (batchSelected.contains(messageRecord)) setSelected(true);
    else                                       setSelected(false);
  }

  private void setTimerRecord(final DcMsg messageRecord) {
    if (messageRecord.getExpiresIn() > 0) {
      icon.setImageResource(R.drawable.ic_timer);
      icon.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#757575"), PorterDuff.Mode.MULTIPLY));
    } else {
      icon.setImageResource(R.drawable.ic_timer_disabled);
      icon.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#757575"), PorterDuff.Mode.MULTIPLY));
    }

    title.setText(ExpirationUtil.getExpirationDisplayValue(getContext(), (int)(messageRecord.getExpiresIn() / 1000)));
    body.setText(messageRecord.getDisplayBody());

    title.setVisibility(VISIBLE);
    body.setVisibility(VISIBLE);
    date.setVisibility(GONE);
  }

  private void setIdentityRecord(final DcMsg messageRecord) {
    icon.setImageResource(R.drawable.ic_security_white_24dp);
    icon.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#757575"), PorterDuff.Mode.MULTIPLY));
    body.setText(messageRecord.getDisplayBody());

    title.setVisibility(GONE);
    body.setVisibility(VISIBLE);
    date.setVisibility(GONE);
  }

  private void setIdentityVerifyUpdate(final DcMsg messageRecord) {
    if (messageRecord.isIdentityVerified()) icon.setImageResource(R.drawable.ic_check_white_24dp);
    else                                    icon.setImageResource(R.drawable.ic_info_outline_white_24dp);

    icon.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#757575"), PorterDuff.Mode.MULTIPLY));
    body.setText(messageRecord.getDisplayBody());

    title.setVisibility(GONE);
    body.setVisibility(VISIBLE);
    date.setVisibility(GONE);
  }

  private void setGroupRecord(DcMsg messageRecord) {
    icon.setImageResource(R.drawable.ic_group_grey600_24dp);
    icon.clearColorFilter();

    body.setText(messageRecord.getDisplayBody());

    title.setVisibility(GONE);
    body.setVisibility(VISIBLE);
    date.setVisibility(GONE);
  }

  private void setJoinedRecord(DcMsg messageRecord) {
    icon.setImageResource(R.drawable.ic_favorite_grey600_24dp);
    icon.clearColorFilter();
    body.setText(messageRecord.getDisplayBody());

    title.setVisibility(GONE);
    body.setVisibility(VISIBLE);
    date.setVisibility(GONE);
  }

  private void setEndSessionRecord(DcMsg messageRecord) {
    icon.setImageResource(R.drawable.ic_refresh_white_24dp);
    icon.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#757575"), PorterDuff.Mode.MULTIPLY));
    body.setText(messageRecord.getDisplayBody());

    title.setVisibility(GONE);
    body.setVisibility(VISIBLE);
    date.setVisibility(GONE);
  }

  private void setGenericInfoRecord(DcMsg messageRecord) {
    icon.setImageResource(R.drawable.ic_group_grey600_24dp);
    icon.clearColorFilter();

    body.setText(messageRecord.getDisplayBody());

    title.setVisibility(GONE);
    body.setVisibility(VISIBLE);
    date.setVisibility(GONE);
  }

  @Override
  public void onModified(Recipient recipient) {
    Util.runOnMain(() -> bind(messageRecord, locale));
  }

  @Override
  public void unbind() {
    if (sender != null) {
      sender.removeListener(this);
    }
  }
}
