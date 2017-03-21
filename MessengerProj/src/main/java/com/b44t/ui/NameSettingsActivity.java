/*******************************************************************************
 *
 *                          Messenger Android Frontend
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
 *******************************************************************************
 *
 * File:    NameSettingsActivity.java
 * Purpose: Let the user configure his name
 *
 ******************************************************************************/

package com.b44t.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.NotificationCenter;
import com.b44t.messenger.R;
import com.b44t.ui.ActionBar.ActionBar;
import com.b44t.ui.ActionBar.ActionBarMenu;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.Adapters.BaseFragmentAdapter;
import com.b44t.ui.Cells.HeaderCell;
import com.b44t.ui.Cells.EditTextCell;
import com.b44t.ui.Cells.TextInfoCell;
import com.b44t.ui.Components.LayoutHelper;


public class NameSettingsActivity extends BaseFragment {

    // the list
    private ListAdapter listAdapter;

    private int         rowNameTitle;
    private int         rowDisplayname;
    private int         rowDisplaynameInfo;
    private int         rowCount;

    private final int   typeInfo      = 0; // no gaps here!
    private final int   typeTextEntry = 1;
    private final int   typeSection   = 2;

    EditTextCell displaynameCell; // warning all these objects may be null!

    // misc.
    private final static int done_button = 1;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        rowCount = 0;
        rowNameTitle = -1; // rowCount++;
        rowDisplayname = rowCount++;
        rowDisplaynameInfo = rowCount++;

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context) {

        // create action bar
        actionBar.setBackButtonImage(R.drawable.ic_close_white);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("MyName", R.string.MyName));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == done_button) {
                    saveData();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItemWithWidth(done_button, R.drawable.ic_done, AndroidUtilities.dp(56));

        // create object to hold the whole view
        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(0xfff0f0f0);

        // create the main layout list
        listAdapter = new ListAdapter(context);

        ListView listView = new ListView(context);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setVerticalScrollBarEnabled(false);
        listView.setDrawSelectorOnTop(true);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> adapterView, View view, final int i, long l) {
            }
        });

        return fragmentView;
    }

    private void saveData() {
        if( displaynameCell != null ) {
            String v = displaynameCell.getValue().trim();

            if( v.length()>=1 && v.charAt(0)=='.') {
                String cmd = v.substring(1);
                String execute_result = MrMailbox.cmdline(cmd);
                if( execute_result==null || execute_result.isEmpty()) {
                    execute_result = "ERROR: Unknown command.";
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setMessage(execute_result);
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });
                showDialog(builder.create());
                return;
            }

            MrMailbox.setConfig("displayname", v.isEmpty() ? null : v);
        }

        NotificationCenter.getInstance().postNotificationName(NotificationCenter.mainUserInfoChanged);

        finishFragment();
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen && displaynameCell!=null) {
            if(displaynameCell.getValue().isEmpty()) {
                displaynameCell.getEditTextView().requestFocus();
                AndroidUtilities.showKeyboard(displaynameCell.getEditTextView());
            }
        }
    }

    private class ListAdapter extends BaseFragmentAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int i) {
            return (i == rowDisplayname);
        }

        @Override
        public int getCount() {
            return rowCount;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int type = getItemViewType__(i);
            if (type == typeSection) {
                if (view == null) {
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                if (i == rowNameTitle) {
                    ((HeaderCell) view).setText(LocaleController.getString("MyName", R.string.MyName));
                }
            }
            else if (type == typeTextEntry) {
                if (i == rowDisplayname) {
                    if(displaynameCell==null) {
                        displaynameCell = new EditTextCell(mContext);
                        displaynameCell.getEditTextView().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                        displaynameCell.setValueHintAndLabel(MrMailbox.getConfig("displayname", ""),
                                "", "", true);
                    }
                    view = displaynameCell;
                }
            } else if (type == typeInfo) {
                if (view == null) {
                    view = new TextInfoCell(mContext);
                }
                if( i==rowDisplaynameInfo) {
                    ((TextInfoCell) view).setText(LocaleController.getString("MyNameExplain", R.string.MyNameExplain));
                }
                view.setBackgroundResource(R.drawable.greydivider_bottom);
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            return IGNORE_ITEM_VIEW_TYPE;
        }

        private int getItemViewType__(int i) {
            if (i == rowDisplayname) {
                return typeTextEntry;
            }
            else if(i==rowNameTitle) {
                return typeSection;
            }
            return typeInfo;
        }


        @Override
        public int getViewTypeCount() {
            return 1; /* SIC! internally, we ingnore the type, each row has its own type--otherwise text entry stuff would not work */
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
