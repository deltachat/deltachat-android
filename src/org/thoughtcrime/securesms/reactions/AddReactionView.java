package org.thoughtcrime.securesms.reactions;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.b44t.messenger.DcMsg;

public class AddReactionView extends LinearLayout {
    public AddReactionView(Context context) {
        super(context);
    }

    public AddReactionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void show(DcMsg msgToReactTo, View parentView) {
        setVisibility(View.VISIBLE);
    }

    public void hide() {
        setVisibility(View.GONE);
    }
}
