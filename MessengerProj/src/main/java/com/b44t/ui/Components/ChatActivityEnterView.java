/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *                        (C) 2013-2016 Nikolai Kudashov
 *                           (C) 2017 Björn Petersen
 *                    Contact: r10s@b44t.com, http://b44t.com
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see http://www.gnu.org/licenses/ .
 *
 ******************************************************************************/


package com.b44t.ui.Components;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.Emoji;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MediaController;
import com.b44t.messenger.MessageObject;
import com.b44t.messenger.MessagesController;
import com.b44t.messenger.SendMessagesHelper;
import com.b44t.messenger.FileLog;
import com.b44t.messenger.NotificationCenter;
import com.b44t.messenger.R;
import com.b44t.messenger.TLRPC;
import com.b44t.messenger.UserConfig;
import com.b44t.messenger.AnimatorListenerAdapterProxy;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.ui.ActionBar.Theme;
import com.b44t.ui.ChatActivity;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class ChatActivityEnterView extends FrameLayout implements NotificationCenter.NotificationCenterDelegate, SizeNotifierFrameLayout.SizeNotifierFrameLayoutDelegate, StickersAlert.StickersAlertDelegate {

    public interface ChatActivityEnterViewDelegate {
        void onMessageSend(CharSequence message);
        void needSendTyping();
        void onTextChanged(CharSequence text, boolean bigChange);
        void onWindowSizeChanged(int size);
        void onStickersTab(boolean opened);
    }

    private class SeekBarWaveformView extends View {

        private SeekBarWaveform seekBarWaveform;

        public SeekBarWaveformView(Context context) {
            super(context);
            seekBarWaveform = new SeekBarWaveform(context);
            seekBarWaveform.setColors(0xffa2cef8, 0xffffffff, 0xffa2cef8);
            seekBarWaveform.setDelegate(new SeekBar.SeekBarDelegate() {
                @Override
                public void onSeekBarDrag(float progress) {
                    if (audioToSendMessageObject != null) {
                        audioToSendMessageObject.audioProgress = progress;
                        MediaController.getInstance().seekToProgress(audioToSendMessageObject, progress);
                    }
                }
            });
        }

        public void setWaveform(byte[] waveform) {
            seekBarWaveform.setWaveform(waveform);
            invalidate();
        }

        public void setProgress(float progress) {
            seekBarWaveform.setProgress(progress);
            invalidate();
        }

        public boolean isDragging() {
            return seekBarWaveform.isDragging();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            boolean result = seekBarWaveform.onTouch(event.getAction(), event.getX(), event.getY());
            if (result) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    requestDisallowInterceptTouchEvent(true);
                }
                invalidate();
            }
            return result || super.onTouchEvent(event);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            seekBarWaveform.setSize(right - left, bottom - top);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            seekBarWaveform.draw(canvas);
        }
    }

    private class EditTextCaption extends EditText {

        private Object editor;
        private Field editorField;
        private Drawable[] mCursorDrawable;
        private Field mCursorDrawableField;

        public EditTextCaption(Context context) {
            super(context);

            try {
                Field field = TextView.class.getDeclaredField("mEditor");
                field.setAccessible(true);
                editor = field.get(this);
                Class editorClass = Class.forName("android.widget.Editor");
                editorField = editorClass.getDeclaredField("mShowCursor");
                editorField.setAccessible(true);
                mCursorDrawableField = editorClass.getDeclaredField("mCursorDrawable");
                mCursorDrawableField.setAccessible(true);
                mCursorDrawable = (Drawable[]) mCursorDrawableField.get(editor);
            } catch (Throwable e) {
            }
        }

        @SuppressLint("DrawAllocation")
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            try {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            } catch (Exception e) {
                setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(51));
                FileLog.e("messenger", e);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            try {
                super.onDraw(canvas);
            } catch (Exception e) {
            }

            try {
                // the following lines are because otherwise the cursor stops blinking if
                // the focus was set to another text field in between (eg. search)
                if (editorField != null && mCursorDrawable != null && mCursorDrawable[0] != null) {
                    long mShowCursor = editorField.getLong(editor);
                    boolean showCursor = (SystemClock.uptimeMillis() - mShowCursor) % (2 * 500) < 500;
                    if (showCursor) {
                        canvas.save();
                        canvas.translate(0, getPaddingTop());
                        mCursorDrawable[0].draw(canvas);
                        canvas.restore();
                    }
                }
            } catch (Throwable e) {
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            // show the keyboard again if it was hidden by the emoji view
            if (isPopupShowing() && event.getAction() == MotionEvent.ACTION_DOWN) {
                showPopup(AndroidUtilities.usingHardwareInput ? 0 : 2, 0);
                openKeyboardInternal();
            }
            try {
                return super.onTouchEvent(event);
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
            return false;
        }
    }

    private EditTextCaption messageEditText;
    private ImageView sendButton;
    private ImageView emojiButton;
    private EmojiView emojiView;
    private TextView recordTimeText;
    private ImageView audioRecordButton;
    private FrameLayout recordPanel;
    private FrameLayout recordedAudioPanel;
    private SeekBarWaveformView recordedAudioSeekBar;
    private ImageView recordedAudioPlayButton;
    private TextView recordedAudioTimeTextView;
    private LinearLayout slideText;
    private RecordDot recordDot;
    private SizeNotifierFrameLayout sizeNotifierLayout;
    private LinearLayout attachButton;
    private LinearLayout textFieldContainer;
    private FrameLayout sendButtonContainer;
    private RecordCircle recordCircle;
    private Drawable backgroundDrawable;

    private int currentPopupContentType = -1;

    private boolean isPaused = true;
    private boolean showKeyboardOnResume;

    private PowerManager.WakeLock mWakeLock;
    private AnimatorSet runningAnimation;
    private AnimatorSet runningAnimation2;
    private AnimatorSet runningAnimationAudio;
    private int runningAnimationType;
    private int audioInterfaceState;

    private int keyboardHeight;
    private int keyboardHeightLand;
    private boolean keyboardVisible;
    private int emojiPadding;
    private boolean sendByEnter;
    private String lastTimeString;
    private float startedDraggingX = -1;
    private float distCanMove = AndroidUtilities.dp(80);
    private boolean recordingAudio;
    private boolean allowStickers;
    private boolean allowGifs;

    private int lastSizeChangeValue1;
    private boolean lastSizeChangeValue2;

    private Activity parentActivity;
    private ChatActivity parentFragment;
    private long dialog_id;
    private boolean ignoreTextChange;
    private int innerTextChange;
    private MessageObject replyingMessageObject;
    private ChatActivityEnterViewDelegate delegate;

    private TLRPC.TL_document audioToSend;
    private String audioToSendPath;
    private MessageObject audioToSendMessageObject;

    private boolean waitingForKeyboardOpen;
    private Runnable openKeyboardRunnable = new Runnable() {
        @Override
        public void run() {
            if (messageEditText != null && waitingForKeyboardOpen && !keyboardVisible && !AndroidUtilities.usingHardwareInput) {
                messageEditText.requestFocus();
                AndroidUtilities.showKeyboard(messageEditText);
                AndroidUtilities.cancelRunOnUIThread(openKeyboardRunnable);
                AndroidUtilities.runOnUIThread(openKeyboardRunnable, 100);
            }
        }
    };

    private class RecordDot extends View {

        private Drawable dotDrawable;
        private float alpha;
        private long lastUpdateTime;
        private boolean isIncr;

        public RecordDot(Context context) {
            super(context);

            dotDrawable = getResources().getDrawable(R.drawable.rec);
        }

        public void resetAlpha() {
            alpha = 1.0f;
            lastUpdateTime = System.currentTimeMillis();
            isIncr = false;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            dotDrawable.setBounds(0, 0, AndroidUtilities.dp(11), AndroidUtilities.dp(11));
            dotDrawable.setAlpha((int) (255 * alpha));
            long dt = (System.currentTimeMillis() - lastUpdateTime);
            if (!isIncr) {
                alpha -= dt / 400.0f;
                if (alpha <= 0) {
                    alpha = 0;
                    isIncr = true;
                }
            } else {
                alpha += dt / 400.0f;
                if (alpha >= 1) {
                    alpha = 1;
                    isIncr = false;
                }
            }
            lastUpdateTime = System.currentTimeMillis();
            dotDrawable.draw(canvas);
            invalidate();
        }
    }

    private class RecordCircle extends View {

        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint paintRecord = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Drawable micDrawable;
        private float scale;
        private float amplitude;
        private float animateToAmplitude;
        private float animateAmplitudeDiff;
        private long lastUpdateTime;

        public RecordCircle(Context context) {
            super(context);
            paint.setColor(Theme.MSG_AUDIO_SEEKBAR_DARK_COLOR);
            paintRecord.setColor(0x0d000000);
            micDrawable = getResources().getDrawable(R.drawable.mic_pressed);
        }

        public void setAmplitude(double value) {
            animateToAmplitude = (float) Math.min(100, value) / 100.0f;
            animateAmplitudeDiff = (animateToAmplitude - amplitude) / 150.0f;
            lastUpdateTime = System.currentTimeMillis();
            invalidate();
        }

        public float getScale() {
            return scale;
        }

        public void setScale(float value) {
            scale = value;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int cx = getMeasuredWidth() / 2;
            int cy = getMeasuredHeight() / 2;
            float sc;
            float alpha;
            if (scale <= 0.5f) {
                alpha = sc = scale / 0.5f;
            } else if (scale <= 0.75f) {
                sc = 1.0f - (scale - 0.5f) / 0.25f * 0.1f;
                alpha = 1;
            } else {
                sc = 0.9f + (scale - 0.75f) / 0.25f * 0.1f;
                alpha = 1;
            }
            long dt = System.currentTimeMillis() - lastUpdateTime;
            if (animateToAmplitude != amplitude) {
                amplitude += animateAmplitudeDiff * dt;
                if (animateAmplitudeDiff > 0) {
                    if (amplitude > animateToAmplitude) {
                        amplitude = animateToAmplitude;
                    }
                } else {
                    if (amplitude < animateToAmplitude) {
                        amplitude = animateToAmplitude;
                    }
                }
                invalidate();
            }
            lastUpdateTime = System.currentTimeMillis();
            if (amplitude != 0) {
                canvas.drawCircle(getMeasuredWidth() / 2.0f, getMeasuredHeight() / 2.0f, (AndroidUtilities.dp(42) + AndroidUtilities.dp(20) * amplitude) * scale, paintRecord);
            }
            canvas.drawCircle(getMeasuredWidth() / 2.0f, getMeasuredHeight() / 2.0f, AndroidUtilities.dp(42) * sc, paint);
            micDrawable.setBounds(cx - micDrawable.getIntrinsicWidth() / 2, cy - micDrawable.getIntrinsicHeight() / 2, cx + micDrawable.getIntrinsicWidth() / 2, cy + micDrawable.getIntrinsicHeight() / 2);
            micDrawable.setAlpha((int) (255 * alpha));
            micDrawable.draw(canvas);
        }
    }

    public ChatActivityEnterView(Activity context, SizeNotifierFrameLayout parent, ChatActivity fragment, boolean isChat) {
        super(context);
        backgroundDrawable = context.getResources().getDrawable(R.drawable.compose_panel);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setWillNotDraw(false);

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.recordStarted);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.recordStartError);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.recordStopped);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.recordProgressChanged);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.closeChats);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioDidSent);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.emojiDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioRouteChanged);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioDidReset);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioProgressDidChanged);
        parentActivity = context;
        parentFragment = fragment;
        sizeNotifierLayout = parent;
        sizeNotifierLayout.setDelegate(this);
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        sendByEnter = preferences.getBoolean("send_by_enter", false);

        textFieldContainer = new LinearLayout(context);
        //textFieldContainer.setBackgroundColor(0xffffffff);
        textFieldContainer.setOrientation(LinearLayout.HORIZONTAL);
        addView(textFieldContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 0, 2, 0, 0));

        FrameLayout frameLayout = new FrameLayout(context);
        textFieldContainer.addView(frameLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        emojiButton = new ImageView(context);
        emojiButton.setImageResource(R.drawable.ic_msg_panel_smiles);
        emojiButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        emojiButton.setPadding(0, AndroidUtilities.dp(1), 0, 0);
        if (Build.VERSION.SDK_INT >= 21) {
            emojiButton.setBackgroundDrawable(Theme.createBarSelectorDrawable(Theme.INPUT_FIELD_SELECTOR_COLOR));
        }
        frameLayout.addView(emojiButton, LayoutHelper.createFrame(48, 48, Gravity.BOTTOM | Gravity.LEFT, 3, 0, 0, 0));
        emojiButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isPopupShowing() || currentPopupContentType != 0) {
                    showPopup(1, 0);
                } else {
                    openKeyboardInternal();
                    removeGifFromInputField();
                }
            }
        });

        messageEditText = new EditTextCaption(context);
        messageEditText.setHint(LocaleController.getString("TypeMessage", R.string.TypeMessage));

        int addImeFlag = 0, addInputType = 0;
        if( sendByEnter ) {
            // using IME_ACTION_SEND together with EditText over multiple lines is a little bit tricky:
            // - setSingleLine(true) and the missing TYPE_TEXT_FLAG_MULTI_LINE is needed to show the IME_ACTION_SEND-button
            // - setHorizontallyScrolling(false) forces the field to scroll vertically (see setMaxLines() below) instead of horizontally
            addImeFlag = EditorInfo.IME_ACTION_SEND;
            messageEditText.setSingleLine(true);
            addInputType = 0;
            messageEditText.setHorizontallyScrolling(false);
        }
        else {
            messageEditText.setSingleLine(false);
            addInputType = EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
        }
        messageEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI | addImeFlag);
        messageEditText.setInputType(messageEditText.getInputType() | EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES | addInputType);
        messageEditText.setMaxLines(4);
        messageEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        messageEditText.setGravity(Gravity.BOTTOM);
        messageEditText.setPadding(0, AndroidUtilities.dp(11), 0, AndroidUtilities.dp(12));
        messageEditText.setBackgroundDrawable(null);
        messageEditText.setTextColor(0xff000000);
        messageEditText.setHintTextColor(0xffb2b2b2);
        frameLayout.addView(messageEditText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM, 52, 0, isChat ? 50 : 2, 0));
        messageEditText.setOnKeyListener(new OnKeyListener() {

            boolean ctrlPressed = false;

            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (i == KeyEvent.KEYCODE_BACK && !keyboardVisible && isPopupShowing()) {
                    if (keyEvent.getAction() == 1) {
                        showPopup(0, 0);
                        removeGifFromInputField();
                    }
                    return true;
                } else if (i == KeyEvent.KEYCODE_ENTER && (ctrlPressed || sendByEnter) && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    sendMessage();
                    return true;
                } else if (i == KeyEvent.KEYCODE_CTRL_LEFT || i == KeyEvent.KEYCODE_CTRL_RIGHT) {
                    ctrlPressed = keyEvent.getAction() == KeyEvent.ACTION_DOWN;
                    return true;
                }
                return false;
            }
        });
        messageEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            boolean ctrlPressed = false;

            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEND) {
                    sendMessage();
                    return true;
                } else if (keyEvent != null && i == EditorInfo.IME_NULL) {
                    if ((ctrlPressed || sendByEnter) && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                        sendMessage();
                        return true;
                    } else if (i == KeyEvent.KEYCODE_CTRL_LEFT || i == KeyEvent.KEYCODE_CTRL_RIGHT) {
                        ctrlPressed = keyEvent.getAction() == KeyEvent.ACTION_DOWN;
                        return true;
                    }
                }
                return false;
            }
        });
        messageEditText.addTextChangedListener(new TextWatcher() {
            boolean processChange = false;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (innerTextChange == 1) {
                    return;
                }
                checkSendButton(true);
                if (delegate != null) {
                    if (!ignoreTextChange) {
                        delegate.onTextChanged(charSequence, before > count + 1 || (count - before) > 2);
                    }
                }
                if (innerTextChange != 2 && before != count && (count - before) > 1) {
                    processChange = true;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (innerTextChange != 0) {
                    return;
                }
                if (sendByEnter && editable.length() > 0 && editable.charAt(editable.length() - 1) == '\n') {
                    sendMessage();
                }
                if (processChange) {
                    ImageSpan[] spans = editable.getSpans(0, editable.length(), ImageSpan.class);
                    for (int i = 0; i < spans.length; i++) {
                        editable.removeSpan(spans[i]);
                    }
                    Emoji.replaceEmoji(editable, messageEditText.getPaint().getFontMetricsInt(), AndroidUtilities.dp(20), false);
                    processChange = false;
                }
            }
        });
        try {
            Field mCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            mCursorDrawableRes.setAccessible(true);
            mCursorDrawableRes.set(messageEditText, R.drawable.field_carret);
        } catch (Exception e) {
            //nothing to do
        }

        if (isChat) {
            attachButton = new LinearLayout(context);
            attachButton.setOrientation(LinearLayout.HORIZONTAL);
            attachButton.setEnabled(false);
            attachButton.setPivotX(AndroidUtilities.dp(48));
            frameLayout.addView(attachButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 48, Gravity.BOTTOM | Gravity.RIGHT));
        }

        recordedAudioPanel = new FrameLayout(context);
        recordedAudioPanel.setVisibility(audioToSend == null ? GONE : VISIBLE);
        recordedAudioPanel.setBackgroundColor(0xffffffff);
        recordedAudioPanel.setFocusable(true);
        recordedAudioPanel.setFocusableInTouchMode(true);
        recordedAudioPanel.setClickable(true);
        frameLayout.addView(recordedAudioPanel, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM));

        ImageView imageView = new ImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setImageResource(R.drawable.ic_ab_fwd_delete);
        recordedAudioPanel.addView(imageView, LayoutHelper.createFrame(48, 48));
        imageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
                if (playing != null && playing == audioToSendMessageObject) {
                    MediaController.getInstance().cleanupPlayer(true, true);
                }
                if (audioToSendPath != null) {
                    new File(audioToSendPath).delete();
                }
                hideRecordedAudioPanel();
                checkSendButton(true);
            }
        });

        View view = new View(context);
        view.setBackgroundResource(R.drawable.recorded);
        recordedAudioPanel.addView(view, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 32, Gravity.CENTER_VERTICAL | Gravity.LEFT, 48, 0, 0, 0));

        recordedAudioSeekBar = new SeekBarWaveformView(context);
        recordedAudioPanel.addView(recordedAudioSeekBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 32, Gravity.CENTER_VERTICAL | Gravity.LEFT, 48 + 44, 0, 52, 0));

        recordedAudioPlayButton = new ImageView(context);
        recordedAudioPlayButton.setImageResource(R.drawable.s_player_play_states);
        recordedAudioPlayButton.setScaleType(ImageView.ScaleType.CENTER);
        recordedAudioPanel.addView(recordedAudioPlayButton, LayoutHelper.createFrame(48, 48, Gravity.LEFT | Gravity.BOTTOM, 48, 0, 0, 0));
        recordedAudioPlayButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioToSend == null) {
                    return;
                }
                if (MediaController.getInstance().isMessageOnAir(audioToSendMessageObject) && !MediaController.getInstance().isAudioPaused()) {
                    MediaController.getInstance().pauseAudio(audioToSendMessageObject);
                    recordedAudioPlayButton.setImageResource(R.drawable.s_player_play_states);
                } else {
                    recordedAudioPlayButton.setImageResource(R.drawable.s_player_pause_states);
                    MediaController.getInstance().playAudio(audioToSendMessageObject);
                }
            }
        });

        recordedAudioTimeTextView = new TextView(context);
        recordedAudioTimeTextView.setTextColor(0xffffffff);
        recordedAudioTimeTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        recordedAudioTimeTextView.setText("0:13");
        recordedAudioPanel.addView(recordedAudioTimeTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 13, 0));

        recordPanel = new FrameLayout(context);
        recordPanel.setVisibility(GONE);
        recordPanel.setBackgroundColor(0xffffffff);
        frameLayout.addView(recordPanel, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM));

        slideText = new LinearLayout(context);
        slideText.setOrientation(LinearLayout.HORIZONTAL);
        recordPanel.addView(slideText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 30, 0, 0, 0));

        imageView = new ImageView(context);
        imageView.setImageResource(R.drawable.slidearrow);
        slideText.addView(imageView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 0, 1, 0, 0));

        TextView textView = new TextView(context);
        textView.setText(LocaleController.getString("SlideToCancel", R.string.SlideToCancel));
        textView.setTextColor(0xff999999);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        slideText.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 6, 0, 0, 0));

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setPadding(AndroidUtilities.dp(13), 0, 0, 0);
        linearLayout.setBackgroundColor(0xffffffff);
        recordPanel.addView(linearLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        recordDot = new RecordDot(context);
        linearLayout.addView(recordDot, LayoutHelper.createLinear(11, 11, Gravity.CENTER_VERTICAL, 0, 1, 0, 0));

        recordTimeText = new TextView(context);
        recordTimeText.setText("00:00");
        recordTimeText.setTextColor(0xff4d4c4b);
        recordTimeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        linearLayout.addView(recordTimeText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 6, 0, 0, 0));

        sendButtonContainer = new FrameLayout(context);
        textFieldContainer.addView(sendButtonContainer, LayoutHelper.createLinear(48, 48, Gravity.BOTTOM));

        audioRecordButton = new ImageView(context);
        audioRecordButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        audioRecordButton.setImageResource(R.drawable.mic);
        audioRecordButton.setBackgroundColor(0xffffffff);
        audioRecordButton.setSoundEffectsEnabled(false);
        audioRecordButton.setPadding(0, 0, AndroidUtilities.dp(4), 0);
        sendButtonContainer.addView(audioRecordButton, LayoutHelper.createFrame(48, 48));
        audioRecordButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    if (parentFragment != null) {
                        if (Build.VERSION.SDK_INT >= 23 && parentActivity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            parentActivity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 3);
                            return false;
                        }
                    }
                    startedDraggingX = -1;
                    MediaController.getInstance().startRecording(dialog_id, replyingMessageObject);
                    updateAudioRecordInterface();
                    audioRecordButton.getParent().requestDisallowInterceptTouchEvent(true);
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                    startedDraggingX = -1;
                    MediaController.getInstance().stopRecording(1);
                    recordingAudio = false;
                    updateAudioRecordInterface();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE && recordingAudio) {
                    float x = motionEvent.getX();
                    if (x < -distCanMove) {
                        MediaController.getInstance().stopRecording(0);
                        recordingAudio = false;
                        updateAudioRecordInterface();
                    }

                    x = x + audioRecordButton.getX();
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) slideText.getLayoutParams();
                    if (startedDraggingX != -1) {
                        float dist = (x - startedDraggingX);
                        recordCircle.setTranslationX(dist);
                        params.leftMargin = AndroidUtilities.dp(30) + (int) dist;
                        slideText.setLayoutParams(params);
                        float alpha = 1.0f + dist / distCanMove;
                        if (alpha > 1) {
                            alpha = 1;
                        } else if (alpha < 0) {
                            alpha = 0;
                        }
                        slideText.setAlpha(alpha);
                    }
                    if (x <= slideText.getX() + slideText.getWidth() + AndroidUtilities.dp(30)) {
                        if (startedDraggingX == -1) {
                            startedDraggingX = x;
                            distCanMove = (recordPanel.getMeasuredWidth() - slideText.getMeasuredWidth() - AndroidUtilities.dp(48)) / 2.0f;
                            if (distCanMove <= 0) {
                                distCanMove = AndroidUtilities.dp(80);
                            } else if (distCanMove > AndroidUtilities.dp(80)) {
                                distCanMove = AndroidUtilities.dp(80);
                            }
                        }
                    }
                    if (params.leftMargin > AndroidUtilities.dp(30)) {
                        params.leftMargin = AndroidUtilities.dp(30);
                        recordCircle.setTranslationX(0);
                        slideText.setLayoutParams(params);
                        slideText.setAlpha(1);
                        startedDraggingX = -1;
                    }
                }
                view.onTouchEvent(motionEvent);
                return true;
            }
        });

        recordCircle = new RecordCircle(context);
        recordCircle.setVisibility(GONE);
        sizeNotifierLayout.addView(recordCircle, LayoutHelper.createFrame(124, 124, Gravity.BOTTOM | Gravity.RIGHT, 0, 0, -36, -38));

        sendButton = new ImageView(context);
        sendButton.setVisibility(INVISIBLE);
        sendButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        sendButton.setImageResource(R.drawable.ic_send);
        sendButton.setSoundEffectsEnabled(false);
        sendButton.setScaleX(0.1f);
        sendButton.setScaleY(0.1f);
        sendButton.setAlpha(0.0f);
        sendButtonContainer.addView(sendButton, LayoutHelper.createFrame(48, 48));
        sendButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("emoji", Context.MODE_PRIVATE);
        keyboardHeight = sharedPreferences.getInt("kbd_height", AndroidUtilities.dp(200));
        keyboardHeightLand = sharedPreferences.getInt("kbd_height_land3", AndroidUtilities.dp(200));

        checkSendButton(false);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean result = super.drawChild(canvas, child, drawingTime);
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int top = 0;
        backgroundDrawable.setBounds(0, top, getMeasuredWidth(), getMeasuredHeight());
        backgroundDrawable.draw(canvas);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setAllowStickersAndGifs(boolean value, boolean value2) {
        if ((allowStickers != value || allowGifs != value2) && emojiView != null) {
            if (emojiView.getVisibility() == VISIBLE) {
                hidePopup(false);
            }
            sizeNotifierLayout.removeView(emojiView);
            emojiView = null;
        }
        allowStickers = value;
        allowGifs = value2;
    }

    public void setOpenGifsTabFirst() {
        createEmojiView();
        emojiView.loadGifRecent();
        emojiView.switchToGifRecent();
    }

    public boolean isTopViewVisible() {
        return false;
    }

    private void onWindowSizeChanged() {
        int size = sizeNotifierLayout.getHeight();
        if (!keyboardVisible) {
            size -= emojiPadding;
        }
        if (delegate != null) {
            delegate.onWindowSizeChanged(size);
        }
    }

    public void onDestroy() {
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.recordStarted);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.recordStartError);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.recordStopped);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.recordProgressChanged);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.closeChats);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioDidSent);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.emojiDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioRouteChanged);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioDidReset);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioProgressDidChanged);
        if (emojiView != null) {
            emojiView.onDestroy();
        }
        if (mWakeLock != null) {
            try {
                mWakeLock.release();
                mWakeLock = null;
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
        }
        if (sizeNotifierLayout != null) {
            sizeNotifierLayout.setDelegate(null);
        }
    }

    public void onPause() {
        isPaused = true;
        closeKeyboard();
    }

    public void onResume() {
        isPaused = false;
        if (showKeyboardOnResume) {
            showKeyboardOnResume = false;
            messageEditText.requestFocus();
            AndroidUtilities.showKeyboard(messageEditText);
            if (!AndroidUtilities.usingHardwareInput && !keyboardVisible) {
                waitingForKeyboardOpen = true;
                AndroidUtilities.cancelRunOnUIThread(openKeyboardRunnable);
                AndroidUtilities.runOnUIThread(openKeyboardRunnable, 100);
            }
        }
    }

    public void setDialogId(long id) {
        dialog_id = id;
        if ((int) dialog_id < 0) {
            if (attachButton != null) {
                updateFieldRight(attachButton.getVisibility() == VISIBLE ? 1 : 0);
            }
        }
    }

    private void hideRecordedAudioPanel() {
        audioToSendPath = null;
        audioToSend = null;
        audioToSendMessageObject = null;
        AnimatorSet AnimatorSet = new AnimatorSet();
        AnimatorSet.playTogether(
                ObjectAnimator.ofFloat(recordedAudioPanel, "alpha", 0.0f)
        );
        AnimatorSet.setDuration(200);
        AnimatorSet.addListener(new AnimatorListenerAdapterProxy() {
            @Override
            public void onAnimationEnd(Animator animation) {
                recordedAudioPanel.setVisibility(GONE);

            }
        });
        AnimatorSet.start();
    }

    private void sendMessage() {
        if (parentFragment != null) {
            String action;
            TLRPC.Chat currentChat;
            if ((int) dialog_id < 0) {
                currentChat = MessagesController.getInstance().getChat(-(int) dialog_id);
                /*if (currentChat != null && currentChat.participants_count > MessagesController.getInstance().groupBigSize) {
                    action = "bigchat_message";
                } else*/ {
                    action = "chat_message";
                }
            } else {
                action = "pm_message";
            }
        }
        if (audioToSend != null) {
            MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
            if (playing != null && playing == audioToSendMessageObject) {
                MediaController.getInstance().cleanupPlayer(true, true);
            }
            SendMessagesHelper.getInstance().sendMessageDocument(audioToSend, null, audioToSendPath, dialog_id, null);
            if (delegate != null) {
                delegate.onMessageSend(null);
            }
            hideRecordedAudioPanel();
            checkSendButton(true);
            return;
        }
        CharSequence message = messageEditText.getText();
        if (processSendingText(message)) {
            messageEditText.setText("");
            if (delegate != null) {
                delegate.onMessageSend(message);
            }
        }
    }

    public boolean processSendingText(CharSequence text) {
        text = AndroidUtilities.getTrimmedString(text);
        if (text.length() != 0) {
            SendMessagesHelper.getInstance().sendMessageText(text.toString(), dialog_id, null);
            return true;
        }
        return false;
    }

    private void checkSendButton(boolean animated) {
        if (isPaused) {
            animated = false;
        }
        CharSequence message = AndroidUtilities.getTrimmedString(messageEditText.getText());
        if (message.length() > 0 || audioToSend != null) {
            if (audioRecordButton.getVisibility() == VISIBLE ) {
                // hide audio-record-button and attach-button, show send-button
                if (animated) {
                    if(runningAnimationType == 1) {
                        return;
                    }
                    if (runningAnimation != null) {
                        runningAnimation.cancel();
                        runningAnimation = null;
                    }
                    if (runningAnimation2 != null) {
                        runningAnimation2.cancel();
                        runningAnimation2 = null;
                    }

                    if (attachButton != null) {
                        runningAnimation2 = new AnimatorSet();
                        runningAnimation2.playTogether(
                                ObjectAnimator.ofFloat(attachButton, "alpha", 0.0f),
                                ObjectAnimator.ofFloat(attachButton, "scaleX", 0.0f)
                        );
                        runningAnimation2.setDuration(100);
                        runningAnimation2.addListener(new AnimatorListenerAdapterProxy() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (runningAnimation2 != null && runningAnimation2.equals(animation)) {
                                    attachButton.setVisibility(GONE);
                                }
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                                if (runningAnimation2 != null && runningAnimation2.equals(animation)) {
                                    runningAnimation2 = null;
                                }
                            }
                        });
                        runningAnimation2.start();
                        updateFieldRight(0);
                    }

                    runningAnimation = new AnimatorSet();

                    ArrayList<Animator> animators = new ArrayList<>();
                    if (audioRecordButton.getVisibility() == VISIBLE) {
                        animators.add(ObjectAnimator.ofFloat(audioRecordButton, "scaleX", 0.1f));
                        animators.add(ObjectAnimator.ofFloat(audioRecordButton, "scaleY", 0.1f));
                        animators.add(ObjectAnimator.ofFloat(audioRecordButton, "alpha", 0.0f));
                    }
                    runningAnimationType = 1;

                    if( !sendByEnter ) {
                        animators.add(ObjectAnimator.ofFloat(sendButton, "scaleX", 1.0f));
                        animators.add(ObjectAnimator.ofFloat(sendButton, "scaleY", 1.0f));
                        animators.add(ObjectAnimator.ofFloat(sendButton, "alpha", 1.0f));
                        sendButton.setVisibility(VISIBLE);
                    }

                    runningAnimation.playTogether(animators);
                    runningAnimation.setDuration(150);
                    runningAnimation.addListener(new AnimatorListenerAdapterProxy() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (runningAnimation != null && runningAnimation.equals(animation)) {

                                if( !sendByEnter ) {
                                    sendButton.setVisibility(VISIBLE);
                                }

                                audioRecordButton.setVisibility(GONE);
                                runningAnimation = null;
                                runningAnimationType = 0;
                            }
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            if (runningAnimation != null && runningAnimation.equals(animation)) {
                                runningAnimation = null;
                            }
                        }
                    });
                    runningAnimation.start();
                } else {
                    audioRecordButton.setScaleX(0.1f);
                    audioRecordButton.setScaleY(0.1f);
                    audioRecordButton.setAlpha(0.0f);

                    if( !sendByEnter ) {
                        sendButton.setScaleX(1.0f);
                        sendButton.setScaleY(1.0f);
                        sendButton.setAlpha(1.0f);
                        sendButton.setVisibility(VISIBLE);
                    }

                    audioRecordButton.setVisibility(GONE);
                    if (attachButton != null) {
                        attachButton.setVisibility(GONE);
                        updateFieldRight(0);
                    }
                }
            }
        } else if (audioRecordButton.getVisibility() != VISIBLE ) {
            // hide send-button, show audio-record-button and attach-button
            if (animated) {
                if (runningAnimationType == 2) {
                    return;
                }

                if (runningAnimation != null) {
                    runningAnimation.cancel();
                    runningAnimation = null;
                }
                if (runningAnimation2 != null) {
                    runningAnimation2.cancel();
                    runningAnimation2 = null;
                }

                if (attachButton != null) {
                    attachButton.setVisibility(VISIBLE);
                    runningAnimation2 = new AnimatorSet();
                    runningAnimation2.playTogether(
                            ObjectAnimator.ofFloat(attachButton, "alpha", 1.0f),
                            ObjectAnimator.ofFloat(attachButton, "scaleX", 1.0f)
                    );
                    runningAnimation2.setDuration(100);
                    runningAnimation2.start();
                    updateFieldRight(1);
                }

                audioRecordButton.setVisibility(VISIBLE);
                runningAnimation = new AnimatorSet();
                runningAnimationType = 2;

                ArrayList<Animator> animators = new ArrayList<>();
                animators.add(ObjectAnimator.ofFloat(audioRecordButton, "scaleX", 1.0f));
                animators.add(ObjectAnimator.ofFloat(audioRecordButton, "scaleY", 1.0f));
                animators.add(ObjectAnimator.ofFloat(audioRecordButton, "alpha", 1.0f));
                animators.add(ObjectAnimator.ofFloat(sendButton, "scaleX", 0.1f));
                animators.add(ObjectAnimator.ofFloat(sendButton, "scaleY", 0.1f));
                animators.add(ObjectAnimator.ofFloat(sendButton, "alpha", 0.0f));

                runningAnimation.playTogether(animators);
                runningAnimation.setDuration(150);
                runningAnimation.addListener(new AnimatorListenerAdapterProxy() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (runningAnimation != null && runningAnimation.equals(animation)) {
                            sendButton.setVisibility(GONE);
                            audioRecordButton.setVisibility(VISIBLE);
                            runningAnimation = null;
                            runningAnimationType = 0;
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        if (runningAnimation != null && runningAnimation.equals(animation)) {
                            runningAnimation = null;
                        }
                    }
                });
                runningAnimation.start();
            } else {
                sendButton.setScaleX(0.1f);
                sendButton.setScaleY(0.1f);
                sendButton.setAlpha(0.0f);
                audioRecordButton.setScaleX(1.0f);
                audioRecordButton.setScaleY(1.0f);
                audioRecordButton.setAlpha(1.0f);
                sendButton.setVisibility(GONE);
                audioRecordButton.setVisibility(VISIBLE);
                if (attachButton != null) {
                    attachButton.setVisibility(VISIBLE);
                    updateFieldRight(1);
                }
            }
        }
    }

    private void updateFieldRight(int attachVisible) {
        if (messageEditText == null /*|| editingMessageObject != null*/) {
            return;
        }
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) messageEditText.getLayoutParams();
        if (attachVisible == 1) {
            layoutParams.rightMargin = AndroidUtilities.dp(50);
        } else if (attachVisible == 2) {
            if (layoutParams.rightMargin != AndroidUtilities.dp(2)) {
                layoutParams.rightMargin = AndroidUtilities.dp(50);
            }
        } else {
            layoutParams.rightMargin = AndroidUtilities.dp(2);
        }
        messageEditText.setLayoutParams(layoutParams);
    }

    private void updateAudioRecordInterface() {
        if (recordingAudio) {
            if (audioInterfaceState == 1) {
                return;
            }
            audioInterfaceState = 1;
            try {
                if (mWakeLock == null) {
                    PowerManager pm = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
                    mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "audio record lock");
                    mWakeLock.acquire();
                }
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
            AndroidUtilities.lockOrientation(parentActivity);

            recordPanel.setVisibility(VISIBLE);
            recordCircle.setVisibility(VISIBLE);
            recordCircle.setAmplitude(0);
            recordTimeText.setText("00:00");
            recordDot.resetAlpha();
            lastTimeString = null;

            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) slideText.getLayoutParams();
            params.leftMargin = AndroidUtilities.dp(30);
            slideText.setLayoutParams(params);
            slideText.setAlpha(1);
            recordPanel.setX(AndroidUtilities.displaySize.x);
            recordCircle.setTranslationX(0);
            if (runningAnimationAudio != null) {
                runningAnimationAudio.cancel();
            }
            runningAnimationAudio = new AnimatorSet();
            runningAnimationAudio.playTogether(ObjectAnimator.ofFloat(recordPanel, "translationX", 0),
                    ObjectAnimator.ofFloat(recordCircle, "scale", 1),
                    ObjectAnimator.ofFloat(audioRecordButton, "alpha", 0));
            runningAnimationAudio.setDuration(300);
            runningAnimationAudio.addListener(new AnimatorListenerAdapterProxy() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    if (runningAnimationAudio != null && runningAnimationAudio.equals(animator)) {
                        recordPanel.setX(0);
                        runningAnimationAudio = null;
                    }
                }
            });
            runningAnimationAudio.setInterpolator(new DecelerateInterpolator());
            runningAnimationAudio.start();
        } else {
            if (mWakeLock != null) {
                try {
                    mWakeLock.release();
                    mWakeLock = null;
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                }
            }
            AndroidUtilities.unlockOrientation(parentActivity);
            if (audioInterfaceState == 0) {
                return;
            }
            audioInterfaceState = 0;

            if (runningAnimationAudio != null) {
                runningAnimationAudio.cancel();
            }
            runningAnimationAudio = new AnimatorSet();
            runningAnimationAudio.playTogether(ObjectAnimator.ofFloat(recordPanel, "translationX", AndroidUtilities.displaySize.x),
                    ObjectAnimator.ofFloat(recordCircle, "scale", 0.0f),
                    ObjectAnimator.ofFloat(audioRecordButton, "alpha", 1.0f));
            runningAnimationAudio.setDuration(300);
            runningAnimationAudio.addListener(new AnimatorListenerAdapterProxy() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    if (runningAnimationAudio != null && runningAnimationAudio.equals(animator)) {
                        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) slideText.getLayoutParams();
                        params.leftMargin = AndroidUtilities.dp(30);
                        slideText.setLayoutParams(params);
                        slideText.setAlpha(1);
                        recordPanel.setVisibility(GONE);
                        recordCircle.setVisibility(GONE);
                        runningAnimationAudio = null;
                    }
                }
            });
            runningAnimationAudio.setInterpolator(new AccelerateInterpolator());
            runningAnimationAudio.start();
        }
    }

    public void setDelegate(ChatActivityEnterViewDelegate delegate) {
        this.delegate = delegate;
    }

    public void setFieldText(CharSequence text) {
        if (messageEditText == null) {
            return;
        }
        ignoreTextChange = true;
        messageEditText.setText(text);
        messageEditText.setSelection(messageEditText.getText().length());
        ignoreTextChange = false;
        if (delegate != null) {
            delegate.onTextChanged(messageEditText.getText(), true);
        }
    }

    public void setSelection(int start) {
        if (messageEditText == null) {
            return;
        }
        messageEditText.setSelection(start, messageEditText.length());
    }

    public int getCursorPosition() {
        if (messageEditText == null) {
            return 0;
        }
        return messageEditText.getSelectionStart();
    }

    public void replaceWithText(int start, int len, CharSequence text) {
        try {
            SpannableStringBuilder builder = new SpannableStringBuilder(messageEditText.getText());
            builder.replace(start, start + len, text);
            messageEditText.setText(builder);
            messageEditText.setSelection(start + text.length());
        } catch (Exception e) {
            FileLog.e("messenger", e);
        }
    }

    public void setFieldFocused(boolean focus) {
        if (messageEditText == null) {
            return;
        }
        if (focus) {
            if (!messageEditText.isFocused()) {
                messageEditText.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (messageEditText != null) {
                            try {
                                messageEditText.requestFocus();
                            } catch (Exception e) {
                                FileLog.e("messenger", e);
                            }
                        }
                    }
                }, 600);
            }
        } else {
            if (messageEditText.isFocused() && !keyboardVisible) {
                messageEditText.clearFocus();
            }
        }
    }

    public boolean hasText() {
        return messageEditText != null && messageEditText.length() > 0;
    }

    public CharSequence getFieldText() {
        if (messageEditText != null && messageEditText.length() > 0) {
            return messageEditText.getText();
        }
        return null;
    }

    public void addToAttachLayout(View view) {
        if (attachButton == null) {
            return;
        }
        if (view.getParent() != null) {
            ViewGroup viewGroup = (ViewGroup) view.getParent();
            viewGroup.removeView(view);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            view.setBackgroundDrawable(Theme.createBarSelectorDrawable(Theme.INPUT_FIELD_SELECTOR_COLOR));
        }
        attachButton.addView(view, LayoutHelper.createLinear(48, 48));
    }

    public boolean isPopupView(View view) {
        return view == emojiView;
    }

    public boolean isRecordCircle(View view) {
        return view == recordCircle;
    }

    private void createEmojiView() {
        if (emojiView != null) {
            return;
        }
        emojiView = new EmojiView(allowStickers, allowGifs, parentActivity);
        emojiView.setVisibility(GONE);
        emojiView.setListener(new EmojiView.Listener() {
            public boolean onBackspace() {
                if (messageEditText.length() == 0) {
                    return false;
                }
                messageEditText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                return true;
            }

            public void onEmojiSelected(String symbol) {
                int i = messageEditText.getSelectionEnd();
                if (i < 0) {
                    i = 0;
                }
                try {
                    innerTextChange = 2;
                    CharSequence localCharSequence = Emoji.replaceEmoji(symbol, messageEditText.getPaint().getFontMetricsInt(), AndroidUtilities.dp(20), false);
                    messageEditText.setText(messageEditText.getText().insert(i, localCharSequence));
                    int j = i + localCharSequence.length();
                    messageEditText.setSelection(j, j);
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                } finally {
                    innerTextChange = 0;
                }
            }

            public void onStickerSelected(TLRPC.Document sticker) {
                ChatActivityEnterView.this.onStickerSelected(sticker);
            }

            @Override
            public void onStickersSettingsClick() {
                if (parentFragment != null) {
                    //parentFragment.presentFragment(new StickersActivity()); -- EDIT BY MR
                }
            }

            @Override
            public void onGifSelected(TLRPC.Document gif) {
                SendMessagesHelper.getInstance().sendSticker(gif, dialog_id);
                if ((int) dialog_id == 0) {
                    MessagesController.getInstance().saveGif(gif);
                }
                if (delegate != null) {
                    delegate.onMessageSend(null);
                }
            }

            @Override
            public void onGifTab(boolean opened) {
                if (!AndroidUtilities.usingHardwareInput) {
                    if (opened) {
                        if (messageEditText.length() == 0) {
                            messageEditText.setText("@gif ");
                            messageEditText.setSelection(messageEditText.length());
                        }
                    } else if (messageEditText.getText().toString().equals("@gif ")) {
                        messageEditText.setText("");
                    }
                }
            }

            @Override
            public void onStickersTab(boolean opened) {
                delegate.onStickersTab(opened);
            }

            @Override
            public void onClearEmojiRecent() {
                if (parentFragment == null || parentActivity == null) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
                builder.setMessage(LocaleController.getString("ClearRecentEmoji", R.string.ClearRecentEmoji));
                builder.setPositiveButton(LocaleController.getString("ClearButton", R.string.ClearButton).toUpperCase(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        emojiView.clearRecentEmoji();
                    }
                });
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                parentFragment.showDialog(builder.create());
            }
        });
        emojiView.setVisibility(GONE);
        sizeNotifierLayout.addView(emojiView);
    }

    @Override
    public void onStickerSelected(TLRPC.Document sticker) {
        SendMessagesHelper.getInstance().sendSticker(sticker, dialog_id);
        if (delegate != null) {
            delegate.onMessageSend(null);
        }
    }

    private void showPopup(int show, int contentType /*0=emojiView, 1=botKeyboardView*/ ) {
        if (show == 1) {
            if (contentType == 0 && emojiView == null) {
                if (parentActivity == null) {
                    return;
                }
                createEmojiView();
            }

            View currentView = null;
            if (contentType == 0) {
                emojiView.setVisibility(VISIBLE);
                currentView = emojiView;
            }
            currentPopupContentType = contentType;

            if (keyboardHeight <= 0) {
                keyboardHeight = ApplicationLoader.applicationContext.getSharedPreferences("emoji", Context.MODE_PRIVATE).getInt("kbd_height", AndroidUtilities.dp(200));
            }
            if (keyboardHeightLand <= 0) {
                keyboardHeightLand = ApplicationLoader.applicationContext.getSharedPreferences("emoji", Context.MODE_PRIVATE).getInt("kbd_height_land3", AndroidUtilities.dp(200));
            }
            int currentHeight = AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y ? keyboardHeightLand : keyboardHeight;

            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) currentView.getLayoutParams();
            layoutParams.width = AndroidUtilities.displaySize.x;
            layoutParams.height = currentHeight;
            currentView.setLayoutParams(layoutParams);
            AndroidUtilities.hideKeyboard(messageEditText);
            if (sizeNotifierLayout != null) {
                emojiPadding = currentHeight;
                sizeNotifierLayout.requestLayout();
                if (contentType == 0) {
                    emojiButton.setImageResource(R.drawable.ic_msg_panel_kb);
                } else if (contentType == 1) {
                    emojiButton.setImageResource(R.drawable.ic_msg_panel_smiles);
                }
                onWindowSizeChanged();
            }
        } else {
            if (emojiButton != null) {
                emojiButton.setImageResource(R.drawable.ic_msg_panel_smiles);
            }
            currentPopupContentType = -1;
            if (emojiView != null) {
                emojiView.setVisibility(GONE);
            }
            if (sizeNotifierLayout != null) {
                if (show == 0) {
                    emojiPadding = 0;
                }
                sizeNotifierLayout.requestLayout();
                onWindowSizeChanged();
            }
        }
    }

    public void hidePopup(boolean byBackButton) {
        if (isPopupShowing()) {
            showPopup(0, 0);
            removeGifFromInputField();
        }
    }

    private void removeGifFromInputField() {
        if (!AndroidUtilities.usingHardwareInput) {
            if (messageEditText.getText().toString().equals("@gif ")) {
                messageEditText.setText("");
            }
        }
    }

    private void openKeyboardInternal() {
        showPopup(AndroidUtilities.usingHardwareInput || isPaused ? 0 : 2, 0);
        messageEditText.requestFocus();
        AndroidUtilities.showKeyboard(messageEditText);
        if (isPaused) {
            showKeyboardOnResume = true;
        } else if (!AndroidUtilities.usingHardwareInput && !keyboardVisible) {
            waitingForKeyboardOpen = true;
            AndroidUtilities.cancelRunOnUIThread(openKeyboardRunnable);
            AndroidUtilities.runOnUIThread(openKeyboardRunnable, 100);
        }
    }

    public boolean hasAudioToSend() {
        return audioToSendMessageObject != null;
    }

    public void openKeyboard() {
        AndroidUtilities.showKeyboard(messageEditText);
    }

    public void closeKeyboard() {
        AndroidUtilities.hideKeyboard(messageEditText);
    }

    public boolean isPopupShowing() {
        return emojiView != null && emojiView.getVisibility() == VISIBLE;
    }

    public boolean isKeyboardVisible() {
        return keyboardVisible;
    }

    public void addRecentGif(MediaController.SearchImage searchImage) {
        if (emojiView == null) {
            return;
        }
        emojiView.addRecentGif(searchImage);
    }

    @Override
    public void onSizeChanged(int height, boolean isWidthGreater) {
        if (height > AndroidUtilities.dp(50) && keyboardVisible) {
            if (isWidthGreater) {
                keyboardHeightLand = height;
                ApplicationLoader.applicationContext.getSharedPreferences("emoji", 0).edit().putInt("kbd_height_land3", keyboardHeightLand).apply();
            } else {
                keyboardHeight = height;
                ApplicationLoader.applicationContext.getSharedPreferences("emoji", 0).edit().putInt("kbd_height", keyboardHeight).apply();
            }
        }

        if (isPopupShowing()) {
            int newHeight = isWidthGreater ? keyboardHeightLand : keyboardHeight;

            View currentView = null;
            if (currentPopupContentType == 0) {
                currentView = emojiView;
            }

            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) currentView.getLayoutParams();
            if (layoutParams.width != AndroidUtilities.displaySize.x || layoutParams.height != newHeight) {
                layoutParams.width = AndroidUtilities.displaySize.x;
                layoutParams.height = newHeight;
                currentView.setLayoutParams(layoutParams);
                if (sizeNotifierLayout != null) {
                    emojiPadding = layoutParams.height;
                    sizeNotifierLayout.requestLayout();
                    onWindowSizeChanged();
                }
            }
        }

        if (lastSizeChangeValue1 == height && lastSizeChangeValue2 == isWidthGreater) {
            onWindowSizeChanged();
            return;
        }
        lastSizeChangeValue1 = height;
        lastSizeChangeValue2 = isWidthGreater;

        boolean oldValue = keyboardVisible;
        keyboardVisible = height > 0;
        if (keyboardVisible && isPopupShowing()) {
            showPopup(0, currentPopupContentType);
        }
        if (emojiPadding != 0 && !keyboardVisible && keyboardVisible != oldValue && !isPopupShowing()) {
            emojiPadding = 0;
            sizeNotifierLayout.requestLayout();
        }
        if (keyboardVisible && waitingForKeyboardOpen) {
            waitingForKeyboardOpen = false;
            AndroidUtilities.cancelRunOnUIThread(openKeyboardRunnable);
        }
        onWindowSizeChanged();
    }

    public int getEmojiPadding() {
        return emojiPadding;
    }

    public int getEmojiHeight() {
        if (AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y) {
            return keyboardHeightLand;
        } else {
            return keyboardHeight;
        }
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.emojiDidLoaded) {
            if (emojiView != null) {
                emojiView.invalidateViews();
            }
        } else if (id == NotificationCenter.recordProgressChanged) {
            long t = (Long) args[0];
            Long time = t / 1000;
            int ms = (int) (t % 1000L) / 10;
            String str = String.format("%02d:%02d.%02d", time / 60, time % 60, ms);
            if (lastTimeString == null || !lastTimeString.equals(str)) {
                if (time % 5 == 0) {
                    MessagesController.getInstance().sendTyping(dialog_id, 1, 0);
                }
                if (recordTimeText != null) {
                    recordTimeText.setText(str);
                }
            }
            if (recordCircle != null) {
                recordCircle.setAmplitude((Double) args[1]);
            }
        } else if (id == NotificationCenter.closeChats) {
            if (messageEditText != null && messageEditText.isFocused()) {
                AndroidUtilities.hideKeyboard(messageEditText);
            }
        } else if (id == NotificationCenter.recordStartError || id == NotificationCenter.recordStopped) {
            if (recordingAudio) {
                MessagesController.getInstance().sendTyping(dialog_id, 2, 0);
                recordingAudio = false;
                updateAudioRecordInterface();
            }
        } else if (id == NotificationCenter.recordStarted) {
            if (!recordingAudio) {
                recordingAudio = true;
                updateAudioRecordInterface();
            }
        } else if (id == NotificationCenter.audioDidSent) {
            audioToSend = (TLRPC.TL_document) args[0];
            audioToSendPath = (String) args[1];
            if (audioToSend != null) {
                if (recordedAudioPanel == null) {
                    return;
                }

                TLRPC.TL_message message = new TLRPC.TL_message();
                message.out = true;
                message.id = 0;
                message.to_id = new TLRPC.TL_peerUser();
                message.to_id.user_id = message.from_id = UserConfig.getClientUserId();
                message.date = (int) (System.currentTimeMillis() / 1000);
                message.message = "-1";
                message.attachPath = audioToSendPath;
                message.media = new TLRPC.TL_messageMediaDocument();
                message.media.document = audioToSend;
                message.flags |= TLRPC.MESSAGE_FLAG_HAS_MEDIA | TLRPC.MESSAGE_FLAG_HAS_FROM_ID;
                audioToSendMessageObject = new MessageObject(message, false);

                recordedAudioPanel.setAlpha(1.0f);
                recordedAudioPanel.setVisibility(VISIBLE);
                int duration = 0;
                for (int a = 0; a < audioToSend.attributes.size(); a++) {
                    TLRPC.DocumentAttribute attribute = audioToSend.attributes.get(a);
                    if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                        duration = attribute.duration;
                        break;
                    }
                }

                for (int a = 0; a < audioToSend.attributes.size(); a++) {
                    TLRPC.DocumentAttribute attribute = audioToSend.attributes.get(a);
                    if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                        if (attribute.waveform == null || attribute.waveform.length == 0) {
                            attribute.waveform = MediaController.getInstance().getWaveform(audioToSendPath);
                        }
                        recordedAudioSeekBar.setWaveform(attribute.waveform);
                        break;
                    }
                }
                recordedAudioTimeTextView.setText(String.format("%d:%02d", duration / 60, duration % 60));
                closeKeyboard();
                hidePopup(false);
                checkSendButton(false);
            } else {
                if (delegate != null) {
                    delegate.onMessageSend(null);
                }
            }
        } else if (id == NotificationCenter.audioRouteChanged) {
            if (parentActivity != null) {
                boolean frontSpeaker = (Boolean) args[0];
                parentActivity.setVolumeControlStream(frontSpeaker ? AudioManager.STREAM_VOICE_CALL : AudioManager.USE_DEFAULT_STREAM_TYPE);
            }
        } else if (id == NotificationCenter.audioDidReset) {
            if (audioToSendMessageObject != null && !MediaController.getInstance().isMessageOnAir(audioToSendMessageObject)) {
                recordedAudioPlayButton.setImageResource(R.drawable.s_player_play_states);
                recordedAudioSeekBar.setProgress(0);
            }
        } else if (id == NotificationCenter.audioProgressDidChanged) {
            Integer mid = (Integer) args[0];
            if (audioToSendMessageObject != null && MediaController.getInstance().isMessageOnAir(audioToSendMessageObject)) {
                MessageObject player = MediaController.getInstance().getPlayingMessageObject();
                audioToSendMessageObject.audioProgress = player.audioProgress;
                audioToSendMessageObject.audioProgressSec = player.audioProgressSec;
                if (!recordedAudioSeekBar.isDragging()) {
                    recordedAudioSeekBar.setProgress(audioToSendMessageObject.audioProgress);
                }
            }
        }
    }

    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {
        /*if (requestCode == 2) {
            if (pendingLocationButton != null) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SendMessagesHelper.getInstance().sendCurrentLocation(pendingMessageObject, pendingLocationButton);
                }
                pendingLocationButton = null;
                pendingMessageObject = null;
            }
        }*/
    }
}
