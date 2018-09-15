package org.thoughtcrime.securesms.connect;


import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import org.thoughtcrime.securesms.util.AsyncLoader;

public class DcContactsLoader extends AsyncLoader<int[]> {

    private static final String TAG = DcContactsLoader.class.getName();

    private final int    listflags;
    private final String query;

    public DcContactsLoader(Context context, int listflags, String query) {
        super(context);
        this.listflags = listflags;
        this.query     = query;
    }

    @Override
    public @NonNull
    int[] loadInBackground() {
        try {
            return DcHelper.getContext(getContext()).getContacts(listflags, query);

        } catch (Exception e) {
            Log.w(TAG, e);
        }

        return new int[0];
    }
}
