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

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

public class TypefaceSpan extends MetricAffectingSpan {

    private Typeface mTypeface;
    private int textSize;
    private int color;

    public TypefaceSpan(Typeface typeface) {
        mTypeface = typeface;
    }

    public TypefaceSpan(Typeface typeface, int size) {
        mTypeface = typeface;
        textSize = size;
    }

    public TypefaceSpan(Typeface typeface, int size, int textColor) {
        mTypeface = typeface;
        textSize = size;
        color = textColor;
    }

    @Override
    public void updateMeasureState(TextPaint p) {
        if (mTypeface != null) {
            p.setTypeface(mTypeface);
        }
        if (textSize != 0) {
            p.setTextSize(textSize);
        }
        p.setFlags(p.getFlags() | Paint.SUBPIXEL_TEXT_FLAG);
    }

    @Override
    public void updateDrawState(TextPaint tp) {
        if (mTypeface != null) {
            tp.setTypeface(mTypeface);
        }
        if (textSize != 0) {
            tp.setTextSize(textSize);
        }
        if (color != 0) {
            tp.setColor(color);
        }
        tp.setFlags(tp.getFlags() | Paint.SUBPIXEL_TEXT_FLAG);
    }
}
