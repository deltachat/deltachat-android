package org.thoughtcrime.securesms;

import android.content.Intent;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.connect.DcHelper;

public class AttachContactActivity extends ContactSelectionActivity {

  public static final String SHARE_CONTACT_NAME_EXTRA = "share_contact_name";
  public static final String SHARE_CONTACT_MAIL_EXTRA = "share_contact_mail";

  @Override
  public void onContactSelected(int specialId, String addr) {
    String name = "";
    DcContext dcContext = DcHelper.getContext(this);
    int contactId = dcContext.lookupContactIdByAddr(addr);
    if (contactId != 0) {
      name = dcContext.getContact(contactId).getName();
    }
    Intent intent = new Intent();
    intent.putExtra(SHARE_CONTACT_NAME_EXTRA, name);
    intent.putExtra(SHARE_CONTACT_MAIL_EXTRA, addr);
    setResult(RESULT_OK, intent);
    finish();
  }
}
