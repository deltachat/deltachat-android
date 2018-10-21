/*
 * Copyright (C) 2015 Open Whisper Systems
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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.ThreadDatabase;
import org.thoughtcrime.securesms.recipients.Recipient;

/**
 * Activity container for starting a new conversation.
 *
 * @author Moxie Marlinspike
 *
 */
public class NewConversationActivity extends ContactSelectionActivity {

  @SuppressWarnings("unused")
  private static final String TAG = NewConversationActivity.class.getSimpleName();

  @Override
  public void onCreate(Bundle bundle, boolean ready) {
    super.onCreate(bundle, ready);
    assert getSupportActionBar() != null;
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }

  @Override
  public void onContactSelected(int specialId, String addr) {
    final DcContext dcContext = DcHelper.getContext(this);
    if(specialId==DcContact.DC_CONTACT_ID_NEW_GROUP || specialId==DcContact.DC_CONTACT_ID_NEW_VERIFIED_GROUP) {
      Intent intent = new Intent(this, GroupCreateActivity.class);
      intent.putExtra(GroupCreateActivity.GROUP_CREATE_VERIFIED_EXTRA, specialId==DcContact.DC_CONTACT_ID_NEW_VERIFIED_GROUP);
      startActivity(intent);
      finish();
    }
    else {
      if(!dcContext.mayBeValidAddr(addr)) {
        Toast.makeText(this, R.string.bad_email_address, Toast.LENGTH_LONG).show();
        return;
      }

      int contactId = dcContext.lookupContactIdByAddr(addr);
      int chatId = dcContext.getChatIdByContactId(contactId);
      if (chatId == 0) {
        String nameNAddr = contactId==0? addr : dcContext.getContact(contactId).getNameNAddr();
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.new_conversation_activity__ask_start_chat_with, nameNAddr))
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    int contactId = dcContext.createContact(null, addr);
                    if(contactId==0) {
                      Toast.makeText(NewConversationActivity.this, R.string.bad_email_address, Toast.LENGTH_LONG).show();
                      return;
                    }
                    openConversation(dcContext.createChatByContactId(contactId));
                  }
                }).show();
      } else {
        openConversation(chatId);
      }
    }
  }

  private void openConversation(int chatId) {
    final DcContext dcContext = DcHelper.getContext(this);

    Intent intent = new Intent(this, ConversationActivity.class);
    intent.putExtra(ConversationActivity.TEXT_EXTRA, getIntent().getStringExtra(ConversationActivity.TEXT_EXTRA));
    intent.setDataAndType(getIntent().getData(), getIntent().getType());

    intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, (long)chatId);
    intent.putExtra(ConversationActivity.DISTRIBUTION_TYPE_EXTRA, ThreadDatabase.DistributionTypes.DEFAULT);
    startActivity(intent);
    finish();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
    case android.R.id.home:   super.onBackPressed(); return true;
    //case R.id.menu_refresh:   handleManualRefresh(); return true; -- seems unneeded for delta
    case R.id.menu_new_group: handleCreateGroup();   return true;
    case R.id.menu_invite:    handleInvite();        return true;
    }

    return false;
  }

  /* seems unneeded for delta
  private void handleManualRefresh() {
    contactsFragment.setRefreshing(true);
    onRefresh();
  }
  */

  private void handleCreateGroup() {
    startActivity(new Intent(this, GroupCreateActivity.class));
  }

  private void handleInvite() {
    startActivity(new Intent(this, InviteActivity.class));
  }

  @Override
  protected boolean onPrepareOptionsPanel(View view, Menu menu) {
    /* currently not needed
    MenuInflater inflater = this.getMenuInflater();
    menu.clear();
    inflater.inflate(R.menu.new_conversation_activity, menu);
    */
    super.onPrepareOptionsMenu(menu);
    return true;
  }
}
