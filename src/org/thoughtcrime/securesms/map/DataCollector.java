package org.thoughtcrime.securesms.map;

import com.b44t.messenger.DcArray;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;

import org.thoughtcrime.securesms.components.emoji.EmojiProvider;
import org.thoughtcrime.securesms.map.model.MapSource;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.thoughtcrime.securesms.map.MapDataManager.ACCURACY;
import static org.thoughtcrime.securesms.map.MapDataManager.CONTACT_ID;
import static org.thoughtcrime.securesms.map.MapDataManager.IS_EMOJI_CHAR;
import static org.thoughtcrime.securesms.map.MapDataManager.IS_POI;
import static org.thoughtcrime.securesms.map.MapDataManager.LAST_LOCATION;
import static org.thoughtcrime.securesms.map.MapDataManager.LAST_POSITION_ICON;
import static org.thoughtcrime.securesms.map.MapDataManager.LAST_POSITION_LABEL;
import static org.thoughtcrime.securesms.map.MapDataManager.MARKER_CHAR;
import static org.thoughtcrime.securesms.map.MapDataManager.MARKER_ICON;
import static org.thoughtcrime.securesms.map.MapDataManager.MARKER_SELECTED;
import static org.thoughtcrime.securesms.map.MapDataManager.MESSAGE_ID;
import static org.thoughtcrime.securesms.map.MapDataManager.POI_LONG_DESCRIPTION;
import static org.thoughtcrime.securesms.map.MapDataManager.TIMESTAMP;

/**
 * Created by cyberta on 18.04.19.
 */

public class DataCollector {


    private final DcContext dcContext;
    private ConcurrentHashMap<Integer, MapSource> contactMapSources;
    private ConcurrentHashMap<String, LinkedList<Feature>> featureCollections;
    private ConcurrentHashMap<Integer, Feature> lastPositions;
    private Set<String> emojiCodePoints;
    private LatLngBounds.Builder boundingBuilder;
    private EmojiProvider emojiProvider;

    public DataCollector(DcContext dcContext,
                         ConcurrentHashMap<Integer, MapSource> contactMapSources,
                         ConcurrentHashMap<String, LinkedList<Feature>> featureCollections,
                         ConcurrentHashMap<Integer, Feature> lastPositions,
                         Set<String> emojiCodePoints,
                         EmojiProvider emojiProvider,
                         LatLngBounds.Builder boundingBuilder) {
        this.dcContext = dcContext;
        this.contactMapSources = contactMapSources;
        this.featureCollections = featureCollections;
        this.lastPositions = lastPositions;
        this.boundingBuilder = boundingBuilder;
        this.emojiCodePoints = emojiCodePoints;
        this.emojiProvider = emojiProvider;
    }


    public void updateSource(int chatId,
                              int contactId,
                              long startTimestamp,
                              long endTimestamp) {
        DcArray locations = dcContext.getLocations(chatId, contactId, startTimestamp, endTimestamp);

        MapSource contactMapMetadata = contactMapSources.get(contactId);
        if (contactMapMetadata == null) {
            contactMapMetadata = addContactMapSource(contactMapSources, contactId);
        }
        int count = locations.getCnt();

        LinkedList<Feature> sortedPointFeatures = featureCollections.get(contactMapMetadata.getMarkerFeatureCollection());
        if (sortedPointFeatures != null && sortedPointFeatures.size() == count) {
            return;
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
            boolean isPoi = locations.isIndependent(i);
            int messageId = locations.getMsgId(i);
            boolean isEmojiChar = !codepointChar.isEmpty() && emojiProvider.isEmoji(codepointChar);

            Feature pointFeature = Feature.fromGeometry(point, new JsonObject(), String.valueOf(locations.getLocationId(i)));
            pointFeature.addBooleanProperty(MARKER_SELECTED, false);
            pointFeature.addBooleanProperty(LAST_LOCATION, false);
            pointFeature.addNumberProperty(CONTACT_ID, contactId);
            pointFeature.addNumberProperty(TIMESTAMP, locations.getTimestamp(i));
            pointFeature.addNumberProperty(MESSAGE_ID, messageId);
            pointFeature.addNumberProperty(ACCURACY, locations.getAccuracy(i));
            pointFeature.addStringProperty(MARKER_CHAR, codepointChar);
            pointFeature.addBooleanProperty(IS_EMOJI_CHAR, isEmojiChar);
            if (isPoi && isEmojiChar) {
              //we save emoji bitmaps in mapboxstyle with the codepoint as the key
              pointFeature.addStringProperty(MARKER_ICON, codepointChar);
              emojiCodePoints.add(codepointChar);
            } else if (isPoi) {
              pointFeature.addStringProperty(MARKER_ICON, contactMapMetadata.getMarkerPoi());
            } else {
              pointFeature.addStringProperty(MARKER_ICON, contactMapMetadata.getMarkerIcon());
            }

            pointFeature.addBooleanProperty(IS_POI, isPoi);
            if (isPoi && codepointChar.length() == 0 && messageId != 0) {
                //has a long poi label
                DcMsg poiMsg = dcContext.getMsg(messageId);
                String poiLongDescription = poiMsg.getSummarytext(16);
                pointFeature.addStringProperty(POI_LONG_DESCRIPTION, poiLongDescription);
            }

            sortedPointFeatures.addFirst(pointFeature);

            if (!locations.isIndependent(i) && sortedPointFeatures.size() > 1) {
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
            for (Feature position : sortedPointFeatures) {
                if (!position.getBooleanProperty(IS_POI)) {
                    position.addStringProperty(LAST_POSITION_ICON, contactMapMetadata.getMarkerLastPositon());
                    position.addStringProperty(LAST_POSITION_LABEL, contactMapMetadata.getDisplayName());
                    position.removeProperty(MARKER_ICON);
                    position.addBooleanProperty(LAST_LOCATION, true);
                    lastPositions.put(contactId, position);
                    break;
                }
            }
        }

        featureCollections.put(contactMapMetadata.getMarkerFeatureCollection(), sortedPointFeatures);
        featureCollections.put(contactMapMetadata.getLineFeatureCollection(), sortedLineFeatures);
    }

    private MapSource addContactMapSource(ConcurrentHashMap<Integer, MapSource> contactMapSources, int contactId) {
        if (contactMapSources.get(contactId) != null) {
            return contactMapSources.get(contactId);
        }

        DcContact contact = dcContext.getContact(contactId);
        MapSource contactMapSource = new MapSource(contact);
        contactMapSources.put(contactId, contactMapSource);
        return contactMapSource;
    }
}
