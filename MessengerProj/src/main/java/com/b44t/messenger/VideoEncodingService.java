/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *                        (C) 2013-2016 Nikolai Kudashov
 *                           (C) 2017 Björn Petersen
 *                    Contact: r10s@b44t.com, http://b44t.com
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see http://www.gnu.org/licenses/ .
 *
 ******************************************************************************/


package com.b44t.messenger;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

public class VideoEncodingService extends Service implements NotificationCenter.NotificationCenterDelegate {

    private Context mContext = ApplicationLoader.applicationContext;
    private NotificationCompat.Builder builder = null;
    private String path = null;
    private int currentProgress = 0;

    public VideoEncodingService() {
        super();
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileUploadProgressChanged);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.stopEncodingService);
    }

    public IBinder onBind(Intent arg2) {
        return null;
    }

    public void onDestroy() {
        stopForeground(true);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.FileUploadProgressChanged);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.stopEncodingService);
        FileLog.e("messenger", "destroy video service");
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.FileUploadProgressChanged) {
            String fileName = (String)args[0];
            if (path != null && path.equals(fileName)) {
                Float progress = (Float) args[1];
                Boolean enc = (Boolean) args[2];
                currentProgress = (int)(progress * 100);
                builder.setProgress(100, currentProgress, currentProgress == 0);
                NotificationManagerCompat.from(mContext).notify(4, builder.build());
            }
        } else if (id == NotificationCenter.stopEncodingService) {
            String filepath = (String)args[0];
            if (filepath == null || filepath.equals(path)) {
                stopSelf();
            }
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        path = intent.getStringExtra("path");
        if (path == null) {
            stopSelf();
            return Service.START_NOT_STICKY;
        }
        FileLog.e("messenger", "start video service");
        if (builder == null) {
            builder = new NotificationCompat.Builder(mContext);
            builder.setSmallIcon(android.R.drawable.stat_sys_upload);
            builder.setWhen(System.currentTimeMillis());
            builder.setContentTitle(mContext.getString(R.string.AppName));
            builder.setTicker(mContext.getString(R.string.SendingVideo));
            builder.setContentText(mContext.getString(R.string.SendingVideo));
        }
        currentProgress = 0;
        builder.setProgress(100, currentProgress, currentProgress == 0);
        startForeground(4, builder.build());
        NotificationManagerCompat.from(mContext).notify(4, builder.build());
        return Service.START_NOT_STICKY;
    }
}
