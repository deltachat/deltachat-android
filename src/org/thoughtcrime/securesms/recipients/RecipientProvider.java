/*
 * Copyright (C) 2011 Whisper Systems
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

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.thoughtcrime.securesms.color.MaterialColor;
import org.thoughtcrime.securesms.database.RecipientDatabase.RecipientSettings;
import org.thoughtcrime.securesms.database.RecipientDatabase.RegisteredState;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.guava.Optional;

import java.util.LinkedList;
import java.util.List;

public class RecipientProvider {

  @SuppressWarnings("unused")
  private static final String TAG = RecipientProvider.class.getSimpleName();

  public static class RecipientDetails {
    @Nullable final String               name;
    @Nullable final String               customLabel;
    @Nullable final Uri                  systemContactPhoto;
    @Nullable final Uri                  contactUri;
    @Nullable final Long                 groupAvatarId;
    @Nullable final MaterialColor        color;
              final boolean              blocked;
              final int                  expireMessages;
    @NonNull  final List<Recipient>      participants;
    @Nullable final String               profileName;
              final boolean              seenInviteReminder;
              final Optional<Integer>    defaultSubscriptionId;
    @NonNull  final RegisteredState      registered;
    @Nullable final byte[]               profileKey;
    @Nullable final String               profileAvatar;
              final boolean              systemContact;

    public RecipientDetails(@Nullable String name, @Nullable Long groupAvatarId,
                            boolean systemContact, @Nullable RecipientSettings settings,
                            @Nullable List<Recipient> participants)
    {
      this.groupAvatarId         = groupAvatarId;
      this.systemContactPhoto    = settings     != null ? Util.uri(settings.getSystemContactPhotoUri()) : null;
      this.customLabel           = settings     != null ? settings.getSystemPhoneLabel() : null;
      this.contactUri            = settings     != null ? Util.uri(settings.getSystemContactUri()) : null;
      this.color                 = settings     != null ? settings.getColor() : null;
      this.blocked               = settings     != null && settings.isBlocked();
      this.expireMessages        = settings     != null ? settings.getExpireMessages() : 0;
      this.participants          = participants == null ? new LinkedList<>() : participants;
      this.profileName           = settings     != null ? settings.getProfileName() : null;
      this.seenInviteReminder    = settings     != null && settings.hasSeenInviteReminder();
      this.defaultSubscriptionId = settings     != null ? settings.getDefaultSubscriptionId() : Optional.absent();
      this.registered            = settings     != null ? settings.getRegistered() : RegisteredState.UNKNOWN;
      this.profileKey            = settings     != null ? settings.getProfileKey() : null;
      this.profileAvatar         = settings     != null ? settings.getProfileAvatar() : null;
      this.systemContact         = systemContact;

      if (name == null && settings != null) this.name = settings.getSystemDisplayName();
      else                                  this.name = name;
    }
  }
}