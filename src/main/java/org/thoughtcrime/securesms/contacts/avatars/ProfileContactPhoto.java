package org.thoughtcrime.securesms.contacts.avatars;

import android.content.Context;

import com.b44t.messenger.DcContact;

import org.thoughtcrime.securesms.database.Address;

public class ProfileContactPhoto extends LocalFileContactPhoto {

    public ProfileContactPhoto(Context context, Address address, DcContact dcContact) {
        super(context, address, null, dcContact);
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
        String profileImage = dcContact.getProfileImage();
        return profileImage != null ? profileImage : "";
    }

}
