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


package com.b44t.messenger.ActionBar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.R;
import com.b44t.messenger.Components.ChatAttachAlert;

public class Theme {

    public static final int ACTION_BAR_COLOR = 0xff415e6b; // also used for the notification icon background and as pincode-default-background color, TODO: let player_big_notification/player_small_notification use this color automatically
    public static final int ACTION_BAR_PHOTO_VIEWER_COLOR = 0x7f000000;
    public static final int ACTION_BAR_MEDIA_PICKER_COLOR = 0xff333333;
    public static final int ACTION_BAR_TITLE_COLOR = 0xffffffff;
    public static final int ACTION_BAR_SUBTITLE_COLOR = 0xffd5e8f7;
    public static final int ACTION_BAR_ACTION_MODE_TEXT_COLOR = 0xff737373;
    public static final int ACTION_BAR_SELECTOR_COLOR = 0xff406d94;
    public static final int ACTION_BAR_SUBTITLE_TEXT_SIZE = 14;

    public static final int INPUT_FIELD_SELECTOR_COLOR = 0xffd6d6d6;
    public static final int ACTION_BAR_PICKER_SELECTOR_COLOR = 0xff3d3d3d;
    public static final int ACTION_BAR_WHITE_SELECTOR_COLOR = 0x40ffffff;
    public static final int ACTION_BAR_AUDIO_SELECTOR_COLOR = 0x2f000000;
    public static final int ACTION_BAR_MODE_SELECTOR_COLOR = 0xfff0f0f0;

    public static final int ATTACH_SHEET_TEXT_COLOR = 0xff757575;

    public static final int CHATLIST_BACKGROUND_COLOR = 0xffffffff;
    public static final int CHATLIST_DEADDROP_BACKGROUND_COLOR = 0xffd3d3d3;
    public static final int CHATLIST_TITLE_TEXT_COLOR = 0xff000000;
    public static final int CHATLIST_MESSAGE_TEXT_COLOR = 0xff8f8f8f;
    public static final int CHATLIST_NAME_TEXT_COLOR = 0xff555555;
    public static final int CHATLIST_SELF_TEXT_COLOR = 0xff00a60e; // from encrypted chat title: 0xff00a60e, alternative: 0xff3c912e
    public static final int CHATLIST_DRAFT_TEXT_COLOR = 0xffdd4b39;

    public static final int CHAT_BOTTOM_OVERLAY_TEXT_COLOR = 0xff7f7f7f;
    public static final int CHAT_EMPTY_VIEW_TEXT_COLOR = 0xffffffff;

    public static final int MSG_SELECTED_BACKGROUND_COLOR = 0x6633b5e5;
    public static final int MSG_STICKER_NAME_TEXT_COLOR = 0xffffffff;
    public static final int MSG_IN_TIME_N_FWD_TEXT_COLOR = 0xff9ea7b0;
    public static final int MSG_OUT_TIME_TEXT_COLOR = 0xff70b15c;
    public static final int MSG_MEDIA_TIME_TEXT_COLOR = 0xffffffff;
    public static final int MSG_IN_FILE_BACKGROUND_COLOR = 0xffebf0f5;
    public static final int MSG_OUT_FILE_BACKGROUND_COLOR = 0xffdaf5c3;
    public static final int MSG_IN_FILE_BACKGROUND_SELECTED_COLOR = 0xffcbeaf6;
    public static final int MSG_OUT_FILE_BACKGROUND_SELECTED_COLOR = 0xffc5eca7;
    public static final int MSG_MEDIA_INFO_TEXT_COLOR = 0xffffffff;
    public static final int MSG_TEXT_COLOR = 0xff000000;
    public static final int MSG_LINK_TEXT_COLOR = 0xff2678b6;
    public static final int MSG_LINK_SELECT_BACKGROUND_COLOR = 0x3362a9e3;
    public static final int MSG_TEXT_SELECT_BACKGROUND_COLOR = 0x6662a9e3;
    public static final int MSG_DOC_NAME_COLOR = 0xFF3093e0; // similar color as the file-icon

    public static final int MSG_AUDIO_NAME_COLOR = 0xFFed824e; // similar color as the audio-icon
    public static final int MSG_AUDIO_SEEKBAR_DARK_COLOR = 0xFFf68751; // same color as in audio-icon, also used for the record-button
    public static final int MSG_AUDIO_SEEKBAR_LITE_COLOR = 0xFFfbc8af;

    public static final int MSG_SYSTEM_CMD_COLOR = MSG_AUDIO_NAME_COLOR; // just the same color, there is no other dependency between system commands and audio messages

    public static Drawable backgroundDrawableIn;
    public static Drawable backgroundDrawableInSelected;
    public static Drawable backgroundDrawableOut;
    public static Drawable backgroundDrawableOutSelected;
    public static Drawable checkDrawable;
    public static Drawable halfCheckDrawable;
    public static Drawable clockDrawable;
    public static Drawable checkMediaDrawable;
    public static Drawable halfCheckMediaDrawable;
    public static Drawable clockMediaDrawable;
    public static Drawable errorDrawable;
    public static Drawable systemDrawable;
    public static Drawable timeBackgroundDrawable;
    public static Drawable timeStickerBackgroundDrawable;
    public static Drawable encrOutDrawable;
    public static Drawable encrInDrawable;
    public static Drawable encrMediaDrawable;

    public static Drawable[] cornerOuter = new Drawable[4];
    public static Drawable[] cornerInner = new Drawable[4];

    public static Drawable newchatIconDrawable;

    public static final int INLIST_FILE        = 0;
    public static final int INLIST_PLAY        = 1;
    public static final int INLIST_PAUSE       = 2;
    public static final int INLIST_TRANSP_PLAY = 3;
    public static final int INLIST_COUNT       = 4;
    public static Drawable[] inlistDrawable = new Drawable[INLIST_COUNT];

    public static PorterDuffColorFilter colorFilter;
    private static int currentColor;

    public static Drawable attachButtonDrawables[] = new Drawable[8];

    private static Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public static void loadRecources(Context context) {
        if (backgroundDrawableIn == null) {
            backgroundDrawableIn = context.getResources().getDrawable(R.drawable.msg_in);
            backgroundDrawableInSelected = context.getResources().getDrawable(R.drawable.msg_in_selected);
            backgroundDrawableOut = context.getResources().getDrawable(R.drawable.msg_out);
            backgroundDrawableOutSelected = context.getResources().getDrawable(R.drawable.msg_out_selected);
            checkDrawable = context.getResources().getDrawable(R.drawable.msg_check);
            halfCheckDrawable = context.getResources().getDrawable(R.drawable.msg_halfcheck);
            clockDrawable = context.getResources().getDrawable(R.drawable.msg_clock);
            checkMediaDrawable = context.getResources().getDrawable(R.drawable.msg_check_w);
            halfCheckMediaDrawable = context.getResources().getDrawable(R.drawable.msg_halfcheck_w);
            clockMediaDrawable = context.getResources().getDrawable(R.drawable.msg_clock_photo);
            errorDrawable = context.getResources().getDrawable(R.drawable.msg_warning);
            timeBackgroundDrawable = context.getResources().getDrawable(R.drawable.phototime2_b);
            timeStickerBackgroundDrawable = context.getResources().getDrawable(R.drawable.phototime2);
            systemDrawable = context.getResources().getDrawable(R.drawable.system);

            encrInDrawable = context.getResources().getDrawable(R.drawable.msg_encr_in);
            encrOutDrawable = context.getResources().getDrawable(R.drawable.msg_encr_out);
            encrMediaDrawable = context.getResources().getDrawable(R.drawable.msg_encr_media);

            inlistDrawable[INLIST_FILE]        = context.getResources().getDrawable(R.drawable.attach_file_inlist);
            inlistDrawable[INLIST_PLAY]        = context.getResources().getDrawable(R.drawable.attach_audio_inlist_play);
            inlistDrawable[INLIST_TRANSP_PLAY] = context.getResources().getDrawable(R.drawable.video_play);
            inlistDrawable[INLIST_PAUSE]       = context.getResources().getDrawable(R.drawable.attach_audio_inlist_pause);

            newchatIconDrawable = context.getResources().getDrawable(R.drawable.ic_ab_reply);

            cornerOuter[0] = context.getResources().getDrawable(R.drawable.corner_out_tl);
            cornerOuter[1] = context.getResources().getDrawable(R.drawable.corner_out_tr);
            cornerOuter[2] = context.getResources().getDrawable(R.drawable.corner_out_br);
            cornerOuter[3] = context.getResources().getDrawable(R.drawable.corner_out_bl);

            cornerInner[0] = context.getResources().getDrawable(R.drawable.corner_in_tr);
            cornerInner[1] = context.getResources().getDrawable(R.drawable.corner_in_tl);
            cornerInner[2] = context.getResources().getDrawable(R.drawable.corner_in_br);
            cornerInner[3] = context.getResources().getDrawable(R.drawable.corner_in_bl);
        }

        int color = ApplicationLoader.getServiceMessageColor();
        if (currentColor != color) {
            colorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY);
            currentColor = color;
            for (int a = 0; a < 4; a++) {
                cornerOuter[a].setColorFilter(colorFilter);
                cornerInner[a].setColorFilter(colorFilter);
            }
            timeStickerBackgroundDrawable.setColorFilter(colorFilter);
        }
    }

    public static void loadChatResources(Context context) {
        if (attachButtonDrawables[0] == null) {
            attachButtonDrawables[ChatAttachAlert.ATTACH_BUTTON_IDX_CAMERA]      = context.getResources().getDrawable(R.drawable.attach_camera_states);
            attachButtonDrawables[ChatAttachAlert.ATTACH_BUTTON_IDX_GALLERY]     = context.getResources().getDrawable(R.drawable.attach_gallery_states);
            attachButtonDrawables[ChatAttachAlert.ATTACH_BUTTON_IDX_VIDEO]       = context.getResources().getDrawable(R.drawable.attach_video_states);
            attachButtonDrawables[ChatAttachAlert.ATTACH_BUTTON_IDX_MUSIC]       = context.getResources().getDrawable(R.drawable.attach_audio_states);
            attachButtonDrawables[ChatAttachAlert.ATTACH_BUTTON_IDX_FILE]        = context.getResources().getDrawable(R.drawable.attach_file_states);
            attachButtonDrawables[ChatAttachAlert.ATTACH_BUTTON_IDX_CONTACT]     = context.getResources().getDrawable(R.drawable.attach_contact_states);
            attachButtonDrawables[ChatAttachAlert.ATTACH_BUTTON_IDX_EMPTY]       = context.getResources().getDrawable(R.drawable.attach_empty_states);
            attachButtonDrawables[ChatAttachAlert.ATTACH_BUTTON_IDX_SENDSELECTED]= context.getResources().getDrawable(R.drawable.attach_hide_states);
        }
    }

    public static Drawable createBarSelectorDrawable(int color) {
        return createBarSelectorDrawable(color, true);
    }

    public static Drawable createBarSelectorDrawable(int color, boolean masked) {
        if (Build.VERSION.SDK_INT >= 21) {
            Drawable maskDrawable = null;
            if (masked) {
                maskPaint.setColor(0xffffffff);
                maskDrawable = new Drawable() {
                    @Override
                    public void draw(Canvas canvas) {
                        android.graphics.Rect bounds = getBounds();
                        canvas.drawCircle(bounds.centerX(), bounds.centerY(), AndroidUtilities.dp(18), maskPaint);
                    }

                    @Override
                    public void setAlpha(int alpha) {

                    }

                    @Override
                    public void setColorFilter(ColorFilter colorFilter) {

                    }

                    @Override
                    public int getOpacity() {
                        return 0;
                    }
                };
            }
            ColorStateList colorStateList = new ColorStateList(
                    new int[][]{new int[]{}},
                    new int[]{color}
            );
            return new RippleDrawable(colorStateList, null, maskDrawable);
        } else {
            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(color));
            stateListDrawable.addState(new int[]{android.R.attr.state_focused}, new ColorDrawable(color));
            stateListDrawable.addState(new int[]{android.R.attr.state_selected}, new ColorDrawable(color));
            stateListDrawable.addState(new int[]{android.R.attr.state_activated}, new ColorDrawable(color));
            stateListDrawable.addState(new int[]{}, new ColorDrawable(0x00000000));
            return stateListDrawable;
        }
    }
}
