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

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.avatars.ContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.FallbackContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.GeneratedContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.GroupRecordContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.LocalFileContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.ProfileContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.SystemContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.TransparentContactPhoto;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public class Recipient implements RecipientModifiedListener {

  private final Set<RecipientModifiedListener> listeners = Collections.newSetFromMap(new WeakHashMap<RecipientModifiedListener, Boolean>());

  private final @NonNull Address address;

  private @Nullable String  name;
  private @Nullable String  customLabel;

  private @Nullable Uri                  systemContactPhoto;
  private           Uri                  contactUri;
  private @Nullable Uri                  messageRingtone       = null;
  private           boolean              blocked               = false;

  private @Nullable String         profileName;
  private @Nullable String         profileAvatar;

  // either dcChat or dcContact are set
  private @Nullable DcChat dcChat;
  private @Nullable DcContact dcContact;

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

  public Recipient(@NonNull Address address, @Nullable String name, @Nullable DcChat dcChat, @Nullable DcContact dcContact) {
    this.dcChat                = dcChat;
    this.dcContact             = dcContact;
    this.address               = address;
    this.contactUri            = null;
    this.name                  = name;
    this.systemContactPhoto    = null;
    this.customLabel           = null;
    this.blocked               = false;
    this.profileName           = null;
    this.profileAvatar         = null;
  }

  public synchronized @Nullable Uri getContactUri() {
    return this.contactUri;
  }

  public synchronized @Nullable String getName() {
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
    return dcChat!=null && dcChat.isGroup();
  }

  public @NonNull synchronized List<Recipient> loadParticipants(Context context) {
    List<Recipient> participants = new ArrayList<>();
    if (dcChat!=null) {
      ApplicationDcContext dcContext = DcHelper.getContext(context);
      int[] contactIds = dcContext.getChatContacts(dcChat.getId());
      for (int contactId : contactIds) {
        participants.add(dcContext.getRecipient(ApplicationDcContext.RECIPIENT_TYPE_CONTACT, contactId));
      }
    }
    return participants;
  }

  public synchronized void addListener(RecipientModifiedListener listener) {
    // TODO: better use DC_EVENT_*
  }

  public synchronized void removeListener(RecipientModifiedListener listener) {
    // TODO: better use DC_EVENT_*
  }

  public synchronized String toShortString() {
    return (getName() == null ? address.serialize() : getName());
  }

  public int getFallbackAvatarColor(Context context) {
    int rgb = 0x00808080;
    if(dcContact!=null) {
      rgb = dcContact.getColor();
    }
    else if(dcChat!=null){
      rgb = dcChat.getColor();
    }
    int argb = Color.argb(0xFF, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
    return argb;
  }

  public synchronized @NonNull Drawable getFallbackAvatarDrawable(Context context) {
    return getFallbackContactPhoto().asDrawable(context, getFallbackAvatarColor(context), false);
  }

  public synchronized @NonNull FallbackContactPhoto getFallbackContactPhoto() {
         if (!TextUtils.isEmpty(name)) return new GeneratedContactPhoto(name);
    else                               return new GeneratedContactPhoto("#");
  }

  public synchronized @Nullable ContactPhoto getContactPhoto(Context context) {
    LocalFileContactPhoto contactPhoto = null;
    if (dcChat!=null) {
      contactPhoto = new GroupRecordContactPhoto(context, address);
    }
    else if (dcContact!=null) {
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
        ", name='" + name + '\'' +
        ", customLabel='" + customLabel + '\'' +
        ", systemContactPhoto=" + systemContactPhoto +
        ", contactUri=" + contactUri +
        ", blocked=" + blocked +
        ", profileName='" + profileName + '\'' +
        ", profileAvatar='" + profileAvatar + '\'' +
        '}';
  }

  @Override
  public void onModified(Recipient recipient) {
    notifyListeners();
  }
}
