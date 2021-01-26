package org.thoughtcrime.securesms.map;

/**
 * Created by cyberta on 13.03.19.
 */

import android.content.Context;
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
import org.thoughtcrime.securesms.components.emoji.EmojiTextView;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.BitmapUtil;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.DynamicLanguage;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Locale;

import static android.view.View.GONE;
import static org.thoughtcrime.securesms.map.MapDataManager.CONTACT_ID;
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
class GenerateInfoWindowTask extends AsyncTask<Feature, Bitmap, Bitmap> {

    public interface GenerateInfoWindowCallback {
        Context getContext();

        /**
         * Invoked when the bitmaps have been generated from a view.
         */
        void setInfoWindowResults(Bitmap result);
    }

    private static final String TAG = GenerateInfoWindowCallback.class.getName();
    private final WeakReference<GenerateInfoWindowCallback> callbackRef;
    private final static HashSet<GenerateInfoWindowTask> instances = new HashSet<>();

    GenerateInfoWindowTask(GenerateInfoWindowCallback callback) {
        this.callbackRef = new WeakReference<>(callback);
        instances.add(this);
    }

    public static void cancelRunningTasks() {
        for (GenerateInfoWindowTask task : instances) {
            task.cancel(true);
        }
    }

    @SuppressWarnings("WrongThread")
    @Override
    protected Bitmap doInBackground(Feature... params) {
        Log.d(TAG, "GenerateInfoWindowTask start");
        Thread.currentThread().setName(GenerateInfoWindowTask.class.getName());
        Bitmap bitmap = null;

        try {
            LayoutInflater inflater = LayoutInflater.from(callbackRef.get().getContext());

            Feature feature = params[0];
            Log.d(TAG, "GenerateInfoWindowTask: feature " + feature.id());

            LinearLayout bubbleLayout = (LinearLayout)
                    inflater.inflate(R.layout.map_bubble_layout, null);
            bubbleLayout.setBackgroundResource(R.drawable.message_bubble_background_received_alone);
            EmojiTextView conversationItemBody = bubbleLayout.findViewById(R.id.conversation_item_body);
            Locale locale = DynamicLanguage.getSelectedLocale(callbackRef.get().getContext());
            int messageId = (int) feature.getNumberProperty(MESSAGE_ID);
            int contactId = (int) feature.getNumberProperty(CONTACT_ID);

            DcContact contact = DcHelper.getContext(callbackRef.get().getContext()).getContact(contactId);
            TextView contactTextView = bubbleLayout.findViewById(R.id.message_sender);
            contactTextView.setText(contact.getDisplayName());

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

            bitmap = BitmapUtil.generate(bubbleLayout);
        } catch (NullPointerException npe) {
            npe.printStackTrace();
            Log.e(TAG, "Callback was GC'ed before task finished.");
        }

        Log.d(TAG, "GenerateInfoWindowTask finished");
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (!isCancelled() && bitmap != null) {
            try {
                callbackRef.get().setInfoWindowResults(bitmap);
            } catch (NullPointerException npe) {
                npe.printStackTrace();
                Log.e(TAG, "Callback was GC'ed before task finished.");
            }
        }
        instances.remove(this);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        instances.remove(this);
    }

    private boolean hasImgThumbnail(DcMsg dcMsg) {
        int type = dcMsg.getType();
        return type == DcMsg.DC_MSG_IMAGE && dcMsg.hasFile();
    }

    public Uri getThumbnailUri(DcMsg dcMsg) {
        return Uri.fromFile(new File(dcMsg.getFile()));
    }
}