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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.MotionEvent;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.ContactsController;
import com.b44t.messenger.EmojiInputView;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MrChat;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.MrMsg;
import com.b44t.messenger.MrPoortext;
import com.b44t.messenger.R;
import com.b44t.messenger.ImageReceiver;
import com.b44t.messenger.ActionBar.Theme;
import com.b44t.messenger.Components.AvatarDrawable;


public class ChatlistCell extends BaseCell {

    private static TextPaint namePaint;
    private static TextPaint messagePaint;
    private static TextPaint timePaint;
    private static TextPaint countPaint;

    private static Drawable checkDrawable;
    private static Drawable halfCheckDrawable;
    private static Drawable clockDrawable;
    private static Drawable errorDrawable;
    private static Drawable countDrawable;
    private static Drawable groupDrawable;
    private static Drawable muteDrawable;
    private static Drawable closeDrawable;

    private static Paint linePaint;

    private long currentChatId;
    private int unreadCount;
    private boolean chatMuted;
    private int index;

    private ImageReceiver avatarImage;

    public boolean useSeparator = false;

    private int nameLeft;
    private StaticLayout nameLayout;
    private boolean drawGroupIcon;
    private int nameMuteLeft;
    private int nameLockLeft;
    private int nameLockTop;

    private int timeLeft;
    private int timeTop = AndroidUtilities.dp(17);
    private StaticLayout timeLayout;

    private boolean drawCheck1;
    private boolean drawCheck2;
    private boolean drawClock;
    private int checkDrawLeft;
    private int checkDrawTop = AndroidUtilities.dp(18);
    private int halfCheckDrawLeft;

    private int messageTop = AndroidUtilities.dp(40);
    private int messageLeft;
    private StaticLayout messageLayout;

    private boolean drawError;
    private int errorTop = AndroidUtilities.dp(42);
    private int errorLeft;

    private boolean drawCount;
    private int countTop = AndroidUtilities.dp(39);
    private int countLeft;
    private int countWidth;
    private StaticLayout countLayout;

    private int avatarLeft;
    private int avatarTop = AndroidUtilities.dp(10);
    private int avatarWH = AndroidUtilities.dp(52);

    private MrChat m_mrChat = new MrChat(0);
    private MrPoortext m_summary = new MrPoortext(0);
    private boolean m_showUnreadCount;

    public ChatlistCell(Context context) {
        super(context);

        if (namePaint == null) {
            namePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            namePaint.setTextSize(AndroidUtilities.dp(17));
            namePaint.setColor(Theme.CHATLIST_TITLE_TEXT_COLOR);

            messagePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            messagePaint.setTextSize(AndroidUtilities.dp(16));
            messagePaint.setColor(Theme.CHATLIST_MESSAGE_TEXT_COLOR);
            messagePaint.linkColor = Theme.CHATLIST_MESSAGE_TEXT_COLOR;

            linePaint = new Paint();
            linePaint.setColor(0xffdcdcdc);

            timePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            timePaint.setTextSize(AndroidUtilities.dp(13));
            timePaint.setColor(0xff999999);

            countPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            countPaint.setTextSize(AndroidUtilities.dp(13));
            countPaint.setColor(0xffffffff);
            countPaint.setTypeface(Typeface.DEFAULT_BOLD);

            checkDrawable = getResources().getDrawable(R.drawable.dialogs_check);
            halfCheckDrawable = getResources().getDrawable(R.drawable.dialogs_halfcheck);
            clockDrawable = getResources().getDrawable(R.drawable.msg_clock);
            errorDrawable = getResources().getDrawable(R.drawable.msg_warning);
            countDrawable = getResources().getDrawable(R.drawable.dialogs_badge);
            groupDrawable = getResources().getDrawable(R.drawable.list_group);
            muteDrawable = getResources().getDrawable(R.drawable.mute_grey);
            closeDrawable = getResources().getDrawable(R.drawable.ic_dismiss_deaddrop);
        }

        avatarImage = new ImageReceiver(this);
        avatarImage.setRoundRadius(AndroidUtilities.dp(26));
    }

    public void setChat(MrChat mrChat, MrPoortext mrSummary, int i, boolean showUnreadCount) {
        // called for the chats overview
        m_mrChat  = mrChat;
        m_summary = mrSummary;
        m_showUnreadCount = showUnreadCount;

        currentChatId = mrChat.getId();
        index = i;

        if(currentChatId==MrChat.MR_CHAT_ID_DEADDROP) {
            setBackgroundColor(Theme.CHATLIST_DEADDROP_BACKGROUND_COLOR);
        }
        else {
            setBackgroundResource(R.drawable.list_selector);
        }

        update(0);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        avatarImage.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        avatarImage.onAttachedToWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(72) + (useSeparator ? 1 : 0));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (currentChatId == 0) {
            super.onLayout(changed, left, top, right, bottom);
            return;
        }
        if (changed) {
            buildLayout();
        }
    }

    static public boolean deaddropClosePressed;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (Build.VERSION.SDK_INT >= 21 && getBackground() != null) {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                getBackground().setHotspot(event.getX(), event.getY());
            }
        }

        if( currentChatId==MrChat.MR_CHAT_ID_DEADDROP ) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float x = event.getX();
                deaddropClosePressed = (x < avatarLeft + avatarWH);
            }
        }

        return super.onTouchEvent(event);
    }

    public void buildLayout() {
        String nameString = "";
        String timeString = "";
        CharSequence messageString = "";
        TextPaint currentNamePaint = namePaint;
        TextPaint currentMessagePaint = messagePaint;

        drawGroupIcon = false;

        if (m_mrChat.getType()==MrChat.MR_CHAT_GROUP && currentChatId!=MrChat.MR_CHAT_ID_DEADDROP) {
            drawGroupIcon = true;
            nameLockTop = AndroidUtilities.dp(17.5f);

            if (!LocaleController.isRTL) {
                nameLockLeft = AndroidUtilities.dp(AndroidUtilities.leftBaseline);
                nameLeft = AndroidUtilities.dp(AndroidUtilities.leftBaseline + 4) + (groupDrawable.getIntrinsicWidth());
            } else {
                nameLockLeft = getMeasuredWidth() - AndroidUtilities.dp(AndroidUtilities.leftBaseline) - (groupDrawable.getIntrinsicWidth());
                nameLeft = AndroidUtilities.dp(14);
            }
        } else {
            if (!LocaleController.isRTL) {
                nameLeft = AndroidUtilities.dp(AndroidUtilities.leftBaseline);
            } else {
                nameLeft = AndroidUtilities.dp(14);
            }
        }

        String mess = m_summary.getText2();
        if (mess.length() > 150) {
            mess = mess.substring(0, 150);
        }
        String title = m_summary.getText1();
        if( !title.isEmpty() )
        {
            int title_meaning = m_summary.getText1Meaning();
            int title_color = Theme.CHATLIST_NAME_TEXT_COLOR;
            switch( title_meaning ) {
                case MrPoortext.MR_TEXT1_SELF:  title_color = Theme.CHATLIST_SELF_TEXT_COLOR; break;
                case MrPoortext.MR_TEXT1_DRAFT: title_color = Theme.CHATLIST_DRAFT_TEXT_COLOR; break;
            }
            SpannableStringBuilder stringBuilder = SpannableStringBuilder.valueOf(String.format("%s: %s", title, mess));
            stringBuilder.setSpan(new ForegroundColorSpan(title_color), 0, title.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            messageString = EmojiInputView.replaceEmoji(stringBuilder, false);
        }
        else
        {
            messageString = mess;
        }

        long timestmp =m_summary.getTimestamp();
        if( timestmp!=0 ) {
            timeString = LocaleController.dateForChatlist(timestmp);
        }
        else {
            timeString = "";
        }

        drawCheck1 = false;
        drawCheck2 = false;
        drawClock = false;
        drawCount = false;
        drawError = false;
        switch( m_summary.getState() ) {
            case MrMsg.MR_OUT_ERROR: drawError = true; break;
            case MrMsg.MR_OUT_PENDING: drawClock = true; break;
            case MrMsg.MR_OUT_DELIVERED: drawCheck2 = true; break;
            case MrMsg.MR_OUT_MDN_RCVD: drawCheck1 = true; drawCheck2 = true; break;
        }

        int timeWidth = (int) Math.ceil(timePaint.measureText(timeString));
        timeLayout = new StaticLayout(timeString, timePaint, timeWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        if (!LocaleController.isRTL) {
            timeLeft = getMeasuredWidth() - AndroidUtilities.dp(15) - timeWidth;
        } else {
            timeLeft = AndroidUtilities.dp(15);
        }

        nameString = m_mrChat.getName();

        int nameWidth;

        if (!LocaleController.isRTL) {
            nameWidth = getMeasuredWidth() - nameLeft - AndroidUtilities.dp(14) - timeWidth;
        } else {
            nameWidth = getMeasuredWidth() - nameLeft - AndroidUtilities.dp(AndroidUtilities.leftBaseline) - timeWidth;
            nameLeft += timeWidth;
        }

        if (drawGroupIcon) {
            nameWidth -= AndroidUtilities.dp(4) + groupDrawable.getIntrinsicWidth();
        }

        if (drawClock) {
            int w = clockDrawable.getIntrinsicWidth() + AndroidUtilities.dp(5);
            nameWidth -= w;
            if (!LocaleController.isRTL) {
                checkDrawLeft = timeLeft - w;
            } else {
                checkDrawLeft = timeLeft + timeWidth + AndroidUtilities.dp(5);
                nameLeft += w;
            }
        } else if (drawCheck2) {
            int w = checkDrawable.getIntrinsicWidth() + AndroidUtilities.dp(5);
            nameWidth -= w;
            if (drawCheck1) {
                nameWidth -= halfCheckDrawable.getIntrinsicWidth() - AndroidUtilities.dp(8);
                if (!LocaleController.isRTL) {
                    halfCheckDrawLeft = timeLeft - w;
                    checkDrawLeft = halfCheckDrawLeft - AndroidUtilities.dp(5.5f);
                } else {
                    checkDrawLeft = timeLeft + timeWidth + AndroidUtilities.dp(5);
                    halfCheckDrawLeft = checkDrawLeft + AndroidUtilities.dp(5.5f);
                    nameLeft += w + halfCheckDrawable.getIntrinsicWidth() - AndroidUtilities.dp(8);
                }
            } else {
                if (!LocaleController.isRTL) {
                    checkDrawLeft = timeLeft - w;
                } else {
                    checkDrawLeft = timeLeft + timeWidth + AndroidUtilities.dp(5);
                    nameLeft += w;
                }
            }
        }

        if (chatMuted) {
            int w = AndroidUtilities.dp(6) + muteDrawable.getIntrinsicWidth();
            nameWidth -= w;
            if (LocaleController.isRTL) {
                nameLeft += w;
            }
        }

        // build name
        nameWidth = Math.max(AndroidUtilities.dp(12), nameWidth);
        CharSequence nameStringFinal = TextUtils.ellipsize(nameString.replace('\n', ' '), currentNamePaint, nameWidth - AndroidUtilities.dp(12), TextUtils.TruncateAt.END);
        try {
            nameLayout = new StaticLayout(nameStringFinal, currentNamePaint, nameWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        } catch (Exception e) {

        }

        // build avatar
        int messageWidth = getMeasuredWidth() - AndroidUtilities.dp(AndroidUtilities.leftBaseline + 16);
        if (!LocaleController.isRTL) {
            messageLeft = AndroidUtilities.dp(AndroidUtilities.leftBaseline);
            avatarLeft = AndroidUtilities.dp(9);
        } else {
            messageLeft = AndroidUtilities.dp(16);
            avatarLeft = getMeasuredWidth() - AndroidUtilities.dp(61);
        }

        if( currentChatId==MrChat.MR_CHAT_ID_DEADDROP ) {

        }
        else {
            avatarImage.setImageCoords(avatarLeft, avatarTop, avatarWH, avatarWH);
        }

        // build counter
        if (drawError) {
            int w = errorDrawable.getIntrinsicWidth() + AndroidUtilities.dp(8);
            messageWidth -= w;
            if (!LocaleController.isRTL) {
                errorLeft = getMeasuredWidth() - errorDrawable.getIntrinsicWidth() - AndroidUtilities.dp(16);
            } else {
                errorLeft = AndroidUtilities.dp(16);
                messageLeft += w;
            }
        } else if (unreadCount != 0 && currentChatId!=MrChat.MR_CHAT_ID_DEADDROP) {
            String countString = String.format("%d", unreadCount);
            countWidth = Math.max(AndroidUtilities.dp(12), (int)Math.ceil(countPaint.measureText(countString)));
            countLayout = new StaticLayout(countString, countPaint, countWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
            int w = countWidth + AndroidUtilities.dp(18);
            messageWidth -= w;
            if (!LocaleController.isRTL) {
                countLeft = getMeasuredWidth() - countWidth - AndroidUtilities.dp(19);
            } else {
                countLeft = AndroidUtilities.dp(19);
                messageLeft += w;
            }
            drawCount = true;
        }

        messageWidth = Math.max(AndroidUtilities.dp(12), messageWidth);
        CharSequence messageStringFinal = TextUtils.ellipsize(messageString, currentMessagePaint, messageWidth - AndroidUtilities.dp(12), TextUtils.TruncateAt.END);
        try {
            messageLayout = new StaticLayout(messageStringFinal, currentMessagePaint, messageWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        } catch (Exception e) {

        }

        double widthpx;
        float left;
        if (LocaleController.isRTL) {
            if (nameLayout != null && nameLayout.getLineCount() > 0) {
                left = nameLayout.getLineLeft(0);
                widthpx = Math.ceil(nameLayout.getLineWidth(0));
                if (chatMuted) {
                    nameMuteLeft = (int) (nameLeft + (nameWidth - widthpx) - AndroidUtilities.dp(6) - muteDrawable.getIntrinsicWidth());
                }
                if (left == 0) {
                    if (widthpx < nameWidth) {
                        nameLeft += (nameWidth - widthpx);
                    }
                }
            }
            if (messageLayout != null && messageLayout.getLineCount() > 0) {
                left = messageLayout.getLineLeft(0);
                if (left == 0) {
                    widthpx = Math.ceil(messageLayout.getLineWidth(0));
                    if (widthpx < messageWidth) {
                        messageLeft += (messageWidth - widthpx);
                    }
                }
            }
        } else {
            if (nameLayout != null && nameLayout.getLineCount() > 0) {
                left = nameLayout.getLineRight(0);
                if (left == nameWidth) {
                    widthpx = Math.ceil(nameLayout.getLineWidth(0));
                    if (widthpx < nameWidth) {
                        nameLeft -= (nameWidth - widthpx);
                    }
                }
                if (chatMuted) {
                    nameMuteLeft = (int) (nameLeft + left + AndroidUtilities.dp(6));
                }
            }
            if (messageLayout != null && messageLayout.getLineCount() > 0) {
                left = messageLayout.getLineRight(0);
                if (left == messageWidth) {
                    widthpx = Math.ceil(messageLayout.getLineWidth(0));
                    if (widthpx < messageWidth) {
                        messageLeft -= (messageWidth - widthpx);
                    }
                }
            }
        }
    }

    public void update(int mask) {
        if( m_showUnreadCount ) {
            unreadCount = m_mrChat.getFreshMsgCount();
        }

        if (mask != 0) {
            boolean continueUpdate = false;
            if (!continueUpdate && (mask & MrMailbox.UPDATE_MASK_AVATAR) != 0) {
                continueUpdate = true;
            }
            if (!continueUpdate && (mask & MrMailbox.UPDATE_MASK_NAME) != 0) {
                continueUpdate = true;
            }
            if (!continueUpdate && (mask & MrMailbox.UPDATE_MASK_CHAT_AVATAR) != 0) {
                continueUpdate = true;
            }
            if (!continueUpdate && (mask & MrMailbox.UPDATE_MASK_CHAT_NAME) != 0) {
                continueUpdate = true;
            }

            if (!continueUpdate) {
                return;
            }
        }



        if( currentChatId == MrChat.MR_CHAT_ID_DEADDROP ) {
            chatMuted = false; // never draw mute icon, the deaddrop is always muted
        }
        else {
            chatMuted = MrMailbox.isDialogMuted(currentChatId);
            ContactsController.setupAvatar(this, avatarImage, new AvatarDrawable(), null, m_mrChat);
        }

        if (getMeasuredWidth() != 0 || getMeasuredHeight() != 0) {
            buildLayout();
        } else {
            requestLayout();
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (currentChatId == 0) {
            return;
        }

        if (drawGroupIcon) {
            setDrawableBounds(groupDrawable, nameLockLeft, nameLockTop);
            groupDrawable.draw(canvas);
        }

        if (nameLayout != null) {
            canvas.save();
            canvas.translate(nameLeft, AndroidUtilities.dp(13));
            nameLayout.draw(canvas);
            canvas.restore();
        }

        canvas.save();
        canvas.translate(timeLeft, timeTop);
        timeLayout.draw(canvas);
        canvas.restore();

        if (messageLayout != null) {
            canvas.save();
            canvas.translate(messageLeft, messageTop);
            try {
                messageLayout.draw(canvas);
            } catch (Exception e) {

            }
            canvas.restore();
        }

        if (drawClock) {
            setDrawableBounds(clockDrawable, checkDrawLeft, checkDrawTop);
            clockDrawable.draw(canvas);
        } else if (drawCheck2) {
            if (drawCheck1) {
                setDrawableBounds(halfCheckDrawable, halfCheckDrawLeft, checkDrawTop);
                halfCheckDrawable.draw(canvas);
                setDrawableBounds(checkDrawable, checkDrawLeft, checkDrawTop);
                checkDrawable.draw(canvas);
            } else {
                setDrawableBounds(checkDrawable, checkDrawLeft, checkDrawTop);
                checkDrawable.draw(canvas);
            }
        }

        if (chatMuted) {
            setDrawableBounds(muteDrawable, nameMuteLeft, AndroidUtilities.dp(16.5f));
            muteDrawable.draw(canvas);
        }

        if (drawError) {
            setDrawableBounds(errorDrawable, errorLeft, errorTop);
            errorDrawable.draw(canvas);
        } else if (drawCount) {
            setDrawableBounds(countDrawable, countLeft - AndroidUtilities.dp(5.5f), countTop, countWidth + AndroidUtilities.dp(11), countDrawable.getIntrinsicHeight());
            countDrawable.draw(canvas);

            canvas.save();
            canvas.translate(countLeft, countTop + AndroidUtilities.dp(4));
            countLayout.draw(canvas);
            canvas.restore();
        }

        if( currentChatId==MrChat.MR_CHAT_ID_DEADDROP ) {
            int shrink = AndroidUtilities.dp(12);
            setDrawableBounds(closeDrawable, avatarLeft+shrink, avatarTop+shrink, avatarWH-shrink*2, avatarWH-shrink*2);
            closeDrawable.draw(canvas);
        }
        else {
            avatarImage.draw(canvas);
        }

        if (useSeparator) {
            if (LocaleController.isRTL) {
                canvas.drawLine(0, getMeasuredHeight() - 1, getMeasuredWidth() - AndroidUtilities.dp(AndroidUtilities.leftBaseline), getMeasuredHeight() - 1, linePaint);
            } else {
                canvas.drawLine(AndroidUtilities.dp(AndroidUtilities.leftBaseline), getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight() - 1, linePaint);
            }
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
