/*
 * Copyright (C) 2013-2017 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.contacts;

public class ContactsCursorLoader {
  public static final class DisplayMode {
    public static final int FLAG_PUSH   = 1;
    public static final int FLAG_SMS    = 1 << 1;
    public static final int FLAG_GROUPS = 1 << 2;
    public static final int FLAG_ALL    = FLAG_PUSH | FLAG_SMS | FLAG_GROUPS;
  }
}
