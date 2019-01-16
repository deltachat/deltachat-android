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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcChatlist;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEventCenter;

import org.thoughtcrime.securesms.ConversationListAdapter.ItemClickListener;
import org.thoughtcrime.securesms.components.recyclerview.DeleteItemAnimator;
import org.thoughtcrime.securesms.components.registration.PulsingFloatingActionButton;
import org.thoughtcrime.securesms.components.reminder.Reminder;
import org.thoughtcrime.securesms.components.reminder.ReminderView;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcChatlistLoader;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.task.SnackbarAsyncTask;
import org.thoughtcrime.securesms.util.guava.Optional;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;


public class ConversationListFragment extends Fragment
  implements LoaderManager.LoaderCallbacks<DcChatlist>, ActionMode.Callback, ItemClickListener, DcEventCenter.DcEventDelegate
{
  public static final String ARCHIVE = "archive";

  @SuppressWarnings("unused")
  private static final String TAG = ConversationListFragment.class.getSimpleName();

  private ActionMode                  actionMode;
  private RecyclerView                list;
  private ReminderView                reminderView;
  private View                        emptyState;
  private TextView                    emptySearch;
  private PulsingFloatingActionButton fab;
  private Locale                      locale;
  private String                      queryFilter  = "";
  private boolean                     archive;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    locale  = (Locale) getArguments().getSerializable(PassphraseRequiredActionBarActivity.LOCALE_EXTRA);
    archive = getArguments().getBoolean(ARCHIVE, false);

    ApplicationDcContext dcContext = DcHelper.getContext(getActivity());
    dcContext.eventCenter.addObserver(this, DcContext.DC_EVENT_CHAT_MODIFIED);
    dcContext.eventCenter.addObserver(this, DcContext.DC_EVENT_INCOMING_MSG);
    dcContext.eventCenter.addObserver(this, DcContext.DC_EVENT_MSGS_CHANGED);
    dcContext.eventCenter.addObserver(this, DcContext.DC_EVENT_MSG_DELIVERED);
    dcContext.eventCenter.addObserver(this, DcContext.DC_EVENT_MSG_FAILED);
    dcContext.eventCenter.addObserver(this, DcContext.DC_EVENT_MSG_READ);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    DcHelper.getContext(getActivity()).eventCenter.removeObservers(this);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {
    final View view = inflater.inflate(R.layout.conversation_list_fragment, container, false);

    reminderView = ViewUtil.findById(view, R.id.reminder);
    list         = ViewUtil.findById(view, R.id.list);
    fab          = ViewUtil.findById(view, R.id.fab);
    emptyState   = ViewUtil.findById(view, R.id.empty_state);
    emptySearch  = ViewUtil.findById(view, R.id.empty_search);

    if (archive) fab.setVisibility(View.GONE);
    else         fab.setVisibility(View.VISIBLE);

    reminderView.setOnDismissListener(() -> updateReminders(true));

    list.setHasFixedSize(true);
    list.setLayoutManager(new LinearLayoutManager(getActivity()));
    list.setItemAnimator(new DeleteItemAnimator());

    new ItemTouchHelper(new ArchiveListenerCallback()).attachToRecyclerView(list);

    return view;
  }

  @Override
  public void onActivityCreated(Bundle bundle) {
    super.onActivityCreated(bundle);

    setHasOptionsMenu(true);
    fab.setOnClickListener(v -> startActivity(new Intent(getActivity(), NewConversationActivity.class)));
    initializeListAdapter();
  }

  @Override
  public void onResume() {
    super.onResume();

    updateReminders(true);
    list.getAdapter().notifyDataSetChanged();
  }

  @Override
  public void onPause() {
    super.onPause();

    fab.stopPulse();
  }

  public ConversationListAdapter getListAdapter() {
    return (ConversationListAdapter) list.getAdapter();
  }

  public void setQueryFilter(String query) {
    this.queryFilter = query;
    getLoaderManager().restartLoader(0, null, this);
  }

  public void resetQueryFilter() {
    if (!TextUtils.isEmpty(this.queryFilter)) {
      setQueryFilter("");
    }
  }

  @SuppressLint("StaticFieldLeak")
  private void updateReminders(boolean hide) {
    new AsyncTask<Context, Void, Optional<? extends Reminder>>() {
      @Override
      protected Optional<? extends Reminder> doInBackground(Context... params) {
        final Context context = params[0];
//        if (ExpiredBuildReminder.isEligible()) {
//          return Optional.of(new ExpiredBuildReminder(context));
//        } else if (OutdatedBuildReminder.isEligible()) {
//          return Optional.of(new OutdatedBuildReminder(context));
//        } else if (DozeReminder.isEligible(context)) {
//          return Optional.of(new DozeReminder(context));
//        } else {
          return Optional.absent();
//        }
      }

      @Override
      protected void onPostExecute(Optional<? extends Reminder> reminder) {
        if (reminder.isPresent() && getActivity() != null && !isRemoving()) {
          reminderView.showReminder(reminder.get());
        } else if (!reminder.isPresent()) {
          reminderView.hide();
        }
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getActivity());
  }

  private void initializeListAdapter() {
    list.setAdapter(new ConversationListAdapter(getActivity(), GlideApp.with(this), locale, null, this));
    getLoaderManager().restartLoader(0, null, this);
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
        for (long threadId : selectedConversations) {
          dcContext.archiveChat((int)threadId, !archive? 1 : 0);
        }
      }

      @Override
      protected void reverseAction(@Nullable Void parameter) {
        for (long threadId : selectedConversations) {
          dcContext.archiveChat((int)threadId, !archive? 0 : 1);
        }
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  @SuppressLint("StaticFieldLeak")
  private void handleDeleteAllSelected() {
    final DcContext     dcContext          = DcHelper.getContext(getActivity());
    int                 conversationsCount = getListAdapter().getBatchSelections().size();
    AlertDialog.Builder alert              = new AlertDialog.Builder(getActivity());
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
            for (long threadId : selectedConversations) {
              dcContext.deleteChat((int)threadId);
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

  private void handleCreateConversation(int threadId, long lastSeen) {
    ((ConversationSelectedListener)getActivity()).onCreateConversation(threadId, lastSeen);
  }

  @Override
  public Loader<DcChatlist> onCreateLoader(int arg0, Bundle arg1) {
    int listflags = 0;
    if(archive) {
      listflags |= DcContext.DC_GCL_ARCHIVED_ONLY;
    }
    else {
      listflags |= DcContext.DC_GCL_ADD_ALLDONE_HINT;
    }
    return new DcChatlistLoader(getActivity(), listflags, queryFilter.isEmpty()? null : queryFilter, 0);
  }

  @Override
  public void onLoadFinished(Loader<DcChatlist> arg0, DcChatlist chatlist) {
    if (chatlist.getCnt() <= 0 && TextUtils.isEmpty(queryFilter) && !archive) {
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
  }

  @Override
  public void onLoaderReset(Loader<DcChatlist> arg0) {
    getListAdapter().changeData(null);
  }

  @Override
  public void onItemClick(ConversationListItem item) {
    if (actionMode == null) {
      int threadId = (int)item.getThreadId();

      if (threadId==DcChat.DC_CHAT_ID_DEADDROP) {
        DcContext dcContext = DcHelper.getContext(getActivity());
        int msgId = item.getMsgId();
        int contactId = item.getContactId();
        DcContact contact = dcContext.getContact(contactId);
        new AlertDialog.Builder(getActivity())
          .setMessage(getActivity().getString(R.string.ask_start_chat_with, contact.getNameNAddr()))
          .setPositiveButton(android.R.string.ok, (dialog, which) ->  {
              int belongingChatId = dcContext.createChatByMsgId(msgId);
              if( belongingChatId != 0 ) {
                handleCreateConversation(belongingChatId, 0);
              }
          })
          .setNegativeButton(R.string.not_now, null)
          .setNeutralButton(R.string.never, (dialog, which) -> {
            dcContext.blockContact(contactId, 1);
          })
          .show();
        return;
      }

      handleCreateConversation(threadId, 0);
    } else {
      ConversationListAdapter adapter = (ConversationListAdapter)list.getAdapter();
      adapter.toggleThreadInBatchSet(item.getThreadId());

      if (adapter.getBatchSelections().size() == 0) {
        actionMode.finish();
      } else {
        actionMode.setTitle(String.valueOf(getListAdapter().getBatchSelections().size()));
      }

      adapter.notifyDataSetChanged();
    }
  }

  @Override
  public void onItemLongClick(ConversationListItem item) {
    actionMode = ((AppCompatActivity)getActivity()).startSupportActionMode(ConversationListFragment.this);

    getListAdapter().initializeBatchMode(true);
    getListAdapter().toggleThreadInBatchSet(item.getThreadId());
    getListAdapter().notifyDataSetChanged();
  }

  @Override
  public void onSwitchToArchive() {
    ((ConversationSelectedListener)getActivity()).onSwitchToArchive();
  }

  public interface ConversationSelectedListener {
    void onCreateConversation(int threadId, long lastSeen);
    void onSwitchToArchive();
}

  @Override
  public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    MenuInflater inflater = getActivity().getMenuInflater();

    if (archive) inflater.inflate(R.menu.conversation_list_batch_unarchive, menu);
    else         inflater.inflate(R.menu.conversation_list_batch_archive, menu);

    inflater.inflate(R.menu.conversation_list, menu);

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

    actionMode = null;
  }

  private class ArchiveListenerCallback extends ItemTouchHelper.SimpleCallback {

    ArchiveListenerCallback() {
      super(0, ItemTouchHelper.RIGHT);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView,
                          RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target)
    {
      return false;
    }

    @Override
    public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
      if (viewHolder.itemView instanceof ConversationListItemAction) {
        return 0;
      }

      if (actionMode != null) {
        return 0;
      }

      return super.getSwipeDirs(recyclerView, viewHolder);
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
      if (viewHolder.itemView instanceof ConversationListItemInboxZero) return;
      final long threadId    = ((ConversationListItem)viewHolder.itemView).getThreadId();
      final DcContext dcContext = DcHelper.getContext(getActivity());

      if (archive) {
        new SnackbarAsyncTask<Long>(getView(),
                                    getResources().getQuantityString(R.plurals.chat_unarchived, 1, 1),
                                    getString(R.string.undo),
                                    Snackbar.LENGTH_LONG, false)
        {
          @Override
          protected void executeAction(@Nullable Long parameter) {
            dcContext.archiveChat((int)threadId, 0);
          }

          @Override
          protected void reverseAction(@Nullable Long parameter) {
            dcContext.archiveChat((int)threadId, 1);
          }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, threadId);
      } else {
        if (threadId==DcChat.DC_CHAT_ID_DEADDROP) {
          int contactId = ((ConversationListItem)viewHolder.itemView).getContactId();
          dcContext.marknoticedContact(contactId);
          return;
        }

        new SnackbarAsyncTask<Long>(getView(),
                                    getResources().getQuantityString(R.plurals.chat_archived, 1, 1),
                                    getString(R.string.undo),
                                    Snackbar.LENGTH_LONG, false)
        {
          @Override
          protected void executeAction(@Nullable Long parameter) {
            dcContext.archiveChat((int)threadId, 1);
          }

          @Override
          protected void reverseAction(@Nullable Long parameter) {
            dcContext.archiveChat((int)threadId, 0);
          }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, threadId);
      }
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState,
                            boolean isCurrentlyActive)
    {
      if (viewHolder.itemView instanceof ConversationListItemInboxZero) return;
      if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
        View  itemView = viewHolder.itemView;
        Paint p        = new Paint();
        float alpha    = 1.0f - Math.abs(dX) / (float) viewHolder.itemView.getWidth();

        if (dX > 0) {
          Bitmap icon;

          if (archive) icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_unarchive_white_36dp);
          else         icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_archive_white_36dp);

          if (alpha > 0) p.setColor(getResources().getColor(R.color.signal_primary));
          else           p.setColor(Color.WHITE);

          c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX,
                     (float) itemView.getBottom(), p);

          c.drawBitmap(icon,
                       (float) itemView.getLeft() + getResources().getDimension(R.dimen.conversation_list_fragment_archive_padding),
                       (float) itemView.getTop() + ((float) itemView.getBottom() - (float) itemView.getTop() - icon.getHeight())/2,
                       p);
        }

        viewHolder.itemView.setAlpha(alpha);
        viewHolder.itemView.setTranslationX(dX);
      } else {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
      }
    }
  }

  @Override
  public void handleEvent(int eventId, Object data1, Object data2) {
    getLoaderManager().restartLoader(0,null,this);
  }
}


