package org.thoughtcrime.securesms.components.rangeslider;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.v4.content.ContextCompat;
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
    protected float thumbRadius;
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
    protected float delta;

    protected float offsetY;
    protected float maxValueDisplayLabelOffsetY;
    protected float minValueDisplayLabelOffsetY;
    protected float trackOffsetY;

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

        int [] attributes = new int [] {android.R.attr.paddingLeft,
                android.R.attr.paddingStart,
                android.R.attr.paddingRight,
                android.R.attr.paddingEnd};

        TypedArray arr = context.obtainStyledAttributes(attrs, attributes);

        sliderPaddingLeft = arr.getDimensionPixelOffset(0,
                arr.getDimensionPixelOffset(1, -1));
        sliderPaddingRight = arr.getDimensionPixelOffset(2,
                arr.getDimensionPixelOffset(3, -1));

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
        thumbRadius = typedArray.getDimension(
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

        minValueThumb = new ThumbLayer(thumbRadius, thumbOutlineSize, trackHighlightTintColor, trackTintColor);
        minValueDisplayLabel = new TextLayer(displayTextFontSize, trackHighlightTintColor);

        maxValueThumb = new ThumbLayer(thumbRadius, thumbOutlineSize, trackHighlightTintColor, trackTintColor);
        maxValueDisplayLabel = new TextLayer(displayTextFontSize, trackHighlightTintColor);

        track = new TrackLayer(sliderPaddingLeft, sliderPaddingRight, trackHeight, trackTintColor, trackHighlightTintColor);

        values = new ArrayList<>();
        for (int index = 1; index <= 100; index++) {
            values.add(index);
        }
        delta = 0;

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
        maxValueDisplayLabelOffsetY = offsetY - thumbRadius - thumbOutlineSize - trackHeight / 2 - displayTextBasicOffsetY - displayTextFontSize;
        minValueDisplayLabelOffsetY = maxValueDisplayLabelOffsetY + displayTextFontSize + displayTextFontSize / 2;
        trackOffsetY = offsetY - trackHeight / 2;
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
        if (offsetX < sliderPaddingLeft || offsetX > getWidth() - sliderPaddingRight) {
            Log.d(TAG, "ignore touch in padding");
            return true;
        }

        float radius = getThumbRadius();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                float minValuePosition = getPositionFromIndex(minValue);
                Log.d(TAG, "action_down minVal: " + minValue + ", " + minValuePosition );
                if (offsetX >= minValuePosition - radius && offsetX <= minValuePosition + radius) {
                    minValueThumb.isHighlight = true;
                } else {
                    float maxValuePosition = getPositionFromIndex(maxValue);
                    if (offsetX >= maxValuePosition - radius && offsetX <= maxValuePosition + radius) {
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
        return ((offsetX - sliderPaddingLeft) * getCount() / getSliderWidth());
    }
    protected float getPositionFromIndex(int value) {
        Log.d(TAG, "value: " + value +", position: " + (getSliderWidth() / getCount()) * value + sliderPaddingLeft);
        return (getSliderWidth() / getCount()) * value + sliderPaddingLeft;
    }

    public float getDeltaInPixel() {
        // '* 2' is just an offset factor to increase the visibility
        return (getSliderWidth() / getCount()) * delta + (2 * thumbRadius + thumbOutlineSize);
    }

    public float getDeltaInValues() {
        return delta;
    }

    public int getCount() {
        return values.size();
    }

    public int getSliderWidth() {
        return getWidth() - sliderPaddingLeft - sliderPaddingRight;
    }

    private int getThumbRadius() {
       return (int) (thumbRadius + thumbOutlineSize / 2);
    }
}
