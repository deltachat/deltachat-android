package org.thoughtcrime.securesms.calls;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import org.thoughtcrime.securesms.EglUtils;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

public class MediaStreamManager {

  private static final String TAG = MediaStreamManager.class.getSimpleName();
  private static final String STREAM_ID = "local_stream";
  private static final String AUDIO_TRACK_ID = "audio_track";
  private static final String VIDEO_TRACK_ID = "video_track";

  private final Context context;
  private final PeerConnectionFactory peerConnectionFactory;

  private VideoCapturer videoCapturer;
  private VideoSource videoSource;
  private AudioSource audioSource;
  private SurfaceTextureHelper surfaceTextureHelper;
  private volatile boolean isFrontCamera = true;

  public interface Callback {
    void onMediaStreamReady(MediaStream stream);

    void onError(String error);
  }

  public interface CameraSwitchCallback {
    void onCameraSwitch(boolean isFrontCamera);

    void onError(String error);
  }

  public MediaStreamManager(@NonNull Context context, PeerConnectionFactory peerConnectionFactory) {
    this.context = context.getApplicationContext();

    this.peerConnectionFactory = peerConnectionFactory;
  }

  /** Create media stream with audio and optionally video */
  @RequiresApi(api = Build.VERSION_CODES.M)
  public void createMediaStream(Callback callback) {
    try {
      MediaStream mediaStream = peerConnectionFactory.createLocalMediaStream(STREAM_ID);

      // Create audio track
      MediaConstraints audioConstraints = new MediaConstraints();
      audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
      AudioTrack audioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
      mediaStream.addTrack(audioTrack);

      // Create video track
      videoCapturer = createVideoCapturer();
      if (videoCapturer == null) {
        callback.onError("No camera available");
        callback.onMediaStreamReady(mediaStream);
        return;
      }

      videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
      VideoTrack videoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
      mediaStream.addTrack(videoTrack);

      // Start capturing
      surfaceTextureHelper =
          SurfaceTextureHelper.create("CaptureThread", EglUtils.getEglBase().getEglBaseContext());
      videoCapturer.initialize(surfaceTextureHelper, context, videoSource.getCapturerObserver());
      videoCapturer.startCapture(1280, 720, 30);

      callback.onMediaStreamReady(mediaStream);

    } catch (Exception e) {
      Log.e(TAG, "Failed to create media stream", e);
      callback.onError("Failed to access camera/microphone: " + e.getMessage());
    }
  }

  @Nullable
  private VideoCapturer createVideoCapturer() {
    Camera2Enumerator enumerator = new Camera2Enumerator(context);

    // Try front camera first
    String[] deviceNames = enumerator.getDeviceNames();
    for (String deviceName : deviceNames) {
      if (enumerator.isFrontFacing(deviceName)) {
        VideoCapturer capturer = enumerator.createCapturer(deviceName, null);
        if (capturer != null) {
          isFrontCamera = true;
          return capturer;
        }
      }
    }

    // Fall back to any camera
    for (String deviceName : deviceNames) {
      VideoCapturer capturer = enumerator.createCapturer(deviceName, null);
      if (capturer != null) {
        isFrontCamera = enumerator.isFrontFacing(deviceName);
        return capturer;
      }
    }

    return null;
  }

  public void switchCamera(@Nullable CameraSwitchCallback callback) {
    if (!(videoCapturer instanceof CameraVideoCapturer)) {
      Log.e(TAG, "switchCamera called but videoCapturer is not a CameraVideoCapturer");
      return;
    }

    CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) videoCapturer;

    // Find the opposite-facing camera
    Camera2Enumerator enumerator = new Camera2Enumerator(context);
    String[] deviceNames = enumerator.getDeviceNames();

    String targetCameraName = null;
    for (String deviceName : deviceNames) {
      boolean isTargetFront = !isFrontCamera;
      boolean deviceIsFront = enumerator.isFrontFacing(deviceName);

      if (deviceIsFront == isTargetFront) {
        targetCameraName = deviceName;
        break; // Take the first match
      }
    }

    if (targetCameraName == null) {
      Log.e(TAG, "No camera found with opposite facing direction");
      if (callback != null) {
        callback.onError("No opposite camera available");
      }
      return;
    }

    final String finalTargetCameraName = targetCameraName;
    Log.d(TAG, "Switching to camera: " + finalTargetCameraName);

    // Call with explicit camera name
    cameraVideoCapturer.switchCamera(
        new CameraVideoCapturer.CameraSwitchHandler() {
          @Override
          public void onCameraSwitchDone(boolean isFront) {
            Log.d(TAG, "switchCamera SUCCESS, isFront=" + isFront);
            isFrontCamera = isFront;
            if (callback != null) callback.onCameraSwitch(isFront);
          }

          @Override
          public void onCameraSwitchError(String errorDescription) {
            Log.e(TAG, "switchCamera FAILED: " + errorDescription);
            if (callback != null) callback.onError(errorDescription);
          }
        },
        finalTargetCameraName);
  }

  public boolean isFrontCamera() {
    return isFrontCamera;
  }

  /** Cleanup resources */
  public void dispose() {
    if (videoCapturer != null) {
      try {
        videoCapturer.stopCapture();
      } catch (InterruptedException e) {
        Log.e(TAG, "Error stopping capture", e);
      }
      videoCapturer.dispose();
      videoCapturer = null;
    }

    if (surfaceTextureHelper != null) {
      surfaceTextureHelper.dispose();
      surfaceTextureHelper = null;
    }

    if (videoSource != null) {
      videoSource.dispose();
      videoSource = null;
    }

    if (audioSource != null) {
      audioSource.dispose();
      audioSource = null;
    }
  }
}
