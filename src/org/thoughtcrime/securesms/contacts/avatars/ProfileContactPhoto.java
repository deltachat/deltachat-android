package org.thoughtcrime.securesms.contacts.avatars;

import android.content.Context;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.Address;

public class ProfileContactPhoto extends LocalFileContactPhoto {

    public ProfileContactPhoto(Context context, Address address) {
        super(context, address);
    }

    @Override
    public boolean isProfilePhoto() {
        return true;
    }

    @Override
    int getId() {
        return address.getDcContactId();
    }

    @Override
    public String getPath(Context context) {
        String profileImage = DcHelper.getContext(context).getContact(getId()).getProfileImage();
        return profileImage != null ? profileImage : "";
    }

}
