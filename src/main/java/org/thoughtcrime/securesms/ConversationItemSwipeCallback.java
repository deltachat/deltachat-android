package org.thoughtcrime.securesms;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.util.AccessibilityUtil;
import org.thoughtcrime.securesms.util.ServiceUtil;

class ConversationItemSwipeCallback extends ItemTouchHelper.SimpleCallback {

  private static final float SWIPE_SUCCESS_DX           = ConversationSwipeAnimationHelper.TRIGGER_DX;
  private static final long  SWIPE_SUCCESS_VIBE_TIME_MS = 10;

  private boolean swipeBack;
  private boolean shouldTriggerSwipeFeedback;
  private boolean canTriggerSwipe;
  private float   latestDownX;
  private float   latestDownY;

  private final SwipeAvailabilityProvider     swipeAvailabilityProvider;
  private final ConversationItemTouchListener itemTouchListener;
  private final OnSwipeListener               onSwipeListener;

  ConversationItemSwipeCallback(@NonNull SwipeAvailabilityProvider swipeAvailabilityProvider,
                                @NonNull OnSwipeListener onSwipeListener)
  {
    super(0, ItemTouchHelper.END);
    this.itemTouchListener          = new ConversationItemTouchListener(this::updateLatestDownCoordinate);
    this.swipeAvailabilityProvider  = swipeAvailabilityProvider;
    this.onSwipeListener            = onSwipeListener;
    this.shouldTriggerSwipeFeedback = true;
    this.canTriggerSwipe            = true;
  }

  void attachToRecyclerView(@NonNull RecyclerView recyclerView) {
    recyclerView.addOnItemTouchListener(itemTouchListener);
    new ItemTouchHelper(this).attachToRecyclerView(recyclerView);
  }

  @Override
  public boolean onMove(@NonNull RecyclerView recyclerView,
                        @NonNull RecyclerView.ViewHolder viewHolder,
                        @NonNull RecyclerView.ViewHolder target)
  {
    return false;
  }

  @Override
  public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
  }

  @Override
  public int getSwipeDirs(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder)
  {
    if (cannotSwipeViewHolder(viewHolder)) return 0;
    return super.getSwipeDirs(recyclerView, viewHolder);
  }

  @Override
  public int convertToAbsoluteDirection(int flags, int layoutDirection) {
    if (swipeBack) {
      swipeBack = false;
      return 0;
    }
    return super.convertToAbsoluteDirection(flags, layoutDirection);
  }

  @Override
  public void onChildDraw(
          @NonNull Canvas c,
          @NonNull RecyclerView recyclerView,
          @NonNull RecyclerView.ViewHolder viewHolder,
          float dx, float dy, int actionState, boolean isCurrentlyActive)
  {
    if (cannotSwipeViewHolder(viewHolder)) return;

    float   sign              = getSignFromDirection(viewHolder.itemView);
    boolean isCorrectSwipeDir = sameSign(dx, sign);

    if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && isCorrectSwipeDir) {
      ConversationSwipeAnimationHelper.update((ConversationItem) viewHolder.itemView, Math.abs(dx), sign);
      handleSwipeFeedback((ConversationItem) viewHolder.itemView, Math.abs(dx));
      if (canTriggerSwipe) {
        setTouchListener(recyclerView, viewHolder, Math.abs(dx));
      }
    } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE || dx == 0) {
      ConversationSwipeAnimationHelper.update((ConversationItem) viewHolder.itemView, 0, 1);
    }

    if (dx == 0) {
      shouldTriggerSwipeFeedback = true;
      canTriggerSwipe            = true;
    }
  }

  private void handleSwipeFeedback(@NonNull ConversationItem item, float dx) {
    if (dx > SWIPE_SUCCESS_DX && shouldTriggerSwipeFeedback) {
      vibrate(item.getContext());
      ConversationSwipeAnimationHelper.trigger(item);
      shouldTriggerSwipeFeedback = false;
    }
  }

  private void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder) {
    if (cannotSwipeViewHolder(viewHolder)) return;

    ConversationItem item = ((ConversationItem) viewHolder.itemView);
    DcMsg messageRecord = item.getMessageRecord();

    onSwipeListener.onSwipe(messageRecord);
  }

  @SuppressLint("ClickableViewAccessibility")
  private void setTouchListener(@NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                float dx)
  {
    recyclerView.setOnTouchListener(new View.OnTouchListener() {

      // This variable is necessary to make sure that the handleTouchActionUp() and therefore onSwiped() is called only once.
      // Otherwise, any subsequent little swipe would invoke onSwiped().
      // We can't call recyclerView.setOnTouchListener(null) because another ConversationItem might have set its own
      // on touch listener in the meantime and we don't want to cancel it
      private boolean listenerCalled = false;

      @Override
      public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
          case MotionEvent.ACTION_DOWN:
            shouldTriggerSwipeFeedback = true;
            break;
          case MotionEvent.ACTION_UP:
            if (!listenerCalled) {
              listenerCalled = true;
              ConversationItemSwipeCallback.this.handleTouchActionUp(recyclerView, viewHolder, dx);
            }
            //fallthrough
          case MotionEvent.ACTION_CANCEL:
            swipeBack = true;
            shouldTriggerSwipeFeedback = false;
            // Sometimes the view does not go back correctly, so make sure that after 2s the progress is reset:
            viewHolder.itemView.postDelayed(() -> resetProgress(viewHolder), 2000);
            if (AccessibilityUtil.areAnimationsDisabled(viewHolder.itemView.getContext())) {
              resetProgress(viewHolder);
            }
            break;
        }
        return false;
      }

    });
  }

  private void handleTouchActionUp(@NonNull RecyclerView recyclerView,
                                   @NonNull RecyclerView.ViewHolder viewHolder,
                                   float dx)
  {
    if (dx > SWIPE_SUCCESS_DX) {
      canTriggerSwipe = false;
      onSwiped(viewHolder);
      if (shouldTriggerSwipeFeedback) {
        vibrate(viewHolder.itemView.getContext());
      }
    }
    recyclerView.cancelPendingInputEvents();
  }

  private static void resetProgress(RecyclerView.ViewHolder viewHolder) {
    ConversationSwipeAnimationHelper.update((ConversationItem) viewHolder.itemView,
            0f,
            getSignFromDirection(viewHolder.itemView));
  }

  private boolean cannotSwipeViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
    if (!(viewHolder.itemView instanceof ConversationItem)) return true;

    ConversationItem item = ((ConversationItem) viewHolder.itemView);
    return !swipeAvailabilityProvider.isSwipeAvailable(item.getMessageRecord()) ||
            item.disallowSwipe(latestDownX, latestDownY);
  }

  private void updateLatestDownCoordinate(float x, float y) {
    latestDownX = x;
    latestDownY = y;
  }

  private static float getSignFromDirection(@NonNull View view) {
    return view.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ? -1f : 1f;
  }

  private static boolean sameSign(float dX, float sign) {
    return dX * sign > 0;
  }

  private static void vibrate(@NonNull Context context) {
    Vibrator vibrator = ServiceUtil.getVibrator(context);
    if (vibrator != null) vibrator.vibrate(SWIPE_SUCCESS_VIBE_TIME_MS);
  }

  interface SwipeAvailabilityProvider {
    boolean isSwipeAvailable(DcMsg conversationMessage);
  }

  interface OnSwipeListener {
    void onSwipe(DcMsg conversationMessage);
  }
}
