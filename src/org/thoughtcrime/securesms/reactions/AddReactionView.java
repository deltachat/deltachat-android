package org.thoughtcrime.securesms.reactions;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.rpc.Rpc;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.emoji.EmojiTextView;
import org.thoughtcrime.securesms.connect.DcHelper;

public class AddReactionView extends LinearLayout {
    private EmojiTextView [] defaultReactions;
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
          defaultReactions = new EmojiTextView[]{
              findViewById(R.id.reaction_0),
              findViewById(R.id.reaction_1),
              findViewById(R.id.reaction_2),
              findViewById(R.id.reaction_3),
              findViewById(R.id.reaction_4),
          };
          for (int i = 0; i < defaultReactions.length; i++) {
              final int ii = i;
              defaultReactions[i].setOnClickListener(v -> reactionClicked(ii));
          }
      }
    }

    public void show(int msgIdToReactTo, View parentView, AddReactionListener listener) {
        init(); // init delayed as needed
        this.msgIdToReactTo = msgIdToReactTo;
        this.listener = listener;
        setVisibility(View.VISIBLE);
    }

    public void hide() {
        setVisibility(View.GONE);
    }

    private void reactionClicked(int i) {
        try {
            final String reaction = defaultReactions[i].getText().toString();
            rpc.sendReaction(dcContext.getAccountId(), msgIdToReactTo, reaction);
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
