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
import android.graphics.Rect;
import android.widget.EditText;

import com.b44t.messenger.AndroidUtilities;

public class HintEditText extends EditText {

    private String hintText;
    private float textOffset;
    private float spaceSize;
    private float numberSize;
    private Paint paint = new Paint();
    private Rect rect = new Rect();

    public HintEditText(Context context) {
        super(context);
        paint.setColor(0xff979797);
    }

    public String getHintText() {
        return hintText;
    }

    public void setHintText(String value) {
        hintText = value;
        onTextChange();
        setText(getText());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        onTextChange();
    }

    public void onTextChange() {
        textOffset = (length() > 0 ? getPaint().measureText(getText(), 0, length()) : 0);
        spaceSize = getPaint().measureText(" ");
        numberSize = getPaint().measureText("1");
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (hintText != null && length() < hintText.length()) {
            int top = getMeasuredHeight() / 2;
            float offsetX = textOffset;
            for (int a = length(); a < hintText.length(); a++) {
                if (hintText.charAt(a) == ' ') {
                    offsetX += spaceSize;
                } else {
                    rect.set((int) offsetX + AndroidUtilities.dp(1), top, (int) (offsetX + numberSize) - AndroidUtilities.dp(1), top + AndroidUtilities.dp(2));
                    canvas.drawRect(rect, paint);
                    offsetX += numberSize;
                }
            }
        }
    }
}
