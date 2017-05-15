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


package com.b44t.ui.Cells;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.R;
import com.b44t.ui.ActionBar.DrawerLayoutContainer;
import com.b44t.ui.Components.LayoutHelper;
import com.b44t.ui.ActionBar.Theme;

public class DrawerProfileCell extends FrameLayout {

    private TextView nameTextView;
    private TextView subtitleTextView;
    private Rect srcRect = new Rect();
    private Rect destRect = new Rect();
    private Paint paint = new Paint();

    public DrawerProfileCell(Context context) {
        super(context);
        setBackgroundColor(Theme.ACTION_BAR_COLOR);

        ImageView shadowView = new ImageView(context);
        shadowView.setScaleType(ImageView.ScaleType.FIT_XY);
        shadowView.setImageResource(R.drawable.bottom_shadow);
        addView(shadowView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 100/*EDIT BY MR, was 70*/, Gravity.START | Gravity.BOTTOM));

        nameTextView = new TextView(context);
        nameTextView.setTextColor(0xffffffff);
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, DrawerLayoutContainer.USE_DRAWER? 23 : 26);
        nameTextView.setLines(1);
        nameTextView.setMaxLines(1);
        nameTextView.setSingleLine(true);
        nameTextView.setGravity(Gravity.START);
        nameTextView.setEllipsize(TextUtils.TruncateAt.END);
        addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.BOTTOM, 16, 0, 16, 28));

        subtitleTextView = new TextView(context);
        subtitleTextView.setTextColor(0xffc2e5ff);
        subtitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, DrawerLayoutContainer.USE_DRAWER? 13 : Theme.ACTION_BAR_SUBTITLE_TEXT_SIZE);
        subtitleTextView.setLines(1);
        subtitleTextView.setMaxLines(1);
        subtitleTextView.setSingleLine(true);
        subtitleTextView.setGravity(Gravity.START);
        subtitleTextView.setTextColor(0xffffffff);
        addView(subtitleTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.BOTTOM, 16, 0, 16, 9));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mrHeight = DrawerLayoutContainer.USE_DRAWER? 180 : 150;
        if (Build.VERSION.SDK_INT >= 21) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(mrHeight) + AndroidUtilities.statusBarHeight, MeasureSpec.EXACTLY));
        } else {
            try {
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(mrHeight), MeasureSpec.EXACTLY));
            } catch (Exception e) {
                setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(mrHeight));

            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable backgroundDrawable = ApplicationLoader.getCachedWallpaper();
        if (backgroundDrawable instanceof ColorDrawable) {
            backgroundDrawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
            backgroundDrawable.draw(canvas);
        } else if (backgroundDrawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();
            float scaleX = (float) getMeasuredWidth() / (float) bitmap.getWidth();
            float scaleY = (float) getMeasuredHeight() / (float) bitmap.getHeight();
            float scale = scaleX < scaleY ? scaleY : scaleX;
            int width = (int) (getMeasuredWidth() / scale);
            int height = (int) (getMeasuredHeight() / scale);
            int x = (bitmap.getWidth() - width) / 2;
            int y = (bitmap.getHeight() - height) / 2;
            srcRect.set(x, y, x + width, y + height);
            destRect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
            canvas.drawBitmap(bitmap, srcRect, destRect, paint);
        }
    }

    public void updateUserName() {
        String displayname = MrMailbox.getConfig("displayname", ApplicationLoader.applicationContext.getString(R.string.MyAccount));
        String addr;
        if( MrMailbox.isConfigured()!=0) {
            addr = MrMailbox.getConfig("addr", "");
        }
        else {
            addr = ApplicationLoader.applicationContext.getString(R.string.AccountNotConfigured);
        }
        nameTextView.setText(displayname);
        subtitleTextView.setText(addr);
    }
}
