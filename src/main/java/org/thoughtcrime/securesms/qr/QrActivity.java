package org.thoughtcrime.securesms.qr;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import org.thoughtcrime.securesms.BaseActionBarActivity;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.NewConversationActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.NewContactActivity;
import org.thoughtcrime.securesms.mms.AttachmentManager;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.io.FileNotFoundException;
import java.io.InputStream;

import chat.delta.rpc.types.SecurejoinSource;
import chat.delta.rpc.types.SecurejoinUiPath;

public class QrActivity extends BaseActionBarActivity implements View.OnClickListener {

    private final static String TAG = QrActivity.class.getSimpleName();
    public final static String EXTRA_SCAN_RELAY = "scan_relay";

    private final static int REQUEST_CODE_IMAGE = 46243;
    private final static int TAB_SHOW = 0;
    private final static int TAB_SCAN = 1;

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private QrShowFragment qrShowFragment;
    private boolean scanRelay;

    @Override
    protected void onPreCreate() {
        dynamicTheme = new DynamicNoActionBarTheme();
        super.onPreCreate();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);

        scanRelay = getIntent().getBooleanExtra(EXTRA_SCAN_RELAY, false);

        qrShowFragment = new QrShowFragment(this);
        tabLayout = ViewUtil.findById(this, R.id.tab_layout);
        viewPager = ViewUtil.findById(this, R.id.pager);
        ProfilePagerAdapter adapter = new ProfilePagerAdapter(this, getSupportFragmentManager());
        viewPager.setAdapter(adapter);

        setSupportActionBar(ViewUtil.findById(this, R.id.toolbar));
        assert getSupportActionBar() != null;
        getSupportActionBar().setTitle(scanRelay? R.string.add_transport : R.string.menu_new_contact);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        viewPager.setCurrentItem(scanRelay? TAB_SCAN : TAB_SHOW);
        if (scanRelay) tabLayout.setVisibility(View.GONE);

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
    }

    private void checkPermissions(int position, ProfilePagerAdapter adapter, ViewPager viewPager) {
        if (position == TAB_SCAN) {
            Permissions.with(QrActivity.this)
                    .request(Manifest.permission.CAMERA)
                    .ifNecessary()
                    .withPermanentDenialDialog(getString(R.string.perm_explain_access_to_camera_denied))
                    .onAllGranted(() -> ((QrScanFragment) adapter.getItem(TAB_SCAN)).handleQrScanWithPermissions(QrActivity.this))
                    .onAnyDenied(() -> {
                        if (scanRelay) {
                            Toast.makeText(this, getString(R.string.chat_camera_unavailable), Toast.LENGTH_LONG).show();
                        } else {
                            viewPager.setCurrentItem(TAB_SHOW);
                        }
                     })
                    .execute();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
      menu.clear();
      getMenuInflater().inflate(R.menu.qr_show, menu);
      menu.findItem(R.id.new_classic_contact).setVisible(!scanRelay && !DcHelper.getContext(this).isChatmail());

      Util.redMenuItem(menu, R.id.withdraw);
      if(tabLayout.getSelectedTabPosition() == TAB_SCAN) {
        menu.findItem(R.id.withdraw).setVisible(false);
      }

      return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

      int itemId = item.getItemId();
      if (itemId == android.R.id.home) {
        finish();
        return true;
      } else if (itemId == R.id.new_classic_contact) {
        this.startActivity(new Intent(this, NewContactActivity.class));
      } else if (itemId == R.id.withdraw) {
        qrShowFragment.withdrawQr();
      } else if (itemId == R.id.load_from_image) {
        AttachmentManager.selectImage(this, REQUEST_CODE_IMAGE);
      } else if (itemId == R.id.paste) {
        QrCodeHandler qrCodeHandler = new QrCodeHandler(this);
        qrCodeHandler.handleQrData(Util.getTextFromClipboard(this), SecurejoinSource.Clipboard, getUiPath());
      }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
        if (permissions.length > 0
                && Manifest.permission.CAMERA.equals(permissions[0])
                && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            if (scanRelay) {
                Toast.makeText(this, getString(R.string.chat_camera_unavailable), Toast.LENGTH_LONG).show();
            } else {
                viewPager.setCurrentItem(TAB_SHOW);
            }
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
                            qrCodeHandler.handleQrData(result.getText(), SecurejoinSource.ImageLoaded, getUiPath());
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

    private SecurejoinUiPath getUiPath() {
        SecurejoinUiPath uiPath = null;
        ComponentName caller = this.getCallingActivity();
        if (caller != null) {
            if (caller.getClassName().equals(NewConversationActivity.class.getName())) {
                uiPath = SecurejoinUiPath.NewContact;
            } else if (caller.getClassName().equals(ConversationListActivity.class.getName())
                    // RoutingActivity is an alias for ConversationListActivity
                    || caller.getClassName().endsWith(".RoutingActivity")) {
                uiPath = SecurejoinUiPath.QrIcon;
            }
        }
        return uiPath;
    }

    @Override
    public void onClick(View v) {
        viewPager.setCurrentItem(TAB_SCAN);
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
