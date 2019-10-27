package org.thoughtcrime.securesms.geolocation;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Date;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.ServiceUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.b44t.messenger.DcArray;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import com.mapbox.mapboxsdk.location.CompassEngine;
import com.mapbox.mapboxsdk.location.CompassListener;

import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.FileObserver;
import android.util.JsonReader;
import android.util.Log;
import android.view.WindowManager;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Location__DRAFT implements CompassListener {

    private static final String TAG = "Location__DRAFT";

    private static final String PERMANENT_LOCATION_STREAMING_CONTACT_NAME_QUERY = "% *";

    private static final String WELL_KNOWN_WIFI_MESSAGE_TO_ID_SELF_QUERY = "% *";

    private static final int ONE_DAY_SECONDS = 24 * 60 * 60;

    private static final float GPS_UNDEFINED_VALUE = 0f;

    private static final String GPS_START = "immediatestart", GPS_STOP = "immediatestop";

    private static final float COMPASS_UNDEFINED_VALUE = 360f;

    private static final float COMPASS_DIFFERENCE_MAX = 10f;

    private final Context context;

    private long previousLocationTime;

    private XmlPullParserFactory xmlParserFactory;

    private String actionGpsLogger;

    private CompassEngine compassEngine;

    private int compassCount;

    private float lastHeading;

    private boolean isStationary;

    private FileObserver fileObserver;

    public Location__DRAFT(Context context) {
        this.context = context.getApplicationContext();
        try {
            this.xmlParserFactory = XmlPullParserFactory.newInstance();
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Couldn't create XmlPullParserFactory instance", e);
        }
        this.previousLocationTime = System.currentTimeMillis();
        this.compassEngine = createCompassEngine();
        this.lastHeading = COMPASS_UNDEFINED_VALUE;
        this.fileObserver = new FileObserver(getGpsLoggerDir(this.context).getPath(), FileObserver.CLOSE_WRITE) {
            @Override
            public void onEvent(int event, String path) {
                if (path != null) {
                    Log.d(TAG, String.format("GPSLogger file written: %s", path));
                    readGpsLoggerLocation();
                }
            }
        };
    }

    private static File getGpsLoggerDir(Context context) {
        File filesDir = context.getExternalFilesDir(null);
        return new File(filesDir.getParentFile().getParentFile(), "com.mendhak.gpslogger/files");
    }

    private CompassEngine createCompassEngine() {
        try {
            Class<?> packagePrivateClass = Class
                    .forName("com.mapbox.mapboxsdk.location.LocationComponentCompassEngine");
            Constructor<?> packagePrivateConstructor = packagePrivateClass.getDeclaredConstructor(WindowManager.class,
                    SensorManager.class);
            packagePrivateConstructor.setAccessible(true);
            WindowManager windowManager = ServiceUtil.getWindowManager(context);
            SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            return (CompassEngine) packagePrivateConstructor.newInstance(windowManager, sensorManager);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
                | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Log.e(TAG, "Couldn't create a Mapbox CompassEngine", e);
            return new CompassEngine() {
                @Override
                public void removeCompassListener(CompassListener compassListener) {
                }

                @Override
                public float getLastHeading() {
                    return COMPASS_UNDEFINED_VALUE;
                }

                @Override
                public int getLastAccuracySensorStatus() {
                    return SensorManager.SENSOR_STATUS_NO_CONTACT;
                }

                @Override
                public void addCompassListener(CompassListener compassListener) {
                    Log.e(TAG, "CompassEngine not available");
                }
            };
        }
    }

    public void updateLocation() {
        if (!enablePermanentLocationStreaming()) {
            if (GPS_START.equals(actionGpsLogger)) {
                sendIntentToGpsLogger(GPS_STOP);
            }
            return;
        }
        if (isGpsLoggerInstalled(context)) {
            fileObserver.startWatching();
            if (readWifiLocation() || detectStationary()) {
                sendIntentToGpsLogger(GPS_STOP);
            } else {
                sendIntentToGpsLogger(GPS_START);
            }
        }
    }

    private boolean detectStationary() {
        // removing listener here to be save, see: #idleCompass(float)
        compassEngine.removeCompassListener(this);
        compassEngine.addCompassListener(this);
        return isStationary;
    }

    @Override
    public void onCompassAccuracyChange(int compassStatus) {
        Log.d(TAG, String.format("Compass accuracy change: %s", compassStatus));
    }

    @Override
    public void onCompassChanged(float userHeading) {
        compassCount++;
        if (compassCount > 15 && isCompassChanged(lastHeading, userHeading)) {
            Log.d(TAG, String.format("Compass motion at: %s, current: %s, previous: %s", compassCount, userHeading,
                    lastHeading));
            isStationary = false;
            idleCompass(userHeading);
            sendIntentToGpsLogger(GPS_START);
        } else if (compassCount > 20) {
            isStationary = isStationary(lastHeading, userHeading);
            Log.d(TAG, String.format("Compass heading: %s, previous: %s", userHeading, lastHeading));
            idleCompass(userHeading);
            if (isStationary) {
                sendIntentToGpsLogger(GPS_STOP);
            } else {
                sendIntentToGpsLogger(GPS_START);
            }
        }
    }

    private boolean isCompassChanged(float previous, float userHeading) {
        return previous != COMPASS_UNDEFINED_VALUE && compassDiff(previous, userHeading) > COMPASS_DIFFERENCE_MAX;
    }

    private boolean isStationary(float previous, float userHeading) {
        return previous != COMPASS_UNDEFINED_VALUE && compassDiff(previous, userHeading) <= COMPASS_DIFFERENCE_MAX;
    }

    private float compassDiff(float previous, float userHeading) {
        float prev = previous + COMPASS_UNDEFINED_VALUE % COMPASS_UNDEFINED_VALUE;
        float next = userHeading + COMPASS_UNDEFINED_VALUE % COMPASS_UNDEFINED_VALUE;
        return prev > next ? prev - next : next - prev;
    }

    private void idleCompass(float current) {
        lastHeading = current;
        compassCount = 0;
        compassEngine.removeCompassListener(this);
    }

    private boolean enablePermanentLocationStreaming() {
        boolean result = false;
        ApplicationDcContext ac = DcHelper.getContext(context);
        if (ac != null) {
            int[] homeMates = ac.getContacts(DcContext.DC_GCL_NO_SPECIALS,
                    PERMANENT_LOCATION_STREAMING_CONTACT_NAME_QUERY);
            Log.d(TAG, String.format("Contacts with permanent location streaming, query: '%s': %s",
                    PERMANENT_LOCATION_STREAMING_CONTACT_NAME_QUERY, Arrays.toString(homeMates)));
            for (int i = 0; i < homeMates.length; i++) {
                result = true;
                int contactId = homeMates[i];
                int chatId = ac.getChatIdByContactId(contactId);
                if (!ac.isSendingLocationsToChat(chatId)) {
                    Log.d(TAG, String.format("Locations to Chat ID: %s for Contact ID: %s", chatId, contactId));
                    ac.sendLocationsToChat(chatId, ONE_DAY_SECONDS);
                }
            }
        }
        return result;
    }

    private void sendIntentToGpsLogger(String action) {
        if (!action.equals(actionGpsLogger)) {
            actionGpsLogger = action;
            Intent i = new Intent("com.mendhak.gpslogger.TASKER_COMMAND");
            i.setPackage("com.mendhak.gpslogger");
            i.putExtra(action, true);
            context.sendBroadcast(i);
            Log.d(TAG, String.format("Intent to com.mendhak.gpslogger: %s", action));
        }
    }

    private boolean readWifiLocation() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            Log.e(TAG, String.format("ConnectivityManager is null"));
            return false;
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null || activeNetwork.getType() != ConnectivityManager.TYPE_WIFI) {
            Log.d(TAG, String.format("WIFI Location skipped: no wifi"));
            return false;
        }
        String wifiName = activeNetwork.getExtraInfo();
        return readWifiOnIceLocation(wifiName) || readWellKnownWifiLocation(wifiName);
    }

    private boolean readWifiOnIceLocation(String wifiName) {
        if (!"\"WIFIonICE\"".equals(wifiName)) {
            Log.d(TAG, String.format("ICE Location skipped: no ICE '%s'", wifiName));
            return false;
        }
        OkHttpClient client = new OkHttpClient/* .Builder().callTimeout(500, TimeUnit.MILLISECONDS).build */();
        Request request = new Request.Builder().url("https://iceportal.de/api1/rs/status").build();
        Call call = client.newCall(request);
        try (Response response = call.execute(); JsonReader reader = new JsonReader(response.body().charStream())) {
            if (response.code() != 200) {
                Log.d(TAG, String.format("ICE Location returns %s, Content-type '%s'", response.code(),
                        response.header("Content-type")));
                return false;
            }
            float latitude = 0;
            float longitude = 0;
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("latitude")) {
                    latitude = (float) reader.nextDouble();
                } else if (name.equals("longitude")) {
                    longitude = (float) reader.nextDouble();
                } else if (name.equals("serverTime")) {
                    long serverTime = reader.nextLong();
                    if (serverTime + 60000 < System.currentTimeMillis()) {
                        Log.d(TAG,
                                String.format("ICE Location outdated: %sms", System.currentTimeMillis() - serverTime));
                    } else {
                        Log.d(TAG, "ICE Location");
                    }
                    return updateLocation(System.currentTimeMillis(), latitude, longitude);
                } else {
                    reader.skipValue();
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "ICE Location failed: " + e);
        }
        return false;
    }

    private boolean readWellKnownWifiLocation(String wifiName) {
        ApplicationDcContext ac = DcHelper.getContext(context);
        if (ac != null) {
            int contactId = DcContact.DC_CONTACT_ID_SELF;
            int chatId = ac.getChatIdByContactId(contactId);
            String messageText = WELL_KNOWN_WIFI_MESSAGE_TO_ID_SELF_QUERY.replace("%", wifiName);
            int[] msgs = ac.searchMsgs(chatId, messageText);
            Log.d(TAG, String.format("Query: %s, contact: %s, chat: %s, msgs: %s", messageText, contactId, chatId,
                    Arrays.toString(msgs)));
            if (msgs.length > 0) {
                if (GPS_STOP.equals(actionGpsLogger)) {
                    return true;
                }
                DcMsg msg = ac.getMsg(msgs[msgs.length - 1]);
                if (msg.hasLocation()) {
                    DcArray dcarr = ac.getLocations(chatId, contactId, msg.getTimestamp(), msg.getTimestamp());
//                    debug(dcarr);
                    for (int i = 0, len = dcarr.getCnt(); i < len; i++) {
                        if (dcarr.getMsgId(i) == msg.getId()) {
                            Log.d(TAG, String.format("Well Known Network: '%s' %s %s", wifiName, dcarr.getLatitude(i),
                                    dcarr.getLongitude(i)));
                            updateLocation(System.currentTimeMillis(), dcarr.getLatitude(i), dcarr.getLongitude(i));
                            if (ONE_DAY_SECONDS * 1000 + msg.getTimestamp() < System.currentTimeMillis()) {
                                DcMsg current = new DcMsg(ac, DcMsg.DC_MSG_TEXT);
                                current.setLocation(dcarr.getLatitude(i), dcarr.getLongitude(i));
                                current.setText(messageText);
                                // synchronous since it's already in the mailer thread
                                ac.sendMsg(chatId, current);
                                ac.deleteMsgs(msgs);
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    void debug(DcArray dcarr) {
        if (dcarr == null) {
            Log.d(TAG, "DcArray is null");
        } else {
            for (int i = 0; i < dcarr.getCnt(); i++) {
                Log.d(TAG, String.format("%s: %s %s, loc: %s, msg: %s, at: %s", //
                        i, //
                        dcarr.getLatitude(i), //
                        dcarr.getLongitude(i), //
                        dcarr.getLocationId(i), //
                        dcarr.getMsgId(i), //
                        new Date(dcarr.getTimestamp(i))));
            }
        }
    }

    private boolean updateLocation(long timestamp, float latitude, float longitude) {
        if (timestamp > previousLocationTime) {
            Log.d(TAG, String.format("Update latitude: %s, longitude: %s, timestamp: %s, after: %sms", latitude,
                    longitude, timestamp, timestamp - previousLocationTime));
            previousLocationTime = timestamp;
            ApplicationDcContext ac = DcHelper.getContext(context);
            if (ac != null) {
                ac.setLocation(latitude, longitude, 16);
                return true;
            } else {
                Log.d(TAG, "ApplicationDcContext null");
                return false;
            }
        }
        Log.d(TAG, String.format("Update ignored timestamp: %s, last: %s", timestamp, previousLocationTime));
        return false;
    }

    public static final boolean isGpsLoggerInstalled(Context context) {
        return getGpsLoggerDir(context).isDirectory();
    }

    private boolean readGpsLoggerLocation() {
        File fileOfToday = new File(context.getExternalFilesDir(null).getParentFile().getParentFile(),
                String.format("com.mendhak.gpslogger/files/%1$tY%1$tm%1$td.gpx", new Date()));
        long lastModified = fileOfToday.lastModified();
        if (lastModified > previousLocationTime) {
            Log.d(TAG, "readLocationsFromFile: " + fileOfToday);
            if (fileOfToday.isFile()) {
                try (InputStream inputStream = new FileInputStream(fileOfToday)) {
                    XmlPullParser xpp = xmlParserFactory.newPullParser();
                    xpp.setInput(inputStream, null);
                    int eventType = xpp.next();
                    if (eventType != XmlPullParser.START_TAG || !"gpx".equals(xpp.getName())) {
                        Log.e(TAG, "GPS Logger GPX file does not contain gpx");
                        return false;
                    }
                    Log.d(TAG, "GPX Version=" + parseString(xpp, "version", "--"));
                    float latitude = GPS_UNDEFINED_VALUE;
                    float longitude = GPS_UNDEFINED_VALUE;
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG && "trkpt".equals(xpp.getName())) {
                            latitude = parseFloat(xpp, "lat", GPS_UNDEFINED_VALUE);
                            longitude = parseFloat(xpp, "lon", GPS_UNDEFINED_VALUE);
                        }
                        eventType = xpp.next();
                    }
                    Log.d(TAG, String.format("Location parsed latitude=%s, longitude=%s", latitude, longitude));
                    return updateLocation(lastModified, latitude, longitude);
                } catch (Exception e) {
                    Log.e(TAG, "Parsing GPS Logger GPX file failed", e);
                }
            }
        }
        return false;
    }

    private float parseFloat(XmlPullParser xpp, String name, float defaultValue) {
        String value = xpp.getAttributeValue(null, name);
        return value == null ? defaultValue : Float.parseFloat(value);
    }

    private String parseString(XmlPullParser xpp, String name, String defaultValue) {
        String value = xpp.getAttributeValue(null, name);
        return value == null ? defaultValue : value;
    }

}
