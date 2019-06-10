package org.thoughtcrime.securesms.video.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.ArrayList;

public class VideoTimelineView extends View {

    private long videoLength = 0;
    private float progressLeft = 0;
    private float progressRight = 1;
    private Paint paint;
    private Paint paint2;
    private boolean pressedLeft = false;
    private boolean pressedRight = false;
    private float pressDx = 0;
    private MediaMetadataRetriever mediaMetadataRetriever = null;
    private VideoTimelineViewDelegate delegate = null;
    private ArrayList<Bitmap> frames = new ArrayList<>();
    private AsyncTask<Integer, Integer, Bitmap> currentTask = null;
    private static final Object sync = new Object();
    private long frameTimeOffset = 0;
    private int frameWidth = 0;
    private int frameHeight = 0;
    private int framesToLoad = 0;
    private Drawable pickDrawable = null;
    private Context context;

    public interface VideoTimelineViewDelegate {
        void onLeftProgressChanged(float progress);
        void onRifhtProgressChanged(float progress);
    }

    private void init(Context context) {
        this.context = context;
        paint = new Paint();
        paint.setColor(0xff66d1ee);
        paint2 = new Paint();
        paint2.setColor(0x7f000000);
        pickDrawable = getResources().getDrawable(R.drawable.videotrimmer);
    }

    public VideoTimelineView(Context context) {
        super(context);
        init(context);
    }

    public VideoTimelineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VideoTimelineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public float getLeftProgress() {
        return progressLeft;
    }

    public float getRightProgress() {
        return progressRight;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) {
            return false;
        }
        float x = event.getX();
        float y = event.getY();

        int width = getMeasuredWidth() - ViewUtil.dpToPx(context, 32);
        int startX = (int)(width * progressLeft) + ViewUtil.dpToPx(context, 16);
        int endX = (int)(width * progressRight) + ViewUtil.dpToPx(context, 16);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int additionWidth = ViewUtil.dpToPx(context, 12);
            if (startX - additionWidth <= x && x <= startX + additionWidth && y >= 0 && y <= getMeasuredHeight()) {
                pressedLeft = true;
                pressDx = (int)(x - startX);
                getParent().requestDisallowInterceptTouchEvent(true);
                invalidate();
                return true;
            } else if (endX - additionWidth <= x && x <= endX + additionWidth && y >= 0 && y <= getMeasuredHeight()) {
                pressedRight = true;
                pressDx = (int)(x - endX);
                getParent().requestDisallowInterceptTouchEvent(true);
                invalidate();
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (pressedLeft) {
                pressedLeft = false;
                return true;
            } else if (pressedRight) {
                pressedRight = false;
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (pressedLeft) {
                startX = (int)(x - pressDx);
                if (startX < ViewUtil.dpToPx(context, 16)) {
                    startX = ViewUtil.dpToPx(context, 16);
                } else if (startX > endX) {
                    startX = endX;
                }
                progressLeft = (float)(startX - ViewUtil.dpToPx(context, 16)) / (float)width;
                if (delegate != null) {
                    delegate.onLeftProgressChanged(progressLeft);
                }
                invalidate();
                return true;
            } else if (pressedRight) {
                endX = (int)(x - pressDx);
                if (endX < startX) {
                    endX = startX;
                } else if (endX > width + ViewUtil.dpToPx(context, 16)) {
                    endX = width + ViewUtil.dpToPx(context, 16);
                }
                progressRight = (float)(endX - ViewUtil.dpToPx(context, 16)) / (float)width;
                if (delegate != null) {
                    delegate.onRifhtProgressChanged(progressRight);
                }
                invalidate();
                return true;
            }
        }
        return false;
    }

    public void setVideoPath(String path) {
        mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            mediaMetadataRetriever.setDataSource(path);
            String duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            videoLength = Long.parseLong(duration);
        } catch (Exception e) {

        }
    }

    public void setDelegate(VideoTimelineViewDelegate delegate) {
        this.delegate = delegate;
    }

    private void reloadFrames(int frameNum) {
        if (mediaMetadataRetriever == null) {
            return;
        }
        if (frameNum == 0) {
            frameHeight = ViewUtil.dpToPx(context, 40);
            framesToLoad = (getMeasuredWidth() - ViewUtil.dpToPx(context, 16)) / frameHeight;
            frameWidth = (int)Math.ceil((float)(getMeasuredWidth() - ViewUtil.dpToPx(context, 16)) / (float)framesToLoad);
            frameTimeOffset = videoLength / framesToLoad;
        }
        currentTask = new AsyncTask<Integer, Integer, Bitmap>() {
            private int frameNum = 0;

            @Override
            protected Bitmap doInBackground(Integer... objects) {
                frameNum = objects[0];
                Bitmap bitmap = null;
                if (isCancelled()) {
                    return null;
                }
                try {
                    bitmap = mediaMetadataRetriever.getFrameAtTime(frameTimeOffset * frameNum * 1000);
                    if (isCancelled()) {
                        return null;
                    }
                    if (bitmap != null) {
                        Bitmap result = Bitmap.createBitmap(frameWidth, frameHeight, bitmap.getConfig());
                        Canvas canvas = new Canvas(result);
                        float scaleX = (float) frameWidth / (float) bitmap.getWidth();
                        float scaleY = (float) frameHeight / (float) bitmap.getHeight();
                        float scale = scaleX > scaleY ? scaleX : scaleY;
                        int w = (int) (bitmap.getWidth() * scale);
                        int h = (int) (bitmap.getHeight() * scale);
                        Rect srcRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                        Rect destRect = new Rect((frameWidth - w) / 2, (frameHeight - h) / 2, w, h);
                        canvas.drawBitmap(bitmap, srcRect, destRect, null);
                        bitmap.recycle();
                        bitmap = result;
                    }
                } catch (Exception e) {

                }
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (!isCancelled()) {
                    frames.add(bitmap);
                    invalidate();
                    if (frameNum < framesToLoad) {
                        reloadFrames(frameNum + 1);
                    }
                }
            }
        };
        currentTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, frameNum, null, null);
    }

    public void destroy() {
        synchronized (sync) {
            try {
                if (mediaMetadataRetriever != null) {
                    mediaMetadataRetriever.release();
                    mediaMetadataRetriever = null;
                }
            } catch (Exception e) {

            }
        }
        for (Bitmap bitmap : frames) {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
        frames.clear();
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
    }

    public void clearFrames() {
        for (Bitmap bitmap : frames) {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
        frames.clear();
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getMeasuredWidth() - ViewUtil.dpToPx(context, 36);
        int startX = (int)(width * progressLeft) + ViewUtil.dpToPx(context, 16);
        int endX = (int)(width * progressRight) + ViewUtil.dpToPx(context, 16);

        canvas.save();
        canvas.clipRect(ViewUtil.dpToPx(context, 16), 0, width + ViewUtil.dpToPx(context, 20), ViewUtil.dpToPx(context, 44));
        if (frames.isEmpty() && currentTask == null) {
            reloadFrames(0);
        } else {
            int offset = 0;
            for (Bitmap bitmap : frames) {
                if (bitmap != null) {
                    canvas.drawBitmap(bitmap, ViewUtil.dpToPx(context, 16) + offset * frameWidth, ViewUtil.dpToPx(context, 2), null);
                }
                offset++;
            }
        }

        canvas.drawRect(ViewUtil.dpToPx(context, 16), ViewUtil.dpToPx(context, 2), startX, ViewUtil.dpToPx(context, 42), paint2);
        canvas.drawRect(endX + ViewUtil.dpToPx(context, 4), ViewUtil.dpToPx(context, 2), ViewUtil.dpToPx(context, 16) + width + ViewUtil.dpToPx(context, 4), ViewUtil.dpToPx(context, 42), paint2);

        canvas.drawRect(startX, 0, startX + ViewUtil.dpToPx(context, 2), ViewUtil.dpToPx(context, 44), paint);
        canvas.drawRect(endX + ViewUtil.dpToPx(context, 2), 0, endX + ViewUtil.dpToPx(context, 4), ViewUtil.dpToPx(context, 44), paint);
        canvas.drawRect(startX + ViewUtil.dpToPx(context, 2), 0, endX + ViewUtil.dpToPx(context, 4), ViewUtil.dpToPx(context, 2), paint);
        canvas.drawRect(startX + ViewUtil.dpToPx(context, 2), ViewUtil.dpToPx(context, 42), endX + ViewUtil.dpToPx(context, 4), ViewUtil.dpToPx(context, 44), paint);
        canvas.restore();

        int drawableWidth = pickDrawable.getIntrinsicWidth();
        int drawableHeight = pickDrawable.getIntrinsicHeight();
        pickDrawable.setBounds(startX - drawableWidth / 2, getMeasuredHeight() - drawableHeight, startX + drawableWidth / 2, getMeasuredHeight());
        pickDrawable.draw(canvas);

        pickDrawable.setBounds(endX - drawableWidth / 2 + ViewUtil.dpToPx(context, 4), getMeasuredHeight() - drawableHeight, endX + drawableWidth / 2 + ViewUtil.dpToPx(context, 4), getMeasuredHeight());
        pickDrawable.draw(canvas);
    }
}
