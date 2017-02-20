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


package com.b44t.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.MotionEvent;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.MessageObject;
import com.b44t.messenger.MessagesController;
import com.b44t.messenger.FileLog;
import com.b44t.ui.ActionBar.Theme;

public class ChatActionCell extends BaseCell {

    private static TextPaint textPaint;
    private static Paint backPaint;

    private StaticLayout textLayout;
    private int textWidth = 0;
    private int textHeight = 0;
    private int textX = 0;
    private int textY = 0;
    private int textXLeft = 0;
    private int previousWidth = 0;

    private boolean hasReplyMessage;

    private MessageObject currentMessageObject;

    public ChatActionCell(Context context) {
        super(context);
        if (textPaint == null) {
            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(0xffffffff);
            textPaint.linkColor = 0xffffffff;
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);

            backPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }
        backPaint.setColor(ApplicationLoader.getServiceMessageColor());

        textPaint.setTextSize(AndroidUtilities.dp(MessagesController.getInstance().fontSize - 2));
    }

    public void setMessageObject(MessageObject messageObject) {
        if (currentMessageObject == messageObject && (hasReplyMessage || messageObject.replyMessageObject == null)) {
            return;
        }
        currentMessageObject = messageObject;
        hasReplyMessage = messageObject.replyMessageObject != null;
        previousWidth = 0;
        requestLayout();
    }

    public MessageObject getMessageObject() {
        return currentMessageObject;
    }

    @Override
    protected void onLongPress() {
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (currentMessageObject == null) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), textHeight + AndroidUtilities.dp(14));
            return;
        }
        int width = Math.max(AndroidUtilities.dp(30), MeasureSpec.getSize(widthMeasureSpec));
        if (width != previousWidth) {
            previousWidth = width;
            int maxWidth = width - AndroidUtilities.dp(30);
            textLayout = new StaticLayout(currentMessageObject.messageText, textPaint, maxWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
            textHeight = 0;
            textWidth = 0;
            try {
                int linesCount = textLayout.getLineCount();
                for (int a = 0; a < linesCount; a++) {
                    float lineWidth;
                    try {
                        lineWidth = textLayout.getLineWidth(a);
                        if (lineWidth > maxWidth) {
                            lineWidth = maxWidth;
                        }
                        textHeight = (int)Math.max(textHeight, Math.ceil(textLayout.getLineBottom(a)));
                    } catch (Exception e) {
                        FileLog.e("messenger", e);
                        return;
                    }
                    textWidth = (int)Math.max(textWidth, Math.ceil(lineWidth));
                }
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }

            textX = (width - textWidth) / 2;
            textY = AndroidUtilities.dp(7);
            textXLeft = (width - textLayout.getWidth()) / 2;
        }
        setMeasuredDimension(width, textHeight + AndroidUtilities.dp(14 + 0));
    }

    private int findMaxWidthAroundLine(int line) {
        int width = (int) Math.ceil(textLayout.getLineWidth(line));
        int count = textLayout.getLineCount();
        for (int a = line + 1; a < count; a++) {
            int w = (int) Math.ceil(textLayout.getLineWidth(a));
            if (Math.abs(w - width) < AndroidUtilities.dp(12)) {
                width = Math.max(w, width);
            } else {
                break;
            }
        }
        for (int a = line - 1; a >= 0; a--) {
            int w = (int) Math.ceil(textLayout.getLineWidth(a));
            if (Math.abs(w - width) < AndroidUtilities.dp(12)) {
                width = Math.max(w, width);
            } else {
                break;
            }
        }
        return width;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (currentMessageObject == null) {
            return;
        }

        if (textLayout != null) {
            final int count = textLayout.getLineCount();
            final int corner = AndroidUtilities.dp(6);
            int y = AndroidUtilities.dp(7);
            int previousLineBottom = 0;
            int dx;
            int dy;
            for (int a = 0; a < count; a++) {
                int width = findMaxWidthAroundLine(a);
                int x = (getMeasuredWidth() - width) / 2 - AndroidUtilities.dp(3);
                width += AndroidUtilities.dp(6);
                int lineBottom = textLayout.getLineBottom(a);
                int height = lineBottom - previousLineBottom;
                int additionalHeight = 0;
                previousLineBottom = lineBottom;

                boolean drawBottomCorners = a == count - 1;
                boolean drawTopCorners = a == 0;

                if (drawTopCorners) {
                    y -= AndroidUtilities.dp(3);
                    height += AndroidUtilities.dp(3);
                }
                if (drawBottomCorners) {
                    height += AndroidUtilities.dp(3);
                }
                canvas.drawRect(x, y, x + width, y + height, backPaint);

                if (!drawBottomCorners && a + 1 < count) {
                    int nextLineWidth = findMaxWidthAroundLine(a + 1) + AndroidUtilities.dp(6);
                    if (nextLineWidth + corner * 2 < width) {
                        int nextX = (getMeasuredWidth() - nextLineWidth) / 2;
                        drawBottomCorners = true;
                        additionalHeight = AndroidUtilities.dp(3);

                        canvas.drawRect(x, y + height, nextX, y + height + AndroidUtilities.dp(3), backPaint);
                        canvas.drawRect(nextX + nextLineWidth, y + height, x + width, y + height + AndroidUtilities.dp(3), backPaint);
                    } else if (width + corner * 2 < nextLineWidth) {
                        additionalHeight = AndroidUtilities.dp(3);

                        dy = y + height - AndroidUtilities.dp(9);

                        dx = x - corner * 2;
                        Theme.cornerInner[2].setBounds(dx, dy, dx + corner, dy + corner);
                        Theme.cornerInner[2].draw(canvas);

                        dx = x + width + corner;
                        Theme.cornerInner[3].setBounds(dx, dy, dx + corner, dy + corner);
                        Theme.cornerInner[3].draw(canvas);
                    } else {
                        additionalHeight = AndroidUtilities.dp(6);
                    }
                }
                if (!drawTopCorners && a > 0) {
                    int prevLineWidth = findMaxWidthAroundLine(a - 1) + AndroidUtilities.dp(6);
                    if (prevLineWidth + corner * 2 < width) {
                        int prevX = (getMeasuredWidth() - prevLineWidth) / 2;
                        drawTopCorners = true;
                        y -= AndroidUtilities.dp(3);
                        height += AndroidUtilities.dp(3);

                        canvas.drawRect(x, y, prevX, y + AndroidUtilities.dp(3), backPaint);
                        canvas.drawRect(prevX + prevLineWidth, y, x + width, y + AndroidUtilities.dp(3), backPaint);
                    } else if (width + corner * 2 < prevLineWidth) {
                        y -= AndroidUtilities.dp(3);
                        height += AndroidUtilities.dp(3);

                        dy = y + corner;

                        dx = x - corner * 2;
                        Theme.cornerInner[0].setBounds(dx, dy, dx + corner, dy + corner);
                        Theme.cornerInner[0].draw(canvas);

                        dx = x + width + corner;
                        Theme.cornerInner[1].setBounds(dx, dy, dx + corner, dy + corner);
                        Theme.cornerInner[1].draw(canvas);
                    } else {
                        y -= AndroidUtilities.dp(6);
                        height += AndroidUtilities.dp(6);
                    }
                }

                canvas.drawRect(x - corner, y + corner, x, y + height + additionalHeight - corner, backPaint);
                canvas.drawRect(x + width, y + corner, x + width + corner, y + height + additionalHeight - corner, backPaint);

                if (drawTopCorners) {
                    dx = x - corner;
                    Theme.cornerOuter[0].setBounds(dx, y, dx + corner, y + corner);
                    Theme.cornerOuter[0].draw(canvas);

                    dx = x + width;
                    Theme.cornerOuter[1].setBounds(dx, y, dx + corner, y + corner);
                    Theme.cornerOuter[1].draw(canvas);
                }

                if (drawBottomCorners) {
                    dy = y + height + additionalHeight - corner;

                    dx = x + width;
                    Theme.cornerOuter[2].setBounds(dx, dy, dx + corner, dy + corner);
                    Theme.cornerOuter[2].draw(canvas);

                    dx = x - corner;
                    Theme.cornerOuter[3].setBounds(dx, dy, dx + corner, dy + corner);
                    Theme.cornerOuter[3].draw(canvas);
                }

                y += height;
            }

            canvas.save();
            canvas.translate(textXLeft, textY);
            textLayout.draw(canvas);
            canvas.restore();
        }
    }
}
