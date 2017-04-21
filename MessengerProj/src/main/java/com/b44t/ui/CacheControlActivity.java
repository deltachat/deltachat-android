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

package com.b44t.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.R;
import com.b44t.ui.ActionBar.ActionBar;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.Adapters.BaseFragmentAdapter;
import com.b44t.ui.Cells.TextInfoCell;
import com.b44t.ui.Cells.TextSettingsCell;
import com.b44t.ui.Components.LayoutHelper;

public class CacheControlActivity extends BaseFragment {

    private ListAdapter listAdapter;

    private int rowKeepMediaSetting;
    private int rowKeepMediaInfo;
    private int rowCount;

    private final int ROWTYPE_TEXT_SETTING = 0; // no gaps here
    private final int ROWTYPE_TEXT_INFO = 1;
    private final int ROWTYPE_COUNT = 2;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        rowCount = 0;
        rowKeepMediaSetting = rowCount++;
        rowKeepMediaInfo = rowCount++;

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }

    @Override
    public View createView(final Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(context.getString(R.string.CacheSettings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        listAdapter = new ListAdapter(context);

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(0xfff0f0f0);

        ListView listView = new ListView(context);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setDrawSelectorOnTop(true);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> adapterView, View view, final int i, long l) {
                if (i == rowKeepMediaSetting) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setItems(new CharSequence[]{context.getResources().getQuantityString(R.plurals.Weeks, 1, 1), context.getResources().getQuantityString(R.plurals.Months, 1, 1), context.getString(R.string.KeepMediaForever)}, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, final int which) {
                            SharedPreferences.Editor editor = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE).edit();
                            editor.putInt("keep_media", which).apply();
                            if (listAdapter != null) {
                                listAdapter.notifyDataSetChanged();
                            }
                            Toast.makeText(context, context.getString(R.string.NotYetImplemented), Toast.LENGTH_SHORT).show();

                        }
                    });
                    showDialog(builder.create());
                }
            }
        });

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
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
            return i == rowKeepMediaSetting;
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
            int type = getItemViewType(i);
            if (type == ROWTYPE_TEXT_SETTING) {
                if (view == null) {
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                TextSettingsCell textCell = (TextSettingsCell) view;
                if (i == rowKeepMediaSetting) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    int keepMedia = preferences.getInt("keep_media", 2);
                    String value;
                    if (keepMedia == 0) {
                        value = mContext.getResources().getQuantityString(R.plurals.Weeks, 1, 1);
                    } else if (keepMedia == 1) {
                        value = mContext.getResources().getQuantityString(R.plurals.Months, 1, 1);
                    } else {
                        value = mContext.getString(R.string.KeepMediaForever);
                    }
                    textCell.setTextAndValue(mContext.getString(R.string.KeepMedia), value, false);
                }
            } else if (type == ROWTYPE_TEXT_INFO) {
                if (view == null) {
                    view = new TextInfoCell(mContext);
                }
                if (i == rowKeepMediaInfo) {
                    ((TextInfoCell) view).setText(AndroidUtilities.replaceTags(mContext.getString(R.string.KeepMediaInfo)));
                    view.setBackgroundResource(R.drawable.greydivider_bottom);
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == rowKeepMediaInfo) {
                return ROWTYPE_TEXT_INFO;
            }
            return ROWTYPE_TEXT_SETTING;
        }

        @Override
        public int getViewTypeCount() {
            return ROWTYPE_COUNT;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
