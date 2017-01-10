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

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.AnimatorListenerAdapterProxy;
import com.b44t.messenger.ContactsController;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MrChat;
import com.b44t.messenger.MrContact;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.SendMessagesHelper;
import com.b44t.messenger.UserObject;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.support.widget.LinearLayoutManager;
import com.b44t.messenger.support.widget.RecyclerView;
import com.b44t.messenger.TLRPC;

import com.b44t.messenger.FileLog;
import com.b44t.messenger.MessagesController;
import com.b44t.messenger.NotificationCenter;
import com.b44t.messenger.R;
import com.b44t.messenger.MessageObject;
import com.b44t.ui.ActionBar.BackDrawable;
import com.b44t.ui.ActionBar.SimpleTextView;
import com.b44t.ui.Cells.DividerCell;
import com.b44t.ui.Cells.EmptyCell;
import com.b44t.ui.Cells.ShadowSectionCell;
import com.b44t.ui.Cells.TextCell;
import com.b44t.ui.Cells.TextDetailCell;
import com.b44t.ui.Cells.UserCell;
import com.b44t.ui.ActionBar.ActionBar;
import com.b44t.ui.ActionBar.ActionBarMenu;
import com.b44t.ui.ActionBar.ActionBarMenuItem;
import com.b44t.ui.Components.AvatarDrawable;
import com.b44t.ui.Components.AvatarUpdater;
import com.b44t.ui.Components.BackupImageView;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.Components.LayoutHelper;
import com.b44t.ui.Components.RecyclerListView;
import com.b44t.ui.ActionBar.Theme;

import java.util.ArrayList;


public class ProfileActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, DialogsActivity.DialogsActivityDelegate, PhotoViewer.PhotoViewerProvider {

    private int user_id;  // show the profile of a single user
    private int chat_id;  // show the profile of a group

    private final int typeEmpty = 0;
    private final int typeDivider = 1;
    private final int typeTextDetailCell = 2;
    private final int typeTextCell = 3;
    private final int typeContactCell = 4;
    private final int typeSection = 5;

    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private BackupImageView avatarImage;
    private SimpleTextView nameTextView[] = new SimpleTextView[2];
    private SimpleTextView onlineTextView[] = new SimpleTextView[2];
    private ImageView writeButton;
    private AnimatorSet writeButtonAnimation;
    private AvatarDrawable avatarDrawable;
    private ActionBarMenuItem animatingItem;
    private TopView topView;

    private long dialog_id;

    private boolean openAnimationInProgress;
    private boolean playProfileAnimation;
    private boolean allowProfileAnimation = true;
    private int extraHeight;
    private int initialAnimationExtraHeight;

    private AvatarUpdater avatarUpdater;
    private int[] sortedUserIds;

    private final static int ID_BLOCK_CONTACT = 2;
    private final static int ID_DELETE_CONTACT = 5;
    private final static int ID_INVITE_TO_GROUP = 9;
    private final static int ID_ADD_SHORTCUT = 14;

    private int emptyRow = -1;
    private int userSectionRow = -1;
    private int sectionRow = -1;
    private int settingsNotificationsRow = -1;
    private int changeNameRow = -1;
    private int startChatRow = -1;
    private int addMemberRow = -1;

    private int emptyRowChat = -1;
    private int membersSectionRow = -1;
    private int emptyRowChat2 = -1;

    private int firstMemberRow = -1;
    private int lastMemberRow = -1;


    private int rowCount = 0;

    private class TopView extends View {

        private Paint paint = new Paint();

        public TopView(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + AndroidUtilities.dp(91));
        }

        @Override
        public void setBackgroundColor(int color) {
            paint.setColor(color);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int height = getMeasuredHeight() - AndroidUtilities.dp(91);
            canvas.drawRect(0, 0, getMeasuredWidth(), height + extraHeight, paint);
            if (parentLayout != null) {
                parentLayout.drawHeaderShadow(canvas, height + extraHeight);
            }
        }
    }

    public ProfileActivity(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        user_id = arguments.getInt("user_id", 0);
        chat_id = getArguments().getInt("chat_id", 0);
        if (user_id != 0) {
            dialog_id = arguments.getLong("dialog_id", 0);

            TLRPC.User user = MessagesController.getInstance().getUser(user_id);
            if (user == null) {
                return false;
            }
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.contactsDidLoaded);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.userInfoDidLoaded);


        } else if (chat_id != 0) {
            sortedUserIds = MrMailbox.getChatContacts(chat_id);

            avatarUpdater = new AvatarUpdater();
            avatarUpdater.delegate = new AvatarUpdater.AvatarUpdaterDelegate() {
                @Override
                public void didUploadedPhoto(TLRPC.InputFile file, TLRPC.PhotoSize small, TLRPC.PhotoSize big) {
                    if (chat_id != 0) {
                        MessagesController.getInstance().changeChatAvatar(chat_id, file);
                    }
                }
            };
            avatarUpdater.parentFragment = this;
        } else {
            return false;
        }

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.closeChats);
        updateRowsIds();

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.closeChats);
        if (user_id != 0) {
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.contactsDidLoaded);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.userInfoDidLoaded);
            //MessagesController.getInstance().cancelLoadFullUser(user_id);
        } else if (chat_id != 0) {
            //NotificationCenter.getInstance().removeObserver(this, NotificationCenter.chatInfoDidLoaded);
            avatarUpdater.clear();
        }
    }

    @Override
    protected ActionBar createActionBar(Context context) {
        ActionBar actionBar = new ActionBar(context) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                return super.onTouchEvent(event);
            }
        };
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setCastShadows(false);
        actionBar.setAddToContainer(false);
        actionBar.setOccupyStatusBar(Build.VERSION.SDK_INT >= 21 && !AndroidUtilities.isTablet());
        return actionBar;
    }

    @Override
    public View createView(Context context) {
        hasOwnBackground = true;
        extraHeight = AndroidUtilities.dp(88);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(final int id) {
                if (getParentActivity() == null) {
                    return;
                }
                if (id == -1) {
                    finishFragment();
                } else if (id == ID_BLOCK_CONTACT) {
                    if( userBlocked() ) {
                        MrMailbox.blockContact(user_id, 0);
                        finishFragment(); /* got to the parent, this is important eg. when editing blocking in the BlockedUserActivitiy. Moreover, this saves us updating all the states in the profile */
                    }
                    else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setMessage(LocaleController.getString("AreYouSureBlockContact", R.string.AreYouSureBlockContact));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                MrMailbox.blockContact(user_id, 1);
                                finishFragment(); /* got to the parent, this is important eg. when editing blocking in the BlockedUserActivitiy. Moreover, this saves us updating all the states in the profile */
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                        showDialog(builder.create());
                    }
                } else if (id == ID_DELETE_CONTACT) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setMessage(LocaleController.getString("AreYouSureDeleteContact", R.string.AreYouSureDeleteContact));
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if( MrMailbox.deleteContact(user_id)==0 ) {
                                Toast.makeText(getParentActivity(), LocaleController.getString("CannotDeleteContact", R.string.CannotDeleteContact), Toast.LENGTH_LONG).show();
                            }
                            else {
                                finishFragment();
                            }
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                /*} else if (id == ID_INVITE_TO_GROUP) {
                    final TLRPC.User user = MessagesController.getInstance().getUser(user_id);
                    if (user == null) {
                        return;
                    }
                    Bundle args = new Bundle();
                    args.putBoolean("onlySelect", true);
                    args.putInt("dialogsType", 2);
                    args.putString("addToGroupAlertString", LocaleController.formatString("AddToTheGroupTitle", R.string.AddToTheGroupTitle, UserObject.getUserName(user), "%1$s"));
                    DialogsActivity fragment = new DialogsActivity(args);
                    fragment.setDelegate(new DialogsActivity.DialogsActivityDelegate() {
                        @Override
                        public void didSelectDialog(DialogsActivity fragment, long did, boolean param) {
                            Bundle args = new Bundle();
                            args.putBoolean("scrollToTopOnResume", true);
                            args.putInt("chat_id", -(int) did);

                            NotificationCenter.getInstance().removeObserver(ProfileActivity.this, NotificationCenter.closeChats);
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
                            MessagesController.getInstance().addUserToChat(-(int) did, user, null, 0, null, ProfileActivity.this);
                            presentFragment(new ChatActivity(args), true);
                            removeSelfFromStack();
                        }
                    });
                    presentFragment(fragment);*/
                } else if (id == ID_ADD_SHORTCUT) {
                    try {
                        // draw avatar into a bitmap
                        int wh = avatarImage.imageReceiver.getImageWidth();
                        Bitmap bitmap = Bitmap.createBitmap(wh, wh, Bitmap.Config.ARGB_8888);
                        bitmap.eraseColor(Color.TRANSPARENT);
                        Canvas canvas = new Canvas(bitmap);
                        avatarImage.imageReceiver.draw(canvas);

                        // add shortcut
                        int install_chat_id = chat_id!=0? chat_id : MrMailbox.getChatIdByContactId(user_id);
                        AndroidUtilities.installShortcut(install_chat_id, bitmap);
                        Toast.makeText(getParentActivity(), LocaleController.getString("ShortcutAdded", R.string.ShortcutAdded), Toast.LENGTH_LONG).show();

                    } catch (Exception e) {
                        FileLog.e("messenger", e);
                    }
                }
            }
        });

        createActionBarMenu();

        listAdapter = new ListAdapter(context);
        avatarDrawable = new AvatarDrawable();

        fragmentView = new FrameLayout(context) {
            @Override
            public boolean hasOverlappingRendering() {
                return false;
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                checkListViewScroll();
            }
        };
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context) {
            @Override
            public boolean hasOverlappingRendering() {
                return false;
            }
        };
        listView.setTag(6);
        listView.setPadding(0, AndroidUtilities.dp(88), 0, 0);
        listView.setBackgroundColor(0xffffffff);
        listView.setVerticalScrollBarEnabled(false);
        listView.setItemAnimator(null);
        listView.setLayoutAnimation(null);
        listView.setClipToPadding(false);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        listView.setLayoutManager(layoutManager);
        listView.setGlowColor(Theme.ACTION_BAR_COLOR);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));

        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
            @Override
            public void onItemClick(View view, final int position) {
                if (getParentActivity() == null) {
                    return;
                }
                if (position == settingsNotificationsRow) {
                    Bundle args = new Bundle();
                    if (chat_id != 0) {
                        args.putInt("chat_id", chat_id);
                    } else {
                        args.putInt("chat_id", MrMailbox.getChatIdByContactId(user_id));
                    }
                    presentFragment(new ProfileNotificationsActivity(args));
                }
                else if(position==changeNameRow) {
                    Bundle args = new Bundle();
                    args.putInt("do_what", ContactAddActivity.EDIT_NAME);
                    args.putInt("user_id", user_id);
                    presentFragment(new ContactAddActivity(args));
                }
                else if(position==startChatRow) {
                    int belonging_chat_id = MrMailbox.getChatIdByContactId(user_id);
                    if( belonging_chat_id != 0 ) {
                        Bundle args = new Bundle();
                        args.putInt("chat_id", belonging_chat_id);
                        NotificationCenter.getInstance().removeObserver(ProfileActivity.this, NotificationCenter.closeChats);
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
                        presentFragment(new ChatActivity(args), true /*remove last*/);
                        return;
                    }

                    String name = "";
                    {
                        MrContact mrContact = MrMailbox.getContact(user_id);
                        name = mrContact.getNameNAddr();
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            int belonging_chat_id = MrMailbox.createChatByContactId(user_id);
                            if( belonging_chat_id != 0 ) {
                                Bundle args = new Bundle();
                                args.putInt("chat_id", belonging_chat_id);
                                NotificationCenter.getInstance().removeObserver(ProfileActivity.this, NotificationCenter.closeChats);
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
                                presentFragment(new ChatActivity(args), true /*remove last*/);
                                return;
                            }
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    builder.setMessage(AndroidUtilities.replaceTags(LocaleController.formatString("AskStartChatWith", R.string.AskStartChatWith, name)));
                    showDialog(builder.create());
                } else if (position == addMemberRow) {
                    openAddMember();
                } else if (position >= firstMemberRow && position <= lastMemberRow) {
                    int curr_user_index = position - firstMemberRow;
                    if(curr_user_index>=0 && curr_user_index<sortedUserIds.length) {
                        int curr_user_id = sortedUserIds[curr_user_index];
                        if( curr_user_id > MrContact.MR_CONTACT_ID_LAST_SPECIAL ) {
                            Bundle args = new Bundle();
                            args.putInt("user_id", curr_user_id);
                            presentFragment(new ProfileActivity(args));
                        }
                    }
                } else {
                    processOnClickOrPress(position);
                }
            }
        });

        listView.setOnItemLongClickListener(new RecyclerListView.OnItemLongClickListener() {
            @Override
            public boolean onItemClick(View view, int position) {
                if (position >= firstMemberRow && position <= lastMemberRow) {
                    /*
                    if (getParentActivity() == null) {
                        return false;
                    }
                    boolean allowKick = false;
                    boolean allowSetAdmin = false;

                    final TLRPC.ChatParticipant user;
                    if (!sortedUsers.isEmpty()) {
                        user = info.participants.participants.get(sortedUsers.get(position - emptyRowChat2 - 1));
                    } else {
                        user = info.participants.participants.get(position - emptyRowChat2 - 1);
                    }
                    selectedUser = user.user_id;

                    if (user.user_id != UserConfig.getClientUserId()) {
                        if (currentChat.creator) {
                            allowKick = true;
                        } else if (user instanceof TLRPC.TL_chatParticipant) {
                            if (currentChat.admin && currentChat.admins_enabled || user.inviter_id == UserConfig.getClientUserId()) {
                                allowKick = true;
                            }
                        }
                    }

                    if (!allowKick) {
                        return false;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    if (currentChat.megagroup && currentChat.creator && allowSetAdmin) {
                        CharSequence[] items = new CharSequence[]{LocaleController.getString("SetAsAdmin", R.string.SetAsAdmin), LocaleController.getString("KickFromGroup", R.string.KickFromGroup)};
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (i == 0) {
                                    ;
                                } else if (i == 1) {
                                    kickUser(selectedUser);
                                }
                            }
                        });
                    } else {
                        CharSequence[] items = new CharSequence[]{chat_id > 0 ? LocaleController.getString("KickFromGroup", R.string.KickFromGroup) : LocaleController.getString("KickFromBroadcast", R.string.KickFromBroadcast)};
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (i == 0) {
                                    kickUser(selectedUser);
                                }
                            }
                        });
                    }
                    showDialog(builder.create());
                    */
                    return true;
                } else {
                    return processOnClickOrPress(position);
                }
            }
        });

        topView = new TopView(context);
        topView.setBackgroundColor(Theme.ACTION_BAR_COLOR);
        frameLayout.addView(topView);

        frameLayout.addView(actionBar);

        avatarImage = new BackupImageView(context);
        avatarImage.setRoundRadius(AndroidUtilities.dp(21));
        avatarImage.setPivotX(0);
        avatarImage.setPivotY(0);
        frameLayout.addView(avatarImage, LayoutHelper.createFrame(42, 42, Gravity.TOP | Gravity.LEFT, 64, 0, 0, 0));
        avatarImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (user_id != 0) {
                    TLRPC.User user = MessagesController.getInstance().getUser(user_id);
                    if (user.photo != null && user.photo.photo_big != null) {
                        PhotoViewer.getInstance().setParentActivity(getParentActivity());
                        PhotoViewer.getInstance().openPhoto(user.photo.photo_big, ProfileActivity.this);
                    }
                } else if (chat_id != 0) {
                    TLRPC.Chat chat = MrChat.chatId2chat(chat_id);
                    if (chat.photo != null && chat.photo.photo_big != null) {
                        PhotoViewer.getInstance().setParentActivity(getParentActivity());
                        PhotoViewer.getInstance().openPhoto(chat.photo.photo_big, ProfileActivity.this);
                    }
                }
            }
        });

        for (int a = 0; a < 2; a++) {
            if (!playProfileAnimation && a == 0) {
                continue;
            }
            nameTextView[a] = new SimpleTextView(context);
            nameTextView[a].setTextColor(Theme.ACTION_BAR_TITLE_COLOR);
            nameTextView[a].setTextSize(18);
            nameTextView[a].setGravity(Gravity.LEFT);
            nameTextView[a].setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            nameTextView[a].setLeftDrawableTopPadding(-AndroidUtilities.dp(1.3f));
            nameTextView[a].setRightDrawableTopPadding(-AndroidUtilities.dp(1.3f));
            nameTextView[a].setPivotX(0);
            nameTextView[a].setPivotY(0);
            frameLayout.addView(nameTextView[a], LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 118, 0, a == 0 ? 48 : 0, 0));

            onlineTextView[a] = new SimpleTextView(context);
            onlineTextView[a].setTextColor(Theme.ACTION_BAR_SUBTITLE_COLOR);
            onlineTextView[a].setTextSize(14);
            onlineTextView[a].setGravity(Gravity.LEFT);
            frameLayout.addView(onlineTextView[a], LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 118, 0, a == 0 ? 48 : 8, 0));
        }

        if ( chat_id != 0 && chat_id!= MrChat.MR_CHAT_ID_DEADDROP ) {
            writeButton = new ImageView(context);
            try {
                writeButton.setBackgroundResource(R.drawable.floating_user_states);
            } catch (Throwable e) {
                FileLog.e("messenger", e);
            }
            writeButton.setScaleType(ImageView.ScaleType.CENTER);
            writeButton.setImageResource(R.drawable.floating_camera);

            frameLayout.addView(writeButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.TOP, 0, 0, 16, 0));
            if (Build.VERSION.SDK_INT >= 21) {
                StateListAnimator animator = new StateListAnimator();
                animator.addState(new int[]{android.R.attr.state_pressed}, ObjectAnimator.ofFloat(writeButton, "translationZ", AndroidUtilities.dp(2), AndroidUtilities.dp(4)).setDuration(200));
                animator.addState(new int[]{}, ObjectAnimator.ofFloat(writeButton, "translationZ", AndroidUtilities.dp(4), AndroidUtilities.dp(2)).setDuration(200));
                writeButton.setStateListAnimator(animator);
                writeButton.setOutlineProvider(new ViewOutlineProvider() {
                    @SuppressLint("NewApi")
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setOval(0, 0, AndroidUtilities.dp(56), AndroidUtilities.dp(56));
                    }
                });
            }
            writeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    CharSequence[] items;
                    TLRPC.Chat chat = MrChat.chatId2chat(chat_id);
                    if (chat.photo == null || chat.photo.photo_big == null /*|| chat.photo instanceof TLRPC.TL_chatPhotoEmpty*/) {
                        items = new CharSequence[]{LocaleController.getString("FromCamera", R.string.FromCamera), LocaleController.getString("FromGalley", R.string.FromGalley)};
                    } else {
                        items = new CharSequence[]{LocaleController.getString("FromCamera", R.string.FromCamera), LocaleController.getString("FromGalley", R.string.FromGalley), LocaleController.getString("DeletePhoto", R.string.DeletePhoto)};
                    }

                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (i == 0) {
                                avatarUpdater.openCamera();
                            } else if (i == 1) {
                                avatarUpdater.openGallery();
                            } else if (i == 2) {
                                MessagesController.getInstance().changeChatAvatar(chat_id, null);
                            }
                        }
                    });
                    showDialog(builder.create());
                }
            });
        }
        needLayout();

        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                checkListViewScroll();
            }
        });

        return fragmentView;
    }

    private boolean processOnClickOrPress(final int position) {
        if (position == changeNameRow) {
            return true;
        }
        else if (position == startChatRow) {
            return true;
        }
        return false;
    }

    @Override
    public void saveSelfArgs(Bundle args) {
        if (chat_id != 0) {
            if (avatarUpdater != null && avatarUpdater.currentPicturePath != null) {
                args.putString("path", avatarUpdater.currentPicturePath);
            }
        }
    }

    @Override
    public void restoreSelfArgs(Bundle args) {
        if (chat_id != 0) {
            //MessagesController.getInstance().loadChatInfo(chat_id, null, false);
            if (avatarUpdater != null) {
                avatarUpdater.currentPicturePath = args.getString("path");
            }
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (chat_id != 0) {
            avatarUpdater.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void openAddMember() {
        /*
        Bundle args = new Bundle();
        args.putBoolean("onlyUsers", true);
        args.putBoolean("returnAsResult", true);
        args.putBoolean("needForwardCount", !ChatObject.isChannel(currentChat));
        //args.putBoolean("allowUsernameSearch", false);
        if (chat_id > 0) {
            if (currentChat.creator) {
                args.putInt("chat_id", currentChat.id);
            }
            args.putString("selectAlertString", LocaleController.getString("AddToTheGroup", R.string.AddToTheGroup));
        }
        ContactsActivity fragment = new ContactsActivity(args);
        fragment.setDelegate(new ContactsActivity.ContactsActivityDelegate() {
            @Override
            public void didSelectContact(TLRPC.User user, String param) {
                MessagesController.getInstance().addUserToChat(chat_id, user, info, param != null ? Utilities.parseInt(param) : 0, null, ProfileActivity.this);
            }
        });
        if (info != null && info.participants != null) {
            HashMap<Integer, TLRPC.User> users = new HashMap<>();
            for (int a = 0; a < info.participants.participants.size(); a++) {
                users.put(info.participants.participants.get(a).user_id, null);
            }
            fragment.setIgnoreUsers(users);
        }
        presentFragment(fragment);
        */
    }

    private void checkListViewScroll() {
        if (listView.getChildCount() <= 0 || openAnimationInProgress) {
            return;
        }

        View child = listView.getChildAt(0);
        ListAdapter.Holder holder = (ListAdapter.Holder) listView.findContainingViewHolder(child);
        int top = child.getTop();
        int newOffset = 0;
        if (top >= 0 && holder != null && holder.getAdapterPosition() == 0) {
            newOffset = top;
        }
        if (extraHeight != newOffset) {
            extraHeight = newOffset;
            topView.invalidate();
            if (playProfileAnimation) {
                allowProfileAnimation = extraHeight != 0;
            }
            needLayout();
        }
    }

    private void needLayout() {
        FrameLayout.LayoutParams layoutParams;
        int newTop = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight();
        if (listView != null && !openAnimationInProgress) {
            layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
            if (layoutParams.topMargin != newTop) {
                layoutParams.topMargin = newTop;
                listView.setLayoutParams(layoutParams);
            }
        }

        if (avatarImage != null) {
            float diff = extraHeight / (float) AndroidUtilities.dp(88);
            listView.setTopGlowOffset(extraHeight);

            if (writeButton != null) {
                writeButton.setTranslationY((actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() + extraHeight - AndroidUtilities.dp(29.5f));

                if (!openAnimationInProgress) {
                    final boolean setVisible = diff > 0.2f;
                    boolean currentVisible = writeButton.getTag() == null;
                    if (setVisible != currentVisible) {
                        if (setVisible) {
                            writeButton.setTag(null);
                        } else {
                            writeButton.setTag(0);
                        }
                        if (writeButtonAnimation != null) {
                            AnimatorSet old = writeButtonAnimation;
                            writeButtonAnimation = null;
                            old.cancel();
                        }
                        writeButtonAnimation = new AnimatorSet();
                        if (setVisible) {
                            writeButtonAnimation.setInterpolator(new DecelerateInterpolator());
                            writeButtonAnimation.playTogether(
                                    ObjectAnimator.ofFloat(writeButton, "scaleX", 1.0f),
                                    ObjectAnimator.ofFloat(writeButton, "scaleY", 1.0f),
                                    ObjectAnimator.ofFloat(writeButton, "alpha", 1.0f)
                            );
                        } else {
                            writeButtonAnimation.setInterpolator(new AccelerateInterpolator());
                            writeButtonAnimation.playTogether(
                                    ObjectAnimator.ofFloat(writeButton, "scaleX", 0.2f),
                                    ObjectAnimator.ofFloat(writeButton, "scaleY", 0.2f),
                                    ObjectAnimator.ofFloat(writeButton, "alpha", 0.0f)
                            );
                        }
                        writeButtonAnimation.setDuration(150);
                        writeButtonAnimation.addListener(new AnimatorListenerAdapterProxy() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (writeButtonAnimation != null && writeButtonAnimation.equals(animation)) {
                                    writeButtonAnimation = null;
                                }
                            }
                        });
                        writeButtonAnimation.start();
                    }
                }
            }

            float avatarY = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() / 2.0f * (1.0f + diff) - 21 * AndroidUtilities.density + 27 * AndroidUtilities.density * diff;
            avatarImage.setScaleX((42 + 18 * diff) / 42.0f);
            avatarImage.setScaleY((42 + 18 * diff) / 42.0f);
            avatarImage.setTranslationX(-AndroidUtilities.dp(47) * diff);
            avatarImage.setTranslationY((float) Math.ceil(avatarY));
            for (int a = 0; a < 2; a++) {
                if (nameTextView[a] == null) {
                    continue;
                }
                nameTextView[a].setTranslationX(-21 * AndroidUtilities.density * diff);
                nameTextView[a].setTranslationY((float) Math.floor(avatarY) + AndroidUtilities.dp(1.3f) + AndroidUtilities.dp(7) * diff);
                onlineTextView[a].setTranslationX(-21 * AndroidUtilities.density * diff);
                onlineTextView[a].setTranslationY((float) Math.floor(avatarY) + AndroidUtilities.dp(24) + (float) Math.floor(11 * AndroidUtilities.density) * diff);
                nameTextView[a].setScaleX(1.0f + 0.12f * diff);
                nameTextView[a].setScaleY(1.0f + 0.12f * diff);
                if (a == 1 && !openAnimationInProgress) {
                    int width;
                    if (AndroidUtilities.isTablet()) {
                        width = AndroidUtilities.dp(490);
                    } else {
                        width = AndroidUtilities.displaySize.x;
                    }
                    width = (int) (width - AndroidUtilities.dp(118 + 8 + 40 * (1.0f - diff)) - nameTextView[a].getTranslationX());
                    float width2 = nameTextView[a].getPaint().measureText(nameTextView[a].getText().toString()) * nameTextView[a].getScaleX() + nameTextView[a].getSideDrawablesSize();
                    layoutParams = (FrameLayout.LayoutParams) nameTextView[a].getLayoutParams();
                    if (width < width2) {
                        layoutParams.width = (int) Math.ceil(width / nameTextView[a].getScaleX());
                    } else {
                        layoutParams.width = LayoutHelper.WRAP_CONTENT;
                    }
                    nameTextView[a].setLayoutParams(layoutParams);

                    layoutParams = (FrameLayout.LayoutParams) onlineTextView[a].getLayoutParams();
                    layoutParams.rightMargin = (int) Math.ceil(onlineTextView[a].getTranslationX() + AndroidUtilities.dp(8) + AndroidUtilities.dp(40) * (1.0f - diff));
                    onlineTextView[a].setLayoutParams(layoutParams);
                }
            }
        }
    }

    private void fixLayout() {
        if (fragmentView == null) {
            return;
        }
        fragmentView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (fragmentView != null) {
                    checkListViewScroll();
                    needLayout();
                    fragmentView.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                return true;
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fixLayout();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void didReceivedNotification(int id, final Object... args) {
        if (id == NotificationCenter.updateInterfaces) {
            int mask = (Integer) args[0];
            if (user_id != 0) {
                if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_NAME) != 0 || (mask & MessagesController.UPDATE_MASK_STATUS) != 0) {
                    updateProfileData();
                }
            } else if (chat_id != 0) {
                if ((mask & MessagesController.UPDATE_MASK_CHAT_ADMINS) != 0) {
                    createActionBarMenu();
                    updateRowsIds();
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                }
                if ((mask & MessagesController.UPDATE_MASK_CHANNEL) != 0 || (mask & MessagesController.UPDATE_MASK_CHAT_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_CHAT_NAME) != 0 || (mask & MessagesController.UPDATE_MASK_CHAT_MEMBERS) != 0 || (mask & MessagesController.UPDATE_MASK_STATUS) != 0) {
                    updateProfileData();
                }
                if ((mask & MessagesController.UPDATE_MASK_CHANNEL) != 0) {
                    updateRowsIds();
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                }
                if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_NAME) != 0 || (mask & MessagesController.UPDATE_MASK_STATUS) != 0) {
                    if (listView != null) {
                        int count = listView.getChildCount();
                        for (int a = 0; a < count; a++) {
                            View child = listView.getChildAt(a);
                            if (child instanceof UserCell) {
                                ((UserCell) child).update(mask);
                            }
                        }
                    }
                }
            }
        } else if (id == NotificationCenter.contactsDidLoaded) {
            createActionBarMenu();
        } else if (id == NotificationCenter.closeChats) {
            removeSelfFromStack();
        } else if (id == NotificationCenter.userInfoDidLoaded) {
            int uid = (Integer) args[0];
            if (uid == user_id) {
                updateRowsIds();
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
        updateProfileData();
        fixLayout();
    }

    public void setPlayProfileAnimation(boolean value) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        if (!AndroidUtilities.isTablet() && preferences.getBoolean("view_animations", true)) {
            playProfileAnimation = value;
        }
    }

    @Override
    protected void onTransitionAnimationStart(boolean isOpen, boolean backward) {
        if (!backward && playProfileAnimation && allowProfileAnimation) {
            openAnimationInProgress = true;
        }
        NotificationCenter.getInstance().setAllowedNotificationsDutingAnimation(new int[]{NotificationCenter.dialogsNeedReload, NotificationCenter.closeChats});
        NotificationCenter.getInstance().setAnimationInProgress(true);
    }

    @Override
    protected void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (!backward && playProfileAnimation && allowProfileAnimation) {
            openAnimationInProgress = false;
        }
        NotificationCenter.getInstance().setAnimationInProgress(false);
    }

    public void setAnimationProgress(float progress) {
        //animationProgress = progress;
        listView.setAlpha(progress);

        listView.setTranslationX(AndroidUtilities.dp(48) - AndroidUtilities.dp(48) * progress);
        int color = Theme.ACTION_BAR_COLOR;

        int r = Color.red(Theme.ACTION_BAR_COLOR);
        int g = Color.green(Theme.ACTION_BAR_COLOR);
        int b = Color.blue(Theme.ACTION_BAR_COLOR);

        int rD = (int) ((Color.red(color) - r) * progress);
        int gD = (int) ((Color.green(color) - g) * progress);
        int bD = (int) ((Color.blue(color) - b) * progress);
        topView.setBackgroundColor(Color.rgb(r + rD, g + gD, b + bD));
        color = Theme.ACTION_BAR_SUBTITLE_COLOR;

        r = Color.red(Theme.ACTION_BAR_SUBTITLE_COLOR);
        g = Color.green(Theme.ACTION_BAR_SUBTITLE_COLOR);
        b = Color.blue(Theme.ACTION_BAR_SUBTITLE_COLOR);

        rD = (int) ((Color.red(color) - r) * progress);
        gD = (int) ((Color.green(color) - g) * progress);
        bD = (int) ((Color.blue(color) - b) * progress);
        for (int a = 0; a < 2; a++) {
            if (onlineTextView[a] == null) {
                continue;
            }
            onlineTextView[a].setTextColor(Color.rgb(r + rD, g + gD, b + bD));
        }
        extraHeight = (int) (initialAnimationExtraHeight * progress);
        color = AvatarDrawable.getColorForId(user_id != 0 ? user_id : chat_id);
        int color2 = AvatarDrawable.getColorForId(user_id != 0 ? user_id : chat_id);
        if (color != color2) {
            rD = (int) ((Color.red(color) - Color.red(color2)) * progress);
            gD = (int) ((Color.green(color) - Color.green(color2)) * progress);
            bD = (int) ((Color.blue(color) - Color.blue(color2)) * progress);
            avatarDrawable.setColor_(Color.rgb(Color.red(color2) + rD, Color.green(color2) + gD, Color.blue(color2) + bD));
            avatarImage.invalidate();
        }

        needLayout();
    }

    @Override
    protected AnimatorSet onCustomTransitionAnimation(final boolean isOpen, final Runnable callback) {
        if (playProfileAnimation && allowProfileAnimation) {
            final AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.setDuration(180);
            if (Build.VERSION.SDK_INT > 15) {
                listView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
            ActionBarMenu menu = actionBar.createMenu();
            if (menu.getItem(10) == null) {
                if (animatingItem == null) {
                    animatingItem = menu.addItem(10, R.drawable.ic_ab_other);
                }
            }
            if (isOpen) {
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) onlineTextView[1].getLayoutParams();
                layoutParams.rightMargin = (int) (-21 * AndroidUtilities.density + AndroidUtilities.dp(8));
                onlineTextView[1].setLayoutParams(layoutParams);

                int width = (int) Math.ceil(AndroidUtilities.displaySize.x - AndroidUtilities.dp(118 + 8) + 21 * AndroidUtilities.density);
                float width2 = nameTextView[1].getPaint().measureText(nameTextView[1].getText().toString()) * 1.12f + nameTextView[1].getSideDrawablesSize();
                layoutParams = (FrameLayout.LayoutParams) nameTextView[1].getLayoutParams();
                if (width < width2) {
                    layoutParams.width = (int) Math.ceil(width / 1.12f);
                } else {
                    layoutParams.width = LayoutHelper.WRAP_CONTENT;
                }
                nameTextView[1].setLayoutParams(layoutParams);

                initialAnimationExtraHeight = AndroidUtilities.dp(88);
                fragmentView.setBackgroundColor(0);
                setAnimationProgress(0);
                ArrayList<Animator> animators = new ArrayList<>();
                animators.add(ObjectAnimator.ofFloat(this, "animationProgress", 0.0f, 1.0f));
                if (writeButton != null) {
                    writeButton.setScaleX(0.2f);
                    writeButton.setScaleY(0.2f);
                    writeButton.setAlpha(0.0f);
                    animators.add(ObjectAnimator.ofFloat(writeButton, "scaleX", 1.0f));
                    animators.add(ObjectAnimator.ofFloat(writeButton, "scaleY", 1.0f));
                    animators.add(ObjectAnimator.ofFloat(writeButton, "alpha", 1.0f));
                }
                for (int a = 0; a < 2; a++) {
                    onlineTextView[a].setAlpha(a == 0 ? 1.0f : 0.0f);
                    nameTextView[a].setAlpha(a == 0 ? 1.0f : 0.0f);
                    animators.add(ObjectAnimator.ofFloat(onlineTextView[a], "alpha", a == 0 ? 0.0f : 1.0f));
                    animators.add(ObjectAnimator.ofFloat(nameTextView[a], "alpha", a == 0 ? 0.0f : 1.0f));
                }
                if (animatingItem != null) {
                    animatingItem.setAlpha(1.0f);
                    animators.add(ObjectAnimator.ofFloat(animatingItem, "alpha", 0.0f));
                }
                animatorSet.playTogether(animators);
            } else {
                initialAnimationExtraHeight = extraHeight;
                ArrayList<Animator> animators = new ArrayList<>();
                animators.add(ObjectAnimator.ofFloat(this, "animationProgress", 1.0f, 0.0f));
                if (writeButton != null) {
                    animators.add(ObjectAnimator.ofFloat(writeButton, "scaleX", 0.2f));
                    animators.add(ObjectAnimator.ofFloat(writeButton, "scaleY", 0.2f));
                    animators.add(ObjectAnimator.ofFloat(writeButton, "alpha", 0.0f));
                }
                for (int a = 0; a < 2; a++) {
                    animators.add(ObjectAnimator.ofFloat(onlineTextView[a], "alpha", a == 0 ? 1.0f : 0.0f));
                    animators.add(ObjectAnimator.ofFloat(nameTextView[a], "alpha", a == 0 ? 1.0f : 0.0f));
                }
                if (animatingItem != null) {
                    animatingItem.setAlpha(0.0f);
                    animators.add(ObjectAnimator.ofFloat(animatingItem, "alpha", 1.0f));
                }
                animatorSet.playTogether(animators);
            }
            animatorSet.addListener(new AnimatorListenerAdapterProxy() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (Build.VERSION.SDK_INT > 15) {
                        listView.setLayerType(View.LAYER_TYPE_NONE, null);
                    }
                    if (animatingItem != null) {
                        ActionBarMenu menu = actionBar.createMenu();
                        menu.clearItems();
                        animatingItem = null;
                    }
                    callback.run();
                }
            });
            animatorSet.setInterpolator(new DecelerateInterpolator());

            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    animatorSet.start();
                }
            }, 50);
            return animatorSet;
        }
        return null;
    }

    @Override
    public void updatePhotoAtIndex(int index) {

    }

    @Override
    public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        if (fileLocation == null) {
            return null;
        }

        TLRPC.FileLocation photoBig = null;
        if (user_id != 0) {
            TLRPC.User user = MessagesController.getInstance().getUser(user_id);
            if (user != null && user.photo != null && user.photo.photo_big != null) {
                photoBig = user.photo.photo_big;
            }
        } else if (chat_id != 0) {
            TLRPC.Chat chat = MrChat.chatId2chat(chat_id);
            if (chat != null && chat.photo != null && chat.photo.photo_big != null) {
                photoBig = chat.photo.photo_big;
            }
        }


        if (photoBig != null && photoBig.local_id == fileLocation.local_id && photoBig.volume_id == fileLocation.volume_id && photoBig.dc_id == fileLocation.dc_id) {
            int coords[] = new int[2];
            avatarImage.getLocationInWindow(coords);
            PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
            object.viewX = coords[0];
            object.viewY = coords[1] - AndroidUtilities.statusBarHeight;
            object.parentView = avatarImage;
            object.imageReceiver = avatarImage.getImageReceiver();
            if (user_id != 0) {
                object.dialogId = user_id;
            } else if (chat_id != 0) {
                object.dialogId = -chat_id;
            }
            object.thumb = object.imageReceiver.getBitmap();
            object.size = -1;
            object.radius = avatarImage.getImageReceiver().getRoundRadius();
            object.scale = avatarImage.getScaleX();
            return object;
        }
        return null;
    }

    @Override
    public Bitmap getThumbForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        return null;
    }

    @Override
    public void willSwitchFromPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) { }

    @Override
    public void willHidePhotoViewer() {
        avatarImage.getImageReceiver().setVisible(true, true);
    }

    @Override
    public boolean isPhotoChecked(int index) { return false; }

    @Override
    public void setPhotoChecked(int index) { }

    @Override
    public boolean cancelButtonPressed() { return true; }

    @Override
    public void sendButtonPressed(int index) { }

    @Override
    public int getSelectedCount() { return 0; }

    public boolean isChat() {
        return chat_id != 0;
    }

    private void updateRowsIds() {

        rowCount = 0;

        emptyRow = rowCount++;

        if( (chat_id!=0 && (chat_id!=MrChat.MR_CHAT_ID_DEADDROP || MrMailbox.getConfigInt("show_deaddrop", 0)!=0))
         || MrMailbox.getChatIdByContactId(user_id)!=0 ) {
            settingsNotificationsRow = rowCount++;
        }

        if( user_id!=0 || chat_id!=MrChat.MR_CHAT_ID_DEADDROP) {
            changeNameRow = rowCount++;
        }

        if (user_id != 0) {
            startChatRow = rowCount++;
        } else if (chat_id != 0 && chat_id!=MrChat.MR_CHAT_ID_DEADDROP ) {
            addMemberRow = rowCount++;
        }

        //sectionRow = rowCount++;

        if( chat_id != 0 ) {
            if( rowCount > 1/*first empty row is always added*/ ) {
                emptyRowChat = rowCount++;
                membersSectionRow = rowCount++;
                emptyRowChat2 = rowCount++;
            }

            firstMemberRow = rowCount;
            rowCount += sortedUserIds.length;
            lastMemberRow = rowCount-1;
        }
    }

    private boolean userBlocked()
    {
        boolean blocked = false;
        if( user_id!=0 ) {
            MrContact mrContact = MrMailbox.getContact(user_id);
            blocked = mrContact.isBlocked()!=0;
        }
        return blocked;
    }

    private void updateProfileData() {
        if (avatarImage == null || nameTextView == null) {
            return;
        }

        String newString;
        String newString2;

        MrContact mrContact = null;
        MrChat mrChat = null;
        if( user_id!=0 ) {
            mrContact = MrMailbox.getContact(user_id);
            newString = mrContact.getDisplayName();
            newString2 = mrContact.getAddr();
        }
        else {
            mrChat = MrMailbox.getChat(chat_id);
            newString = mrChat.getName();
            newString2 = mrChat.getSubtitle();
        }

        for (int a = 0; a < 2; a++) {
            if (nameTextView[a] == null) {
                continue;
            }
            if (!nameTextView[a].getText().equals(newString)) {
                nameTextView[a].setText(newString);
            }
            if (!onlineTextView[a].getText().equals(newString2)) {
                onlineTextView[a].setText(newString2);
            }
            int leftIcon = 0;//currentEncryptedChat != null ? R.drawable.ic_lock_header : 0;
            int rightIcon = 0;
            if (a == 0) {
                rightIcon = MessagesController.getInstance().isDialogMuted(dialog_id != 0 ? dialog_id : (long) user_id) ? R.drawable.mute_fixed : 0;
            }
            nameTextView[a].setLeftDrawable(leftIcon);
            nameTextView[a].setRightDrawable(rightIcon);
        }

        ContactsController.setupAvatar(avatarImage, avatarImage.imageReceiver, avatarDrawable, mrContact, mrChat);
    }

    private void createActionBarMenu() {
        ActionBarMenu menu = actionBar.createMenu();
        menu.clearItems();
        animatingItem = null;

        ActionBarMenuItem item = menu.addItem(10, R.drawable.ic_ab_other);

        if( chat_id!=0 || MrMailbox.getChatIdByContactId(user_id)!=0 ) {
            item.addSubItem(ID_ADD_SHORTCUT, LocaleController.getString("AddShortcut", R.string.AddShortcut), 0);
        }

        if (user_id != 0) {
            item.addSubItem(ID_BLOCK_CONTACT, userBlocked()? LocaleController.getString("UnblockContact", R.string.UnblockContact) : LocaleController.getString("BlockContact", R.string.BlockContact), 0);
            item.addSubItem(ID_DELETE_CONTACT, LocaleController.getString("DeleteContact", R.string.DeleteContact), 0);
        }


    }

    @Override
    protected void onDialogDismiss(Dialog dialog) {
        if (listView != null) {
            listView.invalidateViews();
        }
    }

    @Override
    public void didSelectDialog(DialogsActivity fragment, long dialog_id, boolean param) {
        if (dialog_id != 0) {
            Bundle args = new Bundle();
            args.putBoolean("scrollToTopOnResume", true);
            int lower_part = (int) dialog_id;
            if (lower_part != 0) {
                if (lower_part > 0) {
                    args.putInt("user_id", lower_part);
                } else if (lower_part < 0) {
                    args.putInt("chat_id", -lower_part);
                }
            } else {
                args.putInt("enc_id", (int) (dialog_id >> 32));
            }

            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.closeChats);
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
            presentFragment(new ChatActivity(args), true);
            removeSelfFromStack();
            TLRPC.User user = MessagesController.getInstance().getUser(user_id);
            SendMessagesHelper.getInstance().sendMessageContact(user, dialog_id, null, null);
        }
    }

    private class ListAdapter extends RecyclerListView.Adapter {
        private Context mContext;

        private class Holder extends RecyclerView.ViewHolder {

            public Holder(View itemView) {
                super(itemView);
            }
        }

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = null;
            switch (viewType) {
                case typeEmpty:
                    view = new EmptyCell(mContext);
                    break;
                case typeDivider:
                    view = new DividerCell(mContext);
                    view.setPadding(AndroidUtilities.dp(72), 0, 0, 0);
                    break;
                case typeTextDetailCell:
                    view = new TextDetailCell(mContext) {
                        @Override
                        public boolean onTouchEvent(MotionEvent event) {
                            if (Build.VERSION.SDK_INT >= 21 && getBackground() != null) {
                                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                                    getBackground().setHotspot(event.getX(), event.getY());
                                }
                            }
                            return super.onTouchEvent(event);
                        }
                    };
                    break;
                case typeTextCell:
                    view = new TextCell(mContext) {
                        @Override
                        public boolean onTouchEvent(MotionEvent event) {
                            if (Build.VERSION.SDK_INT >= 21 && getBackground() != null) {
                                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                                    getBackground().setHotspot(event.getX(), event.getY());
                                }
                            }
                            return super.onTouchEvent(event);
                        }
                    };
                    break;
                case typeContactCell:
                    view = new UserCell(mContext, 61, 0) {
                        @Override
                        public boolean onTouchEvent(MotionEvent event) {
                            if (Build.VERSION.SDK_INT >= 21 && getBackground() != null) {
                                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                                    getBackground().setHotspot(event.getX(), event.getY());
                                }
                            }
                            return super.onTouchEvent(event);
                        }
                    };
                    break;
                case typeSection:
                    view = new ShadowSectionCell(mContext);
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int i) {
            boolean checkBackground = true;
            switch (holder.getItemViewType()) {
                case typeEmpty:
                    if (i == emptyRowChat || i == emptyRowChat2) {
                        ((EmptyCell) holder.itemView).setHeight(AndroidUtilities.dp(8));
                    } else {
                        ((EmptyCell) holder.itemView).setHeight(AndroidUtilities.dp(14)); // was 36 when we used the "writeButton" for taking photos etc.
                    }
                    break;
                case typeTextDetailCell:
                   TextDetailCell textDetailCell = (TextDetailCell) holder.itemView;
                   break;
                case typeTextCell:
                    TextCell textCell = (TextCell) holder.itemView;
                    textCell.setTextColor(0xff212121);
                    if (i == changeNameRow) {
                        textCell.setText(LocaleController.getString("EditName", R.string.EditName));
                    } else if (i == startChatRow) {
                        textCell.setText(LocaleController.getString("NewChat", R.string.NewChat));
                    } else if (i == settingsNotificationsRow) {
                        textCell.setTextAndIcon(LocaleController.getString("NotificationsAndSounds", R.string.NotificationsAndSounds), R.drawable.menu_settings);
                    } else if (i == addMemberRow) {
                        textCell.setText(LocaleController.getString("AddMember", R.string.AddMember));
                    }
                    break;
                case typeContactCell:
                    UserCell userCell = ((UserCell) holder.itemView);
                    int curr_user_index = i - firstMemberRow;
                    if(curr_user_index>=0 && curr_user_index<sortedUserIds.length) {
                        int curr_user_id = sortedUserIds[curr_user_index];
                        MrContact mrContact = MrMailbox.getContact(curr_user_id);
                            userCell.setData(mrContact, curr_user_index==0? R.drawable.menu_newgroup : 0);
                    }
                    break;

                default:
                    checkBackground = false;
            }
            if (checkBackground) {
                boolean enabled = false;
                if (user_id != 0) {
                    enabled =  i == settingsNotificationsRow
                            || i == changeNameRow
                            || i == startChatRow;
                } else if (chat_id != 0) {
                    enabled =  i == settingsNotificationsRow
                            || i == changeNameRow
                            || i == addMemberRow
                            || (i >= firstMemberRow && i <= lastMemberRow);
                }
                if (enabled) {
                    if (holder.itemView.getBackground() == null) {
                        holder.itemView.setBackgroundResource(R.drawable.list_selector);
                    }
                } else {
                    if (holder.itemView.getBackground() != null) {
                        holder.itemView.setBackgroundDrawable(null);
                    }
                }
            }
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == emptyRow || i == emptyRowChat || i == emptyRowChat2) {
                return typeEmpty;
            } else if (i == sectionRow || i == userSectionRow) {
                return typeDivider;
            } else if ( i == changeNameRow || i==startChatRow || i == settingsNotificationsRow || i == addMemberRow) {
                return typeTextCell;
            } else if (i >= firstMemberRow && i <= lastMemberRow) {
                return typeContactCell;
            } else if(i==membersSectionRow) {
                return typeSection;
            }
            return 0;
        }
    }
}
