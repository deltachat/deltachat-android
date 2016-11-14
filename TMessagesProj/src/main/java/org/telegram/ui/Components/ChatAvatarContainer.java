/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.ui.Components;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MrMailbox;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.ProfileActivity;

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
        titleTextView.setGravity(Gravity.LEFT);
        titleTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleTextView.setLeftDrawableTopPadding(-AndroidUtilities.dp(1.3f));
        titleTextView.setRightDrawableTopPadding(-AndroidUtilities.dp(1.3f));
        addView(titleTextView);

        subtitleTextView = new SimpleTextView(context);
        subtitleTextView.setTextColor(Theme.ACTION_BAR_SUBTITLE_COLOR);
        subtitleTextView.setTextSize(14);
        subtitleTextView.setGravity(Gravity.LEFT);
        addView(subtitleTextView);

        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                boolean setChatInfo = false;
                if( MrMailbox.MrChatGetType(parentFragment.m_hChat)==MrMailbox.MR_CHAT_GROUP ) {
                    args.putInt("chat_id",  MrMailbox.MrChatGetId(parentFragment.m_hChat));
                    setChatInfo = true;
                }
                else {
                    int[] contact_ids = MrMailbox.MrMailboxGetChatContacts(MrMailbox.hMailbox, MrMailbox.MrChatGetId(parentFragment.m_hChat));
                    if( contact_ids.length==0) {
                        return; // should not happen
                    }
                    args.putInt("user_id", contact_ids[0]);
                }

                ProfileActivity fragment = new ProfileActivity(args);
                if( setChatInfo ) {
                    fragment.setChatInfo(parentFragment.getCurrentChatInfo());
                }
                fragment.setPlayProfileAnimation(true);
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
        String text = MrMailbox.MrChatGetSubtitle(parentFragment.m_hChat); // EDIT BY MR
        subtitleTextView.setText(text); // EDIT BY MR
    }

    public void checkAndUpdateAvatar() {
        TLRPC.FileLocation newPhoto = null;
        /* EDIT BY MR
        TLRPC.User user = parentFragment.getCurrentUser();
        TLRPC.Chat chat = parentFragment.getCurrentChat();
        if (user != null) {
            if (user.photo != null) {
                newPhoto = user.photo.photo_small;
            }
            avatarDrawable.setInfo(user);
        } else if (chat != null) {
            if (chat.photo != null) {
                newPhoto = chat.photo.photo_small;
            }
            avatarDrawable.setInfo(chat);
        }
        */

        // MrAvatar ...
        avatarDrawable.setInfoByName(MrMailbox.MrChatGetName(parentFragment.m_hChat));

        if (avatarImageView != null) {
            avatarImageView.setImage(newPhoto, "50_50", avatarDrawable);
        }
    }
}
