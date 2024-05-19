package org.thoughtcrime.securesms.contacts.avatars;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.b44t.messenger.rpc.VcardContact;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class VcardContactPhoto implements ContactPhoto {
  private final VcardContact vContact;

  public VcardContactPhoto(VcardContact vContact) {
    this.vContact = vContact;
  }

  @Override
  public InputStream openInputStream(Context context) throws IOException {
    byte[] blob = vContact.getProfileImage();
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
    messageDigest.update(vContact.getAddr().getBytes());
    messageDigest.update(ByteBuffer.allocate(4).putFloat(vContact.getTimestamp()).array());
  }
}
