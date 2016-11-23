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
 * File:    MrPoortext.java
 * Authors: Björn Petersen
 * Purpose: Wrap around mrpoortext_t
 *
 ******************************************************************************/


package com.b44t.messenger;


public class MrPoortext {

    public final static int      MR_TITLE_NORMAL            = 0;
    public final static int      MR_TITLE_DRAFT             = 1;
    public final static int      MR_TITLE_USERNAME          = 2;
    public final static int      MR_TITLE_SELF              = 3;

    public MrPoortext(long hPoortext) {
        m_hPoortext = hPoortext;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        MrPoortextUnref(m_hPoortext);
    }

    public String getTitle() {
        return MrPoortextGetTitle(m_hPoortext);
    }

    public int getTitleMeaning() {
        return MrPoortextGetTitleMeaning(m_hPoortext);
    }

    public String getText() {
        return MrPoortextGetText(m_hPoortext);
    }

    public long getTimestamp() {
        return MrPoortextGetTimestamp(m_hPoortext);
    }

    public int getState() {
        return MrPoortextGetState(m_hPoortext);
    }

    private long                  m_hPoortext;
    private native static void    MrPoortextUnref            (long hPoortext);
    private native static String  MrPoortextGetTitle         (long hPoortext);
    private native static int     MrPoortextGetTitleMeaning  (long hPoortext);
    private native static String  MrPoortextGetText          (long hPoortext);
    private native static long    MrPoortextGetTimestamp     (long hPoortext);
    private native static int     MrPoortextGetState         (long hPoortext);
}
