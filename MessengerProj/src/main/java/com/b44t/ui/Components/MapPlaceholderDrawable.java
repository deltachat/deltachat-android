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
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

import com.b44t.messenger.AndroidUtilities;

public class MapPlaceholderDrawable extends Drawable {

    private Paint paint;
    private Paint linePaint;

    public MapPlaceholderDrawable() {
        super();
        paint = new Paint();
        paint.setColor(0xffded7d6);
        linePaint = new Paint();
        linePaint.setColor(0xffc6bfbe);
        linePaint.setStrokeWidth(AndroidUtilities.dp(1));
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRect(getBounds(), paint);
        int gap = AndroidUtilities.dp(9);
        int xcount = getBounds().width() / gap;
        int ycount = getBounds().height() / gap;
        int x = getBounds().left;
        int y = getBounds().top;
        for (int a = 0; a < xcount; a++) {
            canvas.drawLine(x + gap * (a + 1), y, x + gap * (a + 1), y + getBounds().height(), linePaint);
        }
        for (int a = 0; a < ycount; a++) {
            canvas.drawLine(x, y + gap * (a + 1), x + getBounds().width(), y + gap * (a + 1), linePaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    public int getIntrinsicWidth() {
        return 0;
    }

    @Override
    public int getIntrinsicHeight() {
        return 0;
    }
}
