package org.thoughtcrime.securesms.util;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import java.util.regex.Pattern;

/* Utility for text linkify-ing */
public class Linkifier {
  private static final int MAX_DISPLAY_LINK_LENGTH = 32;
  private static final String ELLIPSIS = "…";

  private static final Pattern CMD_PATTERN =
      Pattern.compile("(?<=^|\\s)/[a-zA-Z][a-zA-Z@\\d_/.-]{0,254}");
  private static final Pattern PROXY_PATTERN =
      Pattern.compile("(?<=^|\\s)(SOCKS5|socks5|ss|SS):[^ \\n]+");
  private static final Pattern PHONE_PATTERN =
      Pattern.compile( // sdd = space, dot, or dash
          "(?<=^|\\s|\\.|\\()" // no letter at start
              + "(\\+[0-9]+[\\- \\.]*)?" // +<digits><sdd>*
              + "(\\([0-9]+\\)[\\- \\.]*)?" // (<digits>)<sdd>*
              + "([0-9][0-9\\- \\.]{3,}[0-9])" // <digit><digit|sdd>+<digit> (5 characters min)
              + "(?=$|\\s|\\.|\\))"); // no letter at end

  private static int brokenPhoneLinkifier = -1;

  private static boolean internalPhoneLinkifierNeeded() {
    if (brokenPhoneLinkifier == -1) { // unset
      if (Linkify.addLinks(new SpannableStringBuilder("a100b"), Linkify.PHONE_NUMBERS)) {
        brokenPhoneLinkifier = 1; // true
      } else {
        brokenPhoneLinkifier = 0; // false
      }
    }
    return brokenPhoneLinkifier == 1;
  }

  private static void replaceURLSpan(
      SpannableStringBuilder messageBody, boolean shorten, boolean clickable) {
    URLSpan[] urlSpans = messageBody.getSpans(0, messageBody.length(), URLSpan.class);
    for (int i = urlSpans.length - 1; i >= 0; i--) {
      URLSpan urlSpan = urlSpans[i];
      int start = messageBody.getSpanStart(urlSpan);
      int end = messageBody.getSpanEnd(urlSpan);

      if (shorten && start >= 0 && end > start) {
        String linkText = messageBody.subSequence(start, end).toString();
        String shortenedLinkText = shortenLink(linkText, urlSpan.getURL());

        if (shortenedLinkText.length() != linkText.length()) {
          messageBody.replace(start, end, shortenedLinkText);
          end = start + shortenedLinkText.length();
        }
      }

      messageBody.removeSpan(urlSpan);
      if (clickable) {
        // LongClickCopySpan must not be derived from URLSpan,
        // otherwise links will be removed on the next addLinks() call
        messageBody.setSpan(
            new LongClickCopySpan(urlSpan.getURL()), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
    }
  }

  private static String shortenLink(String text, String url) {
    if (text.length() <= MAX_DISPLAY_LINK_LENGTH) return text;

    // keep the domain prefix and shorten only the path
    int schemeEnd = text.indexOf("://");
    int slashAfterDomain = -1;
    if (schemeEnd > 0) {
      slashAfterDomain = text.indexOf('/', schemeEnd + 3);
    } else if (url.indexOf("://") > 0) {
      slashAfterDomain = text.indexOf('/');
    }
    if (slashAfterDomain >= 0) {
      String domainPart = text.substring(0, slashAfterDomain + 1);
      String rest = text.substring(slashAfterDomain + 1);
      int available = MAX_DISPLAY_LINK_LENGTH - domainPart.length();
      int tailLength = available - ELLIPSIS.length();
      if (tailLength > 0) {
        return domainPart
            + ELLIPSIS
            + rest.substring(rest.length() - Math.min(tailLength, rest.length()));
      }
      return domainPart + ELLIPSIS;
    }

    // domain-only URL or not a web link, do not shorten
    return text;
  }

  public static SpannableStringBuilder linkify(SpannableStringBuilder messageBody) {
    return linkify(messageBody, true);
  }

  public static SpannableStringBuilder linkify(
      SpannableStringBuilder messageBody, boolean clickable) {
    // linkify commands such as `/echo` -
    // do this first to avoid `/xkcd_123456` to be treated partly as a phone number
    if (clickable && Linkify.addLinks(messageBody, CMD_PATTERN, "cmd:", null, null)) {
      // replace URLSpan so that it is not removed on the next addLinks() call
      replaceURLSpan(messageBody, false, clickable);
    }

    if (Linkify.addLinks(messageBody, PROXY_PATTERN, null, null, null)) {
      // replace URLSpan so that it is not removed on the next addLinks() call
      replaceURLSpan(messageBody, true, clickable);
    }

    int flags;
    if (clickable && internalPhoneLinkifierNeeded()) {
      if (Linkify.addLinks(
          messageBody,
          PHONE_PATTERN,
          "tel:",
          Linkify.sPhoneNumberMatchFilter,
          Linkify.sPhoneNumberTransformFilter)) {
        // replace URLSpan so that it is not removed on the next addLinks() call
        replaceURLSpan(messageBody, false, clickable);
      }
      flags = Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS;
    } else {
      flags = Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS | Linkify.PHONE_NUMBERS;
    }

    // linkyfiy urls etc., this removes all existing URLSpan
    if (Linkify.addLinks(messageBody, flags)) {
      replaceURLSpan(messageBody, true, clickable);
    }

    return messageBody;
  }
}
