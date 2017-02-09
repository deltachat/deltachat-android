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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.text.format.DateFormat;
import android.util.Xml;

import com.b44t.messenger.time.FastDateFormat;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class LocaleController {

    public static boolean isRTL = false;
    public static int nameDisplayOrder = 1;
    private static boolean is24HourFormat = false;
    public FastDateFormat formatterDay;
    public FastDateFormat formatterWeek;
    public FastDateFormat formatterMonth;
    public FastDateFormat formatterYear;
    public FastDateFormat formatterMonthYear;
    public FastDateFormat chatDate;
    public FastDateFormat chatFullDate;

    private Locale currentLocale;
    private LocaleInfo currentLocaleInfo;
    private LocaleInfo defaultLocalInfo;
    private HashMap<String, String> localeValues = new HashMap<>();
    private String languageOverride;
    private boolean changingConfiguration = false;

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

    public static class LocaleInfo {
        public String name;
        public String nameEnglish;
        public String shortName;
        public String pathToFile;

        public String getSaveString() {
            return name + "|" + nameEnglish + "|" + shortName + "|" + pathToFile;
        }

        public static LocaleInfo createWithString(String string) {
            if (string == null || string.length() == 0) {
                return null;
            }
            String[] args = string.split("\\|");
            if (args.length != 4) {
                return null;
            }
            LocaleInfo localeInfo = new LocaleInfo();
            localeInfo.name = args[0];
            localeInfo.nameEnglish = args[1];
            localeInfo.shortName = args[2];
            localeInfo.pathToFile = args[3];
            return localeInfo;
        }
    }

    public ArrayList<LocaleInfo> sortedLanguages = new ArrayList<>();
    public HashMap<String, LocaleInfo> languagesDict = new HashMap<>();

    private ArrayList<LocaleInfo> otherLanguages = new ArrayList<>();

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
        LocaleInfo localeInfo = new LocaleInfo();
        localeInfo.name = "English";
        localeInfo.nameEnglish = "English";
        localeInfo.shortName = "en";
        localeInfo.pathToFile = null;
        sortedLanguages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleInfo();
        localeInfo.name = "Italiano";
        localeInfo.nameEnglish = "Italian";
        localeInfo.shortName = "it";
        localeInfo.pathToFile = null;
        sortedLanguages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleInfo();
        localeInfo.name = "Español";
        localeInfo.nameEnglish = "Spanish";
        localeInfo.shortName = "es";
        sortedLanguages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleInfo();
        localeInfo.name = "Deutsch";
        localeInfo.nameEnglish = "German";
        localeInfo.shortName = "de";
        localeInfo.pathToFile = null;
        sortedLanguages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleInfo();
        localeInfo.name = "Français";
        localeInfo.nameEnglish = "French";
        localeInfo.shortName = "fr";
        localeInfo.pathToFile = null;
        sortedLanguages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleInfo();
        localeInfo.name = "Nederlands";
        localeInfo.nameEnglish = "Dutch";
        localeInfo.shortName = "nl";
        localeInfo.pathToFile = null;
        sortedLanguages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleInfo();
        localeInfo.name = "Polski";
        localeInfo.nameEnglish = "Polish";
        localeInfo.shortName = "pl";
        localeInfo.pathToFile = null;
        sortedLanguages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleInfo();
        localeInfo.name = "Português";
        localeInfo.nameEnglish = "Portuguese";
        localeInfo.shortName = "pt";
        localeInfo.pathToFile = null;
        sortedLanguages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        loadOtherLanguages();

        for (LocaleInfo locale : otherLanguages) {
            sortedLanguages.add(locale);
            languagesDict.put(locale.shortName, locale);
        }

        Collections.sort(sortedLanguages, new Comparator<LocaleInfo>() {
            @Override
            public int compare(LocaleController.LocaleInfo o, LocaleController.LocaleInfo o2) {
                return o.name.compareTo(o2.name);
            }
        });

        defaultLocalInfo = localeInfo = new LocaleController.LocaleInfo();
        localeInfo.name = "System default";
        localeInfo.nameEnglish = "System default";
        localeInfo.shortName = null;
        localeInfo.pathToFile = null;
        sortedLanguages.add(0, localeInfo);

        is24HourFormat = DateFormat.is24HourFormat(ApplicationLoader.applicationContext);
        LocaleInfo currentInfo = null;
        boolean override = false;

        try {
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
            String lang = preferences.getString("language", null);
            if (lang != null) {
                currentInfo = languagesDict.get(lang);
                if (currentInfo != null) {
                    override = true;
                }
            }

            if (currentInfo == null && Locale.getDefault().getLanguage() != null) {
                currentInfo = languagesDict.get(Locale.getDefault().getLanguage());
            }
            if (currentInfo == null) {
                currentInfo = languagesDict.get(getLocaleString(Locale.getDefault()));
            }
            if (currentInfo == null) {
                currentInfo = languagesDict.get("en");
            }
            applyLanguage(currentInfo, override);
        } catch (Exception e) {
            FileLog.e("messenger", e);
        }

        try {
            IntentFilter timezoneFilter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            ApplicationLoader.applicationContext.registerReceiver(new TimeZoneChangedReceiver(), timezoneFilter);
        } catch (Exception e) {
            FileLog.e("messenger", e);
        }
    }

    private String getLocaleString(Locale locale) {
        if (locale == null) {
            return "en";
        }
        String languageCode = locale.getLanguage();
        String countryCode = locale.getCountry();
        String variantCode = locale.getVariant();
        if (languageCode.length() == 0 && countryCode.length() == 0) {
            return "en";
        }
        StringBuilder result = new StringBuilder(11);
        result.append(languageCode);
        if (countryCode.length() > 0 || variantCode.length() > 0) {
            result.append('_');
        }
        result.append(countryCode);
        if (variantCode.length() > 0) {
            result.append('_');
        }
        result.append(variantCode);
        return result.toString();
    }

    public static String getLocaleStringIso639() {
        Locale locale = Locale.getDefault();
        if (locale == null) {
            return "en";
        }
        String languageCode = locale.getLanguage();
        String countryCode = locale.getCountry();
        String variantCode = locale.getVariant();
        if (languageCode.length() == 0 && countryCode.length() == 0) {
            return "en";
        }
        StringBuilder result = new StringBuilder(11);
        result.append(languageCode);
        if (countryCode.length() > 0 || variantCode.length() > 0) {
            result.append('-');
        }
        result.append(countryCode);
        if (variantCode.length() > 0) {
            result.append('_');
        }
        result.append(variantCode);
        return result.toString();
    }

    private void saveOtherLanguages() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("langconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        String locales = "";
        for (LocaleInfo localeInfo : otherLanguages) {
            String loc = localeInfo.getSaveString();
            if (loc != null) {
                if (locales.length() != 0) {
                    locales += "&";
                }
                locales += loc;
            }
        }
        editor.putString("locales", locales);
        editor.commit();
    }

    public boolean deleteLanguage(LocaleInfo localeInfo) {
        if (localeInfo.pathToFile == null) {
            return false;
        }
        if (currentLocaleInfo == localeInfo) {
            applyLanguage(defaultLocalInfo, true);
        }

        otherLanguages.remove(localeInfo);
        sortedLanguages.remove(localeInfo);
        languagesDict.remove(localeInfo.shortName);
        File file = new File(localeInfo.pathToFile);
        file.delete();
        saveOtherLanguages();
        return true;
    }

    private void loadOtherLanguages() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("langconfig", Activity.MODE_PRIVATE);
        String locales = preferences.getString("locales", null);
        if (locales == null || locales.length() == 0) {
            return;
        }
        String[] localesArr = locales.split("&");
        for (String locale : localesArr) {
            LocaleInfo localeInfo = LocaleInfo.createWithString(locale);
            if (localeInfo != null) {
                otherLanguages.add(localeInfo);
            }
        }
    }

    private HashMap<String, String> getLocaleFileStrings(File file) {
        FileInputStream stream = null;
        try {
            HashMap<String, String> stringMap = new HashMap<>();
            XmlPullParser parser = Xml.newPullParser();
            stream = new FileInputStream(file);
            parser.setInput(stream, "UTF-8");
            int eventType = parser.getEventType();
            String name = null;
            String value = null;
            String attrName = null;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_TAG) {
                    name = parser.getName();
                    int c = parser.getAttributeCount();
                    if (c > 0) {
                        attrName = parser.getAttributeValue(0);
                    }
                } else if(eventType == XmlPullParser.TEXT) {
                    if (attrName != null) {
                        value = parser.getText();
                        if (value != null) {
                            value = value.trim();
                            value = value.replace("\\n", "\n");
                            value = value.replace("\\", "");
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    value = null;
                    attrName = null;
                    name = null;
                }
                if (name != null && name.equals("string") && value != null && attrName != null && value.length() != 0 && attrName.length() != 0) {
                    stringMap.put(attrName, value);
                    name = null;
                    value = null;
                    attrName = null;
                }
                eventType = parser.next();
            }
            return stringMap;
        } catch (Exception e) {
            FileLog.e("messenger", e);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
        }
        return new HashMap<>();
    }

    public void applyLanguage(LocaleInfo localeInfo, boolean override) {
        applyLanguage(localeInfo, override, false);
    }

    public void applyLanguage(LocaleInfo localeInfo, boolean override, boolean fromFile) {
        if (localeInfo == null) {
            return;
        }
        try {
            Locale newLocale;
            if (localeInfo.shortName != null) {
                String[] args = localeInfo.shortName.split("_");
                if (args.length == 1) {
                    newLocale = new Locale(localeInfo.shortName);
                } else {
                    newLocale = new Locale(args[0], args[1]);
                }
                if (newLocale != null) {
                    if (override) {
                        languageOverride = localeInfo.shortName;

                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("language", localeInfo.shortName);
                        editor.commit();
                    }
                }
            } else {
                newLocale = Locale.getDefault();
                languageOverride = null;
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.remove("language");
                editor.commit();

                if (newLocale != null) {
                    LocaleInfo info = null;
                    if (newLocale.getLanguage() != null) {
                        info = languagesDict.get(newLocale.getLanguage());
                    }
                    if (info == null) {
                        info = languagesDict.get(getLocaleString(newLocale));
                    }
                    if (info == null) {
                        newLocale = Locale.US;
                    }
                }
            }
            if (newLocale != null) {
                if (localeInfo.pathToFile == null) {
                    localeValues.clear();
                } else if (!fromFile) {
                    localeValues = getLocaleFileStrings(new File(localeInfo.pathToFile));
                }
                currentLocale = newLocale;
                currentLocaleInfo = localeInfo;
                changingConfiguration = true;
                Locale.setDefault(currentLocale);
                android.content.res.Configuration config = new android.content.res.Configuration();
                config.locale = currentLocale;
                ApplicationLoader.applicationContext.getResources().updateConfiguration(config, ApplicationLoader.applicationContext.getResources().getDisplayMetrics());
                changingConfiguration = false;
            }
        } catch (Exception e) {
            FileLog.e("messenger", e);
            changingConfiguration = false;
        }
        recreateFormatters();
    }

    public static String getCurrentLanguageName() {
        return getString("LanguageName", R.string.LanguageName);
    }

    private String getStringInternal(String key, int res) {
        String value = localeValues.get(key);
        if (value == null) {
            try {
                value = ApplicationLoader.applicationContext.getString(res);
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
        }
        if (value == null) {
            value = "LOC_ERR:" + key;
        }
        return value;
    }

    public static String getString(String key, int res) {
        return getInstance().getStringInternal(key, res);
    }

    public static String formatString(String key, int res, Object... args) {
        try {
            String value = getInstance().localeValues.get(key);
            if (value == null) {
                value = ApplicationLoader.applicationContext.getString(res);
            }

            if (getInstance().currentLocale != null) {
                return String.format(getInstance().currentLocale, value, args);
            } else {
                return String.format(value, args);
            }
        } catch (Exception e) {
            FileLog.e("messenger", e);
            return "LOC_ERR: " + key;
        }
    }

    public static String formatStringSimple(String string, Object... args) {
        try {
            if (getInstance().currentLocale != null) {
                return String.format(getInstance().currentLocale, string, args);
            } else {
                return String.format(string, args);
            }
        } catch (Exception e) {
            FileLog.e("messenger", e);
            return "LOC_ERR: " + string;
        }
    }

    public void onDeviceConfigurationChange(Configuration newConfig) {
        if (changingConfiguration) {
            return;
        }
        is24HourFormat = DateFormat.is24HourFormat(ApplicationLoader.applicationContext);
        if (languageOverride != null) {
            LocaleInfo toSet = currentLocaleInfo;
            currentLocaleInfo = null;
            applyLanguage(toSet, false);
        } else {
            Locale newLocale = newConfig.locale;
            if (newLocale != null) {
                String d1 = newLocale.getDisplayName();
                String d2 = currentLocale.getDisplayName();
                if (d1 != null && d2 != null && !d1.equals(d2)) {
                    recreateFormatters();
                }
                currentLocale = newLocale;
            }
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
            FileLog.e("messenger", e);
        }
        return "LOC_ERR: formatDateChat";
    }

    public static String formatDateAudio(long date) {
        try {
            Calendar rightNow = Calendar.getInstance();
            int day = rightNow.get(Calendar.DAY_OF_YEAR);
            int year = rightNow.get(Calendar.YEAR);
            rightNow.setTimeInMillis(date * 1000);
            int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (dateDay == day && year == dateYear) {
                return String.format("%s %s", LocaleController.getString("TodayAt", R.string.TodayAt), getInstance().formatterDay.format(new Date(date * 1000)));
            } else if (dateDay + 1 == day && year == dateYear) {
                return String.format("%s %s", LocaleController.getString("YesterdayAt", R.string.YesterdayAt), getInstance().formatterDay.format(new Date(date * 1000)));
            } else if (year == dateYear) {
                return LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime, getInstance().formatterMonth.format(new Date(date * 1000)), getInstance().formatterDay.format(new Date(date * 1000)));
            } else {
                return LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime, getInstance().formatterYear.format(new Date(date * 1000)), getInstance().formatterDay.format(new Date(date * 1000)));
            }
        } catch (Exception e) {
            FileLog.e("messenger", e);
        }
        return "LOC_ERR";
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

    public void recreateFormatters() {
        Locale locale = currentLocale;
        if (locale == null) {
            locale = Locale.getDefault();
        }
        String lang = locale.getLanguage();
        if (lang == null) {
            lang = "en";
        }
        isRTL = lang.toLowerCase().equals("ar");
        nameDisplayOrder = lang.toLowerCase().equals("ko") ? 2 : 1;

        formatterMonth = createFormatter(locale, getStringInternal("formatterMonth", R.string.formatterMonth), "dd MMM");
        formatterYear = createFormatter(locale, getStringInternal("formatterYear", R.string.formatterYear), "dd.MM.yyyy");
        chatDate = createFormatter(locale, getStringInternal("chatDate", R.string.chatDate), "d MMMM");
        chatFullDate = createFormatter(locale, getStringInternal("chatFullDate", R.string.chatFullDate), "d MMMM yyyy");
        formatterWeek = createFormatter(locale, getStringInternal("formatterWeek", R.string.formatterWeek), "EEE");
        formatterMonthYear = createFormatter(locale, getStringInternal("formatterMonthYear", R.string.formatterMonthYear), "MMMM yyyy");
        formatterDay = createFormatter(lang.toLowerCase().equals("ar") || lang.toLowerCase().equals("ko") ? locale : Locale.US, is24HourFormat ? getStringInternal("formatterDay24H", R.string.formatterDay24H) : getStringInternal("formatterDay12H", R.string.formatterDay12H), is24HourFormat ? "HH:mm" : "h:mm a");
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
            FileLog.e("messenger", e);
        }
        return "LOC_ERR";
    }
}
