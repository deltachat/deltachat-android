package org.thoughtcrime.securesms.contacts.avatars;

import android.content.Context;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.Address;

public class GroupRecordContactPhoto extends LocalFileContactPhoto {

    public GroupRecordContactPhoto(Context context, Address address) {
        super(context, address);
    }

    @Override
    public boolean isProfilePhoto() {
        return false;
    }

    @Override
    int getId() {
        return address.getDcChatId();
    }

    @Override
    public String getPath(Context context) {
        String profileImage = DcHelper.getContext(context).getChat(getId()).getProfileImage();
        return profileImage != null ? profileImage : "";
    }

}
