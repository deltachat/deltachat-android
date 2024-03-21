package org.thoughtcrime.securesms.reactions;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.emoji2.emojipicker.EmojiPickerView;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import com.b44t.messenger.rpc.Reactions;
import com.b44t.messenger.rpc.Rpc;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.emoji.EmojiTextView;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.Map;

public class AddReactionView extends LinearLayout {
    private EmojiTextView [] defaultReactionViews;
    private Context context;
    private DcContext dcContext;
    private Rpc rpc;
    private DcMsg msgToReactTo;
    private AddReactionListener listener;

    public AddReactionView(Context context) {
        super(context);
    }

    public AddReactionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void init() {
      if (context == null) {
          context = getContext();
          dcContext = DcHelper.getContext(context);
          rpc = DcHelper.getRpc(getContext());
          defaultReactionViews = new EmojiTextView[]{
              findViewById(R.id.reaction_0),
              findViewById(R.id.reaction_1),
              findViewById(R.id.reaction_2),
              findViewById(R.id.reaction_3),
              findViewById(R.id.reaction_4),
          };
          for (int i = 0; i < defaultReactionViews.length; i++) {
              final int ii = i;
              defaultReactionViews[i].setOnClickListener(v -> defaultReactionClicked(ii));
          }
          EmojiTextView reactionAny = findViewById(R.id.reaction_any);
          reactionAny.setOnClickListener(v -> anyReactionClicked());
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
              reactionAny.setVisibility(View.GONE);
          }
      }
    }

    public void show(DcMsg msgToReactTo, View parentView, AddReactionListener listener) {
        init(); // init delayed as needed

        if ( msgToReactTo.isInfo()
          || msgToReactTo.getType() == DcMsg.DC_MSG_VIDEOCHAT_INVITATION
          || !dcContext.getChat(msgToReactTo.getChatId()).canSend()) {
            return;
        }

        this.msgToReactTo = msgToReactTo;
        this.listener = listener;

        final String existingReaction = getSelfReaction();
        for (EmojiTextView defaultReactionView : defaultReactionViews) {
            if (defaultReactionView.getText().toString().equals(existingReaction)) {
                defaultReactionView.setBackground(ContextCompat.getDrawable(context, R.drawable.reaction_pill_background_selected));
            } else {
                defaultReactionView.setBackground(null);
            }
        }

        final int offset = (int)(this.getHeight() * 0.666);
        int x = (int)parentView.getX();
        if (msgToReactTo.isOutgoing()) {
            x += parentView.getWidth() - offset - this.getWidth();
        } else {
            x += offset;
        }
        ViewUtil.setLeftMargin(this, x);

        int y = Math.max((int)parentView.getY() - offset, offset/2);
        ViewUtil.setTopMargin(this, y);

        setVisibility(View.VISIBLE);
    }

    public void hide() {
        setVisibility(View.GONE);
    }

    public void move(int dy) {
        if (msgToReactTo != null && getVisibility() == View.VISIBLE) {
            ViewUtil.setTopMargin(this, (int) this.getY() - dy);
        }
    }

    private String getSelfReaction() {
        String result = null;
        try {
            final Reactions reactions = rpc.getMsgReactions(dcContext.getAccountId(), msgToReactTo.getId());
            final Map<Integer, String[]> reactionsByContact = reactions.getReactionsByContact();
            final String [] selfReactions = reactionsByContact.get(DcContact.DC_CONTACT_ID_SELF);
            if (selfReactions != null && selfReactions.length > 0) {
                result = selfReactions[0];
            }
        } catch(Exception e) {
           e.printStackTrace();
        }
        return result;
    }

    private void defaultReactionClicked(int i) {
        final String reaction = defaultReactionViews[i].getText().toString();
        sendReaction(reaction);

        if (listener != null) {
            listener.onShallHide();
        }
    }

    private void anyReactionClicked() {
        View pickerLayout = View.inflate(context, R.layout.reaction_picker, null);

        final AlertDialog alertDialog = new AlertDialog.Builder(context)
              .setView(pickerLayout)
              .setTitle(R.string.react)
              .setPositiveButton(R.string.cancel, null)
              .create();

        EmojiPickerView pickerView = ViewUtil.findById(pickerLayout, R.id.emoji_picker);
        pickerView.setOnEmojiPickedListener((it) -> {
            final String reaction = it.getEmoji();
            sendReaction(reaction);
            alertDialog.dismiss();
        });

        alertDialog.show();

        if (listener != null) {
            listener.onShallHide();
        }
    }

    private void sendReaction(final String reaction) {
        try {
            if (reaction.equals(getSelfReaction())) {
                rpc.sendReaction(dcContext.getAccountId(), msgToReactTo.getId(), "");
            } else {
                rpc.sendReaction(dcContext.getAccountId(), msgToReactTo.getId(), reaction);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public interface AddReactionListener {
        void onShallHide();
    }
}
