package org.thoughtcrime.securesms.qr;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.service.GenericForegroundService;
import org.thoughtcrime.securesms.service.NotificationController;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;

public class BackupProviderActivity extends AppCompatActivity {

    private final DynamicTheme dynamicTheme = new DynamicTheme();
    private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

    NotificationController notificationController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dynamicTheme.onCreate(this);
        dynamicLanguage.onCreate(this);
        notificationController = GenericForegroundService.startForegroundTask(this, getString(R.string.multidevice_title));

        setContentView(R.layout.backup_provider_activity);

        ActionBar supportActionBar = getSupportActionBar();
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        supportActionBar.setTitle(R.string.multidevice_title);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            notificationController.close();
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

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return false;
    }
}
