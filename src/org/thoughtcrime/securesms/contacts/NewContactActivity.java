package org.thoughtcrime.securesms.contacts;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import com.b44t.messenger.DcContext;
import com.google.android.material.textfield.TextInputEditText;

import org.thoughtcrime.securesms.PassphraseRequiredActionBarActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.ViewUtil;

public class NewContactActivity extends PassphraseRequiredActionBarActivity
{

  public static final String ADDR_EXTRA = "contact_addr";

  private TextInputEditText nameInput;
  private TextInputEditText addrInput;
  private DcContext dcContext;

  @Override
  protected void onCreate(Bundle state, boolean ready) {
    dcContext = DcHelper.getContext(this);
    setContentView(R.layout.new_contact_activity);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setTitle(R.string.menu_new_contact);
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
    }

    nameInput = ViewUtil.findById(this, R.id.name_text);
    addrInput = ViewUtil.findById(this, R.id.email_text);

    addrInput.setText(getIntent().getStringExtra(ADDR_EXTRA));
    addrInput.setOnFocusChangeListener((view, focused) -> {
        if(!focused && !dcContext.mayBeValidAddr(addrInput.getText().toString())) {
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
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
      case R.id.menu_create_contact:
        String addr = addrInput.getText().toString();
        if(!dcContext.mayBeValidAddr(addr)) {
          Toast.makeText(this, getString(R.string.login_error_mail), Toast.LENGTH_LONG).show();
          return true;
        }
        dcContext.createContact(nameInput.getText().toString(), addr);
        finish();
        return true;
    }

    return false;
  }
}
