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
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.ImageReceiver;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.R;
import com.b44t.ui.Components.BackupImageView;
import com.b44t.ui.Components.CheckBoxView;
import com.b44t.ui.Components.LayoutHelper;

public class SharedDocumentCell extends FrameLayout {

    private ImageView placeholderImageView;
    private BackupImageView thumbImageView;
    private TextView nameTextView;
    private TextView dateTextView;
    private CheckBoxView checkBox;

    private static Paint paint;

    public SharedDocumentCell(Context context) {
        super(context);

        if (paint == null) {
            paint = new Paint();
            paint.setColor(0xffd9d9d9);
            paint.setStrokeWidth(1);
        }

        placeholderImageView = new ImageView(context);
        addView(placeholderImageView, LayoutHelper.createFrame(40, 40, Gravity.START | Gravity.TOP, LocaleController.isRTL ? 0 : 12, 8, LocaleController.isRTL ? 12 : 0, 0));

        thumbImageView = new BackupImageView(context);
        addView(thumbImageView, LayoutHelper.createFrame(40, 40, Gravity.START | Gravity.TOP, LocaleController.isRTL ? 0 : 12, 8, LocaleController.isRTL ? 12 : 0, 0));
        thumbImageView.getImageReceiver().setDelegate(new ImageReceiver.ImageReceiverDelegate() {
            @Override
            public void didSetImage(ImageReceiver imageReceiver, boolean set, boolean thumb) {
                placeholderImageView.setVisibility(set ? INVISIBLE : VISIBLE);
            }
        });

        nameTextView = new TextView(context);
        nameTextView.setTextColor(0xff212121);
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        nameTextView.setLines(1);
        nameTextView.setMaxLines(1);
        nameTextView.setSingleLine(true);
        nameTextView.setEllipsize(TextUtils.TruncateAt.END);
        nameTextView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.TOP, LocaleController.isRTL ? 8 : 72, 5, LocaleController.isRTL ? 72 : 8, 0));

        dateTextView = new TextView(context);
        dateTextView.setTextColor(0xff999999);
        dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        dateTextView.setLines(1);
        dateTextView.setMaxLines(1);
        dateTextView.setSingleLine(true);
        dateTextView.setEllipsize(TextUtils.TruncateAt.END);
        dateTextView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        addView(dateTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.TOP, LocaleController.isRTL ? 8 : 72, 30, LocaleController.isRTL ? 72 : 8, 0));

        checkBox = new CheckBoxView(context, R.drawable.round_check2);
        checkBox.setVisibility(INVISIBLE);
        addView(checkBox, LayoutHelper.createFrame(22, 22, Gravity.START | Gravity.TOP, LocaleController.isRTL ? 0 : 34, 30, LocaleController.isRTL ? 34 : 0, 0));
    }

    public void setTextAndValueAndTypeAndThumb(String text, String value, String type, String thumb, int resId) {
        nameTextView.setText(text);
        if( type != null ) {
            dateTextView.setText(value + " " + type);
        }
        else {
            dateTextView.setText(value);
        }
        if (resId == 0) {
            placeholderImageView.setImageResource(R.drawable.attach_file_inlist);
            placeholderImageView.setVisibility(VISIBLE);
        } else {
            placeholderImageView.setVisibility(INVISIBLE);
        }
        if (thumb != null || resId != 0) {
            if (thumb != null) {
                thumbImageView.setImage(thumb, "40_40", null);
            } else  {
                thumbImageView.setImageResource(resId);
            }
            thumbImageView.setVisibility(VISIBLE);
        } else {
            thumbImageView.setVisibility(INVISIBLE);
        }
    }

    public void setChecked(boolean checked, boolean animated) {
        if (checkBox.getVisibility() != VISIBLE) {
            checkBox.setVisibility(VISIBLE);
        }
        checkBox.setChecked(checked, animated);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(56) /*+ (needDivider ? 1 : 0)*/, MeasureSpec.EXACTLY));
    }
}
