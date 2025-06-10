package org.thoughtcrime.securesms.reactions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.util.Pair;

import java.util.ArrayList;

public class ReactionRecipientsAdapter extends RecyclerView.Adapter
{
  private @NonNull ArrayList<Pair<Integer, String>> contactsReactions = new ArrayList<>();
  private final LayoutInflater                layoutInflater;
  private final ItemClickListener             clickListener;
  private final GlideRequests                 glideRequests;

  @Override
  public int getItemCount() {
    return contactsReactions.size();
  }

  public abstract static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View itemView) {
      super(itemView);
    }

    public abstract void bind(@NonNull GlideRequests glideRequests, int contactId, String reaction);
    public abstract void unbind(@NonNull GlideRequests glideRequests);
  }

  public static class ReactionViewHolder extends ViewHolder {

    ReactionViewHolder(@NonNull  final View itemView,
                       @Nullable final ItemClickListener clickListener) {
      super(itemView);
      itemView.setOnClickListener(view -> {
        if (clickListener != null) {
          clickListener.onItemClick(getView());
        }
      });
      ((ReactionRecipientItem) itemView).getReactionView().setOnClickListener(view -> {
        if (clickListener != null) {
          clickListener.onReactionClick(getView());
        }
      });
    }

    public ReactionRecipientItem getView() {
      return (ReactionRecipientItem) itemView;
    }

    public void bind(@NonNull GlideRequests glideRequests, int contactId, String reaction) {
      getView().bind(glideRequests, contactId, reaction);
    }

    @Override
    public void unbind(@NonNull GlideRequests glideRequests) {
      getView().unbind(glideRequests);
    }
  }

  public ReactionRecipientsAdapter(@NonNull  Context context,
                                   @NonNull  GlideRequests glideRequests,
                                   @Nullable ItemClickListener clickListener)
  {
    super();
    this.layoutInflater = LayoutInflater.from(context);
    this.glideRequests = glideRequests;
    this.clickListener = clickListener;
  }

  @NonNull
  @Override
  public ReactionRecipientsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new ReactionViewHolder(layoutInflater.inflate(R.layout.reaction_recipient_item, parent, false), clickListener);
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {

    Pair<Integer, String> pair = contactsReactions.get(i);
    Integer contactId = pair.first();
    String reaction = pair.second();

    ViewHolder holder = (ViewHolder) viewHolder;
    holder.unbind(glideRequests);
    holder.bind(glideRequests, contactId, reaction);
  }

  public interface ItemClickListener {
    void onItemClick(ReactionRecipientItem item);
    void onReactionClick(ReactionRecipientItem item);
  }

  public void changeData(ArrayList<Pair<Integer, String>> contactsReactions) {
    this.contactsReactions = contactsReactions==null? new ArrayList<>() : contactsReactions;
    notifyDataSetChanged();
  }
}
