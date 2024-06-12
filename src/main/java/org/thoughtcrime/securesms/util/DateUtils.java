/*
 * Copyright (C) 2014 Open Whisper Systems
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
package org.thoughtcrime.securesms.util;

import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import android.text.format.DateFormat;

import org.thoughtcrime.securesms.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods to help display dates in a nice, easily readable way.
 */
public class DateUtils extends android.text.format.DateUtils {

  private static boolean isWithin(final long millis, final long span, final TimeUnit unit) {
    return System.currentTimeMillis() - millis <= unit.toMillis(span);
  }

  private static boolean isYesterday(final long when) {
    return DateUtils.isToday(when + TimeUnit.DAYS.toMillis(1));
  }

  private static int convertDelta(final long millis, TimeUnit to) {
    return (int) to.convert(System.currentTimeMillis() - millis, TimeUnit.MILLISECONDS);
  }

  private static String getFormattedDateTime(long time, String template, Locale locale) {
    final String localizedPattern = getLocalizedPattern(template, locale);
    String ret = new SimpleDateFormat(localizedPattern, locale).format(new Date(time));

    // having ".," in very common and known abbreviates as weekdays or month names is not needed,
    // looks ugly and makes the string longer than needed
    ret = ret.replace(".,", ",");

    return ret;
  }

  public static String getBriefRelativeTimeSpanString(final Context c, final Locale locale, final long timestamp) {
    if (isWithin(timestamp, 1, TimeUnit.MINUTES)) {
      return c.getString(R.string.now);
    } else if (isWithin(timestamp, 1, TimeUnit.HOURS)) {
      int mins = convertDelta(timestamp, TimeUnit.MINUTES);
      return c.getResources().getQuantityString(R.plurals.n_minutes, mins, mins);
    } else if (isWithin(timestamp, 1, TimeUnit.DAYS)) {
      int hours = convertDelta(timestamp, TimeUnit.HOURS);
      return c.getResources().getQuantityString(R.plurals.n_hours, hours, hours);
    } else if (isWithin(timestamp, 6, TimeUnit.DAYS)) {
      return getFormattedDateTime(timestamp, "EEE", locale);
    } else if (isWithin(timestamp, 365, TimeUnit.DAYS)) {
      return getFormattedDateTime(timestamp, "MMM d", locale);
    } else {
      return getFormattedDateTime(timestamp, "MMM d, yyyy", locale);
    }
  }

  public static String getExtendedTimeSpanString(final Context c, final Locale locale, final long timestamp) {
    StringBuilder format = new StringBuilder();
    if      (DateUtils.isToday(timestamp))                 {}
    else if (isWithin(timestamp,   6, TimeUnit.DAYS)) format.append("EEE ");
    else if (isWithin(timestamp, 365, TimeUnit.DAYS)) format.append("MMM d, ");
    else                                              format.append("MMM d, yyyy, ");

    if (DateFormat.is24HourFormat(c)) format.append("HH:mm");
    else                              format.append("hh:mm a");

    return getFormattedDateTime(timestamp, format.toString(), locale);
  }

  public static String getExtendedRelativeTimeSpanString(final Context c, final Locale locale, final long timestamp) {
    if (isWithin(timestamp, 1, TimeUnit.MINUTES)) {
      return c.getString(R.string.now);
    } else if (isWithin(timestamp, 1, TimeUnit.HOURS)) {
      int mins = (int)TimeUnit.MINUTES.convert(System.currentTimeMillis() - timestamp, TimeUnit.MILLISECONDS);
      return c.getResources().getQuantityString(R.plurals.n_minutes, mins, mins);
    } else {
      return getExtendedTimeSpanString(c, locale, timestamp);
    }
  }

  public static String getRelativeDate(@NonNull Context context,
                                       @NonNull Locale locale,
                                       long timestamp)
  {
    if (isToday(timestamp)) {
      return context.getString(R.string.today);
    } else if (isYesterday(timestamp)) {
      return context.getString(R.string.yesterday);
    } else {
      return getFormattedDateTime(timestamp, "EEEE, MMMM d, yyyy", locale);
    }
  }

  private static String getLocalizedPattern(String template, Locale locale) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      return DateFormat.getBestDateTimePattern(locale, template);
    } else {
      return new SimpleDateFormat(template, locale).toLocalizedPattern();
    }
  }

  public static String getFormatedDuration(long millis) {
    return String.format("%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(millis),
            TimeUnit.MILLISECONDS.toSeconds(millis-(TimeUnit.MILLISECONDS.toMinutes(millis)*60000)));
  }

  public static String getFormattedTimespan(Context c, int timestamp) {
    int mins = timestamp / (1000 * 60);
    if (mins / 60 == 0) {
      return c.getResources().getQuantityString(R.plurals.n_minutes, mins, mins);
    }
    int hours = mins / 60;
    return c.getResources().getQuantityString(R.plurals.n_hours, hours, hours);
  }
}
