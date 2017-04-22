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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.text.format.DateFormat;

import com.b44t.messenger.time.FastDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class LocaleController {

    public static boolean isRTL = false;
    public FastDateFormat formatterDay;
    public FastDateFormat formatterWeek;
    public FastDateFormat formatterMonth;
    public FastDateFormat formatterYear;
    public FastDateFormat formatterMonthYear;
    public FastDateFormat chatDate;
    public FastDateFormat chatFullDate;

    private String formattersCreatedFor = "";

    private class TimeZoneChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ApplicationLoader.applicationHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (!formatterMonth.getTimeZone().equals(TimeZone.getDefault())) {
                        LocaleController.getInstance().recreateFormatters();
                    }
                }
            });
        }
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

        recreateFormatters();

        try {
            IntentFilter timezoneFilter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            ApplicationLoader.applicationContext.registerReceiver(new TimeZoneChangedReceiver(), timezoneFilter);
        } catch (Exception e) {

        }
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
        if( newLocale!=null && !newLocale.getDisplayName().equals(formattersCreatedFor) ) { // onDeviceConfigurationChange() is also called on screen orientation changes; do not rebuild the locale stuff in these cases
            recreateFormatters();
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
                return getInstance().chatDate.format(date * 1000);
            }
            return getInstance().chatFullDate.format(date * 1000);
        } catch (Exception e) {

        }
        return "LOC_ERR: formatDateChat";
    }

    private FastDateFormat createFormatter(Locale locale, String format, String defaultFormat) {
        if (format == null || format.length() == 0) {
            format = defaultFormat;
        }
        FastDateFormat formatter;
        try {
            formatter = FastDateFormat.getInstance(format, locale);
        } catch (Exception e) {
            format = defaultFormat;
            formatter = FastDateFormat.getInstance(format, locale);
        }
        return formatter;
    }

    private void recreateFormatters() {
        Locale locale = Locale.getDefault();
        String lang = locale.getLanguage();
        if (lang == null) {
            lang = "en";
        }
        isRTL = lang.toLowerCase().equals("ar");
        boolean is24HourFormat = DateFormat.is24HourFormat(ApplicationLoader.applicationContext);

        formatterMonth = createFormatter(locale, ApplicationLoader.applicationContext.getString(R.string.formatterMonth), "dd MMM");
        formatterYear = createFormatter(locale, ApplicationLoader.applicationContext.getString(R.string.formatterYear), "dd.MM.yyyy");
        chatDate = createFormatter(locale, ApplicationLoader.applicationContext.getString(R.string.chatDate), "d MMMM");
        chatFullDate = createFormatter(locale, ApplicationLoader.applicationContext.getString(R.string.chatFullDate), "d MMMM yyyy");
        formatterWeek = createFormatter(locale, ApplicationLoader.applicationContext.getString(R.string.formatterWeek), "EEE");
        formatterMonthYear = createFormatter(locale, ApplicationLoader.applicationContext.getString(R.string.formatterMonthYear), "MMMM yyyy");
        formatterDay = createFormatter(lang.toLowerCase().equals("ar") || lang.toLowerCase().equals("ko") ? locale : Locale.US, is24HourFormat ? ApplicationLoader.applicationContext.getString(R.string.formatterDay24H) : ApplicationLoader.applicationContext.getString(R.string.formatterDay12H), is24HourFormat ? "HH:mm" : "h:mm a");

        formattersCreatedFor = locale.getDisplayName();
    }

    public static String stringForMessageListDate(long date) {
        try {
            Calendar rightNow = Calendar.getInstance();
            int day = rightNow.get(Calendar.DAY_OF_YEAR);
            int year = rightNow.get(Calendar.YEAR);
            rightNow.setTimeInMillis(date * 1000);
            int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (year != dateYear) {
                return getInstance().formatterYear.format(new Date(date * 1000));
            } else {
                int dayDiff = dateDay - day;
                if(dayDiff == 0 || dayDiff == -1 && (int)(System.currentTimeMillis() / 1000) - date < 60 * 60 * 8) {
                    return getInstance().formatterDay.format(new Date(date * 1000));
                } else if(dayDiff > -7 && dayDiff <= -1) {
                    return getInstance().formatterWeek.format(new Date(date * 1000));
                } else {
                    return getInstance().formatterMonth.format(new Date(date * 1000));
                }
            }
        } catch (Exception e) {

        }
        return "LOC_ERR";
    }
}
