package org.thoughtcrime.securesms;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.util.Linkify;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcMsg;
import com.codewaves.stickyheadergrid.StickyHeaderGridLayoutManager;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.loaders.BucketedThreadMediaLoader;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.SaveAttachmentTask;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.Locale;
import java.util.Set;

public class ProfileDocumentsFragment
    extends Fragment
    implements LoaderManager.LoaderCallbacks<BucketedThreadMediaLoader.BucketedThreadMedia>,
               ProfileDocumentsAdapter.ItemClickListener, DcEventCenter.DcEventDelegate
{
  public static final String LOCALE_EXTRA  = "locale_extra";
  public static final String CHAT_ID_EXTRA = "chat_id";

  protected TextView noMedia;
  protected RecyclerView recyclerView;
  private StickyHeaderGridLayoutManager gridManager;
  private ActionMode actionMode;
  private ActionModeCallback actionModeCallback = new ActionModeCallback();

  private ApplicationDcContext dcContext;
  protected int                chatId;
  protected Locale             locale;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    dcContext = DcHelper.getContext(getContext());
    chatId = getArguments().getInt(CHAT_ID_EXTRA, -1);
    locale = (Locale)getArguments().getSerializable(LOCALE_EXTRA);
    if (locale == null) throw new AssertionError();

    getLoaderManager().initLoader(0, null, this);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.profile_documents_fragment, container, false);

    this.recyclerView = ViewUtil.findById(view, R.id.recycler_view);
    this.noMedia      = ViewUtil.findById(view, R.id.no_documents);
    this.gridManager  = new StickyHeaderGridLayoutManager(1);

    this.recyclerView.setAdapter(new ProfileDocumentsAdapter(getContext(),
        new BucketedThreadMediaLoader.BucketedThreadMedia(getContext()),
        locale,
        this));
    this.recyclerView.setLayoutManager(gridManager);
    this.recyclerView.setHasFixedSize(true);

    dcContext.eventCenter.addObserver(DcContext.DC_EVENT_MSGS_CHANGED, this);
    dcContext.eventCenter.addObserver(DcContext.DC_EVENT_INCOMING_MSG, this);
    return view;
  }

  @Override
  public void onDestroyView() {
    dcContext.eventCenter.removeObservers(this);
    super.onDestroyView();
  }

  @Override
  public void handleEvent(DcEvent event) {
    getLoaderManager().restartLoader(0, null, this);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    if (gridManager != null) {
      this.gridManager = new StickyHeaderGridLayoutManager(1);
      this.recyclerView.setLayoutManager(gridManager);
    }
  }

  @Override
  public Loader<BucketedThreadMediaLoader.BucketedThreadMedia> onCreateLoader(int i, Bundle bundle) {
    return new BucketedThreadMediaLoader(getContext(), chatId, DcMsg.DC_MSG_FILE, DcMsg.DC_MSG_AUDIO, 0);
  }

  @Override
  public void onLoadFinished(Loader<BucketedThreadMediaLoader.BucketedThreadMedia> loader, BucketedThreadMediaLoader.BucketedThreadMedia bucketedThreadMedia) {
    ((ProfileDocumentsAdapter) recyclerView.getAdapter()).setMedia(bucketedThreadMedia);
    ((ProfileDocumentsAdapter) recyclerView.getAdapter()).notifyAllSectionsDataSetChanged();

    noMedia.setVisibility(recyclerView.getAdapter().getItemCount() > 0 ? View.GONE : View.VISIBLE);
    getActivity().invalidateOptionsMenu();
  }

  @Override
  public void onLoaderReset(Loader<BucketedThreadMediaLoader.BucketedThreadMedia> cursorLoader) {
    ((ProfileDocumentsAdapter) recyclerView.getAdapter()).setMedia(new BucketedThreadMediaLoader.BucketedThreadMedia(getContext()));
  }

  @Override
  public void onMediaClicked(@NonNull DcMsg mediaRecord) {
    if (actionMode != null) {
      handleMediaMultiSelectClick(mediaRecord);
    } else {
      handleMediaPreviewClick(mediaRecord);
    }
  }

  private void handleMediaMultiSelectClick(@NonNull DcMsg mediaRecord) {
    ProfileDocumentsAdapter adapter = getListAdapter();

    adapter.toggleSelection(mediaRecord);
    if (adapter.getSelectedMediaCount() == 0) {
      actionMode.finish();
      actionMode = null;
    } else {
      actionMode.setTitle(String.valueOf(adapter.getSelectedMediaCount()));
      setCorrectMenuVisibility(actionMode.getMenu());
    }
  }

  private void handleMediaPreviewClick(@NonNull DcMsg dcMsg) {
    // audio is started by the play-button
    if (dcMsg.getType()==DcMsg.DC_MSG_AUDIO || dcMsg.getType()==DcMsg.DC_MSG_VOICE) {
      return;
    }

    Context context = getContext();
    if (context == null) {
      return;
    }

    dcContext.openForViewOrShare(getActivity(), dcMsg.getId(), Intent.ACTION_VIEW);
  }

  @Override
  public void onMediaLongClicked(DcMsg mediaRecord) {
    if (actionMode == null) {
      ((ProfileDocumentsAdapter) recyclerView.getAdapter()).toggleSelection(mediaRecord);
      recyclerView.getAdapter().notifyDataSetChanged();

      actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
    }
  }

  private void handleDisplayDetails(DcMsg dcMsg) {
    String info_str = dcContext.getMsgInfo(dcMsg.getId());
    AlertDialog d = new AlertDialog.Builder(getActivity())
            .setMessage(info_str)
            .setPositiveButton(android.R.string.ok, null)
            .create();
    d.show();
    try {
      //noinspection ConstantConditions
      Linkify.addLinks((TextView) d.findViewById(android.R.id.message), Linkify.WEB_URLS);
    } catch(NullPointerException e) {
      e.printStackTrace();
    }
  }

  private void handleDeleteMedia(final Set<DcMsg> messageRecords) {
    int messagesCount = messageRecords.size();

    new AlertDialog.Builder(getActivity())
            .setMessage(getActivity().getResources().getQuantityString(R.plurals.ask_delete_messages, messagesCount, messagesCount))
            .setCancelable(true)
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                int[] ids = DcMsg.msgSetToIds(messageRecords);
                dcContext.deleteMsgs(ids);
                actionMode.finish();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
  }

  private void handleShowInChat(final DcMsg dcMsg) {
    Intent intent = new Intent(getContext(), ConversationActivity.class);
    intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, dcMsg.getChatId());
    intent.putExtra(ConversationActivity.STARTING_POSITION_EXTRA, DcMsg.getMessagePosition(dcMsg, dcContext));
    startActivity(intent);
  }

  private void handleSaveAttachment(final DcMsg message) {
    SaveAttachmentTask.showWarningDialog(getContext(), (dialogInterface, i) -> {
        Permissions.with(getActivity())
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .ifNecessary()
                .withPermanentDenialDialog(getString(R.string.perm_explain_access_to_storage_denied))
                .onAllGranted(() -> {
                    SaveAttachmentTask saveTask = new SaveAttachmentTask(getContext());
                    SaveAttachmentTask.Attachment attachment = new SaveAttachmentTask.Attachment(
                            Uri.fromFile(message.getFileAsFile()), message.getFilemime(), message.getDateReceived(), message.getFilename());
                    saveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, attachment);
                    actionMode.finish();
                })
                .execute();
    });
  }

  private DcMsg getSelectedMessageRecord() {
    Set<DcMsg> messageRecords = getListAdapter().getSelectedMedia();

    if (messageRecords.size() == 1) return messageRecords.iterator().next();
    else                            throw new AssertionError();
  }

  private void setCorrectMenuVisibility(Menu menu) {
    Set<DcMsg> messageRecords = getListAdapter().getSelectedMedia();

    if (actionMode != null && messageRecords.size() == 0) {
      actionMode.finish();
      return;
    }

    if (messageRecords.size() > 1) {
      menu.findItem(R.id.details).setVisible(false);
      menu.findItem(R.id.show_in_chat).setVisible(false);
      menu.findItem(R.id.save).setVisible(false);
    } else {
      menu.findItem(R.id.details).setVisible(true);
      menu.findItem(R.id.show_in_chat).setVisible(true);
      menu.findItem(R.id.save).setVisible(true);
    }
  }

  private ProfileDocumentsAdapter getListAdapter() {
    return (ProfileDocumentsAdapter) recyclerView.getAdapter();
  }

  private class ActionModeCallback implements ActionMode.Callback {

    private int originalStatusBarColor;

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
      mode.getMenuInflater().inflate(R.menu.profile_context, menu);
      mode.setTitle("1");

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        Window window = getActivity().getWindow();
        originalStatusBarColor = window.getStatusBarColor();
        window.setStatusBarColor(getResources().getColor(R.color.action_mode_status_bar));
      }
      setCorrectMenuVisibility(menu);
      return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
      return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
      switch (menuItem.getItemId()) {
        case R.id.details:
          handleDisplayDetails(getSelectedMessageRecord());
          mode.finish();
          return true;
        case R.id.delete:
          handleDeleteMedia(getListAdapter().getSelectedMedia());
          mode.finish();
          return true;
        case R.id.show_in_chat:
          handleShowInChat(getSelectedMessageRecord());
          return true;
        case R.id.save:
          handleSaveAttachment(getSelectedMessageRecord());
          return true;
      }
      return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
      actionMode = null;
      getListAdapter().clearSelection();

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        getActivity().getWindow().setStatusBarColor(originalStatusBarColor);
      }
    }
  }
}
