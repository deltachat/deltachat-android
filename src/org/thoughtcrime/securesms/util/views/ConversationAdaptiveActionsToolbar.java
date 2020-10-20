package org.thoughtcrime.securesms.util.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.ViewUtil;

/**
 * This class was pulled from Signal (AdaptiveActionsToolbar) and then adapted to the ConversationActivity.
 */
public class ConversationAdaptiveActionsToolbar extends Toolbar {

  private static final int NAVIGATION_DP          = 56;
  private static final int ACTION_VIEW_WIDTH_DP   = 48;
  private static final int OVERFLOW_VIEW_WIDTH_DP = 36;

  private static final int ID_NEVER_SHOW_AS_ACTION  = R.id.menu_context_reply_privately;
  private static final int ID_ALWAYS_SHOW_AS_ACTION = R.id.menu_context_forward;

  private int   maxShown;

  public ConversationAdaptiveActionsToolbar(@NonNull Context context) {
    this(context, null);
  }

  public ConversationAdaptiveActionsToolbar(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, R.attr.toolbarStyle);
  }

  public ConversationAdaptiveActionsToolbar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ConversationAdaptiveActionsToolbar);

    maxShown = array.getInteger(R.styleable.ConversationAdaptiveActionsToolbar_aat_max_shown, 100);

    array.recycle();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    adjustMenuActions(getMenu(), maxShown, getMeasuredWidth());
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }

  public static void adjustMenuActions(@NonNull Menu menu, int maxToShow, int toolbarWidthPx) {
    int menuSize = 0;

    for (int i = 0; i < menu.size(); i++) {
      if (menu.getItem(i).isVisible()) {
        menuSize++;
      }
    }

    int widthAllowed = toolbarWidthPx - ViewUtil.dpToPx(NAVIGATION_DP);
    int nItemsToShow = Math.min(maxToShow, widthAllowed / ViewUtil.dpToPx(ACTION_VIEW_WIDTH_DP));

    if (nItemsToShow < menuSize) {
      widthAllowed -= ViewUtil.dpToPx(OVERFLOW_VIEW_WIDTH_DP);
    }

    nItemsToShow = Math.min(maxToShow, widthAllowed / ViewUtil.dpToPx(ACTION_VIEW_WIDTH_DP));

    menu.findItem(ID_ALWAYS_SHOW_AS_ACTION).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    nItemsToShow--;

    for (int i = 0; i < menu.size(); i++) {
      MenuItem item = menu.getItem(i);

      boolean neverShowAsAction = item.getItemId() == ID_NEVER_SHOW_AS_ACTION;
      boolean alwaysShowAsAction = item.getItemId() == ID_ALWAYS_SHOW_AS_ACTION;

      if (alwaysShowAsAction) continue;

      if (item.isVisible() && nItemsToShow > 0 && !neverShowAsAction) {
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        nItemsToShow--;
      } else {
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
      }
    }
  }
}
