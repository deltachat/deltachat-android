package org.thoughtcrime.securesms.components.rangeslider;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

class TrackLayer {

    private final float height;
    private final float radius;
    private int paddingLeft = 0;
    private int paddingRight = 0;

    private final Paint centerPaint;
    private final Paint backgoundPaint;

    TrackLayer(int paddingLeft, int paddingRight, float height, int color, int highlightColor) {
        this.height = height;
        radius = this.height / 2;

        centerPaint = new Paint();
        centerPaint.setAntiAlias(true);
        centerPaint.setColor(highlightColor);
        centerPaint.setStyle(Paint.Style.FILL);

        backgoundPaint = new Paint();
        backgoundPaint.setAntiAlias(true);
        backgoundPaint.setColor(color);
        backgoundPaint.setStyle(Paint.Style.FILL);

        this.paddingLeft = paddingLeft;
        this.paddingRight = paddingRight;
    }

    void draw(Canvas canvas, int width, float offsetLeft, float offsetRight, float offsetY, float delta) {
        draw(canvas, backgoundPaint, paddingLeft,  width - paddingRight,  offsetY);
        if (offsetLeft == offsetRight) {
            float maxEndX = width - paddingRight;
            float minStartX = paddingLeft;
            float offsetDeltaLeft = offsetLeft - delta;
            float offsetDeltaRight = offsetRight + delta;

            if (offsetLeft - delta < minStartX) {
                offsetDeltaLeft = minStartX;
            } else if (offsetRight + delta > maxEndX) {
                offsetDeltaRight = maxEndX;
            }

            draw(canvas, centerPaint, offsetDeltaLeft, offsetDeltaRight, offsetY);
        }
        else {
            draw(canvas, centerPaint, offsetLeft, offsetRight, offsetY);
        }

    }

    void draw(Canvas canvas, Paint paint, float startX, float endX, float offsetY) {
        canvas.drawRoundRect(
                new RectF(startX, offsetY, endX, offsetY + height),
                radius,
                radius,
                paint
        );
    }
}
