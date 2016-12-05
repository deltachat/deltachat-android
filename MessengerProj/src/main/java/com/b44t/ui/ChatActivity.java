/*
 * This part of the Delta Chat fronted is based on Telegram which is covered by the following note:
 *
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.b44t.ui;

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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.SparseArray;
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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MediaController;
import com.b44t.messenger.MrChat;
import com.b44t.messenger.MrContact;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.MrMsg;
import com.b44t.messenger.NotificationsController;
import com.b44t.messenger.SendMessagesHelper;
import com.b44t.messenger.Utilities;
import com.b44t.messenger.VideoEditedInfo;
import com.b44t.messenger.browser.Browser;
import com.b44t.messenger.support.widget.GridLayoutManager;
import com.b44t.messenger.support.widget.LinearLayoutManager;
import com.b44t.messenger.support.widget.RecyclerView;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.FileLoader;
import com.b44t.messenger.ConnectionsManager;
import com.b44t.messenger.TLRPC;
import com.b44t.messenger.FileLog;
import com.b44t.messenger.MessageObject;
import com.b44t.messenger.MessagesController;
import com.b44t.messenger.NotificationCenter;
import com.b44t.messenger.R;
import com.b44t.messenger.UserConfig;
import com.b44t.ui.ActionBar.BackDrawable;
import com.b44t.ui.ActionBar.BottomSheet;
import com.b44t.ui.ActionBar.SimpleTextView;
import com.b44t.ui.Adapters.MentionsAdapter;
import com.b44t.ui.Adapters.StickersAdapter;
import com.b44t.messenger.AnimatorListenerAdapterProxy;
import com.b44t.ui.Cells.ChatActionCell;
import com.b44t.ui.Cells.ChatLoadingCell;
import com.b44t.ui.ActionBar.ActionBar;
import com.b44t.ui.ActionBar.ActionBarMenu;
import com.b44t.ui.ActionBar.ActionBarMenuItem;
import com.b44t.ui.Cells.ChatMessageCell;
import com.b44t.ui.Cells.ChatUnreadCell;
import com.b44t.ui.Components.BackupImageView;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.Components.ChatActivityEnterView;
import com.b44t.messenger.ImageReceiver;
import com.b44t.ui.Components.ChatAttachAlert;
import com.b44t.ui.Components.ChatAvatarContainer;
import com.b44t.ui.Components.ExtendedGridLayoutManager;
import com.b44t.ui.Components.PlayerView;
import com.b44t.ui.Components.LayoutHelper;
import com.b44t.ui.Components.NumberTextView;
import com.b44t.ui.Components.RecyclerListView;
import com.b44t.ui.Components.Size;
import com.b44t.ui.Components.SizeNotifierFrameLayout;
import com.b44t.ui.Components.StickersAlert;
import com.b44t.ui.ActionBar.Theme;
import com.b44t.ui.Components.URLSpanNoUnderline;
import com.b44t.ui.Components.URLSpanReplacement;
import com.b44t.ui.Components.URLSpanUserMention;
import com.b44t.ui.Components.WebFrameLayout;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;

@SuppressWarnings("unchecked")
public class ChatActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, DialogsActivity.DialogsActivityDelegate,
        PhotoViewer.PhotoViewerProvider {

    public  static final int ROWTYPE_MESSAGE_CELL = 0;
    public  static final int ROWTYPE_ACTION_CELL  = 1;
    public  static final int ROWTYPE_UNREAD_CELL  = 2;
    private static final int ROWTYPE_BOTINFO_CELL = 3;
    private static final int ROWTYPE_LOADING_CELL = 4;

    protected TLRPC.Chat currentChat;

    public MrChat m_mrChat = new MrChat(0);

    private ArrayList<ChatMessageCell> chatMessageCellsCache = new ArrayList<>();

    private FrameLayout progressView;
    private FrameLayout bottomOverlay;
    protected ChatActivityEnterView chatActivityEnterView;
    private ActionBarMenuItem menuItem;
    private ActionBarMenuItem attachItem;
    private ActionBarMenuItem headerItem;
    private ActionBarMenuItem searchItem;
    private RecyclerListView chatListView;
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
    private RecyclerListView stickersListView;
    private RecyclerListView.OnItemClickListener stickersOnItemClickListener;
    private RecyclerListView.OnItemClickListener mentionsOnItemClickListener;
    private StickersAdapter stickersAdapter;
    private FrameLayout stickersPanel;
    private TextView muteMenuEntry;
    private boolean m_canMute;
    private FrameLayout pagedownButton;
    private boolean pagedownButtonShowedByScroll;
    private TextView pagedownButtonCounter;
    private BackupImageView replyImageView;
    private SimpleTextView replyObjectTextView;
    private MentionsAdapter mentionsAdapter;
    private FrameLayout mentionContainer;
    private RecyclerListView mentionListView;
    private LinearLayoutManager mentionLayoutManager;
    private ExtendedGridLayoutManager mentionGridLayoutManager;
    private AnimatorSet mentionListAnimation;
    private ChatAttachAlert chatAttachAlert;
    private PlayerView playerView;
    private TextView gifHintTextView;
    private View emojiButtonRed;
    private TextView alertTextView;
    private FrameLayout searchContainer;
    private ImageView searchUpButton;
    private ImageView searchDownButton;
    private SimpleTextView searchCountText;

    private boolean mentionListViewIgnoreLayout;
    private int mentionListViewScrollOffsetY;
    private int mentionListViewLastViewTop;
    private int mentionListViewLastViewPosition;
    private boolean mentionListViewIsScrolling;

    private ObjectAnimator pagedownButtonAnimation;
    private ObjectAnimator iconAnimator;

    private boolean openSearchKeyboard;

    private boolean allowStickersPanel;
    private boolean allowContextBotPanel;
    private boolean allowContextBotPanelSecond = true;
    private AnimatorSet runningAnimation;

    private MessageObject selectedObject;
    private MessageObject forwaringMessage;
    private final MessageObject replyingMessageObject = null;
    private boolean paused = true;
    private boolean wasPaused = false;
    private boolean readWhenResume = false;
    private final TLRPC.FileLocation replyImageLocation = null;
    private int linkSearchRequestId;
    private TLRPC.WebPage foundWebPage;
    private ArrayList<CharSequence> foundUrls;
    private Runnable waitingForCharaterEnterRunnable;

    private int readWithDate;
    private int readWithMid;
    private boolean scrollToTopOnResume;
    private boolean forceScrollToTop;
    private boolean scrollToTopUnReadOnResume;
    private long dialog_id;
    private HashMap<Integer, MessageObject>[] selectedMessagesIds = new HashMap[]{new HashMap<>(), new HashMap<>()};
    private HashMap<Integer, MessageObject>[] selectedMessagesCanCopyIds = new HashMap[]{new HashMap<>(), new HashMap<>()};

    private int newUnreadMessageCount;

    private HashMap<Integer, MessageObject> messagesDict = new HashMap<>();
    private HashMap<String, ArrayList<MessageObject>> messagesByDays = new HashMap<>();
    protected ArrayList<MessageObject> messages = new ArrayList<>();
    private int maxMessageId = Integer.MAX_VALUE;
    private int minMessageId = Integer.MIN_VALUE;
    private int maxDate = Integer.MIN_VALUE;
    private int minDate;
    private boolean endReached;
    private boolean forwardEndReached = true; // true=newest messages loaded
    private boolean loading;
    private boolean firstLoading = true;
    private int loadsCount;
    private int last_message_id = 0;

    private int startLoadFromMessageId;
    private int returnToMessageId;

    private boolean first = true;
    private int unread_to_load;
    private int first_unread_id;
    private boolean loadingForward;
    private MessageObject unreadMessageObject;
    private MessageObject scrollToMessage;
    private int highlightMessageId = Integer.MAX_VALUE;
    private int scrollToMessagePosition = -10000;

    private String currentPicturePath;

    protected TLRPC.ChatFull info = null;

    private String startVideoEdit = null;

    private final static int id_copy = 10;
    private final static int id_forward = 11;
    private final static int id_delete_messages = 12;
    private final static int chat_menu_attach = 14;
    private final static int id_delete_chat = 16;
    private final static int mute = 18;
    private final static int id_reply = 19;
    private final static int id_info = 20;

    private final static int search = 40;

    private final static int id_chat_compose_panel = 1000;

    RecyclerListView.OnItemLongClickListener onItemLongClickListener = new RecyclerListView.OnItemLongClickListener() {
        @Override
        public boolean onItemClick(View view, int position) {
            if (!actionBar.isActionModeShowed()) {
                createMenu(view, false);
                return true;
            }
            return false;
        }
    };

    RecyclerListView.OnItemClickListener onItemClickListener = new RecyclerListView.OnItemClickListener() {
        @Override
        public void onItemClick(View view, int position) {
            if (actionBar.isActionModeShowed()) {
                processRowSelect(view);
                return;
            }
            createMenu(view, true);
        }
    };

    public ChatActivity(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        // EDIT BY MR -- set up the values so that the activity gets usable
        dialog_id = arguments.getInt("chat_id", 0);
        MrMailbox.markseenChat((int)dialog_id);
        m_mrChat = MrMailbox.getChat((int)dialog_id);
        currentChat = new TLRPC.Chat();
        currentChat.id = (int)dialog_id;
        // /EDIT BY MR

        startLoadFromMessageId = arguments.getInt("message_id", 0);
        scrollToTopOnResume = arguments.getBoolean("scrollToTopOnResume", false);

        /* EDIT BY MR
        if (chatId != 0) {
            currentChat = MessagesController.getInstance().getChat(chatId);
            if (currentChat == null) {
                final Semaphore semaphore = new Semaphore(0);
                MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        currentChat = MessagesStorage.getInstance().getChat(chatId);
                        semaphore.release();
                    }
                });
                try {
                    semaphore.acquire();
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                }
                if (currentChat != null) {
                    MessagesController.getInstance().putChat(currentChat, true);
                } else {
                    return false;
                }
            }
            if (chatId > 0) {
                dialog_id = -chatId;
            } else {
                isBroadcast = true;
                dialog_id = AndroidUtilities.makeBroadcastId(chatId);
            }
            if (ChatObject.isChannel(currentChat)) {
                MessagesController.getInstance().startShortPoll(chatId, false);
            }
        } else if (userId != 0) {
            currentUser = MessagesController.getInstance().getUser(userId);
            if (currentUser == null) {
                final Semaphore semaphore = new Semaphore(0);
                MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        currentUser = MessagesStorage.getInstance().getUser(userId);
                        semaphore.release();
                    }
                });
                try {
                    semaphore.acquire();
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                }
                if (currentUser != null) {
                    MessagesController.getInstance().putUser(currentUser, true);
                } else {
                    return false;
                }
            }
            dialog_id = userId;
            botUser = arguments.getString("botUser");
            if (inlineQuery != null) {
                MessagesController.getInstance().sendBotStart(currentUser, inlineQuery);
            }
        } else if (encId != 0) {
            // ...
        } else {
            return false;
        }
        */

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.emojiDidLoaded);
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
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.screenshotTook);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.blockedUsersDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileNewChunkAvailable);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioDidStarted);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateMessageMedia);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.replaceMessagesObjects);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.notificationsSettingsUpdated);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.didLoadedReplyMessages);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.chatSearchResultsAvailable);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.didUpdatedMessagesViews);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.newDraftReceived);

        super.onFragmentCreate();

        /* EDIT BY MR
        loading = true;
        MessagesController.getInstance().loadPeerSettings(dialog_id, currentUser, currentChat);
        MessagesController.getInstance().setLastCreatedDialogId(dialog_id, true);
        if (startLoadFromMessageId != 0) {
            needSelectFromMessageId = true;
            waitingForLoad.add(lastLoadIndex);
            if (migrated_to != 0) {
                mergeDialogId = migrated_to;
                MessagesController.getInstance().loadMessages(mergeDialogId, AndroidUtilities.isTablet() ? 30 : 20, startLoadFromMessageId, true, 0, classGuid, 3, 0, ChatObject.isChannel(currentChat), lastLoadIndex++);
            } else {
                MessagesController.getInstance().loadMessages(dialog_id, AndroidUtilities.isTablet() ? 30 : 20, startLoadFromMessageId, true, 0, classGuid, 3, 0, ChatObject.isChannel(currentChat), lastLoadIndex++);
            }
        } else {
            waitingForLoad.add(lastLoadIndex);
            MessagesController.getInstance().loadMessages(dialog_id, AndroidUtilities.isTablet() ? 30 : 20, 0, true, 0, classGuid, 2, 0, ChatObject.isChannel(currentChat), lastLoadIndex++);
        }
        */

        // EDIT BY MR -- runOnUIThread() seems to be needed as otherwise the objects needed to build the view are not constructed.
		// (runOnUIThread() seems to delay the construction - the same is done by the original implementation in loadMessages())
        AndroidUtilities.runOnUIThread(new Runnable() {
           @Override
           public void run() {
               messagesDidLoaded();
           }
        });
        // /EDIT BY MR

        /* EDIT BY MR
        if (currentChat != null) {
            Semaphore semaphore = null;
            MessagesController.getInstance().loadChatInfo(currentChat.id, semaphore, ChatObject.isChannel(currentChat));
        }

        if (userId != 0 && currentUser.bot) {
            BotQuery.loadBotInfo(userId, true, classGuid);
        } else if (info instanceof TLRPC.TL_chatFull) {
            for (int a = 0; a < info.participants.participants.size(); a++) {
                TLRPC.ChatParticipant participant = info.participants.participants.get(a);
                TLRPC.User user = MessagesController.getInstance().getUser(participant.user_id);
                if (user != null && user.bot) {
                    BotQuery.loadBotInfo(user.id, true, classGuid);
                }
            }
        }

        if (currentUser != null) {
            userBlocked = MessagesController.getInstance().blockedUsers.contains(currentUser.id);
        }
        */

        if (AndroidUtilities.isTablet()) {
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.openedChatChanged, dialog_id, false);
        }

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (chatActivityEnterView != null) {
            chatActivityEnterView.onDestroy();
        }
        if (mentionsAdapter != null) {
            mentionsAdapter.onDestroy();
        }
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.emojiDidLoaded);
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
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.screenshotTook);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.blockedUsersDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.FileNewChunkAvailable);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioDidStarted);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateMessageMedia);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.replaceMessagesObjects);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.notificationsSettingsUpdated);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didLoadedReplyMessages);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.chatSearchResultsAvailable);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioPlayStateChanged);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didUpdatedMessagesViews);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.newDraftReceived);

        if (AndroidUtilities.isTablet()) {
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.openedChatChanged, dialog_id, true);
        }

        /*
        if (currentUser != null) {
            MessagesController.getInstance().cancelLoadFullUser(currentUser.id);
        }
        */
        AndroidUtilities.removeAdjustResize(getParentActivity(), classGuid);
        if (stickersAdapter != null) {
            stickersAdapter.onDestroy();
        }
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
    public View createView(Context context) {

        if (chatMessageCellsCache.isEmpty()) {
            for (int a = 0; a < 8; a++) {
                chatMessageCellsCache.add(new ChatMessageCell(context));
            }
        }
        for (int a = 1; a >= 0; a--) {
            selectedMessagesIds[a].clear();
            selectedMessagesCanCopyIds[a].clear();
        }

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
                        for (int a = 1; a >= 0; a--) {
                            selectedMessagesIds[a].clear();
                            selectedMessagesCanCopyIds[a].clear();
                        }
                        actionBar.hideActionMode();
                        updateVisibleRows();
                    } else {
                        finishFragment();
                    }
                } else if (id == id_copy) {
                    String str = "";
                    int previousUid = 0;
                    for (int a = 1; a >= 0; a--) {
                        ArrayList<Integer> ids = new ArrayList<>(selectedMessagesCanCopyIds[a].keySet());
                        Collections.sort(ids);
                        for (int b = 0; b < ids.size(); b++) {
                            Integer messageId = ids.get(b);
                            MessageObject messageObject = selectedMessagesCanCopyIds[a].get(messageId);
                            if (str.length() != 0) {
                                str += "\n\n";
                            }
                            str += getMessageContent(messageObject, previousUid, true);
                            previousUid = messageObject.messageOwner.from_id;
                        }
                    }
                    if (str.length() != 0) {
                        AndroidUtilities.addToClipboard(str);
                    }
                    for (int a = 1; a >= 0; a--) {
                        selectedMessagesIds[a].clear();
                        selectedMessagesCanCopyIds[a].clear();
                    }

                    actionBar.hideActionMode();
                    updateVisibleRows();
                } else if (id == id_delete_messages) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    createDeleteMessagesAlert();
                } else if (id == id_forward) {
                    Toast.makeText(getParentActivity(), LocaleController.getString("NotYetImplemented", R.string.NotYetImplemented), Toast.LENGTH_LONG).show();
                    /*
                    Bundle args = new Bundle();
                    args.putBoolean("onlySelect", true);
                    args.putInt("dialogsType", 1);
                    DialogsActivity fragment = new DialogsActivity(args);
                    fragment.setDelegate(ChatActivity.this);
                    presentFragment(fragment);
                    */
                    actionBar.hideActionMode();
                    updateVisibleRows();
                } else if ( id == id_delete_chat) {
                    // as the history may be a mix of messenger-messages and e-mails, it is not safe to delete it.
                    // the user can delete explicit messages or use his e-mail programm to delete masses.
                    if (getParentActivity() == null) {
                        return;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setMessage(LocaleController.getString("AreYouSureDeleteThisChat", R.string.AreYouSureDeleteThisChat));
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if( MrMailbox.deleteChat((int)dialog_id)!=0 ) {
                                finishFragment();
                            }
                            else {
                                Toast.makeText(getParentActivity(), LocaleController.getString("CannotDeleteChat", R.string.CannotDeleteChat), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                } else if (id == mute) {
                    toggleMute();
                } else if (id == id_reply) {
                    if( m_mrChat.getId()==MrChat.MR_CHAT_ID_DEADDROP ){
                        if( selectedMessagesIds[0]!=null && selectedMessagesIds[0].size()==1) {
                            ArrayList<Integer> ids = new ArrayList<>(selectedMessagesIds[0].keySet());
                            createChatByDeaddropMsgId(ids.get(0));
                        }
                    }
                    else{
                        Toast.makeText(getParentActivity(), LocaleController.getString("NotYetImplemented", R.string.NotYetImplemented), Toast.LENGTH_LONG).show();
                        actionBar.hideActionMode();
                        updateVisibleRows();
                    }
                } else if (id == id_info) {
                    if( selectedMessagesIds[0]!=null && selectedMessagesIds[0].size()==1) {
                        ArrayList<Integer> ids = new ArrayList<>(selectedMessagesIds[0].keySet());
                        String info_str = MrMailbox.getMsgInfo(ids.get(0));

                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setMessage(info_str);
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ;
                            }
                        });
                        showDialog(builder.create());
                    }
                    actionBar.hideActionMode();
                    updateVisibleRows();
                } else if (id == chat_menu_attach) {
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
                } else if (id == search) {
                    openSearchWithText(null);
                }
            }
        });

        avatarContainer = new ChatAvatarContainer(context, this);
        actionBar.addView(avatarContainer, 0, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, 56, 0, 40, 0));

        ActionBarMenu menu = actionBar.createMenu();

        {
            searchItem = menu.addItem(0, R.drawable.ic_ab_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {

                @Override
                public void onSearchCollapse() {
                    avatarContainer.setVisibility(View.VISIBLE);
                    if (chatActivityEnterView.hasText()) {
                        if (headerItem != null) {
                            headerItem.setVisibility(View.GONE);
                        }
                        if (attachItem != null) {
                            attachItem.setVisibility(View.VISIBLE);
                        }
                    } else {
                        if (headerItem != null) {
                            headerItem.setVisibility(View.VISIBLE);
                        }
                        if (attachItem != null) {
                            attachItem.setVisibility(View.GONE);
                        }
                    }
                    searchItem.setVisibility(View.GONE);
                    highlightMessageId = Integer.MAX_VALUE;
                    updateVisibleRows();
                    scrollToLastMessage(false);
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
                            Toast.makeText(getParentActivity(), LocaleController.getString("NotYetImplemented", R.string.NotYetImplemented), Toast.LENGTH_LONG).show();
                        }
                    }, 300);
                }

                @Override
                public void onSearchPressed(EditText editText) {
                    updateSearchButtons(0, 0, 0);
                    //MessagesSearchQuery.searchMessagesInChat(editText.getText().toString(), dialog_id, mergeDialogId, classGuid, 0);
                }
            });
            searchItem.getSearchField().setHint(LocaleController.getString("Search", R.string.Search));
            searchItem.setVisibility(View.GONE);
        }

        headerItem = menu.addItem(0, R.drawable.ic_ab_other);
        if (searchItem != null) {
            headerItem.addSubItem(search, LocaleController.getString("Search", R.string.Search), 0);
        }

        boolean isChatWithDeaddrop = m_mrChat.getId()==MrChat.MR_CHAT_ID_DEADDROP;
        m_canMute = true;
        if( isChatWithDeaddrop && MrMailbox.getConfigInt("show_deaddrop", 0)==0 ) {
            m_canMute = false;
        }

        if( m_canMute ) {
            muteMenuEntry = headerItem.addSubItem(mute, null, 0);
        }

        if( !isChatWithDeaddrop ) {
            if (m_mrChat.getType() == MrChat.MR_CHAT_GROUP) {
                headerItem.addSubItem(id_delete_chat, LocaleController.getString("DeleteAndExit", R.string.DeleteAndExit), 0);
            } else {
                headerItem.addSubItem(id_delete_chat, LocaleController.getString("DeleteChat", R.string.DeleteChat), 0);
            }
        }

        updateTitle();
        avatarContainer.updateSubtitle();
        updateTitleIcons();

        attachItem = menu.addItem(chat_menu_attach, R.drawable.ic_ab_other).setOverrideMenuClick(true).setAllowCloseAnimation(false);
        attachItem.setVisibility(View.GONE);
        menuItem = menu.addItem(chat_menu_attach, R.drawable.ic_ab_attach).setAllowCloseAnimation(false);
        menuItem.setBackgroundDrawable(null);

        actionModeViews.clear();

        final ActionBarMenu actionMode = actionBar.createActionMode();

        selectedMessagesCountTextView = new NumberTextView(actionMode.getContext());
        selectedMessagesCountTextView.setTextSize(18);
        selectedMessagesCountTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
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

                actionModeTextView.setTextSize(!AndroidUtilities.isTablet() && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 18 : 20);
                actionModeTextView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(24), MeasureSpec.AT_MOST));

                if (actionModeSubTextView.getVisibility() != GONE) {
                    actionModeSubTextView.setTextSize(!AndroidUtilities.isTablet() && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 14 : 16);
                    actionModeSubTextView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(20), MeasureSpec.AT_MOST));
                }
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                int height = bottom - top;

                int textTop;
                if (actionModeSubTextView.getVisibility() != GONE) {
                    textTop = (height / 2 - actionModeTextView.getTextHeight()) / 2 + AndroidUtilities.dp(!AndroidUtilities.isTablet() && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 2 : 3);
                } else {
                    textTop = (height - actionModeTextView.getTextHeight()) / 2;
                }
                actionModeTextView.layout(0, textTop, actionModeTextView.getMeasuredWidth(), textTop + actionModeTextView.getTextHeight());

                if (actionModeSubTextView.getVisibility() != GONE) {
                    textTop = height / 2 + (height / 2 - actionModeSubTextView.getTextHeight()) / 2 - AndroidUtilities.dp(!AndroidUtilities.isTablet() && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 1 : 1);
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
        actionModeTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        actionModeTextView.setTextColor(Theme.ACTION_BAR_ACTION_MODE_TEXT_COLOR);
        actionModeTextView.setText(LocaleController.getString("Edit", R.string.Edit));
        actionModeTitleContainer.addView(actionModeTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        actionModeSubTextView = new SimpleTextView(context);
        actionModeSubTextView.setGravity(Gravity.LEFT);
        actionModeSubTextView.setTextColor(Theme.ACTION_BAR_ACTION_MODE_TEXT_COLOR);
        actionModeTitleContainer.addView(actionModeSubTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionModeViews.add(actionMode.addItem(id_info, R.drawable.ic_ab_info, Theme.ACTION_BAR_MODE_SELECTOR_COLOR, null, AndroidUtilities.dp(54)));
        actionModeViews.add(actionMode.addItem(id_reply, R.drawable.ic_ab_reply, Theme.ACTION_BAR_MODE_SELECTOR_COLOR, null, AndroidUtilities.dp(54)));
        actionModeViews.add(actionMode.addItem(id_copy, R.drawable.ic_ab_fwd_copy, Theme.ACTION_BAR_MODE_SELECTOR_COLOR, null, AndroidUtilities.dp(54)));
        actionModeViews.add(actionMode.addItem(id_delete_messages, R.drawable.ic_ab_fwd_delete, Theme.ACTION_BAR_MODE_SELECTOR_COLOR, null, AndroidUtilities.dp(54)));
        actionModeViews.add(actionMode.addItem(id_forward, R.drawable.ic_ab_fwd_forward, Theme.ACTION_BAR_MODE_SELECTOR_COLOR, null, AndroidUtilities.dp(54)));
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
                inputFieldHeight = chatActivityEnterView.getMeasuredHeight();

                for (int i = 0; i < childCount; i++) {
                    View child = getChildAt(i);
                    if (child == null || child.getVisibility() == GONE || child == chatActivityEnterView) {
                        continue;
                    }
                    if (child == chatListView || child == progressView) {
                        int contentWidthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
                        int contentHeightSpec = MeasureSpec.makeMeasureSpec(Math.max(AndroidUtilities.dp(10), heightSize - inputFieldHeight + AndroidUtilities.dp(2 + (chatActivityEnterView.isTopViewVisible() ? 48 : 0))), MeasureSpec.EXACTLY);
                        child.measure(contentWidthSpec, contentHeightSpec);
                    } else if (child == emptyViewContainer) {
                        int contentWidthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
                        int contentHeightSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
                        child.measure(contentWidthSpec, contentHeightSpec);
                    } else if (chatActivityEnterView.isPopupView(child)) {
                        child.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(child.getLayoutParams().height, MeasureSpec.EXACTLY));
                    } else if (child == mentionContainer) {
                        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mentionContainer.getLayoutParams();
                        int height;
                        mentionListViewIgnoreLayout = true;

                        /*if (mentionsAdapter.isBotContext() && mentionsAdapter.isMediaLayout()) {
                            int size = mentionGridLayoutManager.getRowsCount(widthSize);
                            int maxHeight = size * 102;
                            if (mentionsAdapter.isBotContext()) {
                                if (mentionsAdapter.getBotContextSwitch() != null) {
                                    maxHeight += 34;
                                }
                            }
                            height = heightSize - chatActivityEnterView.getMeasuredHeight() + (maxHeight != 0 ? AndroidUtilities.dp(2) : 0);
                            mentionListView.setPadding(0, Math.max(0, height - AndroidUtilities.dp(Math.min(maxHeight, 68 * 1.8f))), 0, 0);
                        } else */ {
                            int size = mentionsAdapter.getItemCount();
                            int maxHeight = 0;
                            /*if (mentionsAdapter.isBotContext()) {
                                if (mentionsAdapter.getBotContextSwitch() != null) {
                                    maxHeight += 36;
                                    size -= 1;
                                }
                                maxHeight += size * 68;
                            } else */ {
                                maxHeight += size * 36;
                            }
                            height = heightSize - chatActivityEnterView.getMeasuredHeight() + (maxHeight != 0 ? AndroidUtilities.dp(2) : 0);
                            mentionListView.setPadding(0, Math.max(0, height - AndroidUtilities.dp(Math.min(maxHeight, 68 * 1.8f))), 0, 0);
                        }

                        layoutParams.height = height;
                        layoutParams.topMargin = 0;

                        mentionListViewIgnoreLayout = false;
                        child.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(layoutParams.height, MeasureSpec.EXACTLY));
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
                        gravity = Gravity.TOP | Gravity.LEFT;
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

                    if (child == mentionContainer) {
                        childTop -= chatActivityEnterView.getMeasuredHeight() - AndroidUtilities.dp(2);
                    } else if (child == pagedownButton) {
                        childTop -= chatActivityEnterView.getMeasuredHeight();
                    } else if (child == emptyViewContainer) {
                        childTop -= inputFieldHeight / 2;
                    } else if (chatActivityEnterView.isPopupView(child)) {
                        childTop = chatActivityEnterView.getBottom();
                    } else if (child == gifHintTextView) {
                        childTop -= inputFieldHeight;
                    } else if (child == chatListView || child == progressView) {
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
            emptyView.setText(LocaleController.getString("NoMessages", R.string.NoMessages));
            emptyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setTextColor(Theme.CHAT_EMPTY_VIEW_TEXT_COLOR);
            emptyView.setBackgroundResource(R.drawable.system);
            emptyView.getBackground().setColorFilter(Theme.colorFilter);
            emptyView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            emptyView.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(2), AndroidUtilities.dp(10), AndroidUtilities.dp(3));
            emptyViewContainer.addView(emptyView, new FrameLayout.LayoutParams(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        if (chatActivityEnterView != null) {
            chatActivityEnterView.onDestroy();
        }
        if (mentionsAdapter != null) {
            mentionsAdapter.onDestroy();
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
        chatListView.setOnItemLongClickListener(onItemLongClickListener);
        chatListView.setOnItemClickListener(onItemClickListener);
        chatListView.setOnScrollListener(new RecyclerView.OnScrollListener() {

            private float totalDy = 0;
            private final int scrollValue = AndroidUtilities.dp(100);

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING && highlightMessageId != Integer.MAX_VALUE) {
                    highlightMessageId = Integer.MAX_VALUE;
                    updateVisibleRows();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                checkScrollForLoad(true);
                int firstVisibleItem = chatLayoutManager.findFirstVisibleItemPosition();
                int visibleItemCount = firstVisibleItem == RecyclerView.NO_POSITION ? 0 : Math.abs(chatLayoutManager.findLastVisibleItemPosition() - firstVisibleItem) + 1;
                if (visibleItemCount > 0) {
                    int totalItemCount = chatAdapter.getItemCount();
                    if (firstVisibleItem + visibleItemCount == totalItemCount && forwardEndReached) {
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
                /*if (chatActivityEnterView != null && chatActivityEnterView.isEditingMessage()) {
                    return true;
                }*/
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

        progressView = new FrameLayout(context);
        progressView.setVisibility(View.INVISIBLE);
        contentView.addView(progressView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));

        View view = new View(context);
        view.setBackgroundResource(R.drawable.system_loader);
        view.getBackground().setColorFilter(Theme.colorFilter);
        progressView.addView(view, LayoutHelper.createFrame(36, 36, Gravity.CENTER));

        ProgressBar progressBar = new ProgressBar(context);
        try {
            progressBar.setIndeterminateDrawable(context.getResources().getDrawable(R.drawable.loading_animation));
        } catch (Exception e) {
            //don't promt
        }
        progressBar.setIndeterminate(true);
        AndroidUtilities.setProgressBarAnimationDuration(progressBar, 1500);
        progressView.addView(progressBar, LayoutHelper.createFrame(32, 32, Gravity.CENTER));

        FrameLayout alertView = new FrameLayout(context);
        alertView.setTag(1);
        alertView.setTranslationY(-AndroidUtilities.dp(50));
        alertView.setVisibility(View.GONE);
        alertView.setBackgroundResource(R.drawable.blockpanel);
        contentView.addView(alertView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 50, Gravity.TOP | Gravity.LEFT));

        TextView alertNameTextView = new TextView(context);
        alertNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        alertNameTextView.setTextColor(Theme.ALERT_PANEL_NAME_TEXT_COLOR);
        alertNameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        alertNameTextView.setSingleLine(true);
        alertNameTextView.setEllipsize(TextUtils.TruncateAt.END);
        alertNameTextView.setMaxLines(1);
        alertView.addView(alertNameTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 8, 5, 8, 0));

        alertTextView = new TextView(context);
        alertTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        alertTextView.setTextColor(Theme.ALERT_PANEL_MESSAGE_TEXT_COLOR);
        alertTextView.setSingleLine(true);
        alertTextView.setEllipsize(TextUtils.TruncateAt.END);
        alertTextView.setMaxLines(1);
        alertView.addView(alertTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 8, 23, 8, 0));

        {
            mentionContainer = new FrameLayout(context) {

                private Drawable background;

                @Override
                public void onDraw(Canvas canvas) {
                    if (mentionListView.getChildCount() <= 0) {
                        return;
                    }
                    /*if (mentionsAdapter.isBotContext() && mentionsAdapter.isMediaLayout() && mentionsAdapter.getBotContextSwitch() == null) {
                        background.setBounds(0, mentionListViewScrollOffsetY - AndroidUtilities.dp(4), getMeasuredWidth(), getMeasuredHeight());
                    } else*/ {
                        background.setBounds(0, mentionListViewScrollOffsetY - AndroidUtilities.dp(2), getMeasuredWidth(), getMeasuredHeight());
                    }
                    background.draw(canvas);
                }

                @Override
                public void setBackgroundResource(int resid) {
                    background = getContext().getResources().getDrawable(resid);
                }

                @Override
                public void requestLayout() {
                    if (mentionListViewIgnoreLayout) {
                        return;
                    }
                    super.requestLayout();
                }
            };
            mentionContainer.setBackgroundResource(R.drawable.compose_panel);
            mentionContainer.setVisibility(View.GONE);
            mentionContainer.setWillNotDraw(false);
            contentView.addView(mentionContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 110, Gravity.LEFT | Gravity.BOTTOM));

            mentionListView = new RecyclerListView(context) {

                private int lastWidth;
                private int lastHeight;

                @Override
                public boolean onInterceptTouchEvent(MotionEvent event) {
                    if (!mentionListViewIsScrolling && mentionListViewScrollOffsetY != 0 && event.getY() < mentionListViewScrollOffsetY) {
                        return false;
                    }
                    boolean result = StickerPreviewViewer.getInstance().onInterceptTouchEvent(event, mentionListView, 0);
                    return super.onInterceptTouchEvent(event) || result;
                }

                @Override
                public boolean onTouchEvent(MotionEvent event) {
                    if (!mentionListViewIsScrolling && mentionListViewScrollOffsetY != 0 && event.getY() < mentionListViewScrollOffsetY) {
                        return false;
                    }
                    //supress warning
                    return super.onTouchEvent(event);
                }

                @Override
                public void requestLayout() {
                    if (mentionListViewIgnoreLayout) {
                        return;
                    }
                    super.requestLayout();
                }

                @Override
                protected void onLayout(boolean changed, int l, int t, int r, int b) {
                    int width = r - l;
                    int height = b - t;

                    int newPosition = -1;
                    int newTop = 0;
                    if (mentionListView != null && mentionListViewLastViewPosition >= 0 && width == lastWidth && height - lastHeight != 0) {
                        newPosition = mentionListViewLastViewPosition;
                        newTop = mentionListViewLastViewTop + height - lastHeight - getPaddingTop();
                    }

                    super.onLayout(changed, l, t, r, b);

                    if (newPosition != -1) {
                        mentionListViewIgnoreLayout = true;
                        if (mentionsAdapter.isBotContext() && mentionsAdapter.isMediaLayout()) {
                            mentionGridLayoutManager.scrollToPositionWithOffset(newPosition, newTop);
                        } else {
                            mentionLayoutManager.scrollToPositionWithOffset(newPosition, newTop);
                        }
                        super.onLayout(false, l, t, r, b);
                        mentionListViewIgnoreLayout = false;
                    }

                    lastHeight = height;
                    lastWidth = width;
                    mentionListViewUpdateLayout();
                }
            };
            mentionListView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return StickerPreviewViewer.getInstance().onTouch(event, mentionListView, 0, mentionsOnItemClickListener);
                }
            });
            mentionListView.setTag(2);
            mentionLayoutManager = new LinearLayoutManager(context) {
                @Override
                public boolean supportsPredictiveItemAnimations() {
                    return false;
                }
            };
            mentionLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            mentionGridLayoutManager = new ExtendedGridLayoutManager(context, 100) {

                private Size size = new Size();

                @Override
                protected Size getSizeForItem(int i) {
                    /*if (mentionsAdapter.getBotContextSwitch() != null) {
                        i++;
                    }
                    Object object = mentionsAdapter.getItem(i);
                    if (object instanceof TLRPC.BotInlineResult) {
                        TLRPC.BotInlineResult inlineResult = (TLRPC.BotInlineResult) object;
                        if (inlineResult.document != null) {
                            size.width = inlineResult.document.thumb != null ? inlineResult.document.thumb.w : 100;
                            size.height = inlineResult.document.thumb != null ? inlineResult.document.thumb.h : 100;
                            for (int b = 0; b < inlineResult.document.attributes.size(); b++) {
                                TLRPC.DocumentAttribute attribute = inlineResult.document.attributes.get(b);
                                if (attribute instanceof TLRPC.TL_documentAttributeImageSize || attribute instanceof TLRPC.TL_documentAttributeVideo) {
                                    size.width = attribute.w;
                                    size.height = attribute.h;
                                    break;
                                }
                            }
                        } else {
                            size.width = inlineResult.w;
                            size.height = inlineResult.h;
                        }
                    }*/
                    return size;
                }

                @Override
                protected int getFlowItemCount() {
                    /*if (mentionsAdapter.getBotContextSwitch() != null) {
                        return getItemCount() - 1;
                    }*/
                    return super.getFlowItemCount();
                }
            };
            mentionGridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    //Object object = mentionsAdapter.getItem(position);
                    /*if (object instanceof TLRPC.TL_inlineBotSwitchPM) {
                        return 100;
                    } else*/ {
                        /*if (mentionsAdapter.getBotContextSwitch() != null) {
                            position--;
                        }*/
                        return mentionGridLayoutManager.getSpanSizeForItem(position);
                    }
                }
            });
            mentionListView.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                    outRect.left = 0;
                    outRect.right = 0;
                    outRect.top = 0;
                    outRect.bottom = 0;
                    if (parent.getLayoutManager() == mentionGridLayoutManager) {
                        int position = parent.getChildAdapterPosition(view);
                        /*if (mentionsAdapter.getBotContextSwitch() != null) {
                            if (position == 0) {
                                return;
                            }
                            position--;
                            if (!mentionGridLayoutManager.isFirstRow(position)) {
                                outRect.top = AndroidUtilities.dp(2);
                            }
                        } else */ {
                            outRect.top = AndroidUtilities.dp(2);
                        }
                        outRect.right = mentionGridLayoutManager.isLastInRow(position) ? 0 : AndroidUtilities.dp(2);
                    }
                }
            });
            mentionListView.setItemAnimator(null);
            mentionListView.setLayoutAnimation(null);
            mentionListView.setClipToPadding(false);
            mentionListView.setLayoutManager(mentionLayoutManager);
            mentionListView.setOverScrollMode(ListView.OVER_SCROLL_NEVER);
            mentionContainer.addView(mentionListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            mentionListView.setAdapter(mentionsAdapter = new MentionsAdapter(context, false, dialog_id, new MentionsAdapter.MentionsAdapterDelegate() {
                @Override
                public void needChangePanelVisibility(boolean show) {
                    if (mentionsAdapter.isBotContext() && mentionsAdapter.isMediaLayout()) {
                        mentionListView.setLayoutManager(mentionGridLayoutManager);
                    } else {
                        mentionListView.setLayoutManager(mentionLayoutManager);
                    }
                    if (show) {
                        if (mentionListAnimation != null) {
                            mentionListAnimation.cancel();
                            mentionListAnimation = null;
                        }

                        if (mentionContainer.getVisibility() == View.VISIBLE) {
                            mentionContainer.setAlpha(1.0f);
                            return;
                        }
                        if (mentionsAdapter.isBotContext() && mentionsAdapter.isMediaLayout()) {
                            mentionGridLayoutManager.scrollToPositionWithOffset(0, 10000);
                        } else {
                            mentionLayoutManager.scrollToPositionWithOffset(0, 10000);
                        }
                        if (allowStickersPanel && (!mentionsAdapter.isBotContext() || (allowContextBotPanel || allowContextBotPanelSecond))) {
                            mentionContainer.setVisibility(View.VISIBLE);
                            mentionContainer.setTag(null);
                            mentionListAnimation = new AnimatorSet();
                            mentionListAnimation.playTogether(
                                    ObjectAnimator.ofFloat(mentionContainer, "alpha", 0.0f, 1.0f)
                            );
                            mentionListAnimation.addListener(new AnimatorListenerAdapterProxy() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    if (mentionListAnimation != null && mentionListAnimation.equals(animation)) {
                                        mentionListAnimation = null;
                                    }
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {
                                    if (mentionListAnimation != null && mentionListAnimation.equals(animation)) {
                                        mentionListAnimation = null;
                                    }
                                }
                            });
                            mentionListAnimation.setDuration(200);
                            mentionListAnimation.start();
                        } else {
                            mentionContainer.setAlpha(1.0f);
                            mentionContainer.setVisibility(View.INVISIBLE);
                        }
                    } else {
                        if (mentionListAnimation != null) {
                            mentionListAnimation.cancel();
                            mentionListAnimation = null;
                        }

                        if (mentionContainer.getVisibility() == View.GONE) {
                            return;
                        }
                        if (allowStickersPanel) {
                            mentionListAnimation = new AnimatorSet();
                            mentionListAnimation.playTogether(
                                    ObjectAnimator.ofFloat(mentionContainer, "alpha", 0.0f)
                            );
                            mentionListAnimation.addListener(new AnimatorListenerAdapterProxy() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    if (mentionListAnimation != null && mentionListAnimation.equals(animation)) {
                                        mentionContainer.setVisibility(View.GONE);
                                        mentionContainer.setTag(null);
                                        mentionListAnimation = null;
                                    }
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {
                                    if (mentionListAnimation != null && mentionListAnimation.equals(animation)) {
                                        mentionListAnimation = null;
                                    }
                                }
                            });
                            mentionListAnimation.setDuration(200);
                            mentionListAnimation.start();
                        } else {
                            mentionContainer.setTag(null);
                            mentionContainer.setVisibility(View.GONE);
                        }
                    }
                }

                /*
                @Override
                public void onContextSearch(boolean searching) {
                    if (chatActivityEnterView != null) {
                        chatActivityEnterView.setCaption(mentionsAdapter.getBotCaption());
                        chatActivityEnterView.showContextProgress(searching);
                    }
                }
                */

                @Override
                public void onContextClick(TLRPC.BotInlineResult result) {
                    if (getParentActivity() == null || result.content_url == null) {
                        return;
                    }
                    if (result.type.equals("video") || result.type.equals("web_player_video")) {
                        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
                        builder.setCustomView(new WebFrameLayout(getParentActivity(), builder.create(), result.title != null ? result.title : "", result.description, result.content_url, result.content_url, result.w, result.h));
                        builder.setUseFullWidth(true);
                        showDialog(builder.create());
                    } else {
                        Browser.openUrl(getParentActivity(), result.content_url);
                    }
                }
            }));

            mentionsAdapter.setParentFragment(this);
            mentionsAdapter.setChatInfo(info);
            mentionsAdapter.setNeedUsernames(currentChat != null);
            mentionsAdapter.setNeedBotContext(true);
            mentionListView.setOnItemClickListener(mentionsOnItemClickListener = new RecyclerListView.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    Object object = mentionsAdapter.getItem(position);
                    int start = mentionsAdapter.getResultStartPosition();
                    int len = mentionsAdapter.getResultLength();
                    if (object instanceof TLRPC.User) {
                        TLRPC.User user = (TLRPC.User) object;
                        if (user != null) {
                            if (user.username != null) {
                                chatActivityEnterView.replaceWithText(start, len, "@" + user.username + " ");
                            } else {
                                String name = user.first_name;
                                if (name == null || name.length() == 0) {
                                    name = user.last_name;
                                }
                                Spannable spannable = new SpannableString(name + " ");
                                spannable.setSpan(new URLSpanUserMention("" + user.id), 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                chatActivityEnterView.replaceWithText(start, len, spannable);
                            }
                        }
                    } else if (object instanceof String) {
                        chatActivityEnterView.replaceWithText(start, len, object + " ");
                    }
                }
            });

            mentionListView.setOnItemLongClickListener(new RecyclerListView.OnItemLongClickListener() {
                @Override
                public boolean onItemClick(View view, int position) {
                    if (getParentActivity() == null || !mentionsAdapter.isLongClickEnabled()) {
                        return false;
                    }
                    Object object = mentionsAdapter.getItem(position);
                    if (object instanceof String) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setMessage(LocaleController.getString("ClearSearch", R.string.ClearSearch));
                        builder.setPositiveButton(LocaleController.getString("ClearButton", R.string.ClearButton).toUpperCase(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mentionsAdapter.clearRecentHashtags();
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                        showDialog(builder.create());
                        return true;
                    }
                    return false;
                }
            });

            mentionListView.setOnScrollListener(new RecyclerView.OnScrollListener() {

                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    mentionListViewIsScrolling = newState == RecyclerView.SCROLL_STATE_DRAGGING;
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    int lastVisibleItem;
                    if (mentionsAdapter.isBotContext() && mentionsAdapter.isMediaLayout()) {
                        lastVisibleItem = mentionGridLayoutManager.findLastVisibleItemPosition();
                    } else {
                        lastVisibleItem = mentionLayoutManager.findLastVisibleItemPosition();
                    }
                    int visibleItemCount = lastVisibleItem == RecyclerView.NO_POSITION ? 0 : lastVisibleItem;
                    if (visibleItemCount > 0 && lastVisibleItem > mentionsAdapter.getItemCount() - 5) {
                        mentionsAdapter.searchForContextBotForNextOffset();
                    }
                    mentionListViewUpdateLayout();
                }
            });
        }

        pagedownButton = new FrameLayout(context);
        pagedownButton.setVisibility(View.INVISIBLE);
        contentView.addView(pagedownButton, LayoutHelper.createFrame(46, 59, Gravity.RIGHT | Gravity.BOTTOM, 0, 0, 7, 5));
        pagedownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (returnToMessageId > 0) {
                    scrollToMessageId(returnToMessageId, 0, true);
                } else {
                    scrollToLastMessage(true);
                }
            }
        });

        ImageView pagedownButtonImage = new ImageView(context);
        pagedownButtonImage.setImageResource(R.drawable.pagedown);
        pagedownButton.addView(pagedownButtonImage, LayoutHelper.createFrame(46, 46, Gravity.LEFT | Gravity.BOTTOM));

        pagedownButtonCounter = new TextView(context);
        pagedownButtonCounter.setVisibility(View.INVISIBLE);
        pagedownButtonCounter.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
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
        chatActivityEnterView.setId(id_chat_compose_panel);

        chatActivityEnterView.setAllowStickersAndGifs(false, false); // for the moment, we have no stickers

        contentView.addView(chatActivityEnterView, contentView.getChildCount() - 1, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM));
        chatActivityEnterView.setDelegate(new ChatActivityEnterView.ChatActivityEnterViewDelegate() {
            @Override
            public void onMessageSend(CharSequence message) {
                moveScrollToLastMessage();
                showReplyPanel(false, null, null, null, false, true);
                if (mentionsAdapter != null) {
                    mentionsAdapter.addHashtagsFromMessage(message);
                }
            }

            @Override
            public void onTextChanged(final CharSequence text, boolean bigChange) {
                MediaController.getInstance().setInputFieldHasText(text != null && text.length() != 0 || chatActivityEnterView.isEditingMessage());
                if (stickersAdapter != null && !chatActivityEnterView.isEditingMessage()) {
                    stickersAdapter.loadStikersForEmoji(text);
                }
                if (mentionsAdapter != null && text!=null ) {
                    mentionsAdapter.searchUsernameOrHashtag(text.toString(), chatActivityEnterView.getCursorPosition(), messages);
                }
                if (waitingForCharaterEnterRunnable != null) {
                    AndroidUtilities.cancelRunOnUIThread(waitingForCharaterEnterRunnable);
                    waitingForCharaterEnterRunnable = null;
                }
                if (chatActivityEnterView.isMessageWebPageSearchEnabled() && (!chatActivityEnterView.isEditingMessage() || !chatActivityEnterView.isEditingCaption())) {
                    if (bigChange) {
                        searchLinks(text, true);
                    } else {
                        waitingForCharaterEnterRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (this == waitingForCharaterEnterRunnable) {
                                    searchLinks(text, false);
                                    waitingForCharaterEnterRunnable = null;
                                }
                            }
                        };
                        AndroidUtilities.runOnUIThread(waitingForCharaterEnterRunnable, AndroidUtilities.WEB_URL == null ? 3000 : 1000);
                    }
                }
            }

            @Override
            public void needSendTyping() {
                MessagesController.getInstance().sendTyping(dialog_id, 0, classGuid);
            }

            @Override
            public void onAttachButtonHidden() {
                if (actionBar.isSearchFieldVisible()) {
                    return;
                }
                if (attachItem != null) {
                    attachItem.setVisibility(View.VISIBLE);
                }
                if (headerItem != null) {
                    headerItem.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAttachButtonShow() {
                if (actionBar.isSearchFieldVisible()) {
                    return;
                }
                if (attachItem != null) {
                    attachItem.setVisibility(View.GONE);
                }
                if (headerItem != null) {
                    headerItem.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onWindowSizeChanged(int size) {
                if (size < AndroidUtilities.dp(72) + ActionBar.getCurrentActionBarHeight()) {
                    allowStickersPanel = false;
                    if (stickersPanel.getVisibility() == View.VISIBLE) {
                        stickersPanel.setVisibility(View.INVISIBLE);
                    }
                    if (mentionContainer != null && mentionContainer.getVisibility() == View.VISIBLE) {
                        mentionContainer.setVisibility(View.INVISIBLE);
                    }
                } else {
                    allowStickersPanel = true;
                    if (stickersPanel.getVisibility() == View.INVISIBLE) {
                        stickersPanel.setVisibility(View.VISIBLE);
                    }
                    if (mentionContainer != null && mentionContainer.getVisibility() == View.INVISIBLE && (!mentionsAdapter.isBotContext() || (allowContextBotPanel || allowContextBotPanelSecond))) {
                        mentionContainer.setVisibility(View.VISIBLE);
                        mentionContainer.setTag(null);
                    }
                }

                allowContextBotPanel = !chatActivityEnterView.isPopupShowing();
            }

            @Override
            public void onStickersTab(boolean opened) {
                if (emojiButtonRed != null) {
                    emojiButtonRed.setVisibility(View.GONE);
                }
                allowContextBotPanelSecond = !opened;
            }
        });

        FrameLayout replyLayout = new FrameLayout(context) {
            @Override
            public void setTranslationY(float translationY) {
                super.setTranslationY(translationY);
                if (chatActivityEnterView != null) {
                    chatActivityEnterView.invalidate();
                }
                if (getVisibility() != GONE) {
                    //int height = getLayoutParams().height;
                    if (chatListView != null) {
                        chatListView.setTranslationY(translationY);
                    }
                    if (progressView != null) {
                        progressView.setTranslationY(translationY);
                    }
                    if (mentionContainer != null) {
                        mentionContainer.setTranslationY(translationY);
                    }
                    if (pagedownButton != null) {
                        pagedownButton.setTranslationY(translationY);
                    }
                }
            }

            @Override
            public boolean hasOverlappingRendering() {
                return false;
            }

            @Override
            public void setVisibility(int visibility) {
                super.setVisibility(visibility);
                if (visibility == GONE) {
                    if (chatListView != null) {
                        chatListView.setTranslationY(0);
                    }
                    if (progressView != null) {
                        progressView.setTranslationY(0);
                    }
                    if (mentionContainer != null) {
                        mentionContainer.setTranslationY(0);
                    }
                    if (pagedownButton != null) {
                        pagedownButton.setTranslationY(pagedownButton.getTag() == null ? AndroidUtilities.dp(100) : 0);
                    }
                }
            }
        };
        replyLayout.setClickable(true);
        chatActivityEnterView.addTopView(replyLayout, 48);

        View lineView = new View(context);
        lineView.setBackgroundColor(0xffe8e8e8);
        replyLayout.addView(lineView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1, Gravity.BOTTOM | Gravity.LEFT));

        ImageView replyIconImageView = new ImageView(context);
        replyIconImageView.setScaleType(ImageView.ScaleType.CENTER);
        replyLayout.addView(replyIconImageView, LayoutHelper.createFrame(52, 46, Gravity.TOP | Gravity.LEFT));

        ImageView imageView = new ImageView(context);
        imageView.setImageResource(R.drawable.delete_reply);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        replyLayout.addView(imageView, LayoutHelper.createFrame(52, 46, Gravity.RIGHT | Gravity.TOP, 0, 0.5f, 0, 0));
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReplyPanel(false, null, null, foundWebPage, true, true);
            }
        });

        SimpleTextView replyNameTextView = new SimpleTextView(context);
        replyNameTextView.setTextSize(14);
        replyNameTextView.setTextColor(Theme.REPLY_PANEL_NAME_TEXT_COLOR);
        replyNameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        replyLayout.addView(replyNameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 18, Gravity.TOP | Gravity.LEFT, 52, 6, 52, 0));

        replyObjectTextView = new SimpleTextView(context);
        replyObjectTextView.setTextSize(14);
        replyObjectTextView.setTextColor(Theme.REPLY_PANEL_MESSAGE_TEXT_COLOR);
        replyLayout.addView(replyObjectTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 18, Gravity.TOP | Gravity.LEFT, 52, 24, 52, 0));

        replyImageView = new BackupImageView(context);
        replyLayout.addView(replyImageView, LayoutHelper.createFrame(34, 34, Gravity.TOP | Gravity.LEFT, 52, 6, 0, 0));

        stickersPanel = new FrameLayout(context);
        stickersPanel.setVisibility(View.GONE);
        contentView.addView(stickersPanel, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 81.5f, Gravity.LEFT | Gravity.BOTTOM, 0, 0, 0, 38));

        stickersListView = new RecyclerListView(context) {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent event) {
                boolean result = StickerPreviewViewer.getInstance().onInterceptTouchEvent(event, stickersListView, 0);
                return super.onInterceptTouchEvent(event) || result;
            }
        };
        stickersListView.setTag(3);
        stickersListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return StickerPreviewViewer.getInstance().onTouch(event, stickersListView, 0, stickersOnItemClickListener);
            }
        });
        stickersListView.setDisallowInterceptTouchEvents(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        stickersListView.setLayoutManager(layoutManager);
        stickersListView.setClipToPadding(false);
        stickersListView.setOverScrollMode(RecyclerListView.OVER_SCROLL_NEVER);
        stickersPanel.addView(stickersListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 78));
        initStickers();

        imageView = new ImageView(context);
        imageView.setImageResource(R.drawable.stickers_back_arrow);
        stickersPanel.addView(imageView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 53, 0, 0, 0));

        searchContainer = new FrameLayout(context);
        searchContainer.setBackgroundResource(R.drawable.compose_panel);
        searchContainer.setVisibility(View.INVISIBLE);
        searchContainer.setFocusable(true);
        searchContainer.setFocusableInTouchMode(true);
        searchContainer.setClickable(true);
        searchContainer.setBackgroundResource(R.drawable.compose_panel);
        searchContainer.setPadding(0, AndroidUtilities.dp(3), 0, 0);
        contentView.addView(searchContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 51, Gravity.BOTTOM));

        searchUpButton = new ImageView(context);
        searchUpButton.setScaleType(ImageView.ScaleType.CENTER);
        searchUpButton.setImageResource(R.drawable.search_up);
        searchContainer.addView(searchUpButton, LayoutHelper.createFrame(48, 48));
        searchUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //MessagesSearchQuery.searchMessagesInChat(null, dialog_id, mergeDialogId, classGuid, 1);
            }
        });

        searchDownButton = new ImageView(context);
        searchDownButton.setScaleType(ImageView.ScaleType.CENTER);
        searchDownButton.setImageResource(R.drawable.search_down);
        searchContainer.addView(searchDownButton, LayoutHelper.createFrame(48, 48, Gravity.LEFT | Gravity.TOP, 48, 0, 0, 0));
        searchDownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //MessagesSearchQuery.searchMessagesInChat(null, dialog_id, mergeDialogId, classGuid, 2);
            }
        });

        searchCountText = new SimpleTextView(context);
        searchCountText.setTextColor(Theme.CHAT_SEARCH_COUNT_TEXT_COLOR);
        searchCountText.setTextSize(15);
        searchCountText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        searchContainer.addView(searchCountText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 108, 0, 0, 0));

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
        //bottomOverlayChatText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        bottomOverlayChatText.setTextColor(0xffb2b2b2); // same as hintTextColor of the keyboard
        bottomOverlayChat.addView(bottomOverlayChatText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        chatAdapter.updateRows();
        if (loading && messages.isEmpty()) {
            progressView.setVisibility(View.VISIBLE);
            chatListView.setEmptyView(null);
        } else {
            progressView.setVisibility(View.INVISIBLE);
            chatListView.setEmptyView(emptyViewContainer);
        }

        //chatActivityEnterView.setButtons(userBlocked ? null : botButtons);

        if (!AndroidUtilities.isTablet() || AndroidUtilities.isSmallTablet()) {
            contentView.addView(playerView = new PlayerView(context, this), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 39, Gravity.TOP | Gravity.LEFT, 0, -36, 0, 0));
        }

        updateBottomOverlay();

        fixLayoutInternal();

        return fragmentView;
    }

    private void mentionListViewUpdateLayout() {
        if (mentionListView.getChildCount() <= 0) {
            mentionListViewScrollOffsetY = 0;
            mentionListViewLastViewPosition = -1;
            return;
        }
        View child = mentionListView.getChildAt(mentionListView.getChildCount() - 1);
        MentionsAdapter.Holder holder = (MentionsAdapter.Holder) mentionListView.findContainingViewHolder(child);
        if (holder != null) {
            mentionListViewLastViewPosition = holder.getAdapterPosition();
            mentionListViewLastViewTop = child.getTop();
        } else {
            mentionListViewLastViewPosition = -1;
        }

        child = mentionListView.getChildAt(0);
        holder = (MentionsAdapter.Holder) mentionListView.findContainingViewHolder(child);
        int newOffset = child.getTop() > 0 && holder != null && holder.getAdapterPosition() == 0 ? child.getTop() : 0;
        if (mentionListViewScrollOffsetY != newOffset) {
            mentionListView.setTopGlowOffset(mentionListViewScrollOffsetY = newOffset);
            mentionListView.invalidate();
            mentionContainer.invalidate();
        }
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
                    if (getParentActivity() == null) {
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
                            SendMessagesHelper.prepareSendingPhotos(photos, null, dialog_id, replyingMessageObject, captions);
                            showReplyPanel(false, null, null, null, false, true);
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
        for (int a = messages.size() - 1; a >= 0; a--) {
            MessageObject messageObject = messages.get(a);
            if (messageObject.isVoice() && messageObject.isContentUnread() && !messageObject.isOut() && messageObject.messageOwner.to_id.channel_id == 0) {
                MediaController.getInstance().setVoiceMessagesPlaylist(MediaController.getInstance().playAudio(messageObject) ? createVoiceMessagesPlaylist(messageObject, true) : null, true);
                return true;
            }
        }
        if (Build.VERSION.SDK_INT >= 23 && getParentActivity() != null) {
            if (getParentActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                getParentActivity().requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 3);
                return true;
            }
        }
        return false;
    }

    private void initStickers() {
        if (chatActivityEnterView == null || getParentActivity() == null || stickersAdapter != null ) {
            return;
        }
        if (stickersAdapter != null) {
            stickersAdapter.onDestroy();
        }
        stickersListView.setPadding(AndroidUtilities.dp(18), 0, AndroidUtilities.dp(18), 0);
        stickersListView.setAdapter(stickersAdapter = new StickersAdapter(getParentActivity(), new StickersAdapter.StickersAdapterDelegate() {
            @Override
            public void needChangePanelVisibility(final boolean show) {
                if (show && stickersPanel.getVisibility() == View.VISIBLE || !show && stickersPanel.getVisibility() == View.GONE) {
                    return;
                }
                if (show) {
                    stickersListView.scrollToPosition(0);
                    stickersPanel.setVisibility(allowStickersPanel ? View.VISIBLE : View.INVISIBLE);
                }
                if (runningAnimation != null) {
                    runningAnimation.cancel();
                    runningAnimation = null;
                }
                if (stickersPanel.getVisibility() != View.INVISIBLE) {
                    runningAnimation = new AnimatorSet();
                    runningAnimation.playTogether(
                            ObjectAnimator.ofFloat(stickersPanel, "alpha", show ? 0.0f : 1.0f, show ? 1.0f : 0.0f)
                    );
                    runningAnimation.setDuration(150);
                    runningAnimation.addListener(new AnimatorListenerAdapterProxy() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (runningAnimation != null && runningAnimation.equals(animation)) {
                                if (!show) {
                                    stickersAdapter.clearStickers();
                                    stickersPanel.setVisibility(View.GONE);
                                    if (StickerPreviewViewer.getInstance().isVisible()) {
                                        StickerPreviewViewer.getInstance().close();
                                    }
                                    StickerPreviewViewer.getInstance().reset();
                                }
                                runningAnimation = null;
                            }
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            if (runningAnimation != null && runningAnimation.equals(animation)) {
                                runningAnimation = null;
                            }
                        }
                    });
                    runningAnimation.start();
                } else if (!show) {
                    stickersPanel.setVisibility(View.GONE);
                }
            }
        }));
        stickersListView.setOnItemClickListener(stickersOnItemClickListener = new RecyclerListView.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                TLRPC.Document document = stickersAdapter.getItem(position);
                if (document instanceof TLRPC.TL_document) {
                    SendMessagesHelper.getInstance().sendSticker(document, dialog_id, replyingMessageObject);
                    showReplyPanel(false, null, null, null, false, true);
                    chatActivityEnterView.addStickerToRecent(document);
                }
                chatActivityEnterView.setFieldText("");
            }
        });
    }

    private void showGifHint() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        if (preferences.getBoolean("gifhint", false)) {
            return;
        }
        preferences.edit().putBoolean("gifhint", true).commit();

        if (getParentActivity() == null || fragmentView == null || gifHintTextView != null) {
            return;
        }
        if (!allowContextBotPanelSecond) {
            if (chatActivityEnterView != null) {
                chatActivityEnterView.setOpenGifsTabFirst();
            }
            return;
        }
        SizeNotifierFrameLayout frameLayout = (SizeNotifierFrameLayout) fragmentView;
        int index = frameLayout.indexOfChild(chatActivityEnterView);
        if (index == -1) {
            return;
        }
        chatActivityEnterView.setOpenGifsTabFirst();
        emojiButtonRed = new View(getParentActivity());
        emojiButtonRed.setBackgroundResource(R.drawable.redcircle);
        frameLayout.addView(emojiButtonRed, index + 1, LayoutHelper.createFrame(10, 10, Gravity.BOTTOM | Gravity.LEFT, 30, 0, 0, 27));

        gifHintTextView = new TextView(getParentActivity());
        gifHintTextView.setBackgroundResource(R.drawable.tooltip);
        gifHintTextView.setTextColor(Theme.CHAT_GIF_HINT_TEXT_COLOR);
        gifHintTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        gifHintTextView.setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10), 0);
        gifHintTextView.setText(LocaleController.getString("TapHereGifs", R.string.TapHereGifs));
        gifHintTextView.setGravity(Gravity.CENTER_VERTICAL);
        frameLayout.addView(gifHintTextView, index + 1, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 32, Gravity.LEFT | Gravity.BOTTOM, 5, 0, 0, 3));

        AnimatorSet AnimatorSet = new AnimatorSet();
        AnimatorSet.playTogether(
                ObjectAnimator.ofFloat(gifHintTextView, "alpha", 0.0f, 1.0f),
                ObjectAnimator.ofFloat(emojiButtonRed, "alpha", 0.0f, 1.0f)
        );
        AnimatorSet.addListener(new AnimatorListenerAdapterProxy() {
            @Override
            public void onAnimationEnd(Animator animation) {
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if (gifHintTextView == null) {
                            return;
                        }
                        AnimatorSet AnimatorSet = new AnimatorSet();
                        AnimatorSet.playTogether(
                                ObjectAnimator.ofFloat(gifHintTextView, "alpha", 0.0f)
                        );
                        AnimatorSet.addListener(new AnimatorListenerAdapterProxy() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (gifHintTextView != null) {
                                    gifHintTextView.setVisibility(View.GONE);
                                }
                            }
                        });
                        AnimatorSet.setDuration(300);
                        AnimatorSet.start();
                    }
                }, 2000);
            }
        });
        AnimatorSet.setDuration(300);
        AnimatorSet.start();
    }

    private void checkScrollForLoad(boolean scroll) {
        if (chatLayoutManager == null || paused) {
            return;
        }
        int firstVisibleItem = chatLayoutManager.findFirstVisibleItemPosition();
        int visibleItemCount = firstVisibleItem == RecyclerView.NO_POSITION ? 0 : Math.abs(chatLayoutManager.findLastVisibleItemPosition() - firstVisibleItem) + 1;
        if (visibleItemCount > 0) {
            int totalItemCount = chatAdapter.getItemCount();
            int checkLoadCount;
            if (scroll) {
                checkLoadCount = 25;
            } else  {
                checkLoadCount = 5;
            }
            if (firstVisibleItem <= checkLoadCount && !loading) {
                if (!endReached) {
                    loading = true;
                    /*
                    if (messagesByDays.size() != 0) {
                        MessagesController.getInstance().loadMessages(dialog_id, 50, maxMessageId[0], !cacheEndReached[0], minDate[0], classGuid, 0, 0, ChatObject.isChannel(currentChat), lastLoadIndex++);
                    } else {
                        MessagesController.getInstance().loadMessages(dialog_id, 50, 0, !cacheEndReached[0], minDate[0], classGuid, 0, 0, ChatObject.isChannel(currentChat), lastLoadIndex++);
                    }
                    */
                }
            }
            if (!loadingForward && firstVisibleItem + visibleItemCount >= totalItemCount - 10) {
                if (!forwardEndReached) {
                    //MessagesController.getInstance().loadMessages(dialog_id, 50, minMessageId[0], true, maxDate[0], classGuid, 1, 0, ChatObject.isChannel(currentChat), lastLoadIndex++);
                    loadingForward = true;
                }
            }
        }
    }

    private void processSelectedAttach(int which) {
        if (which == ChatAttachAlert.ATTACH_BUTTON_IDX_CAMERA ) {
            try {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File image = AndroidUtilities.generatePicturePath();
                if (image != null) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(image));
                    currentPicturePath = image.getAbsolutePath();
                }
                startActivityForResult(takePictureIntent, 0);
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
        } else if (which == ChatAttachAlert.ATTACH_BUTTON_IDX_GALLERY ) {
            if (Build.VERSION.SDK_INT >= 23 && getParentActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                getParentActivity().requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 4);
                return;
            }
            PhotoAlbumPickerActivity fragment = new PhotoAlbumPickerActivity(false, true, ChatActivity.this);
            fragment.setDelegate(new PhotoAlbumPickerActivity.PhotoAlbumPickerActivityDelegate() {
                @Override
                public void didSelectPhotos(ArrayList<String> photos, ArrayList<String> captions, ArrayList<MediaController.SearchImage> webPhotos) {
                    SendMessagesHelper.prepareSendingPhotos(photos, null, dialog_id, replyingMessageObject, captions);
                    SendMessagesHelper.prepareSendingPhotosSearch(webPhotos, dialog_id, replyingMessageObject);
                    showReplyPanel(false, null, null, null, false, true);
                    m_mrChat.cleanDraft();
                }

                @Override
                public void startPhotoSelectActivity() {
                    try {
                        Intent videoPickerIntent = new Intent();
                        videoPickerIntent.setType("video/*");
                        videoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
                        videoPickerIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, (long) (1024 * 1024 * 1536));

                        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                        photoPickerIntent.setType("image/*");
                        Intent chooserIntent = Intent.createChooser(photoPickerIntent, null);
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{videoPickerIntent});

                        startActivityForResult(chooserIntent, 1);
                    } catch (Exception e) {
                        FileLog.e("messenger", e);
                    }
                }

                @Override
                public boolean didSelectVideo(String path) {
                    if (Build.VERSION.SDK_INT >= 16) {
                        return !openVideoEditor(path, true, true);
                    } else {
                        SendMessagesHelper.prepareSendingVideo(path, 0, 0, 0, 0, null, dialog_id, replyingMessageObject);
                        showReplyPanel(false, null, null, null, false, true);
                        m_mrChat.cleanDraft();
                        return true;
                    }
                }
            });
            presentFragment(fragment);
        } else if (which == ChatAttachAlert.ATTACH_BUTTON_IDX_VIDEO) {
            try {
                Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                File video = AndroidUtilities.generateVideoPath();
                if (video != null) {
                    if (Build.VERSION.SDK_INT >= 18) {
                        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(video));
                    }
                    takeVideoIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, (long) (1024 * 1024 * 1536));
                    currentPicturePath = video.getAbsolutePath();
                }
                startActivityForResult(takeVideoIntent, 2);
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
        } else if (which == ChatAttachAlert.ATTACH_BUTTON_IDX_LOCATION ) {
            /* Telegram-FOSS  Disabled for now.*/
            Toast.makeText(getParentActivity(), LocaleController.getString("NotYetImplemented", R.string.NotYetImplemented), Toast.LENGTH_LONG).show();
            /*AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                @Override
                public void didSelectLocation(TLRPC.MessageMedia location) {
                    SendMessagesHelper.getInstance().sendMessage(location, dialog_id, replyingMessageObject, null, null);
                    moveScrollToLastMessage();
                    showReplyPanel(false, null, null, null, false, true);
                    DraftQuery.cleanDraft(dialog_id, true);
                    if (paused) {
                        scrollToTopOnResume = true;
                    }
            });
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            builder.setMessage("Send current Location?");
            showDialog(builder.create());*/
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
                    SendMessagesHelper.prepareSendingDocuments(files, files, null, null, dialog_id, replyingMessageObject);
                    showReplyPanel(false, null, null, null, false, true);
                    m_mrChat.cleanDraft();
                }

                @Override
                public void startDocumentSelectActivity() {
                    try {
                        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                        photoPickerIntent.setType("*/*");
                        startActivityForResult(photoPickerIntent, 21);
                    } catch (Exception e) {
                        FileLog.e("messenger", e);
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
                public void didSelectAudio(ArrayList<MessageObject> audios) {
                    SendMessagesHelper.prepareSendingAudioDocuments(audios, dialog_id, replyingMessageObject);
                    showReplyPanel(false, null, null, null, false, true);
                    m_mrChat.cleanDraft();
                }
            });
            presentFragment(fragment);
        } else if (which == ChatAttachAlert.ATTACH_BUTTON_IDX_CONTACT ) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (getParentActivity().checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    getParentActivity().requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 5);
                    return;
                }
            }
            try {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                startActivityForResult(intent, 31);
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
        }
    }

    @Override
    public boolean dismissDialogOnPause(Dialog dialog) {
        return !(dialog == chatAttachAlert && PhotoViewer.getInstance().isVisible()) && super.dismissDialogOnPause(dialog);
    }

    private void searchLinks(final CharSequence charSequence, final boolean force) {
        if (force && foundWebPage != null) {
            if (foundWebPage.url != null) {
                int index = TextUtils.indexOf(charSequence, foundWebPage.url);
                char lastChar = 0;
                boolean lenEqual = false;
                if (index == -1) {
                    if (foundWebPage.display_url != null) {
                        index = TextUtils.indexOf(charSequence, foundWebPage.display_url);
                        lenEqual = index != -1 && index + foundWebPage.display_url.length() == charSequence.length();
                        lastChar = index != -1 && !lenEqual ? charSequence.charAt(index + foundWebPage.display_url.length()) : 0;
                    }
                } else {
                    lenEqual = index + foundWebPage.url.length() == charSequence.length();
                    lastChar = !lenEqual ? charSequence.charAt(index + foundWebPage.url.length()) : 0;
                }
                if (index != -1 && (lenEqual || lastChar == ' ' || lastChar == ',' || lastChar == '.' || lastChar == '!' || lastChar == '/')) {
                    return;
                }
            }
            showReplyPanel(false, null, null, foundWebPage, false, true);
        }
        Utilities.searchQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (linkSearchRequestId != 0) {
                    ConnectionsManager.getInstance().cancelRequest(linkSearchRequestId, true);
                    linkSearchRequestId = 0;
                }
                ArrayList<CharSequence> urls = null;
                CharSequence textToCheck;
                try {
                    Matcher m = AndroidUtilities.WEB_URL.matcher(charSequence);
                    while (m.find()) {
                        if (m.start() > 0) {
                            if (charSequence.charAt(m.start() - 1) == '@') {
                                continue;
                            }
                        }
                        if (urls == null) {
                            urls = new ArrayList<>();
                        }
                        urls.add(charSequence.subSequence(m.start(), m.end()));
                    }
                    if (urls != null && foundUrls != null && urls.size() == foundUrls.size()) {
                        boolean clear = true;
                        for (int a = 0; a < urls.size(); a++) {
                            if (!TextUtils.equals(urls.get(a), foundUrls.get(a))) {
                                clear = false;
                            }
                        }
                        if (clear) {
                            return;
                        }
                    }
                    foundUrls = urls;
                    if (urls == null) {
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                if (foundWebPage != null) {
                                    showReplyPanel(false, null, null, foundWebPage, false, true);
                                    foundWebPage = null;
                                }
                            }
                        });
                        return;
                    }
                    textToCheck = TextUtils.join(" ", urls);
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                    String text = charSequence.toString().toLowerCase();
                    if (charSequence.length() < 13 || !text.contains("http://") && !text.contains("https://")) {
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                if (foundWebPage != null) {
                                    showReplyPanel(false, null, null, foundWebPage, false, true);
                                    foundWebPage = null;
                                }
                            }
                        });
                        return;
                    }
                    textToCheck = charSequence;
                }

                linkSearchRequestId = 0; // was: TL_messages_getWebPagePreview ...
            }
        });
    }

    public void showReplyPanel(boolean show, MessageObject messageObjectToReply, ArrayList<MessageObject> messageObjectsToForward, TLRPC.WebPage webPage, boolean cancel, boolean animated) {
    }

    private void moveScrollToLastMessage() {
        if (chatListView != null && !messages.isEmpty()) {
            chatLayoutManager.scrollToPositionWithOffset(messages.size() - 1, -100000 - chatListView.getPaddingTop());
        }
    }

    private void clearChatData() {
        messages.clear();
        messagesByDays.clear();
        progressView.setVisibility(View.VISIBLE);
        chatListView.setEmptyView(null);
        messagesDict.clear();
        maxMessageId = Integer.MAX_VALUE;
        minMessageId = Integer.MIN_VALUE;
        maxDate = Integer.MIN_VALUE;
        minDate = 0;
        endReached = false;
        forwardEndReached = true;
        first = true;
        firstLoading = true;
        loading = true;
        loadingForward = false;
        startLoadFromMessageId = 0;
        last_message_id = 0;
        chatAdapter.notifyDataSetChanged();
    }

    private void scrollToLastMessage(boolean pagedown) {
        if (forwardEndReached && first_unread_id == 0 && startLoadFromMessageId == 0) {
            if (pagedown && chatLayoutManager.findLastCompletelyVisibleItemPosition() == chatAdapter.getItemCount() - 1) {
                showPagedownButton(false, true);
                highlightMessageId = Integer.MAX_VALUE;
                updateVisibleRows();
            } else {
                chatLayoutManager.scrollToPositionWithOffset(messages.size() - 1, -100000 - chatListView.getPaddingTop());
            }
        } else {
            clearChatData();
            //MessagesController.getInstance().loadMessages(dialog_id, 30, 0, true, 0, classGuid, 0, 0, ChatObject.isChannel(currentChat), lastLoadIndex++);
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
        boolean muted = MessagesController.getInstance().isDialogMuted(dialog_id);
        if (!muted) {
            // EDIT BY MR
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity()); // was: BottomSheet.Builder
            //builder.setTitle(LocaleController.getString("Notifications", R.string.Notifications)); -- a title seems more confusing than helping -- the user has clicked "mute" before and there are several options all starting with "mute...", I think, this is very clear
            CharSequence[] items = new CharSequence[]{
                    LocaleController.formatString("MuteFor", R.string.MuteFor, LocaleController.formatPluralString("Hours", 1)),
                    LocaleController.formatString("MuteFor", R.string.MuteFor, LocaleController.formatPluralString("Hours", 8)),
                    LocaleController.formatString("MuteFor", R.string.MuteFor, LocaleController.formatPluralString("Days", 2)),
                    LocaleController.getString("MuteAlways", R.string.MuteAlways)
            };
            builder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            int untilTime = ConnectionsManager.getInstance().getCurrentTime();
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
                            editor.commit();
                            /*NotificationsController.getInstance().removeNotificationsForDialog(dialog_id);
                            TLRPC.TL_dialog dialog = MessagesController.getInstance().dialogs_dict.get(dialog_id);
                            if (dialog != null) {
                                dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
                                dialog.notify_settings.mute_until = untilTime;
                            }
                            */
                            NotificationsController.updateServerNotificationsSettings(dialog_id);
                        }
                    }
            );
            showDialog(builder.create());
            // EDIT BY MR
        } else {
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("notify2_" + dialog_id, 0);
            editor.commit();
            /*TLRPC.TL_dialog dialog = MessagesController.getInstance().dialogs_dict.get(dialog_id);
            if (dialog != null) {
                dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
            }
            */
            NotificationsController.updateServerNotificationsSettings(dialog_id);
        }
    }

    private void createChatByDeaddropMsgId(int messageId)
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
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                    int chatId = MrMailbox.createChatByContactId(fromId);
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
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.setMessage(AndroidUtilities.replaceTags(LocaleController.formatString("AskStartChatWith", R.string.AskStartChatWith, name)));
        showDialog(builder.create());
    }

    private void scrollToMessageId(int id, int fromMessageId, boolean select) {
        MessageObject object = messagesDict.get(id);
        boolean query = false;
        if (object != null) {
            int index = messages.indexOf(object);
            if (index != -1) {
                if (select) {
                    highlightMessageId = id;
                } else {
                    highlightMessageId = Integer.MAX_VALUE;
                }
                final int yOffset = Math.max(0, (chatListView.getHeight() - object.getApproximateHeight()) / 2);
                if (messages.get(messages.size() - 1) == object) {
                    chatLayoutManager.scrollToPositionWithOffset(0, -chatListView.getPaddingTop() - AndroidUtilities.dp(7) + yOffset);
                } else {
                    chatLayoutManager.scrollToPositionWithOffset(chatAdapter.messagesStartRow + messages.size() - messages.indexOf(object) - 1, -chatListView.getPaddingTop() - AndroidUtilities.dp(7) + yOffset);
                }
                updateVisibleRows();
                boolean found = false;
                int count = chatListView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View view = chatListView.getChildAt(a);
                    if (view instanceof ChatMessageCell) {
                        ChatMessageCell cell = (ChatMessageCell) view;
                        MessageObject messageObject = cell.getMessageObject();
                        if (messageObject != null && messageObject.getId() == object.getId()) {
                            found = true;
                            break;
                        }
                    } else if (view instanceof ChatActionCell) {
                        ChatActionCell cell = (ChatActionCell) view;
                        MessageObject messageObject = cell.getMessageObject();
                        if (messageObject != null && messageObject.getId() == object.getId()) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    showPagedownButton(true, true);
                }
            } else {
                query = true;
            }
        } else {
            query = true;
        }

        if (query) {
            /*clearChatData();
            loadsCount = 0;
            unread_to_load = 0;
            first_unread_id = 0;
            loadingForward = false;
            unreadMessageObject = null;
            scrollToMessage = null;*/

            highlightMessageId = Integer.MAX_VALUE;
            scrollToMessagePosition = -10000;
            startLoadFromMessageId = id;
            //MessagesController.getInstance().loadMessages(loadIndex == 0 ? dialog_id : mergeDialogId, AndroidUtilities.isTablet() ? 30 : 20, startLoadFromMessageId, true, 0, classGuid, 3, 0, ChatObject.isChannel(currentChat), lastLoadIndex++);
            //emptyViewContainer.setVisibility(View.INVISIBLE);
        }
        returnToMessageId = fromMessageId;
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
            newUnreadMessageCount = 0;
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
        if (mentionsAdapter != null) {
            mentionsAdapter.onRequestPermissionsResultFragment(requestCode, permissions, grantResults);
        }
    }

    private void checkActionBarMenu() {
        if (menuItem != null) {
            menuItem.setVisibility(View.VISIBLE);
        }
        checkAndUpdateAvatar();
    }

    private int getMessageType(MessageObject messageObject) {
        if (messageObject == null) {
            return -1;
        }
        {
            if ( messageObject.getId() <= 0 && messageObject.isOut() ) {
                if (messageObject.isSendError()) {
                    if (!messageObject.isMediaEmpty()) {
                        return 0;
                    } else {
                        return 20;
                    }
                } else {
                    return -1;
                }
            } else {
                if (messageObject.type == 6) {
                    return -1;
                } else if (messageObject.type == 10 || messageObject.type == 11) {
                    if (messageObject.getId() == 0) {
                        return -1;
                    }
                    return 1;
                } else {
                    if (messageObject.isVoice()) {
                        return 2;
                    } else if (messageObject.isSticker()) {
                        TLRPC.InputStickerSet inputStickerSet = messageObject.getInputStickerSet();
                        if (inputStickerSet instanceof TLRPC.TL_inputStickerSetID) {
                            if (!StickersAdapter.isStickerPackInstalled(inputStickerSet.id)) {
                                return 7;
                            }
                        } else if (inputStickerSet instanceof TLRPC.TL_inputStickerSetShortName) {
                            if (!StickersAdapter.isStickerPackInstalled(inputStickerSet.short_name)) {
                                return 7;
                            }
                        }
                    } else if (messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaPhoto || messageObject.getDocument() != null || messageObject.isMusic() || messageObject.isVideo()) {
                        boolean canSave = false;
                        if (messageObject.messageOwner.attachPath != null && messageObject.messageOwner.attachPath.length() != 0) {
                            File f = new File(messageObject.messageOwner.attachPath);
                            if (f.exists()) {
                                canSave = true;
                            }
                        }
                        if (!canSave) {
                            File f = FileLoader.getPathToMessage(messageObject.messageOwner);
                            if (f.exists()) {
                                canSave = true;
                            }
                        }
                        if (canSave) {
                            if (messageObject.getDocument() != null) {
                                String mime = messageObject.getDocument().mime_type;
                                if (mime != null) {
                                    if (mime.endsWith("/xml")) {
                                        return 5;
                                    } else if (mime.endsWith("/png") || mime.endsWith("/jpg") || mime.endsWith("/jpeg")) {
                                        return 6;
                                    }
                                }
                            }
                            return 4;
                        }
                    } else if (messageObject.type == 12) {
                        return 8;
                    } else if (messageObject.isMediaEmpty()) {
                        return 3;
                    }
                    return 2;
                }
            }
        }
    }

    private void addToSelectedMessages(MessageObject messageObject) {
        int index = messageObject.getDialogId() == dialog_id ? 0 : 1;
        if (selectedMessagesIds[index].containsKey(messageObject.getId())) {
            selectedMessagesIds[index].remove(messageObject.getId());
            if (messageObject.type == 0 || messageObject.caption != null) {
                selectedMessagesCanCopyIds[index].remove(messageObject.getId());
            }
        } else {
            selectedMessagesIds[index].put(messageObject.getId(), messageObject);
            if (messageObject.type == 0 || messageObject.caption != null) {
                selectedMessagesCanCopyIds[index].put(messageObject.getId(), messageObject);
            }
        }
        if (actionBar.isActionModeShowed()) {
            if (selectedMessagesIds[0].isEmpty() && selectedMessagesIds[1].isEmpty()) {
                actionBar.hideActionMode();
            } else {
                final int newVisibility = selectedMessagesIds[0].size() + selectedMessagesIds[1].size() == 1 ? View.VISIBLE : View.GONE;

                final ActionBarMenuItem infoItem = actionBar.createActionMode().getItem(id_info);
                final ActionBarMenuItem replyItem = actionBar.createActionMode().getItem(id_reply);

                if (infoItem != null && replyItem != null
                 && (infoItem.getVisibility() != newVisibility || replyItem.getVisibility()!=newVisibility)) {
                    if( iconAnimator != null ) {
                        iconAnimator.cancel();
                        iconAnimator = null;
                    }
                    iconAnimator = ObjectAnimator.ofFloat(this, "progress", 1, 0);
                    iconAnimator.setDuration(100);
                    iconAnimator.addListener(new AnimatorListenerAdapterProxy() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            iconAnimator = null;
                            if( newVisibility==View.VISIBLE) {
                                infoItem.setVisibility(newVisibility);
                            }
                            else {
                                replyItem.setVisibility(newVisibility);
                            }
                        }
                    });
                    iconAnimator.start();

                    if( newVisibility==View.VISIBLE) {
                        replyItem.setVisibility(newVisibility);
                    }
                    else {
                        infoItem.setVisibility(newVisibility);
                    }
                }

            }
        }
    }

    private void processRowSelect(View view) {
        MessageObject message = null;
        if (view instanceof ChatMessageCell) {
            message = ((ChatMessageCell) view).getMessageObject();
        } else if (view instanceof ChatActionCell) {
            message = ((ChatActionCell) view).getMessageObject();
        }

        int type = getMessageType(message);

        if (type < 2 || type == 20) {
            return;
        }
        addToSelectedMessages(message);
        updateActionModeTitle();
        updateVisibleRows();
    }

    private void updateActionModeTitle() {
        if (!actionBar.isActionModeShowed()) {
            return;
        }
        if (!selectedMessagesIds[0].isEmpty() || !selectedMessagesIds[1].isEmpty()) {
            selectedMessagesCountTextView.setNumber(selectedMessagesIds[0].size() + selectedMessagesIds[1].size(), true);
        }
    }

    private void updateTitle() {
        if (avatarContainer == null) {
            return;
        }
        avatarContainer.setTitle(m_mrChat.getName()); // EDIT BY MR -- realize the title from m_hChat
    }

    private void updateTitleIcons() {
        if (avatarContainer == null) {
            return;
        }

        int rightIcon = 0;
        if( m_canMute && MessagesController.getInstance().isDialogMuted(dialog_id) ) {
            rightIcon = R.drawable.mute_fixed;
        }

        avatarContainer.setTitleIcons(0, rightIcon);
        if( muteMenuEntry != null ) {
            if (rightIcon != 0) {
                muteMenuEntry.setText(LocaleController.getString("UnmuteNotifications", R.string.UnmuteNotifications));
            } else {
                muteMenuEntry.setText(LocaleController.getString("MuteNotifications", R.string.MuteNotifications));
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
            public void didFinishEditVideo(String videoPath, long startTime, long endTime, int resultWidth, int resultHeight, int rotationValue, int originalWidth, int originalHeight, int bitrate, long estimatedSize, long estimatedDuration) {
                VideoEditedInfo videoEditedInfo = new VideoEditedInfo();
                videoEditedInfo.startTime = startTime;
                videoEditedInfo.endTime = endTime;
                videoEditedInfo.rotationValue = rotationValue;
                videoEditedInfo.originalWidth = originalWidth;
                videoEditedInfo.originalHeight = originalHeight;
                videoEditedInfo.bitrate = bitrate;
                videoEditedInfo.resultWidth = resultWidth;
                videoEditedInfo.resultHeight = resultHeight;
                videoEditedInfo.originalPath = videoPath;
                SendMessagesHelper.prepareSendingVideo(videoPath, estimatedSize, estimatedDuration, resultWidth, resultHeight, videoEditedInfo, dialog_id, replyingMessageObject);
                showReplyPanel(false, null, null, null, false, true);
                m_mrChat.cleanDraft();
            }
        });

        if (parentLayout == null || !fragment.onFragmentCreate()) {
            SendMessagesHelper.prepareSendingVideo(videoPath, 0, 0, 0, 0, null, dialog_id, replyingMessageObject);
            showReplyPanel(false, null, null, null, false, true);
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
        Toast toast = Toast.makeText(getParentActivity(), LocaleController.getString("UnsupportedAttachment", R.string.UnsupportedAttachment), Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 0) {
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
                    FileLog.e("messenger", e);
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
            } else if (requestCode == 1) {
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
                        FileLog.e("messenger", e);
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
                        SendMessagesHelper.prepareSendingVideo(videoPath, 0, 0, 0, 0, null, dialog_id, replyingMessageObject);
                    }
                } else {
                    SendMessagesHelper.prepareSendingPhoto(null, uri, dialog_id, replyingMessageObject, null);
                }
                showReplyPanel(false, null, null, null, false, true);
                m_mrChat.cleanDraft();
            } else if (requestCode == 2) {
                String videoPath = null;
                FileLog.d("messenger", "pic path " + currentPicturePath);
                if (data != null && currentPicturePath != null) {
                    if (new File(currentPicturePath).exists()) {
                        data = null;
                    }
                }
                if (data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        FileLog.d("messenger", "video record uri " + uri.toString());
                        videoPath = AndroidUtilities.getPath(uri);
                        FileLog.d("messenger", "resolved path = " + videoPath);
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
                    SendMessagesHelper.prepareSendingVideo(videoPath, 0, 0, 0, 0, null, dialog_id, replyingMessageObject);
                    showReplyPanel(false, null, null, null, false, true);
                    m_mrChat.cleanDraft();
                }
            } else if (requestCode == 21) {
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
                        FileLog.e("messenger", e);
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
                SendMessagesHelper.prepareSendingDocument(tempPath, originalPath, null, null, dialog_id, replyingMessageObject);
                showReplyPanel(false, null, null, null, false, true);
                m_mrChat.cleanDraft();
            } else if (requestCode == 31) {
                if (data == null || data.getData() == null) {
                    showAttachmentError();
                    return;
                }
                Uri uri = data.getData();
                Cursor c = null;
                try {
                    c = getParentActivity().getContentResolver().query(uri, new String[]{ContactsContract.Data.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null);
                    if (c != null) {
                        boolean sent = false;
                        while (c.moveToNext()) {
                            sent = true;
                            String name = c.getString(0);
                            String number = c.getString(1);
                            TLRPC.User user = new TLRPC.User();
                            user.first_name = name;
                            user.last_name = "";
                            user.phone = number;
                            SendMessagesHelper.getInstance().sendMessageContact(user, dialog_id, replyingMessageObject, null);
                        }
                        if (sent) {
                            showReplyPanel(false, null, null, null, false, true);
                            m_mrChat.cleanDraft();
                        }
                    }
                } finally {
                    try {
                        if (c != null && !c.isClosed()) {
                            c.close();
                        }
                    } catch (Exception e) {
                        FileLog.e("messenger", e);
                    }
                }
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

    private void removeUnreadPlane() {
        if (unreadMessageObject != null) {
            forwardEndReached = true;
            first_unread_id = 0;
            last_message_id = 0;
            unread_to_load = 0;
            removeMessageObject(unreadMessageObject);
            unreadMessageObject = null;
        }
    }

    private void messagesDidLoaded()
    {
        // should be called from the GUI thread only (as the original event was)


        /* EDIT BY MR -- scroll to a specific message
        if (waitingForReplyMessageLoad) {
            boolean found = false;
            for (int a = 0; a < messArr.size(); a++) {
                if (messArr.get(a).getId() == startLoadFromMessageId) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                startLoadFromMessageId = 0;
                return;
            }
            int startLoadFrom = startLoadFromMessageId;
            boolean needSelect = needSelectFromMessageId;
            clearChatData();
            startLoadFromMessageId = startLoadFrom;
            needSelectFromMessageId = needSelect;
        }
        */

        loadsCount++;
        final int fnid = (Integer) Integer.MAX_VALUE;
        final int last_unread_date = 0;
        boolean wasUnread = false;
        if (fnid != 0) {
            first_unread_id = fnid;
            unread_to_load = 0;
        }
        int newRowsCount = 0;

        forwardEndReached = startLoadFromMessageId == 0 && last_message_id == 0;

        if (firstLoading) {
            if (!forwardEndReached) {
                messages.clear();
                messagesByDays.clear();
                messagesDict.clear();
                maxMessageId = Integer.MAX_VALUE;
                minMessageId = Integer.MIN_VALUE;
                maxDate = Integer.MIN_VALUE;
                minDate = 0;
            }
            firstLoading = false;
            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    if (parentLayout != null) {
                        parentLayout.resumeDelayedFragmentAnimation();
                    }
                }
            });
        }

        {
            int[] msglist = MrMailbox.getChatMsgs((int)dialog_id);

            int mrCount = msglist.length;
            for (int a = mrCount - 1; a >= 0; a--) {
                MrMsg mrMsg = MrMailbox.getMsg(msglist[a]);
                TLRPC.Message msg = mrMsg.get_TLRPC_Message();
                MessageObject msgDrawObj = new MessageObject(msg, null, true);
                messages.add(0, msgDrawObj);
                messagesDict.put(msg.id, msgDrawObj);
            }

            if (m_mrChat.getId() == MrChat.MR_CHAT_ID_DEADDROP) {
                updateBottomOverlay();
            }
        }


            /* EDIT BY MR -- the add loop
            int approximateHeightSum = 0;
            for (int a = 0; a < messArr.size(); a++) {


                MessageObject obj = messArr.get(a);

                approximateHeightSum += obj.getApproximateHeight();

                if (currentUser != null && currentUser.bot && obj.isOut()) {
                    obj.setIsRead();
                }

                if (messagesDict[loadIndex].containsKey(obj.getId())) {
                    continue;
                }

                if (loadIndex == 1) {
                    obj.setIsRead();
                }

                if (loadIndex == 0 && ChatObject.isChannel(currentChat) && obj.getId() == 1) {
                    endReached[loadIndex] = true;
                    cacheEndReached[loadIndex] = true;
                }

                if (obj.getId() > 0) {
                    maxMessageId[loadIndex] = Math.min(obj.getId(), maxMessageId[loadIndex]);
                    minMessageId[loadIndex] = Math.max(obj.getId(), minMessageId[loadIndex]);
                }
                if (obj.messageOwner.date != 0) {
                    maxDate[loadIndex] = Math.max(maxDate[loadIndex], obj.messageOwner.date);
                    if (minDate[loadIndex] == 0 || obj.messageOwner.date < minDate[loadIndex]) {
                        minDate[loadIndex] = obj.messageOwner.date;
                    }
                }

                if (obj.type < 0 || loadIndex == 1 && obj.messageOwner.action instanceof TLRPC.TL_messageActionChatMigrateTo) {
                    continue;
                }

                if (!obj.isOut() && obj.isUnread()) {
                    wasUnread = true;
                }

                messagesDict[loadIndex].put(obj.getId(), obj);
                ArrayList<MessageObject> dayArray = messagesByDays.get(obj.dateKey);

                if (dayArray == null) {
                    dayArray = new ArrayList<>();
                    messagesByDays.put(obj.dateKey, dayArray);
                    TLRPC.Message dateMsg = new TLRPC.Message();
                    dateMsg.message = LocaleController.formatDateChat(obj.messageOwner.date);
                    dateMsg.id = 0;
                    dateMsg.date = obj.messageOwner.date;
                    MessageObject dateObj = new MessageObject(dateMsg, null, false);
                    dateObj.type = 10;
                    dateObj.contentType = 1;
                    if (load_type == 1) {
                        messages.add(0, dateObj);
                    } else {
                        messages.add(dateObj);
                    }
                    newRowsCount++;
                }

                newRowsCount++;
                if (load_type == 1) {
                    dayArray.add(obj);
                    messages.add(0, obj);
                }

                if (load_type != 1) {
                    dayArray.add(obj);
                    messages.add(messages.size() - 1, obj);
                }

                if (obj.getId() == last_message_id) {
                    forwardEndReached[loadIndex] = true;
                }

                if (load_type == 2 && obj.getId() == first_unread_id) {
                    if (approximateHeightSum > AndroidUtilities.displaySize.y / 2 || !forwardEndReached[0]) {
                        TLRPC.Message dateMsg = new TLRPC.Message();
                        dateMsg.message = "";
                        dateMsg.id = 0;
                        MessageObject dateObj = new MessageObject(dateMsg, null, false);
                        dateObj.type = 6;
                        dateObj.contentType = 2;
                        messages.add(messages.size() - 1, dateObj);
                        unreadMessageObject = dateObj;
                        scrollToMessage = unreadMessageObject;
                        scrollToMessagePosition = -10000;
                        newRowsCount++;
                    }
                } else if (load_type == 3 && obj.getId() == startLoadFromMessageId) {
                    if (needSelectFromMessageId) {
                        highlightMessageId = obj.getId();
                    } else {
                        highlightMessageId = Integer.MAX_VALUE;
                    }
                    scrollToMessage = obj;
                    startLoadFromMessageId = 0;
                    if (scrollToMessagePosition == -10000) {
                        scrollToMessagePosition = -9000;
                    }
                }

            }
            */



        if (newRowsCount == 0) {
            loadsCount--;
        }

        if (forwardEndReached ) {
            first_unread_id = 0;
            last_message_id = 0;
        }

        {
            loading = false;

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
                        if (!messages.isEmpty()) {
                            if (messages.get(messages.size() - 1) == scrollToMessage || messages.get(messages.size() - 2) == scrollToMessage) {
                                chatLayoutManager.scrollToPositionWithOffset((chatAdapter.isBot ? 1 : 0), -chatListView.getPaddingTop() - AndroidUtilities.dp(7) + yOffset);
                            } else {
                                chatLayoutManager.scrollToPositionWithOffset(chatAdapter.messagesStartRow + messages.size() - messages.indexOf(scrollToMessage) - 1, -chatListView.getPaddingTop() - AndroidUtilities.dp(7) + yOffset);
                            }
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
                } else {
                    if (newRowsCount != 0) {
                        boolean end = false;
                        if (endReached) {
                            end = true;
                            chatAdapter.notifyItemRangeChanged(chatAdapter.isBot ? 1 : 0, 2);
                        }
                        int firstVisPos = chatLayoutManager.findLastVisibleItemPosition();
                        View firstVisView = chatLayoutManager.findViewByPosition(firstVisPos);
                        int top = ((firstVisView == null) ? 0 : firstVisView.getTop()) - chatListView.getPaddingTop();
                        if (newRowsCount - (end ? 1 : 0) > 0) {
                            chatAdapter.notifyItemRangeInserted((chatAdapter.isBot ? 2 : 1) + (end ? 0 : 1), newRowsCount - (end ? 1 : 0));
                        }
                        if (firstVisPos != -1) {
                            chatLayoutManager.scrollToPositionWithOffset(firstVisPos + newRowsCount - (end ? 1 : 0), top);
                        }
                    } else if (endReached) {
                        chatAdapter.notifyItemRemoved(chatAdapter.isBot ? 1 : 0);
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

        if (first && messages.size() > 0) {
                final boolean wasUnreadFinal = wasUnread;
                final int last_unread_date_final = last_unread_date;
                final int lastid = messages.get(0).getId();
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if (last_message_id != 0) {
                            MessagesController.getInstance().markDialogAsRead(dialog_id, lastid, last_message_id, last_unread_date_final, wasUnreadFinal, false);
                        } else {
                            MessagesController.getInstance().markDialogAsRead(dialog_id, lastid, minMessageId, maxDate, wasUnreadFinal, false);
                        }
                    }
                }, 700);
            first = false;
        }

        {
            if (progressView != null) {
                progressView.setVisibility(View.INVISIBLE);
            }
        }
        checkScrollForLoad(false);
    }

    @Override
    public void didReceivedNotification(int id, final Object... args) {
        if( id == NotificationCenter.dialogsNeedReload ) {
            if( args.length >= 3 ) {
                int evt_chat_id = (int) args[1];
                int evt_msg_id = (int) args[2];
                if (evt_chat_id == dialog_id && evt_msg_id > 0) {
                    MrMsg mrMsg = MrMailbox.getMsg(evt_msg_id);
                    TLRPC.Message msg = mrMsg.get_TLRPC_Message();
                    MessageObject msgDrawObj = new MessageObject(msg, null, true);
                    messages.add(0, msgDrawObj);  // TODO: also add date, if needed
                    messagesDict.put(msg.id, msgDrawObj);
                    chatAdapter.notifyDataSetChanged();
                    scrollToLastMessage(false); // TODO markseen
                }
            }
        }
        else if (id == NotificationCenter.emojiDidLoaded) {
            if (chatListView != null) {
                chatListView.invalidateViews();
            }
            if (replyObjectTextView != null) {
                replyObjectTextView.invalidate();
            }
            if (alertTextView != null) {
                alertTextView.invalidate();
            }
            if (mentionListView != null) {
                mentionListView.invalidateViews();
            }
        } else if (id == NotificationCenter.updateInterfaces) {
            int updateMask = (Integer) args[0];
            if ((updateMask & MessagesController.UPDATE_MASK_NAME) != 0 || (updateMask & MessagesController.UPDATE_MASK_CHAT_NAME) != 0) {
                /*if (currentChat != null) {
                    TLRPC.Chat chat = MessagesController.getInstance().getChat(currentChat.id);
                    if (chat != null) {
                        currentChat = chat;
                    }
                } else if (currentUser != null) {
                    TLRPC.User user = MessagesController.getInstance().getUser(currentUser.id);
                    if (user != null) {
                        currentUser = user;
                    }
                }*/
                int back_id = m_mrChat.getId();
                m_mrChat = MrMailbox.getChat(back_id);

                updateTitle();
            }
            boolean updateSubtitle = false;
            if ((updateMask & MessagesController.UPDATE_MASK_CHAT_MEMBERS) != 0 || (updateMask & MessagesController.UPDATE_MASK_STATUS) != 0) {
                updateSubtitle = true;
            }
            if ((updateMask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (updateMask & MessagesController.UPDATE_MASK_CHAT_AVATAR) != 0 || (updateMask & MessagesController.UPDATE_MASK_NAME) != 0) {
                checkAndUpdateAvatar();
                updateVisibleRows();
            }
            if ((updateMask & MessagesController.UPDATE_MASK_USER_PRINT) != 0) {
                updateSubtitle = true;
            }
            /*if ((updateMask & MessagesController.UPDATE_MASK_CHANNEL) != 0 && ChatObject.isChannel(currentChat)) {
                TLRPC.Chat chat = MessagesController.getInstance().getChat(currentChat.id);
                if (chat == null) {
                    return;
                }
                currentChat = chat;
                updateSubtitle = true;
                updateBottomOverlay();
                if (chatActivityEnterView != null) {
                    chatActivityEnterView.setDialogId(dialog_id);
                }
            }*/
            if (avatarContainer != null && updateSubtitle) {
                avatarContainer.updateSubtitle();
            }

        } else if (id == NotificationCenter.didReceivedNewMessages) {
            long did = (Long) args[0];
            if (did == dialog_id) {

                boolean updateChat = false;
                boolean hasFromMe = false;
                ArrayList<MessageObject> arr = (ArrayList<MessageObject>) args[1];

                /*if (currentChat != null || inlineReturn != 0 ) {
                    for (int a = 0; a < arr.size(); a++) {
                        MessageObject messageObject = arr.get(a);
                        if (currentChat != null) {
                            if (messageObject.messageOwner.action instanceof TLRPC.TL_messageActionChatDeleteUser && messageObject.messageOwner.action.user_id == UserConfig.getClientUserId() ||
                                    messageObject.messageOwner.action instanceof TLRPC.TL_messageActionChatAddUser && messageObject.messageOwner.action.users.contains(UserConfig.getClientUserId())) {
                                TLRPC.Chat newChat = MessagesController.getInstance().getChat(currentChat.id);
                                if (newChat != null) {
                                    currentChat = newChat;
                                    checkActionBarMenu();
                                    updateBottomOverlay();
                                    if (avatarContainer != null) {
                                        avatarContainer.updateSubtitle();
                                    }
                                }
                            } else if (messageObject.messageOwner.reply_to_msg_id != 0 && messageObject.replyMessageObject == null) {
                                messageObject.replyMessageObject = messagesDict[0].get(messageObject.messageOwner.reply_to_msg_id);
                                //if (messageObject.messageOwner.action instanceof TLRPC.TL_messageActionPinMessage) {
                                //    messageObject.generatePinMessageText(null, null);
                                //}
                            }
                        }
                    }
                }*/

                if (!forwardEndReached) {
                    int currentMaxDate = Integer.MIN_VALUE;
                    int currentMinMsgId = Integer.MIN_VALUE;
                    boolean currentMarkAsRead = false;

                    for (int a = 0; a < arr.size(); a++) {
                        MessageObject obj = arr.get(a);

                        if (obj.isOut() && obj.isSending()) {
                            scrollToLastMessage(false);
                            return;
                        }
                        if (obj.type < 0 || messagesDict.containsKey(obj.getId())) {
                            continue;
                        }
                        obj.checkLayout();
                        currentMaxDate = Math.max(currentMaxDate, obj.messageOwner.date);
                        if (obj.getId() > 0) {
                            currentMinMsgId = Math.max(obj.getId(), currentMinMsgId);
                            last_message_id = Math.max(last_message_id, obj.getId());
                        }

                        if (!obj.isOut() && obj.isUnread()) {
                            unread_to_load++;
                            currentMarkAsRead = true;
                        }
                        if (obj.type == 10 || obj.type == 11) {
                            updateChat = true;
                        }
                    }

                    if (currentMarkAsRead) {
                        if (paused) {
                            readWhenResume = true;
                            readWithDate = currentMaxDate;
                            readWithMid = currentMinMsgId;
                        } else {
                            if (messages.size() > 0) {
                                MessagesController.getInstance().markDialogAsRead(dialog_id, messages.get(0).getId(), currentMinMsgId, currentMaxDate, true, false);
                            }
                        }
                    }
                    updateVisibleRows();
                } else {
                    boolean markAsRead = false;
                    boolean unreadUpdated = true;
                    int oldCount = messages.size();
                    int addedCount = 0;
                    HashMap<String, ArrayList<MessageObject>> webpagesToReload = null;
                    int placeToPaste = -1;
                    for (int a = 0; a < arr.size(); a++) {
                        MessageObject obj = arr.get(a);
                        if (a == 0) {
                            if (obj.messageOwner.id < 0) {
                                placeToPaste = 0;
                            } else {
                                if (!messages.isEmpty()) {
                                    int size = messages.size();
                                    for (int b = 0; b < size; b++) {
                                        MessageObject lastMessage = messages.get(b);
                                        if (lastMessage.type >= 0 && lastMessage.messageOwner.date > 0) {
                                            if (lastMessage.messageOwner.id > 0 && obj.messageOwner.id > 0) {
                                                if (lastMessage.messageOwner.id < obj.messageOwner.id) {
                                                    placeToPaste = b;
                                                    break;
                                                }
                                            } else {
                                                if (lastMessage.messageOwner.date < obj.messageOwner.date) {
                                                    placeToPaste = b;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    if (placeToPaste == -1 || placeToPaste > messages.size()) {
                                        placeToPaste = messages.size();
                                    }
                                } else {
                                    placeToPaste = 0;
                                }
                            }
                        }

                        if (obj.type < 0 || messagesDict.containsKey(obj.getId())) {
                            continue;
                        }

                        obj.checkLayout();

                        if (minDate == 0 || obj.messageOwner.date < minDate) {
                            minDate = obj.messageOwner.date;
                        }

                        if (obj.isOut()) {
                            removeUnreadPlane();
                            hasFromMe = true;
                        }

                        if (obj.getId() > 0) {
                            maxMessageId = Math.min(obj.getId(), maxMessageId);
                            minMessageId = Math.max(obj.getId(), minMessageId);
                        }
                        maxDate = Math.max(maxDate, obj.messageOwner.date);
                        messagesDict.put(obj.getId(), obj);
                        ArrayList<MessageObject> dayArray = messagesByDays.get(obj.dateKey);
                        if (dayArray == null) {
                            dayArray = new ArrayList<>();
                            messagesByDays.put(obj.dateKey, dayArray);
                            TLRPC.Message dateMsg = new TLRPC.Message();
                            dateMsg.message = LocaleController.formatDateChat(obj.messageOwner.date);
                            dateMsg.id = 0;
                            dateMsg.date = obj.messageOwner.date;
                            MessageObject dateObj = new MessageObject(dateMsg, null, false);
                            dateObj.type = 10;
                            dateObj.contentType = ROWTYPE_ACTION_CELL;
                            messages.add(placeToPaste, dateObj);
                            addedCount++;
                        }
                        if (!obj.isOut()) {
                            if (paused && placeToPaste == 0) {
                                if (!scrollToTopUnReadOnResume && unreadMessageObject != null) {
                                    removeMessageObject(unreadMessageObject);
                                    unreadMessageObject = null;
                                }
                                if (unreadMessageObject == null) {
                                    TLRPC.Message dateMsg = new TLRPC.Message();
                                    dateMsg.message = "";
                                    dateMsg.id = 0;
                                    MessageObject dateObj = new MessageObject(dateMsg, null, false);
                                    dateObj.type = 6;
                                    dateObj.contentType = ROWTYPE_UNREAD_CELL;
                                    messages.add(0, dateObj);
                                    unreadMessageObject = dateObj;
                                    scrollToMessage = unreadMessageObject;
                                    scrollToMessagePosition = -10000;
                                    unreadUpdated = false;
                                    unread_to_load = 0;
                                    scrollToTopUnReadOnResume = true;
                                    addedCount++;
                                }
                            }
                            if (unreadMessageObject != null) {
                                unread_to_load++;
                                unreadUpdated = true;
                            }
                            if (obj.isUnread()) {
                                if (!paused) {
                                    obj.setIsRead();
                                }
                                markAsRead = true;
                            }
                        }

                        dayArray.add(0, obj);
                        messages.add(placeToPaste, obj);
                        addedCount++;
                        newUnreadMessageCount++;
                        if (obj.type == 10 || obj.type == 11) {
                            updateChat = true;
                        }
                    }
                    if (webpagesToReload != null) {
                        MessagesController.getInstance().reloadWebPages(dialog_id, webpagesToReload);
                    }

                    if (progressView != null) {
                        progressView.setVisibility(View.INVISIBLE);
                    }
                    if (chatAdapter != null) {
                        if (unreadUpdated) {
                            chatAdapter.updateRowWithMessageObject(unreadMessageObject);
                        }
                        if (addedCount != 0) {
                            chatAdapter.notifyItemRangeInserted(chatAdapter.getItemCount() - placeToPaste, addedCount);
                        }
                    } else {
                        scrollToTopOnResume = true;
                    }

                    if (chatListView != null && chatAdapter != null) {
                        int lastVisible = chatLayoutManager.findLastVisibleItemPosition();
                        if (lastVisible == RecyclerView.NO_POSITION) {
                            lastVisible = 0;
                        }
                        if (endReached) {
                            lastVisible++;
                        }
                        /*if (chatAdapter.isBot) {
                            oldCount++;
                        }*/
                        if (lastVisible >= oldCount || hasFromMe) {
                            newUnreadMessageCount = 0;
                            if (!firstLoading) {
                                if (paused) {
                                    scrollToTopOnResume = true;
                                } else {
                                    forceScrollToTop = true;
                                    moveScrollToLastMessage();
                                }
                            }
                        } else {
                            if (newUnreadMessageCount != 0 && pagedownButtonCounter != null) {
                                pagedownButtonCounter.setVisibility(View.VISIBLE);
                                pagedownButtonCounter.setText(String.format("%d", newUnreadMessageCount));
                            }
                            showPagedownButton(true, true);
                        }
                    } else {
                        scrollToTopOnResume = true;
                    }

                    if (markAsRead) {
                        if (paused) {
                            readWhenResume = true;
                            readWithDate = maxDate;
                            readWithMid = minMessageId;
                        } else {
                            MessagesController.getInstance().markDialogAsRead(dialog_id, messages.get(0).getId(), minMessageId, maxDate, true, false);
                        }
                    }
                }

                if (updateChat) {
                    updateTitle();
                    checkAndUpdateAvatar();
                }

            }
        } else if (id == NotificationCenter.closeChats) {
            if (args != null && args.length > 0) {
                long did = (Long) args[0];
                if (did == dialog_id) {
                    finishFragment();
                }
            } else {
                removeSelfFromStack();
            }
        } else if (id == NotificationCenter.messagesSentOrRead) {
            int back_id = m_mrChat.getId();
            m_mrChat = MrMailbox.getChat(back_id);

            int evt_read = (int)args[0];
            int evt_chat_id = (int)args[1];
            int evt_msg_id = (int)args[2];
            boolean updated = false;
            if (evt_chat_id == dialog_id) {
                for (int a = 0; a < messages.size(); a++) {
                    MessageObject obj = messages.get(a);
                    if (obj.isOut() && obj.getId() == evt_msg_id) {
                        obj.messageOwner.send_state = MessageObject.MESSAGE_SEND_STATE_SENT;
                        if (evt_read==MrMailbox.MR_EVENT_MSG_READ) {
                            obj.setIsRead();
                        }
                        updated = true;
                        break;
                    }
                }
            }
            if (updated) {
                updateVisibleRows();
            }
        } else if (id == NotificationCenter.messagesDeleted) {
            ArrayList<Integer> markAsDeletedMessages = (ArrayList<Integer>) args[0];
            boolean updated = false;
            for (int a = 0; a < markAsDeletedMessages.size(); a++) {
                Integer ids = markAsDeletedMessages.get(a);
                MessageObject obj = messagesDict.get(ids);
                if (obj != null) {
                    int index = messages.indexOf(obj);
                    if (index != -1) {
                        messages.remove(index);
                        messagesDict.remove(ids);
                        ArrayList<MessageObject> dayArr = messagesByDays.get(obj.dateKey);
                        if (dayArr != null) {
                            dayArr.remove(obj);
                            if (dayArr.isEmpty()) {
                                messagesByDays.remove(obj.dateKey);
                                if (index >= 0 && index < messages.size()) {
                                    messages.remove(index);
                                }
                            }
                        }
                        updated = true;
                    }
                }
            }
            if (messages.isEmpty()) {
                if (!endReached && !loading) {
                    if (progressView != null) {
                        progressView.setVisibility(View.INVISIBLE);
                    }
                    if (chatListView != null) {
                        chatListView.setEmptyView(null);
                    }
                    maxMessageId = Integer.MAX_VALUE;
                    minMessageId = Integer.MIN_VALUE;
                    maxDate = Integer.MIN_VALUE;
                    minDate = 0;
                    //MessagesController.getInstance().loadMessages(dialog_id, 30, 0, !cacheEndReached[0], minDate[0], classGuid, 0, 0, ChatObject.isChannel(currentChat), lastLoadIndex++);
                    loading = true;
                }
            }
            if (updated && chatAdapter != null) {
                removeUnreadPlane();
                chatAdapter.notifyDataSetChanged();
            }
        } else if (id == NotificationCenter.messageSendError) {
            Integer msgId = (Integer) args[0];
            MessageObject obj = messagesDict.get(msgId);
            if (obj != null) {
                obj.messageOwner.send_state = MessageObject.MESSAGE_SEND_STATE_SEND_ERROR;
                updateVisibleRows();
            }
        } else if (id == NotificationCenter.contactsDidLoaded) {
            if (avatarContainer != null) {
                avatarContainer.updateSubtitle();
            }
        } else if (id == NotificationCenter.audioDidReset || id == NotificationCenter.audioPlayStateChanged) {
            if (chatListView != null) {
                int count = chatListView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View view = chatListView.getChildAt(a);
                    if (view instanceof ChatMessageCell) {
                        ChatMessageCell cell = (ChatMessageCell) view;
                        MessageObject messageObject = cell.getMessageObject();
                        if (messageObject != null && (messageObject.isVoice() || messageObject.isMusic())) {
                            cell.updateButtonState(false);
                        }
                    }
                }
            }
        } else if (id == NotificationCenter.audioProgressDidChanged) {
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
        } else if (id == NotificationCenter.blockedUsersDidLoaded) {
            ;
        } else if (id == NotificationCenter.FileNewChunkAvailable) {
            MessageObject messageObject = (MessageObject) args[0];
            long finalSize = (Long) args[2];
            if (finalSize != 0 && dialog_id == messageObject.getDialogId()) {
                MessageObject currentObject = messagesDict.get(messageObject.getId());
                if (currentObject != null) {
                    currentObject.messageOwner.media.document.size = (int) finalSize;
                    updateVisibleRows();
                }
            }
        } else if (id == NotificationCenter.audioDidStarted) {
            MessageObject messageObject = (MessageObject) args[0];
            if (chatListView != null) {
                int count = chatListView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View view = chatListView.getChildAt(a);
                    if (view instanceof ChatMessageCell) {
                        ChatMessageCell cell = (ChatMessageCell) view;
                        MessageObject messageObject1 = cell.getMessageObject();
                        if (messageObject1 != null && (messageObject1.isVoice() || messageObject1.isMusic())) {
                            cell.updateButtonState(false);
                        }
                    }
                }
            }
        } else if (id == NotificationCenter.updateMessageMedia) {
            MessageObject messageObject = (MessageObject) args[0];
            MessageObject existMessageObject = messagesDict.get(messageObject.getId());
            if (existMessageObject != null) {
                existMessageObject.messageOwner.media = messageObject.messageOwner.media;
                existMessageObject.messageOwner.attachPath = messageObject.messageOwner.attachPath;
                existMessageObject.generateThumbs(false);
            }
            updateVisibleRows();
        } else if (id == NotificationCenter.replaceMessagesObjects) {
            long did = (long) args[0];
            if (did != dialog_id ) {
                return;
            }
            boolean changed = false;
            boolean mediaUpdated = false;
            ArrayList<MessageObject> messageObjects = (ArrayList<MessageObject>) args[1];
            for (int a = 0; a < messageObjects.size(); a++) {
                MessageObject messageObject = messageObjects.get(a);
                MessageObject old = messagesDict.get(messageObject.getId());
                if (old != null) {
                    if (!mediaUpdated && messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaWebPage) {
                        mediaUpdated = true;
                    }
                    if (old.replyMessageObject != null) {
                        messageObject.replyMessageObject = old.replyMessageObject;
                    }
                    messageObject.messageOwner.attachPath = old.messageOwner.attachPath;
                    messageObject.attachPathExists = old.attachPathExists;
                    messageObject.mediaExists = old.mediaExists;
                    messagesDict.put(old.getId(), messageObject);
                    int index = messages.indexOf(old);
                    if (index >= 0) {
                        messages.set(index, messageObject);
                        if (chatAdapter != null) {
                            chatAdapter.notifyItemChanged(chatAdapter.messagesStartRow + messages.size() - index - 1);
                        }
                        changed = true;
                    }
                }
            }
            if (changed && chatLayoutManager != null) {
                if (mediaUpdated && chatLayoutManager.findLastVisibleItemPosition() >= messages.size() - (chatAdapter.isBot ? 2 : 1)) {
                    moveScrollToLastMessage();
                }
            }
        } else if (id == NotificationCenter.notificationsSettingsUpdated) {
            updateTitleIcons();
        } else if (id == NotificationCenter.didLoadedReplyMessages) {
            long did = (Long) args[0];
            if (did == dialog_id) {
                updateVisibleRows();
            }
        } else if (id == NotificationCenter.chatSearchResultsAvailable) {
            if (classGuid == (Integer) args[0]) {
                int messageId = (Integer) args[1];
                long did = (Long) args[3];
                if (messageId != 0) {
                    scrollToMessageId(messageId, 0, true);
                }
                updateSearchButtons((Integer) args[2], (Integer) args[4], (Integer) args[5]);
            }
        } else if (id == NotificationCenter.didUpdatedMessagesViews) {
            SparseArray<SparseIntArray> channelViews = (SparseArray<SparseIntArray>) args[0];
            SparseIntArray array = channelViews.get((int) dialog_id);
            if (array != null) {
                boolean updated = false;
                for (int a = 0; a < array.size(); a++) {
                    int messageId = array.keyAt(a);
                    MessageObject messageObject = messagesDict.get(messageId);
                    if (messageObject != null) {
                        int newValue = array.get(messageId);
                        if (newValue > messageObject.messageOwner.views) {
                            messageObject.messageOwner.views = newValue;
                            updated = true;
                        }
                    }
                }
                if (updated) {
                    updateVisibleRows();
                }
            }
        } else if (id == NotificationCenter.newDraftReceived) {
            long did = (Long) args[0];
            if (did == dialog_id) {
                applyDraftMaybe();
            }
        }
    }

    private void updateSearchButtons(int mask, int num, int count) {
        if (searchUpButton != null) {
            searchUpButton.setEnabled((mask & 1) != 0);
            searchDownButton.setEnabled((mask & 2) != 0);
            searchUpButton.setAlpha(searchUpButton.isEnabled() ? 1.0f : 0.5f);
            searchDownButton.setAlpha(searchDownButton.isEnabled() ? 1.0f : 0.5f);
            if (count == 0) {
                searchCountText.setText("");
            } else {
                searchCountText.setText(LocaleController.formatString("Of", R.string.Of, num + 1, count));
            }
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
        // the bottom overlay is also used for going through search results, however, it would be nicer to add the button [^] [v] to the actionbar
        // moreover, we ouse the bottom overlay for the "dead drop chat"
        if (bottomOverlayChatText == null) {
            return;
        }
        bottomOverlayChatText.setText("");

        if (searchItem != null && searchItem.getVisibility() == View.VISIBLE) {
            searchContainer.setVisibility(View.VISIBLE);
            bottomOverlayChat.setVisibility(View.INVISIBLE);
            chatActivityEnterView.setFieldFocused(false);
            chatActivityEnterView.setVisibility(View.INVISIBLE);
        } else {
            searchContainer.setVisibility(View.INVISIBLE);
            {
                if (m_mrChat.getId()==MrChat.MR_CHAT_ID_DEADDROP) {
                    if( messages.isEmpty()) {
                        // showing the DeaddropHint if there are no messages is confusing (there are no "reply arrows" in this case)
                        bottomOverlayChatText.setText(LocaleController.getString("NoMessages", R.string.NoMessages));
                    } else {
                        bottomOverlayChatText.setText(LocaleController.getString("DeaddropHint", R.string.DeaddropHint));
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
        }
        checkRaiseSensors();
    }

    private void checkRaiseSensors() {
        if (!ApplicationLoader.mainInterfacePaused && (bottomOverlayChat == null || bottomOverlayChat.getVisibility() != View.VISIBLE) && (bottomOverlay == null || bottomOverlay.getVisibility() != View.VISIBLE) && (searchContainer == null || searchContainer.getVisibility() != View.VISIBLE)) {
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
        if (replyImageLocation != null && replyImageView != null) {
            replyImageView.setImage(replyImageLocation, "50_50", (Drawable) null);
        }

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
                    chatLayoutManager.scrollToPositionWithOffset(messages.size() - messages.indexOf(scrollToMessage), -chatListView.getPaddingTop() - AndroidUtilities.dp(7) + yOffset);
                }
            } else {
                moveScrollToLastMessage();
            }
            scrollToTopUnReadOnResume = false;
            scrollToTopOnResume = false;
            scrollToMessage = null;
        }
        paused = false;
        if (readWhenResume && !messages.isEmpty()) {
            for (MessageObject messageObject : messages) {
                if (!messageObject.isUnread() && !messageObject.isOut()) {
                    break;
                }
                if (!messageObject.isOut()) {
                    messageObject.setIsRead();
                }
            }
            readWhenResume = false;
            MessagesController.getInstance().markDialogAsRead(dialog_id, messages.get(0).getId(), readWithMid, readWithDate, true, false);
        }
        checkScrollForLoad(false);
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

        if (chatActivityEnterView == null || !chatActivityEnterView.isEditingMessage()) {
            chatListView.setOnItemLongClickListener(onItemLongClickListener);
            chatListView.setOnItemClickListener(onItemClickListener);
            chatListView.setLongClickable(true);
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
            if (!chatActivityEnterView.isEditingMessage()) {
                CharSequence text = AndroidUtilities.getTrimmedString(chatActivityEnterView.getFieldText());
                if (!TextUtils.isEmpty(text) && !TextUtils.equals(text, "@gif")) {
                    draftMessage = text;
                }
            }
            chatActivityEnterView.setFieldFocused(false);
        }
        m_mrChat.saveDraft(draftMessage, replyingMessageObject != null ? replyingMessageObject.messageOwner : null);

        MessagesController.getInstance().cancelTyping(0, dialog_id);
    }

    private void applyDraftMaybe() {
        if (chatActivityEnterView == null) {
            return;
        }
        TLRPC.DraftMessage draftMessage = m_mrChat.getDraftMessageObj();
        if (chatActivityEnterView.getFieldText() == null) {
            if (draftMessage != null) {
                chatActivityEnterView.setWebPage(null, !draftMessage.no_webpage);
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

    private boolean fixLayoutInternal() {
        if (!AndroidUtilities.isTablet() && ApplicationLoader.applicationContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            selectedMessagesCountTextView.setTextSize(18);
        } else {
            selectedMessagesCountTextView.setTextSize(20);
        }

        if (AndroidUtilities.isTablet()) {
            if (AndroidUtilities.isSmallTablet() && ApplicationLoader.applicationContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                actionBar.setBackButtonDrawable(new BackDrawable(false));
                if (playerView != null && playerView.getParent() == null) {
                    ((ViewGroup) fragmentView).addView(playerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 39, Gravity.TOP | Gravity.LEFT, 0, -36, 0, 0));
                }
            } else {
                actionBar.setBackButtonDrawable(new BackDrawable(parentLayout == null || parentLayout.fragmentsStack.isEmpty() || parentLayout.fragmentsStack.get(0) == ChatActivity.this || parentLayout.fragmentsStack.size() == 1));
                if (playerView != null && playerView.getParent() != null) {
                    fragmentView.setPadding(0, 0, 0, 0);
                    ((ViewGroup) fragmentView).removeView(playerView);
                }
            }
            return false;
        }
        return true;
    }

    private void fixLayout() {
        if (avatarContainer != null) {
            avatarContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (avatarContainer != null) {
                        avatarContainer.getViewTreeObserver().removeOnPreDrawListener(this);
                    }
                    return fixLayoutInternal();
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
        builder.setMessage(LocaleController.formatString("AreYouSureDeleteMessages", R.string.AreYouSureDeleteMessages, LocaleController.formatPluralString("messages", selectedMessagesIds[0].size() + selectedMessagesIds[1].size())));
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                for (int a = 1; a >= 0; a--) {
                    ArrayList<Integer> ids = new ArrayList<>(selectedMessagesIds[a].keySet());
                    if( ids.size()>0) {
                        for (HashMap.Entry<Integer, MessageObject> entry : selectedMessagesIds[a].entrySet()) {
                            MessageObject msg = entry.getValue();
                            int id_to_del = msg.messageOwner.id;
                            MrMailbox.deleteMsg(id_to_del);
                        }
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.messagesDeleted, ids, 0);
                    }
                }

                actionBar.hideActionMode();
                updateVisibleRows();

                MrMailbox.reloadMainChatlist();
                NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void createMenu(View v, boolean single) { // single=false: long click
        boolean mr_no_menu = false;

        if (actionBar.isActionModeShowed()) {
            return;
        }

        MessageObject message = null;
        if (v instanceof ChatMessageCell) {
            message = ((ChatMessageCell) v).getMessageObject();
        } else if (v instanceof ChatActionCell) {
            message = ((ChatActionCell) v).getMessageObject();
        }
        if (message == null) {
            return;
        }
        final int type = getMessageType(message); // 3=normal text message
        /*if (single && message.messageOwner.action instanceof TLRPC.TL_messageActionPinMessage) {
            scrollToMessageId(message.messageOwner.reply_to_msg_id, 0, true, 0);
            return;
        }*/

        selectedObject = null;
        forwaringMessage = null;
        for (int a = 1; a >= 0; a--) {
            selectedMessagesCanCopyIds[a].clear();
            selectedMessagesIds[a].clear();
        }
        actionBar.hideActionMode();

        if (single || type < 2 || type == 20) {
            if (type >= 0) {
                selectedObject = message;
                if (getParentActivity() == null) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());

                ArrayList<CharSequence> items = new ArrayList<>();
                final ArrayList<Integer> options = new ArrayList<>();

                if (type == 0) {
                    items.add(LocaleController.getString("Retry", R.string.Retry));
                    options.add(0);
                    items.add(LocaleController.getString("Delete", R.string.Delete));
                    options.add(1);
                } else if (type == 1) {
                    if (currentChat != null ) {
                        items.add(LocaleController.getString("Reply", R.string.Reply));
                        options.add(8);

                        items.add(LocaleController.getString("Delete", R.string.Delete));
                        options.add(1);
                    } else {
                        items.add(LocaleController.getString("Delete", R.string.Delete));
                        options.add(1);
                    }
                } else if (type == 20) {
                    items.add(LocaleController.getString("Retry", R.string.Retry));
                    options.add(0);
                    items.add(LocaleController.getString("Copy", R.string.Copy));
                    options.add(3);
                    items.add(LocaleController.getString("Delete", R.string.Delete));
                    options.add(1);
                } else {
                    {
                        items.add(LocaleController.getString("Reply", R.string.Reply));
                        options.add(8);

                        if (selectedObject.type == 0 || selectedObject.caption != null) {
                            items.add(LocaleController.getString("Copy", R.string.Copy));
                            options.add(3);
                        }
                        if (type == 3) {
                            if (selectedObject.messageOwner.media instanceof TLRPC.TL_messageMediaWebPage && MessageObject.isNewGifDocument(selectedObject.messageOwner.media.webpage.document)) {
                                items.add(LocaleController.getString("SaveToGIFs", R.string.SaveToGIFs));
                                options.add(11);
                            }
                        } else if (type == 4) {
                            if (selectedObject.isVideo()) {
                                mr_no_menu = true; // no menu for videos, use the long click
                            } else if (selectedObject.isMusic()) {
                                mr_no_menu = true; // no menu for audio, use the long click
                            } else if (selectedObject.getDocument() != null) {
                                if (MessageObject.isNewGifDocument(selectedObject.getDocument())) {
                                    items.add(LocaleController.getString("SaveToGIFs", R.string.SaveToGIFs));
                                    options.add(11);
                                }
                                items.add(LocaleController.getString("SaveToDownloads", R.string.SaveToDownloads));
                                options.add(10);
                                items.add(LocaleController.getString("ShareFile", R.string.ShareFile));
                                options.add(6);
                            } else {
                                mr_no_menu = true; // no menu for images, use the long click
                            }
                        } else if (type == 5) {
                            items.add(LocaleController.getString("ApplyLocalizationFile", R.string.ApplyLocalizationFile));
                            options.add(5);
                            items.add(LocaleController.getString("ShareFile", R.string.ShareFile));
                            options.add(6);
                        } else if (type == 6) {
                            items.add(LocaleController.getString("SaveToGallery", R.string.SaveToGallery));
                            options.add(7);
                            items.add(LocaleController.getString("SaveToDownloads", R.string.SaveToDownloads));
                            options.add(10);
                            items.add(LocaleController.getString("ShareFile", R.string.ShareFile));
                            options.add(6);
                        } else if (type == 7) {
                            items.add(LocaleController.getString("AddToStickers", R.string.AddToStickers));
                            options.add(9);
                        } else if (type == 8) {
                            // type=8=contact (?)
                        }
                        items.add(LocaleController.getString("Forward", R.string.Forward));
                        options.add(2);

                        items.add(LocaleController.getString("Delete", R.string.Delete));
                        options.add(1);

                    }

                    if( type == 3 || mr_no_menu ) {
                        // EDIT BY MR: type=3 is a normal message; we do not want a menu on a single click here:
                        // this distubs and the approach does not work for images (they're enlarged on single clicks).
                        // So, it is better to force learning the user to do a long click, which works for all message types equally.
                        options.clear();
                    }
                }

                if (options.isEmpty()) {
                    return;
                }
                final CharSequence[] finalItems = items.toArray(new CharSequence[items.size()]);
                builder.setItems(finalItems, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (selectedObject == null || i < 0 || i >= options.size()) {
                            return;
                        }
                        processSelectedOption(options.get(i));
                    }
                });

                builder.setTitle(LocaleController.getString("Message", R.string.Message));
                showDialog(builder.create());
            }
            return;
        }

        // handle long clicks
        final ActionBarMenu actionMode = actionBar.createActionMode();

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
        updateVisibleRows();
    }

    private String getMessageContent(MessageObject messageObject, int previousUid, boolean name) {
        String str = "";
        if (name) {
            if (previousUid != messageObject.messageOwner.from_id) {
                MrContact mrContact = MrMailbox.getContact(messageObject.messageOwner.from_id);
                str += mrContact.getDisplayName() + ":\n";
            }
        }
        if (messageObject.type == 0 && messageObject.messageOwner.message != null) {
            str += messageObject.messageOwner.message;
        } else if (messageObject.messageOwner.media != null && messageObject.messageOwner.media.caption != null) {
            str += messageObject.messageOwner.media.caption;
        } else {
            str += messageObject.messageText;
        }
        return str;
    }

    private void processSelectedOption(int option) {
        if (selectedObject == null) {
            return;
        }
        switch (option) {
            case 0: {
                /*
                if (SendMessagesHelper.getInstance().retrySendMessage(selectedObject, false)) {
                    moveScrollToLastMessage();
                }
                */
                break;
            }
            case 1: {
                // delete by context menu; this is not supported by us, the user shall use the long-click
                break;
            }
            case 2: {
                forwaringMessage = selectedObject;
                Bundle args = new Bundle();
                args.putBoolean("onlySelect", true);
                args.putInt("dialogsType", 1);
                DialogsActivity fragment = new DialogsActivity(args);
                fragment.setDelegate(this);
                presentFragment(fragment);
                break;
            }
            case 3: {
                AndroidUtilities.addToClipboard(getMessageContent(selectedObject, 0, false));
                break;
            }
            case 4: {
                String path = selectedObject.messageOwner.attachPath;
                if (path != null && path.length() > 0) {
                    File temp = new File(path);
                    if (!temp.exists()) {
                        path = null;
                    }
                }
                if (path == null || path.length() == 0) {
                    path = FileLoader.getPathToMessage(selectedObject.messageOwner).toString();
                }
                if (selectedObject.type == 3 || selectedObject.type == 1) {
                    if (Build.VERSION.SDK_INT >= 23 && getParentActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        getParentActivity().requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4);
                        selectedObject = null;
                        return;
                    }
                    MediaController.saveFile(path, getParentActivity(), selectedObject.type == 3 ? 1 : 0, null, null);
                }
                break;
            }
            case 5: {
                File locFile = null;
                if (selectedObject.messageOwner.attachPath != null && selectedObject.messageOwner.attachPath.length() != 0) {
                    File f = new File(selectedObject.messageOwner.attachPath);
                    if (f.exists()) {
                        locFile = f;
                    }
                }
                if (locFile == null) {
                    File f = FileLoader.getPathToMessage(selectedObject.messageOwner);
                    if (f.exists()) {
                        locFile = f;
                    }
                }
                if (locFile != null) {
                    if (LocaleController.getInstance().applyLanguageFile(locFile)) {
                        presentFragment(new LanguageSelectActivity());
                    } else {
                        if (getParentActivity() == null) {
                            selectedObject = null;
                            return;
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setMessage(LocaleController.getString("IncorrectLocalization", R.string.IncorrectLocalization));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                        showDialog(builder.create());
                    }
                }
                break;
            }
            case 6: {
                String path = selectedObject.messageOwner.attachPath;
                if (path != null && path.length() > 0) {
                    File temp = new File(path);
                    if (!temp.exists()) {
                        path = null;
                    }
                }
                if (path == null || path.length() == 0) {
                    path = FileLoader.getPathToMessage(selectedObject.messageOwner).toString();
                }
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType(selectedObject.getDocument().mime_type);
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(path)));
                getParentActivity().startActivityForResult(Intent.createChooser(intent, LocaleController.getString("ShareFile", R.string.ShareFile)), 500);
                break;
            }
            case 7: {
                String path = selectedObject.messageOwner.attachPath;
                if (path != null && path.length() > 0) {
                    File temp = new File(path);
                    if (!temp.exists()) {
                        path = null;
                    }
                }
                if (path == null || path.length() == 0) {
                    path = FileLoader.getPathToMessage(selectedObject.messageOwner).toString();
                }
                if (Build.VERSION.SDK_INT >= 23 && getParentActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    getParentActivity().requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4);
                    selectedObject = null;
                    return;
                }
                MediaController.saveFile(path, getParentActivity(), 0, null, null);
                break;
            }
            case 8: {
                showReplyPanel(true, selectedObject, null, null, false, true);
                break;
            }
            case 9: {
                showDialog(new StickersAlert(getParentActivity(), selectedObject.getInputStickerSet(), null, bottomOverlayChat.getVisibility() != View.VISIBLE ? chatActivityEnterView : null));
                break;
            }
            case 10: {
                if (Build.VERSION.SDK_INT >= 23 && getParentActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    getParentActivity().requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4);
                    selectedObject = null;
                    return;
                }
                String fileName = FileLoader.getDocumentFileName(selectedObject.getDocument());
                if (fileName == null || fileName.length() == 0) {
                    fileName = selectedObject.getFileName();
                }
                String path = selectedObject.messageOwner.attachPath;
                if (path != null && path.length() > 0) {
                    File temp = new File(path);
                    if (!temp.exists()) {
                        path = null;
                    }
                }
                if (path == null || path.length() == 0) {
                    path = FileLoader.getPathToMessage(selectedObject.messageOwner).toString();
                }
                MediaController.saveFile(path, getParentActivity(), selectedObject.isMusic() ? 3 : 2, fileName, selectedObject.getDocument() != null ? selectedObject.getDocument().mime_type : "");
                break;
            }
            case 11: {
                MediaController.SearchImage searchImage = MessagesController.getInstance().saveGif(selectedObject.getDocument());
                showGifHint();
                chatActivityEnterView.addRecentGif(searchImage);
                break;
            }
        }
        selectedObject = null;
    }

    @Override
    public void didSelectDialog(DialogsActivity activity, long did, boolean param) {
        if (dialog_id != 0 && (forwaringMessage != null || !selectedMessagesIds[0].isEmpty() || !selectedMessagesIds[1].isEmpty())) {
            ArrayList<MessageObject> fmessages = new ArrayList<>();
            if (forwaringMessage != null) {
                fmessages.add(forwaringMessage);
                forwaringMessage = null;
            } else {
                for (int a = 1; a >= 0; a--) {
                    ArrayList<Integer> ids = new ArrayList<>(selectedMessagesIds[a].keySet());
                    Collections.sort(ids);
                    for (int b = 0; b < ids.size(); b++) {
                        Integer id = ids.get(b);
                        MessageObject message = selectedMessagesIds[a].get(id);
                        if (message != null && id > 0) {
                            fmessages.add(message);
                        }
                    }
                    selectedMessagesCanCopyIds[a].clear();
                    selectedMessagesIds[a].clear();
                }

                actionBar.hideActionMode();
            }

            if (did != dialog_id) {
                int lower_part = (int) did;
                if (lower_part != 0) {
                    Bundle args = new Bundle();
                    args.putBoolean("scrollToTopOnResume", scrollToTopOnResume);
                    if (lower_part > 0) {
                        args.putInt("user_id", lower_part);
                    } else if (lower_part < 0) {
                        args.putInt("chat_id", -lower_part);
                    }

                    ChatActivity chatActivity = new ChatActivity(args);
                    if (presentFragment(chatActivity, true)) {
                        chatActivity.showReplyPanel(true, null, fmessages, null, false, false);
                        if (!AndroidUtilities.isTablet()) {
                            removeSelfFromStack();
                        }
                    } else {
                        activity.finishFragment();
                    }
                } else {
                    activity.finishFragment();
                }
            } else {
                activity.finishFragment();
                moveScrollToLastMessage();
                showReplyPanel(true, null, fmessages, null, false, AndroidUtilities.isTablet());
                if (AndroidUtilities.isTablet()) {
                    actionBar.hideActionMode();
                }
                updateVisibleRows();
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        if (actionBar.isActionModeShowed()) {
            for (int a = 1; a >= 0; a--) {
                selectedMessagesIds[a].clear();
                selectedMessagesCanCopyIds[a].clear();
            }
            actionBar.hideActionMode();
            updateVisibleRows();
            return false;
        } else if (chatActivityEnterView.isPopupShowing()) {
            chatActivityEnterView.hidePopup(true);
            return false;
        }
        return true;
    }

    private void updateVisibleRows() {
        if (chatListView == null) {
            return;
        }
        int count = chatListView.getChildCount();
        //MessageObject editingMessageObject = chatActivityEnterView != null ? chatActivityEnterView.getEditingMessageObject() : null;
        for (int a = 0; a < count; a++) {
            View view = chatListView.getChildAt(a);
            if (view instanceof ChatMessageCell) {
                ChatMessageCell cell = (ChatMessageCell) view;

                boolean disableSelection = false;
                boolean selected = false;
                if (actionBar.isActionModeShowed()) {
                    MessageObject messageObject = cell.getMessageObject();
                    if (/*messageObject == editingMessageObject ||*/ selectedMessagesIds[messageObject.getDialogId() == dialog_id ? 0 : 1].containsKey(messageObject.getId())) {
                        view.setBackgroundColor(Theme.MSG_SELECTED_BACKGROUND_COLOR);
                        selected = true;
                    } else {
                        view.setBackgroundColor(0);
                    }
                    disableSelection = true;
                } else {
                    view.setBackgroundColor(0);
                }

                cell.setMessageObject(cell.getMessageObject());
                cell.setCheckPressed(!disableSelection, disableSelection && selected);
                cell.setHighlighted(highlightMessageId != Integer.MAX_VALUE && cell.getMessageObject() != null && cell.getMessageObject().getId() == highlightMessageId);
                /*if (searchContainer != null && searchContainer.getVisibility() == View.VISIBLE && MessagesSearchQuery.getLastSearchQuery() != null) {
                    cell.setHighlightedText(MessagesSearchQuery.getLastSearchQuery());
                } else*/ {
                    cell.setHighlightedText(null);
                }
            } else if (view instanceof ChatActionCell) {
                ChatActionCell cell = (ChatActionCell) view;
                cell.setMessageObject(cell.getMessageObject());
            }
        }
    }

    private ArrayList<MessageObject> createVoiceMessagesPlaylist(MessageObject startMessageObject, boolean playingUnreadMedia) {
        ArrayList<MessageObject> messageObjects = new ArrayList<>();
        messageObjects.add(startMessageObject);
        int messageId = startMessageObject.getId();
        if (messageId != 0) {
            //boolean started = false;
            for (int a = messages.size() - 1; a >= 0; a--) {
                MessageObject messageObject = messages.get(a);
                if( (messageObject.getId() > messageId) && messageObject.isVoice() && (!playingUnreadMedia || (messageObject.isContentUnread() && !messageObject.isOut()))) {
                    messageObjects.add(messageObject);
                }
            }
        }
        return messageObjects;
    }

    private void alertUserOpenError(MessageObject message) {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
        if (message.type == 3) {
            builder.setMessage(LocaleController.getString("NoPlayerInstalled", R.string.NoPlayerInstalled));
        } else {
            builder.setMessage(LocaleController.formatString("NoHandleAppInstalled", R.string.NoHandleAppInstalled, message.getDocument().mime_type));
        }
        showDialog(builder.create());
    }

    private void openSearchWithText(String text) {
        avatarContainer.setVisibility(View.GONE);
        headerItem.setVisibility(View.GONE);
        attachItem.setVisibility(View.GONE);
        searchItem.setVisibility(View.VISIBLE);
        updateSearchButtons(0, 0, 0);
        updateBottomOverlay();
        openSearchKeyboard = text == null;
        searchItem.openSearch(openSearchKeyboard);
        if (text != null) {
            searchItem.getSearchField().setText(text);
            searchItem.getSearchField().setSelection(searchItem.getSearchField().length());
            //MessagesSearchQuery.searchMessagesInChat(text, dialog_id, mergeDialogId, classGuid, 0);
        }
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
            } else if (view instanceof ChatActionCell) {
                ChatActionCell cell = (ChatActionCell) view;
                MessageObject message = cell.getMessageObject();
                if (message != null) {
                    if (messageObject != null) {
                        if (message.getId() == messageObject.getId()) {
                            imageReceiver = cell.getPhotoImage();
                        }
                    } else if (fileLocation != null && message.photoThumbs != null) {
                        for (int b = 0; b < message.photoThumbs.size(); b++) {
                            TLRPC.PhotoSize photoSize = message.photoThumbs.get(b);
                            if (photoSize.location.volume_id == fileLocation.volume_id && photoSize.location.local_id == fileLocation.local_id) {
                                imageReceiver = cell.getPhotoImage();
                                break;
                            }
                        }
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
                if (view instanceof ChatActionCell && currentChat != null) {
                    object.dialogId = -currentChat.id;
                }
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

    public void sendPhoto(MediaController.PhotoEntry photoEntry) {
        if (photoEntry.imagePath != null) {
            SendMessagesHelper.prepareSendingPhoto(photoEntry.imagePath, null, dialog_id, replyingMessageObject, photoEntry.caption);
            showReplyPanel(false, null, null, null, false, true);
            m_mrChat.cleanDraft();
        } else if (photoEntry.path != null) {
            SendMessagesHelper.prepareSendingPhoto(photoEntry.path, null, dialog_id, replyingMessageObject, photoEntry.caption);
            showReplyPanel(false, null, null, null, false, true);
            m_mrChat.cleanDraft();
        }
    }

    public void showOpenUrlAlert(final String url) {
        if (Browser.isInternalUrl(url)) {
            Browser.openUrl(getParentActivity(), url);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setMessage(LocaleController.formatString("OpenUrlAlert", R.string.OpenUrlAlert, url));
            builder.setPositiveButton(LocaleController.getString("Open", R.string.Open), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Browser.openUrl(getParentActivity(), url);
                }
            });
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            showDialog(builder.create());
        }
    }

    private void removeMessageObject(MessageObject messageObject) {
        int index = messages.indexOf(messageObject);
        if (index == -1) {
            return;
        }
        messages.remove(index);
        if (chatAdapter != null) {
            chatAdapter.notifyItemRemoved(chatAdapter.messagesStartRow + messages.size() - index - 1);
        }
    }

    public class ChatActivityAdapter extends RecyclerView.Adapter {

        private Context mContext;
        private final boolean isBot = false;
        private int rowCount;
        private int loadingUpRow;
        private int loadingDownRow;
        private int messagesStartRow;
        private int messagesEndRow;

        public ChatActivityAdapter(Context context) {
            mContext = context;
        }

        public void updateRows() {
            rowCount = 0;
            if (!messages.isEmpty()) {
                if (!endReached) {
                    loadingUpRow = rowCount++;
                } else {
                    loadingUpRow = -1;
                }
                messagesStartRow = rowCount;
                rowCount += messages.size();
                messagesEndRow = rowCount;
                if (!forwardEndReached) {
                    loadingDownRow = rowCount++;
                } else {
                    loadingDownRow = -1;
                }
            } else {
                loadingUpRow = -1;
                loadingDownRow = -1;
                messagesStartRow = -1;
                messagesEndRow = -1;
            }
        }

        private class Holder extends RecyclerView.ViewHolder {

            public Holder(View itemView) {
                super(itemView);
            }
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public long getItemId(int i) {
            return RecyclerListView.NO_ID;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = null;
            if (viewType == ROWTYPE_MESSAGE_CELL) {
                if (!chatMessageCellsCache.isEmpty()) {
                    view = chatMessageCellsCache.get(0);
                    chatMessageCellsCache.remove(0);
                } else {
                    view = new ChatMessageCell(mContext);
                }
                ChatMessageCell chatMessageCell = (ChatMessageCell) view;
                chatMessageCell.setDelegate(new ChatMessageCell.ChatMessageCellDelegate() {
                    @Override
                    public void didPressedShare(ChatMessageCell cell) {
                        // a click on the icon rignt of a deaddrop's messages
                        createChatByDeaddropMsgId(cell.getMessageObject().getId());
                    }

                    @Override
                    public boolean needPlayAudio(MessageObject messageObject) {
                        if (messageObject.isVoice()) {
                            boolean result = MediaController.getInstance().playAudio(messageObject);
                            MediaController.getInstance().setVoiceMessagesPlaylist(result ? createVoiceMessagesPlaylist(messageObject, false) : null, false);
                            return result;
                        } else if (messageObject.isMusic()) {
                            return MediaController.getInstance().setPlaylist(messages, messageObject);
                        }
                        return false;
                    }

                    @Override
                    public void didPressedChannelAvatar(ChatMessageCell cell, TLRPC.Chat chat, int postId) {
                        if (actionBar.isActionModeShowed()) {
                            processRowSelect(cell);
                            return;
                        }
                        if (chat != null && chat != currentChat) {
                            Bundle args = new Bundle();
                            args.putInt("chat_id", chat.id);
                            if (postId != 0) {
                                args.putInt("message_id", postId);
                            }

                            presentFragment(new ChatActivity(args), true);
                        }
                    }

                    @Override
                    public void didPressedOther(ChatMessageCell cell) {
                        createMenu(cell, true);
                    }

                    @Override
                    public void didPressedUserAvatar(ChatMessageCell cell, TLRPC.User user) {
                        // press on the avatar beside the message
                        if (actionBar.isActionModeShowed()) {
                            processRowSelect(cell);
                            return;
                        }
                        if (user != null && user.id != UserConfig.getClientUserId()) {
                            Bundle args = new Bundle();
                            args.putInt("user_id", user.id);
                            ProfileActivity fragment = new ProfileActivity(args);
                            fragment.setPlayProfileAnimation(false/*currentUser != null && currentUser.id == user.id*/);
                            presentFragment(fragment);
                        }
                    }


                    @Override
                    public void didPressedCancelSendButton(ChatMessageCell cell) {
                        MessageObject message = cell.getMessageObject();
                        if (message.messageOwner.send_state != 0) {
                            SendMessagesHelper.getInstance().cancelSendingMessage(message);
                        }
                    }

                    @Override
                    public void didLongPressed(ChatMessageCell cell) {
                        createMenu(cell, false);
                    }

                    @Override
                    public boolean canPerformActions() {
                        return actionBar != null && !actionBar.isActionModeShowed();
                    }

                    @Override
                    public void didPressedUrl(MessageObject messageObject, final ClickableSpan url, boolean longPress) {
                        if (url == null) {
                            return;
                        }
                        if (url instanceof URLSpanUserMention) {
                            /*
                            TLRPC.User user = MessagesController.getInstance().getUser(Utilities.parseInt(((URLSpanUserMention) url).getURL()));
                            if (user != null) {
                                MessagesController.openChatOrProfileWith(user, null, ChatActivity.this, 0);
                            }
                            */
                        } else if (url instanceof URLSpanNoUnderline) {
                            String str = ((URLSpanNoUnderline) url).getURL();
                            if (str.startsWith("@")) {
                                //MessagesController.openByUserName(str.substring(1), ChatActivity.this, 0);
                            } else if (str.startsWith("#")) {
                                /*if (ChatObject.isChannel(currentChat)) {
                                    openSearchWithText(str);
                                } else*/ {
                                    DialogsActivity fragment = new DialogsActivity(null);
                                    fragment.setSearchString(str);
                                    presentFragment(fragment);
                                }
                            } else if (str.startsWith("/")) {
                                /*if (URLSpanBotCommand.enabled) {
                                    chatActivityEnterView.setCommand(messageObject, str, longPress, currentChat != null && currentChat.megagroup);
                                }*/
                            }
                        } else {
                            final String urlFinal = ((URLSpan) url).getURL();
                            if (longPress) {
                                BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
                                builder.setTitle(urlFinal);
                                builder.setItems(new CharSequence[]{LocaleController.getString("Open", R.string.Open), LocaleController.getString("Copy", R.string.Copy)}, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, final int which) {
                                        if (which == 0) {
                                            Browser.openUrl(getParentActivity(), urlFinal);
                                        } else if (which == 1) {
                                            AndroidUtilities.addToClipboard(urlFinal);
                                        }
                                    }
                                });
                                showDialog(builder.create());
                            } else {
                                if (url instanceof URLSpanReplacement) {
                                    showOpenUrlAlert(((URLSpanReplacement) url).getURL());
                                } else if (url instanceof URLSpan) {
                                    Browser.openUrl(getParentActivity(), urlFinal);
                                } else {
                                    url.onClick(fragmentView);
                                }
                            }
                        }
                    }

                    @Override
                    public void needOpenWebView(String url, String title, String description, String originalUrl, int w, int h) {
                        BottomSheet.Builder builder = new BottomSheet.Builder(mContext);
                        builder.setCustomView(new WebFrameLayout(mContext, builder.create(), title, description, originalUrl, url, w, h));
                        builder.setUseFullWidth(true);
                        showDialog(builder.create());
                    }

                    @Override
                    public void didPressedReplyMessage(ChatMessageCell cell, int id) {
                        MessageObject messageObject = cell.getMessageObject();
                        scrollToMessageId(id, messageObject.getId(), true);
                    }

                    @Override
                    public void didPressedImage(ChatMessageCell cell) {
                        MessageObject message = cell.getMessageObject();
                        /*if (message.isSendError()) { -- for our Messenger, sending is completely asynchrounous - as soon as there is a GUI object, it can be accessed
                            createMenu(cell, false);
                            return;
                        } else if (message.isSending()) {
                            return;
                        }*/
                        if (message.type == 13) {
                            showDialog(new StickersAlert(getParentActivity(), message.getInputStickerSet(), null, bottomOverlayChat.getVisibility() != View.VISIBLE ? chatActivityEnterView : null));
                        } else if (Build.VERSION.SDK_INT >= 16 && message.isVideo() || message.type == 1 || message.type == 0 && !message.isWebpageDocument() || message.isGif()) {
                            PhotoViewer.getInstance().setParentActivity(getParentActivity());
                            PhotoViewer.getInstance().openPhoto(message, message.type != 0 ? dialog_id : 0, 0, ChatActivity.this);
                        } else if (message.type == 3) {
                            try {
                                File f = null;
                                if (message.messageOwner.attachPath != null && message.messageOwner.attachPath.length() != 0) {
                                    f = new File(message.messageOwner.attachPath);
                                }
                                if (f == null || !f.exists()) {
                                    f = FileLoader.getPathToMessage(message.messageOwner);
                                }
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.fromFile(f), "video/mp4");
                                getParentActivity().startActivityForResult(intent, 500);
                            } catch (Exception e) {
                                alertUserOpenError(message);
                            }
                        } else if (message.type == 4) {
                            /* Telegram-FOSS: Try to fire off a geo: intent */
                            double lat = message.messageOwner.media.geo.lat;
                            double lon = message.messageOwner.media.geo._long;
                            //TODO: get actual message Sender, localization
                            Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                                    Uri.parse("geo:" + lat + "," + lon + "?z=15&q=" + lat + "," + lon + Uri.encode("(Shared by Delta Chat")));
                            if(intent.resolveActivity(getParentActivity().getPackageManager()) != null) {
                                try{
                                    getParentActivity().startActivity(intent);
                                }
                                catch (Exception e){
                                    Toast.makeText(getParentActivity(), "Error handling geo: intent", Toast.LENGTH_SHORT).show();
                                    FileLog.e("messenger", e);
                                }
                            }
                        } else if (message.type == 9 || message.type == 0) {
                            try {
                                AndroidUtilities.openForView(message, getParentActivity());
                            } catch (Exception e) {
                                alertUserOpenError(message);
                            }
                        }
                    }
                });
                chatMessageCell.setAllowAssistant(true);
            } else if (viewType == ROWTYPE_ACTION_CELL) {
                view = new ChatActionCell(mContext);
                ((ChatActionCell) view).setDelegate(new ChatActionCell.ChatActionCellDelegate() {
                    @Override
                    public void didClickedImage(ChatActionCell cell) {
                        MessageObject message = cell.getMessageObject();
                        PhotoViewer.getInstance().setParentActivity(getParentActivity());
                        TLRPC.PhotoSize photoSize = FileLoader.getClosestPhotoSizeWithSize(message.photoThumbs, 640);
                        if (photoSize != null) {
                            PhotoViewer.getInstance().openPhoto(photoSize.location, ChatActivity.this);
                        } else {
                            PhotoViewer.getInstance().openPhoto(message, 0, 0, ChatActivity.this);
                        }
                    }

                    @Override
                    public void didLongPressed(ChatActionCell cell) {
                        createMenu(cell, false);
                    }

                    @Override
                    public void needOpenUserProfile(int uid) {
                        if (uid < 0) {
                            Bundle args = new Bundle();
                            args.putInt("chat_id", -uid);
                            presentFragment(new ChatActivity(args), true);
                        } else if (uid != UserConfig.getClientUserId()) {
                            Bundle args = new Bundle();
                            args.putInt("user_id", uid);
                            ProfileActivity fragment = new ProfileActivity(args);
                            fragment.setPlayProfileAnimation(false/*currentUser != null && currentUser.id == uid*/);
                            presentFragment(fragment);
                        }
                    }
                });
            } else if (viewType == ROWTYPE_UNREAD_CELL) {
                view = new ChatUnreadCell(mContext);
            } else if (viewType == ROWTYPE_LOADING_CELL) {
                view = new ChatLoadingCell(mContext);
            }

            if( view != null ) {
                view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            }

            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position == loadingDownRow || position == loadingUpRow) {
                ChatLoadingCell loadingCell = (ChatLoadingCell) holder.itemView;
                loadingCell.setProgressVisible(loadsCount > 1);
            } else if (position >= messagesStartRow && position < messagesEndRow) {
                MessageObject message = messages.get(messages.size() - (position - messagesStartRow) - 1);
                View view = holder.itemView;

                boolean selected = false;
                boolean disableSelection = false;
                if (actionBar.isActionModeShowed()) {
                    //MessageObject messageObject = chatActivityEnterView != null ? chatActivityEnterView.getEditingMessageObject() : null;
                    if (/*messageObject == message ||*/ selectedMessagesIds[message.getDialogId() == dialog_id ? 0 : 1].containsKey(message.getId())) {
                        view.setBackgroundColor(Theme.MSG_SELECTED_BACKGROUND_COLOR);
                        selected = true;
                    } else {
                        view.setBackgroundColor(0);
                    }
                    disableSelection = true;
                } else {
                    view.setBackgroundColor(0);
                }

                if (view instanceof ChatMessageCell) {
                    ChatMessageCell messageCell = (ChatMessageCell) view;
                    messageCell.isChat = m_mrChat.getType()==MrChat.MR_CHAT_GROUP;//currentChat != null;
                    messageCell.setMessageObject(message);
                    messageCell.setCheckPressed(!disableSelection, disableSelection && selected);
                    if (view instanceof ChatMessageCell && MediaController.getInstance().canDownloadMedia(MediaController.AUTODOWNLOAD_MASK_AUDIO)) {
                        ((ChatMessageCell) view).downloadAudioIfNeed();
                    }
                    messageCell.setHighlighted(highlightMessageId != Integer.MAX_VALUE && message.getId() == highlightMessageId);
                    /*if (searchContainer != null && searchContainer.getVisibility() == View.VISIBLE && MessagesSearchQuery.getLastSearchQuery() != null) {
                        messageCell.setHighlightedText(MessagesSearchQuery.getLastSearchQuery());
                    } else*/ {
                        messageCell.setHighlightedText(null);
                    }
                } else if (view instanceof ChatActionCell) {
                    ChatActionCell actionCell = (ChatActionCell) view;
                    actionCell.setMessageObject(message);
                } else if (view instanceof ChatUnreadCell) {
                    ChatUnreadCell unreadCell = (ChatUnreadCell) view;
                    unreadCell.setText(LocaleController.formatPluralString("NewMessages", unread_to_load));
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position >= messagesStartRow && position < messagesEndRow) {
                return messages.get(messages.size() - (position - messagesStartRow) - 1).contentType;
                                // may be ROWTYPE_MESSAGE_CELL, ROWTYPE_ACTION_CELL, ROWTYPE_UNREAD_CELL,
            }
            return ROWTYPE_LOADING_CELL;
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
                messageCell.setHighlighted(highlightMessageId != Integer.MAX_VALUE && messageCell.getMessageObject().getId() == highlightMessageId);
            }
        }

        public void updateRowWithMessageObject(MessageObject messageObject) {
            int index = messages.indexOf(messageObject);
            if (index == -1) {
                return;
            }
            notifyItemChanged(messagesStartRow + messages.size() - index - 1);
        }

        @Override
        public void notifyDataSetChanged() {
            updateRows();
            try {
                super.notifyDataSetChanged();
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
        }

        @Override
        public void notifyItemChanged(int position) {
            updateRows();
            try {
                super.notifyItemChanged(position);
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
        }

        @Override
        public void notifyItemRangeChanged(int positionStart, int itemCount) {
            updateRows();
            try {
                super.notifyItemRangeChanged(positionStart, itemCount);
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
        }

        @Override
        public void notifyItemInserted(int position) {
            updateRows();
            try {
                super.notifyItemInserted(position);
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
        }

        @Override
        public void notifyItemMoved(int fromPosition, int toPosition) {
            updateRows();
            try {
                super.notifyItemMoved(fromPosition, toPosition);
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
        }

        @Override
        public void notifyItemRangeInserted(int positionStart, int itemCount) {
            updateRows();
            try {
                super.notifyItemRangeInserted(positionStart, itemCount);
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
        }

        @Override
        public void notifyItemRemoved(int position) {
            updateRows();
            try {
                super.notifyItemRemoved(position);
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
        }

        @Override
        public void notifyItemRangeRemoved(int positionStart, int itemCount) {
            updateRows();
            try {
                super.notifyItemRangeRemoved(positionStart, itemCount);
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
        }
    }
}
