/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.ui.Components;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.ActionBar.Theme;

public class AvatarDrawable extends Drawable {

    private static Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static TextPaint namePaint;
    private static TextPaint namePaintSmall;
    private static int[] arrColors             = {0xffe56555, 0xfff28c48, 0xff8e85ee, 0xff76c84d, 0xff5bb6cc, 0xff549cdd, 0xffd25c99, 0xffb37800}; /* the colors should contrast to typical action bar colors as well as to white (more important, is used as text color)*/

    private static Drawable photoDrawable;

    private int color;
    private StaticLayout textLayout;
    private float textWidth;
    private float textHeight;
    private float textLeft;
    private boolean drawPhoto;
    private boolean smallStyle;
    private StringBuilder stringBuilder = new StringBuilder(5);

    public AvatarDrawable() {
        super();

        if (namePaint == null) {
            namePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            namePaint.setColor(0xffffffff);
            namePaint.setTextSize(AndroidUtilities.dp(20));

            namePaintSmall = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            namePaintSmall.setColor(0xffffffff);
            namePaintSmall.setTextSize(AndroidUtilities.dp(14));
        }
    }

    public AvatarDrawable(TLRPC.User user) {
        this();
        if (user != null) {
            setInfoByName(user.first_name+" "+ user.last_name);
        }
    }

    public AvatarDrawable(TLRPC.Chat chat) {
        this();
        if (chat != null) {
            setInfoByName(chat.title);
        }
    }

    public void setSmallStyle(boolean value) {
        smallStyle = value;
    }

    public static int getColorIndex(int id) {
        return Math.abs(id % arrColors.length);
    }

    public static int getColorForId(int id) {
        return arrColors[getColorIndex(id)];
    }

    public static int getNameColor(String name) {
        int id = strChecksum(name);
        return arrColors[getColorIndex(id)];
    }

    public void setInfoByUser(TLRPC.User user) {
        if (user != null) {
            setInfoByName(user.first_name +" "+ user.last_name);
        }
    }

    public void setInfoByChat(TLRPC.Chat chat) {
        if (chat != null) {
            setInfoByName(chat.title);
        }
    }

    public void setColor_(int value) {
        color = value;
    }

    private static int strChecksum(String str) {
        int ret = 0;
        if( str!=null ) {
            int i;
            for (i = 0; i < str.length(); i++) {
                ret += (i+1)*str.charAt(i);
                ret %= 0x00FFFFFF;
            }
        }
        return ret;
    }

    public void setInfoByName(String firstName) {
        String lastName = null;

        int id = strChecksum(firstName);

        color = arrColors[getColorIndex(id)];

        if (firstName == null || firstName.length() == 0) {
            firstName = lastName;
            lastName = null;
        }

        stringBuilder.setLength(0);
        if (firstName != null && firstName.length() > 0) {
            stringBuilder.append(firstName.substring(0, 1));
        }
        if (lastName != null && lastName.length() > 0) {
            String lastch = null;
            for (int a = lastName.length() - 1; a >= 0; a--) {
                if (lastch != null && lastName.charAt(a) == ' ') {
                    break;
                }
                lastch = lastName.substring(a, a + 1);
            }
            if (Build.VERSION.SDK_INT >= 16) {
                stringBuilder.append("\u200C");
            }
            stringBuilder.append(lastch);
        } else if (firstName != null && firstName.length() > 0) {
            for (int a = firstName.length() - 1; a >= 0; a--) {
                if (firstName.charAt(a) == ' ') {
                    if (a != firstName.length() - 1 && firstName.charAt(a + 1) != ' ') {
                        if (Build.VERSION.SDK_INT >= 16) {
                            stringBuilder.append("\u200C");
                        }
                        stringBuilder.append(firstName.substring(a + 1, a + 2));
                        break;
                    }
                }
            }
        }

        if (stringBuilder.length() > 0) {
            String text = stringBuilder.toString().toUpperCase();
            try {
                textLayout = new StaticLayout(text, (smallStyle ? namePaintSmall : namePaint), AndroidUtilities.dp(100), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                if (textLayout.getLineCount() > 0) {
                    textLeft = textLayout.getLineLeft(0);
                    textWidth = textLayout.getLineWidth(0);
                    textHeight = textLayout.getLineBottom(0);
                }
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        } else {
            textLayout = null;
        }
    }

    public void setDrawPhoto(boolean value) {
        if (value && photoDrawable == null) {
            photoDrawable = ApplicationLoader.applicationContext.getResources().getDrawable(R.drawable.photo_w);
        }
        drawPhoto = value;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds == null) {
            return;
        }
        int size = bounds.width();
        paint.setColor(color);
        canvas.save();
        canvas.translate(bounds.left, bounds.top);
        canvas.drawCircle(size / 2, size / 2, size / 2, paint);

        if (textLayout != null) {
            canvas.translate((size - textWidth) / 2 - textLeft, (size - textHeight) / 2);
            textLayout.draw(canvas);
        } else if (drawPhoto && photoDrawable != null) {
            int x = (size - photoDrawable.getIntrinsicWidth()) / 2;
            int y = (size - photoDrawable.getIntrinsicHeight()) / 2;
            photoDrawable.setBounds(x, y, x + photoDrawable.getIntrinsicWidth(), y + photoDrawable.getIntrinsicHeight());
            photoDrawable.draw(canvas);
        }

        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return 0;
    }

    @Override
    public int getIntrinsicHeight() {
        return 0;
    }
}
