/*******************************************************************************
 *
 *                              Delta Chat Android
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
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.Components.LayoutHelper;


public class TextInfoCell extends FrameLayout {

    private TextView textView;
    private TextView iconView;

    private final int iconDp = 34;

    public TextInfoCell(Context context) {
        super(context);

        textView = new TextView(context);
        textView.setTextColor(0xff808080);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        textView.setGravity(Gravity.START);
        addView(textView);

        iconView = new TextView(context);
        iconView.setTextColor(0xff212121);
        iconView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, iconDp);
        iconView.setGravity(Gravity.START);
        addView(iconView);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    }

    public void setText(CharSequence text)
    {
        setText(text, null, true);
    }

    public void setText(CharSequence text, CharSequence icon, boolean borderBotton)
    {
        textView.setText(text);

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) textView.getLayoutParams();
        lp.width        = LayoutHelper.WRAP_CONTENT;
        lp.height       = LayoutHelper.WRAP_CONTENT;
        lp.gravity      = Gravity.START | Gravity.TOP;
        lp.leftMargin   = AndroidUtilities.dp(17);
        lp.topMargin    = AndroidUtilities.dp(13);
        lp.rightMargin  = AndroidUtilities.dp(17 + (icon!=null?iconDp:0));
        lp.bottomMargin = borderBotton? AndroidUtilities.dp(13) : 0;
        textView.setLayoutParams(lp);

        if( icon != null )
        {
            iconView.setText(icon);
            iconView.setVisibility(VISIBLE);

            lp = (FrameLayout.LayoutParams) iconView.getLayoutParams();
            lp.width        = LayoutHelper.WRAP_CONTENT;
            lp.height       = LayoutHelper.WRAP_CONTENT;
            lp.gravity      = Gravity.END | Gravity.TOP;
            lp.leftMargin   = AndroidUtilities.dp(17);
            lp.topMargin    = AndroidUtilities.dp(3);
            lp.rightMargin  = AndroidUtilities.dp(20);
            iconView.setLayoutParams(lp);
        }
        else
        {
            iconView.setVisibility(GONE);
        }
    }

    public void setTextColor(int color) {
        textView.setTextColor(color);
    }
}
