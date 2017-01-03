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
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.b44t.messenger.AndroidUtilities;

public class LineProgressView extends View {

    private long lastUpdateTime = 0;
    private float currentProgress = 0;
    private float animationProgressStart = 0;
    private long currentProgressTime = 0;
    private float animatedProgressValue = 0;
    private float animatedAlphaValue = 1.0f;

    private int backColor;
    private int progressColor = 0xff36a2ee;

    private static DecelerateInterpolator decelerateInterpolator = null;
    private static Paint progressPaint = null;

    public LineProgressView(Context context) {
        super(context);

        if (decelerateInterpolator == null) {
            decelerateInterpolator = new DecelerateInterpolator();
            progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            progressPaint.setStyle(Paint.Style.STROKE);
            progressPaint.setStrokeCap(Paint.Cap.ROUND);
            progressPaint.setStrokeWidth(AndroidUtilities.dp(2));
        }
    }

    private void updateAnimation() {
        long newTime = System.currentTimeMillis();
        long dt = newTime - lastUpdateTime;
        lastUpdateTime = newTime;

        if (animatedProgressValue != 1 && animatedProgressValue != currentProgress) {
            float progressDiff = currentProgress - animationProgressStart;
            if (progressDiff > 0) {
                currentProgressTime += dt;
                if (currentProgressTime >= 300) {
                    animatedProgressValue = currentProgress;
                    animationProgressStart = currentProgress;
                    currentProgressTime = 0;
                } else {
                    animatedProgressValue = animationProgressStart + progressDiff * decelerateInterpolator.getInterpolation(currentProgressTime / 300.0f);
                }
            }
            invalidate();
        }
        if (animatedProgressValue >= 1 && animatedProgressValue == 1 && animatedAlphaValue != 0) {
            animatedAlphaValue -= dt / 200.0f;
            if (animatedAlphaValue <= 0) {
                animatedAlphaValue = 0.0f;
            }
            invalidate();
        }
    }

    public void setProgressColor(int color) {
        progressColor = color;
    }

    public void setBackColor(int color) {
        backColor = color;
    }

    public void setProgress(float value, boolean animated) {
        if (!animated) {
            animatedProgressValue = value;
            animationProgressStart = value;
        } else {
            animationProgressStart = animatedProgressValue;
        }
        if (value != 1) {
            animatedAlphaValue = 1;
        }
        currentProgress = value;
        currentProgressTime = 0;

        lastUpdateTime = System.currentTimeMillis();
        invalidate();
    }

    public void onDraw(Canvas canvas) {
        if (backColor != 0 && animatedProgressValue != 1) {
            progressPaint.setColor(backColor);
            progressPaint.setAlpha((int) (255 * animatedAlphaValue));
            int start = (int) (getWidth() * animatedProgressValue);
            canvas.drawRect(start, 0, getWidth(), getHeight(), progressPaint);
        }

        progressPaint.setColor(progressColor);
        progressPaint.setAlpha((int)(255 * animatedAlphaValue));
        canvas.drawRect(0, 0, getWidth() * animatedProgressValue, getHeight(), progressPaint);
        updateAnimation();
    }
}
