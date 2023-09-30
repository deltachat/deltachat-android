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
    private final Map<String, Drawable> storedSizes = new HashMap<>();

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
        storedSizes.clear();
        overrideDrawable(defaultDrawable);
    }

    private void overrideDrawable(Drawable newDrawable) {
        if (currentDrawable == newDrawable) return;
        currentDrawable = newDrawable;
        super.setImageDrawable(newDrawable);
    }

    private int landscapeWidth = 0;
    private int landscapeHeight = 0;
    private int portraitWidth = 0;
    private int portraitHeight = 0;
    private boolean keyboardShown = false;

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        if (width == 0 || height == 0) return;
        final String newKey = width + "x" + height;
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

        measureViewSize(width, height, oldWidth, oldHeight, portrait);
        // if the image is already fit for the screen, just show it.
        if (defaultDrawable.getIntrinsicWidth() == width &&
            defaultDrawable.getIntrinsicHeight() == height) {
            overrideDrawable(defaultDrawable);
        }

        // check if we have the new one already
        if (storedSizes.containsKey(newKey)) {
            super.setImageDrawable(storedSizes.get(newKey));
            return;
        }

        if (keyboardShown) {
            // don't scale; Crop.
            Drawable large;
            if (portrait)
                large = storedSizes.get(portraitWidth + "x" + portraitHeight);
            else
                large = storedSizes.get(landscapeWidth + "x" + landscapeHeight);
            if (large == null) return; // no baseline. can't work.
            Bitmap original = ((BitmapDrawable) large).getBitmap();
            if (height <= original.getHeight() && width <= original.getWidth()) {
                Bitmap cropped = Bitmap.createBitmap(original, 0, 0, width, height);
                Drawable croppedDrawable = new BitmapDrawable(getResources(), cropped);
                overrideDrawable(croppedDrawable);
            }
        } else {
            Util.runOnBackground(() -> {
                Bitmap bitmap = ((BitmapDrawable) defaultDrawable).getBitmap();
                Context context = getContext();
                try {
                    Bitmap scaledBitmap = GlideApp.with(context)
                        .asBitmap()
                        .load(bitmap)
                        .centerCrop()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .submit(width, height)
                        .get();
                    Drawable rescaled = new BitmapDrawable(getResources(), scaledBitmap);
                    storedSizes.put(newKey, rescaled);
                    Util.runOnMain(() -> overrideDrawable(rescaled));
                } catch (ExecutionException | InterruptedException ex) {
                    Log.e(TAG, "could not rescale background", ex);
                    // No background set.
                }
            });
        }
        super.onSizeChanged(width, height, oldWidth, oldHeight);
    }

    private void measureViewSize(int width, int height, int oldWidth, int oldHeight, boolean portrait) {
        if (portraitWidth != 0 && portraitHeight != 0 && landscapeWidth != 0 && landscapeHeight != 0)
            return;

        if (oldWidth == 0 && oldHeight == 0) { // screen just opened from inside the app
            if (portrait) { // portrait
                portraitHeight = height;
                portraitWidth = width;
            } else { // landscape
                landscapeHeight = height;
                landscapeWidth = width;
            }
        } else {
            if (oldWidth == portraitWidth) { // was in portrait
                if (!portrait) { // rotate to landscape
                    landscapeHeight = height;
                    landscapeWidth = width;
                }
            } else if (oldHeight == landscapeHeight) {
                if (portrait) {
                    portraitHeight = height;
                    portraitWidth = width;
                }
            }
        }
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
