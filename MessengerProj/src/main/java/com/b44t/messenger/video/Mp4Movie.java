/*******************************************************************************
 *
 *                          Messenger Android Frontend
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


package com.b44t.messenger.video;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;

import com.googlecode.mp4parser.util.Matrix;

import java.io.File;
import java.util.ArrayList;

@TargetApi(16)
public class Mp4Movie {
    private Matrix matrix = Matrix.ROTATE_0;
    private ArrayList<Track> tracks = new ArrayList<>();
    private File cacheFile;
    private int width;
    private int height;

    public Matrix getMatrix() {
        return matrix;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setCacheFile(File file) {
        cacheFile = file;
    }

    public void setRotation(int angle) {
        if (angle == 0) {
            matrix = Matrix.ROTATE_0;
        } else if (angle == 90) {
            matrix = Matrix.ROTATE_90;
        } else if (angle == 180) {
            matrix = Matrix.ROTATE_180;
        } else if (angle == 270) {
            matrix = Matrix.ROTATE_270;
        }
    }

    public void setSize(int w, int h) {
        width = w;
        height = h;
    }

    public ArrayList<Track> getTracks() {
        return tracks;
    }

    public File getCacheFile() {
        return cacheFile;
    }

    public void addSample(int trackIndex, long offset, MediaCodec.BufferInfo bufferInfo) throws Exception {
        if (trackIndex < 0 || trackIndex >= tracks.size()) {
            return;
        }
        Track track = tracks.get(trackIndex);
        track.addSample(offset, bufferInfo);
    }

    public int addTrack(MediaFormat mediaFormat, boolean isAudio) throws Exception {
        tracks.add(new Track(tracks.size(), mediaFormat, isAudio));
        return tracks.size() - 1;
    }
}
