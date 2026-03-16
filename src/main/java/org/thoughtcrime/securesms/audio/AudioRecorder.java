package org.thoughtcrime.securesms.audio;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import androidx.annotation.NonNull;
import chat.delta.util.ListenableFuture;
import chat.delta.util.SettableFuture;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.ThreadUtil;
import org.thoughtcrime.securesms.util.Util;

public class AudioRecorder {

  private static final String TAG = AudioRecorder.class.getSimpleName();

  private static final ExecutorService executor = ThreadUtil.newDynamicSingleThreadedExecutor();

  private final Context context;
  private final PersistentBlobProvider blobProvider;

  private final AtomicReference<RecordingState> recordingState = new AtomicReference<>(null);

  private static class RecordingState {
    final AudioCodec audioCodec;
    final String outputFilePath;

    RecordingState(AudioCodec audioCodec, String outputFilePath) {
      this.audioCodec = audioCodec;
      this.outputFilePath = outputFilePath;
    }
  }

  public AudioRecorder(@NonNull Context context) {
    this.context = context;
    this.blobProvider = PersistentBlobProvider.getInstance();
  }

  public void startRecording() {
    Log.w(TAG, "startRecording()");

    executor.execute(
        () -> {
          Log.w(TAG, "Running startRecording() + " + Thread.currentThread().getId());
          try {
            RecordingState currentState = recordingState.get();
            if (currentState != null) {
              throw new AssertionError("We can only record once at a time.");
            }

            // Create temporary file for M4A output
            File outputFile = File.createTempFile("voice", ".m4a", context.getCacheDir());
            String outputFilePath = outputFile.getAbsolutePath();

            AudioCodec audioCodec = new AudioCodec(context, outputFilePath);
            recordingState.set(new RecordingState(audioCodec, outputFilePath));

            audioCodec.start();
          } catch (IOException e) {
            Log.w(TAG, e);
            recordingState.set(null);
          }
        });
  }

  public @NonNull ListenableFuture<Pair<Uri, Long>> stopRecording() {
    Log.w(TAG, "stopRecording()");

    final SettableFuture<Pair<Uri, Long>> future = new SettableFuture<>();

    executor.execute(
        () -> {
          RecordingState state = recordingState.getAndSet(null);

          if (state == null || state.audioCodec == null) {
            sendToFuture(
                future, new IOException("MediaRecorder was never initialized successfully!"));
            return;
          }

          state.audioCodec.stop();

          try {
            File outputFile = new File(state.outputFilePath);

            if (!outputFile.exists()) {
              sendToFuture(future, new IOException("Output file does not exist"));
              return;
            }

            long size = outputFile.length();

            // Create blob using synchronous file-based method
            Uri captureUri =
                blobProvider.create(context, outputFile, MediaUtil.AUDIO_M4A, "voice.m4a", size);

            if (!outputFile.delete()) {
              Log.d(TAG, "Temp file already moved or couldn't be deleted");
            }

            sendToFuture(future, new Pair<>(captureUri, size));
          } catch (IOException ioe) {
            Log.w(TAG, "Failed to create blob from recording", ioe);
            sendToFuture(future, ioe);
          }
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
