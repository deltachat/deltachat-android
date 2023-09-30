package org.thoughtcrime.securesms.components.rangeslider;

import android.graphics.Canvas;
import android.graphics.Paint;

class TextLayer {

    private final Paint paint;

    TextLayer(float fontSize, int color) {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setTextSize(fontSize);
        paint.setTextAlign(Paint.Align.CENTER);
    }

    void draw(Canvas canvas, String text, float offsetX, float offsetY) {
        canvas.drawText(text, offsetX, offsetY, paint);
    }
}
