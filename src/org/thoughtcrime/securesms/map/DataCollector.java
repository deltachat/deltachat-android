package org.thoughtcrime.securesms.map;

import com.b44t.messenger.DcArray;
import com.b44t.messenger.DcContact;
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
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import static org.thoughtcrime.securesms.map.MapDataManager.ACCURACY;
import static org.thoughtcrime.securesms.map.MapDataManager.CONTACT_ID;
import static org.thoughtcrime.securesms.map.MapDataManager.LAST_LOCATION;
import static org.thoughtcrime.securesms.map.MapDataManager.LAST_POSITION_ICON;
import static org.thoughtcrime.securesms.map.MapDataManager.MARKER_CHAR;
import static org.thoughtcrime.securesms.map.MapDataManager.MARKER_ICON;
import static org.thoughtcrime.securesms.map.MapDataManager.MARKER_SELECTED;
import static org.thoughtcrime.securesms.map.MapDataManager.MESSAGE_ID;
import static org.thoughtcrime.securesms.map.MapDataManager.TIMESTAMP;

/**
 * Created by cyberta on 18.04.19.
 */

public class DataCollector {


    private final DcContext dcContext;
    private ConcurrentHashMap<Integer, MapSource> contactMapSources;
    private ConcurrentHashMap<String, LinkedList<Feature>> featureCollections;
    private ConcurrentHashMap<Integer, Feature> lastPositions;
    private LatLngBounds.Builder boundingBuilder;

    public DataCollector(DcContext dcContext,
                         ConcurrentHashMap<Integer, MapSource> contactMapSources,
                         ConcurrentHashMap<String, LinkedList<Feature>> featureCollections,
                         ConcurrentHashMap<Integer, Feature> lastPositions,
                         LatLngBounds.Builder boundingBuilder) {
        this.dcContext = dcContext;
        this.contactMapSources = contactMapSources;
        this.featureCollections = featureCollections;
        this.lastPositions = lastPositions;
        this.boundingBuilder = boundingBuilder;
    }


    public int updateSource(int chatId,
                              int contactId,
                              long startTimestamp,
                              long endTimestamp) {
        DcArray locations = dcContext.getLocations(chatId, contactId, startTimestamp, endTimestamp);
        int count = locations.getCnt();
        if (count == 0) {
            return 0;
        }

        MapSource contactMapMetadata = contactMapSources.get(contactId);
        if (contactMapMetadata == null) {
            contactMapMetadata = addContactMapSource(contactMapSources, contactId);
        }

        LinkedList<Feature> sortedPointFeatures = featureCollections.get(contactMapMetadata.getMarkerFeatureCollection());
        if (sortedPointFeatures != null && sortedPointFeatures.size() == count) {
            return -1;
        } else {
            sortedPointFeatures = new LinkedList<>();
        }
        LinkedList<Feature> sortedLineFeatures = new LinkedList<>();

        for (int i = count - 1; i >= 0; i--) {
            Point point = Point.fromLngLat(locations.getLongitude(i), locations.getLatitude(i));

            String codepointChar =
                    locations.getMarker(i) != null ?
                            locations.getMarker(i) :
                            "";

            Feature pointFeature = Feature.fromGeometry(point, new JsonObject(), String.valueOf(locations.getLocationId(i)));
            pointFeature.addBooleanProperty(MARKER_SELECTED, false);
            pointFeature.addBooleanProperty(LAST_LOCATION, false);
            pointFeature.addNumberProperty(CONTACT_ID, contactId);
            pointFeature.addNumberProperty(TIMESTAMP, locations.getTimestamp(i));
            pointFeature.addNumberProperty(MESSAGE_ID, locations.getMsgId(i));
            pointFeature.addNumberProperty(ACCURACY, locations.getAccuracy(i));
            pointFeature.addStringProperty(MARKER_CHAR, codepointChar);
            pointFeature.addStringProperty(MARKER_ICON, contactMapMetadata.getMarkerIcon());
            sortedPointFeatures.addFirst(pointFeature);

            if (sortedPointFeatures.size() > 1) {
                Point lastPoint = (Point) sortedPointFeatures.get(1).geometry();
                ArrayList<Point> lineSegmentPoints = new ArrayList<>(3);
                lineSegmentPoints.add(lastPoint);
                lineSegmentPoints.add(point);
                LineString l = LineString.fromLngLats(lineSegmentPoints);
                Feature lineFeature = Feature.fromGeometry(l, new JsonObject(), "l_" + pointFeature.id());
                lineFeature.addNumberProperty(TIMESTAMP, pointFeature.getNumberProperty(TIMESTAMP));
                sortedLineFeatures.addFirst(lineFeature);
            }

            if (boundingBuilder != null) {
                boundingBuilder.include(new LatLng(locations.getLatitude(i), locations.getLongitude(i)));
            }
        }

        if (sortedPointFeatures.size() > 0) {
            Feature lastPostion = sortedPointFeatures.getFirst();
            lastPostion.addStringProperty(LAST_POSITION_ICON, contactMapMetadata.getMarkerLastPositon());
            lastPostion.removeProperty(MARKER_ICON);
            lastPostion.addBooleanProperty(LAST_LOCATION, true);
            lastPositions.put(contactId, lastPostion);
        }

        featureCollections.put(contactMapMetadata.getMarkerFeatureCollection(), sortedPointFeatures);
        featureCollections.put(contactMapMetadata.getLineFeatureCollection(), sortedLineFeatures);

        return count;
    }

    private MapSource addContactMapSource(ConcurrentHashMap<Integer, MapSource> contactMapSources, int contactId) {
        if (contactMapSources.get(contactId) != null) {
            return contactMapSources.get(contactId);
        }

        DcContact contact = dcContext.getContact(contactId);
        MapSource contactMapSource = new MapSource(contactId);
        contactMapSource.setColor(contact.getColor());
        contactMapSources.put(contactId, contactMapSource);
        return contactMapSource;
    }
}
