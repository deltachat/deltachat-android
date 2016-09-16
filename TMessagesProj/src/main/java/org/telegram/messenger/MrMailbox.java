package org.telegram.messenger;

/**
 * Created by bpetersen on 14.09.16.
 */
public class MrMailbox {

    // work with mailboxes
    public native static int     MrMailboxNew         (); // returns hMailbox (Names as mrmailbox_new don't work due to the additional underscore)
    public native static void    MrMailboxDelete      (int hMailbox);
    public native static int     MrMailboxOpen        (int hMailbox, String dbfile);
    public native static void    MrMailboxClose       (int hMailbox);
    public native static int     MrMailboxGetChats    (int hMailbox); // returns hChatlist

    // working with chatlists
    public native static int     MrChatlistGetCnt     (int hChatlist);
    public native static int     MrChatlistGetChat    (int hChatlist, int index); // returns hChat

    // working with chats
    public native static int     MrChatGetId          (int hChat);
    public native static int     MrChatGetType        (int hChat);
    public native static String  MrChatCetName        (int hChat);
}
