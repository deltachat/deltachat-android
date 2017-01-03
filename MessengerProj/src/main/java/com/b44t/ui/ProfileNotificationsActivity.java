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
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.MessagesController;
import com.b44t.messenger.MrChat;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.NotificationsController;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.FileLog;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.NotificationCenter;
import com.b44t.messenger.R;
import com.b44t.messenger.ConnectionsManager;
import com.b44t.messenger.TLRPC;
import com.b44t.ui.ActionBar.Theme;
import com.b44t.ui.Adapters.BaseFragmentAdapter;
import com.b44t.ui.Cells.HeaderCell;
import com.b44t.ui.Cells.TextColorCell;
import com.b44t.ui.Cells.TextSettingsCell;
import com.b44t.ui.Cells.TextDetailSettingsCell;
import com.b44t.ui.ActionBar.ActionBar;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.Components.ColorPickerView;
import com.b44t.ui.Components.LayoutHelper;
import com.b44t.ui.Components.NumberPicker;

public class ProfileNotificationsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private ListView listView;
    private int dialog_id;

    private int headerRow;
    private int settingsNotificationsRow;
    private int settingsVibrateRow;
    private int settingsSoundRow;
    private int settingsPriorityRow;
    private int settingsLedRow;
    private int smartRow;
    private int rowCount = 0;

    private final int TYPE_HEADER       = 0;
    private final int TYPE_TEXTSETTINGS = 1;
    private final int TYPE_TEXTDETAILS  = 2;
    private final int TYPE_COLOR_CELL   = 3;
    private final int TYPE_COUNT        = 4;

    private MrChat m_mrChat;

    public ProfileNotificationsActivity(Bundle args) {
        super(args);
        dialog_id = args.getInt("chat_id");
        m_mrChat = MrMailbox.getChat(dialog_id);
    }

    @Override
    public boolean onFragmentCreate() {
        boolean isGroupChat = m_mrChat.getType()== MrChat.MR_CHAT_GROUP;

        headerRow = rowCount++;
        settingsNotificationsRow = rowCount++;
        settingsSoundRow = rowCount++;
        settingsVibrateRow = rowCount++;
        settingsLedRow = rowCount++;
        settingsPriorityRow = Build.VERSION.SDK_INT >= 21? rowCount++ : -1;

        if (isGroupChat) {
            smartRow = rowCount++;
        } else {
            smartRow = -1;
        }

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
        AndroidUtilities.setListViewEdgeEffectColor(listView, Theme.ACTION_BAR_COLOR);
        frameLayout.addView(listView);
        final FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        listView.setLayoutParams(layoutParams);
        listView.setAdapter(new ListAdapter(context));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                if (i == settingsVibrateRow) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("Vibrate", R.string.Vibrate));
                    builder.setItems(new CharSequence[]{
                            LocaleController.getString("Disabled", R.string.Disabled),
                            LocaleController.getString("Default", R.string.Default),
                            LocaleController.getString("SystemDefault", R.string.SystemDefault),
                            LocaleController.getString("Short", R.string.Short),
                            LocaleController.getString("Long", R.string.Long)
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if( dialog_id != 0 ) {
                                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                if (which == 0) {
                                    editor.putInt("vibrate_" + dialog_id, 2);
                                } else if (which == 1) {
                                    editor.putInt("vibrate_" + dialog_id, 0);
                                } else if (which == 2) {
                                    editor.putInt("vibrate_" + dialog_id, 4);
                                } else if (which == 3) {
                                    editor.putInt("vibrate_" + dialog_id, 1);
                                } else if (which == 4) {
                                    editor.putInt("vibrate_" + dialog_id, 3);
                                }
                                editor.commit();
                            }
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                } else if (i == settingsNotificationsRow) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setItems(new CharSequence[]{
                            LocaleController.getString("Default", R.string.Default),
                            LocaleController.getString("Enabled", R.string.Enabled),
                            LocaleController.getString("Disabled", R.string.Disabled)
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int which) {
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt("notify2_" + dialog_id, which);
                            /*if (which == 2) {
                                NotificationsController.getInstance().removeNotificationsForDialog(dialog_id);
                            }*/
                            //MessagesStorage.getInstance().setDialogFlags(dialog_id, which == 2 ? 1 : 0);
                            editor.commit();
                            TLRPC.TL_dialog dialog = MessagesController.getInstance().dialogs_dict.get(dialog_id);
                            if (dialog != null) {
                                dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
                                if (which == 2) {
                                    dialog.notify_settings.mute_until = Integer.MAX_VALUE;
                                }
                            }
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                            NotificationsController.updateServerNotificationsSettings(dialog_id);
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                } else if (i == settingsSoundRow) {
                    try {
                        Intent tmpIntent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                        tmpIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                        tmpIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                        tmpIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                        Uri currentSound = null;

                        String defaultPath = null;
                        Uri defaultUri = Settings.System.DEFAULT_NOTIFICATION_URI;
                        if (defaultUri != null) {
                            defaultPath = defaultUri.getPath();
                        }

                        String path = preferences.getString("sound_path_" + dialog_id, defaultPath);
                        if (path != null && !path.equals("NoSound")) {
                            if (path.equals(defaultPath)) {
                                currentSound = defaultUri;
                            } else {
                                currentSound = Uri.parse(path);
                            }
                        }

                        tmpIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentSound);
                        startActivityForResult(tmpIntent, 12);
                    } catch (Exception e) {
                        FileLog.e("messenger", e);
                    }
                } else if (i == settingsLedRow) {
                    if (getParentActivity() == null) {
                        return;
                    }

                    LinearLayout linearLayout = new LinearLayout(getParentActivity());
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    final ColorPickerView colorPickerView = new ColorPickerView(getParentActivity());
                    linearLayout.addView(colorPickerView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                    if (preferences.contains("color_" + dialog_id)) {
                        colorPickerView.setOldCenterColor(preferences.getInt("color_" + dialog_id, 0xff00ff00));
                    } else {
                        if ((int) dialog_id < 0) {
                            colorPickerView.setOldCenterColor(preferences.getInt("GroupLed", 0xff00ff00));
                        } else {
                            colorPickerView.setOldCenterColor(preferences.getInt("MessagesLed", 0xff00ff00));
                        }
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("LedColor", R.string.LedColor));
                    builder.setView(linearLayout);
                    builder.setPositiveButton(LocaleController.getString("Set", R.string.Set), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int which) {
                            final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt("color_" + dialog_id, colorPickerView.getColor());
                            editor.commit();
                            listView.invalidateViews();
                        }
                    });
                    builder.setNeutralButton(LocaleController.getString("Disabled", R.string.Disabled), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt("color_" + dialog_id, 0);
                            editor.commit();
                            listView.invalidateViews();
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Default", R.string.Default), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.remove("color_" + dialog_id);
                            editor.commit();
                            listView.invalidateViews();
                        }
                    });
                    showDialog(builder.create());
                } else if (i == settingsPriorityRow) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("NotificationsPriority", R.string.NotificationsPriority));
                    builder.setItems(new CharSequence[]{
                            LocaleController.getString("Default", R.string.Default),
                            LocaleController.getString("NotificationsPriorityDefault", R.string.NotificationsPriorityDefault),
                            LocaleController.getString("NotificationsPriorityHigh", R.string.NotificationsPriorityHigh),
                            LocaleController.getString("NotificationsPriorityMax", R.string.NotificationsPriorityMax)
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 0) {
                                which = 3;
                            } else {
                                which--;
                            }
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            preferences.edit().putInt("priority_" + dialog_id, which).commit();
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                } else if (i == smartRow) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                    int notifyMaxCount = preferences.getInt("smart_max_count_" + dialog_id, 0);
                    int notifyDelay = preferences.getInt("smart_delay_" + dialog_id, 3 * 60);
                    if (notifyMaxCount == 0) {
                        notifyMaxCount = 2;
                    }

                    LinearLayout linearLayout = new LinearLayout(getParentActivity());
                    linearLayout.setOrientation(LinearLayout.VERTICAL);

                    LinearLayout linearLayout2 = new LinearLayout(getParentActivity());
                    linearLayout2.setOrientation(LinearLayout.HORIZONTAL);
                    linearLayout.addView(linearLayout2);
                    LinearLayout.LayoutParams layoutParams1 = (LinearLayout.LayoutParams) linearLayout2.getLayoutParams();
                    layoutParams1.width = LayoutHelper.WRAP_CONTENT;
                    layoutParams1.height = LayoutHelper.WRAP_CONTENT;
                    layoutParams1.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
                    linearLayout2.setLayoutParams(layoutParams1);

                    TextView textView = new TextView(getParentActivity());
                    textView.setText(LocaleController.getString("SmartNotificationsSoundAtMost", R.string.SmartNotificationsSoundAtMost));
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                    linearLayout2.addView(textView);
                    layoutParams1 = (LinearLayout.LayoutParams) textView.getLayoutParams();
                    layoutParams1.width = LayoutHelper.WRAP_CONTENT;
                    layoutParams1.height = LayoutHelper.WRAP_CONTENT;
                    layoutParams1.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
                    textView.setLayoutParams(layoutParams1);

                    final NumberPicker numberPickerTimes = new NumberPicker(getParentActivity());
                    numberPickerTimes.setMinValue(1);
                    numberPickerTimes.setMaxValue(10);
                    numberPickerTimes.setValue(notifyMaxCount);
                    numberPickerTimes.setWrapSelectorWheel(false);
                    linearLayout2.addView(numberPickerTimes);
                    layoutParams1 = (LinearLayout.LayoutParams) numberPickerTimes.getLayoutParams();
                    layoutParams1.width = AndroidUtilities.dp(50);
                    numberPickerTimes.setLayoutParams(layoutParams1);

                    textView = new TextView(getParentActivity());
                    textView.setText(LocaleController.getString("SmartNotificationsTimes", R.string.SmartNotificationsTimes));
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                    linearLayout2.addView(textView);
                    layoutParams1 = (LinearLayout.LayoutParams) textView.getLayoutParams();
                    layoutParams1.width = LayoutHelper.WRAP_CONTENT;
                    layoutParams1.height = LayoutHelper.WRAP_CONTENT;
                    layoutParams1.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
                    textView.setLayoutParams(layoutParams1);

                    linearLayout2 = new LinearLayout(getParentActivity());
                    linearLayout2.setOrientation(LinearLayout.HORIZONTAL);
                    linearLayout.addView(linearLayout2);
                    layoutParams1 = (LinearLayout.LayoutParams) linearLayout2.getLayoutParams();
                    layoutParams1.width = LayoutHelper.WRAP_CONTENT;
                    layoutParams1.height = LayoutHelper.WRAP_CONTENT;
                    layoutParams1.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
                    linearLayout2.setLayoutParams(layoutParams1);

                    textView = new TextView(getParentActivity());
                    textView.setText(LocaleController.getString("SmartNotificationsWithin", R.string.SmartNotificationsWithin));
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                    linearLayout2.addView(textView);
                    layoutParams1 = (LinearLayout.LayoutParams) textView.getLayoutParams();
                    layoutParams1.width = LayoutHelper.WRAP_CONTENT;
                    layoutParams1.height = LayoutHelper.WRAP_CONTENT;
                    layoutParams1.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
                    textView.setLayoutParams(layoutParams1);

                    final NumberPicker numberPickerMinutes = new NumberPicker(getParentActivity());
                    numberPickerMinutes.setMinValue(1);
                    numberPickerMinutes.setMaxValue(10);
                    numberPickerMinutes.setValue(notifyDelay / 60);
                    numberPickerMinutes.setWrapSelectorWheel(false);
                    linearLayout2.addView(numberPickerMinutes);
                    layoutParams1 = (LinearLayout.LayoutParams) numberPickerMinutes.getLayoutParams();
                    layoutParams1.width = AndroidUtilities.dp(50);
                    numberPickerMinutes.setLayoutParams(layoutParams1);

                    textView = new TextView(getParentActivity());
                    textView.setText(LocaleController.getString("SmartNotificationsMinutes", R.string.SmartNotificationsMinutes));
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                    linearLayout2.addView(textView);
                    layoutParams1 = (LinearLayout.LayoutParams) textView.getLayoutParams();
                    layoutParams1.width = LayoutHelper.WRAP_CONTENT;
                    layoutParams1.height = LayoutHelper.WRAP_CONTENT;
                    layoutParams1.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
                    textView.setLayoutParams(layoutParams1);

                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("SmartNotifications", R.string.SmartNotifications));
                    builder.setView(linearLayout);
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            preferences.edit().putInt("smart_max_count_" + dialog_id, numberPickerTimes.getValue()).commit();
                            preferences.edit().putInt("smart_delay_" + dialog_id, numberPickerMinutes.getValue() * 60).commit();
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Disabled", R.string.Disabled), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            preferences.edit().putInt("smart_max_count_" + dialog_id, 0).commit();
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

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                return;
            }
            Uri ringtone = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            String name = null;
            if (ringtone != null) {
                Ringtone rng = RingtoneManager.getRingtone(ApplicationLoader.applicationContext, ringtone);
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

            if (requestCode == 12) {
                if (name != null) {
                    editor.putString("sound_" + dialog_id, name);
                    editor.putString("sound_path_" + dialog_id, ringtone.toString());
                } else {
                    editor.putString("sound_" + dialog_id, "NoSound");
                    editor.putString("sound_path_" + dialog_id, "NoSound");
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
            return true;
        }

        @Override
        public boolean isEnabled(int i) {
            return !(i==headerRow);
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
            if( type == TYPE_HEADER ) {
                if (view == null) {
                    view = new HeaderCell(mContext);
                }
                ((HeaderCell) view).setText(m_mrChat.getName());
            }
            else if (type == TYPE_TEXTSETTINGS) {
                if (view == null) {
                    view = new TextSettingsCell(mContext);
                }
                TextSettingsCell textCell = (TextSettingsCell) view;
                SharedPreferences preferences = mContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);

                if (i == settingsVibrateRow) {
                    int value = preferences.getInt("vibrate_" + dialog_id, 0);
                    if (value == 0) {
                        textCell.setTextAndValue(LocaleController.getString("Vibrate", R.string.Vibrate), LocaleController.getString("Default", R.string.Default), true);
                    } else if (value == 1) {
                        textCell.setTextAndValue(LocaleController.getString("Vibrate", R.string.Vibrate), LocaleController.getString("Short", R.string.Short), true);
                    } else if (value == 2) {
                        textCell.setTextAndValue(LocaleController.getString("Vibrate", R.string.Vibrate), LocaleController.getString("Disabled", R.string.Disabled), true);
                    } else if (value == 3) {
                        textCell.setTextAndValue(LocaleController.getString("Vibrate", R.string.Vibrate), LocaleController.getString("Long", R.string.Long), true);
                    } else if (value == 4) {
                        textCell.setTextAndValue(LocaleController.getString("Vibrate", R.string.Vibrate), LocaleController.getString("SystemDefault", R.string.SystemDefault), true);
                    }
                } else if (i == settingsNotificationsRow) {
                    int value = preferences.getInt("notify2_" + dialog_id, 0);
                    if (value == 0) {
                        textCell.setTextAndValue(LocaleController.getString("Notifications", R.string.Notifications), LocaleController.getString("Default", R.string.Default), true);
                    } else if (value == 1) {
                        textCell.setTextAndValue(LocaleController.getString("Notifications", R.string.Notifications), LocaleController.getString("Enabled", R.string.Enabled), true);
                    } else if (value == 2) {
                        textCell.setTextAndValue(LocaleController.getString("Notifications", R.string.Notifications), LocaleController.getString("Disabled", R.string.Disabled), true);
                    } else if (value == 3) {
                        int delta = preferences.getInt("notifyuntil_" + dialog_id, 0) - ConnectionsManager.getInstance().getCurrentTime();
                        String val;
                        if (delta <= 0) {
                            val = LocaleController.getString("Enabled", R.string.Enabled);
                        } else if (delta < 60 * 60) {
                            val = LocaleController.formatString("WillUnmuteIn", R.string.WillUnmuteIn, LocaleController.formatPluralString("Minutes", delta / 60));
                        } else if (delta < 60 * 60 * 24) {
                            val = LocaleController.formatString("WillUnmuteIn", R.string.WillUnmuteIn, LocaleController.formatPluralString("Hours", (int) Math.ceil(delta / 60.0f / 60)));
                        } else if (delta < 60 * 60 * 24 * 365) {
                            val = LocaleController.formatString("WillUnmuteIn", R.string.WillUnmuteIn, LocaleController.formatPluralString("Days", (int) Math.ceil(delta / 60.0f / 60 / 24)));
                        } else {
                            val = null;
                        }
                        if (val != null) {
                            textCell.setTextAndValue(LocaleController.getString("Notifications", R.string.Notifications), val, true);
                        } else {
                            textCell.setTextAndValue(LocaleController.getString("Notifications", R.string.Notifications), LocaleController.getString("Disabled", R.string.Disabled), true);
                        }
                    }
                } else if (i == settingsSoundRow) {
                    String value = preferences.getString("sound_" + dialog_id, LocaleController.getString("SoundDefault", R.string.SoundDefault));
                    if (value.equals("NoSound")) {
                        value = LocaleController.getString("NoSound", R.string.NoSound);
                    }
                    textCell.setTextAndValue(LocaleController.getString("Sound", R.string.Sound), value, true);
                } else if (i == settingsPriorityRow) {
                    int value = preferences.getInt("priority_" + dialog_id, 3);
                    if (value == 0) {
                        textCell.setTextAndValue(LocaleController.getString("NotificationsPriority", R.string.NotificationsPriority), LocaleController.getString("NotificationsPriorityDefault", R.string.NotificationsPriorityDefault), true);
                    } else if (value == 1) {
                        textCell.setTextAndValue(LocaleController.getString("NotificationsPriority", R.string.NotificationsPriority), LocaleController.getString("NotificationsPriorityHigh", R.string.NotificationsPriorityHigh), true);
                    } else if (value == 2) {
                        textCell.setTextAndValue(LocaleController.getString("NotificationsPriority", R.string.NotificationsPriority), LocaleController.getString("NotificationsPriorityMax", R.string.NotificationsPriorityMax), true);
                    } else if (value == 3) {
                        textCell.setTextAndValue(LocaleController.getString("NotificationsPriority", R.string.NotificationsPriority), LocaleController.getString("Default", R.string.Default), true);
                    }
                }
            } else if (type == TYPE_TEXTDETAILS) {
                if (view == null) {
                    view = new TextDetailSettingsCell(mContext);
                }
                TextDetailSettingsCell textCell = (TextDetailSettingsCell) view;
                SharedPreferences preferences = mContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);

                if (i == smartRow) {
                    int notifyMaxCount = preferences.getInt("smart_max_count_" + dialog_id, 0);
                    int notifyDelay = preferences.getInt("smart_delay_" + dialog_id, 3 * 60);
                    if (notifyMaxCount == 0) {
                        textCell.setTextAndValue(LocaleController.getString("SmartNotifications", R.string.SmartNotifications), LocaleController.getString("Disabled", R.string.Disabled), true);
                    } else {
                        String times = LocaleController.formatPluralString("Times", notifyMaxCount);
                        String minutes = LocaleController.formatPluralString("Minutes", notifyDelay / 60);
                        textCell.setTextAndValue(LocaleController.getString("SmartNotifications", R.string.SmartNotifications), LocaleController.formatString("SmartNotificationsInfo", R.string.SmartNotificationsInfo, times, minutes), true);
                    }
                }
            } else if (type == TYPE_COLOR_CELL) {
                if (view == null) {
                    view = new TextColorCell(mContext);
                }

                TextColorCell textCell = (TextColorCell) view;

                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);

                if (preferences.contains("color_" + dialog_id)) {
                    textCell.setTextAndColor(LocaleController.getString("LedColor", R.string.LedColor), preferences.getInt("color_" + dialog_id, 0xff00ff00), true);
                } else {
                    if ((int)dialog_id < 0) {
                        textCell.setTextAndColor(LocaleController.getString("LedColor", R.string.LedColor), preferences.getInt("GroupLed", 0xff00ff00), true);
                    } else {
                        textCell.setTextAndColor(LocaleController.getString("LedColor", R.string.LedColor), preferences.getInt("MessagesLed", 0xff00ff00), true);
                    }
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if( i==headerRow ) {
                return TYPE_HEADER;
            }
            else if (i == settingsLedRow) {
                return TYPE_COLOR_CELL;
            }
            else if( i==smartRow ) {
                return TYPE_TEXTDETAILS;
            }
            return TYPE_TEXTSETTINGS;
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
