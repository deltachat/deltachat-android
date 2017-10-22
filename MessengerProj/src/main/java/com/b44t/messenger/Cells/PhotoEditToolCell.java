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
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.Components.LayoutHelper;

public class PhotoEditToolCell extends FrameLayout {

    private ImageView iconImage;
    private TextView nameTextView;
    private TextView valueTextView;

    public PhotoEditToolCell(Context context) {
        super(context);

        iconImage = new ImageView(context);
        iconImage.setScaleType(ImageView.ScaleType.CENTER);
        addView(iconImage, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.START, 0, 0, 0, 12));

        nameTextView = new TextView(context);
        nameTextView.setGravity(Gravity.CENTER);
        nameTextView.setTextColor(0xffffffff);
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        nameTextView.setMaxLines(1);
        nameTextView.setSingleLine(true);
        nameTextView.setEllipsize(TextUtils.TruncateAt.END);
        addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.BOTTOM, 4, 0, 4, 0));

        valueTextView = new TextView(context);
        valueTextView.setTextColor(0xff6cc3ff);
        valueTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        addView(valueTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.TOP, 57, 3, 0, 0));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(86), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(60), MeasureSpec.EXACTLY));
    }

    public void setIconAndTextAndValue(int resId, String text, float value) {
        iconImage.setImageResource(resId);
        nameTextView.setText(text.toUpperCase());
        if (value == 0) {
            valueTextView.setText("");
        } else if (value > 0) {
            valueTextView.setText("+" + (int) value);
        } else {
            valueTextView.setText("" + (int) value);
        }
    }

    public void setIconAndTextAndValue(int resId, String text, String value) {
        iconImage.setImageResource(resId);
        nameTextView.setText(text.toUpperCase());
        valueTextView.setText(value);
    }
}
