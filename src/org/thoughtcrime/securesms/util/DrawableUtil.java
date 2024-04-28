package org.thoughtcrime.securesms.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import androidx.annotation.NonNull;

public final class DrawableUtil {

  private static final int SHORTCUT_INFO_BITMAP_SIZE  = ViewUtil.dpToPx(108);
  private static final int SHORTCUT_INFO_WRAPPED_SIZE = ViewUtil.dpToPx(72);
  private static final int SHORTCUT_INFO_PADDING      = (SHORTCUT_INFO_BITMAP_SIZE - SHORTCUT_INFO_WRAPPED_SIZE) / 2;

  public static @NonNull Bitmap wrapBitmapForShortcutInfo(@NonNull Bitmap toWrap) {
    Bitmap bitmap = Bitmap.createBitmap(SHORTCUT_INFO_BITMAP_SIZE, SHORTCUT_INFO_BITMAP_SIZE, Bitmap.Config.ARGB_8888);
    Bitmap scaled = Bitmap.createScaledBitmap(toWrap, SHORTCUT_INFO_WRAPPED_SIZE, SHORTCUT_INFO_WRAPPED_SIZE, true);

    Canvas canvas = new Canvas(bitmap);
    canvas.drawBitmap(scaled, SHORTCUT_INFO_PADDING, SHORTCUT_INFO_PADDING, null);

    return bitmap;
  }
}
