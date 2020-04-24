package org.thoughtcrime.securesms.connect;

import android.content.Context;

import androidx.annotation.NonNull;

import android.util.Log;

import com.b44t.messenger.DcChatlist;

import org.thoughtcrime.securesms.util.AsyncLoader;

public class DcChatlistLoader extends AsyncLoader<DcChatlist> {

    private static final String TAG = DcChatlistLoader.class.getName();

    private final int listflags;
    private final String query;
    private final int queryId;

    public DcChatlistLoader(Context context, int listflags, String query, int queryId) {
        super(context);
        this.listflags = listflags;
        this.query = query;
        this.queryId = queryId;
    }

    @Override
    public @NonNull
    DcChatlist loadInBackground() {
        try {
            return DcHelper.getContext(getContext()).getChatlist(listflags, query, queryId);

        } catch (Exception e) {
            Log.w(TAG, e);
        }

        return new DcChatlist(0);
    }
}
