package org.thoughtcrime.securesms;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.b44t.messenger.DcChat;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;

import static org.thoughtcrime.securesms.ConversationActivity.CHAT_ID_EXTRA;
import static org.thoughtcrime.securesms.ConversationActivity.FROM_ARCHIVED_CHATS_EXTRA;
import static org.thoughtcrime.securesms.util.RelayUtil.acquireRelayMessageContent;
import static org.thoughtcrime.securesms.util.RelayUtil.isRelayingMessageContent;
import static org.thoughtcrime.securesms.util.RelayUtil.isSharing;

public class ConversationListArchiveActivity extends PassphraseRequiredActionBarActivity
    implements ConversationListFragment.ConversationSelectedListener
{
  private ConversationListFragment conversationListFragment;

  private final DynamicTheme    dynamicTheme    = new DynamicTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }

  @Override
  protected void onCreate(Bundle icicle, boolean ready) {
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    if (isRelayingMessageContent(this)) {
      getSupportActionBar().setTitle(isSharing(this) ? R.string.chat_share_with_title : R.string.forward_to);
      getSupportActionBar().setSubtitle(R.string.chat_archived_label);
    } else {
      getSupportActionBar().setTitle(R.string.chat_archived_label);
    }

    Bundle bundle = new Bundle();
    bundle.putBoolean(ConversationListFragment.ARCHIVE, true);

    conversationListFragment = initFragment(android.R.id.content, new ConversationListFragment(), dynamicLanguage.getCurrentLocale(), bundle);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (isFinishing()) overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_right);
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

    inflater.inflate(R.menu.archived_list, menu);
    super.onPrepareOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;

      case R.id.mark_as_read:
        DcHelper.getContext(this).marknoticedChat(DcChat.DC_CHAT_ID_ARCHIVED_LINK);
        return true;
    }

    return false;
  }

  @Override
  public void onBackPressed() {
    if (isRelayingMessageContent(this)) {
      // Go back to the ConversationListRelayingActivity
      super.onBackPressed();
    } else {
      // Load the ConversationListActivity in case it's not existent for some reason
      Intent intent = new Intent(this, ConversationListActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      finish();
    }
  }

  @Override
  public void onCreateConversation(int chatId) {
    Intent intent = new Intent(this, ConversationActivity.class);
    intent.putExtra(CHAT_ID_EXTRA, chatId);
    intent.putExtra(FROM_ARCHIVED_CHATS_EXTRA, true);
    if (isRelayingMessageContent(this)) {
      acquireRelayMessageContent(this, intent);

      // Just finish instead of updating the title and so on. This is not user-visible
      // because the ConversationActivity will restart the ConversationListArchiveActivity
      // after the user left.
      finish();
    }
    startActivity(intent);

    overridePendingTransition(R.anim.slide_from_right, R.anim.fade_scale_out);
  }

  @Override
  public void onSwitchToArchive() {
    throw new AssertionError();
  }
}
