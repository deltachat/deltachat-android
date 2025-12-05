package org.thoughtcrime.securesms;

import static com.b44t.messenger.DcChat.DC_CHAT_NO_CHAT;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcMsg;
import com.codewaves.stickyheadergrid.StickyHeaderGridLayoutManager;

import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.loaders.BucketedThreadMediaLoader;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.Set;

public class AllMediaDocumentsFragment
    extends MessageSelectorFragment
    implements LoaderManager.LoaderCallbacks<BucketedThreadMediaLoader.BucketedThreadMedia>,
               AllMediaDocumentsAdapter.ItemClickListener
{
  public static final String CHAT_ID_EXTRA = "chat_id";
  public static final String VIEWTYPE1 = "viewtype1";
  public static final String VIEWTYPE2 = "viewtype2";

  protected TextView noMedia;
  protected RecyclerView recyclerView;
  private StickyHeaderGridLayoutManager gridManager;
  private final ActionModeCallback actionModeCallback = new ActionModeCallback();
  private int viewtype1;
  private int viewtype2;

  protected int                chatId;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    dcContext = DcHelper.getContext(getContext());
    chatId = getArguments().getInt(CHAT_ID_EXTRA, -1);
    viewtype1 = getArguments().getInt(VIEWTYPE1, 0);
    viewtype2 = getArguments().getInt(VIEWTYPE2, 0);

    getLoaderManager().initLoader(0, null, this);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.profile_documents_fragment, container, false);

    this.recyclerView = ViewUtil.findById(view, R.id.recycler_view);
    this.noMedia      = ViewUtil.findById(view, R.id.no_documents);
    this.gridManager  = new StickyHeaderGridLayoutManager(1);

    // add padding to avoid content hidden behind system bars
    ViewUtil.applyWindowInsets(recyclerView, true, false, true, true);

    this.recyclerView.setAdapter(new AllMediaDocumentsAdapter(getContext(),
        new BucketedThreadMediaLoader.BucketedThreadMedia(getContext()),
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
    return new BucketedThreadMediaLoader(getContext(), chatId, viewtype1, viewtype2, 0);
  }

  @Override
  public void onLoadFinished(Loader<BucketedThreadMediaLoader.BucketedThreadMedia> loader, BucketedThreadMediaLoader.BucketedThreadMedia bucketedThreadMedia) {
    ((AllMediaDocumentsAdapter) recyclerView.getAdapter()).setMedia(bucketedThreadMedia);
    ((AllMediaDocumentsAdapter) recyclerView.getAdapter()).notifyAllSectionsDataSetChanged();

    noMedia.setVisibility(recyclerView.getAdapter().getItemCount() > 0 ? View.GONE : View.VISIBLE);
    if (chatId == DC_CHAT_NO_CHAT) {
      if (viewtype1 == DcMsg.DC_MSG_WEBXDC) {
        noMedia.setText(R.string.all_apps_empty_hint);
      } else if (viewtype1 == DcMsg.DC_MSG_FILE){
        noMedia.setText(R.string.all_files_empty_hint);
      } else {
        noMedia.setText(R.string.tab_all_media_empty_hint);
      }
    } else if (viewtype1 == DcMsg.DC_MSG_AUDIO) {
      noMedia.setText(R.string.tab_audio_empty_hint);
    } else if (viewtype1 == DcMsg.DC_MSG_WEBXDC) {
      noMedia.setText(R.string.tab_webxdc_empty_hint);
    }
    getActivity().invalidateOptionsMenu();
  }

  @Override
  public void onLoaderReset(Loader<BucketedThreadMediaLoader.BucketedThreadMedia> cursorLoader) {
    ((AllMediaDocumentsAdapter) recyclerView.getAdapter()).setMedia(new BucketedThreadMediaLoader.BucketedThreadMedia(getContext()));
  }

  @Override
  public void onMediaClicked(@NonNull DcMsg mediaRecord) {
    if (actionMode != null) {
      handleMediaMultiSelectClick(mediaRecord);
    } else {
      handleMediaPreviewClick(mediaRecord);
    }
  }

  private void updateActionModeBar() {
    actionMode.setTitle(String.valueOf(getListAdapter().getSelectedMediaCount()));
    setCorrectMenuVisibility(actionMode.getMenu());
  }

  private void handleMediaMultiSelectClick(@NonNull DcMsg mediaRecord) {
    AllMediaDocumentsAdapter adapter = getListAdapter();

    adapter.toggleSelection(mediaRecord);
    if (adapter.getSelectedMediaCount() == 0) {
      actionMode.finish();
      actionMode = null;
    } else {
      updateActionModeBar();
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

    if (dcMsg.getType() == DcMsg.DC_MSG_WEBXDC) {
      WebxdcActivity.openWebxdcActivity(context, dcMsg);
    } else {
      DcHelper.openForViewOrShare(getActivity(), dcMsg.getId(), Intent.ACTION_VIEW);
    }
  }

  @Override
  public void onMediaLongClicked(DcMsg mediaRecord) {
    if (actionMode == null) {
      ((AllMediaDocumentsAdapter) recyclerView.getAdapter()).toggleSelection(mediaRecord);

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

    boolean canResend = true;
    for (DcMsg messageRecord : messageRecords) {
      if (!messageRecord.isOutgoing()) {
        canResend = false;
        break;
      }
    }
    menu.findItem(R.id.menu_resend).setVisible(canResend);

    boolean webxdcApp = singleSelection && messageRecords.iterator().next().getType() == DcMsg.DC_MSG_WEBXDC;
    menu.findItem(R.id.menu_add_to_home_screen).setVisible(webxdcApp);
  }

  private AllMediaDocumentsAdapter getListAdapter() {
    return (AllMediaDocumentsAdapter) recyclerView.getAdapter();
  }

  private class ActionModeCallback implements ActionMode.Callback {

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
      mode.getMenuInflater().inflate(R.menu.profile_context, menu);
      mode.setTitle("1");

      setCorrectMenuVisibility(menu);
      return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
      return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
      int itemId = menuItem.getItemId();
      if (itemId == R.id.details) {
        handleDisplayDetails(getSelectedMessageRecord(getListAdapter().getSelectedMedia()));
        mode.finish();
        return true;
      } else if (itemId == R.id.delete) {
        handleDeleteMessages(chatId, getListAdapter().getSelectedMedia());
        mode.finish();
        return true;
      } else if (itemId == R.id.share) {
        handleShare(getSelectedMessageRecord(getListAdapter().getSelectedMedia()));
        return true;
      } else if (itemId == R.id.menu_add_to_home_screen) {
        WebxdcActivity.addToHomeScreen(getActivity(), getSelectedMessageRecord(getListAdapter().getSelectedMedia()).getId());
        mode.finish();
        return true;
      } else if (itemId == R.id.show_in_chat) {
        handleShowInChat(getSelectedMessageRecord(getListAdapter().getSelectedMedia()));
        return true;
      } else if (itemId == R.id.save) {
        handleSaveAttachment(getListAdapter().getSelectedMedia());
        return true;
      } else if (itemId == R.id.menu_resend) {
        handleResendMessage(getListAdapter().getSelectedMedia());
        return true;
      } else if (itemId == R.id.menu_select_all) {
        getListAdapter().selectAll();
        updateActionModeBar();
        return true;
      }
      return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
      actionMode = null;
      getListAdapter().clearSelection();
    }
  }
}
