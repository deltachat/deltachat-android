package org.thoughtcrime.securesms.contacts.avatars;


import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.profiles.AvatarHelper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

public class MyProfileContactPhoto implements ContactPhoto {

    private final @NonNull String address;
    private final @NonNull String avatarObject;

    public MyProfileContactPhoto(@NonNull String address, @NonNull String avatarObject) {
        this.address = address;
        this.avatarObject = avatarObject;
    }

    @Override
    public InputStream openInputStream(Context context) throws IOException {
        return new FileInputStream(AvatarHelper.getSelfAvatarFile(context));
    }

    @Override
    public @Nullable
    Uri getUri(@NonNull Context context) {
        return Uri.fromFile(AvatarHelper.getSelfAvatarFile(context));
    }

    @Override
    public boolean isProfilePhoto() {
        return true;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(address.getBytes());
        messageDigest.update(avatarObject.getBytes());
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof MyProfileContactPhoto)) return false;

        MyProfileContactPhoto that = (MyProfileContactPhoto) other;

        return this.address.equals(that.address) && this.avatarObject.equals(that.avatarObject);
    }

    @Override
    public int hashCode() {
        return address.hashCode() ^ avatarObject.hashCode();
    }
}
