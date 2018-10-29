package org.thoughtcrime.securesms.contacts.avatars;


import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.util.Conversions;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

public class GroupRecordContactPhoto implements ContactPhoto {

  private final int chatId;

  private final Address address;

  private final String path;

  public GroupRecordContactPhoto(Context context, Address address) {
    this.address = address;
    chatId = address.getDcChatId();
    path = DcHelper.getContext(context).getChat(chatId).getProfileImage();
  }

  @Override
  public InputStream openInputStream(Context context) throws IOException {
    return new FileInputStream(path);
  }

  @Override
  public @Nullable Uri getUri(@NonNull Context context) {
    return null;
  }

  @Override
  public boolean isProfilePhoto() {
    return false;
  }

  @Override
  public void updateDiskCacheKey(MessageDigest messageDigest) {
    messageDigest.update(address.serialize().getBytes());
    messageDigest.update(Conversions.longToByteArray(chatId));
    messageDigest.update(path.getBytes());
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof GroupRecordContactPhoto)) return false;

    GroupRecordContactPhoto that = (GroupRecordContactPhoto)other;
    return this.address.equals(that.address) && this.chatId == that.chatId && this.path.equals(that.path);
  }

  @Override
  public int hashCode() {
    return this.address.hashCode() ^ chatId;
  }

}
