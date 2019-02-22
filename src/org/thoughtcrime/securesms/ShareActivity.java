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

import android.Manifest;
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
import android.widget.Toast;

import com.b44t.messenger.DcChatlist;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEventCenter;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcChatlistLoader;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.mms.PartAuthority;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.FileUtils;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
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
  private List<Uri>                    resolvedExtras;
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
  protected void onDestroy() {
    super.onDestroy();
    if (!isPassingAlongMedia && resolvedExtras.size() != 0) {
        for(Uri uri : resolvedExtras) {
          if (uri != null) {
            PersistentBlobProvider.getInstance(this).delete(this, uri);
          }
        }
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
    isPassingAlongMedia = false;
    resolvedExtras = new ArrayList<>();

    List<Uri> streamExtras = new ArrayList<>();
    if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
      streamExtras.add(getIntent().getParcelableExtra(Intent.EXTRA_STREAM));
    } else {
      streamExtras = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
    }
    if (streamExtras == null || streamExtras.isEmpty()) {
      progressWheel.setVisibility(View.GONE);
      return;
    }
    if (needsFilePermission(streamExtras)) {
      if (Permissions.hasAll(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)) {
        resolveUris(streamExtras);
      } else {
        requestPermissionForFiles(streamExtras);
      }
    } else {
      resolveUris(streamExtras);
    }
  }

  private boolean needsFilePermission(List<Uri> uris) {
    for(Uri uri : uris) {
      if (hasFileScheme(uri)) {
        return true;
      }
    }
    return false;
  }

  private void requestPermissionForFiles(List<Uri> streamExtras) {
    Permissions.with(this)
            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
            .ifNecessary()
            .withRationaleDialog(this.getString(R.string.perm_explain_need_for_storage_access_share), R.drawable.ic_folder_white_48dp)
            .onAllGranted(() -> resolveUris(streamExtras))
            .onAnyDenied(this::abortShare)
            .execute();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }

  private void abortShare() {
    Toast.makeText(this, R.string.share_abort, Toast.LENGTH_LONG).show();
    finish();
  }

  private void resolveUris(List<Uri> streamExtras) {
    for (Uri streamExtra : streamExtras) {
      if (streamExtra != null && PartAuthority.isLocalUri(streamExtra)) {
        isPassingAlongMedia = true;
        resolvedExtras.add(streamExtra);
        handleResolvedMedia(getIntent(), false);
      } else {
        shareFragment.getView().setVisibility(View.GONE);
        progressWheel.setVisibility(View.VISIBLE);
        new ResolveMediaTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, streamExtra);
      }
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
    if (resolvedExtras.size() > 1) {
      String message = String.format(getString(R.string.share_multiple_attachments), resolvedExtras.size());
      new AlertDialog.Builder(this)
              .setMessage(message)
              .setCancelable(true)
              .setNegativeButton(android.R.string.cancel, null)
              .setPositiveButton(R.string.menu_send, (dialog, which) -> sendMultipleAttachmentsAndCreateConversation(threadId)).show();
    } else {
      openConversation(threadId);
    }
  }

  private void sendMultipleAttachmentsAndCreateConversation(int threadId) {
    for(Uri uri : resolvedExtras) {
      DcMsg message = createMessage(uri);
      dcContext.sendMsg(threadId, message);
    }
    openConversation(threadId);
  }

  private void openConversation(int threadId) {
      final Intent intent = getBaseShareIntent(ConversationActivity.class);
      intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, threadId);
      isPassingAlongMedia = true;
      startActivity(intent);
  }

  private DcMsg createMessage(Uri uri) {
    DcMsg message;
    String mimeType = MediaUtil.getMimeType(this, uri);
    if (MediaUtil.isImageType(mimeType)) {
      message = new DcMsg(dcContext, DcMsg.DC_MSG_IMAGE);
    }
    else if (MediaUtil.isAudioType(mimeType)) {
      message = new DcMsg(dcContext,DcMsg.DC_MSG_AUDIO);
    }
    else if (MediaUtil.isVideoType(mimeType)) {
      message = new DcMsg(dcContext, DcMsg.DC_MSG_VIDEO);
    }
    else {
      message = new DcMsg(dcContext, DcMsg.DC_MSG_FILE);
    }
    message.setFile(getRealPathFromUri(uri), mimeType);
    return message;
  }

  private String getRealPathFromUri(Uri uri) {
    try {
      String filename = uri.getPathSegments().get(2); // Get real file name from Uri
      String ext = "";
      int i = filename.lastIndexOf(".");
      if(i>=0) {
        ext = filename.substring(i);
        filename = filename.substring(0, i);
      }
      String path = dcContext.getBlobdirFile(filename, ext);

      // copy content to this file
      if(path!=null) {
        InputStream inputStream = PartAuthority.getAttachmentStream(this, uri);
        OutputStream outputStream = new FileOutputStream(path);
        Util.copy(inputStream, outputStream);
      }

      return path;
    }
    catch(Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private Intent getBaseShareIntent(final @NonNull Class<?> target) {
    if (resolvedExtras.size() == 1) {
      final Intent intent = new Intent(this, target);
      final String textExtra = getIntent().getStringExtra(Intent.EXTRA_TEXT);
      intent.putExtra(ConversationActivity.TEXT_EXTRA, textExtra);
      Uri data = resolvedExtras.get(0);
      if (data != null) {
        String mimeType = getMimeType(data);
        intent.setDataAndType(data, mimeType);
      }
      return intent;
    }
    return new Intent(this, target);
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
        Uri uri = uris[0];
        if (uris.length != 1 || uri == null) {
          return null;
        }

        InputStream inputStream;
        String fileName = null;
        Long   fileSize = null;

        if (hasFileScheme(uri)) {
          inputStream = openFileUri(uri);
          if (uri.getPath() != null) {
            File file = new File(uri.getPath());
            fileName = file.getName();
            fileSize = file.length();
          }
        } else {
          inputStream = context.getContentResolver().openInputStream(uri);
        }

        if (inputStream == null) {
          return null;
        }

        Cursor cursor   = getContentResolver().query(uri, new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null);

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
        String mimeType = getMimeType(uri);
        return PersistentBlobProvider.getInstance(context).create(context, inputStream, mimeType, fileName, fileSize);
      } catch (IOException ioe) {
        Log.w(TAG, ioe);
        return null;
      }
    }

    @Override
    protected void onPostExecute(Uri uri) {
      resolvedExtras.add(uri);
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

  private boolean hasFileScheme(Uri uri) {
    return "file".equals(uri.getScheme());
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