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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.google.zxing.integration.android.IntentIntegrator;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.qr.QrActivity;
import org.thoughtcrime.securesms.util.MailtoUtil;

import static org.thoughtcrime.securesms.ConversationActivity.CHAT_ID_EXTRA;
import static org.thoughtcrime.securesms.ConversationActivity.TEXT_EXTRA;
import static org.thoughtcrime.securesms.util.RelayUtil.acquireRelayMessageContent;
import static org.thoughtcrime.securesms.util.RelayUtil.isRelayingMessageContent;

/**
 * Activity container for starting a new conversation.
 *
 * @author Moxie Marlinspike
 *
 */
public class NewConversationActivity extends ContactSelectionActivity {

  private static final String TAG = NewConversationActivity.class.getSimpleName();

  @Override
  public void onCreate(Bundle bundle, boolean ready) {
    super.onCreate(bundle, ready);
    assert getSupportActionBar() != null;
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    handleIntent();
  }

  private void handleIntent() {
    Intent intent = getIntent();
    String action = intent.getAction();
    if(Intent.ACTION_VIEW.equals(action) || Intent.ACTION_SENDTO.equals(action)) {
      try {
        Uri uri = intent.getData();
        if(uri != null) {
          String scheme = uri.getScheme();
          if(MailtoUtil.isMailto(uri)) {
            String textToShare = MailtoUtil.getText(uri);
            String[] recipientsArray = MailtoUtil.getRecipients(uri);
            if (recipientsArray.length >= 1) {
              if (!textToShare.isEmpty()) {
                getIntent().putExtra(TEXT_EXTRA, textToShare);
              }
              onContactSelected(DcContact.DC_CONTACT_ID_NEW_CONTACT, recipientsArray[0]);
            } else {
              Intent shareIntent = new Intent(this, ShareActivity.class);
              shareIntent.putExtra(Intent.EXTRA_TEXT, textToShare);
              startActivity(shareIntent);
              finish();
            }
          }
        }
      }
      catch(Exception e) {
        Log.e(TAG, "start activity from external 'mailto:' link failed", e);
      }
    }
  }

  @Override
  public void onContactSelected(int specialId, String addr) {
    final DcContext dcContext = DcHelper.getContext(this);
    if(specialId == DcContact.DC_CONTACT_ID_NEW_GROUP) {
      startActivity(new Intent(this, GroupCreateActivity.class));
    } else if(specialId == DcContact.DC_CONTACT_ID_NEW_BROADCAST_LIST) {
      Intent intent = new Intent(this, GroupCreateActivity.class);
      intent.putExtra(GroupCreateActivity.CREATE_BROADCAST, true);
      startActivity(intent);
    } else if (specialId == DcContact.DC_CONTACT_ID_QR_INVITE) {
      new IntentIntegrator(this).setCaptureActivity(QrActivity.class).initiateScan();
    }
    else {
      int contactId = dcContext.lookupContactIdByAddr(addr);
      if (contactId!=0 && dcContext.getChatIdByContactId(contactId)!=0) {
        openConversation(dcContext.getChatIdByContactId(contactId));
      } else {
        String nameNAddr = contactId == 0 ? addr : dcContext.getContact(contactId).getNameNAddr();
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.ask_start_chat_with, nameNAddr))
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                  int contactId1 = dcContext.lookupContactIdByAddr(addr);
                  if (contactId1 == 0) {
                    contactId1 = dcContext.createContact(null, addr);
                    if (contactId1 == 0) {
                      Toast.makeText(NewConversationActivity.this, R.string.bad_email_address, Toast.LENGTH_LONG).show();
                      return;
                    }
                  }
                  openConversation(dcContext.createChatByContactId(contactId1));
                }).show();
      }
    }
  }

  private void openConversation(int chatId) {
    Intent intent = new Intent(this, ConversationActivity.class);
    intent.putExtra(TEXT_EXTRA, getIntent().getStringExtra(TEXT_EXTRA));
    intent.setDataAndType(getIntent().getData(), getIntent().getType());

    intent.putExtra(CHAT_ID_EXTRA, chatId);
    if (isRelayingMessageContent(this)) {
      acquireRelayMessageContent(this, intent);
    }
    startActivity(intent);
    finish();
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
