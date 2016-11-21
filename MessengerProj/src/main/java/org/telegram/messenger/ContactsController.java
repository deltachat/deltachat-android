/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.messenger;

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.provider.ContactsContract;
import java.util.HashMap;

public class ContactsController {

    public static class Contact {
        public String name;
        public String email;
    }

    private String[] projectionNames = {
            ContactsContract.Data.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Email.ADDRESS
    };

    private static volatile ContactsController Instance = null;

    public static ContactsController getInstance() {
        ContactsController localInstance = Instance;
        if (localInstance == null) {
            synchronized (ContactsController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new ContactsController();
                }
            }
        }
        return localInstance;
    }

    public ContactsController() {
    }

    public String readContactsFromPhoneBook() {
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

    private boolean hasContactsPermission() {
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
            FileLog.e("messenger", e);
        } finally {
            try {
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
                FileLog.e("messenger", e);
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
}
