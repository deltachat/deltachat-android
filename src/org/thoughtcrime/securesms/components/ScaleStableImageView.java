package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.Util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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
    extends AppCompatImageView
    implements KeyboardAwareLinearLayout.OnKeyboardShownListener, KeyboardAwareLinearLayout.OnKeyboardHiddenListener {

    private static final String TAG = ScaleStableImageView.class.getSimpleName();

    private Drawable defaultDrawable;
    private Drawable currentDrawable;

    private boolean keyboardShown = false;

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
        if(defaultDrawable != null && defaultDrawable.equals(drawable)) {
            Log.i(TAG, "revoked new drawable, it was the same");
            return;
        }
        Log.i(TAG, "new drawable");
        defaultDrawable = drawable;
        overrideDrawable(defaultDrawable);
    }

    private void overrideDrawable(Drawable newDrawable) {
        Log.i(TAG, "override drawable");
        if (currentDrawable == newDrawable) return;
        currentDrawable = newDrawable;
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

        Log.i(TAG, "view size. w: " + width + " h: " + height + " p: " + portrait);
        Log.i(TAG, "image size. w: " + defaultDrawable.getIntrinsicWidth() + " h: " + defaultDrawable.getIntrinsicHeight());
        Log.i(TAG, "with" + (keyboardShown ? "" : "OUT") + " keyboard");
        // if the image is already fit for the screen, just show it.
        if (defaultDrawable.getIntrinsicWidth() == width &&
            defaultDrawable.getIntrinsicHeight() == height) {
            overrideDrawable(defaultDrawable);
        }

        // don't scale; Crop.
        Bitmap original = ((BitmapDrawable) defaultDrawable).getBitmap();
        if(original.getHeight() >= height && original.getWidth() >= width) {
            int startX = (original.getWidth() - width) /2;
            Bitmap cropped = Bitmap.createBitmap(original, startX, 0, width, height);
            Drawable croppedDrawable = new BitmapDrawable(getResources(), cropped);
            overrideDrawable(croppedDrawable);
        } else {
            Log.e(TAG, "could not rescale background image. Original too small");
            Log.i(TAG, "image size. w: " + original.getWidth() + " h: " + original.getHeight());
        }
        super.onSizeChanged(width, height, oldWidth, oldHeight);
    }

    @Override
    public void onKeyboardHidden() {
        keyboardShown = false;
        Log.i(TAG, "Keyboard hidden");
    }

    @Override
    public void onKeyboardShown() {
        keyboardShown = true;
        Log.i(TAG, "Keyboard shown");
    }
}
