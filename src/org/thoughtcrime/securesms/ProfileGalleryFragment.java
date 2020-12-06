package org.thoughtcrime.securesms;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
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
import com.b44t.messenger.DcEventCenter;
import com.b44t.messenger.DcMsg;
import com.codewaves.stickyheadergrid.StickyHeaderGridLayoutManager;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.database.loaders.BucketedThreadMediaLoader;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.task.ProgressDialogAsyncTask;

import java.util.Collection;
import java.util.Locale;

public class ProfileGalleryFragment
    extends Fragment
    implements LoaderManager.LoaderCallbacks<BucketedThreadMediaLoader.BucketedThreadMedia>,
               ProfileGalleryAdapter.ItemClickListener, DcEventCenter.DcEventDelegate
{
  public static final String LOCALE_EXTRA  = "locale_extra";
  public static final String CHAT_ID_EXTRA = "chat_id";

  protected TextView noMedia;
  protected RecyclerView recyclerView;
  private StickyHeaderGridLayoutManager gridManager;
  private ActionMode actionMode;
  private ActionModeCallback actionModeCallback = new ActionModeCallback();

  private ApplicationDcContext dcContext;
  private int                  chatId;
  private Locale               locale;

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
    View view = inflater.inflate(R.layout.profile_gallery_fragment, container, false);

    this.recyclerView = ViewUtil.findById(view, R.id.media_grid);
    this.noMedia      = ViewUtil.findById(view, R.id.no_images);
    this.gridManager  = new StickyHeaderGridLayoutManager(getCols());

    this.recyclerView.setAdapter(new ProfileGalleryAdapter(getContext(),
                                                         GlideApp.with(this),
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

  private int getCols() {
    return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE? 5 : 3;
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    if (gridManager != null) {
      this.gridManager = new StickyHeaderGridLayoutManager(getCols());
      this.recyclerView.setLayoutManager(gridManager);
    }
  }

  @Override
  public Loader<BucketedThreadMediaLoader.BucketedThreadMedia> onCreateLoader(int i, Bundle bundle) {
    return new BucketedThreadMediaLoader(getContext(), chatId, DcMsg.DC_MSG_IMAGE, DcMsg.DC_MSG_GIF, DcMsg.DC_MSG_VIDEO);
  }

  @Override
  public void onLoadFinished(Loader<BucketedThreadMediaLoader.BucketedThreadMedia> loader, BucketedThreadMediaLoader.BucketedThreadMedia bucketedThreadMedia) {
    ((ProfileGalleryAdapter) recyclerView.getAdapter()).setMedia(bucketedThreadMedia);
    ((ProfileGalleryAdapter) recyclerView.getAdapter()).notifyAllSectionsDataSetChanged();

    noMedia.setVisibility(recyclerView.getAdapter().getItemCount() > 0 ? View.GONE : View.VISIBLE);
    getActivity().invalidateOptionsMenu();
  }

  @Override
  public void onLoaderReset(Loader<BucketedThreadMediaLoader.BucketedThreadMedia> cursorLoader) {
    ((ProfileGalleryAdapter) recyclerView.getAdapter()).setMedia(new BucketedThreadMediaLoader.BucketedThreadMedia(getContext()));
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
    ProfileGalleryAdapter adapter = getListAdapter();

    adapter.toggleSelection(mediaRecord);
    if (adapter.getSelectedMediaCount() == 0) {
      actionMode.finish();
      actionMode = null;
    } else {
      actionMode.setTitle(String.valueOf(adapter.getSelectedMediaCount()));
    }
  }

  private void handleMediaPreviewClick(@NonNull DcMsg mediaRecord) {
    if (mediaRecord.getFile() == null) {
      return;
    }

    Context context = getContext();
    if (context == null) {
      return;
    }

    Intent intent = new Intent(context, MediaPreviewActivity.class);
    intent.putExtra(MediaPreviewActivity.DC_MSG_ID, mediaRecord.getId());
    intent.putExtra(MediaPreviewActivity.ADDRESS_EXTRA, Address.fromChat(chatId));
    intent.putExtra(MediaPreviewActivity.OUTGOING_EXTRA, mediaRecord.isOutgoing());
    intent.putExtra(MediaPreviewActivity.LEFT_IS_RECENT_EXTRA, false);
    intent.putExtra(MediaPreviewActivity.OPENED_FROM_PROFILE, true);
    context.startActivity(intent);
  }

  @Override
  public void onMediaLongClicked(DcMsg mediaRecord) {
    if (actionMode == null) {
      ((ProfileGalleryAdapter) recyclerView.getAdapter()).toggleSelection(mediaRecord);
      recyclerView.getAdapter().notifyDataSetChanged();

      actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
    }
  }

  @SuppressLint("StaticFieldLeak")
  private void handleDeleteMedia(@NonNull Collection<DcMsg> mediaRecords) {
    int recordCount       = mediaRecords.size();
    Resources res         = getContext().getResources();
    String confirmMessage = res.getQuantityString(R.plurals.ask_delete_messages,
                                                  recordCount,
                                                  recordCount);

    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    builder.setMessage(confirmMessage);
    builder.setCancelable(true);

    builder.setPositiveButton(R.string.delete, (dialogInterface, i) -> {
      new ProgressDialogAsyncTask<DcMsg, Void, Void>(getContext(),
                                                                         R.string.one_moment,
                                                                         R.string.one_moment)
      {
        @Override
        protected Void doInBackground(DcMsg... records) {
          if (records == null || records.length == 0) {
            return null;
          }

          for (DcMsg record : records) {
            dcContext.deleteMsgs(new int[]{record.getId()});
          }
          return null;
        }

      }.execute(mediaRecords.toArray(new DcMsg[mediaRecords.size()]));
    });
    builder.setNegativeButton(android.R.string.cancel, null);
    builder.show();
  }

  private ProfileGalleryAdapter getListAdapter() {
    return (ProfileGalleryAdapter) recyclerView.getAdapter();
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
      return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
      return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
      switch (menuItem.getItemId()) {
        case R.id.delete:
          handleDeleteMedia(getListAdapter().getSelectedMedia());
          mode.finish();
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
