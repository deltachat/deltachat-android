package org.thoughtcrime.securesms.contacts;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import com.b44t.messenger.DcContext;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.thoughtcrime.securesms.ConversationActivity;
import org.thoughtcrime.securesms.PassphraseRequiredActionBarActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.qr.QrCodeHandler;
import org.thoughtcrime.securesms.util.ViewUtil;

import chat.delta.rpc.types.SecurejoinUiPath;

public class NewContactActivity extends PassphraseRequiredActionBarActivity
{

  public static final String ADDR_EXTRA = "contact_addr";
  public static final String CONTACT_ID_EXTRA = "contact_id";

  private TextInputEditText nameInput;
  private TextInputEditText addrInput;
  private DcContext dcContext;

  @Override
  protected void onCreate(Bundle state, boolean ready) {
    dcContext = DcHelper.getContext(this);
    setContentView(R.layout.new_contact_activity);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setTitle(R.string.menu_new_classic_contact);
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
    }

    // add padding to avoid content hidden behind system bars
    ViewUtil.applyWindowInsets(findViewById(R.id.content_container));

    nameInput = ViewUtil.findById(this, R.id.name_text);
    addrInput = ViewUtil.findById(this, R.id.email_text);
    addrInput.setText(getIntent().getStringExtra(ADDR_EXTRA));
    addrInput.setOnFocusChangeListener((view, focused) -> {
        String addr = addrInput.getText() == null? "" : addrInput.getText().toString();
        if(!focused && !dcContext.mayBeValidAddr(addr)) {
          addrInput.setError(getString(R.string.login_error_mail));
        }
    });
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuInflater inflater = this.getMenuInflater();
    menu.clear();
    inflater.inflate(R.menu.new_contact, menu);
    super.onPrepareOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    super.onOptionsItemSelected(item);
    int itemId = item.getItemId();
    if (itemId == android.R.id.home) {
      finish();
      return true;
    } else if (itemId == R.id.menu_create_contact) {
      String addr = addrInput.getText() == null ? "" : addrInput.getText().toString();
      String name = nameInput.getText() == null ? "" : nameInput.getText().toString();
      if (name.isEmpty()) name = null;
      int contactId = dcContext.mayBeValidAddr(addr) ? dcContext.createContact(name, addr) : 0;
      if (contactId == 0) {
        Toast.makeText(this, getString(R.string.login_error_mail), Toast.LENGTH_LONG).show();
        return true;
      }
      if (getCallingActivity() != null) { // called for result
        Intent intent = new Intent();
        intent.putExtra(CONTACT_ID_EXTRA, contactId);
        setResult(RESULT_OK, intent);
      } else {
        int chatId = dcContext.createChatByContactId(contactId);
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId);
        startActivity(intent);
      }
      finish();
      return true;
    }
    return false;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == IntentIntegrator.REQUEST_CODE) {
      IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
      QrCodeHandler qrCodeHandler = new QrCodeHandler(this);
      qrCodeHandler.onScanPerformed(scanResult, SecurejoinUiPath.NewContact);
    }
  }
}
