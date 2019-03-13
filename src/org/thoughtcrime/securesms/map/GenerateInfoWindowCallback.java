package org.thoughtcrime.securesms.map;

import android.content.Context;
import android.graphics.Bitmap;

import java.util.HashMap;

/**
 * Created by cyberta on 13.03.19.
 */

public interface GenerateInfoWindowCallback {
        Context getContext();

        /**
         * Invoked when the marker resources of contact with contactId should be updated
         * @param contactId
         */
        void refreshSource(int contactId);
        /**
         * Invoked when the bitmaps have been generated from a view.
         */
        void setInfoWindowResults(HashMap<String, Bitmap> results);
}
