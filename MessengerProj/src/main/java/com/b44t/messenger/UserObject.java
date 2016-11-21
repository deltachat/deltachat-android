/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.b44t.messenger;

import com.b44t.tgnet.TLRPC;

public class UserObject {

    public static boolean isDeleted(TLRPC.User user) {
        return user == null || user.deleted;
    }

    public static boolean isUserSelf(TLRPC.User user) {
        return user.self;
    }

    public static String getUserName(TLRPC.User user) {
        if (user == null || isDeleted(user)) {
            return "ErrName";
        }
        String name = ContactsController.formatName(user.first_name, user.last_name);
        return name.length() != 0 || user.phone == null || user.phone.length() == 0 ? name : ""/*PhoneFormat.getInstance().format("+" + user.phone)*/;
    }

    public static String getFirstName(TLRPC.User user) {
        if (user == null || isDeleted(user)) {
            return "DELETED";
        }
        String name = user.first_name;
        if (name == null || name.length() == 0) {
            name = user.last_name;
        }
        return name != null && name.length() > 0 ? name : "ErrFirstName";
    }
}
