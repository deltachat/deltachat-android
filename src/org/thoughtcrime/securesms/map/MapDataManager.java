package org.thoughtcrime.securesms.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.b44t.messenger.DcArray;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcEventCenter;
import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.exceptions.InvalidLatLngBoundsException;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.map.model.FeatureTreeSet;
import org.thoughtcrime.securesms.map.model.MapSource;
import org.thoughtcrime.securesms.map.model.TimeComparableFeature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.b44t.messenger.DcContext.DC_EVENT_LOCATION_CHANGED;
import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.switchCase;
import static com.mapbox.mapboxsdk.style.expressions.Expression.toBool;
import static com.mapbox.mapboxsdk.style.layers.Property.ICON_ANCHOR_BOTTOM_LEFT;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;

/**
 * Created by cyberta on 07.03.19.
 */

public class MapDataManager implements DcEventCenter.DcEventDelegate, GenerateInfoWindowCallback {
    public static final String MARKER_SELECTED = "MARKER_SELECTED";
    public static final String LAST_LOCATION = "LAST_LOCATION";
    public static final String CONTACT_ID = "CONTACT_ID";
    public static final String INFO_WINDOW_ID = "INFO_WINDOW_ID";
    public static final String TIMESTAMP = "TIMESTAMP";
    public static final String MESSAGE_ID = "MESSAGE_ID";
    public static final String ACCURACY = "ACCURACY";
    private static final String TAG = MapDataManager.class.getSimpleName();
    private Style mapboxStyle;
    private HashMap<Integer, MapSource> contactMapSources;
    private HashMap<String, FeatureTreeSet> featureCollections;
    private Feature selectedFeature;
    private int[] chatIds = new int[1];
    private Context context;
    private ApplicationDcContext dcContext;
    private boolean isInitial = true;

    public interface MapDataState {
        void onDataInitialized(LatLngBounds bounds);
    }

    public MapDataManager(Context context, @NonNull Style mapboxMapStyle, int[] chatIds, MapDataState updateCallback) {
        this.mapboxStyle = mapboxMapStyle;
        this.context = context;
        this.dcContext = DcHelper.getContext(context);
        this.chatIds = chatIds;
        contactMapSources = new HashMap<>();
        featureCollections = new HashMap<>();
        LatLngBounds.Builder boundingBuilder = new LatLngBounds.Builder();
        for (int chatId : chatIds) {
            int[] contactIds = dcContext.getChatContacts(chatId);

            for (int contactId : contactIds) {
                if (contactId == 1) {
                    //skip self, it is explicitely added as 1:1 don't include self whereas groups and selftalk do
                    continue;
                }
                addContactMapSource(contactId);
                updateSource(chatId, contactId, boundingBuilder);
                generateInfoWindows(contactId);
            }

            addContactMapSource(1);
            updateSource(chatId, 1, boundingBuilder);
            generateInfoWindows(1);

            try {
                updateCallback.onDataInitialized(boundingBuilder.build());
            } catch (InvalidLatLngBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    public void onResume() {
        dcContext.eventCenter.addObserver(DC_EVENT_LOCATION_CHANGED, this);
        if (!isInitial) {
            updateSources();
        }
        isInitial = false;
    }

    public void onPause() {
        dcContext.eventCenter.removeObserver(DC_EVENT_LOCATION_CHANGED, this);
    }

    public void addContactMapSource(int contactId) {
        if (contactMapSources.get(contactId) != null) {
            return;
        }

        DcContact contact = dcContext.getContact(contactId);
        MapSource contactMapSource = new MapSource(contactId);
        contactMapSource.setColor(contact.getColor());
        contactMapSources.put(contactId, contactMapSource);
        initGeoJsonSources(contactMapSource);
        initLayers(contactMapSource);
    }

    private Bitmap generateColoredLastPositionIcon(int colorFilter) {
        return generateColoredBitmap(colorFilter, R.drawable.ic_location_on_white_24dp);
    }

    private Bitmap generateColoredLocationIcon(int colorFilter) {
        return generateColoredBitmap(colorFilter, R.drawable.ic_location_dot);
    }

    private Bitmap generateColoredBitmap(int colorFilter, @DrawableRes int res) {
        Bitmap icon = getBitmap(res);
        Paint paint = new Paint();
        ColorFilter filter = new PorterDuffColorFilter(colorFilter, PorterDuff.Mode.SRC_IN);
        paint.setColorFilter(filter);
        Canvas canvas = new Canvas(icon);
        canvas.drawBitmap(icon, 0, 0, paint);
        return icon;
    }

    private Bitmap getBitmap(@DrawableRes int res) {
        Drawable drawable = ContextCompat.getDrawable(context, res);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private void updateSources() {
        for (int chatId : chatIds) {
            int[] contacts = dcContext.getChatContacts(chatId);
            for (int contactId : contacts) {
                if (!contactMapSources.containsKey(contactId)) {
                    addContactMapSource(contactId);
                }
                updateSource(chatId, contactId);
                generateMissingInfoWindows(contactId);
            }
        }
    }

    @Override
    public Context getContext() {
        return context;
    }

    /**
     * Invoked when the bitmaps have been generated from a view.
     */
    @Override
    public void setInfoWindowResults(HashMap<String, Bitmap> results) {
        Log.d(TAG, "setInfoWindowResults start");
        mapboxStyle.addImages(results);
        Log.d(TAG, "setInfoWindowResults finished");
    }

    @Override
    public void refreshSource(int contactId) {
        Log.d(TAG, "refreshSource start");
        MapSource source = contactMapSources.get(contactId);
        FeatureTreeSet collection = featureCollections.get(source.getMarkerFeatureCollection());
        GeoJsonSource pointSource = (GeoJsonSource) mapboxStyle.getSource(source.getMarkerSource());
        pointSource.setGeoJson(FeatureCollection.fromFeatures(collection.getFeatureList()));
        Log.d(TAG, "refreshSource finished");
    }

    private void updateSource(int chatId, int contactId) {
        updateSource(chatId, contactId, null);
    }

    private void updateSource(int chatId, int contactId, LatLngBounds.Builder boundingBuilder) {
        DcArray locations = ApplicationContext.getInstance(context).dcContext.getLocations(chatId, contactId, System.currentTimeMillis()-3*60*60*1000, 0);
        MapSource contactMapMetadata = contactMapSources.get(contactId);

        FeatureTreeSet sortedPointFeatures = featureCollections.get(contactMapMetadata.getMarkerFeatureCollection());
        if (sortedPointFeatures == null) {
            sortedPointFeatures = new FeatureTreeSet();
        }

        int count = locations.getCnt();
        for (int i = 0; i < count; i++) {
            Point p = Point.fromLngLat(locations.getLongitude(i), locations.getLatitude(i));
            Feature pointFeature = Feature.fromGeometry(p, new JsonObject(), chatId + "_" + contactId + "_" + i);
            pointFeature.addBooleanProperty(MARKER_SELECTED, false);
            pointFeature.addBooleanProperty(LAST_LOCATION, false);
            pointFeature.addNumberProperty(CONTACT_ID, contactId);
            pointFeature.addStringProperty(INFO_WINDOW_ID, chatId + "_" + contactId + "_info_" + (count - 1 - i));
            pointFeature.addNumberProperty(TIMESTAMP, locations.getTimestamp(i));
            pointFeature.addNumberProperty(MESSAGE_ID, locations.getMsgId(i));
            pointFeature.addNumberProperty(ACCURACY, locations.getAccuracy(i));
            sortedPointFeatures.add(new TimeComparableFeature(pointFeature));

            if (boundingBuilder != null) {
                boundingBuilder.include(new LatLng(locations.getLatitude(i), locations.getLongitude(i)));
            }
        }

        if (sortedPointFeatures.size() > 0) {
            sortedPointFeatures.first().getFeature().addBooleanProperty(LAST_LOCATION, true);
        }

        FeatureCollection pointFeatureCollection = FeatureCollection.fromFeatures(sortedPointFeatures.getFeatureList());
        FeatureCollection lineFeatureCollection = FeatureCollection.fromFeatures(new Feature[] {Feature.fromGeometry(
                LineString.fromLngLats(sortedPointFeatures.getPointList())
        )});

        GeoJsonSource lineSource = (GeoJsonSource) mapboxStyle.getSource(contactMapMetadata.getLineSource());
        lineSource.setGeoJson(lineFeatureCollection);
        GeoJsonSource pointSource = (GeoJsonSource) mapboxStyle.getSource(contactMapMetadata.getMarkerSource());
        pointSource.setGeoJson(pointFeatureCollection);
        featureCollections.put(contactMapMetadata.getMarkerFeatureCollection(), sortedPointFeatures);
    }

    private void generateMissingInfoWindows(int contactId) {
        MapSource contactMapMetadata = contactMapSources.get(contactId);
        FeatureTreeSet featureList = featureCollections.get(contactMapMetadata.getMarkerFeatureCollection());

        ArrayList<Feature> missingWindows = new ArrayList<>();

        for (TimeComparableFeature tcf : featureList) {
            String infoWindowId = tcf.getFeature().getStringProperty(INFO_WINDOW_ID);
            if (mapboxStyle.getImage(infoWindowId) == null) {
                Log.d(TAG, "create new infoWindow for " + infoWindowId);
                missingWindows.add(tcf.getFeature());
            } else {
                // the list is ordered and thus, all older features should already have an info window
                break;
            }
        }

        if (missingWindows.size() > 0) {
            new GenerateInfoWindowTask(this, contactId).execute(missingWindows);
        }
    }


    private void generateInfoWindows(int contactId) {
        MapSource contactMapMetadata = contactMapSources.get(contactId);
        FeatureTreeSet collection = featureCollections.get(contactMapMetadata.getMarkerFeatureCollection());
        new GenerateInfoWindowTask(this, contactId).execute(collection.getFeatureList());
    }

    private void initGeoJsonSources(MapSource source) {
        GeoJsonSource markerPositionSource = new GeoJsonSource(source.getMarkerSource());
        GeoJsonSource linePositionSource = new GeoJsonSource(source.getLineSource());

        try {
            mapboxStyle.addSource(markerPositionSource);
            mapboxStyle.addSource(linePositionSource);
        } catch (RuntimeException e) {
            //TODO: specify exception more
            Log.e(TAG, "Unable to init GeoJsonSources. Already added to mapBoxMap? " + e.getMessage());
        }

    }

    private void initLayers(MapSource source) {
        mapboxStyle.addImage(source.getMarkerLastPositon(),
               generateColoredLastPositionIcon(source.getColorArgb()));
        mapboxStyle.addImage(source.getMarkerIcon(),
                generateColoredLocationIcon(source.getColorArgb()));

        Expression markerSize =
                switchCase(toBool(get(MARKER_SELECTED)), literal(1.5f),
                        switchCase(toBool(get(LAST_LOCATION)), literal(1.0f),
                            switchCase(eq(get(MESSAGE_ID), literal(0)), literal(0.7f), literal(1.1f))));
        Expression markerIcon = switchCase(toBool(get(LAST_LOCATION)), literal(source.getMarkerLastPositon()), literal(source.getMarkerIcon()));
        Expression allowOverlap = eq(get(LAST_LOCATION), literal(true));
        mapboxStyle.addLayer(new SymbolLayer(source.getMarkerLayer(), source.getMarkerSource())
                .withProperties(
                        iconImage(markerIcon),
                        iconSize(markerSize),
                        iconAllowOverlap(allowOverlap))
                );

        mapboxStyle.addLayer(new LineLayer(source.getLineLayer(), source.getLineSource())
                .withProperties(PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        PropertyFactory.lineWidth(3f),
                        PropertyFactory.lineOpacity(0.5f),
                        PropertyFactory.lineColor(source.getColorArgb())));

        Expression filterInfoWindow = eq((get(MARKER_SELECTED)), literal(true));
        Expression iconOffset = switchCase(toBool(get(LAST_LOCATION)), literal(new Float[] {-2f, -25f}), literal(new Float[] {-2f, -15f}));
        mapboxStyle.addLayer(new SymbolLayer(source.getInfoWindowLayer(), source.getMarkerSource()).
                withProperties(
                        iconImage("{"+INFO_WINDOW_ID+"}"),
                        iconAnchor(ICON_ANCHOR_BOTTOM_LEFT),
                         /* all info window and marker image to appear at the same time*/
                        iconAllowOverlap(true),
                        /* offset the info window to be above the marker */
                        iconOffset(iconOffset)
                ).withFilter(filterInfoWindow));
    }

    //FIXME: consider to use data2 parameter to send chatID from core to Android
    @Override
    public void handleEvent(int eventId, Object data1, Object data2) {
        Log.d(TAG, "updateEvent in MapDataManager called. eventId: " + eventId);
        int contactId = (Integer) data1;
        if (contactMapSources.containsKey(contactId)) {
            //FIXME: ---------v this is wrong, but there's no other opportunity for now
            updateSource(chatIds[0], contactId);
            generateMissingInfoWindows(contactId);
        }
    }

    @Override
    public boolean runOnMain() {
        return false;
    }


    public String[] getMarkerLayers() {
        String markerLayers[] = new String[contactMapSources.size()];
        int i = 0;
        for (Map.Entry<Integer, MapSource> entry : contactMapSources.entrySet()) {
            markerLayers[i] = entry.getValue().getMarkerLayer();
            i += 1;
        }

        return markerLayers;
    }

    public String[] getInfoWindowLayers() {
        String markerLayers[] = new String[contactMapSources.size()];
        int i = 0;
        for (Map.Entry<Integer, MapSource> entry : contactMapSources.entrySet()) {
            markerLayers[i] = entry.getValue().getInfoWindowLayer();
            i += 1;
        }

        return markerLayers;
    }

    public void unselectMarker() {
        if (selectedFeature != null) {
            selectedFeature.addBooleanProperty(MARKER_SELECTED, false);
            refreshSource(selectedFeature.getNumberProperty(CONTACT_ID).intValue());
            selectedFeature = null;
        }
    }

    public void setMarkerSelected(String featureId) {
        if (selectedFeature == null) {
            setNewMarkerSelected(featureId);
        } else if (selectedFeature.id().equals(featureId)) {
            updateSelectedMarker();
        } else {
            replaceSelectedMarker(featureId);
        }
    }

    private void replaceSelectedMarker(String featureId) {
        Feature feature = getFeatureWithId(featureId);
        feature.addBooleanProperty(MARKER_SELECTED, true);
        selectedFeature.addBooleanProperty(MARKER_SELECTED, false);

        int lastContactId = selectedFeature.getNumberProperty(CONTACT_ID).intValue();
        int currentContactId = feature.getNumberProperty(CONTACT_ID).intValue();

        selectedFeature = feature;
        refreshSource(currentContactId);
        if (lastContactId != currentContactId) {
            refreshSource(lastContactId);
        }

    }

    private void updateSelectedMarker() {
        boolean isSelected = selectedFeature.getBooleanProperty(MARKER_SELECTED);
        selectedFeature.addBooleanProperty(MARKER_SELECTED, !isSelected);
        refreshSource(selectedFeature.getNumberProperty(CONTACT_ID).intValue());
    }

    private void setNewMarkerSelected(String featureId) {
        Feature feature = getFeatureWithId(featureId);
        feature.addBooleanProperty(MARKER_SELECTED, true);
        selectedFeature = feature;
        refreshSource(selectedFeature.getNumberProperty(CONTACT_ID).intValue());
    }


    private Feature getFeatureWithId(String id) {
        for (String key : featureCollections.keySet()) {
            ArrayList<Feature> featureCollection = featureCollections.get(key).getFeatureList();
            for (Feature f : featureCollection) {
                if (f.id().equals(id)) {
                    return f;
                }
            }
        }
        return null;
    }

}
