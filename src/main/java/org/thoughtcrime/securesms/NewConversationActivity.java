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

import static org.thoughtcrime.securesms.ConversationActivity.CHAT_ID_EXTRA;
import static org.thoughtcrime.securesms.ConversationActivity.TEXT_EXTRA;
import static org.thoughtcrime.securesms.util.ShareUtil.acquireRelayMessageContent;
import static org.thoughtcrime.securesms.util.ShareUtil.isRelayingMessageContent;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.qr.QrActivity;
import org.thoughtcrime.securesms.qr.QrCodeHandler;
import org.thoughtcrime.securesms.util.MailtoUtil;

import chat.delta.rpc.types.SecurejoinUiPath;

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
              final String addr = recipientsArray[0];
              final DcContext dcContext = DcHelper.getContext(this);
              int contactId = dcContext.lookupContactIdByAddr(addr);
              if (contactId == 0 && dcContext.mayBeValidAddr(addr)) {
                contactId = dcContext.createContact(null, recipientsArray[0]);
              }
              if (contactId == 0) {
                Toast.makeText(this, R.string.bad_email_address, Toast.LENGTH_LONG).show();
              } else {
                onContactSelected(contactId);
              }
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
  public void onContactSelected(int contactId) {
    if(contactId == DcContact.DC_CONTACT_ID_NEW_GROUP) {
      startActivity(new Intent(this, GroupCreateActivity.class));
    } else if(contactId == DcContact.DC_CONTACT_ID_NEW_UNENCRYPTED_GROUP) {
      Intent intent = new Intent(this, GroupCreateActivity.class);
      intent.putExtra(GroupCreateActivity.UNENCRYPTED, true);
      startActivity(intent);
    } else if(contactId == DcContact.DC_CONTACT_ID_NEW_BROADCAST) {
      Intent intent = new Intent(this, GroupCreateActivity.class);
      intent.putExtra(GroupCreateActivity.CREATE_BROADCAST, true);
      startActivity(intent);
    } else if (contactId == DcContact.DC_CONTACT_ID_QR_INVITE) {
      new IntentIntegrator(this).setCaptureActivity(QrActivity.class).initiateScan();
    }
    else {
      final DcContext dcContext = DcHelper.getContext(this);
      if (dcContext.getChatIdByContactId(contactId)!=0) {
        openConversation(dcContext.getChatIdByContactId(contactId));
      } else {
        String name = dcContext.getContact(contactId).getDisplayName();
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.ask_start_chat_with, name))
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                  openConversation(dcContext.createChatByContactId(contactId));
                }).show();
      }
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case IntentIntegrator.REQUEST_CODE:
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        QrCodeHandler qrCodeHandler = new QrCodeHandler(this);
        qrCodeHandler.onScanPerformed(scanResult, SecurejoinUiPath.NewContact);
        break;
      default:
        break;
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
}
