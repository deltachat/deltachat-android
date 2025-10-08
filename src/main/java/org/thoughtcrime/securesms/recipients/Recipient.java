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
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.avatars.ContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.FallbackContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.GeneratedContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.GroupRecordContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.LocalFileContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.ProfileContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.SystemContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.VcardContactPhoto;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.util.Hash;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

import chat.delta.rpc.types.VcardContact;

public class Recipient {

  private final Set<RecipientModifiedListener> listeners = Collections.newSetFromMap(new WeakHashMap<RecipientModifiedListener, Boolean>());

  private final @NonNull Address address;

  private final @Nullable String  customLabel;

  private @Nullable Uri                  systemContactPhoto;
  private final Uri                  contactUri;

  private final @Nullable String         profileName;
  private @Nullable String         profileAvatar;

  private final @Nullable DcChat dcChat;
  private @Nullable DcContact dcContact;
  private final @Nullable VcardContact vContact;

  public static @NonNull Recipient fromChat(@NonNull Context context, int dcMsgId) {
    DcContext dcContext = DcHelper.getContext(context);
    return new Recipient(context, dcContext.getChat(dcContext.getMsg(dcMsgId).getChatId()));
  }

  @SuppressWarnings("ConstantConditions")
  public static @NonNull Recipient from(@NonNull Context context, @NonNull Address address) {
    if (address == null) throw new AssertionError(address);
    DcContext dcContext = DcHelper.getContext(context);
    if(address.isDcContact()) {
      return new Recipient(context, dcContext.getContact(address.getDcContactId()));
    } else if (address.isDcChat()) {
      return new Recipient(context, dcContext.getChat(address.getDcChatId()));
    }
    else if(DcHelper.getContext(context).mayBeValidAddr(address.toString())) {
      int contactId = dcContext.lookupContactIdByAddr(address.toString());
      if(contactId!=0) {
        return new Recipient(context, dcContext.getContact(contactId));
      }
    }
    return new Recipient(context, dcContext.getContact(0));
  }

  public Recipient(@NonNull Context context, @NonNull DcChat dcChat) {
    this(context, dcChat, null, null, null);
  }

  public Recipient(@NonNull Context context, @NonNull VcardContact vContact) {
    this(context, null, null, null, vContact);
  }

  public Recipient(@NonNull Context context, @NonNull DcContact dcContact) {
    this(context, null, dcContact, null, null);
  }

  public Recipient(@NonNull Context context, @NonNull DcContact dcContact, @NonNull String profileName) {
    this(context, null, dcContact, profileName, null);
  }

  private Recipient(@NonNull Context context, @Nullable DcChat dcChat, @Nullable DcContact dcContact, @Nullable String profileName, @Nullable VcardContact vContact) {
    this.dcChat                = dcChat;
    this.dcContact             = dcContact;
    this.profileName           = profileName;
    this.vContact              = vContact;
    this.contactUri            = null;
    this.systemContactPhoto    = null;
    this.customLabel           = null;
    this.profileAvatar         = null;

    if(dcContact!=null) {
      this.address = Address.fromContact(dcContact.getId());
      maybeSetSystemContactPhoto(context, dcContact);
      if (dcContact.getId() == DcContact.DC_CONTACT_ID_SELF) {
        setProfileAvatar("SELF");
      }
    }
    else if(dcChat!=null) {
      int chatId = dcChat.getId();
      this.address = Address.fromChat(chatId);
      if (!dcChat.isMultiUser()) {
        DcContext dcContext = DcHelper.getAccounts(context).getAccount(dcChat.getAccountId());
        int[] contacts = dcContext.getChatContacts(chatId);
        if( contacts.length>=1 ) {
          this.dcContact = dcContext.getContact(contacts[0]);
          maybeSetSystemContactPhoto(context, this.dcContact);
        }
      }
    }
    else {
      this.address = Address.UNKNOWN;
    }
  }

  public @Nullable String getName() {
    if(dcChat!=null) {
      return dcChat.getName();
    }
    else if(dcContact!=null) {
      return dcContact.getDisplayName();
    }
    else if(vContact!=null) {
      return vContact.displayName;
    }
    return "";
  }

  public @Nullable DcContact getDcContact() {
    return dcContact;
  }

  public @NonNull Address getAddress() {
    return address;
  }

  public @Nullable String getProfileName() {
    return profileName;
  }

  public void setProfileAvatar(@Nullable String profileAvatar) {
    synchronized (this) {
      this.profileAvatar = profileAvatar;
    }

    notifyListeners();
  }

  public boolean isMultiUserRecipient() {
    return dcChat!=null && dcChat.isMultiUser();
  }

  public synchronized void addListener(RecipientModifiedListener listener) {
    listeners.add(listener);
  }

  public synchronized void removeListener(RecipientModifiedListener listener) {
    listeners.remove(listener);
  }

  public synchronized String toShortString() {
    return getName();
  }

  public int getFallbackAvatarColor() {
    int rgb = 0x00808080;
    if(dcChat!=null) {
      rgb = dcChat.getColor();
    }
    else if(dcContact!=null) {
      rgb = dcContact.getColor();
    }
    else if(vContact!=null) {
      rgb = Color.parseColor(vContact.color);
    }
    return Color.argb(0xFF, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
  }

  public synchronized @NonNull Drawable getFallbackAvatarDrawable(Context context) {
    return getFallbackAvatarDrawable(context, true);
  }

  public synchronized @NonNull Drawable getFallbackAvatarDrawable(Context context, boolean roundShape) {
    return getFallbackContactPhoto().asDrawable(context, getFallbackAvatarColor(), roundShape);
  }

  public synchronized @NonNull GeneratedContactPhoto getFallbackContactPhoto() {
    String name = getName();
    if (!TextUtils.isEmpty(profileName)) return new GeneratedContactPhoto(profileName);
    else if (!TextUtils.isEmpty(name))   return new GeneratedContactPhoto(name);
    else                                 return new GeneratedContactPhoto("#");
  }

  public synchronized @Nullable ContactPhoto getContactPhoto(Context context) {
    LocalFileContactPhoto contactPhoto = null;
    if (dcChat!=null) {
      contactPhoto = new GroupRecordContactPhoto(context, address, dcChat);
    }
    else if (dcContact!=null) {
       contactPhoto = new ProfileContactPhoto(context, address, dcContact);
    }

    if (contactPhoto!=null) {
      String path = contactPhoto.getPath(context);
      if (path != null && !path.isEmpty()) {
        return contactPhoto;
      }
    }

    if (vContact!=null && vContact.profileImage != null) {
      return new VcardContactPhoto(vContact);
    }

    if (systemContactPhoto != null) {
      return new SystemContactPhoto(address, systemContactPhoto, 0);
    }

    return null;
  }

  private void maybeSetSystemContactPhoto(@NonNull Context context, DcContact contact) {
    String identifier = Hash.sha256(contact.getDisplayName() + contact.getAddr());
    Uri systemContactPhoto = Prefs.getSystemContactPhoto(context, identifier);
    if (systemContactPhoto != null) {
      setSystemContactPhoto(systemContactPhoto);
    }
  }

  private void setSystemContactPhoto(@Nullable Uri systemContactPhoto) {
    boolean notify = false;

    synchronized (this) {
      if (!Util.equals(systemContactPhoto, this.systemContactPhoto)) {
        this.systemContactPhoto = systemContactPhoto;
        notify = true;
      }
    }

    if (notify) notifyListeners();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Recipient)) return false;

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

  public DcChat getChat()
  {
    return dcChat!=null? dcChat : new DcChat(0, 0);
  }

  @NonNull
  @Override
  public String toString() {
    return "Recipient{" +
        "listeners=" + listeners +
        ", address=" + address +
        ", customLabel='" + customLabel + '\'' +
        ", systemContactPhoto=" + systemContactPhoto +
        ", contactUri=" + contactUri +
        ", profileName='" + profileName + '\'' +
        ", profileAvatar='" + profileAvatar + '\'' +
        '}';
  }
}
