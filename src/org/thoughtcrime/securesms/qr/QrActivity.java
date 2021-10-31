package org.thoughtcrime.securesms.qr;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.thoughtcrime.securesms.BaseActionBarActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

public class QrActivity extends BaseActionBarActivity {

    private final DynamicTheme dynamicTheme = new DynamicNoActionBarTheme();
    private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

    private final static int TAB_SHOW = 0;
    private final static int TAB_SCAN = 1;

    private TabLayout tabLayout;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dynamicTheme.onCreate(this);
        dynamicLanguage.onCreate(this);

        setContentView(R.layout.activity_qr);
        tabLayout = ViewUtil.findById(this, R.id.tab_layout);
        viewPager = ViewUtil.findById(this, R.id.pager);
        ProfilePagerAdapter adapter = new ProfilePagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);

        setSupportActionBar(ViewUtil.findById(this, R.id.toolbar));
        assert getSupportActionBar() != null;
        getSupportActionBar().setTitle(R.string.qr_code);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        int lastSelectedTab = PreferenceManager.getDefaultSharedPreferences(this).getInt("qrTab", TAB_SHOW);
        viewPager.setCurrentItem(lastSelectedTab);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                checkPermissions(position, adapter, viewPager);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        tabLayout.setupWithViewPager(viewPager);

        checkPermissions(lastSelectedTab, adapter, viewPager);
    }

    private void checkPermissions(int position, ProfilePagerAdapter adapter, ViewPager viewPager) {
        if (position == TAB_SCAN) {
            Permissions.with(QrActivity.this)
                    .request(Manifest.permission.CAMERA)
                    .ifNecessary()
                    .withPermanentDenialDialog(getString(R.string.perm_explain_access_to_camera_denied))
                    .onAllGranted(() -> ((QrScanFragment) adapter.getItem(TAB_SCAN)).handleQrScanWithPermissions(QrActivity.this))
                    .onAnyDenied(() -> viewPager.setCurrentItem(TAB_SHOW))
                    .execute();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
      menu.clear();
      if(tabLayout.getSelectedTabPosition() == TAB_SHOW) {
        getMenuInflater().inflate(R.menu.qr_show, menu);
      } else {
        getMenuInflater().inflate(R.menu.qr_scan, menu);
      }
      return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();

        dynamicTheme.onResume(this);
        dynamicLanguage.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        int currentSelectedTab = tabLayout.getSelectedTabPosition();
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("qrTab", currentSelectedTab).apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.copy:
                Util.writeTextToClipboard(this, DcHelper.getContext(this).getSecurejoinQr(0));
                Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
                break;
            case R.id.paste:
                QrCodeHandler qrCodeHandler = new QrCodeHandler(this);
                qrCodeHandler.handleOpenPgp4Fpr(Util.getTextFromClipboard(this));
                break;
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
        if (permissions.length > 0
                && Manifest.permission.CAMERA.equals(permissions[0])
                && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            viewPager.setCurrentItem(TAB_SHOW);
            // Workaround because sometimes something else requested the permissions before this class
            // (probably the CameraView) and then this class didn't notice when it was denied
        }
    }

    private class ProfilePagerAdapter extends FragmentStatePagerAdapter {

        ProfilePagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager, FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Fragment fragment;

            switch (position) {
                case TAB_SHOW:
                    fragment = new QrShowFragment();
                    break;

                default:
                    fragment = new QrScanFragment();
                    break;
            }

            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case TAB_SHOW:
                    return getString(R.string.qrshow_title);

                default:
                    return getString(R.string.qrscan_title);
            }
        }
    }
}
