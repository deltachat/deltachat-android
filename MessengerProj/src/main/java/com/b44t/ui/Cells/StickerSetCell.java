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
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.R;
import com.b44t.messenger.TLRPC;
import com.b44t.ui.ActionBar.Theme;
import com.b44t.ui.Components.BackupImageView;
import com.b44t.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class StickerSetCell extends FrameLayout {

    private TextView textView;
    private TextView valueTextView;
    private BackupImageView imageView;
    private boolean needDivider;
    private ImageView optionsButton;
    private TLRPC.TL_messages_stickerSet stickersSet;
    private Rect rect = new Rect();

    private static Paint paint;

    public StickerSetCell(Context context) {
        super(context);

        if (paint == null) {
            paint = new Paint();
            paint.setColor(0xffd9d9d9);
        }

        textView = new TextView(context);
        textView.setTextColor(0xff212121);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setLines(1);
        textView.setMaxLines(1);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, LocaleController.isRTL ? 40 : 71, 10, LocaleController.isRTL ? 71 : 40, 0));

        valueTextView = new TextView(context);
        valueTextView.setTextColor(0xff8a8a8a);
        valueTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        valueTextView.setLines(1);
        valueTextView.setMaxLines(1);
        valueTextView.setSingleLine(true);
        valueTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        addView(valueTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, LocaleController.isRTL ? 40 : 71, 35, LocaleController.isRTL ? 71 : 40, 0));

        imageView = new BackupImageView(context);
        imageView.setAspectFit(true);
        addView(imageView, LayoutHelper.createFrame(48, 48, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 0 : 12, 8, LocaleController.isRTL ? 12 : 0, 0));

        optionsButton = new ImageView(context);
        optionsButton.setFocusable(false);
        optionsButton.setBackgroundDrawable(Theme.createBarSelectorDrawable(Theme.ACTION_BAR_AUDIO_SELECTOR_COLOR));
        optionsButton.setImageResource(R.drawable.doc_actions_b);
        optionsButton.setScaleType(ImageView.ScaleType.CENTER);
        addView(optionsButton, LayoutHelper.createFrame(40, 40, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.TOP));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));
    }

    public void setStickersSet(TLRPC.TL_messages_stickerSet set, boolean divider) {
        needDivider = divider;
        stickersSet = set;

        textView.setText(stickersSet.set.title);
        if (stickersSet.set.disabled) {
            textView.setAlpha(0.5f);
            valueTextView.setAlpha(0.5f);
            imageView.setAlpha(0.5f);
        } else {
            textView.setAlpha(1.0f);
            valueTextView.setAlpha(1.0f);
            imageView.setAlpha(1.0f);
        }
        ArrayList<TLRPC.Document> documents = set.documents;
        if (documents != null && !documents.isEmpty()) {
            valueTextView.setText(LocaleController.formatPluralString("Stickers", documents.size()));
            TLRPC.Document document = documents.get(0);
            if (document.thumb != null && document.thumb.location != null) {
                imageView.setImage(document.thumb.location, null, "webp", null);
            }
        } else {
            valueTextView.setText(LocaleController.formatPluralString("Stickers", 0));
        }
    }

    public void setOnOptionsClick(OnClickListener listener) {
        optionsButton.setOnClickListener(listener);
    }

    public TLRPC.TL_messages_stickerSet getStickersSet() {
        return stickersSet;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (Build.VERSION.SDK_INT >= 21 && getBackground() != null) {
            optionsButton.getHitRect(rect);
            if (rect.contains((int) event.getX(), (int) event.getY())) {
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                getBackground().setHotspot(event.getX(), event.getY());
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (needDivider) {
            canvas.drawLine(0, getHeight() - 1, getWidth() - getPaddingRight(), getHeight() - 1, paint);
        }
    }
}
