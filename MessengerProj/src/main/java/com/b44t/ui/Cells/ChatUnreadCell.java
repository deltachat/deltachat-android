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
import android.widget.ImageView;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.MessagesController;
import com.b44t.messenger.R;
import com.b44t.ui.Components.LayoutHelper;
import com.b44t.ui.ActionBar.Theme;

public class ChatUnreadCell extends FrameLayout {

    private TextView textView;

    public ChatUnreadCell(Context context) {
        super(context);

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundResource(R.drawable.newmsg_divider);
        addView(frameLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 27, Gravity.LEFT | Gravity.TOP, 0, 7, 0, 0));

        textView = new TextView(context);
        textView.setPadding(0, 0, 0, AndroidUtilities.dp(1));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, MessagesController.getInstance().fontSize-2);
        textView.setTextColor(Theme.MSG_IN_TIME_TEXT_COLOR);
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
    }

    public void setText(String text) {
        textView.setText(text);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(40), MeasureSpec.EXACTLY));
    }
}
