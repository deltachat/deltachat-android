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


package com.b44t.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MediaController;
import com.b44t.messenger.MessageObject;
import com.b44t.messenger.R;
import com.b44t.ui.Components.CheckBox;
import com.b44t.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class AudioCell extends FrameLayout {

    private ImageView playButton;
    private TextView titleTextView;
    private TextView authorTextView;
    private TextView genreTextView;
    private TextView timeTextView;
    private CheckBox checkBox;

    private MediaController.AudioEntry audioEntry;
    private boolean needDivider;
    private static Paint paint;

    private AudioCellDelegate delegate;

    public interface AudioCellDelegate {
        void startedPlayingAudio(MessageObject messageObject);
    }

    public AudioCell(Context context) {
        super(context);

        if (paint == null) {
            paint = new Paint();
            paint.setColor(0xffd9d9d9);
            paint.setStrokeWidth(1);
        }

        playButton = new ImageView(context);
        playButton.setScaleType(ImageView.ScaleType.CENTER);
        addView(playButton, LayoutHelper.createFrame(46, 46, ((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP), LocaleController.isRTL ? 0 : 13, 13, LocaleController.isRTL ? 13 : 0, 0));
        playButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioEntry != null) {
                    if (MediaController.getInstance().isPlayingAudio(audioEntry.messageObject) && !MediaController.getInstance().isAudioPaused()) {
                        MediaController.getInstance().pauseAudio(audioEntry.messageObject);
                        playButton.setImageResource(R.drawable.audiosend_play);
                    } else {
                        ArrayList<MessageObject> arrayList = new ArrayList<>();
                        arrayList.add(audioEntry.messageObject);
                        if (MediaController.getInstance().setPlaylist(arrayList, audioEntry.messageObject)) {
                            playButton.setImageResource(R.drawable.audiosend_pause);
                            if (delegate != null) {
                                delegate.startedPlayingAudio(audioEntry.messageObject);
                            }
                        }
                    }
                }
            }
        });

        titleTextView = new TextView(context);
        titleTextView.setTextColor(0xff212121);
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        titleTextView.setLines(1);
        titleTextView.setMaxLines(1);
        titleTextView.setSingleLine(true);
        titleTextView.setEllipsize(TextUtils.TruncateAt.END);
        titleTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
        addView(titleTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 50 : 72, 7, LocaleController.isRTL ? 72 : 50, 0));

        genreTextView = new TextView(context);
        genreTextView.setTextColor(0xff8a8a8a);
        genreTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        genreTextView.setLines(1);
        genreTextView.setMaxLines(1);
        genreTextView.setSingleLine(true);
        genreTextView.setEllipsize(TextUtils.TruncateAt.END);
        genreTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
        addView(genreTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 50 : 72, 28, LocaleController.isRTL ? 72 : 50, 0));

        authorTextView = new TextView(context);
        authorTextView.setTextColor(0xff8a8a8a);
        authorTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        authorTextView.setLines(1);
        authorTextView.setMaxLines(1);
        authorTextView.setSingleLine(true);
        authorTextView.setEllipsize(TextUtils.TruncateAt.END);
        authorTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
        addView(authorTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 50 : 72, 44, LocaleController.isRTL ? 72 : 50, 0));

        timeTextView = new TextView(context);
        timeTextView.setTextColor(0xff999999);
        timeTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        timeTextView.setLines(1);
        timeTextView.setMaxLines(1);
        timeTextView.setSingleLine(true);
        timeTextView.setEllipsize(TextUtils.TruncateAt.END);
        timeTextView.setGravity((LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.TOP);
        addView(timeTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.TOP, LocaleController.isRTL ? 18 : 0, 11, LocaleController.isRTL ? 0 : 18, 0));

        checkBox = new CheckBox(context, R.drawable.round_check2);
        checkBox.setVisibility(VISIBLE);
        checkBox.setColor(0xff29b6f7);
        addView(checkBox, LayoutHelper.createFrame(22, 22, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.TOP, LocaleController.isRTL ? 18 : 0, 39, LocaleController.isRTL ? 0 : 18, 0));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(72) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));
    }

    public void setAudio(MediaController.AudioEntry entry, boolean divider, boolean checked) {
        audioEntry = entry;

        titleTextView.setText(audioEntry.title);
        genreTextView.setText(audioEntry.genre);
        authorTextView.setText(audioEntry.author);
        timeTextView.setText(String.format("%d:%02d", audioEntry.duration / 60, audioEntry.duration % 60));
        playButton.setImageResource(MediaController.getInstance().isPlayingAudio(audioEntry.messageObject) && !MediaController.getInstance().isAudioPaused() ? R.drawable.audiosend_pause : R.drawable.audiosend_play);

        needDivider = divider;
        setWillNotDraw(!divider);

        checkBox.setChecked(checked, false);
    }

    public void setChecked(boolean value) {
        checkBox.setChecked(value, true);
    }

    public void setDelegate(AudioCellDelegate audioCellDelegate) {
        delegate = audioCellDelegate;
    }

    public MediaController.AudioEntry getAudioEntry() {
        return audioEntry;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (needDivider) {
            canvas.drawLine(AndroidUtilities.dp(72), getHeight() - 1, getWidth(), getHeight() - 1, paint);
        }
    }
}
