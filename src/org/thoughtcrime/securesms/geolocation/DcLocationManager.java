package org.thoughtcrime.securesms.geolocation;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by cyberta on 06.03.19.
 */

public class DcLocationManager implements Observer {

    private static final String TAG = DcLocationManager.class.getSimpleName();
    private LocationBackgroundService.LocationBackgroundServiceBinder serviceBinder;
    private Context context;
    private DcLocation dcLocation = DcLocation.getInstance();
    private LinkedList<Integer> pendingShareLastLocation = new LinkedList<>();
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceBinder = (LocationBackgroundService.LocationBackgroundServiceBinder) service;
            while (pendingShareLastLocation.size() > 0) {
                shareLastLocation(pendingShareLastLocation.pop());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBinder = null;
        }
    };

    public DcLocationManager(Context context) {
        this.context = context.getApplicationContext();
        DcLocation.getInstance().addObserver(this);
    }


    private void initializeLocationEngine() {
        Intent intent = new Intent(context.getApplicationContext(), LocationBackgroundService.class);
        context.bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    public void startLocationEngine() {
        if (serviceBinder == null) {
            initializeLocationEngine();
        }
    }

    public void stopLocationEngine() {
        if (serviceBinder == null) {
            return;
        }
        serviceBinder.stop();
    }

    public void shareLocation(int duration, int chatId) {
        startLocationEngine();
        Log.d(TAG, String.format("Share location in chat %d for %d seconds", chatId, duration));
    }

    public void shareLastLocation(int chatId) {
        if (serviceBinder == null) {
            pendingShareLastLocation.push(chatId);
            initializeLocationEngine();
            return;
        }

        Location location = dcLocation.getLastLocation();
        Log.d(TAG, "share lastLocation: " + location.getLatitude() + ", " + location.getLongitude());
        //TODO: implement me! write share location message
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof DcLocation) {
            dcLocation = (DcLocation) o;
            writeDcLocationUpdateMessage();
        }
    }

    private void writeDcLocationUpdateMessage() {
        //TODO: implement me!
        Log.d(TAG, "Share location: " + dcLocation.getLastLocation().getLatitude() + ", " + dcLocation.getLastLocation().getLongitude());
    }
}
