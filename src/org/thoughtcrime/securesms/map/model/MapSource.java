package org.thoughtcrime.securesms.map.model;

import android.graphics.Color;

import com.b44t.messenger.DcContact;

/**
 * Created by cyberta on 07.03.19.
 */

public class MapSource {
    public static final String LINE_LAYER = "line_layer";
    public static final String MARKER_LAYER = "symbol_layer";
    public static final String INFO_WINDOW_LAYER = "info_window_layer";
    public static final String LINE_SOURCE = "line_source";
    public static final String MARKER_POSITION_SOURCE = "marker_position";
    public static final String MARKER_ICON = "marker_icon_id";
    public static final String MARKER_POI = "marker_poi";
    public static final String MARKER_LAST_POSITON = "marker_last_position";
    public static final String MARKER_FEATURE_LIST = "marker_feature_list";
    public static final String LINE_FEATURE_LIST = "line_feature_list";

    private final String markerSource;
    private final String lineSource;

    private final String markerLayer;
    private final String lineLayer;

    private final String markerIcon;
    private final String markerLastPositon;
    private final String markerPoi;
    private final String markerFeatureCollection;
    private final String lineFeatureCollection;
    private final String displayName;

    private final int color;
    private final int colorArgb;
    private final int contactId;

    public MapSource(DcContact contact) {
        int contactId = contact.getId();
        markerSource = MARKER_POSITION_SOURCE + "_" + contactId;
        lineSource = LINE_SOURCE + "_" + contactId;
        markerLayer = MARKER_LAYER + "_" + contactId;
        lineLayer = LINE_LAYER + "_" + contactId;
        markerIcon = MARKER_ICON + "_" + contactId;
        markerLastPositon = MARKER_LAST_POSITON + "_" + contactId;
        markerPoi = MARKER_POI + "_" + contactId;
        markerFeatureCollection = MARKER_FEATURE_LIST + "_" + contactId;
        lineFeatureCollection = LINE_FEATURE_LIST + "_" + contactId;
        this.contactId = contactId;
        displayName = contact.getDisplayName();
        color = contact.getColor();
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

    public String getMarkerLayer() {
        return markerLayer;
    }

    public String getLineLayer() {
        return lineLayer;
    }

    public String getMarkerIcon() {
        return markerIcon;
    }

    public String getMarkerLastPositon() {
        return markerLastPositon;
    }

    public String getMarkerPoi() {
        return markerPoi;
    }

    public String getMarkerFeatureCollection() { return markerFeatureCollection; }

    public String getLineFeatureCollection() { return lineFeatureCollection; }

    public int getContactId() { return contactId; }

    public String getDisplayName() {
        return displayName;
    }

}
