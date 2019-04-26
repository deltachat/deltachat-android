package org.thoughtcrime.securesms.map;

import android.content.Context;
import android.os.AsyncTask;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.connect.DcHelper;

public class SendingTask extends AsyncTask<SendingTask.Model, SendingTask.Model, SendingTask.Model> {

    public interface OnMessageSentListener {
        void onMessageSent();
    }

    public static class Model {
        private final DcMsg msg;
        private final int chatId;
        private final OnMessageSentListener callback;

        public Model(DcMsg msg,
                     int chatId,
                     OnMessageSentListener callback) {
            this.msg = msg;
            this.chatId = chatId;
            this.callback = callback;
        }
    }

    private final DcContext dcContext;

    public SendingTask(Context context) {
        this.dcContext = DcHelper.getContext(context);
    }

    @Override
    protected Model doInBackground(Model... param) {
        Model m = param[0];
        if(m.msg!=null) {
            dcContext.sendMsg(m.chatId, m.msg);
        }
        return m;
    }

    @Override
    protected void onPostExecute(Model result) {
        if (result.callback != null) {
            result.callback.onMessageSent();
        }
    }
}