/*
 * Copyright (C) 2014 Open Whisper Systems
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
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMediaGalleryElement;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.components.MediaView;
import org.thoughtcrime.securesms.components.viewpager.ExtendedOnPageChangedListener;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.database.loaders.PagingMediaLoader;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.mms.Slide;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientModifiedListener;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.SaveAttachmentTask;
import org.thoughtcrime.securesms.util.SaveAttachmentTask.Attachment;
import org.thoughtcrime.securesms.util.StorageUtil;
import org.thoughtcrime.securesms.util.Util;

import java.io.IOException;
import java.util.WeakHashMap;

/**
 * Activity for displaying media attachments in-app
 */
public class MediaPreviewActivity extends PassphraseRequiredActionBarActivity
    implements RecipientModifiedListener, LoaderManager.LoaderCallbacks<DcMediaGalleryElement> {

  private final static String TAG = MediaPreviewActivity.class.getSimpleName();

  public static final String ACTIVITY_TITLE_EXTRA = "activity_title";
  public static final String EDIT_AVATAR_CHAT_ID  = "avatar_for_chat_id";
  public static final String ADDRESS_EXTRA        = "address";
  public static final String OUTGOING_EXTRA       = "outgoing";
  public static final String LEFT_IS_RECENT_EXTRA = "left_is_recent";
  public static final String DC_MSG_ID            = "dc_msg_id";
  public static final String OPENED_FROM_PROFILE  = "opened_from_profile";

  /** USE ONLY IF YOU HAVE NO MESSAGE ID! */
  public static final String DATE_EXTRA = "date";

  /** USE ONLY IF YOU HAVE NO MESSAGE ID! */
  public static final String SIZE_EXTRA = "size";

  @Nullable
  private DcMsg     messageRecord;
  private DcContext dcContext;
  private MediaItem initialMedia;
  private ViewPager mediaPager;
  private Recipient conversationRecipient;
  private boolean   leftIsRecent;

  private int restartItem = -1;

  private int editAvatarChatId = 0;

  @Override
  protected void onPreCreate() {
    dynamicTheme = new DynamicTheme() {
            public void onCreate(Activity activity) {
                activity.setTheme(R.style.TextSecure_DarkTheme); // force dark theme
            }
            public void onResume(Activity activity) {}
    };
    super.onPreCreate();
  }

  @SuppressWarnings("ConstantConditions")
  @Override
  protected void onCreate(Bundle bundle, boolean ready) {
    setFullscreenIfPossible();
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                         WindowManager.LayoutParams.FLAG_FULLSCREEN);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    setContentView(R.layout.media_preview_activity);

    editAvatarChatId = getIntent().getIntExtra(EDIT_AVATAR_CHAT_ID, 0);
    @Nullable String title = getIntent().getStringExtra(ACTIVITY_TITLE_EXTRA);
    if (title!=null) {
      getSupportActionBar().setTitle(title);
    }

    initializeViews();
    initializeResources();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }

  private void setFullscreenIfPossible() {
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
  }

  @Override
  public void onModified(Recipient recipient) {
    Util.runOnMain(this::initializeActionBar);
  }

  @SuppressWarnings("ConstantConditions")
  private void initializeActionBar() {
    MediaItem mediaItem = getCurrentMediaItem();

    if (mediaItem != null) {
      CharSequence relativeTimeSpan;

      if (mediaItem.date > 0) {
        relativeTimeSpan = DateUtils.getExtendedRelativeTimeSpanString(this, mediaItem.date);
      } else {
        relativeTimeSpan = getString(R.string.draft);
      }

      if      (mediaItem.outgoing)          getSupportActionBar().setTitle(getString(R.string.self));
      else  {
        int fromId = dcContext.getMsg(mediaItem.msgId).getFromId();
        getSupportActionBar().setTitle(dcContext.getContact(fromId).getDisplayName());
      }

      getSupportActionBar().setSubtitle(relativeTimeSpan);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    initializeMedia();
  }

  @Override
  public void onPause() {
    super.onPause();
    restartItem = cleanupMedia();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    initializeResources();
  }

  private void initializeViews() {
    mediaPager = findViewById(R.id.media_pager);
    mediaPager.setOffscreenPageLimit(1);
    mediaPager.addOnPageChangeListener(new ViewPagerListener());
  }

  private void initializeResources() {
    Address address = getIntent().getParcelableExtra(ADDRESS_EXTRA);

    final Context context = getApplicationContext();
    this.dcContext = DcHelper.getContext(context);
    final int msgId = getIntent().getIntExtra(DC_MSG_ID, DcMsg.DC_MSG_NO_ID);

    if(msgId == DcMsg.DC_MSG_NO_ID) {
      messageRecord = null;
      long date = getIntent().getLongExtra(DATE_EXTRA, 0);
      long size = getIntent().getLongExtra(SIZE_EXTRA, 0);
      initialMedia = new MediaItem(null, getIntent().getData(), null, getIntent().getType(),
          DcMsg.DC_MSG_NO_ID, date, size, false);

      if (address != null) {
        conversationRecipient = Recipient.from(context, address);
      } else {
        conversationRecipient = null;
      }
    } else {
      messageRecord = dcContext.getMsg(msgId);
      initialMedia = new MediaItem(Recipient.fromChat(context, msgId), Uri.fromFile(messageRecord.getFileAsFile()),
          messageRecord.getFilename(), messageRecord.getFilemime(), messageRecord.getId(), messageRecord.getDateReceived(),
          messageRecord.getFilebytes(), messageRecord.isOutgoing());
      conversationRecipient = Recipient.fromChat(context, msgId);
    }
    leftIsRecent     = getIntent().getBooleanExtra(LEFT_IS_RECENT_EXTRA, false);
    restartItem      = -1;

  }

  private void initializeMedia() {

    // if you search for the place where the media are loaded, go to 'onCreateLoader'.

    Log.w(TAG, "Loading Part URI: " + initialMedia);
    if (messageRecord != null) {
      getSupportLoaderManager().restartLoader(0, null, this);
    } else {
      mediaPager.setAdapter(new SingleItemPagerAdapter(this, GlideApp.with(this),
          getWindow(), initialMedia.uri, initialMedia.name, initialMedia.type, initialMedia.size));
    }
  }

  private int cleanupMedia() {
    int restartItem = mediaPager.getCurrentItem();

    mediaPager.removeAllViews();
    mediaPager.setAdapter(null);

    return restartItem;
  }

  private void editAvatar() {
    Intent intent = new Intent(this, GroupCreateActivity.class);
    intent.putExtra(GroupCreateActivity.EDIT_GROUP_CHAT_ID, editAvatarChatId);
    startActivity(intent);
    finish(); // avoid the need to update the enlarged-avatar
  }


  private void showOverview() {
    if (getIntent().getBooleanExtra(OPENED_FROM_PROFILE, false)) {
      finish();
    }
    else if(conversationRecipient.getAddress().isDcChat()) {
      Intent intent = new Intent(this, AllMediaActivity.class);
      intent.putExtra(AllMediaActivity.CHAT_ID_EXTRA, conversationRecipient.getAddress().getDcChatId());
      intent.putExtra(AllMediaActivity.FORCE_GALLERY, true);
      startActivity(intent);
      finish();
    }
    else if(conversationRecipient.getAddress().isDcContact()) {
      Intent intent = new Intent(this, AllMediaActivity.class);
      intent.putExtra(AllMediaActivity.CONTACT_ID_EXTRA, conversationRecipient.getAddress().getDcContactId());
      intent.putExtra(AllMediaActivity.FORCE_GALLERY, true);
      startActivity(intent);
      finish();
    }
  }

  private void share() {
    MediaItem mediaItem = getCurrentMediaItem();
    if (mediaItem != null) {
      DcHelper.openForViewOrShare(this, mediaItem.msgId, Intent.ACTION_SEND);
    }
  }

  @SuppressWarnings("CodeBlock2Expr")
  @SuppressLint("InlinedApi")
  private void saveToDisk() {
    MediaItem mediaItem = getCurrentMediaItem();

    if (mediaItem != null) {
      SaveAttachmentTask.showWarningDialog(this, (dialogInterface, i) -> {
        if (StorageUtil.canWriteToMediaStore(this)) {
          performSavetoDisk(mediaItem);
          return;
        }

        Permissions.with(this)
                   .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                   .alwaysGrantOnSdk30()
                   .ifNecessary()
                   .withPermanentDenialDialog(getString(R.string.perm_explain_access_to_storage_denied))
                   .onAllGranted(() -> {
                     performSavetoDisk(mediaItem);
                   })
                   .execute();
      });
    }
  }

  private void performSavetoDisk(@NonNull MediaItem mediaItem) {
    SaveAttachmentTask saveTask = new SaveAttachmentTask(MediaPreviewActivity.this);
    long               saveDate = (mediaItem.date > 0) ? mediaItem.date : System.currentTimeMillis();
    saveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Attachment(mediaItem.uri, mediaItem.type, saveDate, mediaItem.name));
  }

  private void showInChat() {
    MediaItem mediaItem = getCurrentMediaItem();
    if (mediaItem == null || mediaItem.msgId == DcMsg.DC_MSG_NO_ID) {
      Log.w(TAG, "mediaItem missing.");
      return;
    }

    DcMsg dcMsg = dcContext.getMsg(mediaItem.msgId);
    if (dcMsg.getId() == DcMsg.DC_MSG_NO_ID) {
      Log.w(TAG, "cannot get message object.");
      return;
    }

    Intent intent = new Intent(this, ConversationActivity.class);
    intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, dcMsg.getChatId());
    intent.putExtra(ConversationActivity.STARTING_POSITION_EXTRA, DcMsg.getMessagePosition(dcMsg, dcContext));
    startActivity(intent);
  }

  @SuppressLint("StaticFieldLeak")
  private void deleteMedia() {
    MediaItem mediaItem = getCurrentMediaItem();
    if (mediaItem == null || mediaItem.msgId == DcMsg.DC_MSG_NO_ID) {
      return;
    }

    DcMsg dcMsg = dcContext.getMsg(mediaItem.msgId);
    DcChat dcChat = dcContext.getChat(dcMsg.getChatId());

    String text = getResources().getQuantityString(R.plurals.ask_delete_messages,1, 1);
    int positiveBtnLabel = dcChat.isSelfTalk() ? R.string.delete : R.string.delete_for_me;
    final int[] messageIds = new int[]{mediaItem.msgId};

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(text);
    builder.setCancelable(true);
    builder.setNeutralButton(android.R.string.cancel, null);
    builder.setPositiveButton(positiveBtnLabel, (dialogInterface, which) -> {
      Util.runOnAnyBackgroundThread(() -> dcContext.deleteMsgs(messageIds));
      finish();
    });

    if(dcChat.isEncrypted() && dcChat.canSend() && !dcChat.isSelfTalk() && dcMsg.isOutgoing()) {
      builder.setNegativeButton(R.string.delete_for_everyone, (d, which) -> {
        Util.runOnAnyBackgroundThread(() -> dcContext.sendDeleteRequest(messageIds));
        finish();
      });
      AlertDialog dialog = builder.show();
      Util.redButton(dialog, AlertDialog.BUTTON_NEGATIVE);
      Util.redPositiveButton(dialog);
    } else {
      AlertDialog dialog = builder.show();
      Util.redPositiveButton(dialog);
    }
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);

    menu.clear();
    MenuInflater inflater = this.getMenuInflater();
    inflater.inflate(R.menu.media_preview, menu);
    Util.redMenuItem(menu, R.id.delete);

    if (!isMediaInDb()) {
      menu.findItem(R.id.media_preview__overview).setVisible(false);
      menu.findItem(R.id.media_preview__share).setVisible(false);
      menu.findItem(R.id.delete).setVisible(false);
      menu.findItem(R.id.show_in_chat).setVisible(false);
    }

    if (editAvatarChatId==0) {
        menu.findItem(R.id.media_preview__edit).setVisible(false);
    }

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    int itemId = item.getItemId();
    if (itemId == R.id.media_preview__edit) {
      editAvatar();
      return true;
    } else if (itemId == R.id.media_preview__overview) {
      showOverview();
      return true;
    } else if (itemId == R.id.media_preview__share) {
      share();
      return true;
    } else if (itemId == R.id.save) {
      saveToDisk();
      return true;
    } else if (itemId == R.id.delete) {
      deleteMedia();
      return true;
    } else if (itemId == R.id.show_in_chat) {
      showInChat();
      return true;
    } else if (itemId == android.R.id.home) {
      finish();
      return true;
    }

    return false;
  }

  private boolean isMediaInDb() {
    return conversationRecipient != null;
  }

  private @Nullable MediaItem getCurrentMediaItem() {
    MediaItemAdapter adapter = (MediaItemAdapter)mediaPager.getAdapter();

    if (adapter != null) {
      return adapter.getMediaItemFor(mediaPager.getCurrentItem());
    } else {
      return null;
    }
  }

  public static boolean isTypeSupported(final Slide slide) {
    return slide != null && (slide.hasVideo() || slide.hasImage());
  }

  @Override
  public Loader<DcMediaGalleryElement> onCreateLoader(int id, Bundle args) {
    return new PagingMediaLoader(this, messageRecord, false);
  }

  @Override
  public void onLoadFinished(Loader<DcMediaGalleryElement> loader, @Nullable DcMediaGalleryElement data) {
    if (data != null) {
      @SuppressWarnings("ConstantConditions")
      DcMediaPagerAdapter adapter = new DcMediaPagerAdapter(this, GlideApp.with(this),
          getWindow(), data, leftIsRecent);
      mediaPager.setAdapter(adapter);
      adapter.setActive(true);

      if (restartItem < 0) mediaPager.setCurrentItem(data.getPosition());
      else                 mediaPager.setCurrentItem(restartItem);
    }
  }

  @Override
  public void onLoaderReset(Loader<DcMediaGalleryElement> loader) {

  }

  private class ViewPagerListener extends ExtendedOnPageChangedListener {

    @Override
    public void onPageSelected(int position) {
      super.onPageSelected(position);

      MediaItemAdapter adapter = (MediaItemAdapter)mediaPager.getAdapter();

      if (adapter != null) {
        MediaItem item = adapter.getMediaItemFor(position);
        if (item.recipient != null) item.recipient.addListener(MediaPreviewActivity.this);

        initializeActionBar();
      }
    }


    @Override
    public void onPageUnselected(int position) {
      MediaItemAdapter adapter = (MediaItemAdapter)mediaPager.getAdapter();

      if (adapter != null) {
        try {
          MediaItem item = adapter.getMediaItemFor(position);
          if (item.recipient != null) item.recipient.removeListener(MediaPreviewActivity.this);
        } catch (IllegalArgumentException e) {
          Log.w(TAG, "Ignoring invalid position index");
        }
        adapter.pause(position);
      }
    }
  }

  private static class SingleItemPagerAdapter extends PagerAdapter implements MediaItemAdapter {

    private final GlideRequests glideRequests;
    private final Window        window;
    private final Uri           uri;
    private final String        name;
    private final String        mediaType;
    private final long          size;

    private final LayoutInflater inflater;

    SingleItemPagerAdapter(@NonNull Context context, @NonNull GlideRequests glideRequests,
                           @NonNull Window window, @NonNull Uri uri, @Nullable String name, @NonNull String mediaType,
                           long size)
    {
      this.glideRequests = glideRequests;
      this.window        = window;
      this.uri           = uri;
      this.name          = name;
      this.mediaType     = mediaType;
      this.size          = size;
      this.inflater      = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
      return 1;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
      return view == object;
    }

    @Override
    public @NonNull Object instantiateItem(@NonNull ViewGroup container, int position) {
      View      itemView  = inflater.inflate(R.layout.media_view_page, container, false);
      MediaView mediaView = itemView.findViewById(R.id.media_view);

      try {
        mediaView.set(glideRequests, window, uri, name, mediaType, size, true);
      } catch (IOException e) {
        Log.w(TAG, e);
      }

      container.addView(itemView);

      return itemView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
      MediaView mediaView = ((FrameLayout)object).findViewById(R.id.media_view);
      mediaView.cleanup();

      container.removeView((FrameLayout)object);
    }

    @Override
    public MediaItem getMediaItemFor(int position) {
      return new MediaItem(null, uri, name, mediaType, DcMsg.DC_MSG_NO_ID, -1, -1, true);
    }

    @Override
    public void pause(int position) {

    }
  }

  private static class DcMediaPagerAdapter extends PagerAdapter implements MediaItemAdapter {

    private final WeakHashMap<Integer, MediaView> mediaViews = new WeakHashMap<>();

    private final Context       context;
    private final GlideRequests glideRequests;
    private final Window        window;
    private final DcMediaGalleryElement gallery;
    private final boolean       leftIsRecent;

    private boolean active;
    private int     autoPlayPosition;

    DcMediaPagerAdapter(@NonNull Context context, @NonNull GlideRequests glideRequests,
                       @NonNull Window window, @NonNull DcMediaGalleryElement gallery,
                       boolean leftIsRecent)
    {
      this.context          = context.getApplicationContext();
      this.glideRequests    = glideRequests;
      this.window           = window;
      this.gallery          = gallery;
      this.leftIsRecent     = leftIsRecent;
      this.autoPlayPosition = gallery.getPosition();
    }

    public void setActive(boolean active) {
      this.active = active;
      notifyDataSetChanged();
    }

    @Override
    public int getCount() {
      if (!active) return 0;
      else         return gallery.getCount();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
      return view == object;
    }

    @Override
    public @NonNull Object instantiateItem(@NonNull ViewGroup container, int position) {
      View      itemView       = LayoutInflater.from(context).inflate(R.layout.media_view_page, container, false);
      MediaView mediaView      = itemView.findViewById(R.id.media_view);
      boolean   autoplay       = position == autoPlayPosition;
      int       cursorPosition = getCursorPosition(position);

      autoPlayPosition = -1;

      gallery.moveToPosition(cursorPosition);

      DcMsg msg = gallery.getMessage();

      try {
        //noinspection ConstantConditions
        mediaView.set(glideRequests, window, Uri.fromFile(msg.getFileAsFile()), msg.getFilename(),
            msg.getFilemime(), msg.getFilebytes(), autoplay);
      } catch (IOException e) {
        Log.w(TAG, e);
      }

      mediaViews.put(position, mediaView);
      container.addView(itemView);

      return itemView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
      MediaView mediaView = ((FrameLayout)object).findViewById(R.id.media_view);
      mediaView.cleanup();

      mediaViews.remove(position);
      container.removeView((FrameLayout)object);
    }

    public MediaItem getMediaItemFor(int position) {
      gallery.moveToPosition(getCursorPosition(position));
      DcMsg msg               = gallery.getMessage();

      if (msg.getFile() == null) throw new AssertionError();

      return new MediaItem(Recipient.fromChat(context, msg.getId()),
                           Uri.fromFile(msg.getFileAsFile()),
                           msg.getFilename(),
                           msg.getFilemime(),
                           msg.getId(),
                           msg.getDateReceived(),
                           msg.getFilebytes(),
                           msg.isOutgoing());
    }

    @Override
    public void pause(int position) {
      MediaView mediaView = mediaViews.get(position);
      if (mediaView != null) mediaView.pause();
    }

    private int getCursorPosition(int position) {
      if (leftIsRecent) return position;
      else              return gallery.getCount() - 1 - position;
    }
  }

  private static class MediaItem {
    private final @Nullable Recipient          recipient;
    private final @NonNull  Uri                uri;
    private final @Nullable String             name;
    private final @NonNull  String             type;
    private final           int                msgId;
    private final           long               date;
    private final           long               size;
    private final           boolean            outgoing;

    private MediaItem(@Nullable Recipient recipient,
                      @NonNull Uri uri,
                      @Nullable String name,
                      @NonNull String type,
                      int msgId,
                      long date,
                      long size,
                      boolean outgoing)
    {
      this.recipient  = recipient;
      this.uri        = uri;
      this.name       = name;
      this.type       = type;
      this.msgId      = msgId;
      this.date       = date;
      this.size       = size;
      this.outgoing   = outgoing;
    }
  }

  interface MediaItemAdapter {
    MediaItem getMediaItemFor(int position);
    void pause(int position);
  }
}
