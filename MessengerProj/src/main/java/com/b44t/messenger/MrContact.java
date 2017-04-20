/*******************************************************************************
 *
 *                          Messenger Android Frontend
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
 * File:    MrContact.java
 * Purpose: Wrap around mrcontact_t
 *
 ******************************************************************************/


package com.b44t.messenger;


public class MrContact {

    public final static int MR_CONTACT_ID_SELF = 1;
    public final static int MR_CONTACT_ID_LAST_SPECIAL = 9;

    public MrContact(long hContact) {
        m_hContact = hContact;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        MrContactUnref(m_hContact);
    }

    public String getName() {
        return MrContactGetName(m_hContact);
    }
    public native String getAuthName();

    public String getAddr() {
        return MrContactGetAddr(m_hContact);
    }

    public int isBlocked() {
        return MrContactIsBlocked(m_hContact);
    }

    private long                  m_hContact;
    private native static void    MrContactUnref             (long hContact);
    private native static String  MrContactGetName           (long hContact);
    private native static String  MrContactGetAddr           (long hContact);
    private native static int     MrContactIsBlocked         (long hContact);


    /* additional functions that are not 1:1 available in the backend
     **********************************************************************************************/

    public static TLRPC.User contactId2user(int id)
    {
        TLRPC.User ret = new TLRPC.User();
        ret.id = id;
        return ret;
    }

    public String getDisplayName() {
        String s=MrContactGetName(m_hContact);
        if(s.isEmpty()) {
            s=MrContactGetAddr(m_hContact);
        }
        return s;
    }

    public String getNameNAddr() {
        String s=MrContactGetName(m_hContact);
        if(s.isEmpty()) {
            s=MrContactGetAddr(m_hContact);
        }
        else {
            s+=" ("+MrContactGetAddr(m_hContact)+")";
        }
        return s;
    }
}
