/*
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.mms;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.MediaPreviewActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.ShareLocationDialog;
import org.thoughtcrime.securesms.WebxdcActivity;
import org.thoughtcrime.securesms.WebxdcStoreActivity;
import org.thoughtcrime.securesms.attachments.Attachment;
import org.thoughtcrime.securesms.attachments.UriAttachment;
import org.thoughtcrime.securesms.audio.AudioSlidePlayer;
import org.thoughtcrime.securesms.components.AudioView;
import org.thoughtcrime.securesms.components.DocumentView;
import org.thoughtcrime.securesms.components.RemovableEditableMediaView;
import org.thoughtcrime.securesms.components.ThumbnailView;
import org.thoughtcrime.securesms.components.VcardView;
import org.thoughtcrime.securesms.components.WebxdcView;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.AttachmentDatabase;
import org.thoughtcrime.securesms.geolocation.DcLocationManager;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.scribbles.ScribbleActivity;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.guava.Optional;
import org.thoughtcrime.securesms.util.views.Stub;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import chat.delta.rpc.RpcException;
import chat.delta.util.ListenableFuture;
import chat.delta.util.SettableFuture;


public class AttachmentManager {

  private final static String TAG = AttachmentManager.class.getSimpleName();

  private final @NonNull Context                    context;
  private final @NonNull Stub<View>                 attachmentViewStub;
  private final @NonNull
  AttachmentListener attachmentListener;

  private RemovableEditableMediaView removableMediaView;
  private ThumbnailView              thumbnail;
  private AudioView                  audioView;
  private DocumentView               documentView;
  private WebxdcView                 webxdcView;
  private VcardView                  vcardView;
  //private SignalMapView              mapView;

  private final @NonNull  List<Uri>       garbage = new LinkedList<>();
  private @NonNull  Optional<Slide> slide   = Optional.absent();
  private @Nullable Uri             imageCaptureUri;
  private @Nullable Uri             videoCaptureUri;
  private boolean                   attachmentPresent;
  private boolean                   hidden;

  public AttachmentManager(@NonNull Activity activity, @NonNull AttachmentListener listener) {
    this.context            = activity;
    this.attachmentListener = listener;
    this.attachmentViewStub = ViewUtil.findStubById(activity, R.id.attachment_editor_stub);
  }

  private void inflateStub() {
    if (!attachmentViewStub.resolved()) {
      View root = attachmentViewStub.get();

      this.thumbnail          = ViewUtil.findById(root, R.id.attachment_thumbnail);
      this.audioView          = ViewUtil.findById(root, R.id.attachment_audio);
      this.documentView       = ViewUtil.findById(root, R.id.attachment_document);
      this.webxdcView         = ViewUtil.findById(root, R.id.attachment_webxdc);
      this.vcardView          = ViewUtil.findById(root, R.id.attachment_vcard);
      //this.mapView            = ViewUtil.findById(root, R.id.attachment_location);
      this.removableMediaView = ViewUtil.findById(root, R.id.removable_media_view);

      removableMediaView.setRemoveClickListener(new RemoveButtonListener());
      removableMediaView.setEditClickListener(new EditButtonListener());
      thumbnail.setOnClickListener(new ThumbnailClickListener());
    }

  }

  public void clear(@NonNull GlideRequests glideRequests, boolean animate) {
    if (attachmentViewStub.resolved()) {

      if (animate) {
        ViewUtil.fadeOut(attachmentViewStub.get(), 200).addListener(new ListenableFuture.Listener<Boolean>() {
          @Override
          public void onSuccess(Boolean result) {
            thumbnail.clear(glideRequests);
            setAttachmentPresent(false);
            attachmentListener.onAttachmentChanged();
          }

          @Override
          public void onFailure(ExecutionException e) {
          }
        });
      } else {
        thumbnail.clear(glideRequests);
        setAttachmentPresent(false);
        attachmentListener.onAttachmentChanged();
      }

      markGarbage(getSlideUri());
      slide = Optional.absent();

      audioView.cleanup();
    }
  }

  public void cleanup() {
    cleanup(imageCaptureUri);
    cleanup(videoCaptureUri);
    cleanup(getSlideUri());

    imageCaptureUri = null;
    videoCaptureUri = null;
    slide           = Optional.absent();

    Iterator<Uri> iterator = garbage.listIterator();

    while (iterator.hasNext()) {
      cleanup(iterator.next());
      iterator.remove();
    }
  }

  private void cleanup(final @Nullable Uri uri) {
    if (uri != null && PersistentBlobProvider.isAuthority(context, uri)) {
      Log.w(TAG, "cleaning up " + uri);
      PersistentBlobProvider.getInstance().delete(context, uri);
    }
  }

  private void markGarbage(@Nullable Uri uri) {
    if (uri != null && PersistentBlobProvider.isAuthority(context, uri)) {
      Log.w(TAG, "Marking garbage that needs cleaning: " + uri);
      garbage.add(uri);
    }
  }

  private void setSlide(@NonNull Slide slide) {
    if (getSlideUri() != null)                                              cleanup(getSlideUri());
    if (imageCaptureUri != null && !imageCaptureUri.equals(slide.getUri())) cleanup(imageCaptureUri);
    if (videoCaptureUri != null && !videoCaptureUri.equals(slide.getUri())) cleanup(videoCaptureUri);

    this.imageCaptureUri = null;
    this.videoCaptureUri = null;
    this.slide           = Optional.of(slide);
  }

  /*
  public ListenableFuture<Boolean> setLocation(@NonNull final SignalPlace place,
                                               @NonNull final MediaConstraints constraints)
  {
    inflateStub();

    SettableFuture<Boolean>  returnResult = new SettableFuture<>();
    ListenableFuture<Bitmap> future       = mapView.display(place);

    attachmentViewStub.get().setVisibility(View.VISIBLE);
    removableMediaView.display(mapView, false);

    future.addListener(new AssertedSuccessListener<Bitmap>() {
      @Override
      public void onSuccess(@NonNull Bitmap result) {
        byte[]        blob          = BitmapUtil.toByteArray(result);
        Uri           uri           = PersistentBlobProvider.getInstance(context)
                                                            .create(context, blob, MediaUtil.IMAGE_PNG, null);
        LocationSlide locationSlide = new LocationSlide(context, uri, blob.length, place);

        setSlide(locationSlide);
        attachmentListener.onAttachmentChanged();
        returnResult.set(true);
      }
    });

    return returnResult;
  }
  */

  @SuppressLint("StaticFieldLeak")
  public ListenableFuture<Boolean> setMedia(@NonNull final GlideRequests glideRequests,
                                            @NonNull final Uri uri,
                                            @Nullable final DcMsg msg,
                                            @NonNull final MediaType mediaType,
                                            final int width,
                                            final int height,
                                            final int chatId)
  {
    inflateStub();

    final SettableFuture<Boolean> result = new SettableFuture<>();

    new AsyncTask<Void, Void, Slide>() {
      @Override
      protected void onPreExecute() {
        thumbnail.clear(glideRequests);
        setAttachmentPresent(true);
      }

      @Override
      protected @Nullable Slide doInBackground(Void... params) {
        try {
          if (msg != null && msg.getType() == DcMsg.DC_MSG_WEBXDC) {
            return new DocumentSlide(context, msg);
          }
          else if (PartAuthority.isLocalUri(uri)) {
            return getManuallyCalculatedSlideInfo(uri, width, height, msg);
          } else {
            Slide result = getContentResolverSlideInfo(uri, width, height, chatId);

            if (result == null) return getManuallyCalculatedSlideInfo(uri, width, height, msg);
            else                return result;
          }
        } catch (IOException e) {
          Log.w(TAG, e);
          return null;
        }
      }

      @Override
      protected void onPostExecute(@Nullable final Slide slide) {
        if (slide == null) {
          setAttachmentPresent(false);
          result.set(false);
        } else if (slide.getFileSize() > 1024 * 1024 * 1024) {
          // this is only a rough check, videos and images may be recoded
          // and the core checks more carefully later.
          setAttachmentPresent(false);
          Log.w(TAG, "File too large.");
          Toast.makeText(slide.context, "File too large.", Toast.LENGTH_LONG).show();
          result.set(false);
        } else {
          setSlide(slide);
          setAttachmentPresent(true);

          if (slide.hasAudio()) {
            class SetDurationListener implements AudioSlidePlayer.Listener {
              @Override
              public void onStart() {}

              @Override
              public void onStop() {}

              @Override
              public void onProgress(AudioSlide slide, double progress, long millis) {}

              @Override
              public void onReceivedDuration(int millis) {
                ((AudioView) removableMediaView.getCurrent()).setDuration(millis);
              }
            }
            AudioSlidePlayer audioSlidePlayer = AudioSlidePlayer.createFor(context, (AudioSlide) slide, new SetDurationListener());
            audioSlidePlayer.requestDuration();

            audioView.setAudio((AudioSlide) slide, 0);
            removableMediaView.display(audioView, false);
            result.set(true);
          } else if (slide.isVcard()) {
              vcardView.setVcard(glideRequests, (VcardSlide)slide, DcHelper.getRpc(context));
              removableMediaView.display(vcardView, false);
          } else if (slide.hasDocument()) {
            if (slide.isWebxdcDocument()) {
              DcMsg instance = msg != null ? msg : DcHelper.getContext(context).getMsg(slide.dcMsgId);
              webxdcView.setWebxdc(instance, context.getString(R.string.webxdc_draft_hint));
              webxdcView.setWebxdcClickListener((v, s) -> {
                WebxdcActivity.openWebxdcActivity(context, instance);
              });
              removableMediaView.display(webxdcView, false);
            } else {
              documentView.setDocument((DocumentSlide) slide);
              removableMediaView.display(documentView, false);
            }
            result.set(true);
          } else {
            Attachment attachment = slide.asAttachment();
            result.deferTo(thumbnail.setImageResource(glideRequests, slide, attachment.getWidth(), attachment.getHeight()));
            removableMediaView.display(thumbnail, mediaType == MediaType.IMAGE);
          }

          attachmentListener.onAttachmentChanged();
        }
      }

      private @Nullable Slide getContentResolverSlideInfo(Uri uri, int width, int height, int chatId) {

        long   start  = System.currentTimeMillis();
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {

          if (cursor != null && cursor.moveToFirst()) {
            String fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
            long fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
            String mimeType = context.getContentResolver().getType(uri);

            if (width == 0 || height == 0) {
              Pair<Integer, Integer> dimens = MediaUtil.getDimensions(context, mimeType, uri);
              width = dimens.first;
              height = dimens.second;
            }

            Log.w(TAG, "remote slide with size " + fileSize + " took " + (System.currentTimeMillis() - start) + "ms");
            return mediaType.createSlide(context, uri, fileName, mimeType, fileSize, width, height, chatId);
          }
        }

        return null;
      }

      private @NonNull Slide getManuallyCalculatedSlideInfo(Uri uri, int width, int height, @Nullable DcMsg msg) throws IOException {
        long start      = System.currentTimeMillis();
        Long mediaSize  = null;
        String fileName = null;
        String mimeType = null;

        if (msg != null) {
          fileName = msg.getFilename();
          mimeType = msg.getFilemime();
        }

        if (PartAuthority.isLocalUri(uri)) {
          mediaSize = PartAuthority.getAttachmentSize(context, uri);
          if (fileName == null) fileName = PartAuthority.getAttachmentFileName(context, uri);
          if (mimeType == null) mimeType = PartAuthority.getAttachmentContentType(context, uri);
        }

        if (mediaSize == null) {
          mediaSize = MediaUtil.getMediaSize(context, uri);
        }

        if (mimeType == null) {
          mimeType = MediaUtil.getMimeType(context, uri);
        }

        if (width == 0 || height == 0) {
          Pair<Integer, Integer> dimens = MediaUtil.getDimensions(context, mimeType, uri);
          width  = dimens.first;
          height = dimens.second;
        }

        if (fileName == null) {
          try {
            fileName = new File(uri.getPath()).getName();
          } catch(Exception e) {
            Log.w(TAG, "Could not get file name from uri: " + e);
          }
        }

        Log.w(TAG, "local slide with size " + mediaSize + " took " + (System.currentTimeMillis() - start) + "ms");
        return mediaType.createSlide(context, uri, fileName, mimeType, mediaSize, width, height, chatId);
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    return result;
  }

  // should be called when the attachment manager comes into view again.
  // if the attachment manager contains a webxdc, its summary is updated.
  public void onResume() {
    if (slide.isPresent()) {
      if (slide.get().isWebxdcDocument()) {
        if (webxdcView != null) {
          webxdcView.setWebxdc(DcHelper.getContext(context).getMsg(slide.get().dcMsgId), context.getString(R.string.webxdc_draft_hint));
        }
      }
    }
  }

  public boolean isAttachmentPresent() {
    return attachmentPresent;
  }

  public @NonNull SlideDeck buildSlideDeck() {
    SlideDeck deck = new SlideDeck();
    if (slide.isPresent()) deck.addSlide(slide.get());
    return deck;
  }

  public static @Nullable String getFileName(Context context, Uri uri) {
    String result = null;
    if ("content".equals(uri.getScheme())) {
      try (Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
        if (cursor != null && cursor.moveToFirst()) {
          result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
        }
      }
    }
    if (result == null) {
      result = uri.getLastPathSegment();
    }
    return result;
  }

  public static void selectDocument(Activity activity, int requestCode) {
    selectMediaType(activity, "*/*", null, requestCode);
  }

  public static void selectWebxdc(Activity activity, int requestCode) {
    Intent intent = new Intent(activity, WebxdcStoreActivity.class);
    activity.startActivityForResult(intent, requestCode);
  }

  public static void selectGallery(Activity activity, int requestCode) {
    // to enable camera roll,
    // we're asking for "gallery permissions" also on newer systems that do not strictly require that.
    // (asking directly after tapping "attachment" would be not-so-good as the user may want to attach sth. else
    // and asking for permissions is better done on-point)
    Permissions.with(activity)
               .request(Permissions.galleryPermissions())
               .ifNecessary()
               .withPermanentDenialDialog(activity.getString(R.string.perm_explain_access_to_storage_denied))
               .onAllGranted(() -> selectMediaType(activity, "image/*", new String[] {"image/*", "video/*"}, requestCode, null, true))
               .execute();
  }

  public static void selectImage(Activity activity, int requestCode) {
    Permissions.with(activity)
            .request(Permissions.galleryPermissions())
            .ifNecessary()
            .withPermanentDenialDialog(activity.getString(R.string.perm_explain_access_to_storage_denied))
            .onAllGranted(() -> selectMediaType(activity, "image/*", null, requestCode))
            .execute();
  }

  public static void selectLocation(Activity activity, int chatId) {
    ApplicationContext applicationContext = ApplicationContext.getInstance(activity);
    DcLocationManager dcLocationManager = applicationContext.dcLocationManager;

    if (DcHelper.getContext(applicationContext).isSendingLocationsToChat(chatId)) {
      dcLocationManager.stopSharingLocation(chatId);
      return;
    }

    // see https://support.google.com/googleplay/android-developer/answer/9799150#zippy=%2Cstep-provide-prominent-in-app-disclosure
    // for rationale dialog requirements
    Permissions.PermissionsBuilder permissionsBuilder = Permissions.with(activity)
            .ifNecessary()
            .withRationaleDialog("To share your live location with chat members, allow Delta Chat to use your location data.\n\nTo make live location work gaplessly, location data is used even when the app is closed or not in use.", R.drawable.ic_location_on_white_24dp)
            .withPermanentDenialDialog(activity.getString(R.string.perm_explain_access_to_location_denied))
            .onAllGranted(() -> {
              ShareLocationDialog.show(activity, durationInSeconds -> {
                if (durationInSeconds == 1) {
                  dcLocationManager.shareLastLocation(chatId);
                } else {
                  dcLocationManager.shareLocation(durationInSeconds, chatId);
                }
              });
            });
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
      permissionsBuilder.request(Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);
    } else {
      permissionsBuilder.request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);
    }
    permissionsBuilder.execute();
  }

  private @Nullable Uri getSlideUri() {
    return slide.isPresent() ? slide.get().getUri() : null;
  }

  public @Nullable Uri getImageCaptureUri() {
    return imageCaptureUri;
  }

  public @Nullable Uri getVideoCaptureUri() {
    return videoCaptureUri;
  }

  public void capturePhoto(Activity activity, int requestCode) {
    Permissions.with(activity)
               .request(Manifest.permission.CAMERA)
               .ifNecessary()
               .withPermanentDenialDialog(activity.getString(R.string.perm_explain_access_to_camera_denied))
               .onAllGranted(() -> {
                 try {
                   Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                   if (captureIntent.resolveActivity(activity.getPackageManager()) != null) {
                     if (imageCaptureUri == null) {
                       imageCaptureUri = PersistentBlobProvider.getInstance().createForExternal(context, MediaUtil.IMAGE_JPEG);
                     }
                     Log.w(TAG, "imageCaptureUri path is " + imageCaptureUri.getPath());
                     captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageCaptureUri);
                     activity.startActivityForResult(captureIntent, requestCode);
                   }
                 } catch (Exception e) {
                   Log.w(TAG, e);
                 }
               })
               .execute();
  }

  public void captureVideo(Activity activity, int requestCode) {
    Permissions.with(activity)
        .request(Manifest.permission.CAMERA)
        .ifNecessary()
        .withPermanentDenialDialog(activity.getString(R.string.perm_explain_access_to_camera_denied))
        .onAllGranted(() -> {
          try {
            Intent captureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            if (captureIntent.resolveActivity(activity.getPackageManager()) != null) {
              if (videoCaptureUri==null) {
                videoCaptureUri = PersistentBlobProvider.getInstance().createForExternal(context, "video/mp4");
              }
              Log.w(TAG, "videoCaptureUri path is " + videoCaptureUri.getPath());
              captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoCaptureUri);
              captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
              captureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
              activity.startActivityForResult(captureIntent, requestCode);
            } else {
              new AlertDialog.Builder(activity)
                .setCancelable(false)
                .setMessage("Video recording not available")
                .setPositiveButton(android.R.string.ok, null)
                .show();
            }
          } catch (Exception e) {
            Log.w(TAG, e);
          }
        })
        .execute();
  }

  public static void selectMediaType(Activity activity, @NonNull String type, @Nullable String[] extraMimeType, int requestCode) {
    selectMediaType(activity, type, extraMimeType, requestCode, null, false);
  }

  public static void selectMediaType(Activity activity, @NonNull String type, @Nullable String[] extraMimeType, int requestCode, @Nullable Uri initialUri) {
    selectMediaType(activity, type, extraMimeType, requestCode, initialUri, false);
  }

  public static void selectMediaType(Activity activity, @NonNull String type, @Nullable String[] extraMimeType, int requestCode, @Nullable Uri initialUri, boolean allowMultiple) {
    final Intent intent = new Intent();
    intent.setType(type);

    if (extraMimeType != null) {
      intent.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeType);
    }

    if (initialUri != null && Build.VERSION.SDK_INT >= 26) {
      intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
    }

    if (allowMultiple) {
      intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
    }

    intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
    try {
      activity.startActivityForResult(intent, requestCode);
      return;
    } catch (ActivityNotFoundException anfe) {
      Log.w(TAG, "couldn't complete ACTION_OPEN_DOCUMENT, no activity found. falling back.");
    }

    intent.setAction(Intent.ACTION_GET_CONTENT);

    try {
      activity.startActivityForResult(intent, requestCode);
    } catch (ActivityNotFoundException anfe) {
      Log.w(TAG, "couldn't complete ACTION_GET_CONTENT intent, no activity found. falling back.");
      Toast.makeText(activity, R.string.no_app_to_handle_data, Toast.LENGTH_LONG).show();
    }
  }

  private void previewImageDraft(final @NonNull Slide slide) {
    if (MediaPreviewActivity.isTypeSupported(slide) && slide.getUri() != null) {
      Intent intent = new Intent(context, MediaPreviewActivity.class);
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      intent.putExtra(MediaPreviewActivity.DC_MSG_ID, slide.getDcMsgId());
      intent.putExtra(MediaPreviewActivity.OUTGOING_EXTRA, true);
      intent.setDataAndType(slide.getUri(), slide.getContentType());

      context.startActivity(intent);
    }
  }

  private class ThumbnailClickListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
      if (slide.isPresent()) previewImageDraft(slide.get());
    }
  }

  private class RemoveButtonListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
      cleanup();
      clear(GlideApp.with(context.getApplicationContext()), true);
    }
  }

  private class EditButtonListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
      Uri imgUri = getSlideUri();
      if (imgUri != null) {
        Intent intent = new Intent(context, ScribbleActivity.class);
        intent.setData(imgUri);
        ((Activity) context).startActivityForResult(intent, ScribbleActivity.SCRIBBLE_REQUEST_CODE);
      }
    }
  }

  public interface AttachmentListener {
    void onAttachmentChanged();
  }

  public enum MediaType {
    IMAGE, GIF, AUDIO, VIDEO, DOCUMENT;

    public @NonNull Slide createSlide(@NonNull  Context context,
                                      @NonNull  Uri     uri,
                                      @Nullable String fileName,
                                      @Nullable String mimeType,
                                                long    dataSize,
                                                int     width,
                                                int     height,
                                                int     chatId)
    {
      if (mimeType == null) {
        mimeType = "application/octet-stream";
      }

      switch (this) {
      case IMAGE:    return new ImageSlide(context, uri, fileName, dataSize, width, height);
      case GIF:      return new GifSlide(context, uri, fileName, dataSize, width, height);
      case AUDIO:    return new AudioSlide(context, uri, dataSize, false, fileName);
      case VIDEO:    return new VideoSlide(context, uri, fileName, dataSize);
      case DOCUMENT:
        // We have to special-case Webxdc slides: The user can interact with them as soon as a draft
        // is set. Therefore we need to create a DcMsg already now.
        if (fileName != null && fileName.endsWith(".xdc")) {
          DcContext dcContext = DcHelper.getContext(context);
          DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_WEBXDC);
          Attachment attachment = new UriAttachment(uri, null, MediaUtil.WEBXDC, AttachmentDatabase.TRANSFER_PROGRESS_STARTED, 0, 0, 0, fileName, null, false);
          String path = attachment.getRealPath(context);
          msg.setFileAndDeduplicate(path, fileName, MediaUtil.WEBXDC);
          dcContext.setDraft(chatId, msg);
          return new DocumentSlide(context, msg);
        }

        if (mimeType.equals(MediaUtil.VCARD) || (fileName != null && (fileName.endsWith(".vcf") || fileName.endsWith(".vcard")))) {
          VcardSlide slide = new VcardSlide(context, uri, dataSize, fileName);
          String path = slide.asAttachment().getRealPath(context);
          try {
            if (DcHelper.getRpc(context).parseVcard(path).size() == 1) {
              return slide;
            }
          } catch (RpcException e) {
            Log.e(TAG, "Error in call to rpc.parseVcard()", e);
          }
        }

        return new DocumentSlide(context, uri, mimeType, dataSize, fileName);
      default:       throw  new AssertionError("unrecognized enum");
      }
    }
  }

  public void setHidden(boolean hidden) {
    this.hidden = hidden;
    updateVisibility();
  }

  private void setAttachmentPresent(boolean isPresent) {
    this.attachmentPresent = isPresent;
    updateVisibility();
  }

  private void updateVisibility() {
    int vis;
    if (attachmentPresent && !hidden) {
      vis = View.VISIBLE;
    } else {
      vis = View.GONE;
    }
    if (vis == View.GONE && !attachmentViewStub.resolved()) {
      return;
    }
    attachmentViewStub.get().setVisibility(vis);
  }
}
