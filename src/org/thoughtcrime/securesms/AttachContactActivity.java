package org.thoughtcrime.securesms;

import android.content.Intent;

import org.thoughtcrime.securesms.connect.DcHelper;

public class AttachContactActivity extends ContactSelectionActivity {

  public static final String CONTACT_ID_EXTRA = "contact_id_extra";

  @Override
  public void onContactSelected(int specialId, String addr) {
    Intent intent = new Intent();
    int contactId = DcHelper.getContext(this).lookupContactIdByAddr(addr);
    intent.putExtra(CONTACT_ID_EXTRA, contactId);
    setResult(RESULT_OK, intent);
    finish();
  }
}
