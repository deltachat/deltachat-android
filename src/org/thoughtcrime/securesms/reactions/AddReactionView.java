package org.thoughtcrime.securesms.reactions;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.rpc.Reactions;
import com.b44t.messenger.rpc.Rpc;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.emoji.EmojiTextView;
import org.thoughtcrime.securesms.connect.DcHelper;

import java.util.Map;

public class AddReactionView extends LinearLayout {
    private EmojiTextView [] defaultReactionViews;
    private Context context;
    private DcContext dcContext;
    private Rpc rpc;
    private int msgIdToReactTo;
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
              defaultReactionViews[i].setOnClickListener(v -> reactionClicked(ii));
          }
      }
    }

    public void show(int msgIdToReactTo, View parentView, AddReactionListener listener) {
        init(); // init delayed as needed
        this.msgIdToReactTo = msgIdToReactTo;
        this.listener = listener;

        final String existingReaction = getSelfReaction();
        for (EmojiTextView defaultReactionView : defaultReactionViews) {
            if (defaultReactionView.getText().toString().equals(existingReaction)) {
                defaultReactionView.setBackground(ContextCompat.getDrawable(context, R.drawable.reaction_pill_background_selected));
            } else {
                defaultReactionView.setBackground(null);
            }
        }

        setVisibility(View.VISIBLE);
    }

    public void hide() {
        setVisibility(View.GONE);
    }

    private String getSelfReaction() {
        String result = null;
        try {
            final Reactions reactions = rpc.getMsgReactions(dcContext.getAccountId(), msgIdToReactTo);
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

    private void reactionClicked(int i) {
        try {
            final String reaction = defaultReactionViews[i].getText().toString();
            if (reaction.equals(getSelfReaction())) {
                rpc.sendReaction(dcContext.getAccountId(), msgIdToReactTo, "");
            } else {
                rpc.sendReaction(dcContext.getAccountId(), msgIdToReactTo, reaction);
            }
            if (listener != null) {
                listener.onShallHide();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public interface AddReactionListener {
        void onShallHide();
    }
}
