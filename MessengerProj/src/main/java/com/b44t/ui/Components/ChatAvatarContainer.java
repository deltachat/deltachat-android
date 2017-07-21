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


package com.b44t.ui.Components;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.ContactsController;
import com.b44t.messenger.MrChat;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.TLRPC;
import com.b44t.ui.ActionBar.ActionBar;
import com.b44t.ui.ActionBar.SimpleTextView;
import com.b44t.ui.ActionBar.Theme;
import com.b44t.ui.ChatActivity;
import com.b44t.ui.ProfileActivity;

public class ChatAvatarContainer extends FrameLayout {

    private BackupImageView avatarImageView;
    private SimpleTextView titleTextView;
    private SimpleTextView subtitleTextView;
    private ChatActivity parentFragment;
    private AvatarDrawable avatarDrawable = new AvatarDrawable();

    public ChatAvatarContainer(Context context, ChatActivity chatActivity /*, boolean needTime*/) {
        super(context);
        parentFragment = chatActivity;

        avatarImageView = new BackupImageView(context);
        avatarImageView.setRoundRadius(AndroidUtilities.dp(21));
        addView(avatarImageView);

        titleTextView = new SimpleTextView(context);
        titleTextView.setTextColor(Theme.ACTION_BAR_TITLE_COLOR);
        titleTextView.setTextSize(18);
        titleTextView.setGravity(Gravity.START);
        titleTextView.setLeftDrawableTopPadding(-AndroidUtilities.dp(1.3f));
        titleTextView.setRightDrawableTopPadding(-AndroidUtilities.dp(1.3f));
        addView(titleTextView);

        subtitleTextView = new SimpleTextView(context);
        subtitleTextView.setTextColor(Theme.ACTION_BAR_SUBTITLE_COLOR);
        subtitleTextView.setTextSize(Theme.ACTION_BAR_SUBTITLE_TEXT_SIZE);
        subtitleTextView.setGravity(Gravity.START);
        addView(subtitleTextView);

        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                if( parentFragment.m_mrChat.getType()== MrChat.MR_CHAT_GROUP ) {
                    args.putInt("chat_id",  parentFragment.m_mrChat.getId());
                }
                else {
                    int[] contact_ids = MrMailbox.getChatContacts(parentFragment.m_mrChat.getId());
                    if( contact_ids.length==0) {
                        return; // should not happen
                    }
                    args.putInt("user_id", contact_ids[0]);
                }

                ProfileActivity fragment = new ProfileActivity(args);
                parentFragment.presentFragment(fragment);
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int availableWidth = width - AndroidUtilities.dp(54 + 16);
        avatarImageView.measure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(42), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(42), MeasureSpec.EXACTLY));
        titleTextView.measure(MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(24), MeasureSpec.AT_MOST));
        subtitleTextView.measure(MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(20), MeasureSpec.AT_MOST));
        setMeasuredDimension(width, MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int actionBarHeight = ActionBar.getCurrentActionBarHeight();
        int viewTop = (actionBarHeight - AndroidUtilities.dp(42)) / 2 + (Build.VERSION.SDK_INT >= 21 ? AndroidUtilities.statusBarHeight : 0);
        avatarImageView.layout(AndroidUtilities.dp(8), viewTop, AndroidUtilities.dp(42 + 8), viewTop + AndroidUtilities.dp(42));
        titleTextView.layout(AndroidUtilities.dp(8 + 54), viewTop + AndroidUtilities.dp(1.3f), AndroidUtilities.dp(8 + 54) + titleTextView.getMeasuredWidth(), viewTop + titleTextView.getTextHeight() + AndroidUtilities.dp(1.3f));
        subtitleTextView.layout(AndroidUtilities.dp(8 + 54), viewTop + AndroidUtilities.dp(24), AndroidUtilities.dp(8 + 54) + subtitleTextView.getMeasuredWidth(), viewTop + subtitleTextView.getTextHeight() + AndroidUtilities.dp(24));
    }

    public void setTitleIcons(int leftIcon, int rightIcon) {
        titleTextView.setLeftDrawable(leftIcon);
        titleTextView.setRightDrawable(rightIcon);
    }

    public void setTitle(CharSequence value) {
        titleTextView.setText(value);
    }

    public void updateSubtitle() {
        String text = parentFragment.m_mrChat.getSubtitle(); // EDIT BY MR
        subtitleTextView.setText(text); // EDIT BY MR
    }

    public void checkAndUpdateAvatar() {
        if (avatarImageView != null) {
            ContactsController.setupAvatar(avatarImageView, avatarImageView.imageReceiver, avatarDrawable, null, parentFragment.m_mrChat);
        }
    }
}
