/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *     Copyright (C) 2016 Björn Petersen Software Design and Development
 *                   Contact: r10s@b44t.com, http://b44t.com
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see http://www.gnu.org/licenses/ .
 *
 *******************************************************************************
 *
 * File:    ContactsController.java
 * Authors: Björn Petersen
 *
 ******************************************************************************/


package com.b44t.messenger;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;

import com.b44t.ui.Components.AvatarDrawable;

import java.io.InputStream;
import java.util.HashMap;

public class ContactsController {

    static ContentResolver s_cr;
    private static final String TAG = "ContactsController";

    public static class Contact {
        public String name;
        public String email;
    }

    private final static String[] projectionNames = {
            ContactsContract.Data.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Email.ADDRESS
    };

    public static String readContactsFromPhoneBook() {
        HashMap<String, String> contactsMap = new HashMap<String, String>();
        String allContacts = "";
        try {
            if (!hasContactsPermission()) {
                return "";
            }
            ContentResolver cr = ApplicationLoader.applicationContext.getContentResolver();
            Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, // <-- this works, but I do not understand it (did not got the time, yet (bp))
                    projectionNames, null, null, null);
            if (pCur != null && pCur.getCount() > 0) {
                while (pCur.moveToNext()) {
                    String display_name = pCur.getString(0);
                    String email        = pCur.getString(1);
                    if( email!=null && !email.isEmpty() && contactsMap.get(email)==null ) {
                        String nameToAdd = display_name!=null? display_name : "";
                        if( nameToAdd.isEmpty()) {
                            nameToAdd = email;
                        }

                        allContacts += nameToAdd + "\n" + email + "\n";
                        contactsMap.put(email, nameToAdd);
                    }
                }
                pCur.close();
            }
        } catch (Exception e) {
            FileLog.e("messenger", e);
        }
        return allContacts;
    }

    private static boolean hasContactsPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            return ApplicationLoader.applicationContext.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        }
        Cursor cursor = null;
        try {
            ContentResolver cr = ApplicationLoader.applicationContext.getContentResolver();
            cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projectionNames, null, null, null);
            if (cursor == null || cursor.getCount() == 0) {
                return false;
            }
        } catch (Exception e) {
            ;
        } finally {
            try {
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
                ;
            }
        }
        return true;
    }

    public static String formatName(String firstName, String lastName) {

        if (firstName != null) {
            firstName = firstName.trim();
        }
        if (lastName != null) {
            lastName = lastName.trim();
        }
        StringBuilder result = new StringBuilder((firstName != null ? firstName.length() : 0) + (lastName != null ? lastName.length() : 0) + 1);
        if (LocaleController.nameDisplayOrder == 1) {
            if (firstName != null && firstName.length() > 0) {
                result.append(firstName);
                if (lastName != null && lastName.length() > 0) {
                    result.append(" ");
                    result.append(lastName);
                }
            } else if (lastName != null && lastName.length() > 0) {
                result.append(lastName);
            }
        } else {
            if (lastName != null && lastName.length() > 0) {
                result.append(lastName);
                if (firstName != null && firstName.length() > 0) {
                    result.append(" ");
                    result.append(firstName);
                }
            } else if (firstName != null && firstName.length() > 0) {
                result.append(firstName);
            }
        }
        return result.toString();
    }


    /* Handle contact images
     **********************************************************************************************/

    static class AvtCacheEntry {
        public Bitmap m_avatarBitmap;
        public String m_fallbackName;
        AvtCacheEntry(Bitmap avatarBitmap, String fallbackName) {
            m_avatarBitmap = avatarBitmap;
            m_fallbackName = fallbackName;
        }
    }

    private final static String[] s_projectionAvatars = new String[]{
        ContactsContract.Contacts._ID,
        ContactsContract.Contacts.PHOTO_ID,
        ContactsContract.CommonDataKinds.Email.ADDRESS
        //ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
    };
    private static HashMap<View, String> s_viewBindings = new HashMap<>();
    private static HashMap<String, AvtCacheEntry> s_avtCache = new HashMap<>();

    public static void cleanupAvatarCache() {
        // to detect changes of the avatar images eg. in the Contacts App,
        // this function should be called whenever the App goes to background.
        synchronized (s_viewBindings /*we use s_viewBindings for locking!*/ ) {
            Log.i(TAG, "Avatar cache discarded.");
            s_avtCache.clear();
        }
    }

    public static void setupAvatar(final View avtView,
                                   final ImageReceiver avtImageReceiver,
                                   final AvatarDrawable avtDrawable,
                                   MrContact mrContact,
                                   MrChat mrChat)
    {
        // get email/name to search avatar image for
        String tempEmail = null;
        String tempName = "";
        if (mrContact != null) {
            tempEmail = mrContact.getAddr();
            tempName = mrContact.getDisplayName();
        } else if (mrChat != null) {
            tempName = mrChat.getName();
            if (mrChat.getType() == MrChat.MR_CHAT_NORMAL) {
                int[] contact_ids = MrMailbox.getChatContacts(mrChat.getId());
                if (contact_ids.length == 1) {
                    MrContact mrc = MrMailbox.getContact(contact_ids[0]);
                    tempEmail = mrc.getAddr();
                    tempName = mrc.getDisplayName();
                }
            }
        }

        if( tempEmail == null ) {
            tempEmail = "fallback:" + tempName;
        }

        final String email = tempEmail;
        final String fallbackName = tempName;

        // bind e-email address to view object to detect overwrites and discard loading old images (may happen on fast scrolling)
        // moreover, check if the avatar is in cache
        AvtCacheEntry cacheEntry;
        synchronized (s_viewBindings) {
            s_viewBindings.put(avtView, email);
            cacheEntry = s_avtCache.get(email);
        }

        if( cacheEntry != null )
        {
            // can use avatar from cache, very fine
            if (cacheEntry.m_avatarBitmap != null) {
                avtImageReceiver.setImageBitmap(cacheEntry.m_avatarBitmap);
            } else {
                avtDrawable.setInfoByName(cacheEntry.m_fallbackName);
                avtImageReceiver.setImage(null, "50_50", avtDrawable, null, false);
            }
        }
        else
        {
            // avatar is not in cache, empty the image (may be the bitmap of another use as we re-use the objects)
            avtImageReceiver.setImage(null, "50_50", null, null, false);

            // load avatar in a working thread (when loaded, we'll add it to cache and invalidate back in the GUI thread)
            Utilities.searchQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    // is the avatar still desired?
                    synchronized (s_viewBindings) {
                        if (!s_viewBindings.get(avtView).equals(email)) {
                            return;
                        }
                    }

                    // try to get avatar image from the address book
                    Bitmap tempBitmap = null;
                    if (!email.startsWith("fallback:")) {
                        try {
                            if (s_cr == null) {
                                s_cr = ApplicationLoader.applicationContext.getContentResolver();
                            }
                            Uri uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Email.CONTENT_LOOKUP_URI, Uri.encode(email));
                            Cursor pCur = s_cr.query(uri, s_projectionAvatars, null, null, null);
                            if (pCur != null) {
                                if (pCur.getCount() > 0) {
                                    while (pCur.moveToNext()) {
                                        int contact_id = pCur.getInt(0);
                                        int photo_id = pCur.getInt(1);
                                        String addr = pCur.getString(2);
                                        if (addr.equalsIgnoreCase(email) && contact_id > 0 && photo_id > 0) {
                                            Bitmap tempBitmap2 = loadContactPhoto(s_cr, contact_id, photo_id);
                                            if (tempBitmap2 != null) {
                                                tempBitmap = createRoundBitmap(tempBitmap2);
                                            }
                                            break;
                                        }
                                    }
                                }
                                pCur.close();
                            }
                        } catch (Exception e) {
                            ;
                        }
                    }

                    final Bitmap photoBitmap = tempBitmap;

                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            // is the avatar still desired?
                            synchronized (s_viewBindings) {
                                if (!s_viewBindings.get(avtView).equals(email)) {
                                    return;
                                }
                            }

                            if (photoBitmap != null) {
                                avtImageReceiver.setImageBitmap(photoBitmap);
                            } else {
                                avtDrawable.setInfoByName(fallbackName);
                                avtImageReceiver.setImage(null, "50_50", avtDrawable, null, false);
                            }
                            avtView.invalidate();

                            synchronized (s_viewBindings /*we use s_viewBindings for locking!*/ ) {
                                s_avtCache.put(email, new AvtCacheEntry(photoBitmap, fallbackName));
                            }
                        }
                    });
                }
            });
        }
    }

    // from http://stackoverflow.com/questions/2383580/how-do-i-load-a-contact-photo
    public static Bitmap loadContactPhoto(ContentResolver cr, long contact_id, long photo_id)
    {
        // first try using photo_id
        byte[] photoBytes = null;
        Uri photoUri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, photo_id);
        Cursor c = cr.query(photoUri, new String[] {ContactsContract.CommonDataKinds.Photo.PHOTO}, null, null, null);
        if( c != null ) {
            try {
                if (c.moveToFirst()) {
                    photoBytes = c.getBlob(0);
                }

            } catch (Exception e) {
                ;
            } finally {
                c.close();
            }
        }
        if (photoBytes != null) {
            return BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);
        }

        // second try using contact_id
        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact_id);
        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);
        if (input != null) {
            return BitmapFactory.decodeStream(input);
        }

        return null;
    }

    private static Paint roundPaint;
    private static RectF bitmapRect;
    private static Bitmap createRoundBitmap(Bitmap bitmap) {
        try {
            Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            result.eraseColor(Color.TRANSPARENT);
            Canvas canvas = new Canvas(result);
            BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            if (roundPaint == null) {
                roundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                bitmapRect = new RectF();
            }
            roundPaint.setShader(shader);
            bitmapRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
            canvas.drawRoundRect(bitmapRect, bitmap.getWidth(), bitmap.getHeight(), roundPaint);
            return result;
        } catch (Throwable e) {
            ;
        }
        return null;
    }
}
