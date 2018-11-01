package org.thoughtcrime.securesms.profiles;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Stream;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.Address;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

    public static void setGroupAvatar(Context context, int chatId, Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        DcContext dcContext = DcHelper.getContext(context);
        String avatarPath = dcContext.getChat(chatId).getProfileImage();
        if (avatarPath != null && !avatarPath.isEmpty()) {
            File oldImage = new File(avatarPath);
            if (oldImage.exists()) {
                //noinspection ResultOfMethodCallIgnored
                oldImage.delete();
            }
        }
        avatarPath = getFilePathForGroupAvatar(context, chatId, System.currentTimeMillis());
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(avatarPath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outStream);
            dcContext.setChatProfileImage(chatId, avatarPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    @SuppressLint("DefaultLocale")
    public static String getFilePathForContactAvatar(Context context, int contactId, long timestamp) {
        return getAvatarDirectoryPath(context) + String.format(CONTACT_TEMPLATE, contactId, timestamp);
    }

    public static void setContactAvatar(Context context, int contactId, Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        DcContext dcContext = DcHelper.getContext(context);
        String avatarPath = dcContext.getContact(contactId).getProfileImage();
        if (avatarPath != null && !avatarPath.isEmpty()) {
            File oldImage = new File(avatarPath);
            if (oldImage.exists()) {
                //noinspection ResultOfMethodCallIgnored
                oldImage.delete();
            }
        }
        avatarPath = getFilePathForContactAvatar(context, contactId, System.currentTimeMillis());
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(avatarPath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outStream);
            //dcContext.setContactProfileImage(contactId, avatarPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static InputStream getInputStreamFor(@NonNull Context context, @NonNull String address)
            throws IOException {
        return new FileInputStream(getSelfAvatarFile(context, address));
    }

    public static List<File> getAvatarFiles(@NonNull Context context) {
        File avatarDirectory = new File(context.getFilesDir(), AVATAR_DIRECTORY);
        File[] results = avatarDirectory.listFiles();

        if (results == null) return new LinkedList<>();
        else return Stream.of(results).toList();
    }

    public static void deleteSelfAvatar(@NonNull Context context, @NonNull Address address) {
        deleteSelfAvatar(context, getSelfAvatarFile(context, address));
    }

    public static void deleteSelfAvatar(@NonNull Context context, @NonNull File avatar) {
        avatar.delete();
        DcHelper.set(context, DcHelper.CONFIG_SELF_AVATAR, null);
    }

    public static File getSelfAvatarFile(@NonNull Context context, @NonNull Address address) {
        String name = new File(address.serialize()).getName();
        return getSelfAvatarFile(context, name);
    }

    public static File getSelfAvatarFile(@NonNull Context context, @NonNull String address) {
        File avatarDirectory = new File(context.getFilesDir(), AVATAR_DIRECTORY);
        avatarDirectory.mkdirs();
        return new File(avatarDirectory, address);
    }

    public static void setSelfAvatar(@NonNull Context context, @NonNull Address address, @Nullable byte[] data)
            throws IOException {
        if (data == null) {
            deleteSelfAvatar(context, address);
        } else {
            File avatar = getSelfAvatarFile(context, address);
            writeSelfAvatarFile(context, avatar, data);
        }
    }

    public static void setSelfAvatar(@NonNull Context context, @NonNull String address, @Nullable byte[] data) throws IOException {
        File avatar = getSelfAvatarFile(context, address);
        if (data == null) {
            deleteSelfAvatar(context, avatar);
        } else {
            writeSelfAvatarFile(context, avatar, data);
        }
    }

    private static void writeSelfAvatarFile(@NonNull Context context, @NonNull File avatar, @Nullable byte[] data) throws IOException {
        FileOutputStream out = new FileOutputStream(avatar);
        if (data != null) {
            out.write(data);
        }
        out.close();
        String path = avatar.getPath();
        DcHelper.set(context, DcHelper.CONFIG_SELF_AVATAR, path);
    }

}
