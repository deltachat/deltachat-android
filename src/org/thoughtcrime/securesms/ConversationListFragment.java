/*
 * Copyright (C) 2015 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcChatlist;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.google.android.material.snackbar.Snackbar;

import org.thoughtcrime.securesms.ConversationListAdapter.ItemClickListener;
import org.thoughtcrime.securesms.components.recyclerview.DeleteItemAnimator;
import org.thoughtcrime.securesms.components.registration.PulsingFloatingActionButton;
import org.thoughtcrime.securesms.components.reminder.DozeReminder;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.RelayUtil;
import org.thoughtcrime.securesms.util.SendRelayedMessageUtil;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.task.SnackbarAsyncTask;
import org.thoughtcrime.securesms.util.views.ProgressDialog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static org.thoughtcrime.securesms.util.RelayUtil.REQUEST_RELAY;
import static org.thoughtcrime.securesms.util.RelayUtil.acquireRelayMessageContent;
import static org.thoughtcrime.securesms.util.RelayUtil.getSharedText;
import static org.thoughtcrime.securesms.util.RelayUtil.getSharedUris;
import static org.thoughtcrime.securesms.util.RelayUtil.isForwarding;
import static org.thoughtcrime.securesms.util.RelayUtil.isRelayingMessageContent;


public class ConversationListFragment extends Fragment
        implements ActionMode.Callback, ItemClickListener, DcEventCenter.DcEventDelegate {
  public static final String ARCHIVE = "archive";
  public static final String RELOAD_LIST = "reload_list";

  @SuppressWarnings("unused")
  private static final String TAG = ConversationListFragment.class.getSimpleName();

  private ActionMode                  actionMode;
  private RecyclerView                list;
  private View                        emptyState;
  private TextView                    emptySearch;
  private PulsingFloatingActionButton fab;
  private Locale                      locale;
  private String                      queryFilter  = "";
  private boolean                     archive;
  private Timer                       reloadTimer;
  private boolean                     chatlistJustLoaded;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    locale = (Locale) getArguments().getSerializable(PassphraseRequiredActionBarActivity.LOCALE_EXTRA);
    archive = getArguments().getBoolean(ARCHIVE, false);

    DcEventCenter eventCenter = DcHelper.getEventCenter(getActivity());
    eventCenter.addObserver(DcContext.DC_EVENT_CHAT_MODIFIED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_CONTACTS_CHANGED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_INCOMING_MSG, this);
    eventCenter.addObserver(DcContext.DC_EVENT_MSGS_CHANGED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_MSGS_NOTICED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_MSG_DELIVERED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_MSG_FAILED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_MSG_READ, this);
    eventCenter.addObserver(DcContext.DC_EVENT_MSG_READ, this);
    eventCenter.addObserver(DcContext.DC_EVENT_CONNECTIVITY_CHANGED, this);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    DcHelper.getEventCenter(getActivity()).removeObservers(this);
  }

  @SuppressLint("RestrictedApi")
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {
    final View view = inflater.inflate(R.layout.conversation_list_fragment, container, false);

    list         = ViewUtil.findById(view, R.id.list);
    fab          = ViewUtil.findById(view, R.id.fab);
    emptyState   = ViewUtil.findById(view, R.id.empty_state);
    emptySearch  = ViewUtil.findById(view, R.id.empty_search);

    if (archive) {
      fab.setVisibility(View.GONE);
      TextView emptyTitle = ViewUtil.findById(view, R.id.empty_title);
      TextView emptySubtitle = ViewUtil.findById(view, R.id.empty_subtitle);
      emptyTitle.setText(R.string.archive_empty_hint);
      emptySubtitle.setVisibility(View.GONE);
    } else {
      fab.setVisibility(View.VISIBLE);
    }

    list.setHasFixedSize(true);
    list.setLayoutManager(new LinearLayoutManager(getActivity()));
    list.setItemAnimator(new DeleteItemAnimator());

    return view;
  }

  @Override
  public void onActivityCreated(Bundle bundle) {
    super.onActivityCreated(bundle);

    setHasOptionsMenu(true);
    initializeFabClickListener(false);
    list.setAdapter(new ConversationListAdapter(getActivity(), GlideApp.with(this), locale, this));
    loadChatlistAsync();
    chatlistJustLoaded = true;
  }

  @Override
  public void onResume() {
    super.onResume();

    updateReminders();

    if (getActivity().getIntent().getIntExtra(RELOAD_LIST, 0) == 1
        && !chatlistJustLoaded) {
      loadChatlist();
    }
    chatlistJustLoaded = false;

    reloadTimer = new Timer();
    reloadTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        Util.runOnMain(() -> { list.getAdapter().notifyDataSetChanged(); });
      }
    }, 60 * 1000, 60 * 1000);
  }

  @Override
  public void onPause() {
    super.onPause();
    reloadTimer.cancel();
    fab.stopPulse();
  }

  public void onNewIntent() {
    initializeFabClickListener(actionMode != null);
  }

  public ConversationListAdapter getListAdapter() {
    return (ConversationListAdapter) list.getAdapter();
  }

  private void initializeFabClickListener(boolean isActionMode) {
    Intent intent = new Intent(getActivity(), NewConversationActivity.class);
    if (isRelayingMessageContent(getActivity())) {
      if (isActionMode) {
        fab.setOnClickListener(v -> {
          final Set<Long> selectedChats = getListAdapter().getBatchSelections();
          ArrayList<Uri> uris = getSharedUris(getActivity());
          String message;
          if (isForwarding(getActivity())) {
            message = String.format(Locale.getDefault(), getString(R.string.ask_forward_multiple), selectedChats.size());
          } else if (uris.size() > 0) {
            message = String.format(Locale.getDefault(), getString(R.string.share_multiple_attachments_multiple_chats), uris.size(), selectedChats.size());
          } else {
            message = String.format(Locale.getDefault(), getString(R.string.share_text_multiple_chats), selectedChats.size(), getSharedText(getActivity()));
          }
          Context context = getContext();
          if (context != null) {
            new AlertDialog.Builder(context)
                    .setMessage(message)
                    .setCancelable(false)
                    .setNegativeButton(android.R.string.cancel, ((dialog, which) -> {}))
                    .setPositiveButton(R.string.menu_send, (dialog, which) -> {
                      SendRelayedMessageUtil.immediatelyRelay(getActivity(), selectedChats.toArray(new Long[selectedChats.size()]));
                      actionMode.finish();
                      actionMode = null;
                      getActivity().finish();
                    })
                    .show();
          }
        });
      } else {
        acquireRelayMessageContent(getActivity(), intent);
        fab.setOnClickListener(v -> getActivity().startActivityForResult(intent, REQUEST_RELAY));
      }
    } else {
      fab.setOnClickListener(v -> startActivity(intent));
    }
  }

  @SuppressLint({"StaticFieldLeak", "NewApi"})
  private void updateReminders() {
    new AsyncTask<Context, Void, Void>() {
      @Override
      protected Void doInBackground(Context... params) {
        final Context context = params[0];
        try {
          if (DozeReminder.isEligible(context)) {
            DozeReminder.addDozeReminderDeviceMsg(context);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
        return null;
      }

      @Override
      protected void onPostExecute(Void result) {
        DozeReminder.maybeAskDirectly(getActivity());
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getActivity());
  }

  private void handlePinAllSelected() {
    final DcContext dcContext             = DcHelper.getContext(getActivity());
    final Set<Long> selectedConversations = new HashSet<>(getListAdapter().getBatchSelections());
    boolean doPin = areSomeSelectedChatsUnpinned();
    for (long chatId : selectedConversations) {
      dcContext.setChatVisibility((int)chatId,
              doPin? DcChat.DC_CHAT_VISIBILITY_PINNED : DcChat.DC_CHAT_VISIBILITY_NORMAL);
    }
    if (actionMode != null) {
      actionMode.finish();
      actionMode = null;
    }
  }

  @SuppressLint("StaticFieldLeak")
  private void handleArchiveAllSelected() {
    final DcContext dcContext             = DcHelper.getContext(getActivity());
    final Set<Long> selectedConversations = new HashSet<>(getListAdapter().getBatchSelections());
    final boolean   archive               = this.archive;

    int snackBarTitleId;

    if (archive) snackBarTitleId = R.plurals.chat_unarchived;
    else         snackBarTitleId = R.plurals.chat_archived;

    int count            = selectedConversations.size();
    String snackBarTitle = getResources().getQuantityString(snackBarTitleId, count, count);

    new SnackbarAsyncTask<Void>(getView(), snackBarTitle,
                                getString(R.string.undo),
                                Snackbar.LENGTH_LONG, true)
    {

      @Override
      protected void onPostExecute(Void result) {
        super.onPostExecute(result);

        if (actionMode != null) {
          actionMode.finish();
          actionMode = null;
        }
      }

      @Override
      protected void executeAction(@Nullable Void parameter) {
        for (long chatId : selectedConversations) {
          dcContext.setChatVisibility((int)chatId,
                  !archive? DcChat.DC_CHAT_VISIBILITY_ARCHIVED : DcChat.DC_CHAT_VISIBILITY_NORMAL);
        }
      }

      @Override
      protected void reverseAction(@Nullable Void parameter) {
        for (long threadId : selectedConversations) {
          dcContext.setChatVisibility((int)threadId,
                  !archive? DcChat.DC_CHAT_VISIBILITY_NORMAL : DcChat.DC_CHAT_VISIBILITY_ARCHIVED);
        }
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  @SuppressLint("StaticFieldLeak")
  private void handleDeleteAllSelected() {
    final DcContext            dcContext          = DcHelper.getContext(getActivity());
    int                        conversationsCount = getListAdapter().getBatchSelections().size();
    AlertDialog.Builder        alert              = new AlertDialog.Builder(getActivity());
    alert.setMessage(getActivity().getResources().getQuantityString(R.plurals.ask_delete_chat,
                                                                    conversationsCount, conversationsCount));
    alert.setCancelable(true);

    alert.setPositiveButton(R.string.delete, (dialog, which) -> {
      final Set<Long> selectedConversations = (getListAdapter())
          .getBatchSelections();

      if (!selectedConversations.isEmpty()) {
        new AsyncTask<Void, Void, Void>() {
          private ProgressDialog dialog;

          @Override
          protected void onPreExecute() {
            dialog = ProgressDialog.show(getActivity(),
                "",
                getActivity().getString(R.string.one_moment),
                true, false);
          }

          @Override
          protected Void doInBackground(Void... params) {
            for (long chatId : selectedConversations) {
              DcHelper.getNotificationCenter(getContext()).removeNotifications((int) chatId);
              dcContext.deleteChat((int) chatId);
            }
            return null;
          }

          @Override
          protected void onPostExecute(Void result) {
            dialog.dismiss();
            if (actionMode != null) {
              actionMode.finish();
              actionMode = null;
            }
          }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      }
    });

    alert.setNegativeButton(android.R.string.cancel, null);
    alert.show();
  }

  private void handleSelectAllThreads() {
    getListAdapter().selectAllThreads();
    actionMode.setTitle(String.valueOf(getListAdapter().getBatchSelections().size()));
  }

  private void handleCreateConversation(int chatId) {
    ((ConversationSelectedListener)getActivity()).onCreateConversation(chatId);
  }

  private final Object loadChatlistLock = new Object();
  private boolean inLoadChatlist;
  private boolean needsAnotherLoad;
  private void loadChatlistAsync() {
    synchronized (loadChatlistLock) {
      needsAnotherLoad = true;
      if (inLoadChatlist) {
        Log.i(TAG, "chatlist loading debounced");
        return;
      }
      inLoadChatlist = true;
    }

    Util.runOnAnyBackgroundThread(() -> {
      while(true) {
        synchronized (loadChatlistLock) {
          if (!needsAnotherLoad) {
            inLoadChatlist = false;
            return;
          }
          needsAnotherLoad = false;
        }

        Log.i(TAG, "executing debounced chatlist loading");
        loadChatlist();
        Util.sleep(100);
      }
    });
  }

  private void loadChatlist() {
    int listflags = 0;
    if (archive) {
      listflags |= DcContext.DC_GCL_ARCHIVED_ONLY;
    } else if (RelayUtil.isRelayingMessageContent(getActivity())) {
      listflags |= DcContext.DC_GCL_FOR_FORWARDING;
    } else {
      listflags |= DcContext.DC_GCL_ADD_ALLDONE_HINT;
    }

    Context context = getContext();
    if (context == null) {
      // can't load chat list at this time, see: https://github.com/deltachat/deltachat-android/issues/2012
      Log.w(TAG, "Ignoring call to loadChatlist()");
      return;
    }
    DcChatlist chatlist = DcHelper.getContext(context).getChatlist(listflags, queryFilter.isEmpty() ? null : queryFilter, 0);

    Util.runOnMain(() -> {
      if (chatlist.getCnt() <= 0 && TextUtils.isEmpty(queryFilter)) {
        list.setVisibility(View.INVISIBLE);
        emptyState.setVisibility(View.VISIBLE);
        emptySearch.setVisibility(View.INVISIBLE);
        fab.startPulse(3 * 1000);
      } else if (chatlist.getCnt() <= 0 && !TextUtils.isEmpty(queryFilter)) {
        list.setVisibility(View.INVISIBLE);
        emptyState.setVisibility(View.GONE);
        emptySearch.setVisibility(View.VISIBLE);
        emptySearch.setText(getString(R.string.search_no_result_for_x, queryFilter));
      } else {
        list.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        emptySearch.setVisibility(View.INVISIBLE);
        fab.stopPulse();
      }

      getListAdapter().changeData(chatlist);
    });
  }

  @Override
  public void onItemClick(ConversationListItem item) {
    if (actionMode == null) {
      int chatId = (int)item.getChatId();
      handleCreateConversation(chatId);
    } else {
      ConversationListAdapter adapter = (ConversationListAdapter) list.getAdapter();
      adapter.toggleThreadInBatchSet(item.getChatId());

      if (adapter.getBatchSelections().size() == 0) {
        actionMode.finish();
      } else {
        updateActionModeItems(actionMode.getMenu());
        actionMode.setTitle(String.valueOf(getListAdapter().getBatchSelections().size()));
      }

      adapter.notifyDataSetChanged();
    }
  }

  @Override
  public void onItemLongClick(ConversationListItem item) {
    actionMode = ((AppCompatActivity)getActivity()).startSupportActionMode(ConversationListFragment.this);

    if (actionMode != null) {
      getListAdapter().initializeBatchMode(true);
      getListAdapter().toggleThreadInBatchSet(item.getChatId());
      getListAdapter().notifyDataSetChanged();
      Menu menu = actionMode.getMenu();
      if (menu != null) {
        updateActionModeItems(menu);
      }
    }
  }

  @Override
  public void onSwitchToArchive() {
    ((ConversationSelectedListener)getActivity()).onSwitchToArchive();
  }

  public interface ConversationSelectedListener {
    void onCreateConversation(int chatId);
    void onSwitchToArchive();
  }

  private boolean areSomeSelectedChatsUnpinned() {
    DcContext dcContext = DcHelper.getContext(getActivity());
    final Set<Long> selectedChats = getListAdapter().getBatchSelections();
    for (long chatId : selectedChats) {
      DcChat dcChat = dcContext.getChat((int)chatId);
      if (dcChat.getVisibility()!=DcChat.DC_CHAT_VISIBILITY_PINNED) {
        return true;
      }
    }
    return false;
  }

  private void updateActionModeItems(Menu menu) {
    // We do not show action mode icons when relaying (= sharing or forwarding).
    if (!isRelayingMessageContent(getActivity())) {
      MenuItem pinItem = menu.findItem(R.id.menu_pin_selected);
      if (areSomeSelectedChatsUnpinned()) {
        pinItem.setIcon(R.drawable.ic_pin_white);
        pinItem.setTitle(R.string.pin_chat);
      } else {
        pinItem.setIcon(R.drawable.ic_unpin_white);
        pinItem.setTitle(R.string.unpin_chat);
      }
    }
  }

  @Override
  public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    if (isRelayingMessageContent(getActivity())) {
      Context context = getContext();
      if (context != null) {
        fab.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_send_sms_white_24dp));
      }
      fab.setVisibility(View.VISIBLE);
      initializeFabClickListener(true);
    } else {

      MenuInflater inflater = getActivity().getMenuInflater();

      inflater.inflate(R.menu.conversation_list_batch_pin, menu);

      if (archive) inflater.inflate(R.menu.conversation_list_batch_unarchive, menu);
      else inflater.inflate(R.menu.conversation_list_batch_archive, menu);

      inflater.inflate(R.menu.conversation_list, menu);
    }

    mode.setTitle("1");

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getActivity().getWindow().setStatusBarColor(getResources().getColor(R.color.action_mode_status_bar));
    }

    return true;
  }

  @Override
  public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
    return false;
  }

  @Override
  public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    switch (item.getItemId()) {
    case R.id.menu_select_all:       handleSelectAllThreads();   return true;
    case R.id.menu_delete_selected:  handleDeleteAllSelected();  return true;
    case R.id.menu_pin_selected:     handlePinAllSelected();     return true;
    case R.id.menu_archive_selected: handleArchiveAllSelected(); return true;
    }

    return false;
  }

  @Override
  public void onDestroyActionMode(ActionMode mode) {
    getListAdapter().initializeBatchMode(false);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      TypedArray color = getActivity().getTheme().obtainStyledAttributes(new int[] {android.R.attr.statusBarColor});
      getActivity().getWindow().setStatusBarColor(color.getColor(0, Color.BLACK));
      color.recycle();
    }

    Context context = getContext();
    if (context != null) {
      fab.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_add_white_24dp));
    }
    if (archive) fab.setVisibility(View.GONE);
    else         fab.setVisibility(View.VISIBLE);
    initializeFabClickListener(false);

    actionMode = null;
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    if (event.getId() == DcContext.DC_EVENT_CONNECTIVITY_CHANGED) {
      ((ConversationListActivity) getActivity()).refreshTitle();
    } else {
      loadChatlistAsync();
    }
  }

}


