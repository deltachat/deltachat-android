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
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.R;
import com.b44t.ui.Components.LayoutHelper;

public class LocationLoadingCell extends FrameLayout {

    private ProgressBar progressBar;
    private TextView textView;

    public LocationLoadingCell(Context context) {
        super(context);

        progressBar = new ProgressBar(context);
        addView(progressBar, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        textView = new TextView(context);
        textView.setTextColor(0xff999999);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setText(LocaleController.getString("NoResult", R.string.NoResult));
        addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec((int) (AndroidUtilities.dp(56) * 2.5f), MeasureSpec.EXACTLY));
    }

    public void setLoading(boolean value) {
        progressBar.setVisibility(value ? VISIBLE : INVISIBLE);
        textView.setVisibility(value ? INVISIBLE : VISIBLE);
    }
}
