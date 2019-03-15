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
import android.support.annotation.Nullable;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.b44t.messenger.DcContext.DC_EVENT_LOCATION_CHANGED;
import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.switchCase;
import static com.mapbox.mapboxsdk.style.expressions.Expression.toBool;
import static com.mapbox.mapboxsdk.style.layers.Property.ICON_ANCHOR_BOTTOM;
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
    public static final String CONTACT_ID = "CONTACT_ID";
    public static final String INFO_WINDOW_ID = "INFO_WINDOW_ID";
    public static final String TIMESTAMP = "TIMESTAMP";
    public static final String MESSAGE_ID = "MESSAGE_ID";
    public static final String ACCURACY = "ACCURACY";
    private static final String TAG = MapDataManager.class.getSimpleName();
    private Style mapboxStyle;
    private HashMap<Integer, MapSource> contactMapSources;
    private HashMap<String, ArrayList<Feature>> featureCollections;
    private Feature selectedFeature;
    private int chatId;
    private Context context;

    public interface MapDataState {
        void onDataInitialized(LatLngBounds bounds);
    }

    public MapDataManager(Context context, @NonNull Style mapboxMapStyle, int chatId, MapDataState updateCallback) {
        this.mapboxStyle = mapboxMapStyle;
        this.context = context;
        this.chatId = chatId;
        contactMapSources = new HashMap<>();
        featureCollections = new HashMap<>();
        LatLngBounds.Builder boundingBuilder = new LatLngBounds.Builder();
        int[] contactIds = ApplicationContext.getInstance(context).dcContext.getChatContacts(chatId);

        for (int contactId : contactIds) {
            if (contactId == 1) {
                //skip self, it is explicitely added as 1:1 don't include self whereas groups and selftalk do
                continue;
            }
            addContactMapSource(contactId);
            updateSource(contactId, boundingBuilder);
            generateInfoWindows(contactId);
        }

        addContactMapSource(1);
        updateSource(1, boundingBuilder);
        generateMissingInfoWindows(1);


        try {
            updateCallback.onDataInitialized(boundingBuilder.build());
        } catch (InvalidLatLngBoundsException e) {
            e.printStackTrace();
        }
    }

    public void onResume() {
        ApplicationContext.getInstance(context).dcContext.eventCenter.addObserver(DC_EVENT_LOCATION_CHANGED, this);
        updateSources();
    }

    public void onPause() {
        ApplicationContext.getInstance(context).dcContext.eventCenter.removeObserver(DC_EVENT_LOCATION_CHANGED, this);
    }

    public void addContactMapSource(int contactId) {
        DcContact contact = ApplicationContext.getInstance(context).dcContext.getContact(contactId);
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
        for (Integer contactId : contactMapSources.keySet()) {
            updateSource(contactId);
            generateMissingInfoWindows(contactId);
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
        ArrayList<Feature> collection = featureCollections.get(source.getMarkerFeatureCollection());
        GeoJsonSource pointSource = (GeoJsonSource) mapboxStyle.getSource(source.getMarkerSource());
        pointSource.setGeoJson(FeatureCollection.fromFeatures(collection));
        Log.d(TAG, "refreshSource finished");
    }

    private void updateSource(int contactId) {
        updateSource(contactId, null);
    }

    private void updateSource(int contactId, @Nullable LatLngBounds.Builder boundingBuilder) {
        DcArray locations = ApplicationContext.getInstance(context).dcContext.getLocations(chatId, contactId);
        int count = locations.getCnt();
        if (count == 0) {
            return;
        }

        ArrayList<Point>  coordinateList = new ArrayList<>();
        ArrayList<Feature> pointFeatureList = new ArrayList<>();

        MapSource contactMapMetadata = contactMapSources.get(contactId);

        for (int i = 0; i < count; i++) {
            Point p = Point.fromLngLat(locations.getLongitude(i), locations.getLatitude(i));
            coordinateList.add(p);
            Feature pointFeature = Feature.fromGeometry(p, new JsonObject(), contactId + "_" + i);
            pointFeature.addBooleanProperty(MARKER_SELECTED, false);
            pointFeature.addNumberProperty(CONTACT_ID, contactId);
            pointFeature.addStringProperty(INFO_WINDOW_ID, contactId + "_info_" + (count-i));
            pointFeature.addNumberProperty(TIMESTAMP, locations.getTimestamp(i));
            pointFeature.addNumberProperty(MESSAGE_ID, locations.getMsgId(i));
            pointFeature.addNumberProperty(ACCURACY, locations.getAccuracy(i));
            pointFeatureList.add(pointFeature);
            if (boundingBuilder != null) {
                boundingBuilder.include(new LatLng(locations.getLatitude(i), locations.getLongitude(i)));
            }
        }

        FeatureCollection pointFeatureCollection = FeatureCollection.fromFeatures(pointFeatureList);
        FeatureCollection lineFeatureCollection = FeatureCollection.fromFeatures(new Feature[] {Feature.fromGeometry(
                LineString.fromLngLats(coordinateList)
        )});

        GeoJsonSource lineSource = (GeoJsonSource) mapboxStyle.getSource(contactMapMetadata.getLineSource());
        lineSource.setGeoJson(lineFeatureCollection);
        GeoJsonSource pointSource = (GeoJsonSource) mapboxStyle.getSource(contactMapMetadata.getMarkerSource());
        pointSource.setGeoJson(pointFeatureCollection);
        GeoJsonSource lastPostionSource = (GeoJsonSource) mapboxStyle.getSource(contactMapMetadata.getLastPositionSource());
        lastPostionSource.setGeoJson(pointFeatureList.get(0));
        featureCollections.put(contactMapMetadata.getMarkerFeatureCollection(), pointFeatureList);
    }

    private void generateMissingInfoWindows(int contactId) {
        MapSource contactMapMetadata = contactMapSources.get(contactId);
        ArrayList<Feature> featureList = featureCollections.get(contactMapMetadata.getMarkerFeatureCollection());
        ArrayList<Feature> missingWindows = new ArrayList<>();

        for (Feature f : featureList) {
            String infoWindowId = f.getStringProperty(INFO_WINDOW_ID);
            if (mapboxStyle.getImage(infoWindowId) == null) {
                Log.d(TAG, "create new infoWindow for " + infoWindowId);
                missingWindows.add(f);
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
        ArrayList<Feature> featureList = featureCollections.get(contactMapMetadata.getMarkerFeatureCollection());
        new GenerateInfoWindowTask(this, contactId).execute(featureList);
    }

    private void initGeoJsonSources(MapSource source) {
        GeoJsonSource lastPositionSource = new GeoJsonSource(source.getLastPositionSource());
        GeoJsonSource markerPositionSource = new GeoJsonSource(source.getMarkerSource());
        GeoJsonSource linePositionSource = new GeoJsonSource(source.getLineSource());

        try {
            mapboxStyle.addSource(lastPositionSource);
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

        Expression markerSizeExpression = switchCase(toBool(get(MARKER_SELECTED)), literal(1.5f), literal(1.0f));
        mapboxStyle.addLayer(new SymbolLayer(source.getMarkerLayer(), source.getMarkerSource())
                .withProperties(
                        iconImage(source.getMarkerIcon()),
                        iconSize(markerSizeExpression))
                );
        mapboxStyle.addLayer(new SymbolLayer(source.getLastPositionLayer(), source.getLastPositionSource())
                .withProperties(
                        iconImage(source.getMarkerLastPositon()),
                        iconAnchor(ICON_ANCHOR_BOTTOM),
                        iconSize(markerSizeExpression))
                );

        mapboxStyle.addLayer(new LineLayer(source.getLineLayer(), source.getLineSource())
                .withProperties(PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        PropertyFactory.lineWidth(3f),
                        PropertyFactory.lineColor(source.getColorArgb())));

        Expression filterInfoWindowExpression = eq((get(MARKER_SELECTED)), literal(true));
        mapboxStyle.addLayer(new SymbolLayer(source.getInfoWindowLayer(), source.getMarkerSource()).
                withProperties(
                        iconImage("{"+INFO_WINDOW_ID+"}"),
                        iconAnchor(ICON_ANCHOR_BOTTOM_LEFT),
                         /* all info window and marker image to appear at the same time*/
                        iconAllowOverlap(true),
                        /* offset the info window to be above the marker */
                        iconOffset(new Float[] {-2f, -15f})
                ).withFilter(filterInfoWindowExpression));
    }

    @Override
    public void handleEvent(int eventId, Object data1, Object data2) {
        Log.d(TAG, "updateEvent in MapDataManager called. eventId: " + eventId);
        int contactId = (Integer) data1;
        if (contactMapSources.containsKey(contactId)) {
            updateSource(contactId);
            generateMissingInfoWindows(contactId);
        }
    }

    @Override
    public boolean runOnMain() {
        return false;
    }


    public String[] getMarkerLayers() {
        String markerLayers[] = new String[contactMapSources.size()*2];
        int i = 0;
        for (Map.Entry<Integer, MapSource> entry : contactMapSources.entrySet()) {
            markerLayers[i] = entry.getValue().getMarkerLayer();
            markerLayers[i+1] = entry.getValue().getLastPositionLayer();
            i += 2;
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
            ArrayList<Feature> featureCollection  = featureCollections.get(key);
            for (Feature f : featureCollection) {
                if (f.id().equals(id)) {
                    return f;
                }
            }
        }
        return null;
    }

}
