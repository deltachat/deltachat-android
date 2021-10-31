package org.thoughtcrime.securesms.qr;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Util;

public class QrShowActivity extends AppCompatActivity {

    private final DynamicTheme dynamicTheme = new DynamicTheme();
    private final DynamicLanguage dynamicLanguage = new DynamicLanguage();
    private int chatId = 0;

    public final static String CHAT_ID = "chat_id";

    DcEventCenter dcEventCenter;

    DcContext dcContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dynamicTheme.onCreate(this);
        dynamicLanguage.onCreate(this);

        setContentView(R.layout.activity_qr_show);

        dcContext = DcHelper.getContext(this);
        dcEventCenter = DcHelper.getEventCenter(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            chatId = extras.getInt(CHAT_ID);
        }

        ActionBar supportActionBar = getSupportActionBar();
        assert supportActionBar != null;
        supportActionBar.setDisplayHomeAsUpEnabled(true);

        if (chatId != 0) {
            // verified-group
            String groupName = dcContext.getChat(chatId).getName();
            supportActionBar.setTitle(groupName);
            supportActionBar.setSubtitle(R.string.qrshow_join_group_title);
        } else {
            // verify-contact
            String selfName = DcHelper.get(this, DcHelper.CONFIG_DISPLAY_NAME); // we cannot use MrContact.getDisplayName() as this would result in "Me" instead of
            if (selfName.isEmpty()) {
                selfName = DcHelper.get(this, DcHelper.CONFIG_ADDRESS, "unknown");
            }
            supportActionBar.setTitle(selfName);
            supportActionBar.setSubtitle(R.string.qrshow_join_contact_title);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.qr_show, menu);
      return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        dynamicTheme.onResume(this);
        dynamicLanguage.onResume(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.copy:
                Util.writeTextToClipboard(this, DcHelper.getContext(this).getSecurejoinQr(chatId));
                Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
                break;
        }

        return false;
    }
}
