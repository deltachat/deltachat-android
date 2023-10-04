package org.thoughtcrime.securesms;

import android.content.Intent;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.connect.DcHelper;

public class AttachContactActivity extends ContactSelectionActivity {

  public static final String NAME_EXTRA = "name_extra";
  public static final String ADDR_EXTRA = "addr_extra";

  @Override
  public void onContactSelected(int specialId, String addr) {
    String name = "";
    DcContext dcContext = DcHelper.getContext(this);
    int contactId = dcContext.lookupContactIdByAddr(addr);
    if (contactId != 0) {
      name = dcContext.getContact(contactId).getDisplayName();
    }
    Intent intent = new Intent();
    intent.putExtra(NAME_EXTRA, name);
    intent.putExtra(ADDR_EXTRA, addr);
    setResult(RESULT_OK, intent);
    finish();
  }
}
