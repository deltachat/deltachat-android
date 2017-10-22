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


package com.b44t.messenger.Components;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.b44t.messenger.AndroidUtilities;

public class SizeNotifierFrameLayoutPhoto extends FrameLayout {

    private Rect rect = new Rect();
    private int keyboardHeight;
    private SizeNotifierFrameLayoutPhotoDelegate delegate;
    private WindowManager windowManager;

    public interface SizeNotifierFrameLayoutPhotoDelegate {
        void onSizeChanged(int keyboardHeight, boolean isWidthGreater);
    }

    public SizeNotifierFrameLayoutPhoto(Context context) {
        super(context);
    }

    public void setDelegate(SizeNotifierFrameLayoutPhotoDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        notifyHeightChanged();
    }

    public int getKeyboardHeight() {
        View rootView = getRootView();
        int usableViewHeight = rootView.getHeight() - AndroidUtilities.getViewInset(rootView);
        getWindowVisibleDisplayFrame(rect);
        int top = rect.top;
        int size = AndroidUtilities.displaySize.y - top - usableViewHeight;
        if (size <= AndroidUtilities.dp(10)) {
            size = 0;
        }
        return size;
    }

    public void notifyHeightChanged() {
        if (delegate != null) {
            keyboardHeight = getKeyboardHeight();
            final boolean isWidthGreater = AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y;
            post(new Runnable() {
                @Override
                public void run() {
                    if (delegate != null) {
                        delegate.onSizeChanged(keyboardHeight, isWidthGreater);
                    }
                }
            });
        }
    }
}
