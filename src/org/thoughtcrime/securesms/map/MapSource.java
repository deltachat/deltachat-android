package org.thoughtcrime.securesms.map;

import android.graphics.Color;

/**
 * Created by cyberta on 07.03.19.
 */

public class MapSource {
    public static final String LINE_LAYER = "line_layer";
    public static final String MARKER_LAYER = "symbol_layer";
    public static final String LAST_POSITON_LAYER = "last_position_layer";
    public static final String LINE_SOURCE = "line_source";
    public static final String MARKER_POSITION_SOURCE = "marker_position";
    public static final String LAST_POSITION_SOURCE = "marker_last_position";
    public static final String MARKER_ICON = "marker_icon_id";
    public static final String MARKER_LAST_POSITON = "marker_last_position";
    public static final String MARKER_FEATURE_LIST = "marker_feature_list";

    private final String markerSource;
    private final String lineSource;
    private final String lastPositionSource;

    private final String markerLayer;
    private final String lineLayer;
    private final String lastPositionLayer;

    private final String markerIcon;
    private final String markerLastPositon;
    private final String markerFeatureCollection;

    private int color;
    private int colorArgb;

    public MapSource(int chatId) {
        markerSource = MARKER_POSITION_SOURCE + "_" + chatId;
        lineSource = LINE_SOURCE + "_" + chatId;
        lastPositionSource = LAST_POSITION_SOURCE + "_" + chatId;
        markerLayer = MARKER_LAYER + "_" + chatId;
        lineLayer = LINE_LAYER + "_" + chatId;
        lastPositionLayer = LAST_POSITON_LAYER + "_" + chatId;
        markerIcon = MARKER_ICON + "_" + chatId;
        markerLastPositon = MARKER_LAST_POSITON + "_" + chatId;
        markerFeatureCollection = MARKER_FEATURE_LIST + "_" + chatId;
    }

    public void setColor(int color) {
        this.color = color;
        colorArgb = Color.argb(0xFF, Color.red(color), Color.green(color), Color.blue(color));
    }

    public int getColorArgb() {
        return colorArgb;
    }

    public int getColor() {
        return color;
    }

    public String getMarkerSource() {
        return markerSource;
    }

    public String getLineSource() {
        return lineSource;
    }

    public String getLastPositionSourceName() {
        return lastPositionSource;
    }

    public String getMarkerLayer() {
        return markerLayer;
    }

    public String getLineLayer() {
        return lineLayer;
    }

    public String getLastPositionLayer() {
        return lastPositionLayer;
    }

    public String getMarkerIcon() {
        return markerIcon;
    }

    public String getMarkerLastPositon() {
        return markerLastPositon;
    }

    public String getLastPositionSource() {
        return lastPositionSource;
    }

    public String getMarkerFeatureCollection() { return markerFeatureCollection; }
}
