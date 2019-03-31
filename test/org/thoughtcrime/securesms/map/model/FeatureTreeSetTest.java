package org.thoughtcrime.securesms.map.model;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.thoughtcrime.securesms.map.MapDataManager.TIMESTAMP;
import static utils.TestUtils.getPointFeature;

/**
 * Created by cyberta on 22.03.19.
 */
public class FeatureTreeSetTest {

    @Test
    public void add_differentFeaturesIds_differentTimestamps_descendingOrder() {
        FeatureTreeSet featureTreeSet = new FeatureTreeSet();

        Feature point2 = getPointFeature("3");
        point2.addNumberProperty(TIMESTAMP, 2);
        featureTreeSet.add(new TimeComparableFeature(point2));

        Feature point = getPointFeature("2");
        point.addNumberProperty(TIMESTAMP, 1);
        featureTreeSet.add(new TimeComparableFeature(point));

        featureTreeSet.add(new TimeComparableFeature(getPointFeature("1")));

        Iterator<TimeComparableFeature> iterator = featureTreeSet.iterator();
        assertEquals(123456789, iterator.next().getFeature().getNumberProperty(TIMESTAMP).longValue());
        assertEquals(2, iterator.next().getFeature().getNumberProperty(TIMESTAMP).longValue());
        assertEquals(1, iterator.next().getFeature().getNumberProperty(TIMESTAMP).longValue());

    }


    @Test
    public void add_sameFeaturesIds_differentTimestamps_descendingOrder() {
        FeatureTreeSet featureTreeSet = new FeatureTreeSet();

        Feature point2 = getPointFeature();
        point2.addNumberProperty(TIMESTAMP, 2);
        featureTreeSet.add(new TimeComparableFeature(point2));

        Feature point = getPointFeature();
        point.addNumberProperty(TIMESTAMP, 1);
        featureTreeSet.add(new TimeComparableFeature(point));

        featureTreeSet.add(new TimeComparableFeature(getPointFeature("1")));


        Iterator<TimeComparableFeature> iterator = featureTreeSet.iterator();
        assertEquals(123456789, iterator.next().getFeature().getNumberProperty(TIMESTAMP).longValue());
        assertEquals(2, iterator.next().getFeature().getNumberProperty(TIMESTAMP).longValue());
        assertEquals(1, iterator.next().getFeature().getNumberProperty(TIMESTAMP).longValue());
    }

    @Test
    public void add_sameFeaturesIds_sameTimestamps_noDuplicatedEntries() {
        FeatureTreeSet featureTreeSet = new FeatureTreeSet();
        featureTreeSet.add(new TimeComparableFeature(getPointFeature()));
        featureTreeSet.add(new TimeComparableFeature(getPointFeature()));
        assertEquals(1, featureTreeSet.size());
    }

    @Test
    public void add_sameFeaturesIds_sameTimestamps_elementReplaced() {
        FeatureTreeSet featureTreeSet = new FeatureTreeSet();
        Feature feature = getPointFeature();
        feature.addStringProperty("Test", "element1");
        featureTreeSet.add(new TimeComparableFeature(feature));
        featureTreeSet.add(new TimeComparableFeature(getPointFeature()));
        assertFalse(featureTreeSet.first().getFeature().hasProperty("Test"));
    }



    @Test
    public void getFeatureList_returnsOrderedList() throws Exception {
        FeatureTreeSet featureTreeSet = new FeatureTreeSet();

        Feature point2 = getPointFeature();
        point2.addNumberProperty(TIMESTAMP, 2);
        featureTreeSet.add(new TimeComparableFeature(point2));

        Feature point = getPointFeature();
        point.addNumberProperty(TIMESTAMP, 1);
        featureTreeSet.add(new TimeComparableFeature(point));

        featureTreeSet.add(new TimeComparableFeature(getPointFeature("1")));

        ArrayList<Feature> features = featureTreeSet.getFeatureList();
        assertEquals(123456789, features.get(0).getNumberProperty(TIMESTAMP).longValue());
        assertEquals(2, features.get(1).getNumberProperty(TIMESTAMP).longValue());
        assertEquals(1, features.get(2).getNumberProperty(TIMESTAMP).longValue());

    }

    @Test
    public void getPointList_returnsOrderedPointList() throws Exception {
        FeatureTreeSet featureTreeSet = new FeatureTreeSet();

        Feature point = getPointFeature("id2", 1.00, 1.00);
        point.addNumberProperty(TIMESTAMP, 2);
        featureTreeSet.add(new TimeComparableFeature(point));

        Feature point2 = getPointFeature("id1", 2.00, 2.00);
        point2.addNumberProperty(TIMESTAMP, 1);
        featureTreeSet.add(new TimeComparableFeature(point2));


        Feature point3 = getPointFeature("id3", 3.00, 3.00);
        point3.addNumberProperty(TIMESTAMP, 3);

        featureTreeSet.add(new TimeComparableFeature(point3));


        ArrayList<Point> points = featureTreeSet.getPointList();
        assertEquals(new Double(3.00), points.get(0).coordinates().get(0));
        assertEquals(new Double(1.00), points.get(1).coordinates().get(0));
        assertEquals(new Double(2.00), points.get(2).coordinates().get(0));

    }

}