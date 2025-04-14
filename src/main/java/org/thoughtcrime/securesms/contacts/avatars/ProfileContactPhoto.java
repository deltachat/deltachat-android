package org.thoughtcrime.securesms.contacts.avatars;

import android.content.Context;

import com.b44t.messenger.DcContact;

import org.thoughtcrime.securesms.database.Address;

public class ProfileContactPhoto extends LocalFileContactPhoto {

    public ProfileContactPhoto(Context context, Address address, String path) {
        super(context, address, address.getDcContactId(), path);
    }

    @Override
    public boolean isProfilePhoto() {
        return true;
    }
}
