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
import android.net.MailTo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.connect.DcHelper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import static org.thoughtcrime.securesms.ConversationActivity.CHAT_ID_EXTRA;
import static org.thoughtcrime.securesms.ConversationActivity.TEXT_EXTRA;
import static org.thoughtcrime.securesms.util.RelayUtil.REQUEST_RELAY;
import static org.thoughtcrime.securesms.util.RelayUtil.acquireRelayMessageContent;
import static org.thoughtcrime.securesms.util.RelayUtil.isRelayingMessageContent;

/**
 * Activity container for starting a new conversation.
 *
 * @author Moxie Marlinspike
 *
 */
public class NewConversationActivity extends ContactSelectionActivity {

  @SuppressWarnings("unused")
  private static final String TAG = NewConversationActivity.class.getSimpleName();
  public  static final String MAILTO = "mailto";
  private static final String SUBJECT = "subject";
  private static final String BODY = "body";
  private static final String QUERY_SEPARATOR = "&";
  private static final String KEY_VALUE_SEPARATOR = "=";

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
          if(scheme != null && scheme.equals(MAILTO) ) {
            String textToShare = getTextToShare(uri);
            MailTo mailto = MailTo.parse(uri.toString());
            String recipientsList = mailto.getTo();
            if(recipientsList != null && !recipientsList.trim().isEmpty()) {
              String[] recipientsArray = recipientsList.trim().split(",");
              if (recipientsArray.length >= 1) {
                String recipient = recipientsArray[0];
                if (textToShare != null && !textToShare.isEmpty()) {
                  getIntent().putExtra(TEXT_EXTRA, textToShare);
                }
                onContactSelected(DcContact.DC_CONTACT_ID_NEW_CONTACT, recipient);
              }
            } else {
              Intent shareIntent = new Intent(this, ShareActivity.class);
              shareIntent.putExtra(Intent.EXTRA_TEXT, textToShare);
              startActivity(shareIntent);
              finish();
            }
          } else if(scheme != null && scheme.startsWith("http")) {
            Intent shareIntent = new Intent(this, ShareActivity.class);
            shareIntent.putExtra(Intent.EXTRA_TEXT, uri.toString());
            startActivity(shareIntent);
            finish();
          }
        }
      }
      catch(Exception e) {
        Log.e(TAG, "start activity from external 'mailto:' link failed", e);
      }
    }
  }

  private String getTextToShare(Uri uri) {
    Map<String, String> mailtoQueryMap = getMailtoQueryMap(uri);
    String textToShare = mailtoQueryMap.get(SUBJECT);
    String body = mailtoQueryMap.get(BODY);
    if (body != null && !body.isEmpty()) {
      if (textToShare != null && !textToShare.isEmpty()) {
        textToShare += "\n" + body;
      } else {
        textToShare = body;
      }
    }
    return textToShare;
  }

  private Map<String, String> getMailtoQueryMap(Uri uri) {
    Map<String, String> mailtoQueryMap = new HashMap<>();
    String query =  uri.getEncodedQuery();
    if (query != null && !query.isEmpty()) {
      String[] queryArray = query.split(QUERY_SEPARATOR);
      for(String queryEntry : queryArray) {
        String[] queryEntryArray = queryEntry.split(KEY_VALUE_SEPARATOR);
        try {
          mailtoQueryMap.put(queryEntryArray[0], URLDecoder.decode(queryEntryArray[1], "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      }
    }
    return mailtoQueryMap;
  }

  @Override
  public void onContactSelected(int specialId, String addr) {
    final DcContext dcContext = DcHelper.getContext(this);
    if(specialId == DcContact.DC_CONTACT_ID_NEW_GROUP || specialId == DcContact.DC_CONTACT_ID_NEW_VERIFIED_GROUP) {
      Intent intent = new Intent(this, GroupCreateActivity.class);
      intent.putExtra(GroupCreateActivity.GROUP_CREATE_VERIFIED_EXTRA, specialId == DcContact.DC_CONTACT_ID_NEW_VERIFIED_GROUP);
      startActivity(intent);
      finish();
    } else if(specialId == DcContact.DC_CONTACT_ID_NEW_BROADCAST_LIST) {
      Intent intent = new Intent(this, GroupCreateActivity.class);
      intent.putExtra(GroupCreateActivity.CREATE_BROADCAST, true);
      startActivity(intent);
      finish();
    }
    else {
      if(!dcContext.mayBeValidAddr(addr)) {
        Toast.makeText(this, R.string.bad_email_address, Toast.LENGTH_LONG).show();
        return;
      }

      int contactId = dcContext.lookupContactIdByAddr(addr);
      if (contactId!=0 && dcContext.getChatIdByContactId(contactId)!=0) {
        openConversation(dcContext.createChatByContactId(contactId));
      } else {
        String nameNAddr = contactId == 0 ? addr : dcContext.getContact(contactId).getNameNAddr();
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.ask_start_chat_with, nameNAddr))
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                  int contactId1 = dcContext.createContact(null, addr);
                  if (contactId1 == 0) {
                    Toast.makeText(NewConversationActivity.this, R.string.bad_email_address, Toast.LENGTH_LONG).show();
                    return;
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
      startActivityForResult(intent, REQUEST_RELAY);
    } else {
      startActivity(intent);
      finish();
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
    case android.R.id.home:   super.onBackPressed(); return true;
    }

    return false;
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

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_RELAY && resultCode == RESULT_OK) {
      setResult(RESULT_OK);
      finish();
    }
  }

}
