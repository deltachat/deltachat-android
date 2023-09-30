package org.thoughtcrime.securesms.components.rangeslider;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;

import androidx.core.content.ContextCompat;

import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import org.thoughtcrime.securesms.R;

import java.util.ArrayList;

public class RangeSliderView extends View {

    private static final String TAG = RangeSliderView.class.getName();

    protected int trackTintColor;
    protected int trackHighlightTintColor;
    protected float trackHeight;
    protected float thumbInnerRadius;
    protected float thumbOutlineSize;
    protected float displayTextFontSize;
    protected float displayTextBasicOffsetY;
    protected int sliderPaddingLeft;
    protected int sliderPaddingRight;

    private ThumbLayer minValueThumb;
    private TextLayer minValueDisplayLabel;

    private ThumbLayer maxValueThumb;
    private TextLayer maxValueDisplayLabel;

    private TrackLayer track;

    private ArrayList<Integer> values;

    protected int minValue;
    protected int maxValue;
    protected float deltaValue;

    protected float offsetY;
    protected float maxValueDisplayLabelOffsetY;
    protected float minValueDisplayLabelOffsetY;
    protected float trackOffsetY;
    protected float thumbRadius;
    protected float sliderWidth;

    private float beginTrackOffsetX;
    private boolean isThumbViewLocked;

    private OnValueChangedListener onValueChangedListener;
    GestureDetector longPressDetector;


    public interface OnValueChangedListener {
        void onValueChanged(int minValue, int maxValue);
        String parseMinValueDisplayText(int minValue);
        String parseMaxValueDisplayText(int maxValue);
    }

    public RangeSliderView(Context context) {
        this(context, null);
    }

    public RangeSliderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);


    }

    public RangeSliderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RangeSliderView);
        trackTintColor = typedArray.getColor(
                R.styleable.RangeSliderView_trackTintColor,
                ContextCompat.getColor(context, R.color.trackTintColor)
        );
        trackHighlightTintColor = typedArray.getColor(
                R.styleable.RangeSliderView_trackHighlightTintColor,
                ContextCompat.getColor(context, R.color.trackHighlightTintColor)
        );

        Resources resources = context.getResources();
        trackHeight = typedArray.getDimension(
                R.styleable.RangeSliderView_trackHeight,
                resources.getDimension(R.dimen.slider_trackHeight)
        );
        thumbInnerRadius = typedArray.getDimension(
                R.styleable.RangeSliderView_thumbRadius,
                resources.getDimension(R.dimen.slider_thumbRadius)
        );
        thumbOutlineSize = typedArray.getDimension(
                R.styleable.RangeSliderView_thumbOutlineSize,
                resources.getDimension(R.dimen.slider_thumbOutlineSize)
        );
        displayTextFontSize = typedArray.getDimension(
                R.styleable.RangeSliderView_displayTextFontSize,
                resources.getDimension(R.dimen.slider_displayTextFontSize)
        );
        displayTextBasicOffsetY = resources.getDimension(R.dimen.slider_displayTextBasicOffsetY);
        minValue = typedArray.getInt(
                R.styleable.RangeSliderView_minValue,
                100
        );
        maxValue = typedArray.getInt(
                R.styleable.RangeSliderView_maxValue,
                100
        );

        sliderPaddingRight = typedArray.getDimensionPixelOffset(R.styleable.RangeSliderView_paddingRight, 20);
        sliderPaddingLeft = typedArray.getDimensionPixelOffset(R.styleable.RangeSliderView_paddingLeft, 20);

        typedArray.recycle();

        minValueThumb = new ThumbLayer(thumbInnerRadius, thumbOutlineSize, trackHighlightTintColor, trackTintColor);
        minValueDisplayLabel = new TextLayer(displayTextFontSize, trackHighlightTintColor);

        maxValueThumb = new ThumbLayer(thumbInnerRadius, thumbOutlineSize, trackHighlightTintColor, trackTintColor);
        maxValueDisplayLabel = new TextLayer(displayTextFontSize, trackHighlightTintColor);

        track = new TrackLayer(sliderPaddingLeft, sliderPaddingRight, trackHeight, trackTintColor, trackHighlightTintColor);

        values = new ArrayList<>();
        for (int index = 1; index <= 100; index++) {
            values.add(index);
        }
        deltaValue = 0;

        longPressDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            public void onLongPress(MotionEvent e) {
                if (minValue == maxValue && (minValueThumb.isHighlight || maxValueThumb.isHighlight)) {
                    isThumbViewLocked = true;
                    minValueThumb.isHighlight = true;
                    maxValueThumb.isHighlight = true;
                    invalidate();
                }
            }
        });
    }

    void setOnValueChangedListener(OnValueChangedListener onValueChangedListener) {
        this.onValueChangedListener = onValueChangedListener;
        invalidate();
    }

    public void setRangeValues(ArrayList<Integer> values) {
        this.values = values;
        setMinAndMaxValue(this.values.get(0), this.values.get(this.values.size() - 1));
    }

    public void setMinAndMaxValue(int minValue, int maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        invalidate();

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.offsetY = getHeight() / 5 * 3;
        maxValueDisplayLabelOffsetY = offsetY - thumbInnerRadius - thumbOutlineSize - trackHeight / 2 - displayTextBasicOffsetY - displayTextFontSize;
        minValueDisplayLabelOffsetY = maxValueDisplayLabelOffsetY + displayTextFontSize + displayTextFontSize / 2;
        trackOffsetY = offsetY - trackHeight / 2;
        sliderWidth = getWidth() - sliderPaddingLeft - sliderPaddingRight;
        thumbRadius = thumbInnerRadius + thumbOutlineSize / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float minValueOffsetX = getPositionFromIndex(minValue);
        float maxValueOffsetX = getPositionFromIndex(maxValue);
        track.draw(canvas, getWidth(), minValueOffsetX, maxValueOffsetX, trackOffsetY, getDeltaInPixel());
        minValueThumb.draw(canvas, minValueOffsetX, offsetY);
        maxValueThumb.draw(canvas, maxValueOffsetX, offsetY);

        if (onValueChangedListener != null) {
            minValueDisplayLabel.draw(
                    canvas,
                    onValueChangedListener.parseMinValueDisplayText(minValue),
                    minValueOffsetX,
                    minValueDisplayLabelOffsetY
            );
            maxValueDisplayLabel.draw(
                    canvas,
                    onValueChangedListener.parseMaxValueDisplayText(maxValue),
                    maxValueOffsetX,
                    maxValueDisplayLabelOffsetY
            );
        } else {
            minValueDisplayLabel.draw(canvas, String.valueOf(minValue), minValueOffsetX, minValueDisplayLabelOffsetY);
            maxValueDisplayLabel.draw(canvas, String.valueOf(maxValue), maxValueOffsetX, maxValueDisplayLabelOffsetY);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        longPressDetector.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (onValueChangedListener != null) {
                onValueChangedListener.onValueChanged(minValue, maxValue);
            }
            minValueThumb.isHighlight = false;
            maxValueThumb.isHighlight = false;
            isThumbViewLocked = false;
            invalidate();
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            isThumbViewLocked = false;
            if (minValueThumb.isHighlight || maxValueThumb.isHighlight) {
                if (onValueChangedListener != null) {
                    onValueChangedListener.onValueChanged(minValue, maxValue);
                }
                minValueThumb.isHighlight = false;
                maxValueThumb.isHighlight = false;
                invalidate();
                return true;
            }
        }

        int offsetX = (int) event.getX();
        if (offsetX < (sliderPaddingLeft - thumbRadius) || offsetX > getWidth() - sliderPaddingRight + thumbRadius) {
            //Log.d(TAG, "ignore touch in padding");
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                float minValuePosition = getPositionFromIndex(minValue);
                Log.d(TAG, "action_down minVal: " + minValue + ", " + minValuePosition );
                if (offsetX >= minValuePosition - thumbRadius && offsetX <= minValuePosition + thumbRadius) {
                    minValueThumb.isHighlight = true;
                } else {
                    float maxValuePosition = getPositionFromIndex(maxValue);
                    if (offsetX >= maxValuePosition - thumbRadius && offsetX <= maxValuePosition + thumbRadius) {
                        maxValueThumb.isHighlight = true;
                    }
                }

                if (minValueThumb.isHighlight || maxValueThumb.isHighlight) {
                    beginTrackOffsetX = offsetX;
                    invalidate();
                } else {
                    beginTrackOffsetX = -1;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (minValue == maxValue && beginTrackOffsetX > -1 && offsetX > beginTrackOffsetX && !maxValueThumb.isHighlight) {
                    minValueThumb.isHighlight = false;
                    maxValueThumb.isHighlight = true;
                }

                int count = values.size();
                int index = getIndexFromPosition(offsetX);
                Log.d(TAG, "move - index: " + index + ", offsetX: " + offsetX);
                if (index < 0) {
                    index = 0;
                } else if (index > count - 1) {
                    index = count - 1;
                }

                if (isThumbViewLocked) {
                    minValue = values.get(index);
                    maxValue = values.get(index);
                } else if (minValueThumb.isHighlight) {
                    if (index > values.indexOf(maxValue)) {
                        minValue = maxValue;
                    } else {
                        minValue = values.get(index);
                    }
                } else if (maxValueThumb.isHighlight) {
                    if (index < values.indexOf(minValue)) {
                        maxValue = minValue;
                    } else {
                        maxValue = values.get(index);
                    }
                }

                if (minValueThumb.isHighlight || maxValueThumb.isHighlight) {
                    invalidate();
                }
                break;
        }

        return true;
    }

    protected int getIndexFromPosition(int offsetX) {
        return  (int) (((offsetX - sliderPaddingLeft) * getCount()) / sliderWidth);
    }

    protected float getPositionFromIndex(int value) {
        return ((sliderWidth / (float) getCount()) * value) + sliderPaddingLeft;
    }

    /**
     * When filtering for a point (in time) a delta (time) span is added to improve the search results.
     * This method converts the delta to pixel related to the slider width.
     * @return
     */
    public float getDeltaInPixel() {
        float deltaInPixel = (sliderWidth / getCount()) * deltaValue;
        if (deltaInPixel >= 2 * (thumbInnerRadius + thumbOutlineSize)) {
            return deltaInPixel;
        }
        // for better visibility return a minimum limit of the diameter of the thumb element
        return 2 * thumbInnerRadius + thumbOutlineSize;
    }

    public int getCount() {
        return values.size();
    }

}
