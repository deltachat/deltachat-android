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
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import com.b44t.messenger.Cells.GreySectionCell;
import com.b44t.messenger.aosp.LinearLayoutManager;
import com.b44t.messenger.aosp.RecyclerView;
import com.b44t.messenger.ActionBar.BackDrawable;
import com.b44t.messenger.Cells.UserCell;
import com.b44t.messenger.Cells.ChatlistCell;
import com.b44t.messenger.ActionBar.ActionBar;
import com.b44t.messenger.ActionBar.ActionBarMenu;
import com.b44t.messenger.ActionBar.ActionBarMenuItem;
import com.b44t.messenger.ActionBar.BaseFragment;
import com.b44t.messenger.Components.EmptyTextProgressView;
import com.b44t.messenger.Components.LayoutHelper;
import com.b44t.messenger.Components.RecyclerListView;
import com.b44t.messenger.ActionBar.Theme;

import java.util.ArrayList;


public class ChatlistActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    
    private RecyclerListView listView;
    private LinearLayoutManager layoutManager;
    private ChatlistAdapter chatlistAdapter;
    private ChatlistSearchAdapter chatlistSearchAdapter;
    private EmptyTextProgressView searchEmptyView;
    private LinearLayout emptyView;
    private ActionBarMenuItem passcodeItem;
    private ImageView floatingButton;

    // Floating hiding action as in T'gram - I think this is not useful:
    // - it always takes a moment to check if the button is there or not (ot to let it appear)
    // - if there is nothing to scroll the floting button does not move away -
    //   and covers always a part of the last row. This is not better than without moving away.
    // - hidden or not, covered parts oif the last row can be seen by moving the content
        /* private int prevPosition;
        private int prevTop;
        private boolean scrollUpdated; */
    // /Floating hiding action

    private boolean floatingHidden;
    private final AccelerateDecelerateInterpolator floatingInterpolator = new AccelerateDecelerateInterpolator();

    private boolean checkPermission = true;

    private String selectAlertString, selectAlertPreviewString, selectAlertOkButtonString;

    private static boolean dialogsLoaded;
    private boolean searching;
    private boolean searchWas;
    private boolean onlySelect;
    private String onlySelectTitle = "";

    private boolean showArchivedOnly;

    private ChatlistActivityDelegate delegate;

    ActionBarMenuItem headerItem;

    private static final int ID_LOCK_APP = 1;
    private static final int ID_NEW_CHAT = 2;
    private static final int ID_NEW_GROUP= 3;
    private static final int ID_NEW_VERIFIED_GROUP = 4;
    private static final int ID_SETTINGS = 5;
    private static final int ID_DEADDROP = 7;
    private static final int ID_SCAN_QR  = 8;
    private static final int ID_SHOW_QR  = 9;

    public interface ChatlistActivityDelegate {
        void didSelectChat(ChatlistActivity fragment, long dialog_id, boolean param);
    }

    public ChatlistActivity(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        if (getArguments() != null) {
            onlySelect = arguments.getBoolean("onlySelect", false);
            onlySelectTitle = arguments.getString("onlySelectTitle");
            if( onlySelectTitle==null || onlySelectTitle.isEmpty()) {
                onlySelectTitle = ApplicationLoader.applicationContext.getString(R.string.SelectChat);
            }
            selectAlertString = arguments.getString("selectAlertString");
            selectAlertPreviewString = arguments.getString("selectAlertPreviewString");
            selectAlertOkButtonString = arguments.getString("selectAlertOkButtonString");
            showArchivedOnly = arguments.getBoolean("showArchivedOnly");
        }

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.dialogsNeedReload);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.contactsDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.notificationsSettingsUpdated);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.messageSendError);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.didSetPasscode);

        if (!dialogsLoaded) {
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload); // this is the rest of the first call to the removed MessagesController.loadDialogs(); not sure, if this is really needed
            dialogsLoaded = true;
        }
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();

        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.dialogsNeedReload);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.contactsDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.notificationsSettingsUpdated);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messageSendError);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didSetPasscode);

        delegate = null;
    }

    @Override
    public View createView(final Context context) {
        searching = false;
        searchWas = false;

        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                Theme.loadRecources(context);
            }
        });

        if( !showArchivedOnly ) {
            ActionBarMenu menu = actionBar.createMenu();
            if (!onlySelect) {
                passcodeItem = menu.addItem(ID_LOCK_APP, R.drawable.ic_ab_lock_screen);
                updatePasscodeButton();
            }
            final ActionBarMenuItem item = menu.addItem(0, R.drawable.ic_ab_search).setIsSearchField(true, true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
                @Override
                public void onSearchExpand() {
                    if (headerItem != null) {
                        headerItem.setVisibility(View.GONE);
                        actionBar.setBackButtonDrawable(new BackDrawable(false));
                    }
                    searching = true;
                    if (listView != null) {
                        if (floatingButton!=null) {
                            floatingButton.setVisibility(View.GONE);
                        }
                    }
                    updatePasscodeButton();
                }

                @Override
                public boolean canCollapseSearch() {
                    return true;
                }

                @Override
                public void onSearchCollapse() {
                    if (headerItem != null) {
                        headerItem.setVisibility(View.VISIBLE);
                        actionBar.setBackButtonDrawable(null);
                    }
                    searching = false;
                    searchWas = false;
                    if (listView != null) {
                        searchEmptyView.setVisibility(View.GONE);
                        listView.setEmptyView(emptyView);
                        if (floatingButton!=null) {
                            floatingButton.setVisibility(View.VISIBLE);
                            floatingHidden = true;
                            floatingButton.setTranslationY(AndroidUtilities.dp(100));
                            hideFloatingButton(false);
                        }
                        if (listView.getAdapter() != chatlistAdapter) {
                            listView.setAdapter(chatlistAdapter);
                            chatlistAdapter.reloadChatlist();
                            chatlistAdapter.notifyDataSetChanged();
                        }
                    }
                    updatePasscodeButton();
                }

                @Override
                public void onTextChanged(EditText editText) {
                    String text = editText.getText().toString();
                    if (text.length() != 0) {
                        // text entered
                        searchWas = true;
                        if (searchEmptyView != null && listView.getEmptyView() != searchEmptyView) {
                            emptyView.setVisibility(View.GONE);
                            searchEmptyView.showTextView();
                            listView.setEmptyView(searchEmptyView);
                        }
                        if (chatlistSearchAdapter != null) {
                            if (listView.getAdapter() != chatlistSearchAdapter) {
                                listView.setAdapter(chatlistSearchAdapter);
                            }
                            chatlistSearchAdapter.doSearch(text);
                            chatlistSearchAdapter.notifyDataSetChanged();
                        }
                    } else if (listView.getAdapter() == chatlistSearchAdapter) {
                        // empty text
                        listView.setAdapter(chatlistAdapter);
                        chatlistAdapter.reloadChatlist();
                        chatlistAdapter.notifyDataSetChanged();
                    }
                }
            });
            item.getSearchField().setHint(ApplicationLoader.applicationContext.getString(R.string.Search));

            headerItem = menu.addItem(0, R.drawable.ic_ab_other);
            headerItem.addSubItem(ID_NEW_CHAT, ApplicationLoader.applicationContext.getString(R.string.NewChat));
            headerItem.addSubItem(ID_NEW_GROUP, ApplicationLoader.applicationContext.getString(R.string.NewGroup));
            if(!onlySelect) {
                if( MrMailbox.getConfigInt("qr_enabled", 0) != 0 ) {
                    headerItem.addSubItem(ID_NEW_VERIFIED_GROUP, ApplicationLoader.applicationContext.getString(R.string.NewVerifiedGroup));
                    headerItem.addSubItem(ID_SCAN_QR, ApplicationLoader.applicationContext.getString(R.string.QrScan));
                    headerItem.addSubItem(ID_SHOW_QR, ApplicationLoader.applicationContext.getString(R.string.QrShow));
                }
                headerItem.addSubItem(ID_DEADDROP, ApplicationLoader.applicationContext.getString(R.string.Deaddrop));
                headerItem.addSubItem(ID_SETTINGS, ApplicationLoader.applicationContext.getString(R.string.Settings));
            }
        }

        if( showArchivedOnly ) {
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            actionBar.setTitle(ApplicationLoader.applicationContext.getString(R.string.ArchivedChats));
        }
        else if (onlySelect) {
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            actionBar.setTitle(onlySelectTitle);
        } else {
            actionBar.setTitle(ApplicationLoader.applicationContext.getString(R.string.AppName));
        }

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    if (onlySelect || showArchivedOnly) {
                        finishFragment();
                    }
                } else if (id == ID_LOCK_APP) {

                    listView.setVisibility(View.INVISIBLE);
                    UserConfig.appLocked = !UserConfig.appLocked;
                    UserConfig.saveConfig();
                    if( UserConfig.appLocked )
                    {
                        // hide list as it is visible in the "last app switcher" otherwise, save state
                        updatePasscodeButton();

                        // finish the activity after a little delay; 200 ms shoud be enough to
                        // let the system update its screenshots for the "last app switcher".
                        // FLAG_SECURE may be a little too much as it affects display and screenshots;
                        // it also does not really direct this problem.
                        Utilities.searchQueue.postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                AndroidUtilities.runOnUIThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        getParentActivity().finish();
                                    }
                                });
                            }
                        }, 200);
                    }
                }
                else {
                    if (id == ID_NEW_CHAT) {
                        Bundle args = new Bundle();
                        args.putInt("do_what", ContactsActivity.SELECT_CONTACT_FOR_NEW_CHAT);
                        presentFragment(new ContactsActivity(args));
                    } else if (id == ID_NEW_GROUP) {
                        Bundle args = new Bundle();
                        args.putInt("do_what", ContactsActivity.SELECT_CONTACTS_FOR_NEW_GROUP);
                        presentFragment(new ContactsActivity(args));
                    } else if (id == ID_NEW_VERIFIED_GROUP) {
                        Bundle args = new Bundle();
                        args.putInt("do_what", ContactsActivity.SELECT_CONTACTS_FOR_NEW_VERIFIED_GROUP);
                        presentFragment(new ContactsActivity(args));
                    } else if (id == ID_DEADDROP) {
                        Bundle args = new Bundle();
                        args.putInt("chat_id", MrChat.MR_CHAT_ID_DEADDROP);
                        presentFragment(new ChatActivity(args));
                    } else if (id == ID_SETTINGS) {
                        presentFragment(new SettingsFragment());
                    }
                    else if(id == ID_SCAN_QR) {
                        new IntentIntegrator(getParentActivity()).setCaptureActivity(QRscanActivity.class).initiateScan();
                    }
                    else if(id == ID_SHOW_QR) {
                        Intent intent2 = new Intent(getParentActivity(), QRshowActivity.class);
                        getParentActivity().startActivity(intent2);
                    }

                }
            }
        });


        FrameLayout frameLayout = new FrameLayout(context);
        fragmentView = frameLayout;
        
        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(true);
        listView.setItemAnimator(null);
        listView.setInstantClick(true);
        listView.setLayoutAnimation(null);
        listView.setTag(4);
        layoutManager = new LinearLayoutManager(context) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        listView.setLayoutManager(layoutManager);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (listView == null || listView.getAdapter() == null) {
                    return;
                }

                // handle single click
                long chat_id = 0;
                int message_id = 0;
                RecyclerView.Adapter adapter = listView.getAdapter();
                if (adapter == chatlistAdapter) {
                    MrChat mrChat = chatlistAdapter.getChatByIndex(position);
                    if (mrChat == null) {
                        return;
                    }
                    chat_id = mrChat.getId();
                } else if (adapter == chatlistSearchAdapter) {
                    Object obj  = chatlistSearchAdapter.getItem(position);
                    if( obj instanceof MrChat ) {
                        chat_id = ((MrChat)obj).getId();
                    }
                    else if( obj instanceof MrMsg) {
                        MrMsg  mrMsg = (MrMsg)obj;
                        chat_id = mrMsg.getChatId();
                        message_id = mrMsg.getId();
                    }
                }

                if (chat_id == 0) {
                    return;
                }

                if (onlySelect) {
                    /* select a chat */
                    didSelectResult(chat_id, true, false);
                } else if( chat_id == MrChat.MR_CHAT_ID_DEADDROP ) {
                    /* start new chat */
                    final MrMsg msg = chatlistAdapter.getMsgByIndex(position);
                    if( ChatlistCell.deaddropClosePressed ) {
                        MrMailbox.marknoticedContact(msg.getFromId());
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
                    }
                    else {
                        MrContact contact = MrMailbox.getContact(msg.getFromId());
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                int belonging_chat_id = MrMailbox.createChatByMsgId(msg.getId());
                                if( belonging_chat_id != 0 ) {
                                    Bundle args = new Bundle();
                                    args.putInt("chat_id", belonging_chat_id);
                                    presentFragment(new ChatActivity(args), false);
                                }
                            }
                        });
                        builder.setNegativeButton(R.string.NotNow, null);
                        builder.setNeutralButton(R.string.Never, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                MrMailbox.blockContact(msg.getFromId(), 1);
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
                            }
                        });
                        builder.setMessage(AndroidUtilities.replaceTags(String.format(context.getString(R.string.AskStartChatWith), contact.getNameNAddr())));
                        showDialog(builder.create());
                    }
                } else if( chat_id == MrChat.MR_CHAT_ID_ARCHIVED_LINK ) {
                    Bundle args = new Bundle();
                    args.putBoolean("showArchivedOnly", true);
                    args.putBoolean("onlySelect", onlySelect);
                    args.putString("onlySelectTitle", onlySelectTitle);
                    args.putString("selectAlertString", selectAlertString);
                    args.putString("selectAlertPreviewString", selectAlertPreviewString);
                    args.putString("selectAlertOkButtonString", selectAlertOkButtonString);
                    presentFragment(new ChatlistActivity(args));
                } else {
                    /* open exiting chat */
                    Bundle args = new Bundle();
                    args.putInt("chat_id", (int)chat_id);
                    if (message_id != 0) {
                        args.putInt("message_id", message_id);
                    }

                    presentFragment(new ChatActivity(args));
                }
            }
        });

        listView.setOnItemLongClickListener(new RecyclerListView.OnItemLongClickListener() {
            @Override
            public boolean onItemClick(View view, int position) {
                RecyclerView.Adapter adapter = listView.getAdapter();
                MrChat tempChat  = null;
                if (adapter == chatlistAdapter) {
                    tempChat = chatlistAdapter.getChatByIndex(position);
                }
                else if( adapter == chatlistSearchAdapter ) {
                    Object obj  = chatlistSearchAdapter.getItem(position);
                    if( obj instanceof MrChat ) {
                        tempChat = ((MrChat)obj);
                    }
                    else if( obj instanceof MrMsg ) {
                        MrMsg mrMsg = (MrMsg) obj;
                        tempChat = MrMailbox.getChat(mrMsg.getChatId());
                    }
                }

                if( tempChat!=null ) {
                    final MrChat mrChat = tempChat;
                    if (mrChat != null && mrChat.getId()>MrChat.MR_CHAT_ID_LAST_SPECIAL) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        CharSequence[] items = new CharSequence[]{
                                context.getString(mrChat.getArchived()==0? R.string.ArchiveChat : R.string.UnarchiveChat),
                                context.getString(R.string.DeleteChat)
                        };
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int clickedEntry) {
                                if( clickedEntry==0 ) {
                                    MrMailbox.archiveChat(mrChat.getId(), mrChat.getArchived() == 0 ? 1 : 0);
                                    AndroidUtilities.showDoneHint(context);
                                }
                                else if( clickedEntry==1 ) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                                    builder.setMessage(context.getString(R.string.AreYouSureDeleteThisChat));
                                    builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            MrMailbox.deleteChat(mrChat.getId());
                                            AndroidUtilities.showDoneHint(context);
                                        }
                                    });
                                    builder.setNegativeButton(R.string.Cancel, null);
                                    showDialog(builder.create());

                                }
                            }
                        });
                        showDialog(builder.create());
                        return true; // do haptical feedback
                    }
                }
                return false;
            }
        });

        searchEmptyView = new EmptyTextProgressView(context);
        searchEmptyView.setVisibility(View.GONE);
        searchEmptyView.setShowAtCenter(true);
        searchEmptyView.setText(context.getString(R.string.NoResult));
        frameLayout.addView(searchEmptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        emptyView = new LinearLayout(context);
        emptyView.setOrientation(LinearLayout.VERTICAL);
        emptyView.setVisibility(View.GONE);
        emptyView.setGravity(Gravity.CENTER);
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        emptyView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        if( !showArchivedOnly ) {
            TextView textView = new TextView(context);
            textView.setText(context.getString(R.string.NoChats));
            textView.setTextColor(0xff959595);
            textView.setGravity(Gravity.CENTER);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            emptyView.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            textView = new TextView(context);
            String help = context.getString(R.string.NoChatsHelp);
            textView.setText(help);
            textView.setTextColor(0xff959595);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            textView.setGravity(Gravity.CENTER);
            textView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(64) /*move the whole stuff a little bit up*/);
            textView.setLineSpacing(AndroidUtilities.dp(2), 1);
            emptyView.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        }

        if(!onlySelect && !showArchivedOnly) {
            floatingButton = new ImageView(context);
            floatingButton.setVisibility(View.VISIBLE);
            floatingButton.setScaleType(ImageView.ScaleType.CENTER);
            floatingButton.setBackgroundResource(R.drawable.floating_states);
            floatingButton.setImageResource(R.drawable.floating_pencil);
            if (Build.VERSION.SDK_INT >= 21) {
                StateListAnimator animator = new StateListAnimator();
                animator.addState(new int[]{android.R.attr.state_pressed}, ObjectAnimator.ofFloat(floatingButton, "translationZ", AndroidUtilities.dp(2), AndroidUtilities.dp(4)).setDuration(200));
                animator.addState(new int[]{}, ObjectAnimator.ofFloat(floatingButton, "translationZ", AndroidUtilities.dp(4), AndroidUtilities.dp(2)).setDuration(200));
                floatingButton.setStateListAnimator(animator);
                floatingButton.setOutlineProvider(new ViewOutlineProvider() {
                    @SuppressLint("NewApi")
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setOval(0, 0, AndroidUtilities.dp(56), AndroidUtilities.dp(56));
                    }
                });
            }
            frameLayout.addView(floatingButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.END | Gravity.BOTTOM, LocaleController.isRTL ? 14 : 0, 0, LocaleController.isRTL ? 0 : 14, 14));
            floatingButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bundle args = new Bundle();
                    args.putInt("do_what", ContactsActivity.SELECT_CONTACT_FOR_NEW_CHAT);
                    presentFragment(new ContactsActivity(args));
                }
            });
        }

        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                // if we disable this, the keyboard always lays over the list and the end of the list is never reachable -- was: due to the setIsSearchField()-HACK, we do not want force keyboard disappering (HACK looks smaller so ;-)
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING && searching && searchWas) {
                    AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                //int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                //int visibleItemCount = Math.abs(layoutManager.findLastVisibleItemPosition() - firstVisibleItem) + 1;
                //int totalItemCount = recyclerView.getAdapter().getItemCount();

                //if (searching && searchWas) {
                    //if (visibleItemCount > 0 && layoutManager.findLastVisibleItemPosition() == totalItemCount - 1 && !dialogsSearchAdapter.isMessagesSearchEndReached()) {
                    //    dialogsSearchAdapter.loadMoreSearchMessages();
                    //}
                    //return;
                //}

                // Floating hiding action
                /* if (floatingButton.getVisibility() != View.GONE) {
                    final View topChild = recyclerView.getChildAt(0);
                    int firstViewTop = 0;
                    if (topChild != null) {
                        firstViewTop = topChild.getTop();
                    }
                    boolean goingDown;
                    boolean changed = true;
                    if (prevPosition == firstVisibleItem) {
                        final int topDelta = prevTop - firstViewTop;
                        goingDown = firstViewTop < prevTop;
                        changed = Math.abs(topDelta) > 1;
                    } else {
                        goingDown = firstVisibleItem > prevPosition;
                    }
                    if (changed && scrollUpdated) {
                        hideFloatingButton(goingDown);
                    }
                    prevPosition = firstVisibleItem;
                    prevTop = firstViewTop;
                    scrollUpdated = true;
                } */
                // /Floating hiding action
            }
        });

        int listflags = 0;
        if( showArchivedOnly ) {
            listflags = MrMailbox.MR_GCL_ARCHIVED_ONLY;
        }
        else if( onlySelect ) {
            listflags = MrMailbox.MR_GCL_NO_SPECIALS;
        }
        chatlistAdapter = new ChatlistAdapter(context, listflags);

        listView.setAdapter(chatlistAdapter);
        chatlistSearchAdapter = new ChatlistSearchAdapter(context);

        searchEmptyView.setVisibility(View.GONE);
        listView.setEmptyView(emptyView);

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (chatlistAdapter != null) {
            chatlistAdapter.notifyDataSetChanged();
        }
        if (chatlistSearchAdapter != null) {
            chatlistSearchAdapter.notifyDataSetChanged();
        }
        if (checkPermission && !onlySelect && Build.VERSION.SDK_INT >= 23) {
            checkPermission = false;
            askForIgnoreBatteryOptimization(); // after that, requestForOtherPermissions() is called
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void askForIgnoreBatteryOptimization() {
        boolean requestIgnoreActivityStarted = false;

        /* -- we do not ask for this permission as this would require the permission
           -- android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS which is ususally
           -- not permitted in the playstore :-(

        try {
            String packageName = ApplicationLoader.applicationContext.getPackageName();
            PowerManager pm = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {

                // As FCM (was: GCM) does not support IMAP servers and there are other server involved,
                // the only possibility for us to get notified about new messages, is to keep the connection to the server alive.
                // This is done by ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS.
                //
                // This is an "Acceptable Use Cases for Whitelisting", see
                // https://developer.android.com/training/monitoring-device-state/doze-standby.html#whitelisting-cases
                // "Instant messaging, chat, or calling app; enterprise VOIP apps |
                //  No, can't use FCM because of technical dependency on another messaging service or Doze and App Standby break the core function of the app |
                //  Acceptable"
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                getParentActivity().startActivityForResult(intent, BaseFragment.RC600_BATTERY_REQUEST_DONE);
                requestIgnoreActivityStarted = true;
            }
        } catch (Exception e) {
            Log.e("DeltaChat", "cannot ignore battery optimizations.", e);
        }
        */

        if( !requestIgnoreActivityStarted ) {
            askForOtherPermissons(); // otherwise, it gets started on RC600_BATTERY_REQUEST_DONE
        }
    }

    private ProgressDialog progressDialog = null;
    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {

        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

            if (scanResult == null || scanResult.getFormatName() == null) {
                return; // Should not happen!
            }

            if( progressDialog!=null ) {
                progressDialog.dismiss();
                progressDialog = null;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            final String qrRawString = scanResult.getContents();
            final MrLot  qrParsed = MrMailbox.checkQr(qrRawString);
            String nameNAddr = MrMailbox.getContact(qrParsed.getId()).getNameNAddr();
            switch( qrParsed.getState() ) {

                case MrMailbox.MR_QR_FINGERPRINT_ASK_OOB:
                    builder.setMessage(AndroidUtilities.replaceTags(String.format(ApplicationLoader.applicationContext.getString(R.string.OobFingerprintAskOob), nameNAddr)));
                    builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
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

                                    synchronized (MrMailbox.m_lastErrorLock) {
                                        MrMailbox.m_showNextErrorAsToast = false;
                                        MrMailbox.m_lastErrorString = "";
                                    }

                                    final boolean oobDone = MrMailbox.joinSecurejoin(qrRawString); // oobJoin() runs until all needed messages are sent+received!
                                    AndroidUtilities.runOnUIThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            String errorString;
                                            synchronized (MrMailbox.m_lastErrorLock) {
                                                MrMailbox.m_showNextErrorAsToast = true;
                                                errorString = MrMailbox.m_lastErrorString;
                                            }

                                            if( progressDialog != null ) {
                                                progressDialog.dismiss();
                                                progressDialog = null;
                                            }
                                            if( oobDone  ) {
                                                Bundle args = new Bundle();
                                                args.putInt("chat_id", MrMailbox.createChatByContactId(qrParsed.getId()));
                                                presentFragment(new ChatActivity(args), false /*removeLast*/);
                                            }
                                            else if( !errorString.isEmpty() ) {
                                                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                                                builder.setMessage(errorString);
                                                builder.setPositiveButton(R.string.OK, null);
                                                showDialog(builder.create());
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    });
                    builder.setNegativeButton(R.string.Cancel, null);
                    break;


                case MrMailbox.MR_QR_FINGERPRINT_WITHOUT_ADDR:
                    builder.setMessage(AndroidUtilities.replaceTags(ApplicationLoader.applicationContext.getString(R.string.OobFingerprintWithoutAddr)+"\n\n<c#808080>"+ApplicationLoader.applicationContext.getString(R.string.OobFingerprint)+":\n"+qrParsed.getText1()+"</c>"));
                    builder.setPositiveButton(R.string.OK, null);
                    builder.setNeutralButton(R.string.CopyToClipboard, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AndroidUtilities.addToClipboard(qrParsed.getText1());
                            AndroidUtilities.showDoneHint(getParentActivity());
                        }
                    });
                    break;

                case MrMailbox.MR_QR_FINGERPRINT_MISMATCH:
                    builder.setMessage(AndroidUtilities.replaceTags(String.format(ApplicationLoader.applicationContext.getString(R.string.OobFingerprintMismatch), nameNAddr)));
                    builder.setPositiveButton(R.string.OK, null);
                    break;

                case MrMailbox.MR_QR_FINGERPRINT_OK:
                case MrMailbox.MR_QR_ADDR:
                    @StringRes int resId = qrParsed.getState()==MrMailbox.MR_QR_ADDR? R.string.AskStartChatWith : R.string.OobFingerprintOk;
                    builder.setMessage(AndroidUtilities.replaceTags(String.format(ApplicationLoader.applicationContext.getString(resId, nameNAddr))));
                    builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Bundle args = new Bundle();
                            args.putInt("chat_id", MrMailbox.createChatByContactId(qrParsed.getId()));
                            presentFragment(new ChatActivity(args), false /*removeLast*/);
                        }
                    });
                    builder.setNegativeButton(R.string.Cancel, null);
                    break;

                default:
                    String msg;
                    final String scannedText;
                    switch( qrParsed.getState() ) {
                        case MrMailbox.MR_QR_ERROR:scannedText = qrRawString;         msg = qrParsed.getText1()+"\n\n<c#808080>"+String.format(ApplicationLoader.applicationContext.getString(R.string.QrScanContainsText), scannedText)+"</c>"; break;
                        case MrMailbox.MR_QR_TEXT: scannedText = qrParsed.getText1(); msg = String.format(ApplicationLoader.applicationContext.getString(R.string.QrScanContainsText), scannedText); break;
                        default:                   scannedText = qrRawString;         msg = String.format(ApplicationLoader.applicationContext.getString(R.string.QrScanContainsText), scannedText); break;
                    }
                    builder.setMessage(AndroidUtilities.replaceTags(msg));
                    builder.setPositiveButton(R.string.OK, null);
                    builder.setNeutralButton(R.string.CopyToClipboard, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AndroidUtilities.addToClipboard(scannedText);
                            AndroidUtilities.showDoneHint(getParentActivity());
                        }
                    });
                    break;
            }

            showDialog(builder.create());
        }

        /* -- see comment above
        if (requestCode == BaseFragment.RC600_BATTERY_REQUEST_DONE) {
            boolean requestIgnoreActivityMaybeRestarted = false;

            if( Build.VERSION.SDK_INT >= 23 ) {
                PowerManager pm = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
                if (!pm.isIgnoringBatteryOptimizations(ApplicationLoader.applicationContext.getPackageName())) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setMessage(AndroidUtilities.replaceTags(ApplicationLoader.applicationContext.getString(R.string.PermissionBattery)));
                    builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            askForIgnoreBatteryOptimization();
                        }
                    });
                    builder.setCancelable(false);
                    builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            askForOtherPermissons();
                        }
                    });
                    builder.show();
                    requestIgnoreActivityMaybeRestarted  = true;

                    // -- this is an alternative implementation to the alert above
                    // IF we use this, we should handle the situation, ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS cannot be started due to a missing REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission
                    // checkPermission = true;
                    // getParentActivity().finish();
                    //return;
                }
            }

            if( !requestIgnoreActivityMaybeRestarted ) {
                askForOtherPermissons();
            }
        }
        */
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void askForOtherPermissons() {
        Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }

        if (activity.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
         && activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return; // everything is fine
        }

        ArrayList<String> permissons = new ArrayList<>();
        if (activity.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissons.add(Manifest.permission.READ_CONTACTS);
        }

        if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissons.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissons.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        String[] items = permissons.toArray(new String[permissons.size()]);
        activity.requestPermissions(items, LaunchActivity.REQ_CONTACT_N_STORAGE_PERMISON_ID);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (floatingButton != null) {
            floatingButton.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    floatingButton.setTranslationY(floatingHidden ? AndroidUtilities.dp(100) : 0);
                    floatingButton.setClickable(!floatingHidden);
                    if (floatingButton != null) {
                        if (Build.VERSION.SDK_INT < 16) {
                            floatingButton.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        } else {
                            floatingButton.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == LaunchActivity.REQ_CONTACT_N_STORAGE_PERMISON_ID) {
            for (int a = 0; a < permissions.length; a++) {
                if (grantResults.length <= a || grantResults[a] != PackageManager.PERMISSION_GRANTED) {
                    continue;
                }
                switch (permissions[a]) {
                    case Manifest.permission.READ_CONTACTS:
                        //ContactsController.getInstance().readContacts();
                        break;
                    case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                        ImageLoader.getInstance().checkMediaPaths();
                        break;
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.dialogsNeedReload) {
            if (chatlistAdapter != null) {
                chatlistAdapter.reloadChatlist();
                chatlistAdapter.notifyDataSetChanged();
            }
            if (chatlistSearchAdapter != null) {
                chatlistSearchAdapter.searchAgain();
                chatlistSearchAdapter.notifyDataSetChanged();
            }
            if (listView != null) {
                try {
                        if (searching && searchWas) {
                            emptyView.setVisibility(View.GONE);
                            listView.setEmptyView(searchEmptyView);
                        } else {
                            searchEmptyView.setVisibility(View.GONE);
                            listView.setEmptyView(emptyView);
                        }
                } catch (Exception e) {
                }
            }
        } else if (id == NotificationCenter.updateInterfaces) {
            updateVisibleRows((Integer) args[0]);
        } else if (id == NotificationCenter.contactsDidLoaded) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.notificationsSettingsUpdated) {
            updateVisibleRows(0);
        } else if ( id == NotificationCenter.messageSendError) {
            updateVisibleRows(MrMailbox.UPDATE_MASK_SEND_STATE);
        } else if (id == NotificationCenter.didSetPasscode) {
            updatePasscodeButton();
        }
    }

    private void updatePasscodeButton() {
        if (passcodeItem == null) {
            return;
        }
        if (UserConfig.passcodeHash.length() != 0 && !searching) {
            passcodeItem.setVisibility(View.VISIBLE);
        } else {
            passcodeItem.setVisibility(View.GONE);
        }
    }

    private void hideFloatingButton(boolean hide) {
        if (floatingButton == null || floatingHidden == hide) {
            return;
        }
        floatingHidden = hide;
        ObjectAnimator animator = ObjectAnimator.ofFloat(floatingButton, "translationY", floatingHidden ? AndroidUtilities.dp(100) : 0).setDuration(300);
        animator.setInterpolator(floatingInterpolator);
        floatingButton.setClickable(!hide);
        animator.start();
    }

    private void updateVisibleRows(int mask) {
        if (listView == null) {
            return;
        }
        int count = listView.getChildCount();
        for (int a = 0; a < count; a++) {
            View child = listView.getChildAt(a);
            if (child instanceof ChatlistCell) {
                if (listView.getAdapter() != chatlistSearchAdapter) {
                    ChatlistCell cell = (ChatlistCell) child;
                    if ((mask & MrMailbox.UPDATE_MASK_NEW_MESSAGE) != 0) {
                        //cell.checkCurrentChatlistIndex();
                        cell.update(mask);
                    } else if ((mask & MrMailbox.UPDATE_MASK_SELECT_DIALOG) != 0) {
                        ;
                    } else {
                        cell.update(mask);
                    }
                }
            } else if (child instanceof UserCell) {
                ((UserCell) child).update();
            }
        }
    }

    public void setDelegate(ChatlistActivityDelegate chatlistActivityDelegate) {
        delegate = chatlistActivityDelegate;
    }

    private void didSelectResult(final long dialog_id, boolean useAlert, final boolean param) {
        if (useAlert && (selectAlertString != null )) {
            if (getParentActivity() == null) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());

            MrChat mrChat = MrMailbox.getChat((int)dialog_id);

            builder.setMessage(AndroidUtilities.replaceTags(
                    String.format(selectAlertString, mrChat.getNameNAddr()) // display addr as there may be contacts with the same name but different addresses
                +   (selectAlertPreviewString==null? "" : ("\n\n<c#808080>"+selectAlertPreviewString+"</c>"))));

            builder.setPositiveButton(selectAlertOkButtonString!=null? selectAlertOkButtonString : ApplicationLoader.applicationContext.getString(R.string.OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    didSelectResult(dialog_id, false, false);
                }
            });
            builder.setNegativeButton(R.string.Cancel, null);
            showDialog(builder.create());
        } else {
            if (delegate != null) {
                delegate.didSelectChat(ChatlistActivity.this, dialog_id, param);
                delegate = null;
            } else {
                finishFragment();
            }
        }
    }

    private class ChatlistAdapter extends RecyclerView.Adapter {

        private Context mContext;

        private int m_listflags;
        private MrChatlist m_chatlist = new MrChatlist(0);

        private class Holder extends RecyclerView.ViewHolder {
            public Holder(View itemView) {
                super(itemView);
            }
        }

        public void reloadChatlist() {
            m_chatlist = MrMailbox.getChatlist(m_listflags, null, 0);
        }
        public ChatlistAdapter(Context context, int listflags) {
            mContext = context;
            m_listflags = listflags;
            reloadChatlist();
        }

        @Override
        public int getItemCount() {
            return m_chatlist.getCnt();
        }

        public MrChat getChatByIndex(int i) {
            return m_chatlist.getChatByIndex(i);
        }

        public MrMsg getMsgByIndex(int i) {
            return m_chatlist.getMsgByIndex(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = new ChatlistCell(mContext);
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new ChatlistAdapter.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
            if (viewHolder.getItemViewType() == 0) {
                ChatlistCell cell = (ChatlistCell) viewHolder.itemView;
                cell.useSeparator = (i != getItemCount() - 1);
                MrChat mrChat = getChatByIndex(i);

                MrLot mrSummary = m_chatlist.getSummaryByIndex(i, mrChat);
                cell.setChat(mrChat, mrSummary, i, true);
            }
        }

        @Override
        public int getItemViewType(int i) {
            return 0;
        }
    }

    private class ChatlistSearchAdapter extends RecyclerView.Adapter {

        private Context mContext;

        private static final int ROWTYPE_HEADLINE = 0;
        private static final int ROWTYPE_CHAT     = 1;
        private static final int ROWTYPE_MSG      = 2;

        int rowChatsHeadline = -1;
        int rowFirstChat = -1, rowLastChat = -1;
        int rowMsgsHeadline = -1;
        int rowFirstMsg = -1, rowLastMsg = -1;
        int rowCount = 0;

        MrChatlist m_chatlist = new MrChatlist(0);
        int        m_chatlistCnt = 0;
        int[]      m_msgIds = {};

        public String m_lastQuery;

        private class Holder extends RecyclerView.ViewHolder {
            public Holder(View itemView) {
                super(itemView);
            }
        }

        public ChatlistSearchAdapter(Context context) {
            mContext = context;
        }

        public void searchAgain()
        {
            doSearch(m_lastQuery);
        }

        public void doSearch(String query) {
            m_lastQuery = query;

            rowCount = 0;

            m_chatlist = MrMailbox.getChatlist(0, query, 0);
            m_chatlistCnt = m_chatlist.getCnt();
            if( m_chatlistCnt>0 ) {
                rowChatsHeadline = rowCount++;

                rowFirstChat = rowCount;
                rowCount += m_chatlistCnt;
                rowLastChat = rowCount-1;
            }
            else {
                rowChatsHeadline = -1;
                rowFirstChat = -1;
                rowLastChat = -1;
            }

            m_msgIds = MrMailbox.searchMsgs(0, query);
            if( m_msgIds.length>0 ) {
                rowMsgsHeadline = rowCount++;

                rowFirstMsg = rowCount;
                rowCount += m_msgIds.length;
                rowLastMsg = rowCount-1;
            }
            else {
                rowMsgsHeadline = -1;
                rowFirstMsg = -1;
                rowLastMsg = -1;
            }
        }


        @Override
        public int getItemCount() {
            return rowCount;
        }

        public Object getItem(int i) {
            if( i>=rowFirstChat && i<=rowLastChat ) {
                return m_chatlist.getChatByIndex(i-rowFirstChat);
            }
            else if( i>=rowFirstMsg && i<=rowLastMsg ) {
                return MrMailbox.getMsg(m_msgIds[i-rowFirstMsg]);
            }
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view;
            switch( viewType ) {
                case ROWTYPE_CHAT:
                case ROWTYPE_MSG:
                    view = new ChatlistCell(mContext);
                    view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                    break;

                default:
                    view = new GreySectionCell(mContext);
                    break;
            }

            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i)
        {
            switch (viewHolder.getItemViewType() )
            {
                case ROWTYPE_CHAT: {
                    int j = i - rowFirstChat;
                    if( j >= 0 && j < m_chatlistCnt ) {
                        ChatlistCell cell = (ChatlistCell) viewHolder.itemView;
                        cell.useSeparator = (j != m_chatlistCnt - 1);

                        MrChat mrChat = m_chatlist.getChatByIndex(j);
                        MrLot mrSummary = m_chatlist.getSummaryByIndex(j, mrChat);

                        cell.setChat(mrChat, mrSummary, -1,
                                true /*always show unread count*/);
                    }
                }
                break;

                case ROWTYPE_MSG: {
                    int j = i - rowFirstMsg;
                    if( j >= 0 && j < m_msgIds.length ) {
                        ChatlistCell cell = (ChatlistCell) viewHolder.itemView;
                        cell.useSeparator = (j != m_msgIds.length - 1);

                        MrMsg mrMsg = MrMailbox.getMsg(m_msgIds[j]);
                        MrChat mrChat = MrMailbox.getChat(mrMsg.getChatId());
                        MrLot mrSummary = mrMsg.getSummary(mrChat);

                        cell.setChat(mrChat, mrSummary, -1,
                                mrMsg.getState()==MrMsg.MR_IN_FRESH /*show unread count only if the message itself is unread*/ );
                    }
                }
                break;

                case ROWTYPE_HEADLINE: {
                    GreySectionCell headlineCell = (GreySectionCell) viewHolder.itemView;
                    if (i == rowChatsHeadline) {
                        headlineCell.setText(mContext.getResources().getQuantityString(R.plurals.Chats, m_chatlistCnt, m_chatlistCnt));
                    } else if (i == rowMsgsHeadline) {
                        headlineCell.setText(mContext.getResources().getQuantityString(R.plurals.messages, m_msgIds.length, m_msgIds.length));
                    }
                }
                break;
            }
        }

        @Override
        public int getItemViewType(int i) {
            if( i>=rowFirstChat && i<=rowLastChat ) {
                return ROWTYPE_CHAT;
            }
            else if( i>=rowFirstMsg && i<=rowLastMsg ) {
                return ROWTYPE_MSG;
            }
            return ROWTYPE_HEADLINE;
        }
    }
}
