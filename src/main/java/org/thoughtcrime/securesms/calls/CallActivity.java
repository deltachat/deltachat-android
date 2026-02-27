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
import android.util.Log;
import android.util.Rational;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.EglUtils;
import org.thoughtcrime.securesms.R;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen call activity for VoIP calls
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class CallActivity extends AppCompatActivity {

  private static final String TAG = CallActivity.class.getSimpleName();
  private static final int PERMISSION_REQUEST_CODE = 1001;
  private static final int PERMISSION_REQUEST_FULL_SCREEN = 1002;

  public static final String ACTION_ANSWER_CALL = BuildConfig.APPLICATION_ID + ".ANSWER_CALL";
  public static final String ACTION_DECLINE_CALL = BuildConfig.APPLICATION_ID + ".DECLINE_CALL";
  public static final String ACTION_HANGUP_CALL = BuildConfig.APPLICATION_ID + ".HANGUP_CALL";

  public static final String EXTRA_AUTO_ANSWER = "auto_answer";

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

  private MaterialButtonToggleGroup answerModeSelector;

  // Layouts and elements
  private ConstraintLayout topBar;
  private MaterialCardView bottomSheetContainer;
  private CardView localVideoContainer;
  private ImageButton endCallButton;
  private MaterialButton answerButton;
  private MaterialButton declineButton;
  private ImageButton muteButton;
  private ImageButton videoButton;
  private ImageButton speakerButton;
  private ImageButton switchCameraButton;

  private final MediatorLiveData<Boolean> videoConfigChanged = new MediatorLiveData<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // TODO: check if we need landscape layout
    setContentView(R.layout.activity_call);

    setupWindowFlags();

    initializeViews();

    setupInsets();

    if (!hasRequiredPermissions()) {
      requestRequiredPermissions();
      return;
    }

    // TODO: permission check
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//      checkFullScreenIntentPermission();
//    }

    // PiP listener
    addOnPictureInPictureModeChangedListener(pipModeInfo -> {
      boolean isInPipMode = pipModeInfo.isInPictureInPictureMode();

      if (isInPipMode) {
        topBar.setVisibility(View.GONE);
        bottomSheetContainer.setVisibility(View.GONE);
        localVideoContainer.setVisibility(View.GONE);
        incomingCallPrompt.setVisibility(View.GONE);
      } else {
        topBar.setVisibility(View.VISIBLE);
        bottomSheetContainer.setVisibility(View.VISIBLE);
        localVideoContainer.setVisibility(View.VISIBLE);
        if (viewModel != null) {
          CallViewModel.CallState state = viewModel.getCallState().getValue();
          if (state != null) {
            updateUIForState(state);
          }
        }
      }
    });

    initializeViewModel();

    handleIntents(getIntent());
  }

  private void handleIntents(Intent intent) {
    if (intent == null || viewModel == null) {
      return;
    }

    String action = intent.getAction();
    Log.d(TAG, "handleIntents: action=" + action);

    // Get coordinator to check call state
    CallCoordinator coordinator = CallCoordinator.getInstance(getApplication());

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

    boolean autoAnswer = intent.getBooleanExtra(EXTRA_AUTO_ANSWER, false);

    if (coordinator.isIncomingCall()) {
      if (autoAnswer) {
        // Notification answer
        Log.d(TAG, "Auto-answering call (WebRTC only)");
        viewModel.answerCallWhenReady();
      }
      // else: Wait for user to click Answer button
    } else {
      Log.d(TAG, "Starting outgoing call");
      viewModel.startOutgoingCallWhenReady();
    }
  }

  private void setupInsets() {
    View rootView = findViewById(R.id.call_root_view);

    ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
      Insets systemBars = windowInsets.getInsets(
        WindowInsetsCompat.Type.systemBars()
      );

      v.setPadding(
        systemBars.left,
        systemBars.top,
        systemBars.right,
        systemBars.bottom
      );

      return windowInsets;
    });
  }

  private void setupWindowFlags() {
    WindowCompat.enableEdgeToEdge(getWindow());

    // Show when locked
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true);
      setTurnScreenOn(true);

      KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
      if (keyguardManager != null) {
        keyguardManager.requestDismissKeyguard(this, null);
      }
    } else {
      getWindow().addFlags(
        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
          WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
          WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
      );
    }

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    // Control status bar icon colors
    WindowInsetsControllerCompat controller =
      WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
    controller.setAppearanceLightStatusBars(false);
    controller.setAppearanceLightNavigationBars(false);
  }

  private void initializeViews() {
    topBar = findViewById(R.id.top_bar);
    bottomSheetContainer = findViewById(R.id.bottom_sheet_container);
    localVideoContainer = findViewById(R.id.local_video_container);

    localVideoView = findViewById(R.id.local_video_view);
    remoteVideoView = findViewById(R.id.remote_video_view);

    statusText = findViewById(R.id.status_text);
    displayNameText = findViewById(R.id.display_name_text);
    incomingCallPrompt = findViewById(R.id.incoming_call_prompt);

    callerIconContainer = findViewById(R.id.caller_icon_container);
    callerIconView = findViewById(R.id.caller_icon);

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
    answerButton.setOnClickListener(v -> {
      if (viewModel != null) {
        viewModel.answerCall();
      }
    });

    declineButton.setOnClickListener(v -> {
      if (viewModel != null) {
        viewModel.declineCall();
      }
      finish();
    });

    endCallButton.setOnClickListener(v -> {
      if (viewModel != null) {
        viewModel.hangUp();
      }
      finish();
    });

    muteButton.setOnClickListener(v -> {
      if (viewModel != null) {
        viewModel.toggleAudio();
      }
    });

    videoButton.setOnClickListener(v -> {
      if (viewModel != null) {
        viewModel.toggleVideo();
      }
    });

    speakerButton.setOnClickListener(v -> {
      if (viewModel != null) {
        showAudioDevicePicker();
      }
    });

    switchCameraButton.setOnClickListener(v -> {
      if (viewModel != null) {
        viewModel.switchCamera();
      }
    });

    getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
      @Override
      public void handleOnBackPressed() {
        CallViewModel.CallState state = viewModel.getCallState().getValue();

        // Only enter PiP if call is active and device is unlocked
        if (state == CallViewModel.CallState.CONNECTED && !isDeviceLocked()) {
          enterPictureInPictureMode(createPipParams());
        } else {
          finish();
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
    viewModel.getDisplayName().observe(this, name -> {
      displayNameText.setText(name != null ? name : "Unknown");
    });

    viewModel.getDisplayIcon().observe(this, icon -> {
      if (icon != null && callerIconView != null) {
        callerIconView.setImageIcon(icon);
      }
    });

    // Audio/video state
    viewModel.getAudioEnabled().observe(this, enabled -> {
      muteButton.setSelected(!enabled);
      muteButton.setImageResource(enabled ?
        R.drawable.ic_mic_on : R.drawable.ic_mic_off);
    });

    viewModel.getVideoEnabled().observe(this, enabled -> {
      videoButton.setSelected(!enabled);
      videoButton.setImageResource(enabled ?
        R.drawable.ic_videocam_on : R.drawable.ic_videocam_off);
    });

    viewModel.getCurrentAudioEndpoint().observe(this, this::updateSpeakerButton);

    viewModel.getAvailableAudioEndpoints().observe(this, endpoints -> {
      // Need observe to trigger flow emit
      Log.d(TAG, "Available endpoints updated, count: " +
        (endpoints != null ? endpoints.size() : 0));
    });

    // Errors
    viewModel.getErrorMessage().observe(this, error -> {
      if (error != null && !error.isEmpty()) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
      }
    });

    // Relay usage
    viewModel.getIsRelayUsed().observe(this, isRelayUsed -> {
      if (isRelayUsed != null) {
        Log.d(TAG, "TURN relay is " + (isRelayUsed ? "being used" : "NOT being used"));
      }
    });

    // Set up LiveData sources
    videoConfigChanged.addSource(viewModel.getCallState(), v -> videoConfigChanged.setValue(true));
    videoConfigChanged.addSource(viewModel.getLocalVideoTrack(), v -> videoConfigChanged.setValue(true));
    videoConfigChanged.addSource(viewModel.getRemoteVideoTrack(), v -> videoConfigChanged.setValue(true));
    videoConfigChanged.addSource(viewModel.getVideoEnabled(), v -> videoConfigChanged.setValue(true));

    // Video layout
    videoConfigChanged.observe(this, changed -> {
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

  // TODO: resource strings
  private void updateUIForState(CallViewModel.CallState state) {
    Log.d(TAG, "Call state: " + state);

    switch (state) {
      case INITIALIZING:
        statusText.setText("Initializing...");
        incomingCallPrompt.setVisibility(View.GONE);
        bottomSheetContainer.setVisibility(View.GONE);
        callerIconContainer.setVisibility(View.GONE);
        answerModeSelector.setVisibility(View.GONE);
        break;

      case PROMPTING_USER_ACCEPT:
        statusText.setText("Incoming call");
        incomingCallPrompt.setVisibility(View.VISIBLE);
        bottomSheetContainer.setVisibility(View.GONE);
        callerIconContainer.setVisibility(View.VISIBLE);
        answerModeSelector.setVisibility(View.VISIBLE);
        initializeAnswerModeSelector();
        break;

      case RINGING:
        statusText.setText("Ringing...");
        incomingCallPrompt.setVisibility(View.GONE);
        bottomSheetContainer.setVisibility(View.VISIBLE);
        callerIconContainer.setVisibility(View.GONE);
        answerModeSelector.setVisibility(View.GONE);
        break;

      case CONNECTING:
        statusText.setText("Connecting...");
        incomingCallPrompt.setVisibility(View.GONE);
        bottomSheetContainer.setVisibility(View.VISIBLE);
        callerIconContainer.setVisibility(View.GONE);
        answerModeSelector.setVisibility(View.GONE);
        break;

      case CONNECTED:
        statusText.setText("Connected");
        incomingCallPrompt.setVisibility(View.GONE);
        bottomSheetContainer.setVisibility(View.VISIBLE);
        callerIconContainer.setVisibility(View.GONE);
        answerModeSelector.setVisibility(View.GONE);
        break;

      case RECONNECTING:
        statusText.setText("Reconnecting...");
        break;

      case ENDED:
        statusText.setText("Call ended");
        finish();
        break;

      case ERROR:
        statusText.setText("Call failed");
        incomingCallPrompt.setVisibility(View.GONE);
        bottomSheetContainer.setVisibility(View.GONE);
        callerIconContainer.setVisibility(View.GONE);
        answerModeSelector.setVisibility(View.GONE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
          if (!isFinishing()) {
            finish();
          }
        }, 2500);
        break;
    }
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
    AudioDevicePickerDialog dialog = new AudioDevicePickerDialog(
      this,
      endpoints,
      currentEndpoint,
      selectedEndpoint -> {
        viewModel.selectAudioDevice(selectedEndpoint);
      }
    );

    dialog.show();
  }

  private void initializeAnswerModeSelector() {
    CallCoordinator coordinator = CallCoordinator.getInstance(getApplication());

    // Set initial selection without triggering listener
    answerModeSelector.clearOnButtonCheckedListeners();
    answerModeSelector.check(coordinator.isStartsWithVideo() ?
      R.id.answer_video_button : R.id.answer_audio_only_button);

    // Set listener
    answerModeSelector.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
      if (isChecked && viewModel != null) {
        boolean startsWithVideo = (checkedId == R.id.answer_video_button);
        viewModel.setStartsWithVideo(startsWithVideo);
        Log.d(TAG, "Answer mode changed to: " + (startsWithVideo ? "Video" : "Audio"));
      }
    });
  }

  private void layoutVideos() {
    if (isFinishing() || isDestroyed()) return;
    if (viewModel == null) return;

    CallViewModel.CallState state = viewModel.getCallState().getValue();
    if (state == CallViewModel.CallState.ENDED ||
      state == CallViewModel.CallState.ERROR) return;

    detachAllTracks();

    Boolean videoEnabled = viewModel.getVideoEnabled().getValue();
    CallCoordinator coordinator = CallCoordinator.getInstance(getApplication());
    VideoTrack localTrack = viewModel.getLocalVideoTrack().getValue();
    VideoTrack remoteTrack = viewModel.getRemoteVideoTrack().getValue();

    if (state == CallViewModel.CallState.CONNECTED) {
      // Call established: local in corner, remote in center

      if (remoteTrack != null) {
        remoteTrack.addSink(remoteVideoView);
      }

      if (localTrack != null && Boolean.TRUE.equals(videoEnabled)) {
        localTrack.addSink(localVideoView);
        localVideoContainer.setVisibility(View.VISIBLE);
      } else {
        localVideoContainer.setVisibility(View.GONE);
      }

      Log.d(TAG, "Video layout: Connected (local corner, remote full-screen)");

    } else if (!coordinator.isIncomingCall() &&
      localTrack != null &&
      Boolean.TRUE.equals(videoEnabled)) {
      // Outgoing call before connected: local preview in center, hide corner
      localTrack.addSink(remoteVideoView);
      localVideoContainer.setVisibility(View.GONE);

      Log.d(TAG, "Video layout: Outgoing preview (local full-screen)");

    } else {
      // All other cases: no video shown
      localVideoContainer.setVisibility(View.GONE);

      Log.d(TAG, "Video layout: No video");
    }
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
      == PackageManager.PERMISSION_GRANTED &&
      ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        == PackageManager.PERMISSION_GRANTED;
  }

  private void requestRequiredPermissions() {
    ActivityCompat.requestPermissions(
      this,
      new String[] {
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
      },
      PERMISSION_REQUEST_CODE
    );
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode == PERMISSION_REQUEST_CODE) {
      boolean allGranted = true;
      for (int result : grantResults) {
        if (result != PackageManager.PERMISSION_GRANTED) {
          allGranted = false;
          break;
        }
      }

      if (allGranted) {
        initializeViewModel();
      } else {
        Toast.makeText(this, "Camera and microphone permissions required",
          Toast.LENGTH_LONG).show();
        finish();
      }
    }
  }

  // Picture-in-Picture

  @Override
  public void onUserLeaveHint() {
    super.onUserLeaveHint();

    // Enter PiP mode when user presses home button during active call
    CallViewModel.CallState state = viewModel.getCallState().getValue();
    if (state == CallViewModel.CallState.CONNECTED) {
        enterPictureInPictureMode(createPipParams());
    }
  }

   private PictureInPictureParams createPipParams() {
     Rational aspectRatio = new Rational(9, 16);
     PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder()
       .setAspectRatio(aspectRatio)
       .setActions(new ArrayList<>());

     // FIXME: PiP currently shows media controls.
     // Will fix later: to fix this, we may need changes to audio playback implementation.

     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
       builder.setAutoEnterEnabled(true);
     }

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
