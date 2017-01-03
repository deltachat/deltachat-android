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


package com.b44t.ui.Components;

import android.text.SpannableString;

import com.b44t.messenger.FileLog;

import java.lang.reflect.Field;

public class SpannableStringLight extends SpannableString {

    private static Field mSpansField;
    private static Field mSpanDataField;
    private static Field mSpanCountField;
    private static boolean fieldsAvailable;

    private Object[] mSpansOverride;
    private int[] mSpanDataOverride;
    private int mSpanCountOverride;
    private int num;

    public SpannableStringLight(CharSequence source) {
        super(source);

        try {
            mSpansOverride = (Object[]) mSpansField.get(this);
            mSpanDataOverride = (int[]) mSpanDataField.get(this);
            mSpanCountOverride = (int) mSpanCountField.get(this);
        } catch (Throwable e) {
            FileLog.e("messenger", e);
        }
    }

    public void setSpansCount(int count) {
        count += mSpanCountOverride;
        mSpansOverride = new Object[count];
        mSpanDataOverride = new int[count * 3];
        num = mSpanCountOverride;
        mSpanCountOverride = count;

        try {
            mSpansField.set(this, mSpansOverride);
            mSpanDataField.set(this, mSpanDataOverride);
            mSpanCountField.set(this, mSpanCountOverride);
        } catch (Throwable e) {
            FileLog.e("messenger", e);
        }
    }

    public static boolean isFieldsAvailable() {
        if (!fieldsAvailable && mSpansField == null) {
            try {
                mSpansField = SpannableString.class.getSuperclass().getDeclaredField("mSpans");
                mSpansField.setAccessible(true);

                mSpanDataField = SpannableString.class.getSuperclass().getDeclaredField("mSpanData");
                mSpanDataField.setAccessible(true);

                mSpanCountField = SpannableString.class.getSuperclass().getDeclaredField("mSpanCount");
                mSpanCountField.setAccessible(true);
            } catch (Throwable e) {
                FileLog.e("messenger", e);
            }
            fieldsAvailable = true;
        }
        return mSpansField != null;
    }

    public void setSpanLight(Object what, int start, int end, int flags) {
        mSpansOverride[num] = what;
        mSpanDataOverride[num * 3] = start;
        mSpanDataOverride[num * 3 + 1] = end;
        mSpanDataOverride[num * 3 + 2] = flags;
        num++;
    }

    @Override
    public void removeSpan(Object what) {
        super.removeSpan(what);
    }
}
