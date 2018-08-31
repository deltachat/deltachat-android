package org.thoughtcrime.securesms;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEventCenter;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;

import java.io.File;

/**
 * The welcome activity.  Provides the user with useful information about the app.
 * It also allows the triggering of the backup import process.
 *
 * @author Daniel Böhrs
 */
public class WelcomeActivity extends BaseActionBarActivity implements DcEventCenter.DcEventDelegate {

    private class WelcomePagerAdapter extends PagerAdapter {

        private static final int PAGE_COUNT = 7;

        private Context context;

        private int icons[] = new int[]{R.drawable.intro1, R.drawable.intro2, R.drawable.intro3, R.drawable.intro4, R.drawable.intro5, R.drawable.intro6, R.drawable.intro7};

        private int titles[] = new int[]{R.string.WelcomeActivity_intro1_headline, R.string.WelcomeActivity_intro2_headline, R.string.WelcomeActivity_intro3_headline, R.string.WelcomeActivity_intro4_headline, R.string.WelcomeActivity_intro5_headline, R.string.WelcomeActivity_intro6_headline, R.string.WelcomeActivity_intro7_headline};

        private int messages[] = new int[]{R.string.WelcomeActivity_intro1_message, R.string.WelcomeActivity_intro2_message, R.string.WelcomeActivity_intro3_message, R.string.WelcomeActivity_intro4_message, R.string.WelcomeActivity_intro5_message, R.string.WelcomeActivity_intro6_message, R.string.WelcomeActivity_intro7_message};

        WelcomePagerAdapter(Context context) {
            this.context = context;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            LayoutInflater inflater = LayoutInflater.from(context);
            ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.welcome_page, container, false);
            TextView welcomeHeader = layout.findViewById(R.id.welcome_header);
            welcomeHeader.setText(titles[position]);
            TextView welcomeSubHeader = layout.findViewById(R.id.welcome_sub_header);
            welcomeSubHeader.setText(messages[position]);
            ImageView welcomeIcon = layout.findViewById(R.id.welcome_icon);
            welcomeIcon.setImageResource(icons[position]);
            container.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }


        @Override
        public int getCount() {
            return PAGE_COUNT;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.welcome_activity);

        initializeResources();

        ApplicationDcContext dcContext = DcHelper.getContext(this);
        dcContext.eventCenter.addObserver(this, DcContext.DC_EVENT_CONFIGURE_PROGRESS);
        dcContext.eventCenter.addObserver(this, DcContext.DC_EVENT_IMEX_PROGRESS);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DcHelper.getContext(this).eventCenter.removeObservers(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private void initializeResources() {
        ViewPager viewPager = findViewById(R.id.welcome_viewpager);
        viewPager.setAdapter(new WelcomePagerAdapter(this));
        TabLayout tabLayout = findViewById(R.id.welcome_page_tab_layout);
        tabLayout.setupWithViewPager(viewPager, true);
        Button skipButton = findViewById(R.id.skip_button);
        View backupText = findViewById(R.id.backup_text);
        View backupImage = findViewById(R.id.backup_icon);
        skipButton.setOnClickListener((view) -> startRegistrationActivity());
        backupText.setOnClickListener((view) -> startImportBackup());
        backupImage.setOnClickListener((view) -> startImportBackup());
    }

    private void startRegistrationActivity() {
        Intent intent = new Intent(this, RegistrationActivity.class);
        startActivity(intent);
        // no finish() here, the back key should take the user back from RegistrationActivity to WelcomeActivity
    }

    @SuppressLint("InlinedApi")
    private void startImportBackup() {
        Permissions.with(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .ifNecessary()
                .withRationaleDialog("Delta Chat needs access to your files in order start the backup import",
                        R.drawable.ic_folder_white_48dp)
                .onAllGranted(() -> {
                    ApplicationDcContext dcContext = DcHelper.getContext(this);
                    File imexDir = dcContext.getImexDir();
                    final String backupFile = dcContext.imexHasBackup(imexDir.getAbsolutePath());
                    if (backupFile != null) {
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.import_backup_title)
                                .setMessage(String.format(getResources().getString(R.string.import_backup_ask), backupFile))
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                        startImport(backupFile);
                                })
                                .show();
                    }
                    else {
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.import_backup_title)
                                .setMessage(String.format(getResources().getString(R.string.import_backup_no_backup_found), imexDir.getAbsolutePath()))
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    }
                })
                .execute();
    }

    private ProgressDialog progressDialog = null;
    private void startImport(final String backupFile)
    {
        ApplicationDcContext dcContext = DcHelper.getContext(this);

        if( progressDialog!=null ) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.one_moment));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(android.R.string.cancel), (dialog, which) -> {
            dcContext.stopOngoingProcess();
        });
        progressDialog.show();

        dcContext.captureNextError();
        dcContext.imex(DcContext.DC_IMEX_IMPORT_BACKUP, backupFile);
    }

    public void handleEvent(int eventId, Object data1, Object data2) {
        if (eventId== DcContext.DC_EVENT_IMEX_PROGRESS) {
            ApplicationDcContext dcContext = DcHelper.getContext(this);
            long progress = (Long)data1;
            if (progress==0/*error/aborted*/) {
                dcContext.endCaptureNextError();
                progressDialog.dismiss();
                if (dcContext.hasCapturedError()) {
                    new AlertDialog.Builder(this)
                            .setMessage(dcContext.getCapturedError())
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            }
            else if (progress<1000/*progress in permille*/) {
                int percent = (int)progress / 10;
                progressDialog.setMessage(getResources().getString(R.string.one_moment)+String.format(" %d%%", percent));
            }
            else if (progress==1000/*done*/) {
                dcContext.endCaptureNextError();
                progressDialog.dismiss();
                Intent conversationList = new Intent(getApplicationContext(), ConversationListActivity.class);
                startActivity(conversationList);
                finish();
            }
        }
        else if (eventId== DcContext.DC_EVENT_CONFIGURE_PROGRESS) {
            long progress = (Long)data1;
            if (progress==1000/*done*/) {
                finish(); // remove ourself from the activity stack (finishAffinity is available in API 16, we're targeting API 14)
            }
        }
    }
}
