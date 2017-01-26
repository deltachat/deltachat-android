/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *                        (C) 2013-2016 Nikolai Kudashov
 *                           (C) 2017 Björn Petersen
 *                    Contact: r10s@b44t.com, http://b44t.com
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see http://www.gnu.org/licenses/ .
 *
 ******************************************************************************/


package com.b44t.ui.ActionBar;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.R;
import com.b44t.ui.Components.LayoutHelper;

import java.lang.reflect.Field;

public class ActionBarMenuItem extends FrameLayout {

    public static class ActionBarMenuItemSearchListener {
        public void onSearchExpand() {
        }

        public boolean canCollapseSearch() {
            return true;
        }

        public void onSearchCollapse() {

        }

        public void onTextChanged(EditText editText) {
        }

        public void onSearchPressed(EditText editText) {
        }

        public void onUpPressed() {
        }

        public void onDownPressed() {
        }
    }

    public interface ActionBarMenuItemDelegate {
        void onItemClick(int id);
    }

    private ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout;
    private ActionBarMenu parentMenu;
    private ActionBarPopupWindow popupWindow;
    private EditText searchField;
    private ImageView searchUpButton;
    private ImageView searchDownButton;
    private SimpleTextView searchCountText;
    protected ImageView iconView;
    private LinearLayout searchContainer;
    private boolean isSearchField = false;
    private ActionBarMenuItemSearchListener listener;
    private Rect rect;
    private int[] location;
    private View selectedMenuView;
    private Runnable showMenuRunnable;
    //private boolean showFromBottom;
    private int menuHeight = AndroidUtilities.dp(16);
    private int subMenuOpenSide = 0;
    private ActionBarMenuItemDelegate delegate;
    private boolean allowCloseAnimation = true;
    //protected boolean overrideMenuClick;
    private boolean processedPopupClick;

    public ActionBarMenuItem(Context context, ActionBarMenu menu, int backgroundColor) {
        super(context);
        if (backgroundColor != 0) {
            setBackgroundDrawable(Theme.createBarSelectorDrawable(backgroundColor));
        }
        parentMenu = menu;

        iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.CENTER);
        addView(iconView);
        LayoutParams layoutParams = (LayoutParams) iconView.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        iconView.setLayoutParams(layoutParams);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (hasSubMenu() && (popupWindow == null || popupWindow != null && !popupWindow.isShowing())) {
                showMenuRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (getParent() != null) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        toggleSubMenu();
                    }
                };
                AndroidUtilities.runOnUIThread(showMenuRunnable, 200);
            }
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (hasSubMenu() && (popupWindow == null || popupWindow != null && !popupWindow.isShowing())) {
                if (event.getY() > getHeight()) {
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    toggleSubMenu();
                    return true;
                }
            } else if (popupWindow != null && popupWindow.isShowing()) {
                getLocationOnScreen(location);
                float x = event.getX() + location[0];
                float y = event.getY() + location[1];
                popupLayout.getLocationOnScreen(location);
                x -= location[0];
                y -= location[1];
                selectedMenuView  = null;
                for (int a = 0; a < popupLayout.getItemsCount(); a++) {
                    View child = popupLayout.getItemAt(a);
                    child.getHitRect(rect);
                    if ((Integer) child.getTag() < 100) {
                        if (!rect.contains((int) x, (int) y)) {
                            child.setPressed(false);
                            child.setSelected(false);
                            if (Build.VERSION.SDK_INT == 21) {
                                child.getBackground().setVisible(false, false);
                            }
                        } else {
                            child.setPressed(true);
                            child.setSelected(true);
                            if (Build.VERSION.SDK_INT >= 21) {
                                if (Build.VERSION.SDK_INT == 21) {
                                    child.getBackground().setVisible(true, false);
                                }
                                child.drawableHotspotChanged(x, y - child.getTop());
                            }
                            selectedMenuView = child;
                        }
                    }
                }
            }
        } else if (popupWindow != null && popupWindow.isShowing() && event.getActionMasked() == MotionEvent.ACTION_UP) {
            if (selectedMenuView != null) {
                selectedMenuView.setSelected(false);
                if (parentMenu != null) {
                    parentMenu.onItemClick((Integer) selectedMenuView.getTag());
                } else if (delegate != null) {
                    delegate.onItemClick((Integer) selectedMenuView.getTag());
                }
                popupWindow.dismiss(allowCloseAnimation);
            } else {
                popupWindow.dismiss();
            }
        } else {
            if (selectedMenuView != null) {
                selectedMenuView.setSelected(false);
                selectedMenuView = null;
            }
        }
        return super.onTouchEvent(event);
    }

    public void setDelegate(ActionBarMenuItemDelegate delegate) {
        this.delegate = delegate;
    }

    /*
    public void setShowFromBottom(boolean value) {
        showFromBottom = value;
        if (popupLayout != null) {
            popupLayout.setShowedFromBotton(showFromBottom);
        }
    }
    */

    public void setSubMenuOpenSide(int side) {
        subMenuOpenSide = side;
    }

    public TextView addSubItem(int id, String text, int icon) {
        if (popupLayout == null) {
            rect = new Rect();
            location = new int[2];
            popupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(getContext());
            popupLayout.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        if (popupWindow != null && popupWindow.isShowing()) {
                            v.getHitRect(rect);
                            if (!rect.contains((int) event.getX(), (int) event.getY())) {
                                popupWindow.dismiss();
                            }
                        }
                    }
                    return false;
                }
            });
            popupLayout.setDispatchKeyEventListener(new ActionBarPopupWindow.OnDispatchKeyEventListener() {
                @Override
                public void onDispatchKeyEvent(KeyEvent keyEvent) {
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && keyEvent.getRepeatCount() == 0 && popupWindow != null && popupWindow.isShowing()) {
                        popupWindow.dismiss();
                    }
                }
            });
        }
        TextView textView = new TextView(getContext());
        textView.setTextColor(0xff212121);
        textView.setBackgroundResource(R.drawable.list_selector);
        if (!LocaleController.isRTL) {
            textView.setGravity(Gravity.CENTER_VERTICAL);
        } else {
            textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        }
        textView.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), 0);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        textView.setMinWidth(AndroidUtilities.dp(196));
        textView.setTag(id);
        textView.setText(text);
        if (icon != 0) {
            textView.setCompoundDrawablePadding(AndroidUtilities.dp(12));
            if (!LocaleController.isRTL) {
                textView.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(icon), null, null, null);
            } else {
                textView.setCompoundDrawablesWithIntrinsicBounds(null, null, getResources().getDrawable(icon), null);
            }
        }
        //popupLayout.setShowedFromBotton(showFromBottom);
        popupLayout.addView(textView);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) textView.getLayoutParams();
        if (LocaleController.isRTL) {
            layoutParams.gravity = Gravity.RIGHT;
        }
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = AndroidUtilities.dp(48);
        textView.setLayoutParams(layoutParams);
        textView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (popupWindow != null && popupWindow.isShowing()) {
                    if (processedPopupClick) {
                        return;
                    }
                    processedPopupClick = true;
                    popupWindow.dismiss(allowCloseAnimation);
                }
                if (parentMenu != null) {
                    parentMenu.onItemClick((Integer) view.getTag());
                } else if (delegate != null) {
                    delegate.onItemClick((Integer) view.getTag());
                }
            }
        });
        menuHeight += layoutParams.height;
        return textView;
    }

    public boolean hasSubMenu() {
        return popupLayout != null;
    }

    public void toggleSubMenu() {
        if (popupLayout == null) {
            return;
        }
        if (showMenuRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(showMenuRunnable);
            showMenuRunnable = null;
        }
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
            return;
        }
        if (popupWindow == null) {
            popupWindow = new ActionBarPopupWindow(popupLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
            if (Build.VERSION.SDK_INT >= 19) {
                popupWindow.setAnimationStyle(0);
            } else {
                popupWindow.setAnimationStyle(R.style.PopupAnimation);
            }
            popupWindow.setOutsideTouchable(true);
            popupWindow.setClippingEnabled(true);
            popupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
            popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
            int wh = MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), MeasureSpec.AT_MOST);
            popupLayout.measure(wh, wh);
            popupWindow.getContentView().setFocusableInTouchMode(true);
            popupWindow.getContentView().setOnKeyListener(new OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_MENU && event.getRepeatCount() == 0 && event.getAction() == KeyEvent.ACTION_UP && popupWindow != null && popupWindow.isShowing()) {
                        popupWindow.dismiss();
                        return true;
                    }
                    return false;
                }
            });
        }
        processedPopupClick = false;
        popupWindow.setFocusable(true);
        if (popupLayout.getMeasuredWidth() == 0) {
            updateOrShowPopup(true, true);
        } else {
            updateOrShowPopup(true, false);
        }
        popupWindow.startAnimation();
    }

    public void openSearch(boolean openKeyboard) {
        if (searchContainer == null || searchContainer.getVisibility() == VISIBLE || parentMenu == null) {
            return;
        }
        parentMenu.parentActionBar.onSearchFieldVisibilityChanged(toggleSearch(openKeyboard));
    }

    public boolean toggleSearch(boolean openKeyboard) {
        if (searchContainer == null) {
            return false;
        }
        if (searchContainer.getVisibility() == VISIBLE) {
            if (listener == null || listener != null && listener.canCollapseSearch()) {
                searchContainer.setVisibility(GONE);
                searchField.clearFocus();
                setVisibility(VISIBLE);
                AndroidUtilities.hideKeyboard(searchField);
                if (listener != null) {
                    listener.onSearchCollapse();
                }
            }
            return false;
        } else {
            searchContainer.setVisibility(VISIBLE);
            setVisibility(GONE);
            searchField.setText("");
            searchField.requestFocus();
            if (openKeyboard) {
                AndroidUtilities.showKeyboard(searchField);
            }
            if (listener != null) {
                listener.onSearchExpand();
            }
            return true;
        }
    }

    public void closeSubMenu() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    public void setIcon(int resId) {
        iconView.setImageResource(resId);
    }

    public ImageView getImageView() {
        return iconView;
    }

    public EditText getSearchField() {
        return searchField;
    }

    /*
    public ActionBarMenuItem setOverrideMenuClick(boolean value) {
        overrideMenuClick = value;
        return this;
    }
    */

    public ActionBarMenuItem setIsSearchField(boolean value, boolean applyHack) {
        if (parentMenu == null) {
            return this;
        }
        if (value && searchContainer == null) {
            searchContainer = new LinearLayout(getContext());
            parentMenu.addView(searchContainer, 0);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) searchContainer.getLayoutParams();
            layoutParams.weight = 1;
            layoutParams.width = 0;
            layoutParams.height = LayoutHelper.MATCH_PARENT;
            layoutParams.leftMargin = AndroidUtilities.dp(6);
            searchContainer.setLayoutParams(layoutParams);
            searchContainer.setVisibility(GONE);

            searchField = new EditText(getContext());
            searchField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            searchField.setHintTextColor(0x88ffffff);
            searchField.setTextColor(0xffffffff);
            searchField.setSingleLine(true);
            searchField.setBackgroundResource(0);
            searchField.setPadding(0, 0, 0, 0);
            int inputType = searchField.getInputType() | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
            searchField.setInputType(inputType);
            searchField.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI); // IME_ACTION_SEARCH is confusing as we use a real time search and the search is already performed when the user will hit the "search" button
            searchField.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                public void onDestroyActionMode(ActionMode mode) {

                }

                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    return false;
                }
            });
            searchField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (/*actionId == EditorInfo.IME_ACTION_SEARCH || */event != null && (event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_SEARCH || event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                        AndroidUtilities.hideKeyboard(searchField);
                        if (listener != null) {
                            listener.onSearchPressed(searchField);
                        }
                    }
                    return false;
                }
            });
            searchField.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (listener != null) {
                        listener.onTextChanged(searchField);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            try {
                Field mCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
                mCursorDrawableRes.setAccessible(true);
                mCursorDrawableRes.set(searchField, R.drawable.search_carret);
            } catch (Exception e) {
                //nothing to do
            }
            searchField.setTextIsSelectable(false);
            if( applyHack ) {
                searchField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            // This is a HACK: normally, the soft keyboard should show up automatically whenever the user
                            // clicks the search input field - however, if the user selected an item in the search and comes back to the search result,
                            // a click on the search input field does ... nothing.
                            AndroidUtilities.showKeyboard(v);
                            // ... maybe re-setting the soft input mode may help, I've not tested this.
                            // finally, a combinaction of showKeyboard()/hideKeyboard() will show up the keyboard on the next click, too.
                            //parentMenu.getParent().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        }
                    }
                });
            }

            searchContainer.addView(searchField, LayoutHelper.createLinear(48, 48, 10.0f, Gravity.CENTER_VERTICAL));

            searchCountText = new SimpleTextView(getContext());
            searchCountText.setTextColor(Theme.ACTION_BAR_SUBTITLE_COLOR);
            searchCountText.setTextSize(14);
            searchCountText.setText("1/10");
            searchCountText.setVisibility(View.GONE);
            searchContainer.addView(searchCountText, LayoutHelper.createLinear(48, LayoutHelper.WRAP_CONTENT, Gravity.LEFT|Gravity.CENTER_VERTICAL));

            searchUpButton = new ImageView(getContext());
            searchUpButton.setImageResource(R.drawable.search_up);
            searchUpButton.setScaleType(ImageView.ScaleType.CENTER);
            searchContainer.addView(searchUpButton, LayoutHelper.createLinear(48, 48, Gravity.CENTER_VERTICAL));
            searchUpButton.setVisibility(View.GONE);
            searchUpButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onUpPressed();
                }
            });

            searchDownButton = new ImageView(getContext());
            searchDownButton.setImageResource(R.drawable.search_down);
            searchDownButton.setScaleType(ImageView.ScaleType.CENTER);
            searchContainer.addView(searchDownButton, LayoutHelper.createLinear(48, 48, Gravity.CENTER_VERTICAL));
            searchDownButton.setVisibility(View.GONE);
            searchDownButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onDownPressed();
                }
            });
        }
        isSearchField = value;
        return this;
    }

    public void setExtraSearchInfo(String text, boolean upDown, boolean enableUp, boolean enableDown)
    {
        if( searchCountText!=null && searchUpButton!=null && searchDownButton!=null ) {
            if( text!=null && !text.isEmpty() ) {
                searchCountText.setVisibility(View.VISIBLE);
                searchCountText.setText(text);
            }
            else {
                searchCountText.setVisibility(View.GONE);
            }

            if( upDown ) {
                searchUpButton.setVisibility(View.VISIBLE);
                searchUpButton.setEnabled(enableUp);
                searchUpButton.setAlpha(enableUp ? 1.0f : 0.25f);
                searchDownButton.setVisibility(View.VISIBLE);
                searchDownButton.setEnabled(enableDown);
                searchDownButton.setAlpha(enableDown ? 1.0f : 0.25f);
            }
            else {
                searchUpButton.setVisibility(View.GONE);
                searchDownButton.setVisibility(View.GONE);
            }
        }
    }

    public boolean isSearchField() {
        return isSearchField;
    }

    public ActionBarMenuItem setActionBarMenuItemSearchListener(ActionBarMenuItemSearchListener listener) {
        this.listener = listener;
        return this;
    }

    public ActionBarMenuItem setAllowCloseAnimation(boolean value) {
        allowCloseAnimation = value;
        return this;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (popupWindow != null && popupWindow.isShowing()) {
            updateOrShowPopup(false, true);
        }
    }

    private void updateOrShowPopup(boolean show, boolean update) {
        int offsetY;
        /*if (showFromBottom) {
            getLocationOnScreen(location);
            int diff = location[1] - AndroidUtilities.statusBarHeight + getMeasuredHeight() - menuHeight;
            offsetY = -menuHeight;
            if (diff < 0) {
                offsetY -= diff;
            }
        } else*/ {
            if (parentMenu != null && subMenuOpenSide == 0) {
                ActionBarMenu pm = parentMenu.parentActionBar.createMenu(); // for normal items, this is identical to parentMenu; for the action bar we take the normal menu as a a reference for the position
                offsetY = -pm.parentActionBar.getMeasuredHeight() + pm.getTop();
            } else {
                offsetY = -getMeasuredHeight();
            }
        }

        if (show) {
            popupLayout.scrollToTop();
        }

        if (subMenuOpenSide == 0) {
            /*if (showFromBottom) {
                if (show) {
                    popupWindow.showAsDropDown(this, -popupLayout.getMeasuredWidth() + getMeasuredWidth(), offsetY);
                }
                if (update) {
                    popupWindow.update(this, -popupLayout.getMeasuredWidth() + getMeasuredWidth(), offsetY, -1, -1);
                }
            } else*/ {
                if (parentMenu != null) {
                    View parent = parentMenu.parentActionBar;
                    if (show) {
                        popupWindow.showAsDropDown(parent, getLeft() + parentMenu.getLeft() + getMeasuredWidth() - popupLayout.getMeasuredWidth(), offsetY);
                    }
                    if (update) {
                        popupWindow.update(parent, getLeft() + parentMenu.getLeft() + getMeasuredWidth() - popupLayout.getMeasuredWidth(), offsetY, -1, -1);
                    }
                } else if (getParent() != null) {
                    View parent = (View) getParent();
                    if (show) {
                        popupWindow.showAsDropDown(parent, parent.getMeasuredWidth() - popupLayout.getMeasuredWidth() - getLeft() - parent.getLeft(), offsetY);
                    }
                    if (update) {
                        popupWindow.update(parent, parent.getMeasuredWidth() - popupLayout.getMeasuredWidth() - getLeft() - parent.getLeft(), offsetY, -1, -1);
                    }
                }
            }
        } else {
            if (show) {
                popupWindow.showAsDropDown(this, -AndroidUtilities.dp(8), offsetY);
            }
            if (update) {
                popupWindow.update(this, -AndroidUtilities.dp(8), offsetY, -1, -1);
            }
        }
    }

    public void hideSubItem(int id) {
        View view = popupLayout.findViewWithTag(id);
        if (view != null) {
            view.setVisibility(GONE);
        }
    }

    public void showSubItem(int id) {
        View view = popupLayout.findViewWithTag(id);
        if (view != null) {
            view.setVisibility(VISIBLE);
        }
    }
}
