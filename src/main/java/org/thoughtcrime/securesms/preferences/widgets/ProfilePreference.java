package org.thoughtcrime.securesms.preferences.widgets;


import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

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
  private TextView profileAddressView;

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
  public void onBindViewHolder(PreferenceViewHolder viewHolder) {
    super.onBindViewHolder(viewHolder);
    avatarView        = (ImageView)viewHolder.findViewById(R.id.avatar);
    profileNameView   = (TextView)viewHolder.findViewById(R.id.profile_name);
    profileAddressView = (TextView)viewHolder.findViewById(R.id.number);

    refresh();
  }

  public void refresh() {
    if (profileAddressView == null) return;

    final String address = DcHelper.get(getContext(), DcHelper.CONFIG_CONFIGURED_ADDRESS);
    String profileName  = DcHelper.get(getContext(), DcHelper.CONFIG_DISPLAY_NAME);

    if(profileName==null || profileName.isEmpty()) {
      profileName = getContext().getString(R.string.pref_profile_info_headline);
    }

    final MyProfileContactPhoto profileImage = new MyProfileContactPhoto(address, String.valueOf(Prefs.getProfileAvatarId(getContext())));

    GlideApp.with(getContext().getApplicationContext())
            .load(profileImage)
            .error(new ResourceContactPhoto(R.drawable.ic_camera_alt_white_24dp).asDrawable(getContext(), getContext().getResources().getColor(R.color.grey_400)))
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(avatarView);

    if (!TextUtils.isEmpty(profileName)) {
      profileNameView.setText(profileName);
    }

    profileAddressView.setText(address);
  }
}
