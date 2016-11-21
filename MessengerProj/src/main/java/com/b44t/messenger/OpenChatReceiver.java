/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.b44t.messenger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.b44t.ui.LaunchActivity;

public class OpenChatReceiver extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null) {
            finish();
        }
        if (intent.getAction() == null || !intent.getAction().startsWith("com.b44t.messenger.openchat")) {
            finish();
            return;
        }
        Intent intent2 = new Intent(this, LaunchActivity.class);
        intent2.setAction(intent.getAction());
        intent2.putExtras(intent);
        startActivity(intent2);
        finish();
    }
}
