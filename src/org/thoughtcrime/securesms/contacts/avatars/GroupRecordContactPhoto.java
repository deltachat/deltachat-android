package org.thoughtcrime.securesms.contacts.avatars;

import android.content.Context;

import com.b44t.messenger.DcChat;

import org.thoughtcrime.securesms.database.Address;

public class GroupRecordContactPhoto extends LocalFileContactPhoto {

    public GroupRecordContactPhoto(Context context, Address address, DcChat dcChat) {
        super(context, address, dcChat, null);
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
        String profileImage = dcChat.getProfileImage();
        return profileImage != null ? profileImage : "";
    }

}
