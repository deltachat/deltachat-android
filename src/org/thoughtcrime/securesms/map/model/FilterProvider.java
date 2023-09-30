package org.thoughtcrime.securesms.map.model;

import androidx.annotation.NonNull;

import com.mapbox.mapboxsdk.style.expressions.Expression;

import java.util.HashMap;

import static com.mapbox.mapboxsdk.style.expressions.Expression.all;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.gte;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.lte;
import static com.mapbox.mapboxsdk.style.expressions.Expression.neq;
import static org.thoughtcrime.securesms.map.MapDataManager.MESSAGE_ID;
import static org.thoughtcrime.securesms.map.MapDataManager.TIMESTAMP;
import static org.thoughtcrime.securesms.map.model.FilterProvider.FilterType.LAST_POSITION;
import static org.thoughtcrime.securesms.map.model.FilterProvider.FilterType.MESSAGES;
import static org.thoughtcrime.securesms.map.model.FilterProvider.FilterType.RANGE;

/**
 * Created by cyberta on 11.04.19.
 */

public class FilterProvider {
    public enum FilterType {
        LAST_POSITION,
        RANGE,
        MESSAGES
    }

    private final HashMap<FilterType, Expression> expressions = new HashMap();


    public void setRangeFilter(long startTimestamp, long endTimestamp) {
        removeFilter(LAST_POSITION);
        addFilter(RANGE, all(
                lte(get(TIMESTAMP), endTimestamp),
                gte(get(TIMESTAMP), startTimestamp)));
    }

    public void setLastPositionFilter(long startTimestamp) {
        removeFilter(RANGE);
        addFilter(LAST_POSITION, gte(get(TIMESTAMP), startTimestamp));
    }

    public void setMessageFilter(boolean filter) {
        if (filter) {
            addFilter(MESSAGES, neq(get(MESSAGE_ID), literal(0)));
        } else {
            removeFilter(MESSAGES);
        }
    }

    private void addFilter(FilterType type, @NonNull Expression expression) {
        expressions.put(type, expression);
    }

    private void removeFilter(FilterType type) {
        expressions.remove(type);
    }

    public Expression getMarkerFilter() {
        return all(expressions.values().toArray(new Expression[expressions.values().size()]));
    }

    public Expression getTimeFilter() {
        if (expressions.get(LAST_POSITION) != null) {
            return expressions.get(LAST_POSITION);
        } else if (expressions.get(RANGE) != null) {
            return expressions.get(RANGE);
        }

        return all();
    }

}
