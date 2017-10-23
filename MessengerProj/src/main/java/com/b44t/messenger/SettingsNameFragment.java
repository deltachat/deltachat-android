/*******************************************************************************
 *
 *                              Delta Chat Android
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

package com.b44t.messenger;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.b44t.messenger.ActionBar.ActionBar;
import com.b44t.messenger.ActionBar.ActionBarMenu;
import com.b44t.messenger.ActionBar.BaseFragment;
import com.b44t.messenger.Components.BaseFragmentAdapter;
import com.b44t.messenger.Cells.HeaderCell;
import com.b44t.messenger.Cells.EditTextCell;
import com.b44t.messenger.Cells.TextInfoCell;
import com.b44t.messenger.Components.LayoutHelper;


public class SettingsNameFragment extends BaseFragment {

    // the list
    private ListAdapter listAdapter;

    private int         rowDisplaynameHeadline, rowDisplayname, rowDisplaynameInfo;
    private int         rowStatusHeadline, rowStatus, rowStatusInfo;
    private int         rowCount;

    private final int   ROWTYPE_INFO       = 0; // no gaps here!
    private final int   ROWTYPE_TEXT_ENTRY = 1;
    private final int   ROWTYPE_HEADLINE   = 2;

    private EditTextCell displaynameCell; // warning all these objects may be null!
    private EditTextCell statusCell;

    // misc.
    private final static int done_button = 1;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        rowCount = 0;

        rowDisplaynameHeadline = rowCount++;
        rowDisplayname         = rowCount++;
        rowDisplaynameInfo     = rowCount++;

        rowStatusHeadline      = rowCount++;
        rowStatus              = rowCount++;
        rowStatusInfo          = rowCount++;

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
        actionBar.setTitle(context.getString(R.string.NameAndStatus));
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
                builder.setPositiveButton(R.string.OK, null);
                showDialog(builder.create());
                return;
            }

            MrMailbox.setConfig("displayname", v.isEmpty() ? null : v);
        }

        if( statusCell != null ) {
            String newstatus = statusCell.getValue().trim();
            String defstatus = ApplicationLoader.applicationContext.getString(R.string.DefaultStatusText);
            if( newstatus.equals(defstatus))  {
                MrMailbox.setConfig("selfstatus", null); // use default status
            }
            else {
                MrMailbox.setConfig("selfstatus", newstatus); // if v is empty, no status is send
            }
        }

        NotificationCenter.getInstance().postNotificationName(NotificationCenter.mainUserInfoChanged);

        finishFragment();
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
            return (i == rowDisplayname || i==rowStatus);
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
            if (type == ROWTYPE_HEADLINE) {
                if (view == null) {
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                if (i == rowDisplaynameHeadline) {
                    ((HeaderCell) view).setText(mContext.getString(R.string.MyName));
                }
                else if (i == rowStatusHeadline) {
                    ((HeaderCell) view).setText(mContext.getString(R.string.MyStatus));
                }
            }
            else if (type == ROWTYPE_TEXT_ENTRY) {
                if (i == rowDisplayname) {
                    if(displaynameCell==null) {
                        displaynameCell = new EditTextCell(mContext, false/*useLabel*/);
                        displaynameCell.getEditTextView().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                        displaynameCell.setValueHintAndLabel(MrMailbox.getConfig("displayname", ""),
                                "", "", true);
                    }
                    view = displaynameCell;
                }
                else if (i == rowStatus) {
                    if(statusCell==null) {
                        String statusText = MrMailbox.getConfig("selfstatus", null);
                        if( statusText == null ) {
                            statusText = mContext.getString(R.string.DefaultStatusText);
                        }
                        statusCell = new EditTextCell(mContext, false/*useLabel*/, true/*multiLine*/);
                        statusCell.setValueHintAndLabel(statusText, "", "", true);
                    }
                    view = statusCell;
                }
            } else if (type == ROWTYPE_INFO) {
                if (view == null) {
                    view = new TextInfoCell(mContext);
                }
                if( i==rowDisplaynameInfo) {
                    ((TextInfoCell) view).setText(mContext.getString(R.string.MyNameExplain));
                    view.setBackgroundResource(R.drawable.greydivider);
                }
                else if( i==rowStatusInfo) {
                    ((TextInfoCell) view).setText(mContext.getString(R.string.MyStatusExplain));
                    view.setBackgroundResource(R.drawable.greydivider_bottom);
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            return IGNORE_ITEM_VIEW_TYPE;
        }

        private int getItemViewType__(int i) {
            if (i == rowDisplayname|| i==rowStatus ) {
                return ROWTYPE_TEXT_ENTRY;
            }
            else if(i==rowDisplaynameHeadline || i==rowStatusHeadline) {
                return ROWTYPE_HEADLINE;
            }
            return ROWTYPE_INFO;
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
