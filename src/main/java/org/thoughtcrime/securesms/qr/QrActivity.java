package org.thoughtcrime.securesms.qr;

import android.Manifest;
import android.app.Activity;
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
import androidx.constraintlayout.widget.Guideline;
import com.b44t.messenger.DcContext;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.client.android.Intents;
import com.google.zxing.common.HybridBinarizer;
import java.io.FileNotFoundException;
import java.io.InputStream;
import org.thoughtcrime.securesms.BaseActionBarActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.NewContactActivity;
import org.thoughtcrime.securesms.mms.AttachmentManager;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

public class QrActivity extends BaseActionBarActivity {

  private static final String TAG = "QrActivity";
  public static final String EXTRA_SCAN_RELAY = "scan_relay";

  private static final int REQUEST_CODE_IMAGE = 46243;

  private QrShowView qrShowView;
  private QrScanFragment qrScanFragment;
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
    ViewUtil.applyWindowInsetsAsMargin(
        findViewById(R.id.qrScannerFragment), false, true, false, false);

    scanRelay = getIntent().getBooleanExtra(EXTRA_SCAN_RELAY, false);

    setSupportActionBar(ViewUtil.findById(this, R.id.toolbar));
    assert getSupportActionBar() != null;
    getSupportActionBar().setTitle(scanRelay ? R.string.add_transport : R.string.menu_new_contact);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    qrScanFragment =
        (QrScanFragment) getSupportFragmentManager().findFragmentById(R.id.qrScannerFragment);
    qrShowView = findViewById(R.id.qr_show_view);

    Guideline scannerGuideline = findViewById(R.id.scanner_guideline);
    scannerGuideline.setGuidelinePercent(scanRelay ? 1.0f : 0.5f);

    if (scanRelay) {
      qrShowView.setVisibility(View.GONE);
    } else {
      BottomSheetBehavior<QrShowView> behavior = BottomSheetBehavior.from(qrShowView);
      behavior.setFitToContents(false);
      behavior.setHalfExpandedRatio(0.5f);
      behavior.setDraggable(true);
      behavior.setDraggableOnNestedScroll(true);
      behavior.setHideable(false);

      behavior.addBottomSheetCallback(
          new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
              updateSheetViewport(behavior, newState);
              if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
              }
              if (qrScanFragment != null) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                  qrScanFragment.setScanningSuppressed(true);
                } else if (newState == BottomSheetBehavior.STATE_HALF_EXPANDED
                    || newState == BottomSheetBehavior.STATE_COLLAPSED) {
                  qrScanFragment.setScanningSuppressed(false);
                }
              }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
          });
      qrShowView.addOnLayoutChangeListener(
          (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
              updateSheetViewport(behavior, behavior.getState()));

      AppBarLayout appBar = findViewById(R.id.app_bar);
      appBar.addOnLayoutChangeListener(
          (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
              behavior.setExpandedOffset(v.getHeight()));

      qrShowView.bind(0, this::finish);
      qrShowView.activate();

      behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
    }

    checkPermissions();
  }

  private void checkPermissions() {
    Permissions.with(this)
        .request(Manifest.permission.CAMERA)
        .ifNecessary()
        .withPermanentDenialDialog(getString(R.string.perm_explain_access_to_camera_denied))
        .onAllGranted(
            () -> {
              if (qrScanFragment != null) {
                qrScanFragment.handleQrScanWithPermissions(this);
              }
            })
        .onAnyDenied(
            () -> {
              if (scanRelay) {
                Toast.makeText(this, R.string.chat_camera_unavailable, Toast.LENGTH_LONG).show();
              }
            })
        .execute();
  }

  private void updateSheetViewport(BottomSheetBehavior<QrShowView> behavior, int state) {
    View parent = (View) qrShowView.getParent();
    int parentHeight = parent.getHeight();

    int top;
    if (state == BottomSheetBehavior.STATE_HALF_EXPANDED) {
      top = (int) (parentHeight * (1 - behavior.getHalfExpandedRatio()));
    } else if (state == BottomSheetBehavior.STATE_EXPANDED) {
      top = behavior.getExpandedOffset();
    } else {
      return;
    }

    qrShowView.setHiddenHeight(Math.max(0, top + qrShowView.getHeight() - parentHeight));
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    DcContext dcContext = DcHelper.getContext(this);

    menu.clear();
    getMenuInflater().inflate(R.menu.qr_show, menu);
    menu.findItem(R.id.new_classic_contact)
        .setVisible(!scanRelay && dcContext.getConfigInt(DcHelper.CONFIG_FORCE_ENCRYPTION) == 0);
    menu.findItem(R.id.withdraw).setVisible(!scanRelay);

    Util.redMenuItem(menu, R.id.withdraw);
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
      qrShowView.withdrawQr();
    } else if (itemId == R.id.load_from_image) {
      AttachmentManager.selectImage(this, REQUEST_CODE_IMAGE);
    } else if (itemId == R.id.paste) {
      setQrResult(Util.getTextFromClipboard(this));
    }

    return false;
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);

    if (scanRelay
        && permissions.length > 0
        && Manifest.permission.CAMERA.equals(permissions[0])
        && grantResults[0] == PackageManager.PERMISSION_DENIED) {
      Toast.makeText(this, R.string.chat_camera_unavailable, Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public void onActivityResult(int reqCode, int resultCode, final Intent data) {
    super.onActivityResult(reqCode, resultCode, data);

    if (resultCode != Activity.RESULT_OK) return;

    switch (reqCode) {
      case REQUEST_CODE_IMAGE:
        Uri uri = (data != null ? data.getData() : null);
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
              setQrResult(result.getText());
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

  private void setQrResult(String qrData) {
    Intent intent = new Intent();
    intent.putExtra(Intents.Scan.RESULT, qrData);
    setResult(RESULT_OK, intent);
    finish();
  }
}
