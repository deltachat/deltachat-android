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
import com.b44t.messenger.MrChat;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.NotificationCenter;
import com.b44t.messenger.NotificationsController;
import com.b44t.messenger.R;
import com.b44t.ui.Adapters.BaseFragmentAdapter;
import com.b44t.ui.Cells.HeaderCell;
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
    private final int TYPE_COUNT        = 3;

    private MrChat m_mrChat;
    private boolean m_isGroupChat;

    public ProfileNotificationsActivity(Bundle args) {
        super(args);
        dialog_id = args.getInt("chat_id");
        m_mrChat = MrMailbox.getChat(dialog_id);
        m_isGroupChat = m_mrChat.getType()== MrChat.MR_CHAT_GROUP;
    }

    @Override
    public boolean onFragmentCreate() {

        headerRow = rowCount++;
        settingsNotificationsRow = rowCount++;
        settingsSoundRow = rowCount++;
        settingsVibrateRow = rowCount++;
        settingsLedRow = rowCount++;
        settingsPriorityRow = Build.VERSION.SDK_INT >= 21? rowCount++ : -1;

        if (m_isGroupChat) {
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
    public View createView(final Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(String.format(context.getString(R.string.SettingsFor), m_mrChat.getName()));
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
                    builder.setTitle(context.getString(R.string.Vibrate));
                    builder.setItems(new CharSequence[]{
                            context.getString(R.string.Disabled),
                            context.getString(R.string.Default),
                            context.getString(R.string.Short),
                            context.getString(R.string.Long)
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if( dialog_id != 0 ) {
                                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                if (which == 0) {
                                    editor.putInt("vibrate_" + dialog_id, 2); // off
                                } else if (which == 1) {
                                    editor.remove("vibrate_" + dialog_id); // delta-chat-default
                                } else if (which == 2) {
                                    editor.putInt("vibrate_" + dialog_id, 1); // short
                                } else if (which == 3) {
                                    editor.putInt("vibrate_" + dialog_id, 3); // long
                                }
                                editor.apply();
                            }
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    builder.setNegativeButton(context.getString(R.string.Cancel), null);
                    showDialog(builder.create());
                } else if (i == settingsNotificationsRow) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setItems(new CharSequence[]{
                            context.getString(R.string.Default),
                            context.getString(R.string.Enabled),
                            context.getString(R.string.Disabled)
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
                            editor.apply();
                            /*TLRPC.TL_dialog dialog = MessagesController.getInstance().dialogs_dict.get(dialog_id);
                            if (dialog != null) {
                                dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
                                if (which == 2) {
                                    dialog.notify_settings.mute_until = Integer.MAX_VALUE;
                                }
                            }*/
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.notificationsSettingsUpdated);
                        }
                    });
                    builder.setNegativeButton(context.getString(R.string.Cancel), null);
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
                        startActivityForResult(tmpIntent, RC12_PROFILE_RINGTONE_PICKER);
                    } catch (Exception e) {

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
                        colorPickerView.setColor(preferences.getInt("color_" + dialog_id, NotificationsController.DEF_LED_COLOR));
                    } else {
                        colorPickerView.setColor(preferences.getInt(m_isGroupChat? "GroupLed" : "MessagesLed", NotificationsController.DEF_LED_COLOR));
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(context.getString(R.string.LedColor));
                    builder.setView(linearLayout);
                    builder.setPositiveButton(context.getString(R.string.Set), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int which) {
                            final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt("color_" + dialog_id, colorPickerView.getColor()); // use selected color
                            editor.apply();
                            listView.invalidateViews();
                        }
                    });
                    builder.setNeutralButton(context.getString(R.string.Disabled), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt("color_" + dialog_id, 0/*0=off*/);
                            editor.apply();
                            listView.invalidateViews();
                        }
                    });
                    builder.setNegativeButton(context.getString(R.string.Default), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.remove("color_" + dialog_id); // use delta-chat-default
                            editor.apply();
                            listView.invalidateViews();
                        }
                    });
                    showDialog(builder.create());
                } else if (i == settingsPriorityRow) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(context.getString(R.string.NotificationsPriority));
                    builder.setItems(new CharSequence[]{
                            context.getString(R.string.Default),
                            context.getString(R.string.NotificationsPriorityDefault),
                            context.getString(R.string.NotificationsPriorityHigh),
                            context.getString(R.string.NotificationsPriorityMax)
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 0) {
                                which = 3;
                            } else {
                                which--;
                            }
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            preferences.edit().putInt("priority_" + dialog_id, which).apply();
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    builder.setNegativeButton(context.getString(R.string.Cancel), null);
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
                    textView.setText(context.getString(R.string.SmartNotificationsSoundAtMost));
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
                    textView.setText(context.getString(R.string.SmartNotificationsTimes));
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
                    textView.setText(context.getString(R.string.SmartNotificationsWithin));
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
                    textView.setText(context.getString(R.string.SmartNotificationsMinutes));
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                    linearLayout2.addView(textView);
                    layoutParams1 = (LinearLayout.LayoutParams) textView.getLayoutParams();
                    layoutParams1.width = LayoutHelper.WRAP_CONTENT;
                    layoutParams1.height = LayoutHelper.WRAP_CONTENT;
                    layoutParams1.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
                    textView.setLayoutParams(layoutParams1);

                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(context.getString(R.string.SmartNotifications));
                    builder.setView(linearLayout);
                    builder.setPositiveButton(context.getString(R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            preferences.edit().putInt("smart_max_count_" + dialog_id, numberPickerTimes.getValue()).apply();
                            preferences.edit().putInt("smart_delay_" + dialog_id, numberPickerMinutes.getValue() * 60).apply();
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    builder.setNegativeButton(context.getString(R.string.Disabled), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            preferences.edit().putInt("smart_max_count_" + dialog_id, 0).apply();
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
                        name = "use-delta-chat-default";
                    } else {
                        name = rng.getTitle(getParentActivity());
                    }
                    rng.stop();
                }
            }

            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();

            if (requestCode == RC12_PROFILE_RINGTONE_PICKER) {
                if( name==null ) {
                    editor.putString("sound_" + dialog_id, "NoSound");
                    editor.putString("sound_path_" + dialog_id, "NoSound");
                }
                else if( name.equals("use-delta-chat-default") ) {
                    editor.remove("sound_" + dialog_id); // use the Delta Chat standard sound, not the system standard sound!
                    editor.remove("sound_path_" + dialog_id);
                }
                else {
                    editor.putString("sound_" + dialog_id, name);
                    editor.putString("sound_path_" + dialog_id, ringtone.toString());
                }
            }
            editor.apply();
            listView.invalidateViews();
        }
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.notificationsSettingsUpdated) {
            listView.invalidateViews();
        }
    }

    static String muteForString(int seconds)
    {
        String val;
        if (seconds < 60 * 60) {
            val = ApplicationLoader.applicationContext.getResources().getQuantityString(R.plurals.Minutes, seconds / 60, seconds / 60);
        } else if (seconds < 24 * 60 * 60) {
            val = ApplicationLoader.applicationContext.getResources().getQuantityString(R.plurals.Hours, (int) Math.ceil(seconds / 60.0f / 60), (int) Math.ceil(seconds / 60.0f / 60));
        } else {
            val = ApplicationLoader.applicationContext.getResources().getQuantityString(R.plurals.Days, (int) Math.ceil(seconds / 60.0f / 60 / 24), (int) Math.ceil(seconds / 60.0f / 60 / 24));
        }
        return String.format(ApplicationLoader.applicationContext.getString(R.string.MuteFor), val);
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
                ((HeaderCell) view).setText(mContext.getString(R.string.NotificationsAndSounds));
            }
            else if (type == TYPE_TEXTSETTINGS) {
                if (view == null) {
                    view = new TextSettingsCell(mContext);
                }
                TextSettingsCell textCell = (TextSettingsCell) view;
                SharedPreferences preferences = mContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                if (i == settingsVibrateRow) {
                    int value = preferences.getInt("vibrate_" + dialog_id, 0);
                    if (value == 1) {
                        textCell.setTextAndValue(mContext.getString(R.string.Vibrate), mContext.getString(R.string.Short), true);
                    } else if (value == 2) {
                        textCell.setTextAndValue(mContext.getString(R.string.Vibrate), mContext.getString(R.string.Disabled), true);
                    } else if (value == 3) {
                        textCell.setTextAndValue(mContext.getString(R.string.Vibrate), mContext.getString(R.string.Long), true);
                    } else {
                        textCell.setTextAndValue(mContext.getString(R.string.Vibrate), mContext.getString(R.string.Default), true);
                    }
                } else if (i == settingsNotificationsRow) {
                    int value = preferences.getInt("notify2_" + dialog_id, 0);
                    if (value == 0) {
                        textCell.setTextAndValue(mContext.getString(R.string.Notifications), mContext.getString(R.string.Default), true);
                    } else if (value == 1) {
                        textCell.setTextAndValue(mContext.getString(R.string.Notifications), mContext.getString(R.string.Enabled), true);
                    } else if (value == 2) {
                        textCell.setTextAndValue(mContext.getString(R.string.Notifications), mContext.getString(R.string.Disabled), true);
                    } else if (value == 3) {
                        int delta = preferences.getInt("notifyuntil_" + dialog_id, 0) - MrMailbox.getCurrentTime();
                        String val;
                        if (delta <= 0) {
                            val = mContext.getString(R.string.Enabled);
                        } else if (delta < 60 * 60 * 24 * 365) {
                            val = muteForString(delta);
                        } else {
                            val = mContext.getString(R.string.Disabled);
                        }
                        textCell.setTextAndValue(mContext.getString(R.string.Notifications), val, true);
                    }
                } else if (i == settingsSoundRow) {
                    String value = preferences.getString("sound_" + dialog_id, mContext.getString(R.string.Default));
                    if (value.equals("NoSound")) {
                        value = mContext.getString(R.string.Disabled);
                    }
                    textCell.setTextAndValue(mContext.getString(R.string.Sound), value, true);
                } else if (i == settingsPriorityRow) {
                    int value = preferences.getInt("priority_" + dialog_id, 3);
                    if (value == 0) {
                        textCell.setTextAndValue(mContext.getString(R.string.NotificationsPriority), mContext.getString(R.string.NotificationsPriorityDefault), true);
                    } else if (value == 1) {
                        textCell.setTextAndValue(mContext.getString(R.string.NotificationsPriority), mContext.getString(R.string.NotificationsPriorityHigh), true);
                    } else if (value == 2) {
                        textCell.setTextAndValue(mContext.getString(R.string.NotificationsPriority), mContext.getString(R.string.NotificationsPriorityMax), true);
                    } else if (value == 3) {
                        textCell.setTextAndValue(mContext.getString(R.string.NotificationsPriority), mContext.getString(R.string.Default), true);
                    }
                }
                else if( i== settingsLedRow )
                {
                    if (preferences.contains("color_" + dialog_id)) {
                        int color = preferences.getInt("color_" + dialog_id, NotificationsController.DEF_LED_COLOR);
                        if( color == 0 ) {
                            textCell.setTextAndValue(mContext.getString(R.string.LedColor), mContext.getString(R.string.Disabled), true);
                        }
                        else {
                            textCell.setTextAndColor(mContext.getString(R.string.LedColor), color, true);
                        }
                    } else {
                        textCell.setTextAndValue(mContext.getString(R.string.LedColor), mContext.getString(R.string.Default), true);
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
                        textCell.setTextAndValue(mContext.getString(R.string.SmartNotifications), mContext.getString(R.string.Disabled), true);
                    } else {
                        String minutes = mContext.getResources().getQuantityString(R.plurals.Minutes, notifyDelay / 60, notifyDelay / 60);
                        String value = mContext.getResources().getQuantityString(R.plurals.MaxNotifications, notifyMaxCount, notifyMaxCount, minutes);
                        textCell.setTextAndValue(mContext.getString(R.string.SmartNotifications), value, true);
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
