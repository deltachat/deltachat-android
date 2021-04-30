package org.thoughtcrime.securesms;

import android.content.Context;
import android.content.Intent;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;

import java.util.HashSet;
import java.util.Set;

public abstract class BaseConversationItem extends LinearLayout
    implements BindableConversationItem
{
  protected DcMsg         messageRecord;
  protected DcChat        dcChat;
  protected TextView      bodyText;

  protected final Context              context;
  protected final ApplicationDcContext dcContext;

  protected @NonNull  Set<DcMsg> batchSelected = new HashSet<>();

  protected final PassthroughClickListener passthroughClickListener = new PassthroughClickListener();

  public BaseConversationItem(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.context = context;
    this.dcContext = DcHelper.getContext(context);
  }

  protected void bind(@NonNull DcMsg            messageRecord,
                      @NonNull DcChat           dcChat,
                      @NonNull Set<DcMsg>       batchSelected)
  {
    this.messageRecord  = messageRecord;
    this.dcChat         = dcChat;
    this.batchSelected  = batchSelected;
  }

  @Override
  public void setOnClickListener(OnClickListener l) {
    super.setOnClickListener(new ClickListener(l));
  }

  protected boolean shouldInterceptClicks(DcMsg messageRecord) {
    return batchSelected.isEmpty() && (messageRecord.isFailed());
  }

  protected void handleDeadDropClick() {
    ConversationListFragment.DeaddropQuestionHelper helper = new ConversationListFragment.DeaddropQuestionHelper(context, messageRecord);
    new AlertDialog.Builder(context)
      .setPositiveButton(android.R.string.ok, (dialog, which) -> {
        int chatId = dcContext.decideOnContactRequest(messageRecord.getId(), DcContext.DC_DECISION_START_CHAT);
        if( chatId != 0 ) {
          Intent intent = new Intent(context, ConversationActivity.class);
          intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId);
          context.startActivity(intent);
        }
      })
      .setNegativeButton(android.R.string.cancel, null)
      .setNeutralButton(helper.answerBlock, (dialog, which) -> dcContext.decideOnContactRequest(messageRecord.getId(), DcContext.DC_DECISION_BLOCK))
      .setMessage(helper.question)
      .show();
  }

  protected class PassthroughClickListener implements View.OnLongClickListener, View.OnClickListener {

    @Override
    public boolean onLongClick(View v) {
      if (bodyText.hasSelection()) {
        return false;
      }
      performLongClick();
      return true;
    }

    @Override
    public void onClick(View v) {
      performClick();
    }
  }

  protected class ClickListener implements View.OnClickListener {
    private OnClickListener parent;

    ClickListener(@Nullable OnClickListener parent) {
      this.parent = parent;
    }

    public void onClick(View v) {
      if (dcChat.getId() == DcChat.DC_CHAT_ID_DEADDROP && batchSelected.isEmpty()) {
        handleDeadDropClick();
      } else if (!shouldInterceptClicks(messageRecord) && parent != null) {
        parent.onClick(v);
      } else if (messageRecord.isFailed()) {
        View view = View.inflate(context, R.layout.message_details_view, null);
        TextView detailsText = view.findViewById(R.id.details_text);
        detailsText.setText(messageRecord.getError());

        AlertDialog d = new AlertDialog.Builder(context)
                .setView(view)
                .setTitle(R.string.error)
                .setPositiveButton(R.string.ok, null)
                .create();
        d.show();
        try {
          //noinspection ConstantConditions
          Linkify.addLinks((TextView) d.findViewById(android.R.id.message), Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
        } catch(NullPointerException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
