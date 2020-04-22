package org.thoughtcrime.securesms.profiles;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.Address;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AvatarHelper {

    private static final String GROUP_TEMPLATE = "group_chat_avatar_%s_%o.jpg";

    private static final String AVATAR_DIRECTORY = "avatars";

    @SuppressLint("DefaultLocale")
    private static String getFilePathForGroupAvatar(Context context, int chatId, long timestamp) {
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
        //noinspection ResultOfMethodCallIgnored
        new File(getAvatarDirectoryPath(context)).mkdirs();
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

    public static InputStream getInputStreamFor(@NonNull Context context)
            throws IOException {
        return new FileInputStream(getSelfAvatarFile(context));
    }

    public static File getSelfAvatarFile(@NonNull Context context) {
        String dirString = DcHelper.getContext(context).getConfig(DcHelper.CONFIG_SELF_AVATAR);
        return new File(dirString);
    }

    public static void setSelfAvatar(@NonNull Context context, @NonNull String address, @Nullable byte[] data) throws IOException {
        if (data == null) {
            DcHelper.set(context, DcHelper.CONFIG_SELF_AVATAR, null);
        } else {
            File avatar = File.createTempFile("prefix", "extension", context.getCacheDir());
            FileOutputStream out = new FileOutputStream(avatar);
            if (data != null) {
                out.write(data);
            }
            out.close();
            String path = avatar.getPath();
            DcHelper.set(context, DcHelper.CONFIG_SELF_AVATAR, path);
            avatar.delete();
        }
    }
}
