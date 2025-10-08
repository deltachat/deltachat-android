package org.thoughtcrime.securesms.reactions;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.ArrayList;
import java.util.List;

import chat.delta.rpc.types.Reaction;

public class ReactionsConversationView extends LinearLayout {

  // Normally 6dp, but we have 1dp left+right margin on the pills themselves
  private static final int OUTER_MARGIN = ViewUtil.dpToPx(5);

  private final List<Reaction> reactions = new ArrayList<>();
  private boolean isIncoming;

  public ReactionsConversationView(Context context) {
    super(context);
    init(null);
  }

  public ReactionsConversationView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(attrs);
  }

  private void init(@Nullable AttributeSet attrs) {
    if (attrs != null) {
      TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.ReactionsConversationView, 0, 0);
      isIncoming = typedArray.getInt(R.styleable.ReactionsConversationView_reaction_type, 0) == 2;
    }
  }

  public void clear() {
    this.reactions.clear();
    removeAllViews();
  }

  public void setReactions(List<Reaction> reactions) {
    if (reactions.equals(this.reactions)) {
      return;
    }

    clear();
    this.reactions.addAll(reactions);

    for (Reaction reaction : buildShortenedReactionsList(this.reactions)) {
      View pill = buildPill(getContext(), this, reaction);
      addView(pill);
    }

    if (isIncoming) {
      ViewUtil.setLeftMargin(this, OUTER_MARGIN);
    } else {
      ViewUtil.setRightMargin(this, OUTER_MARGIN);
    }
  }

  private static @NonNull List<Reaction> buildShortenedReactionsList(@NonNull List<Reaction> reactions) {
    if (reactions.size() > 3) {
      List<Reaction> shortened = new ArrayList<>(3);
      shortened.add(reactions.get(0));
      shortened.add(reactions.get(1));
      int count = 0;
      boolean isFromSelf = false;
      for (int index = 2; index < reactions.size(); index++) {
          count += reactions.get(index).count;
          isFromSelf = isFromSelf || reactions.get(index).isFromSelf;
      }
      Reaction reaction = new Reaction();
      reaction.emoji = null;
      reaction.count = count;
      reaction.isFromSelf = isFromSelf;
      shortened.add(reaction);

      return shortened;
    } else {
      return reactions;
    }
  }

  private static View buildPill(@NonNull Context context, @NonNull ViewGroup parent, @NonNull Reaction reaction) {
    View           root      = LayoutInflater.from(context).inflate(R.layout.reactions_pill, parent, false);
    AppCompatTextView emojiView = root.findViewById(R.id.reactions_pill_emoji);
    TextView       countView = root.findViewById(R.id.reactions_pill_count);
    View           spacer    = root.findViewById(R.id.reactions_pill_spacer);

    if (reaction.emoji != null) {
      emojiView.setText(reaction.emoji);

      if (reaction.count > 1) {
        countView.setText(String.valueOf(reaction.count));
      } else {
        countView.setVisibility(GONE);
        spacer.setVisibility(GONE);
      }
    } else {
      emojiView.setVisibility(GONE);
      spacer.setVisibility(GONE);
      countView.setText("+" + reaction.count);
    }

    if (reaction.isFromSelf) {
      root.setBackground(ContextCompat.getDrawable(context, R.drawable.reaction_pill_background_selected));
      countView.setTextColor(ContextCompat.getColor(context, R.color.reaction_pill_text_color_selected));
    } else {
      root.setBackground(ContextCompat.getDrawable(context, R.drawable.reaction_pill_background));
    }

    return root;
  }
}
