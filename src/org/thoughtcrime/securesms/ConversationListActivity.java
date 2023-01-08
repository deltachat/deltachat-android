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
import static org.thoughtcrime.securesms.map.MapDataManager.ALL_CHATS_GLOBAL_MAP;
import static org.thoughtcrime.securesms.util.RelayUtil.REQUEST_RELAY;
import static org.thoughtcrime.securesms.util.RelayUtil.acquireRelayMessageContent;
import static org.thoughtcrime.securesms.util.RelayUtil.getDirectSharingChatId;
import static org.thoughtcrime.securesms.util.RelayUtil.isDirectSharing;
import static org.thoughtcrime.securesms.util.RelayUtil.isForwarding;
import static org.thoughtcrime.securesms.util.RelayUtil.isRelayingMessageContent;
import static org.thoughtcrime.securesms.util.RelayUtil.resetRelayingMessageContent;

import android.content.Intent;
import android.net.Uri;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.TooltipCompat;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.thoughtcrime.securesms.components.AvatarImageView;
import org.thoughtcrime.securesms.components.SearchToolbar;
import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.connect.DirectShareUtil;
import org.thoughtcrime.securesms.map.MapActivity;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.qr.QrActivity;
import org.thoughtcrime.securesms.qr.QrCodeHandler;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.search.SearchFragment;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.SendRelayedMessageUtil;

public class ConversationListActivity extends PassphraseRequiredActionBarActivity
    implements ConversationListFragment.ConversationSelectedListener
{
  @SuppressWarnings("unused")
  private static final String TAG = ConversationListActivity.class.getSimpleName();
  private static final String OPENPGP4FPR = "openpgp4fpr";
  private static final String NDK_ARCH_WARNED = "ndk_arch_warned";
  public static final String CLEAR_NOTIFICATIONS = "clear_notifications";

  private final DynamicTheme    dynamicTheme    = new DynamicNoActionBarTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private ConversationListFragment conversationListFragment;
  public TextView                  title;
  private AvatarImageView          selfAvatar;
  private SearchFragment           searchFragment;
  private SearchToolbar            searchToolbar;
  private ImageView                searchAction;
  private ViewGroup                fragmentContainer;

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }

  @Override
  protected void onCreate(Bundle icicle, boolean ready) {
    // update messages - for new messages, do not reuse or modify strings but create new ones.
    // it is not needed to keep all past update messages, however, when deleted, also the strings should be deleted.
    DcContext dcContext = DcHelper.getContext(this);
    DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
    msg.setText(getString(R.string.update_1_34_android));
    dcContext.addDeviceMsg("update_1_34d_android", msg);

    // create view
    setContentView(R.layout.conversation_list_activity);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    selfAvatar               = findViewById(R.id.self_avatar);
    title                    = findViewById(R.id.toolbar_title);
    searchToolbar            = findViewById(R.id.search_toolbar);
    searchAction             = findViewById(R.id.search_action);
    fragmentContainer        = findViewById(R.id.fragment_container);

    Bundle bundle = new Bundle();
    conversationListFragment = initFragment(R.id.fragment_container, new ConversationListFragment(), dynamicLanguage.getCurrentLocale(), bundle);

    initializeSearchListener();
    initializeTitleListener();

    TooltipCompat.setTooltipText(searchAction, getText(R.string.search_explain));

    TooltipCompat.setTooltipText(selfAvatar, getText(R.string.switch_account));
    selfAvatar.setOnClickListener(v -> AccountManager.getInstance().showSwitchAccountMenu(this));

    refresh();

    if (BuildConfig.DEBUG) checkNdkArchitecture();
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

      if (archProperty.startsWith("armv7")) arch = "armeabi-v7a";
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
    super.onNewIntent(intent);
    setIntent(intent);
    refresh();
    conversationListFragment.onNewIntent();
    invalidateOptionsMenu();
  }

  private void refresh() {
    refreshAvatar();
    refreshTitle();
    handleOpenpgp4fpr();
    if (isDirectSharing(this)) {
      openConversation(getDirectSharingChatId(this), -1);
    }

    if (isDirectSharing(this)) {
      openConversation(getDirectSharingChatId(this), -1);
    }

    if (getIntent().getBooleanExtra(CLEAR_NOTIFICATIONS, false)) {
      DcHelper.getNotificationCenter(this).removeAllNotifiations();
    }
  }

  public void refreshTitle() {
    if (isRelayingMessageContent(this)) {
      title.setText(isForwarding(this) ? R.string.forward_to : R.string.chat_share_with_title);
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    } else {
      title.setText(DcHelper.getConnectivitySummary(this, R.string.app_name));
      getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }
  }

  public void refreshAvatar() {
    if (isRelayingMessageContent(this)) {
      selfAvatar.setVisibility(View.GONE);
    } else {
      selfAvatar.setVisibility(View.VISIBLE);
      DcContext dcContext = DcHelper.getContext(this);
      DcContact self = dcContext.getContact(DcContact.DC_CONTACT_ID_SELF);
      String name = dcContext.getConfig("displayname");
      if (TextUtils.isEmpty(name)) {
        name = self.getAddr();
      }
      selfAvatar.setAvatar(GlideApp.with(this), new Recipient(this, self, name), false);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);

    DirectShareUtil.triggerRefreshDirectShare(this);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuInflater inflater = this.getMenuInflater();
    menu.clear();

    if (!isRelayingMessageContent(this)) {
      inflater.inflate(R.menu.text_secure_normal, menu);
      MenuItem item = menu.findItem(R.id.menu_global_map);
      if (Prefs.isLocationStreamingEnabled(this)) {
        item.setVisible(true);
      }

      if (!Prefs.isLocationStreamingEnabled(this)) {
        menu.findItem(R.id.menu_global_map).setVisible(false);
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
            searchFragment = SearchFragment.newInstance(dynamicLanguage.getCurrentLocale());
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

  private void initializeTitleListener() {
    title.setOnClickListener(v -> startActivity(new Intent(this, ConnectivityActivity.class)));
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
      case R.id.menu_new_chat:
        createChat();
        return true;
      case R.id.menu_settings:
        startActivity(new Intent(this, ApplicationPreferencesActivity.class));
        return true;
      case R.id.menu_help:
        startActivity(new Intent(this, LocalHelpActivity.class));
        return true;
      case R.id.menu_qr:
        new IntentIntegrator(this).setCaptureActivity(QrActivity.class).initiateScan();
        return true;
      case R.id.menu_global_map:
        handleShowMap();
        return true;
      case R.id.menu_switch_account:
        AccountManager.getInstance().showSwitchAccountMenu(this);
        return true;
      case android.R.id.home:
        onBackPressed();
        return true;
    }

    return false;
  }

  private void handleOpenpgp4fpr() {
    if (getIntent() != null &&
            Intent.ACTION_VIEW.equals(getIntent().getAction())) {
      Uri uri = getIntent().getData();
      if (uri != null && uri.getScheme().equalsIgnoreCase(OPENPGP4FPR)) {
        String uriString = uri.toString();
        uriString = uriString.replaceFirst(OPENPGP4FPR, OPENPGP4FPR.toUpperCase());
        QrCodeHandler qrCodeHandler = new QrCodeHandler(this);
        qrCodeHandler.handleQrData(uriString);
      }
    }
  }

  private void handleResetRelaying() {
    resetRelayingMessageContent(this);
    refreshTitle();
    selfAvatar.setVisibility(View.VISIBLE);
    conversationListFragment.onNewIntent();
    invalidateOptionsMenu();
  }

  private void handleShowMap() {
      Intent intent = new Intent(this, MapActivity.class);
      intent.putExtra(MapActivity.CHAT_IDS, ALL_CHATS_GLOBAL_MAP);
      startActivity(intent);
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
        startActivityForResult(intent, REQUEST_RELAY);
      } else {
        startActivity(intent);
      }

      overridePendingTransition(R.anim.slide_from_right, R.anim.fade_scale_out);
    }
  }

  @Override
  public void onSwitchToArchive() {
    Intent intent = new Intent(this, ConversationListArchiveActivity.class);
    if (isRelayingMessageContent(this)) {
      acquireRelayMessageContent(this, intent);
      startActivityForResult(intent, REQUEST_RELAY);
    } else {
      startActivity(intent);
    }
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
      startActivityForResult(intent, REQUEST_RELAY);
    } else {
      startActivity(intent);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case IntentIntegrator.REQUEST_CODE:
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        QrCodeHandler qrCodeHandler = new QrCodeHandler(this);
        qrCodeHandler.onScanPerformed(scanResult);
        break;
      case REQUEST_RELAY:
        if (resultCode == RESULT_OK) {
          handleResetRelaying();
          setResult(RESULT_OK);
          finish();
        }
        break;
      default:
        break;
    }
  }
}
