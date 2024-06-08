package org.thoughtcrime.securesms.contacts.avatars;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.util.JsonUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

import chat.delta.rpc.types.VcardContact;

public class VcardContactPhoto implements ContactPhoto {
  private final VcardContact vContact;

  public VcardContactPhoto(VcardContact vContact) {
    this.vContact = vContact;
  }

  @Override
  public InputStream openInputStream(Context context) throws IOException {
    byte[] blob = JsonUtils.decodeBase64(vContact.profileImage);
    return (blob == null)? null : new ByteArrayInputStream(blob);
  }

  @Override
  public @Nullable Uri getUri(@NonNull Context context) {
    return null;
  }

  @Override
  public boolean isProfilePhoto() {
    return true;
  }

  @Override
  public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
    messageDigest.update(vContact.addr.getBytes());
    messageDigest.update(ByteBuffer.allocate(4).putFloat(vContact.timestamp).array());
  }
}
