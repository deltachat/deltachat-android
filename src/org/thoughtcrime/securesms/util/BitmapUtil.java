package org.thoughtcrime.securesms.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.exifinterface.media.ExifInterface;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.util.Pair;
import android.view.View;

import com.b44t.messenger.DcMsg;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.mms.MediaConstraints;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

public class BitmapUtil {

  private static final String TAG = BitmapUtil.class.getSimpleName();

  private static final int MAX_COMPRESSION_QUALITY          = 90;
  private static final int MIN_COMPRESSION_QUALITY          = 45;
  private static final int MAX_COMPRESSION_ATTEMPTS         = 5;
  private static final int MIN_COMPRESSION_QUALITY_DECREASE = 5;

  @WorkerThread
  public static <T> ScaleResult createScaledBytes(Context context, T model, MediaConstraints constraints)
      throws BitmapDecodingException
  {
    return createScaledBytes(context, model,
                             constraints.getImageMaxWidth(context),
                             constraints.getImageMaxHeight(context),
                             constraints.getImageMaxSize(context));
  }

  @WorkerThread
  public static <T> ScaleResult createScaledBytes(Context context, T model, int maxImageWidth, int maxImageHeight, int maxImageSize)
      throws BitmapDecodingException
  {
    try {
      int    quality  = MAX_COMPRESSION_QUALITY;
      int    attempts = 0;
      byte[] bytes;

      Bitmap scaledBitmap = GlideApp.with(context.getApplicationContext())
                                    .asBitmap()
                                    .load(model)
                                    .skipMemoryCache(true)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .downsample(DownsampleStrategy.AT_MOST)
                                    .submit(maxImageWidth, maxImageHeight)
                                    .get();

      if (scaledBitmap == null) {
        throw new BitmapDecodingException("Unable to decode image");
      }

      Log.i(TAG, "Initial scaled bitmap has size of " + scaledBitmap.getByteCount() + " bytes.");

      try {
        do {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          scaledBitmap.compress(CompressFormat.JPEG, quality, baos);
          bytes = baos.toByteArray();

          Log.w(TAG, "iteration with quality " + quality + " size " + (bytes.length / 1024) + "kb");
          if (quality == MIN_COMPRESSION_QUALITY) break;

          int nextQuality = (int)Math.floor(quality * Math.sqrt((double)maxImageSize / bytes.length));
          if (quality - nextQuality < MIN_COMPRESSION_QUALITY_DECREASE) {
            nextQuality = quality - MIN_COMPRESSION_QUALITY_DECREASE;
          }
          quality = Math.max(nextQuality, MIN_COMPRESSION_QUALITY);
        }
        while (bytes.length > maxImageSize && attempts++ < MAX_COMPRESSION_ATTEMPTS);

        if (bytes.length > maxImageSize) {
          throw new BitmapDecodingException("Unable to scale image below: " + bytes.length);
        }

        if (bytes.length <= 0) {
          throw new BitmapDecodingException("Decoding failed. Bitmap has a length of " + bytes.length + " bytes.");
        }

        Log.w(TAG, "createScaledBytes(" + model.toString() + ") -> quality " + Math.min(quality, MAX_COMPRESSION_QUALITY) + ", " + attempts + " attempt(s)");
        return new ScaleResult(bytes, scaledBitmap.getWidth(), scaledBitmap.getHeight());
      } finally {
        if (scaledBitmap != null) scaledBitmap.recycle();
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new BitmapDecodingException(e);
    }
  }

  @WorkerThread
  public static <T> Bitmap createScaledBitmap(Context context, T model, int maxWidth, int maxHeight)
      throws BitmapDecodingException
  {
    try {
      return GlideApp.with(context.getApplicationContext())
                     .asBitmap()
                     .load(model)
                     .downsample(DownsampleStrategy.AT_MOST)
                     .submit(maxWidth, maxHeight)
                     .get();
    } catch (InterruptedException | ExecutionException e) {
      throw new BitmapDecodingException(e);
    }
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

  public static InputStream toCompressedJpeg(Bitmap bitmap) {
    ByteArrayOutputStream thumbnailBytes = new ByteArrayOutputStream();
    bitmap.compress(CompressFormat.JPEG, 85, thumbnailBytes);
    return new ByteArrayInputStream(thumbnailBytes.toByteArray());
  }

  public static @Nullable byte[] toByteArray(@Nullable Bitmap bitmap) {
    if (bitmap == null) return null;
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
    return stream.toByteArray();
  }

  public static @Nullable Bitmap fromByteArray(@Nullable byte[] bytes) {
    if (bytes == null) return null;
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
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

  public static class ScaleResult {
    private final byte[] bitmap;
    private final int    width;
    private final int    height;

    public ScaleResult(byte[] bitmap, int width, int height) {
      this.bitmap = bitmap;
      this.width  = width;
      this.height = height;
    }


    public byte[] getBitmap() {
      return bitmap;
    }

    public int getWidth() {
      return width;
    }

    public int getHeight() {
      return height;
    }
  }

  // recode jpeg
  //////////////////////////////////////////////////////////////////////////////////////////////////

  public static void recodeImageMsg(Context context, DcMsg msg)
  {
    int desiredWH = 1280;
    int desiredJpegQuality = 85;

    if (Prefs.isHardCompressionEnabled(context)) {
      desiredWH = 640;
      desiredJpegQuality = 75;
    }

    try {
      String inPath = msg.getFile();
      if(inPath==null || !new File(inPath).exists()) {
        return;
      }

      Matrix matrix = new Matrix();

      // find out scaling
      BitmapFactory.Options bmOptions = new BitmapFactory.Options();
      bmOptions.inJustDecodeBounds = true;
      BitmapFactory.decodeFile(inPath, bmOptions);

      if( bmOptions.outWidth<1 || bmOptions.outHeight<1
       || (bmOptions.outWidth <= desiredWH && bmOptions.outHeight <= desiredWH)) {
        Log.i(TAG, String.format("recoding of %s not needed, image is small enough", inPath));
        return;
      }

      float scaleFactor = Math.min((float)desiredWH/(float)bmOptions.outWidth, (float)desiredWH/(float)bmOptions.outHeight);
      matrix.postScale(scaleFactor, scaleFactor);

      // find out rotating
      {
        android.media.ExifInterface exif;
        try {
          exif = new android.media.ExifInterface(inPath);
          int orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1);
          switch (orientation) {
            case android.media.ExifInterface.ORIENTATION_ROTATE_90:
              matrix.postRotate(90);
              break;
            case android.media.ExifInterface.ORIENTATION_ROTATE_180:
              matrix.postRotate(180);
              break;
            case android.media.ExifInterface.ORIENTATION_ROTATE_270:
              matrix.postRotate(270);
              break;
          }
        } catch (Throwable e) {
          Log.i(TAG, String.format("cannot get exif information for %s", inPath));
        }
      }

      // recode file
      Bitmap inBitmap = BitmapFactory.decodeFile(inPath, null);
      Bitmap outBitmap = Bitmap.createBitmap(inBitmap, 0, 0, inBitmap.getWidth(), inBitmap.getHeight(), matrix, false);
      inBitmap.recycle();

      String outPath = DcHelper.getContext(context).getBlobdirFile(inPath);
      FileOutputStream outStream = new FileOutputStream(outPath);
      if(!outBitmap.compress(Bitmap.CompressFormat.JPEG, desiredJpegQuality, outStream)) {
        return;
      }

      msg.setDimension(outBitmap.getWidth(), outBitmap.getHeight());
      msg.setFile(outPath, null);
      outBitmap.recycle();

      Log.i(TAG, String.format("recoding for %s done", inPath));
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }


  /**
   * Generate a Bitmap from an Android SDK View.
   *
   * @param view the View to be drawn to a Bitmap
   * @return the generated bitmap
   */
  public static Bitmap generate(@NonNull View view) {
    int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
    view.measure(measureSpec, measureSpec);

    int measuredWidth = view.getMeasuredWidth();
    int measuredHeight = view.getMeasuredHeight();

    view.layout(0, 0, measuredWidth, measuredHeight);
    Bitmap bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888);
    bitmap.eraseColor(Color.TRANSPARENT);
    Canvas canvas = new Canvas(bitmap);
    view.draw(canvas);
    return bitmap;
  }

  public static Bitmap generateColoredBitmap(Context context, int colorFilter, @DrawableRes int res) {
    Bitmap icon = getBitmap(context, res);
    Paint paint = new Paint();
    ColorFilter filter = new PorterDuffColorFilter(colorFilter, PorterDuff.Mode.SRC_IN);
    paint.setColorFilter(filter);
    Canvas canvas = new Canvas(icon);
    canvas.drawBitmap(icon, 0, 0, paint);
    return icon;
  }

  private static Bitmap getBitmap(Context context, @DrawableRes int res) {
    Drawable drawable = ContextCompat.getDrawable(context, res);
    Canvas canvas = new Canvas();
    Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    canvas.setBitmap(bitmap);
    drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
    drawable.draw(canvas);
    return bitmap;
  }
}
