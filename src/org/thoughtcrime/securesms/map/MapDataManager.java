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

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.map.model.FilterProvider;
import org.thoughtcrime.securesms.map.model.MapSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static com.b44t.messenger.DcContext.DC_EVENT_LOCATION_CHANGED;
import static com.b44t.messenger.DcContext.DC_GCL_ADD_SELF;
import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.switchCase;
import static com.mapbox.mapboxsdk.style.expressions.Expression.toBool;
import static com.mapbox.mapboxsdk.style.layers.Property.ICON_ANCHOR_BOTTOM_LEFT;
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;
import static org.thoughtcrime.securesms.map.model.MapSource.INFO_WINDOW_LAYER;
import static org.thoughtcrime.securesms.map.model.MapSource.LINE_FEATURE_LIST;


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
    private static final String INFO_WINDOW_SRC = "INFO_WINDOW_SRC";

    public static final int ALL_CHATS_GLOBAL_MAP = 0;
    public static final long TIMESTAMP_NOW = 0L;
    public static final long TIME_FRAME = 1000 * 60 * 60 * 24 * 2; // 2d
    private static final long DEFAULT_LAST_POSITION_DELTA = 1000 * 60 * 60 * 24; // 1d

    private static final String TAG = MapDataManager.class.getSimpleName();
    private Style mapboxStyle;
    private HashMap<Integer, MapSource> contactMapSources = new HashMap<>();
    private HashMap<String, LinkedList<Feature>> featureCollections = new HashMap<>();
    private FilterProvider filterProvider = new FilterProvider();
    private Feature selectedFeature;
    private int chatId;
    private Context context;
    private ApplicationDcContext dcContext;
    private boolean isInitial = true;
    private boolean showTraces = false;

    public interface MapDataState {
        void onDataInitialized(LatLngBounds bounds);
    }

    public MapDataManager(Context context, @NonNull Style mapboxMapStyle, int chatId, MapDataState updateCallback) {
        this.mapboxStyle = mapboxMapStyle;
        this.context = context;
        this.dcContext = DcHelper.getContext(context);
        this.chatId = chatId;
        LatLngBounds.Builder boundingBuilder = new LatLngBounds.Builder();

        int[] contactIds = getContactIds(chatId);
        initInfoWindowLayer();

        filterProvider.setMessageFilter(true);
        filterProvider.setLastPositionFilter(System.currentTimeMillis() - DEFAULT_LAST_POSITION_DELTA);
        for (int contactId : contactIds) {
            updateSource(chatId, contactId, boundingBuilder);
            MapSource source = contactMapSources.get(contactId);
            if (source != null) {
                applyMarkerFilter(source);
                applyLineFilter(source);
            }
        }

        dcContext.eventCenter.addObserver(DC_EVENT_LOCATION_CHANGED, this);

        try {
            updateCallback.onDataInitialized(boundingBuilder.build());
        } catch (InvalidLatLngBoundsException e) {
            e.printStackTrace();
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

    public void onDestroy() {
        GenerateInfoWindowTask.cancelRunningTasks();
    }

    @Override
    public Context getContext() {
        return context;
    }

    public void refreshSource(int contactId) {
        MapSource source = contactMapSources.get(contactId);
        LinkedList<Feature> collection = featureCollections.get(source.getMarkerFeatureCollection());
        GeoJsonSource pointSource = (GeoJsonSource) mapboxStyle.getSource(source.getMarkerSource());
        pointSource.setGeoJson(FeatureCollection.fromFeatures(collection));
        LinkedList<Feature> lineFeatures = featureCollections.get(source.getLineFeatureCollection());
        GeoJsonSource lineSource = (GeoJsonSource) mapboxStyle.getSource(source.getLineSource());
        lineSource.setGeoJson(FeatureCollection.fromFeatures(lineFeatures));
    }

    @Override
    public void handleEvent(int eventId, Object data1, Object data2) {
        Log.d(TAG, "updateEvent in MapDataManager called. eventId: " + eventId);
        int contactId = ((Long) data1).intValue();
        if (contactMapSources.containsKey(contactId)) {
            updateSource(chatId, contactId);
        }
    }

    @Override
    public boolean runOnMain() {
        return true;
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

    public void unselectMarker() {
        if (selectedFeature != null) {
            selectedFeature.addBooleanProperty(MARKER_SELECTED, false);
            refreshSource(selectedFeature.getNumberProperty(CONTACT_ID).intValue());
            selectedFeature = null;
            GeoJsonSource source = (GeoJsonSource) mapboxStyle.getSource(INFO_WINDOW_SRC);
            source.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
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

        new GenerateInfoWindowTask(this).execute(selectedFeature);
    }

    /**
     * Invoked when the bitmaps have been generated from a view.
     */
    @Override
    public void setInfoWindowResults(Bitmap result) {
        mapboxStyle.addImage(INFO_WINDOW_ID, result);
        GeoJsonSource infoWindowSource = (GeoJsonSource) mapboxStyle.getSource(INFO_WINDOW_SRC);
        infoWindowSource.setGeoJson(selectedFeature);
    }

    public void filterRange(long startTimestamp, long endTimestamp) {
        int[] contactIds = getContactIds(chatId);
        filterProvider.setRangeFilter(startTimestamp, endTimestamp);
        applyFilters(contactIds);
    }

    public void filterLastPositions(long startTimestamp) {
        int[] contactIds = getContactIds(chatId);
        filterProvider.setLastPositionFilter(startTimestamp);
        applyFilters(contactIds);
    }

    public void showTraces(boolean show) {
        int[] contactIds = getContactIds(chatId);
        this.showTraces = show;
        filterProvider.setMessageFilter(!show);
        applyFilters(contactIds);
    }

    private void showLineLayer(MapSource source) {
        LineLayer lineLayer = (LineLayer) mapboxStyle.getLayer(source.getLineLayer());
        lineLayer.setProperties(visibility(showTraces ? VISIBLE : NONE));
    }

    private void applyFilters(int[] contactIds) {
        for (int contactId : contactIds) {
            MapSource contactMapMetadata = contactMapSources.get(contactId);
            if (contactMapMetadata == null) {
                continue;
            }
            showLineLayer(contactMapMetadata);
            applyMarkerFilter(contactMapMetadata);
            applyLineFilter(contactMapMetadata);
        }
    }

    private void applyMarkerFilter(MapSource source) {
        SymbolLayer markerLayer = (SymbolLayer) mapboxStyle.getLayer(source.getMarkerLayer());
        markerLayer.setFilter(filterProvider.getMarkerFilter());
    }

    private void applyLineFilter(MapSource source) {
        LineLayer lineLayer = (LineLayer) mapboxStyle.getLayer(source.getLineLayer());
        lineLayer.setFilter(filterProvider.getLineFilter());
    }

    private void updateSource(int chatId, int contactId) {
        updateSource(chatId, contactId, null);
    }

    private void updateSource(int chatId, int contactId, LatLngBounds.Builder boundingBuilder) {
        updateSource(chatId, contactId, System.currentTimeMillis() - TIME_FRAME, TIMESTAMP_NOW, boundingBuilder );
    }

    private void updateSource(int chatId, int contactId, long startTimestamp, long endTimestamp, LatLngBounds.Builder boundingBuilder) {
        //long start = System.currentTimeMillis();
        DcArray locations = dcContext.getLocations(chatId, contactId, startTimestamp, endTimestamp);
        int count = locations.getCnt();
        if (count == 0) {
            return;
        }

        MapSource contactMapMetadata = contactMapSources.get(contactId);
        if (contactMapMetadata == null) {
            contactMapMetadata = addContactMapSource(contactId);
        }

        LinkedList<Feature> sortedPointFeatures = featureCollections.get(contactMapMetadata.getMarkerFeatureCollection());
        if (sortedPointFeatures != null && sortedPointFeatures.size() == count) {
            return;
        } else {
            sortedPointFeatures = new LinkedList<>();
        }
        LinkedList<Feature> sortedLineFeatures = new LinkedList<>();

        for (int i = count - 1; i >= 0; i--) {
            Point point = Point.fromLngLat(locations.getLongitude(i), locations.getLatitude(i));

            Feature pointFeature = Feature.fromGeometry(point, new JsonObject(), String.valueOf(locations.getLocationId(i)));
            pointFeature.addBooleanProperty(MARKER_SELECTED, false);
            pointFeature.addBooleanProperty(LAST_LOCATION, false);
            pointFeature.addNumberProperty(CONTACT_ID, contactId);
            pointFeature.addNumberProperty(TIMESTAMP, locations.getTimestamp(i));
            pointFeature.addNumberProperty(MESSAGE_ID, locations.getMsgId(i));
            pointFeature.addNumberProperty(ACCURACY, locations.getAccuracy(i));
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
            sortedPointFeatures.get(0).addBooleanProperty(LAST_LOCATION, true);
        }

        featureCollections.put(contactMapMetadata.getMarkerFeatureCollection(), sortedPointFeatures);
        featureCollections.put(contactMapMetadata.getLineFeatureCollection(), sortedLineFeatures);

        refreshSource(contactId);

        //Log.d(TAG, "update Source took " + (System.currentTimeMillis() - start) + " ms");
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

    private int[] getContactIds(int chatId) {
        if (chatId == ALL_CHATS_GLOBAL_MAP) {
            return dcContext.getContacts(DC_GCL_ADD_SELF, "");
        } else {
            int[] contactIds = dcContext.getChatContacts(chatId);
            boolean hasSelf = false;
            for (int contact : contactIds) {
                if (contact == 1) {
                    hasSelf = true;
                    break;
                }
            }
            if (!hasSelf) {
                contactIds = Arrays.copyOf(contactIds, contactIds.length + 1);
                contactIds[contactIds.length - 1] = 1;
            }
            return contactIds;
        }
    }

    private void initInfoWindowLayer() {
        Expression iconOffset = switchCase(toBool(get(LAST_LOCATION)),
                literal(new Float[] {-2f, -25f}), literal(new Float[] {-2f, -15f}));
        GeoJsonSource infoWindowSource = new GeoJsonSource(INFO_WINDOW_SRC);
        mapboxStyle.addSource(infoWindowSource);
        mapboxStyle.addLayer(new SymbolLayer(INFO_WINDOW_LAYER, INFO_WINDOW_SRC).withProperties(
                iconImage(INFO_WINDOW_ID),
                iconAnchor(ICON_ANCHOR_BOTTOM_LEFT),
                     /* all info window and marker image to appear at the same time*/
                iconAllowOverlap(true),
                    /* offset the info window to be above the marker */
                iconOffset(iconOffset)
        ));
    }

    private void initContactBasedLayers(MapSource source) {
        mapboxStyle.addImage(source.getMarkerLastPositon(),
                generateColoredLastPositionIcon(source.getColorArgb()));
        mapboxStyle.addImage(source.getMarkerIcon(),
                generateColoredLocationIcon(source.getColorArgb()));

        Expression markerSize =
                switchCase(toBool(get(MARKER_SELECTED)), literal(1.5f),
                        switchCase(toBool(get(LAST_LOCATION)), literal(1.0f),
                                switchCase(eq(get(MESSAGE_ID), literal(0)), literal(0.7f), literal(1.1f))));
        Expression markerIcon = switchCase(toBool(get(LAST_LOCATION)), literal(source.getMarkerLastPositon()), literal(source.getMarkerIcon()));
        Expression markerAllowOverlap =  toBool(get(LAST_LOCATION));
        mapboxStyle.addLayerBelow(new SymbolLayer(source.getMarkerLayer(), source.getMarkerSource())
                .withProperties(
                        iconImage(markerIcon),
                        iconSize(markerSize),
                        iconAllowOverlap(markerAllowOverlap))
                .withFilter(filterProvider.getMarkerFilter()),
                INFO_WINDOW_LAYER);

        mapboxStyle.addLayerBelow(new LineLayer(source.getLineLayer(), source.getLineSource())
                .withProperties(PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        PropertyFactory.lineWidth(3f),
                        PropertyFactory.lineOpacity(0.5f),
                        PropertyFactory.lineColor(source.getColorArgb()),
                        PropertyFactory.visibility(NONE)),
                INFO_WINDOW_LAYER);
    }

    private MapSource addContactMapSource(int contactId) {
        if (contactMapSources.get(contactId) != null) {
            return contactMapSources.get(contactId);
        }

        DcContact contact = dcContext.getContact(contactId);
        MapSource contactMapSource = new MapSource(contactId);
        contactMapSource.setColor(contact.getColor());
        contactMapSources.put(contactId, contactMapSource);
        initGeoJsonSources(contactMapSource);
        initContactBasedLayers(contactMapSource);
        return contactMapSource;
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
        int[] contactIds = getContactIds(chatId);
        for (int contactId : contactIds) {
            updateSource(chatId, contactId);
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
            if (key.startsWith(LINE_FEATURE_LIST)) {
                continue;
            }
            LinkedList<Feature> featureCollection = featureCollections.get(key);
            for (Feature f : featureCollection) {
                if (f.id().equals(id)) {
                    return f;
                }
            }
        }
        return null;
    }

}
