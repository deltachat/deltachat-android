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
 ******************************************************************************/


package com.b44t.messenger;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.NumberPicker;

import com.b44t.messenger.Components.BaseFragmentAdapter;
import com.b44t.messenger.Cells.HeaderCell;
import com.b44t.messenger.Cells.ShadowSectionCell;
import com.b44t.messenger.Cells.TextCheckCell;
import com.b44t.messenger.Cells.TextSettingsCell;
import com.b44t.messenger.ActionBar.ActionBar;
import com.b44t.messenger.ActionBar.BaseFragment;
import com.b44t.messenger.Components.LayoutHelper;

import java.io.File;


public class SettingsAdvFragment extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    // the list
    private int accountSettingsRow;
    private int directShareRow;
    private int cacheRow;
    private int autoplayGifsRow;
    private int textSizeRow;
    private int showUnknownSendersRow;
    private int sendByEnterRow;
    private int raiseToSpeakRow;
    private int blockedRow;
    private int e2eEncryptionRow;
    private int settingsShadowRow;

    private int imexHeaderRow;
    private int initiateKeyTransferRow;
    private int manageKeysRow;
    private int backupRow;
    private int backupShadowRow;
    private int rowCount;

    private static final int ROWTYPE_SHADOW          = 0;
    private static final int ROWTYPE_TEXT_SETTINGS   = 1;
    private static final int ROWTYPE_CHECK           = 2;
    private static final int ROWTYPE_HEADER          = 3;
    private static final int ROWTYPE_COUNT           = 4;

    private ListView listView;

    public final int MR_E2EE_DEFAULT_ENABLED = 1; // when changing this constant, also change it in the C-part

    private File imexDir;

    public static int defMsgFontSize() {
        return 16;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.imexEnded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.imexProgress);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.imexFileWritten);

        rowCount = 0;
        accountSettingsRow = rowCount++;
        if (Build.VERSION.SDK_INT >= 23) {
            directShareRow = -1; // for now, seems not really to work, however, in T'gram it does
        }
        else {
            directShareRow = -1;
        }
        autoplayGifsRow         = rowCount++;
        textSizeRow             = rowCount++; // for now, we have the font size in the advanced settings; this is because the numberical selection is a little bit weird and does only affect the message text. It would be better to use the font size defined by the system with "sp" (Scale-independent Pixels which included the user's font size preference)
        showUnknownSendersRow   = -1;// rowCount++;
        sendByEnterRow          = rowCount++;
        raiseToSpeakRow         = rowCount++; // outgoing message
        cacheRow                = -1;// for now, the - non-functional - page is reachable by the "storage settings" in the "android App Settings" only
        blockedRow              = rowCount++;
        e2eEncryptionRow        = rowCount++;
        settingsShadowRow       = rowCount++;

        imexHeaderRow           = rowCount++;
        initiateKeyTransferRow  = rowCount++;
        manageKeysRow           = rowCount++;
        backupRow               = rowCount++;
        backupShadowRow         = rowCount++;

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.imexEnded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.imexProgress);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.imexFileWritten);
    }

    @Override
    public View createView(Context context)
    {
        // create action bar
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(ApplicationLoader.applicationContext.getString(R.string.AdvancedSettings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        // create object to hold the whole view
        fragmentView = new FrameLayout(context) {};
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(0xfff0f0f0);

        // create the main layout list
        ListAdapter listAdapter = new ListAdapter(context);

        listView = new ListView(context);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setDrawSelectorOnTop(true);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.START));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                if (i == sendByEnterRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    boolean send = preferences.getBoolean("send_by_enter", false);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("send_by_enter", !send);
                    editor.apply();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(!send);
                    }
                } else if (i == raiseToSpeakRow) {
                    MediaController.getInstance().toogleRaiseToSpeak();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(MediaController.getInstance().canRaiseToSpeak());
                    }
                } else if (i == autoplayGifsRow) {
                    MediaController.getInstance().toggleAutoplayGifs();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(MediaController.getInstance().canAutoplayGifs());
                    }
                } else if(i == directShareRow) {
                    MediaController.getInstance().toggleDirectShare();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(MediaController.getInstance().canDirectShare());
                    }
                }
                else if (i == blockedRow) {
                    presentFragment(new BlockedUsersActivity());
                }
                else if( i==showUnknownSendersRow) {
                    int oldval = MrMailbox.getConfigInt("show_deaddrop", 0);
                    if( oldval == 1 ) {
                        MrMailbox.setConfig("show_deaddrop", "0");
                    }
                    else {
                        MrMailbox.setConfig("show_deaddrop", "1");
                    }
                    MrMailbox.MrCallback(MrMailbox.MR_EVENT_MSGS_CHANGED, 0, 0);
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(oldval == 0);
                    }

                    // if showing deaddrop is disabled, also disable notifications for this chat (cannot be displayed otherwise)
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt("notify2_" + MrChat.MR_CHAT_ID_DEADDROP, oldval==1? 2 : 0);
                    editor.apply();
                }
                else if(i==e2eEncryptionRow )
                {
                    int oldval = MrMailbox.getConfigInt("e2ee_enabled", MR_E2EE_DEFAULT_ENABLED);
                    if( oldval == 1 ) {
                        MrMailbox.setConfig("e2ee_enabled", "0");
                    }
                    else {
                        MrMailbox.setConfig("e2ee_enabled", "1");
                    }
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(oldval == 0);
                    }
                }
                else if(i==accountSettingsRow)
                {
                    presentFragment(new SettingsAccountFragment(null));
                }
                else if(i==initiateKeyTransferRow )
                {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(getParentActivity());
                    builder1.setTitle(ApplicationLoader.applicationContext.getString(R.string.AutocryptKeyTransfer));
                    builder1.setMessage(AndroidUtilities.replaceTags(ApplicationLoader.applicationContext.getString(R.string.AutocryptKeyTransferMsgBeforeSend)));
                    builder1.setNegativeButton(R.string.Cancel, null);
                    builder1.setPositiveButton(R.string.AutocryptKeyTransferInitiate, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            progressDialog = new ProgressDialog(getParentActivity());
                            progressDialog.setMessage(ApplicationLoader.applicationContext.getString(R.string.OneMoment));
                            progressDialog.setCanceledOnTouchOutside(false);
                            progressDialog.setCancelable(false);
                            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, ApplicationLoader.applicationContext.getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    MrMailbox.stopOngoingProcess();
                                }
                            });
                            progressDialog.show();
                            Utilities.searchQueue.postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    final String sc = MrMailbox.initiateKeyTransfer();
                                    AndroidUtilities.runOnUIThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if( progressDialog != null ) {
                                                progressDialog.dismiss();
                                                progressDialog = null;
                                            }

                                            if( sc != null ) {
                                                String scFormatted = "";
                                                try {
                                                    scFormatted = sc.substring(0, 4) + "  -  " + sc.substring(5, 9) + "  -  " + sc.substring(10, 14) + "  -\n\n" +
                                                            sc.substring(15, 19) + "  -  " + sc.substring(20, 24) + "  -  " + sc.substring(25, 29) + "  -\n\n" +
                                                            sc.substring(30, 34) + "  -  " + sc.substring(35, 39) + "  -  " + sc.substring(40, 44);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                                AlertDialog.Builder builder2 = new AlertDialog.Builder(getParentActivity());
                                                builder2.setTitle(ApplicationLoader.applicationContext.getString(R.string.AutocryptKeyTransfer));
                                                builder2.setMessage(AndroidUtilities.replaceTags(String.format(ApplicationLoader.applicationContext.getString(R.string.AutocryptKeyTransferMsgAfterSend), scFormatted)));
                                                builder2.setPositiveButton(R.string.OK, null);
                                                builder2.setCancelable(false); // prevent the dialog from being dismissed accidentally (when the dialog is closed, the setup code is gone forever and the user has to create a new setup message)
                                                showDialog(builder2.create());
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    });
                    showDialog(builder1.create());

                }
                else if(i==manageKeysRow )
                {
                    imexShowMenu(ApplicationLoader.applicationContext.getString(R.string.E2EManagePrivateKeys),
                            MrMailbox.MR_IMEX_EXPORT_SELF_KEYS, ApplicationLoader.applicationContext.getString(R.string.ExportPrivateKeys),
                            MrMailbox.MR_IMEX_IMPORT_SELF_KEYS, ApplicationLoader.applicationContext.getString(R.string.ImportPrivateKeys));
                }
                else if( i == backupRow )
                {
                    imexShowMenu(ApplicationLoader.applicationContext.getString(R.string.Backup),
                            MrMailbox.MR_IMEX_EXPORT_BACKUP, ApplicationLoader.applicationContext.getString(R.string.ExportBackup),
                            0, ApplicationLoader.applicationContext.getString(R.string.ImportBackup));
                }
                else if (i == textSizeRow) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(R.string.TextSize);
                    final NumberPicker numberPicker = new NumberPicker(getParentActivity());
                    numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // otherwise the EditText gains the focus
                    final int MIN_VAL = 12;
                    final int MAX_VAL = 30;
                    final int DEF_VAL = SettingsAdvFragment.defMsgFontSize();
                    String displayValues[] = new String[MAX_VAL-MIN_VAL+1];
                    for( int v = MIN_VAL; v <= MAX_VAL; v++ ) {
                        String cur = String.format("%d", v);
                        if( v==DEF_VAL ) {
                            cur += " (" +ApplicationLoader.applicationContext.getString(R.string.Default)+ ")";
                        }
                        displayValues[v-MIN_VAL] = cur;
                    }
                    numberPicker.setMinValue(MIN_VAL);
                    numberPicker.setMaxValue(MAX_VAL);
                    numberPicker.setDisplayedValues(displayValues);
                    numberPicker.setWrapSelectorWheel(false);
                    numberPicker.setValue(ApplicationLoader.fontSize);
                    builder.setView(numberPicker);
                    builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt("msg_font_size", numberPicker.getValue());
                            ApplicationLoader.fontSize = numberPicker.getValue();
                            editor.apply();
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    showDialog(builder.create());
                }
            }
        });

        return fragmentView;
    }

    static public File getImexDir()
    {
        // DIRECTORY_DOCUMENTS is only available since KitKat; as we also support Ice Cream Sandwich and Jellybean (2017: 11% in total), this is no option
        // moreover, DIRECTORY_DOWNLOADS is easier accessible.
        // CAVE: do not use DownloadManager to add the file as it is deleted on uninstall then ...
        File imexDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return imexDir;
    }

    private void imexShowMenu(String title, final int exportCommand, final String exportMenuEntry, final int importCommand, final String importMenuEntry)
    {
        imexDir = getImexDir();
        imexDir.mkdirs();
        imexDir.setExecutable(true);
        imexDir.setReadable(true);
        imexDir.setWritable(true);

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity()); // was: BottomSheet.Builder
        builder.setTitle(title);
        CharSequence[] items = new CharSequence[]{exportMenuEntry, importMenuEntry};
        builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        /* when and why do we ask for a password on import/export?
                        - we ask for a password on exporting private keys to prevent them from being stolen
                          if the user hands out the device for a moment.
                          this is important as esp. the user won't even recognise keys stolen this way.
                        - we do _not_ ask for a password on import - of course, this can result in data loss, however,
                          if the device is under control of an attacker he can also just delete all data.
                          in difference to export, all these attacks are typically visible to the user.
                          so, asking for a password here won't increase the security significally. */

                        if( i==0 ) /*export*/ {
                            if (Build.VERSION.SDK_INT >= 23 && getParentActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                getParentActivity().requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4);
                                return;
                            }
                            AlertDialog.Builder builderPw = new AlertDialog.Builder(getParentActivity());

                            final EditText input = new EditText(getParentActivity());
                            input.setSingleLine();

                            FrameLayout container = new FrameLayout(getParentActivity());
                            FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            params.leftMargin = AndroidUtilities.dp(24);
                            params.rightMargin = AndroidUtilities.dp(24);
                            params.topMargin = AndroidUtilities.dp(16);
                            input.setLayoutParams(params);
                            input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS| InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            container.addView(input);

                            builderPw.setView(container);
                            builderPw.setTitle(exportMenuEntry);
                            String enterPwMsg = String.format(ApplicationLoader.applicationContext.getString(R.string.ImportExportExplain)+"\n\n"+ApplicationLoader.applicationContext.getString(R.string.EnterPasswordToContinue), MrMailbox.getConfig("addr", ""));
                            builderPw.setMessage(enterPwMsg);
                            builderPw.setNegativeButton(R.string.Cancel, null);
                            builderPw.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    String pw = input.getText().toString().trim();
                                    if( MrMailbox.checkPassword(pw)!=0 ) {
                                        startImex(exportCommand);
                                    }
                                    else {
                                        AndroidUtilities.showHint(ApplicationLoader.applicationContext, ApplicationLoader.applicationContext.getString(R.string.IncorrectPassword));
                                    }
                                }
                            });
                            showDialog(builderPw.create());
                        }
                        else /*import*/ {
                            if( importCommand ==MrMailbox.MR_IMEX_IMPORT_SELF_KEYS ) {
                                if (Build.VERSION.SDK_INT >= 23 && getParentActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    getParentActivity().requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 4);
                                    return;
                                }

                                AlertDialog.Builder builder3 = new AlertDialog.Builder(getParentActivity());
                                builder3.setTitle(importMenuEntry);
                                builder3.setMessage(AndroidUtilities.replaceTags(String.format(ApplicationLoader.applicationContext.getString(R.string.ImportPrivateKeysAsk2), imexDir.getAbsolutePath())));
                                builder3.setNegativeButton(R.string.Cancel, null);
                                builder3.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        startImex(importCommand);
                                    }
                                });
                                showDialog(builder3.create());
                            }
                            else {
                                AlertDialog.Builder builder3 = new AlertDialog.Builder(getParentActivity());
                                builder3.setTitle(importMenuEntry);
                                builder3.setPositiveButton(R.string.OK, null);
                                builder3.setMessage(AndroidUtilities.replaceTags(String.format(ApplicationLoader.applicationContext.getString(R.string.ImportBackupExplain2), imexDir.getAbsolutePath())));
                                showDialog(builder3.create());
                            }
                        }
                    }
                }
        );
        showDialog(builder.create());
    }

    private ProgressDialog progressDialog = null;
    private int            progressWhat = 0;
    private void startImex(int what)
    {
        if( progressDialog!=null ) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        progressWhat = what;
        progressDialog = new ProgressDialog(getParentActivity());
        progressDialog.setMessage(ApplicationLoader.applicationContext.getString(R.string.OneMoment));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, ApplicationLoader.applicationContext.getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MrMailbox.stopOngoingProcess();
            }
        });
        progressDialog.show();

        synchronized (MrMailbox.m_lastErrorLock) {
            MrMailbox.m_showNextErrorAsToast = false;
            MrMailbox.m_lastErrorString = "";
        }

        Utilities.searchQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                final int res = MrMailbox.imex(progressWhat, imexDir.getAbsolutePath());
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.imexEnded, (int)res);
                    }
                });
            }
        });
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if( id==NotificationCenter.imexProgress ) {
            if( progressDialog!=null ) {
                // we want the spinner together with a progress info
                int percent = (Integer)args[0] / 10;
                progressDialog.setMessage(ApplicationLoader.applicationContext.getString(R.string.OneMoment)+String.format(" %d%%", percent));
            }
        }
        else if( id==NotificationCenter.imexFileWritten ) {
            // Force media scanner to scan the new file - otherwise, it may not be visible eg. via USB/MTP.
            // Do _not_ add the file with DownloadManager.addCompletedDownload() as the file would be deleted when the app is uninstalled.  This frustrates the usecase export-uninstall-install-import.
            Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File permFile = new File((String)args[0]);
            Uri fileContentUri = Uri.fromFile(permFile);
            mediaScannerIntent.setData(fileContentUri);
            getParentActivity().sendBroadcast(mediaScannerIntent);
        }
        else if( id==NotificationCenter.imexEnded ) {
            final String errorString;

            synchronized (MrMailbox.m_lastErrorLock) {
                MrMailbox.m_showNextErrorAsToast = true;
                errorString = MrMailbox.m_lastErrorString;
            }

            if( progressDialog!=null ) {
                progressDialog.dismiss();
                progressDialog = null;
            }

            if( (int)args[0]==1 ) {
                // success
                if( progressWhat==MrMailbox.MR_IMEX_EXPORT_SELF_KEYS || progressWhat==MrMailbox.MR_IMEX_EXPORT_BACKUP ) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setMessage(AndroidUtilities.replaceTags(String.format(ApplicationLoader.applicationContext.getString(R.string.FilesSuccessfullyExported), imexDir.getAbsoluteFile()) + "\n\n" + ApplicationLoader.applicationContext.getString(R.string.ImportExportExplain)));
                    builder.setPositiveButton(R.string.OK, null);
                    showDialog(builder.create());
                }
                else {
                    AndroidUtilities.showDoneHint(ApplicationLoader.applicationContext);
                }
            }
            else if( !errorString.isEmpty() ) {
                // error (errorString is empty when the user has cancelled the export manually, don't show a message in this case)
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setMessage(errorString);
                builder.setPositiveButton(R.string.OK, null);
                showDialog(builder.create());
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
            int type = getItemViewType(i);
            return (type!=ROWTYPE_SHADOW && type!=ROWTYPE_HEADER);
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
            if (type == ROWTYPE_SHADOW) {
                if (view == null) {
                    view = new ShadowSectionCell(mContext);
                }
                view.setBackgroundResource(i==backupShadowRow? R.drawable.greydivider_bottom : R.drawable.greydivider);
            } else if (type == ROWTYPE_TEXT_SETTINGS) {
                if (view == null) {
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                TextSettingsCell textCell = (TextSettingsCell) view;
                if (i == cacheRow) {
                    textCell.setText(mContext.getString(R.string.CacheSettings), true);
                }
                else if (i == blockedRow) {
                    String cntStr = String.format("%d", MrMailbox.getBlockedCount());
                    textCell.setTextAndValue(ApplicationLoader.applicationContext.getString(R.string.BlockedContacts), cntStr, true);
                }
                else if( i==initiateKeyTransferRow ) {
                    textCell.setText(mContext.getString(R.string.AutocryptKeyTransferInitiate), true);
                }
                else if( i==manageKeysRow ) {
                    textCell.setText(mContext.getString(R.string.E2EManagePrivateKeys), true);
                }
                else if( i==backupRow ) {
                    textCell.setText(mContext.getString(R.string.Backup), false);
                }
                else if( i == accountSettingsRow ) {
                    textCell.setText(mContext.getString(R.string.AccountSettings), true);
                }
                else if (i == textSizeRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    int size = preferences.getInt("msg_font_size", SettingsAdvFragment.defMsgFontSize());
                    textCell.setTextAndValue(mContext.getString(R.string.TextSize), String.format("%d", size), true);
                }
            } else if (type == ROWTYPE_CHECK) {
                if (view == null) {
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                TextCheckCell textCell = (TextCheckCell) view;

                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                if (i == sendByEnterRow) {
                    textCell.setTextAndCheck(mContext.getString(R.string.SendByEnter), preferences.getBoolean("send_by_enter", false), true);
                } else if (i == raiseToSpeakRow) {
                    textCell.setTextAndCheck(mContext.getString(R.string.RaiseToSpeak), MediaController.getInstance().canRaiseToSpeak(), true);
                } else if (i == autoplayGifsRow) {
                    textCell.setTextAndCheck(mContext.getString(R.string.AutoplayGifs), MediaController.getInstance().canAutoplayGifs(), true);
                } else if (i == directShareRow) {
                    textCell.setTextAndValueAndCheck(mContext.getString(R.string.DirectShare), mContext.getString(R.string.DirectShareInfo), MediaController.getInstance().canDirectShare(), false, true);
                }
                else if( i==showUnknownSendersRow) {
                    textCell.setTextAndCheck(mContext.getString(R.string.DeaddropInChatlist),
                            MrMailbox.getConfigInt("show_deaddrop", 0)!=0, true);
                }
                else if( i == e2eEncryptionRow ) {
                    textCell.setTextAndCheck(mContext.getString(R.string.PreferE2EEncryption),
                            MrMailbox.getConfigInt("e2ee_enabled", MR_E2EE_DEFAULT_ENABLED)!=0, false);
                }
            }
            else if (type == ROWTYPE_HEADER) {
                if (view == null) {
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                if (i == imexHeaderRow) {
                    ((HeaderCell) view).setText(mContext.getString(R.string.ExportImportHeader));
                }
            }

            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (i== settingsShadowRow || i==backupShadowRow ) {
                return ROWTYPE_SHADOW;
            } else if( i==imexHeaderRow ) {
                return ROWTYPE_HEADER;
            } else if ( i == sendByEnterRow || i == raiseToSpeakRow || i == autoplayGifsRow || i==showUnknownSendersRow || i == directShareRow || i==e2eEncryptionRow) {
                return ROWTYPE_CHECK;
            } else {
                return ROWTYPE_TEXT_SETTINGS;
            }
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
