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

import static org.thoughtcrime.securesms.ConversationActivity.CHAT_ID_EXTRA;
import static org.thoughtcrime.securesms.ConversationActivity.STARTING_POSITION_EXTRA;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_PROXY_ENABLED;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_PROXY_URL;
import static org.thoughtcrime.securesms.util.ShareUtil.acquireRelayMessageContent;
import static org.thoughtcrime.securesms.util.ShareUtil.getDirectSharingChatId;
import static org.thoughtcrime.securesms.util.ShareUtil.getSharedTitle;
import static org.thoughtcrime.securesms.util.ShareUtil.isDirectSharing;
import static org.thoughtcrime.securesms.util.ShareUtil.isForwarding;
import static org.thoughtcrime.securesms.util.ShareUtil.isRelayingMessageContent;
import static org.thoughtcrime.securesms.util.ShareUtil.resetRelayingMessageContent;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.TooltipCompat;

import com.amulyakhare.textdrawable.TextDrawable;
import com.b44t.messenger.DcAccounts;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.thoughtcrime.securesms.components.AvatarView;
import org.thoughtcrime.securesms.components.SearchToolbar;
import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.connect.DirectShareUtil;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.proxy.ProxySettingsActivity;
import org.thoughtcrime.securesms.qr.QrActivity;
import org.thoughtcrime.securesms.qr.QrCodeHandler;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.search.SearchFragment;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ShareUtil;
import org.thoughtcrime.securesms.util.SaveAttachmentTask;
import org.thoughtcrime.securesms.util.SendRelayedMessageUtil;
import org.thoughtcrime.securesms.util.StorageUtil;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;
import chat.delta.rpc.types.SecurejoinSource;
import chat.delta.rpc.types.SecurejoinUiPath;

import java.util.ArrayList;
import java.util.Date;

public class ConversationListActivity extends PassphraseRequiredActionBarActivity
    implements ConversationListFragment.ConversationSelectedListener
{
  private static final String TAG = ConversationListActivity.class.getSimpleName();
  private static final String OPENPGP4FPR = "openpgp4fpr";
  private static final String NDK_ARCH_WARNED = "ndk_arch_warned";
  public static final String CLEAR_NOTIFICATIONS = "clear_notifications";
  public static final String ACCOUNT_ID_EXTRA = "account_id";
  public static final String FROM_WELCOME   = "from_welcome";

  private ConversationListFragment conversationListFragment;
  public TextView                  title;
  private AvatarView               selfAvatar;
  private ImageView                unreadIndicator;
  private SearchFragment           searchFragment;
  private SearchToolbar            searchToolbar;
  private ImageView                searchAction;
  private ViewGroup                fragmentContainer;
  private ViewGroup                selfAvatarContainer;

  @Override
  protected void onPreCreate() {
    dynamicTheme = new DynamicNoActionBarTheme();
    super.onPreCreate();
  }

  @Override
  protected void onCreate(Bundle icicle, boolean ready) {
    // update messages - for new messages, do not reuse or modify strings but create new ones.
    // it is not needed to keep all past update messages, however, when deleted, also the strings should be deleted.
    try {
      DcContext dcContext = DcHelper.getContext(this);
      final String deviceMsgLabel = "update_2_0_0_android-h";
      if (!dcContext.wasDeviceMsgEverAdded(deviceMsgLabel)) {
        DcMsg msg = null;
        if (!getIntent().getBooleanExtra(FROM_WELCOME, false)) {
          msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);

          // InputStream inputStream = getResources().getAssets().open("device-messages/green-checkmark.jpg");
          // String outputFile = DcHelper.getBlobdirFile(dcContext, "green-checkmark", ".jpg");
          // Util.copy(inputStream, new FileOutputStream(outputFile));
          // msg.setFile(outputFile, "image/jpeg");

          msg.setText(getString(R.string.update_2_0, "https://delta.chat/donate"));
        }
        dcContext.addDeviceMsg(deviceMsgLabel, msg);

        if (Prefs.getStringPreference(this, Prefs.LAST_DEVICE_MSG_LABEL, "").equals(deviceMsgLabel)) {
          int deviceChatId = dcContext.getChatIdByContactId(DcContact.DC_CONTACT_ID_DEVICE);
          if (deviceChatId != 0) {
            dcContext.marknoticedChat(deviceChatId);
          }
        }
        Prefs.setStringPreference(this, Prefs.LAST_DEVICE_MSG_LABEL, deviceMsgLabel);
      }

    } catch(Exception e) {
      e.printStackTrace();
    }

    // create view
    setContentView(R.layout.conversation_list_activity);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    selfAvatar               = findViewById(R.id.self_avatar);
    selfAvatarContainer      = findViewById(R.id.self_avatar_container);
    unreadIndicator          = findViewById(R.id.unread_indicator);
    title                    = findViewById(R.id.toolbar_title);
    searchToolbar            = findViewById(R.id.search_toolbar);
    searchAction             = findViewById(R.id.search_action);
    fragmentContainer        = findViewById(R.id.fragment_container);

    // add margin to avoid content hidden behind system bars
    ViewUtil.applyWindowInsetsAsMargin(searchToolbar, true, true, true, false);

    Bundle bundle = new Bundle();
    conversationListFragment = initFragment(R.id.fragment_container, new ConversationListFragment(), bundle);

    initializeSearchListener();

    TooltipCompat.setTooltipText(searchAction, getText(R.string.search_explain));

    TooltipCompat.setTooltipText(selfAvatar, getText(R.string.switch_account));
    selfAvatar.setOnClickListener(v -> AccountManager.getInstance().showSwitchAccountMenu(this));
    findViewById(R.id.avatar_and_title).setOnClickListener(v -> {
      if (!isRelayingMessageContent(this)) {
        AccountManager.getInstance().showSwitchAccountMenu(this);
      }
    });

    refresh();

    if (BuildConfig.DEBUG) checkNdkArchitecture();

    DcHelper.maybeShowMigrationError(this);
  }

  /**
   * If the build script is invoked with a specific architecture (e.g.`ndk-make.sh arm64-v8a`), it
   * will compile the core only for this arch. This method checks if the arch was correct.
   *
   * In order to do this, `ndk-make.sh` writes its argument into the file `ndkArch`.
   * `getNdkArch()` in `build.gradle` then reads this file and its content is assigned to
   * `BuildConfig.NDK_ARCH`.
   */
  @SuppressWarnings("ConstantConditions")
  private void checkNdkArchitecture() {
    boolean wrongArch = false;

    if (!TextUtils.isEmpty(BuildConfig.NDK_ARCH)) {
      String archProperty = System.getProperty("os.arch");
      String arch;

      // armv8l is 32 bit mode in 64 bit CPU:
      if (archProperty.startsWith("armv7") || archProperty.startsWith("armv8l")) arch = "armeabi-v7a";
      else if (archProperty.equals("aarch64")) arch = "arm64-v8a";
      else if (archProperty.equals("i686")) arch = "x86";
      else if (archProperty.equals("x86_64")) arch = "x86_64";
      else {
        Log.e(TAG, "Unknown os.arch: " + archProperty);
        arch = "";
      }

      if (!arch.equals(BuildConfig.NDK_ARCH)) {
        wrongArch = true;

        String message;
        if (arch.equals("")) {
          message = "This phone has the unknown architecture " + archProperty + ".\n\n"+
                  "Please open an issue at https://github.com/deltachat/deltachat-android/issues.";
        } else {
          message = "Apparently you used `ndk-make.sh " + BuildConfig.NDK_ARCH + "`, but this device is " + arch + ".\n\n" +
                  "You can use the app, but changes you made to the Rust code were not applied.\n\n" +
                  "To compile in your changes, you can:\n" +
                  "- Either run `ndk-make.sh " + arch + "` to build only for " + arch + " in debug mode\n" +
                  "- Or run `ndk-make.sh` without argument to build for all architectures in release mode\n\n" +
                  "If something doesn't work, please open an issue at https://github.com/deltachat/deltachat-android/issues!!";
        }
        Log.e(TAG, message);

        if (!Prefs.getBooleanPreference(this, NDK_ARCH_WARNED, false)) {
          new AlertDialog.Builder(this)
                  .setMessage(message)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
          Prefs.setBooleanPreference(this, NDK_ARCH_WARNED, true);
        }
      }
    }

    if (!wrongArch) Prefs.setBooleanPreference(this, NDK_ARCH_WARNED, false);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    if (isFinishing()) {
      Log.w(TAG, "Activity is finishing, aborting onNewIntent()");
      return;
    }
    super.onNewIntent(intent);
    setIntent(intent);
    refresh();
    conversationListFragment.onNewIntent();
    invalidateOptionsMenu();
  }

  private void refresh() {
    DcContext dcContext = DcHelper.getContext(this);
    int accountId = getIntent().getIntExtra(ACCOUNT_ID_EXTRA, dcContext.getAccountId());
    if (getIntent().getBooleanExtra(CLEAR_NOTIFICATIONS, false)) {
      DcHelper.getNotificationCenter(this).removeAllNotifications(accountId);
    }
    if (accountId != dcContext.getAccountId()) {
      AccountManager.getInstance().switchAccountAndStartActivity(this, accountId);
    }

    refreshAvatar();
    refreshUnreadIndicator();
    refreshTitle();
    handleOpenpgp4fpr();
    if (isDirectSharing(this)) {
      openConversation(getDirectSharingChatId(this), -1);
    }
  }

  public void refreshTitle() {
    if (isRelayingMessageContent(this)) {
      if (isForwarding(this)) {
        title.setText(R.string.forward_to);
      } else {
        String titleStr = getSharedTitle(this);
        if (titleStr != null) { // sharing from sendToChat
          title.setText(titleStr);
        } else { // normal sharing
          title.setText(R.string.chat_share_with_title);
        }
      }
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    } else {
      boolean multiProfile = DcHelper.getAccounts(this).getAll().length > 1;
      String defText = multiProfile? DcHelper.getContext(this).getName() : getString(R.string.app_name);
      title.setText(DcHelper.getConnectivitySummary(this, defText));
      // refreshTitle is called by ConversationListFragment when connectivity changes so update connectivity dot here
      selfAvatar.setConnectivity(DcHelper.getContext(this).getConnectivity());
      getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }
  }

  public void refreshAvatar() {
    if (selfAvatarContainer == null) return;

    if (isRelayingMessageContent(this)) {
      selfAvatarContainer.setVisibility(View.GONE);
    } else {
      selfAvatarContainer.setVisibility(View.VISIBLE);
      DcContext dcContext = DcHelper.getContext(this);
      DcContact self = dcContext.getContact(DcContact.DC_CONTACT_ID_SELF);
      String name = dcContext.getConfig("displayname");
      if (TextUtils.isEmpty(name)) {
        name = self.getAddr();
      }
      selfAvatar.setAvatar(GlideApp.with(this), new Recipient(this, self, name), false);
    }
  }

  public void refreshUnreadIndicator() {
    int unreadCount = 0;
    DcAccounts dcAccounts = DcHelper.getAccounts(this);
    int skipId = dcAccounts.getSelectedAccount().getAccountId();
    for (int accountId : dcAccounts.getAll()) {
      if (accountId != skipId) {
        DcContext dcContext = dcAccounts.getAccount(accountId);
        if (!dcContext.isMuted()) {
          unreadCount += dcContext.getFreshMsgs().length;
        }
      }
    }

    if(unreadCount == 0) {
      unreadIndicator.setVisibility(View.GONE);
    } else {
      unreadIndicator.setImageDrawable(TextDrawable.builder()
              .beginConfig()
              .width(ViewUtil.dpToPx(this, 24))
              .height(ViewUtil.dpToPx(this, 24))
              .textColor(Color.WHITE)
              .bold()
              .endConfig()
              .buildRound(String.valueOf(unreadCount), getResources().getColor(R.color.unread_count)));
      unreadIndicator.setVisibility(View.VISIBLE);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    refreshTitle();
    invalidateOptionsMenu();
    DirectShareUtil.triggerRefreshDirectShare(this);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuInflater inflater = this.getMenuInflater();
    menu.clear();

    if (isRelayingMessageContent(this)) {
      inflater.inflate(R.menu.forwarding_menu, menu);
      menu.findItem(R.id.menu_export_attachment).setVisible(
        ShareUtil.isFromWebxdc(this) && ShareUtil.getSharedUris(this).size() == 1
      );
    } else {
      inflater.inflate(R.menu.text_secure_normal, menu);
      menu.findItem(R.id.menu_global_map).setVisible(Prefs.isLocationStreamingEnabled(this));
      MenuItem proxyItem = menu.findItem(R.id.menu_proxy_settings);
      if (TextUtils.isEmpty(DcHelper.get(this, CONFIG_PROXY_URL))) {
        proxyItem.setVisible(false);
      } else {
        boolean proxyEnabled = DcHelper.getInt(this, CONFIG_PROXY_ENABLED) == 1;
        proxyItem.setIcon(proxyEnabled? R.drawable.ic_proxy_enabled_24 : R.drawable.ic_proxy_disabled_24);
        proxyItem.setVisible(true);
      }
    }

    super.onPrepareOptionsMenu(menu);
    return true;
  }

  private void initializeSearchListener() {
    searchAction.setOnClickListener(v -> {
      searchToolbar.display(searchAction.getX() + (searchAction.getWidth() / 2),
                            searchAction.getY() + (searchAction.getHeight() / 2));
    });

    searchToolbar.setListener(new SearchToolbar.SearchListener() {
      @Override
      public void onSearchTextChange(String text) {
        String trimmed = text.trim();

        if (trimmed.length() > 0) {
          if (searchFragment == null) {
            searchFragment = SearchFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                                       .add(R.id.fragment_container, searchFragment, null)
                                       .commit();
          }
          searchFragment.updateSearchQuery(trimmed);
        } else if (searchFragment != null) {
          getSupportFragmentManager().beginTransaction()
                                     .remove(searchFragment)
                                     .commit();
          searchFragment = null;
        }
      }

      @Override
      public void onSearchClosed() {
        if (searchFragment != null) {
          getSupportFragmentManager().beginTransaction()
                                     .remove(searchFragment)
                                     .commit();
          searchFragment = null;
        }
      }
    });
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    int itemId = item.getItemId();
    if (itemId == R.id.menu_new_chat) {
      createChat();
      return true;
    } else if (itemId == R.id.menu_invite_friends) {
      shareInvite();
      return true;
    } else if (itemId == R.id.menu_settings) {
      startActivity(new Intent(this, ApplicationPreferencesActivity.class));
      return true;
    } else if (itemId == R.id.menu_qr) {
      new IntentIntegrator(this).setCaptureActivity(QrActivity.class).initiateScan();
      return true;
    } else if (itemId == R.id.menu_global_map) {
      WebxdcActivity.openMaps(this, 0);
      return true;
    } else if (itemId == R.id.menu_proxy_settings) {
      startActivity(new Intent(this, ProxySettingsActivity.class));
      return true;
    } else if (itemId == android.R.id.home) {
      onBackPressed();
      return true;
    } else if (itemId == R.id.menu_all_media) {
      startActivity(new Intent(this, AllMediaActivity.class));
      return true;
    } else if (itemId == R.id.menu_export_attachment) {
      handleSaveAttachment();
      return true;
    }

    return false;
  }

  private void handleSaveAttachment() {
    SaveAttachmentTask.showWarningDialog(this, (dialogInterface, i) -> {
      if (StorageUtil.canWriteToMediaStore(this)) {
        performSave();
        return;
      }

      Permissions.with(this)
              .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
              .alwaysGrantOnSdk30()
              .ifNecessary()
              .withPermanentDenialDialog(getString(R.string.perm_explain_access_to_storage_denied))
              .onAllGranted(this::performSave)
              .execute();
    });
  }

  private void performSave() {
    ArrayList<Uri> uriList =  ShareUtil.getSharedUris(this);
    Uri uri = uriList.get(0);
    String mimeType = PersistentBlobProvider.getMimeType(this, uri);
    String fileName = PersistentBlobProvider.getFileName(this, uri);
    SaveAttachmentTask.Attachment[] attachments = new SaveAttachmentTask.Attachment[]{
      new SaveAttachmentTask.Attachment(uri, mimeType, new Date().getTime(), fileName)
    };
    SaveAttachmentTask saveTask = new SaveAttachmentTask(this);
    saveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, attachments);
    onBackPressed();
  }

  private void handleOpenpgp4fpr() {
    if (getIntent() != null &&
            Intent.ACTION_VIEW.equals(getIntent().getAction())) {
      Uri uri = getIntent().getData();
      if (uri == null) {
        return;
      }

      if (uri.getScheme().equalsIgnoreCase(OPENPGP4FPR) || Util.isInviteURL(uri)) {
        QrCodeHandler qrCodeHandler = new QrCodeHandler(this);
        qrCodeHandler.handleQrData(uri.toString(), SecurejoinSource.ExternalLink, null);
      }
    }
  }

  private void handleResetRelaying() {
    resetRelayingMessageContent(this);
    refreshTitle();
    selfAvatarContainer.setVisibility(View.VISIBLE);
    conversationListFragment.onNewIntent();
    invalidateOptionsMenu();
  }

  @Override
  public void onCreateConversation(int chatId) {
    openConversation(chatId, -1);
  }

  public void openConversation(int chatId, int startingPosition) {
    searchToolbar.clearFocus();

    final DcContext dcContext = DcHelper.getContext(this);
    if (isForwarding(this) && dcContext.getChat(chatId).isSelfTalk()) {
      SendRelayedMessageUtil.immediatelyRelay(this, chatId);
      Toast.makeText(this, DynamicTheme.getCheckmarkEmoji(this) + " " + getString(R.string.saved), Toast.LENGTH_SHORT).show();
      handleResetRelaying();
      finish();
    } else {
      Intent intent = new Intent(this, ConversationActivity.class);
      intent.putExtra(CHAT_ID_EXTRA, chatId);
      intent.putExtra(STARTING_POSITION_EXTRA, startingPosition);
      if (isRelayingMessageContent(this)) {
        acquireRelayMessageContent(this, intent);
      }
      startActivity(intent);

      overridePendingTransition(R.anim.slide_from_right, R.anim.fade_scale_out);
    }
  }

  @Override
  public void onSwitchToArchive() {
    Intent intent = new Intent(this, ConversationListArchiveActivity.class);
    if (isRelayingMessageContent(this)) {
      acquireRelayMessageContent(this, intent);
    }
    startActivity(intent);
    overridePendingTransition(R.anim.slide_from_right, R.anim.fade_scale_out);
  }

  @Override
  public void onBackPressed() {
    if (searchToolbar.isVisible()) searchToolbar.collapse();
    else if (isRelayingMessageContent(this)) {
      handleResetRelaying();
      finish();
    } else super.onBackPressed();
  }

  private void createChat() {
    Intent intent = new Intent(this, NewConversationActivity.class);
    if (isRelayingMessageContent(this)) {
      acquireRelayMessageContent(this, intent);
    }
    startActivity(intent);
  }

  private void shareInvite() {
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.setType("text/plain");
    String inviteURL = DcHelper.getContext(this).getSecurejoinQr(0);
    intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_friends_text, inviteURL));
    startActivity(Intent.createChooser(intent, getString(R.string.chat_share_with_title)));
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case IntentIntegrator.REQUEST_CODE:
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        QrCodeHandler qrCodeHandler = new QrCodeHandler(this);
        qrCodeHandler.onScanPerformed(scanResult, SecurejoinUiPath.QrIcon);
        break;
      default:
        break;
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }
}
