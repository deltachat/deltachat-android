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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.b44t.messenger.AndroidUtilities;

public class RadialProgress {

    private long lastUpdateTime = 0;
    private float radOffset = 0;
    private float currentProgress = 0;
    private float animationProgressStart = 0;
    private long currentProgressTime = 0;
    private float animatedProgressValue = 0;
    private RectF progressRect = new RectF();
    private RectF cicleRect = new RectF();
    private View parent;
    private float animatedAlphaValue = 1.0f;

    private boolean currentWithRound;
    private boolean previousWithRound;
    private Drawable currentDrawable;
    private Drawable previousDrawable;
    private boolean hideCurrentDrawable;
    private int progressColor = 0xffffffff;

    private static DecelerateInterpolator decelerateInterpolator;
    private static Paint progressPaint;
    private boolean alphaForPrevious = true;

    public RadialProgress(View parentView) {
        if (decelerateInterpolator == null) {
            decelerateInterpolator = new DecelerateInterpolator();
            progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            progressPaint.setStyle(Paint.Style.STROKE);
            progressPaint.setStrokeCap(Paint.Cap.ROUND);
            progressPaint.setStrokeWidth(AndroidUtilities.dp(3));
        }
        parent = parentView;
    }

    public void setProgressRect(int left, int top, int right, int bottom) {
        progressRect.set(left, top, right, bottom);
    }

    public void setAlphaForPrevious(boolean value) {
        alphaForPrevious = value;
    }

    private void updateAnimation(boolean progress) {
        long newTime = System.currentTimeMillis();
        long dt = newTime - lastUpdateTime;
        lastUpdateTime = newTime;

        if (progress) {
            if (animatedProgressValue != 1) {
                radOffset += 360 * dt / 3000.0f;
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
                invalidateParent();
            }
            if (animatedProgressValue >= 1 && previousDrawable != null) {
                animatedAlphaValue -= dt / 200.0f;
                if (animatedAlphaValue <= 0) {
                    animatedAlphaValue = 0.0f;
                    previousDrawable = null;
                }
                invalidateParent();
            }
        } else {
            if (previousDrawable != null) {
                animatedAlphaValue -= dt / 200.0f;
                if (animatedAlphaValue <= 0) {
                    animatedAlphaValue = 0.0f;
                    previousDrawable = null;
                }
                invalidateParent();
            }
        }
    }

    public void setProgressColor(int color) {
        progressColor = color;
    }

    public void setHideCurrentDrawable(boolean value) {
        hideCurrentDrawable = value;
    }

    public void setProgress(float value, boolean animated) {
        if (value != 1 && animatedAlphaValue != 0 && previousDrawable != null) {
            animatedAlphaValue = 0.0f;
            previousDrawable = null;
        }
        if (!animated) {
            animatedProgressValue = value;
            animationProgressStart = value;
        } else {
            if (animatedProgressValue > value) {
                animatedProgressValue = value;
            }
            animationProgressStart = animatedProgressValue;
        }
        currentProgress = value;
        currentProgressTime = 0;

        invalidateParent();
    }

    private void invalidateParent() {
        int offset = AndroidUtilities.dp(2);
        parent.invalidate((int)progressRect.left - offset, (int)progressRect.top - offset, (int)progressRect.right + offset * 2, (int)progressRect.bottom + offset * 2);
    }

    public void setBackground(Drawable drawable /*may be null*/, boolean withRound, boolean animated) {
        lastUpdateTime = System.currentTimeMillis();
        if (animated && currentDrawable != drawable) {
            previousDrawable = currentDrawable;
            previousWithRound = currentWithRound;
            animatedAlphaValue = 1.0f;
            setProgress(1, animated);
        } else {
            previousDrawable = null;
            previousWithRound = false;
        }
        currentWithRound = withRound;
        currentDrawable = drawable;
        if (!animated) {
            parent.invalidate();
        } else {
            invalidateParent();
        }
    }

    public boolean swapBackground(Drawable drawable /*may be null*/) {
        if (currentDrawable != drawable) {
            currentDrawable = drawable;
            return true;
        }
        return false;
    }

    public float getAlpha() {
        return previousDrawable != null || currentDrawable != null ? animatedAlphaValue : 0.0f;
    }

    public void draw(Canvas canvas) {
        if (previousDrawable != null) {
            if (alphaForPrevious) {
                previousDrawable.setAlpha((int) (255 * animatedAlphaValue));
            } else {
                previousDrawable.setAlpha(255);
            }
            previousDrawable.setBounds((int)progressRect.left, (int)progressRect.top, (int)progressRect.right, (int)progressRect.bottom);
            previousDrawable.draw(canvas);
        }

        if (!hideCurrentDrawable && currentDrawable != null) {
            if (previousDrawable != null) {
                currentDrawable.setAlpha((int)(255 * (1.0f - animatedAlphaValue)));
            } else {
                currentDrawable.setAlpha(255);
            }
            currentDrawable.setBounds((int)progressRect.left, (int)progressRect.top, (int)progressRect.right, (int)progressRect.bottom);
            currentDrawable.draw(canvas);
        }

        if (currentWithRound || previousWithRound) {
            int diff = AndroidUtilities.dp(4);
            progressPaint.setColor(progressColor);
            if (previousWithRound) {
                progressPaint.setAlpha((int)(255 * animatedAlphaValue));
            } else {
                progressPaint.setAlpha(255);
            }
            cicleRect.set(progressRect.left + diff, progressRect.top + diff, progressRect.right - diff, progressRect.bottom - diff);
            canvas.drawArc(cicleRect, -90 + radOffset, Math.max(4, 360 * animatedProgressValue), false, progressPaint);
            updateAnimation(true);
        } else {
            updateAnimation(false);
        }
    }
}
