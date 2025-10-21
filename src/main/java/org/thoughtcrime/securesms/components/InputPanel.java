package org.thoughtcrime.securesms.components;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.animation.AnimationCompleteListener;
import org.thoughtcrime.securesms.components.emoji.EmojiToggle;
import org.thoughtcrime.securesms.components.emoji.MediaKeyboard;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.mms.QuoteModel;
import org.thoughtcrime.securesms.mms.SlideDeck;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.concurrent.AssertedSuccessListener;
import org.thoughtcrime.securesms.util.guava.Optional;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import chat.delta.util.ListenableFuture;
import chat.delta.util.SettableFuture;

public class InputPanel extends ConstraintLayout
  implements MicrophoneRecorderView.Listener,
             KeyboardAwareLinearLayout.OnKeyboardShownListener,
             MediaKeyboard.MediaKeyboardListener
{

  private static final String TAG = InputPanel.class.getSimpleName();

  private static final int FADE_TIME = 150;

  private QuoteView       quoteView;
  private EmojiToggle     emojiToggle;
  private ComposeText     composeText;
  private View            quickCameraToggle;
  private View            quickAudioToggle;
  private View            buttonToggle;
  private View            recordingContainer;
  private View            recordLockCancel;

  private MicrophoneRecorderView microphoneRecorderView;
  private SlideToCancel          slideToCancel;
  private RecordTime             recordTime;
  private ValueAnimator quoteAnimator;

  private @Nullable Listener listener;

  public InputPanel(Context context) {
    super(context);
  }

  public InputPanel(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public InputPanel(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  public void onFinishInflate() {
    super.onFinishInflate();

    View quoteDismiss           = findViewById(R.id.quote_dismiss);

    this.quoteView              = findViewById(R.id.quote_view);
    this.emojiToggle            = findViewById(R.id.emoji_toggle);
    this.composeText            = findViewById(R.id.embedded_text_editor);
    this.quickCameraToggle      = findViewById(R.id.quick_camera_toggle);
    this.quickAudioToggle       = findViewById(R.id.quick_audio_toggle);
    this.buttonToggle           = findViewById(R.id.button_toggle);
    this.recordingContainer     = findViewById(R.id.recording_container);
    this.recordLockCancel       = findViewById(R.id.record_cancel);
    this.recordTime             = new RecordTime(findViewById(R.id.record_time));
    this.slideToCancel          = new SlideToCancel(findViewById(R.id.slide_to_cancel));
    this.microphoneRecorderView = findViewById(R.id.recorder_view);
    this.microphoneRecorderView.setListener(this);

    this.recordLockCancel.setOnClickListener(v -> microphoneRecorderView.cancelAction());

    quoteDismiss.setOnClickListener(v -> clearQuote());
  }

  public void setListener(final @NonNull Listener listener) {
    this.listener = listener;

    emojiToggle.setOnClickListener(v -> listener.onEmojiToggle());
  }

  public void setMediaListener(@NonNull MediaListener listener) {
    composeText.setMediaListener(listener);
  }

  public void setQuote(@NonNull GlideRequests glideRequests,
                       @NonNull DcMsg msg,
                       long id,
                       @NonNull Recipient author,
                       @NonNull CharSequence body,
                       @NonNull SlideDeck attachments,
                       @NonNull boolean isEdit)
  {
   this.quoteView.setQuote(glideRequests, msg, author, body, attachments, false, isEdit);

    int originalHeight = this.quoteView.getVisibility() == VISIBLE ? this.quoteView.getMeasuredHeight()
            : 0;

    this.quoteView.setVisibility(VISIBLE);
    this.quoteView.measure(0, 0);

    if (quoteAnimator != null) {
      quoteAnimator.cancel();
    }

    quoteAnimator = createHeightAnimator(quoteView, originalHeight, this.quoteView.getMeasuredHeight(), null);

    quoteAnimator.start();
  }

  public void clearQuoteWithoutAnimation() {
    quoteView.dismiss();
    if (listener != null) listener.onQuoteDismissed();
  }

  public void clearQuote() {
    if (quoteAnimator != null) {
      quoteAnimator.cancel();
    }

    quoteAnimator = createHeightAnimator(quoteView, quoteView.getMeasuredHeight(), 0, new AnimationCompleteListener() {
      @Override
      public void onAnimationEnd(@NonNull Animator animation) {
        quoteView.dismiss();
        if (listener != null) listener.onQuoteDismissed();
      }
    });

    quoteAnimator.start();
  }

  private static ValueAnimator createHeightAnimator(@NonNull View view,
                                                    int originalHeight,
                                                    int finalHeight,
                                                    @Nullable AnimationCompleteListener onAnimationComplete)
  {
    ValueAnimator animator = ValueAnimator.ofInt(originalHeight, finalHeight)
            .setDuration(200);

    animator.addUpdateListener(animation -> {
      ViewGroup.LayoutParams params = view.getLayoutParams();
      params.height = Math.max(1, (int) animation.getAnimatedValue());
      view.setLayoutParams(params);
    });

    if (onAnimationComplete != null) {
      animator.addListener(onAnimationComplete);
    }

    return animator;
  }

  public Optional<QuoteModel> getQuote() {
    if (quoteView.getVisibility() == View.VISIBLE && quoteView.getBody() != null) {
      return Optional.of(new QuoteModel(
              quoteView.getDcContact(), quoteView.getBody().toString(),
              quoteView.getAttachments(), quoteView.getOriginalMsg()
      ));
    } else {
      return Optional.absent();
    }
  }

  public void clickOnComposeInput() {
    composeText.performClick();
  }

  public void setMediaKeyboard(@NonNull MediaKeyboard mediaKeyboard) {
    mediaKeyboard.setKeyboardListener(this);
  }

  @Override
  public void onRecordPermissionRequired() {
    if (listener != null) listener.onRecorderPermissionRequired();
  }

  @Override
  public void onRecordPressed() {
    if (listener != null) listener.onRecorderStarted();
    recordTime.display();
    slideToCancel.display();

    ViewUtil.fadeOut(emojiToggle, FADE_TIME, View.INVISIBLE);
    ViewUtil.fadeOut(composeText, FADE_TIME, View.INVISIBLE);
    ViewUtil.fadeOut(quickCameraToggle, FADE_TIME, View.INVISIBLE);
    ViewUtil.fadeOut(quickAudioToggle, FADE_TIME, View.INVISIBLE);
    buttonToggle.animate().alpha(0).setDuration(FADE_TIME).start();
  }

  @Override
  public void onRecordReleased() {
    long elapsedTime = onRecordHideEvent();

    if (listener != null) {
      Log.d(TAG, "Elapsed time: " + elapsedTime);
      if (elapsedTime > 1000) {
        listener.onRecorderFinished();
      } else {
        Toast.makeText(getContext(), R.string.chat_record_explain, Toast.LENGTH_LONG).show();
        listener.onRecorderCanceled();
      }
    }
  }

  @Override
  public void onRecordMoved(float offsetX, float absoluteX) {
    slideToCancel.moveTo(offsetX);

    float position  = absoluteX / recordingContainer.getWidth();

    if (ViewUtil.isLtr(this) && position <= 0.5 ||
        ViewUtil.isRtl(this) && position >= 0.6)
    {
      this.microphoneRecorderView.cancelAction();
    }
  }

  @Override
  public void onRecordCanceled() {
    onRecordHideEvent();
    if (listener != null) listener.onRecorderCanceled();
  }

  @Override
  public void onRecordLocked() {
    slideToCancel.hide();
    recordLockCancel.setVisibility(View.VISIBLE);
    buttonToggle.animate().alpha(1).setDuration(FADE_TIME).start();
    if (listener != null) listener.onRecorderLocked();
  }

  public void onPause() {
    this.microphoneRecorderView.cancelAction();
  }

  public void setEnabled(boolean enabled) {
    composeText.setEnabled(enabled);
    emojiToggle.setEnabled(enabled);
    quickAudioToggle.setEnabled(enabled);
    quickCameraToggle.setEnabled(enabled);
  }

  private long onRecordHideEvent() {
    recordLockCancel.setVisibility(View.GONE);

    ListenableFuture<Void> future      = slideToCancel.hide();
    long                   elapsedTime = recordTime.hide();

    future.addListener(new AssertedSuccessListener<Void>() {
      @Override
      public void onSuccess(Void result) {
        ViewUtil.fadeIn(emojiToggle, FADE_TIME);
        ViewUtil.fadeIn(composeText, FADE_TIME);
        ViewUtil.fadeIn(quickCameraToggle, FADE_TIME);
        ViewUtil.fadeIn(quickAudioToggle, FADE_TIME);
        buttonToggle.animate().alpha(1).setDuration(FADE_TIME).start();
        composeText.requestFocus();
      }
    });

    return elapsedTime;
  }

  @Override
  public void onKeyboardShown() {
    emojiToggle.setToMedia();
  }

  public boolean isRecordingInLockedMode() {
    return microphoneRecorderView.isRecordingLocked();
  }

  public void releaseRecordingLock() {
    microphoneRecorderView.unlockAction();
  }

  @Override
  public void onShown() {
    emojiToggle.setToIme();
  }

  @Override
  public void onHidden() {
    emojiToggle.setToMedia();
  }

  @Override
  public void onEmojiPicked(String emoji) {
    final int start = composeText.getSelectionStart();
    final int end   = composeText.getSelectionEnd();

    composeText.getText().replace(Math.min(start, end), Math.max(start, end), emoji);
    composeText.setSelection(start + emoji.length());
  }

  public interface Listener {
    void onRecorderStarted();
    void onRecorderLocked();
    void onRecorderFinished();
    void onRecorderCanceled();
    void onRecorderPermissionRequired();
    void onEmojiToggle();
    void onQuoteDismissed();
  }

  private static class SlideToCancel {

    private final View slideToCancelView;

    SlideToCancel(View slideToCancelView) {
      this.slideToCancelView = slideToCancelView;
    }

    public void display() {
      ViewUtil.fadeIn(this.slideToCancelView, FADE_TIME);
    }

    public ListenableFuture<Void> hide() {
      final SettableFuture<Void> future = new SettableFuture<>();

      AnimationSet animation = new AnimationSet(true);
      animation.addAnimation(new TranslateAnimation(Animation.ABSOLUTE, slideToCancelView.getTranslationX(),
                                                    Animation.ABSOLUTE, 0,
                                                    Animation.RELATIVE_TO_SELF, 0,
                                                    Animation.RELATIVE_TO_SELF, 0));
      animation.addAnimation(new AlphaAnimation(1, 0));

      animation.setDuration(MicrophoneRecorderView.ANIMATION_DURATION);
      animation.setFillBefore(true);
      animation.setFillAfter(false);

      slideToCancelView.postDelayed(() -> future.set(null), MicrophoneRecorderView.ANIMATION_DURATION);
      slideToCancelView.setVisibility(View.GONE);
      slideToCancelView.startAnimation(animation);

      return future;
    }

    void moveTo(float offset) {
      Animation animation = new TranslateAnimation(Animation.ABSOLUTE, offset,
                                                   Animation.ABSOLUTE, offset,
                                                   Animation.RELATIVE_TO_SELF, 0,
                                                   Animation.RELATIVE_TO_SELF, 0);

      animation.setDuration(0);
      animation.setFillAfter(true);
      animation.setFillBefore(true);

      slideToCancelView.startAnimation(animation);
    }
  }

  private static class RecordTime implements Runnable {

    private final TextView recordTimeView;
    private final AtomicLong startTime = new AtomicLong(0);
    private final int UPDATE_EVERY_MS = 99;

    private RecordTime(TextView recordTimeView) {
      this.recordTimeView = recordTimeView;
    }

    public void display() {
      this.startTime.set(System.currentTimeMillis());
      this.recordTimeView.setText(formatElapsedTime(0));
      ViewUtil.fadeIn(this.recordTimeView, FADE_TIME);
      Util.runOnMainDelayed(this, UPDATE_EVERY_MS);
    }

    public long hide() {
      long elapsedtime = System.currentTimeMillis() - startTime.get();
      this.startTime.set(0);
      ViewUtil.fadeOut(this.recordTimeView, FADE_TIME, View.INVISIBLE);
      return elapsedtime;
    }

    @Override
    public void run() {
      long localStartTime = startTime.get();
      if (localStartTime > 0) {
        long elapsedTime = System.currentTimeMillis() - localStartTime;
        recordTimeView.setText(formatElapsedTime(elapsedTime));
        Util.runOnMainDelayed(this, UPDATE_EVERY_MS);
      }
    }

    private String formatElapsedTime(long ms)
    {
      return DateUtils.formatElapsedTime(TimeUnit.MILLISECONDS.toSeconds(ms))
              + String.format(".%01d", ((ms/100)%10));

    }
  }

  public interface MediaListener {
    void onMediaSelected(@NonNull Uri uri, String contentType);
  }
}
