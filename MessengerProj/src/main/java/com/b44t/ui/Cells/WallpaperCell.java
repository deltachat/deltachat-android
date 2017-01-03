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
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.R;
import com.b44t.messenger.TLRPC;
import com.b44t.ui.Components.BackupImageView;
import com.b44t.ui.Components.LayoutHelper;

public class WallpaperCell extends FrameLayout {

    private BackupImageView imageView;
    private View selectionView;
    private ImageView imageView2;

    public WallpaperCell(Context context) {
        super(context);

        imageView = new BackupImageView(context);
        addView(imageView, LayoutHelper.createFrame(100, 100, Gravity.LEFT | Gravity.BOTTOM));

        imageView2 = new ImageView(context);
        imageView2.setImageResource(R.drawable.ic_gallery_background);
        imageView2.setScaleType(ImageView.ScaleType.CENTER);
        addView(imageView2, LayoutHelper.createFrame(100, 100, Gravity.LEFT | Gravity.BOTTOM));

        selectionView = new View(context);
        selectionView.setBackgroundResource(R.drawable.wall_selection);
        addView(selectionView, LayoutHelper.createFrame(100, 102));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(100), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(102), MeasureSpec.EXACTLY));
    }

    public void setWallpaper(TLRPC.WallPaper wallpaper, int selectedBackground) {
        if (wallpaper == null) {
            imageView.setVisibility(INVISIBLE);
            imageView2.setVisibility(VISIBLE);
            selectionView.setVisibility(selectedBackground == -1 ? View.VISIBLE : INVISIBLE);
            imageView2.setBackgroundColor(selectedBackground == -1 || selectedBackground == 1000001 ? 0x5a475866 : 0x5a000000);
        } else {
            imageView.setVisibility(VISIBLE);
            imageView2.setVisibility(INVISIBLE);
            selectionView.setVisibility(selectedBackground == wallpaper.id ? View.VISIBLE : INVISIBLE);

            if (wallpaper instanceof TLRPC.TL_wallPaperSolid) {
                imageView.setImageBitmap(null);
                imageView.setBackgroundColor(0xff000000 | wallpaper.bg_color);
            } else {
                int side = AndroidUtilities.dp(100);
                TLRPC.PhotoSize size = null;
                for (int a = 0; a < wallpaper.sizes.size(); a++) {
                    TLRPC.PhotoSize obj = wallpaper.sizes.get(a);
                    if (obj == null) {
                        continue;
                    }
                    int currentSide = obj.w >= obj.h ? obj.w : obj.h;
                    if (size == null || side > 100 && size.location != null && size.location.dc_id == Integer.MIN_VALUE || obj instanceof TLRPC.TL_photoCachedSize || currentSide <= side) {
                        size = obj;
                    }
                }

                // EDIT BY MR
                if( wallpaper.id==1000001 ) {
                    imageView.setImageResource(R.drawable.background_hd);
                    size = null;
                }
                // /EDIT BY MR

                if (size != null && size.location != null) {
                    imageView.setImage(size.location, "100_100", (Drawable) null);
                }
                imageView.setBackgroundColor(0x5a475866);
            }
        }
    }
}
