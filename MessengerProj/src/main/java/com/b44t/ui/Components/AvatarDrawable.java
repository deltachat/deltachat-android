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


package com.b44t.ui.Components;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.b44t.messenger.AndroidUtilities;

public class AvatarDrawable extends Drawable {

    private static Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static TextPaint namePaint;
    private static int[] arrColors = {0xffe56555, 0xfff28c48, 0xff8e85ee, 0xff76c84d, 0xff5bb6cc, 0xff549cdd, 0xffd25c99, 0xffb37800}; /* the colors should contrast to typical action bar colors as well as to white (more important, is used as text color)*/

    private int color;
    private StaticLayout textLayout;
    private float textWidth;
    private float textHeight;
    private float textLeft;
    private StringBuilder stringBuilder = new StringBuilder(5);

    public AvatarDrawable() {
        super();

        if (namePaint == null) {
            namePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            namePaint.setColor(0xffffffff);
            namePaint.setTextSize(AndroidUtilities.dp(20));
        }
    }

    private static int getColorIndex(int id) {
        return Math.abs(id % arrColors.length);
    }

    public static int getNameColor(String name) {
        int id = strChecksum(name);
        return arrColors[getColorIndex(id)];
    }

    private static int strChecksum(String str) {
        int ret = 0;
        if( str!=null ) {
            int i;
            for (i = 0; i < str.length(); i++) {
                ret += (i+1)*str.charAt(i);
                ret %= 0x00FFFFFF;
            }
        }
        return ret;
    }

    public void setInfoByName(String name) {
        int id = strChecksum(name);

        color = arrColors[getColorIndex(id)];

        stringBuilder.setLength(0);
        if (name != null && name.length() > 0) {
            stringBuilder.append(name.substring(0, 1));

            for (int a = name.length() - 1; a >= 0; a--) {
                if (name.charAt(a) == ' ') {
                    if (a != name.length() - 1 && name.charAt(a + 1) != ' ') {
                        if (Build.VERSION.SDK_INT >= 16) {
                            stringBuilder.append("\u200C"); // ZERO WIDTH NON-JOINER - avoids the two letter to melt into a ligature which would be incorrect on the initials
                        }
                        stringBuilder.append(name.substring(a + 1, a + 2));
                        break;
                    }
                }
            }
        }

        if (stringBuilder.length() > 0) {
            String text = stringBuilder.toString().toUpperCase();
            try {
                textLayout = new StaticLayout(text, namePaint, AndroidUtilities.dp(100), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                if (textLayout.getLineCount() > 0) {
                    textLeft = textLayout.getLineLeft(0);
                    textWidth = textLayout.getLineWidth(0);
                    textHeight = textLayout.getLineBottom(0);
                }
            } catch (Exception e) {

            }
        } else {
            textLayout = null;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds == null) {
            return;
        }
        int size = bounds.width();
        paint.setColor(color);
        canvas.save();
        canvas.translate(bounds.left, bounds.top);
        canvas.drawCircle(size / 2, size / 2, size / 2, paint);

        if (textLayout != null) {
            canvas.translate((size - textWidth) / 2 - textLeft, (size - textHeight) / 2);
            textLayout.draw(canvas);
        }

        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) { // must be present in non-abstract classes derived from Drawable
    }

    @Override
    public void setColorFilter(ColorFilter cf) { // must be present in non-abstract classes derived from Drawable
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
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
