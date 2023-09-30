package org.thoughtcrime.securesms.map;

import android.os.AsyncTask;
import android.util.Log;

import com.b44t.messenger.DcContext;
import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;

import org.thoughtcrime.securesms.components.emoji.EmojiProvider;
import org.thoughtcrime.securesms.map.model.MapSource;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.thoughtcrime.securesms.map.MapDataManager.TIMESTAMP_NOW;
import static org.thoughtcrime.securesms.map.MapDataManager.TIME_FRAME;

/**
 * Created by cyberta on 15.04.19.
 */

public class DataCollectionTask extends AsyncTask<Void, Void, Set<String>> {

    public interface DataCollectionCallback {
        void onDataCollectionFinished(Set<String> emojiCodepoints);
    }

    private static final String TAG = DataCollectionTask.class.getSimpleName();
    private static final Set<DataCollectionTask> instances = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final int chatId;
    private final int[] contactIds;
    private final ConcurrentHashMap<Integer, MapSource> contactMapSources;
    private final ConcurrentHashMap<String, LinkedList<Feature>> featureCollections;
    private final ConcurrentHashMap<Integer, Feature> lastPositions;
    private final LatLngBounds.Builder boundingBuilder;
    private final DcContext dcContext;
    private final DataCollectionCallback callback;
    private final EmojiProvider emojiProvider;

    public DataCollectionTask(DcContext context,
                              int chatId,
                              int[] contactIds,
                              ConcurrentHashMap<Integer, MapSource> contactMapSources,
                              ConcurrentHashMap featureCollections,
                              ConcurrentHashMap<Integer, Feature> lastPositions,
                              LatLngBounds.Builder boundingBuilder,
                              EmojiProvider emojiProvider,
                              DataCollectionCallback callback) {
        this.chatId = chatId;
        this.contactMapSources = contactMapSources;
        this.featureCollections = featureCollections;
        this.lastPositions = lastPositions;
        this.boundingBuilder = boundingBuilder;
        this.dcContext = context;
        this.callback = callback;
        this.contactIds = contactIds;
        this.emojiProvider = emojiProvider;
        instances.add(this);
    }

    public static void cancelRunningTasks() {
        for (DataCollectionTask task : instances) {
            task.cancel(true);
        }
    }

    @Override
    protected Set<String> doInBackground(Void... voids) {
        Log.d(TAG, "performance test - collect Data start");
        HashSet<String> emojiCodePoints = new HashSet<>();
        DataCollector dataCollector = new DataCollector(dcContext,
                contactMapSources,
                featureCollections,
                lastPositions,
                emojiCodePoints,
                emojiProvider,
                boundingBuilder);
        for (int contactId : contactIds) {
            dataCollector.updateSource(chatId,
                    contactId,
                    System.currentTimeMillis() - TIME_FRAME,
                    TIMESTAMP_NOW);
            if (this.isCancelled()) {
                break;
            }
        }
        return emojiCodePoints;
    }

    @Override
    protected void onPostExecute(Set<String> emojiCodePoints) {
        if (!this.isCancelled()) {
            callback.onDataCollectionFinished(emojiCodePoints);
        }
        instances.remove(this);
        Log.d(TAG, "performance test - collect Data finished");
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        instances.remove(this);
    }
}
