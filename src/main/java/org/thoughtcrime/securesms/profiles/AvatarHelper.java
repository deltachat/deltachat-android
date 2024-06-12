package org.thoughtcrime.securesms.profiles;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.b44t.messenger.DcContext;
import com.soundcloud.android.crop.Crop;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.scribbles.ScribbleActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AvatarHelper {

    public static void setGroupAvatar(Context context, int chatId, Bitmap bitmap) {
        DcContext dcContext = DcHelper.getContext(context);

        if (bitmap == null) {
            dcContext.setChatProfileImage(chatId, null);
        } else {
            try {
                File avatar = File.createTempFile("groupavatar", ".jpg", context.getCacheDir());
                FileOutputStream out = new FileOutputStream(avatar);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
                out.close();
                dcContext.setChatProfileImage(chatId, avatar.getPath()); // The avatar is copied to the blobs directory here...
                //noinspection ResultOfMethodCallIgnored
                avatar.delete(); // ..., now we can delete it.
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static File getSelfAvatarFile(@NonNull Context context) {
        String dirString = DcHelper.getContext(context).getConfig(DcHelper.CONFIG_SELF_AVATAR);
        return new File(dirString);
    }

    public static void setSelfAvatar(@NonNull Context context, @Nullable Bitmap bitmap) throws IOException {
        if (bitmap == null) {
            DcHelper.set(context, DcHelper.CONFIG_SELF_AVATAR, null);
        } else {
            File avatar = File.createTempFile("selfavatar", ".jpg", context.getCacheDir());
            FileOutputStream out = new FileOutputStream(avatar);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
            DcHelper.set(context, DcHelper.CONFIG_SELF_AVATAR, avatar.getPath()); // The avatar is copied to the blobs directory here...
            //noinspection ResultOfMethodCallIgnored
            avatar.delete(); // ..., now we can delete it.
        }
    }

    public static void cropAvatar(Activity context, Uri imageUri) {
        if (Build.VERSION.SDK_INT >= 19) { // Image editor requires Android 4.4 KitKat or newer.
            Intent intent = new Intent(context, ScribbleActivity.class);
            intent.setData(imageUri);
            intent.putExtra(ScribbleActivity.CROP_AVATAR, true);
            context.startActivityForResult(intent, ScribbleActivity.SCRIBBLE_REQUEST_CODE);
        } else {
            Uri outputFile = Uri.fromFile(new File(context.getCacheDir(), "cropped"));
            Crop.of(imageUri, outputFile).asSquare().start(context);
        }
    }

}
