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


package com.b44t.messenger.Cells;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.ContactsController;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MrContact;
import com.b44t.messenger.R;
import com.b44t.messenger.Components.AvatarDrawable;
import com.b44t.messenger.Components.BackupImageView;
import com.b44t.messenger.Components.CheckBoxView;
import com.b44t.messenger.Components.LayoutHelper;
import com.b44t.messenger.ActionBar.SimpleTextView;

public class UserCell extends FrameLayout {

    private BackupImageView avatarImageView;
    private SimpleTextView nameTextView;
    private SimpleTextView statusTextView;
    private ImageView imageView;
    private CheckBoxView checkBox;

    private AvatarDrawable avatarDrawable;
    private MrContact m_mrContact;

    private CharSequence currentName;
    private CharSequence currentStatus;
    private int          currentResId;

    private int statusColor = 0xffa8a8a8;

    public UserCell(Context context, int padding, int useCheckboxes) {
        super(context);

        avatarDrawable = new AvatarDrawable();

        avatarImageView = new BackupImageView(context);
        avatarImageView.setRoundRadius(AndroidUtilities.dp(24));
        addView(avatarImageView, LayoutHelper.createFrame(48, 48, Gravity.START | Gravity.TOP, LocaleController.isRTL ? 0 : 7 + padding, 8, LocaleController.isRTL ? 7 + padding : 0, 0));

        nameTextView = new SimpleTextView(context);
        nameTextView.setTextColor(0xff212121);
        nameTextView.setTextSize(16); /*same size as in TextCell */
        nameTextView.setGravity(Gravity.START | Gravity.TOP);
        addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 20, Gravity.START | Gravity.TOP, LocaleController.isRTL ? 28 + (useCheckboxes == 2 ? 18 : 0) : (68 + padding), 11.5f, LocaleController.isRTL ? (68 + padding) : 28 + (useCheckboxes == 2 ? 18 : 0), 0));

        statusTextView = new SimpleTextView(context);
        statusTextView.setTextSize(14);
        statusTextView.setGravity(Gravity.START | Gravity.TOP);
        addView(statusTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 20, Gravity.START | Gravity.TOP, LocaleController.isRTL ? 28 : (68 + padding), 34.5f, LocaleController.isRTL ? (68 + padding) : 28, 0));

        imageView = new ImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setVisibility(GONE);
        addView(imageView, LayoutHelper.createFrame(LayoutParams.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL, LocaleController.isRTL ? 0 : 16, 0, LocaleController.isRTL ? 16 : 0, 0));

        if( useCheckboxes == 1 ) {
            checkBox = new CheckBoxView(context, R.drawable.round_check2);
            checkBox.setVisibility(INVISIBLE);
            addView(checkBox, LayoutHelper.createFrame(22, 22, Gravity.START | Gravity.TOP, LocaleController.isRTL ? 0 : 37 + padding, 38, LocaleController.isRTL ? 37 + padding : 0, 0));
        }
    }

    public void setData(MrContact mrContact, int resId) {
        m_mrContact = mrContact;
        if( m_mrContact != null ) {
            currentName = m_mrContact.getDisplayName();
            currentStatus = m_mrContact.getAddr();
        }
        currentResId = resId;
        update();
    }

    public void setChecked(boolean checked, boolean animated) {
        if( checkBox!=null ) {
            if (checkBox.getVisibility() != VISIBLE) {
                checkBox.setVisibility(VISIBLE);
            }
            checkBox.setChecked(checked, animated);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY));
    }

    public void setStatusColors(int color) {
        statusColor = color;
    }

    public void update() {
        if (m_mrContact==null) {
            return;
        }

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

        ContactsController.setupAvatar(avatarImageView, avatarImageView.imageReceiver, avatarDrawable, m_mrContact, null);
        avatarImageView.setVerifiedDrawable(m_mrContact.isVerified());
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
