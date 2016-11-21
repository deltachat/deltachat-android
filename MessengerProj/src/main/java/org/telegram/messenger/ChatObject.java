/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;

public class ChatObject {

    public static final int CHAT_TYPE_CHAT = 0;

    public static boolean isNotInChat(TLRPC.Chat chat) {
        return chat == null || chat instanceof TLRPC.TL_chatEmpty || chat.left || chat.kicked || chat.deactivated;
    }

    public static boolean isChannel(TLRPC.Chat chat) {
        return false;
    }

    public static boolean isChannel(int chatId) {
        return false;
    }

    public static boolean canWriteToChat(TLRPC.Chat chat) {
        return true;
    }
}
