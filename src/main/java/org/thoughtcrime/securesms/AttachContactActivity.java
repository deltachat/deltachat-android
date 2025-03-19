package org.thoughtcrime.securesms;

import android.content.Intent;

import org.thoughtcrime.securesms.connect.DcHelper;

public class AttachContactActivity extends ContactSelectionActivity {

  public static final String CONTACT_ID_EXTRA = "contact_id_extra";

  @Override
  public void onContactSelected(int contactId) {
    Intent intent = new Intent();
    intent.putExtra(CONTACT_ID_EXTRA, contactId);
    setResult(RESULT_OK, intent);
    finish();
  }
}
