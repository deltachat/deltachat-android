/**
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
package org.thoughtcrime.securesms.contacts;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import org.thoughtcrime.securesms.ContactSelectionListFragment;
import org.thoughtcrime.securesms.util.Hash;
import org.thoughtcrime.securesms.util.Prefs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class was originally a layer of indirection between
 * ContactAccessorNewApi and ContactAccesorOldApi, which corresponded
 * to the API changes between 1.x and 2.x.
 *
 * Now that we no longer support 1.x, this class mostly serves as a place
 * to encapsulate Contact-related logic.  It's still a singleton, mostly
 * just because that's how it's currently called from everywhere.
 *
 * @author Moxie Marlinspike
 */

public class ContactAccessor {
  private static final String TAG = ContactSelectionListFragment.class.getSimpleName();

  private static final int CONTACT_CURSOR_NAME = 0;

  private static final int CONTACT_CURSOR_MAIL = 1;

  private static final int CONTACT_CURSOR_CONTACT_ID = 2;

  private static final ContactAccessor instance = new ContactAccessor();

  public static synchronized ContactAccessor getInstance() {
    return instance;
  }

  public Cursor getAllSystemContacts(Context context) {
    String[] projection = {ContactsContract.Data.DISPLAY_NAME, ContactsContract.CommonDataKinds.Email.ADDRESS, ContactsContract.Data.CONTACT_ID};
    return context.getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, projection, null, null, null);
  }

  public String getAllSystemContactsAsString(Context context) {
    Cursor systemContactsCursor = getAllSystemContacts(context);
    StringBuilder result = new StringBuilder();
    List<String> mailList = new ArrayList<>();
    Set<String> contactPhotoIdentifiers = new HashSet<>();
    while (systemContactsCursor != null && systemContactsCursor.moveToNext()) {

      String name;
      try {
        name = systemContactsCursor.getString(CONTACT_CURSOR_NAME);
        if (name != null) {
          name = name.replace("\r", ""); // remove characters later used as field separator
          name = name.replace("\n", "");
        } else {
          name = "";
        }
      } catch(Exception e) {
        Log.e(TAG, "Can't get contact name: " + e);
        name = "";
      }

      String mail = null;
      try {
        mail = systemContactsCursor.getString(CONTACT_CURSOR_MAIL);
        if (mail != null) {
          mail = mail.replace("\r", ""); // remove characters later used as field separator
          mail = mail.replace("\n", "");
        }
      } catch(Exception e) {
        Log.e(TAG, "Can't get contact addr: " + e);
      }

      String contactId = systemContactsCursor.getString(CONTACT_CURSOR_CONTACT_ID);
      if (contactId != null) {
        String identifier = name + mail;
        String hashedIdentifierAndId = Hash.sha256(identifier) + "|" + contactId;
        contactPhotoIdentifiers.add(hashedIdentifierAndId);
      }
      if (mail != null && !mail.isEmpty() && !mailList.contains(mail)) {
          mailList.add(mail);
          if (name.isEmpty()) {
            name = mail;
          }
          result.append(name).append("\n").append(mail).append("\n");
      }
    }
    Prefs.setSystemContactPhotos(context, contactPhotoIdentifiers);
    return result.toString();
  }
}
