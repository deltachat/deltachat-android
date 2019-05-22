package org.thoughtcrime.securesms;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import com.codewaves.stickyheadergrid.StickyHeaderGridLayoutManager;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.loaders.BucketedThreadMediaLoader;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.task.ProgressDialogAsyncTask;

import java.util.Collection;
import java.util.Locale;

public class ProfileDocumentsFragment
    extends Fragment implements LoaderManager.LoaderCallbacks<BucketedThreadMediaLoader.BucketedThreadMedia>, ProfileDocumentsAdapter.ItemClickListener
{
  public static final String LOCALE_EXTRA  = "locale_extra";
  public static final String CHAT_ID_EXTRA = "chat_id";

  protected TextView noMedia;
  protected RecyclerView recyclerView;
  private StickyHeaderGridLayoutManager gridManager;
  private ActionMode actionMode;
  private ActionModeCallback actionModeCallback = new ActionModeCallback();

  protected int          chatId;
  protected Locale locale;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);

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

    return view;
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
    }
  }

  private void handleMediaPreviewClick(@NonNull DcMsg dcMsg) {
    // audio is stated by the play-button
    if (dcMsg.getType()==DcMsg.DC_MSG_AUDIO || dcMsg.getType()==DcMsg.DC_MSG_VOICE) {
      return;
    }

    Context context = getContext();
    if (context == null) {
      return;
    }

    ApplicationDcContext dcContext = DcHelper.getContext(context);
    dcContext.openForViewOrShare(dcMsg.getId(), Intent.ACTION_VIEW);
  }

  @Override
  public void onMediaLongClicked(DcMsg mediaRecord) {
    if (actionMode == null) {
      ((ProfileDocumentsAdapter) recyclerView.getAdapter()).toggleSelection(mediaRecord);
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
    final DcContext dcContext = DcHelper.getContext(getContext());

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
