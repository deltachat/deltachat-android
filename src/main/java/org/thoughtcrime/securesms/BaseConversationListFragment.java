package org.thoughtcrime.securesms;

import static org.thoughtcrime.securesms.util.ShareUtil.acquireRelayMessageContent;
import static org.thoughtcrime.securesms.util.ShareUtil.getSharedText;
import static org.thoughtcrime.securesms.util.ShareUtil.getSharedUris;
import static org.thoughtcrime.securesms.util.ShareUtil.isForwarding;
import static org.thoughtcrime.securesms.util.ShareUtil.isRelayingMessageContent;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.Fragment;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
import com.google.android.material.snackbar.Snackbar;

import org.thoughtcrime.securesms.components.registration.PulsingFloatingActionButton;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.connect.DirectShareUtil;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.ShareUtil;
import org.thoughtcrime.securesms.util.SendRelayedMessageUtil;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.task.SnackbarAsyncTask;
import org.thoughtcrime.securesms.util.views.ProgressDialog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public abstract class BaseConversationListFragment extends Fragment implements ActionMode.Callback {
  protected ActionMode actionMode;
  protected PulsingFloatingActionButton fab;

  protected abstract boolean offerToArchive();
  protected abstract void setFabVisibility(boolean isActionMode);
  protected abstract BaseConversationListAdapter getListAdapter();

  protected void onItemClick(long chatId) {
    if (actionMode == null) {
      ((ConversationSelectedListener) requireActivity()).onCreateConversation((int)chatId);
    } else {
      BaseConversationListAdapter adapter = getListAdapter();
      adapter.toggleThreadInBatchSet(chatId);

      if (adapter.getBatchSelections().isEmpty()) {
        actionMode.finish();
      } else {
        updateActionModeItems(actionMode.getMenu());
        actionMode.setTitle(String.valueOf(adapter.getBatchSelections().size()));
      }

      adapter.notifyDataSetChanged();
    }
  }
  public void onItemLongClick(long chatId) {
    actionMode = ((AppCompatActivity)requireActivity()).startSupportActionMode(this);

    if (actionMode != null) {
      getListAdapter().initializeBatchMode(true);
      getListAdapter().toggleThreadInBatchSet(chatId);
      getListAdapter().notifyDataSetChanged();
      Menu menu = actionMode.getMenu();
      if (menu != null) {
        updateActionModeItems(menu);
      }
    }
  }

  protected void initializeFabClickListener(boolean isActionMode) {
    Intent intent = new Intent(getActivity(), NewConversationActivity.class);
    if (isRelayingMessageContent(getActivity())) {
      if (isActionMode) {
        fab.setOnClickListener(v -> {
          final Set<Long> selectedChats = getListAdapter().getBatchSelections();
          ArrayList<Uri> uris = getSharedUris(getActivity());
          String message;
          if (isForwarding(getActivity())) {
            message = String.format(Util.getLocale(), getString(R.string.ask_forward_multiple), selectedChats.size());
          } else if (!uris.isEmpty()) {
            message = String.format(Util.getLocale(), getString(R.string.ask_send_files_to_selected_chats), uris.size(), selectedChats.size());
          } else {
            message = String.format(Util.getLocale(), getString(R.string.share_text_multiple_chats), selectedChats.size(), getSharedText(getActivity()));
          }

          Context context = getContext();
          if (context != null) {
            if (SendRelayedMessageUtil.containsVideoType(context, uris)) {
              message += "\n\n" + getString(R.string.videos_sent_without_recoding);
            }
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
        fab.setOnClickListener(v -> requireActivity().startActivity(intent));
      }
    } else {
      fab.setOnClickListener(v -> startActivity(intent));
    }
  }

  private boolean areSomeSelectedChatsUnpinned() {
    DcContext dcContext = DcHelper.getContext(requireActivity());
    final Set<Long> selectedChats = getListAdapter().getBatchSelections();
    for (long chatId : selectedChats) {
      DcChat dcChat = dcContext.getChat((int)chatId);
      if (dcChat.getVisibility()!=DcChat.DC_CHAT_VISIBILITY_PINNED) {
        return true;
      }
    }
    return false;
  }

  private boolean areSomeSelectedChatsUnmuted() {
    DcContext dcContext = DcHelper.getContext(requireActivity());
    final Set<Long> selectedChats = getListAdapter().getBatchSelections();
    for (long chatId : selectedChats) {
      DcChat dcChat = dcContext.getChat((int)chatId);
      if (!dcChat.isMuted()) {
        return true;
      }
    }
    return false;
  }

  private void handlePinAllSelected() {
    final DcContext dcContext             = DcHelper.getContext(requireActivity());
    final Set<Long> selectedConversations = new HashSet<Long>(getListAdapter().getBatchSelections());
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

  private void handleMuteAllSelected() {
    final DcContext dcContext             = DcHelper.getContext(requireActivity());
    final Set<Long> selectedConversations = new HashSet<Long>(getListAdapter().getBatchSelections());
    if (areSomeSelectedChatsUnmuted()) {
      MuteDialog.show(getActivity(), duration -> {
          for (long chatId : selectedConversations) {
            dcContext.setChatMuteDuration((int)chatId, duration);
          }

          if (actionMode != null) {
            actionMode.finish();
            actionMode = null;
          }
      });
    } else {
      // unmute
      for (long chatId : selectedConversations) {
        dcContext.setChatMuteDuration((int)chatId, 0);
      }

      if (actionMode != null) {
        actionMode.finish();
        actionMode = null;
      }
    }
  }

  private void handleMarknoticedSelected() {
    final DcContext dcContext             = DcHelper.getContext(requireActivity());
    final Set<Long> selectedConversations = new HashSet<Long>(getListAdapter().getBatchSelections());
    for (long chatId : selectedConversations) {
      dcContext.marknoticedChat((int)chatId);
    }
    if (actionMode != null) {
      actionMode.finish();
      actionMode = null;
    }
  }

  @SuppressLint("StaticFieldLeak")
  private void handleArchiveAllSelected() {
    final DcContext dcContext             = DcHelper.getContext(requireActivity());
    final Set<Long> selectedConversations = new HashSet<Long>(getListAdapter().getBatchSelections());
    final boolean   archive               = offerToArchive();

    int snackBarTitleId;

    if (archive) snackBarTitleId = R.plurals.chat_archived;
    else         snackBarTitleId = R.plurals.chat_unarchived;

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
                  archive? DcChat.DC_CHAT_VISIBILITY_ARCHIVED : DcChat.DC_CHAT_VISIBILITY_NORMAL);
        }
      }

      @Override
      protected void reverseAction(@Nullable Void parameter) {
        for (long threadId : selectedConversations) {
          dcContext.setChatVisibility((int)threadId,
                  archive? DcChat.DC_CHAT_VISIBILITY_NORMAL : DcChat.DC_CHAT_VISIBILITY_ARCHIVED);
        }
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  @SuppressLint("StaticFieldLeak")
  private void handleDeleteAllSelected() {
    final Activity activity = requireActivity();
    final DcContext dcContext = DcHelper.getContext(activity);
    final Set<Long> selectedChats = getListAdapter().getBatchSelections();

    final int chatsCount = selectedChats.size();
    final String alertText;
    if (chatsCount == 1) {
      long chatId = selectedChats.iterator().next();
      alertText = activity.getResources().getString(R.string.ask_delete_named_chat, dcContext.getChat((int)chatId).getName());
    } else {
      alertText = activity.getResources().getQuantityString(R.plurals.ask_delete_chat, chatsCount, chatsCount);
    }

    AlertDialog.Builder alert = new AlertDialog.Builder(activity);
    alert.setMessage(alertText);
    alert.setCancelable(true);

    alert.setPositiveButton(R.string.delete, (dialog, which) -> {

      if (!selectedChats.isEmpty()) {
        new AsyncTask<Void, Void, Void>() {
          private ProgressDialog dialog;

          @Override
          protected void onPreExecute() {
            dialog = ProgressDialog.show(getActivity(),
                "",
                requireActivity().getString(R.string.one_moment),
                true, false);
          }

          @Override
          protected Void doInBackground(Void... params) {
            int accountId = dcContext.getAccountId();
            for (long chatId : selectedChats) {
              DcHelper.getNotificationCenter(requireContext()).removeNotifications(accountId, (int) chatId);
              dcContext.deleteChat((int) chatId);
              DirectShareUtil.clearShortcut(requireContext(), (int) chatId);
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
    AlertDialog dialog = alert.show();
    Util.redPositiveButton(dialog);
  }

  private void handleSelectAllThreads() {
    getListAdapter().selectAllThreads();
    actionMode.setTitle(String.valueOf(getListAdapter().getBatchSelections().size()));
    updateActionModeItems(actionMode.getMenu());
  }

  private void handleAddToHomeScreen() {
    final Activity activity = requireActivity();
    final DcContext dcContext = DcHelper.getContext(activity);
    final Set<Long> selectedChats = getListAdapter().getBatchSelections();
    final DcChat chat = dcContext.getChat(selectedChats.iterator().next().intValue());

    Intent intent = new Intent(activity, ShareActivity.class);
    intent.setAction(Intent.ACTION_SEND);
    intent.putExtra(ShareActivity.EXTRA_ACC_ID, dcContext.getAccountId());
    intent.putExtra(ShareActivity.EXTRA_CHAT_ID, chat.getId());

    Recipient recipient = new Recipient(activity, chat);
    Util.runOnAnyBackgroundThread(() -> {
      Bitmap avatar = DirectShareUtil.getIconForShortcut(activity, recipient);
      ShortcutInfoCompat shortcutInfoCompat = new ShortcutInfoCompat.Builder(activity, "chat-" + dcContext.getAccountId() + "-" + chat.getId())
        .setShortLabel(chat.getName())
        .setIcon(IconCompat.createWithAdaptiveBitmap(avatar))
        .setIntent(intent)
        .build();
      Util.runOnMain(() -> {
        if (!ShortcutManagerCompat.requestPinShortcut(activity, shortcutInfoCompat, null)) {
          Toast.makeText(activity, "ErrAddToHomescreen: requestPinShortcut() failed", Toast.LENGTH_LONG).show();
        } else if (actionMode != null) {
          actionMode.finish();
          actionMode = null;
        }
      });
    });
  }

  private void updateActionModeItems(Menu menu) {
    // We do not show action mode icons when relaying (= sharing or forwarding).
    if (!isRelayingMessageContent(requireActivity())) {
      final int selectedCount = getListAdapter().getBatchSelections().size();
      menu.findItem(R.id.menu_add_to_home_screen).setVisible(selectedCount == 1);
      MenuItem archiveItem = menu.findItem(R.id.menu_archive_selected);
      if (offerToArchive()) {
          archiveItem.setIcon(R.drawable.ic_archive_white_24dp);
          archiveItem.setTitle(R.string.menu_archive_chat);
      } else {
          archiveItem.setIcon(R.drawable.ic_unarchive_white_24dp);
          archiveItem.setTitle(R.string.menu_unarchive_chat);
      }
      MenuItem pinItem = menu.findItem(R.id.menu_pin_selected);
      if (areSomeSelectedChatsUnpinned()) {
        pinItem.setIcon(R.drawable.ic_pin_white);
        pinItem.setTitle(R.string.pin_chat);
      } else {
        pinItem.setIcon(R.drawable.ic_unpin_white);
        pinItem.setTitle(R.string.unpin_chat);
      }
      MenuItem muteItem = menu.findItem(R.id.menu_mute_selected);
      if (areSomeSelectedChatsUnmuted()) {
        muteItem.setTitle(R.string.menu_mute);
      } else {
        muteItem.setTitle(R.string.menu_unmute);
      }
    }
  }

  @Override
  public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    if (isRelayingMessageContent(getActivity())) {
      if (ShareUtil.getSharedContactId(getActivity()) != 0) {
        return false; // no sharing of a contact to multiple recipients at the same time, we can reconsider when that becomes a real-world need
      }
      Context context = getContext();
      if (context != null) {
        fab.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_send_sms_white_24dp));
      }
      setFabVisibility(true);
      initializeFabClickListener(true);
    } else {

      MenuInflater inflater = requireActivity().getMenuInflater();
      inflater.inflate(R.menu.conversation_list, menu);
    }

    mode.setTitle("1");

    return true;
  }

  @Override
  public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
    return false;
  }

  @Override
  public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    int itemId = item.getItemId();
    if (itemId == R.id.menu_select_all) {
      handleSelectAllThreads();
      return true;
    } else if (itemId == R.id.menu_delete_selected) {
      handleDeleteAllSelected();
      return true;
    } else if (itemId == R.id.menu_pin_selected) {
      handlePinAllSelected();
      return true;
    } else if (itemId == R.id.menu_archive_selected) {
      handleArchiveAllSelected();
      return true;
    } else if (itemId == R.id.menu_mute_selected) {
      handleMuteAllSelected();
      return true;
    } else if (itemId == R.id.menu_marknoticed_selected) {
      handleMarknoticedSelected();
      return true;
    } else if (itemId == R.id.menu_add_to_home_screen) {
      handleAddToHomeScreen();
      return true;
    }

    return false;
  }

  @Override
  public void onDestroyActionMode(ActionMode mode) {
    actionMode = null;
    getListAdapter().initializeBatchMode(false);

    Context context = getContext();
    if (context != null) {
      fab.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_add_white_24dp));
    }
    setFabVisibility(false);
    initializeFabClickListener(false);
  }

  public interface ConversationSelectedListener {
    void onCreateConversation(int chatId);
    void onSwitchToArchive();
  }
}
