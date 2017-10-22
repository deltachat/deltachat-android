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


package com.b44t.messenger.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.Components.LayoutHelper;

import static com.b44t.messenger.AndroidUtilities.dp;

public class TextSettingsCell extends FrameLayout {

    private TextView textView;
    private TextView valueTextView;
    private boolean  valueIsColor;
    private Paint dividerpaint;
    private Paint circlepaint;
    private Paint circleborderpaint;
    private boolean needDivider;

    public TextSettingsCell(Context context) {
        super(context);

        dividerpaint = new Paint();
        dividerpaint.setColor(0xffd9d9d9);
        dividerpaint.setStrokeWidth(1);

        circleborderpaint = new Paint();
        circleborderpaint.setColor(0xff000000);
        circleborderpaint.setStrokeWidth(1);

        circlepaint = new Paint();
        circlepaint.setAntiAlias(true);

        textView = new TextView(context);
        textView.setTextColor(0xff212121);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setLines(1);
        textView.setMaxLines(1);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.START | Gravity.TOP, 17, 0, 17, 0));

        valueTextView = new TextView(context);
        valueTextView.setTextColor(0xff2f8cc9);
        valueTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        valueTextView.setLines(1);
        valueTextView.setMaxLines(1);
        valueTextView.setSingleLine(true);
        valueTextView.setEllipsize(TextUtils.TruncateAt.END);
        valueTextView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        addView(valueTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.END | Gravity.TOP, 17, 0, 17, 0));
    }

    private final int RADIUS = dp(8);

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(48) + (needDivider ? 1 : 0));

        int availableWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - AndroidUtilities.dp(34);
        int width = availableWidth / 2;
        if (valueTextView.getVisibility() == VISIBLE) {
            valueTextView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
            width = availableWidth - valueTextView.getMeasuredWidth() - AndroidUtilities.dp(8);
        } else {
            width = availableWidth;
            if( valueIsColor ) {
                width -= RADIUS * 3/*2 for the circle, 1 for space*/;
            }
        }
        textView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
    }

    public void setTextColor(int color) {
        textView.setTextColor(color);
    }

    public void setText(String text, boolean divider) {
        textView.setText(text);
        valueTextView.setVisibility(INVISIBLE);
        needDivider = divider;
        setWillNotDraw(!divider);
    }

    public void setTextAndValue(String text, String value, boolean divider) {
        textView.setText(text);
        valueIsColor = false;
        if (value != null) {
            valueTextView.setText(value);
            valueTextView.setVisibility(VISIBLE);
        } else {
            valueTextView.setVisibility(INVISIBLE);
        }
        needDivider = divider;
        setWillNotDraw(!needDivider);
        requestLayout();
    }

    public void setTextAndColor(String text, int color, boolean divider) {
        textView.setText(text);
        valueIsColor = true;
        valueTextView.setVisibility(INVISIBLE);
        needDivider = divider;
        circlepaint.setColor(color);
        setWillNotDraw(false);
        requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (needDivider) {
            canvas.drawLine(getPaddingLeft(), getHeight() - 1, getWidth() - getPaddingRight(), getHeight() - 1, dividerpaint);
        }

        if( valueIsColor ) {
            int x = getWidth()-getPaddingRight()-dp(17)-RADIUS;
            int y = getHeight()/2;
            canvas.drawCircle(x, y, RADIUS+dp(1), circleborderpaint);
            canvas.drawCircle(x, y, RADIUS, circlepaint);
        }
    }
}
