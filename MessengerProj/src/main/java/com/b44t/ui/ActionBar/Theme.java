/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.b44t.ui.ActionBar;

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
import com.b44t.ui.Components.ChatAttachAlert;

public class Theme {

    public static final int ACTION_BAR_COLOR = 0xff415e6b;
    public static final int ACTION_BAR_PHOTO_VIEWER_COLOR = 0x7f000000;
    public static final int ACTION_BAR_MEDIA_PICKER_COLOR = 0xff333333;
    public static final int ACTION_BAR_PLAYER_COLOR = 0xffffffff;
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

    public static final int DIALOGS_TITLE_TEXT_COLOR = 0xff212121;
    public static final int DIALOGS_MESSAGE_TEXT_COLOR = 0xff8f8f8f;
    public static final int DIALOGS_NAME_TEXT_COLOR = 0xff555555;
    public static final int DIALOGS_SELF_TEXT_COLOR = 0xff00a60e; // from encrypted chat title: 0xff00a60e, alternative: 0xff3c912e
    public static final int DIALOGS_PRINTING_TEXT_COLOR = 0xff4d83b3;
    public static final int DIALOGS_DRAFT_TEXT_COLOR = 0xffdd4b39;

    public static final int CHAT_UNREAD_TEXT_COLOR = 0xff5695cc;
    public static final int CHAT_ADD_CONTACT_TEXT_COLOR = 0xff4a82b5;
    public static final int CHAT_REPORT_SPAM_TEXT_COLOR = 0xffcf5957;
    public static final int CHAT_BOTTOM_OVERLAY_TEXT_COLOR = 0xff7f7f7f;
    public static final int CHAT_GIF_HINT_TEXT_COLOR = 0xffffffff;
    public static final int CHAT_EMPTY_VIEW_TEXT_COLOR = 0xffffffff;
    public static final int CHAT_SEARCH_COUNT_TEXT_COLOR = 0xff4e9ad4;

    public static final int INAPP_PLAYER_PERFORMER_TEXT_COLOR = 0xff2f3438;
    public static final int INAPP_PLAYER_TITLE_TEXT_COLOR = 0xff2f3438;
    public static final int INAPP_PLAYER_BACKGROUND_COLOR = 0xffffffff;

    public static final int REPLY_PANEL_NAME_TEXT_COLOR = 0xff3a8ccf;
    public static final int REPLY_PANEL_MESSAGE_TEXT_COLOR = 0xff222222;

    public static final int ALERT_PANEL_NAME_TEXT_COLOR = 0xff3a8ccf;
    public static final int ALERT_PANEL_MESSAGE_TEXT_COLOR = 0xff999999;

    public static final int AUTODOWNLOAD_SHEET_SAVE_TEXT_COLOR = 0xff3a8ccf;

    public static final int STICKERS_SHEET_TITLE_TEXT_COLOR = 0xff212121;
    public static final int STICKERS_SHEET_SEND_TEXT_COLOR = 0xff3a8ccf;
    public static final int STICKERS_SHEET_ADD_TEXT_COLOR = 0xff3a8ccf;
    public static final int STICKERS_SHEET_CLOSE_TEXT_COLOR = 0xff3a8ccf;
    public static final int STICKERS_SHEET_REMOVE_TEXT_COLOR = 0xffcd5a5a;

    public static final int MSG_SELECTED_BACKGROUND_COLOR = 0x6633b5e5;
    public static final int MSG_WEB_PREVIEW_DURATION_TEXT_COLOR = 0xffffffff;
    public static final int MSG_SECRET_TIME_TEXT_COLOR = 0xffe4e2e0;
    public static final int MSG_STICKER_NAME_TEXT_COLOR = 0xffffffff;
    public static final int MSG_IN_FORDWARDED_NAME_TEXT_COLOR = 0xff3886c7;
    public static final int MSG_OUT_FORDWARDED_NAME_TEXT_COLOR = 0xff55ab4f;
    public static final int MSG_IN_REPLY_LINE_COLOR = 0xff70b4e8;
    public static final int MSG_OUT_REPLY_LINE_COLOR = 0xff88c97b;
    public static final int MSG_STICKER_REPLY_LINE_COLOR = 0xffffffff;
    public static final int MSG_IN_REPLY_NAME_TEXT_COLOR = 0xff3a8ccf;
    public static final int MSG_OUT_REPLY_NAME_TEXT_COLOR = 0xff55ab4f;
    public static final int MSG_STICKER_REPLY_NAME_TEXT_COLOR = 0xffffffff;
    public static final int MSG_IN_REPLY_MESSAGE_TEXT_COLOR = 0xff000000;
    public static final int MSG_OUT_REPLY_MESSAGE_TEXT_COLOR = 0xff000000;
    public static final int MSG_IN_REPLY_MEDIA_MESSAGE_TEXT_COLOR = 0xffa1aab3;
    public static final int MSG_OUT_REPLY_MEDIA_MESSAGE_TEXT_COLOR = 0xff65b05b;
    public static final int MSG_IN_REPLY_MEDIA_MESSAGE_SELETED_TEXT_COLOR = 0xff89b4c1;
    public static final int MSG_OUT_REPLY_MEDIA_MESSAGE_SELETED_TEXT_COLOR = 0xff65b05b;
    public static final int MSG_STICKER_REPLY_MESSAGE_TEXT_COLOR = 0xffffffff;
    public static final int MSG_IN_WEB_PREVIEW_LINE_COLOR = 0xff70b4e8;
    public static final int MSG_OUT_WEB_PREVIEW_LINE_COLOR = 0xff88c97b;
    public static final int MSG_IN_SITE_NAME_TEXT_COLOR = 0xff3a8ccf;
    public static final int MSG_OUT_SITE_NAME_TEXT_COLOR = 0xff55ab4f;
    public static final int MSG_IN_CONTACT_NAME_TEXT_COLOR = 0xff4e9ad4;
    public static final int MSG_OUT_CONTACT_NAME_TEXT_COLOR = 0xff55ab4f;
    public static final int MSG_IN_CONTACT_PHONE_TEXT_COLOR = 0xff2f3438;
    public static final int MSG_OUT_CONTACT_PHONE_TEXT_COLOR = 0xff354234;
    public static final int MSG_MEDIA_PROGRESS_COLOR = 0xffffffff;
    public static final int MSG_IN_AUDIO_PROGRESS_COLOR = 0xffffffff;
    public static final int MSG_OUT_AUDIO_PROGRESS_COLOR = 0xffefffde;
    public static final int MSG_IN_AUDIO_SELECTED_PROGRESS_COLOR = 0xffe2f8ff;
    public static final int MSG_OUT_AUDIO_SELECTED_PROGRESS_COLOR = 0xffd4f5bc;
    public static final int MSG_MEDIA_TIME_TEXT_COLOR = 0xffffffff;
    public static final int MSG_IN_TIME_TEXT_COLOR = 0xffa1aab3;
    public static final int MSG_OUT_TIME_TEXT_COLOR = 0xff70b15c;
    public static final int MSG_IN_TIME_SELECTED_TEXT_COLOR = 0xff89b4c1;
    public static final int MSG_OUT_TIME_SELECTED_TEXT_COLOR = 0xff70b15c;
    public static final int MSG_IN_AUDIO_PERFORMER_TEXT_COLOR = 0xff2f3438;
    public static final int MSG_OUT_AUDIO_PERFORMER_TEXT_COLOR = 0xff354234;
    public static final int MSG_IN_AUDIO_TITLE_TEXT_COLOR = 0xff4e9ad4;
    public static final int MSG_OUT_AUDIO_TITLE_TEXT_COLOR = 0xff55ab4f;
    public static final int MSG_IN_AUDIO_DURATION_TEXT_COLOR = 0xffa1aab3;
    public static final int MSG_OUT_AUDIO_DURATION_TEXT_COLOR = 0xff65b05b;
    public static final int MSG_IN_AUDIO_DURATION_SELECTED_TEXT_COLOR = 0xff89b4c1;
    public static final int MSG_OUT_AUDIO_DURATION_SELECTED_TEXT_COLOR = 0xff65b05b;
    public static final int MSG_IN_AUDIO_SEEKBAR_COLOR = 0xffe4eaf0;
    public static final int MSG_OUT_AUDIO_SEEKBAR_COLOR = 0xffbbe3ac;
    public static final int MSG_IN_AUDIO_SEEKBAR_SELECTED_COLOR = 0xffbcdee8;
    public static final int MSG_OUT_AUDIO_SEEKBAR_SELECTED_COLOR = 0xffa9dd96;
    public static final int MSG_IN_AUDIO_SEEKBAR_FILL_COLOR = 0xff72b5e8;
    public static final int MSG_OUT_AUDIO_SEEKBAR_FILL_COLOR = 0xff78c272;
    public static final int MSG_IN_VOICE_SEEKBAR_COLOR = 0xffdee5eb;
    public static final int MSG_OUT_VOICE_SEEKBAR_COLOR = 0xffbbe3ac;
    public static final int MSG_IN_VOICE_SEEKBAR_SELECTED_COLOR = 0xffbcdee8;
    public static final int MSG_OUT_VOICE_SEEKBAR_SELECTED_COLOR = 0xffa9dd96;
    public static final int MSG_IN_VOICE_SEEKBAR_FILL_COLOR = 0xff72b5e8;
    public static final int MSG_OUT_VOICE_SEEKBAR_FILL_COLOR = 0xff78c272;
    public static final int MSG_IN_FILE_PROGRESS_COLOR = 0xffebf0f5;
    public static final int MSG_OUT_FILE_PROGRESS_COLOR = 0xffdaf5c3;
    public static final int MSG_IN_FILE_PROGRESS_SELECTED_COLOR = 0xffcbeaf6;
    public static final int MSG_OUT_FILE_PROGRESS_SELECTED_COLOR = 0xffc5eca7;
    public static final int MSG_IN_FILE_NAME_TEXT_COLOR = 0xff4e9ad4;
    public static final int MSG_OUT_FILE_NAME_TEXT_COLOR = 0xff55ab4f;
    public static final int MSG_IN_FILE_INFO_TEXT_COLOR = 0xffa1aab3;
    public static final int MSG_OUT_FILE_INFO_TEXT_COLOR = 0xff65b05b;
    public static final int MSG_IN_FILE_INFO_SELECTED_TEXT_COLOR = 0xff89b4c1;
    public static final int MSG_OUT_FILE_INFO_SELECTED_TEXT_COLOR = 0xff65b05b;
    public static final int MSG_IN_FILE_BACKGROUND_COLOR = 0xffebf0f5;
    public static final int MSG_OUT_FILE_BACKGROUND_COLOR = 0xffdaf5c3;
    public static final int MSG_IN_FILE_BACKGROUND_SELECTED_COLOR = 0xffcbeaf6;
    public static final int MSG_OUT_FILE_BACKGROUND_SELECTED_COLOR = 0xffc5eca7;
    public static final int MSG_IN_VENUE_NAME_TEXT_COLOR = 0xff4e9ad4;
    public static final int MSG_OUT_VENUE_NAME_TEXT_COLOR = 0xff55ab4f;
    public static final int MSG_IN_VENUE_INFO_TEXT_COLOR = 0xffa1aab3;
    public static final int MSG_OUT_VENUE_INFO_TEXT_COLOR = 0xff65b05b;
    public static final int MSG_IN_VENUE_INFO_SELECTED_TEXT_COLOR = 0xff89b4c1;
    public static final int MSG_OUT_VENUE_INFO_SELECTED_TEXT_COLOR = 0xff65b05b;
    public static final int MSG_MEDIA_INFO_TEXT_COLOR = 0xffffffff;
    public static final int MSG_TEXT_COLOR = 0xff000000;
    public static final int MSG_LINK_TEXT_COLOR = 0xff2678b6;
    public static final int MSG_LINK_SELECT_BACKGROUND_COLOR = 0x3362a9e3;
    public static final int MSG_TEXT_SELECT_BACKGROUND_COLOR = 0x6662a9e3;


    public static Drawable backgroundDrawableIn;
    public static Drawable backgroundDrawableInSelected;
    public static Drawable backgroundDrawableOut;
    public static Drawable backgroundDrawableOutSelected;
    public static Drawable backgroundMediaDrawableIn;
    public static Drawable backgroundMediaDrawableInSelected;
    public static Drawable backgroundMediaDrawableOut;
    public static Drawable backgroundMediaDrawableOutSelected;
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

    public static Drawable[] cornerOuter = new Drawable[4];
    public static Drawable[] cornerInner = new Drawable[4];

    public static Drawable shareIconDrawable;

    public static Drawable geoInDrawable;
    public static Drawable geoOutDrawable;

    public static Drawable[] contactDrawable = new Drawable[2];
    public static Drawable[][] fileStatesDrawable = new Drawable[10][2];
    public static Drawable[][] photoStatesDrawables = new Drawable[13][2];
    public static Drawable[] docMenuDrawable = new Drawable[4];

    public static PorterDuffColorFilter colorFilter;
    public static PorterDuffColorFilter colorPressedFilter;
    private static int currentColor;

    public static Drawable attachButtonDrawables[] = new Drawable[8];

    private static Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public static void loadRecources(Context context) {
        if (backgroundDrawableIn == null) {
            backgroundDrawableIn = context.getResources().getDrawable(R.drawable.msg_in);
            backgroundDrawableInSelected = context.getResources().getDrawable(R.drawable.msg_in_selected);
            backgroundDrawableOut = context.getResources().getDrawable(R.drawable.msg_out);
            backgroundDrawableOutSelected = context.getResources().getDrawable(R.drawable.msg_out_selected);
            backgroundMediaDrawableIn = context.getResources().getDrawable(R.drawable.msg_in_photo);
            backgroundMediaDrawableInSelected = context.getResources().getDrawable(R.drawable.msg_in_photo_selected);
            backgroundMediaDrawableOut = context.getResources().getDrawable(R.drawable.msg_out_photo);
            backgroundMediaDrawableOutSelected = context.getResources().getDrawable(R.drawable.msg_out_photo_selected);
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

            fileStatesDrawable[0][0] = context.getResources().getDrawable(R.drawable.play_g);         // self
            fileStatesDrawable[0][1] = context.getResources().getDrawable(R.drawable.play_g_s);
            fileStatesDrawable[1][0] = context.getResources().getDrawable(R.drawable.pause_g);
            fileStatesDrawable[1][1] = context.getResources().getDrawable(R.drawable.pause_g_s);
            fileStatesDrawable[2][0] = context.getResources().getDrawable(R.drawable.file_g_load);
            fileStatesDrawable[2][1] = context.getResources().getDrawable(R.drawable.file_g_load_s);
            fileStatesDrawable[3][0] = context.getResources().getDrawable(R.drawable.file_g);
            fileStatesDrawable[3][1] = context.getResources().getDrawable(R.drawable.file_g_s);
            fileStatesDrawable[4][0] = context.getResources().getDrawable(R.drawable.file_g_cancel);
            fileStatesDrawable[4][1] = context.getResources().getDrawable(R.drawable.file_g_cancel_s);
            fileStatesDrawable[5][0] = context.getResources().getDrawable(R.drawable.play_b);         // from other = self+5
            fileStatesDrawable[5][1] = context.getResources().getDrawable(R.drawable.play_b_s);
            fileStatesDrawable[6][0] = context.getResources().getDrawable(R.drawable.pause_b);
            fileStatesDrawable[6][1] = context.getResources().getDrawable(R.drawable.pause_b_s);
            fileStatesDrawable[7][0] = context.getResources().getDrawable(R.drawable.file_b_load);
            fileStatesDrawable[7][1] = context.getResources().getDrawable(R.drawable.file_b_load_s);
            fileStatesDrawable[8][0] = context.getResources().getDrawable(R.drawable.file_b);
            fileStatesDrawable[8][1] = context.getResources().getDrawable(R.drawable.file_b_s);
            fileStatesDrawable[9][0] = context.getResources().getDrawable(R.drawable.file_b_cancel);
            fileStatesDrawable[9][1] = context.getResources().getDrawable(R.drawable.file_b_cancel_s);

            photoStatesDrawables[0][0] = context.getResources().getDrawable(R.drawable.photoload);
            photoStatesDrawables[0][1] = context.getResources().getDrawable(R.drawable.photoload_pressed);
            photoStatesDrawables[1][0] = context.getResources().getDrawable(R.drawable.photocancel);
            photoStatesDrawables[1][1] = context.getResources().getDrawable(R.drawable.photocancel_pressed);
            photoStatesDrawables[2][0] = context.getResources().getDrawable(R.drawable.photogif);
            photoStatesDrawables[2][1] = context.getResources().getDrawable(R.drawable.photogif_pressed);
            photoStatesDrawables[3][0] = context.getResources().getDrawable(R.drawable.playvideo);
            photoStatesDrawables[3][1] = context.getResources().getDrawable(R.drawable.playvideo_pressed);
            //photoStatesDrawables[4] = context.getResources().getDrawable(R.drawable.photopause);
            photoStatesDrawables[4][0] = photoStatesDrawables[4][1] = context.getResources().getDrawable(R.drawable.burn);
            photoStatesDrawables[5][0] = photoStatesDrawables[5][1] = context.getResources().getDrawable(R.drawable.circle);
            photoStatesDrawables[6][0] = photoStatesDrawables[6][1] = context.getResources().getDrawable(R.drawable.photocheck);

            photoStatesDrawables[7][0] = context.getResources().getDrawable(R.drawable.photoload_g);
            photoStatesDrawables[7][1] = context.getResources().getDrawable(R.drawable.photoload_g_s);
            photoStatesDrawables[8][0] = context.getResources().getDrawable(R.drawable.photocancel_g);
            photoStatesDrawables[8][1] = context.getResources().getDrawable(R.drawable.photocancel_g_s);
            photoStatesDrawables[9][0] = context.getResources().getDrawable(R.drawable.doc_green);
            photoStatesDrawables[9][1] = context.getResources().getDrawable(R.drawable.doc_green);

            photoStatesDrawables[10][0] = context.getResources().getDrawable(R.drawable.photoload_b);
            photoStatesDrawables[10][1] = context.getResources().getDrawable(R.drawable.photoload_b_s);
            photoStatesDrawables[11][0] = context.getResources().getDrawable(R.drawable.photocancel_b);
            photoStatesDrawables[11][1] = context.getResources().getDrawable(R.drawable.photocancel_b_s);
            photoStatesDrawables[12][0] = context.getResources().getDrawable(R.drawable.doc_blue);
            photoStatesDrawables[12][1] = context.getResources().getDrawable(R.drawable.doc_blue_s);

            docMenuDrawable[0] = context.getResources().getDrawable(R.drawable.doc_actions_b);
            docMenuDrawable[1] = context.getResources().getDrawable(R.drawable.doc_actions_g);
            docMenuDrawable[2] = context.getResources().getDrawable(R.drawable.doc_actions_b_s);
            docMenuDrawable[3] = context.getResources().getDrawable(R.drawable.video_actions);

            contactDrawable[0] = context.getResources().getDrawable(R.drawable.contact_blue);
            contactDrawable[1] = context.getResources().getDrawable(R.drawable.contact_green);

            shareIconDrawable = context.getResources().getDrawable(R.drawable.ic_ab_reply);

            geoInDrawable = context.getResources().getDrawable(R.drawable.location_b);
            geoOutDrawable = context.getResources().getDrawable(R.drawable.location_g);

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
            colorPressedFilter = new PorterDuffColorFilter(ApplicationLoader.getServiceSelectedMessageColor(), PorterDuff.Mode.MULTIPLY);
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
            attachButtonDrawables[ChatAttachAlert.ATTACH_BUTTON_IDX_LOCATION]    = context.getResources().getDrawable(R.drawable.attach_location_states);
            attachButtonDrawables[ChatAttachAlert.ATTACH_BUTTON_IDX_SENDSELECTED]= context.getResources().getDrawable(R.drawable.attach_hide_states);
        }
    }

    public static Drawable createBarSelectorDrawable(int color) {
        return createBarSelectorDrawable(color, true);
    }

    public static Drawable createBarSelectorDrawable(int color, boolean masked) {
        Drawable drawable;
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
