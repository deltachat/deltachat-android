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

import com.b44t.messenger.AndroidUtilities;

public class ProgressView {
    
    private Paint innerPaint;
    private Paint outerPaint;

    public float currentProgress = 0;
    public int width;
    public int height;
    public float progressHeight = AndroidUtilities.dp(2.0f);

    public ProgressView() {
        innerPaint = new Paint();
        outerPaint = new Paint();
    }

    public void setProgressColors(int innerColor, int outerColor) {
        innerPaint.setColor(innerColor);
        outerPaint.setColor(outerColor);
    }

    public void setProgress(float progress) {
        currentProgress = progress;
        if (currentProgress < 0) {
            currentProgress = 0;
        } else if (currentProgress > 1) {
            currentProgress = 1;
        }
    }

    public void draw(Canvas canvas) {
        canvas.drawRect(0, height / 2 - progressHeight / 2.0f, width, height / 2 + progressHeight / 2.0f, innerPaint);
        canvas.drawRect(0, height / 2 - progressHeight / 2.0f, width * currentProgress, height / 2 + progressHeight / 2.0f, outerPaint);
    }
}
