package org.thoughtcrime.securesms;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.loader.app.LoaderManager;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.textfield.TextInputEditText;
import com.soundcloud.android.crop.Crop;

import org.thoughtcrime.securesms.components.AvatarSelector;
import org.thoughtcrime.securesms.components.InputAwareLayout;
import org.thoughtcrime.securesms.components.emoji.EmojiKeyboardProvider;
import org.thoughtcrime.securesms.components.emoji.MediaKeyboard;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.avatars.ResourceContactPhoto;
import org.thoughtcrime.securesms.mms.AttachmentManager;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.profiles.AvatarHelper;
import org.thoughtcrime.securesms.profiles.ProfileMediaConstraints;
import org.thoughtcrime.securesms.util.BitmapDecodingException;
import org.thoughtcrime.securesms.util.BitmapUtil;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.SecureRandom;

@SuppressLint("StaticFieldLeak")
public class CreateProfileActivity extends BaseActionBarActivity implements EmojiKeyboardProvider.EmojiEventListener {

  private static final String TAG = CreateProfileActivity.class.getSimpleName();

  public static final String FROM_WELCOME   = "from_welcome";

  private static final int REQUEST_CODE_AVATAR = 1;

  private final DynamicTheme    dynamicTheme    = new DynamicTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private InputAwareLayout       container;
  private ImageView              avatar;
  private EditText               name;
  private MediaKeyboard          emojiDrawer;
  private TextInputEditText statusView;
  private View                   reveal;

  private boolean fromWelcome;

  private byte[] avatarBytes;
  private AttachmentManager attachmentManager;


  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    this.fromWelcome  = getIntent().getBooleanExtra(FROM_WELCOME, false);
    attachmentManager = new AttachmentManager(this, () -> {});

    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);

    setContentView(R.layout.profile_create_activity);

    getSupportActionBar().setTitle(R.string.pref_profile_info_headline);
    getSupportActionBar().setDisplayHomeAsUpEnabled(!this.fromWelcome);
    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

    initializeResources();
    initializeEmojiInput();
    initializeProfileName();
    initializeProfileAvatar();
    initializeStatusText();
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuInflater inflater = this.getMenuInflater();
    inflater.inflate(R.menu.preferences_create_profile_menu, menu);
    return true;
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;
      case R.id.menu_create_profile:
        handleUpload();
        break;
    }

    return false;
  }

  @Override
  public void onBackPressed() {
    if (container.isInputOpen()) {
      container.hideCurrentInput(name);
    } else if (fromWelcome) {
      startActivity(new Intent(getApplicationContext(), ConversationListActivity.class));
      finish();
    } else {
      super.onBackPressed();
    }
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

    if (container.getCurrentInput() == emojiDrawer) {
      container.hideAttachedInput(true);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    switch (requestCode) {
      case REQUEST_CODE_AVATAR:
        if (resultCode == Activity.RESULT_OK) {
          Uri inputFile  = (data != null ? data.getData() : null);
          onFileSelected(inputFile);
        }
        break;

      case Crop.REQUEST_CROP:
        if (resultCode == Activity.RESULT_OK) {

          new AsyncTask<Void, Void, byte[]>() {
            @Override
            protected byte[] doInBackground(Void... params) {
              try {
                try {
                  Uri imageUri = Crop.getOutput(data);
                  Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                  ByteArrayOutputStream stream = new ByteArrayOutputStream();
                  bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                  return stream.toByteArray();
                } catch (Exception any) {
                  Log.e(TAG, "could not send raw JPG to core. Using scaled JPG.", any);
                }
                BitmapUtil.ScaleResult result =
                    BitmapUtil.createScaledBytes(CreateProfileActivity.this, Crop.getOutput(data), new ProfileMediaConstraints());
                return result.getBitmap();
              } catch (BitmapDecodingException e) {
                Log.w(TAG, e);
                return null;
              }
            }

            @Override
            protected void onPostExecute(byte[] result) {
              if (result != null) {
                avatarBytes = result;
                GlideApp.with(CreateProfileActivity.this)
                        .load(avatarBytes)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .circleCrop()
                        .into(avatar);
              } else {
                Toast.makeText(CreateProfileActivity.this, R.string.error, Toast.LENGTH_LONG).show();
              }
            }
          }.execute();
        }
        break;
    }
  }

  private void onFileSelected(Uri inputFile) {
    Uri outputFile = Uri.fromFile(new File(getCacheDir(), "cropped"));
    if (inputFile == null) {
      inputFile = attachmentManager.getImageCaptureUri();
    }

    new Crop(inputFile).output(outputFile).asSquare().start(this);
  }

  private void initializeResources() {
    TextView passwordAccountSettings       = ViewUtil.findById(this, R.id.password_account_settings_button);
    TextView loginSuccessText              = ViewUtil.findById(this, R.id.login_success_text);
    this.avatar       = ViewUtil.findById(this, R.id.avatar);
    this.name         = ViewUtil.findById(this, R.id.name_text);
    this.emojiDrawer  = ViewUtil.findById(this, R.id.emoji_drawer);
    this.container    = ViewUtil.findById(this, R.id.container);
    this.reveal       = ViewUtil.findById(this, R.id.reveal);
    this.statusView   = ViewUtil.findById(this, R.id.status_text);

    this.avatar.setImageDrawable(new ResourceContactPhoto(R.drawable.ic_camera_alt_white_24dp).asDrawable(this, getResources().getColor(R.color.grey_400)));

    this.avatar.setOnClickListener(view -> Permissions.with(this)
                                                      .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                                      .ifNecessary()
                                                      .onAnyResult(this::handleAvatarSelectionWithPermissions)
                                                      .execute());

    passwordAccountSettings.setOnClickListener(view -> {
      Intent intent = new Intent(this, RegistrationActivity.class);
      startActivity(intent);
    });

    if (fromWelcome) {
      String addr = DcHelper.get(this, "addr");
      loginSuccessText.setText(getString(R.string.qraccount_success_enter_name, addr));
      ViewUtil.findById(this, R.id.status_text_layout).setVisibility(View.GONE);
      ViewUtil.findById(this, R.id.information_label).setVisibility(View.GONE);
      passwordAccountSettings.setVisibility(View.GONE);
    } else {
      loginSuccessText.setVisibility(View.GONE);
    }
  }

  private void initializeProfileName() {
    String profileName  = DcHelper.get(this, DcHelper.CONFIG_DISPLAY_NAME);
    if (!TextUtils.isEmpty(profileName)) {
      name.setText(profileName);
      name.setSelection(profileName.length(), profileName.length());
    }
  }

  private void initializeProfileAvatar() {
    String address = DcHelper.get(this, DcHelper.CONFIG_ADDRESS);

    if (AvatarHelper.getSelfAvatarFile(this).exists() && AvatarHelper.getSelfAvatarFile(this).length() > 0) {
      new AsyncTask<Void, Void, byte[]>() {
        @Override
        protected byte[] doInBackground(Void... params) {
          try {
            return Util.readFully(new FileInputStream(AvatarHelper.getSelfAvatarFile(CreateProfileActivity.this)));
          } catch (IOException e) {
            Log.w(TAG, e);
            return null;
          }
        }

        @Override
        protected void onPostExecute(byte[] result) {
          if (result != null) {
            avatarBytes = result;
            GlideApp.with(CreateProfileActivity.this)
                    .load(result)
                    .circleCrop()
                    .into(avatar);
          }
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }


  @Override
  public void onEmojiSelected(String emoji) {
    final int start = name.getSelectionStart();
    final int end   = name.getSelectionEnd();

    name.getText().replace(Math.min(start, end), Math.max(start, end), emoji);
    name.setSelection(start + emoji.length());
  }

  @Override
  public void onKeyEvent(KeyEvent keyEvent) {
    name.dispatchKeyEvent(keyEvent);
  }

  private void initializeMediaKeyboardProviders(@NonNull MediaKeyboard mediaKeyboard) {
    boolean isSystemEmojiPreferred   = Prefs.isSystemEmojiPreferred(this);
    if (!isSystemEmojiPreferred) {
      mediaKeyboard.setProviders(0, new EmojiKeyboardProvider(this, this));
    }
  }

  private void initializeEmojiInput() {
    initializeMediaKeyboardProviders(emojiDrawer);
    this.name.setOnClickListener(v -> container.showSoftkey(name));
  }

  private void initializeStatusText() {
    String status = DcHelper.get(this, DcHelper.CONFIG_SELF_STATUS);
    statusView.setText(status);
  }

  private void handleAvatarSelectionWithPermissions() {
    new AvatarSelector(this, LoaderManager.getInstance(this), new AvatarSelectedListener(), avatarBytes != null).show(this, avatar);
  }

  private void handleUpload() {
    final String        name;

    if (TextUtils.isEmpty(this.name.getText().toString())) name = null;
    else                                                   name = this.name.getText().toString();

    new AsyncTask<Void, Void, Boolean>() {
      @Override
      protected Boolean doInBackground(Void... params) {
        Context context    = CreateProfileActivity.this;
        DcHelper.set(context, DcHelper.CONFIG_DISPLAY_NAME, name);
        setStatusText();

        try {
          AvatarHelper.setSelfAvatar(CreateProfileActivity.this, avatarBytes);
          Prefs.setProfileAvatarId(CreateProfileActivity.this, new SecureRandom().nextInt());
        } catch (IOException e) {
          Log.w(TAG, e);
          return false;
        }

        return true;
      }

      @Override
      public void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        if (result) {
          attachmentManager.cleanup();
          if (fromWelcome) {
            startActivity(new Intent(getApplicationContext(), ConversationListActivity.class));
          }
          finish();
        } else        {
          Toast.makeText(CreateProfileActivity.this, R.string.error, Toast.LENGTH_LONG).show();
        }
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private void setStatusText() {
    String newStatus = statusView.getText().toString().trim();
    DcHelper.set(this, DcHelper.CONFIG_SELF_STATUS, newStatus);
  }

  private class AvatarSelectedListener implements AvatarSelector.AttachmentClickedListener {
    @Override
    public void onClick(int type) {
      switch (type) {
        case AvatarSelector.ADD_GALLERY:
          AttachmentManager.selectGallery(CreateProfileActivity.this, REQUEST_CODE_AVATAR);
          break;
        case AvatarSelector.REMOVE_PHOTO:
          new AlertDialog.Builder(CreateProfileActivity.this)
                  .setTitle(R.string.pref_profile_photo_remove_ask)
                  .setNegativeButton(android.R.string.cancel, (dialog, which) -> finish())
                  .setPositiveButton(R.string.ok, (dialog, which) -> {
                    avatarBytes = null;
                    avatar.setImageDrawable(new ResourceContactPhoto(R.drawable.ic_camera_alt_white_24dp).asDrawable(CreateProfileActivity.this, getResources().getColor(R.color.grey_400)));
                  })
                  .show();
          break;
        case AvatarSelector.TAKE_PHOTO:
          attachmentManager.capturePhoto(CreateProfileActivity.this, REQUEST_CODE_AVATAR);
          break;
      }
    }

    @Override
    public void onQuickAttachment(Uri inputFile) {
      onFileSelected(inputFile);
    }
  }
}
