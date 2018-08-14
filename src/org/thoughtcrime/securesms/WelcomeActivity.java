package org.thoughtcrime.securesms;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
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

import org.thoughtcrime.securesms.permissions.Permissions;

/**
 * The welcome activity.  Provides the user with useful information about the app.
 * It also allows the triggering of the backup import process.
 *
 * @author Daniel Böhrs
 */
public class WelcomeActivity extends BaseActionBarActivity {

    private class WelcomePagerAdapter extends PagerAdapter {

        private static final int PAGE_COUNT = 7;

        private Context context;

        private int icons[] = new int[]{R.drawable.intro1, R.drawable.intro2, R.drawable.intro3, R.drawable.intro4, R.drawable.intro5, R.drawable.intro6, R.drawable.intro7};

        private int titles[] = new int[]{R.string.Intro1Headline, R.string.Intro2Headline, R.string.Intro3Headline, R.string.Intro4Headline, R.string.Intro5Headline, R.string.Intro6Headline, R.string.Intro7Headline};

        private int messages[] = new int[]{R.string.Intro1Message, R.string.Intro2Message, R.string.Intro3Message, R.string.Intro4Message, R.string.Intro5Message, R.string.Intro6Message, R.string.Intro7Message};

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
        backupText.setOnClickListener((view) -> initializePermissions());
        backupImage.setOnClickListener((view) -> initializePermissions());
    }

    private void startRegistrationActivity() {
        Intent intent = new Intent(this, RegistrationActivity.class);
        startActivity(intent);
        finish();
    }

    @SuppressLint("InlinedApi")
    private void initializePermissions() {
        Permissions.with(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .ifNecessary()
                .withRationaleDialog("Delta Chat needs access to your files in order start the backup import",
                        R.drawable.ic_folder_white_48dp)
                .onAllGranted(() -> {
                    //JavaBindings.loadBackup(); TODO load backup value via bindings
                })
                .execute();
    }

}
