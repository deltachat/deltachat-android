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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.PartAuthority;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.RelayUtil;

import java.util.ArrayList;
import java.util.List;

import static org.thoughtcrime.securesms.util.RelayUtil.setSharedText;

/**
 * An activity to quickly share content with chats
 *
 * @author Jake McGinty
 */
public class ShareActivity extends PassphraseRequiredActionBarActivity implements ResolveMediaTask.OnMediaResolvedListener
{
  private static final String TAG = ShareActivity.class.getSimpleName();

  public static final String EXTRA_CHAT_ID = "chat_id";

  private final DynamicTheme    dynamicTheme    = new DynamicNoActionBarTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private ArrayList<Uri>               resolvedExtras;
  private ApplicationDcContext         dcContext;
  private boolean                      isResolvingUrisOnMainThread;

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }

  @Override
  protected void onCreate(Bundle icicle, boolean ready) {
    dcContext = DcHelper.getContext(this);

    setContentView(R.layout.share_activity);

    initializeToolbar();
    initializeMedia();
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
    ResolveMediaTask.cancelTasks();
  }

  private void initializeToolbar() {
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    ActionBar actionBar = getSupportActionBar();

    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
  }

  private void initializeMedia() {
    resolvedExtras = new ArrayList<>();

    List<Uri> streamExtras = new ArrayList<>();
    if (Intent.ACTION_SEND.equals(getIntent().getAction()) &&
            getIntent().getParcelableExtra(Intent.EXTRA_STREAM) != null) {
        Uri uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        streamExtras.add(uri);
    } else if (getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM) != null) {
      streamExtras = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
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
      // uri may be null, however, hasFileScheme() just returns false in this case
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
    isResolvingUrisOnMainThread = true;
    for (Uri streamExtra : streamExtras) {
      if (streamExtra != null && PartAuthority.isLocalUri(streamExtra)) {
        resolvedExtras.add(streamExtra);
      } else {
        new ResolveMediaTask(this, this).execute(streamExtra);
      }
    }

    if (!ResolveMediaTask.isExecuting()) {
      handleResolvedMedia(getIntent());
    }
    isResolvingUrisOnMainThread = false;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
    case android.R.id.home:     finish();                return true;
    }
    return false;
  }

  @Override
  public void onMediaResolved(Uri uri) {
    if (uri != null) {
      resolvedExtras.add(uri);
    }

    if (!ResolveMediaTask.isExecuting() && !isResolvingUrisOnMainThread) {
     handleResolvedMedia(getIntent());
    }
  }

  private void handleResolvedMedia(Intent intent) {
    int       chatId           = intent.getIntExtra(EXTRA_CHAT_ID, -1);
    Intent composeIntent = getBaseShareIntent(ConversationListActivity.class);
    RelayUtil.setSharedUris(composeIntent, resolvedExtras);
    if (chatId != -1) {
      RelayUtil.setDirectSharing(composeIntent, chatId);
    }
    startActivity(composeIntent);
    finish();

  }

  private Intent getBaseShareIntent(final @NonNull Class<?> target) {
    final Intent intent = new Intent(this, target);
    setSharedText(intent, getIntent().getStringExtra(Intent.EXTRA_TEXT));
    if (resolvedExtras.size() > 0) {
      Uri data = resolvedExtras.get(0);
      if (data != null) {
        String mimeType = getMimeType(data);
        intent.setDataAndType(data, mimeType);
      }
    }
    return intent;
  }

  private String getMimeType(@Nullable Uri uri) {
    if (uri != null) {
      final String mimeType = MediaUtil.getMimeType(getApplicationContext(), uri);
      if (mimeType != null) return mimeType;
    }
    return MediaUtil.getCorrectedMimeType(getIntent().getType());
  }

  private boolean hasFileScheme(Uri uri) {
    if (uri==null) {
      return false;
    }
    return "file".equals(uri.getScheme());
  }

}