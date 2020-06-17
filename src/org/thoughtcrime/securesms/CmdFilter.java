package org.thoughtcrime.securesms;

import android.text.util.Linkify;
import java.util.regex.Matcher;


public class CmdFilter implements Linkify.TransformFilter {
    public String transformUrl(Matcher match, String url) {
	return url.trim();
    }
}
