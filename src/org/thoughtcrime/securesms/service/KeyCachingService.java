/*
 * Copyright (C) 2011 Whisper Systems
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
package org.thoughtcrime.securesms.service;

import android.app.Service;
import android.content.Context;
import android.support.annotation.Nullable;
import org.thoughtcrime.securesms.crypto.MasterSecret;

/**
 * Small service that stays running to keep a key cached in memory.
 *
 * @author Moxie Marlinspike
 */

public class KeyCachingService {

  private static final String TAG = KeyCachingService.class.getSimpleName();

  public static final int SERVICE_RUNNING_ID = 4141;

  public KeyCachingService() {}

  public static synchronized boolean isLocked(Context context) {
    return getMasterSecret(context) == null;
  }

  public static synchronized @Nullable MasterSecret getMasterSecret(Context context) {
    return null;
  }

  public static void registerPassphraseActivityStarted(Context activity) {
  }

  public static void registerPassphraseActivityStopped(Context activity) {
  }
}
