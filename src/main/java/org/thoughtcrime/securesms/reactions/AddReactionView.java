package org.thoughtcrime.securesms.reactions;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.emoji2.emojipicker.EmojiPickerView;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;
import chat.delta.rpc.types.Reactions;

public class AddReactionView extends LinearLayout {
    private AppCompatTextView[] defaultReactionViews;
    private AppCompatTextView anyReactionView;
    private boolean anyReactionClearsReaction;
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
          defaultReactionViews = new AppCompatTextView[]{
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
          anyReactionView = findViewById(R.id.reaction_any);
          anyReactionView.setOnClickListener(v -> anyReactionClicked());
      }
    }

    public void show(DcMsg msgToReactTo, View parentView, AddReactionListener listener) {
        init(); // init delayed as needed

        if ( msgToReactTo.isInfo()
          || !dcContext.getChat(msgToReactTo.getChatId()).canSend()) {
            return;
        }

        this.msgToReactTo = msgToReactTo;
        this.listener = listener;

        final String existingReaction = getSelfReaction();
        boolean existingHilited = false;
        for (AppCompatTextView defaultReactionView : defaultReactionViews) {
            if (defaultReactionView.getText().toString().equals(existingReaction)) {
                defaultReactionView.setBackground(ContextCompat.getDrawable(context, R.drawable.reaction_pill_background_selected));
                existingHilited = true;
            } else {
                defaultReactionView.setBackground(null);
            }
        }

        if (existingReaction != null && !existingHilited) {
            anyReactionView.setText(existingReaction);
            anyReactionView.setBackground(ContextCompat.getDrawable(context, R.drawable.reaction_pill_background_selected));
            anyReactionClearsReaction = true;
        } else {
            anyReactionView.setText("⋯");
            anyReactionView.setBackground(null);
            anyReactionClearsReaction = false;
        }

        final int offset = (int)(this.getHeight() * 0.666);
        int x = (int)parentView.getX();
        if (msgToReactTo.isOutgoing()) {
            x += parentView.getWidth() - offset - this.getWidth();
        } else {
            x += offset;
        }
        ViewUtil.setLeftMargin(this, Math.max(x, 0));

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
            final Reactions reactions = rpc.getMessageReactions(dcContext.getAccountId(), msgToReactTo.getId());
            if (reactions != null) {
                final Map<String, List<String>> reactionsByContact = reactions.reactionsByContact;
                final List<String> selfReactions = reactionsByContact.get(String.valueOf(DcContact.DC_CONTACT_ID_SELF));
                if (selfReactions != null && !selfReactions.isEmpty()) {
                    result = selfReactions.get(0);
                }
            }
        } catch(RpcException e) {
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
        if (anyReactionClearsReaction) {
            sendReaction(null);
        } else {
            View pickerLayout = View.inflate(context, R.layout.reaction_picker, null);

            final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setView(pickerLayout)
                .setTitle(R.string.react)
                .setPositiveButton(R.string.cancel, null)
                .create();

            EmojiPickerView pickerView = ViewUtil.findById(pickerLayout, R.id.emoji_picker);
            pickerView.setOnEmojiPickedListener((it) -> {
                sendReaction(it.getEmoji());
                alertDialog.dismiss();
            });

            alertDialog.show();
        }

        if (listener != null) {
            listener.onShallHide();
        }
    }

    private void sendReaction(final String reaction) {
        try {
            if (reaction == null || reaction.equals(getSelfReaction())) {
                rpc.sendReaction(dcContext.getAccountId(), msgToReactTo.getId(), Collections.singletonList(""));
            } else {
                rpc.sendReaction(dcContext.getAccountId(), msgToReactTo.getId(), Collections.singletonList(reaction));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public interface AddReactionListener {
        void onShallHide();
    }
}
