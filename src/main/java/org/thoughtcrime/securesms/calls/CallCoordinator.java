package org.thoughtcrime.securesms.calls;

import static org.thoughtcrime.securesms.calls.CallUtil.getIconFromChat;
import static org.thoughtcrime.securesms.calls.CallUtil.getNameFromChat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Person;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telecom.DisconnectCause;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.telecom.CallAttributesCompat;
import androidx.core.telecom.CallControlResult;
import androidx.core.telecom.CallControlScope;
import androidx.core.telecom.CallEndpointCompat;
import androidx.core.telecom.CallException;
import androidx.core.telecom.CallsManager;
import androidx.lifecycle.FlowLiveDataConversions;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.webrtc.PeerConnection;
import org.webrtc.VideoTrack;

import java.util.List;

import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.Dispatchers;

@RequiresApi(api = Build.VERSION_CODES.O)
public class CallCoordinator implements DcEventCenter.DcEventDelegate {
  private static final String TAG = CallCoordinator.class.getSimpleName();

  // Notification channels
  private static final String CHANNEL_ID_INCOMING = "voip_incoming_calls";
  private static final String CHANNEL_ID_ONGOING = "voip_ongoing_calls";
  private static final int NOTIFICATION_ID_CALL = 1001;

  private static final String CALL_IDENTIFIER_SCHEME = "deltachat:";

  private static CallCoordinator instance;
  private final Context appContext;
  private final CallsManager callsManager;
  private final NotificationManagerCompat notificationManager;
  private final Rpc rpc;
  private CoroutineScope audioFlowScope;

  // LiveData for Observable State
  private final MutableLiveData<PeerConnection.PeerConnectionState> connectionState =
    new MutableLiveData<>(PeerConnection.PeerConnectionState.NEW);
  private final MutableLiveData<VideoTrack> localVideoTrack = new MutableLiveData<>();
  private final MutableLiveData<VideoTrack> remoteVideoTrack = new MutableLiveData<>();
  private final MutableLiveData<Boolean> localAudioEnabled = new MutableLiveData<>(true);
  private final MutableLiveData<Boolean> localVideoEnabled = new MutableLiveData<>(true);
  private final MutableLiveData<Boolean> remoteAudioEnabled = new MutableLiveData<>(true);
  private final MutableLiveData<Boolean> remoteVideoEnabled = new MutableLiveData<>(true);
  private final MutableLiveData<Boolean> isRelayUsed = new MutableLiveData<>(false);
  private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
  private final MutableLiveData<String> displayName = new MutableLiveData<>();
  private final MutableLiveData<Icon> displayIcon = new MutableLiveData<>();

  // Audio Routing Support
  private final MediatorLiveData<CallEndpointCompat> currentAudioEndpoint = new MediatorLiveData<>();
  private final MediatorLiveData<List<CallEndpointCompat>> availableAudioEndpoints = new MediatorLiveData<>();
  private LiveData<CallEndpointCompat> currentAudioEndpointSource;
  private LiveData<List<CallEndpointCompat>> availableAudioEndpointsSource;

  private CallService callService;
  private ServiceConnection serviceConnection;
  private boolean isServiceBound = false;

  // Call metadata, single source of truth
  private Integer activeAccId;
  private Integer activeCallId;
  private Integer activeChatId;
  private boolean isIncomingCall;
  private boolean startsWithVideo;
  private String pendingOfferSdp;
  private boolean hasNotifiedBackend = false;

  private CallControlScope activeCallControlScope;
  private CallViewModel activeCallViewModel;

  private CallCoordinator(Context context) {
    this.appContext = context.getApplicationContext();
    this.rpc = DcHelper.getRpc(this.appContext);
    this.callsManager = new CallsManager(this.appContext);
    this.notificationManager = NotificationManagerCompat.from(this.appContext);

    addEventListeners();
    createNotificationChannels();
    registerTelecom();
    createServiceConnection();
  }

  public static synchronized CallCoordinator getInstance(Context context) {
    if (instance == null) {
      instance = new CallCoordinator(context);
    }
    return instance;
  }

  private void createNotificationChannels() {
    NotificationChannel incomingChannel = new NotificationChannel(
      CHANNEL_ID_INCOMING,
      "Incoming Calls",
      NotificationManager.IMPORTANCE_HIGH
    );
    incomingChannel.setDescription("Notifications for incoming DeltaChat calls");
    incomingChannel.setSound(null, null);

    NotificationChannel ongoingChannel = new NotificationChannel(
      CHANNEL_ID_ONGOING,
      "Active Calls",
      NotificationManager.IMPORTANCE_DEFAULT
    );
    ongoingChannel.setDescription("Notifications for active DeltaChat calls");
    ongoingChannel.setSound(null, null);

    notificationManager.createNotificationChannel(incomingChannel);
    notificationManager.createNotificationChannel(ongoingChannel);
  }

  private void registerTelecom() {
    try {
      int capabilities = CallsManager.CAPABILITY_BASELINE
        | CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING;
      callsManager.registerAppWithTelecom(capabilities);
      Log.d(TAG, "Successfully registered through Telecom");
    } catch (Exception e) {
      Log.e(TAG, "Failed to register with Telecom", e);
    }
  }

  private void addEventListeners() {
    DcEventCenter eventCenter = DcHelper.getEventCenter(this.appContext);
    eventCenter.removeObservers(this);

    eventCenter.addMultiAccountObserver(DcContext.DC_EVENT_INCOMING_CALL, this);
    eventCenter.addMultiAccountObserver(DcContext.DC_EVENT_INCOMING_CALL_ACCEPTED, this);
    eventCenter.addMultiAccountObserver(DcContext.DC_EVENT_OUTGOING_CALL_ACCEPTED, this);
    eventCenter.addMultiAccountObserver(DcContext.DC_EVENT_CALL_ENDED, this);
  }

  private void createServiceConnection() {
    serviceConnection = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName name, IBinder service) {
        CallService.LocalBinder binder = (CallService.LocalBinder) service;
        callService = binder.getService();
        Log.d(TAG, "Bound to CallService");

        if (!isIncomingCall) {

          // For outgoing call, show notification immediately
          String calleeName = displayName.getValue();
          if (calleeName == null) {
            calleeName = "Unknown";
          }

          showOrUpdateOngoingNotification("Calling " + calleeName + "...");
        }

        // Initialize call
        callService.initializeCall();

        if (isIncomingCall) {
          callService.startRingtone();
        }
      }

      @Override
      public void onServiceDisconnected(ComponentName name) {
        callService = null;
        isServiceBound = false;
        Log.d(TAG, "Unbound from CallService");
      }
    };
  }

  private void startAndBindService() {
    Intent serviceIntent = new Intent(this.appContext, CallService.class);
    this.appContext.startService(serviceIntent);

    // Set isServiceBound based on bindService return value
    isServiceBound = this.appContext.bindService(
      serviceIntent,
      serviceConnection,
      Context.BIND_AUTO_CREATE
    );

    Log.d(TAG, "Bind service result: " + isServiceBound);
  }

  private void stopAndUnbindService() {
    if (isServiceBound) {
      try {
        this.appContext.unbindService(serviceConnection);
        Log.d(TAG, "Service unbound successfully");
      } catch (IllegalArgumentException e) {
        Log.e(TAG, "Service was not registered or already unbound", e);
      } finally {
        isServiceBound = false;
        callService = null;
      }
    }

    Intent serviceIntent = new Intent(this.appContext, CallService.class);
    this.appContext.stopService(serviceIntent);
  }

  // LiveData Getters

  public LiveData<PeerConnection.PeerConnectionState> getConnectionState() {
    return connectionState;
  }

  public LiveData<VideoTrack> getLocalVideoTrack() {
    return localVideoTrack;
  }

  public LiveData<VideoTrack> getRemoteVideoTrack() {
    return remoteVideoTrack;
  }

  public LiveData<Boolean> getLocalAudioEnabled() {
    return localAudioEnabled;
  }

  public LiveData<Boolean> getLocalVideoEnabled() {
    return localVideoEnabled;
  }

  public LiveData<Boolean> getRemoteAudioEnabled() {
    return remoteAudioEnabled;
  }

  public LiveData<Boolean> getRemoteVideoEnabled() {
    return remoteVideoEnabled;
  }

  public LiveData<Boolean> getIsRelayUsed() {
    return isRelayUsed;
  }

  public LiveData<String> getErrorMessage() {
    return errorMessage;
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

  // State Update Methods (CallService)

  public void updateConnectionState(PeerConnection.PeerConnectionState state) {
    Log.d(TAG, "updateConnectionState: " + state);
    connectionState.postValue(state);

    // Handle connection failures
    if (state == PeerConnection.PeerConnectionState.FAILED ||
      state == PeerConnection.PeerConnectionState.CLOSED) {
      handleConnectionEnded(state);
    }
  }

  public void updateLocalVideoTrack(VideoTrack track) {
    Log.d(TAG, "updateLocalVideoTrack: " + (track != null));
    localVideoTrack.postValue(track);
  }

  public void updateRemoteVideoTrack(VideoTrack track) {
    Log.d(TAG, "updateRemoteVideoTrack: " + (track != null));
    remoteVideoTrack.postValue(track);
  }

  public void updateRemoteMutedState(boolean audioEnabled, boolean videoEnabled) {
    Log.d(TAG, "updateRemoteMutedState: audio=" + audioEnabled + ", video=" + videoEnabled);
    remoteAudioEnabled.postValue(audioEnabled);
    remoteVideoEnabled.postValue(videoEnabled);
  }

  public void updateRelayUsage(Boolean isRelay) {
    Log.d(TAG, "updateRelayUsage: " + isRelay);
    isRelayUsed.postValue(isRelay);
  }

  public void reportError(String error) {
    Log.e(TAG, "reportError: " + error);
    errorMessage.postValue(error);
  }

  // Delayed Media Initialization Support

  public void startMediaCapture() {
    Log.d(TAG, "startMediaCapture");
    if (callService != null) {
      callService.startMediaCapture();
    } else {
      Log.w(TAG, "Cannot start media capture, service not ready");
    }
  }

  public void handleCallControlScopeAnswer() {
    Log.d(TAG, "handleCallControlScopeAnswer");

    if (!isIncomingCall) {
      Log.w(TAG, "Not an incoming call");
      return;
    }

    if (callService != null) {
      callService.stopRingtone();
    }

    // Notify Android system with CallControlScope
    CallControlScope scope = activeCallControlScope;
    if (scope != null) {
      scope.answer(
        CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
        new Continuation<CallControlResult>() {
          @NonNull
          @Override
          public CoroutineContext getContext() {
            return EmptyCoroutineContext.INSTANCE;
          }

          @Override
          public void resumeWith(@NonNull Object result) {
            if (result instanceof CallControlResult) {
              Log.d(TAG, "Answer succeeded with CallControlScope");
            } else if (result instanceof kotlin.Result.Failure) {
              Log.e(TAG, "Answer failed",
                ((kotlin.Result.Failure) result).exception);
              reportError("Failed to answer call");
            }
          }
        }
      );
    }

    notificationManager.cancel(NOTIFICATION_ID_CALL);
  }

  public void answerWebRTC() {
    Log.d(TAG, "answerWebRTC");

    if (!isIncomingCall) {
      Log.w(TAG, "Not an incoming call");
      return;
    }

    if (pendingOfferSdp == null) {
      Log.e(TAG, "No pending offer SDP");
      reportError("Call data missing");
      return;
    }

    // Tell service to process offer and create answer
    if (callService != null) {
      callService.answerIncomingCall();
    } else {
      Log.e(TAG, "Cannot answer, service not ready");
      reportError("Service not ready");
    }
  }

  public String getPendingOfferSdp() {
    return pendingOfferSdp;
  }

  public void clearPendingOfferSdp() {
    pendingOfferSdp = null;
  }

  // RPC Signaling (CallService)

  public void handleOfferReady(String offerSdp) {
    Log.d(TAG, "Offer SDP ready, sending via RPC");

    new Thread(() -> {
      try {
        // RPC returns the final callId
        int callId = rpc.placeOutgoingCall(activeAccId, activeChatId, offerSdp, startsWithVideo);

        Log.d(TAG, "Outgoing call initiated, final callId: " + callId);

        // Update our stored callId
        this.activeCallId = callId;

        completeOutgoingCall(activeAccId, callId, activeChatId);

      } catch (RpcException e) {
        Log.e(TAG, "Failed to send offer with RPC", e);
        reportError("Failed to initiate call: " + e.getMessage());
      }
    }).start();
  }

  public void handleAnswerReady(String answerSdp) {
    Log.d(TAG, "handleAnswerReady, sending via RPC");

    new Thread(() -> {
      try {
        rpc.acceptIncomingCall(activeAccId, activeCallId, answerSdp);
        Log.d(TAG, "Answer sent successfully");

      } catch (RpcException e) {
        Log.e(TAG, "Failed to send answer with RPC", e);
        reportError("Failed to answer call: " + e.getMessage());
      }
    }).start();
  }

  // Call Control Methods (CallViewModel)

  public void answerCall() {
    Log.d(TAG, "answerCall called");

    if (!isIncomingCall) {
      Log.w(TAG, "answerCall() called but this is not an incoming call");
      return;
    }

    handleCallControlScopeAnswer();
  }

  public void declineCall() {
    Log.d(TAG, "declineCall called");

    if (activeCallId == null) {
      Log.w(TAG, "Call already ended or no active call");
      return;
    }

    if (callService != null) {
      callService.stopRingtone();
    }

    notifyBackendCallEnded();

    // Disconnect with CallControlScope
    CallControlScope scope = activeCallControlScope;
    if (scope != null) {
      scope.disconnect(
        new DisconnectCause(DisconnectCause.REJECTED),
        new Continuation<CallControlResult>() {
          @NonNull
          @Override
          public CoroutineContext getContext() {
            return EmptyCoroutineContext.INSTANCE;
          }

          @Override
          public void resumeWith(@NonNull Object result) {
            if (result instanceof CallControlResult) {
              Log.d(TAG, "Decline succeeded with CallControlScope");
            } else if (result instanceof kotlin.Result.Failure) {
              Log.e(TAG, "Decline failed",
                ((kotlin.Result.Failure) result).exception);
            }
          }
        }
      );
    }

    // End call on service
    if (callService != null) {
      callService.endCall();
    }

    // Cleanup
    cleanupCall(activeAccId, activeCallId);
  }

  public void hangUp() {
    Log.d(TAG, "hangUp called");

    if (activeCallId == null) {
      Log.w(TAG, "Call already ended or no active call");
      return;
    }

    notifyBackendCallEnded();

    // Disconnect with CallControlScope
    CallControlScope scope = activeCallControlScope;
    if (scope != null) {
      scope.disconnect(
        new DisconnectCause(DisconnectCause.LOCAL),
        new Continuation<CallControlResult>() {
          @NonNull
          @Override
          public CoroutineContext getContext() {
            return EmptyCoroutineContext.INSTANCE;
          }

          @Override
          public void resumeWith(@NonNull Object result) {
            if (result instanceof CallControlResult) {
              Log.d(TAG, "Hang up succeeded with CallControlScope");
            } else if (result instanceof kotlin.Result.Failure) {
              Log.e(TAG, "Hang up failed",
                ((kotlin.Result.Failure) result).exception);
            }
          }
        }
      );
    }

    // End call on service
    if (callService != null) {
      callService.endCall();
    }

    // Cleanup
    cleanupCall(activeAccId, activeCallId);
  }

  public void setAudioEnabled(boolean enabled) {
    Log.d(TAG, "setAudioEnabled: " + enabled);

    localAudioEnabled.postValue(enabled);

    if (callService != null) {
      callService.setAudioEnabled(enabled);

      callService.sendMutedState(
        enabled,
        Boolean.TRUE.equals(localVideoEnabled.getValue())
      );
    }
  }

  public void setVideoEnabled(boolean enabled) {
    Log.d(TAG, "setVideoEnabled: " + enabled);

    localVideoEnabled.postValue(enabled);

    if (callService != null) {
      callService.setVideoEnabled(enabled);

      callService.sendMutedState(
        Boolean.TRUE.equals(localAudioEnabled.getValue()),
        enabled
      );
    }
  }

  public void switchCamera() {
    Log.d(TAG, "switchCamera");
    if (callService != null) {
      callService.switchCamera();
    }
  }

  public void startOutgoingCall() {
    Log.d(TAG, "startOutgoingCall");
    if (callService != null) {
      callService.startOutgoingCall();
    }
  }

  // Setup Audio Endpoint Flow Collection

  private void setupAudioEndpointCollection(CallControlScope scope) {
    Log.d(TAG, "Setting up audio endpoint flow collection");

    // Create CoroutineScope for Flow collection
    audioFlowScope = CoroutineScopeKt.CoroutineScope(
      Dispatchers.getMain()
    );

    // Create LiveData sources from Flows
    currentAudioEndpointSource = FlowLiveDataConversions.asLiveData(
      scope.getCurrentCallEndpoint(),
      audioFlowScope.getCoroutineContext(),
      5000L
    );

    availableAudioEndpointsSource = FlowLiveDataConversions.asLiveData(
      scope.getAvailableEndpoints(),
      audioFlowScope.getCoroutineContext(),
      5000L
    );

    // Add sources to MediatorLiveData
    currentAudioEndpoint.addSource(currentAudioEndpointSource, value -> {
      Log.d(TAG, "Current audio endpoint changed: " +
        (value != null ? value.getName() : "null"));
      currentAudioEndpoint.setValue(value);
    });

    availableAudioEndpoints.addSource(availableAudioEndpointsSource, value -> {
      Log.d(TAG, "Available audio endpoints changed, count: " +
        (value != null ? value.size() : 0));
      availableAudioEndpoints.setValue(value);
    });
  }

  // Request Audio Endpoint Change
  public void requestAudioEndpointChange(CallEndpointCompat endpoint) {
    Log.d(TAG, "Requesting audio endpoint change to: " + endpoint.getName());

    if (activeCallControlScope == null) {
      Log.w(TAG, "No active call scope, cannot change endpoint");
      return;
    }

    activeCallControlScope.requestEndpointChange(
      endpoint,
      new Continuation<CallControlResult>() {
        @NonNull
        @Override
        public CoroutineContext getContext() {
          return EmptyCoroutineContext.INSTANCE;
        }

        @Override
        public void resumeWith(@NonNull Object result) {
          if (result instanceof CallControlResult.Success) {
            Log.d(TAG, "Audio endpoint change succeeded");
          } else if (result instanceof CallControlResult.Error) {
            int errorCode = ((CallControlResult.Error) result).getErrorCode();
            Log.e(TAG, "Audio endpoint change failed with error code: " + errorCode);
            reportError("Failed to change audio device");
          }
        }
      }
    );
  }

  // Helper (CallService)

  public String fetchIceServers() throws RpcException {
    if (activeAccId == null) {
      throw new RpcException("No active account");
    }
    return rpc.iceServers(activeAccId);
  }


  @Override
  public void handleEvent(@NonNull DcEvent event) {
    int eventId = event.getId();
    int accId = event.getAccountId();
    int callId = event.getData1Int();   // This is not semantically correct but shall work fine

    // It seems this observer can fire on either main or background thread
    // Always move to background
    new Thread(() -> {
      boolean hasVideo;

      switch (eventId) {
        case DcContext.DC_EVENT_INCOMING_CALL:
          try {
            hasVideo = this.rpc.callInfo(accId, callId).hasVideo;
          } catch (RpcException e) {
            Log.e(TAG, "Rpc.callInfo() failed", e);
            hasVideo = false;
          }
          onIncomingCall(accId, callId, event.getData2Str(), hasVideo);
          break;
        case DcContext.DC_EVENT_INCOMING_CALL_ACCEPTED:
          onIncomingCallAccepted(callId);
          break;
        case DcContext.DC_EVENT_OUTGOING_CALL_ACCEPTED:
          String answerSDP = event.getData2Str();
          onOutgoingCallAccepted(callId, answerSDP);
          break;
        case DcContext.DC_EVENT_CALL_ENDED:
          // This event is problematic because it can trigger in both directions,
          // in addition to multiple other scenarios which cannot easily be distinguished
          // May cause problems in edge cases
          onCallEnded(accId, callId);
          break;
      }
    }).start();
  }

  private void onIncomingCall(int accId, int callId, String offerSdp, boolean startsWithVideo) {
    Log.d(TAG, "onIncomingCall: accId=" + accId + ", callId=" + callId);

    if (activeCallId != null) {
      Log.w(TAG, "Already have an active call, ignoring incoming call");
      return;
    }

    resetLiveDataForNewCall();

    this.activeAccId = accId;
    this.activeCallId = callId;
    this.isIncomingCall = true;
    this.startsWithVideo = startsWithVideo;
    this.pendingOfferSdp = offerSdp;

    // Get caller info
    DcContext dcContext = ApplicationContext.getDcAccounts().getAccount(accId);
    int chatId = dcContext.getMsg(callId).getChatId();
    this.activeChatId = chatId;
    DcChat dcChat = dcContext.getChat(chatId);
    String callerName = getNameFromChat(dcChat);
    Icon callerIcon = getIconFromChat(this.appContext, dcChat);

    displayName.postValue(callerName);
    displayIcon.postValue(callerIcon);

    // Add to CallsManager
    CallAttributesCompat callAttributes = createCallAttributes(
      callerName, callId, true);
    addCallToTelecom(callAttributes, callerName, callerIcon);

    // Show CallStyle notification
    showIncomingCallNotification(callerName, callerIcon);

    startAndBindService();
  }

  private void onIncomingCallAccepted(int callId) {
    Log.d(TAG, "onIncomingCallAccepted: callId=" + callId);

    if (activeCallId == null || !activeCallId.equals(callId)) {
      Log.w(TAG, "Accepted call ID doesn't match active call");
      return;
    }

    String callerName = displayName.getValue();
    if (callerName == null) {
      callerName = "Unknown";
    }

    showOrUpdateOngoingNotification("Call with " + callerName);
  }

  private void onOutgoingCallAccepted(int callId, String answerSdp) {
    Log.d(TAG, "onOutgoingCallAccepted: callId=" + callId + ", got answer SDP");

    if (activeCallId == null || !activeCallId.equals(callId)) {
      Log.w(TAG, "Answered call ID doesn't match active call");
      return;
    }

    if (callService != null) {
      callService.handleAnswerSdp(answerSdp);
    }

    // Call control scope should transition to ACTIVE
    if (activeCallControlScope != null) {
      activeCallControlScope.setActive(new Continuation<CallControlResult>() {
        @NonNull
        @Override
        public CoroutineContext getContext() {
          return EmptyCoroutineContext.INSTANCE;
        }

        @Override
        public void resumeWith(@NonNull Object result) {
          if (result instanceof CallControlResult) {
            Log.d(TAG, "Outgoing call set to active: " + result);
          } else if (result instanceof kotlin.Result.Failure) {
            Log.e(TAG, "Failed to set active", ((kotlin.Result.Failure) result).exception);
          }
        }
      });
    }

    String calleeName = displayName.getValue();
    if (calleeName == null) {
      calleeName = "Unknown";
    }
    showOrUpdateOngoingNotification("Call with " + calleeName);
  }

  private void onCallEnded(int accId, int callId) {
    Log.d(TAG, "onCallEnded: accId=" + accId + ", callId=" + callId);

    if (callService != null) {
      callService.stopRingtone();
    }

    // Disconnect from CallControlScope
    if (activeCallControlScope != null) {
      activeCallControlScope.disconnect(
        // We actually don't know if this is incoming or outgoing
        // But we have to provide one of LOCAL, REMOTE, MISSED, REJECTED
        new DisconnectCause(DisconnectCause.REMOTE),
        new Continuation<CallControlResult>() {
          @NonNull
          @Override
          public CoroutineContext getContext() {
            return EmptyCoroutineContext.INSTANCE;
          }

          @Override
          public void resumeWith(@NonNull Object result) {
            Log.d(TAG, "Disconnect completed");
          }
        }
      );
    }

    if (callService != null) {
      callService.endCall();
    }

    // Clear active states
    cleanupCall(accId, callId);
  }

  private void handleConnectionEnded(PeerConnection.PeerConnectionState state) {
    Log.d(TAG, "handleConnectionEnded: " + state);

    if (activeCallId == null) {
      Log.w(TAG, "Call already ended or no active call");
      return;
    }

    notifyBackendCallEnded();

    // Cleanup
    if (activeAccId != null && activeCallId != null) {
      cleanupCall(activeAccId, activeCallId);
    }
  }

  /**
   * Cleanup call state, used for clean up not initialized from backend events
   */
  public void cleanupCall(int accId, int callId) {
    Log.d(TAG, "cleanupCall: accId=" + accId + ", callId=" + callId);

    if (activeCallId != null && !activeCallId.equals(callId) ||
      activeAccId != null && !activeAccId.equals(accId)) {
      Log.w(TAG, "Cleanup accountId or callId doesn't match active call");
      // Clean up anyway. Otherwise, no new calls can happen.
    }

    if (currentAudioEndpointSource != null) {
      currentAudioEndpoint.removeSource(currentAudioEndpointSource);
      currentAudioEndpointSource = null;
      Log.d(TAG, "Removed current audio endpoint source");
    }

    if (availableAudioEndpointsSource != null) {
      availableAudioEndpoints.removeSource(availableAudioEndpointsSource);
      availableAudioEndpointsSource = null;
      Log.d(TAG, "Removed available audio endpoints source");
    }

    if (audioFlowScope != null) {
      CoroutineScopeKt.cancel(audioFlowScope, null);
      audioFlowScope = null;
      Log.d(TAG, "Cancelled audio flow scope");
    }

    // Clear notifications
    notificationManager.cancel(NOTIFICATION_ID_CALL);

    // Stop service
    stopAndUnbindService();

    // Clear state
    this.activeAccId = null;
    this.activeCallId = null;
    this.activeChatId = null;
    this.activeCallControlScope = null;
    this.activeCallViewModel = null;
    this.isIncomingCall = false;
    this.startsWithVideo = false;
    this.pendingOfferSdp = null;
    this.hasNotifiedBackend = false;

    // Clear LiveData
    connectionState.postValue(PeerConnection.PeerConnectionState.CLOSED);
    clearLiveData();

    Log.d(TAG, "Call cleanup complete");
  }

  private void notifyBackendCallEnded() {
    if (hasNotifiedBackend) {
      Log.d(TAG, "Backend already notified of call end");
      return;
    }

    if (activeCallId == null || activeCallId < 0) {
      Log.w(TAG, "Cannot notify backend, invalid callId");
      return;
    }

    hasNotifiedBackend = true;

    new Thread(() -> {
      try {
        rpc.endCall(activeAccId, activeCallId);
        Log.d(TAG, "Backend notified: call ended");
      } catch (RpcException e) {
        Log.e(TAG, "Failed to notify backend of call end", e);
      }
    }).start();
  }

  private void resetLiveDataForNewCall() {
    connectionState.postValue(PeerConnection.PeerConnectionState.NEW);
    clearLiveData();
  }

  private void clearLiveData() {
    localVideoTrack.postValue(null);
    remoteVideoTrack.postValue(null);
    localAudioEnabled.postValue(true);
    localVideoEnabled.postValue(true);
    remoteAudioEnabled.postValue(true);
    remoteVideoEnabled.postValue(true);
    isRelayUsed.postValue(false);
    errorMessage.postValue(null);
    displayName.postValue(null);
    displayIcon.postValue(null);
  }

  public void initiateOutgoingCall(int accId, int chatId, boolean startsWithVideo) {
    Log.d(TAG, "Initiating outgoing call:accId=" + accId + ", chatId=" + chatId);

    if (activeCallId != null) {
      Log.w(TAG, "Already have an active call, cannot start new one");
      return;
    }

    resetLiveDataForNewCall();

    this.activeCallId = -1;  // Placeholder call ID for Intent
    this.activeAccId = accId;
    this.activeChatId = chatId;
    this.isIncomingCall = false;
    this.startsWithVideo = startsWithVideo;
    this.pendingOfferSdp = null;

    // Get callee info
    DcContext dcContext = ApplicationContext.getDcAccounts().getAccount(accId);
    DcChat dcChat = dcContext.getChat(chatId);
    String calleeName = getNameFromChat(dcChat);
    Icon calleeIcon = getIconFromChat(this.appContext, dcChat);

    displayName.postValue(calleeName);
    displayIcon.postValue(calleeIcon);

    startAndBindService();

    launchCallActivity();
  }

  public void completeOutgoingCall(int accId, int callId, int chatId) {
    Log.d(TAG, "Completing outgoing call with accId=" + accId + ", callId=" + callId);

    if (activeAccId == null || activeCallId == null || activeChatId == null) {
      Log.w(TAG, "No active call, cannot complete setting up outgoing call");
      return;
    }

    if (!activeChatId.equals(chatId) || !activeAccId.equals(accId)) {
      Log.w(TAG, "Cannot complete outgoing call,mismatch in call parameters");
      return;
    }

    this.activeCallId = callId;

    // Get callee info
    String calleeName = displayName.getValue();
    Icon calleeIcon = displayIcon.getValue();

    // Create call attributes
    CallAttributesCompat callAttributes = createCallAttributes(
      calleeName, activeCallId, false
    );

    // Add call to CallsManager
    addCallToTelecom(callAttributes, calleeName, calleeIcon);
  }

  private void addCallToTelecom(CallAttributesCompat callAttributes,
                                String displayName, Icon icon) {
    try {
      callsManager.addCall(
        callAttributes,
        // onAnswer
        (callType, continuation) -> {
          Log.d(TAG, "CallControlScope: onAnswer with type: " + callType);
          if (activeCallViewModel != null && isIncomingCall) {
            activeCallViewModel.onCallAnswered();
          }
          return Unit.INSTANCE;
        },
        // onDisconnect
        (disconnectCause, continuation) -> {
          Log.d(TAG, "CallControlScope: onDisconnect, cause: " + disconnectCause);
          if (activeCallViewModel != null) {
            activeCallViewModel.onCallDisconnected(disconnectCause);
          }
          return Unit.INSTANCE;
        },
        // onSetActive
        continuation -> {
          Log.d(TAG, "CallControlScope: onSetActive");
          if (activeCallViewModel != null) {
            activeCallViewModel.onCallActive();
          }
          return Unit.INSTANCE;
        },
        // onSetInactive
        continuation -> {
          Log.d(TAG, "CallControlScope: onSetInactive");
          if (activeCallViewModel != null) {
            activeCallViewModel.onCallInactive();
          }
          return Unit.INSTANCE;
        },
        // CallControlScope lambda
        scope -> {
          Log.d(TAG, "CallControlScope initialized");
          activeCallControlScope = scope;

          new Handler(Looper.getMainLooper()).post(() -> {
            setupAudioEndpointCollection(scope);
          });

          return Unit.INSTANCE;
        },
        new Continuation<Unit>() {
          @NonNull
          @Override
          public CoroutineContext getContext() {
            return EmptyCoroutineContext.INSTANCE;
          }

          @Override
          public void resumeWith(@NonNull Object result) {
            Log.d(TAG, "addCall completed");
          }
        }
      );
    } catch (CallException e) {
      Log.e(TAG, "Failed to add call to Telecom", e);
    }
  }

  private CallAttributesCompat createCallAttributes(String callerName,
                                                    int callId,
                                                    boolean isIncomingCall) {
    Uri addressUri = Uri.parse(CALL_IDENTIFIER_SCHEME + callId);

    return new CallAttributesCompat(
      callerName,
      addressUri,
      isIncomingCall ?
        CallAttributesCompat.DIRECTION_INCOMING :
        CallAttributesCompat.DIRECTION_OUTGOING,
      CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
      CallAttributesCompat.SUPPORTS_SET_INACTIVE,
      null
    );
  }

  private void showIncomingCallNotification(String callerName, Icon callerIcon) {
    // Answer intent
    Intent answerIntent = new Intent(this.appContext, CallActivity.class);
    answerIntent.setAction(CallActivity.ACTION_ANSWER_CALL);
    answerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

    PendingIntent answerPendingIntent = PendingIntent.getActivity(
      this.appContext, 0, answerIntent,
      PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );

    // Decline intent
    Intent declineIntent = new Intent(this.appContext, CallActivity.class);
    declineIntent.setAction(CallActivity.ACTION_DECLINE_CALL);
    declineIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

    PendingIntent declinePendingIntent = PendingIntent.getActivity(
      this.appContext, 1, declineIntent,
      PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );

    // Full screen intent
    Intent fullScreenIntent = new Intent(this.appContext, CallActivity.class);
    fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

    PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
      this.appContext, 2, fullScreenIntent,
      PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );

    Notification.Builder builder;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      // Android 12+, CallStyle
      Person caller = new Person.Builder()
        .setName(callerName)
        .setIcon(callerIcon)
        .setImportant(true)
        .build();

      // TODO: Add Permission Check
      builder = new Notification.Builder(this.appContext, CHANNEL_ID_INCOMING)
        .setSmallIcon(R.drawable.icon_notification)
        .setContentTitle("Incoming call")
        .setContentText("Incoming call from " + callerName)
        .setStyle(Notification.CallStyle.forIncomingCall(
          caller, declinePendingIntent, answerPendingIntent))
        .addPerson(caller)
        .setFullScreenIntent(fullScreenPendingIntent, true)
        .setOngoing(true)
        .setCategory(Notification.CATEGORY_CALL);
    } else {
      // Android 8-12: Notification with actions
      builder = new Notification.Builder(this.appContext, CHANNEL_ID_INCOMING)
        .setSmallIcon(R.drawable.icon_notification)
        .setContentTitle("Incoming call")
        .setContentText("Incoming call from " + callerName)
        .setFullScreenIntent(fullScreenPendingIntent, true)
        .setOngoing(true)
        .setColorized(true)
        .setCategory(Notification.CATEGORY_CALL)
        .addAction(new Notification.Action.Builder(
          Icon.createWithResource(this.appContext, R.drawable.baseline_call_end_24),
          "Decline", declinePendingIntent).build())
        .addAction(new Notification.Action.Builder(
          Icon.createWithResource(this.appContext, R.drawable.baseline_call_24),
          "Answer", answerPendingIntent).build());
    }

    notificationManager.notify(NOTIFICATION_ID_CALL, builder.build());
  }

  private Notification buildOngoingCallNotification(String statusText, String displayName, Icon icon) {
    Intent activityIntent = new Intent(this.appContext, CallActivity.class);
    activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

    Intent hangupIntent = new Intent(this.appContext, CallActivity.class);
    hangupIntent.setAction(CallActivity.ACTION_HANGUP_CALL);
    hangupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

    PendingIntent hangupPendingIntent = PendingIntent.getActivity(
      this.appContext, 3, hangupIntent,
      PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );

    PendingIntent contentIntent = PendingIntent.getActivity(
      this.appContext, 4, activityIntent,
      PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );

    Notification.Builder builder;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      Person person = new Person.Builder()
        .setName(displayName)
        .setIcon(icon)
        .setImportant(true)
        .build();

      builder = new Notification.Builder(this.appContext, CHANNEL_ID_ONGOING)
        .setSmallIcon(R.drawable.icon_notification)
        .setContentTitle("Ongoing call")
        .setContentText(statusText)
        .setContentIntent(contentIntent)
        .setStyle(Notification.CallStyle.forOngoingCall(person, hangupPendingIntent))
        .addPerson(person)
        .setOngoing(true)
        .setCategory(Notification.CATEGORY_CALL);
    } else {
      builder = new Notification.Builder(this.appContext, CHANNEL_ID_ONGOING)
        .setSmallIcon(R.drawable.icon_notification)
        .setContentTitle("Ongoing call")
        .setContentText(statusText)
        .setContentIntent(contentIntent)
        .setOngoing(true)
        .setColorized(true)
        .setCategory(Notification.CATEGORY_CALL)
        .addAction(new Notification.Action.Builder(
          Icon.createWithResource(this.appContext, R.drawable.baseline_call_end_24),
          "Hang up", hangupPendingIntent).build());
    }

    return builder.build();
  }

  private void showOrUpdateOngoingNotification(String statusText) {
    if (callService == null) {
      Log.w(TAG, "Cannot show notification, service not ready");
      return;
    }

    String name = displayName.getValue();
    if (name == null) {
      name = "Unknown";
    }

    Icon icon = displayIcon.getValue();

    Notification notification = buildOngoingCallNotification(statusText, name, icon);
    callService.startForegroundWithNotification(NOTIFICATION_ID_CALL, notification);

    Log.d(TAG, "Ongoing notification shown/updated: " + statusText);
  }

  private void launchCallActivity() {
    Intent intent = new Intent(this.appContext, CallActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
      Intent.FLAG_ACTIVITY_SINGLE_TOP);
    this.appContext.startActivity(intent);

    Log.d(TAG, "Launching CallActivity, hasActiveViewModel: " + (activeCallViewModel != null));
  }

  public void setActiveCallViewModel(CallViewModel viewModel) {
    this.activeCallViewModel = viewModel;
  }

  public void clearActiveCallViewModel() {
    this.activeCallViewModel = null;
  }

  public CallControlScope getActiveCallControlScope() {
    return activeCallControlScope;
  }

  public CallService getCallService() {
    return callService;
  }

  public boolean hasActiveCall() {
    return activeCallId != null;
  }

  public boolean isIncomingCall() {
    return isIncomingCall;
  }

  public boolean isStartsWithVideo() {
    return startsWithVideo;
  }

  public void setStartsWithVideo(boolean startsWithVideo) {
    this.startsWithVideo = startsWithVideo;
  }
}
