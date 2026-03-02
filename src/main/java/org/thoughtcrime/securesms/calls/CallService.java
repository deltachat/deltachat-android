package org.thoughtcrime.securesms.calls;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.thoughtcrime.securesms.webrtc.WebRTCClient;
import org.webrtc.AudioTrack;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.VideoTrack;

import chat.delta.rpc.RpcException;

/**
 * Foreground service for VoIP calls
 * Required to post CallStyle notifications on Android 12+
 * Owns WebRTC resources and keeps call alive
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class CallService extends Service implements WebRTCClient.Callbacks {

  private static final String TAG = CallService.class.getSimpleName();

  private final IBinder binder = new LocalBinder();

  // WebRTC Resources
  private WebRTCClient webRTCClient;
  private MediaStreamManager mediaStreamManager;

  // Ringtone Resources
  private Ringtone ringtone;
  private AudioManager audioManager;
  private AudioFocusRequest audioFocusRequest;

  private CallCoordinator callCoordinator;

  public class LocalBinder extends Binder {
    CallService getService() {
      return CallService.this;
    }
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "CallService onCreate");

    callCoordinator = CallCoordinator.getInstance(getApplicationContext());

    audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

    Log.d(TAG, "CallService created");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d(TAG, "CallService started");
    return START_STICKY;
  }

  // Initialization

  /**
   * Initialize call infrastructure
   * Does NOT start camera/microphone
   */
  public void initializeCall() {
    Log.d(TAG, "initializeCall (Infrastructure only)");

    if (webRTCClient != null) {
      Log.w(TAG, "Infrastructure already initialized, ignoring");
      return;
    }

    webRTCClient = new WebRTCClient(getApplicationContext(), this);

    mediaStreamManager = new MediaStreamManager(
      getApplicationContext(),
      webRTCClient.getPeerConnectionFactory()
    );

    fetchIceServersAndSetup();
  }

  private void fetchIceServersAndSetup() {
    new Thread(() -> {
      try {
        String iceServersJson = callCoordinator.fetchIceServers();
        Log.d(TAG, "ICE servers fetched: " + iceServersJson);

        webRTCClient.configure(iceServersJson);

        Log.d(TAG, "Infrastructure initialized, waiting for media capture");

      } catch (RpcException e) {
        Log.e(TAG, "Failed to fetch ICE servers", e);
        callCoordinator.reportError("Failed to connect: " + e.getMessage());
      }
    }).start();
  }

  /**
   * Start camera/microphone capture
   * Must be called when app is in foreground
   * Called by coordinator when ViewModel/Activity is ready
   */
  public void startMediaCapture() {
    Log.d(TAG, "startMediaCapture (Camera/Microphone)");

    if (webRTCClient != null && webRTCClient.hasLocalMediaStream()) {
      Log.w(TAG, "Media already initialized, skipping");
      return;
    }

    if (mediaStreamManager == null) {
      Log.e(TAG, "MediaStreamManager not initialized");
      callCoordinator.reportError("MediaStreamManager not initialized");
      return;
    }

    boolean startsWithVideo = callCoordinator.isStartsWithVideo();

    Log.d(TAG, "Creating media stream with video: " + startsWithVideo);

    mediaStreamManager.createMediaStream(new MediaStreamManager.Callback() {
      @Override
      public void onMediaStreamReady(MediaStream stream) {
        Log.d(TAG, "Media stream ready");

        webRTCClient.setLocalMediaStream(stream);
//        webRTCClient.setVideoEnabled(startsWithVideo);

        callCoordinator.setVideoEnabled(startsWithVideo);

        if (!stream.videoTracks.isEmpty()) {
          VideoTrack localTrack = stream.videoTracks.get(0);
          callCoordinator.updateLocalVideoTrack(localTrack);
        } else {
          Log.w(TAG, "Camera unavailable, call will be audio-only");
          callCoordinator.reportError("Camera unavailable, using audio only");
          callCoordinator.setVideoEnabled(false);
        }

        Log.d(TAG, "Media capture complete, ready for call");

      }

      @Override
      public void onError(String error) {
        Log.e(TAG, "Failed to setup media: " + error);
        callCoordinator.reportError("Camera/microphone error: " + error);
        callCoordinator.setVideoEnabled(false);
      }
    });
  }

  // Ringtone Management

  public void startRingtone() {
    Log.d(TAG, "startRingtone");

    // If already playing, don't start again
    if (ringtone != null && ringtone.isPlaying()) {
      Log.d(TAG, "Ringtone already playing");
      return;
    }

    try {
      // Get system default ringtone URI
      Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

      if (ringtoneUri == null) {
        Log.w(TAG, "No default ringtone available");
        return;
      }

      ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);

      if (ringtone == null) {
        Log.e(TAG, "Failed to create Ringtone from URI: " + ringtoneUri);
        return;
      }

      AudioAttributes audioAttributes = new AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
        .setLegacyStreamType(AudioManager.STREAM_RING)
        .build();

      ringtone.setAudioAttributes(audioAttributes);

      // Request audio focus
      audioFocusRequest = new AudioFocusRequest.Builder(
        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
      )
        .setAudioAttributes(audioAttributes)
        .setOnAudioFocusChangeListener(focusChange -> {
          if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            Log.w(TAG, "Lost audio focus, stopping ringtone");
            stopRingtone();
          }
        })
        .setWillPauseWhenDucked(false)
        .build();

      int result = audioManager.requestAudioFocus(audioFocusRequest);
      if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
        Log.w(TAG, "Audio focus not granted");
      }

      ringtone.play();
      Log.d(TAG, "Ringtone started playing");

    } catch (Exception e) {
      Log.e(TAG, "Failed to start ringtone", e);
      // Clean up on error
      stopRingtone();
    }
  }

  /**
   * Stop playing ringtone
   */
  public void stopRingtone() {
    Log.d(TAG, "stopRingtone");

    if (ringtone != null) {
      try {
        if (ringtone.isPlaying()) {
          ringtone.stop();
          Log.d(TAG, "Ringtone stopped");
        }
      } catch (Exception e) {
        Log.e(TAG, "Error stopping ringtone", e);
      }
      ringtone = null;
    }

    if (audioFocusRequest != null && audioManager != null) {
      audioManager.abandonAudioFocusRequest(audioFocusRequest);
      audioFocusRequest = null;
      Log.d(TAG, "Audio focus abandoned");
    }
  }

  // Call Control

  public void startOutgoingCall() {
    Log.d(TAG, "startOutgoingCall");

    if (webRTCClient == null) {
      Log.e(TAG, "Cannot start call, not initialized");
      callCoordinator.reportError("Service not ready");
      return;
    }

    if (!webRTCClient.hasLocalMediaStream()) {
      Log.e(TAG, "Cannot start call, media not ready");
      callCoordinator.reportError("Media not ready");
      return;
    }

    webRTCClient.startOutgoingCall();
  }

  public void answerIncomingCall() {
    Log.d(TAG, "answerIncomingCall");

    String offerSdp = callCoordinator.getPendingOfferSdp();

    if (offerSdp == null) {
      Log.e(TAG, "No offer SDP available");
      callCoordinator.reportError("Call data missing");
      return;
    }

    if (webRTCClient == null) {
      Log.e(TAG, "Cannot answer call, not initialized");
      callCoordinator.reportError("Service not ready");
      return;
    }

    if (!webRTCClient.hasLocalMediaStream()) {
      Log.e(TAG, "Cannot answer, media not ready");
      callCoordinator.reportError("Media not ready");
      return;
    }

    callCoordinator.clearPendingOfferSdp();

    webRTCClient.acceptIncomingCall(offerSdp);
  }

  public void handleAnswerSdp(String answerSdp) {
    Log.d(TAG, "handleAnswerSdp");

    if (webRTCClient == null) {
      Log.e(TAG, "Cannot handle answer, not initialized");
      return;
    }

    webRTCClient.handleAnswerSdp(answerSdp);
  }

  public void setAudioEnabled(boolean enabled) {
    Log.d(TAG, "setAudioEnabled: " + enabled);

    if (webRTCClient != null) {
      webRTCClient.setAudioEnabled(enabled);
    }
  }

  public void setVideoEnabled(boolean enabled) {
    Log.d(TAG, "setVideoEnabled: " + enabled);

    if (webRTCClient != null) {
      webRTCClient.setVideoEnabled(enabled);
    }
  }

  public void sendMutedState(boolean audioEnabled, boolean videoEnabled) {
    Log.d(TAG, "sendMutedState: audio=" + audioEnabled + ", video=" + videoEnabled);

    if (webRTCClient != null) {
      webRTCClient.sendMutedState(audioEnabled, videoEnabled);
    }
  }

  public void switchCamera() {
    Log.d(TAG, "switchCamera");

    if (mediaStreamManager != null) {
      mediaStreamManager.switchCamera();
    }
  }

  public void endCall() {
    Log.d(TAG, "endCall");

    disposeWebRTC();

    stopService();
  }

  // WebRTCClient.Callbacks

  @Override
  public void onOfferReady(String offerSdp) {
    Log.d(TAG, "onOfferReady callback");

    callCoordinator.handleOfferReady(offerSdp);
  }

  @Override
  public void onAnswerReady(String answerSdp) {
    Log.d(TAG, "onAnswerReady callback");

    callCoordinator.handleAnswerReady(answerSdp);
  }

  @Override
  public void onRemoteVideoTrack(VideoTrack videoTrack) {
    Log.d(TAG, "onRemoteVideoTrack callback");

    callCoordinator.updateRemoteVideoTrack(videoTrack);
  }

  @Override
  public void onRemoteAudioTrack(AudioTrack audioTrack) {
    Log.d(TAG, "onRemoteAudioTrack callback");
  }

  @Override
  public void onConnectionStateChanged(PeerConnection.PeerConnectionState state) {
    Log.d(TAG, "onConnectionStateChanged: " + state);

    callCoordinator.updateConnectionState(state);
  }

  @Override
  public void onRemoteMutedStateChanged(boolean audioEnabled, boolean videoEnabled) {
    Log.d(TAG, "onRemoteMutedStateChanged: audio=" + audioEnabled + ", video=" + videoEnabled);

    callCoordinator.updateRemoteMutedState(audioEnabled, videoEnabled);
  }

  @Override
  public void onRelayUsageChanged(Boolean isRelayUsed) {
    Log.d(TAG, "onRelayUsageChanged: " + isRelayUsed);

    callCoordinator.updateRelayUsage(isRelayUsed);
  }

  @Override
  public void onError(String error) {
    Log.e(TAG, "onError: " + error);

    callCoordinator.reportError(error);
  }

  // Foreground Notification

  public void startForegroundWithNotification(int id, Notification notification) {
    Log.d(TAG, "Starting foreground with notification id: " + id);
    startForeground(id, notification);
  }

  public void stopForegroundAndDismiss() {
    Log.d(TAG, "Stopping foreground and dismissing notification");
    stopForeground(STOP_FOREGROUND_REMOVE);
  }

  // Cleanup

  private void disposeWebRTC() {
    Log.d(TAG, "Disposing WebRTC resources");

    if (mediaStreamManager != null) {
      mediaStreamManager.dispose();
      mediaStreamManager = null;
    }

    if (webRTCClient != null) {
      webRTCClient.dispose();
      webRTCClient = null;
    }
  }

  public void stopService() {
    Log.d(TAG, "Stopping CallService");
    stopSelf();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "CallService onDestroy");

    stopRingtone();

    disposeWebRTC();

    Log.d(TAG, "CallService destroyed");
  }
}
