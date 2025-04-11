package org.thoughtcrime.securesms.preferences.widgets;


import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.avatars.MyProfileContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.ResourceContactPhoto;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.Prefs;

public class ProfilePreference extends Preference {

  private ImageView avatarView;
  private TextView  profileNameView;
  private TextView  profileStatusView;

  public ProfilePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    initialize();
  }

  public ProfilePreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
  }

  public ProfilePreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public ProfilePreference(Context context) {
    super(context);
    initialize();
  }

  private void initialize() {
    setLayoutResource(R.layout.profile_preference_view);
  }

  @Override
  public void onBindViewHolder(@NonNull PreferenceViewHolder viewHolder) {
    super.onBindViewHolder(viewHolder);
    avatarView        = (ImageView)viewHolder.findViewById(R.id.avatar);
    profileNameView   = (TextView)viewHolder.findViewById(R.id.profile_name);
    profileStatusView = (TextView)viewHolder.findViewById(R.id.profile_status);

    refresh();
  }

  public void refresh() {
    if (profileNameView == null) return;

    final String address = DcHelper.get(getContext(), DcHelper.CONFIG_CONFIGURED_ADDRESS);
    final MyProfileContactPhoto profileImage = new MyProfileContactPhoto(address, String.valueOf(Prefs.getProfileAvatarId(getContext())));

    GlideApp.with(getContext().getApplicationContext())
            .load(profileImage)
            .error(new ResourceContactPhoto(R.drawable.ic_camera_alt_white_24dp).asDrawable(getContext(), getContext().getResources().getColor(R.color.grey_400)))
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(avatarView);

    final String profileName  = DcHelper.get(getContext(), DcHelper.CONFIG_DISPLAY_NAME);
    if (!TextUtils.isEmpty(profileName)) {
      profileNameView.setText(profileName);
    } else {
      profileNameView.setText(getContext().getString(R.string.pref_profile_info_headline));
    }

    final String status = DcHelper.get(getContext(), DcHelper.CONFIG_SELF_STATUS);
    if (!TextUtils.isEmpty(status)) {
      profileStatusView.setText(status);
    } else {
      profileStatusView.setText(getContext().getString(R.string.pref_default_status_label));
    }
  }
}
