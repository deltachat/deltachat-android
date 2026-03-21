package org.thoughtcrime.securesms.calls;

import android.Manifest;
import android.app.KeyguardManager;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.util.Rational;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.telecom.CallEndpointCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import java.util.ArrayList;
import java.util.List;
import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.EglUtils;
import org.thoughtcrime.securesms.R;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

/** Full-screen call activity for VoIP calls */
@RequiresApi(api = Build.VERSION_CODES.O)
public class CallActivity extends AppCompatActivity {

  private static final String TAG = CallActivity.class.getSimpleName();
  private static final int PERMISSION_REQUEST_CODE = 1001;

  public static final String ACTION_ANSWER_CALL = BuildConfig.APPLICATION_ID + ".ANSWER_CALL";
  public static final String ACTION_DECLINE_CALL = BuildConfig.APPLICATION_ID + ".DECLINE_CALL";
  public static final String ACTION_HANGUP_CALL = BuildConfig.APPLICATION_ID + ".HANGUP_CALL";

  // Views

  private CallViewModel viewModel;

  private SurfaceViewRenderer localVideoView;
  private SurfaceViewRenderer remoteVideoView;

  // Status and info
  private TextView statusText;
  private TextView displayNameText;
  private View incomingCallPrompt;

  private View callerIconContainer;
  private ImageView callerIconView;
  private ImageView remoteAvatarView;

  private MaterialButtonToggleGroup answerModeSelector;

  // Layouts and elements
  private ConstraintLayout topBar;
  private LinearLayout bottomLayoutContainer;
  private CardView localVideoContainer;
  private ImageButton endCallButton;
  private MaterialButton answerButton;
  private MaterialButton declineButton;
  private ImageButton muteButton;
  private ImageButton videoButton;
  private ImageButton speakerButton;
  private ImageButton switchCameraButton;

  private final MediatorLiveData<Boolean> videoConfigChanged = new MediatorLiveData<>();

  // Misc

  private PowerManager.WakeLock proximityWakeLock;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_call);

    setupWindowFlags();

    initializeViews();

    setupInsets();

    initializeProximityWakeLock();

    if (!hasRequiredPermissions()) {
      requestRequiredPermissions();
      return;
    }

    // PiP listener
    addOnPictureInPictureModeChangedListener(
        pipModeInfo -> {
          boolean isInPipMode = pipModeInfo.isInPictureInPictureMode();

          if (isInPipMode) {
            topBar.setVisibility(View.GONE);
            bottomLayoutContainer.setVisibility(View.GONE);
            incomingCallPrompt.setVisibility(View.GONE);
          } else {
            topBar.setVisibility(View.VISIBLE);
            bottomLayoutContainer.setVisibility(View.VISIBLE);
            if (viewModel != null) {
              CallViewModel.CallState state = viewModel.getCallState().getValue();
              if (state != null) {
                updateUIForState(state);
              }
            }
          }

          layoutVideos();
          updateProximityWakeLock();
        });

    initializeViewModel();

    handleIntents(getIntent());
  }

  private void handleIntents(Intent intent) {
    if (intent == null || viewModel == null) {
      return;
    }

    CallCoordinator coordinator = CallCoordinator.getInstance(getApplication());

    if (!coordinator.hasActiveCall()) {
      Log.e(TAG, "No active call exists, cannot proceed");
      Toast.makeText(this, "No active call", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    String action = intent.getAction();
    Log.d(TAG, "handleIntents: action=" + action);

    // Handle notification actions
    if (ACTION_ANSWER_CALL.equals(action)) {
      Log.d(TAG, "Handling ANSWER_CALL action from notification");
      viewModel.handleNotificationAnswer();
      return;
    }

    if (ACTION_DECLINE_CALL.equals(action)) {
      Log.d(TAG, "Handling DECLINE_CALL action from notification");
      viewModel.handleNotificationDecline();
      finish();
      return;
    }

    if (ACTION_HANGUP_CALL.equals(action)) {
      Log.d(TAG, "Handling HANGUP_CALL action from notification");
      viewModel.handleNotificationHangup();
      finish();
      return;
    }

    if (coordinator.hasOngoingCall()) {
      Log.d(TAG, "Resuming existing call");
    } else if (!coordinator.isIncomingCall()) {
      Log.d(TAG, "Starting outgoing call");
      viewModel.startOutgoingCallWhenReady();
    }
  }

  private void setupInsets() {
    View rootView = findViewById(R.id.call_root_view);

    ViewCompat.setOnApplyWindowInsetsListener(
        rootView,
        (v, windowInsets) -> {
          Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

          v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

          return windowInsets;
        });
  }

  private void setupWindowFlags() {
    WindowCompat.enableEdgeToEdge(getWindow());

    // Show when locked
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true);
      setTurnScreenOn(true);

      KeyguardManager keyguardManager =
          (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
      if (keyguardManager != null) {
        keyguardManager.requestDismissKeyguard(this, null);
      }
    } else {
      getWindow()
          .addFlags(
              WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                  | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                  | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    // Control status bar icon colors
    WindowInsetsControllerCompat controller =
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
    controller.setAppearanceLightStatusBars(false);
    controller.setAppearanceLightNavigationBars(false);
  }

  private void initializeProximityWakeLock() {
    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

    if (powerManager == null) {
      Log.w(TAG, "PowerManager not available");
      return;
    }

    if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
      proximityWakeLock =
          powerManager.newWakeLock(
              PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "DeltaChat:ProximityLock");
      proximityWakeLock.setReferenceCounted(false);
      Log.d(TAG, "Proximity wake lock initialized");
    } else {
      Log.w(TAG, "Proximity wake lock not supported on this device");
    }
  }

  private void initializeViews() {
    topBar = findViewById(R.id.top_bar);
    bottomLayoutContainer = findViewById(R.id.call_controls_layout);
    localVideoContainer = findViewById(R.id.local_video_container);

    localVideoView = findViewById(R.id.local_video_view);
    remoteVideoView = findViewById(R.id.remote_video_view);

    statusText = findViewById(R.id.status_text);
    displayNameText = findViewById(R.id.display_name_text);
    incomingCallPrompt = findViewById(R.id.incoming_call_prompt);

    callerIconContainer = findViewById(R.id.caller_icon_container);
    callerIconView = findViewById(R.id.caller_icon);
    remoteAvatarView = findViewById(R.id.remote_avatar_view);

    answerModeSelector = findViewById(R.id.answer_mode_selector);

    endCallButton = findViewById(R.id.end_call_button);
    answerButton = findViewById(R.id.answer_button);
    declineButton = findViewById(R.id.decline_button);
    muteButton = findViewById(R.id.mute_button);
    videoButton = findViewById(R.id.video_button);
    speakerButton = findViewById(R.id.speaker_button);
    switchCameraButton = findViewById(R.id.switch_camera_button);

    initializeVideoRenderers();

    setupButtonListeners();
  }

  private void initializeVideoRenderers() {
    // Local video (the small one)
    localVideoView.init(EglUtils.getEglBase().getEglBaseContext(), null);
    localVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
    localVideoView.setZOrderMediaOverlay(true);
    localVideoView.setEnableHardwareScaler(true);

    // Remote video (full screen one)
    remoteVideoView.init(EglUtils.getEglBase().getEglBaseContext(), null);
    remoteVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
    remoteVideoView.setEnableHardwareScaler(true);

    Log.d(TAG, "Video renderers initialized");
  }

  private void setupButtonListeners() {
    answerButton.setOnClickListener(
        v -> {
          if (viewModel != null) {
            viewModel.answerCall();
          }
        });

    declineButton.setOnClickListener(
        v -> {
          if (viewModel != null) {
            viewModel.declineCall();
          }
          finish();
        });

    endCallButton.setOnClickListener(
        v -> {
          if (viewModel != null) {
            viewModel.hangUp();
          }
          finish();
        });

    muteButton.setOnClickListener(
        v -> {
          if (viewModel != null) {
            viewModel.toggleAudio();
          }
        });

    videoButton.setOnClickListener(
        v -> {
          if (viewModel != null) {
            viewModel.toggleVideo();
          }
        });

    speakerButton.setOnClickListener(
        v -> {
          if (viewModel != null) {
            showAudioDevicePicker();
          }
        });

    switchCameraButton.setOnClickListener(
        v -> {
          if (viewModel != null) {
            viewModel.switchCamera();
          }
        });

    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                CallViewModel.CallState state = viewModel.getCallState().getValue();

                if (isDeviceLocked() || state == null) {
                  finish();
                  return;
                }

                switch (state) {
                  case RINGING:
                  case CONNECTING:
                  case CONNECTED:
                  case RECONNECTING:
                    enterPictureInPictureMode(createPipParams());
                    break;

                  case INITIALIZING:
                  case PROMPTING_USER_ACCEPT:
                  case ENDED:
                  case ERROR:
                  default:
                    finish();
                    break;
                }
              }
            });
  }

  private void initializeViewModel() {
    viewModel = new ViewModelProvider(this).get(CallViewModel.class);

    observeViewModel();

    viewModel.initialize();

    Log.d(TAG, "CallViewModel initialized");
  }

  private void observeViewModel() {
    // Call state
    viewModel.getCallState().observe(this, this::updateUIForState);

    // Display info
    viewModel
        .getDisplayName()
        .observe(
            this,
            name -> {
              if (name != null) {
                displayNameText.setText(name);
              } else {
                displayNameText.setText(R.string.unknown);
              }
            });

    viewModel
        .getDisplayIcon()
        .observe(
            this,
            icon -> {
              if (callerIconView != null) {
                if (icon != null) {
                  callerIconView.setImageIcon(icon);
                } else {
                  callerIconView.setImageResource(R.drawable.ic_person);
                }
              }

              if (remoteAvatarView != null) {
                if (icon != null) {
                  remoteAvatarView.setImageIcon(icon);
                } else {
                  remoteAvatarView.setImageResource(R.drawable.ic_person);
                }
              }
            });

    // Audio/video state
    viewModel
        .getAudioEnabled()
        .observe(
            this,
            enabled -> {
              muteButton.setSelected(!enabled);
              muteButton.setImageResource(enabled ? R.drawable.ic_mic_on : R.drawable.ic_mic_off);
            });

    viewModel
        .getVideoEnabled()
        .observe(
            this,
            enabled -> {
              videoButton.setSelected(!enabled);
              videoButton.setImageResource(
                  enabled ? R.drawable.ic_videocam_on : R.drawable.ic_videocam_off);
            });

    viewModel
        .getCurrentAudioEndpoint()
        .observe(
            this,
            endpoint -> {
              updateSpeakerButton(endpoint);
              updateProximityWakeLock();
            });

    viewModel
        .getAvailableAudioEndpoints()
        .observe(
            this,
            endpoints -> {
              // Need observe to trigger flow emit
              Log.d(
                  TAG,
                  "Available endpoints updated, count: "
                      + (endpoints != null ? endpoints.size() : 0));
            });

    // Errors
    viewModel
        .getErrorMessage()
        .observe(
            this,
            error -> {
              if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
              }
            });

    // Relay usage
    viewModel
        .getIsRelayUsed()
        .observe(
            this,
            isRelayUsed -> {
              if (isRelayUsed != null) {
                Log.d(TAG, "TURN relay is " + (isRelayUsed ? "being used" : "NOT being used"));
              }
            });

    // Set up LiveData sources
    videoConfigChanged.addSource(viewModel.getCallState(), v -> videoConfigChanged.setValue(true));
    videoConfigChanged.addSource(
        viewModel.getLocalVideoTrack(), v -> videoConfigChanged.setValue(true));
    videoConfigChanged.addSource(
        viewModel.getRemoteVideoTrack(), v -> videoConfigChanged.setValue(true));
    videoConfigChanged.addSource(
        viewModel.getVideoEnabled(), v -> videoConfigChanged.setValue(true));
    videoConfigChanged.addSource(
        viewModel.getRemoteVideoEnabled(), v -> videoConfigChanged.setValue(true));

    // Video layout
    videoConfigChanged.observe(
        this,
        changed -> {
          if (!isFinishing() && !isDestroyed()) {
            layoutVideos();
          }
        });
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleIntents(intent);
    Log.d(TAG, "onNewIntent called - activity reused");
  }

  private void updateUIForState(CallViewModel.CallState state) {
    Log.d(TAG, "Call state: " + state);

    if (isInPictureInPictureMode()
        && state != CallViewModel.CallState.ENDED
        && state != CallViewModel.CallState.ERROR) {
      return;
    }

    switch (state) {
      case INITIALIZING:
        statusText.setText(R.string.call_initializing);
        incomingCallPrompt.setVisibility(View.GONE);
        bottomLayoutContainer.setVisibility(View.GONE);
        callerIconContainer.setVisibility(View.GONE);
        answerModeSelector.setVisibility(View.GONE);
        break;

      case PROMPTING_USER_ACCEPT:
        statusText.setText(R.string.call_incoming);
        incomingCallPrompt.setVisibility(View.VISIBLE);
        bottomLayoutContainer.setVisibility(View.GONE);
        callerIconContainer.setVisibility(View.VISIBLE);
        answerModeSelector.setVisibility(View.VISIBLE);
        initializeAnswerModeSelector();
        break;

      case RINGING:
        statusText.setText(R.string.call_ringing);
        incomingCallPrompt.setVisibility(View.GONE);
        bottomLayoutContainer.setVisibility(View.VISIBLE);
        callerIconContainer.setVisibility(View.GONE);
        answerModeSelector.setVisibility(View.GONE);
        break;

      case CONNECTING:
        statusText.setText(R.string.connectivity_connecting);
        incomingCallPrompt.setVisibility(View.GONE);
        bottomLayoutContainer.setVisibility(View.VISIBLE);
        callerIconContainer.setVisibility(View.GONE);
        answerModeSelector.setVisibility(View.GONE);
        break;

      case CONNECTED:
        statusText.setText(R.string.connectivity_connected);
        incomingCallPrompt.setVisibility(View.GONE);
        bottomLayoutContainer.setVisibility(View.VISIBLE);
        callerIconContainer.setVisibility(View.GONE);
        answerModeSelector.setVisibility(View.GONE);
        break;

      case RECONNECTING:
        statusText.setText(R.string.call_reconnecting);
        break;

      case ENDED:
        statusText.setText(R.string.call_ended);
        finish();
        break;

      case ERROR:
        statusText.setText(R.string.call_failed);
        incomingCallPrompt.setVisibility(View.GONE);
        bottomLayoutContainer.setVisibility(View.GONE);
        callerIconContainer.setVisibility(View.GONE);
        answerModeSelector.setVisibility(View.GONE);

        new Handler(Looper.getMainLooper())
            .postDelayed(
                () -> {
                  if (!isFinishing()) {
                    finish();
                  }
                },
                2500);
        break;
    }

    updateProximityWakeLock();
  }

  private void updateSpeakerButton(CallEndpointCompat endpoint) {
    if (endpoint == null) {
      speakerButton.setImageResource(R.drawable.ic_volume_up);
      return;
    }

    int iconRes = CallUtil.getIconResByCallEndpoint(endpoint);

    speakerButton.setImageResource(iconRes);
    Log.d(TAG, "Speaker button updated for endpoint: " + endpoint.getName());
  }

  private void showAudioDevicePicker() {
    List<CallEndpointCompat> endpoints = viewModel.getAvailableAudioEndpoints().getValue();
    CallEndpointCompat currentEndpoint = viewModel.getCurrentAudioEndpoint().getValue();

    if (endpoints == null || endpoints.isEmpty()) {
      Log.w(TAG, "No audio endpoints available");
      Toast.makeText(this, "No audio devices available", Toast.LENGTH_SHORT).show();
      return;
    }

    // Create and show BottomSheetDialog
    AudioDevicePickerDialog dialog =
        new AudioDevicePickerDialog(
            this,
            endpoints,
            currentEndpoint,
            selectedEndpoint -> {
              viewModel.selectAudioDevice(selectedEndpoint);
            });

    dialog.show();
  }

  private void updateProximityWakeLock() {
    if (proximityWakeLock == null) {
      return;
    }

    boolean shouldHoldLock = shouldHoldLock();

    // Acquire or release based on conditions
    if (shouldHoldLock && !proximityWakeLock.isHeld()) {
      proximityWakeLock.acquire(10 * 60 * 1000L);
      Log.d(TAG, "Proximity wake lock acquired");
    } else if (!shouldHoldLock && proximityWakeLock.isHeld()) {
      // Prevent screen from turning on immediately if phone is still near face
      proximityWakeLock.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY);
      Log.d(TAG, "Proximity wake lock released with wait flag");
    }
  }

  private boolean shouldHoldLock() {
    CallViewModel.CallState state = viewModel.getCallState().getValue();
    CallEndpointCompat endpoint = viewModel.getCurrentAudioEndpoint().getValue();

    return (state == CallViewModel.CallState.CONNECTED
            || state == CallViewModel.CallState.RINGING
            || state == CallViewModel.CallState.CONNECTING
            || state == CallViewModel.CallState.RECONNECTING)
        && endpoint != null
        && endpoint.getType() == CallEndpointCompat.TYPE_EARPIECE
        && !isInPictureInPictureMode();
  }

  private void initializeAnswerModeSelector() {
    CallCoordinator coordinator = CallCoordinator.getInstance(getApplication());

    // Set initial selection without triggering listener
    answerModeSelector.clearOnButtonCheckedListeners();
    answerModeSelector.check(
        coordinator.isStartsWithVideo() ? R.id.answer_video_button : R.id.answer_audio_only_button);

    // Set listener
    answerModeSelector.addOnButtonCheckedListener(
        (group, checkedId, isChecked) -> {
          if (isChecked && viewModel != null) {
            boolean startsWithVideo = (checkedId == R.id.answer_video_button);
            viewModel.setStartsWithVideo(startsWithVideo);
            viewModel.switchToEarpiece(!startsWithVideo);
            Log.d(TAG, "Answer mode changed to: " + (startsWithVideo ? "Video" : "Audio"));
          }
        });
  }

  private void layoutVideos() {
    if (isFinishing() || isDestroyed()) return;
    if (viewModel == null) return;

    CallViewModel.CallState state = viewModel.getCallState().getValue();
    if (state == CallViewModel.CallState.ENDED || state == CallViewModel.CallState.ERROR) return;

    detachAllTracks();

    Boolean videoEnabled = viewModel.getVideoEnabled().getValue();
    Boolean remoteVideoEnabled = viewModel.getRemoteVideoEnabled().getValue();
    CallCoordinator coordinator = CallCoordinator.getInstance(getApplication());
    VideoTrack localTrack = viewModel.getLocalVideoTrack().getValue();
    VideoTrack remoteTrack = viewModel.getRemoteVideoTrack().getValue();

    boolean showFullScreen = false;

    if (state == CallViewModel.CallState.CONNECTED
        && remoteTrack != null
        && Boolean.TRUE.equals(remoteVideoEnabled)) {
      remoteTrack.addSink(remoteVideoView);
      showFullScreen = true;
    } else if (!coordinator.isIncomingCall()
        && (state == CallViewModel.CallState.RINGING || state == CallViewModel.CallState.CONNECTING)
        && localTrack != null
        && Boolean.TRUE.equals(videoEnabled)) {
      localTrack.addSink(remoteVideoView);
      showFullScreen = true;
    }

    remoteVideoView.setVisibility(showFullScreen ? View.VISIBLE : View.GONE);

    boolean showCorner =
        state == CallViewModel.CallState.CONNECTED
            && localTrack != null
            && Boolean.TRUE.equals(videoEnabled)
            && !isInPictureInPictureMode();

    if (showCorner) {
      localTrack.addSink(localVideoView);
    }

    localVideoContainer.setVisibility(showCorner ? View.VISIBLE : View.GONE);

    boolean showAvatar =
        !showFullScreen
            && state != CallViewModel.CallState.PROMPTING_USER_ACCEPT
            && state != CallViewModel.CallState.INITIALIZING;

    remoteAvatarView.setVisibility(showAvatar ? View.VISIBLE : View.GONE);
  }

  private void detachAllTracks() {
    VideoTrack localTrack = viewModel.getLocalVideoTrack().getValue();
    VideoTrack remoteTrack = viewModel.getRemoteVideoTrack().getValue();

    // Clear all sinks first
    if (localTrack != null) {
      localTrack.removeSink(localVideoView);
      localTrack.removeSink(remoteVideoView);
    }
    if (remoteTrack != null) {
      remoteTrack.removeSink(remoteVideoView);
    }
  }

  // Permissions

  private boolean hasRequiredPermissions() {
    return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED;
  }

  private void requestRequiredPermissions() {
    ActivityCompat.requestPermissions(
        this,
        new String[] {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
        PERMISSION_REQUEST_CODE);
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode == PERMISSION_REQUEST_CODE) {
      boolean microphoneGranted = false;
      boolean cameraGranted = false;

      for (int i = 0; i < permissions.length; i++) {
        if (permissions[i].equals(Manifest.permission.RECORD_AUDIO)) {
          microphoneGranted = (grantResults[i] == PackageManager.PERMISSION_GRANTED);
        } else if (permissions[i].equals(Manifest.permission.CAMERA)) {
          cameraGranted = (grantResults[i] == PackageManager.PERMISSION_GRANTED);
        }
      }

      if (!microphoneGranted) {
        Toast.makeText(this, "Microphone permission is required for calls", Toast.LENGTH_LONG)
            .show();
        finish();
        return;
      }

      CallCoordinator coordinator = CallCoordinator.getInstance(getApplication());

      if (!cameraGranted && coordinator.isStartsWithVideo()) {
        Log.w(TAG, "Camera permission denied, switching to audio-only");
        Toast.makeText(
                this, "Starting audio-only call (camera permission denied)", Toast.LENGTH_SHORT)
            .show();
        coordinator.setStartsWithVideo(false);
      }

      initializeViewModel();
      handleIntents(getIntent());
    }
  }

  // Picture-in-Picture

  @Override
  public void onUserLeaveHint() {
    super.onUserLeaveHint();

    // Enter PiP mode when user presses home button during active call
    if (viewModel != null) {
      CallViewModel.CallState state = viewModel.getCallState().getValue();

      if (state != null) {
        switch (state) {
          case RINGING:
          case CONNECTING:
          case CONNECTED:
          case RECONNECTING:
            enterPictureInPictureMode(createPipParams());
            break;

          case INITIALIZING:
          case PROMPTING_USER_ACCEPT:
          case ENDED:
          case ERROR:
          default:
            finish();
            break;
        }
      }
    } else {
      Log.w(TAG, "No View Model exists");
    }
  }

  private PictureInPictureParams createPipParams() {
    Rational aspectRatio = new Rational(9, 16);
    PictureInPictureParams.Builder builder =
        new PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .setActions(new ArrayList<>());

    // FIXME: PiP currently shows media controls.
    // Will fix later: to fix this, we may need changes to audio playback implementation.

    return builder.build();
  }

  // Helper

  private boolean isDeviceLocked() {
    KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
    if (keyguardManager != null) {
      return keyguardManager.isKeyguardLocked();
    }
    return false;
  }

  // Cleanup

  @Override
  protected void onPause() {
    super.onPause();

    if (proximityWakeLock != null && proximityWakeLock.isHeld()) {
      proximityWakeLock.release();
      Log.d(TAG, "Proximity wake lock released in onDestroy");
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    detachAllTracks();

    // Release video renderers
    if (localVideoView != null) {
      localVideoView.release();
    }
    if (remoteVideoView != null) {
      remoteVideoView.release();
    }

    Log.d(TAG, "CallActivity destroyed");
  }
}
