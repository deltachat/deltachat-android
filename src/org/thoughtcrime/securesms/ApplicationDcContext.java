/*
 * Copyright (C) 2018 Delta Chat contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.util.Util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class ApplicationDcContext extends DcContext {

    public static final Object lastErrorLock = new Object();
    public static int lastErrorCode = 0;
    public static String lastErrorString = "";
    public static boolean showNextErrorAsToast = true;
    public Context context;

    public ApplicationDcContext(Context context) {
        super("Android");
        this.context = context;
        new ForegroundDetector(ApplicationContext.getInstance(context));
    }

    @Override public long handleEvent(final int event, final long data1, final long data2) {
        switch(event) {
            case DC_EVENT_INFO:
                Log.i("DeltaChat", dataToString(data2));
                break;

            case DC_EVENT_WARNING:
                Log.w("DeltaChat", dataToString(data2));
                break;

            case DC_EVENT_ERROR:
                Log.e("DeltaChat", dataToString(data2));
                synchronized (lastErrorLock) {
                    lastErrorCode = (int)data1;
                    lastErrorString = dataToString(data2);
                }
                Util.runOnMain(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (lastErrorLock) {
                            if (showNextErrorAsToast) {
                                if (ForegroundDetector.getInstance().isForeground()) {
                                    Toast.makeText(context, lastErrorString, Toast.LENGTH_LONG).show();
                                }
                            }
                            showNextErrorAsToast = true;
                        }
                    }
                });
                break;

            case DC_EVENT_HTTP_GET:
                String httpContent = null;
                try {
                    URL url = new URL(dataToString(data1));
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    try {
                        urlConnection.setConnectTimeout(10*1000);
                        InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());

                        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));

                        StringBuilder total = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) {
                            total.append(line).append('\n');
                        }
                        httpContent = total.toString();
                    } finally {
                        urlConnection.disconnect();
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
                return stringToData(httpContent);
        }
        return 0;
    }
}
