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
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.ClearCacheService;
import com.b44t.messenger.FileLoader;
import com.b44t.messenger.FileLog;
import com.b44t.messenger.ImageLoader;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.R;
import com.b44t.messenger.Utilities;
import com.b44t.ui.ActionBar.ActionBar;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.Adapters.BaseFragmentAdapter;
import com.b44t.ui.Cells.HeaderCell;
import com.b44t.ui.Cells.TextInfoPrivacyCell;
import com.b44t.ui.Cells.TextSettingsCell;
import com.b44t.ui.Components.LayoutHelper;

import java.io.File;

public class CacheControlActivity extends BaseFragment {

    private ListAdapter listAdapter;

    private int headlineRow; // EDIT BY MR -- added
    private int databaseRow;
    private int databaseInfoRow;
    private int keepMediaRow;
    private int keepMediaInfoRow;
    private int cacheRow;
    private int cacheInfoRow;
    private int rowCount;

    private int typeTextSetting  = 0; // EDIT BY MR -- no gaps, please
    private int typeTextInfo     = 1;
    private int typeSectionTitle = 2;
    private int typeCount        = 3; // /EDIT BY MR -- no gaps, please

    //private long databaseSize = -1;
    //private long cacheSize = -1;
    //private long documentsSize = -1;
    //private long audioSize = -1;
    //private long musicSize = -1;
    //private long photoSize = -1;
    //private long videoSize = -1;
    //private long totalSize = -1;
    //private boolean clear[] = new boolean[6];
    //private boolean calculating = true;

    private volatile boolean canceled = false;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        rowCount = 0;
        headlineRow = -1;// rowCount++; // EDIT BY MR -- added
        keepMediaRow = rowCount++;
        keepMediaInfoRow = rowCount++;
        cacheRow = -1; // EDIT BY MR -- rowCount++;
        cacheInfoRow = -1; // EDIT BY MR -- rowCount++;

        databaseRow = -1; // EDIT BY MR -- was: rowCount++;
        databaseInfoRow = -1; // EDIT BY MR -- was: rowCount++;

        File file = new File(ApplicationLoader.getFilesDirFixed(), "cache4.db");
        //databaseSize = file.length();

        /*
        Utilities.globalQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                cacheSize = getDirectorySize(FileLoader.getInstance().checkDirectory(FileLoader.MEDIA_DIR_CACHE), 0);
                if (canceled) {
                    return;
                }
                photoSize = getDirectorySize(FileLoader.getInstance().checkDirectory(FileLoader.MEDIA_DIR_IMAGE), 0);
                if (canceled) {
                    return;
                }
                videoSize = getDirectorySize(FileLoader.getInstance().checkDirectory(FileLoader.MEDIA_DIR_VIDEO), 0);
                if (canceled) {
                    return;
                }
                documentsSize = getDirectorySize(FileLoader.getInstance().checkDirectory(FileLoader.MEDIA_DIR_DOCUMENT), 1);
                if (canceled) {
                    return;
                }
                musicSize = getDirectorySize(FileLoader.getInstance().checkDirectory(FileLoader.MEDIA_DIR_DOCUMENT), 2);
                if (canceled) {
                    return;
                }
                audioSize = getDirectorySize(FileLoader.getInstance().checkDirectory(FileLoader.MEDIA_DIR_AUDIO), 0);
                totalSize = cacheSize + videoSize + audioSize + photoSize + documentsSize + musicSize;
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        calculating = false;
                        if (listAdapter != null) {
                            listAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        });
        */

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        canceled = true;
    }

    /*private long getDirectorySize2(File dir) {
        long size = 0;
        if (dir.isDirectory()) {
            File[] array = dir.listFiles();
            if (array != null) {
                for (int a = 0; a < array.length; a++) {
                    File file = array[a];
                    if (file.isDirectory()) {
                        size += getDirectorySize2(file);
                    } else {
                        size += file.length();
                        FileLog.e("messenger", "" + file + " size = " + file.length());
                    }
                }
            }
        } else if (dir.isFile()) {
            FileLog.e("messenger", "" + dir + " size = " + dir.length());
            size += dir.length();
        }
        return size;
    }*/

    private long getDirectorySize(File dir, int documentsMusicType) {
        if (dir == null || canceled) {
            return 0;
        }
        long size = 0;
        if (dir.isDirectory()) {
            try {
                File[] array = dir.listFiles();
                if (array != null) {
                    for (int a = 0; a < array.length; a++) {
                        if (canceled) {
                            return 0;
                        }
                        File file = array[a];
                        if (documentsMusicType == 1 || documentsMusicType == 2) {
                            String name = file.getName().toLowerCase();
                            if (name.endsWith(".mp3") || name.endsWith(".m4a")) {
                                if (documentsMusicType == 1) {
                                    continue;
                                }
                            } else if (documentsMusicType == 2) {
                                continue;
                            }
                        }
                        if (file.isDirectory()) {
                            size += getDirectorySize(file, documentsMusicType);
                        } else {
                            size += file.length();
                        }
                    }
                }
            } catch (Throwable e) {
                FileLog.e("messenger", e);
            }
        } else if (dir.isFile()) {
            size += dir.length();
        }
        return size;
    }

    /*
    private void cleanupFolders() {
        final ProgressDialog progressDialog = new ProgressDialog(getParentActivity());
        progressDialog.setMessage(LocaleController.getString("Loading", R.string.Loading));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.show();
        Utilities.globalQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                boolean imagesCleared = false;
                for (int a = 0; a < 6; a++) {
                    if (!clear[a]) {
                        continue;
                    }
                    int type = -1;
                    int documentsMusicType = 0;
                    if (a == 0) {
                        type = FileLoader.MEDIA_DIR_IMAGE;
                    } else if (a == 1) {
                        type = FileLoader.MEDIA_DIR_VIDEO;
                    } else if (a == 2) {
                        type = FileLoader.MEDIA_DIR_DOCUMENT;
                        documentsMusicType = 1;
                    } else if (a == 3) {
                        type = FileLoader.MEDIA_DIR_DOCUMENT;
                        documentsMusicType = 2;
                    } else if (a == 4) {
                        type = FileLoader.MEDIA_DIR_AUDIO;
                    } else if (a == 5) {
                        type = FileLoader.MEDIA_DIR_CACHE;
                    }
                    if (type == -1) {
                        continue;
                    }
                    File file = FileLoader.getInstance().checkDirectory(type);
                    if (file != null) {
                        try {
                            File[] array = file.listFiles();
                            if (array != null) {
                                for (int b = 0; b < array.length; b++) {
                                    String name = array[b].getName().toLowerCase();
                                    if (documentsMusicType == 1 || documentsMusicType == 2) {
                                        if (name.endsWith(".mp3") || name.endsWith(".m4a")) {
                                            if (documentsMusicType == 1) {
                                                continue;
                                            }
                                        } else if (documentsMusicType == 2) {
                                            continue;
                                        }
                                    }
                                    if (name.equals(".nomedia")) {
                                        continue;
                                    }
                                    if (array[b].isFile()) {
                                        array[b].delete();
                                    }
                                }
                            }
                        } catch (Throwable e) {
                            FileLog.e("messenger", e);
                        }
                    }
                    if (type == FileLoader.MEDIA_DIR_CACHE) {
                        cacheSize = getDirectorySize(FileLoader.getInstance().checkDirectory(FileLoader.MEDIA_DIR_CACHE), documentsMusicType);
                        imagesCleared = true;
                    } else if (type == FileLoader.MEDIA_DIR_AUDIO) {
                        audioSize = getDirectorySize(FileLoader.getInstance().checkDirectory(FileLoader.MEDIA_DIR_AUDIO), documentsMusicType);
                    } else if (type == FileLoader.MEDIA_DIR_DOCUMENT) {
                        if (documentsMusicType == 1) {
                            documentsSize = getDirectorySize(FileLoader.getInstance().checkDirectory(FileLoader.MEDIA_DIR_DOCUMENT), documentsMusicType);
                        } else {
                            musicSize = getDirectorySize(FileLoader.getInstance().checkDirectory(FileLoader.MEDIA_DIR_DOCUMENT), documentsMusicType);
                        }
                    } else if (type == FileLoader.MEDIA_DIR_IMAGE) {
                        imagesCleared = true;
                        photoSize = getDirectorySize(FileLoader.getInstance().checkDirectory(FileLoader.MEDIA_DIR_IMAGE), documentsMusicType);
                    } else if (type == FileLoader.MEDIA_DIR_VIDEO) {
                        videoSize = getDirectorySize(FileLoader.getInstance().checkDirectory(FileLoader.MEDIA_DIR_VIDEO), documentsMusicType);
                    }
                }
                final boolean imagesClearedFinal = imagesCleared;
                totalSize = cacheSize + videoSize + audioSize + photoSize + documentsSize + musicSize;
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if (imagesClearedFinal) {
                            ImageLoader.getInstance().clearMemory();
                        }
                        if (listAdapter != null) {
                            listAdapter.notifyDataSetChanged();
                        }
                        try {
                            progressDialog.dismiss();
                        } catch (Exception e) {
                            FileLog.e("messenger", e);
                        }
                    }
                });
            }
        });
    }
    */

    @Override
    public View createView(final Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("CacheSettings", R.string.CacheSettings));
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
        listView.setVerticalScrollBarEnabled(false);
        listView.setDrawSelectorOnTop(true);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> adapterView, View view, final int i, long l) {
                if (i == keepMediaRow) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setItems(new CharSequence[]{context.getResources().getQuantityString(R.plurals.Weeks, 1, 1), LocaleController.formatPluralString("Months", 1), LocaleController.getString("KeepMediaForever", R.string.KeepMediaForever)}, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, final int which) {
                            SharedPreferences.Editor editor = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE).edit();
                            editor.putInt("keep_media", which).commit();
                            if (listAdapter != null) {
                                listAdapter.notifyDataSetChanged();
                            }
                            PendingIntent pintent = PendingIntent.getService(ApplicationLoader.applicationContext, 0, new Intent(ApplicationLoader.applicationContext, ClearCacheService.class), 0);
                            AlarmManager alarmManager = (AlarmManager) ApplicationLoader.applicationContext.getSystemService(Context.ALARM_SERVICE);
                            if (which == 2) {
                                alarmManager.cancel(pintent);
                            } else {
                                alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, AlarmManager.INTERVAL_DAY, AlarmManager.INTERVAL_DAY, pintent);
                            }
                        }
                    });
                    showDialog(builder.create());
                } else if (i == databaseRow) {
                    /* EDIT BY MR
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    builder.setMessage(LocaleController.getString("LocalDatabaseClear", R.string.LocalDatabaseClear));
                    builder.setPositiveButton(LocaleController.getString("CacheClear", R.string.CacheClear), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            final ProgressDialog progressDialog = new ProgressDialog(getParentActivity());
                            progressDialog.setMessage(LocaleController.getString("Loading", R.string.Loading));
                            progressDialog.setCanceledOnTouchOutside(false);
                            progressDialog.setCancelable(false);
                            progressDialog.show();
                            MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        SQLiteDatabase database = MessagesStorage.getInstance().getDatabase();
                                        ArrayList<Long> dialogsToCleanup = new ArrayList<>();
                                        SQLiteCursor cursor = database.queryFinalized("SELECT did FROM dialogs WHERE 1");
                                        StringBuilder ids = new StringBuilder();
                                        while (cursor.next()) {
                                            long did = cursor.longValue(0);
                                            int lower_id = (int) did;
                                            int high_id = (int) (did >> 32);
                                            if (lower_id != 0 && high_id != 1) {
                                                dialogsToCleanup.add(did);
                                            }
                                        }
                                        cursor.dispose();

                                        SQLitePreparedStatement state5 = database.executeFast("REPLACE INTO messages_holes VALUES(?, ?, ?)");
                                        SQLitePreparedStatement state6 = database.executeFast("REPLACE INTO media_holes_v2 VALUES(?, ?, ?, ?)");

                                        database.beginTransaction();
                                        for (int a = 0; a < dialogsToCleanup.size(); a++) {
                                            Long did = dialogsToCleanup.get(a);
                                            int messagesCount = 0;
                                            cursor = database.queryFinalized("SELECT COUNT(mid) FROM messages WHERE uid = " + did);
                                            if (cursor.next()) {
                                                messagesCount = cursor.intValue(0);
                                            }
                                            cursor.dispose();
                                            if (messagesCount <= 2) {
                                                continue;
                                            }

                                            cursor = database.queryFinalized("SELECT last_mid_i, last_mid FROM dialogs WHERE did = " + did);
                                            int messageId = -1;
                                            if (cursor.next()) {
                                                long last_mid_i = cursor.longValue(0);
                                                long last_mid = cursor.longValue(1);
                                                SQLiteCursor cursor2 = database.queryFinalized("SELECT data FROM messages WHERE uid = " + did + " AND mid IN (" + last_mid_i + "," + last_mid + ")");
                                                try {
                                                    while (cursor2.next()) {
                                                        NativeByteBuffer data = cursor2.byteBufferValue(0);
                                                        if (data != null) {
                                                            TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                                                            data.reuse();
                                                            if (message != null) {
                                                                messageId = message.id;
                                                            }
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    FileLog.e("messenger", e);
                                                }
                                                cursor2.dispose();

                                                database.executeFast("DELETE FROM messages WHERE uid = " + did + " AND mid != " + last_mid_i + " AND mid != " + last_mid).stepThis().dispose();
                                                database.executeFast("DELETE FROM messages_holes WHERE uid = " + did).stepThis().dispose();
                                                database.executeFast("DELETE FROM bot_keyboard WHERE uid = " + did).stepThis().dispose();
                                                database.executeFast("DELETE FROM media_counts_v2 WHERE uid = " + did).stepThis().dispose();
                                                database.executeFast("DELETE FROM media_v2 WHERE uid = " + did).stepThis().dispose();
                                                database.executeFast("DELETE FROM media_holes_v2 WHERE uid = " + did).stepThis().dispose();
                                                BotQuery.clearBotKeyboard(did, null);
                                                if (messageId != -1) {
                                                    MessagesStorage.createFirstHoles(did, state5, state6, messageId);
                                                }
                                            }
                                            cursor.dispose();
                                        }
                                        state5.dispose();
                                        state6.dispose();
                                        database.commitTransaction();
                                        database.executeFast("VACUUM").stepThis().dispose();
                                    } catch (Exception e) {
                                        FileLog.e("messenger", e);
                                    } finally {
                                        AndroidUtilities.runOnUIThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    progressDialog.dismiss();
                                                } catch (Exception e) {
                                                    FileLog.e("messenger", e);
                                                }
                                                if (listAdapter != null) {
                                                    File file = new File(ApplicationLoader.getFilesDirFixed(), "cache4.db");
                                                    databaseSize = file.length();
                                                    listAdapter.notifyDataSetChanged();
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    });
                    showDialog(builder.create());
                    */
                } else if (i == cacheRow) {
                    /* EDIT BY MR
                    if (totalSize <= 0 || getParentActivity() == null) {
                        return;
                    }
                    BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
                    builder.setApplyTopPadding(false);
                    builder.setApplyBottomPadding(false);
                    LinearLayout linearLayout = new LinearLayout(getParentActivity());
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    for (int a = 0; a < 6; a++) {
                        long size = 0;
                        String name = null;
                        if (a == 0) {
                            size = photoSize;
                            name = LocaleController.getString("LocalPhotoCache", R.string.LocalPhotoCache);
                        } else if (a == 1) {
                            size = videoSize;
                            name = LocaleController.getString("LocalVideoCache", R.string.LocalVideoCache);
                        } else if (a == 2) {
                            size = documentsSize;
                            name = LocaleController.getString("LocalDocumentCache", R.string.LocalDocumentCache);
                        } else if (a == 3) {
                            size = musicSize;
                            name = LocaleController.getString("LocalMusicCache", R.string.LocalMusicCache);
                        } else if (a == 4) {
                            size = audioSize;
                            name = LocaleController.getString("LocalAudioCache", R.string.LocalAudioCache);
                        } else if (a == 5) {
                            size = cacheSize;
                            name = LocaleController.getString("LocalCache", R.string.LocalCache);
                        }
                        if (size > 0) {
                            clear[a] = true;
                            CheckBoxCell checkBoxCell = new CheckBoxCell(getParentActivity());
                            checkBoxCell.setTag(a);
                            checkBoxCell.setBackgroundResource(R.drawable.list_selector);
                            linearLayout.addView(checkBoxCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
                            checkBoxCell.setText(name, AndroidUtilities.formatFileSize(size), true, true);
                            checkBoxCell.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    CheckBoxCell cell = (CheckBoxCell) v;
                                    int num = (Integer) cell.getTag();
                                    clear[num] = !clear[num];
                                    cell.setChecked(clear[num], true);
                                }
                            });
                        } else {
                            clear[a] = false;
                        }
                    }
                    BottomSheet.BottomSheetCell cell = new BottomSheet.BottomSheetCell(getParentActivity(), 1);
                    cell.setBackgroundResource(R.drawable.list_selector);
                    cell.setTextAndIcon(LocaleController.getString("ClearMediaCache", R.string.ClearMediaCache).toUpperCase(), 0);
                    cell.setTextColor(0xffcd5a5a);
                    cell.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                if (visibleDialog != null) {
                                    visibleDialog.dismiss();
                                }
                            } catch (Exception e) {
                                FileLog.e("messenger", e);
                            }
                            cleanupFolders();
                        }
                    });
                    linearLayout.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
                    builder.setCustomView(linearLayout);
                    showDialog(builder.create());
                    */
                }
            }
        });

        Toast.makeText(context, LocaleController.getString("NotYetImplemented", R.string.NotYetImplemented), Toast.LENGTH_SHORT).show();

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
            return i == databaseRow || /*i == cacheRow && totalSize > 0 ||*/ i == keepMediaRow;
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
            int type_ = getItemViewType(i);
            if (type_ == typeSectionTitle) {
                if (view == null) {
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                if (i == headlineRow) {
                    ((HeaderCell) view).setText(LocaleController.getString("Settings", R.string.Settings));
                }
            }
            else if (type_ == typeTextSetting) {
                if (view == null) {
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                TextSettingsCell textCell = (TextSettingsCell) view;
                if (i == databaseRow) {
                    //textCell.setTextAndValue(LocaleController.getString("LocalDatabase", R.string.LocalDatabase), AndroidUtilities.formatFileSize(databaseSize), false);
                } else if (i == cacheRow) {
                    /*if (calculating) {
                        textCell.setTextAndValue(LocaleController.getString("ClearMediaCache", R.string.ClearMediaCache), LocaleController.getString("CalculatingSize", R.string.CalculatingSize), false);
                    } else  {
                        textCell.setTextAndValue(LocaleController.getString("ClearMediaCache", R.string.ClearMediaCache), totalSize == 0 ? LocaleController.getString("CacheEmpty", R.string.CacheEmpty) : AndroidUtilities.formatFileSize(totalSize), false);
                    }
                    */
                } else if (i == keepMediaRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    int keepMedia = preferences.getInt("keep_media", 2);
                    String value;
                    if (keepMedia == 0) {
                        value = mContext.getResources().getQuantityString(R.plurals.Weeks, 1, 1);
                    } else if (keepMedia == 1) {
                        value = LocaleController.formatPluralString("Months", 1);
                    } else {
                        value = mContext.getString(R.string.KeepMediaForever);
                    }
                    textCell.setTextAndValue(mContext.getString(R.string.KeepMedia), value, false);
                }
            } else if (type_ == typeTextInfo) {
                if (view == null) {
                    view = new TextInfoPrivacyCell(mContext);
                }
                if (i == databaseInfoRow) {
                    ((TextInfoPrivacyCell) view).setText(LocaleController.getString("LocalDatabaseInfo", R.string.LocalDatabaseInfo));
                    view.setBackgroundResource(R.drawable.greydivider_bottom);
                } else if (i == cacheInfoRow) {
                    ((TextInfoPrivacyCell) view).setText("");
                    view.setBackgroundResource(R.drawable.greydivider);
                } else if (i == keepMediaInfoRow) {
                    ((TextInfoPrivacyCell) view).setText(AndroidUtilities.replaceTags(LocaleController.getString("KeepMediaInfo", R.string.KeepMediaInfo)));
                    view.setBackgroundResource(R.drawable.greydivider_bottom);
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == databaseRow || i == cacheRow || i == keepMediaRow) {
                return typeTextSetting;
            } else if (i == databaseInfoRow || i == cacheInfoRow || i == keepMediaInfoRow) {
                return typeTextInfo;
            }
            else if(i==headlineRow) {
                return typeSectionTitle;
            }
            return typeTextSetting;
        }

        @Override
        public int getViewTypeCount() {
            return typeCount;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
