package org.thoughtcrime.securesms.map.model;

import android.support.annotation.NonNull;

import com.mapbox.geojson.Feature;

import static org.thoughtcrime.securesms.map.MapDataManager.TIMESTAMP;

/**
 * Created by cyberta on 21.03.19.
 */

public class TimeComparableFeature implements Comparable<TimeComparableFeature> {
    private Feature feature;

    public TimeComparableFeature(@NonNull Feature feature) {
        if (!feature.hasProperty(TIMESTAMP)) {
            throw new IllegalArgumentException("Time comparable features need to have a TIMESTAMP property");
        }

        this.feature = feature;
    }

    public Feature getFeature() {
        return feature;
    }

    /**
     *
     * @param o for same timestamps but different featureIds, this object is considered lower
     * @return
     */
    @Override
    public int compareTo(@NonNull TimeComparableFeature o) {
        if (this.equals(o) && this.feature.getNumberProperty(TIMESTAMP).longValue() == o.getFeature().getNumberProperty(TIMESTAMP).longValue()) {
            return 0;
        }
        return this.feature.getNumberProperty(TIMESTAMP).longValue() > o.getFeature().getNumberProperty(TIMESTAMP).longValue() ? -1 : 1;
    }


    @Override
    public int hashCode() {
        return this.feature.id().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof TimeComparableFeature)) {
            return false;
        }

        TimeComparableFeature compare = (TimeComparableFeature) obj;
        return this.feature.id() != null &&
               compare.feature.id() != null &&
                this.feature.id().equals(compare.feature.id());
    }

}
