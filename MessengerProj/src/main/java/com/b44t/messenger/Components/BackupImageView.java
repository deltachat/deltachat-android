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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.b44t.messenger.ImageReceiver;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.R;
import com.b44t.messenger.TLObject;
import com.b44t.messenger.TLRPC;

public class BackupImageView extends View {

    public ImageReceiver imageReceiver;
    private int width = -1;
    private int height = -1;
    private static Drawable verifiedDrawable = null;
    private boolean drawVerifiedDrawable = false;

    public BackupImageView(Context context) {
        super(context);
        init();
    }

    public BackupImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BackupImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        imageReceiver = new ImageReceiver(this);
    }

    public void setImage(TLObject path, String filter, String ext, Drawable thumb) {
        setImage(path, null, filter, thumb, null, null, null, ext, 0);
    }

    public void setImage(TLObject path, String filter, Drawable thumb) {
        setImage(path, null, filter, thumb, null, null, null, null, 0);
    }

    public void setImage(TLObject path, String filter, Bitmap thumb) {
        setImage(path, null, filter, null, thumb, null, null, null, 0);
    }

    public void setImage(TLObject path, String filter, Drawable thumb, int size) {
        setImage(path, null, filter, thumb, null, null, null, null, size);
    }

    public void setImage(TLObject path, String filter, Bitmap thumb, int size) {
        setImage(path, null, filter, null, thumb, null, null, null, size);
    }

    public void setImage(TLObject path, String filter, TLRPC.FileLocation thumb, int size) {
        setImage(path, null, filter, null, null, thumb, null, null, size);
    }

    public void setImage(String path, String filter, Drawable thumb) {
        setImage(null, path, filter, thumb, null, null, null, null, 0);
    }

    public void setOrientation(int angle, boolean center) {
        imageReceiver.setOrientation(angle, center);
    }

    public void setImage(TLObject path, String httpUrl, String filter, Drawable thumb, Bitmap thumbBitmap, TLRPC.FileLocation thumbLocation, String thumbFilter, String ext, int size) {
        if (thumbBitmap != null) {
            thumb = new BitmapDrawable(null, thumbBitmap);
        }
        imageReceiver.setImage(path, httpUrl, filter, thumb, thumbLocation, thumbFilter, size, ext, false);
    }

    public void setImageBitmap(Bitmap bitmap) {
        imageReceiver.setImageBitmap(bitmap);
    }

    public void setImageResource(int resId) {
        imageReceiver.setImageBitmap(getResources().getDrawable(resId));
    }

    public void setImageDrawable(Drawable drawable) {
        imageReceiver.setImageBitmap(drawable);
    }

    public void setRoundRadius(int value) {
        imageReceiver.setRoundRadius(value);
    }

    public void setAspectFit(boolean value) {
        imageReceiver.setAspectFit(value);
    }

    public void setVerifiedDrawable(boolean enable)
    {
        if( enable && verifiedDrawable == null ) {
            verifiedDrawable = getResources().getDrawable(R.drawable.verified);
        }
        drawVerifiedDrawable = enable;

        // can be deleted if this gets out-of-labs
        if( drawVerifiedDrawable && MrMailbox.getConfigInt("qr_enabled", 0)==0 ) {
            drawVerifiedDrawable = false;
        }
        // /can be deleted if this gets out-of-labs
    }

    public ImageReceiver getImageReceiver() {
        return imageReceiver;
    }

    public void setSize(int w, int h) {
        width = w;
        height = h;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        imageReceiver.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        imageReceiver.onAttachedToWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int x, y, w, h;
        if (width != -1 && height != -1) {
            x = (getWidth() - width) / 2;
            y = (getHeight() - height) / 2;
            w = width;
            h = height;
        } else {
            x = 0;
            y = 0;
            w = getWidth();
            h = getHeight();
        }
        imageReceiver.setImageCoords(x, y, w, h);
        imageReceiver.draw(canvas);

        if( drawVerifiedDrawable ) {
            int verifiedWH = (int)((float)w * 0.4f);
            verifiedDrawable.setBounds(x+w-verifiedWH, y+h-verifiedWH, x+w, y+h);
            verifiedDrawable.draw(canvas);
        }

    }
}
