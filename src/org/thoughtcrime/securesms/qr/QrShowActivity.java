package org.thoughtcrime.securesms.qr;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.b44t.messenger.DcEventCenter;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;

public class QrShowActivity extends AppCompatActivity {

	private final DynamicTheme dynamicTheme = new DynamicTheme(); //needs to stay when using qrShowFragment because I deleted it there
	private final DynamicLanguage dynamicLanguage = new DynamicLanguage(); //needs to stay when using qrShowFragment because I deleted it there

	public final static int WHITE = 0xFFFFFFFF;
	public final static int BLACK = 0xFF000000;
	public final static int WIDTH = 400;
	public final static int HEIGHT = 400;
	public final static String CHAT_ID = "chat_id";

	//public int numJoiners;

	DcEventCenter dcEventCenter;

	ApplicationDcContext dcContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dynamicTheme.onCreate(this);
		dynamicLanguage.onCreate(this);

		setContentView(R.layout.activity_qr_show);

		dcContext = DcHelper.getContext(this);
		dcEventCenter = dcContext.eventCenter;

		Bundle extras = getIntent().getExtras();
		int chatId = 0;
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
	protected void onResume() {
		super.onResume();
		dynamicTheme.onResume(this);
		dynamicLanguage.onResume(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}

		return false;
	}
}
