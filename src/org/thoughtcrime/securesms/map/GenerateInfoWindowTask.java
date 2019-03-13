package org.thoughtcrime.securesms.map;

/**
 * Created by cyberta on 13.03.19.
 */

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mapbox.geojson.Feature;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.BitmapUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import static org.thoughtcrime.securesms.map.MapDataManager.INFO_WINDOW_ID;
import static org.thoughtcrime.securesms.map.MapDataManager.TIMESTAMP;

/**
 * AsyncTask to generate Bitmap from Views to be used as iconImage in a SymbolLayer.
 * <p>
 * Call be optionally be called to update the underlying data source after execution.
 * </p>
 * <p>
 * Generating Views on background thread since we are not going to be adding them to the view hierarchy.
 * </p>
 */
public class GenerateInfoWindowTask extends AsyncTask<ArrayList<Feature>, HashMap<String, Bitmap>, HashMap<String, Bitmap>> {

    private static final String TAG = GenerateInfoWindowCallback.class.getName();
    private final WeakReference<GenerateInfoWindowCallback> callbackRef;
    private final int contactId;

    GenerateInfoWindowTask(GenerateInfoWindowCallback callback, int contactId) {
        this.callbackRef = new WeakReference<>(callback);
        this.contactId = contactId;
    }

    @SuppressWarnings("WrongThread")
    @Override
    protected HashMap<String, Bitmap> doInBackground(ArrayList<Feature>... params) {
        Log.d(TAG, "GenerateInfoWindowTask start");
        Thread.currentThread().setName(GenerateInfoWindowTask.class.getName());
        HashMap<String, Bitmap> imagesMap = new HashMap<>();

        try {
            LayoutInflater inflater = LayoutInflater.from(callbackRef.get().getContext());

            ArrayList<Feature> featureList = params[0];
            for (int i = 0; i < featureList.size(); i++) {
                Feature feature = featureList.get(i);

                Log.d(TAG, "GenerateInfoWindowTask: feature " + i+ ": " + feature.id());

                LinearLayout bubbleLayout = (LinearLayout)
                        inflater.inflate(R.layout.map_bubble_layout, null);
                TextView titleTextView = bubbleLayout.findViewById(R.id.conversation_item_body);
                titleTextView.setText("TIMESTAMP: " + feature.getNumberProperty(TIMESTAMP));
                bubbleLayout.setBackgroundResource(R.drawable.message_bubble_background_received_alone);

                Bitmap bitmap = BitmapUtil.generate(bubbleLayout);

                String id = feature.getStringProperty(INFO_WINDOW_ID);
                imagesMap.put(id, bitmap);

                if (i % 10 == 0) {
                    publishProgress(imagesMap);
                    imagesMap.clear();
                }
            }
        } catch (NullPointerException npe) {
            npe.printStackTrace();
            Log.e(TAG, "Callback was GC'ed before task finished.");
        }

        Log.d(TAG, "GenerateInfoWindowTask finished");
        return imagesMap;
    }

    @Override
    protected void onProgressUpdate(HashMap<String, Bitmap>... imagesMap) {
        callbackRef.get().setInfoWindowResults(imagesMap[0]);
        callbackRef.get().refreshSource(contactId);
        Log.d(TAG, "updating progress for contact: " + contactId);
    }

    @Override
    protected void onPostExecute(HashMap<String, Bitmap> bitmapHashMap) {
        GenerateInfoWindowCallback callback = callbackRef.get();
        if (callback != null && bitmapHashMap != null && bitmapHashMap.size() > 0) {
            callback.setInfoWindowResults(bitmapHashMap);
            callback.refreshSource(contactId);
        }
    }
}