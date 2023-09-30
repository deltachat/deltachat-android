package org.thoughtcrime.securesms.map;

import android.content.Context;
import androidx.appcompat.widget.LinearLayoutCompat;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;

/**
 * Created by cyberta on 24.04.19.
 */

public class AddPoiView extends LinearLayoutCompat {
    private final ImageButton sendView;
    private final EditText messageView;
    private final ProgressBar progressBar;
    private LatLng latLng;
    private SendingTask.OnMessageSentListener listener;
    private int chatId;

    public AddPoiView(Context context) {
        this(context, null);
    }

    public AddPoiView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AddPoiView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflate(context, R.layout.add_poi_view, this);
        sendView = this.findViewById(R.id.sendView);
        messageView = this.findViewById(R.id.message_view);
        progressBar = this.findViewById(R.id.sending_progress);

        messageView.requestFocus();
        sendView.setOnClickListener(v -> {
            if (messageView.getText().toString().length() == 0) {
                return;
            }

            progressBar.setVisibility(VISIBLE);
            sendView.setVisibility(INVISIBLE);
            DcContext dcContext = DcHelper.getContext(AddPoiView.this.getContext());
            DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
            msg.setLocation((float) latLng.getLatitude(), (float) latLng.getLongitude());
            msg.setText(messageView.getText().toString());

            SendingTask.Model model = new SendingTask.Model(msg, chatId, listener);
            new SendingTask(AddPoiView.this.getContext()).execute(model);
        });
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    public void setChatId(int chatId) {
        this.chatId = chatId;
    }

    public void setOnMessageSentListener(SendingTask.OnMessageSentListener listener) {
        this.listener = listener;
    }

    public EditText getMessageView() {
        return messageView;
    }
}
