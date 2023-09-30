package org.thoughtcrime.securesms.components.rangeslider;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

class ThumbLayer {

    private final float radius;
    private final float outlineSize;

    private final int highlightColor;

    private final Paint outlinePaint;
    private final Paint fillPaint;

    boolean isHighlight = false;

    ThumbLayer(float radius, float outlineSize, int outlineColor, int highlightColor) {
        this.radius = radius;
        this.outlineSize = outlineSize;
        this.highlightColor = highlightColor;

        outlinePaint = new Paint();
        outlinePaint.setAntiAlias(true);
        outlinePaint.setColor(outlineColor);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(outlineSize);

        fillPaint = new Paint();
        fillPaint.setAntiAlias(true);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setStrokeWidth(0);
    }

    void draw(Canvas canvas, float cx, float cy) {
        canvas.drawCircle(cx, cy, radius, outlinePaint);

        if (isHighlight) {
            fillPaint.setColor(highlightColor);
        } else {
            fillPaint.setColor(Color.WHITE);
        }
        canvas.drawCircle(cx, cy, radius - outlineSize / 2, fillPaint);
    }
}
