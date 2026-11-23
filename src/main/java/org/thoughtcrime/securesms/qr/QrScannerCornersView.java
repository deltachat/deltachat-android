package org.thoughtcrime.securesms.qr;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.thoughtcrime.securesms.util.ViewUtil;

public class QrScannerCornersView extends View {

  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final float strokeWidth = ViewUtil.dpToPx(2);
  private final float cornerLength = ViewUtil.dpToPx(24);

  public QrScannerCornersView(@NonNull Context context) {
    this(context, null);
  }

  public QrScannerCornersView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public QrScannerCornersView(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    paint.setColor(Color.WHITE);
    paint.setStrokeWidth(strokeWidth);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeCap(Paint.Cap.SQUARE);
  }

  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    super.onDraw(canvas);

    float left = getPaddingLeft() + strokeWidth / 2;
    float top = getPaddingTop() + strokeWidth / 2;
    float right = getWidth() - getPaddingRight() - strokeWidth / 2;
    float bottom = getHeight() - getPaddingBottom() - strokeWidth / 2;
    float corner = Math.min(cornerLength, Math.min(getWidth(), getHeight()) / 4.0f);

    canvas.drawLine(left, top, left + corner, top, paint);
    canvas.drawLine(left, top, left, top + corner, paint);

    canvas.drawLine(right - corner, top, right, top, paint);
    canvas.drawLine(right, top, right, top + corner, paint);

    canvas.drawLine(left, bottom - corner, left, bottom, paint);
    canvas.drawLine(left, bottom, left + corner, bottom, paint);

    canvas.drawLine(right, bottom - corner, right, bottom, paint);
    canvas.drawLine(right - corner, bottom, right, bottom, paint);
  }
}
