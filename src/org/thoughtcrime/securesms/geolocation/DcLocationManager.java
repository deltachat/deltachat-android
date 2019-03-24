package org.thoughtcrime.securesms.geolocation;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import com.b44t.messenger.DcChatlist;

import org.thoughtcrime.securesms.connect.DcHelper;

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
            Log.d(TAG, "background service connected");
            serviceBinder = (LocationBackgroundService.LocationBackgroundServiceBinder) service;
            while (pendingShareLastLocation.size() > 0) {
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
        DcChatlist chats = DcHelper.getContext(context).getChatlist(0, null, 0);
        for (int i = 0; i < chats.getCnt(); i++) {
            if (DcHelper.getContext(context).isSendingLocationsToChat(chats.getChat(i).getId())) {
                initializeLocationEngine();
                return;
            }
        }
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

    public void stopSharingLocation(int chatId) {
        DcHelper.getContext(context).sendLocationsToChat(chatId, 0);
    }

    public void shareLocation(int duration, int chatId) {
        startLocationEngine();
        Log.d(TAG, String.format("Share location in chat %d for %d seconds", chatId, duration));
        DcHelper.getContext(context).sendLocationsToChat(chatId, duration);
    }

    public void shareLastLocation(int chatId) {
        if (serviceBinder == null) {
            pendingShareLastLocation.push(chatId);
            initializeLocationEngine();
            return;
        }

        if (dcLocation.isValid()) {
            DcHelper.getContext(context).sendLocationsToChat(chatId, 1);
            writeDcLocationUpdateMessage();
        }
    }

    public void deleteAllLocations() {
        DcHelper.getContext(context).deleteAllLocations();
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
