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
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.R;

import java.util.ArrayList;
import java.util.HashMap;

import static com.b44t.messenger.DcContext.DC_EVENT_LOCATION_CHANGED;
import static com.mapbox.mapboxsdk.style.layers.Property.ICON_ANCHOR_BOTTOM;

/**
 * Created by cyberta on 07.03.19.
 */

public class MapDataManager implements DcEventCenter.DcEventDelegate {
    private static final String TAG = MapDataManager.class.getSimpleName();
    private Style mapboxStyle;
    private HashMap<Integer, MapSource> contactMapSources;
    private int chatId;
    private Context context;

    public MapDataManager(Context context, @NonNull Style mapboxMapStyle, int chatId) {
        this.mapboxStyle = mapboxMapStyle;
        this.context = context;
        this.chatId = chatId;
        contactMapSources = new HashMap<>();
        int[] contactIds = ApplicationContext.getInstance(context).dcContext.getChatContacts(chatId);
        for (int contactId : contactIds) {
            addContactMapSource(contactId);
            updateSource(contactId);
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
        }
    }

    private void updateSource(int contactId) {
        DcArray locations = ApplicationContext.getInstance(context).dcContext.getLocations(chatId, contactId);
        int count = locations.getCnt();
        if (count == 0) {
            return;
        }

        ArrayList<Point>  coordinateList = new ArrayList<>();
        ArrayList<Feature> pointFeatureList = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Point p = Point.fromLngLat(locations.getLongitude(i), locations.getLatitude(i));
            coordinateList.add(p);
            pointFeatureList.add(Feature.fromGeometry(p));
        }

        FeatureCollection pointFeatureCollection = FeatureCollection.fromFeatures(pointFeatureList);
        FeatureCollection lineFeatureCollection = FeatureCollection.fromFeatures(new Feature[] {Feature.fromGeometry(
                LineString.fromLngLats(coordinateList)
        )});

        MapSource contactMapMetadata = contactMapSources.get(contactId);
        GeoJsonSource lineSource = (GeoJsonSource) mapboxStyle.getSource(contactMapMetadata.getLineSource());
        lineSource.setGeoJson(lineFeatureCollection);
        GeoJsonSource pointSource = (GeoJsonSource) mapboxStyle.getSource(contactMapMetadata.getMarkerSource());
        pointSource.setGeoJson(pointFeatureCollection);
        GeoJsonSource lastPostionSource = (GeoJsonSource) mapboxStyle.getSource(contactMapMetadata.getLastPositionSource());
        lastPostionSource.setGeoJson(pointFeatureList.get(0));
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


        mapboxStyle.addLayer(new SymbolLayer(source.getMarkerLayer(), source.getMarkerSource())
                .withProperties(PropertyFactory.iconImage(source.getMarkerIcon())));
        mapboxStyle.addLayer(new SymbolLayer(source.getLastPositionLayer(), source.getLastPositionSource())
                .withProperties(PropertyFactory.iconImage(source.getMarkerLastPositon()),
                        PropertyFactory.iconAnchor(ICON_ANCHOR_BOTTOM)));
        mapboxStyle.addLayer(new LineLayer(source.getLineLayer(), source.getLineSource())
                .withProperties(PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        PropertyFactory.lineWidth(3f),
                        PropertyFactory.lineColor(source.getColorArgb())));
    }


    @Override
    public void handleEvent(int eventId, Object data1, Object data2) {
        int contactId = (Integer) data1;
        if (contactMapSources.containsKey(contactId)) {
            updateSource(contactId);
        }
    }

    @Override
    public boolean runOnMain() {
        return false;
    }

}
