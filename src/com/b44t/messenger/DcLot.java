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
 ******************************************************************************/


package com.b44t.messenger;


public class DcLot {

    public final static int DC_TEXT1_DRAFT = 1;
    public final static int DC_TEXT1_USERNAME = 2;
    public final static int DC_TEXT1_SELF = 3;

    public DcLot(long hLot) {
        m_hLot = hLot;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        DcLotUnref(m_hLot);
        m_hLot = 0;
    }

    public native String getText1();
    public native int getText1Meaning();
    public native String getText2();
    public native long getTimestamp();
    public native int getState();
    public native int getId();

    // working with raw c-data
    private long m_hLot; // must not be renamed as referenced by JNI
    private native static void DcLotUnref(long hLot);
}
