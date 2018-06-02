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

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.b44t.messenger.Components.Browser;
import com.b44t.messenger.aosp.LinearLayoutManager;
import com.b44t.messenger.aosp.RecyclerView;
import com.b44t.messenger.ActionBar.BackDrawable;
import com.b44t.messenger.ActionBar.SimpleTextView;
import com.b44t.messenger.Cells.ChatActionCell;
import com.b44t.messenger.ActionBar.ActionBar;
import com.b44t.messenger.ActionBar.ActionBarMenu;
import com.b44t.messenger.ActionBar.ActionBarMenuItem;
import com.b44t.messenger.Cells.ChatMessageCell;
import com.b44t.messenger.Cells.ChatUnreadCell;
import com.b44t.messenger.ActionBar.BaseFragment;
import com.b44t.messenger.Components.ChatActivityEnterView;
import com.b44t.messenger.Components.ChatAttachAlert;
import com.b44t.messenger.Components.ChatAvatarContainer;
import com.b44t.messenger.Components.LayoutHelper;
import com.b44t.messenger.Components.NumberTextView;
import com.b44t.messenger.Components.RecyclerListView;
import com.b44t.messenger.Components.SizeNotifierFrameLayout;
import com.b44t.messenger.ActionBar.Theme;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

@SuppressWarnings("unchecked")
public class ChatActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, ChatlistActivity.ChatlistActivityDelegate,
        PhotoViewer.PhotoViewerProvider {

    // data
    private long  dialog_id;
    public MrChat m_mrChat = new MrChat(0);
    private boolean m_isChatWithDeaddrop /*, m_isDeaddropInChatlist*/;
    private int[] m_msglist = {};

    // the list view
    private RecyclerListView                chatListView;
    private static final int                ROWTYPE_MESSAGE_CELL    = 0;
    private static final int                ROWTYPE_DATE_HEADLINE   = 1;
    private static final int                ROWTYPE_UNREAD_HEADLINE = 2;
    private int                             markerUnreadMessageId = 0; // if set, the "unread messages" headline is show before this message
    private int                             markerUnreadCount     = 0;

    // misc
    private FrameLayout bottomOverlay;
    private ChatActivityEnterView chatActivityEnterView;
    private ActionBarMenuItem menuItem;
    private ActionBarMenuItem headerItem;
    private ActionBarMenuItem searchItem;
    private LinearLayoutManager chatLayoutManager;
    private ChatActivityAdapter chatAdapter;
    private TextView bottomOverlayChatText;
    private FrameLayout bottomOverlayChat;
    private FrameLayout emptyViewContainer;
    private ArrayList<View> actionModeViews = new ArrayList<>();
    private ChatAvatarContainer avatarContainer;
    private NumberTextView selectedMessagesCountTextView;
    private SimpleTextView actionModeTextView;
    private SimpleTextView actionModeSubTextView;
    private TextView muteMenuEntry;
    private boolean m_canMute;
    private FrameLayout pagedownButton;
    private boolean pagedownButtonShowedByScroll;
    private TextView pagedownButtonCounter;
    private ChatAttachAlert chatAttachAlert;

    private ObjectAnimator pagedownButtonAnimation;

    private boolean openSearchKeyboard;

    private boolean paused = true;
    private boolean wasPaused = false;
    private SparseIntArray markseenWhenResume = new SparseIntArray();

    private boolean scrollToTopOnResume;
    private boolean forceScrollToTop;
    private boolean scrollToTopUnReadOnResume;

    private HashMap<Integer, Integer> selectedMessagesIds = new HashMap<>();

    private boolean firstLoading = true;

    private int startLoadFromMessageId;
    private int returnToMessageId;

    private boolean first = true;

    private MessageObject scrollToMessage;
    private int scrollToMessagePosition = -10000;

    private int highlightMessageId = 0;

    private String currentPicturePath;

    private String startVideoEdit = null;

    private final static int ID_COPY = 10;
    private final static int ID_FORWARD = 11;
    private final static int ID_DELETE_MESSAGES = 12;
    private final static int ID_LEAVE_GROUP = 13;
    private final static int ID_ATTACH = 14;
    private final static int ID_SHOW_PROFILE = 15;
    private final static int ID_DELETE_CHAT = 16;
    private final static int ID_ARCHIVE_CHAT = 17;
    private final static int ID_MUTE = 18;
    private final static int ID_REPLY = 19;
    private final static int ID_INFO = 20;
    private final static int ID_SAVE_TO_XX = 21;
    private final static int ID_SHARE = 22;
    private final static int ID_OPEN = 23;
    private final static int ID_SEARCH = 40;
    private final static int ID_SHOW_QR = 50;
    private final static int ID_CHAT_COMPOSE_PANEL = 1000;
    private TextView m_replyMenuItem, m_infoMenuItem, m_saveToXXMenuItem, m_openMenuItem, m_shareMenuItem;

    private static final String TAG = "ChatActivity";

    public ChatActivity(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        dialog_id = arguments.getInt("chat_id", 0);
        m_mrChat = MrMailbox.getChat((int)dialog_id);
        m_isChatWithDeaddrop = m_mrChat.getId()==MrChat.MR_CHAT_ID_DEADDROP;

        /*m_isDeaddropInChatlist = false;
        if( m_isChatWithDeaddrop && MrMailbox.getConfigInt("show_deaddrop", 0)!=0 ) {
            m_isDeaddropInChatlist = true;
        }*/

        startLoadFromMessageId = arguments.getInt("message_id", 0);
        scrollToTopOnResume = arguments.getBoolean("scrollToTopOnResume", false);

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.dialogsNeedReload);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.didReceivedNewMessages);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.closeChats);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.messagesSentOrRead);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.messagesDeleted);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.messageSendError);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.contactsDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioProgressDidChanged);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioDidReset);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioPlayStateChanged);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.blockedUsersDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileNewChunkAvailable);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioDidStarted);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.waveformCalculated);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.notificationsSettingsUpdated);

        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                updateMsglist();
                messagesDidLoaded();
            }
        });

        return true;
    }

    private void updateMsglist()
    {
        m_msglist = MrMailbox.getChatMsgs((int)dialog_id, MrMailbox.MR_GCM_ADDDAYMARKER,
                markerUnreadMessageId /*add a marker before this ID*/);
    }

    private void messagesDidLoaded()
    {
        firstLoading = false;
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (parentLayout != null) {
                    parentLayout.resumeDelayedFragmentAnimation();
                }
            }
        });

        if (m_isChatWithDeaddrop) {
            updateBottomOverlay();
        }

        {
            if (chatListView != null) {
                if (first || scrollToTopOnResume || forceScrollToTop) {
                    forceScrollToTop = false;
                    chatAdapter.notifyDataSetChanged();
                    if (scrollToMessage != null) {
                        int yOffset;
                        if (scrollToMessagePosition == -9000) {
                            yOffset = Math.max(0, (chatListView.getHeight() - scrollToMessage.getApproximateHeight()) / 2);
                        } else if (scrollToMessagePosition == -10000) {
                            yOffset = 0;
                        } else {
                            yOffset = scrollToMessagePosition;
                        }
                        if ( m_msglist.length>0 ) {
                            /*if (messages.get(messages.size() - 1) == scrollToMessage || messages.get(messages.size() - 2) == scrollToMessage) {
                                chatLayoutManager.scrollToPositionWithOffset(0, -chatListView.getPaddingTop() - AndroidUtilities.dp(7) + yOffset);
                            } else {
                                chatLayoutManager.scrollToPositionWithOffset(chatAdapter.messagesStartRow + messages.size() - messages.indexOf(scrollToMessage) - 1, -chatListView.getPaddingTop() - AndroidUtilities.dp(7) + yOffset);
                            }*/
                        }
                        chatListView.invalidate();
                        if (scrollToMessagePosition == -10000 || scrollToMessagePosition == -9000) {
                            showPagedownButton(true, true);
                        }
                        scrollToMessagePosition = -10000;
                        scrollToMessage = null;
                    } else {
                        moveScrollToLastMessage();
                    }
                }

                if (paused) {
                    scrollToTopOnResume = true;
                    if (scrollToMessage != null) {
                        scrollToTopUnReadOnResume = true;
                    }
                }

                if (first) {
                    if (chatListView != null) {
                        chatListView.setEmptyView(emptyViewContainer);
                    }
                }
            } else {
                scrollToTopOnResume = true;
                if (scrollToMessage != null) {
                    scrollToTopUnReadOnResume = true;
                }
            }
        }

        if (first && m_msglist.length > 0) {
            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    MrMailbox.marknoticedChat((int)dialog_id);
                    NotificationsController.getInstance().removeSeenMessages();
                }
            }, 700);
            first = false;
        }

        if( startLoadFromMessageId!=0 ) {
            scrollToMessageId(startLoadFromMessageId, true);
        }
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (chatActivityEnterView != null) {
            chatActivityEnterView.onDestroy();
        }

        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.dialogsNeedReload);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didReceivedNewMessages);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.closeChats);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messagesSentOrRead);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messagesDeleted);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messageSendError);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.contactsDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioProgressDidChanged);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioDidReset);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.blockedUsersDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.FileNewChunkAvailable);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioDidStarted);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.waveformCalculated);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.notificationsSettingsUpdated);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioPlayStateChanged);

        AndroidUtilities.removeAdjustResize(getParentActivity(), classGuid);

        if (chatAttachAlert != null) {
            chatAttachAlert.onDestroy();
        }
        AndroidUtilities.unlockOrientation(getParentActivity());
        /*MessageObject messageObject = MediaController.getInstance().getPlayingMessageObject();
        if (messageObject != null && !messageObject.isMusic()) {
            MediaController.getInstance().stopAudio();
        }*/
        /*if (ChatObject.isChannel(currentChat)) {
            MessagesController.getInstance().startShortPoll(currentChat.id, true);
        }*/
    }

    @Override
    public View createView(final Context context) {

        selectedMessagesIds.clear();

        hasOwnBackground = true;
        if (chatAttachAlert != null){
            chatAttachAlert.onDestroy();
            chatAttachAlert = null;
        }

        Theme.loadRecources(context);
        Theme.loadChatResources(context);

        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(final int id) {
                if (id == -1) {
                    if (actionBar.isActionModeShowed()) {
                        // finish selection mode
                        selectedMessagesIds.clear();
                        actionBar.hideActionMode();
                        updateVisibleRowsFast();
                    } else {
                        finishFragment();
                    }
                } else if (id == ID_COPY) {
                    String str = "";
                    int previousMid = 0;
                    ArrayList<Integer> ids = new ArrayList<>(selectedMessagesIds.keySet());
                    Collections.sort(ids);
                    for (int b = 0; b < ids.size(); b++) {
                        Integer messageId = ids.get(b);
                        if (str.length() != 0) {
                            str += "\n\n";
                        }
                        str += getMessageContent(messageId, previousMid);
                        previousMid = messageId;
                    }
                    AndroidUtilities.addToClipboard(str);
                    selectedMessagesIds.clear();
                    actionBar.hideActionMode();
                    updateVisibleRowsFast();
                    AndroidUtilities.showDoneHint(getParentActivity());
                } else if (id == ID_DELETE_MESSAGES) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    createDeleteMessagesAlert();
                }
                else if (id == ID_FORWARD)
                {
                    Bundle args = new Bundle();
                    args.putBoolean("onlySelect", true);
                    args.putString("onlySelectTitle", context.getString(R.string.ForwardToTitle));
                    args.putString("selectAlertString", context.getString(R.string.ForwardMessagesTo));
                    ChatlistActivity fragment = new ChatlistActivity(args);
                    fragment.setDelegate(ChatActivity.this);
                    presentFragment(fragment); // this results in a call to didSelectChat()
                }
                else if( id == ID_SHOW_PROFILE )
                {
                    Bundle args = new Bundle();
                    if( m_mrChat.isGroup() ) {
                        args.putInt("chat_id",  m_mrChat.getId());
                    }
                    else {
                        int[] contact_ids = MrMailbox.getChatContacts(m_mrChat.getId());
                        if( contact_ids.length==0) {
                            return; // should not happen
                        }
                        args.putInt("user_id", contact_ids[0]);
                    }
                    ProfileActivity fragment = new ProfileActivity(args);
                    presentFragment(fragment);
                }
                else if( id == ID_LEAVE_GROUP )
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            MrMailbox.removeContactFromChat(m_mrChat.getId(), MrContact.MR_CONTACT_ID_SELF);
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces, MrMailbox.UPDATE_MASK_CHAT_MEMBERS);
                            AndroidUtilities.showDoneHint(context);
                        }
                    });
                    builder.setNegativeButton(R.string.Cancel, null);
                    builder.setMessage(AndroidUtilities.replaceTags(context.getString(R.string.AskLeaveGroup)));
                    showDialog(builder.create());
                }
                else if ( id == ID_ARCHIVE_CHAT)
                {
                    int do_archive = m_mrChat.getArchived()==0? 1: 0;
                    MrMailbox.archiveChat((int)dialog_id, do_archive);
                    AndroidUtilities.showDoneHint(context);
                    if( do_archive == 1 ) {
                        finishFragment();
                    }
                }
                else if ( id == ID_DELETE_CHAT)
                {
                    // as the history may be a mix of messenger-messages and emails, it is not safe to delete it.
                    // the user can delete explicit messages or use his email programm to delete masses.
                    if (getParentActivity() == null) {
                        return;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setMessage(context.getString(R.string.AreYouSureDeleteThisChat));
                    builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            MrMailbox.deleteChat((int)dialog_id);
                            AndroidUtilities.showDoneHint(getParentActivity());
                            finishFragment();
                        }
                    });
                    builder.setNegativeButton(R.string.Cancel, null);
                    showDialog(builder.create());
                } else if (id == ID_MUTE) {
                    toggleMute();
                } else if (id == ID_REPLY) {
                    if( m_isChatWithDeaddrop ){
                        if( selectedMessagesIds!=null && selectedMessagesIds.size()==1) {
                            ArrayList<Integer> ids = new ArrayList<>(selectedMessagesIds.keySet());
                            createChatByDeaddropMsgId(ids.get(0));
                        }
                    }
                    else{
                        actionBar.hideActionMode();
                        updateVisibleRowsFast();
                    }
                } else if (id == ID_INFO) {
                    String info_str = MrMailbox.getMsgInfo(getFirstSelectedId());
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setMessage(AndroidUtilities.replaceTags(info_str));
                    builder.setPositiveButton(R.string.OK, null);
                    showDialog(builder.create());
                    actionBar.hideActionMode();
                    updateVisibleRowsFast();
                }
                else if( id== ID_SAVE_TO_XX )
                {
                    AndroidUtilities.saveMessageFileToExt(getParentActivity(), getFirstSelectedId());
                    actionBar.hideActionMode();
                    updateVisibleRowsFast();
                }
                else if( id == ID_OPEN )
                {
                    // for files, that cannot be handled internally (documents ...), this is equal to a normal click
                    AndroidUtilities.openForViewOrShare(getParentActivity(), getFirstSelectedId(), Intent.ACTION_VIEW);
                    actionBar.hideActionMode();
                    updateVisibleRowsFast();
                }
                else if( id== ID_SHARE )
                {
                    AndroidUtilities.openForViewOrShare(getParentActivity(), getFirstSelectedId(), Intent.ACTION_SEND);
                    actionBar.hideActionMode();
                    updateVisibleRowsFast();
                }
                else if (id == ID_ATTACH) {
                    if (getParentActivity() == null) {
                        return;
                    }

                    createChatAttachView();
                    chatAttachAlert.loadGalleryPhotos();
                    if (Build.VERSION.SDK_INT == 21 || Build.VERSION.SDK_INT == 22) {
                        chatActivityEnterView.closeKeyboard();
                    }
                    chatAttachAlert.init(ChatActivity.this);
                    showDialog(chatAttachAlert);
                } else if (id == ID_SEARCH) {
                    openSearchWithText("");
                }
                else if( id == ID_SHOW_QR ) {
                    Intent intent2 = new Intent(getParentActivity(), QRshowActivity.class);
                    Bundle b = new Bundle();
                    b.putInt("chat_id", (int)dialog_id);
                    intent2.putExtras(b);
                    getParentActivity().startActivity(intent2);
                }
            }
        });

        if( m_isChatWithDeaddrop /*&& !m_isDeaddropInChatlist*/ ) {
            actionBar.setTitle(context.getString(R.string.Deaddrop));
        }
        else {
            avatarContainer = new ChatAvatarContainer(context, this);
            actionBar.addView(avatarContainer, 0, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.START, ActionBar.AVATAR_AFTER_BACK_X, 0, 40, 0));
        }

        ActionBarMenu menu = actionBar.createMenu();

        searchItem = menu.addItem(0, R.drawable.ic_ab_search).setIsSearchField(true, false).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchCollapse() {
                if( avatarContainer != null ) {
                    avatarContainer.setVisibility(View.VISIBLE);
                }
                headerItem.setVisibility(View.VISIBLE);
                searchItem.setVisibility(View.GONE);
                highlightMessageId = 0;
                m_searching = false;
                updateVisibleRowsFast();
                //scrollToLastMessage(false); -- wo do not scroll down; this does not make sense if the user has just selected a different position by "search"
                updateBottomOverlay();
            }

            @Override
            public void onSearchExpand() {
                if (!openSearchKeyboard) {
                    return;
                }
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        searchItem.getSearchField().requestFocus();
                        AndroidUtilities.showKeyboard(searchItem.getSearchField());
                    }
                }, 300); // don't know why, but this delay is needed to show up the keyboard (otherwise, it is displayed after a click)
            }

            @Override
            public void onTextChanged(EditText editText) {
                handleSearch(SEARCH_QUERY, editText.getText().toString());
            }

            @Override
            public void onUpPressed() {
                handleSearch(SEARCH_UP, null);
            }

            @Override
            public void onDownPressed() {
                handleSearch(SEARCH_DOWN, null);
            }
        });
        searchItem.getSearchField().setHint(context.getString(R.string.Search));
        searchItem.setVisibility(View.GONE);


        headerItem = menu.addItem(0, R.drawable.ic_ab_other);
        headerItem.addSubItem(ID_SEARCH, context.getString(R.string.Search));

        m_canMute = true;
        if( m_isChatWithDeaddrop /*&& !m_isDeaddropInChatlist*/ ) {
            m_canMute = false;
        }

        if( m_canMute ) {
            muteMenuEntry = headerItem.addSubItem(ID_MUTE, null);
        }

        //if( !m_isChatWithDeaddrop ) {
            //headerItem.addSubItem(ID_ATTACH, context.getString(R.string.AttachFiles), 0); // "Attach" means "Attach to chat", not "Attach to message" (which is not possible)
        //}

        if( !m_isChatWithDeaddrop ) {
            headerItem.addSubItem(ID_SHOW_PROFILE, context.getString(R.string.ViewProfile));
            if( m_mrChat.isGroup() ) {
                if ( m_mrChat.isVerified() ) {
                    headerItem.addSubItem(ID_SHOW_QR, context.getString(R.string.QrShowInviteCode));
                }
                headerItem.addSubItem(ID_LEAVE_GROUP, context.getString(R.string.LeaveGroup));
            }
            headerItem.addSubItem(ID_ARCHIVE_CHAT, context.getString(m_mrChat.getArchived()==0? R.string.ArchiveChat : R.string.UnarchiveChat));
            headerItem.addSubItem(ID_DELETE_CHAT, context.getString(R.string.DeleteChat));
        }

        updateTitle();
        if( avatarContainer!=null ) {
            avatarContainer.updateSubtitle();
        }
        updateTitleIcons();

        menuItem = menu.addItem(ID_ATTACH, R.drawable.ic_ab_attach).setAllowCloseAnimation(false); // "menuItem" is added to ChatEnterViewActivity
        menuItem.setBackgroundDrawable(null);

        actionModeViews.clear();

        final ActionBarMenu actionMode = actionBar.createActionMode();

        selectedMessagesCountTextView = new NumberTextView(actionMode.getContext());
        selectedMessagesCountTextView.setTextSize(18);
        selectedMessagesCountTextView.setTextColor(Theme.ACTION_BAR_ACTION_MODE_TEXT_COLOR);
        actionMode.addView(selectedMessagesCountTextView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 65, 0, 0, 0));
        selectedMessagesCountTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        FrameLayout actionModeTitleContainer = new FrameLayout(context) {

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int width = MeasureSpec.getSize(widthMeasureSpec);
                int height = MeasureSpec.getSize(heightMeasureSpec);

                setMeasuredDimension(width, height);

                actionModeTextView.setTextSize(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 18 : 20);
                actionModeTextView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(24), MeasureSpec.AT_MOST));

                if (actionModeSubTextView.getVisibility() != GONE) {
                    actionModeSubTextView.setTextSize(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 14 : 16);
                    actionModeSubTextView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(20), MeasureSpec.AT_MOST));
                }
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                int height = bottom - top;

                int textTop;
                if (actionModeSubTextView.getVisibility() != GONE) {
                    textTop = (height / 2 - actionModeTextView.getTextHeight()) / 2 + AndroidUtilities.dp(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 2 : 3);
                } else {
                    textTop = (height - actionModeTextView.getTextHeight()) / 2;
                }
                actionModeTextView.layout(0, textTop, actionModeTextView.getMeasuredWidth(), textTop + actionModeTextView.getTextHeight());

                if (actionModeSubTextView.getVisibility() != GONE) {
                    textTop = height / 2 + (height / 2 - actionModeSubTextView.getTextHeight()) / 2 - AndroidUtilities.dp(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 1 : 1);
                    actionModeSubTextView.layout(0, textTop, actionModeSubTextView.getMeasuredWidth(), textTop + actionModeSubTextView.getTextHeight());
                }
            }
        };
        actionMode.addView(actionModeTitleContainer, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 65, 0, 0, 0));
        actionModeTitleContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        actionModeTitleContainer.setVisibility(View.GONE);

        actionModeTextView = new SimpleTextView(context);
        actionModeTextView.setTextSize(18);
        actionModeTextView.setTextColor(Theme.ACTION_BAR_ACTION_MODE_TEXT_COLOR);
        actionModeTextView.setText(context.getString(R.string.Edit));
        actionModeTitleContainer.addView(actionModeTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        actionModeSubTextView = new SimpleTextView(context);
        actionModeSubTextView.setGravity(Gravity.START);
        actionModeSubTextView.setTextColor(Theme.ACTION_BAR_ACTION_MODE_TEXT_COLOR);
        actionModeTitleContainer.addView(actionModeSubTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionModeViews.add(actionMode.addItem(ID_DELETE_MESSAGES, R.drawable.ic_ab_fwd_delete, Theme.ACTION_BAR_MODE_SELECTOR_COLOR, null, AndroidUtilities.dp(54)));
        actionModeViews.add(actionMode.addItem(ID_FORWARD, R.drawable.ic_ab_fwd_forward, Theme.ACTION_BAR_MODE_SELECTOR_COLOR, null, AndroidUtilities.dp(54)));
        ActionBarMenuItem submenu = actionMode.addItem(0, R.drawable.ic_ab_other_grey);
            if( m_isChatWithDeaddrop ) {
                m_replyMenuItem = submenu.addSubItem(ID_REPLY, context.getString(R.string.Reply));
            }
            submenu.addSubItem(ID_COPY, context.getString(R.string.CopyToClipboard));
            m_saveToXXMenuItem = submenu.addSubItem(ID_SAVE_TO_XX, "");
            m_openMenuItem = submenu.addSubItem(ID_OPEN, context.getString(R.string.Open));
            m_shareMenuItem = submenu.addSubItem(ID_SHARE, context.getString(R.string.Share));
            m_infoMenuItem = submenu.addSubItem(ID_INFO, context.getString(R.string.Info));
        actionModeViews.add(submenu);
        checkActionBarMenu();

        fragmentView = new SizeNotifierFrameLayout(context) {

            int inputFieldHeight = 0;

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int widthSize = MeasureSpec.getSize(widthMeasureSpec);
                int heightSize = MeasureSpec.getSize(heightMeasureSpec);

                setMeasuredDimension(widthSize, heightSize);
                heightSize -= getPaddingTop();

                int keyboardSize = getKeyboardHeight();

                if (keyboardSize <= AndroidUtilities.dp(20)) {
                    heightSize -= chatActivityEnterView.getEmojiPadding();
                }

                int childCount = getChildCount();

                measureChildWithMargins(chatActivityEnterView, widthMeasureSpec, 0, heightMeasureSpec, 0);
                inputFieldHeight = m_searching? 0 : chatActivityEnterView.getMeasuredHeight();

                for (int i = 0; i < childCount; i++) {
                    View child = getChildAt(i);
                    if (child == null || child.getVisibility() == GONE || child == chatActivityEnterView) {
                        continue;
                    }
                    if (child == chatListView ) {
                        int contentWidthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
                        int contentHeightSpec = MeasureSpec.makeMeasureSpec(Math.max(AndroidUtilities.dp(10), heightSize - inputFieldHeight + AndroidUtilities.dp(2 + (chatActivityEnterView.isTopViewVisible() ? 48 : 0))), MeasureSpec.EXACTLY);
                        child.measure(contentWidthSpec, contentHeightSpec);
                    } else if (child == emptyViewContainer) {
                        int contentWidthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
                        int contentHeightSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
                        child.measure(contentWidthSpec, contentHeightSpec);
                    } else if (chatActivityEnterView.isPopupView(child)) {
                        child.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(child.getLayoutParams().height, MeasureSpec.EXACTLY));
                    } else {
                        measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                    }
                }
            }

            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                final int count = getChildCount();

                int paddingBottom = getKeyboardHeight() <= AndroidUtilities.dp(20) ? chatActivityEnterView.getEmojiPadding() : 0;
                setBottomClip(paddingBottom);

                for (int i = 0; i < count; i++) {
                    final View child = getChildAt(i);
                    if (child.getVisibility() == GONE) {
                        continue;
                    }
                    final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                    final int width = child.getMeasuredWidth();
                    final int height = child.getMeasuredHeight();

                    int childLeft;
                    int childTop;

                    int gravity = lp.gravity;
                    if (gravity == -1) {
                        gravity = Gravity.TOP | Gravity.START;
                    }

                    final int absoluteGravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                    final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

                    switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                        case Gravity.CENTER_HORIZONTAL:
                            childLeft = (r - l - width) / 2 + lp.leftMargin - lp.rightMargin;
                            break;
                        case Gravity.RIGHT:
                            childLeft = r - width - lp.rightMargin;
                            break;
                        case Gravity.LEFT:
                        default:
                            childLeft = lp.leftMargin;
                    }

                    switch (verticalGravity) {
                        case Gravity.TOP:
                            childTop = lp.topMargin + getPaddingTop();
                            break;
                        case Gravity.CENTER_VERTICAL:
                            childTop = ((b - paddingBottom) - t - height) / 2 + lp.topMargin - lp.bottomMargin;
                            break;
                        case Gravity.BOTTOM:
                            childTop = ((b - paddingBottom) - t) - height - lp.bottomMargin;
                            break;
                        default:
                            childTop = lp.topMargin;
                    }

                    if (child == pagedownButton) {
                        childTop -= m_searching? 0 : chatActivityEnterView.getMeasuredHeight();
                    } else if (child == emptyViewContainer) {
                        childTop -= inputFieldHeight / 2;
                    } else if (chatActivityEnterView.isPopupView(child)) {
                        childTop = chatActivityEnterView.getBottom();
                    } else if (child == chatListView ) {
                        if (chatActivityEnterView.isTopViewVisible()) {
                            childTop -= AndroidUtilities.dp(48);
                        }
                    }
                    child.layout(childLeft, childTop, childLeft + width, childTop + height);
                }

                updateMessagesVisisblePart();
                notifyHeightChanged();
            }
        };

        SizeNotifierFrameLayout contentView = (SizeNotifierFrameLayout) fragmentView;

        contentView.setBackgroundImage(ApplicationLoader.getCachedWallpaper());

        emptyViewContainer = new FrameLayout(context);
        emptyViewContainer.setVisibility(View.INVISIBLE);
        contentView.addView(emptyViewContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        emptyViewContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

            TextView emptyView = new TextView(context);

            if( m_mrChat.isUnpromoted() ) {
                emptyView.setText(context.getString(R.string.MsgNewGroupDraftHint));
                emptyView.setGravity(Gravity.START);
            }
            else if( !m_mrChat.isGroup() && !m_mrChat.isSelfTalk() ){
                String name = m_mrChat.getName();
                emptyView.setText(AndroidUtilities.replaceTags(String.format(context.getString(R.string.NoMessagesHint), name, name)));
                emptyView.setGravity(Gravity.START);
            }
            else {
                emptyView.setText(context.getString(R.string.NoMessages));
                emptyView.setGravity(Gravity.CENTER);
            }

            emptyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            emptyView.setTextColor(Theme.CHAT_EMPTY_VIEW_TEXT_COLOR);
            emptyView.setBackgroundResource(R.drawable.system);
            emptyView.getBackground().setColorFilter(Theme.colorFilter);
            emptyView.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(2), AndroidUtilities.dp(10), AndroidUtilities.dp(3));
            FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER);
            fl.leftMargin = AndroidUtilities.dp(24);
            fl.rightMargin = AndroidUtilities.dp(24);
            emptyViewContainer.addView(emptyView, fl);

        if (chatActivityEnterView != null) {
            chatActivityEnterView.onDestroy();
        }

        chatListView = new RecyclerListView(context) {
            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                super.onLayout(changed, l, t, r, b);
                forceScrollToTop = false;
            }
        };
        chatListView.setTag(1);
        chatListView.setVerticalScrollBarEnabled(true);
        chatListView.setAdapter(chatAdapter = new ChatActivityAdapter(context));
        chatListView.setClipToPadding(false);
        chatListView.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(3));
        chatListView.setItemAnimator(null);
        chatListView.setLayoutAnimation(null);
        chatLayoutManager = new LinearLayoutManager(context) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };
        chatLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        chatLayoutManager.setStackFromEnd(true);
        chatListView.setLayoutManager(chatLayoutManager);
        contentView.addView(chatListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        chatListView.setOnItemLongClickListener(new RecyclerListView.OnItemLongClickListener() {
            @Override
            public boolean onItemClick(View view, int position) {
                if (!actionBar.isActionModeShowed()) {
                    handleClick(view, true);
                    return true;
                }
                return false;
            }
        });
        chatListView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (actionBar.isActionModeShowed()) {
                    processRowSelect(view);
                    return;
                }
                handleClick(view, false);
            }
        });
        chatListView.setOnScrollListener(new RecyclerView.OnScrollListener() {

            private float totalDy = 0;
            private final int scrollValue = AndroidUtilities.dp(100);

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING && highlightMessageId != 0) {
                    highlightMessageId = 0;
                    updateVisibleRows();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

                int firstVisibleItem = chatLayoutManager.findFirstVisibleItemPosition();
                int visibleItemCount = firstVisibleItem == RecyclerView.NO_POSITION ? 0 : Math.abs(chatLayoutManager.findLastVisibleItemPosition() - firstVisibleItem) + 1;
                if (visibleItemCount > 0) {
                    int totalItemCount = chatAdapter.getItemCount();
                    if (firstVisibleItem + visibleItemCount == totalItemCount ) {
                        showPagedownButton(false, true);
                    } else {
                        if (dy > 0) {
                            if (pagedownButton.getTag() == null) {
                                totalDy += dy;
                                if (totalDy > scrollValue) {
                                    totalDy = 0;
                                    showPagedownButton(true, true);
                                    pagedownButtonShowedByScroll = true;
                                }
                            }
                        } else {
                            if (pagedownButtonShowedByScroll && pagedownButton.getTag() != null) {
                                totalDy += dy;
                                if (totalDy < -scrollValue) {
                                    showPagedownButton(false, true);
                                    totalDy = 0;
                                }
                            }
                        }
                    }
                }
                updateMessagesVisisblePart();
            }
        });

        chatListView.setOnInterceptTouchListener(new RecyclerListView.OnInterceptTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent event) {
                if (actionBar.isActionModeShowed()) {
                    return false;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    int x = (int) event.getX();
                    int y = (int) event.getY();
                    int count = chatListView.getChildCount();
                    for (int a = 0; a < count; a++) {
                        View view = chatListView.getChildAt(a);
                        int top = view.getTop();
                        int bottom = view.getBottom();
                        if (top > y || bottom < y) {
                            continue;
                        }
                        if (!(view instanceof ChatMessageCell)) {
                            break;
                        }
                        //final ChatMessageCell cell = (ChatMessageCell) view;
                        //final MessageObject messageObject = cell.getMessageObject();
                        //if (messageObject == null || messageObject.isSending() || !messageObject.isSecretPhoto() || !cell.getPhotoImage().isInsideImage(x, y - top)) {

                        //  =====>>  the condition above is always true due to !messageObject.isSecretPhoto(), however, the loop is important, note the continue above

                            break;

                        /*
                        }
                        File file = FileLoader.getPathToMessage(messageObject.messageOwner);
                        if (!file.exists()) {
                            break;
                        }
                        startX = x;
                        startY = y;
                        chatListView.setOnItemClickListener(null);
                        openSecretPhotoRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (openSecretPhotoRunnable == null) {
                                    return;
                                }
                                chatListView.requestDisallowInterceptTouchEvent(true);
                                chatListView.setOnItemLongClickListener(null);
                                chatListView.setLongClickable(false);
                                openSecretPhotoRunnable = null;
                                if (sendSecretMessageRead(messageObject)) {
                                    cell.invalidate();
                                }
                                SecretPhotoViewer.getInstance().setParentActivity(getParentActivity());
                                SecretPhotoViewer.getInstance().openPhoto(messageObject);
                            }
                        };
                        AndroidUtilities.runOnUIThread(openSecretPhotoRunnable, 100);
                        return true;
                        */
                    }
                }
                return false;
            }
        });

        pagedownButton = new FrameLayout(context);
        pagedownButton.setVisibility(View.INVISIBLE);
        contentView.addView(pagedownButton, LayoutHelper.createFrame(46, 59, Gravity.END | Gravity.BOTTOM, 0, 0, 7, 5));
        pagedownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (returnToMessageId > 0) {
                    scrollToMessageId(returnToMessageId, true);
                } else {
                    scrollToLastMessage(true);
                }
            }
        });

        ImageView pagedownButtonImage = new ImageView(context);
        pagedownButtonImage.setImageResource(R.drawable.pagedown);
        pagedownButton.addView(pagedownButtonImage, LayoutHelper.createFrame(46, 46, Gravity.START | Gravity.BOTTOM));

        pagedownButtonCounter = new TextView(context);
        pagedownButtonCounter.setVisibility(View.INVISIBLE);
        pagedownButtonCounter.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        pagedownButtonCounter.setTextColor(0xffffffff);
        pagedownButtonCounter.setGravity(Gravity.CENTER);
        pagedownButtonCounter.setBackgroundResource(R.drawable.chat_badge);
        pagedownButtonCounter.setMinWidth(AndroidUtilities.dp(23));
        pagedownButtonCounter.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), AndroidUtilities.dp(1));
        pagedownButton.addView(pagedownButtonCounter, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 23, Gravity.TOP | Gravity.CENTER_HORIZONTAL));

        chatActivityEnterView = new ChatActivityEnterView(getParentActivity(), contentView, this, true);
        chatActivityEnterView.setDialogId(dialog_id);
        chatActivityEnterView.addToAttachLayout(menuItem);

        //noinspection ResourceType
        chatActivityEnterView.setId(ID_CHAT_COMPOSE_PANEL);

        contentView.addView(chatActivityEnterView, contentView.getChildCount() - 1, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.BOTTOM));
        chatActivityEnterView.setDelegate(new ChatActivityEnterView.ChatActivityEnterViewDelegate() {
            @Override
            public void onMessageSend(CharSequence message) {
                moveScrollToLastMessage();
            }

            @Override
            public void onTextChanged(final CharSequence text, boolean bigChange) {
                MediaController.getInstance().setInputFieldHasText(text != null && text.length() != 0);
            }

            @Override
            public void needSendTyping() {
                MrMailbox.sendTyping(dialog_id, 0, classGuid);
            }

            @Override
            public void onWindowSizeChanged(int size) {
            }
        });

        bottomOverlay = new FrameLayout(context);
        bottomOverlay.setVisibility(View.INVISIBLE);
        bottomOverlay.setFocusable(true);
        bottomOverlay.setFocusableInTouchMode(true);
        bottomOverlay.setClickable(true);
        bottomOverlay.setBackgroundResource(R.drawable.compose_panel);
        bottomOverlay.setPadding(0, AndroidUtilities.dp(3), 0, 0);
        contentView.addView(bottomOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 51, Gravity.BOTTOM));

        TextView bottomOverlayText = new TextView(context);
        bottomOverlayText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        bottomOverlayText.setTextColor(Theme.CHAT_BOTTOM_OVERLAY_TEXT_COLOR);
        bottomOverlay.addView(bottomOverlayText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        bottomOverlayChat = new FrameLayout(context);
        bottomOverlayChat.setBackgroundResource(R.drawable.compose_panel);
        bottomOverlayChat.setPadding(0, AndroidUtilities.dp(3), 0, 0);
        bottomOverlayChat.setVisibility(View.INVISIBLE);
        contentView.addView(bottomOverlayChat, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 51, Gravity.BOTTOM));
        bottomOverlayChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getParentActivity() == null) {
                    return;
                }
                // handle clicks on the bottom-overlay
            }
        });

        bottomOverlayChatText = new TextView(context);
        bottomOverlayChatText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        bottomOverlayChatText.setTextColor(0xffb2b2b2); // same as hintTextColor of the keyboard
        bottomOverlayChat.addView(bottomOverlayChatText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        //chatAdapter.updateRows();
        chatListView.setEmptyView(emptyViewContainer);

        updateBottomOverlay();

        return fragmentView;
    }

    private void createChatAttachView() {
        if (getParentActivity() == null) {
            return;
        }
        if (chatAttachAlert == null) {
            chatAttachAlert = new ChatAttachAlert(getParentActivity());
            chatAttachAlert.setDelegate(new ChatAttachAlert.ChatAttachViewDelegate() {
                @Override
                public void didPressedButton(int button) {
                    if (getParentActivity() == null || button == ChatAttachAlert.ATTACH_BUTTON_IDX_EMPTY) {
                        return;
                    }
                    if (button == ChatAttachAlert.ATTACH_BUTTON_IDX_SENDSELECTED) {
                        chatAttachAlert.dismiss();
                        HashMap<Integer, MediaController.PhotoEntry> selectedPhotos = chatAttachAlert.getSelectedPhotos();
                        if (!selectedPhotos.isEmpty()) {
                            ArrayList<String> photos = new ArrayList<>();
                            ArrayList<String> captions = new ArrayList<>();
                            for (HashMap.Entry<Integer, MediaController.PhotoEntry> entry : selectedPhotos.entrySet()) {
                                MediaController.PhotoEntry photoEntry = entry.getValue();
                                if (photoEntry.imagePath != null) {
                                    photos.add(photoEntry.imagePath);
                                    captions.add(photoEntry.caption != null ? photoEntry.caption.toString() : null);
                                } else if (photoEntry.path != null) {
                                    photos.add(photoEntry.path);
                                    captions.add(photoEntry.caption != null ? photoEntry.caption.toString() : null);
                                }
                                photoEntry.imagePath = null;
                                photoEntry.thumbPath = null;
                                photoEntry.caption = null;
                            }
                            SendMessagesHelper.prepareSendingPhotos(photos, null, dialog_id, captions);
                            m_mrChat.cleanDraft();
                        }
                        return;
                    } else if (chatAttachAlert != null) {
                        chatAttachAlert.dismissWithButtonClick(button);
                    }
                    processSelectedAttach(button);
                }

                @Override
                public View getRevealView() {
                    return menuItem;
                }
            });
        }
    }

    public long getDialogId() {
        return dialog_id;
    }

    public boolean playFirstUnreadVoiceMessage() {
        /* -- maybe we should implement this also for Delta Chat (raise-to-hear if there are unheard messages)
        for (int a = messages.size() - 1; a >= 0; a--) {
            MessageObject messageObject = messages.get(a);
            if (messageObject.isVoice() && messageObject.isContentUnread() && !messageObject.isOut() && messageObject.messageOwner.to_id.channel_id == 0) {
                MediaController.getInstance().setVoiceMessagesPlaylist(MediaController.getInstance().playAudio(messageObject) ? createVoiceMessagesPlaylist(messageObject, true) : null, true);
                return true;
            }
        }*/
        if (Build.VERSION.SDK_INT >= 23 && getParentActivity() != null) {
            if (getParentActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                getParentActivity().requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 3);
                return true;
            }
        }
        return false;
    }

    private void processSelectedAttach(int which) {
        if (which == ChatAttachAlert.ATTACH_BUTTON_IDX_CAMERA ) {
            if (Build.VERSION.SDK_INT >= 23 && getParentActivity().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                getParentActivity().requestPermissions(new String[]{Manifest.permission.CAMERA}, 4);
                return;
            }
            try {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File image = AndroidUtilities.generatePicturePath();
                if (image != null) {
                    if (Build.VERSION.SDK_INT >= 24) {
                        // a file:-url (Uri.fromFile()) is no longer allowed in Nougat, see https://inthecheesefactory.com/blog/how-to-share-access-to-file-with-fileprovider-on-android-nougat/en
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(getParentActivity(), BuildConfig.APPLICATION_ID + ".provider", image));
                        takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    else {
                        // for eg. Kitkat everything but file:-urls crash
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(image));
                    }
                    currentPicturePath = image.getAbsolutePath();
                }
                startActivityForResult(takePictureIntent, RC0_CHAT_IMAGE_CAPTURE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (which == ChatAttachAlert.ATTACH_BUTTON_IDX_GALLERY ) {
            if (Build.VERSION.SDK_INT >= 23 && getParentActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                getParentActivity().requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 4);
                return;
            }
            PhotoAlbumPickerActivity fragment = new PhotoAlbumPickerActivity(false, ChatActivity.this);
            fragment.setDelegate(new PhotoAlbumPickerActivity.PhotoAlbumPickerActivityDelegate() {
                @Override
                public void didSelectPhotos(ArrayList<String> photos, ArrayList<String> captions) {
                    SendMessagesHelper.prepareSendingPhotos(photos, null, dialog_id, captions);
                    m_mrChat.cleanDraft();
                }

                @Override
                public void startPhotoSelectActivity() {
                    /* -- this is the fallback implementation of the gallery selection, see ID_OPEN_SYSTEM_SELECTOR
                    try {
                        Intent videoPickerIntent = new Intent();
                        videoPickerIntent.setType("video/*");
                        videoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
                        videoPickerIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, (long) (1024 * 1024 * 1536));

                        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                        photoPickerIntent.setType("image/*");
                        Intent chooserIntent = Intent.createChooser(photoPickerIntent, null);
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{videoPickerIntent});

                        startActivityForResult(chooserIntent, RC1_CHAT_PICK);
                    } catch (Exception e) {

                    } */
                }

                @Override
                public boolean didSelectVideo(String path) {
                    if (Build.VERSION.SDK_INT >= 16) {
                        return !openVideoEditor(path, true, true);
                    } else {
                        SendMessagesHelper.prepareSendingVideo(path, 0, 0, 0, 0, null, dialog_id);
                        m_mrChat.cleanDraft();
                        return true;
                    }
                }
            });
            presentFragment(fragment);
        } else if (which == ChatAttachAlert.ATTACH_BUTTON_IDX_VIDEO) {
            try {
                if (Build.VERSION.SDK_INT >= 23 && getParentActivity().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    getParentActivity().requestPermissions(new String[]{Manifest.permission.CAMERA}, 4);
                    return;
                }
                Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                File video = AndroidUtilities.generateVideoPath();
                if (video != null) {
                    if (Build.VERSION.SDK_INT >= 24) {
                        // a file:-url is no longer allowed in Nougat, see https://inthecheesefactory.com/blog/how-to-share-access-to-file-with-fileprovider-on-android-nougat/en
                        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(getParentActivity(), BuildConfig.APPLICATION_ID + ".provider", video));
                        takeVideoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        takeVideoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    else if (Build.VERSION.SDK_INT >= 18) {
                        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(video));
                    }
                    takeVideoIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, (long) (1024 * 1024 * 1536));
                    currentPicturePath = video.getAbsolutePath();
                }
                startActivityForResult(takeVideoIntent, RC2_CHAT_VIDEO_CAPTURE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (which == ChatAttachAlert.ATTACH_BUTTON_IDX_FILE ) {
            if (Build.VERSION.SDK_INT >= 23 && getParentActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                getParentActivity().requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 4);
                return;
            }
            DocumentSelectActivity fragment = new DocumentSelectActivity();
            fragment.setDelegate(new DocumentSelectActivity.DocumentSelectActivityDelegate() {
                @Override
                public void didSelectFiles(DocumentSelectActivity activity, ArrayList<String> files) {
                    activity.finishFragment();
                    SendMessagesHelper.prepareSendingDocuments(files, files, null, null, dialog_id);
                    m_mrChat.cleanDraft();
                }

                @Override
                public void startDocumentSelectActivity() {
                    try {
                        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                        photoPickerIntent.setType("*/*");
                        startActivityForResult(photoPickerIntent, RC21_CHAT_PICK_WO_COMPR);
                    } catch (Exception e) {

                    }
                }
            });
            presentFragment(fragment);
        } else if (which == ChatAttachAlert.ATTACH_BUTTON_IDX_MUSIC ) {
            if (Build.VERSION.SDK_INT >= 23 && getParentActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                getParentActivity().requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 4);
                return;
            }
            AudioSelectActivity fragment = new AudioSelectActivity();
            fragment.setDelegate(new AudioSelectActivity.AudioSelectActivityDelegate() {
                @Override
                public void didSelectAudio(ArrayList<MessageObject> messageObjects) {
                    ArrayList<String> files = new ArrayList<>();
                    int size = messageObjects.size();
                    for (int a = 0; a < size; a++) {
                        MessageObject messageObject = messageObjects.get(a);
                        files.add(messageObject.messageOwner.attachPath);
                    }
                    SendMessagesHelper.prepareSendingDocuments(files, files, null, null, dialog_id);
                    m_mrChat.cleanDraft();
                }
            });
            presentFragment(fragment);
        }
        else if (which == ChatAttachAlert.ATTACH_BUTTON_IDX_CONTACT )
        {
            Bundle args = new Bundle();
            args.putInt("do_what", ContactsActivity.SELECT_CONTACT_TO_ATTACH);
            ContactsActivity fragment = new ContactsActivity(args);
            fragment.setDelegate(new ContactsActivity.ContactsActivityDelegate() {
                @Override
                public void didSelectContact(final int selected_user_id) {
                    SendMessagesHelper.getInstance().sendMessageContact(selected_user_id, (int)dialog_id);
                }
            });
            presentFragment(fragment);
        }
    }

    @Override
    public boolean dismissDialogOnPause(Dialog dialog) {
        return !(dialog == chatAttachAlert && PhotoViewer.getInstance().isVisible()) && super.dismissDialogOnPause(dialog);
    }

    private void moveScrollToLastMessage() {
        if( m_msglist.length > 0  ) {
            chatLayoutManager.scrollToPositionWithOffset(m_msglist.length - 1, -100000 - chatListView.getPaddingTop());
        }
    }

    private void scrollToLastMessage(boolean pagedown) {
        if( m_msglist.length > 0  ) {
            chatLayoutManager.scrollToPositionWithOffset(m_msglist.length - 1, -100000 - chatListView.getPaddingTop());
        }
        if( highlightMessageId!=0 ) {
            highlightMessageId = 0;
            updateVisibleRows();
        }
    }

    private void updateMessagesVisisblePart() {
        if (chatListView == null) {
            return;
        }
        int count = chatListView.getChildCount();
        //int additionalTop = chatActivityEnterView.isTopViewVisible() ? AndroidUtilities.dp(48) : 0;
        int height = chatListView.getMeasuredHeight();
        for (int a = 0; a < count; a++) {
            View view = chatListView.getChildAt(a);
            if (view instanceof ChatMessageCell) {
                ChatMessageCell messageCell = (ChatMessageCell) view;
                int top = messageCell.getTop();
                //int bottom = messageCell.getBottom();
                int viewTop = top >= 0 ? 0 : -top;
                int viewBottom = messageCell.getMeasuredHeight();
                if (viewBottom > height) {
                    viewBottom = viewTop + height;
                }
                messageCell.setVisiblePart(viewTop, viewBottom - viewTop);
            }
        }
    }

    private void toggleMute() {
        boolean muted = MrMailbox.isDialogMuted(dialog_id);
        if (!muted) {
            // EDIT BY MR
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity()); // was: BottomSheet.Builder
            //builder.setTitle(LocaleController.getString("Notifications", R.string.Notifications)); -- a title seems more confusing than helping -- the user has clicked "mute" before and there are several options all starting with "mute...", I think, this is very clear
            CharSequence[] items = new CharSequence[]{
                    ProfileNotificationsActivity.muteForString(   60*60),
                    ProfileNotificationsActivity.muteForString( 8*60*60),
                    ProfileNotificationsActivity.muteForString(48*60*60),
                    ApplicationLoader.applicationContext.getString(R.string.MuteAlways)
            };
            builder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            int untilTime = MrMailbox.getCurrentTime();
                                 if (i == 0) { untilTime += 60 * 60; }
                            else if (i == 1) { untilTime += 60 * 60 * 8;  }
                            else if (i == 2) { untilTime += 60 * 60 * 48; }
                            else if (i == 3) { untilTime = Integer.MAX_VALUE; }

                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            if (i == 3) {
                                editor.putInt("notify2_" + dialog_id, 2);
                            } else {
                                editor.putInt("notify2_" + dialog_id, 3);
                                editor.putInt("notifyuntil_" + dialog_id, untilTime);
                            }
                            editor.apply();
                            /*NotificationsController.getInstance().removeNotificationsForDialog(dialog_id);
                            TLRPC.TL_dialog dialog = MessagesController.getInstance().dialogs_dict.get(dialog_id);
                            if (dialog != null) {
                                dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
                                dialog.notify_settings.mute_until = untilTime;
                            }
                            */
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.notificationsSettingsUpdated);
                        }
                    }
            );
            showDialog(builder.create());
            // EDIT BY MR
        } else {
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("notify2_" + dialog_id, 0);
            editor.apply();
            /*TLRPC.TL_dialog dialog = MessagesController.getInstance().dialogs_dict.get(dialog_id);
            if (dialog != null) {
                dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
            }
            */
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.notificationsSettingsUpdated);
        }
    }

    private void createChatByDeaddropMsgId(final int messageId)
    {
        final Context context = getParentActivity();
        if (context == null) {
            return;
        }

        if (chatActivityEnterView != null) {
            chatActivityEnterView.closeKeyboard();
        }

        MrMsg mrMsg = MrMailbox.getMsg(messageId);

        final int fromId =mrMsg.getFromId();
        MrContact mrContact = MrMailbox.getContact(fromId);
        String name = mrContact.getNameNAddr();


        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                int chatId = MrMailbox.createChatByMsgId(messageId);
                if( chatId != 0 ) {
                    Bundle args = new Bundle();
                    args.putInt("chat_id", chatId);
                    presentFragment(new ChatActivity(args), true /*removeLast*/);
                }
                else {
                    Toast.makeText(context, "Cannot create chat.", Toast.LENGTH_LONG).show(); // should not happen
                }
            }
        });
        builder.setNegativeButton(R.string.Cancel, null);
        builder.setMessage(AndroidUtilities.replaceTags(String.format(context.getString(R.string.AskStartChatWith), name)));
        showDialog(builder.create());
    }

    private void scrollToMessageId(int id, boolean select) {
        int i, icnt = m_msglist.length;
        for( i = 0; i < icnt; i++ ) {
            if( m_msglist[i]==id ) {
                chatLayoutManager.scrollToPosition(i);
                highlightMessageId = select? id : 0;
                updateVisibleRows();
                return;
            }
        }
    }

    private void showPagedownButton(boolean show, boolean animated) {
        if (pagedownButton == null) {
            return;
        }
        if (show) {
            pagedownButtonShowedByScroll = false;
            if (pagedownButton.getTag() == null) {
                if (pagedownButtonAnimation != null) {
                    pagedownButtonAnimation.cancel();
                    pagedownButtonAnimation = null;
                }
                if (animated) {
                    if (pagedownButton.getTranslationY() == 0) {
                        pagedownButton.setTranslationY(AndroidUtilities.dp(100));
                    }
                    pagedownButton.setVisibility(View.VISIBLE);
                    pagedownButton.setTag(1);
                    pagedownButtonAnimation = ObjectAnimator.ofFloat(pagedownButton, "translationY", 0).setDuration(200);
                    pagedownButtonAnimation.start();
                } else {
                    pagedownButton.setVisibility(View.VISIBLE);
                }
            }
        } else {
            returnToMessageId = 0;
            if (pagedownButton.getTag() != null) {
                pagedownButton.setTag(null);
                if (pagedownButtonAnimation != null) {
                    pagedownButtonAnimation.cancel();
                    pagedownButtonAnimation = null;
                }
                if (animated) {
                    pagedownButtonAnimation = ObjectAnimator.ofFloat(pagedownButton, "translationY", AndroidUtilities.dp(100)).setDuration(200);
                    pagedownButtonAnimation.addListener(new AnimatorListenerAdapterProxy() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            pagedownButtonCounter.setVisibility(View.INVISIBLE);
                            pagedownButton.setVisibility(View.INVISIBLE);
                        }
                    });
                    pagedownButtonAnimation.start();
                } else {
                    pagedownButton.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {
        if (chatActivityEnterView != null) {
            chatActivityEnterView.onRequestPermissionsResultFragment(requestCode, permissions, grantResults);
        }

    }

    private void checkActionBarMenu() {
        if (menuItem != null) {
            menuItem.setVisibility(View.VISIBLE);
        }
        checkAndUpdateAvatar();
    }

    private void addToSelectedMessages(MessageObject messageObject) {
        if( messageObject.getDialogId() != dialog_id ) {
            return;
        }

        if (selectedMessagesIds.containsKey(messageObject.getId())) {
            selectedMessagesIds.remove(messageObject.getId());
        } else {
            selectedMessagesIds.put(messageObject.getId(), 1);
        }
        if (actionBar.isActionModeShowed()) {
            if (selectedMessagesIds.isEmpty() ) {
                actionBar.hideActionMode();
            } else {
                boolean isSingleSelection = selectedMessagesIds.size()==1;
                boolean hasAttachment = false;
                if( m_saveToXXMenuItem != null ) {
                    if( isSingleSelection ) {
                        MrMsg selMsg = MrMailbox.getMsg(getFirstSelectedId());
                        int type = selMsg.getType();
                        if( type==MrMsg.MR_MSG_FILE ) {
                            hasAttachment = true;
                            m_saveToXXMenuItem.setText(R.string.SaveToDownloads);
                        }
                        else if( type == MrMsg.MR_MSG_AUDIO || type == MrMsg.MR_MSG_VOICE ) {
                            hasAttachment = true;
                            m_saveToXXMenuItem.setText(R.string.SaveToMusic);
                        }
                        if( type==MrMsg.MR_MSG_IMAGE || type==MrMsg.MR_MSG_GIF || type == MrMsg.MR_MSG_VIDEO ) {
                            hasAttachment = true;
                            m_saveToXXMenuItem.setText(R.string.SaveToGallery);
                        }
                    }
                    m_saveToXXMenuItem.setVisibility(hasAttachment? View.VISIBLE : View.GONE);
                }
                if( m_openMenuItem != null )  { m_openMenuItem.setVisibility(hasAttachment? View.VISIBLE : View.GONE); }
                if( m_shareMenuItem != null ) { m_shareMenuItem.setVisibility(hasAttachment? View.VISIBLE : View.GONE); }
                if( m_infoMenuItem != null  ) { m_infoMenuItem.setVisibility(isSingleSelection? View.VISIBLE : View.GONE); }
                if( m_replyMenuItem != null ) { m_replyMenuItem.setVisibility(isSingleSelection? View.VISIBLE : View.GONE); }
            }
        }
    }

    private int getFirstSelectedId()
    {
        ArrayList<Integer> ids_list = new ArrayList<>(selectedMessagesIds.keySet());
        if(ids_list.size() > 0 ) {
            return ids_list.get(0);
        }
        return 0;
    }

    private void processRowSelect(View view) {
        // add/remove a selected messages in selection mode
        MessageObject message = null;
        if (view instanceof ChatMessageCell) {
            message = ((ChatMessageCell) view).getMessageObject();
        }
        else if( view instanceof ChatActionCell) {
            message = ((ChatActionCell) view).getMessageObject();
        }

        if (message==null || !message.isSelectable()) {
            return;
        }
        addToSelectedMessages(message);
        updateActionModeTitle();
        updateVisibleRowsFast();
    }

    private void updateActionModeTitle() {
        if (!actionBar.isActionModeShowed()) {
            return;
        }
        if (!selectedMessagesIds.isEmpty() ) {
            selectedMessagesCountTextView.setNumber(selectedMessagesIds.size(), true);
        }
    }

    private void updateTitle() {
        if (avatarContainer == null) {
            return;
        }
        avatarContainer.setTitle(m_mrChat.getName()); // realize the title from m_hChat
    }

    private void updateTitleIcons() {
        int leftIcon = 0, rightIcon = 0;
        if( m_canMute && MrMailbox.isDialogMuted(dialog_id) ) {
            leftIcon = R.drawable.mute_blue;
        }

        if( m_mrChat!=null && m_mrChat.isVerified() ) {
            rightIcon = R.drawable.verified;
        }

        if (avatarContainer != null)  {
            avatarContainer.setTitleIcons(leftIcon, rightIcon);
        }

        if( muteMenuEntry != null ) {
            if (leftIcon != 0) {
                muteMenuEntry.setText(ApplicationLoader.applicationContext.getString(R.string.UnmuteNotifications));
            } else {
                muteMenuEntry.setText(ApplicationLoader.applicationContext.getString(R.string.MuteNotifications));
            }
        }
    }

    private void checkAndUpdateAvatar() {
        if (avatarContainer != null) {
            avatarContainer.checkAndUpdateAvatar();
        }
    }

    public boolean openVideoEditor(String videoPath, boolean removeLast, boolean animated) {
        Bundle args = new Bundle();
        args.putString("videoPath", videoPath);
        VideoEditorActivity fragment = new VideoEditorActivity(args);
        fragment.setDelegate(new VideoEditorActivity.VideoEditorActivityDelegate() {
            @Override
            public void didFinishEditVideo(VideoEditedInfo vei, long estimatedSize, long estimatedDuration) {
                SendMessagesHelper.prepareSendingVideo(vei.originalPath, estimatedSize, estimatedDuration, vei.resultWidth, vei.resultHeight, vei, dialog_id);
                m_mrChat.cleanDraft();
            }
        });

        if (parentLayout == null || !fragment.onFragmentCreate()) {
            SendMessagesHelper.prepareSendingVideo(videoPath, 0, 0, 0, 0, null, dialog_id);
            m_mrChat.cleanDraft();
            return false;
        }
        parentLayout.presentFragment(fragment, removeLast, !animated, true);
        return true;
    }

    private void showAttachmentError() {
        if (getParentActivity() == null) {
            return;
        }
        AndroidUtilities.showErrorHint(getParentActivity()); // should not happen
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == RC0_CHAT_IMAGE_CAPTURE) {
                PhotoViewer.getInstance().setParentActivity(getParentActivity());
                final ArrayList<Object> arrayList = new ArrayList<>();
                int orientation = 0;
                try {
                    ExifInterface ei = new ExifInterface(currentPicturePath);
                    int exif = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    switch (exif) {
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            orientation = 90;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            orientation = 180;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            orientation = 270;
                            break;
                    }
                } catch (Exception e) {

                }
                arrayList.add(new MediaController.PhotoEntry(0, 0, 0, currentPicturePath, orientation, false));

                PhotoViewer.getInstance().openPhotoForSelect(arrayList, 0, 2, new PhotoViewer.EmptyPhotoViewerProvider() {
                    @Override
                    public void sendButtonPressed(int index) {
                        sendPhoto((MediaController.PhotoEntry) arrayList.get(0));
                    }
                }, this);
                AndroidUtilities.addMediaToGallery(currentPicturePath);
                currentPicturePath = null;
            } else if (requestCode == RC1_CHAT_PICK) {
                /* -- this is the fallback implementation of the gallery selection, see ID_OPEN_SYSTEM_SELECTOR
                if (data == null || data.getData() == null) {
                    showAttachmentError();
                    return;
                }
                Uri uri = data.getData();
                if (uri.toString().contains("video")) {
                    String videoPath = null;
                    try {
                        videoPath = AndroidUtilities.getPath(uri);
                    } catch (Exception e) {

                    }
                    if (videoPath == null) {
                        showAttachmentError();
                    }
                    if (Build.VERSION.SDK_INT >= 16) {
                        if (paused) {
                            startVideoEdit = videoPath;
                        } else {
                            openVideoEditor(videoPath, false, false);
                        }
                    } else {
                        SendMessagesHelper.prepareSendingVideo(videoPath, 0, 0, 0, 0, null, dialog_id);
                    }
                } else {
                    SendMessagesHelper.prepareSendingPhoto(null, uri, dialog_id, null);
                }
                m_mrChat.cleanDraft(); */
            } else if (requestCode == RC2_CHAT_VIDEO_CAPTURE) {
                String videoPath = null;
                //Log.i("DeltaChat", "pic path " + currentPicturePath);
                if (data != null && currentPicturePath != null) {
                    if (new File(currentPicturePath).exists()) {
                        data = null;
                    }
                }
                if (data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        //Log.i("DeltaChat", "video record uri " + uri.toString());
                        videoPath = AndroidUtilities.getPath(uri);
                        //Log.i("DeltaChat", "resolved path = " + videoPath);
                        if (!(new File(videoPath).exists())) {
                            videoPath = currentPicturePath;
                        }
                    } else {
                        videoPath = currentPicturePath;
                    }
                    AndroidUtilities.addMediaToGallery(currentPicturePath);
                    currentPicturePath = null;
                }
                if (videoPath == null && currentPicturePath != null) {
                    File f = new File(currentPicturePath);
                    if (f.exists()) {
                        videoPath = currentPicturePath;
                    }
                    currentPicturePath = null;
                }
                if (Build.VERSION.SDK_INT >= 16) {
                    if (paused) {
                        startVideoEdit = videoPath;
                    } else {
                        openVideoEditor(videoPath, false, false);
                    }
                } else {
                    SendMessagesHelper.prepareSendingVideo(videoPath, 0, 0, 0, 0, null, dialog_id);
                    m_mrChat.cleanDraft();
                }
            } else if (requestCode == RC21_CHAT_PICK_WO_COMPR) {
                if (data == null || data.getData() == null) {
                    showAttachmentError();
                    return;
                }
                Uri uri = data.getData();

                String extractUriFrom = uri.toString();
                if (extractUriFrom.contains("com.google.android.apps.photos.contentprovider")) {
                    try {
                        String firstExtraction = extractUriFrom.split("/1/")[1];
                        int index = firstExtraction.indexOf("/ACTUAL");
                        if (index != -1) {
                            firstExtraction = firstExtraction.substring(0, index);
                            String secondExtraction = URLDecoder.decode(firstExtraction, "UTF-8");
                            uri = Uri.parse(secondExtraction);
                        }
                    } catch (Exception e) {

                    }
                }
                String tempPath = AndroidUtilities.getPath(uri);
                String originalPath = tempPath;
                if (tempPath == null) {
                    originalPath = data.toString();
                    tempPath = MediaController.copyFileToCache(data.getData(), "file");
                }
                if (tempPath == null) {
                    showAttachmentError();
                    return;
                }
                SendMessagesHelper.prepareSendingDocument(tempPath, originalPath, null, null, dialog_id);
                m_mrChat.cleanDraft();
            }
        }
    }

    @Override
    public void saveSelfArgs(Bundle args) {
        if (currentPicturePath != null) {
            args.putString("path", currentPicturePath);
        }
    }

    @Override
    public void restoreSelfArgs(Bundle args) {
        currentPicturePath = args.getString("path");
    }

    @Override
    public void didReceivedNotification(int id, final Object... args)
    {
        if( id == NotificationCenter.dialogsNeedReload )
        {
            if( args.length >= 3 ) {
                // add incoming messages
                int evt_chat_id = (int) args[1];
                int evt_msg_id = (int) args[2];
                if (evt_chat_id == dialog_id && evt_msg_id > 0)
                {
                    boolean markAsRead = false;
                    MrMsg mrMsg = MrMailbox.getMsg(evt_msg_id);
                    if ( mrMsg.getFromId()!=MrContact.MR_CONTACT_ID_SELF ) {
                        if (paused) {
                            if( !scrollToTopUnReadOnResume && markerUnreadMessageId != 0 ) {
                                markerUnreadMessageId = 0;
                            }
                            if( markerUnreadMessageId == 0 ) {
                                markerUnreadMessageId = mrMsg.getId();
                                scrollToMessage = null;
                                scrollToMessagePosition = -10000;
                                markerUnreadCount = 0;
                                scrollToTopUnReadOnResume = true;
                            }
                        }

                        if (markerUnreadMessageId != 0) {
                            markerUnreadCount++;
                        }

                        markAsRead = true;
                    }

                    updateMsglist();
                    chatAdapter.notifyDataSetChanged();
                    scrollToLastMessage(false);

                    if (markAsRead) {
                        if (paused) {
                            markseenWhenResume.put(evt_msg_id, 1);
                        } else {
                            int msg_ids[] = new int[1];
                            msg_ids[0] = evt_msg_id;
                            MrMailbox.markseenMsgs(msg_ids);
                            NotificationsController.getInstance().removeSeenMessages();
                        }
                    }
                }
            }
        }
        else if (id == NotificationCenter.updateInterfaces)
        {
            int updateMask = (Integer) args[0];
            if ((updateMask & MrMailbox.UPDATE_MASK_NAME) != 0 || (updateMask & MrMailbox.UPDATE_MASK_CHAT_NAME) != 0) {
                int back_id = m_mrChat.getId();
                m_mrChat = MrMailbox.getChat(back_id);
                updateTitle();
            }
            boolean updateSubtitle = false;
            if ((updateMask & MrMailbox.UPDATE_MASK_CHAT_MEMBERS) != 0 || (updateMask & MrMailbox.UPDATE_MASK_STATUS) != 0) {
                updateSubtitle = true;
            }
            if ((updateMask & MrMailbox.UPDATE_MASK_AVATAR) != 0 || (updateMask & MrMailbox.UPDATE_MASK_CHAT_AVATAR) != 0 || (updateMask & MrMailbox.UPDATE_MASK_NAME) != 0) {
                checkAndUpdateAvatar();
                updateVisibleRows();
            }
            if (avatarContainer != null && updateSubtitle) {
                avatarContainer.updateSubtitle();
            }

        }
        else if (id == NotificationCenter.didReceivedNewMessages)
        {
            markerUnreadMessageId = 0;
            updateMsglist();
            chatAdapter.notifyDataSetChanged();
            scrollToLastMessage(false);
        }
        else if (id == NotificationCenter.closeChats)
        {
            if (args != null && args.length > 0) {
                long did = (Long) args[0];
                if (did == dialog_id) {
                    finishFragment();
                }
            } else {
                removeSelfFromStack();
            }
        }
        else if (id == NotificationCenter.messagesSentOrRead)
        {
            chatAdapter.notifyDataSetChanged();
        }
        else if (id == NotificationCenter.messagesDeleted)
        {
            markerUnreadMessageId = 0;
            updateMsglist();
            chatAdapter.notifyDataSetChanged();
        }
        else if (id == NotificationCenter.messageSendError)
        {
            chatAdapter.notifyDataSetChanged();
            AndroidUtilities.showErrorHint(getParentActivity());
        }
        else if (id == NotificationCenter.contactsDidLoaded)
        {
            if (avatarContainer != null) {
                avatarContainer.updateSubtitle();
            }
        }
        else if (id == NotificationCenter.audioDidReset || id == NotificationCenter.audioPlayStateChanged)
        {
            if (chatListView != null) {
                int count = chatListView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View view = chatListView.getChildAt(a);
                    if (view instanceof ChatMessageCell) {
                        ChatMessageCell cell = (ChatMessageCell) view;
                        MessageObject messageObject = cell.getMessageObject();
                        if (messageObject != null && (messageObject.isVoice() || messageObject.isMusic())) {
                            cell.updateButtonState();
                        }
                    }
                }
            }
        }
        else if (id == NotificationCenter.audioProgressDidChanged)
        {
            Integer mid = (Integer) args[0];
            if (chatListView != null) {
                int count = chatListView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View view = chatListView.getChildAt(a);
                    if (view instanceof ChatMessageCell) {
                        ChatMessageCell cell = (ChatMessageCell) view;
                        if (cell.getMessageObject() != null && cell.getMessageObject().getId() == mid) {
                            MessageObject playing = cell.getMessageObject();
                            MessageObject player = MediaController.getInstance().getPlayingMessageObject();
                            if (player != null) {
                                playing.audioProgress = player.audioProgress;
                                playing.audioProgressSec = player.audioProgressSec;
                                cell.updateAudioProgress();
                            }
                            break;
                        }
                    }
                }
            }
        }
        else if (id == NotificationCenter.blockedUsersDidLoaded)
        {
        }
        else if (id == NotificationCenter.FileNewChunkAvailable)
        {
        }
        else if (id == NotificationCenter.audioDidStarted)
        {
            if (chatListView != null) {
                int count = chatListView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View view = chatListView.getChildAt(a);
                    if (view instanceof ChatMessageCell) {
                        ChatMessageCell cell = (ChatMessageCell) view;
                        MessageObject messageObject1 = cell.getMessageObject();
                        if (messageObject1 != null && (messageObject1.isVoice() || messageObject1.isMusic())) {
                            cell.updateButtonState();
                        }
                    }
                }
            }
        }
        else if (id == NotificationCenter.waveformCalculated)
        {
            if (chatListView != null) {
                int count = chatListView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View view = chatListView.getChildAt(a);
                    if (view instanceof ChatMessageCell) {
                        ChatMessageCell cell = (ChatMessageCell) view;
                        MessageObject messageObject1 = cell.getMessageObject();
                        if( messageObject1.getId()==(Integer)args[0] ) {
                            cell.waveformCalculated();
                        }
                    }
                }
            }
        }
        else if (id == NotificationCenter.notificationsSettingsUpdated)
        {
            updateTitleIcons();
        }
    }

    private boolean m_searching = false;
    private static final int SEARCH_QUERY      = 0;
    private static final int SEARCH_QUERY_CONT = 1;
    private static final int SEARCH_UP         = 2;
    private static final int SEARCH_DOWN       = 3;
    private String m_lastSearchQuery = "";
    private int[] m_searchResult = {};
    private int   m_searchIndex = -1;
    private void handleSearch(int action, String query)
    {
        int     doScroll = 0;
        boolean doHilite = false;

        m_searching = true;
        if( action==SEARCH_QUERY ) {
            if( query==null ) {query="";}
            query = query.trim();
            if (!query.equals(m_lastSearchQuery)) {
                m_lastSearchQuery = query;
                Utilities.searchQueue.postRunnable(new Runnable() {
                   @Override
                   public void run() {
                       final int temp[] = MrMailbox.searchMsgs((int) dialog_id, m_lastSearchQuery);
                       AndroidUtilities.runOnUIThread(new Runnable() {
                           @Override
                           public void run() {
                               if( m_searching ) {
                                   m_searchResult = temp;
                                   if (m_searchResult.length > 0) {
                                       m_searchIndex = 0;
                                   } else {
                                       m_searchIndex = -1;
                                   }
                                   handleSearch(SEARCH_QUERY_CONT, null);
                               }
                           }
                       });
                   }
                });
            }
        }
        else if( action==SEARCH_QUERY_CONT ) {
            if( m_searchIndex >= 0 && m_searchIndex < m_searchResult.length ) {
                doScroll = m_searchResult[m_searchIndex];
                doHilite = true;
            }
        }
        else if( action==SEARCH_UP ) {
            if( m_searchResult.length>0 ) {
                m_searchIndex = Math.max(m_searchIndex-1, 0);
                doScroll = m_searchResult[m_searchIndex];
                doHilite = true;
            }
            else if( m_msglist.length>0 ) {
                doScroll = m_msglist[0]; // if there are no results, go to the first entry
            }
        }
        else if( action==SEARCH_DOWN ) {
            if( m_searchResult.length>0 ) {
                m_searchIndex = Math.min(m_searchIndex+1, m_searchResult.length-1);
                doScroll = m_searchResult[m_searchIndex];
                doHilite = true;
            }
            else if( m_msglist.length>0 ) {
                doScroll = m_msglist[m_msglist.length-1]; // if there are no results, go to the last entry
            }
        }

        if( doScroll!=0 ) {
            scrollToMessageId(doScroll, doHilite);
        }
        else {
            highlightMessageId = 0;
        }
        updateVisibleRowsFast();

        if( m_lastSearchQuery.isEmpty() ) {
            searchItem.setExtraSearchInfo("", true, true, true);
        } else if (m_searchResult.length == 0) {
            searchItem.setExtraSearchInfo("0/0", true, true, true);
        } else {
            searchItem.setExtraSearchInfo(String.format("%d/%d", m_searchIndex+1, m_searchResult.length), true, true, true);
        }
    }

    @Override
    public boolean needDelayOpenAnimation() {
        return firstLoading;
    }

    @Override
    public void onTransitionAnimationStart(boolean isOpen, boolean backward) {
        NotificationCenter.getInstance().setAllowedNotificationsDutingAnimation(new int[]{NotificationCenter.dialogsNeedReload,
                NotificationCenter.closeChats});
        NotificationCenter.getInstance().setAnimationInProgress(true);
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        NotificationCenter.getInstance().setAnimationInProgress(false);
        if (isOpen) {
            if (Build.VERSION.SDK_INT >= 21) {
                createChatAttachView();
            }
        }
    }

    private void updateBottomOverlay() {
        // we use the bottom overlay for the "dead drop chat" and for the search result line
        if (bottomOverlayChatText == null) {
            return;
        }

        bottomOverlayChatText.setText("");

        if (searchItem != null && searchItem.getVisibility() == View.VISIBLE) {
            bottomOverlayChat.setVisibility(View.INVISIBLE);
            chatActivityEnterView.setFieldFocused(false);
            chatActivityEnterView.setVisibility(View.INVISIBLE);
        } else {
            if (m_isChatWithDeaddrop) {
                if( m_msglist.length==0 ) {
                    // showing the DeaddropHint if there are no messages is confusing (there are no "reply arrows" in this case)
                    bottomOverlayChatText.setText(ApplicationLoader.applicationContext.getString(R.string.NoMessages));
                } else {
                    bottomOverlayChatText.setText(ApplicationLoader.applicationContext.getString(R.string.DeaddropHint));
                }
                bottomOverlayChat.setVisibility(View.VISIBLE);
                chatActivityEnterView.setVisibility(View.INVISIBLE);
            } else {
                chatActivityEnterView.setVisibility(View.VISIBLE);
                bottomOverlayChat.setVisibility(View.INVISIBLE);
            }
            if(muteMenuEntry !=null) {
                muteMenuEntry.setVisibility(View.VISIBLE);
            }
        }
        checkRaiseSensors();
    }

    private void checkRaiseSensors() {
        if (!ApplicationLoader.mainInterfacePaused && (bottomOverlayChat == null || bottomOverlayChat.getVisibility() != View.VISIBLE) && (bottomOverlay == null || bottomOverlay.getVisibility() != View.VISIBLE) ) {
            MediaController.getInstance().setAllowStartRecord(true);
        } else {
            MediaController.getInstance().setAllowStartRecord(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        AndroidUtilities.requestAdjustResize(getParentActivity(), classGuid);
        MediaController.getInstance().startRaiseToEarSensors(this);
        checkRaiseSensors();

        checkActionBarMenu();

        NotificationsController.getInstance().setOpenedDialogId(dialog_id);
        if (scrollToTopOnResume) {
            if (scrollToTopUnReadOnResume && scrollToMessage != null) {
                if (chatListView != null) {
                    int yOffset;
                    if (scrollToMessagePosition == -9000) {
                        yOffset = Math.max(0, (chatListView.getHeight() - scrollToMessage.getApproximateHeight()) / 2);
                    } else if (scrollToMessagePosition == -10000) {
                        yOffset = 0;
                    } else {
                        yOffset = scrollToMessagePosition;
                    }
                    // TODO: chatLayoutManager.scrollToPositionWithOffset(messages.size() - messages.indexOf(scrollToMessage), -chatListView.getPaddingTop() - AndroidUtilities.dp(7) + yOffset);
                }
            } else {
                moveScrollToLastMessage();
            }
            scrollToTopUnReadOnResume = false;
            scrollToTopOnResume = false;
            scrollToMessage = null;
        }
        paused = false;

        int markseenWhenResumeCnt = markseenWhenResume.size();
        if (markseenWhenResumeCnt>0 ) {
            int msg_ids[] = new int[markseenWhenResumeCnt];
            for(int i=0; i<markseenWhenResumeCnt; i++) {
                msg_ids[i] = markseenWhenResume.keyAt(i);
            }
            MrMailbox.markseenMsgs(msg_ids);
            markseenWhenResume.clear();
            NotificationsController.getInstance().removeSeenMessages();
        }

        if (wasPaused) {
            wasPaused = false;
            if (chatAdapter != null) {
                chatAdapter.notifyDataSetChanged();
            }
        }

        fixLayout();
        applyDraftMaybe();
        if (bottomOverlayChat.getVisibility() != View.VISIBLE) {
            chatActivityEnterView.setFieldFocused(true);
        }
        chatActivityEnterView.onResume();

        if (startVideoEdit != null) {
            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    openVideoEditor(startVideoEdit, false, false);
                    startVideoEdit = null;
                }
            });
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        MediaController.getInstance().stopRaiseToEarSensors(this);
        if (menuItem != null) {
            menuItem.closeSubMenu();
        }
        paused = true;
        wasPaused = true;
        NotificationsController.getInstance().setOpenedDialogId(0);
        CharSequence draftMessage = null;
        if (chatActivityEnterView != null) {
            chatActivityEnterView.onPause();
            CharSequence text = AndroidUtilities.getTrimmedString(chatActivityEnterView.getFieldText());
            if (!TextUtils.isEmpty(text) && !TextUtils.equals(text, "@gif")) {
                draftMessage = text;
            }
            chatActivityEnterView.setFieldFocused(false);
        }
        m_mrChat.saveDraft(draftMessage, null);

        MrMailbox.cancelTyping(0, dialog_id);
    }

    private void applyDraftMaybe() {
        if (chatActivityEnterView == null) {
            return;
        }
        TLRPC.DraftMessage draftMessage = m_mrChat.getDraftMessageObj();
        if (chatActivityEnterView.getFieldText() == null) {
            if (draftMessage != null) {
                CharSequence message;
                {
                    message = draftMessage.message;
                }
                chatActivityEnterView.setFieldText(message);
                if (getArguments().getBoolean("hasUrl", false)) {
                    chatActivityEnterView.setSelection(draftMessage.message.indexOf('\n') + 1);
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            if (chatActivityEnterView != null) {
                                chatActivityEnterView.setFieldFocused(true);
                                chatActivityEnterView.openKeyboard();
                            }
                        }
                    }, 700);
                }
            }
        }
    }

    private void fixLayout() {
        if (avatarContainer != null) {
            avatarContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (avatarContainer != null) {
                        avatarContainer.getViewTreeObserver().removeOnPreDrawListener(this);
                    }
                    return true;
                }
            });
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        fixLayout();
    }

    private void createDeleteMessagesAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setMessage(ApplicationLoader.applicationContext.getResources().getQuantityString(R.plurals.AreYouSureDeleteMessages, selectedMessagesIds.size(), selectedMessagesIds.size()));
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                ArrayList<Integer> ids_list = new ArrayList<>(selectedMessagesIds.keySet());
                if( ids_list.size()>0) {
                    int ids_arr[] = new int[selectedMessagesIds.size()], j = 0;
                    for (HashMap.Entry<Integer, Integer> entry : selectedMessagesIds.entrySet()) {
                        ids_arr[j++] = entry.getKey();
                    }
                    MrMailbox.deleteMsgs(ids_arr);
                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.messagesDeleted, ids_list, 0);
                }

                actionBar.hideActionMode();
                updateVisibleRows();

                NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
            }
        });
        builder.setNegativeButton(R.string.Cancel, null);
        showDialog(builder.create());
    }

    private void handleClick(View v, boolean longClick)
    {
        MessageObject message = null;
        if (v instanceof ChatMessageCell) {
            message = ((ChatMessageCell) v).getMessageObject();
        } else if (v instanceof ChatActionCell) {
            message = ((ChatActionCell) v).getMessageObject();
        }

        if( !longClick || message == null || !message.isSelectable() || actionBar.isActionModeShowed() ) {
            return;
        }

        // long click: start selection mode
        selectedMessagesIds.clear();
        actionBar.hideActionMode();
        actionBar.createActionMode();
        actionBar.showActionMode();

        AnimatorSet animatorSet = new AnimatorSet();
        ArrayList<Animator> animators = new ArrayList<>();
        for (int a = 0; a < actionModeViews.size(); a++) {
            View view = actionModeViews.get(a);
            AndroidUtilities.clearDrawableAnimation(view);
            animators.add(ObjectAnimator.ofFloat(view, "scaleY", 0.1f, 1.0f));
        }
        animatorSet.playTogether(animators);
        animatorSet.setDuration(250);
        animatorSet.start();

        addToSelectedMessages(message);
        selectedMessagesCountTextView.setNumber(1, false);
        updateVisibleRowsFast();
    }

    private String getMessageContent(int msg_id, int prev_msg_id)
    {
        String ret = "";

        MrMsg msg = MrMailbox.getMsg(msg_id);
        MrMsg prev_msg = MrMailbox.getMsg(prev_msg_id);

        if (msg.getFromId() != prev_msg.getFromId()) {
            MrContact mrContact = MrMailbox.getContact(msg.getFromId());
            ret += mrContact.getDisplayName() + ":\n";
        }

        if( msg.getType()==MrMsg.MR_MSG_TEXT ) {
            ret += msg.getText();
        }
        else {
            ret += msg.getSummarytext(1000);
        }

        return ret;
    }

    @Override
    public void didSelectChat(ChatlistActivity dialogsFragment, long fwd_chat_id, boolean param)
    {
        if( selectedMessagesIds.size()>0) {
            int ids[] = new int[selectedMessagesIds.size()], i = 0;
            for (HashMap.Entry<Integer, Integer> entry : selectedMessagesIds.entrySet()) {
                ids[i++] = entry.getKey();
            }
            MrMailbox.forwardMsgs(ids, (int)fwd_chat_id);
        }

        if( fwd_chat_id == dialog_id ) {
            dialogsFragment.finishFragment(true);
            actionBar.hideActionMode();
            updateVisibleRows();
        }
        else {
            Bundle args = new Bundle();
            args.putInt("chat_id", (int)fwd_chat_id);
            ChatActivity fragment = new ChatActivity(args);
            if( presentFragment(fragment, true /*remove last*/) ) {
                removeSelfFromStack();
            }
            else {
                dialogsFragment.finishFragment(false);
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        if (actionBar.isActionModeShowed()) {
            // finish selection mode
            selectedMessagesIds.clear();
            actionBar.hideActionMode();
            updateVisibleRowsFast();
            return false;
        } else if (chatActivityEnterView.isPopupShowing()) {
            chatActivityEnterView.hidePopup(true);
            return false;
        }
        return true;
    }

    private void updateVisibleRows() {
        // update by reloading all messages cells from disk.
        // this is slow, but works always, eg. on deletion of messages.
        chatAdapter.notifyDataSetChanged();
    }

    private void updateVisibleRowsFast()
    {
        // faster update, works only for selecting message cells
        // (we do some stuff normally done in onBindViewHolder() here)
        int count = chatListView.getChildCount();
        for (int a = 0; a < count; a++) {
            View view_ = chatListView.getChildAt(a);
            if (view_ instanceof ChatMessageCell) {
                ChatMessageCell messageCell = (ChatMessageCell)view_;
                boolean selected = false;
                boolean disableSelection = false;
                if (actionBar.isActionModeShowed()) {
                    if ( selectedMessagesIds.containsKey(messageCell.getMessageObject().getId()) ) {
                        messageCell.setBackgroundColor(Theme.MSG_SELECTED_BACKGROUND_COLOR);
                        selected = true;
                    } else {
                        messageCell.setBackgroundColor(0);
                    }
                    disableSelection = true;
                } else {
                    messageCell.setBackgroundColor(0);
                }

                messageCell.setCheckPressed(!disableSelection, disableSelection && selected);
                messageCell.setHighlighted(highlightMessageId != 0 && messageCell.getMessageObject().getId() == highlightMessageId);

                if (m_searching && !m_lastSearchQuery.isEmpty()) {
                    messageCell.setHighlightedText(m_lastSearchQuery);
                } else {
                    messageCell.setHighlightedText(null);
                }
            }
            else if( view_ instanceof ChatActionCell ) {
                ChatActionCell actionCell = (ChatActionCell)view_;
                if (actionBar.isActionModeShowed()) {
                    if ( selectedMessagesIds.containsKey(actionCell.getMessageObject().getId()) ) {
                        actionCell.setBackgroundColor(Theme.MSG_SELECTED_BACKGROUND_COLOR);
                    } else {
                        actionCell.setBackgroundColor(0);
                    }
                } else {
                    actionCell.setBackgroundColor(0);
                }
            }
        }

        chatListView.invalidate();
    }

    private void openSearchWithText(String text) {
        if( avatarContainer!= null ) {
            avatarContainer.setVisibility(View.GONE);
        }
        headerItem.setVisibility(View.GONE);
        searchItem.setVisibility(View.VISIBLE);
        updateBottomOverlay();
        openSearchKeyboard = true;
        searchItem.openSearch(openSearchKeyboard);
        searchItem.getSearchField().setText(text);
        searchItem.getSearchField().setSelection(searchItem.getSearchField().length());
        searchItem.setExtraSearchInfo("", true, false, false);
        handleSearch(SEARCH_QUERY, text);
    }

    @Override
    public void updatePhotoAtIndex(int index) {

    }

    @Override
    public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        int count = chatListView.getChildCount();

        for (int a = 0; a < count; a++) {
            ImageReceiver imageReceiver = null;
            View view = chatListView.getChildAt(a);
            if (view instanceof ChatMessageCell) {
                if (messageObject != null) {
                    ChatMessageCell cell = (ChatMessageCell) view;
                    MessageObject message = cell.getMessageObject();
                    if (message != null && message.getId() == messageObject.getId()) {
                        imageReceiver = cell.getPhotoImage();
                    }
                }
            }

            if (imageReceiver != null) {
                int coords[] = new int[2];
                view.getLocationInWindow(coords);
                PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
                object.viewX = coords[0];
                object.viewY = coords[1] - AndroidUtilities.statusBarHeight;
                object.parentView = chatListView;
                object.imageReceiver = imageReceiver;
                object.thumb = imageReceiver.getBitmap();
                object.radius = imageReceiver.getRoundRadius();
                object.dialogId = (int)dialog_id;
                return object;
            }
        }
        return null;
    }

    @Override
    public Bitmap getThumbForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        return null;
    }

    @Override
    public void willSwitchFromPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
    }

    @Override
    public void willHidePhotoViewer() {
    }

    @Override
    public boolean isPhotoChecked(int index) {
        return false;
    }

    @Override
    public void setPhotoChecked(int index) {
    }

    @Override
    public boolean cancelButtonPressed() {
        return true;
    }

    @Override
    public void sendButtonPressed(int index) {
    }

    @Override
    public int getSelectedCount() {
        return 0;
    }

    // PhotoViewerProvider
    public void sendPhoto(MediaController.PhotoEntry photoEntry) {
        if (photoEntry.imagePath != null) {
            SendMessagesHelper.prepareSendingPhoto(photoEntry.imagePath, null, dialog_id, photoEntry.caption);
            m_mrChat.cleanDraft();
        } else if (photoEntry.path != null) {
            SendMessagesHelper.prepareSendingPhoto(photoEntry.path, null, dialog_id, photoEntry.caption);
            m_mrChat.cleanDraft();
        }
    }

    public class ChatActivityAdapter extends RecyclerView.Adapter {

        private Context mContext;

        public ChatActivityAdapter(Context context) {
            mContext = context;
        }

        private class Holder extends RecyclerView.ViewHolder {
            public Holder(View itemView) { super(itemView); }
        }

        @Override
        public int getItemCount() {
            return m_msglist.length;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = null;
            if (viewType == ROWTYPE_MESSAGE_CELL) {
                view = new ChatMessageCell(mContext);
                ChatMessageCell chatMessageCell = (ChatMessageCell) view;
                chatMessageCell.setDelegate(new ChatMessageCell.ChatMessageCellDelegate() {
                    @Override
                    public void didPressedNewchat(ChatMessageCell cell) {
                        createChatByDeaddropMsgId(cell.getMessageObject().getId());
                    }

                    @Override
                    public boolean needPlayAudio(MessageObject messageObject) {
                        return MediaController.getInstance().playAudio(messageObject);
                    }

                    @Override
                    public void didLongPressed(ChatMessageCell cell) {
                        handleClick(cell, true);
                    }

                    @Override
                    public void didPressedUserAvatar(ChatMessageCell cell, TLRPC.User user) {
                        // press on the avatar beside the message
                        if (actionBar.isActionModeShowed()) {
                            processRowSelect(cell);
                            return;
                        }
                        if (user != null && user.id != MrContact.MR_CONTACT_ID_SELF) {
                            Bundle args = new Bundle();
                            args.putInt("user_id", user.id);
                            ProfileActivity fragment = new ProfileActivity(args);
                            presentFragment(fragment);
                        }
                    }

                    @Override
                    public void didPressedSetupMessage(ChatMessageCell cell) {
                        querySetupCode(cell.getMessageObject().getId(), null);
                    }

                    @Override
                    public boolean canPerformActions() {
                        return actionBar != null && !actionBar.isActionModeShowed();
                    }

                    @Override
                    public void didPressedUrl(MessageObject messageObject, final ClickableSpan url, boolean longPress)
                    {
                        if (!(url instanceof URLSpan)) {
                            return;
                        }

                        final String urlFinal = ((URLSpan) url).getURL();
                        boolean      isMailto = urlFinal.startsWith("mailto:");
                        final String urlTitle = isMailto? urlFinal.substring(7) : urlFinal;

                        if (longPress)
                        {
                            ArrayList<CharSequence>  menuItems = new ArrayList<>();
                            final ArrayList<Integer> menuIDs   = new ArrayList<>();

                            if( isMailto ) {
                                menuItems.add(ApplicationLoader.applicationContext.getString(R.string.NewChat));
                                menuIDs.add(100);
                                menuItems.add(ApplicationLoader.applicationContext.getString(R.string.NewContactTitle));
                                menuIDs.add(110);
                            }
                            menuItems.add(ApplicationLoader.applicationContext.getString(R.string.Open));
                            menuIDs.add(200);
                            menuItems.add(ApplicationLoader.applicationContext.getString(R.string.CopyToClipboard));
                            menuIDs.add(210);

                            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                            builder.setTitle(urlTitle);
                            builder.setItems(menuItems.toArray(new CharSequence[menuItems.size()]), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, final int selIndex) {
                                    switch( menuIDs.get(selIndex) ) {
                                        case 100:
                                            createChat(urlFinal);
                                            break;
                                        case 110:
                                            MrMailbox.createContact("", urlFinal);
                                            Toast.makeText(getParentActivity(), ApplicationLoader.applicationContext.getString(R.string.ContactCreated), Toast.LENGTH_LONG).show();
                                            break;
                                        case 200:
                                            Browser.openUrl(getParentActivity(), urlFinal);
                                            break;
                                        case 210:
                                            AndroidUtilities.addToClipboard(urlTitle);
                                            AndroidUtilities.showDoneHint(getParentActivity());
                                            break;
                                    }
                                }
                            });
                            showDialog(builder.create());
                        }
                        else
                        {
                            if( isMailto )
                            {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                                builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        createChat(urlFinal);
                                    }
                                });
                                builder.setNegativeButton(R.string.Cancel, null);
                                builder.setMessage(AndroidUtilities.replaceTags(String.format(mContext.getString(R.string.AskStartChatWith), urlTitle)));
                                showDialog(builder.create());
                            }
                            else
                            {
                                Browser.openUrl(getParentActivity(), urlFinal);
                            }
                        }
                    }

                    private void createChat(String urlFinal)
                    {
                        int chatId = MrMailbox.createChatByContactId(MrMailbox.createContact("", urlFinal));
                        if( chatId != 0 ) {
                            Bundle args = new Bundle();
                            args.putInt("chat_id", chatId);
                            presentFragment(new ChatActivity(args), true /*removeLast*/);
                        }
                    }

                    @Override
                    public void didPressedImage(ChatMessageCell cell) {
                        MessageObject message = cell.getMessageObject();

                        if( message.type == MessageObject.MO_TYPE3_VIDEO && MrMailbox.getMsg(message.getId()).isIncreation() ) {
                            return; // we're not ready (still compressing), a hint is already shown in the message itself
                        }

                        if( /*(Build.VERSION.SDK_INT >= 16 && message.isVideo()) ||*/ message.type == MessageObject.MO_TYPE1_PHOTO || message.isGif()) {
                            PhotoViewer.getInstance().setParentActivity(getParentActivity());
                            PhotoViewer.getInstance().openPhoto(message, message.type != MessageObject.MO_TYPE0_TEXT ? dialog_id : 0, ChatActivity.this);
                        } else if (message.type == MessageObject.MO_TYPE9_FILE || message.type == MessageObject.MO_TYPE3_VIDEO ) {
                            AndroidUtilities.openForViewOrShare(getParentActivity(), message.getId(), Intent.ACTION_VIEW);
                        }
                    }
                });
                chatMessageCell.setAllowAssistant(true);
            } else if (viewType == ROWTYPE_DATE_HEADLINE ) {
                view = new ChatActionCell(mContext);
            } else if (viewType == ROWTYPE_UNREAD_HEADLINE) {
                view = new ChatUnreadCell(mContext);
            }

            if( view != null ) {
                view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            }

            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int i) {
            if (i >= 0 && i < m_msglist.length) {
                View view = holder.itemView;
                int msg_id = m_msglist[i];

                //Log.i(TAG, String.format("Loading message #%d to index #%d", msg_id, i));
                boolean selected = false;
                boolean disableSelection = false;
                if (actionBar.isActionModeShowed()) {
                    if ( selectedMessagesIds.containsKey(msg_id) ) {
                        view.setBackgroundColor(Theme.MSG_SELECTED_BACKGROUND_COLOR);
                        selected = true;
                    } else {
                        view.setBackgroundColor(0);
                    }
                    disableSelection = true;
                } else {
                    view.setBackgroundColor(0);
                }

                if( view instanceof ChatMessageCell )
                {
                    // show a normal message
                    MrMsg mrMsg = MrMailbox.getMsg(msg_id);
                    TLRPC.Message msg = mrMsg.get_TLRPC_Message();
                    MessageObject msgDrawObj = new MessageObject(msg, true);

                    ChatMessageCell messageCell = (ChatMessageCell) view;
                    messageCell.isGroupChat = m_mrChat.isGroup();
                    messageCell.setMessageObject(msgDrawObj, m_isChatWithDeaddrop);
                    messageCell.setCheckPressed(!disableSelection, disableSelection && selected);
                    messageCell.setHighlighted(highlightMessageId != 0 && msgDrawObj.getId() == highlightMessageId);

                    if (m_searching && !m_lastSearchQuery.isEmpty()) {
                        messageCell.setHighlightedText(m_lastSearchQuery);
                    } else {
                        messageCell.setHighlightedText(null);
                    }

                    // mark message as being read
                    int state = mrMsg.getState();
                    if( state == MrMsg.MR_IN_FRESH || state==MrMsg.MR_IN_NOTICED ) {
                        if(paused) {
                            markseenWhenResume.put(msg_id, 1);
                        }
                        else {
                            int msg_ids[] = new int[1];
                            msg_ids[0] = msg_id;
                            MrMailbox.markseenMsgs(msg_ids);
                        }
                    }
                }
                else if( view instanceof ChatActionCell )
                {
                    if( msg_id == MrMsg.MR_MSG_ID_DAYMARKER && i+1 < m_msglist.length ) {
                        // show a date headline (the date comes from the _next_ message)
                        MrMsg mrMsg = MrMailbox.getMsg(m_msglist[i+1]);

                        TLRPC.Message dateMsg = new TLRPC.Message();
                        dateMsg.id = 0;
                        dateMsg.date = (int)mrMsg.getTimestamp();
                        dateMsg.message = LocaleController.formatDateChat(dateMsg.date);
                        MessageObject msgDrawObj = new MessageObject(dateMsg, false);
                        msgDrawObj.type = MessageObject.MO_TYPE10_DATE_HEADLINE;
                        msgDrawObj.contentType = ROWTYPE_DATE_HEADLINE;

                        ChatActionCell actionCell = (ChatActionCell) view;
                        actionCell.setMessageObject(msgDrawObj);
                    }
                    else {
                        MrMsg mrMsg = MrMailbox.getMsg(msg_id);

                        MessageObject msgDrawObj = new MessageObject(mrMsg.get_TLRPC_Message(), false);

                        // Add the suffix " (by ...)" to the action, see strings.xml for details.
                        int fromId = mrMsg.getFromId();
                        if( fromId != MrContact.MR_CONTACT_ID_DEVICE ) {
                            String actionStr = msgDrawObj.messageText.toString();
                            if(actionStr.endsWith(".")) { actionStr = actionStr.substring(0, actionStr.length() - 1); }
                            if( fromId == MrContact.MR_CONTACT_ID_SELF ) {
                                msgDrawObj.messageText = String.format(ApplicationLoader.applicationContext.getString(R.string.ActionBySelf), actionStr);
                            }
                            else {
                                String fromStr = MrMailbox.getContact(mrMsg.getFromId()).getFirstName();
                                msgDrawObj.messageText = String.format(ApplicationLoader.applicationContext.getString(R.string.ActionByUser), actionStr, fromStr);
                            }
                        }

                        ChatActionCell actionCell = (ChatActionCell) view;
                        actionCell.setMessageObject(msgDrawObj);
                    }
                }
                else if (view instanceof ChatUnreadCell)
                {
                    ChatUnreadCell unreadCell = (ChatUnreadCell) view;
                    unreadCell.setText(mContext.getResources().getQuantityString(R.plurals.NewMessages, markerUnreadCount, markerUnreadCount));
                }
            }
        }

        @Override
        public int getItemViewType(int i) {
            if (i >= 0 && i < m_msglist.length) {
                if( m_msglist[i]==MrMsg.MR_MSG_ID_DAYMARKER ) {
                    return ROWTYPE_DATE_HEADLINE;
                }
                else if( m_msglist[i]==MrMsg.MR_MSG_ID_MARKER1 ) {
                    return ROWTYPE_UNREAD_HEADLINE;
                }

                if( MrMailbox.getMsg(m_msglist[i]).isInfo() ) {
                    return ROWTYPE_DATE_HEADLINE;
                }

            }
            return ROWTYPE_MESSAGE_CELL;
        }

        @Override
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            if (holder.itemView instanceof ChatMessageCell) {
                final ChatMessageCell messageCell = (ChatMessageCell) holder.itemView;
                messageCell.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        messageCell.getViewTreeObserver().removeOnPreDrawListener(this);

                        int height = chatListView.getMeasuredHeight();
                        int top = messageCell.getTop();
                        int bottom = messageCell.getBottom();
                        int viewTop = top >= 0 ? 0 : -top;
                        int viewBottom = messageCell.getMeasuredHeight();
                        if (viewBottom > height) {
                            viewBottom = viewTop + height;
                        }
                        messageCell.setVisiblePart(viewTop, viewBottom - viewTop);

                        return true;
                    }
                });

                //messageCell.setHighlighted(highlightMessageId != 0 && messageCell.getMessageObject().getId() == highlightMessageId);
            }
        }
    }

    void querySetupCode(final int msg_id, String[] preload)
    {
        if( !MrMailbox.getMsg(msg_id).isSetupMessage()) {
            return;
        }

        View gl = View.inflate(getParentActivity(), R.layout.setup_code_grid, null);
        final EditText[] editTexts = {
                (EditText) gl.findViewById(R.id.setupCode0), (EditText) gl.findViewById(R.id.setupCode1), (EditText) gl.findViewById(R.id.setupCode2),
                (EditText) gl.findViewById(R.id.setupCode3), (EditText) gl.findViewById(R.id.setupCode4), (EditText) gl.findViewById(R.id.setupCode5),
                (EditText) gl.findViewById(R.id.setupCode6), (EditText) gl.findViewById(R.id.setupCode7), (EditText) gl.findViewById(R.id.setupCode8)
        };
        AlertDialog.Builder builder1 = new AlertDialog.Builder(getParentActivity());
        builder1.setView(gl);
        editTexts[0].setText(MrMailbox.getMsg(msg_id).getSetupCodeBegin());
        editTexts[0].setSelection(editTexts[0].getText().length());

        for( int i = 0; i < 9; i++ ) {
            if( preload != null && i < preload.length ) {
                editTexts[i].setText(preload[i]);
                editTexts[i].setSelection(editTexts[i].getText().length());
            }
            editTexts[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if( s.length()==4 ) {
                        for ( int i = 0; i < 8; i++ ) {
                            if( editTexts[i].hasFocus() && editTexts[i+1].getText().length()<4 ) {
                                editTexts[i+1].requestFocus();
                                break;
                            }
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        builder1.setTitle(ApplicationLoader.applicationContext.getString(R.string.AutocryptKeyTransfer));
        builder1.setMessage(AndroidUtilities.replaceTags(ApplicationLoader.applicationContext.getString(R.string.AutocryptKeyTransferPleaseEnterCode)));
        builder1.setNegativeButton(R.string.Cancel, null);
        builder1.setCancelable(false); // prevent the dialog from being dismissed accidentally (when the dialog is closed, the setup code is gone forever and the user has to create a new setup message)
        builder1.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String setup_code = "";
                final String[] preload = new String[9];
                for ( int i = 0; i < 9; i++ ) {
                    preload[i] = editTexts[i].getText().toString();
                    setup_code += preload[i];
                }
                boolean success = MrMailbox.continueKeyTransfer(msg_id, setup_code);

                AlertDialog.Builder builder2 = new AlertDialog.Builder(getParentActivity());
                builder2.setTitle(ApplicationLoader.applicationContext.getString(R.string.AutocryptKeyTransfer));
                builder2.setMessage(AndroidUtilities.replaceTags(ApplicationLoader.applicationContext.getString(success? R.string.AutocryptKeyTransferSucceeded : R.string.AutocryptKeyTransferBadCode)));
                if( success ) {
                    builder2.setPositiveButton(R.string.OK, null);
                }
                else {
                    builder2.setNegativeButton(R.string.Cancel, null);
                    builder2.setPositiveButton(R.string.Retry, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            querySetupCode(msg_id, preload);
                        }
                    });
                }
                showDialog(builder2.create());
            }
        });
        showDialog(builder1.create());
    }
}
