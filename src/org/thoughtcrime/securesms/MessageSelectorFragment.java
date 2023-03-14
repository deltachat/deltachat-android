package org.thoughtcrime.securesms;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.SaveAttachmentTask;
import org.thoughtcrime.securesms.util.StorageUtil;

import java.util.Set;

public abstract class MessageSelectorFragment
    extends Fragment
    implements DcEventCenter.DcEventDelegate
{
  protected ActionMode actionMode;
  protected DcContext dcContext;

  protected abstract void setCorrectMenuVisibility(Menu menu);

  protected ActionMode getActionMode() {
    return actionMode;
  }

  protected DcMsg getSelectedMessageRecord(Set<DcMsg> messageRecords) {
    if (messageRecords.size() == 1) return messageRecords.iterator().next();
    else                            throw new AssertionError();
  }

  protected void handleDisplayDetails(DcMsg dcMsg) {
    View view = View.inflate(getActivity(), R.layout.message_details_view, null);
    TextView detailsText = view.findViewById(R.id.details_text);
    detailsText.setText(dcContext.getMsgInfo(dcMsg.getId()));

    AlertDialog d = new AlertDialog.Builder(getActivity())
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .create();
    d.show();
  }

  protected void handleDeleteMessages(int chatId, final Set<DcMsg> messageRecords) {
    handleDeleteMessages(chatId, DcMsg.msgSetToIds(messageRecords));
  }

  protected void handleDeleteMessages(int chatId, final int[] messageIds) {
    DcChat dcChat = DcHelper.getContext(getActivity()).getChat(chatId);

    String text = getActivity().getResources().getQuantityString(
      dcChat.isDeviceTalk()? R.plurals.ask_delete_messages_simple : R.plurals.ask_delete_messages,
      messageIds.length, messageIds.length);

    new AlertDialog.Builder(getActivity())
            .setMessage(text)
            .setCancelable(true)
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                dcContext.deleteMsgs(messageIds);
                if (actionMode != null) actionMode.finish();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
  }

  protected void handleSaveAttachment(final Set<DcMsg> messageRecords) {
    SaveAttachmentTask.showWarningDialog(getContext(), (dialogInterface, i) -> {
      if (StorageUtil.canWriteToMediaStore(getContext())) {
        performSave(messageRecords);
        return;
      }

      Permissions.with(getActivity())
              .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
              .ifNecessary()
              .withPermanentDenialDialog(getString(R.string.perm_explain_access_to_storage_denied))
              .onAllGranted(() -> performSave(messageRecords))
              .execute();
    });
  }

  private void performSave(Set<DcMsg> messageRecords) {
    SaveAttachmentTask.Attachment[] attachments = new SaveAttachmentTask.Attachment[messageRecords.size()];
    int index = 0;
    for (DcMsg message : messageRecords) {
        attachments[index] = new SaveAttachmentTask.Attachment(
            Uri.fromFile(message.getFileAsFile()), message.getFilemime(), message.getDateReceived(), message.getFilename());
        index++;
    }
    SaveAttachmentTask saveTask = new SaveAttachmentTask(getContext());
    saveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, attachments);
    if (actionMode != null) actionMode.finish();
  }

  protected void handleShowInChat(final DcMsg dcMsg) {
    Intent intent = new Intent(getContext(), ConversationActivity.class);
    intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, dcMsg.getChatId());
    intent.putExtra(ConversationActivity.STARTING_POSITION_EXTRA, DcMsg.getMessagePosition(dcMsg, dcContext));
    startActivity(intent);
  }

  protected void handleShare(final DcMsg dcMsg) {
    DcHelper.openForViewOrShare(getContext(), dcMsg.getId(), Intent.ACTION_SEND);
  }
}
