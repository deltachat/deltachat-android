package org.thoughtcrime.securesms;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.thoughtcrime.securesms.components.AvatarSelector;
import org.thoughtcrime.securesms.components.InputAwareLayout;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.avatars.ResourceContactPhoto;
import org.thoughtcrime.securesms.mms.AttachmentManager;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.profiles.AvatarHelper;
import org.thoughtcrime.securesms.scribbles.ScribbleActivity;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;


@SuppressLint("StaticFieldLeak")
public class CreateProfileActivity extends BaseActionBarActivity {

  private static final String TAG = CreateProfileActivity.class.getSimpleName();

  public static final String FROM_WELCOME   = "from_welcome";

  private static final int REQUEST_CODE_AVATAR = 1;

  private InputAwareLayout       container;
  private ImageView              avatar;
  private EditText               name;
  private EditText               statusView;

  private boolean fromWelcome;
  private boolean avatarChanged;
  private boolean imageLoaded;

  private Bitmap avatarBmp;
  private AttachmentManager attachmentManager;


  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    this.fromWelcome  = getIntent().getBooleanExtra(FROM_WELCOME, false);

    setContentView(R.layout.profile_create_activity);

    getSupportActionBar().setTitle(R.string.pref_profile_info_headline);
    getSupportActionBar().setDisplayHomeAsUpEnabled(!this.fromWelcome);
    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

    attachmentManager = new AttachmentManager(this, () -> {});
    avatarChanged = false;
    initializeResources();
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
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    int itemId = item.getItemId();
    if (itemId == android.R.id.home) {
      onBackPressed();
      return true;
    } else if (itemId == R.id.menu_create_profile) {
      updateProfile();
    }

    return false;
  }

  @Override
  public void onBackPressed() {
    if (container.isInputOpen()) {
      container.hideCurrentInput(name);
    } else if (fromWelcome) {
      updateProfile();
    } else {
      super.onBackPressed();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (resultCode != Activity.RESULT_OK) {
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
        .into(new SimpleTarget<Bitmap>() {
              @Override
              public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                avatarChanged = true;
                imageLoaded = true;
                avatarBmp = resource;
              }
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
    TextView loginSuccessText              = ViewUtil.findById(this, R.id.login_success_text);
    this.avatar       = ViewUtil.findById(this, R.id.avatar);
    this.name         = ViewUtil.findById(this, R.id.name_text);
    this.container    = ViewUtil.findById(this, R.id.container);
    this.statusView   = ViewUtil.findById(this, R.id.status_text);

    // add padding to avoid content hidden behind system bars
    ViewUtil.applyWindowInsets(container);

    if (fromWelcome) {
      String addr = DcHelper.get(this, "addr");
      loginSuccessText.setText(R.string.set_name_and_avatar_explain);
      ViewUtil.findById(this, R.id.status_text_layout).setVisibility(View.GONE);
      ViewUtil.findById(this, R.id.information_label).setVisibility(View.GONE);
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
    File avatarFile = AvatarHelper.getSelfAvatarFile(this);
    if (avatarFile.exists() && avatarFile.length() > 0) {
      imageLoaded = true;
      GlideApp.with(this)
              .load(avatarFile)
              .circleCrop()
              .into(avatar);
    } else {
      imageLoaded = false;
      avatar.setImageDrawable(new ResourceContactPhoto(R.drawable.ic_camera_alt_white_24dp).asDrawable(this, getResources().getColor(R.color.grey_400)));
    }
    avatar.setOnClickListener(view ->
            new AvatarSelector(this, LoaderManager.getInstance(this), new AvatarSelectedListener(), imageLoaded)
                    .show(this, avatar)
    );
  }

  private void initializeStatusText() {
    String status = DcHelper.get(this, DcHelper.CONFIG_SELF_STATUS);
    statusView.setText(status);
  }

  private void updateProfile() {
    if (TextUtils.isEmpty(this.name.getText())) {
      Toast.makeText(this, R.string.please_enter_name, Toast.LENGTH_LONG).show();
      return;
    }
    final String name = this.name.getText().toString();

    new AsyncTask<Void, Void, Boolean>() {
      @Override
      protected Boolean doInBackground(Void... params) {
        Context context    = CreateProfileActivity.this;
        DcHelper.set(context, DcHelper.CONFIG_DISPLAY_NAME, name);
        setStatusText();

        if (avatarChanged) {
          try {
            AvatarHelper.setSelfAvatar(CreateProfileActivity.this, avatarBmp);
            Prefs.setProfileAvatarId(CreateProfileActivity.this, new SecureRandom().nextInt());
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
          if (fromWelcome) {
            Intent intent = new Intent(getApplicationContext(), ConversationListActivity.class);
            intent.putExtra(ConversationListActivity.FROM_WELCOME, true);
            startActivity(intent);
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
          AttachmentManager.selectImage(CreateProfileActivity.this, REQUEST_CODE_AVATAR);
          break;
        case AvatarSelector.REMOVE_PHOTO:
          avatarBmp = null;
          imageLoaded = false;
          avatarChanged = true;
          avatar.setImageDrawable(new ResourceContactPhoto(R.drawable.ic_camera_alt_white_24dp).asDrawable(CreateProfileActivity.this, getResources().getColor(R.color.grey_400)));
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
