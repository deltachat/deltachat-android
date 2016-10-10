/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *     Copyright (C) 2016 Björn Petersen Software Design and Development
 *                   Contact: r10s@b44t.com, http://b44t.com
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
 * File:    MrEditTextCell.java
 * Authors: Björn Petersen
 * Purpose: A simple text-edit-cell that can be used in list layouts
 *
 ******************************************************************************/


package org.telegram.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.Components.LayoutHelper;

public class MrEditTextCell extends FrameLayout {

    private EditText editView;
    private TextView labelTextView;
    private static Paint paint;
    private boolean needDivider;
    private String originalValue;

    public MrEditTextCell(Context context) {
        super(context);

        if (paint == null) {
            paint = new Paint();
            paint.setColor(0xffd9d9d9);
            paint.setStrokeWidth(1);
        }

        labelTextView = new TextView(context);
        labelTextView.setTextColor(0xff8a8a8a);
        labelTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        labelTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        labelTextView.setLines(1);
        labelTextView.setMaxLines(1);
        labelTextView.setSingleLine(true);
        labelTextView.setPadding(0, 0, 0, 0);
        addView(labelTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP,
                    17, 10, 17, 0));


        editView = new EditText(context);
        editView.setTextColor(0xff212121); // ok
        editView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18); // ok, normal text is 16
        editView.setLines(1);
        editView.setMaxLines(1);
        editView.setSingleLine(true);
        editView.setHintTextColor(0xff979797);
        editView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        //editView.setInputType(0);
        editView.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        AndroidUtilities.clearCursorDrawable(editView);
        /*
        e.setPadding(0, 0, 0, 0);
        e.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE && doneButton != null) {
                    doneButton.performClick();
                    return true;
                }
                return false;
            }
        });
        */

        addView(editView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP,
                17, 25, 17, 0));

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));
    }

    public void setValueHintAndLabel(String value, String hint, String label, boolean divider) {
        originalValue = value;

        editView.setText(value);
        editView.setHint(hint);
        editView.setSelection(value.length());

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

    public boolean isValueModified()
    {
        return !originalValue.equals(getValue());
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
