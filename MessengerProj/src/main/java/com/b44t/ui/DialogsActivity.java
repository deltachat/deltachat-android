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
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
//import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.BuildVars;
import com.b44t.messenger.ImageLoader;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MessageObject;
import com.b44t.messenger.UserObject;
import com.b44t.messenger.support.widget.LinearLayoutManager;
import com.b44t.messenger.support.widget.RecyclerView;
import com.b44t.messenger.FileLog;
import com.b44t.messenger.TLRPC;
import com.b44t.messenger.ContactsController;
import com.b44t.messenger.MessagesController;
import com.b44t.messenger.NotificationCenter;
import com.b44t.messenger.R;
import com.b44t.messenger.UserConfig;
import com.b44t.ui.Adapters.DialogsAdapter;
import com.b44t.ui.Adapters.DialogsSearchAdapter;
import com.b44t.ui.Cells.HintDialogCell;
import com.b44t.ui.Cells.ProfileSearchCell;
import com.b44t.ui.Cells.UserCell;
import com.b44t.ui.Cells.DialogCell;
import com.b44t.ui.ActionBar.ActionBar;
import com.b44t.ui.ActionBar.ActionBarMenu;
import com.b44t.ui.ActionBar.ActionBarMenuItem;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.ActionBar.MenuDrawable;
import com.b44t.ui.Components.PlayerView;
import com.b44t.ui.Components.EmptyTextProgressView;
import com.b44t.ui.Components.LayoutHelper;
import com.b44t.ui.Components.RecyclerListView;
import com.b44t.ui.ActionBar.Theme;

import java.util.ArrayList;

public class DialogsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    
    private RecyclerListView listView;
    private LinearLayoutManager layoutManager;
    private DialogsAdapter dialogsAdapter;
    private DialogsSearchAdapter dialogsSearchAdapter;
    private EmptyTextProgressView searchEmptyView;
    private LinearLayout emptyView;
    private ActionBarMenuItem passcodeItem;
    private ImageView floatingButton;

    private AlertDialog permissionDialog;

    // Floating hiding action
    /*
    private int prevPosition;
    private int prevTop;
    private boolean scrollUpdated;
    */
    // /Floating hiding action

    private boolean floatingHidden;
    private final AccelerateDecelerateInterpolator floatingInterpolator = new AccelerateDecelerateInterpolator();

    private boolean checkPermission = true;

    private String selectAlertString;
    private String selectAlertStringGroup;
    private String addToGroupAlertString;
    private int dialogsType;

    private static boolean dialogsLoaded;
    private boolean searching;
    private boolean searchWas;
    private boolean onlySelect;
    private String searchString;
    private long openedDialogId;

    private DialogsActivityDelegate delegate;

    public interface DialogsActivityDelegate {
        void didSelectDialog(DialogsActivity fragment, long dialog_id, boolean param);
    }

    public DialogsActivity(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        if (getArguments() != null) {
            onlySelect = arguments.getBoolean("onlySelect", false);
            dialogsType = arguments.getInt("dialogsType", 0);
            selectAlertString = arguments.getString("selectAlertString");
            selectAlertStringGroup = arguments.getString("selectAlertStringGroup");
            addToGroupAlertString = arguments.getString("addToGroupAlertString");
        }

        if (searchString == null) {
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.dialogsNeedReload);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.emojiDidLoaded);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.contactsDidLoaded);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.appDidLogout);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.openedChatChanged);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.notificationsSettingsUpdated);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.messageSendError);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.didSetPasscode);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.needReloadRecentDialogsSearch);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.reloadHints);
        }


        if (!dialogsLoaded) {
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload); // this is the rest of the first call to the removed MessagesController.loadDialogs(); not sure, if this is really needed
            dialogsLoaded = true;
        }
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (searchString == null) {
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.dialogsNeedReload);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.emojiDidLoaded);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.contactsDidLoaded);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.appDidLogout);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.openedChatChanged);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.notificationsSettingsUpdated);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messageSendError);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didSetPasscode);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.needReloadRecentDialogsSearch);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.reloadHints);
        }
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

        ActionBarMenu menu = actionBar.createMenu();
        if (!onlySelect && searchString == null) {
            passcodeItem = menu.addItem(1, R.drawable.lock_close);
            updatePasscodeButton();
        }
        final ActionBarMenuItem item = menu.addItem(0, R.drawable.ic_ab_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchExpand() {
                searching = true;
                if (listView != null) {
                    if (searchString != null) {
                        listView.setEmptyView(searchEmptyView);
                        emptyView.setVisibility(View.GONE);
                    }
                    if (!onlySelect) {
                        floatingButton.setVisibility(View.GONE);
                    }
                }
                updatePasscodeButton();
                Toast.makeText(getParentActivity(), LocaleController.getString("NotYetImplemented", R.string.NotYetImplemented), Toast.LENGTH_LONG).show();
            }

            @Override
            public boolean canCollapseSearch() {
                if (searchString != null) {
                    finishFragment();
                    return false;
                }
                return true;
            }

            @Override
            public void onSearchCollapse() {
                searching = false;
                searchWas = false;
                if (listView != null) {
                    searchEmptyView.setVisibility(View.GONE);
                    listView.setEmptyView(emptyView);
                    if (!onlySelect) {
                        floatingButton.setVisibility(View.VISIBLE);
                        floatingHidden = true;
                        floatingButton.setTranslationY(AndroidUtilities.dp(100));
                        hideFloatingButton(false);
                    }
                    if (listView.getAdapter() != dialogsAdapter) {
                        listView.setAdapter(dialogsAdapter);
                        dialogsAdapter.notifyDataSetChanged();
                    }
                }
                if (dialogsSearchAdapter != null) {
                    dialogsSearchAdapter.searchDialogs(null);
                }
                updatePasscodeButton();
            }

            @Override
            public void onTextChanged(EditText editText) {
                String text = editText.getText().toString();
                if (text.length() != 0 || dialogsSearchAdapter != null && dialogsSearchAdapter.hasRecentRearch()) {
                    searchWas = true;
                    if (dialogsSearchAdapter != null && listView.getAdapter() != dialogsSearchAdapter) {
                        listView.setAdapter(dialogsSearchAdapter);
                        dialogsSearchAdapter.notifyDataSetChanged();
                    }
                    if (searchEmptyView != null && listView.getEmptyView() != searchEmptyView) {
                        emptyView.setVisibility(View.GONE);
                        searchEmptyView.showTextView();
                        listView.setEmptyView(searchEmptyView);
                    }
                }
                if (dialogsSearchAdapter != null) {
                    dialogsSearchAdapter.searchDialogs(text);
                }
            }
        });
        item.getSearchField().setHint(LocaleController.getString("Search", R.string.Search));
        if (onlySelect) {
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            actionBar.setTitle(LocaleController.getString("SelectChat", R.string.SelectChat));
        } else {
            if (searchString != null) {
                actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            } else {
                actionBar.setBackButtonDrawable(new MenuDrawable());
            }
            if (BuildVars.DEBUG_VERSION) {
                actionBar.setTitle(LocaleController.getString("AppNameBeta", R.string.AppNameBeta));
            } else {
                actionBar.setTitle(LocaleController.getString("AppName", R.string.AppName));
            }
        }
        actionBar.setAllowOverlayTitle(true);

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    if (onlySelect) {
                        finishFragment();
                    } else if (parentLayout != null) {
                        parentLayout.getDrawerLayoutContainer().openDrawer(false);
                    }
                } else if (id == 1) {
                    UserConfig.appLocked = !UserConfig.appLocked;
                    UserConfig.saveConfig(false);
                    updatePasscodeButton();
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
        listView.setVerticalScrollbarPosition(LocaleController.isRTL ? ListView.SCROLLBAR_POSITION_LEFT : ListView.SCROLLBAR_POSITION_RIGHT);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (listView == null || listView.getAdapter() == null) {
                    return;
                }

                // handle single click
                long dialog_id = 0;
                int message_id = 0;
                RecyclerView.Adapter adapter = listView.getAdapter();
                if (adapter == dialogsAdapter) {
                    TLRPC.TL_dialog dialog = dialogsAdapter.getItem(position);
                    if (dialog == null) {
                        return;
                    }
                    dialog_id = dialog.id;
                } else if (adapter == dialogsSearchAdapter) {
                    Object obj = dialogsSearchAdapter.getItem(position);
                    if (obj instanceof TLRPC.User) {
                        dialog_id = ((TLRPC.User) obj).id;
                        if (dialogsSearchAdapter.isGlobalSearch(position)) {
                            //ArrayList<TLRPC.User> users = new ArrayList<>();
                            //users.add((TLRPC.User) obj);
                            //MessagesController.getInstance().putUsers(users, false);
                            //MessagesStorage.getInstance().putUsersAndChats(users, null, false, true);
                        }
                        if (!onlySelect) {
                            dialogsSearchAdapter.putRecentSearch(dialog_id, (TLRPC.User) obj);
                        }
                    } else if (obj instanceof TLRPC.Chat) {
                        if (dialogsSearchAdapter.isGlobalSearch(position)) {
                            ArrayList<TLRPC.Chat> chats = new ArrayList<>();
                            chats.add((TLRPC.Chat) obj);
                            //MessagesController.getInstance().putChats(chats, false);
                            //MessagesStorage.getInstance().putUsersAndChats(null, chats, false, true);
                        }
                        if (((TLRPC.Chat) obj).id > 0) {
                            dialog_id = -((TLRPC.Chat) obj).id;
                        } /*else {
                            dialog_id = AndroidUtilities.makeBroadcastId(((TLRPC.Chat) obj).id);
                        }*/
                        if (!onlySelect) {
                            dialogsSearchAdapter.putRecentSearch(dialog_id, (TLRPC.Chat) obj);
                        }
                    } else if (obj instanceof MessageObject) {
                        MessageObject messageObject = (MessageObject) obj;
                        dialog_id = messageObject.getDialogId();
                        message_id = messageObject.getId();
                        dialogsSearchAdapter.addHashtagsFromMessage(dialogsSearchAdapter.getLastSearchString());
                    } else if (obj instanceof String) {
                        actionBar.openSearchField((String) obj);
                    }
                }

                if (dialog_id == 0) {
                    return;
                }

                if (onlySelect) {
                    didSelectResult(dialog_id, true, false);
                } else {
                    Bundle args = new Bundle();
                    args.putInt("chat_id", (int)dialog_id);
                    if (message_id != 0) {
                        args.putInt("message_id", message_id);
                    } else {
                        if (actionBar != null) {
                            actionBar.closeSearchField();
                        }
                    }

                    if (AndroidUtilities.isTablet()) {
                        if (openedDialogId == dialog_id && adapter != dialogsSearchAdapter) {
                            return;
                        }
                        if (dialogsAdapter != null) {
                            dialogsAdapter.setOpenedDialogId(openedDialogId = dialog_id);
                            updateVisibleRows(MessagesController.UPDATE_MASK_SELECT_DIALOG);
                        }
                    }
                    if (searchString != null) {
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
                            presentFragment(new ChatActivity(args));
                    } else {
                            presentFragment(new ChatActivity(args));
                    }
                }
            }
        });

        searchEmptyView = new EmptyTextProgressView(context);
        searchEmptyView.setVisibility(View.GONE);
        searchEmptyView.setShowAtCenter(true);
        searchEmptyView.setText(LocaleController.getString("NoResult", R.string.NoResult));
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

        TextView textView = new TextView(context);
        textView.setText(LocaleController.getString("NoChats", R.string.NoChats));
        textView.setTextColor(0xff959595);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        emptyView.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        textView = new TextView(context);
        String help = LocaleController.getString("NoChatsHelp", R.string.NoChatsHelp);
        textView.setText(help);
        textView.setTextColor(0xff959595);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(64) /*move the whole stuff a little bit up*/);
        textView.setLineSpacing(AndroidUtilities.dp(2), 1);
        emptyView.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        floatingButton = new ImageView(context);
        floatingButton.setVisibility(onlySelect ? View.GONE : View.VISIBLE);
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
        frameLayout.addView(floatingButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.BOTTOM, LocaleController.isRTL ? 14 : 0, 0, LocaleController.isRTL ? 0 : 14, 14));
        floatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putInt("do_what", ContactsActivity.SELECT_CONTACT_FOR_NEW_CHAT);
                presentFragment(new ContactsActivity(args));
            }
        });

        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING && searching && searchWas) {
                    AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                //int visibleItemCount = Math.abs(layoutManager.findLastVisibleItemPosition() - firstVisibleItem) + 1;
                //int totalItemCount = recyclerView.getAdapter().getItemCount();

                if (searching && searchWas) {
                    //if (visibleItemCount > 0 && layoutManager.findLastVisibleItemPosition() == totalItemCount - 1 && !dialogsSearchAdapter.isMessagesSearchEndReached()) {
                    //    dialogsSearchAdapter.loadMoreSearchMessages(); // TODO BY MR - implement searching
                    //}
                    return;
                }

                // Floating hiding action - I'm not sure if it is really useful. If there is nothing to scroll the floting button cannot does not move away - and covers always a part of the last row. This is not better than without moving away.
                /*
                if (floatingButton.getVisibility() != View.GONE) {
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
                }
                */
                // /Floating hiding action
            }
        });

        if (searchString == null) {
            dialogsAdapter = new DialogsAdapter(context, dialogsType);
            if (AndroidUtilities.isTablet() && openedDialogId != 0) {
                dialogsAdapter.setOpenedDialogId(openedDialogId);
            }
            listView.setAdapter(dialogsAdapter);
        }
        int type = 0;
        if (searchString != null) {
            type = 2;
        } else if (!onlySelect) {
            type = 1;
        }
        dialogsSearchAdapter = new DialogsSearchAdapter(context, type, dialogsType);
        dialogsSearchAdapter.setDelegate(new DialogsSearchAdapter.DialogsSearchAdapterDelegate() {
            @Override
            public void searchStateChanged(boolean search) {
                if (searching && searchWas && searchEmptyView != null) {
                    if (search) {
                        searchEmptyView.showProgress();
                    } else {
                        searchEmptyView.showTextView();
                    }
                }
            }

            @Override
            public void didPressedOnSubDialog(int did) {
                if (onlySelect) {
                    didSelectResult(did, true, false);
                } else {
                    Bundle args = new Bundle();
                    if (did > 0) {
                        args.putInt("user_id", did);
                    } else {
                        args.putInt("chat_id", -did);
                    }
                    if (actionBar != null) {
                        actionBar.closeSearchField();
                    }
                    if (AndroidUtilities.isTablet()) {
                        if (dialogsAdapter != null) {
                            dialogsAdapter.setOpenedDialogId(openedDialogId = did);
                            updateVisibleRows(MessagesController.UPDATE_MASK_SELECT_DIALOG);
                        }
                    }
                    if (searchString != null) {
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
                            presentFragment(new ChatActivity(args));
                    } else {
                            presentFragment(new ChatActivity(args));
                    }
                }
            }

            @Override
            public void needRemoveHint(final int did) {
                if (getParentActivity() == null) {
                    return;
                }
                TLRPC.User user = MessagesController.getInstance().getUser(did);
                if (user == null) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setMessage(LocaleController.formatString("ChatHintsDelete", R.string.ChatHintsDelete, ContactsController.formatName(user.first_name, user.last_name)));
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //SearchQuery.removePeer(did);
                    }
                });
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                showDialog(builder.create());
            }
        });

        searchEmptyView.setVisibility(View.GONE);
        listView.setEmptyView(emptyView);

        if (searchString != null) {
            actionBar.openSearchField(searchString);
        }

        if (!onlySelect && dialogsType == 0) {
            frameLayout.addView(new PlayerView(context, this), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 39, Gravity.TOP | Gravity.LEFT, 0, -36, 0, 0));
        }

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dialogsAdapter != null) {
            dialogsAdapter.notifyDataSetChanged();
        }
        if (dialogsSearchAdapter != null) {
            dialogsSearchAdapter.notifyDataSetChanged();
        }
        if (checkPermission && !onlySelect && Build.VERSION.SDK_INT >= 23) {
            Activity activity = getParentActivity();
            if (activity != null) {
                checkPermission = false;
                if (activity.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED || activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setMessage(LocaleController.getString("PermissionContacts", R.string.PermissionContacts));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                        showDialog(permissionDialog = builder.create());
                    } else if (activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setMessage(LocaleController.getString("PermissionStorage", R.string.PermissionStorage));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                        showDialog(permissionDialog = builder.create());
                    } else {
                        askForPermissons();
                    }
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void askForPermissons() {
        Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        ArrayList<String> permissons = new ArrayList<>();
        if (activity.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissons.add(Manifest.permission.READ_CONTACTS);
            permissons.add(Manifest.permission.WRITE_CONTACTS);
            permissons.add(Manifest.permission.GET_ACCOUNTS);
        }
        if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissons.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissons.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        String[] items = permissons.toArray(new String[permissons.size()]);
        activity.requestPermissions(items, 1);
    }

    @Override
    protected void onDialogDismiss(Dialog dialog) {
        super.onDialogDismiss(dialog);
        if (permissionDialog != null && dialog == permissionDialog && getParentActivity() != null) {
            askForPermissons();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!onlySelect && floatingButton != null) {
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
        if (requestCode == 1) {
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
            if (dialogsAdapter != null) {
                dialogsAdapter.notifyDataSetChanged();
                /* EDIT BY MR
                if (dialogsAdapter.isDataSetChanged()) {
                    dialogsAdapter.notifyDataSetChanged();
                } else {
                    updateVisibleRows(MessagesController.UPDATE_MASK_NEW_MESSAGE);
                }
                */
            }
            if (dialogsSearchAdapter != null) {
                dialogsSearchAdapter.notifyDataSetChanged();
            }
            if (listView != null) {
                try {
                    /* EDIT BY MR
                    if (MessagesController.getInstance().loadingDialogs && MessagesController.getInstance().dialogs.isEmpty()) {
                        searchEmptyView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.GONE);
                        listView.setEmptyView(progressView);
                    } else
                    */
                    {
                        //progressView.setVisibility(View.GONE); -- EDIT BY MR
                        if (searching && searchWas) {
                            emptyView.setVisibility(View.GONE);
                            listView.setEmptyView(searchEmptyView);
                        } else {
                            searchEmptyView.setVisibility(View.GONE);
                            listView.setEmptyView(emptyView);
                        }
                    }
                } catch (Exception e) {
                    FileLog.e("messenger", e); //TODO fix it in other way?
                }
            }
        } else if (id == NotificationCenter.emojiDidLoaded) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.updateInterfaces) {
            updateVisibleRows((Integer) args[0]);
        } else if (id == NotificationCenter.appDidLogout) {
            dialogsLoaded = false;
        } else if (id == NotificationCenter.contactsDidLoaded) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.openedChatChanged) {
            if (dialogsType == 0 && AndroidUtilities.isTablet()) {
                boolean close = (Boolean) args[1];
                long dialog_id = (Long) args[0];
                if (close) {
                    if (dialog_id == openedDialogId) {
                        openedDialogId = 0;
                    }
                } else {
                    openedDialogId = dialog_id;
                }
                if (dialogsAdapter != null) {
                    dialogsAdapter.setOpenedDialogId(openedDialogId);
                }
                updateVisibleRows(MessagesController.UPDATE_MASK_SELECT_DIALOG);
            }
        } else if (id == NotificationCenter.notificationsSettingsUpdated) {
            updateVisibleRows(0);
        } else if ( id == NotificationCenter.messageSendError) {
            updateVisibleRows(MessagesController.UPDATE_MASK_SEND_STATE);
        } else if (id == NotificationCenter.didSetPasscode) {
            updatePasscodeButton();
        } if (id == NotificationCenter.needReloadRecentDialogsSearch) {
            if (dialogsSearchAdapter != null) {
                dialogsSearchAdapter.loadRecentSearch();
            }
        } else if (id == NotificationCenter.reloadHints) {
            if (dialogsSearchAdapter != null) {
                dialogsSearchAdapter.notifyDataSetChanged();
            }
        }
    }

    private void updatePasscodeButton() {
        if (passcodeItem == null) {
            return;
        }
        if (UserConfig.passcodeHash.length() != 0 && !searching) {
            passcodeItem.setVisibility(View.VISIBLE);
            if (UserConfig.appLocked) {
                passcodeItem.setIcon(R.drawable.lock_close);
            } else {
                passcodeItem.setIcon(R.drawable.lock_open);
            }
        } else {
            passcodeItem.setVisibility(View.GONE);
        }
    }

    private void hideFloatingButton(boolean hide) {
        if (floatingHidden == hide) {
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
            if (child instanceof DialogCell) {
                if (listView.getAdapter() != dialogsSearchAdapter) {
                    DialogCell cell = (DialogCell) child;
                    if ((mask & MessagesController.UPDATE_MASK_NEW_MESSAGE) != 0) {
                        cell.checkCurrentDialogIndex();
                        if (dialogsType == 0 && AndroidUtilities.isTablet()) {
                            cell.setDialogSelected(cell.getDialogId() == openedDialogId);
                        }
                    } else if ((mask & MessagesController.UPDATE_MASK_SELECT_DIALOG) != 0) {
                        if (dialogsType == 0 && AndroidUtilities.isTablet()) {
                            cell.setDialogSelected(cell.getDialogId() == openedDialogId);
                        }
                    } else {
                        cell.update(mask);
                    }
                }
            } else if (child instanceof UserCell) {
                ((UserCell) child).update(mask);
            } else if (child instanceof ProfileSearchCell) {
                ((ProfileSearchCell) child).update(mask);
            } else if (child instanceof RecyclerListView) {
                RecyclerListView innerListView = (RecyclerListView) child;
                int count2 = innerListView.getChildCount();
                for (int b = 0; b < count2; b++) {
                    View child2 = innerListView.getChildAt(b);
                    if (child2 instanceof HintDialogCell) {
                        ((HintDialogCell) child2).checkUnreadCounter(mask);
                    }
                }
            }
        }
    }

    public void setDelegate(DialogsActivityDelegate dialogsActivityDelegate) {
        delegate = dialogsActivityDelegate;
    }

    public void setSearchString(String string) {
        searchString = string;
    }

    public boolean isMainDialogList() {
        return delegate == null && searchString == null;
    }

    private void didSelectResult(final long dialog_id, boolean useAlert, final boolean param) {
        if (useAlert && (selectAlertString != null && selectAlertStringGroup != null || addToGroupAlertString != null)) {
            if (getParentActivity() == null) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            int lower_part = (int) dialog_id;
            int high_id = (int) (dialog_id >> 32);
            if (lower_part != 0) {
                if (high_id == 1) {
                    TLRPC.Chat chat = MessagesController.getInstance().getChat(lower_part);
                    if (chat == null) {
                        return;
                    }
                    builder.setMessage(LocaleController.formatStringSimple(selectAlertStringGroup, chat.title));
                } else {
                    if (lower_part > 0) {
                        TLRPC.User user = MessagesController.getInstance().getUser(lower_part);
                        if (user == null) {
                            return;
                        }
                        builder.setMessage(LocaleController.formatStringSimple(selectAlertString, UserObject.getUserName(user)));
                    } else if (lower_part < 0) {
                        TLRPC.Chat chat = MessagesController.getInstance().getChat(-lower_part);
                        if (chat == null) {
                            return;
                        }
                        if (addToGroupAlertString != null) {
                            builder.setMessage(LocaleController.formatStringSimple(addToGroupAlertString, chat.title));
                        } else {
                            builder.setMessage(LocaleController.formatStringSimple(selectAlertStringGroup, chat.title));
                        }
                    }
                }
            }

            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    didSelectResult(dialog_id, false, false);
                }
            });
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            showDialog(builder.create());
        } else {
            if (delegate != null) {
                delegate.didSelectDialog(DialogsActivity.this, dialog_id, param);
                delegate = null;
            } else {
                finishFragment();
            }
        }
    }
}
