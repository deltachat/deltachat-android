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
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.LocaleController;
import com.b44t.ui.ActionBar.SimpleTextView;

public class TextCell extends FrameLayout {

    private SimpleTextView textView;
    private SimpleTextView valueTextView;
    private ImageView imageView;
    private ImageView valueImageView;

    public TextCell(Context context) {
        super(context);

        textView = new SimpleTextView(context);
        textView.setTextColor(0xff212121);
        textView.setTextSize(16);
        textView.setGravity(Gravity.START);
        addView(textView);

        valueTextView = new SimpleTextView(context);
        valueTextView.setTextColor(0xff2f8cc9);
        valueTextView.setTextSize(16);
        valueTextView.setGravity(Gravity.END);
        addView(valueTextView);

        imageView = new ImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        addView(imageView);

        valueImageView = new ImageView(context);
        valueImageView.setScaleType(ImageView.ScaleType.CENTER);
        addView(valueImageView);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = AndroidUtilities.dp(48);

        valueTextView.measure(MeasureSpec.makeMeasureSpec(width - AndroidUtilities.dp(24), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(20), MeasureSpec.EXACTLY));
        textView.measure(MeasureSpec.makeMeasureSpec(width - AndroidUtilities.dp(71 + 24), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(20), MeasureSpec.EXACTLY));
        imageView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
        valueImageView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));

        setMeasuredDimension(width, AndroidUtilities.dp(48));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int height = bottom - top;
        int width = right - left;

        int viewTop = (height - valueTextView.getTextHeight()) / 2;
        int viewLeft = LocaleController.isRTL ? AndroidUtilities.dp(24) : 0;
        valueTextView.layout(viewLeft, viewTop, viewLeft + valueTextView.getMeasuredWidth(), viewTop + valueTextView.getMeasuredHeight());

        viewTop = (height - textView.getTextHeight()) / 2;
        viewLeft = !LocaleController.isRTL ? AndroidUtilities.dp(71) : AndroidUtilities.dp(24);
        textView.layout(viewLeft, viewTop, viewLeft + textView.getMeasuredWidth(), viewTop + textView.getMeasuredHeight());

        viewTop = AndroidUtilities.dp(5);
        viewLeft = !LocaleController.isRTL ? AndroidUtilities.dp(16) : width - imageView.getMeasuredWidth() - AndroidUtilities.dp(16);
        imageView.layout(viewLeft, viewTop, viewLeft + imageView.getMeasuredWidth(), viewTop + imageView.getMeasuredHeight());

        viewTop = (height - valueImageView.getMeasuredHeight()) / 2;
        viewLeft = LocaleController.isRTL ? AndroidUtilities.dp(24) : width - valueImageView.getMeasuredWidth() - AndroidUtilities.dp(24);
        valueImageView.layout(viewLeft, viewTop, viewLeft + valueImageView.getMeasuredWidth(), viewTop + valueImageView.getMeasuredHeight());
    }

    public void setTextColor(int color) {
        textView.setTextColor(color);
    }

    public void setText(String text) {
        textView.setText(text);
        valueTextView.setText(null);
        imageView.setVisibility(INVISIBLE);
        valueTextView.setVisibility(INVISIBLE);
        valueImageView.setVisibility(INVISIBLE);
    }

    public void setTextAndIcon(String text, int resId) {
        textView.setText(text);
        valueTextView.setText(null);
        imageView.setImageResource(resId);
        imageView.setVisibility(VISIBLE);
        valueTextView.setVisibility(INVISIBLE);
        valueImageView.setVisibility(INVISIBLE);
        imageView.setPadding(0, AndroidUtilities.dp(7), 0, 0);
    }

    public void setTextAndValue(String text, String value) {
        textView.setText(text);
        valueTextView.setText(value);
        valueTextView.setVisibility(VISIBLE);
        imageView.setVisibility(INVISIBLE);
        valueImageView.setVisibility(INVISIBLE);
    }

    public void setTextAndValueDrawable(String text, Drawable drawable) {
        textView.setText(text);
        valueTextView.setText(null);
        valueImageView.setVisibility(VISIBLE);
        valueImageView.setImageDrawable(drawable);
        valueTextView.setVisibility(INVISIBLE);
        imageView.setVisibility(INVISIBLE);
        imageView.setPadding(0, AndroidUtilities.dp(7), 0, 0);
    }
}
