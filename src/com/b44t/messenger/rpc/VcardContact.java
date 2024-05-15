package com.b44t.messenger.rpc;

import android.util.Base64;

public class VcardContact {
  // Email address.
  private final String addr;

  // The contact's name, or the email address if no name was given.
  private final String displayName;

  // Public PGP key in Base64.
  private final String key;

  // Profile image in Base64.
  private final String profileImage;

  // Contact color in HTML color format.
  private final String color;

  // Last update timestamp.
  private final int timestamp;

  public VcardContact(String addr, String displayName, String key, String profileImage, String color, int timestamp) {
    this.addr = addr;
    this.displayName = displayName;
    this.key = key;
    this.profileImage = profileImage;
    this.color = color;
    this.timestamp = timestamp;
  }

  public String getAddr() {
    return addr;
  }

  public String getDisplayName() {
    return displayName;
  }

  public byte[] getKey() {
    return key == null? null : Base64.decode(key, Base64.NO_WRAP | Base64.NO_PADDING);
  }

  public boolean hasProfileImage() {
    return profileImage != null;
  }

  public byte[] getProfileImage() {
    return profileImage == null? null : Base64.decode(profileImage, Base64.NO_WRAP | Base64.NO_PADDING);
  }

  public String getColor() {
    return color;
  }

  public int getTimestamp() {
    return timestamp;
  }
}
