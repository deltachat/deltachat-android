package org.thoughtcrime.securesms.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.exifinterface.media.ExifInterface;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

public class BitmapUtil {

  private static final String TAG = BitmapUtil.class.getSimpleName();

  @WorkerThread
  public static Bitmap createScaledBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
    if (bitmap.getWidth() <= maxWidth && bitmap.getHeight() <= maxHeight) {
      return bitmap;
    }

    if (maxWidth <= 0 || maxHeight <= 0) {
      return bitmap;
    }

    int newWidth  = maxWidth;
    int newHeight = maxHeight;

    float widthRatio  = bitmap.getWidth()  / (float) maxWidth;
    float heightRatio = bitmap.getHeight() / (float) maxHeight;

    if (widthRatio > heightRatio) {
      newHeight = (int) (bitmap.getHeight() / widthRatio);
    } else {
      newWidth = (int) (bitmap.getWidth() / heightRatio);
    }

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
  }

  private static BitmapFactory.Options getImageDimensions(InputStream inputStream)
      throws BitmapDecodingException
  {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds    = true;
    BufferedInputStream fis       = new BufferedInputStream(inputStream);
    BitmapFactory.decodeStream(fis, null, options);
    try {
      fis.close();
    } catch (IOException ioe) {
      Log.w(TAG, "failed to close the InputStream after reading image dimensions");
    }

    if (options.outWidth == -1 || options.outHeight == -1) {
      throw new BitmapDecodingException("Failed to decode image dimensions: " + options.outWidth + ", " + options.outHeight);
    }

    return options;
  }

  @Nullable
  public static Pair<Integer, Integer> getExifDimensions(InputStream inputStream) throws IOException {
    ExifInterface exif   = new ExifInterface(inputStream);
    int           width  = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0);
    int           height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0);
    if (width == 0 && height == 0) {
      return null;
    }

    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
    if (orientation == ExifInterface.ORIENTATION_ROTATE_90  ||
        orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
        orientation == ExifInterface.ORIENTATION_TRANSVERSE ||
        orientation == ExifInterface.ORIENTATION_TRANSPOSE)
    {
      return new Pair<>(height, width);
    }
    return new Pair<>(width, height);
  }

  public static Pair<Integer, Integer> getDimensions(InputStream inputStream) throws BitmapDecodingException {
    BitmapFactory.Options options = getImageDimensions(inputStream);
    return new Pair<>(options.outWidth, options.outHeight);
  }

  public static byte[] createFromNV21(@NonNull final byte[] data,
                                      final int width,
                                      final int height,
                                      int rotation,
                                      final Rect croppingRect,
                                      final boolean flipHorizontal)
      throws IOException
  {
    byte[] rotated = rotateNV21(data, width, height, rotation, flipHorizontal);
    final int rotatedWidth  = rotation % 180 > 0 ? height : width;
    final int rotatedHeight = rotation % 180 > 0 ? width  : height;
    YuvImage previewImage = new YuvImage(rotated, ImageFormat.NV21,
                                         rotatedWidth, rotatedHeight, null);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    previewImage.compressToJpeg(croppingRect, 80, outputStream);
    byte[] bytes = outputStream.toByteArray();
    outputStream.close();
    return bytes;
  }

  /*
   * NV21 a.k.a. YUV420sp
   * YUV 4:2:0 planar image, with 8 bit Y samples, followed by interleaved V/U plane with 8bit 2x2
   * subsampled chroma samples.
   *
   * http://www.fourcc.org/yuv.php#NV21
   */
  public static byte[] rotateNV21(@NonNull final byte[] yuv,
                                  final int width,
                                  final int height,
                                  final int rotation,
                                  final boolean flipHorizontal)
      throws IOException
  {
    if (rotation == 0) return yuv;
    if (rotation % 90 != 0 || rotation < 0 || rotation > 270) {
      throw new IllegalArgumentException("0 <= rotation < 360, rotation % 90 == 0");
    } else if ((width * height * 3) / 2 != yuv.length) {
      throw new IOException("provided width and height don't jive with the data length (" +
                            yuv.length + "). Width: " + width + " height: " + height +
                            " = data length: " + (width * height * 3) / 2);
    }

    final byte[]  output    = new byte[yuv.length];
    final int     frameSize = width * height;
    final boolean swap      = rotation % 180 != 0;
    final boolean xflip     = flipHorizontal ? rotation % 270 == 0 : rotation % 270 != 0;
    final boolean yflip     = rotation >= 180;

    for (int j = 0; j < height; j++) {
      for (int i = 0; i < width; i++) {
        final int yIn = j * width + i;
        final int uIn = frameSize + (j >> 1) * width + (i & ~1);
        final int vIn = uIn       + 1;

        final int wOut     = swap ? height              : width;
        final int hOut     = swap ? width               : height;
        final int iSwapped = swap ? j                   : i;
        final int jSwapped = swap ? i                   : j;
        final int iOut     = xflip ? wOut - iSwapped - 1 : iSwapped;
        final int jOut     = yflip ? hOut - jSwapped - 1 : jSwapped;

        final int yOut = jOut * wOut + iOut;
        final int uOut = frameSize + (jOut >> 1) * wOut + (iOut & ~1);
        final int vOut = uOut + 1;

        output[yOut] = (byte)(0xff & yuv[yIn]);
        output[uOut] = (byte)(0xff & yuv[uIn]);
        output[vOut] = (byte)(0xff & yuv[vIn]);
      }
    }
    return output;
  }

  public static Bitmap createFromDrawable(final Drawable drawable, final int width, final int height) {
    final AtomicBoolean created = new AtomicBoolean(false);
    final Bitmap[]      result  = new Bitmap[1];

    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        if (drawable instanceof BitmapDrawable) {
          result[0] = ((BitmapDrawable) drawable).getBitmap();
        } else {
          int canvasWidth = drawable.getIntrinsicWidth();
          if (canvasWidth <= 0) canvasWidth = width;

          int canvasHeight = drawable.getIntrinsicHeight();
          if (canvasHeight <= 0) canvasHeight = height;

          Bitmap bitmap;

          try {
            bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
          } catch (Exception e) {
            Log.w(TAG, e);
            bitmap = null;
          }

          result[0] = bitmap;
        }

        synchronized (result) {
          created.set(true);
          result.notifyAll();
        }
      }
    };

    Util.runOnMain(runnable);

    synchronized (result) {
      while (!created.get()) Util.wait(result, 0);
      return result[0];
    }
  }

  public static int getMaxTextureSize() {
    final int MAX_ALLOWED_TEXTURE_SIZE = 2048;

    EGL10 egl = (EGL10) EGLContext.getEGL();
    EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

    int[] version = new int[2];
    egl.eglInitialize(display, version);

    int[] totalConfigurations = new int[1];
    egl.eglGetConfigs(display, null, 0, totalConfigurations);

    EGLConfig[] configurationsList = new EGLConfig[totalConfigurations[0]];
    egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations);

    int[] textureSize = new int[1];
    int maximumTextureSize = 0;

    for (int i = 0; i < totalConfigurations[0]; i++) {
      egl.eglGetConfigAttrib(display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize);

      if (maximumTextureSize < textureSize[0])
        maximumTextureSize = textureSize[0];
    }

    egl.eglTerminate(display);

    return Math.min(maximumTextureSize, MAX_ALLOWED_TEXTURE_SIZE);
  }
}
