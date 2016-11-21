/*
 * This part of the Delta Chat fronted is based on Telegram which is covered by the following note:
 *
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MrMailbox;
import org.telegram.tgnet.TLRPC;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.AvatarUpdater;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;


public class ContactAddActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, AvatarUpdater.AvatarUpdaterDelegate {

    private int             do_what = 0;
    public final static int CREATE_CONTACT   = 1;
    public final static int EDIT_NAME        = 2;

    private EditText nameTextView;
    private EditText emailTextView;
    private TLRPC.FileLocation avatar;
    private TLRPC.InputFile uploadedAvatar;
    private BackupImageView avatarImage;
    private AvatarDrawable avatarDrawable;
    private AvatarUpdater avatarUpdater = new AvatarUpdater();
    private String nameToSet = null;
    private int user_id;
    private boolean create_chat_when_done;

    private final static int done_button = 1;

    public ContactAddActivity(Bundle args) {
        super(args);
        avatarDrawable = new AvatarDrawable();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);
        avatarUpdater.parentFragment = this;
        avatarUpdater.delegate = this;
        avatarUpdater.returnOnly = true;
        do_what = getArguments().getInt("do_what", 0);
        user_id = getArguments().getInt("user_id", 0);
        create_chat_when_done = getArguments().getBoolean("create_chat_when_done", false);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);

        avatarUpdater.clear();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View createView(Context context) {

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        if (do_what==CREATE_CONTACT) {
            actionBar.setTitle(LocaleController.getString("NewContactTitle", R.string.NewContactTitle));
        } else {
            actionBar.setTitle(LocaleController.getString("EditName", R.string.EditName));
            long hContact = MrMailbox.MrMailboxGetContact(MrMailbox.hMailbox, user_id);
                nameToSet = MrMailbox.MrContactGetDisplayName(hContact);
            MrMailbox.MrContactUnref(hContact);
        }

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == done_button) {
                    String name = nameTextView.getText().toString();
                    String addr = "";
                    if (do_what==CREATE_CONTACT) {
                        addr = emailTextView.getText().toString();
                    }
                    else {
                        long hContact = MrMailbox.MrMailboxGetContact(MrMailbox.hMailbox, user_id);
                            addr = MrMailbox.MrContactGetAddr(hContact);
                        MrMailbox.MrContactUnref(hContact);
                    }

                    int new_user_id;
                    if( (new_user_id=MrMailbox.MrMailboxCreateContact(MrMailbox.hMailbox, name, addr))==0 ) {
                        Toast.makeText(getParentActivity(), LocaleController.getString("BadEmailAddress", R.string.BadEmailAddress), Toast.LENGTH_LONG).show();
                        return;
                    }
                    else if (do_what==CREATE_CONTACT) {
                        if(create_chat_when_done) {
                            int belonging_chat_id = MrMailbox.MrMailboxCreateChatByContactId(MrMailbox.hMailbox, new_user_id);
                            if( belonging_chat_id != 0 ) {
                                Bundle args = new Bundle();
                                args.putInt("chat_id", belonging_chat_id);
                                presentFragment(new ChatActivity(args), true);
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.chatDidCreated, belonging_chat_id); /*this will remove the contact list from stack */
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_NAME);
                                return;
                            }
                        }
                        else {
                            Toast.makeText(getParentActivity(), LocaleController.getString("ContactCreated", R.string.ContactCreated), Toast.LENGTH_LONG).show();
                        }
                    }

                    finishFragment();
                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_NAME);
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItemWithWidth(done_button, R.drawable.ic_done, AndroidUtilities.dp(56));

        fragmentView = new LinearLayout(context);
        LinearLayout linearLayout = (LinearLayout) fragmentView;
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        FrameLayout frameLayout = new FrameLayout(context);
        linearLayout.addView(frameLayout);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) frameLayout.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.WRAP_CONTENT;
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        frameLayout.setLayoutParams(layoutParams);

        avatarImage = new BackupImageView(context);
        avatarImage.setRoundRadius(AndroidUtilities.dp(32));
        avatarDrawable.setInfoByName(nameToSet!=null? nameToSet : "?");
        avatarImage.setImageDrawable(avatarDrawable);
        frameLayout.addView(avatarImage);
        FrameLayout.LayoutParams layoutParams1 = (FrameLayout.LayoutParams) avatarImage.getLayoutParams();
        layoutParams1.width = AndroidUtilities.dp(64);
        layoutParams1.height = AndroidUtilities.dp(64);
        layoutParams1.topMargin = AndroidUtilities.dp(12);
        layoutParams1.bottomMargin = AndroidUtilities.dp(12);
        layoutParams1.leftMargin = LocaleController.isRTL ? 0 : AndroidUtilities.dp(16);
        layoutParams1.rightMargin = LocaleController.isRTL ? AndroidUtilities.dp(16) : 0;
        layoutParams1.gravity = Gravity.TOP | (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        avatarImage.setLayoutParams(layoutParams1);
        {
            avatarDrawable.setDrawPhoto(true);
            avatarImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());

                    CharSequence[] items;

                    if (avatar != null) {
                        items = new CharSequence[]{LocaleController.getString("FromCamera", R.string.FromCamera), LocaleController.getString("FromGalley", R.string.FromGalley), LocaleController.getString("DeletePhoto", R.string.DeletePhoto)};
                    } else {
                        items = new CharSequence[]{LocaleController.getString("FromCamera", R.string.FromCamera), LocaleController.getString("FromGalley", R.string.FromGalley)};
                    }

                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (i == 0) {
                                avatarUpdater.openCamera();
                            } else if (i == 1) {
                                avatarUpdater.openGallery();
                            } else if (i == 2) {
                                avatar = null;
                                uploadedAvatar = null;
                                avatarImage.setImage(avatar, "50_50", avatarDrawable);
                            }
                        }
                    });
                    showDialog(builder.create());
                }
            });
        }

        nameTextView = new EditText(context);
        nameTextView.setHint(LocaleController.getString("Name", R.string.Name));
        if (nameToSet != null) {
            nameTextView.setText(nameToSet);
        }
        nameTextView.setMaxLines(4);
        nameTextView.setGravity(Gravity.CENTER_VERTICAL | (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT));
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        nameTextView.setHintTextColor(0xff979797);
        nameTextView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        nameTextView.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nameTextView.setPadding(0, 0, 0, AndroidUtilities.dp(8));
        InputFilter[] inputFilters = new InputFilter[1];
        inputFilters[0] = new InputFilter.LengthFilter(100);
        nameTextView.setFilters(inputFilters);
        AndroidUtilities.clearCursorDrawable(nameTextView);
        nameTextView.setTextColor(0xff212121);
        frameLayout.addView(nameTextView);
        layoutParams1 = (FrameLayout.LayoutParams) nameTextView.getLayoutParams();
        layoutParams1.width = LayoutHelper.MATCH_PARENT;
        layoutParams1.height = LayoutHelper.WRAP_CONTENT;
        layoutParams1.leftMargin = LocaleController.isRTL ? AndroidUtilities.dp(16) : AndroidUtilities.dp(96);
        layoutParams1.rightMargin = LocaleController.isRTL ? AndroidUtilities.dp(96) : AndroidUtilities.dp(16);
        layoutParams1.gravity = Gravity.CENTER_VERTICAL;
        nameTextView.setLayoutParams(layoutParams1);
        {
            nameTextView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    avatarDrawable.setInfoByName(nameTextView.length() > 0 ? nameTextView.getText().toString() : "?");
                    avatarImage.invalidate();
                }
            });
        }

        if( do_what==CREATE_CONTACT ) {
            emailTextView = new EditText(context);
            emailTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            emailTextView.setHintTextColor(0xff979797);
            emailTextView.setTextColor(0xff212121);
            emailTextView.setMaxLines(4);
            emailTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            emailTextView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            emailTextView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            emailTextView.setPadding(0, 0, 0, AndroidUtilities.dp(8));
            emailTextView.setHint(LocaleController.getString("EmailAddress", R.string.EmailAddress));
            AndroidUtilities.clearCursorDrawable(emailTextView);
            linearLayout.addView(emailTextView);
            LinearLayout.LayoutParams layoutParams2 = (LinearLayout.LayoutParams) emailTextView.getLayoutParams();
            layoutParams2.width = LayoutHelper.MATCH_PARENT;
            layoutParams2.height = LayoutHelper.WRAP_CONTENT;
            layoutParams2.topMargin = AndroidUtilities.dp(16);
            layoutParams2.leftMargin = AndroidUtilities.dp(16);
            layoutParams2.rightMargin = AndroidUtilities.dp(16);
            layoutParams2.gravity = Gravity.CENTER_VERTICAL;
            emailTextView.setLayoutParams(layoutParams2);
        }

        nameToSet = null;
        return fragmentView;
    }

    @Override
    public void didUploadedPhoto(final TLRPC.InputFile file, final TLRPC.PhotoSize small, final TLRPC.PhotoSize big) {
        Toast.makeText(getParentActivity(), LocaleController.getString("NotYetImplemented", R.string.NotYetImplemented), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        avatarUpdater.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void saveSelfArgs(Bundle args) {
        if (avatarUpdater != null && avatarUpdater.currentPicturePath != null) {
            args.putString("path", avatarUpdater.currentPicturePath);
        }
        if (nameTextView != null) {
            String text = nameTextView.getText().toString();
            if (text != null && text.length() != 0) {
                args.putString("nameTextView", text);
            }
        }
    }

    @Override
    public void restoreSelfArgs(Bundle args) {
        if (avatarUpdater != null) {
            avatarUpdater.currentPicturePath = args.getString("path");
        }
        String text = args.getString("nameTextView");
        if (text != null) {
            if (nameTextView != null) {
                nameTextView.setText(text);
            } else {
                nameToSet = text;
            }
        }
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen) {
            nameTextView.requestFocus();
            AndroidUtilities.showKeyboard(nameTextView);
        }
    }

    @Override
    public void didReceivedNotification(int id, final Object... args) {
        if (id == NotificationCenter.updateInterfaces) {
            int mask = (Integer)args[0];
            if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_NAME) != 0 || (mask & MessagesController.UPDATE_MASK_STATUS) != 0) {
                updateVisibleRows(mask);
            }
        }
    }

    private void updateVisibleRows(int mask) {
    }
}
