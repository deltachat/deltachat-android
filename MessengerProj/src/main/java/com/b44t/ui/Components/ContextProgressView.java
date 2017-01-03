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
import android.graphics.RectF;
import android.view.View;

import com.b44t.messenger.AndroidUtilities;

public class ContextProgressView extends View {

    private Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint outerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF cicleRect = new RectF();
    private int radOffset = 0;
    private long lastUpdateTime;

    public ContextProgressView(Context context) {
        super(context);
        innerPaint.setColor(0xffbfdff6);
        innerPaint.setStyle(Paint.Style.STROKE);
        innerPaint.setStrokeWidth(AndroidUtilities.dp(2));
        outerPaint.setColor(0xff2b96e2);
        outerPaint.setStyle(Paint.Style.STROKE);
        outerPaint.setStrokeWidth(AndroidUtilities.dp(2));
        outerPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        lastUpdateTime = System.currentTimeMillis();
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        lastUpdateTime = System.currentTimeMillis();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getVisibility() != VISIBLE) {
            return;
        }
        long newTime = System.currentTimeMillis();
        long dt = newTime - lastUpdateTime;
        lastUpdateTime = newTime;
        radOffset += 360 * dt / 1000.0f;

        int x = getMeasuredWidth() / 2 - AndroidUtilities.dp(9);
        int y = getMeasuredHeight() / 2 - AndroidUtilities.dp(9);
        cicleRect.set(x, y, x + AndroidUtilities.dp(18), y + AndroidUtilities.dp(18));
        canvas.drawCircle(getMeasuredWidth() / 2, getMeasuredHeight() / 2, AndroidUtilities.dp(9), innerPaint);
        canvas.drawArc(cicleRect, -90 + radOffset, 90, false, outerPaint);
        invalidate();
    }
}
