package org.thoughtcrime.securesms;

import android.content.Context;
import android.os.Parcelable;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.thoughtcrime.securesms.util.ViewUtil;

/**
 * Like LinearLayoutManager but you can set a starting position
 */
class SetStartingPositionLinearLayoutManager extends LinearLayoutManager {

  private int pendingStartingPos = -1;

  SetStartingPositionLinearLayoutManager(Context context, int vertical, boolean b) {
    super(context, vertical, b);
  }

  @Override
  public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
    if (pendingStartingPos != -1 && state.getItemCount() > 0) {
      // scrollToPositionWithOffset(mPendingTargetPos, 0) would also do the job but the target item
      // would be at the bottom of the screen, not at the top
      int position = pendingStartingPos + 1;
      if (position < state.getItemCount()) {
        scrollToPositionWithOffset(pendingStartingPos + 1, getHeight() - ViewUtil.dpToPx(10));
      } else {
        // pendingTargetPos is the top-most item
        scrollToPosition(pendingStartingPos);
      }
      pendingStartingPos = -1;
    }

    super.onLayoutChildren(recycler, state);
  }

  @Override
  public void onRestoreInstanceState(Parcelable state) {
    pendingStartingPos = -1;
    super.onRestoreInstanceState(state);
  }

  public void setStartingPosition(int position) {
    pendingStartingPos = position;
  }
}