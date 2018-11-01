package org.thoughtcrime.securesms.contacts.avatars;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.profiles.AvatarHelper;
import org.thoughtcrime.securesms.util.Conversions;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

public abstract class LocalFileContactPhoto implements ContactPhoto {

    final Address address;

    private final int id;

    private final String path;

    LocalFileContactPhoto(Context context, Address address) {
        this.address = address;
        id = getId();
        path = getPath(context);
    }

    @Override
    public InputStream openInputStream(Context context) throws IOException {
        return new FileInputStream(path);
    }

    @Override
    public @Nullable Uri getUri(@NonNull Context context) {
        return isProfilePhoto() ? Uri.fromFile(AvatarHelper.getSelfAvatarFile(context, address)) : null;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(address.serialize().getBytes());
        messageDigest.update(Conversions.longToByteArray(id));
        messageDigest.update(path.getBytes());
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof LocalFileContactPhoto)) return false;

        LocalFileContactPhoto that = (LocalFileContactPhoto) other;
        return this.address.equals(that.address) && this.id == that.id && this.path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return this.address.hashCode() ^ id;
    }

    abstract int getId();

    abstract String getPath(Context context);

}
