package org.thoughtcrime.securesms.contacts.avatars;

import android.content.Context;

import com.b44t.messenger.DcChat;

import org.thoughtcrime.securesms.database.Address;

public class GroupRecordContactPhoto extends LocalFileContactPhoto {

    public GroupRecordContactPhoto(Context context, Address address, String path) {
        super(context, address, address.getDcChatId(), path);
    }

    @Override
    public boolean isProfilePhoto() {
        return false;
    }
}
