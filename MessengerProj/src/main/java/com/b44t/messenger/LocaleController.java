/*******************************************************************************
 *
 *                              Delta Chat Android
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

import android.content.res.Configuration;
import android.os.Build;
import android.text.format.DateFormat;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class LocaleController {

    public static boolean isRTL = false;

    private SimpleDateFormat getFormatter(int resId)
    {
        // do not cache the instances as SimpleDateFormat is not thread safe
        try {
            return new SimpleDateFormat(ApplicationLoader.applicationContext.getString(resId), Locale.getDefault());
        }
        catch (Exception e) {
            // a fallback if the translated resource contains bad data
            return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US);
        }
    }

    private SimpleDateFormat getFormatterChatDate()
    {
        return getFormatter(R.string.chatDate);
    }

    private SimpleDateFormat getFormatterChatFullDate()
    {
        return getFormatter(R.string.chatFullDate);
    }

    private SimpleDateFormat getFormatterWeek()
    {
        return getFormatter(R.string.formatterWeek);
    }

    private SimpleDateFormat getFormatterMonth()
    {
        return getFormatter(R.string.formatterMonth);
    }

    public SimpleDateFormat getFormatterYear()
    {
        return getFormatter(R.string.formatterYear);
    }

    public SimpleDateFormat getFormatterDay()
    {
        boolean is24HourFormat = DateFormat.is24HourFormat(ApplicationLoader.applicationContext);
        return getFormatter(is24HourFormat? R.string.formatterDay24H : R.string.formatterDay12H);
    }

    private static volatile LocaleController Instance = null;
    public static LocaleController getInstance() {
        LocaleController localInstance = Instance;
        if (localInstance == null) {
            synchronized (LocaleController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new LocaleController();
                }
            }
        }
        return localInstance;
    }

    public LocaleController() {
        checkRTL();
    }

    public void rebuildUiParts()
    {
        // While android makes sure, the activities are re-translated on locale changes, this is
        // not done for the permanent notification "Connected to foo@bar - Waiting for messages ..."
        // To re-translate this, it is needed to update the notification (which is a good idea as the
        // notification is really permanent and would stuck in the wrong language for weeks otherwise ...)
        KeepAliveService kas = KeepAliveService.getInstance();
        if( kas != null ) {
            kas.updateForegroundNotification();
        }
    }

    public void onDeviceConfigurationChange(Configuration newConfig) {
        // rebuild some UI parts after a language change:
        // - the permanent notification (Waiting for messages ...) - notfifications are not re-translated after a locale change. This cannot be done without listening to the language-change-envent.
        // - some formatters and other cached values, maybe this can be done in other ways, however, it's just working.
        Locale newLocale = Locale.getDefault();
        if( newLocale!=null ) { // onDeviceConfigurationChange() is also called on screen orientation changes; do not rebuild the locale stuff in these cases
            checkRTL();
            rebuildUiParts(); // this is really needed, see comment in rebuildUiParts()
        }
    }

    public static String formatDateChat(long date) {
        try {
            Calendar rightNow = Calendar.getInstance();
            int year = rightNow.get(Calendar.YEAR);

            rightNow.setTimeInMillis(date * 1000);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (year == dateYear) {
                return getInstance().getFormatterChatDate().format(date * 1000);
            }
            return getInstance().getFormatterChatFullDate().format(date * 1000);
        } catch (Exception e) {

        }
        return "LOC_ERR: formatDateChat";
    }

    private void checkRTL() {
        isRTL = false;
        if (Build.VERSION.SDK_INT >= 17) {
            // ViewCompat.getLayoutDirection() can also be used, however, this function always returns LTR for SDK < 17, too, but requires a additinal view handle
            Configuration config = ApplicationLoader.applicationContext.getResources().getConfiguration();
            if(config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                isRTL = true;
            }
        }
    }

    public static String dateForChatlist(long date) {
        try {
            Calendar rightNow = Calendar.getInstance();
            int day = rightNow.get(Calendar.DAY_OF_YEAR);
            int year = rightNow.get(Calendar.YEAR);
            rightNow.setTimeInMillis(date * 1000);
            int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (year != dateYear) {
                return getInstance().getFormatterYear().format(new Date(date * 1000));
            } else {
                int dayDiff = dateDay - day;
                if(dayDiff == 0 || dayDiff == -1 && (int)(System.currentTimeMillis() / 1000) - date < 60 * 60 * 8) {
                    return getInstance().getFormatterDay().format(new Date(date * 1000));
                } else if(dayDiff > -7 && dayDiff <= -1) {
                    return getInstance().getFormatterWeek().format(new Date(date * 1000));
                } else {
                    return getInstance().getFormatterMonth().format(new Date(date * 1000));
                }
            }
        } catch (Exception e) {

        }
        return "LOC_ERR";
    }
}
