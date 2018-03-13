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


package com.b44t.messenger;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Vibrator;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.b44t.messenger.ActionBar.ActionBar;
import com.b44t.messenger.ActionBar.ActionBarMenu;
import com.b44t.messenger.ActionBar.ActionBarMenuItem;
import com.b44t.messenger.ActionBar.BaseFragment;
import com.b44t.messenger.Components.BaseFragmentAdapter;
import com.b44t.messenger.Cells.TextCheckCell;
import com.b44t.messenger.Cells.TextInfoCell;
import com.b44t.messenger.Cells.TextSettingsCell;
import com.b44t.messenger.Components.LayoutHelper;

public class PasscodeActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private ListAdapter listAdapter;
    private ListView listView;
    private TextView titleTextView;
    private EditText passwordEditText;
    private TextView dropDown;
    private ActionBarMenuItem dropDownContainer;

    public final static int SCREEN0_SETTINGS = 0;
    public final static int SCREEN1_ENTER_CODE1 = 1;
    public final static int SCREEN2_ENTER_CODE2 = 2;
    private int screen;

    private int currentPasswordType = 0;
    private int passcodeSetStep = 0;
    private String firstPassword;

    private int passcodeOnOffRow;
    private int changePasscodeRow;
    private int passcodeDetailRow;
    private int fingerprintRow;
    private int autoLockRow;
    private int autoLockDetailRow;
    private int rowCount;

    private final static int done_button = 1;
    private final static int pin_item = 2;
    private final static int password_item = 3;

    public PasscodeActivity(int screen) {
        super();
        this.screen = screen;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateRows();
        if (screen == SCREEN0_SETTINGS ) {
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.didSetPasscode);
        }
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (screen == SCREEN0_SETTINGS ) {
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didSetPasscode);
        }
    }

    @Override
    public View createView(final Context context) {
        actionBar.setBackButtonImage(screen == SCREEN0_SETTINGS? R.drawable.ic_ab_back : R.drawable.ic_close_white);
        actionBar.setAllowOverlayTitle(false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == done_button) {
                    if (passcodeSetStep == 0) {
                        processNext();
                    } else if (passcodeSetStep == 1) {
                        processDone();
                    }
                } else if (id == pin_item) {
                    currentPasswordType = 0;
                    updateDropDownTextView();
                } else if (id == password_item) {
                    currentPasswordType = 1;
                    updateDropDownTextView();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        if (screen != SCREEN0_SETTINGS ) {
            ActionBarMenu menu = actionBar.createMenu();
            menu.addItemWithWidth(done_button, R.drawable.ic_done, AndroidUtilities.dp(56));

            titleTextView = new TextView(context);
            titleTextView.setTextColor(0xff757575);
            if (screen == SCREEN1_ENTER_CODE1) {
                if (UserConfig.passcodeHash.length() != 0) {
                    titleTextView.setText(context.getString(R.string.EnterNewPasscode));
                } else {
                    titleTextView.setText(context.getString(R.string.EnterNewFirstPasscode));
                }
            } else {
                titleTextView.setText(context.getString(R.string.EnterCurrentPasscode));
            }
            titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            titleTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            frameLayout.addView(titleTextView);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) titleTextView.getLayoutParams();
            layoutParams.width = LayoutHelper.WRAP_CONTENT;
            layoutParams.height = LayoutHelper.WRAP_CONTENT;
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
            layoutParams.topMargin = AndroidUtilities.dp(38);
            titleTextView.setLayoutParams(layoutParams);

            passwordEditText = new EditText(context);
            passwordEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            passwordEditText.setTextColor(0xff000000);
            passwordEditText.setMaxLines(1);
            passwordEditText.setLines(1);
            passwordEditText.setGravity(Gravity.CENTER_HORIZONTAL);
            passwordEditText.setSingleLine(true);
            if (screen == SCREEN1_ENTER_CODE1) {
                passcodeSetStep = 0;
                passwordEditText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            } else {
                passcodeSetStep = 1;
                passwordEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            }
            passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            passwordEditText.setTypeface(Typeface.DEFAULT);
            frameLayout.addView(passwordEditText);
            layoutParams = (FrameLayout.LayoutParams) passwordEditText.getLayoutParams();
            layoutParams.topMargin = AndroidUtilities.dp(90);
            layoutParams.height = AndroidUtilities.dp(36);
            layoutParams.leftMargin = AndroidUtilities.dp(40);
            layoutParams.gravity = Gravity.TOP | Gravity.START;
            layoutParams.rightMargin = AndroidUtilities.dp(40);
            layoutParams.width = LayoutHelper.MATCH_PARENT;
            passwordEditText.setLayoutParams(layoutParams);
            passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if (passcodeSetStep == 0) {
                        processNext();
                        return true;
                    } else if (passcodeSetStep == 1) {
                        processDone();
                        return true;
                    }
                    return false;
                }
            });
            passwordEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (passwordEditText.length() == 4) {
                        if (screen == SCREEN2_ENTER_CODE2 && UserConfig.passcodeType == 0) {
                            processDone();
                        } else if (screen == SCREEN1_ENTER_CODE1 && currentPasswordType == 0) {
                            if (passcodeSetStep == 0) {
                                processNext();
                            } else if (passcodeSetStep == 1) {
                                processDone();
                            }
                        }
                    }
                }
            });

            passwordEditText.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
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

            if (screen == SCREEN1_ENTER_CODE1) {
                dropDownContainer = new ActionBarMenuItem(context, menu, 0);
                dropDownContainer.setSubMenuOpenSide(1);
                dropDownContainer.addSubItem(pin_item, context.getString(R.string.PasscodePIN));
                dropDownContainer.addSubItem(password_item, context.getString(R.string.PasscodePassword));
                actionBar.addView(dropDownContainer);
                layoutParams = (FrameLayout.LayoutParams) dropDownContainer.getLayoutParams();
                layoutParams.height = LayoutHelper.MATCH_PARENT;
                layoutParams.width = LayoutHelper.WRAP_CONTENT;
                layoutParams.rightMargin = AndroidUtilities.dp(40);
                layoutParams.leftMargin = AndroidUtilities.dp(56);
                layoutParams.gravity = Gravity.TOP | Gravity.START;
                dropDownContainer.setLayoutParams(layoutParams);
                dropDownContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dropDownContainer.toggleSubMenu();
                    }
                });

                dropDown = new TextView(context);
                dropDown.setGravity(Gravity.START);
                dropDown.setSingleLine(true);
                dropDown.setLines(1);
                dropDown.setMaxLines(1);
                dropDown.setEllipsize(TextUtils.TruncateAt.END);
                dropDown.setTextColor(0xffffffff);
                dropDown.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0);
                dropDown.setCompoundDrawablePadding(AndroidUtilities.dp(4));
                dropDown.setPadding(0, 0, AndroidUtilities.dp(10), 0);
                dropDownContainer.addView(dropDown);
                layoutParams = (FrameLayout.LayoutParams) dropDown.getLayoutParams();
                layoutParams.width = LayoutHelper.WRAP_CONTENT;
                layoutParams.height = LayoutHelper.WRAP_CONTENT;
                layoutParams.leftMargin = AndroidUtilities.dp(16);
                layoutParams.gravity = Gravity.CENTER_VERTICAL;
                layoutParams.bottomMargin = AndroidUtilities.dp(1);
                dropDown.setLayoutParams(layoutParams);
            } else {
                actionBar.setTitle(context.getString(R.string.Passcode));
            }

            updateDropDownTextView();
        } else {
            actionBar.setTitle(context.getString(R.string.Passcode));
            frameLayout.setBackgroundColor(0xfff0f0f0);
            listView = new ListView(context);
            listView.setDivider(null);
            listView.setDividerHeight(0);
            listView.setDrawSelectorOnTop(true);
            frameLayout.addView(listView);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
            layoutParams.width = LayoutHelper.MATCH_PARENT;
            layoutParams.height = LayoutHelper.MATCH_PARENT;
            layoutParams.gravity = Gravity.TOP;
            listView.setLayoutParams(layoutParams);
            listView.setAdapter(listAdapter = new ListAdapter(context));
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                    if (i == changePasscodeRow) {
                        presentFragment(new PasscodeActivity(SCREEN1_ENTER_CODE1));
                    } else if (i == passcodeOnOffRow) {
                        TextCheckCell cell = (TextCheckCell) view;
                        if (UserConfig.passcodeHash.length() != 0) {
                            UserConfig.passcodeHash = "";
                            UserConfig.appLocked = false;
                            UserConfig.saveConfig();
                            int count = listView.getChildCount();
                            for (int a = 0; a < count; a++) {
                                View child = listView.getChildAt(a);
                                if (child instanceof TextSettingsCell) {
                                    TextSettingsCell textCell = (TextSettingsCell) child;
                                    textCell.setTextColor(0xffc6c6c6);
                                    break;
                                }
                            }
                            cell.setChecked(UserConfig.passcodeHash.length() != 0);
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.didSetPasscode);
                        } else {
                            presentFragment(new PasscodeActivity(SCREEN1_ENTER_CODE1));
                        }
                    } else if (i == autoLockRow) {
                        if (getParentActivity() == null) {
                            return;
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setTitle(context.getString(R.string.AutoLock));
                        final NumberPicker numberPicker = new NumberPicker(getParentActivity());
                        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // otherwise the EditText gains the focus
                        numberPicker.setMinValue(0);
                        numberPicker.setMaxValue(4);
                        String[] displayedValues = {context.getString(R.string.Disabled), context.getResources().getQuantityString(R.plurals.Minutes, 1, 1), context.getResources().getQuantityString(R.plurals.Minutes, 5, 5), context.getResources().getQuantityString(R.plurals.Hours, 1, 1), context.getResources().getQuantityString(R.plurals.Hours, 5, 5)};
                        numberPicker.setDisplayedValues(displayedValues);
                        if (UserConfig.autoLockIn >= 60 * 60 * 5) {
                            numberPicker.setValue(4);
                        } else if (UserConfig.autoLockIn >= 60 * 60) {
                            numberPicker.setValue(3);
                        } else if (UserConfig.autoLockIn >= 60 * 5) {
                            numberPicker.setValue(2);
                        } else if (UserConfig.autoLockIn >= 60) {
                            numberPicker.setValue(1);
                        } else {
                            numberPicker.setValue(0);
                        }
                        numberPicker.setWrapSelectorWheel(false);
                        builder.setView(numberPicker);
                        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                which = numberPicker.getValue();
                                if (which == 0) {
                                    UserConfig.autoLockIn = 0;
                                } else if (which == 1) {
                                    UserConfig.autoLockIn = 60;
                                } else if (which == 2) {
                                    UserConfig.autoLockIn = 60 * 5;
                                } else if (which == 3) {
                                    UserConfig.autoLockIn = 60 * 60;
                                } else if (which == 4) {
                                    UserConfig.autoLockIn = 60 * 60 * 5;
                                }
                                listView.invalidateViews();
                                UserConfig.saveConfig();
                            }
                        });
                        showDialog(builder.create());
                    } else if (i == fingerprintRow) {
                        UserConfig.useFingerprint = !UserConfig.useFingerprint;
                        UserConfig.saveConfig();
                        ((TextCheckCell) view).setChecked(UserConfig.useFingerprint);
                    }
                }
            });
        }

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
        if (screen != SCREEN0_SETTINGS) {
            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    if (passwordEditText != null) {
                        passwordEditText.requestFocus();
                        AndroidUtilities.showKeyboard(passwordEditText);
                    }
                }
            }, 200);
        }
        fixLayoutInternal();
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.didSetPasscode) {
            if (screen == SCREEN0_SETTINGS) {
                updateRows();
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    private void updateRows() {
        rowCount = 0;
        passcodeOnOffRow = rowCount++;
        changePasscodeRow = rowCount++;
        passcodeDetailRow = rowCount++;
        if (UserConfig.passcodeHash.length() > 0) {
            try {
                if (Build.VERSION.SDK_INT >= 23) {
                    FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(ApplicationLoader.applicationContext);
                    if (fingerprintManager.isHardwareDetected()) {
                        fingerprintRow = rowCount++;
                    }
                }
            } catch (Throwable e) {

            }
            autoLockRow = rowCount++;
            autoLockDetailRow = rowCount++;
        } else {
            fingerprintRow = -1;
            autoLockRow = -1;
            autoLockDetailRow = -1;
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (listView != null) {
            ViewTreeObserver obs = listView.getViewTreeObserver();
            obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    listView.getViewTreeObserver().removeOnPreDrawListener(this);
                    fixLayoutInternal();
                    return true;
                }
            });
        }
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen && screen != SCREEN0_SETTINGS) {
            AndroidUtilities.showKeyboard(passwordEditText);
        }
    }

    private void updateDropDownTextView() {
        if (dropDown != null) {
            if (currentPasswordType == 0) {
                dropDown.setText(ApplicationLoader.applicationContext.getString(R.string.PasscodePIN));
            } else if (currentPasswordType == 1) {
                dropDown.setText(ApplicationLoader.applicationContext.getString(R.string.PasscodePassword));
            }
        }
        if (screen == SCREEN1_ENTER_CODE1 && currentPasswordType == 0 || screen == SCREEN2_ENTER_CODE2 && UserConfig.passcodeType == 0) {
            InputFilter[] filterArray = new InputFilter[1];
            filterArray[0] = new InputFilter.LengthFilter(4);
            passwordEditText.setFilters(filterArray);
            passwordEditText.setInputType(InputType.TYPE_CLASS_PHONE);
            passwordEditText.setKeyListener(DigitsKeyListener.getInstance("1234567890"));
        } else if (screen == SCREEN1_ENTER_CODE1 && currentPasswordType == 1 || screen == SCREEN2_ENTER_CODE2 && UserConfig.passcodeType == 1) {
            passwordEditText.setFilters(new InputFilter[0]);
            passwordEditText.setKeyListener(null);
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
    }

    private void processNext() {
        if (passwordEditText.getText().length() == 0 || currentPasswordType == 0 && passwordEditText.getText().length() != 4) {
            onPasscodeError();
            return;
        }
        if (currentPasswordType == 0) {
            actionBar.setTitle(ApplicationLoader.applicationContext.getString(R.string.PasscodePIN));
        } else {
            actionBar.setTitle(ApplicationLoader.applicationContext.getString(R.string.PasscodePassword));
        }
        dropDownContainer.setVisibility(View.GONE);
        titleTextView.setText(ApplicationLoader.applicationContext.getString(R.string.ReEnterYourPasscode));
        firstPassword = passwordEditText.getText().toString();
        passwordEditText.setText("");
        passcodeSetStep = 1;
    }

    private void processDone() {
        if (passwordEditText.getText().length() == 0) {
            onPasscodeError();
            return;
        }
        if (screen == SCREEN1_ENTER_CODE1) {
            if (!firstPassword.equals(passwordEditText.getText().toString())) {
                try {
                    Toast.makeText(getParentActivity(), ApplicationLoader.applicationContext.getString(R.string.PasscodeDoNotMatch), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {

                }
                AndroidUtilities.shakeView(titleTextView, 2, 0);
                passwordEditText.setText("");
                return;
            }

            try {
                UserConfig.passcodeSalt = new byte[16];
                Utilities.random.nextBytes(UserConfig.passcodeSalt);
                byte[] passcodeBytes = firstPassword.getBytes("UTF-8");
                byte[] bytes = new byte[32 + passcodeBytes.length];
                System.arraycopy(UserConfig.passcodeSalt, 0, bytes, 0, 16);
                System.arraycopy(passcodeBytes, 0, bytes, 16, passcodeBytes.length);
                System.arraycopy(UserConfig.passcodeSalt, 0, bytes, passcodeBytes.length + 16, 16);
                UserConfig.passcodeHash = Utilities.bytesToHex(Utilities.computeSHA256(bytes, 0, bytes.length));
            } catch (Exception e) {

            }

            UserConfig.passcodeType = currentPasswordType;
            UserConfig.saveConfig();
            finishFragment();
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.didSetPasscode);
            passwordEditText.clearFocus();
            AndroidUtilities.hideKeyboard(passwordEditText);
        } else if (screen == SCREEN2_ENTER_CODE2) {
            if (!UserConfig.checkPasscode(passwordEditText.getText().toString())) {
                passwordEditText.setText("");
                onPasscodeError();
                return;
            }
            passwordEditText.clearFocus();
            AndroidUtilities.hideKeyboard(passwordEditText);
            presentFragment(new PasscodeActivity(SCREEN0_SETTINGS), true);
        }
    }

    private void onPasscodeError() {
        if (getParentActivity() == null) {
            return;
        }
        Vibrator v = (Vibrator) getParentActivity().getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(200);
        }
        AndroidUtilities.shakeView(titleTextView, 2, 0);
    }

    private void fixLayoutInternal() {
        if (dropDownContainer != null) {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) dropDownContainer.getLayoutParams();
            layoutParams.topMargin = (Build.VERSION.SDK_INT >= 21 ? AndroidUtilities.statusBarHeight : 0);
            dropDownContainer.setLayoutParams(layoutParams);
            if (ApplicationLoader.applicationContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                dropDown.setTextSize(18);
            } else {
                dropDown.setTextSize(20);
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
            return i == passcodeOnOffRow || i == fingerprintRow || i == autoLockRow || UserConfig.passcodeHash.length() != 0 && i == changePasscodeRow;
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
            int viewType = getItemViewType(i);
            if (viewType == 0) {
                if (view == null) {
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                TextCheckCell textCell = (TextCheckCell) view;

                if (i == passcodeOnOffRow) {
                    textCell.setTextAndCheck(mContext.getString(R.string.Passcode), UserConfig.passcodeHash.length() > 0, true);
                } else if (i == fingerprintRow) {
                    textCell.setTextAndCheck(mContext.getString(R.string.UnlockFingerprint), UserConfig.useFingerprint, true);
                }
            } else if (viewType == 1) {
                if (view == null) {
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                TextSettingsCell textCell = (TextSettingsCell) view;
                if (i == changePasscodeRow) {
                    textCell.setText(mContext.getString(R.string.ChangePasscode), false);
                    textCell.setTextColor(UserConfig.passcodeHash.length() == 0 ? 0xffc6c6c6 : 0xff000000);
                } else if (i == autoLockRow) {
                    String val;
                    if (UserConfig.autoLockIn == 0) {
                        val = mContext.getString(R.string.Disabled);
                    } else if (UserConfig.autoLockIn < 60 * 60) {
                        val = mContext.getResources().getQuantityString(R.plurals.Minutes, UserConfig.autoLockIn / 60, UserConfig.autoLockIn / 60);
                    } else if (UserConfig.autoLockIn < 60 * 60 * 24) {
                        val = mContext.getResources().getQuantityString(R.plurals.Hours, (int) Math.ceil(UserConfig.autoLockIn / 60.0f / 60), (int) Math.ceil(UserConfig.autoLockIn / 60.0f / 60));
                    } else {
                        val = mContext.getResources().getQuantityString(R.plurals.Days, (int) Math.ceil(UserConfig.autoLockIn / 60.0f / 60 / 24), (int) Math.ceil(UserConfig.autoLockIn / 60.0f / 60 / 24));
                    }
                    textCell.setTextAndValue(mContext.getString(R.string.AutoLock), val, true);
                    textCell.setTextColor(0xff000000);
                }
            } else if (viewType == 2) {
                if (view == null) {
                    view = new TextInfoCell(mContext);
                }
                if (i == passcodeDetailRow) {
                    ((TextInfoCell) view).setText(mContext.getString(R.string.ChangePasscodeInfo));
                    if (autoLockDetailRow != -1) {
                        view.setBackgroundResource(R.drawable.greydivider);
                    } else {
                        view.setBackgroundResource(R.drawable.greydivider_bottom);
                    }
                } else if (i == autoLockDetailRow) {
                    ((TextInfoCell) view).setText(mContext.getString(R.string.AutoLockInfo));
                    view.setBackgroundResource(R.drawable.greydivider_bottom);
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == passcodeOnOffRow || i == fingerprintRow) {
                return 0;
            } else if (i == changePasscodeRow || i == autoLockRow) {
                return 1;
            } else if (i == passcodeDetailRow || i == autoLockDetailRow) {
                return 2;
            }
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
