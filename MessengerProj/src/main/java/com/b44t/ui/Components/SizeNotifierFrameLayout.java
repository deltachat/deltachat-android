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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.FileLog;

public class SizeNotifierFrameLayout extends FrameLayout {

    private Rect rect = new Rect();
    private Drawable backgroundDrawable;
    private int keyboardHeight;
    private int bottomClip;
    private SizeNotifierFrameLayoutDelegate delegate;

    public interface SizeNotifierFrameLayoutDelegate {
        void onSizeChanged(int keyboardHeight, boolean isWidthGreater);
    }

    public SizeNotifierFrameLayout(Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public void setBackgroundImage(int resourceId) {
        try {
            backgroundDrawable = getResources().getDrawable(resourceId);
        } catch (Throwable e) {

        }
    }

    public void setBackgroundImage(Drawable bitmap) {
        backgroundDrawable = bitmap;
    }

    public Drawable getBackgroundImage() {
        return backgroundDrawable;
    }

    public void setDelegate(SizeNotifierFrameLayoutDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        notifyHeightChanged();
    }

    public int getKeyboardHeight() {
        View rootView = getRootView();
        getWindowVisibleDisplayFrame(rect);
        int usableViewHeight = rootView.getHeight() - (rect.top != 0 ? AndroidUtilities.statusBarHeight : 0) - AndroidUtilities.getViewInset(rootView);
        return usableViewHeight - (rect.bottom - rect.top);
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

    public void setBottomClip(int value) {
        bottomClip = value;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (backgroundDrawable != null) {
            if (backgroundDrawable instanceof ColorDrawable) {
                if (bottomClip != 0) {
                    canvas.save();
                    canvas.clipRect(0, 0, getMeasuredWidth(), getMeasuredHeight() - bottomClip);
                }
                backgroundDrawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
                backgroundDrawable.draw(canvas);
                if (bottomClip != 0) {
                    canvas.restore();
                }
            } else {
                float scaleX = (float) getMeasuredWidth() / (float) backgroundDrawable.getIntrinsicWidth();
                float scaleY = (float) (getMeasuredHeight() + keyboardHeight) / (float) backgroundDrawable.getIntrinsicHeight();
                float scale = scaleX < scaleY ? scaleY : scaleX;
                int width = (int) Math.ceil(backgroundDrawable.getIntrinsicWidth() * scale);
                int height = (int) Math.ceil(backgroundDrawable.getIntrinsicHeight() * scale);
                int x = (getMeasuredWidth() - width) / 2;
                int y = (getMeasuredHeight() - height + keyboardHeight) / 2;
                if (bottomClip != 0) {
                    canvas.save();
                    canvas.clipRect(0, 0, width, getMeasuredHeight() - bottomClip);
                }
                backgroundDrawable.setBounds(x, y, x + width, y + height);
                backgroundDrawable.draw(canvas);
                if (bottomClip != 0) {
                    canvas.restore();
                }
            }
        } else {
            super.onDraw(canvas);
        }
    }
}
