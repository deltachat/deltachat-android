/*******************************************************************************
 *
 *                              Delta Chat Android
 *                        (C) 2013-2016 Nikolai Kudashov
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


package com.b44t.messenger.Components;

import android.graphics.Path;
import android.text.StaticLayout;

public class LinkPath extends Path {

    private StaticLayout currentLayout;
    private int currentLine;
    private float lastTop = -1;
    private float heightOffset;

    public void setCurrentLayout(StaticLayout layout, int start, float yOffset) {
        currentLayout = layout;
        currentLine = layout.getLineForOffset(start);
        lastTop = -1;
        heightOffset = yOffset;
    }

    @Override
    public void addRect(float left, float top, float right, float bottom, Direction dir) {
        top += heightOffset;
        bottom += heightOffset;
        if (lastTop == -1) {
            lastTop = top;
        } else if (lastTop != top) {
            lastTop = top;
            currentLine++;
        }
        float lineRight = currentLayout.getLineRight(currentLine);
        float lineLeft = currentLayout.getLineLeft(currentLine);
        if (left >= lineRight) {
            return;
        }
        if (right > lineRight) {
            right = lineRight;
        }
        if (left < lineLeft) {
            left = lineLeft;
        }
        super.addRect(left, top, right, bottom, dir);
    }
}
