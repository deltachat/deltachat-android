package org.thoughtcrime.securesms;

import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class BaseConversationListAdapter<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<T> {
  protected final Set<Long> batchSet  = Collections.synchronizedSet(new HashSet<Long>());
  protected       boolean   batchMode = false;

  public abstract void selectAllThreads();

  public void initializeBatchMode(boolean toggle) {
    batchMode = toggle;
    batchSet.clear();
    notifyDataSetChanged();
  }

  public void toggleThreadInBatchSet(long threadId) {
    if (batchSet.contains(threadId)) {
      batchSet.remove(threadId);
    } else if (threadId != -1) {
      batchSet.add(threadId);
    }
  }

  public Set<Long> getBatchSelections() {
    return batchSet;
  }
}
