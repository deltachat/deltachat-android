/*******************************************************************************
 *
 *                              Delta Chat Android
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


package com.b44t.messenger.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import com.b44t.messenger.AndroidUtilities;

public class PhotoEditorSeekBar extends View {

    private Paint innerPaint = new Paint();
    private Paint outerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int thumbSize = AndroidUtilities.dp(16);
    private int thumbDX = 0;
    private float progress = 0;
    private boolean pressed = false;
    private int minValue;
    private int maxValue;
    private PhotoEditorSeekBarDelegate delegate;

    public interface PhotoEditorSeekBarDelegate {
        void onProgressChanged();
    }

    public PhotoEditorSeekBar(Context context) {
        super(context);

        innerPaint.setColor(0x99383838);
        outerPaint.setColor(0xff53aeef);
    }

    public void setDelegate(PhotoEditorSeekBarDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) {
            return false;
        }
        float x = event.getX();
        float y = event.getY();
        float thumbX = (int)((getMeasuredWidth() - thumbSize) * progress);
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int additionWidth = (getMeasuredHeight() - thumbSize) / 2;
            if (thumbX - additionWidth <= x && x <= thumbX + thumbSize + additionWidth && y >= 0 && y <= getMeasuredHeight()) {
                pressed = true;
                thumbDX = (int)(x - thumbX);
                getParent().requestDisallowInterceptTouchEvent(true);
                invalidate();
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (pressed) {
                pressed = false;
                invalidate();
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (pressed) {
                thumbX = (int)(x - thumbDX);
                if (thumbX < 0) {
                    thumbX = 0;
                } else if (thumbX > getMeasuredWidth() - thumbSize) {
                    thumbX = getMeasuredWidth() - thumbSize;
                }
                progress = thumbX / (getMeasuredWidth() - thumbSize);
                if (delegate != null) {
                    delegate.onProgressChanged();
                }
                invalidate();
                return true;
            }
        }
        return false;
    }

    public void setProgress(int progress) {
        setProgress(progress, true);
    }

    public void setProgress(int progress, boolean notify) {
        if (progress < minValue) {
            progress = minValue;
        } else if (progress > maxValue) {
            progress = maxValue;
        }
        this.progress = (progress - minValue) / (float) (maxValue - minValue);
        invalidate();
        if (notify && delegate != null) {
            delegate.onProgressChanged();
        }
    }

    public int getProgress() {
        return (int) (minValue + progress * (maxValue - minValue));
    }

    public void setMinMax(int min, int max) {
        minValue = min;
        maxValue = max;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int y = (getMeasuredHeight() - thumbSize) / 2;
        int thumbX = (int)((getMeasuredWidth() - thumbSize) * progress);
        canvas.drawRect(thumbSize / 2, getMeasuredHeight() / 2 - AndroidUtilities.dp(1), getMeasuredWidth() - thumbSize / 2, getMeasuredHeight() / 2 + AndroidUtilities.dp(1), innerPaint);
        if (minValue == 0) {
            canvas.drawRect(thumbSize / 2, getMeasuredHeight() / 2 - AndroidUtilities.dp(1), thumbX, getMeasuredHeight() / 2 + AndroidUtilities.dp(1), outerPaint);
        } else {
            if (progress > 0.5f) {
                canvas.drawRect(getMeasuredWidth() / 2 - AndroidUtilities.dp(1), (getMeasuredHeight() - thumbSize) / 2, getMeasuredWidth() / 2, (getMeasuredHeight() + thumbSize) / 2, outerPaint);
                canvas.drawRect(getMeasuredWidth() / 2, getMeasuredHeight() / 2 - AndroidUtilities.dp(1), thumbX, getMeasuredHeight() / 2 + AndroidUtilities.dp(1), outerPaint);
            } else {
                canvas.drawRect(getMeasuredWidth() / 2, (getMeasuredHeight() - thumbSize) / 2, getMeasuredWidth() / 2 + AndroidUtilities.dp(1), (getMeasuredHeight() + thumbSize) / 2, outerPaint);
                canvas.drawRect(thumbX, getMeasuredHeight() / 2 - AndroidUtilities.dp(1), getMeasuredWidth() / 2, getMeasuredHeight() / 2 + AndroidUtilities.dp(1), outerPaint);
            }
        }
        canvas.drawCircle(thumbX + thumbSize / 2, y + thumbSize / 2, thumbSize / 2, outerPaint);
    }
}
