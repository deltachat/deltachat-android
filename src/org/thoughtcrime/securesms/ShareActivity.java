/*
 * Copyright (C) 2014-2017 Open Whisper Systems
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
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Process;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.b44t.messenger.DcChatlist;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEventCenter;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcChatlistLoader;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.mms.PartAuthority;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.FileUtils;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * An activity to quickly share content with chats
 *
 * @author Jake McGinty
 */
public class ShareActivity extends PassphraseRequiredActionBarActivity
{

  interface ConversationClickedListener {

    void onConversationClicked(int chatId);

  }

  private static final String TAG = ShareActivity.class.getSimpleName();

  public static final String EXTRA_THREAD_ID          = "thread_id";
  public static final String EXTRA_ADDRESS_MARSHALLED = "address_marshalled";
  public static final String EXTRA_DISTRIBUTION_TYPE  = "distribution_type";
  public static final String EXTRA_MSG_IDS  = "message_ids";
  public static final String EXTRA_FORWARD  = "forward";

  private final DynamicTheme    dynamicTheme    = new DynamicNoActionBarTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private ShareFragment shareFragment;
  private View                         progressWheel;
  private Uri                          resolvedExtra;
  private String                       mimeType;
  private boolean                      isPassingAlongMedia;
  private ApplicationDcContext         dcContext;
  private boolean                      isForward;

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }

  @Override
  protected void onCreate(Bundle icicle, boolean ready) {
    dcContext = DcHelper.getContext(this);

    setContentView(R.layout.share_activity);

    initializeActivityContext();
    initializeToolbar();
    initializeResources();
    initializeMedia();
  }

  private void initializeActivityContext() {
    isForward = getIntent().getBooleanExtra(EXTRA_FORWARD, false);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    Log.w(TAG, "onNewIntent()");
    super.onNewIntent(intent);
    setIntent(intent);
    initializeMedia();
  }

  @Override
  public void onResume() {
    Log.w(TAG, "onResume()");
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
  }

  @Override
  public void onPause() {
    super.onPause();
    if (!isPassingAlongMedia && resolvedExtra != null) {
      PersistentBlobProvider.getInstance(this).delete(this, resolvedExtra);
    }
    if (!isFinishing()) {
      finish();
    }
  }

  @Override
  public void finish() {
    super.finish();
    overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_right);
  }

  private void initializeToolbar() {
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    if(isForward) {
      TextView title = toolbar.findViewById(R.id.title);
      title.setText(R.string.forward_to);
    }

    ActionBar actionBar = getSupportActionBar();

    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
  }

  private void initializeResources() {
    progressWheel    = findViewById(R.id.progress_wheel);
    shareFragment = (ShareFragment) getSupportFragmentManager().findFragmentById(R.id.share_fragment);
    shareFragment.setConversationClickedListener(this::onConversationClick);
    shareFragment.setLocale(dynamicLanguage.getCurrentLocale());
  }

  private void onConversationClick(int chatId) {
    String name = dcContext.getChat(chatId).getName();
    if (isForward) {
      new AlertDialog.Builder(this)
        .setMessage(getString(R.string.ask_forward, name))
        .setPositiveButton(R.string.ok, (dialogInterface, i) -> delegateMessage(chatId))
        .setNegativeButton(R.string.cancel, null)
        .show();
    } else {
      delegateMessage(chatId);
    }
  }

  private void delegateMessage(int chatId) {
    int[] value = getIntent().getIntArrayExtra(EXTRA_MSG_IDS);
    dcContext.forwardMsgs(value, chatId);
    createConversation(chatId);
  }

  private void initializeMedia() {
    final Context context = this;
    isPassingAlongMedia = false;

    Uri streamExtra = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
    mimeType        = getMimeType(streamExtra);

    if (streamExtra != null && PartAuthority.isLocalUri(streamExtra)) {
      isPassingAlongMedia = true;
      resolvedExtra       = streamExtra;
      handleResolvedMedia(getIntent(), false);
    } else {
      shareFragment.getView().setVisibility(View.GONE);
      progressWheel.setVisibility(View.VISIBLE);
      new ResolveMediaTask(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, streamExtra);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
    case android.R.id.home:     finish();                return true;
    }
    return false;
  }

  private void handleResolvedMedia(Intent intent, boolean animate) {
    int       threadId         = intent.getIntExtra(EXTRA_THREAD_ID, -1);
    int       distributionType = intent.getIntExtra(EXTRA_DISTRIBUTION_TYPE, -1);
    Address   address          = null;

    if (intent.hasExtra(EXTRA_ADDRESS_MARSHALLED)) {
      Parcel parcel = Parcel.obtain();
      byte[] marshalled = intent.getByteArrayExtra(EXTRA_ADDRESS_MARSHALLED);
      parcel.unmarshall(marshalled, 0, marshalled.length);
      parcel.setDataPosition(0);
      address = parcel.readParcelable(getClassLoader());
      parcel.recycle();
    }

    boolean hasResolvedDestination = threadId != -1 && address != null && distributionType != -1;

    if (!hasResolvedDestination && animate) {
      ViewUtil.fadeIn(shareFragment.getView(), 300);
      ViewUtil.fadeOut(progressWheel, 300);
    } else if (!hasResolvedDestination) {
      shareFragment.getView().setVisibility(View.VISIBLE);
      progressWheel.setVisibility(View.GONE);
    } else {
      createConversation(threadId);
    }
  }

  private void createConversation(int threadId) {
    final Intent intent = getBaseShareIntent(ConversationActivity.class);
    intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, threadId);
    isPassingAlongMedia = true;
    startActivity(intent);
  }

  private Intent getBaseShareIntent(final @NonNull Class<?> target) {
    final Intent intent      = new Intent(this, target);
    final String textExtra   = getIntent().getStringExtra(Intent.EXTRA_TEXT);
    intent.putExtra(ConversationActivity.TEXT_EXTRA, textExtra);
    if (resolvedExtra != null) intent.setDataAndType(resolvedExtra, mimeType);

    return intent;
  }

  private String getMimeType(@Nullable Uri uri) {
    if (uri != null) {
      final String mimeType = MediaUtil.getMimeType(getApplicationContext(), uri);
      if (mimeType != null) return mimeType;
    }
    return MediaUtil.getCorrectedMimeType(getIntent().getType());
  }

  @SuppressLint("StaticFieldLeak")
  private class ResolveMediaTask extends AsyncTask<Uri, Void, Uri> {
    private final Context context;

    ResolveMediaTask(Context context) {
      this.context = context;
    }

    @Override
    protected Uri doInBackground(Uri... uris) {
      try {
        if (uris.length != 1 || uris[0] == null) {
          return null;
        }

        InputStream inputStream;

        if ("file".equals(uris[0].getScheme())) {
          inputStream = openFileUri(uris[0]);
        } else {
          inputStream = context.getContentResolver().openInputStream(uris[0]);
        }

        if (inputStream == null) {
          return null;
        }

        Cursor cursor   = getContentResolver().query(uris[0], new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null);
        String fileName = null;
        Long   fileSize = null;

        try {
          if (cursor != null && cursor.moveToFirst()) {
            try {
              fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
              fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
            } catch (IllegalArgumentException e) {
              Log.w(TAG, e);
            }
          }
        } finally {
          if (cursor != null) cursor.close();
        }

        return PersistentBlobProvider.getInstance(context).create(context, inputStream, mimeType, fileName, fileSize);
      } catch (IOException ioe) {
        Log.w(TAG, ioe);
        return null;
      }
    }

    @Override
    protected void onPostExecute(Uri uri) {
      resolvedExtra = uri;
      handleResolvedMedia(getIntent(), true);
    }

    private InputStream openFileUri(Uri uri) throws IOException {
      FileInputStream fin   = new FileInputStream(uri.getPath());
      int             owner = FileUtils.getFileDescriptorOwner(fin.getFD());
      
      if (owner == -1 || owner == Process.myUid()) {
        fin.close();
        throw new IOException("File owned by application");
      }

      return fin;
    }

  }

  public static class ShareFragment
          extends Fragment
          implements LoaderManager.LoaderCallbacks<DcChatlist>, ConversationListAdapter.ItemClickListener, DcEventCenter.DcEventDelegate {


    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ConversationClickedListener conversationClickedListener;
    private Locale locale;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
      View view = inflater.inflate(R.layout.contact_selection_list_fragment, container, false);
      recyclerView  = ViewUtil.findById(view, R.id.recycler_view);
      swipeRefreshLayout  = ViewUtil.findById(view, R.id.swipe_refresh);
      recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
      return view;
    }

    @Override
    public void onCreate(Bundle bundle) {
      super.onCreate(bundle);
      getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
      super.onActivityCreated(bundle);
      initializeAdapter();
    }

    private void initializeAdapter() {
      recyclerView.setAdapter(new ConversationListAdapter(getActivity(), GlideApp.with(this),
          locale, null, this));
      getLoaderManager().restartLoader(0, null, this);
      swipeRefreshLayout.setRefreshing(false);
      swipeRefreshLayout.setEnabled(false);
    }

    @Override
    public Loader<DcChatlist> onCreateLoader(int arg0, Bundle arg1) {
      return new DcChatlistLoader(getActivity(), 0, null , 0);
    }

    @Override
    public void onLoadFinished(Loader<DcChatlist> arg0, DcChatlist chatlist) {
      ConversationListAdapter adapter = getConversationListAdapter();
      adapter.changeData(chatlist);
      adapter.notifyDataSetChanged();
    }

    private ConversationListAdapter getConversationListAdapter() {
      return (ConversationListAdapter) recyclerView.getAdapter();
    }

    @Override
    public void onLoaderReset(Loader<DcChatlist> loader) {
      ConversationListAdapter adapter = getConversationListAdapter();
      adapter.changeData(null);
      adapter.notifyDataSetChanged();
    }

    @Override
    public void handleEvent(int eventId, Object data1, Object data2) {
      if (eventId==DcContext.DC_EVENT_CONTACTS_CHANGED) {
        restartLoader();
      }
    }

    private void restartLoader() {
      getLoaderManager().restartLoader(0, null, ShareFragment.this);
    }

    @Override
    public void onItemClick(ConversationListItem item) {
      if (conversationClickedListener != null) {
        conversationClickedListener.onConversationClicked((int) item.getThreadId());
      }
    }

    @Override
    public void onItemLongClick(ConversationListItem item) {
      // Not needed
    }

    @Override
    public void onSwitchToArchive() {
      // Not needed
    }

    public void setLocale(Locale locale) {
      this.locale = locale;
    }

    public void setConversationClickedListener(ConversationClickedListener listener) {
      conversationClickedListener = listener;
    }
  }

}