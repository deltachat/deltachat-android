package org.thoughtcrime.securesms.util;

import android.net.MailTo;
import android.net.Uri;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class MailtoUtil {
    private static final String MAILTO = "mailto";
    private static final String SUBJECT = "subject";
    private static final String BODY = "body";
    private static final String QUERY_SEPARATOR = "&";
    private static final String KEY_VALUE_SEPARATOR = "=";

    public static boolean isMailto(Uri uri) {
        return uri != null && MAILTO.equals(uri.getScheme());
    }

    public static String[] getRecipients(Uri uri) {
        String[] recipientsArray = new String[0];
        if (uri != null) {
            MailTo mailto = MailTo.parse(uri.toString());
            String recipientsList = mailto.getTo();
            if(recipientsList != null && !recipientsList.trim().isEmpty()) {
                recipientsArray = recipientsList.trim().split(",");
            }
        }
        return recipientsArray;
    }

    public static String getText(Uri uri) {
        Map<String, String> mailtoQueryMap = getMailtoQueryMap(uri);
        String textToShare = mailtoQueryMap.get(SUBJECT);
        String body = mailtoQueryMap.get(BODY);
        if (body != null && !body.isEmpty()) {
            if (textToShare != null && !textToShare.isEmpty()) {
                textToShare += "\n" + body;
            } else {
                textToShare = body;
            }
        }
        return textToShare != null? textToShare : "";
    }

    private static Map<String, String> getMailtoQueryMap(Uri uri) {
        Map<String, String> mailtoQueryMap = new HashMap<>();
        String query =  uri.getEncodedQuery();
        if (query != null && !query.isEmpty()) {
            String[] queryArray = query.split(QUERY_SEPARATOR);
            for(String queryEntry : queryArray) {
                String[] queryEntryArray = queryEntry.split(KEY_VALUE_SEPARATOR);
                try {
                    mailtoQueryMap.put(queryEntryArray[0], URLDecoder.decode(queryEntryArray[1], "UTF-8"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return mailtoQueryMap;
    }
}
