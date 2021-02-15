package org.thoughtcrime.securesms.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcEventCenter;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.exceptions.InvalidLatLngBoundsException;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.emoji.EmojiProvider;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.map.DataCollectionTask.DataCollectionCallback;
import org.thoughtcrime.securesms.map.EmojiBitmapGenerationTask.EmojiBitmapGenerationCallback;
import org.thoughtcrime.securesms.map.GenerateInfoWindowTask.GenerateInfoWindowCallback;
import org.thoughtcrime.securesms.map.model.FilterProvider;
import org.thoughtcrime.securesms.map.model.MapSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.b44t.messenger.DcContext.DC_EVENT_LOCATION_CHANGED;
import static com.b44t.messenger.DcContext.DC_GCL_ADD_SELF;
import static com.mapbox.mapboxsdk.location.modes.RenderMode.COMPASS;
import static com.mapbox.mapboxsdk.style.expressions.Expression.all;
import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.has;
import static com.mapbox.mapboxsdk.style.expressions.Expression.length;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.neq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.not;
import static com.mapbox.mapboxsdk.style.expressions.Expression.switchCase;
import static com.mapbox.mapboxsdk.style.expressions.Expression.toBool;
import static com.mapbox.mapboxsdk.style.layers.Property.ICON_ANCHOR_BOTTOM_LEFT;
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.TEXT_ANCHOR_CENTER;
import static com.mapbox.mapboxsdk.style.layers.Property.TEXT_ANCHOR_TOP;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textField;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textOffset;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;
import static org.thoughtcrime.securesms.map.model.MapSource.INFO_WINDOW_LAYER;
import static org.thoughtcrime.securesms.map.model.MapSource.LINE_FEATURE_LIST;
import static org.thoughtcrime.securesms.util.BitmapUtil.generateColoredBitmap;


/**
 * Created by cyberta on 07.03.19.
 */

public class MapDataManager implements DcEventCenter.DcEventDelegate,
  GenerateInfoWindowCallback,
  DataCollectionCallback,
  EmojiBitmapGenerationCallback {
    public static final String MARKER_SELECTED = "MARKER_SELECTED";
    public static final String LAST_LOCATION = "LAST_LOCATION";
    public static final String CONTACT_ID = "CONTACT_ID";
    public static final String INFO_WINDOW_ID = "INFO_WINDOW_ID";
    public static final String TIMESTAMP = "TIMESTAMP";
    public static final String MESSAGE_ID = "MESSAGE_ID";
    public static final String ACCURACY = "ACCURACY";
    public static final String MARKER_CHAR = "MARKER_CHAR";
    public static final String IS_EMOJI_CHAR = "IS_EMOJI_CHAR";
    public static final String POI_LONG_DESCRIPTION = "POI_LONG_DESCRIPTION";
    public static final String MARKER_ICON = "MARKER_ICON";
    public static final String IS_POI = "IS_POI";
    public static final String LAST_POSITION_ICON = "LAST_POSITION_ICON";
    public static final String LAST_POSITION_LABEL = "LAST_POSITION_LABEL";
    private static final String INFO_WINDOW_SRC = "INFO_WINDOW_SRC";
    private static final String LAST_POSITION_LAYER = "LAST_POSITION_LAYER";
    private static final String LAST_POSITION_SOURCE = "LAST_POSITION_SRC";

    public static final int ALL_CHATS_GLOBAL_MAP = 0;
    public static final long TIMESTAMP_NOW = 0L;
    public static final long TIME_FRAME = 1000 * 60 * 60 * 24 * 2; // 2d
    private static final long DEFAULT_LAST_POSITION_DELTA = 1000 * 60 * 60 * 24; // 1d

    private static final String TAG = MapDataManager.class.getSimpleName();
    private Style mapboxStyle;
    private ConcurrentHashMap<Integer, MapSource> contactMapSources = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, LinkedList<Feature>> featureCollections = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Feature> lastPositions = new ConcurrentHashMap<>();
    private Set<String> emojiCodePoints = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private FilterProvider filterProvider = new FilterProvider();
    private Feature selectedFeature;
    private int chatId;
    private LatLngBounds.Builder boundingBuilder;
    private Context context;
    private ApplicationDcContext dcContext;
    private MapDataState callback;
    private boolean isInitial = true;
    private boolean showTraces = false;
    private LocationComponent locationComponent;
    private EmojiProvider emojiProvider;

    public interface MapDataState {
        void onDataInitialized(LatLngBounds bounds);
    }

    public MapDataManager(Context context, @NonNull Style mapboxMapStyle, LocationComponent locationComponent, int chatId, MapDataState updateCallback) {
        Log.d(TAG, "performance test - create map manager");
        this.mapboxStyle = mapboxMapStyle;
        this.context = context;
        this.dcContext = DcHelper.getContext(context);
        this.chatId = chatId;
        boundingBuilder = new LatLngBounds.Builder();
        this.callback = updateCallback;
        this.locationComponent = locationComponent;
        emojiProvider = EmojiProvider.getInstance(context);

        initInfoWindowLayer();
        initLastPositionLayer();
        initLocationComponent();

        filterProvider.setMessageFilter(true);
        filterProvider.setLastPositionFilter(System.currentTimeMillis() - DEFAULT_LAST_POSITION_DELTA);
        applyLastPositionFilter();

        updateSources();
        dcContext.eventCenter.addObserver(DC_EVENT_LOCATION_CHANGED, this);

        Log.d(TAG, "performance test - create map manager finished");
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
        DataCollectionTask.cancelRunningTasks();
        Log.d(TAG, "performance test - Map manager destroyed");
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
        GeoJsonSource lastPostionSource = (GeoJsonSource) mapboxStyle.getSource(LAST_POSITION_SOURCE);
        lastPostionSource.setGeoJson(FeatureCollection.fromFeatures(new LinkedList<>(lastPositions.values())));
    }

    @Override
    public void handleEvent(DcEvent event) {
        int eventId = event.getId();
        Log.d(TAG, "updateEvent in MapDataManager called. eventId: " + eventId);
        int contactId = event.getData1Int();
        if (contactMapSources.containsKey(contactId)) {
            HashSet<String> emojiCodePoints = new HashSet<>();
            DataCollector collector = new DataCollector(dcContext,
              contactMapSources,
              featureCollections,
              lastPositions,
              emojiCodePoints,
              emojiProvider,
              null
              );
            collector.updateSource(chatId,
                    contactId,
                    System.currentTimeMillis() - TIME_FRAME,
                    TIMESTAMP_NOW);
            refreshSource(contactId);
            handleEmojiCodepoints(emojiCodePoints);
        }
        Log.d(TAG, "updateEvent in MapDataManager called. finished: " + eventId);
    }

    @Override
    public boolean runOnMain() {
        return true;
    }


    public String[] getMarkerLayers() {
        String markerLayers[] = new String[contactMapSources.size() + 1];
        int i = 0;
        for (Map.Entry<Integer, MapSource> entry : contactMapSources.entrySet()) {
            markerLayers[i] = entry.getValue().getMarkerLayer();
            i += 1;
        }

        markerLayers[contactMapSources.size()] = LAST_POSITION_LAYER;
        return markerLayers;
    }

    public boolean unselectMarker() {
        if (selectedFeature != null) {
            selectedFeature.addBooleanProperty(MARKER_SELECTED, false);
            refreshSource(selectedFeature.getNumberProperty(CONTACT_ID).intValue());
            selectedFeature = null;
            GeoJsonSource source = (GeoJsonSource) mapboxStyle.getSource(INFO_WINDOW_SRC);
            source.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
            return true;
        }
        return false;
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

    public boolean isSelected(@NonNull Feature feature) {
        return selectedFeature != null && selectedFeature.id().equals(feature.id());
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

    @Override
    public void onEmojiBitmapCreated(String codePoint, Bitmap emoji) {
      mapboxStyle.addImage(codePoint, emoji);
    }

    @Override
    public void onDataCollectionFinished(Set <String> emojiCodePoints) {
        handleEmojiCodepoints(emojiCodePoints);
        for (MapSource source : contactMapSources.values()) {
            initContactBasedLayers(source);
            refreshSource(source.getContactId());
            applyMarkerFilter(source);
            applyLineFilter(source);
        }

        if (boundingBuilder != null && callback != null) {
            LatLngBounds bound = null;
            try {
                bound = boundingBuilder.build();
            } catch (InvalidLatLngBoundsException e) {
                Log.w(TAG, e.getLocalizedMessage());
            }
            callback.onDataInitialized(bound);
        }
    }

    public void handleEmojiCodepoints(Set<String> emojiCodePoints) {
      // generate only new emoji bitmaps, so check the diff
      emojiCodePoints.removeAll(this.emojiCodePoints);
      if (!emojiCodePoints.isEmpty()) {
        EmojiBitmapGenerationTask task = new EmojiBitmapGenerationTask(this, emojiProvider);
        task.execute(emojiCodePoints.toArray(new String[0]));
        this.emojiCodePoints.addAll(emojiCodePoints);
      }
    }

    public void filterRange(long startTimestamp, long endTimestamp) {
        int[] contactIds = getContactIds(chatId);
        filterProvider.setRangeFilter(startTimestamp, endTimestamp);
        applyFilters(contactIds);
    }

    public void filterLastPositions(long timestamp) {
        int[] contactIds = getContactIds(chatId);
        filterProvider.setLastPositionFilter(timestamp);
        applyFilters(contactIds);
    }

    public void showTraces(boolean show) {
        int[] contactIds = getContactIds(chatId);
        this.showTraces = show;
        filterProvider.setMessageFilter(!show);
        applyFilters(contactIds);
    }

    public int getChatId() {
        return chatId;
    }

    private void showLineLayer(MapSource source) {
        LineLayer lineLayer = (LineLayer) mapboxStyle.getLayer(source.getLineLayer());
        if (lineLayer != null) {
            lineLayer.setProperties(visibility(showTraces ? VISIBLE : NONE));
        }
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
        applyLastPositionFilter();
    }

    private void applyLastPositionFilter() {
        SymbolLayer markerLayer = (SymbolLayer) mapboxStyle.getLayer(LAST_POSITION_LAYER);
        if (markerLayer != null) {
            markerLayer.setFilter(filterProvider.getTimeFilter());
        }
    }

    private void applyMarkerFilter(MapSource source) {
        SymbolLayer markerLayer = (SymbolLayer) mapboxStyle.getLayer(source.getMarkerLayer());
        if (markerLayer != null) {
            markerLayer.setFilter(filterProvider.getMarkerFilter());
        }
    }

    private void applyLineFilter(MapSource source) {
        LineLayer lineLayer = (LineLayer) mapboxStyle.getLayer(source.getLineLayer());
        if (lineLayer != null) {
            lineLayer.setFilter(filterProvider.getTimeFilter());
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
        Expression iconOffset = switchCase(
                toBool(get(IS_EMOJI_CHAR)), literal(new Float[] {-2f, -23f}),
                toBool(get(LAST_LOCATION)), literal(new Float[] {-2f, -25f}),
                literal(new Float[] {-2f, -20f}));
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

    private void initLastPositionLayer() {
        GeoJsonSource lastPositionSource = new GeoJsonSource(LAST_POSITION_SOURCE);
        mapboxStyle.addSource(lastPositionSource);
        Expression markerSize =
                switchCase(toBool(get(MARKER_SELECTED)), literal(1.75f), literal(1.25f));
        mapboxStyle.addLayerBelow(new SymbolLayer(LAST_POSITION_LAYER, LAST_POSITION_SOURCE).withProperties(
                iconImage(get(LAST_POSITION_ICON)),
                     /* all info window and marker image to appear at the same time*/
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
                iconSize(markerSize),
                textField(get(LAST_POSITION_LABEL)),
                textAnchor(TEXT_ANCHOR_TOP),
                textOffset(new Float[]{0.0f, 1.0f}),
                textAllowOverlap(true),
                textIgnorePlacement(true)
        ).withFilter(filterProvider.getTimeFilter()), INFO_WINDOW_LAYER);
    }

    @SuppressWarnings( {"MissingPermission"})
    private void initLocationComponent() {
        if (! PermissionsManager.areLocationPermissionsGranted(context)) {
            return;
        }

        LocationComponentActivationOptions locationComponentActivationOptions = LocationComponentActivationOptions
                .builder(context, mapboxStyle)
                .build();
        locationComponent.activateLocationComponent(locationComponentActivationOptions);
        locationComponent.setRenderMode(COMPASS);
        locationComponent.setLocationComponentEnabled(true);
    }

    private void initContactBasedLayers(MapSource source) {
        if (mapboxStyle.getLayer(source.getMarkerLayer()) != null) {
            return;
        }

        GeoJsonSource markerPositionSource = new GeoJsonSource(source.getMarkerSource());
        GeoJsonSource linePositionSource = new GeoJsonSource(source.getLineSource());

        try {
            mapboxStyle.addSource(markerPositionSource);
            mapboxStyle.addSource(linePositionSource);
        } catch (RuntimeException e) {
            //TODO: specify exception more
            Log.e(TAG, "Unable to init GeoJsonSources. Already added to mapBoxMap? " + e.getMessage());
        }

        mapboxStyle.addImage(source.getMarkerLastPositon(),
                generateColoredLastPositionIcon(source.getColorArgb()));
        mapboxStyle.addImage(source.getMarkerIcon(),
                generateColoredLocationIcon(source.getColorArgb()));
        mapboxStyle.addImage(source.getMarkerPoi(),
                generateColoredPoiIcon(source.getColorArgb()));

        Expression markerSize =
                switchCase(
                        neq(length(get(MARKER_CHAR)), literal(0)),
                            switchCase(toBool(get(MARKER_SELECTED)), literal(2.25f), literal(2.0f)),
                        neq(get(MESSAGE_ID), literal(0)),
                            switchCase(toBool(get(MARKER_SELECTED)), literal(2.25f), literal(2.0f)),
                        switchCase(toBool(get(MARKER_SELECTED)), literal(1.1f), literal(0.7f)));
        Expression markerIcon = get(MARKER_ICON);

        mapboxStyle.addLayerBelow(new LineLayer(source.getLineLayer(), source.getLineSource())
                .withProperties(PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        lineJoin(Property.LINE_JOIN_ROUND),
                        lineWidth(3f),
                        lineOpacity(0.5f),
                        lineColor(source.getColorArgb()),
                        visibility(NONE)
                )
                .withFilter(filterProvider.getTimeFilter()),
                LAST_POSITION_LAYER);


        Expression textField = switchCase(eq(length(get(MARKER_CHAR)), 1), get(MARKER_CHAR),
                get(POI_LONG_DESCRIPTION));
        Float[] offset = new Float[] {0.0f, 1.25f};
        Float[] zeroOffset = new Float[] {0.0f, 0.0f};
        Expression textOffset = switchCase(
                has(POI_LONG_DESCRIPTION), literal(offset),
                literal(zeroOffset));
        Expression textColor = switchCase(
                has(POI_LONG_DESCRIPTION), literal("#000000"),
                literal("#FFFFFF")
        );
        Expression textAnchor = switchCase(
                has(POI_LONG_DESCRIPTION), literal(TEXT_ANCHOR_TOP),
                literal(TEXT_ANCHOR_CENTER)
        );
        Expression textSize = switchCase(
                has(POI_LONG_DESCRIPTION), literal(12.0f),
                literal(15.0f)
        );

        mapboxStyle.addLayerBelow(new SymbolLayer(source.getMarkerLayer(), source.getMarkerSource())
                        .withProperties(
                                iconImage(markerIcon),
                                iconSize(markerSize),
                                iconIgnorePlacement(false),
                                iconAllowOverlap(false),
                                textField(textField),
                                textOffset(textOffset),
                                textAnchor(textAnchor),
                                textSize(textSize),
                                textColor(textColor))
                        .withFilter(all(filterProvider.getMarkerFilter(),
                                not(get(LAST_LOCATION)))),
                LAST_POSITION_LAYER);
    }

    private Bitmap generateColoredLastPositionIcon(int colorFilter) {
        return generateColoredBitmap(context, colorFilter, R.drawable.ic_location_on_white_48dp);
    }

    private Bitmap generateColoredLocationIcon(int colorFilter) {
        return generateColoredBitmap(context, colorFilter, R.drawable.ic_location_dot);
    }

    private Bitmap generateColoredPoiIcon(int colorFilter) {
        return generateColoredBitmap(context, colorFilter, R.drawable.ic_location_poi_dot);
    }

    private void updateSources() {
        new DataCollectionTask(dcContext,
                chatId,
                getContactIds(chatId),
                contactMapSources,
                featureCollections,
                lastPositions,

                boundingBuilder,
                emojiProvider,
                this).execute();
    }

    public void updateSource(int contactId) {
        new DataCollectionTask(dcContext,
                chatId,
                new int[]{contactId},
                contactMapSources,
                featureCollections,
                lastPositions,
                boundingBuilder,
                emojiProvider,
                this).execute();
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
        for (Map.Entry<String, LinkedList<Feature>> e : featureCollections.entrySet()) {
            String key = e.getKey();
            if (key.startsWith(LINE_FEATURE_LIST)) {
                continue;
            }
            LinkedList<Feature> featureCollection = e.getValue();
            for (Feature f : featureCollection) {
                if (f.id().equals(id)) {
                    return f;
                }
            }
        }
        return null;
    }

}
