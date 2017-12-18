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
        m_hContact = 0;
    }

    public native String getName();
    public native String getDisplayName();
    public native String getAddr();
    public native String getNameNAddr();
    public native boolean isBlocked();

    private long                  m_hContact;
    private native static void    MrContactUnref             (long hContact);
}
