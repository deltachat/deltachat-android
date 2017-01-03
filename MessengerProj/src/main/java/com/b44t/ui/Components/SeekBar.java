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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;

import com.b44t.messenger.AndroidUtilities;

public class SeekBar {

    public interface SeekBarDelegate {
        void onSeekBarDrag(float progress);
    }

    private static Paint innerPaint;
    private static Paint outerPaint;
    private static int thumbWidth;
    private int thumbX = 0;
    private int thumbDX = 0;
    private boolean pressed = false;
    private int width;
    private int height;
    private SeekBarDelegate delegate;
    private int innerColor;
    private int outerColor;
    private int selectedColor;
    private boolean selected;

    public SeekBar(Context context) {
        if (innerPaint == null) {
            innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            outerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            thumbWidth = AndroidUtilities.dp(24);
        }
    }

    public void setDelegate(SeekBarDelegate seekBarDelegate) {
        delegate = seekBarDelegate;
    }

    public boolean onTouch(int action, float x, float y) {
        if (action == MotionEvent.ACTION_DOWN) {
            int additionWidth = (height - thumbWidth) / 2;
            if (thumbX - additionWidth <= x && x <= thumbX + thumbWidth + additionWidth && y >= 0 && y <= height) {
                pressed = true;
                thumbDX = (int)(x - thumbX);
                return true;
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (pressed) {
                if (action == MotionEvent.ACTION_UP && delegate != null) {
                    delegate.onSeekBarDrag((float)thumbX / (float)(width - thumbWidth));
                }
                pressed = false;
                return true;
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (pressed) {
                thumbX = (int)(x - thumbDX);
                if (thumbX < 0) {
                    thumbX = 0;
                } else if (thumbX > width - thumbWidth) {
                    thumbX = width - thumbWidth;
                }
                return true;
            }
        }
        return false;
    }

    public void setColors(int inner, int outer, int selected) {
        innerColor = inner;
        outerColor = outer;
        selectedColor = selected;
    }

    public void setProgress(float progress) {
        thumbX = (int)Math.ceil((width - thumbWidth) * progress);
        if (thumbX < 0) {
            thumbX = 0;
        } else if (thumbX > width - thumbWidth) {
            thumbX = width - thumbWidth;
        }
    }

    public boolean isDragging() {
        return pressed;
    }

    public void setSelected(boolean value) {
        selected = value;
    }

    public void setSize(int w, int h) {
        width = w;
        height = h;
    }

    public void draw(Canvas canvas) {
        innerPaint.setColor(selected ? selectedColor : innerColor);
        outerPaint.setColor(outerColor);

        canvas.drawRect(thumbWidth / 2, height / 2 - AndroidUtilities.dp(1), width - thumbWidth / 2, height / 2 + AndroidUtilities.dp(1), innerPaint);
        canvas.drawRect(thumbWidth / 2, height / 2 - AndroidUtilities.dp(1), thumbWidth / 2 + thumbX, height / 2 + AndroidUtilities.dp(1), outerPaint);
        canvas.drawCircle(thumbX + thumbWidth / 2, height / 2, AndroidUtilities.dp(pressed ? 8 : 6), outerPaint);
    }
}
