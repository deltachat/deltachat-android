package org.thoughtcrime.securesms.calls;

import android.app.Application;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.telecom.DisconnectCause;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.telecom.CallEndpointCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import org.webrtc.PeerConnection;
import org.webrtc.VideoTrack;

import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.O)
public class CallViewModel extends AndroidViewModel {

  private static final String TAG = CallViewModel.class.getSimpleName();

  private final CallCoordinator callCoordinator;

  // UI State LiveData (from Coordinator)

  private final LiveData<VideoTrack> localVideoTrack;
  private final LiveData<VideoTrack> remoteVideoTrack;
  private final LiveData<Boolean> localAudioEnabled;
  private final LiveData<Boolean> localVideoEnabled;
  private final LiveData<Boolean> remoteAudioEnabled;
  private final LiveData<Boolean> remoteVideoEnabled;
  private final LiveData<Boolean> isRelayUsed;
  private final LiveData<String> errorMessage;
  private final LiveData<String> displayName;
  private final LiveData<Icon> displayIcon;
  private final LiveData<CallEndpointCompat> currentAudioEndpoint;
  private final LiveData<List<CallEndpointCompat>> availableAudioEndpoints;

  // Translated from coordinator's connectionState
  private final MediatorLiveData<CallState> callState;

  // Observer References for one-time observe
  private Observer<VideoTrack> answerCallObserver;
  private Observer<VideoTrack> startOutgoingCallObserver;

  private boolean hasCallEnded = false;

  // User-facing call states
  public enum CallState {
    INITIALIZING,
    PROMPTING_USER_ACCEPT,
    RINGING,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ENDED,
    ERROR
  }

  public CallViewModel(@NonNull Application application) {
    super(application);

    this.callCoordinator = CallCoordinator.getInstance(application);

    // Setup LiveData observations
    this.localVideoTrack = callCoordinator.getLocalVideoTrack();
    this.remoteVideoTrack = callCoordinator.getRemoteVideoTrack();
    this.localAudioEnabled = callCoordinator.getLocalAudioEnabled();
    this.localVideoEnabled = callCoordinator.getLocalVideoEnabled();
    this.remoteAudioEnabled = callCoordinator.getRemoteAudioEnabled();
    this.remoteVideoEnabled = callCoordinator.getRemoteVideoEnabled();
    this.isRelayUsed = callCoordinator.getIsRelayUsed();
    this.errorMessage = callCoordinator.getErrorMessage();
    this.displayName = callCoordinator.getDisplayName();
    this.displayIcon = callCoordinator.getDisplayIcon();
    this.currentAudioEndpoint = callCoordinator.getCurrentAudioEndpoint();
    this.availableAudioEndpoints = callCoordinator.getAvailableAudioEndpoints();

    this.callState = new MediatorLiveData<>(CallState.INITIALIZING);

    setupConnectionStateObserver();

    Log.d(TAG, "CallViewModel created");
  }

  public void initialize() {
    Log.d(TAG, "Initializing CallViewModel");

    callCoordinator.setActiveCallViewModel(this);

    if (callCoordinator.isIncomingCall()) {
      callState.setValue(CallState.PROMPTING_USER_ACCEPT);
    } else {
      callState.setValue(CallState.RINGING);
    }

    Log.d(TAG, "CallViewModel initialized");
  }

  private void setupConnectionStateObserver() {
    callState.addSource(callCoordinator.getConnectionState(), state -> {
      CallState newState = translateConnectionState(state);

      if (callState.getValue() != newState) {
        callState.setValue(newState);
      }

      if (state == PeerConnection.PeerConnectionState.FAILED ||
        state == PeerConnection.PeerConnectionState.CLOSED) {
        if (!hasCallEnded) {
          hasCallEnded = true;
        }
      }
    });
  }

  private CallState translateConnectionState(PeerConnection.PeerConnectionState state) {
    switch (state) {
      case NEW:
        if (callCoordinator.isIncomingCall()) {
          return CallState.PROMPTING_USER_ACCEPT;
        } else {
          return CallState.RINGING;
        }

      case CONNECTING:
        if (callCoordinator.isIncomingCall()) {
          return CallState.CONNECTING;
        } else {
          return CallState.RINGING;  // Mirror TypeScript
        }

      case CONNECTED:
        return CallState.CONNECTED;

      case DISCONNECTED:
        return CallState.RECONNECTING;

      case FAILED:
        return CallState.ERROR;

      case CLOSED:
        return CallState.ENDED;

      default:
        return CallState.INITIALIZING;
    }
  }

  // Call Control

  public void answerCall() {
    Log.d(TAG, "answerCall");

    if (!callCoordinator.isIncomingCall()) {
      Log.w(TAG, "answerCall() called but this is not an incoming call");
      return;
    }

    // System integration
    callCoordinator.handleCallControlScopeAnswer();

    answerCallWhenReady();
  }

  /**
   * Answer incoming call (WebRTC only)
   * Used when system answer already happened
   */
  public void answerCallWhenReady() {
    Log.d(TAG, "answerCallWhenReady");

    // Start media capture
    callCoordinator.startMediaCapture();

    // Create one-time observer
    LiveData<VideoTrack> localTrack = callCoordinator.getLocalVideoTrack();

    answerCallObserver = new Observer<VideoTrack>() {
      @Override
      public void onChanged(VideoTrack videoTrack) {
        if (videoTrack != null) {
          // Media is ready, remove observer
          localTrack.removeObserver(this);
          answerCallObserver = null;

          Log.d(TAG, "Local video ready, answering call (WebRTC)");

          callCoordinator.answerWebRTC();
        }
      }
    };

    localTrack.observeForever(answerCallObserver);
  }

  /**
   * Start outgoing call with media capture
   * Called by Activity for outgoing calls
   */
  public void startOutgoingCallWhenReady() {
    Log.d(TAG, "startOutgoingCallWhenReady");

    callCoordinator.startMediaCapture();

    // Create one-time observer
    LiveData<VideoTrack> localTrack = callCoordinator.getLocalVideoTrack();

    startOutgoingCallObserver = new Observer<VideoTrack>() {
      @Override
      public void onChanged(VideoTrack videoTrack) {
        if (videoTrack != null) {
          // Media is ready, remove observer
          localTrack.removeObserver(this);
          startOutgoingCallObserver = null;

          Log.d(TAG, "Local video ready, starting outgoing call");

          callCoordinator.startOutgoingCall();
        }
      }
    };

    localTrack.observeForever(startOutgoingCallObserver);
  }

  public void declineCall() {
    Log.d(TAG, "declineCall");

    if (hasCallEnded) {
      Log.w(TAG, "Call already ended");
      return;
    }
    hasCallEnded = true;

    callCoordinator.declineCall();
  }

  public void hangUp() {
    Log.d(TAG, "hangUp");

    if (hasCallEnded) {
      Log.w(TAG, "Call already ended");
      return;
    }
    hasCallEnded = true;

    callCoordinator.hangUp();
  }

  public void toggleAudio() {
    Boolean current = localAudioEnabled.getValue();
    boolean newState = current == null || !current;

    Log.d(TAG, "toggleAudio: " + newState);

    callCoordinator.setAudioEnabled(newState);
  }

  public void toggleVideo() {
    Boolean current = localVideoEnabled.getValue();
    boolean newState = current == null || !current;

    Log.d(TAG, "toggleVideo: " + newState);

    callCoordinator.setVideoEnabled(newState);
  }

  public void switchCamera() {
    Log.d(TAG, "switchCamera");

    callCoordinator.switchCamera();
  }

  public void selectAudioDevice(CallEndpointCompat endpoint) {
    Log.d(TAG, "selectAudioDevice: " + endpoint.getName());

    callCoordinator.requestAudioEndpointChange(endpoint);
  }

  public void setStartsWithVideo(boolean startsWithVideo) {
    Log.d(TAG, "setStartsWithVideo: " + startsWithVideo);
    callCoordinator.setStartsWithVideo(startsWithVideo);
  }

  // CallControlScope Callbacks

  public void onCallAnswered() {
    Log.d(TAG, "onCallAnswered callback from CallControlScope");
  }

  public void onCallActive() {
    Log.d(TAG, "onCallActive callback from CallControlScope");
  }

  public void onCallInactive() {
    Log.d(TAG, "onCallInactive callback from CallControlScope");
  }

  public void onCallDisconnected(DisconnectCause disconnectCause) {
    Log.d(TAG, "onCallDisconnected callback from CallControlScope, cause: " + disconnectCause);

    if (!hasCallEnded) {
      hasCallEnded = true;
      callState.postValue(CallState.ENDED);
    }
  }

  // LiveData Getters

  public LiveData<CallState> getCallState() {
    return callState;
  }

  public LiveData<Boolean> getAudioEnabled() {
    return localAudioEnabled;
  }

  public LiveData<Boolean> getVideoEnabled() {
    return localVideoEnabled;
  }

  public LiveData<Boolean> getRemoteAudioEnabled() {
    return remoteAudioEnabled;
  }

  public LiveData<Boolean> getRemoteVideoEnabled() {
    return remoteVideoEnabled;
  }

  public LiveData<VideoTrack> getLocalVideoTrack() {
    return localVideoTrack;
  }

  public LiveData<VideoTrack> getRemoteVideoTrack() {
    return remoteVideoTrack;
  }

  public LiveData<String> getErrorMessage() {
    return errorMessage;
  }

  public LiveData<Boolean> getIsRelayUsed() {
    return isRelayUsed;
  }

  public LiveData<String> getDisplayName() {
    return displayName;
  }

  public LiveData<Icon> getDisplayIcon() {
    return displayIcon;
  }

  public LiveData<CallEndpointCompat> getCurrentAudioEndpoint() {
    return currentAudioEndpoint;
  }

  public LiveData<List<CallEndpointCompat>> getAvailableAudioEndpoints() {
    return availableAudioEndpoints;
  }


  // Notification Action Handlers

  public void handleNotificationAnswer() {
    Log.d(TAG, "handleNotificationAnswer");

    if (!callCoordinator.isIncomingCall()) {
      Log.w(TAG, "Not an incoming call");
      return;
    }

    callCoordinator.handleCallControlScopeAnswer();

    answerCallWhenReady();
  }

  public void handleNotificationDecline() {
    Log.d(TAG, "handleNotificationDecline");

    declineCall();
  }

  public void handleNotificationHangup() {
    Log.d(TAG, "handleNotificationHangup");

    hangUp();
  }

  // Cleanup

  @Override
  protected void onCleared() {
    super.onCleared();
    Log.d(TAG, "CallViewModel cleared");

    if (answerCallObserver != null) {
      callCoordinator.getLocalVideoTrack().removeObserver(answerCallObserver);
      answerCallObserver = null;
    }

    if (startOutgoingCallObserver != null) {
      callCoordinator.getLocalVideoTrack().removeObserver(startOutgoingCallObserver);
      startOutgoingCallObserver = null;
    }

    callCoordinator.clearActiveCallViewModel();
  }
}
