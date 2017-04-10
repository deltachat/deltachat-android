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


package com.b44t.ui.Adapters;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.b44t.messenger.NotificationCenter;
import com.b44t.messenger.Utilities;
import com.b44t.messenger.support.widget.RecyclerView;
import com.b44t.messenger.FileLoader;
import com.b44t.messenger.TLRPC;
import com.b44t.ui.Cells.StickerCell;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class StickersAdapter extends RecyclerView.Adapter implements NotificationCenter.NotificationCenterDelegate {

    private Context mContext;
    private ArrayList<TLRPC.Document> stickers;
    private ArrayList<String> stickersToLoad = new ArrayList<>();
    private StickersAdapterDelegate delegate;
    private String lastSticker;
    private boolean visible;
    private ArrayList<Long> newRecentStickers = new ArrayList<>();
    private long recentLoadDate;

    public interface StickersAdapterDelegate {
        void needChangePanelVisibility(boolean show);
    }

    private class Holder extends RecyclerView.ViewHolder {

        public Holder(View itemView) {
            super(itemView);
        }
    }

    public StickersAdapter(Context context, StickersAdapterDelegate delegate) {
        mContext = context;
        this.delegate = delegate;
        //StickersQuery.checkStickers();
    }

    public void onDestroy() {
    }

    @Override
    public void didReceivedNotification(int id, final Object... args) {

    }

    @Override
    public int getItemCount() {
        return stickers != null ? stickers.size() : 0;
    }

    public TLRPC.Document getItem(int i) {
        return stickers != null && i >= 0 && i < stickers.size() ? stickers.get(i) : null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        StickerCell view = new StickerCell(mContext);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        int side = 0;
        if (i == 0) {
            if (stickers.size() == 1) {
                side = 2;
            } else {
                side = -1;
            }
        } else if (i == stickers.size() - 1) {
            side = 1;
        }
        ((StickerCell) viewHolder.itemView).setSticker(stickers.get(i), side);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // was: StickersQuery

    public static TLRPC.Document getStickerById(long id) {
        return null;
    }

    public static TLRPC.TL_messages_stickerSet getStickerSetByName(String name) {
        return null;
    }

    public static TLRPC.TL_messages_stickerSet getStickerSetById(Long id) {
        return null;
    }

    public static HashMap<String, ArrayList<TLRPC.Document>> getAllStickers() {
        return null;
    }

    public static ArrayList<TLRPC.TL_messages_stickerSet> getStickerSets() {
        return new ArrayList<>();
    }

    public static boolean isStickerPackInstalled(long id) {
        return false;
    }
}
