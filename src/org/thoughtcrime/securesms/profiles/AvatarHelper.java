package org.thoughtcrime.securesms.profiles;


import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Stream;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.Address;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class AvatarHelper {

    public static final String GROUP_TEMPLATE = "group_chat_avatar_%s_%o.jpg";

    public static final String CONTACT_TEMPLATE = "contact_avatar_%s_%o.jpg";

    private static final String AVATAR_DIRECTORY = "avatars";

    @SuppressLint("DefaultLocale")
    public static String getFilePathForGroupAvatar(Context context, int chatId, long timestamp) {
        return getAvatarDirectoryPath(context) + String.format(GROUP_TEMPLATE, chatId, timestamp);
    }

    @NonNull
    private static String getAvatarDirectoryPath(Context context) {
        return context.getFilesDir().getAbsolutePath() + File.separator + AVATAR_DIRECTORY + File.separator;
    }

    @SuppressLint("DefaultLocale")
    public static String getFilePathForContactAvatar(Context context, int contactId, long timestamp) {
        return getAvatarDirectoryPath(context) + String.format(CONTACT_TEMPLATE, contactId, timestamp);
    }

    public static InputStream getInputStreamFor(@NonNull Context context, @NonNull Address address)
            throws IOException {
        return new FileInputStream(getAvatarFile(context, address));
    }

    public static InputStream getInputStreamFor(@NonNull Context context, @NonNull String address)
            throws IOException {
        return new FileInputStream(getAvatarFile(context, address));
    }

    public static List<File> getAvatarFiles(@NonNull Context context) {
        File avatarDirectory = new File(context.getFilesDir(), AVATAR_DIRECTORY);
        File[] results = avatarDirectory.listFiles();

        if (results == null) return new LinkedList<>();
        else return Stream.of(results).toList();
    }

    public static void delete(@NonNull Context context, @NonNull Address address) {
        delete(context, getAvatarFile(context, address));
    }

    public static void delete(@NonNull Context context, @NonNull File avatar) {
        avatar.delete();
        DcHelper.set(context, DcHelper.CONFIG_SELF_AVATAR, null);
    }

    public static @NonNull
    File getAvatarFile(@NonNull Context context, @NonNull Address address) {
        String name = new File(address.serialize()).getName();
        return getAvatarFile(context, name);
    }

    public static @NonNull
    File getAvatarFile(@NonNull Context context, @NonNull String address) {
        File avatarDirectory = new File(context.getFilesDir(), AVATAR_DIRECTORY);
        avatarDirectory.mkdirs();
        return new File(avatarDirectory, address);
    }

    public static void setAvatar(@NonNull Context context, @NonNull Address address, @Nullable byte[] data)
            throws IOException {
        if (data == null) {
            delete(context, address);
        } else {
            File avatar = getAvatarFile(context, address);
            writeAvatarFile(context, avatar, data);
        }
    }

    public static void setAvatar(@NonNull Context context, @NonNull String address, @Nullable byte[] data) throws IOException {
        File avatar = getAvatarFile(context, address);
        if (data == null) {
            delete(context, avatar);
        } else {
            writeAvatarFile(context, avatar, data);
        }
    }

    private static void writeAvatarFile(@NonNull Context context, @NonNull File avatar, @Nullable byte[] data) throws IOException {
        FileOutputStream out = new FileOutputStream(avatar);
        out.write(data);
        out.close();
        String path = avatar.getPath();
        DcHelper.set(context, DcHelper.CONFIG_SELF_AVATAR, path);
    }

}
