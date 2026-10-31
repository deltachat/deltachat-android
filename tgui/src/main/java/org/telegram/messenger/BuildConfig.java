package org.telegram.messenger;

/**
 * TGDELTA: Stub replacing Telegram's generated BuildConfig.
 *
 * Values are fixed because this module is a library, not a Telegram app build.
 * BUNDLE must be boolean: donor code uses it as `!BuildConfig.BUNDLE`.
 */
public final class BuildConfig {
    public static final boolean DEBUG = false;
    public static final boolean DEBUG_VERSION = false;
    public static final boolean DEBUG_PRIVATE_VERSION = false;
    public static final boolean BUNDLE = false;
    public static final String BUILD_VERSION_STRING = "tgui";

    private BuildConfig() {}
}
