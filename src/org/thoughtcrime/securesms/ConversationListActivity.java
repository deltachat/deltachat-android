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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.TooltipCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.b44t.messenger.DcChat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.thoughtcrime.securesms.components.SearchToolbar;
import org.thoughtcrime.securesms.map.MapActivity;
import org.thoughtcrime.securesms.qr.QrScanHandler;
import org.thoughtcrime.securesms.search.SearchFragment;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Prefs;

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

public class ConversationListActivity extends PassphraseRequiredActionBarActivity
    implements ConversationListFragment.ConversationSelectedListener
{
  @SuppressWarnings("unused")
  private static final String TAG = ConversationListActivity.class.getSimpleName();

  private final DynamicTheme    dynamicTheme    = new DynamicNoActionBarTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private ConversationListFragment conversationListFragment;
  private TextView                 title;
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
    setContentView(R.layout.conversation_list_activity);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    title                    = findViewById(R.id.toolbar_title);
    searchToolbar            = findViewById(R.id.search_toolbar);
    searchAction             = findViewById(R.id.search_action);
    fragmentContainer        = findViewById(R.id.fragment_container);
    conversationListFragment = initFragment(R.id.fragment_container, new ConversationListFragment(), dynamicLanguage.getCurrentLocale());

    initializeSearchListener();

    TooltipCompat.setTooltipText(searchAction, getText(R.string.search_explain));
    if (isRelayingMessageContent(this)) {
      title.setText(isForwarding(this) ? R.string.forward_to : R.string.chat_share_with_title);
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      if (isDirectSharing(this)) {
        openConversation(getDirectSharingChatId(this), -1);
      }
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    if (isRelayingMessageContent(this)) {
      title.setText(isForwarding(this) ? R.string.forward_to : R.string.chat_share_with_title);
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    } else {
      title.setText(R.string.dc_app_name);
      getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }
    conversationListFragment.onNewIntent();
    invalidateOptionsMenu();
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
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

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
      case R.id.menu_new_chat:          createChat();            return true;
      case R.id.menu_settings:          handleDisplaySettings(); return true;
      case R.id.menu_qr_scan:           handleQrScan();          return true;
      case R.id.menu_qr_show:           handleQrShow();          return true;
      case R.id.menu_deaddrop:          handleDeaddrop();        return true;
      case R.id.menu_global_map:        handleShowMap();         return true;
      case android.R.id.home:           onBackPressed();         return true;
    }

    return false;
  }

  private void handleResetRelaying() {
    resetRelayingMessageContent(this);
    title.setText(R.string.dc_app_name);
    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    conversationListFragment.onNewIntent();
    invalidateOptionsMenu();
  }

  private void handleShowMap() {
      Intent intent = new Intent(this, MapActivity.class);
      intent.putExtra(MapActivity.CHAT_IDS, ALL_CHATS_GLOBAL_MAP);
      startActivity(intent);
  }

  private void handleQrScan() {
    new IntentIntegrator(this).setCaptureActivity(QrScanActivity.class).initiateScan();
  }

  private void handleQrShow() {
    Intent qrIntent = new Intent(this, QrShowActivity.class);
    startActivity(qrIntent);
  }

  @Override
  public void onCreateConversation(int chatId) {
    openConversation(chatId, -1);
  }

  public void openConversation(int chatId, int startingPosition) {
    searchToolbar.clearFocus();

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

  @Override
  public void onSwitchToArchive() {
    Intent intent = new Intent(this, ConversationListArchiveActivity.class);
    if (isRelayingMessageContent(this)) {
      acquireRelayMessageContent(this, intent);
      startActivityForResult(intent, REQUEST_RELAY);
    } else {
      startActivity(intent);
    }
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

  private void handleDeaddrop() {
    Intent intent = new Intent(this, ConversationActivity.class);
    intent.putExtra(CHAT_ID_EXTRA, DcChat.DC_CHAT_ID_DEADDROP);
    startActivity(intent);
  }

  private void handleDisplaySettings() {
    Intent preferencesIntent = new Intent(this, ApplicationPreferencesActivity.class);
    startActivity(preferencesIntent);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case IntentIntegrator.REQUEST_CODE:
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        QrScanHandler qrScanHandler = new QrScanHandler(this);
        qrScanHandler.onScanPerformed(scanResult);
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
