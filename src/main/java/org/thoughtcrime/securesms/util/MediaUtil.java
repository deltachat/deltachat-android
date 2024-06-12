package org.thoughtcrime.securesms.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.b44t.messenger.DcMsg;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.gif.GifDrawable;

import org.thoughtcrime.securesms.mms.AudioSlide;
import org.thoughtcrime.securesms.mms.DecryptableStreamUriLoader.DecryptableUri;
import org.thoughtcrime.securesms.mms.DocumentSlide;
import org.thoughtcrime.securesms.mms.GifSlide;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.mms.ImageSlide;
import org.thoughtcrime.securesms.mms.PartAuthority;
import org.thoughtcrime.securesms.mms.Slide;
import org.thoughtcrime.securesms.mms.StickerSlide;
import org.thoughtcrime.securesms.mms.VcardSlide;
import org.thoughtcrime.securesms.mms.VideoSlide;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

public class MediaUtil {

  private static final String TAG = MediaUtil.class.getSimpleName();

  public static final String IMAGE_WEBP        = "image/webp";
  public static final String IMAGE_JPEG        = "image/jpeg";
  public static final String IMAGE_GIF         = "image/gif";
  public static final String AUDIO_AAC         = "audio/aac";
  public static final String AUDIO_UNSPECIFIED = "audio/*";
  public static final String VIDEO_UNSPECIFIED = "video/*";
  public static final String OCTET             = "application/octet-stream";
  public static final String WEBXDC            = "application/webxdc+zip";
  public static final String VCARD             = "text/vcard";


  public static Slide getSlideForMsg(Context context, DcMsg dcMsg) {
    Slide slide = null;
    if (dcMsg.getType() == DcMsg.DC_MSG_GIF) {
      slide = new GifSlide(context, dcMsg);
    } else if (dcMsg.getType() == DcMsg.DC_MSG_IMAGE) {
      slide = new ImageSlide(context, dcMsg);
    } else if (dcMsg.getType() == DcMsg.DC_MSG_STICKER) {
      slide = new StickerSlide(context, dcMsg);
    } else if (dcMsg.getType() == DcMsg.DC_MSG_VIDEO) {
      slide = new VideoSlide(context, dcMsg);
    } else if (dcMsg.getType() == DcMsg.DC_MSG_AUDIO
            || dcMsg.getType() == DcMsg.DC_MSG_VOICE) {
      slide = new AudioSlide(context, dcMsg);
    } else if (dcMsg.getType() == DcMsg.DC_MSG_VCARD) {
      slide = new VcardSlide(context, dcMsg);
    } else if (dcMsg.getType() == DcMsg.DC_MSG_FILE
            || dcMsg.getType() == DcMsg.DC_MSG_WEBXDC) {
      slide = new DocumentSlide(context, dcMsg);
    }

    return slide;
  }

  public static @Nullable String getMimeType(Context context, Uri uri) {
    if (uri == null) return null;

    if (PersistentBlobProvider.isAuthority(context, uri)) {
      return PersistentBlobProvider.getMimeType(context, uri);
    }

    String type = context.getContentResolver().getType(uri);
    if (type == null) {
      final String extension = getFileExtensionFromUrl(uri.toString());
      type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
      if (type == null) {
        type = "application/octet-stream";
      }
    }
    return getCorrectedMimeType(type);
  }

  public static @Nullable String getCorrectedMimeType(@Nullable String mimeType) {
    if (mimeType == null) return null;

    switch(mimeType) {
    case "image/jpg":
      return MimeTypeMap.getSingleton().hasMimeType(IMAGE_JPEG)
             ? IMAGE_JPEG
             : mimeType;
    default:
      return mimeType;
    }
  }

  /**
   * This is a version of android.webkit.MimeTypeMap.getFileExtensionFromUrl() that
   * doesn't refuse to do its job when there are characters in the URL it doesn't know.
   * Using MimeTypeMap.getFileExtensionFromUrl() led to bugs like this one:
   * https://github.com/deltachat/deltachat-android/issues/2306
   *
   * @return The url's file extension, or "" if there is none.
   */
  public static String getFileExtensionFromUrl(String url) {
    if (TextUtils.isEmpty(url)) return "";

    int fragment = url.lastIndexOf('#');
    if (fragment > 0) {
      url = url.substring(0, fragment);
    }

    int query = url.lastIndexOf('?');
    if (query > 0) {
      url = url.substring(0, query);
    }

    int filenamePos = url.lastIndexOf('/');
    String filename =
            0 <= filenamePos ? url.substring(filenamePos + 1) : url;

    if (!filename.isEmpty()) {
      int dotPos = filename.lastIndexOf('.');
      if (0 <= dotPos) {
        return filename.substring(dotPos + 1);
      }
    }

    return "";
  }

  public static long getMediaSize(Context context, Uri uri) throws IOException {
    InputStream in = PartAuthority.getAttachmentStream(context, uri);
    if (in == null) throw new IOException("Couldn't obtain input stream.");

    long   size   = 0;
    byte[] buffer = new byte[4096];
    int    read;

    while ((read = in.read(buffer)) != -1) {
      size += read;
    }
    in.close();

    return size;
  }

  @WorkerThread
  public static Pair<Integer, Integer> getDimensions(@NonNull Context context, @Nullable String contentType, @Nullable Uri uri) {
    if (uri == null || !MediaUtil.isImageType(contentType)) {
      return new Pair<>(0, 0);
    }

    Pair<Integer, Integer> dimens = null;

    if (MediaUtil.isGif(contentType)) {
      try {
        GifDrawable drawable = GlideApp.with(context)
                .asGif()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .load(new DecryptableUri(uri))
                .submit()
                .get();
        dimens = new Pair<>(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
      } catch (InterruptedException e) {
        Log.w(TAG, "Was unable to complete work for GIF dimensions.", e);
      } catch (ExecutionException e) {
        Log.w(TAG, "Glide experienced an exception while trying to get GIF dimensions.", e);
      }
    } else {
      InputStream attachmentStream = null;
      try {
        if (MediaUtil.isJpegType(contentType)) {
          attachmentStream = PartAuthority.getAttachmentStream(context, uri);
          dimens = BitmapUtil.getExifDimensions(attachmentStream);
          attachmentStream.close();
          attachmentStream = null;
        }
        if (dimens == null) {
          attachmentStream = PartAuthority.getAttachmentStream(context, uri);
          dimens = BitmapUtil.getDimensions(attachmentStream);
        }
      } catch (FileNotFoundException e) {
        Log.w(TAG, "Failed to find file when retrieving media dimensions.", e);
      } catch (IOException e) {
        Log.w(TAG, "Experienced a read error when retrieving media dimensions.", e);
      } catch (BitmapDecodingException e) {
        Log.w(TAG, "Bitmap decoding error when retrieving dimensions.", e);
      } finally {
        if (attachmentStream != null) {
          try {
            attachmentStream.close();
          } catch (IOException e) {
            Log.w(TAG, "Failed to close stream after retrieving dimensions.", e);
          }
        }
      }
    }
    if (dimens == null) {
      dimens = new Pair<>(0, 0);
    }
    Log.d(TAG, "Dimensions for [" + uri + "] are " + dimens.first + " x " + dimens.second);
    return dimens;
  }

  public static boolean isVideo(String contentType) {
    return !TextUtils.isEmpty(contentType) && contentType.trim().startsWith("video/");
  }

  public static boolean isGif(String contentType) {
    return !TextUtils.isEmpty(contentType) && contentType.trim().equals("image/gif");
  }

  public static boolean isJpegType(String contentType) {
    return !TextUtils.isEmpty(contentType) && contentType.trim().equals(IMAGE_JPEG);
  }

  public static boolean isImageType(String contentType) {
    return (null != contentType) && contentType.startsWith("image/");
  }

  public static boolean isAudioType(String contentType) {
    return (null != contentType) && contentType.startsWith("audio/");
  }

  public static boolean isVideoType(String contentType) {
    return (null != contentType) && contentType.startsWith("video/");
  }

  public static boolean isOctetStream(@Nullable String contentType) {
    return OCTET.equals(contentType);
  }

  public static boolean isImageOrVideoType(String contentType) {
    return isImageType(contentType) || isVideoType(contentType);
  }

  public static boolean isImageVideoOrAudioType(String contentType) {
    return isImageOrVideoType(contentType) || isAudioType(contentType);
  }

  public static class ThumbnailSize {
    public ThumbnailSize(int width, int height) {
      this.width = width;
      this.height = height;
    }
    public int width;
    public int height;
  }

  public static boolean createVideoThumbnailIfNeeded(Context context, Uri dataUri, Uri thumbnailUri, ThumbnailSize retWh) {
    boolean success = false;
    try {
      File thumbnailFile = new File(thumbnailUri.getPath());
      File dataFile = new File(dataUri.getPath());
      if (!thumbnailFile.exists() || dataFile.lastModified()>thumbnailFile.lastModified()) {
        Bitmap bitmap = null;

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(context, dataUri);
        bitmap = retriever.getFrameAtTime(-1);
        if (retWh!=null) {
          retWh.width = bitmap.getWidth();
          retWh.height = bitmap.getHeight();
        }
        retriever.release();

        if (bitmap != null) {
          FileOutputStream out = new FileOutputStream(thumbnailFile);
          bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
          success = true;
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return success;
  }

  public static String getExtensionFromMimeType(String contentType) {
    String extension =  MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType);
    if (extension != null) {
      return extension;
    }

    //custom handling needed for unsupported extensions on Android 4.X
    switch (contentType) {
      case AUDIO_AAC:
        return "aac";
      case IMAGE_WEBP:
        return "webp";
      case WEBXDC:
        return "xdc";
      case VCARD:
        return "vcf";
    }
    return null;
  }

}
