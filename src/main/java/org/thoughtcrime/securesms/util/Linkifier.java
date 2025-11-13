package org.thoughtcrime.securesms.util;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;

import java.util.regex.Pattern;

/* Utility for text linkify-ing */
public class Linkifier {
  private static final Pattern CMD_PATTERN = Pattern.compile("(?<=^|\\s)/[a-zA-Z][a-zA-Z@\\d_/.-]{0,254}");
  private static final Pattern PROXY_PATTERN = Pattern.compile("(?<=^|\\s)(SOCKS5|socks5|ss|SS):[^ \\n]+");

  private static void replaceURLSpan(SpannableString messageBody) {
    URLSpan[] urlSpans = messageBody.getSpans(0, messageBody.length(), URLSpan.class);
    for (URLSpan urlSpan : urlSpans) {
      int start = messageBody.getSpanStart(urlSpan);
      int end = messageBody.getSpanEnd(urlSpan);
      // LongClickCopySpan must not be derived from URLSpan, otherwise links will be removed on the next addLinks() call
      messageBody.setSpan(new LongClickCopySpan(urlSpan.getURL()), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

      if (urlSpan.getURL().startsWith("tel:")) {
        int color = 0xFFFF00FF;
        messageBody.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
    }
  }

  public static SpannableString linkify(SpannableString messageBody) {
    // linkify commands such as `/echo` -
    // do this first to avoid `/xkcd_123456` to be treated partly as a phone number
    if (Linkify.addLinks(messageBody, CMD_PATTERN, "cmd:", null, null)) {
      replaceURLSpan(messageBody); // replace URLSpan so that it is not removed on the next addLinks() call
    }

    if (Linkify.addLinks(messageBody, PROXY_PATTERN, null, null, null)) {
      replaceURLSpan(messageBody); // replace URLSpan so that it is not removed on the next addLinks() call
    }

    // linkyfiy urls etc., this removes all existing URLSpan
    if (Linkify.addLinks(messageBody, Linkify.EMAIL_ADDRESSES|Linkify.WEB_URLS|Linkify.PHONE_NUMBERS)) {
      replaceURLSpan(messageBody);
    }

    return messageBody;
  }

}
