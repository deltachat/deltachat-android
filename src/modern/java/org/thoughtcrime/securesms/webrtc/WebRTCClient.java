package org.thoughtcrime.securesms.webrtc;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.thoughtcrime.securesms.EglUtils;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RTCStats;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Manages WebRTC PeerConnection.
 * Mirrors TypeScript CallsManager.
 */
public class WebRTCClient {

  private static final String TAG = WebRTCClient.class.getSimpleName();

  private static final int DC_ID_ICE_TRICKLING = 1;
  private static final int DC_ID_MUTED_STATE = 3;

  // ICE gathering timeouts
  private static final int RELAY_WAIT_MS = 10000;
  private static final int SRFLX_WAIT_MS = 5000;
  private static final int SRFLX_BURST_DELAY_MS = 150;
  private static final int HOST_WAIT_MS = 3000;

  private final Context context;
  private final Handler mainHandler;
  private PeerConnectionFactory peerConnectionFactory;
  private PeerConnection peerConnection;
  private List<PeerConnection.IceServer> iceServers;

  private DataChannel iceTricklingDataChannel;
  private DataChannel mutedStateDataChannel;

  // ICE candidate
  private final List<IceCandidate> iceCandidateBuffer;
  private boolean iceTricklingChannelOpen;
  private boolean mutedStateChannelOpen;
  private boolean enableIceTrickling;
  private boolean isEnded = false;

  // ICE gathering
  private volatile boolean isIceGatheringComplete;
  private volatile boolean hasRelayCandidate;
  private volatile boolean hasSrflxCandidate;
  private volatile boolean hasHostCandidate;
  private CountDownLatch iceGatheringLatch;
  private CountDownLatch relayCandidateLatch;
  private CountDownLatch srflxCandidateLatch;

  // Media
  private MediaStream localStream;
  private VideoTrack localVideoTrack;
  private AudioTrack localAudioTrack;
  private VideoTrack remoteVideoTrack;
  private AudioTrack remoteAudioTrack;

  // Callbacks to ViewModel
  private final Callbacks callbacks;

  /**
   * Callbacks to ViewModel of connection events
   */
  public interface Callbacks {
    void onOfferReady(String offerSdp);
    void onAnswerReady(String answerSdp);
    void onRemoteVideoTrack(VideoTrack videoTrack);
    void onRemoteAudioTrack(AudioTrack audioTrack);
    void onConnectionStateChanged(PeerConnection.PeerConnectionState state);
    void onRemoteMutedStateChanged(boolean audioEnabled, boolean videoEnabled);
    void onRelayUsageChanged(Boolean isRelayUsed);
    void onError(String error);
  }

  public WebRTCClient(@NonNull Context context, Callbacks callbacks) {
    this.context = context.getApplicationContext();
    this.callbacks = callbacks;
    this.mainHandler = new Handler(Looper.getMainLooper());
    this.iceCandidateBuffer = new ArrayList<>();
    this.iceServers = new ArrayList<>();
    this.iceTricklingChannelOpen = false;
    this.mutedStateChannelOpen = false;
    this.enableIceTrickling = false;

    initializePeerConnectionFactory();
  }

  private void initializePeerConnectionFactory() {
    PeerConnectionFactory.InitializationOptions initOptions =
      PeerConnectionFactory.InitializationOptions.builder(context)
        .setEnableInternalTracer(false)
        .setFieldTrials("")
        .createInitializationOptions();
    PeerConnectionFactory.initialize(initOptions);

    EglBase.Context eglBaseContext = EglUtils.getEglBase().getEglBaseContext();

    // Create video encoder/decoder factories
    VideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(
      eglBaseContext,
      true,  // enableIntelVp8Encoder
      true   // enableH264HighProfile
    );

    VideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(eglBaseContext);

    PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
    options.disableEncryption = false;
    options.disableNetworkMonitor = false;

    peerConnectionFactory = PeerConnectionFactory.builder()
      .setOptions(options)
      .setVideoEncoderFactory(encoderFactory)
      .setVideoDecoderFactory(decoderFactory)
      .createPeerConnectionFactory();

    Log.d(TAG, "PeerConnectionFactory initialized");
  }

  /**
   * Expected JSON format:
   * [
   *   {"urls": "stun:stun.example.com:3478"},
   *   {"urls": "turn:turn.example.com", "username": "user", "credential": "pass"}
   * ]
   */
  public void configure(String iceServersJson) {
    this.iceServers = parseIceServers(iceServersJson);

    if (this.iceServers.isEmpty()) {
      Log.w(TAG, "No ICE servers configured, will use host candidates only");
    } else {
      Log.d(TAG, "Configured " + iceServers.size() + " ICE servers");
    }
  }

  private List<PeerConnection.IceServer> parseIceServers(String json) {
    List<PeerConnection.IceServer> servers = new ArrayList<>();

    if (json == null || json.trim().isEmpty()) {
      return servers;
    }

    try {
      JSONArray array = new JSONArray(json);
      for (int i = 0; i < array.length(); i++) {
        JSONObject serverObj = array.getJSONObject(i);

        // Parse URLs string or array
        List<String> urls = new ArrayList<>();
        Object urlsObj = serverObj.get("urls");
        if (urlsObj instanceof String) {
          urls.add((String) urlsObj);
        } else if (urlsObj instanceof JSONArray) {
          JSONArray urlsArray = (JSONArray) urlsObj;
          for (int j = 0; j < urlsArray.length(); j++) {
            urls.add(urlsArray.getString(j));
          }
        }

        PeerConnection.IceServer.Builder builder =
          PeerConnection.IceServer.builder(urls);

        if (serverObj.has("username")) {
          builder.setUsername(serverObj.getString("username"));
        }
        if (serverObj.has("credential")) {
          builder.setPassword(serverObj.getString("credential"));
        }

        servers.add(builder.createIceServer());
      }
    } catch (JSONException e) {
      Log.e(TAG, "Failed to parse ICE servers JSON", e);
    }

    return servers;
  }

  /**
   * Set local media stream
   */
  public void setLocalMediaStream(MediaStream stream) {
    this.localStream = stream;

    if (!stream.audioTracks.isEmpty()) {
      this.localAudioTrack = stream.audioTracks.get(0);
    }
    if (!stream.videoTracks.isEmpty()) {
      this.localVideoTrack = stream.videoTracks.get(0);
    }

    Log.d(TAG, "Local media stream set: audio: " + (localAudioTrack != null)
      + ", video: " + (localVideoTrack != null));
  }

  public boolean hasLocalMediaStream() {
    return localStream != null;
  }

  private void createPeerConnection() {
    PeerConnection.RTCConfiguration rtcConfig =
      new PeerConnection.RTCConfiguration(iceServers);
    rtcConfig.iceTransportsType = PeerConnection.IceTransportsType.ALL;
    rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
    rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
    rtcConfig.iceCandidatePoolSize = 1;
    rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

    peerConnection = peerConnectionFactory.createPeerConnection(
      rtcConfig,
      new PeerConnectionObserver()
    );

    if (peerConnection == null) {
      callbacks.onError("Failed to create PeerConnection");
      return;
    }

    // Reset ICE gathering state
    this.isIceGatheringComplete = false;
    this.hasRelayCandidate = false;
    this.hasSrflxCandidate = false;
    this.hasHostCandidate = false;
    this.iceGatheringLatch = new CountDownLatch(1);
    this.relayCandidateLatch = new CountDownLatch(1);
    this.srflxCandidateLatch = new CountDownLatch(1);
    this.enableIceTrickling = false;

    // Create data channels
    setupIceTricklingDataChannel();
    setupMutedStateDataChannel();

    Log.d(TAG, "PeerConnection created");
  }

  private class PeerConnectionObserver implements PeerConnection.Observer {
    @Override
    public void onSignalingChange(PeerConnection.SignalingState state) {
      Log.d(TAG, "Signaling state: " + state);
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
      Log.d(TAG, "ICE connection state: " + state);
    }

    @Override
    public void onConnectionChange(PeerConnection.PeerConnectionState state) {
      Log.d(TAG, "Connection state: " + state);

      mainHandler.post(() -> callbacks.onConnectionStateChanged(state));

      if (state == PeerConnection.PeerConnectionState.CONNECTED) {
        detectRelayUsage();
      }
    }

    @Override
    public void onIceConnectionReceivingChange(boolean receiving) {
      Log.d(TAG, "ICE connection receiving: " + receiving);
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState state) {
      Log.d(TAG, "ICE gathering state: " + state);
      if (state == PeerConnection.IceGatheringState.COMPLETE) {
        isIceGatheringComplete = true;
        iceGatheringLatch.countDown();
      }
    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {
      onIceCandidateGathered(candidate);
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates) {
      Log.d(TAG, "ICE candidates removed, current length: " + candidates.length);
    }

    @Override
    public void onAddStream(MediaStream stream) {
      Log.d(TAG, "onAddStream: " + stream.getId());
    }

    @Override
    public void onRemoveStream(MediaStream stream) {
      Log.d(TAG, "onRemoveStream: " + stream.getId());
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
      Log.d(TAG, "onDataChannel: " + dataChannel.label());
    }

    @Override
    public void onRenegotiationNeeded() {
      Log.d(TAG, "Renegotiation needed");
    }

    @Override
    public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {
      Log.d(TAG, "onAddTrack: " + receiver.track().kind());

      MediaStreamTrack track = receiver.track();
      if (track instanceof VideoTrack) {
        remoteVideoTrack = (VideoTrack) track;
        Log.d(TAG, "Remote video track received");
        mainHandler.post(() -> callbacks.onRemoteVideoTrack(remoteVideoTrack));
      } else if (track instanceof AudioTrack) {
        remoteAudioTrack = (AudioTrack) track;
        Log.d(TAG, "Remote audio track received");
        mainHandler.post(() -> callbacks.onRemoteAudioTrack(remoteAudioTrack));
      }
    }
  }

  // Data Channels
  private void setupIceTricklingDataChannel() {
    DataChannel.Init init = new DataChannel.Init();
    init.negotiated = true;
    init.id = DC_ID_ICE_TRICKLING;

    iceTricklingDataChannel = peerConnection.createDataChannel("iceTrickling", init);

    iceTricklingDataChannel.registerObserver(new DataChannel.Observer() {
      @Override
      public void onBufferedAmountChange(long amount) {}

      @Override
      public void onStateChange() {
        DataChannel.State state = iceTricklingDataChannel.state();
        Log.d(TAG, "ICE Trickling channel state: " + state);

        if (state == DataChannel.State.OPEN) {
          iceTricklingChannelOpen = true;
          flushIceCandidateBuffer();
        } else if (state == DataChannel.State.CLOSED) {
          iceTricklingChannelOpen = false;
        }
      }

      @Override
      public void onMessage(DataChannel.Buffer buffer) {
        String json = extractString(buffer);
        Log.d(TAG, "Received ICE candidate: " + json);

        IceCandidate candidate = parseIceCandidate(json);

        if (candidate != null) {
          peerConnection.addIceCandidate(candidate);
        } else {
          Log.d(TAG, "Received end-of-candidates signal");
        }
      }
    });
  }

  private void setupMutedStateDataChannel() {
    DataChannel.Init init = new DataChannel.Init();
    init.negotiated = true;
    init.id = DC_ID_MUTED_STATE;

    mutedStateDataChannel = peerConnection.createDataChannel("mutedState", init);

    mutedStateDataChannel.registerObserver(new DataChannel.Observer() {
      @Override
      public void onBufferedAmountChange(long amount) {}

      @Override
      public void onStateChange() {
        DataChannel.State state = mutedStateDataChannel.state();
        Log.d(TAG, "mutedState channel state: " + state);

        if (state == DataChannel.State.OPEN) {
          mutedStateChannelOpen = true;

          boolean audioEnabled = localAudioTrack != null && localAudioTrack.enabled();
          boolean videoEnabled = localVideoTrack != null && localVideoTrack.enabled();
          sendMutedState(audioEnabled, videoEnabled);

        } else if (state == DataChannel.State.CLOSED) {
          mutedStateChannelOpen = false;
        }
      }

      @Override
      public void onMessage(DataChannel.Buffer buffer) {
        String json = extractString(buffer);
        Log.d(TAG, "Received muted state: " + json);

        try {
          JSONObject obj = new JSONObject(json);
          boolean audioEnabled = obj.getBoolean("audioEnabled");
          boolean videoEnabled = obj.getBoolean("videoEnabled");

          mainHandler.post(() ->
            callbacks.onRemoteMutedStateChanged(audioEnabled, videoEnabled));
        } catch (JSONException e) {
          Log.e(TAG, "Failed to parse muted state JSON", e);
        }
      }
    });
  }

  // Flush buffered ICE candidates when data channel opens
  private void flushIceCandidateBuffer() {
    Log.d(TAG, "Flushing " + iceCandidateBuffer.size() + " buffered ICE candidates");
    for (IceCandidate candidate : iceCandidateBuffer) {
      sendIceCandidateByDataChannel(candidate);
    }
    iceCandidateBuffer.clear();
  }

  private void sendIceCandidateByDataChannel(IceCandidate candidate) {
    if (!iceTricklingChannelOpen) {
      Log.d(TAG, "Data channel not open, buffering candidate");
      iceCandidateBuffer.add(candidate);
      return;
    }

    String json = serializeIceCandidate(candidate);
    ByteBuffer buffer = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
    iceTricklingDataChannel.send(new DataChannel.Buffer(buffer, false));

    Log.d(TAG, "Sent ICE candidate using data channel: " + candidate.sdpMid);
  }

  /**
   * Send current muted state to remote peer
   */
  public void sendMutedState(boolean audioEnabled, boolean videoEnabled) {
    if (!mutedStateChannelOpen) {
      Log.w(TAG, "Muted state channel not open");
      return;
    }

    try {
      JSONObject json = new JSONObject();
      json.put("audioEnabled", audioEnabled);
      json.put("videoEnabled", videoEnabled);

      ByteBuffer buffer = ByteBuffer.wrap(
        json.toString().getBytes(StandardCharsets.UTF_8));
      mutedStateDataChannel.send(new DataChannel.Buffer(buffer, false));

      Log.d(TAG, "Sent muted state: audio: " + audioEnabled + ", video: " + videoEnabled);
    } catch (JSONException e) {
      Log.e(TAG, "Failed to create muted state JSON", e);
    }
  }

  // ICE Gathering
  private void onIceCandidateGathered(IceCandidate candidate) {
    if (candidate == null) {
      Log.d(TAG, "ICE gathering complete, getting null candidate");
      isIceGatheringComplete = true;
      iceGatheringLatch.countDown();
      return;
    }

    // Determine candidate type from SDP
    String sdp = candidate.sdp;
    if (sdp.contains("typ relay")) {
      hasRelayCandidate = true;
      Log.d(TAG, "Got TURN/relay candidate");
      relayCandidateLatch.countDown();
    } else if (sdp.contains("typ srflx")) {
      hasSrflxCandidate = true;
      Log.d(TAG, "Got STUN/srflx candidate");
      srflxCandidateLatch.countDown();
    } else if (sdp.contains("typ host")) {
      hasHostCandidate = true;
      Log.d(TAG, "Got host candidate");
    }

    // If trickling enabled, send immediately or buffer
    if (enableIceTrickling) {
      sendIceCandidateByDataChannel(candidate);
    }
  }

  /**
   * Wait for enough ICE before sending offer/answer
   * Mirrors TypeScript gatheredEnoughIce()
   */
  private boolean waitForEnoughIce() {
    boolean hasTurnServer = false;
    boolean hasStunServer = false;

    searchLoop:
    for (PeerConnection.IceServer s : iceServers) {
      for (String url : s.urls) {
        if (url.startsWith("turn:")) {
          hasTurnServer = true;
        }
        if (url.startsWith("stun:")) {
          hasStunServer = true;
        }
        if (hasTurnServer && hasStunServer) {
          break searchLoop;
        }
      }
    }

    try {
      if (hasTurnServer) {
        // Wait for TURN relay candidate
        Log.d(TAG, "Waiting for TURN/relay candidate...");
        boolean gotRelay = relayCandidateLatch.await(RELAY_WAIT_MS, TimeUnit.MILLISECONDS);
        if (gotRelay && hasRelayCandidate) {
          Log.d(TAG, "Got relay candidate, proceeding");
          return true;
        }
        Log.w(TAG, "Timeout waiting for relay, proceeding anyway");
      } else if (hasStunServer) {
        // Wait for srflx, then delay for burst
        Log.d(TAG, "Waiting for STUN/srflx candidate...");
        boolean gotSrflx = srflxCandidateLatch.await(SRFLX_WAIT_MS, TimeUnit.MILLISECONDS);

        if (gotSrflx) {
          // Wait 150ms more for burst of additional srflx candidates
          Log.d(TAG, "Got srflx, waiting for burst...");
          Thread.sleep(SRFLX_BURST_DELAY_MS);
          return true;
        }
        Log.w(TAG, "Timeout waiting for srflx, proceeding anyway");
      } else if (iceServers.isEmpty()) {
        // No servers: Wait for host candidates
        Log.d(TAG, "No ICE servers, waiting for host candidates...");
        boolean gotComplete = iceGatheringLatch.await(HOST_WAIT_MS, TimeUnit.MILLISECONDS);
        Log.d(TAG, "Proceeding with host candidates only");
        return hasHostCandidate;
      }

      // Fallback
      return iceGatheringLatch.await(RELAY_WAIT_MS, TimeUnit.MILLISECONDS);

    } catch (InterruptedException e) {
      Log.e(TAG, "Interrupted while waiting for ICE", e);
      Thread.currentThread().interrupt();
      return false;
    }
  }

  // Outgoing Call

  /**
   * Start outgoing call
   */
  public void startOutgoingCall() {
    if (localStream == null) {
      mainHandler.post(() -> callbacks.onError("Local media stream not set"));
      return;
    }

    Log.d(TAG, "Starting outgoing call");
    mainHandler.post(() ->
      callbacks.onConnectionStateChanged(PeerConnection.PeerConnectionState.CONNECTING));

    createPeerConnection();

    for (AudioTrack track : localStream.audioTracks) {
      RtpSender sender = peerConnection.addTrack(track, Collections.singletonList(localStream.getId()));
      Log.d(TAG, "Added audio track, sender: " + sender);
    }
    for (VideoTrack track : localStream.videoTracks) {
      RtpSender sender = peerConnection.addTrack(track, Collections.singletonList(localStream.getId()));
      Log.d(TAG, "Added video track, sender: " + sender);
    }
    Log.d(TAG, "Total senders: " + peerConnection.getSenders().size());

    // Create offer

    peerConnection.createOffer(new SdpObserver() {
      @Override
      public void onCreateSuccess(SessionDescription sdp) {
        onOfferCreated(sdp);
      }

      @Override
      public void onSetSuccess() {}

      @Override
      public void onCreateFailure(String error) {
        Log.e(TAG, "Failed to create offer: " + error);
        mainHandler.post(() -> callbacks.onError("Failed to create offer: " + error));
      }

      @Override
      public void onSetFailure(String error) {}
    }, new MediaConstraints());
  }

  private void onOfferCreated(SessionDescription offerSdp) {
    peerConnection.setLocalDescription(new SdpObserver() {
      @Override
      public void onSetSuccess() {
        Log.d(TAG, "Local description set, waiting for ICE...");

        // Wait for ICE in background
        new Thread(() -> {
          boolean gotIce = waitForEnoughIce();

          if (!gotIce) {
            Log.w(TAG, "Proceeding without optimal ICE candidates");
          }

          // Get final SDP
          SessionDescription localDesc = peerConnection.getLocalDescription();
          if (localDesc != null) {
            String finalSdp = localDesc.description;

            // Enable trickling for additional candidates
            enableIceTrickling = true;

            // Notify ViewModel
            mainHandler.post(() -> callbacks.onOfferReady(finalSdp));
          } else {
            mainHandler.post(() ->
              callbacks.onError("Local description is null after ICE gathering"));
          }
        }).start();
      }

      @Override
      public void onCreateSuccess(SessionDescription sdp) {}

      @Override
      public void onCreateFailure(String error) {}

      @Override
      public void onSetFailure(String error) {
        Log.e(TAG, "Failed to set local description: " + error);
        mainHandler.post(() ->
          callbacks.onError("Failed to set local description: " + error));
      }
    }, offerSdp);
  }

  /**
   * Handle answer SDP from remote peer for outgoing call
   */
  public void handleAnswerSdp(String answerSdp) {
    Log.d(TAG, "Handling answer SDP");

    SessionDescription remoteSdp = new SessionDescription(
      SessionDescription.Type.ANSWER,
      answerSdp
    );

    peerConnection.setRemoteDescription(new SdpObserver() {
      @Override
      public void onSetSuccess() {
        Log.d(TAG, "Remote answer set, connection should establish");
      }

      @Override
      public void onCreateSuccess(SessionDescription sdp) {}

      @Override
      public void onCreateFailure(String error) {}

      @Override
      public void onSetFailure(String error) {
        Log.e(TAG, "Failed to set remote description: " + error);
        mainHandler.post(() ->
          callbacks.onError("Failed to set remote description: " + error));
      }
    }, remoteSdp);
  }

  // Incoming Call

  /**
   * Accept incoming call
   */
  public void acceptIncomingCall(String offerSdp) {
    if (localStream == null) {
      mainHandler.post(() -> callbacks.onError("Local media stream not set"));
      return;
    }

    Log.d(TAG, "Accepting incoming call");
    mainHandler.post(() ->
      callbacks.onConnectionStateChanged(PeerConnection.PeerConnectionState.CONNECTING));

    createPeerConnection();

    SessionDescription remoteSdp = new SessionDescription(
      SessionDescription.Type.OFFER,
      offerSdp
    );

    peerConnection.setRemoteDescription(new SdpObserver() {
      @Override
      public void onSetSuccess() {
        Log.d(TAG, "Remote offer set, adding local tracks...");
        addLocalTracksAndCreateAnswer();
      }

      @Override
      public void onCreateSuccess(SessionDescription sdp) {}

      @Override
      public void onCreateFailure(String error) {}

      @Override
      public void onSetFailure(String error) {
        Log.e(TAG, "Failed to set remote description: " + error);
        mainHandler.post(() ->
          callbacks.onError("Failed to set remote description: " + error));
      }
    }, remoteSdp);
  }

  private void addLocalTracksAndCreateAnswer() {
    // Add local tracks
    for (AudioTrack track : localStream.audioTracks) {
      peerConnection.addTrack(track, Collections.singletonList(localStream.getId()));
    }
    for (VideoTrack track : localStream.videoTracks) {
      peerConnection.addTrack(track, Collections.singletonList(localStream.getId()));
    }

    // Create answer
    peerConnection.createAnswer(new SdpObserver() {
      @Override
      public void onCreateSuccess(SessionDescription sdp) {
        onAnswerCreated(sdp);
      }

      @Override
      public void onSetSuccess() {}

      @Override
      public void onCreateFailure(String error) {
        Log.e(TAG, "Failed to create answer: " + error);
        mainHandler.post(() -> callbacks.onError("Failed to create answer: " + error));
      }

      @Override
      public void onSetFailure(String error) {}
    }, new MediaConstraints());
  }

  private void onAnswerCreated(SessionDescription answerSdp) {
    peerConnection.setLocalDescription(new SdpObserver() {
      @Override
      public void onSetSuccess() {
        Log.d(TAG, "Local answer set, waiting for ICE...");

        // Wait for ICE on background thread
        new Thread(() -> {
          boolean gotIce = waitForEnoughIce();

          if (!gotIce) {
            Log.w(TAG, "Proceeding without optimal ICE candidates");
          }

          // Get final SDP
          SessionDescription localDesc = peerConnection.getLocalDescription();
          if (localDesc != null) {
            String finalSdp = localDesc.description;

            // Enable trickling for additional candidates
            enableIceTrickling = true;

            // Notify ViewModel
            mainHandler.post(() -> callbacks.onAnswerReady(finalSdp));
          } else {
            mainHandler.post(() ->
              callbacks.onError("Local description is null after ICE gathering"));
          }
        }).start();
      }

      @Override
      public void onCreateSuccess(SessionDescription sdp) {}

      @Override
      public void onCreateFailure(String error) {}

      @Override
      public void onSetFailure(String error) {
        Log.e(TAG, "Failed to set local description: " + error);
        mainHandler.post(() ->
          callbacks.onError("Failed to set local description: " + error));
      }
    }, answerSdp);
  }

  // Media Control
  public void setAudioEnabled(boolean enabled) {
    if (localAudioTrack != null) {
      localAudioTrack.setEnabled(enabled);
      Log.d(TAG, "Local audio " + (enabled ? "enabled" : "disabled"));
    }
  }

  public void setVideoEnabled(boolean enabled) {
    if (localVideoTrack != null) {
      localVideoTrack.setEnabled(enabled);
      Log.d(TAG, "Local video " + (enabled ? "enabled" : "disabled"));
    }
  }

  public VideoTrack getLocalVideoTrack() {
    return localVideoTrack;
  }

  public VideoTrack getRemoteVideoTrack() {
    return remoteVideoTrack;
  }

  private void detectRelayUsage() {
    // Schedule detection after connection settles
    mainHandler.postDelayed(() -> {
      if (peerConnection == null) return;

      peerConnection.getStats(report -> {
        boolean relayUsed = false;

        Map<String, RTCStats> statsMap = report.getStatsMap();

        // First, find the selected candidate pair
        String selectedLocalCandidateId = null;
        String selectedRemoteCandidateId = null;

        for (Map.Entry<String, RTCStats> entry : statsMap.entrySet()) {
          RTCStats stats = entry.getValue();

          if ("candidate-pair".equals(stats.getType())) {
            Map<String, Object> members = stats.getMembers();

            // Check if this pair is selected/nominated
            Object state = members.get("state");
            if ("succeeded".equals(state) || Boolean.TRUE.equals(members.get("nominated"))) {
              selectedLocalCandidateId = (String) members.get("localCandidateId");
              selectedRemoteCandidateId = (String) members.get("remoteCandidateId");
              break;
            }
          }
        }

        // Now check if either candidate is relay type
        if (selectedLocalCandidateId != null || selectedRemoteCandidateId != null) {
          for (Map.Entry<String, RTCStats> entry : statsMap.entrySet()) {
            RTCStats stats = entry.getValue();
            String id = stats.getId();

            if (id.equals(selectedLocalCandidateId) || id.equals(selectedRemoteCandidateId)) {
              if ("local-candidate".equals(stats.getType()) ||
                "remote-candidate".equals(stats.getType())) {

                Map<String, Object> members = stats.getMembers();
                String candidateType = (String) members.get("candidateType");

                if ("relay".equals(candidateType)) {
                  relayUsed = true;
                  break;
                }
              }
            }
          }
        }

        boolean finalRelayUsed = relayUsed;
        mainHandler.post(() -> callbacks.onRelayUsageChanged(finalRelayUsed));

        Log.d(TAG, "Relay usage detected: " + finalRelayUsed);
      });
    }, 500);
  }

  // Cleanup

  public void endCall() {
    if (isEnded) {
      Log.d(TAG, "endCall() already called, skipping");
      return;
    }

    isEnded = true;
    Log.d(TAG, "Ending call");

    if (peerConnection != null) {
      peerConnection.close();
      peerConnection.dispose();
      peerConnection = null;
    }

    if (iceTricklingDataChannel != null) {
      iceTricklingDataChannel.close();
      iceTricklingDataChannel.dispose();
      iceTricklingDataChannel = null;
    }

    if (mutedStateDataChannel != null) {
      mutedStateDataChannel.close();
      mutedStateDataChannel.dispose();
      mutedStateDataChannel = null;
    }

    if (localStream != null) {
      for (AudioTrack track : localStream.audioTracks) {
        track.setEnabled(false);
      }
      for (VideoTrack track : localStream.videoTracks) {
        track.setEnabled(false);
      }
    }

    iceCandidateBuffer.clear();
    iceTricklingChannelOpen = false;
    mutedStateChannelOpen = false;
    enableIceTrickling = false;
    localStream = null;
    localVideoTrack = null;
    localAudioTrack = null;
    remoteVideoTrack = null;
    remoteAudioTrack = null;
  }

  public void dispose() {
    endCall();

    if (peerConnectionFactory != null) {
      peerConnectionFactory.dispose();
      peerConnectionFactory = null;
    }
  }

  public PeerConnectionFactory getPeerConnectionFactory() {
    return peerConnectionFactory;
  }

  private String extractString(@NonNull DataChannel.Buffer buffer) {
    byte[] bytes = new byte[buffer.data.remaining()];
    buffer.data.get(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private IceCandidate parseIceCandidate(String json) {
    if (json == null || json.equals("null")) {
      return null; // End of candidates
    }

    try {
      JSONObject obj = new JSONObject(json);
      String sdpMid = obj.getString("sdpMid");
      int sdpMLineIndex = obj.getInt("sdpMLineIndex");
      String sdp = obj.getString("candidate");

      return new IceCandidate(sdpMid, sdpMLineIndex, sdp);
    } catch (JSONException e) {
      Log.e(TAG, "Failed to parse ICE candidate JSON", e);
      return null;
    }
  }

  @NonNull
  private String serializeIceCandidate(@NonNull IceCandidate candidate) {
    try {
      JSONObject obj = new JSONObject();
      obj.put("sdpMid", candidate.sdpMid);
      obj.put("sdpMLineIndex", candidate.sdpMLineIndex);
      obj.put("candidate", candidate.sdp);
      return obj.toString();
    } catch (JSONException e) {
      Log.e(TAG, "Failed to serialize ICE candidate", e);
      return "null";
    }
  }
}
