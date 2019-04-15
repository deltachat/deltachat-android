package org.thoughtcrime.securesms.map;

import android.os.AsyncTask;

import com.b44t.messenger.DcArray;
import com.b44t.messenger.DcContext;
import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;

import org.thoughtcrime.securesms.map.model.MapSource;

import java.util.ArrayList;
import java.util.HashMap;

import static org.thoughtcrime.securesms.map.MapDataManager.ACCURACY;
import static org.thoughtcrime.securesms.map.MapDataManager.CONTACT_ID;
import static org.thoughtcrime.securesms.map.MapDataManager.LAST_LOCATION;
import static org.thoughtcrime.securesms.map.MapDataManager.MARKER_SELECTED;
import static org.thoughtcrime.securesms.map.MapDataManager.MESSAGE_ID;
import static org.thoughtcrime.securesms.map.MapDataManager.TIMESTAMP;
import static org.thoughtcrime.securesms.map.MapDataManager.TIMESTAMP_NOW;
import static org.thoughtcrime.securesms.map.MapDataManager.TIME_FRAME;

/**
 * Created by cyberta on 15.04.19.
 */

public class DataCollectionTask extends AsyncTask<Void, DataCollectionTask.DataCollection, DataCollectionTask.DataCollection> {

    public class DataCollection {
        HashMap<String, ArrayList<Feature>> featureCollections;

        public DataCollection(HashMap<String, ArrayList<Feature>> featureCollections) {
            this.featureCollections = featureCollections;
        }
    }

    private final int chatId;
    private final HashMap<Integer, MapSource> contactMapSources;
    private final LatLngBounds.Builder boundingBuilder;
    private final DcContext dcContext;

    public DataCollectionTask(DcContext context, int chatId, HashMap<Integer, MapSource> contactMapSources, LatLngBounds.Builder boundingBuilder) {
        this.chatId = chatId;
        this.contactMapSources = contactMapSources;
        this.boundingBuilder = boundingBuilder;
        this.dcContext = context;
    }

    @Override
    protected DataCollection doInBackground(Void... voids) {
        HashMap<String, ArrayList<Feature>> featureCollections = new HashMap<>();
        for (int contactId : contactMapSources.keySet()) {
            updateSource(chatId, contactId, System.currentTimeMillis() - TIME_FRAME, TIMESTAMP_NOW, featureCollections, boundingBuilder);
        }

        return new DataCollection(featureCollections);
    }

    private void updateSource(int chatId, int contactId, long startTimestamp, long endTimestamp, HashMap<String, ArrayList<Feature>> featureCollections, LatLngBounds.Builder boundingBuilder) {
        //long start = System.currentTimeMillis();
        DcArray locations = dcContext.getLocations(chatId, contactId, startTimestamp, endTimestamp);
        MapSource contactMapMetadata = contactMapSources.get(contactId);

        ArrayList<Feature> sortedPointFeatures = new ArrayList<>();
        ArrayList<Feature> sortedLineFeatures = new ArrayList<>();

        int count = locations.getCnt();
        for (int i = sortedPointFeatures.size(); i < count; i++) {
            Point point = Point.fromLngLat(locations.getLongitude(i), locations.getLatitude(i));

            Feature pointFeature = Feature.fromGeometry(point, new JsonObject(), String.valueOf(locations.getLocationId(i)));
            pointFeature.addBooleanProperty(MARKER_SELECTED, false);
            pointFeature.addBooleanProperty(LAST_LOCATION, false);
            pointFeature.addNumberProperty(CONTACT_ID, contactId);
            pointFeature.addNumberProperty(TIMESTAMP, locations.getTimestamp(i));
            pointFeature.addNumberProperty(MESSAGE_ID, locations.getMsgId(i));
            pointFeature.addNumberProperty(ACCURACY, locations.getAccuracy(i));
            sortedPointFeatures.add(pointFeature);

            if (i > 0) {
                Point lastPoint = (Point) sortedPointFeatures.get(i - 1).geometry();
                ArrayList<Point> lineSegmentPoints = new ArrayList<>(3);
                lineSegmentPoints.add(lastPoint);
                lineSegmentPoints.add(point);
                LineString l = LineString.fromLngLats(lineSegmentPoints);
                Feature lineFeature = Feature.fromGeometry(l, new JsonObject(), "l_" + pointFeature.id());
                lineFeature.addNumberProperty(TIMESTAMP, pointFeature.getNumberProperty(TIMESTAMP));
                sortedLineFeatures.add(lineFeature);
            }

            if (boundingBuilder != null) {
                boundingBuilder.include(new LatLng(locations.getLatitude(i), locations.getLongitude(i)));
            }
        }

        if (sortedPointFeatures.size() > 0) {
            sortedPointFeatures.get(0).addBooleanProperty(LAST_LOCATION, true);
        }

        featureCollections.put(contactMapMetadata.getMarkerFeatureCollection(), sortedPointFeatures);
        featureCollections.put(contactMapMetadata.getLineFeatureCollection(), sortedLineFeatures);



        //Log.d(TAG, "update Source took " + (System.currentTimeMillis() - start) + " ms");
    }


}
