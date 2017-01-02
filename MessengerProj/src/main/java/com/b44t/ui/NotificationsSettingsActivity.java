/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.b44t.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.b44t.messenger.LocaleController;
import com.b44t.messenger.NotificationsController;
import com.b44t.messenger.NotificationCenter;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.ConnectionsManager;
import com.b44t.messenger.FileLog;
import com.b44t.messenger.R;
import com.b44t.ui.Adapters.BaseFragmentAdapter;
import com.b44t.ui.Cells.HeaderCell;
import com.b44t.ui.Cells.ShadowSectionCell;
import com.b44t.ui.Cells.TextCheckCell;
import com.b44t.ui.Cells.TextColorCell;
import com.b44t.ui.Cells.TextDetailSettingsCell;
import com.b44t.ui.ActionBar.ActionBar;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.Cells.TextSettingsCell;
import com.b44t.ui.Components.ColorPickerView;
import com.b44t.ui.Components.LayoutHelper;

public class NotificationsSettingsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private ListView listView;

    private int notificationsServiceRow;
    private int notificationsServiceConnectionRow;
    private int messageSectionRow;
    private int messageAlertRow;
    private int messagePreviewRow;
    private int messageVibrateRow;
    private int messageSoundRow;
    private int messageLedRow;
    private int messagePriorityRow;
    private int groupSectionRow2;
    private int groupSectionRow;
    private int groupAlertRow;
    //private int groupPreviewRow;
    private int groupVibrateRow;
    private int groupSoundRow;
    private int groupLedRow;
    private int groupPriorityRow;
    private int inappSectionRow2;
    private int inappSectionRow;
    private int inappSoundRow;
    private int inappVibrateRow;
    //private int inappPreviewRow;
    private int inchatSoundRow;
    private int inappPriorityRow;
    private int otherSectionRow2;
    private int otherSectionRow;
    private int badgeNumberRow;
    private int repeatRow;
    private int resetNotificationsRow;
    private int rowCount = 0;

    private final int TYPE_HEADER      = 0;
    private final int TYPE_CHECK_CELL  = 1;
    private final int TYPE_TEXTDETAIL  = 2;
    private final int TYPE_COLOR_CELL  = 3;
    private final int TYPE_SHADOW      = 4;
    private final int TYPE_TEXTSETTING = 5;
    private final int TYPE_COUNT       = 6;

    @Override
    public boolean onFragmentCreate() {
        messageSectionRow = rowCount++;
        messageAlertRow = rowCount++;
        messageSoundRow = rowCount++;
        messageVibrateRow = rowCount++;
        messageLedRow = rowCount++;
        messagePriorityRow = Build.VERSION.SDK_INT >= 21? rowCount++ : -1;

        groupSectionRow2 = rowCount++;
        groupSectionRow = rowCount++;
        groupAlertRow = rowCount++;
        //groupPreviewRow = rowCount++;
        groupSoundRow = rowCount++;
        groupVibrateRow = rowCount++;
        groupLedRow = rowCount++;
        groupPriorityRow = Build.VERSION.SDK_INT >= 21? rowCount++ : -1;

        inappSectionRow2 = rowCount++;
        inappSectionRow = rowCount++;
        inappSoundRow = rowCount++;
        inappVibrateRow = rowCount++;
        //inappPreviewRow = rowCount++;
        inappPriorityRow = Build.VERSION.SDK_INT >= 21? rowCount++ : -1;

        otherSectionRow2 = rowCount++;
        otherSectionRow = rowCount++;
        inchatSoundRow = rowCount++;
        messagePreviewRow = rowCount++;
        badgeNumberRow = rowCount++;
        repeatRow = rowCount++;
        notificationsServiceRow = rowCount++;
        notificationsServiceConnectionRow = rowCount++;
        resetNotificationsRow = rowCount++;

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.notificationsSettingsUpdated);

        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.notificationsSettingsUpdated);
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("NotificationsAndSounds", R.string.NotificationsAndSounds));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new ListView(context);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setVerticalScrollBarEnabled(false);
        frameLayout.addView(listView);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        listView.setLayoutParams(layoutParams);
        listView.setAdapter(new ListAdapter(context));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, final View view, final int i, long l) {
                boolean enabled = false;
                if (i == messageAlertRow || i == groupAlertRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    if (i == messageAlertRow) {
                        enabled = preferences.getBoolean("EnableAll", true);
                        editor.putBoolean("EnableAll", !enabled);
                    } else if (i == groupAlertRow) {
                        enabled = preferences.getBoolean("EnableGroup", true);
                        editor.putBoolean("EnableGroup", !enabled);
                    }
                    editor.commit();
                } else if (i == messagePreviewRow /*|| i == groupPreviewRow*/) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    //if (i == messagePreviewRow) {
                        enabled = preferences.getBoolean("EnablePreviewAll", true);
                        editor.putBoolean("EnablePreviewAll", !enabled);
                    //} else if (i == groupPreviewRow) {
                    //    enabled = preferences.getBoolean("EnablePreviewGroup", true);
                    //    editor.putBoolean("EnablePreviewGroup", !enabled);
                    //}
                    editor.commit();
                } else if (i == messageSoundRow || i == groupSoundRow) {
                    try {
                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                        Intent tmpIntent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                        tmpIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                        tmpIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                        tmpIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                        Uri currentSound = null;

                        String defaultPath = null;
                        Uri defaultUri = Settings.System.DEFAULT_NOTIFICATION_URI;
                        if (defaultUri != null) {
                            defaultPath = defaultUri.getPath();
                        }

                        if (i == messageSoundRow) {
                            String path = preferences.getString("GlobalSoundPath", defaultPath);
                            if (path != null && !path.equals("NoSound")) {
                                if (path.equals(defaultPath)) {
                                    currentSound = defaultUri;
                                } else {
                                    currentSound = Uri.parse(path);
                                }
                            }
                        } else if (i == groupSoundRow) {
                            String path = preferences.getString("GroupSoundPath", defaultPath);
                            if (path != null && !path.equals("NoSound")) {
                                if (path.equals(defaultPath)) {
                                    currentSound = defaultUri;
                                } else {
                                    currentSound = Uri.parse(path);
                                }
                            }
                        }
                        tmpIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentSound);
                        startActivityForResult(tmpIntent, i);
                    } catch (Exception e) {
                        FileLog.e("messenger", e);
                    }
                } else if (i == resetNotificationsRow) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setMessage(LocaleController.formatString("AskResetNotifications", R.string.AskResetNotifications));
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.clear();
                            editor.commit();
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    showDialog(builder.create());


                } else if (i == inappSoundRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    enabled = preferences.getBoolean("EnableInAppSounds", true);
                    editor.putBoolean("EnableInAppSounds", !enabled);
                    editor.commit();
                } else if (i == inappVibrateRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    enabled = preferences.getBoolean("EnableInAppVibrate", true);
                    editor.putBoolean("EnableInAppVibrate", !enabled);
                    editor.commit();
                /*} else if (i == inappPreviewRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    enabled = preferences.getBoolean("EnableInAppPreview", true);
                    editor.putBoolean("EnableInAppPreview", !enabled);
                    editor.commit();*/
                } else if (i == inchatSoundRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    enabled = preferences.getBoolean("EnableInChatSound", true);
                    editor.putBoolean("EnableInChatSound", !enabled);
                    editor.commit();
                    NotificationsController.getInstance().setInChatSoundEnabled(!enabled);
                } else if (i == badgeNumberRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    enabled = preferences.getBoolean("badgeNumber", true);
                    editor.putBoolean("badgeNumber", !enabled);
                    editor.commit();
                    NotificationsController.getInstance().setBadgeEnabled(!enabled);
                } else if (i == notificationsServiceConnectionRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                    enabled = preferences.getBoolean("pushConnection", true);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("pushConnection", !enabled);
                    editor.commit();
                    if (!enabled) {
                        ConnectionsManager.getInstance().setPushConnectionEnabled(true);
                    } else {
                        ConnectionsManager.getInstance().setPushConnectionEnabled(false);
                    }
                } else if (i == notificationsServiceRow) {
                    final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                    final SharedPreferences.Editor editor = preferences.edit();
                    enabled = preferences.getBoolean("pushService", true);
                    editor.putBoolean("pushService", !enabled);
                    editor.commit();
                    ApplicationLoader.stopPushService();
                    ApplicationLoader.startPushService();
                } else if (i == messageLedRow || i == groupLedRow) {
                    if (getParentActivity() == null) {
                        return;
                    }

                    LinearLayout linearLayout = new LinearLayout(getParentActivity());
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    final ColorPickerView colorPickerView = new ColorPickerView(getParentActivity());
                    linearLayout.addView(colorPickerView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                    if (i == messageLedRow) {
                        colorPickerView.setOldCenterColor(preferences.getInt("MessagesLed", 0xff00ff00));
                    } else if (i == groupLedRow) {
                        colorPickerView.setOldCenterColor(preferences.getInt("GroupLed", 0xff00ff00));
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("LedColor", R.string.LedColor));
                    builder.setView(linearLayout);
                    builder.setPositiveButton(LocaleController.getString("Set", R.string.Set), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int which) {
                            final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            TextColorCell textCell = (TextColorCell) view;
                            if (i == messageLedRow) {
                                editor.putInt("MessagesLed", colorPickerView.getColor());
                                textCell.setTextAndColor(LocaleController.getString("LedColor", R.string.LedColor), colorPickerView.getColor(), true);
                            } else if (i == groupLedRow) {
                                editor.putInt("GroupLed", colorPickerView.getColor());
                                textCell.setTextAndColor(LocaleController.getString("LedColor", R.string.LedColor), colorPickerView.getColor(), true);
                            }
                            editor.commit();
                        }
                    });
                    builder.setNeutralButton(LocaleController.getString("Disabled", R.string.Disabled), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            TextColorCell textCell = (TextColorCell) view;
                            if (i == messageLedRow) {
                                editor.putInt("MessagesLed", 0);
                                textCell.setTextAndColor(LocaleController.getString("LedColor", R.string.LedColor), 0, true);
                            } else if (i == groupLedRow) {
                                editor.putInt("GroupLed", 0);
                                textCell.setTextAndColor(LocaleController.getString("LedColor", R.string.LedColor), 0, true);
                            }
                            editor.commit();
                            listView.invalidateViews();
                        }
                    });
                    showDialog(builder.create());
                } else if (i == messageVibrateRow || i == groupVibrateRow) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("Vibrate", R.string.Vibrate));
                    builder.setItems(new CharSequence[]{
                            LocaleController.getString("Disabled", R.string.Disabled),
                            LocaleController.getString("VibrationDefault", R.string.VibrationDefault),
                            LocaleController.getString("Short", R.string.Short),
                            LocaleController.getString("Long", R.string.Long),
                            LocaleController.getString("OnlyIfSilent", R.string.OnlyIfSilent)
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            String param = "vibrate_messages";
                            if (i == groupVibrateRow) {
                                param = "vibrate_group";
                            }
                            if (which == 0) {
                                editor.putInt(param, 2);
                            } else if (which == 1) {
                                editor.putInt(param, 0);
                            } else if (which == 2) {
                                editor.putInt(param, 1);
                            } else if (which == 3) {
                                editor.putInt(param, 3);
                            } else if (which == 4) {
                                editor.putInt(param, 4);
                            }
                            editor.commit();
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                } else if (i == messagePriorityRow || i == groupPriorityRow || i == inappPriorityRow ) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("NotificationsPriority", R.string.NotificationsPriority));
                    builder.setItems(new CharSequence[]{
                            LocaleController.getString("NotificationsPriorityDefault", R.string.NotificationsPriorityDefault),
                            LocaleController.getString("NotificationsPriorityHigh", R.string.NotificationsPriorityHigh),
                            LocaleController.getString("NotificationsPriorityMax", R.string.NotificationsPriorityMax)
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            if (i == messagePriorityRow) {
                                preferences.edit().putInt("priority_messages", which).commit();
                            } else if (i == groupPriorityRow) {
                                preferences.edit().putInt("priority_group", which).commit();
                            } else if (i == inappPriorityRow) {
                                preferences.edit().putInt("priority_inapp", which).commit();
                            }
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                } else if (i == repeatRow) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("RepeatNotifications", R.string.RepeatNotifications));
                    builder.setItems(new CharSequence[]{
                            LocaleController.getString("Disabled", R.string.Disabled),
                            LocaleController.formatPluralString("Minutes", 5),
                            LocaleController.formatPluralString("Minutes", 10),
                            LocaleController.formatPluralString("Minutes", 30),
                            LocaleController.formatPluralString("Hours", 1),
                            LocaleController.formatPluralString("Hours", 2),
                            LocaleController.formatPluralString("Hours", 4)
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            int minutes = 0;
                            if (which == 1) {
                                minutes = 5;
                            } else if (which == 2) {
                                minutes = 10;
                            } else if (which == 3) {
                                minutes = 30;
                            } else if (which == 4) {
                                minutes = 60;
                            } else if (which == 5) {
                                minutes = 60 * 2;
                            } else if (which == 6) {
                                minutes = 60 * 4;
                            }
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            preferences.edit().putInt("repeat_messages", minutes).commit();
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                }
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(!enabled);
                }
            }
        });

        return fragmentView;
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri ringtone = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            String name = null;
            if (ringtone != null) {
                Ringtone rng = RingtoneManager.getRingtone(getParentActivity(), ringtone);
                if (rng != null) {
                    if(ringtone.equals(Settings.System.DEFAULT_NOTIFICATION_URI)) {
                        name = LocaleController.getString("SoundDefault", R.string.SoundDefault);
                    } else {
                        name = rng.getTitle(getParentActivity());
                    }
                    rng.stop();
                }
            }

            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();

            if (requestCode == messageSoundRow) {
                if (name != null && ringtone != null) {
                    editor.putString("GlobalSound", name);
                    editor.putString("GlobalSoundPath", ringtone.toString());
                } else {
                    editor.putString("GlobalSound", "NoSound");
                    editor.putString("GlobalSoundPath", "NoSound");
                }
            } else if (requestCode == groupSoundRow) {
                if (name != null && ringtone != null) {
                    editor.putString("GroupSound", name);
                    editor.putString("GroupSoundPath", ringtone.toString());
                } else {
                    editor.putString("GroupSound", "NoSound");
                    editor.putString("GroupSoundPath", "NoSound");
                }
            }
            editor.commit();
            listView.invalidateViews();
        }
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.notificationsSettingsUpdated) {
            listView.invalidateViews();
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
            return !(i == messageSectionRow || i == groupSectionRow || i == inappSectionRow ||
                    i == otherSectionRow ||
                    i == groupSectionRow2 ||
                    i == inappSectionRow2 || i == otherSectionRow2 );
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
            if (type == TYPE_HEADER) {
                if (view == null) {
                    view = new HeaderCell(mContext);
                }
                if (i == messageSectionRow) {
                    ((HeaderCell) view).setText(LocaleController.getString("NormalMessagess", R.string.NormalMessages));
                } else if (i == groupSectionRow) {
                    ((HeaderCell) view).setText(LocaleController.getString("GroupMessages", R.string.GroupMessages));
                } else if (i == inappSectionRow) {
                    ((HeaderCell) view).setText(LocaleController.getString("InAppNotifications", R.string.InAppNotifications));
                } else if (i == otherSectionRow) {
                    ((HeaderCell) view).setText(LocaleController.getString("NotificationsOther", R.string.NotificationsOther));
                }
            } if (type == TYPE_CHECK_CELL) {
                if (view == null) {
                    view = new TextCheckCell(mContext);
                }
                TextCheckCell checkCell = (TextCheckCell) view;

                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                if (i == messageAlertRow) {
                    checkCell.setTextAndCheck(LocaleController.getString("Alert", R.string.Alert), preferences.getBoolean("EnableAll", true), true);
                } else if (i == groupAlertRow) {
                    checkCell.setTextAndCheck(LocaleController.getString("Alert", R.string.Alert), preferences.getBoolean("EnableGroup", true), true);
                } else if (i == messagePreviewRow) {
                    checkCell.setTextAndCheck(LocaleController.getString("MessagePreview", R.string.MessagePreview), preferences.getBoolean("EnablePreviewAll", true), true);
                /*} else if (i == groupPreviewRow) {
                    checkCell.setTextAndCheck(LocaleController.getString("MessagePreview", R.string.MessagePreview), preferences.getBoolean("EnablePreviewGroup", true), true);*/
                } else if (i == inappSoundRow) {
                    checkCell.setTextAndCheck(LocaleController.getString("Sound", R.string.Sound), preferences.getBoolean("EnableInAppSounds", true), true);
                } else if (i == inappVibrateRow) {
                    checkCell.setTextAndCheck(LocaleController.getString("Vibrate", R.string.Vibrate), preferences.getBoolean("EnableInAppVibrate", true), true);
                /*} else if (i == inappPreviewRow) {
                    checkCell.setTextAndCheck(LocaleController.getString("MessagePreview", R.string.MessagePreview), preferences.getBoolean("EnableInAppPreview", true), true);*/
                } else if (i == notificationsServiceRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString("NotificationsService", R.string.NotificationsService), LocaleController.getString("NotificationsServiceInfo", R.string.NotificationsServiceInfo), preferences.getBoolean("pushService", true), true, true);
                } else if (i == notificationsServiceConnectionRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString("NotificationsServiceConnection", R.string.NotificationsServiceConnection), LocaleController.getString("NotificationsServiceConnectionInfo", R.string.NotificationsServiceConnectionInfo), preferences.getBoolean("pushConnection", true), true, true);
                } else if (i == badgeNumberRow) {
                    checkCell.setTextAndCheck(LocaleController.getString("BadgeNumber", R.string.BadgeNumber), preferences.getBoolean("badgeNumber", true), true);
                } else if (i == inchatSoundRow) {
                    checkCell.setTextAndCheck(LocaleController.getString("InChatSound", R.string.InChatSound), preferences.getBoolean("EnableInChatSound", true), true);
                }
            } else if (type == TYPE_TEXTDETAIL) {
                if (view == null) {
                    view = new TextDetailSettingsCell(mContext);
                }
                TextDetailSettingsCell textCell = (TextDetailSettingsCell) view;

                if (i == resetNotificationsRow) {
                    textCell.setMultilineDetail(true);
                    textCell.setTextAndValue(LocaleController.getString("Reset", R.string.Reset), LocaleController.getString("ResetAllNotifications", R.string.ResetAllNotifications), false);
                }
            } else if (type == TYPE_TEXTSETTING) {
                if (view == null) {
                    view = new TextSettingsCell(mContext);
                }
                TextSettingsCell textCell = (TextSettingsCell) view;
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);

                if (i == messageSoundRow || i == groupSoundRow) {
                    String value = null;
                    if (i == messageSoundRow) {
                        value = preferences.getString("GlobalSound", LocaleController.getString("SoundDefault", R.string.SoundDefault));
                    } else if (i == groupSoundRow) {
                        value = preferences.getString("GroupSound", LocaleController.getString("SoundDefault", R.string.SoundDefault));
                    }
                    if (value.equals("NoSound")) {
                        value = LocaleController.getString("NoSound", R.string.NoSound);
                    }
                    textCell.setTextAndValue(LocaleController.getString("Sound", R.string.Sound), value, true);
                } else if (i == messageVibrateRow || i == groupVibrateRow) {
                    int value = 0;
                    if (i == messageVibrateRow) {
                        value = preferences.getInt("vibrate_messages", 0);
                    } else if (i == groupVibrateRow) {
                        value = preferences.getInt("vibrate_group", 0);
                    }
                    if (value == 0) {
                        textCell.setTextAndValue(LocaleController.getString("Vibrate", R.string.Vibrate), LocaleController.getString("VibrationDefault", R.string.VibrationDefault), true);
                    } else if (value == 1) {
                        textCell.setTextAndValue(LocaleController.getString("Vibrate", R.string.Vibrate), LocaleController.getString("Short", R.string.Short), true);
                    } else if (value == 2) {
                        textCell.setTextAndValue(LocaleController.getString("Vibrate", R.string.Vibrate), LocaleController.getString("Disabled", R.string.Disabled), true);
                    } else if (value == 3) {
                        textCell.setTextAndValue(LocaleController.getString("Vibrate", R.string.Vibrate), LocaleController.getString("Long", R.string.Long), true);
                    } else if (value == 4) {
                        textCell.setTextAndValue(LocaleController.getString("Vibrate", R.string.Vibrate), LocaleController.getString("OnlyIfSilent", R.string.OnlyIfSilent), true);
                    }
                } else if (i == messagePriorityRow || i == groupPriorityRow  || i == inappPriorityRow ) {
                    int value = 0;
                    if (i == messagePriorityRow) {
                        value = preferences.getInt("priority_messages", 1);
                    } else if (i == groupPriorityRow) {
                        value = preferences.getInt("priority_group", 1);
                    } else if (i == inappPriorityRow) {
                        value = preferences.getInt("priority_inapp", 0);
                    }
                    if (value == 0) {
                        textCell.setTextAndValue(LocaleController.getString("NotificationsPriority", R.string.NotificationsPriority), LocaleController.getString("NotificationsPriorityDefault", R.string.NotificationsPriorityDefault), false);
                    } else if (value == 1) {
                        textCell.setTextAndValue(LocaleController.getString("NotificationsPriority", R.string.NotificationsPriority), LocaleController.getString("NotificationsPriorityHigh", R.string.NotificationsPriorityHigh), false);
                    } else if (value == 2) {
                        textCell.setTextAndValue(LocaleController.getString("NotificationsPriority", R.string.NotificationsPriority), LocaleController.getString("NotificationsPriorityMax", R.string.NotificationsPriorityMax), false);
                    }
                } else if (i == repeatRow) {
                    int minutes = preferences.getInt("repeat_messages", 0);
                    String value;
                    if (minutes == 0) {
                        value = LocaleController.getString("Disabled", R.string.Disabled);
                    } else if (minutes < 60) {
                        value = LocaleController.formatPluralString("Minutes", minutes);
                    } else {
                        value = LocaleController.formatPluralString("Hours", minutes / 60);
                    }
                    textCell.setTextAndValue(LocaleController.getString("RepeatNotifications", R.string.RepeatNotifications), value, true);
                }
            } else if (type == TYPE_COLOR_CELL) {
                if (view == null) {
                    view = new TextColorCell(mContext);
                }

                TextColorCell textCell = (TextColorCell) view;

                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                if (i == messageLedRow) {
                    textCell.setTextAndColor(LocaleController.getString("LedColor", R.string.LedColor), preferences.getInt("MessagesLed", 0xff00ff00), true);
                } else if (i == groupLedRow) {
                    textCell.setTextAndColor(LocaleController.getString("LedColor", R.string.LedColor), preferences.getInt("GroupLed", 0xff00ff00), true);
                }
            } else if (type == TYPE_SHADOW) {
                if (view == null) {
                    view = new ShadowSectionCell(mContext);
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == messageSectionRow || i == groupSectionRow || i == inappSectionRow ||
                    i == otherSectionRow ) {
                return TYPE_HEADER;
            } else if (i == messageAlertRow || i == messagePreviewRow || i == groupAlertRow ||
                    /*i == groupPreviewRow ||*/ i == inappSoundRow || i == inappVibrateRow ||
                    /*i == inappPreviewRow ||*/
                    i == notificationsServiceRow || i == badgeNumberRow ||
                    i == inchatSoundRow || i == notificationsServiceConnectionRow) {
                return TYPE_CHECK_CELL;
            } else if (i == messageLedRow || i == groupLedRow) {
                return TYPE_COLOR_CELL;
            } else if ( i == groupSectionRow2 ||
                    i == inappSectionRow2 || i == otherSectionRow2 ) {
                return TYPE_SHADOW;
            } else if( i==resetNotificationsRow ){
                return TYPE_TEXTDETAIL;
            } else {
                return TYPE_TEXTSETTING;
            }
        }

        @Override
        public int getViewTypeCount() {
            return TYPE_COUNT;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
