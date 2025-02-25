package org.thoughtcrime.securesms.database;


import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Address implements Parcelable, Comparable<Address> {

  public static final Parcelable.Creator<Address> CREATOR = new Parcelable.Creator<Address>() {
    public Address createFromParcel(Parcel in) {
      return new Address(in);
    }

    public Address[] newArray(int size) {
      return new Address[size];
    }
  };

  public static final Address UNKNOWN = new Address("Unknown");

  private final static String DC_CHAT_PREFIX = "dc:";
  private final static String DC_CONTACT_PREFIX = "dcc:";

  private final String address;

  public static Address fromChat(int chatId) {
    return new Address(DC_CHAT_PREFIX + chatId);
  }

  public static Address fromContact(int contactId) {
    return new Address(DC_CONTACT_PREFIX + contactId);
  }

  private Address(@NonNull String address) {
    if (address == null) throw new AssertionError(address);
    this.address = address;
  }

  public Address(Parcel in) {
    this(in.readString());
  }

  public static @NonNull Address fromSerialized(@NonNull String serialized) {
    return new Address(serialized);
  }

  public boolean isDcChat() { return address.startsWith(DC_CHAT_PREFIX); };

  public boolean isDcContact() { return address.startsWith(DC_CONTACT_PREFIX); };

  public int getDcChatId() {
    if(!isDcChat()) throw new AssertionError("Not dc chat: " + address);
    return Integer.valueOf(address.substring(DC_CHAT_PREFIX.length()));
  }

  public int getDcContactId() {
    if(!isDcContact()) throw new AssertionError("Not dc contact: " + address);
    return Integer.valueOf(address.substring(DC_CONTACT_PREFIX.length()));
  }

  @Override
  public String toString() {
    return address;
  }

  public String serialize() {
    return address;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null || !(other instanceof Address)) return false;
    return address.equals(((Address) other).address);
  }

  @Override
  public int hashCode() {
    return address.hashCode();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(address);
  }

  @Override
  public int compareTo(@NonNull Address other) {
    return address.compareTo(other.address);
  }
}
