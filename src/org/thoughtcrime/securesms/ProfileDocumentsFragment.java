package org.thoughtcrime.securesms;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
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

import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.loaders.BucketedThreadMediaLoader;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.Locale;
import java.util.Set;

public class ProfileDocumentsFragment
    extends MessageSelectorFragment
    implements LoaderManager.LoaderCallbacks<BucketedThreadMediaLoader.BucketedThreadMedia>,
               ProfileDocumentsAdapter.ItemClickListener
{
  public static final String LOCALE_EXTRA  = "locale_extra";
  public static final String CHAT_ID_EXTRA = "chat_id";

  protected TextView noMedia;
  protected RecyclerView recyclerView;
  private StickyHeaderGridLayoutManager gridManager;
  private ActionModeCallback actionModeCallback = new ActionModeCallback();

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

    DcEventCenter eventCenter = DcHelper.getEventCenter(getContext());
    eventCenter.addObserver(DcContext.DC_EVENT_MSGS_CHANGED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_INCOMING_MSG, this);
    return view;
  }

  @Override
  public void onDestroyView() {
    DcHelper.getEventCenter(getContext()).removeObservers(this);
    super.onDestroyView();
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
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

    DcHelper.openForViewOrShare(getActivity(), dcMsg.getId(), Intent.ACTION_VIEW);
  }

  @Override
  public void onMediaLongClicked(DcMsg mediaRecord) {
    if (actionMode == null) {
      ((ProfileDocumentsAdapter) recyclerView.getAdapter()).toggleSelection(mediaRecord);
      recyclerView.getAdapter().notifyDataSetChanged();

      actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
    }
  }

  @Override
  protected void setCorrectMenuVisibility(Menu menu) {
    Set<DcMsg> messageRecords = getListAdapter().getSelectedMedia();

    if (actionMode != null && messageRecords.size() == 0) {
      actionMode.finish();
      return;
    }

    boolean singleSelection = messageRecords.size() == 1;
    menu.findItem(R.id.details).setVisible(singleSelection);
    menu.findItem(R.id.show_in_chat).setVisible(singleSelection);
    menu.findItem(R.id.share).setVisible(singleSelection);
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
          handleDisplayDetails(getSelectedMessageRecord(getListAdapter().getSelectedMedia()));
          mode.finish();
          return true;
        case R.id.delete:
          handleDeleteMessages(getListAdapter().getSelectedMedia());
          mode.finish();
          return true;
        case R.id.share:
          handleShare(getSelectedMessageRecord(getListAdapter().getSelectedMedia()));
          return true;
        case R.id.show_in_chat:
          handleShowInChat(getSelectedMessageRecord(getListAdapter().getSelectedMedia()));
          return true;
        case R.id.save:
          handleSaveAttachment(getListAdapter().getSelectedMedia());
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
