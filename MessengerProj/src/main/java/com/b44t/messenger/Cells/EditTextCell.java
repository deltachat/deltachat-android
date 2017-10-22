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
 *******************************************************************************
 *
 * File:    EditTextCell.java
 * Purpose: A simple text-edit-cell that can be used in list layouts
 *
 ******************************************************************************/


package com.b44t.messenger.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.Components.LayoutHelper;

public class EditTextCell extends FrameLayout {

    private EditText editView;
    private TextView labelTextView;
    private static Paint paint;
    private boolean needDivider;
    private boolean useLabel;
    private boolean multiLine;

    public EditTextCell(Context context) {
        this(context, true, false);
    }

    public EditTextCell(Context context, boolean useLabel) {
        this(context, useLabel, false);
    }

    public EditTextCell(Context context, boolean useLabel__, boolean multiLine__) {
        super(context);
        useLabel = useLabel__;
        multiLine = multiLine__;

        if (paint == null) {
            paint = new Paint();
            paint.setColor(0xffd9d9d9);
            paint.setStrokeWidth(1);
        }

        labelTextView = new TextView(context);
        labelTextView.setTextColor(0xff212121);
        labelTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        labelTextView.setGravity(Gravity.START);
        labelTextView.setLines(1);
        labelTextView.setMaxLines(1);
        labelTextView.setSingleLine(true);
        labelTextView.setPadding(0, 0, 0, 0);
        addView(labelTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.TOP,
                    17, 8, 17, 0));

        editView = new EditText(context);
        editView.setTextColor(0xff212121);
        editView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18); // normal text is 16
        int addImeFlag = 0, addInputType = 0;
        if( multiLine__ ) {
            editView.setLines(2);
            editView.setMaxLines(2);
            editView.setSingleLine(false);
            editView.setVerticalScrollBarEnabled(true);
            editView.setHorizontalScrollBarEnabled(false);
            editView.setMinimumHeight(AndroidUtilities.dp(100));
            addInputType = EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE|EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES;
        }
        else {
            editView.setLines(1);
            editView.setMaxLines(1);
            editView.setSingleLine(true);
            addInputType = InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
            addImeFlag = EditorInfo.IME_ACTION_DONE; // just close the keyboard, NEXT would not work as the other entries nay not yet loaded
        }
        editView.setHintTextColor(0xffBBBBBB); // was: 0xff979797
        editView.setGravity(Gravity.START);
        editView.setInputType(editView.getInputType()|addInputType);
        editView.setImeOptions(addImeFlag|EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        addView(editView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.TOP,
                17, useLabel? 25 : 25-17, 17, multiLine?17:0));

        setBackgroundColor(0xffffffff);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int dpheight = multiLine? 49*2 : 49;
        if( useLabel ) {
            dpheight += 15;
        }
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(dpheight)+(needDivider ? 1 : 0), MeasureSpec.EXACTLY));
    }

    public void setValueHintAndLabel(String value, String hint, String label, boolean divider) {
        editView.setText(value);
        editView.setSelection(value.length());

        editView.setHint(hint);

        if( label.isEmpty()) {
            labelTextView.setVisibility(INVISIBLE);
        }
        else {
            labelTextView.setVisibility(VISIBLE);
            labelTextView.setText(label + ":");
        }

        needDivider = divider;
        setWillNotDraw(!divider);
    }

    public String getValue()
    {
        return editView.getText().toString();
    }

    public EditText getEditTextView()
    {
        return editView;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (needDivider) {
            canvas.drawLine(getPaddingLeft(), getHeight() - 1, getWidth() - getPaddingRight(), getHeight() - 1, paint);
        }
    }
}
