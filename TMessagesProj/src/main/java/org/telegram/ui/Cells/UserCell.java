/*
 * This part of the Delta Chat fronted is based on Telegram which is covered by the note below.
 *
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.ui.Cells;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CheckBox;
import org.telegram.ui.Components.CheckBoxSquare;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.ActionBar.SimpleTextView;

public class UserCell extends FrameLayout {

    private BackupImageView avatarImageView;
    private SimpleTextView nameTextView;
    private SimpleTextView statusTextView;
    private ImageView imageView;
    private CheckBox checkBox;
    private CheckBoxSquare checkBoxBig;

    private AvatarDrawable avatarDrawable;
    private int user_id;
    private int chat_id;

    private CharSequence currentName;
    private CharSequence currentStatus;
    private int          currentResId;

    private int statusColor = 0xffa8a8a8;

    public UserCell(Context context, int padding, int checkbox) {
        super(context);

        avatarDrawable = new AvatarDrawable();

        avatarImageView = new BackupImageView(context);
        avatarImageView.setRoundRadius(AndroidUtilities.dp(24));
        addView(avatarImageView, LayoutHelper.createFrame(48, 48, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 0 : 7 + padding, 8, LocaleController.isRTL ? 7 + padding : 0, 0));

        nameTextView = new SimpleTextView(context);
        nameTextView.setTextColor(0xff212121);
        nameTextView.setTextSize(16); /*same size as in TextCell */
        nameTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
        addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 20, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 28 + (checkbox == 2 ? 18 : 0) : (68 + padding), 11.5f, LocaleController.isRTL ? (68 + padding) : 28 + (checkbox == 2 ? 18 : 0), 0));

        statusTextView = new SimpleTextView(context);
        statusTextView.setTextSize(14);
        statusTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
        addView(statusTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 20, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 28 : (68 + padding), 34.5f, LocaleController.isRTL ? (68 + padding) : 28, 0));

        imageView = new ImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setVisibility(GONE);
        addView(imageView, LayoutHelper.createFrame(LayoutParams.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL, LocaleController.isRTL ? 0 : 16, 0, LocaleController.isRTL ? 16 : 0, 0));

        if (checkbox == 2) {
            checkBoxBig = new CheckBoxSquare(context);
            addView(checkBoxBig, LayoutHelper.createFrame(18, 18, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL, LocaleController.isRTL ? 19 : 0, 0, LocaleController.isRTL ? 0 : 19, 0));
        } else if (checkbox == 1) {
            checkBox = new CheckBox(context, R.drawable.round_check2);
            checkBox.setVisibility(INVISIBLE);
            addView(checkBox, LayoutHelper.createFrame(22, 22, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 0 : 37 + padding, 38, LocaleController.isRTL ? 37 + padding : 0, 0));
        }
    }

    public void setData(int new_user_id, int new_chat_id, CharSequence name, CharSequence status, int resId) {
        currentStatus = null;
        currentStatus = status;
        currentName = name;
        user_id = new_user_id;
        chat_id = new_chat_id;
        currentResId = resId;
        update(0);
    }

    public void setChecked(boolean checked, boolean animated) {
        if (checkBox != null) {
            if (checkBox.getVisibility() != VISIBLE) {
                checkBox.setVisibility(VISIBLE);
            }
            checkBox.setChecked(checked, animated);
        } else if (checkBoxBig != null) {
            if (checkBoxBig.getVisibility() != VISIBLE) {
                checkBoxBig.setVisibility(VISIBLE);
            }
            checkBoxBig.setChecked(checked, animated);
        }
    }

    public void setCheckDisabled(boolean disabled) {
        if (checkBoxBig != null) {
            checkBoxBig.setDisabled(disabled);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY));
    }

    public void setStatusColors(int color) {
        statusColor = color;
    }

    public void update(int mask) {
        if (user_id == 0 && chat_id==0) {
            return;
        }
        TLRPC.FileLocation photo = null;
        //String newName = null;

        /*
        if (mask != 0) {
            boolean continueUpdate = false;
            if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0) {
                if (lastAvatar != null && photo == null || lastAvatar == null && photo != null && lastAvatar != null && photo != null && (lastAvatar.volume_id != photo.volume_id || lastAvatar.local_id != photo.local_id)) {
                    continueUpdate = true;
                }
            }

            if (user_id != 0 && !continueUpdate && (mask & MessagesController.UPDATE_MASK_STATUS) != 0) {
                int newStatus = 0;
                if (currentUser.status != null) {
                    newStatus = currentUser.status.expires;
                }
                if (newStatus != lastStatus) {
                    continueUpdate = true;
                }
            }

            if (!continueUpdate && currentName == null && lastName != null && (mask & MessagesController.UPDATE_MASK_NAME) != 0) {
                if (currentUser != null) {
                    newName = UserObject.getUserName(currentUser);
                } else {
                    newName = currentChat.title;
                }
                if (!newName.equals(lastName)) {
                    continueUpdate = true;
                }
            }
            if (!continueUpdate) {
                return;
            }
        }
        */

        /*
        if (currentUser != null) {
            avatarDrawable.setInfoByUser(currentUser);
            if (currentUser.status != null) {
                lastStatus = currentUser.status.expires;
            } else {
                lastStatus = 0;
            }
        } else {
            avatarDrawable.setInfoByChat(currentChat);
        }
        */

        if (currentName != null) {
            nameTextView.setText(currentName);
        }

        if (currentStatus != null) {
            statusTextView.setTextColor(statusColor);
            statusTextView.setText(currentStatus);
        }

        if (imageView.getVisibility() == VISIBLE && currentResId == 0 || imageView.getVisibility() == GONE && currentResId != 0) {
            imageView.setVisibility(currentResId == 0 ? GONE : VISIBLE);
            imageView.setImageResource(currentResId);
        }

        if( currentName != null ) {
            avatarDrawable.setInfoByName(currentName.toString());
            avatarImageView.setImage(photo, "50_50", avatarDrawable);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
