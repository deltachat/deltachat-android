/*
 * Copyright (C) 2011 Whisper Systems
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

import static org.thoughtcrime.securesms.TransportOption.Type;
import static org.thoughtcrime.securesms.util.ShareUtil.getSharedText;
import static org.thoughtcrime.securesms.util.ShareUtil.isForwarding;
import static org.thoughtcrime.securesms.util.ShareUtil.isSharing;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Browser;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.attachments.Attachment;
import org.thoughtcrime.securesms.attachments.UriAttachment;
import org.thoughtcrime.securesms.audio.AudioRecorder;
import org.thoughtcrime.securesms.audio.AudioSlidePlayer;
import org.thoughtcrime.securesms.components.AnimatingToggle;
import org.thoughtcrime.securesms.components.AttachmentTypeSelector;
import org.thoughtcrime.securesms.components.ComposeText;
import org.thoughtcrime.securesms.components.HidingLinearLayout;
import org.thoughtcrime.securesms.components.InputAwareLayout;
import org.thoughtcrime.securesms.components.InputPanel;
import org.thoughtcrime.securesms.components.KeyboardAwareLinearLayout.OnKeyboardShownListener;
import org.thoughtcrime.securesms.components.ScaleStableImageView;
import org.thoughtcrime.securesms.components.SendButton;
import org.thoughtcrime.securesms.components.emoji.MediaKeyboard;
import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.connect.DirectShareUtil;
import org.thoughtcrime.securesms.database.AttachmentDatabase;
import org.thoughtcrime.securesms.messagerequests.MessageRequestsBottomView;
import org.thoughtcrime.securesms.mms.AttachmentManager;
import org.thoughtcrime.securesms.mms.AttachmentManager.MediaType;
import org.thoughtcrime.securesms.mms.AudioSlide;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.mms.QuoteModel;
import org.thoughtcrime.securesms.mms.SlideDeck;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.scribbles.ScribbleActivity;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ShareUtil;
import org.thoughtcrime.securesms.util.SendRelayedMessageUtil;
import org.thoughtcrime.securesms.util.ServiceUtil;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.concurrent.AssertedSuccessListener;
import org.thoughtcrime.securesms.util.guava.Optional;
import org.thoughtcrime.securesms.util.views.ProgressDialog;
import org.thoughtcrime.securesms.video.recode.VideoRecoder;
import org.thoughtcrime.securesms.calls.CallUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;
import chat.delta.util.ListenableFuture;
import chat.delta.util.SettableFuture;

/**
 * Activity for displaying a message thread, as well as
 * composing/sending a new message into that thread.
 *
 * @author Moxie Marlinspike
 *
 */
@SuppressLint("StaticFieldLeak")
public class ConversationActivity extends PassphraseRequiredActionBarActivity
    implements ConversationFragment.ConversationFragmentListener,
               AttachmentManager.AttachmentListener,
               SearchView.OnQueryTextListener,
               DcEventCenter.DcEventDelegate,
               OnKeyboardShownListener,
               InputPanel.Listener,
               InputPanel.MediaListener
{
  private static final String TAG = ConversationActivity.class.getSimpleName();

  public static final String ACCOUNT_ID_EXTRA        = "account_id";
  public static final String CHAT_ID_EXTRA           = "chat_id";
  public static final String FROM_ARCHIVED_CHATS_EXTRA = "from_archived";
  public static final String TEXT_EXTRA              = "draft_text";
  public static final String STARTING_POSITION_EXTRA = "starting_position";

  private static final int PICK_GALLERY        = 1;
  private static final int PICK_DOCUMENT       = 2;
  private static final int PICK_CONTACT        = 4;
  private static final int GROUP_EDIT          = 6;
  private static final int TAKE_PHOTO          = 7;
  private static final int RECORD_VIDEO        = 8;
  private static final int PICK_WEBXDC         = 9;

  private   GlideRequests               glideRequests;
  protected ComposeText                 composeText;
  private   AnimatingToggle             buttonToggle;
  private   SendButton                  sendButton;
  private   ImageButton                 attachButton;
  protected ConversationTitleView       titleView;
  private   ConversationFragment        fragment;
  private   InputAwareLayout            container;
  private   View                        composePanel;
  private   ScaleStableImageView        backgroundView;
  private   MessageRequestsBottomView   messageRequestBottomView;
  private   ProgressDialog              progressDialog;

  private   AttachmentTypeSelector attachmentTypeSelector;
  private   AttachmentManager      attachmentManager;
  private   AudioRecorder          audioRecorder;
  private   FrameLayout            emojiPickerContainer;
  private   MediaKeyboard          emojiPicker;
  protected HidingLinearLayout     quickAttachmentToggle;
  private   InputPanel             inputPanel;

  private ApplicationContext context;
  private Recipient  recipient;
  private DcContext  dcContext;
  private Rpc rpc;
  private DcChat     dcChat                = new DcChat(0, 0);
  private int        chatId;
  private final boolean isSecureText          = true;
  private boolean    isDefaultSms             = true;
  private boolean    isSecurityInitialized    = false;
  private boolean successfulForwardingAttempt = false;
  private boolean isEditing = false;

  @Override
  protected void onCreate(Bundle state, boolean ready) {
    this.context = ApplicationContext.getInstance(getApplicationContext());
    this.dcContext = DcHelper.getContext(context);
    this.rpc = DcHelper.getRpc(context);

    supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
    setContentView(R.layout.conversation_activity);

    TypedArray typedArray = obtainStyledAttributes(new int[] {R.attr.conversation_background});
    int color = typedArray.getColor(0, Color.WHITE);
    typedArray.recycle();

    getWindow().getDecorView().setBackgroundColor(color);

    fragment = initFragment(R.id.fragment_content, new ConversationFragment());

    initializeActionBar();
    initializeViews();
    initializeResources();
    initializeSecurity(false, isDefaultSms).addListener(new AssertedSuccessListener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
        initializeDraft().addListener(new AssertedSuccessListener<Boolean>() {
          @Override
          public void onSuccess(Boolean result) {
            if (result != null && result) {
              Util.runOnMain(() -> {
                if (fragment != null && fragment.isResumed()) {
                  fragment.moveToLastSeen();
                } else {
                  Log.w(TAG, "Wanted to move to the last seen position, but the fragment was in an invalid state");
                }
              });
            }
          }
        });
      }
    });

    DcEventCenter eventCenter = DcHelper.getEventCenter(this);
    eventCenter.addObserver(DcContext.DC_EVENT_CHAT_MODIFIED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_CHAT_EPHEMERAL_TIMER_MODIFIED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_CONTACTS_CHANGED, this);

    if (!isMultiUser()) {
      eventCenter.addObserver(DcContext.DC_EVENT_INCOMING_MSG, this);
      eventCenter.addObserver(DcContext.DC_EVENT_MSG_READ, this);
    }

    handleRelaying();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);

    if (isFinishing()) {
      return;
    }

    if (!Util.isEmpty(composeText) || attachmentManager.isAttachmentPresent()) {
      processComposeControls(ACTION_SAVE_DRAFT);
      attachmentManager.clear(glideRequests, false);
      composeText.setText("");
    }

    setIntent(intent);
    initializeResources();
    initializeSecurity(false, isDefaultSms).addListener(new AssertedSuccessListener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
        initializeDraft();
      }
    });

    handleRelaying();

    if (fragment != null) {
      fragment.onNewIntent();
    }
  }

  private void handleRelaying() {
    if (isForwarding(this)) {
      handleForwarding();
    } else if (isSharing(this)) {
      handleSharing();
    }

    ConversationListRelayingActivity.finishActivity();
  }

  @Override
  protected void onResume() {
    super.onResume();

    initializeEnabledCheck();
    composeText.setTransport(sendButton.getSelectedTransport());

    titleView.setTitle(glideRequests, dcChat);

    DcHelper.getNotificationCenter(this).updateVisibleChat(dcContext.getAccountId(), chatId);

    attachmentManager.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();

    processComposeControls(ACTION_SAVE_DRAFT);

    DcHelper.getNotificationCenter(this).clearVisibleChat();
    if (isFinishing()) overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_right);
    inputPanel.onPause();
    AudioSlidePlayer.stopAll();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    Log.i(TAG, "onConfigurationChanged(" + newConfig.orientation + ")");
    super.onConfigurationChanged(newConfig);
    composeText.setTransport(sendButton.getSelectedTransport());

    if (emojiPicker != null && container.getCurrentInput() == emojiPicker) {
      container.hideAttachedInput(true);
    }

    emojiPicker = null; // force reloading next time onEmojiToggle() is called
    initializeBackground();
  }

  @Override
  protected void onDestroy() {
    DcHelper.getEventCenter(this).removeObservers(this);
    super.onDestroy();
  }

  @Override
  public void onActivityResult(final int reqCode, int resultCode, Intent data) {
    super.onActivityResult(reqCode, resultCode, data);

    if (resultCode != RESULT_OK || (data == null && reqCode != TAKE_PHOTO && reqCode != RECORD_VIDEO))
    {
      return;
    }

    switch (reqCode) {
    case PICK_GALLERY:
      final Uri singleUri = data.getData();
      if (singleUri != null) {
        MediaType mediaType;
        String mimeType = MediaUtil.getMimeType(this, singleUri);
             if (MediaUtil.isGif(mimeType))   mediaType = MediaType.GIF;
        else if (MediaUtil.isVideo(mimeType)) mediaType = MediaType.VIDEO;
        else                                  mediaType = MediaType.IMAGE;
        setMedia(singleUri, mediaType);
      } else {
        final ClipData multipleUris = data.getClipData();
        if (multipleUris != null) {
          final int uriCount = multipleUris.getItemCount();
          if (uriCount > 0) {
            ArrayList<Uri> uriList = new ArrayList<>(uriCount);
            for (int i = 0; i < uriCount; i++) {
              uriList.add(multipleUris.getItemAt(i).getUri());
            }
            askSendingFiles(uriList, () -> {
              Util.runOnAnyBackgroundThread(() -> {
                SendRelayedMessageUtil.sendMultipleMsgs(this, chatId, uriList, null);
              });
            });
          }
        }
      }
      break;

    case PICK_DOCUMENT:
      final String docMimeType = MediaUtil.getMimeType(this, data.getData());
      final MediaType docMediaType = MediaUtil.isAudioType(docMimeType) ? MediaType.AUDIO : MediaType.DOCUMENT;
      setMedia(data.getData(), docMediaType);
      break;

    case PICK_WEBXDC:
      setMedia(data.getData(), MediaType.DOCUMENT);
      break;

    case PICK_CONTACT:
      addAttachmentContactInfo(data.getIntExtra(AttachContactActivity.CONTACT_ID_EXTRA, 0));
      break;

    case GROUP_EDIT:
      dcChat = dcContext.getChat(chatId);
      titleView.setTitle(glideRequests, dcChat);
      break;

    case TAKE_PHOTO:
      if (attachmentManager.getImageCaptureUri() != null) {
        setMedia(attachmentManager.getImageCaptureUri(), MediaType.IMAGE);
      }
      break;

    case RECORD_VIDEO:
      Uri uri = null;
      if (data!=null) { uri = data.getData(); }
      if (uri==null) { uri = attachmentManager.getVideoCaptureUri(); }
      if (uri!=null) {
        setMedia(uri, MediaType.VIDEO);
      }
      else {
        Toast.makeText(this, "No video returned from system", Toast.LENGTH_LONG).show();
      }
      break;

    case ScribbleActivity.SCRIBBLE_REQUEST_CODE:
      setMedia(data.getData(), MediaType.IMAGE);
      break;
    }
  }

  @Override
  public void startActivity(Intent intent) {
    if (intent.getStringExtra(Browser.EXTRA_APPLICATION_ID) != null) {
      intent.removeExtra(Browser.EXTRA_APPLICATION_ID);
    }

    try {
      super.startActivity(intent);
    } catch (ActivityNotFoundException e) {
      Log.w(TAG, e);
      Toast.makeText(this, R.string.no_app_to_handle_data, Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menu.clear();

    getMenuInflater().inflate(R.menu.conversation, menu);

    if (dcChat.isSelfTalk() || dcChat.isOutBroadcast()) {
      menu.findItem(R.id.menu_mute_notifications).setVisible(false);
    } else if(dcChat.isMuted()) {
      menu.findItem(R.id.menu_mute_notifications).setTitle(R.string.menu_unmute);
    }

    if (!Prefs.isLocationStreamingEnabled(this)) {
      menu.findItem(R.id.menu_show_map).setVisible(false);
    }

    menu.findItem(R.id.menu_start_call).setVisible(
      Prefs.isCallsEnabled(this)
      && dcChat.canSend()
      && dcChat.isEncrypted()
      && !dcChat.isSelfTalk()
      && !dcChat.isMultiUser()
    );

    if (!dcChat.isEncrypted() || !dcChat.canSend() || dcChat.isMailingList() ) {
      menu.findItem(R.id.menu_ephemeral_messages).setVisible(false);
    }

    if (isMultiUser()) {
      if (dcChat.isInBroadcast() && !dcChat.isContactRequest()) {
        menu.findItem(R.id.menu_leave).setTitle(R.string.menu_leave_channel).setVisible(true);
      } else if (dcChat.isEncrypted()
          && dcChat.canSend()
          && !dcChat.isOutBroadcast()
          && !dcChat.isMailingList()) {
        menu.findItem(R.id.menu_leave).setVisible(true);
      }
    }

    if (isArchived()) {
      menu.findItem(R.id.menu_archive_chat).setTitle(R.string.menu_unarchive_chat);
    }


    Util.redMenuItem(menu, R.id.menu_leave);
    Util.redMenuItem(menu, R.id.menu_clear_chat);
    Util.redMenuItem(menu, R.id.menu_delete_chat);

    try {
      MenuItem searchItem = menu.findItem(R.id.menu_search_chat);
      searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
        @Override
        public boolean onMenuItemActionExpand(final MenuItem item) {
          searchExpand(menu, item);
          return true;
        }

        @Override
        public boolean onMenuItemActionCollapse(final MenuItem item) {
          searchCollapse(menu, item);
          return true;
        }
      });
      SearchView searchView = (SearchView) searchItem.getActionView();
      searchView.setOnQueryTextListener(this);
      searchView.setQueryHint(getString(R.string.search));
      searchView.setIconifiedByDefault(true);

      // hide the [X] beside the search field - this is too much noise, search can be aborted eg. by "back"
      ImageView closeBtn = searchView.findViewById(R.id.search_close_btn);
      if (closeBtn!=null) {
        closeBtn.setEnabled(false);
        closeBtn.setImageDrawable(null);
      }
    } catch (Exception e) {
      Log.e(TAG, "cannot set up in-chat-search: ", e);
    }

    if (!dcChat.canSend() || isEditing) {
      MenuItem attachItem =  menu.findItem(R.id.menu_add_attachment);
      if (attachItem!=null) {
        attachItem.setVisible(false);
      }
    }

    super.onPrepareOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    int itemId = item.getItemId();
    if (itemId == R.id.menu_add_attachment) {
      handleAddAttachment();
      return true;
    } else if (itemId == R.id.menu_leave) {
      handleLeaveGroup();
      return true;
    } else if (itemId == R.id.menu_archive_chat) {
      handleArchiveChat();
      return true;
    } else if (itemId == R.id.menu_clear_chat) {
      fragment.handleClearChat();
      return true;
    } else if (itemId == R.id.menu_delete_chat) {
      handleDeleteChat();
      return true;
    } else if (itemId == R.id.menu_mute_notifications) {
      handleMuteNotifications();
      return true;
    } else if (itemId == R.id.menu_show_map) {
      WebxdcActivity.openMaps(this, chatId);
      return true;
    } else if (itemId == R.id.menu_start_call) {
      CallUtil.startCall(this, chatId);
      return true;
    } else if (itemId == R.id.menu_all_media) {
      handleAllMedia();
      return true;
    } else if (itemId == R.id.menu_search_up) {
      handleMenuSearchNext(false);
      return true;
    } else if (itemId == R.id.menu_search_down) {
      handleMenuSearchNext(true);
      return true;
    } else if (itemId == android.R.id.home) {
      handleReturnToConversationList();
      return true;
    } else if (itemId == R.id.menu_ephemeral_messages) {
      handleEphemeralMessages();
      return true;
    }

    return false;
  }

  @Override
  public void onBackPressed() {
    if (container.isInputOpen()){
      container.hideCurrentInput(composeText);
    } else {
      handleReturnToConversationList();
    }
  }

  @Override
  public void onKeyboardShown() {
    inputPanel.onKeyboardShown();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }

  public void setDraftText(String txt) {
    composeText.setText(txt);
    composeText.setSelection(composeText.getText().length());
  }

  public void hideSoftKeyboard() {
    container.hideCurrentInput(composeText);
  }

  //////// Event Handlers

  private void handleEphemeralMessages() {
      int preselected = dcContext.getChatEphemeralTimer(chatId);
      EphemeralMessagesDialog.show(this, preselected, duration -> {
        dcContext.setChatEphemeralTimer(chatId, (int) duration);
      });
  }

  private void handleReturnToConversationList() {
    handleReturnToConversationList(null);
  }

  private void handleReturnToConversationList(@Nullable Bundle extras) {
    boolean archived = getIntent().getBooleanExtra(FROM_ARCHIVED_CHATS_EXTRA, false);
    Intent intent = new Intent(this, (archived ? ConversationListArchiveActivity.class : ConversationListActivity.class));
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    if (extras != null) intent.putExtras(extras);
    startActivity(intent);
    finish();
  }

  private void handleMuteNotifications() {
    if(!dcChat.isMuted()) {
      MuteDialog.show(this, duration -> {
        dcContext.setChatMuteDuration(chatId, duration);
        titleView.setTitle(glideRequests, dcChat);
      });
    } else {
      // unmute
      dcContext.setChatMuteDuration(chatId, 0);
      titleView.setTitle(glideRequests, dcChat);
    }
  }

  private void handleProfile() {
    Intent intent = new Intent(this, ProfileActivity.class);
    intent.putExtra(ProfileActivity.CHAT_ID_EXTRA, chatId);
    startActivity(intent);
  }

  private void handleAllMedia() {
    Intent intent = new Intent(this, AllMediaActivity.class);
    intent.putExtra(AllMediaActivity.CHAT_ID_EXTRA, chatId);
    startActivity(intent);
  }

  private void handleLeaveGroup() {
    @StringRes int leaveLabel;
    if (dcChat.isInBroadcast()) {
      leaveLabel = R.string.menu_leave_channel;
    } else {
      leaveLabel = R.string.menu_leave_group;
    }

    AlertDialog dialog = new AlertDialog.Builder(this)
      .setMessage(getString(R.string.ask_leave_group))
      .setPositiveButton(leaveLabel, (d, which) -> {
        dcContext.removeContactFromChat(chatId, DcContact.DC_CONTACT_ID_SELF);
        Toast.makeText(this, getString(R.string.done), Toast.LENGTH_SHORT).show();
      })
      .setNegativeButton(R.string.cancel, null)
      .show();
    Util.redPositiveButton(dialog);
  }

  private void handleArchiveChat() {
    int newVisibility = isArchived() ?
            DcChat.DC_CHAT_VISIBILITY_NORMAL : DcChat.DC_CHAT_VISIBILITY_ARCHIVED;
    dcContext.setChatVisibility(chatId, newVisibility);
    Toast.makeText(this, getString(R.string.done), Toast.LENGTH_SHORT).show();
    if (newVisibility==DcChat.DC_CHAT_VISIBILITY_ARCHIVED) {
      finish();
      return;
    }
    dcChat = dcContext.getChat(chatId);
  }

  private void handleDeleteChat() {
    AlertDialog dialog = new AlertDialog.Builder(this)
        .setMessage(getResources().getString(R.string.ask_delete_named_chat, dcChat.getName()))
        .setPositiveButton(R.string.delete, (d, which) -> {
          dcContext.deleteChat(chatId);
          DirectShareUtil.clearShortcut(this, chatId);
          finish();
        })
        .setNegativeButton(R.string.cancel, null)
        .show();
    Util.redPositiveButton(dialog);
  }

  private void handleAddAttachment() {
    if (attachmentTypeSelector == null) {
      attachmentTypeSelector = new AttachmentTypeSelector(this, getSupportLoaderManager(), new AttachmentTypeListener(), chatId);
    }
    attachmentTypeSelector.show(this, attachButton);
  }

  private void handleSecurityChange(boolean isSecureText, boolean isDefaultSms) {
    Log.i(TAG, "handleSecurityChange(" + isSecureText + ", " + isDefaultSms + ")");
    if (isSecurityInitialized && isSecureText == this.isSecureText && isDefaultSms == this.isDefaultSms) {
      return;
    }

    this.isDefaultSms          = isDefaultSms;
    this.isSecurityInitialized = true;

    sendButton.resetAvailableTransports();
    sendButton.setDefaultTransport(Type.NORMAL_MAIL);
  }

  private void handleForwarding() {
    DcChat dcChat = dcContext.getChat(chatId);
    if (dcChat.isSelfTalk()) {
      SendRelayedMessageUtil.immediatelyRelay(this, chatId);
    } else {
      String name = dcChat.getName();
      if (!dcChat.isMultiUser()) {
        int[] contactIds = dcContext.getChatContacts(chatId);
        if (contactIds.length == 1 || contactIds.length == 2) {
          name = dcContext.getContact(contactIds[0]).getDisplayName();
        }
      }
      new AlertDialog.Builder(this)
              .setMessage(getString(R.string.ask_forward, name))
              .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                SendRelayedMessageUtil.immediatelyRelay(this, chatId);
                successfulForwardingAttempt = true;
              })
              .setNegativeButton(R.string.cancel, (dialogInterface, i) -> finish())
              .setOnCancelListener(dialog -> finish())
              .show();
    }
  }

  private void askSendingFiles(ArrayList<Uri> uriList, Runnable onConfirm) {
    String message = String.format(getString(R.string.ask_send_files_to_chat), uriList.size(), dcChat.getName());
    if (SendRelayedMessageUtil.containsVideoType(context, uriList)) {
      message += "\n\n" + getString(R.string.videos_sent_without_recoding);
    }
    new AlertDialog.Builder(this)
      .setMessage(message)
      .setCancelable(false)
      .setNegativeButton(android.R.string.cancel, null)
      .setPositiveButton(R.string.menu_send, (dialog, which) -> onConfirm.run())
      .show();
  }

  private void handleSharing() {
    ArrayList<Uri> uriList =  ShareUtil.getSharedUris(this);
    int sharedContactId = ShareUtil.getSharedContactId(this);
    if (uriList.size() > 1) {
      askSendingFiles(uriList, () -> SendRelayedMessageUtil.immediatelyRelay(this, chatId));
    } else {
      if (sharedContactId != 0) {
        addAttachmentContactInfo(sharedContactId);
      } else if (uriList.isEmpty()) {
        dcContext.setDraft(chatId, SendRelayedMessageUtil.createMessage(this, null, getSharedText(this)));
      } else {
        dcContext.setDraft(chatId, SendRelayedMessageUtil.createMessage(this, uriList.get(0), getSharedText(this)));
      }
      initializeDraft();
    }
  }

  ///// Initializers

  /**
   * Drafts can be initialized by click on a mailto: link or from the database
   * @return
   */
  private ListenableFuture<Boolean> initializeDraft() {
    isEditing = false;
    final SettableFuture<Boolean> future = new SettableFuture<>();
    DcMsg draft = dcContext.getDraft(chatId);
    final String sharedText = ShareUtil.getSharedText(this);

    if (!draft.isOk()) {
      if (TextUtils.isEmpty(sharedText)) {
        future.set(false);
      } else {
        composeText.setText(sharedText);
        future.set(true);
      }
      updateToggleButtonState();
      return future;
    }

    final String text = TextUtils.isEmpty(sharedText)? draft.getText() : sharedText;
    if(!text.isEmpty()) {
      composeText.setText(text);
      composeText.setSelection(composeText.getText().length());
    }

    DcMsg quote = draft.getQuotedMsg();
    if (quote == null) {
      inputPanel.clearQuoteWithoutAnimation();
    } else {
      handleReplyMessage(quote);
    }

    String file = draft.getFile();
    if (file.isEmpty() || !new File(file).exists()) {
      future.set(!text.isEmpty());
      updateToggleButtonState();
      return future;
    }

    ListenableFuture.Listener<Boolean> listener = new ListenableFuture.Listener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
        future.set(result || !text.isEmpty());
        updateToggleButtonState();
      }

      @Override
      public void onFailure(ExecutionException e) {
        future.set(!text.isEmpty());
        updateToggleButtonState();
      }
    };

    switch (draft.getType()) {
      case DcMsg.DC_MSG_IMAGE:
        setMedia(draft, MediaType.IMAGE).addListener(listener);
        break;
      case DcMsg.DC_MSG_GIF:
        setMedia(draft, MediaType.GIF).addListener(listener);
        break;
      case DcMsg.DC_MSG_AUDIO:
        setMedia(draft, MediaType.AUDIO).addListener(listener);
        break;
      case DcMsg.DC_MSG_VIDEO:
        setMedia(draft, MediaType.VIDEO).addListener(listener);
        break;
      default:
        setMedia(draft, MediaType.DOCUMENT).addListener(listener);
        break;
    }

    return future;
  }

  private void initializeEnabledCheck() {
    boolean enabled = true;
    inputPanel.setEnabled(enabled);
    sendButton.setEnabled(enabled);
    attachButton.setEnabled(enabled);
  }

  private ListenableFuture<Boolean> initializeSecurity(final boolean currentSecureText,
                                                       final boolean currentIsDefaultSms)
  {
    final SettableFuture<Boolean> future = new SettableFuture<>();

    handleSecurityChange(currentSecureText || isMultiUser(), currentIsDefaultSms);

    future.set(true);
    return future;
  }

  private void initializeViews() {
    ActionBar supportActionBar = getSupportActionBar();
    if (supportActionBar == null) throw new AssertionError();

    titleView             = (ConversationTitleView) supportActionBar.getCustomView();
    buttonToggle          = ViewUtil.findById(this, R.id.button_toggle);
    sendButton            = ViewUtil.findById(this, R.id.send_button);
    attachButton          = ViewUtil.findById(this, R.id.attach_button);
    composeText           = ViewUtil.findById(this, R.id.embedded_text_editor);
    emojiPickerContainer  = ViewUtil.findById(this, R.id.emoji_picker_container);
    composePanel          = ViewUtil.findById(this, R.id.bottom_panel);
    container             = ViewUtil.findById(this, R.id.layout_container);
    quickAttachmentToggle = ViewUtil.findById(this, R.id.quick_attachment_toggle);
    inputPanel            = ViewUtil.findById(this, R.id.bottom_panel);
    backgroundView        = ViewUtil.findById(this, R.id.conversation_background);
    messageRequestBottomView = ViewUtil.findById(this, R.id.conversation_activity_message_request_bottom_bar);

    ImageButton quickCameraToggle = ViewUtil.findById(this, R.id.quick_camera_toggle);

    if (!ViewUtil.isEdgeToEdgeSupported()) {
      // since insets will not be applied, we need to set top padding to avoid drawing behind toolbar
      try (TypedArray typedArray = obtainStyledAttributes(new int[]{android.R.attr.actionBarSize})) {
        int paddingTop = typedArray.getDimensionPixelSize(0, 0);
        container.setPadding(container.getPaddingLeft(), paddingTop, container.getPaddingRight() , container.getPaddingBottom());
      }
    }
    // apply padding top to avoid drawing behind top bar
    ViewUtil.applyWindowInsets(findViewById(R.id.fragment_content), false, true, false, false);
    // apply padding to root to avoid collision with system bars
    ViewUtil.applyWindowInsets(findViewById(R.id.root_layout), true, false, true, true);

    container.addOnKeyboardShownListener(this);
    container.addOnKeyboardHiddenListener(backgroundView);
    container.addOnKeyboardShownListener(backgroundView);
    inputPanel.setListener(this);
    inputPanel.setMediaListener(this);

    attachmentTypeSelector = null;
    attachmentManager      = new AttachmentManager(this, this);
    audioRecorder          = new AudioRecorder(this);

    SendButtonListener        sendButtonListener        = new SendButtonListener();
    ComposeKeyPressedListener composeKeyPressedListener = new ComposeKeyPressedListener();

    composeText.setOnEditorActionListener(sendButtonListener);
    attachButton.setOnClickListener(new AttachButtonListener());
    attachButton.setOnLongClickListener(new AttachButtonLongClickListener());
    sendButton.setOnClickListener(sendButtonListener);
    sendButton.setEnabled(true);
    sendButton.addOnTransportChangedListener((newTransport, manuallySelected) -> {
      composeText.setTransport(newTransport);
      buttonToggle.getBackground().invalidateSelf();
    });

    titleView.setOnClickListener(v -> handleProfile());
    titleView.setOnBackClickedListener(view -> handleReturnToConversationList());

    composeText.setOnKeyListener(composeKeyPressedListener);
    composeText.addTextChangedListener(composeKeyPressedListener);
    composeText.setOnEditorActionListener(sendButtonListener);
    composeText.setOnClickListener(composeKeyPressedListener);
    composeText.setOnFocusChangeListener(composeKeyPressedListener);

    quickCameraToggle.setOnClickListener(v -> attachmentManager.capturePhoto(ConversationActivity.this, TAKE_PHOTO));

    initializeBackground();
  }

  private void initializeBackground() {
    String backgroundImagePath = Prefs.getBackgroundImagePath(this, dcContext.getAccountId());
    Drawable background;
    if(!backgroundImagePath.isEmpty()) {
      background = Drawable.createFromPath(backgroundImagePath);
    }
    else if(DynamicTheme.isDarkTheme(this)) {
      background = getResources().getDrawable(R.drawable.background_hd_dark);
    }
    else {
      background = getResources().getDrawable(R.drawable.background_hd);
    }
    backgroundView.setImageDrawable(background);
  }

  protected void initializeActionBar() {
    ActionBar supportActionBar = getSupportActionBar();
    if (supportActionBar == null) throw new AssertionError();

    supportActionBar.setDisplayHomeAsUpEnabled(false);
    supportActionBar.setCustomView(R.layout.conversation_title_view);
    supportActionBar.setDisplayShowCustomEnabled(true);
    supportActionBar.setDisplayShowTitleEnabled(false);

    Toolbar parent = (Toolbar) supportActionBar.getCustomView().getParent();
    parent.setPadding(0,0,0,0);
    parent.setContentInsetsAbsolute(0,0);
  }

  private void initializeResources() {
    int accountId = getIntent().getIntExtra(ACCOUNT_ID_EXTRA, dcContext.getAccountId());
    if (accountId != dcContext.getAccountId()) {
      AccountManager.getInstance().switchAccount(context, accountId);
      fragment.dcContext = dcContext = context.getDcContext();
      initializeBackground();
    }
    chatId = getIntent().getIntExtra(CHAT_ID_EXTRA, -1);
    if(chatId == DcChat.DC_CHAT_NO_CHAT)
      throw new IllegalStateException("can't display a conversation for no chat.");
    dcChat           = dcContext.getChat(chatId);
    recipient        = new Recipient(this, dcChat);
    glideRequests    = GlideApp.with(this);

    setComposePanelVisibility();
    initializeContactRequest();
  }

  private void setComposePanelVisibility() {
    if (dcChat.canSend()) {
      composePanel.setVisibility(View.VISIBLE);
      attachmentManager.setHidden(false);
    } else {
      composePanel.setVisibility(View.GONE);
      attachmentManager.setHidden(true);
      hideSoftKeyboard();
    }
  }

  //////// Helper Methods

  private void addAttachment(int type) {
    switch (type) {
    case AttachmentTypeSelector.ADD_GALLERY:
      AttachmentManager.selectGallery(this, PICK_GALLERY); break;
    case AttachmentTypeSelector.ADD_DOCUMENT:
      AttachmentManager.selectDocument(this, PICK_DOCUMENT); break;
    case AttachmentTypeSelector.ADD_CONTACT_INFO:
      startContactChooserActivity(); break;
    case AttachmentTypeSelector.ADD_LOCATION:
      AttachmentManager.selectLocation(this, chatId); break;
    case AttachmentTypeSelector.TAKE_PHOTO:
      attachmentManager.capturePhoto(this, TAKE_PHOTO); break;
    case AttachmentTypeSelector.RECORD_VIDEO:
      attachmentManager.captureVideo(this, RECORD_VIDEO);
      break;
    case AttachmentTypeSelector.ADD_WEBXDC:
      AttachmentManager.selectWebxdc(this, PICK_WEBXDC); break;
    }
  }

  private void startContactChooserActivity() {
    Intent intent = new Intent(ConversationActivity.this, AttachContactActivity.class);
    intent.putExtra(ContactSelectionListFragment.ALLOW_CREATION, false);
    startActivityForResult(intent, PICK_CONTACT);
  }

  private ListenableFuture<Boolean> setMedia(@Nullable Uri uri, @NonNull MediaType mediaType) {
    if (uri == null) {
      return new SettableFuture<>(false);
    }

    return attachmentManager.setMedia(glideRequests, uri, null, mediaType, 0, 0, chatId);
  }

  private ListenableFuture<Boolean> setMedia(DcMsg msg, @NonNull MediaType mediaType) {
    return attachmentManager.setMedia(glideRequests, Uri.fromFile(new File(msg.getFile())), msg, mediaType, 0, 0, chatId);
  }

  private void addAttachmentContactInfo(int contactId) {
    if (contactId == 0) {
      return;
    }

    try {
      byte[] vcard = rpc.makeVcard(dcContext.getAccountId(), Collections.singletonList(contactId)).getBytes();
      String mimeType = "application/octet-stream";
      setMedia(PersistentBlobProvider.getInstance().create(this, vcard, mimeType, "vcard.vcf"), MediaType.DOCUMENT);
    } catch (RpcException e) {
      Log.e(TAG, "makeVcard() failed", e);
    }
  }

  private boolean isMultiUser() {
    return dcChat.isMultiUser();
  }

  private boolean isArchived() {
    return dcChat.getVisibility() == DcChat.DC_CHAT_VISIBILITY_ARCHIVED;
  }

  //////// send message or save draft

  protected static final int ACTION_SEND_OUT = 1;
  protected static final int ACTION_SAVE_DRAFT = 2;

  protected ListenableFuture<Integer> processComposeControls(int action) {
    return processComposeControls(action, composeText.getTextTrimmed(),
      attachmentManager.isAttachmentPresent() ?
        attachmentManager.buildSlideDeck() : null);
  }

  protected ListenableFuture<Integer> processComposeControls(final int action, String body, SlideDeck slideDeck) {

    final SettableFuture<Integer> future  = new SettableFuture<>();

    Optional<QuoteModel> quote = inputPanel.getQuote();
    boolean editing = isEditing;

    // for a quick ui feedback, we clear the related controls immediately on sending messages.
    // for drafts, however, we do not change the controls, the activity may be resumed.
    if (action==ACTION_SEND_OUT) {
      composeText.setText("");
      inputPanel.clearQuote();
    }

    Util.runOnAnyBackgroundThread(() -> {
      DcMsg msg = null;
      int recompress = 0;

      if (editing) {
        int msgId = quote.get().getQuotedMsg().getId();
        if (action == ACTION_SEND_OUT) {
          dcContext.sendEditRequest(msgId, body);
        } else {
          dcContext.setDraft(chatId, null);
        }
        future.set(chatId);
        return;
      }

      if(slideDeck!=null) {
        if (action==ACTION_SEND_OUT) {
          Util.runOnMain(() -> attachmentManager.clear(glideRequests, false));
        }

        try {
          if (slideDeck.getWebxdctDraftId() != 0) {
            msg = dcContext.getDraft(chatId);
          } else {
            List<Attachment> attachments = slideDeck.asAttachments();
            for (Attachment attachment : attachments) {
              String contentType = attachment.getContentType();
              if (MediaUtil.isImageType(contentType) && slideDeck.getDocumentSlide() == null) {
                msg = new DcMsg(dcContext,
                                MediaUtil.isGif(contentType) ? DcMsg.DC_MSG_GIF : DcMsg.DC_MSG_IMAGE);
                msg.setDimension(attachment.getWidth(), attachment.getHeight());
              } else if (MediaUtil.isAudioType(contentType)) {
                msg = new DcMsg(dcContext,
                                attachment.isVoiceNote() ? DcMsg.DC_MSG_VOICE : DcMsg.DC_MSG_AUDIO);
              } else if (MediaUtil.isVideoType(contentType) && slideDeck.getDocumentSlide() == null) {
                msg = new DcMsg(dcContext, DcMsg.DC_MSG_VIDEO);
                recompress = DcMsg.DC_MSG_VIDEO;
              } else {
                msg = new DcMsg(dcContext, DcMsg.DC_MSG_FILE);
              }
              String path = attachment.getRealPath(this);
              msg.setFileAndDeduplicate(path, attachment.getFileName(), null);
            }
          }
          if (msg != null) {
            msg.setText(body);
          }
        }
        catch(Exception e) {
          e.printStackTrace();
        }
      }
      else if (!body.isEmpty()){
        msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
        msg.setText(body);
      }

      if (quote.isPresent()) {
        if (msg == null) msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
        msg.setQuote(quote.get().getQuotedMsg());
      }

      if (action==ACTION_SEND_OUT) {

        // for WEBXDC, drafts are just sent out as is.
        // for preparations and other cases, cleanup draft soon.
        if (msg == null || msg.getType() != DcMsg.DC_MSG_WEBXDC) {
          dcContext.setDraft(dcChat.getId(), null);
        }

        if(msg!=null) {
          boolean doSend = true;
          if (recompress==DcMsg.DC_MSG_VIDEO) {
            Util.runOnMain(() -> {
              if (isFinishing()) return;
              progressDialog = ProgressDialog.show(
                                                   ConversationActivity.this,
                                                   "",
                                                   getString(R.string.one_moment),
                                                   true,
                                                   false
                                                   );
            });
            doSend = VideoRecoder.prepareVideo(ConversationActivity.this, dcChat.getId(), msg);
            Util.runOnMain(() -> {
              try {
                if (progressDialog != null) progressDialog.dismiss();
              } catch (final IllegalArgumentException e) {
                // The activity is finishing/destroyed, do nothing.
              }
            });
          }

          if (doSend) {
            if (dcContext.sendMsg(dcChat.getId(), msg) == 0) {
              String lastError = dcContext.getLastError();
              if (!"".equals(lastError)) {
                Util.runOnMain(() -> Toast.makeText(ConversationActivity.this, lastError, Toast.LENGTH_LONG).show());
              }
              future.set(chatId);
              return;
            }
          }

          Util.runOnMain(() -> sendComplete(dcChat.getId()));
        }
      } else {
        dcContext.setDraft(dcChat.getId(), msg);
      }
      future.set(chatId);
    });

    return future;
  }


  protected void sendComplete(int chatId) {
    boolean refreshFragment = (chatId != this.chatId);
    this.chatId = chatId;

    if (fragment == null || !fragment.isVisible() || isFinishing()) {
      return;
    }

    fragment.setLastSeen(-1);

    if (refreshFragment) {
      fragment.reload(recipient, chatId);
      DcHelper.getNotificationCenter(this).updateVisibleChat(dcContext.getAccountId(), chatId);
    }

    fragment.scrollToBottom();
    attachmentManager.cleanup();
  }


  // handle attachment drawer, camera, recorder

  private void updateToggleButtonState() {
    if (inputPanel.isRecordingInLockedMode()) {
      buttonToggle.display(sendButton);
      quickAttachmentToggle.hide();
      return;
    }

    if (!isEditing && composeText.getText().length() == 0 && !attachmentManager.isAttachmentPresent()) {
      buttonToggle.display(attachButton);
      quickAttachmentToggle.show();
    } else {
      buttonToggle.display(sendButton);
      quickAttachmentToggle.hide();
    }
  }

  @Override
  public void onRecorderPermissionRequired() {
    Permissions.with(this)
               .request(Manifest.permission.RECORD_AUDIO)
               .ifNecessary()
               .withPermanentDenialDialog(getString(R.string.perm_explain_access_to_mic_denied))
               .execute();
  }

  @Override
  public void onRecorderStarted() {
    fragment.hideAddReactionView();
    Vibrator vibrator = ServiceUtil.getVibrator(this);
    vibrator.vibrate(20);

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    audioRecorder.startRecording();
  }

  @Override
  public void onRecorderLocked() {
    updateToggleButtonState();
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
  }

  @Override
  public void onRecorderFinished() {
    updateToggleButtonState();
    Vibrator vibrator = ServiceUtil.getVibrator(this);
    vibrator.vibrate(20);

    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    ListenableFuture<Pair<Uri, Long>> future = audioRecorder.stopRecording();
    future.addListener(new ListenableFuture.Listener<Pair<Uri, Long>>() {
      @Override
      public void onSuccess(final @NonNull Pair<Uri, Long> result) {
        AudioSlide audioSlide     = new AudioSlide(ConversationActivity.this, result.first, result.second, MediaUtil.AUDIO_AAC, true);
        SlideDeck  slideDeck      = new SlideDeck();
        slideDeck.addSlide(audioSlide);

        processComposeControls(ACTION_SEND_OUT, "", slideDeck).addListener(new AssertedSuccessListener<Integer>() {
          @Override
          public void onSuccess(Integer chatId) {
            new AsyncTask<Void, Void, Void>() {
              @Override
              protected Void doInBackground(Void... params) {
                PersistentBlobProvider.getInstance().delete(ConversationActivity.this, result.first);
                return null;
              }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
          }
        });
      }

      @Override
      public void onFailure(ExecutionException e) {
        Toast.makeText(ConversationActivity.this, R.string.chat_unable_to_record_audio, Toast.LENGTH_LONG).show();
      }
    });
  }

  @Override
  public void onRecorderCanceled() {
    updateToggleButtonState();
    Vibrator vibrator = ServiceUtil.getVibrator(this);
    vibrator.vibrate(50);

    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    ListenableFuture<Pair<Uri, Long>> future = audioRecorder.stopRecording();
    future.addListener(new ListenableFuture.Listener<Pair<Uri, Long>>() {
      @Override
      public void onSuccess(final Pair<Uri, Long> result) {
        new AsyncTask<Void, Void, Void>() {
          @Override
          protected Void doInBackground(Void... params) {
            PersistentBlobProvider.getInstance().delete(ConversationActivity.this, result.first);
            return null;
          }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      }

      @Override
      public void onFailure(ExecutionException e) {}
    });
  }

  private void reloadEmojiPicker() {
    emojiPickerContainer.removeAllViews();
    emojiPicker = (MediaKeyboard) LayoutInflater.from(this).inflate(R.layout.conversation_activity_emojidrawer_stub, emojiPickerContainer, false);
    emojiPickerContainer.addView(emojiPicker);
    inputPanel.setMediaKeyboard(emojiPicker);
  }

  @Override
  public void onEmojiToggle() {
    if (emojiPicker == null) {
      reloadEmojiPicker();
    }

    if (container.getCurrentInput() == emojiPicker) {
      container.showSoftkey(composeText);
    } else {
      container.show(composeText, emojiPicker);
    }
  }

  @Override
  public void onQuoteDismissed() {
    if (isEditing) composeText.setText("");
    isEditing = false;
  }

  // media selected by the system keyboard
  @Override
  public void onMediaSelected(@NonNull Uri uri, String contentType) {
    if (isEditing) return;
    if (MediaUtil.isImageType(contentType)) {
      sendSticker(uri, contentType);
    } else if (MediaUtil.isVideoType(contentType)) {
      setMedia(uri, MediaType.VIDEO);
    } else if (MediaUtil.isAudioType(contentType)) {
      setMedia(uri, MediaType.AUDIO);
    }
  }

  private void sendSticker(@NonNull Uri uri, String contentType) {
    Attachment attachment = new UriAttachment(uri, null, contentType,
      AttachmentDatabase.TRANSFER_PROGRESS_STARTED, 0, 0, 0, null, null, false);
    String path = attachment.getRealPath(this);

    Optional<QuoteModel> quote = inputPanel.getQuote();
    inputPanel.clearQuote();

    DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_STICKER);
    if (quote.isPresent()) {
      msg.setQuote(quote.get().getQuotedMsg());
    }
    msg.setFileAndDeduplicate(path, null, null);
    dcContext.sendMsg(chatId, msg);
  }

  // Listeners

  private class AttachmentTypeListener implements AttachmentTypeSelector.AttachmentClickedListener {
    @Override
    public void onClick(int type) {
      addAttachment(type);
    }

    @Override
    public void onQuickAttachment(Uri uri) {
      Intent intent = new Intent();
      intent.setData(uri);

      onActivityResult(PICK_GALLERY, RESULT_OK, intent);
    }
  }

  private class SendButtonListener implements OnClickListener, TextView.OnEditorActionListener {
    @Override
    public void onClick(View v) {
      if (inputPanel.isRecordingInLockedMode()) {
        inputPanel.releaseRecordingLock();
        return;
      }

      String rawText = composeText.getTextTrimmed();
      if (rawText.length() < 1 && !attachmentManager.isAttachmentPresent()) {
        Toast.makeText(ConversationActivity.this, R.string.chat_please_enter_message,
            Toast.LENGTH_SHORT).show();
      }
      else {
        processComposeControls(ACTION_SEND_OUT).addListener(new AssertedSuccessListener<Integer>() {
          @Override
          public void onSuccess(Integer chatId) {
            DcHelper.getNotificationCenter(ConversationActivity.this).maybePlaySendSound(dcChat);
          }
        });
      }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
      if (actionId == EditorInfo.IME_ACTION_SEND) {
        sendButton.performClick();
        return true;
      }
      return false;
    }
  }

  private class AttachButtonListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      fragment.hideAddReactionView();
      handleAddAttachment();
    }
  }

  private class AttachButtonLongClickListener implements View.OnLongClickListener {
    @Override
    public boolean onLongClick(View v) {
      return sendButton.performLongClick();
    }
  }

  private class ComposeKeyPressedListener implements OnKeyListener, OnClickListener, TextWatcher, OnFocusChangeListener {

    int beforeLength;

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
      if (event.getAction() == KeyEvent.ACTION_DOWN) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
          if (Prefs.isEnterSendsEnabled(ConversationActivity.this)) {
            sendButton.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            sendButton.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
            return true;
          }
        }
      }
      return false;
    }

    @Override
    public void onClick(View v) {
      container.showSoftkey(composeText);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,int after) {
      beforeLength = composeText.getTextTrimmed().length();
    }

    @Override
    public void afterTextChanged(Editable s) {
      if (composeText.getTextTrimmed().length() == 0 || beforeLength == 0) {
        composeText.postDelayed(ConversationActivity.this::updateToggleButtonState, 50);
      }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before,int count) {}

    @Override
    public void onFocusChange(View v, boolean hasFocus) {}
  }

  @Override
  public void handleReplyMessage(DcMsg msg) {
    if (isEditing) composeText.setText("");
    isEditing = false;
    // If you modify these lines you may also want to modify ConversationItem.setQuote():
    Recipient author = new Recipient(this, dcContext.getContact(msg.getFromId()));

    SlideDeck slideDeck = new SlideDeck();
    if (msg.hasFile()) {
      slideDeck.addSlide(MediaUtil.getSlideForMsg(this, msg));
    }

    String text = msg.getSummarytext(500);

    inputPanel.setQuote(GlideApp.with(this),
            msg,
            msg.getTimestamp(),
            author,
            text,
            slideDeck,
            false);

    inputPanel.clickOnComposeInput();
  }

  @Override
  public void handleEditMessage(DcMsg msg) {
    isEditing = true;
    Recipient author = new Recipient(this, dcContext.getContact(msg.getFromId()));

    SlideDeck slideDeck = new SlideDeck();
    String text = msg.getSummarytext(500);

    inputPanel.setQuote(GlideApp.with(this),
            msg,
            msg.getTimestamp(),
            author,
            text,
            slideDeck,
            true);

    setDraftText(msg.getText());
    inputPanel.clickOnComposeInput();
  }

  @Override
  public void onAttachmentChanged() {
    handleSecurityChange(isSecureText, isDefaultSms);
    updateToggleButtonState();
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    int eventId = event.getId();
    if ((eventId == DcContext.DC_EVENT_CHAT_MODIFIED && event.getData1Int() == chatId)
     || (eventId == DcContext.DC_EVENT_CHAT_EPHEMERAL_TIMER_MODIFIED && event.getData1Int() == chatId)
     || eventId == DcContext.DC_EVENT_CONTACTS_CHANGED) {
      dcChat = dcContext.getChat(chatId);
      titleView.setTitle(glideRequests, dcChat);
      initializeSecurity(isSecureText, isDefaultSms);
      setComposePanelVisibility();
      initializeContactRequest();
    } else if ((eventId == DcContext.DC_EVENT_INCOMING_MSG
                || eventId == DcContext.DC_EVENT_MSG_READ)
               && event.getData1Int() == chatId) {
        DcContact contact = recipient.getDcContact();
        titleView.setSeenRecently(contact!=null? dcContext.getContact(contact.getId()).wasSeenRecently() : false);
    }
  }


  // in-chat search

  private int beforeSearchComposeVisibility = View.VISIBLE;

  private Menu  searchMenu = null;
  private int[] searchResult = {};
  private int   searchResultPosition = -1;

  private Toast lastToast = null;

  private void updateResultCounter(int curr, int total) {
    if (searchMenu!=null) {
      MenuItem item = searchMenu.findItem(R.id.menu_search_counter);
      if (curr!=-1) {
        item.setTitle(String.format("%d/%d", total==0? 0 : curr+1, total));
        item.setVisible(true);
      } else {
        item.setVisible(false);
      }
    }
  }

  private void searchExpand(final Menu menu, final MenuItem searchItem) {
    searchMenu = menu;

    beforeSearchComposeVisibility = composePanel.getVisibility();
    composePanel.setVisibility(View.GONE);

    ConversationActivity.this.makeSearchMenuVisible(menu, searchItem, true);
  }

  private void searchCollapse(final Menu menu, final MenuItem searchItem) {
    searchMenu = null;
    composePanel.setVisibility(beforeSearchComposeVisibility);

    ConversationActivity.this.makeSearchMenuVisible(menu, searchItem, false);
  }

  private void handleMenuSearchNext(boolean searchNext) {
    if(searchResult.length>0) {
      searchResultPosition += searchNext? 1 : -1;
      if(searchResultPosition<0) searchResultPosition = searchResult.length-1;
      if(searchResultPosition>=searchResult.length) searchResultPosition = 0;
      fragment.scrollToMsgId(searchResult[searchResultPosition]);
      updateResultCounter(searchResultPosition, searchResult.length);
    } else {
      // no search, scroll to first/last message
      if(searchNext) {
        fragment.scrollToBottom();
      } else {
        fragment.scrollToTop();
      }
    }
  }

  @Override
  public boolean onQueryTextSubmit(String query) {
    return true; // action handled by listener
  }

  @Override
  public boolean onQueryTextChange(String query) {
    if (lastToast!=null) {
      lastToast.cancel();
      lastToast = null;
    }

    String normQuery = query.trim();
    searchResult = dcContext.searchMsgs(chatId, normQuery);

    if(searchResult.length>0) {
      searchResultPosition = searchResult.length - 1;
      fragment.scrollToMsgId(searchResult[searchResultPosition]);
      updateResultCounter(searchResultPosition, searchResult.length);
    } else {
      searchResultPosition = -1;
      if (normQuery.isEmpty()) {
        updateResultCounter(-1, 0); // hide
      } else {
        String msg = getString(R.string.search_no_result_for_x, normQuery);
        if (lastToast != null) {
          lastToast.cancel();
        }
        lastToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        lastToast.show();
        updateResultCounter(0, 0); // show as "0/0"
      }
    }
    return true; // action handled by listener
  }

  public void initializeContactRequest() {
    if (!dcChat.isContactRequest()) {
      messageRequestBottomView.setVisibility(View.GONE);
      return;
    }

    messageRequestBottomView.setVisibility(View.VISIBLE);
    messageRequestBottomView.setAcceptOnClickListener(v -> {
      dcContext.acceptChat(chatId);
      messageRequestBottomView.setVisibility(View.GONE);
      composePanel.setVisibility(View.VISIBLE);
    });


    if (dcChat.getType() == DcChat.DC_CHAT_TYPE_GROUP) {
      // We don't support blocking groups yet, so offer to delete it instead
      messageRequestBottomView.setBlockText(R.string.delete);
      messageRequestBottomView.setBlockOnClickListener(v -> handleDeleteChat());
      messageRequestBottomView.setQuestion(null);

    } else {
      messageRequestBottomView.setBlockText(R.string.block);
      messageRequestBottomView.setBlockOnClickListener(v -> {
        // avoid showing compose panel on receiving DC_EVENT_CONTACTS_CHANGED for the chat that is no longer a request after blocking
        DcHelper.getEventCenter(this).removeObserver(DcContext.DC_EVENT_CONTACTS_CHANGED, this);

        dcContext.blockChat(chatId);
        Bundle extras = new Bundle();
        extras.putInt(ConversationListFragment.RELOAD_LIST, 1);
        handleReturnToConversationList(extras);
      });
      messageRequestBottomView.setQuestion(null);
    }
  }
}
