package org.thoughtcrime.securesms;

import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_PROXY_ENABLED;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_PROXY_URL;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.loader.app.LoaderManager;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcLot;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.thoughtcrime.securesms.components.AvatarSelector;
import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.avatars.ResourceContactPhoto;
import org.thoughtcrime.securesms.mms.AttachmentManager;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.profiles.AvatarHelper;
import org.thoughtcrime.securesms.proxy.ProxySettingsActivity;
import org.thoughtcrime.securesms.qr.RegistrationQrActivity;
import org.thoughtcrime.securesms.relay.EditRelayActivity;
import org.thoughtcrime.securesms.relay.RelayListActivity;
import org.thoughtcrime.securesms.scribbles.ScribbleActivity;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.views.ProgressDialog;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Objects;

import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;

public class InstantOnboardingActivity extends BaseActionBarActivity implements DcEventCenter.DcEventDelegate {

  private static final String TAG = InstantOnboardingActivity.class.getSimpleName();
  private static final String DCACCOUNT = "dcaccount";
  private static final String DCLOGIN = "dclogin";
  private static final String INSTANCES_URL = "https://chatmail.at/relays";
  private static final String DEFAULT_CHATMAIL_HOST = "nine.testrun.org";

  public static final String FROM_WELCOME = "from_welcome";
  private static final int REQUEST_CODE_AVATAR = 1;

  private ImageView avatar;
  private EditText name;
  private TextView privacyPolicyBtn;
  private Button signUpBtn;

  private boolean avatarChanged;
  private boolean imageLoaded;
  private String providerHost;
  private String providerQrData;
  private boolean isDcLogin;

  private AttachmentManager attachmentManager;
  private Bitmap avatarBmp;

  private @Nullable ProgressDialog progressDialog;
  private boolean cancelled;

  private DcContext dcContext;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    setContentView(R.layout.instant_onboarding_activity);

    Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.onboarding_create_instant_account);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    boolean fromWelcome  = getIntent().getBooleanExtra(FROM_WELCOME, false);

    if (DcHelper.getContext(this).isConfigured() == 1) {
      // if account is configured it means we didn't come from Welcome screen nor from QR scanner,
      // instead, user clicked a dcaccount:// URI directly, so we need to just offer to add a new relay
      Uri uri = getIntent().getData();
      if (uri != null) {
        Intent intent = new Intent(this, RelayListActivity.class);
        intent.putExtra(RelayListActivity.EXTRA_QR_DATA, uri.toString());
        startActivity(intent);
        finish();
        return;
      }
      // if URI is unexpectedly null, then fallback to new profile creation
      AccountManager.getInstance().beginAccountCreation(this);
    }

    getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(!fromWelcome) {
      @Override
      public void handleOnBackPressed() {
        AccountManager accountManager = AccountManager.getInstance();
        if (accountManager.canRollbackAccountCreation(InstantOnboardingActivity.this)) {
          accountManager.rollbackAccountCreation(InstantOnboardingActivity.this);
        } else {
          finish();
        }
      }
    });

    isDcLogin = false;
    providerHost = DEFAULT_CHATMAIL_HOST;
    providerQrData = DCACCOUNT + ":" + providerHost;
    attachmentManager = new AttachmentManager(this, () -> {});
    avatarChanged = false;
    registerForEvents();
    initializeResources();
    initializeProfile();
    handleIntent();
    updateProvider();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleIntent();
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menu.clear();
    getMenuInflater().inflate(R.menu.instant_onboarding_menu, menu);
    MenuItem proxyItem = menu.findItem(R.id.menu_proxy_settings);
    if (TextUtils.isEmpty(DcHelper.get(this, CONFIG_PROXY_URL))) {
      proxyItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    } else {
      boolean proxyEnabled = DcHelper.getInt(this, CONFIG_PROXY_ENABLED) == 1;
      proxyItem.setIcon(proxyEnabled? R.drawable.ic_proxy_enabled_24 : R.drawable.ic_proxy_disabled_24);
      proxyItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    super.onOptionsItemSelected(item);

    int itemId = item.getItemId();
    if (itemId == android.R.id.home) {
      getOnBackPressedDispatcher().onBackPressed();
      return true;
    } else if (itemId == R.id.menu_proxy_settings) {
      startActivity(new Intent(this, ProxySettingsActivity.class));
      return true;
    } else if (itemId == R.id.menu_view_log) {
      startActivity(new Intent(this, LogViewActivity.class));
      return true;
    }

    return false;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (resultCode != RESULT_OK) {
        return;
    }

    switch (requestCode) {
      case REQUEST_CODE_AVATAR:
        Uri inputFile  = (data != null ? data.getData() : null);
        onFileSelected(inputFile);
        break;

      case ScribbleActivity.SCRIBBLE_REQUEST_CODE:
        setAvatarView(data.getData());
        break;

      case IntentIntegrator.REQUEST_CODE:
        String qrRaw = data.getStringExtra(RegistrationQrActivity.QRDATA_EXTRA);
        if (qrRaw == null) {
          IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
          if (scanResult != null && scanResult.getFormatName() != null) {
            qrRaw = scanResult.getContents();
          }
        }
        if (qrRaw != null) {
          setProviderFromQr(qrRaw);
        }
        break;
    }
  }

  private void setProviderFromQr(String rawQr) {
    DcLot qrParsed = dcContext.checkQr(rawQr);
    boolean isDcLogin = qrParsed.getState() == DcContext.DC_QR_LOGIN;
    if (isDcLogin || qrParsed.getState() == DcContext.DC_QR_ACCOUNT) {
      this.isDcLogin = isDcLogin;
      providerHost = qrParsed.getText1();
      providerQrData = rawQr;
      updateProvider();
    } else {
      new AlertDialog.Builder(this)
        .setMessage(R.string.qraccount_qr_code_cannot_be_used)
        .setPositiveButton(R.string.ok, null)
        .show();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }

  @Override
  protected void onPause() {
    super.onPause();

    // Save display name and avatar in the unconfigured profile.
    // If the currently selected profile is configured, then this means that rollbackAccountCreation()
    // was called (see handleOnBackPressed() above), i.e. the newly created profile was removed already
    // and we can't save the display name & avatar.
    if (DcHelper.getContext(this).isConfigured() == 0) {
      final String displayName = name.getText().toString();
      DcHelper.set(this, DcHelper.CONFIG_DISPLAY_NAME, TextUtils.isEmpty(displayName) ? null : displayName);

      if (avatarChanged) {
        try {
          AvatarHelper.setSelfAvatar(InstantOnboardingActivity.this, avatarBmp);
          Prefs.setProfileAvatarId(InstantOnboardingActivity.this, new SecureRandom().nextInt());
          avatarChanged = false;
        } catch (IOException e) {
          Log.e(TAG, "Failed to save avatar", e);
        }
      }
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    invalidateOptionsMenu();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    DcHelper.getEventCenter(this).removeObservers(this);
  }

  private void handleIntent() {
    if (getIntent() != null && Intent.ACTION_VIEW.equals(getIntent().getAction())) {
      Uri uri = getIntent().getData();
      if (uri == null) return;

      if (uri.getScheme().equalsIgnoreCase(DCACCOUNT) || uri.getScheme().equalsIgnoreCase(DCLOGIN)) {
        setProviderFromQr(uri.toString());
      }
    }
  }

  private void setAvatarView(Uri output) {
    GlideApp.with(this)
      .asBitmap()
      .load(output)
      .skipMemoryCache(true)
      .diskCacheStrategy(DiskCacheStrategy.NONE)
      .centerCrop()
      .override(AvatarHelper.AVATAR_SIZE, AvatarHelper.AVATAR_SIZE)
      .into(new CustomTarget<Bitmap>() {
          @Override
          public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
            avatarChanged = true;
            imageLoaded = true;
            avatarBmp = resource;
          }

          @Override
          public void onLoadCleared(@Nullable Drawable placeholder) {}
      });
    GlideApp.with(this)
            .load(output)
            .circleCrop()
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(avatar);
  }

  private void onFileSelected(Uri inputFile) {
    if (inputFile == null) {
      inputFile = attachmentManager.getImageCaptureUri();
    }

    AvatarHelper.cropAvatar(this, inputFile);
  }

  private void initializeResources() {
    this.avatar           = findViewById(R.id.avatar);
    this.name             = findViewById(R.id.name_text);
    this.privacyPolicyBtn = findViewById(R.id.privacy_policy_button);
    this.signUpBtn        = findViewById(R.id.signup_button);

    // add padding to avoid content hidden behind system bars
    ViewUtil.applyWindowInsets(findViewById(R.id.container));

    privacyPolicyBtn.setOnClickListener(view -> {
      if (!isDcLogin) {
        IntentUtils.showInBrowser(this, "https://" + providerHost + "/privacy.html");
      }
    });

    signUpBtn.setOnClickListener(view -> createProfile());

    Button otherOptionsBtn = findViewById(R.id.other_options_button);
    otherOptionsBtn.setOnClickListener(view -> showOtherOptionsDialog());
  }

  private void showOtherOptionsDialog() {
    View view = View.inflate(this, R.layout.signup_options_view, null);
    AlertDialog signUpDialog = new AlertDialog.Builder(this)
      .setView(view)
      .setTitle(R.string.instant_onboarding_show_more_instances)
      .setNegativeButton(R.string.cancel, null)
      .create();

    view.findViewById(R.id.use_other_server).setOnClickListener((v) -> {
      IntentUtils.showInBrowser(this, INSTANCES_URL);
      signUpDialog.dismiss();
    });
    view.findViewById(R.id.login_button).setOnClickListener((v) -> {
      startActivity(new Intent(this, EditRelayActivity.class));
      signUpDialog.dismiss();
    });
    view.findViewById(R.id.scan_qr_button).setOnClickListener((v) -> {
      new IntentIntegrator(this).setCaptureActivity(RegistrationQrActivity.class).initiateScan();
      signUpDialog.dismiss();
    });

    signUpDialog.show();
  }

  private void updateProvider() {
    if (isDcLogin) {
      signUpBtn.setText(R.string.login_title);
      privacyPolicyBtn.setTextColor(getResources().getColor(R.color.gray50));
      privacyPolicyBtn.setText(getString(R.string.qrlogin_ask_login, providerHost));
    } else {
      signUpBtn.setText(R.string.instant_onboarding_create);
      privacyPolicyBtn.setTextColor(getResources().getColor(R.color.delta_accent));
      if (DEFAULT_CHATMAIL_HOST.equals(providerHost)) {
        privacyPolicyBtn.setText(getString(R.string.instant_onboarding_agree_default2, providerHost));
      } else {
        privacyPolicyBtn.setText(getString(R.string.instant_onboarding_agree_instance, providerHost));
      }
    }
  }

  private void initializeProfile() {
    File avatarFile = AvatarHelper.getSelfAvatarFile(this);
    if (avatarFile.exists() && avatarFile.length() > 0) {
      imageLoaded = true;
      GlideApp.with(this).load(avatarFile).circleCrop().into(avatar);
    } else {
      imageLoaded = false;
      avatar.setImageDrawable(new ResourceContactPhoto(R.drawable.ic_camera_alt_white_24dp).asDrawable(this, getResources().getColor(R.color.grey_400)));
    }
    avatar.setOnClickListener(view ->
      new AvatarSelector(this, LoaderManager.getInstance(this), new AvatarSelectedListener(), imageLoaded).show(this, avatar)
    );

    name.setText(DcHelper.get(this, DcHelper.CONFIG_DISPLAY_NAME));
  }

  private void registerForEvents() {
    dcContext = DcHelper.getContext(this);
    DcEventCenter eventCenter = DcHelper.getEventCenter(this);
    eventCenter.addObserver(DcContext.DC_EVENT_CONFIGURE_PROGRESS, this);
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    int eventId = event.getId();

    if (eventId == DcContext.DC_EVENT_CONFIGURE_PROGRESS) {
      long progress = event.getData1Int();
      progressUpdate((int)progress);
    }
  }

  private void progressUpdate(int progress) {
    int percent = progress / 10;
    if (progressDialog != null) {
      progressDialog.setMessage(getResources().getString(R.string.one_moment)+String.format(" %d%%", percent));
    }
  }

  private void progressError(String data2) {
    if (progressDialog != null) {
      try {
        progressDialog.dismiss();
      } catch (IllegalArgumentException e) {
        // see https://stackoverflow.com/a/5102572/4557005
        Log.w(TAG, e);
      }
    }
    WelcomeActivity.maybeShowConfigurationError(this, data2);
  }

  private void progressSuccess() {
    if (progressDialog != null) {
      progressDialog.dismiss();
    }

    Intent intent = new Intent(getApplicationContext(), ConversationListActivity.class);
    intent.putExtra(ConversationListActivity.FROM_WELCOME, true);
    startActivity(intent);
    finishAffinity();
  }

  @SuppressLint("StaticFieldLeak")
  private void createProfile() {
    if (TextUtils.isEmpty(this.name.getText())) {
      Toast.makeText(this, R.string.please_enter_name, Toast.LENGTH_LONG).show();
      return;
    }
    final String name = this.name.getText().toString();

    new AsyncTask<Void, Void, Boolean>() {
      @Override
      protected Boolean doInBackground(Void... params) {
        Context context    = InstantOnboardingActivity.this;
        DcHelper.set(context, DcHelper.CONFIG_DISPLAY_NAME, name);

        if (avatarChanged) {
          try {
            AvatarHelper.setSelfAvatar(InstantOnboardingActivity.this, avatarBmp);
            Prefs.setProfileAvatarId(InstantOnboardingActivity.this, new SecureRandom().nextInt());
          } catch (IOException e) {
            Log.w(TAG, e);
            return false;
          }
        }

        return true;
      }

      @Override
      public void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        if (result) {
          attachmentManager.cleanup();
          startQrAccountCreation(providerQrData);
        } else {
          Toast.makeText(InstantOnboardingActivity.this, R.string.error, Toast.LENGTH_LONG).show();
        }
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private void startQrAccountCreation(String qrCode)
  {
    if (progressDialog != null) {
      progressDialog.dismiss();
      progressDialog = null;
    }

    cancelled = false;

    progressDialog = new ProgressDialog(this);
    progressDialog.setMessage(getResources().getString(R.string.one_moment));
    progressDialog.setCanceledOnTouchOutside(false);
    progressDialog.setCancelable(false);
    progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(android.R.string.cancel), (dialog, which) -> {
        cancelled = true;
        dcContext.stopOngoingProcess();
      });
    progressDialog.show();

    DcHelper.getEventCenter(this).captureNextError();

    new Thread(() -> {
      Rpc rpc = DcHelper.getRpc(this);
        try {
          rpc.addTransportFromQr(dcContext.getAccountId(), qrCode);
          DcHelper.getEventCenter(this).endCaptureNextError();
          progressSuccess();
        } catch (RpcException e) {
          DcHelper.getEventCenter(this).endCaptureNextError();
          if (!cancelled) {
            Util.runOnMain(() -> progressError(e.getMessage()));
          }
        }
    }).start();
  }

  private class AvatarSelectedListener implements AvatarSelector.AttachmentClickedListener {
    @Override
    public void onClick(int type) {
      switch (type) {
        case AvatarSelector.ADD_GALLERY:
          AttachmentManager.selectImage(InstantOnboardingActivity.this, REQUEST_CODE_AVATAR);
          break;
        case AvatarSelector.REMOVE_PHOTO:
          avatarBmp = null;
          imageLoaded = false;
          avatarChanged = true;
          avatar.setImageDrawable(new ResourceContactPhoto(R.drawable.ic_camera_alt_white_24dp).asDrawable(InstantOnboardingActivity.this, getResources().getColor(R.color.grey_400)));
          break;
        case AvatarSelector.TAKE_PHOTO:
          attachmentManager.capturePhoto(InstantOnboardingActivity.this, REQUEST_CODE_AVATAR);
          break;
      }
    }

    @Override
    public void onQuickAttachment(Uri inputFile) {
      onFileSelected(inputFile);
    }
  }

}
