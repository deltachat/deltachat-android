package org.thoughtcrime.securesms.qr;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.qr.QrScanFragment;

import java.util.ArrayList;

public class QrScanActivity extends AppCompatActivity {

    private final DynamicTheme dynamicTheme = new DynamicTheme();
    private final DynamicLanguage dynamicLanguage = new DynamicLanguage();
    private ViewPager viewPager;
    private ArrayList<Integer> tabs = new ArrayList<>();
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dynamicTheme.onCreate(this);
        dynamicLanguage.onCreate(this);

        setContentView(R.layout.activity_qr_scan);
        this.tabLayout = ViewUtil.findById(this, R.id.tab_layout);
        this.viewPager = ViewUtil.findById(this, R.id.pager);
        this.viewPager.setAdapter(new ProfilePagerAdapter(getSupportFragmentManager()));

        assert getSupportActionBar() != null;
        getSupportActionBar().setTitle(R.string.qrscan_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.tabLayout.setupWithViewPager(viewPager);

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
        return super.onKeyDown(keyCode, event);
    }


    private class ProfilePagerAdapter extends FragmentStatePagerAdapter {

        ProfilePagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            int tabId = tabs.get(position);
            Fragment fragment;
            Bundle args = new Bundle();

            switch (tabId) {
                case 0:
                    fragment = new QrScanFragment();
                    break;

                default:
                    fragment = new QrShowFragment();
                    break;
            }

            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}