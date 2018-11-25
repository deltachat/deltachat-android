package org.thoughtcrime.securesms.util;

import android.support.annotation.NonNull;

import java.io.IOException;

public class GroupUtil {

  private static final String ENCODED_SIGNAL_GROUP_PREFIX = "__textsecure_group__!";
  private static final String ENCODED_MMS_GROUP_PREFIX    = "__signal_mms_group__!";
  private static final String TAG                  = GroupUtil.class.getSimpleName();

  public static String getEncodedId(byte[] groupId, boolean mms) {
    return (mms ? ENCODED_MMS_GROUP_PREFIX  : ENCODED_SIGNAL_GROUP_PREFIX) + Hex.toStringCondensed(groupId);
  }

  public static byte[] getDecodedId(String groupId) throws IOException {
    if (!isEncodedGroup(groupId)) {
      throw new IOException("Invalid encoding");
    }

    return Hex.fromStringCondensed(groupId.split("!", 2)[1]);
  }

  public static boolean isEncodedGroup(@NonNull String groupId) {
    return groupId.startsWith(ENCODED_SIGNAL_GROUP_PREFIX) || groupId.startsWith(ENCODED_MMS_GROUP_PREFIX);
  }

  public static boolean isMmsGroup(@NonNull String groupId) {
    return groupId.startsWith(ENCODED_MMS_GROUP_PREFIX);
  }
}
