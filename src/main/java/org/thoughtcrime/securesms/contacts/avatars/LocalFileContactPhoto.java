package org.thoughtcrime.securesms.contacts.avatars;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;

import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.profiles.AvatarHelper;
import org.thoughtcrime.securesms.util.Conversions;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

public abstract class LocalFileContactPhoto implements ContactPhoto {

    private final Address address;
    private final int id;
    private final String path;

    LocalFileContactPhoto(Context context, Address address, int id, String path) {
        this.address = address;
        this.id = id;
        this.path = path != null ? path : "";
    }

    @Override
    public InputStream openInputStream(Context context) throws IOException {
        return new FileInputStream(path);
    }

    @Override
    public @Nullable Uri getUri(@NonNull Context context) {
        return isProfilePhoto() ? Uri.fromFile(AvatarHelper.getSelfAvatarFile(context)) : null;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(address.serialize().getBytes());
        messageDigest.update(Conversions.longToByteArray(id));
        messageDigest.update(path.getBytes());
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof LocalFileContactPhoto)) return false;

        LocalFileContactPhoto that = (LocalFileContactPhoto) other;
        return this.address.equals(that.address) && this.id == that.id && this.path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return this.address.hashCode() ^ id;
    }

    public String getPath() {
        return path;
    }
}
