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
    private static final float DEFAULT_DELTA = 1000*60*30; // 30 min
    int timeFrame; // timeframe in seconds

    Locale locale;
    OnTimestampChangedListener listener;
    private TextLayer minRangeDisplayLabel;
    private TextLayer maxRangeDisplayLabel;


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
        delta = DEFAULT_DELTA / (timeFrame * 1000) * getCount();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float offsetY = getHeight() / 2;
        float minValueOffsetX = getPositionFromIndex(minValue);
        float maxValueOffsetX = getPositionFromIndex(maxValue);
        float displayLabelOffsetBelow = offsetY + thumbRadius + thumbOutlineSize + displayTextFontSize;

        if (minValue == maxValue) {
            if (minValueOffsetX - getDeltaInPixel() >= sliderPaddingLeft
                    ) {
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
        void onValueChanged(long startTimestamp, long stopTimestamp);
    }


    public void setOnTimestampChangedListener(OnTimestampChangedListener timestampChangedListener) {
        listener = timestampChangedListener;
    }

    @Override
    public void onValueChanged(int minValue, int maxValue) {
        if (listener != null) {
            listener.onValueChanged(getTimestampForValue(minValue), getTimestampForValue(maxValue));
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
        if (minValue == maxValue && maxValue == getCount()) {
            return getContext().getResources().getString(R.string.filter_last_position);
        }
        return getStringForValue(maxValue);
    }


    private String getStringForValue(int value) {
        return DateUtils.getExtendedRelativeTimeSpanString(this.getContext(), locale, getTimestampForValue(value));
    }

    private long getTimestampForValue(int value) {
        return  System.currentTimeMillis() - ((timeFrame / getCount()) * (getCount() - value) * 1000);
    }

    public String getDeltaInTime(Context context) {
        return DateUtils.getFormattedTimespan(context, (int) (timeFrame * 1000 / getCount() * getDeltaInValues()));
    }


}
