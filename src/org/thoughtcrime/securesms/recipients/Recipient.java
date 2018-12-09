/*
 * Copyright (C) 2011 Whisper Systems
 * Copyright (C) 2013 - 2017 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.recipients;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.thoughtcrime.securesms.color.MaterialColor;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.avatars.ContactColors;
import org.thoughtcrime.securesms.contacts.avatars.ContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.FallbackContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.GeneratedContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.GroupRecordContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.LocalFileContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.ProfileContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.SystemContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.TransparentContactPhoto;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.database.RecipientDatabase.RegisteredState;
import org.thoughtcrime.securesms.recipients.RecipientProvider.RecipientDetails;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.guava.Optional;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public class Recipient implements RecipientModifiedListener {

  private static final String            TAG      = Recipient.class.getSimpleName();

  private final Set<RecipientModifiedListener> listeners = Collections.newSetFromMap(new WeakHashMap<RecipientModifiedListener, Boolean>());

  private final @NonNull Address address;
  private final @NonNull List<Recipient> participants = new LinkedList<>();

  private @Nullable String  name;
  private @Nullable String  customLabel;
  private           boolean resolving;

  private @Nullable Uri                  systemContactPhoto;
  private           Uri                  contactUri;
  private @Nullable Uri                  messageRingtone       = null;
  private           boolean              blocked               = false;
  private           int                  expireMessages        = 0;
  private           Optional<Integer>    defaultSubscriptionId = Optional.absent();
  private @NonNull  RegisteredState      registered            = RegisteredState.UNKNOWN;

  private @Nullable MaterialColor  color;
  private           boolean        seenInviteReminder;
  private @Nullable byte[]         profileKey;
  private @Nullable String         profileName;
  private @Nullable String         profileAvatar;

  public static @NonNull Recipient fromChat(@NonNull Context context, int dcMsgId) {
    ApplicationDcContext dcContext = DcHelper.getContext(context);
    return fromChat(dcContext, dcMsgId);
  }

  public static @NonNull Recipient fromChat (@NonNull ApplicationDcContext dcContext, int dcMsgId) {
    return dcContext.getRecipient(dcContext.getChat(dcContext.getMsg(dcMsgId).getChatId()));
  }

  public static @NonNull Recipient fromMsg (@NonNull ApplicationDcContext dcContext, int dcMsgId) {
    return dcContext.getRecipient(dcContext.getContact(dcContext.getMsg(dcMsgId).getFromId()));
  }

  @SuppressWarnings("ConstantConditions")
  public static @NonNull Recipient from(@NonNull Context context, @NonNull Address address) {
    if (address == null) throw new AssertionError(address);
    ApplicationDcContext dcContext = DcHelper.getContext(context);
    if(address.isDcContact()) {
      return dcContext.getRecipient(dcContext.getContact(address.getDcContactId()));
    } else if (address.isDcChat()) {
      return dcContext.getRecipient(dcContext.getChat(address.getDcChatId()));
    }
    else if(address.isEmail()) {
      int contactId = dcContext.lookupContactIdByAddr(address.toEmailString());
      if(contactId!=0) {
        return dcContext.getRecipient(dcContext.getContact(contactId));
      }
    }
    return dcContext.getRecipient(dcContext.getContact(0));
  }

  public Recipient(@NonNull Address address, @NonNull RecipientDetails details) {
    this.address               = address;
    this.contactUri            = details.contactUri;
    this.name                  = details.name;
    this.systemContactPhoto    = details.systemContactPhoto;
    this.color                 = details.color;
    this.customLabel           = details.customLabel;
    this.blocked               = details.blocked;
    this.expireMessages        = details.expireMessages;
    this.seenInviteReminder    = details.seenInviteReminder;
    this.defaultSubscriptionId = details.defaultSubscriptionId;
    this.registered            = details.registered;
    this.profileKey            = details.profileKey;
    this.profileName           = details.profileName;
    this.profileAvatar         = details.profileAvatar;
    this.participants.addAll(details.participants);
    this.resolving    = false;
  }

  public synchronized @Nullable Uri getContactUri() {
    return this.contactUri;
  }

  public synchronized @Nullable String getName() {
    if (this.name == null && isMmsGroupRecipient()) {
      List<String> names = new LinkedList<>();

      for (Recipient recipient : participants) {
        names.add(recipient.toShortString());
      }

      return Util.join(names, ", ");
    }

    return this.name;
  }

  public void setName(@Nullable String name) {
    boolean notify = false;

    synchronized (this) {
      if (!Util.equals(this.name, name)) {
        this.name = name;
        notify = true;
      }
    }

    if (notify) notifyListeners();
  }

  public synchronized @NonNull MaterialColor getColor() {
    if (color != null)      return color;
    else if (name != null)       return ContactColors.generateFor(name);
    else                         return ContactColors.UNKNOWN_COLOR;
  }

  public void setColor(@NonNull MaterialColor color) {
    synchronized (this) {
      this.color = color;
    }

    notifyListeners();
  }

  public @NonNull Address getAddress() {
    return address;
  }

  public synchronized @Nullable String getProfileName() {
    return profileName;
  }

  public void setProfileAvatar(@Nullable String profileAvatar) {
    synchronized (this) {
      this.profileAvatar = profileAvatar;
    }

    notifyListeners();
  }

  public boolean isGroupRecipient() {
    return participants.size() > 1;
  }

  public boolean isMmsGroupRecipient() {
    return address.isMmsGroup();
  }

  public boolean isPushGroupRecipient() {
    return address.isGroup() && !address.isMmsGroup();
  }

  public @NonNull synchronized List<Recipient> getParticipants() {
    return new LinkedList<>(participants);
  }

  public synchronized void addListener(RecipientModifiedListener listener) {
    if (listeners.isEmpty()) {
      for (Recipient recipient : participants) recipient.addListener(this);
    }
    listeners.add(listener);
  }

  public synchronized void removeListener(RecipientModifiedListener listener) {
    listeners.remove(listener);

    if (listeners.isEmpty()) {
      for (Recipient recipient : participants) recipient.removeListener(this);
    }
  }

  public synchronized String toShortString() {
    return (getName() == null ? address.serialize() : getName());
  }

  public int getFallbackAvatarColor(Context context) {
    int rgb = 0x00808080;
    if(address.isDcContact()) {
      rgb = DcHelper.getContext(context).getContact(address.getDcContactId()).getColor();
    }
    else if(address.isDcChat()){
      rgb = DcHelper.getContext(context).getChat(address.getDcChatId()).getColor();
    }
    int argb = Color.argb(0xFF, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
    return argb;
  }

  public synchronized @NonNull Drawable getFallbackAvatarDrawable(Context context) {
    return getFallbackContactPhoto().asDrawable(context, getFallbackAvatarColor(context), false);
  }

  public synchronized @NonNull FallbackContactPhoto getFallbackContactPhoto() {
    if      (isResolving())            return new TransparentContactPhoto();
    else if (!TextUtils.isEmpty(name)) return new GeneratedContactPhoto(name);
    else                               return new GeneratedContactPhoto("#");
  }

  public synchronized @Nullable ContactPhoto getContactPhoto(Context context) {
    LocalFileContactPhoto contactPhoto = null;
    if (address.isDcChat()) {
      contactPhoto = new GroupRecordContactPhoto(context, address);
    }
    else if (address.isDcContact()) {
       contactPhoto = new ProfileContactPhoto(context, address);
    }

    if (contactPhoto!=null) {
      String path = contactPhoto.getPath(context);
      if (path != null && !path.isEmpty()) {
        return contactPhoto;
      }
    }

    if (systemContactPhoto != null) {
      return new SystemContactPhoto(address, systemContactPhoto, 0);
    }

    return null;
  }

  public void setSystemContactPhoto(@Nullable Uri systemContactPhoto) {
    boolean notify = false;

    synchronized (this) {
      if (!Util.equals(systemContactPhoto, this.systemContactPhoto)) {
        this.systemContactPhoto = systemContactPhoto;
        notify = true;
      }
    }

    if (notify) notifyListeners();
  }

  public synchronized @Nullable Uri getMessageRingtone() {
    if (messageRingtone != null && messageRingtone.getScheme() != null && messageRingtone.getScheme().startsWith("file")) {
      return null;
    }

    return messageRingtone;
  }

  public synchronized boolean isBlocked() {
    return blocked;
  }

  public void setBlocked(boolean blocked) {
    synchronized (this) {
      this.blocked = blocked;
    }

    notifyListeners();
  }

  public synchronized RegisteredState getRegistered() {
    if      (isPushGroupRecipient()) return RegisteredState.REGISTERED;
    else if (isMmsGroupRecipient())  return RegisteredState.NOT_REGISTERED;

    return registered;
  }

  public void setRegistered(@NonNull RegisteredState value) {
    boolean notify = false;

    synchronized (this) {
      if (this.registered != value) {
        this.registered = value;
        notify = true;
      }
    }

    if (notify) notifyListeners();
  }

  public synchronized Recipient resolve() {
    while (resolving) Util.wait(this, 0);
    return this;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || !(o instanceof Recipient)) return false;

    Recipient that = (Recipient) o;

    return this.address.equals(that.address);
  }

  @Override
  public int hashCode() {
    return this.address.hashCode();
  }

  private void notifyListeners() {
    Set<RecipientModifiedListener> localListeners;

    synchronized (this) {
      localListeners = new HashSet<>(listeners);
    }

    for (RecipientModifiedListener listener : localListeners)
      listener.onModified(this);
  }

  @Override
  public String toString() {
    return "Recipient{" +
        "listeners=" + listeners +
        ", address=" + address +
        ", participants=" + participants +
        ", name='" + name + '\'' +
        ", customLabel='" + customLabel + '\'' +
        ", resolving=" + resolving +
        ", systemContactPhoto=" + systemContactPhoto +
        ", contactUri=" + contactUri +
        ", blocked=" + blocked +
        ", expireMessages=" + expireMessages +
        ", defaultSubscriptionId=" + defaultSubscriptionId +
        ", registered=" + registered +
        ", color=" + color +
        ", seenInviteReminder=" + seenInviteReminder +
        ", profileKey=" + Arrays.toString(profileKey) +
        ", profileName='" + profileName + '\'' +
        ", profileAvatar='" + profileAvatar + '\'' +
        '}';
  }

  @Override
  public void onModified(Recipient recipient) {
    notifyListeners();
  }

  public synchronized boolean isResolving() {
    return resolving;
  }


}
