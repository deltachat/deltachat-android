package org.thoughtcrime.securesms.map;

/**
 * Created by cyberta on 13.03.19.
 */

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import com.mapbox.geojson.Feature;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.ConversationItemFooter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.BitmapUtil;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.DynamicLanguage;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import static android.view.View.GONE;
import static org.thoughtcrime.securesms.map.MapDataManager.CONTACT_ID;
import static org.thoughtcrime.securesms.map.MapDataManager.INFO_WINDOW_ID;
import static org.thoughtcrime.securesms.map.MapDataManager.MESSAGE_ID;
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
                bubbleLayout.setBackgroundResource(R.drawable.message_bubble_background_received_alone);
                TextView conversationItemBody = bubbleLayout.findViewById(R.id.conversation_item_body);
                Locale locale = DynamicLanguage.getSelectedLocale(callbackRef.get().getContext());
                int messageId = (int) feature.getNumberProperty(MESSAGE_ID);
                int contactId = (int) feature.getNumberProperty(CONTACT_ID);

                DcContact contact = DcHelper.getContext(callbackRef.get().getContext()).getContact(contactId);
                TextView contactTextView = bubbleLayout.findViewById(R.id.message_sender);
                contactTextView.setText(contact.getFirstName());

                String msgText;
                if (messageId != 0) {
                    DcContext dcContext =  DcHelper.getContext(callbackRef.get().getContext());
                    DcMsg msg = dcContext.getMsg(messageId);
                    if (hasImgThumbnail(msg)) {
                        ImageView thumbnailView = bubbleLayout.findViewById(R.id.map_bubble_img_thumbnail);
                        thumbnailView.setImageURI(getThumbnailUri(msg));
                        thumbnailView.setVisibility(View.VISIBLE);
                        msgText = msg.getText();
                    } else {
                        msgText = msg.getSummarytext(75);
                    }
                    ConversationItemFooter footer = bubbleLayout.findViewById(R.id.conversation_item_footer);
                    footer.setVisibility(View.VISIBLE);
                    footer.setMessageRecord(msg, locale);
                } else {
                    msgText = "Reported: " + DateUtils.getExtendedRelativeTimeSpanString(callbackRef.get().getContext(), locale, (long) feature.getNumberProperty(TIMESTAMP));
                }

                if (msgText.length() == 0) {
                    conversationItemBody.setVisibility(GONE);
                } else {
                    conversationItemBody.setText(msgText);
                }

                Bitmap bitmap = BitmapUtil.generate(bubbleLayout);

                String id = feature.getStringProperty(INFO_WINDOW_ID);
                imagesMap.put(id, bitmap);

                if (i % 10 == 0) {
                    publishProgress(new HashMap<>(imagesMap));
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
        try {
            callbackRef.get().setInfoWindowResults(imagesMap[0]);
            callbackRef.get().refreshSource(contactId);
            Log.d(TAG, "updating progress for contact: " + contactId);
        } catch (NullPointerException npe) {
            npe.printStackTrace();
            Log.e(TAG, "Callback was GC'ed before task finished.");
        }
    }

    @Override
    protected void onPostExecute(HashMap<String, Bitmap> bitmapHashMap) {
        try {
            if (bitmapHashMap.size() > 0) {
                callbackRef.get().setInfoWindowResults(bitmapHashMap);
                callbackRef.get().refreshSource(contactId);
            }
        } catch (NullPointerException npe) {
            npe.printStackTrace();
            Log.e(TAG, "Callback was GC'ed before task finished.");
        }
    }

    private boolean hasImgThumbnail(DcMsg dcMsg) {
        int type = dcMsg.getType();
        return type == DcMsg.DC_MSG_IMAGE && dcMsg.hasFile();
    }

    public Uri getThumbnailUri(DcMsg dcMsg) {
        return Uri.fromFile(new File(dcMsg.getFile()));
    }
}