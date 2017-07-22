/*******************************************************************************
 *
 *                              Delta Chat Android
 *                           (C) 2017 Björn Petersen
 *                    Contact: r10s@b44t.com, http://b44t.com
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

        }
        return allContacts;
    }

    private static boolean hasContactsPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            return ApplicationLoader.applicationContext.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        }
        /* -- no need for a fallback chack as the caller uses a try/catch anyway - and if this fails, there's nothing we can do
        Cursor cursor = null;
        try {
            ContentResolver cr = ApplicationLoader.applicationContext.getContentResolver();
            cursor = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, projectionNames, null, null, null);
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
        */
        return true;
    }

    /* Handle contact images
     **********************************************************************************************/

    static class AvtCacheEntry {
        public Bitmap  m_avatarBitmap;
        public String  m_fallbackName;
        public boolean m_needsReload;
        AvtCacheEntry(Bitmap avatarBitmap, String fallbackName) {
            m_avatarBitmap = avatarBitmap;
            m_fallbackName = fallbackName;
            m_needsReload  = false;
        }
    }

    private final static String[] s_projectionAvatars = new String[]{
        ContactsContract.Contacts._ID,
        ContactsContract.Contacts.PHOTO_ID,
        ContactsContract.CommonDataKinds.Email.ADDRESS
        //ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
    };
    private static final Object s_sync = new Object();
    private static HashMap<String, AvtCacheEntry> s_avtCache = new HashMap<>();

    public static void cleanupAvatarCache() {
        // to detect changes of the avatar images eg. in the Contacts App,
        // this function should be called whenever the App goes to background or back to foreground.
        // instead of emptying the cache, we force a reloading (looks smarter - most times, the avatars do not change)
        synchronized (s_sync) {
            for (AvtCacheEntry cacheEntry : s_avtCache.values()) {
                cacheEntry.m_needsReload = true;
            }
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
        String tempPath = "";
        if (mrContact != null) {
            tempEmail = mrContact.getAddr();
            tempName = mrContact.getDisplayName();
        } else if (mrChat != null) {
            tempName = mrChat.getName();
            int chatType = mrChat.getType();
            if (chatType == MrChat.MR_CHAT_NORMAL) {
                int[] contact_ids = MrMailbox.getChatContacts(mrChat.getId());
                if (contact_ids.length == 1) {
                    MrContact mrc = MrMailbox.getContact(contact_ids[0]);
                    tempEmail = mrc.getAddr();
                    tempName = mrc.getDisplayName();
                }
            }
            else if( chatType == MrChat.MR_CHAT_GROUP ) {
                tempPath = mrChat.getParam(MrChat.MRP_PROFILE_IMAGE, "");
            }
        }

        setupAvatarByStrings(avtView, avtImageReceiver, avtDrawable, tempEmail, tempName, tempPath);
    }

    private static void setupAvatarByStrings(final View avtView,
                                   final ImageReceiver avtImageReceiver,
                                   final AvatarDrawable avtDrawable,
                                   String tempEmail,
                                   String tempName,
                                   String tempPath)
    {
        if( tempEmail == null ) {
            tempEmail = "fallback:" + tempName;
        }

        final String email = tempEmail;
        final String fallbackName = tempName;
        final String path = tempPath;

        // bind email+name address to view object to detect overwrites and discard loading old images (may happen on fast scrolling)
        // moreover, check if the avatar is in cache
        AvtCacheEntry cacheEntry;
        synchronized (s_sync) {
            avtImageReceiver.m_userDataUnique = email+fallbackName+path;
            cacheEntry = s_avtCache.get(email+fallbackName+path);
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
        }

        if( cacheEntry==null || cacheEntry.m_needsReload )
        {
            // avatar is not in cache or needs reloading:
            // load avatar in a working thread (when loaded, we'll add it to cache and invalidate back in the GUI thread)
            Utilities.searchQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    // is the avatar still desired?
                    synchronized (s_sync) {
                        if (!avtImageReceiver.m_userDataUnique.equals(email+fallbackName+path)) {
                            return;
                        }
                    }

                    // try to get avatar image from the address book
                    Bitmap tempBitmap = null;

                    if( !path.isEmpty() ) {
                        try {
                            Bitmap tempBitmap2 = BitmapFactory.decodeFile(path);
                            if (tempBitmap2 != null) {
                                tempBitmap = createRoundBitmap(tempBitmap2);
                            }
                        }
                        catch (Exception e) {
                            ;
                        }
                    }

                    if( tempBitmap==null && !email.startsWith("fallback:")) {
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
                            synchronized (s_sync) {
                                if (!avtImageReceiver.m_userDataUnique.equals(email+fallbackName+path)) {
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

                            synchronized (s_sync) {
                                s_avtCache.put(email+fallbackName+path, new AvtCacheEntry(photoBitmap, fallbackName));
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
            int wh = Math.min(bitmap.getWidth(), bitmap.getHeight());
            Bitmap result = Bitmap.createBitmap(wh, wh, Bitmap.Config.ARGB_8888);
            result.eraseColor(Color.TRANSPARENT);
            Canvas canvas = new Canvas(result);
            BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            if (roundPaint == null) {
                roundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                bitmapRect = new RectF();
            }
            roundPaint.setShader(shader);
            bitmapRect.set(0, 0, wh, wh);
            canvas.drawRoundRect(bitmapRect, wh, wh, roundPaint);
            return result;
        } catch (Throwable e) {
            ;
        }
        return null;
    }
}
