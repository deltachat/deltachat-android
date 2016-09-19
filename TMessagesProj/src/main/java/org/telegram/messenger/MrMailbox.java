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
 * File:    MrMailbox.java
 * Authors: Björn Petersen
 * Purpose: The Java part of the Java<->C Wrapper, see also mr_wrapper.c
 *
 ******************************************************************************/


package org.telegram.messenger;


public class MrMailbox {

    int dummy = 3; // just a counter that can be increased to force recompiling

    // work with mailboxes
    public native static long    MrMailboxNew         (); // returns hMailbox (Names as mrmailbox_new don't work due to the additional underscore)
    public native static void    MrMailboxDelete      (long hMailbox);
    public native static int     MrMailboxOpen        (long hMailbox, String dbfile);
    public native static void    MrMailboxClose       (long hMailbox);
    public native static long    MrMailboxGetChats    (long hMailbox); // returns hChatlist

    // working with chatlists
    public native static int     MrChatlistGetCnt     (long hChatlist);
    public native static int     MrChatlistGetChat    (long hChatlist, int index); // returns hChat

    // working with chats
    public native static int     MrChatGetId          (long hChat);
    public native static int     MrChatGetType        (long hChat);
    public native static String  MrChatCetName        (long hChat);
}
