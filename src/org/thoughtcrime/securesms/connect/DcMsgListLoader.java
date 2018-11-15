package org.thoughtcrime.securesms.connect;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.b44t.messenger.DcChatlist;

import org.thoughtcrime.securesms.util.AsyncLoader;

public class DcMsgListLoader extends AsyncLoader<int[]> {

    private static final String TAG = DcMsgListLoader.class.getName();

    private final int    chatId;
    private final int    listflags;
    private final int    marker1before;

    public DcMsgListLoader(Context context, int chatId, int listflags, int marker1before) {
        super(context);
        this.chatId        = chatId;
        this.listflags     = listflags;
        this.marker1before = marker1before;
    }

    @Override
    public @NonNull
    int[] loadInBackground() {
        try {
            return DcHelper.getContext(getContext()).getChatMsgs(chatId, listflags, marker1before);

        } catch (Exception e) {
            Log.w(TAG, e);
        }

        return new int[0];
    }
}
