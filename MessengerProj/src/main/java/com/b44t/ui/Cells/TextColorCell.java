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


package com.b44t.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.b44t.messenger.LocaleController;
import com.b44t.ui.Components.LayoutHelper;

import static com.b44t.messenger.AndroidUtilities.dp;

public class TextColorCell extends FrameLayout {

    private TextView textView;
    private boolean needDivider;
    private Paint dividerpaint;
    private Paint circlepaint;


    public TextColorCell(Context context) {
        super(context);

        dividerpaint = new Paint();
        dividerpaint.setColor(0xffd9d9d9);
        dividerpaint.setStrokeWidth(1);
        dividerpaint.setAntiAlias(true);

        circlepaint = new Paint();
        circlepaint.setAntiAlias(true);

        textView = new TextView(context);
        textView.setTextColor(0xff212121);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setLines(1);
        textView.setMaxLines(1);
        textView.setSingleLine(true);
        textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 17, 0, 17, 0));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(dp(48) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));
    }

    public void setTextAndColor(String text, int color, boolean divider) {
        textView.setText(text);
        needDivider = divider;
        circlepaint.setColor(color);
        setWillNotDraw(false);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (needDivider) {
            canvas.drawLine(getPaddingLeft(), getHeight() - 1, getWidth() - getPaddingRight(), getHeight() - 1, dividerpaint);
        }

        final int RADIUS = dp(8);
        int x = getWidth()-getPaddingRight()-dp(17)-RADIUS;
        int y = getHeight()/2;
        canvas.drawCircle(x+dp(1), y+dp(1), RADIUS, dividerpaint);
        canvas.drawCircle(x, y, RADIUS, circlepaint);
    }
}
