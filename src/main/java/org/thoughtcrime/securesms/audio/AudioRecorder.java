package org.thoughtcrime.securesms.audio;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.ThreadUtil;
import org.thoughtcrime.securesms.util.Util;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import chat.delta.util.ListenableFuture;
import chat.delta.util.SettableFuture;

public class AudioRecorder {

  private static final String TAG = AudioRecorder.class.getSimpleName();

  private static final ExecutorService executor = ThreadUtil.newDynamicSingleThreadedExecutor();

  private final Context                context;
  private final PersistentBlobProvider blobProvider;

  private AudioCodec audioCodec;
  private Uri        captureUri;

  public AudioRecorder(@NonNull Context context) {
    this.context      = context;
    this.blobProvider = PersistentBlobProvider.getInstance();
  }

  public void startRecording() {
    Log.w(TAG, "startRecording()");

    executor.execute(() -> {
      Log.w(TAG, "Running startRecording() + " + Thread.currentThread().getId());
      try {
        if (audioCodec != null) {
          throw new AssertionError("We can only record once at a time.");
        }

        ParcelFileDescriptor[] fds = ParcelFileDescriptor.createPipe();

        captureUri  = blobProvider.create(context, new ParcelFileDescriptor.AutoCloseInputStream(fds[0]),
                                          MediaUtil.AUDIO_AAC, "voice.aac", null);
        audioCodec  = new AudioCodec(context);

        audioCodec.start(new ParcelFileDescriptor.AutoCloseOutputStream(fds[1]));
      } catch (IOException e) {
        Log.w(TAG, e);
      }
    });
  }

  public @NonNull ListenableFuture<Pair<Uri, Long>> stopRecording() {
    Log.w(TAG, "stopRecording()");

    final SettableFuture<Pair<Uri, Long>> future = new SettableFuture<>();

    executor.execute(() -> {
      if (audioCodec == null) {
        sendToFuture(future, new IOException("MediaRecorder was never initialized successfully!"));
        return;
      }

      audioCodec.stop();

      try {
        long size = MediaUtil.getMediaSize(context, captureUri);
        sendToFuture(future, new Pair<>(captureUri, size));
      } catch (IOException ioe) {
        Log.w(TAG, ioe);
        sendToFuture(future, ioe);
      }

      audioCodec = null;
      captureUri = null;
    });

    return future;
  }

  private <T> void sendToFuture(final SettableFuture<T> future, final Exception exception) {
    Util.runOnMain(() -> future.setException(exception));
  }

  private <T> void sendToFuture(final SettableFuture<T> future, final T result) {
    Util.runOnMain(() -> future.set(result));
  }
}
