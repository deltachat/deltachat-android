package utils;

import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;

import static org.thoughtcrime.securesms.map.MapDataManager.ACCURACY;
import static org.thoughtcrime.securesms.map.MapDataManager.CONTACT_ID;
import static org.thoughtcrime.securesms.map.MapDataManager.INFO_WINDOW_ID;
import static org.thoughtcrime.securesms.map.MapDataManager.LAST_LOCATION;
import static org.thoughtcrime.securesms.map.MapDataManager.MARKER_SELECTED;
import static org.thoughtcrime.securesms.map.MapDataManager.MESSAGE_ID;
import static org.thoughtcrime.securesms.map.MapDataManager.TIMESTAMP;

/**
 * Created by cyberta on 22.03.19.
 */

public class TestUtils {

    public static Feature getPointFeature(String id) {
        return getPointFeature(id, 10.00, 52.00);
    }

    public static Feature getPointFeature() {
        return getPointFeature("id1");
    }

    public static Feature getPointFeature(String id, double latitude, double longitude) {
        Point p = Point.fromLngLat(longitude, latitude);
        Feature pointFeature = Feature.fromGeometry(p, new JsonObject(), id);
        pointFeature.addBooleanProperty(MARKER_SELECTED, false);
        pointFeature.addBooleanProperty(LAST_LOCATION, false);
        pointFeature.addNumberProperty(CONTACT_ID, 1);
        pointFeature.addStringProperty(INFO_WINDOW_ID, "0_1_info_2");
        pointFeature.addNumberProperty(TIMESTAMP, 123456789);
        pointFeature.addNumberProperty(MESSAGE_ID, 1);
        pointFeature.addNumberProperty(ACCURACY, 12);
        return Feature.fromJson(pointFeature.toJson());
    }

}
