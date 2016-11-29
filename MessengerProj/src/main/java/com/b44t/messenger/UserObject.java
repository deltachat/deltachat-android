/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.b44t.messenger;

public class UserObject {

    public static boolean isDeleted(TLRPC.User user) {
        return user == null || user.deleted;
    }

    public static boolean isUserSelf(TLRPC.User user) {
        return user.self;
    }

    public static String getUserName(TLRPC.User user) {
        return "ErrName"; // use MrContact.getName() instead
    }
}
