package org.telegram.localization;

import java.util.Locale;

/**
 * TGDELTA: Stub replacing Telegram's generated localization helper.
 * Delta Chat does its own string localization, so this always reports "no
 * extra localization asset" and callers fall back to the default resources.
 */
public class LocalizationUtils {
    public static final String DEFAULT_LOCALIZATION = "";

    public static String getLocalizationAsset(Locale locale) {
        return DEFAULT_LOCALIZATION;
    }
}
