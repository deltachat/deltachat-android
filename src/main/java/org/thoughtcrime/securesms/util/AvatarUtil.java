package org.thoughtcrime.securesms.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

public class AvatarUtil {

  /**
   * convert image path to data URI.
   *
   * @param filePath File path to image
   * @return data URI like "data:image/jpeg;base64,..."
   */
  public static String asDataUri(String filePath) {
    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
    if (bitmap == null) return null;

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
    byte[] bytes = baos.toByteArray();
    String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
    return "data:image/jpeg;base64," + base64;
  }

  /**
   * convert Drawable to data URI.
   *
   * @param drawable avatar image as Drawable
   * @return data URI like "data:image/png;base64,..."
   */
  public static String asDataUri(Drawable drawable) {
    int w = Math.max(1, drawable.getIntrinsicWidth());
    int h = Math.max(1, drawable.getIntrinsicHeight());
    Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    drawable.setBounds(0, 0, w, h);
    drawable.draw(canvas);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
    byte[] bytes = baos.toByteArray();
    String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
    return "data:image/jpeg;base64," + base64;
  }

}
