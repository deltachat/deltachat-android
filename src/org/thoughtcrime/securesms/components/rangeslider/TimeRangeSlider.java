package org.thoughtcrime.securesms.components.rangeslider;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.DynamicLanguage;

import java.util.Locale;

/**
 * Created by cyberta on 24.03.19.
 */

public class TimeRangeSlider extends RangeSliderView implements RangeSliderView.OnValueChangedListener {
    private static final int DEFAULT_TIMEFRAME_2D = 60 * 60 * 24 * 2; // 2d
    private static final float DEFAULT_DELTA = 1000 * 60 * 30; // 30 min
    final int timeFrame; // timeframe in seconds

    final Locale locale;
    OnTimestampChangedListener listener;
    private final TextLayer minRangeDisplayLabel;
    private final TextLayer maxRangeDisplayLabel;

    float displayLabelOffsetBelow;


    public TimeRangeSlider(Context context) {
        this(context, null, 0);
    }

    public TimeRangeSlider(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimeRangeSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RangeSliderView);
        timeFrame = typedArray.getInt(
                R.styleable.TimeRangeSlider_timeFrame,
                DEFAULT_TIMEFRAME_2D
        );

        locale = DynamicLanguage.getSelectedLocale(context);
        super.setOnValueChangedListener(this);
        minRangeDisplayLabel = new TextLayer(displayTextFontSize, trackHighlightTintColor);
        maxRangeDisplayLabel = new TextLayer(displayTextFontSize, trackHighlightTintColor);
        deltaValue = getDelta();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        displayLabelOffsetBelow = offsetY + thumbInnerRadius + thumbOutlineSize + trackHeight / 2 + displayTextBasicOffsetY;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float minValueOffsetX = getPositionFromIndex(minValue);
        float maxValueOffsetX = getPositionFromIndex(maxValue);

        deltaValue = getDelta();
        if (minValue == maxValue) {
            if (minValueOffsetX - getDeltaInPixel() >= sliderPaddingLeft) {
                minRangeDisplayLabel.draw(
                        canvas,
                        getDeltaInTime(getContext()),
                        minValueOffsetX - getDeltaInPixel(),
                        displayLabelOffsetBelow
                );
            }

            if (maxValueOffsetX + getDeltaInPixel() <= (getWidth() - sliderPaddingRight)) {
                maxRangeDisplayLabel.draw(
                        canvas,
                        getDeltaInTime(getContext()),
                        maxValueOffsetX + getDeltaInPixel(),
                        displayLabelOffsetBelow
                );
            }

        }
    }

    public interface OnTimestampChangedListener {
        void onTimestampChanged(long startTimestamp, long stopTimestamp);

        /**
         * filter for lastPosition beginning from startTimestamp to now
         * @param startTimestamp begin of time frame
         */
        void onFilterLastPosition(long startTimestamp);
    }


    public void setOnTimestampChangedListener(OnTimestampChangedListener timestampChangedListener) {
        listener = timestampChangedListener;
    }

    @Override
    public void onValueChanged(int minValue, int maxValue) {
        if (listener != null) {
            long minTimeStamp = getTimestampForValue(minValue);
            if (maxValue == getCount()) {
                if (minValue == maxValue) {
                    listener.onFilterLastPosition(System.currentTimeMillis() - (long) DEFAULT_DELTA);
                } else {
                    listener.onFilterLastPosition(minTimeStamp);
                }
            } else if (minValue == maxValue) {
                    // filter for time of event with delta before and after
                    listener.onTimestampChanged(minTimeStamp - (long) DEFAULT_DELTA, minTimeStamp + (long) DEFAULT_DELTA);
            } else {
                //filter for time span
                listener.onTimestampChanged(minTimeStamp, getTimestampForValue(maxValue));
            }
        }
    }

    @Override
    public String parseMinValueDisplayText(int minValue) {
        if (minValue == maxValue) {
            return "";
        }
        return getStringForValue(minValue);
    }

    @Override
    public String parseMaxValueDisplayText(int maxValue) {
        return getStringForValue(maxValue);
    }


    private String getStringForValue(int value) {
        return DateUtils.getExtendedRelativeTimeSpanString(this.getContext(), locale, getTimestampForValue(value));
    }

    private long getTimestampForValue(int value) {
        return  System.currentTimeMillis() - ((timeFrame / getCount()) * (getCount() - value) * 1000);
    }

    public String getDeltaInTime(Context context) {
        return DateUtils.getFormattedTimespan(context, (int) (timeFrame * 1000 / getCount() * getDelta()));
    }

    /**
     * When filtering for a point in time a delta time span is added to improve the search results.
     * @return normalized delta
     */
    private float getDelta() {
        return DEFAULT_DELTA / (timeFrame * 1000) * getCount();
    }


}
