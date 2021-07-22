package org.thoughtcrime.securesms.messagerequests;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;

import org.thoughtcrime.securesms.R;

public class MessageRequestsBottomView extends ConstraintLayout {

//  private final Debouncer showProgressDebouncer = new Debouncer(250);

  private AppCompatTextView question;
  private Button            accept;
//  private Button            gv1Continue;
  private View              block;
  private View              delete;
//  private View              bigDelete;
//  private View              bigUnblock;
//  private View              busyIndicator;

//  private Group normalButtons;
//  private Group blockedButtons;
//  private Group gv1MigrationButtons;
//  private Group activeGroup;

  public MessageRequestsBottomView(Context context) {
    super(context);
  }

  public MessageRequestsBottomView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public MessageRequestsBottomView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    inflate(getContext(), R.layout.message_request_bottom_bar, this);

    question            = findViewById(R.id.message_request_question);
    accept              = findViewById(R.id.message_request_accept);
    block               = findViewById(R.id.message_request_block);
    delete              = findViewById(R.id.message_request_delete);
//    normalButtons       = findViewById(R.id.message_request_normal_buttons);
  }

//  public void setMessageData(@NonNull MessageRequestViewModel.MessageData messageData) {
//    Recipient recipient = messageData.getRecipient();
//
//    question.setLearnMoreVisible(false);
//    question.setOnLinkClickListener(null);
//
//    switch (messageData.getMessageState()) {
//      case BLOCKED_INDIVIDUAL:
//        question.setText(HtmlCompat.fromHtml(getContext().getString(R.string.MessageRequestBottomView_do_you_want_to_let_s_message_you_wont_receive_any_messages_until_you_unblock_them,
//                HtmlUtil.bold(recipient.getShortDisplayName(getContext()))), 0));
//        setActiveInactiveGroups(blockedButtons, normalButtons, gv1MigrationButtons);
//        break;
//      case BLOCKED_GROUP:
//        question.setText(R.string.MessageRequestBottomView_unblock_this_group_and_share_your_name_and_photo_with_its_members);
//        setActiveInactiveGroups(blockedButtons, normalButtons, gv1MigrationButtons);
//        break;
//      case LEGACY_INDIVIDUAL:
//        question.setText(getContext().getString(R.string.MessageRequestBottomView_continue_your_conversation_with_s_and_share_your_name_and_photo, recipient.getShortDisplayName(getContext())));
//        question.setLearnMoreVisible(true);
//        question.setOnLinkClickListener(v -> CommunicationActions.openBrowserLink(getContext(), getContext().getString(R.string.MessageRequestBottomView_legacy_learn_more_url)));
//        setActiveInactiveGroups(normalButtons, blockedButtons, gv1MigrationButtons);
//        accept.setText(R.string.MessageRequestBottomView_continue);
//        break;
//      case LEGACY_GROUP_V1:
//        question.setText(R.string.MessageRequestBottomView_continue_your_conversation_with_this_group_and_share_your_name_and_photo);
//        question.setLearnMoreVisible(true);
//        question.setOnLinkClickListener(v -> CommunicationActions.openBrowserLink(getContext(), getContext().getString(R.string.MessageRequestBottomView_legacy_learn_more_url)));
//        setActiveInactiveGroups(normalButtons, blockedButtons, gv1MigrationButtons);
//        accept.setText(R.string.MessageRequestBottomView_continue);
//        break;
//      case DEPRECATED_GROUP_V1:
//        question.setText(R.string.MessageRequestBottomView_upgrade_this_group_to_activate_new_features);
//        setActiveInactiveGroups(gv1MigrationButtons, normalButtons, blockedButtons);
//        gv1Continue.setVisibility(VISIBLE);
//        break;
//      case DEPRECATED_GROUP_V1_TOO_LARGE:
//        question.setText(getContext().getString(R.string.MessageRequestBottomView_this_legacy_group_can_no_longer_be_used, FeatureFlags.groupLimits().getHardLimit() - 1));
//        setActiveInactiveGroups(gv1MigrationButtons, normalButtons, blockedButtons);
//        gv1Continue.setVisibility(GONE);
//        break;
//      case GROUP_V1:
//      case GROUP_V2_INVITE:
//        question.setText(R.string.MessageRequestBottomView_do_you_want_to_join_this_group_they_wont_know_youve_seen_their_messages_until_you_accept);
//        setActiveInactiveGroups(normalButtons, blockedButtons, gv1MigrationButtons);
//        accept.setText(R.string.MessageRequestBottomView_accept);
//        break;
//      case GROUP_V2_ADD:
//        question.setText(R.string.MessageRequestBottomView_join_this_group_they_wont_know_youve_seen_their_messages_until_you_accept);
//        setActiveInactiveGroups(normalButtons, blockedButtons, gv1MigrationButtons);
//        accept.setText(R.string.MessageRequestBottomView_accept);
//        break;
//      case INDIVIDUAL:
//        question.setText(HtmlCompat.fromHtml(getContext().getString(R.string.MessageRequestBottomView_do_you_want_to_let_s_message_you_they_wont_know_youve_seen_their_messages_until_you_accept,
//                HtmlUtil.bold(recipient.getShortDisplayName(getContext()))), 0));
//        setActiveInactiveGroups(normalButtons, blockedButtons, gv1MigrationButtons);
//        accept.setText(R.string.MessageRequestBottomView_accept);
//        break;
//    }
//  }

//  private void setActiveInactiveGroups(@NonNull Group activeGroup, @NonNull Group... inActiveGroups) {
//    int initialVisibility = this.activeGroup != null ? this.activeGroup.getVisibility() : VISIBLE;
//
//    this.activeGroup = activeGroup;
//
//    for (Group inactive : inActiveGroups) {
//      inactive.setVisibility(GONE);
//    }
//
//    activeGroup.setVisibility(initialVisibility);
//  }

//  public void showBusy() {
//    showProgressDebouncer.publish(() -> busyIndicator.setVisibility(VISIBLE));
//    if (activeGroup != null) {
//      activeGroup.setVisibility(INVISIBLE);
//    }
//  }

//  public void hideBusy() {
//    showProgressDebouncer.clear();
//    busyIndicator.setVisibility(GONE);
//    if (activeGroup != null) {
//      activeGroup.setVisibility(VISIBLE);
//    }
//  }

  public void setAcceptOnClickListener(OnClickListener acceptOnClickListener) {
    accept.setOnClickListener(acceptOnClickListener);
  }

  public void setDeleteOnClickListener(OnClickListener deleteOnClickListener) {
    delete.setOnClickListener(deleteOnClickListener);
  }

  public void setBlockOnClickListener(OnClickListener blockOnClickListener) {
    block.setOnClickListener(blockOnClickListener);
  }

  public void setQuestion(String text) {
    question.setText(text);
  }
}
