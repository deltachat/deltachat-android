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

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.system.Os;
import android.system.StructStat;

import java.io.File;
import java.util.HashMap;

public class ClearCacheService extends IntentService {

    public ClearCacheService() {
        super("ClearCacheService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ApplicationLoader.postInitApplication();

        /*
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        final int keepMedia = preferences.getInt("keep_media", 2);
        if (keepMedia == 2) {
            return;
        }
        Utilities.globalQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                long diff = 60 * 60 * 1000 * 24 * (keepMedia == 0 ? 7 : 30);
                final HashMap<Integer, File> paths = ImageLoader.getInstance().createMediaPaths();
                for (HashMap.Entry<Integer, File> entry : paths.entrySet()) {
                    if (entry.getKey() == FileLoader.MEDIA_DIR_CACHE) {
                        continue;
                    }
                    try {
                        File[] array = entry.getValue().listFiles();
                        if (array != null) {
                            for (int b = 0; b < array.length; b++) {
                                File f = array[b];
                                if (f.isFile()) {
                                    if (f.getName().equals(".nomedia")) {
                                        continue;
                                    }
                                    if (Build.VERSION.SDK_INT >= 21) {
                                        try {
                                            StructStat stat = Os.stat(f.getPath());
                                            if (stat.st_atime != 0) {
                                                if (stat.st_atime + diff < currentTime) {
                                                    f.delete();
                                                }
                                            } else if (stat.st_mtime + diff < currentTime) {
                                                f.delete();
                                            }
                                        } catch (Exception e) {
                                            FileLog.e("messenger", e);
                                        }
                                    } else if (f.lastModified() + diff < currentTime) {
                                        f.delete();
                                    }
                                }
                            }
                        }
                    } catch (Throwable e) {
                        FileLog.e("messenger", e);
                    }
                }
            }
        });
        */
    }
}
