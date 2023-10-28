package org.thoughtcrime.securesms.qr;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import org.thoughtcrime.securesms.BaseActionBarActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.mms.AttachmentManager;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class QrActivity extends BaseActionBarActivity {

    private final static String TAG = QrActivity.class.getSimpleName();
    private final static int REQUEST_CODE_IMAGE = 46243;
    private final static int TAB_SHOW = 0;
    private final static int TAB_SCAN = 1;

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private QrShowFragment qrShowFragment;

    @Override
    protected void onPreCreate() {
        dynamicTheme = new DynamicNoActionBarTheme();
        super.onPreCreate();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_qr);
        tabLayout = ViewUtil.findById(this, R.id.tab_layout);
        viewPager = ViewUtil.findById(this, R.id.pager);
        ProfilePagerAdapter adapter = new ProfilePagerAdapter(this, getSupportFragmentManager());
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
                QrActivity.this.invalidateOptionsMenu();
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
            case R.id.share:
                qrShowFragment.shareQr();
                break;
            case R.id.copy:
                qrShowFragment.copyQrData();
                break;
            case R.id.withdraw:
                qrShowFragment.withdrawQr();
                break;
            case R.id.load_from_image:
                AttachmentManager.selectImage(this, REQUEST_CODE_IMAGE);
                break;
            case R.id.paste:
                QrCodeHandler qrCodeHandler = new QrCodeHandler(this);
                qrCodeHandler.handleQrData(Util.getTextFromClipboard(this));
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

    @Override
    public void onActivityResult(int reqCode, int resultCode, final Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK)
            return;

        switch (reqCode) {
            case REQUEST_CODE_IMAGE:
                Uri uri  = (data != null ? data.getData() : null);
                if (uri != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        if (bitmap == null) {
                            Log.e(TAG, "uri is not a bitmap: " + uri.toString());
                            return;
                        }
                        int width = bitmap.getWidth(), height = bitmap.getHeight();
                        int[] pixels = new int[width * height];
                        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                        bitmap.recycle();
                        bitmap = null;
                        RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
                        BinaryBitmap bBitmap = new BinaryBitmap(new HybridBinarizer(source));
                        MultiFormatReader reader = new MultiFormatReader();
                        try {
                            Result result = reader.decode(bBitmap);
                            QrCodeHandler qrCodeHandler = new QrCodeHandler(this);
                            qrCodeHandler.handleQrData(result.getText());
                        } catch (NotFoundException e) {
                            Log.e(TAG, "decode exception", e);
                            Toast.makeText(this, getString(R.string.qrscan_failed), Toast.LENGTH_LONG).show();
                        }
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "can not open file: " + uri.toString(), e);
                    }
                }
                break;
        }
    }

    private class ProfilePagerAdapter extends FragmentStatePagerAdapter {

        private final QrActivity activity;

        ProfilePagerAdapter(QrActivity activity, FragmentManager fragmentManager) {
            super(fragmentManager, FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.activity = activity;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Fragment fragment;

            switch (position) {
                case TAB_SHOW:
                    activity.qrShowFragment = new QrShowFragment();
                    fragment = activity.qrShowFragment;
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
