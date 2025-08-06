package org.thoughtcrime.securesms.geolocation;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import org.thoughtcrime.securesms.connect.DcHelper;

import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import static android.content.Context.BIND_AUTO_CREATE;

public class DcLocationManager implements Observer {

    private static final String TAG = DcLocationManager.class.getSimpleName();
    private LocationBackgroundService.LocationBackgroundServiceBinder serviceBinder;
    private final Context context;
    private DcLocation dcLocation = DcLocation.getInstance();
    private final LinkedList<Integer> pendingShareLastLocation = new LinkedList<>();
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "background service connected");
            serviceBinder = (LocationBackgroundService.LocationBackgroundServiceBinder) service;
            while (!pendingShareLastLocation.isEmpty()) {
                shareLastLocation(pendingShareLastLocation.pop());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "background service disconnected");
            serviceBinder = null;
        }
    };

    public DcLocationManager(Context context) {
        this.context = context.getApplicationContext();
        DcLocation.getInstance().addObserver(this);
        if (DcHelper.getContext(context).isSendingLocationsToChat(0)) {
            startLocationEngine();
        }
    }

    public void startLocationEngine() {
        if (serviceBinder == null) {
            Intent intent = new Intent(context.getApplicationContext(), LocationBackgroundService.class);
            context.bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        }
    }

    public void stopLocationEngine() {
        if (serviceBinder == null) {
            return;
        }
        context.unbindService(serviceConnection);
        serviceBinder.stop();
        serviceBinder = null;
    }

    public void stopSharingLocation(int chatId) {
        DcHelper.getContext(context).sendLocationsToChat(chatId, 0);
        if(!DcHelper.getContext(context).isSendingLocationsToChat(0)) {
            stopLocationEngine();
        }
    }

    public void shareLocation(int duration, int chatId) {
        startLocationEngine();
        Log.d(TAG, String.format("Share location in chat %d for %d seconds", chatId, duration));
        DcHelper.getContext(context).sendLocationsToChat(chatId, duration);
        if (dcLocation.isValid()) {
            writeDcLocationUpdateMessage();
        }
    }

    public void shareLastLocation(int chatId) {
        if (serviceBinder == null) {
            pendingShareLastLocation.push(chatId);
            startLocationEngine();
            return;
        }

        if (dcLocation.isValid()) {
            DcHelper.getContext(context).sendLocationsToChat(chatId, 1);
            writeDcLocationUpdateMessage();
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof DcLocation) {
            dcLocation = (DcLocation) o;
            if (dcLocation.isValid()) {
                writeDcLocationUpdateMessage();
            }
        }
    }

    private void writeDcLocationUpdateMessage() {
        Log.d(TAG, "Share location: " + dcLocation.getLastLocation().getLatitude() + ", " + dcLocation.getLastLocation().getLongitude());
        Location lastLocation = dcLocation.getLastLocation();

        boolean continueLocationStreaming = DcHelper.getContext(context).setLocation((float) lastLocation.getLatitude(), (float) lastLocation.getLongitude(), lastLocation.getAccuracy());
        if (!continueLocationStreaming) {
            stopLocationEngine();
        }
    }
}
