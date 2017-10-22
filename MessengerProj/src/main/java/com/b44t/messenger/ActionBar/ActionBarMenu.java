/*******************************************************************************
 *
 *                              Delta Chat Android
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


package com.b44t.messenger.ActionBar;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.R;
import com.b44t.messenger.Components.LayoutHelper;

public class ActionBarMenu extends LinearLayout {

    protected ActionBar parentActionBar;
    private static Toast s_menuItemHint = null;
    public ActionBarMenu(Context context, ActionBar layer) {
        super(context);
        setOrientation(LinearLayout.HORIZONTAL);
        parentActionBar = layer;
    }

    public ActionBarMenu(Context context) {
        super(context);
    }

    public ActionBarMenuItem addItem(int id, Drawable drawable) {
        return addItem(id, 0, parentActionBar.itemsBackgroundColor, drawable, AndroidUtilities.dp(48));
    }

    public ActionBarMenuItem addItem(int id, int icon) {
        return addItem(id, icon, parentActionBar.itemsBackgroundColor);
    }

    public ActionBarMenuItem addItem(int id, int icon, int backgroundColor) {
        return addItem(id, icon, backgroundColor, null, AndroidUtilities.dp(48));
    }

    public ActionBarMenuItem addItemWithWidth(int id, int icon, int width) {
        return addItem(id, icon, parentActionBar.itemsBackgroundColor, null, width);
    }

    public ActionBarMenuItem addItem(int id, final int icon, int backgroundColor, Drawable drawable, int width) {
        ActionBarMenuItem menuItem = new ActionBarMenuItem(getContext(), this, backgroundColor);

        menuItem.setTag(id);
        if (drawable != null) {
            menuItem.iconView.setImageDrawable(drawable);
        } else {
            menuItem.iconView.setImageResource(icon);
        }
        addView(menuItem);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) menuItem.getLayoutParams();
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        layoutParams.width = width;
        menuItem.setLayoutParams(layoutParams);
        menuItem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ActionBarMenuItem item = (ActionBarMenuItem) view;
                if (item.hasSubMenu()) {
                    if (parentActionBar.actionBarMenuOnItemClick.canOpenMenu()) {
                        item.toggleSubMenu();
                    }
                } else if (item.isSearchField()) {
                    parentActionBar.onSearchFieldVisibilityChanged(item.toggleSearch(true));
                } else {
                    onItemClick((Integer) view.getTag());
                }
            }
        });
        menuItem.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                String hint = null;
                switch(icon) {
                    case R.drawable.ic_ab_fwd_delete:  hint = ApplicationLoader.applicationContext.getString(R.string.Delete); break;
                    case R.drawable.ic_ab_fwd_forward: hint = ApplicationLoader.applicationContext.getString(R.string.Forward); break;
                    case R.drawable.photo_crop:        hint = ApplicationLoader.applicationContext.getString(R.string.CropImage); break;
                    case R.drawable.photo_tools:       hint = ApplicationLoader.applicationContext.getString(R.string.EditImage); break;
                    case R.drawable.ic_ab_search:      hint = ApplicationLoader.applicationContext.getString(R.string.Search); break;
                    case R.drawable.ic_ab_lock_screen: hint = ApplicationLoader.applicationContext.getString(R.string.Passcode); break;
                }
                if( hint != null ) {
                    if( s_menuItemHint != null ) {
                        s_menuItemHint.cancel();
                        s_menuItemHint = null;
                    }
                    s_menuItemHint = AndroidUtilities.showHint(ApplicationLoader.applicationContext, hint);
                    return true;
                }
                else {
                    return false;
                }
            }
        });
        return menuItem;
    }

    public void hideAllPopupMenus() {
        int count = getChildCount();
        for (int a = 0; a < count; a++) {
            View view = getChildAt(a);
            if (view instanceof ActionBarMenuItem) {
                ((ActionBarMenuItem) view).closeSubMenu();
            }
        }
    }

    public void onItemClick(int id) {
        if (parentActionBar.actionBarMenuOnItemClick != null) {
            parentActionBar.actionBarMenuOnItemClick.onItemClick(id);
        }
    }

    public void clearItems() {
        removeAllViews();
    }

    public void onMenuButtonPressed() {
        int count = getChildCount();
        for (int a = 0; a < count; a++) {
            View view = getChildAt(a);
            if (view instanceof ActionBarMenuItem) {
                ActionBarMenuItem item = (ActionBarMenuItem) view;
                if (item.getVisibility() != VISIBLE) {
                    continue;
                }
                if (item.hasSubMenu()) {
                    item.toggleSubMenu();
                    break;
                }
            }
        }
    }

    public void closeSearchField() {
        int count = getChildCount();
        for (int a = 0; a < count; a++) {
            View view = getChildAt(a);
            if (view instanceof ActionBarMenuItem) {
                ActionBarMenuItem item = (ActionBarMenuItem) view;
                if (item.isSearchField()) {
                    parentActionBar.onSearchFieldVisibilityChanged(item.toggleSearch(false));
                    break;
                }
            }
        }
    }

    public void openSearchField(boolean toggle, String text) {
        int count = getChildCount();
        for (int a = 0; a < count; a++) {
            View view = getChildAt(a);
            if (view instanceof ActionBarMenuItem) {
                ActionBarMenuItem item = (ActionBarMenuItem) view;
                if (item.isSearchField()) {
                    if (toggle) {
                        parentActionBar.onSearchFieldVisibilityChanged(item.toggleSearch(true));
                    }
                    item.getSearchField().setText(text);
                    item.getSearchField().setSelection(text.length());
                    break;
                }
            }
        }
    }

    public ActionBarMenuItem getItem(int id) {
        View v = findViewWithTag(id);
        if (v instanceof ActionBarMenuItem) {
            return (ActionBarMenuItem) v;
        }
        return null;
    }
}
