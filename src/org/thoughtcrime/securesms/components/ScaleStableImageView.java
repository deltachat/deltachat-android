package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

/**
 * An image view that maintains the size of the drawable provided.
 * That means: when you set an image drawable it will be scaled to fit the screen once.
 * If you rotate the screen it will be rescaled to fit.
 * If you crop the screen (e.g. because the soft keyboard is displayed) the image is cropped instead.
 *
 * @author Angelo Fuchs
 */
public class ScaleStableImageView
    extends AppCompatImageView {

    private static final String TAG = ScaleStableImageView.class.getSimpleName();

    private Drawable defaultDrawable;

    public ScaleStableImageView(Context context) {
        this(context, null);
    }

    public ScaleStableImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScaleStableImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        defaultDrawable = drawable;
        overrideDrawable(defaultDrawable);
    }

    private void overrideDrawable(Drawable newDrawable) {
        super.setImageDrawable(newDrawable);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        if (width == 0 || height == 0) return;
        int orientation = getResources().getConfiguration().orientation;
        boolean portrait;
        if (orientation == ORIENTATION_PORTRAIT) {
            portrait = true;
        } else if (orientation == ORIENTATION_LANDSCAPE) {
            portrait = false;
        } else {
            Log.i(TAG, "orientation was: " + orientation);
            return; // something fishy happened.
        }
        if (!(defaultDrawable instanceof BitmapDrawable)) {
            return; // need Bitmap for scaling and cropping.
        }

        // if the image is already fit for the screen, just show it.
        if (defaultDrawable.getIntrinsicWidth() == width &&
            defaultDrawable.getIntrinsicHeight() == height) {
            overrideDrawable(defaultDrawable);
        }

        // don't scale; Crop.
        Bitmap original = ((BitmapDrawable) defaultDrawable).getBitmap();
        if(original.getHeight() >= height && original.getWidth() >= width) {
            Bitmap cropped;
            if (portrait) {
                int startX = (original.getWidth() - width) / 2;
                cropped = Bitmap.createBitmap(original, startX, 0, width, height);
            } else {
                int startY = (original.getHeight() - height) /2;
                cropped = Bitmap.createBitmap(original, 0, startY, width, height);
            }
            Drawable croppedDrawable = new BitmapDrawable(getResources(), cropped);
            overrideDrawable(croppedDrawable);
        } else {
            Log.e(TAG, "could not rescale background image. Original too small");
            Log.i(TAG, "image size. w: " + original.getWidth() + " h: " + original.getHeight());
        }
        super.onSizeChanged(width, height, oldWidth, oldHeight);
    }
}
