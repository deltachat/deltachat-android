package org.thoughtcrime.securesms.util;

import android.support.annotation.NonNull;

import java.io.IOException;

public class GroupUtil {

  private static final String ENCODED_SIGNAL_GROUP_PREFIX = "__textsecure_group__!";
  private static final String ENCODED_MMS_GROUP_PREFIX    = "__signal_mms_group__!";
  private static final String TAG                  = GroupUtil.class.getSimpleName();

  public static boolean isEncodedGroup(@NonNull String groupId) {
    return groupId.startsWith(ENCODED_SIGNAL_GROUP_PREFIX) || groupId.startsWith(ENCODED_MMS_GROUP_PREFIX);
  }

}
