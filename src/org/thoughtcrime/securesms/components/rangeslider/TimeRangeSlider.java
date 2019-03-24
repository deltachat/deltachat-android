package org.thoughtcrime.securesms.components.rangeslider;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.DynamicLanguage;

import java.util.Locale;

/**
 * Created by cyberta on 24.03.19.
 */

public class TimeRangeSlider extends RangeSliderView implements RangeSliderView.OnValueChangedListener {
    // timeframe in seconds
    private static final int DEFAULT_TIMEFRAME_2D = 60 * 60 * 24 * 2;
    private static final int DEFAULT_STEPS = 60 * 10;
    int timeFrame;
    Locale locale;
    OnTimestampChangedListener listener;


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
        return  System.currentTimeMillis() - ((timeFrame / 100) * (100 - value) * 1000);
    }


}
