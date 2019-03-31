package org.thoughtcrime.securesms.map.model;

import com.mapbox.geojson.Feature;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.thoughtcrime.securesms.map.MapDataManager.TIMESTAMP;
import static utils.TestUtils.getPointFeature;

/**
 * Created by cyberta on 22.03.19.
 */

public class TimeComparableFeatureTest {

    @Test(expected = IllegalArgumentException.class)
    public void init_noTimestamp_throwIllegalStateException() throws Exception {
        Feature feature = getPointFeature();
        feature.removeProperty(TIMESTAMP);
        new TimeComparableFeature(feature);
    }

    @Test
    public void compareTo_sameTimestamp_return0() throws Exception {
        TimeComparableFeature tcf1 = new TimeComparableFeature(getPointFeature());
        TimeComparableFeature tcf2 = new TimeComparableFeature(getPointFeature());

        assertEquals(0, tcf1.compareTo(tcf2));
        assertEquals(0, tcf2.compareTo(tcf1));
    }

    @Test
    public void compareTo_biggerTimestamp_return1() throws Exception {
        Feature feature = getPointFeature();
        feature.addNumberProperty(TIMESTAMP, 234567890);
        TimeComparableFeature tcf1 = new TimeComparableFeature(getPointFeature());
        TimeComparableFeature tcf2 = new TimeComparableFeature(feature);

        assertEquals(1, tcf1.compareTo(tcf2));
    }

    @Test
    public void compareTo_biggerTimestamp_returnMinus1() throws Exception {
        Feature feature = getPointFeature();
        feature.addNumberProperty(TIMESTAMP, 234567890);
        TimeComparableFeature tcf1 = new TimeComparableFeature(feature);
        TimeComparableFeature tcf2 = new TimeComparableFeature(getPointFeature());

        assertEquals(-1, tcf1.compareTo(tcf2));
    }

    @Test
    public void compareTo_sameTimestamp_differntId_returnMinus1() throws Exception {
        Feature feature = getPointFeature();
        feature.addNumberProperty(TIMESTAMP, 234567890);
        TimeComparableFeature tcf1 = new TimeComparableFeature(feature);
        TimeComparableFeature tcf2 = new TimeComparableFeature(getPointFeature());

        assertEquals(-1, tcf1.compareTo(tcf2));
    }

    @Test
    public void equals_differentObjects_sameId_returnTrue() throws Exception {
        TimeComparableFeature tcf1 = new TimeComparableFeature(getPointFeature());
        TimeComparableFeature tcf2 = new TimeComparableFeature(getPointFeature());

        assertEquals(true, tcf1.equals(tcf2));
    }

    @Test
    public void equals_differentObjects_differentId_returnFalse() throws Exception {
        Feature feature = getPointFeature("id2");
        TimeComparableFeature tcf1 = new TimeComparableFeature(feature);
        TimeComparableFeature tcf2 = new TimeComparableFeature(getPointFeature());

        assertEquals(false, tcf1.equals(tcf2));
    }

    @Test
    public void equals_differentObjects_differentId_sameTimeStamp_returnFalse() throws Exception {
        Feature feature = getPointFeature("id2");
        TimeComparableFeature tcf1 = new TimeComparableFeature(feature);
        TimeComparableFeature tcf2 = new TimeComparableFeature(getPointFeature());

        assertEquals(false, tcf1.equals(tcf2));
    }

    @Test
    public void equals_differentObjects_sameId_differentTimeStamp_returnTrue() throws Exception {
        Feature feature = getPointFeature();
        feature.addNumberProperty(TIMESTAMP, 234567890);
        TimeComparableFeature tcf1 = new TimeComparableFeature(feature);
        TimeComparableFeature tcf2 = new TimeComparableFeature(getPointFeature());

        assertEquals(true, tcf1.equals(tcf2));
    }

}