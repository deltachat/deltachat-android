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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.Emoji;
import com.b44t.messenger.FileLoader;
import com.b44t.messenger.FileLog;
import com.b44t.messenger.ImageLoader;
import com.b44t.messenger.ImageReceiver;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MediaController;
import com.b44t.messenger.MessageObject;
import com.b44t.messenger.R;
import com.b44t.messenger.Utilities;
import com.b44t.messenger.TLRPC;
import com.b44t.ui.Components.LetterDrawable;
import com.b44t.ui.Components.RadialProgress;
import com.b44t.ui.ActionBar.Theme;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class ContextLinkCell extends View implements MediaController.FileDownloadProgressListener {

    private final static int DOCUMENT_ATTACH_TYPE_NONE = 0;
    private final static int DOCUMENT_ATTACH_TYPE_DOCUMENT = 1;
    private final static int DOCUMENT_ATTACH_TYPE_GIF = 2;
    private final static int DOCUMENT_ATTACH_TYPE_VOICE = 3;
    private final static int DOCUMENT_ATTACH_TYPE_VIDEO = 4;
    private final static int DOCUMENT_ATTACH_TYPE_MUSIC = 5;
    private final static int DOCUMENT_ATTACH_TYPE_STICKER = 6;
    private final static int DOCUMENT_ATTACH_TYPE_PHOTO = 7;
    private final static int DOCUMENT_ATTACH_TYPE_GEO = 8;

    public interface ContextLinkCellDelegate {
        void didPressedImage(ContextLinkCell cell);
    }

    private ImageReceiver linkImageView;
    private boolean drawLinkImageView;
    private LetterDrawable letterDrawable;

    private boolean needDivider;
    private boolean buttonPressed;
    private boolean needShadow;

    private int linkY;
    private StaticLayout linkLayout;

    private int titleY = AndroidUtilities.dp(7);
    private StaticLayout titleLayout;

    private int descriptionY = AndroidUtilities.dp(27);
    private StaticLayout descriptionLayout;

    private TLRPC.Document documentAttach;
    private int documentAttachType;
    private boolean mediaWebpage;

    private static TextPaint titleTextPaint;
    private static TextPaint descriptionTextPaint;
    private static Paint paint;

    private int TAG;
    private int buttonState;
    private RadialProgress radialProgress;

    private long lastUpdateTime;
    private boolean scaled;
    private float scale;
    private long time = 0;
    //private static AccelerateInterpolator interpolator = new AccelerateInterpolator(0.5f);

    private ContextLinkCellDelegate delegate;

    public ContextLinkCell(Context context) {
        super(context);

        if (titleTextPaint == null) {
            titleTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            titleTextPaint.setColor(0xff212121);
            titleTextPaint.setTextSize(AndroidUtilities.dp(15));

            descriptionTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            descriptionTextPaint.setTextSize(AndroidUtilities.dp(13));

            paint = new Paint();
            paint.setColor(0xffd9d9d9);
            paint.setStrokeWidth(1);
        }

        linkImageView = new ImageReceiver(this);
        letterDrawable = new LetterDrawable();
        radialProgress = new RadialProgress(this);
        TAG = MediaController.getInstance().generateObserverTag();
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        drawLinkImageView = false;
        descriptionLayout = null;
        titleLayout = null;
        linkLayout = null;
        linkY = AndroidUtilities.dp(27);

        if (documentAttach == null) {
            setMeasuredDimension(AndroidUtilities.dp(100), AndroidUtilities.dp(100));
            return;
        }

        int viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxWidth = viewWidth - AndroidUtilities.dp(AndroidUtilities.leftBaseline) - AndroidUtilities.dp(8);

        TLRPC.PhotoSize currentPhotoObject = null;
        TLRPC.PhotoSize currentPhotoObjectThumb = null;
        ArrayList<TLRPC.PhotoSize> photoThumbs = null;
        String url = null;

        if (documentAttach != null) {
            photoThumbs = new ArrayList<>();
            photoThumbs.add(documentAttach.thumb);
        }

        documentAttachType = DOCUMENT_ATTACH_TYPE_NONE;
        String ext = null;
        if (documentAttach != null) {
            if (MessageObject.isGifDocument(documentAttach)) {
                documentAttachType = DOCUMENT_ATTACH_TYPE_GIF;
                currentPhotoObject = documentAttach.thumb;
            } else if (MessageObject.isStickerDocument(documentAttach)) {
                documentAttachType = DOCUMENT_ATTACH_TYPE_STICKER;
                currentPhotoObject = documentAttach.thumb;
                ext = "webp";
            } else {
                currentPhotoObject = documentAttach.thumb;
            }
        }

        int width;
        int w = 0;
        int h = 0;

        if (documentAttach != null) {
            for (int b = 0; b < documentAttach.attributes.size(); b++) {
                TLRPC.DocumentAttribute attribute = documentAttach.attributes.get(b);
                if (attribute instanceof TLRPC.TL_documentAttributeImageSize || attribute instanceof TLRPC.TL_documentAttributeVideo) {
                    w = attribute.w;
                    h = attribute.h;
                    break;
                }
            }
        }
        if (w == 0 || h == 0) {
            if (currentPhotoObject != null) {
                if (currentPhotoObjectThumb != null) {
                    currentPhotoObjectThumb.size = -1;
                }
                w = currentPhotoObject.w;
                h = currentPhotoObject.h;
            }
        }
        if (w == 0 || h == 0) {
            w = h = AndroidUtilities.dp(80);
        }
        if (documentAttach != null || currentPhotoObject != null || url != null) {
            String currentPhotoFilter;
            String currentPhotoFilterThumb = "52_52_b";

            if (mediaWebpage) {
                width = (int) (w / (h / (float) AndroidUtilities.dp(80)));
                if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF) {
                    currentPhotoFilterThumb = currentPhotoFilter = String.format(Locale.US, "%d_%d_b", (int) (width / AndroidUtilities.density), 80);
                } else {
                    currentPhotoFilter = String.format(Locale.US, "%d_%d", (int) (width / AndroidUtilities.density), 80);
                    currentPhotoFilterThumb = currentPhotoFilter + "_b";
                }
            } else {
                currentPhotoFilter = "52_52";
            }
            linkImageView.setAspectFit(documentAttachType == DOCUMENT_ATTACH_TYPE_STICKER);

            if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF) {
                if (documentAttach != null) {
                    linkImageView.setImage(documentAttach, null, currentPhotoObject != null ? currentPhotoObject.location : null, currentPhotoFilter, documentAttach.size, ext, false);
                } else {
                    linkImageView.setImage(null, url, null, null, currentPhotoObject != null ? currentPhotoObject.location : null, currentPhotoFilter, -1, ext, true);
                }
            } else {
                if (currentPhotoObject != null) {
                    linkImageView.setImage(currentPhotoObject.location, currentPhotoFilter, currentPhotoObjectThumb != null ? currentPhotoObjectThumb.location : null, currentPhotoFilterThumb, currentPhotoObject.size, ext, false);
                } else {
                    linkImageView.setImage(null, url, currentPhotoFilter, null, currentPhotoObjectThumb != null ? currentPhotoObjectThumb.location : null, currentPhotoFilterThumb, -1, ext, true);
                }
            }
            drawLinkImageView = true;
        }

        if (mediaWebpage) {
            setBackgroundDrawable(null);
                width = viewWidth;
                int height = MeasureSpec.getSize(heightMeasureSpec);
                if (height == 0) {
                    height = AndroidUtilities.dp(100);
                }
                setMeasuredDimension(width, height);
                int x = (width - AndroidUtilities.dp(24)) / 2;
                int y = (height - AndroidUtilities.dp(24)) / 2;
                radialProgress.setProgressRect(x, y, x + AndroidUtilities.dp(24), y + AndroidUtilities.dp(24));
                linkImageView.setImageCoords(0, 0, width, height);
        } else {
            setBackgroundResource(R.drawable.list_selector);
            int height = 0;
            if (titleLayout != null && titleLayout.getLineCount() != 0) {
                height += titleLayout.getLineBottom(titleLayout.getLineCount() - 1);
            }
            if (descriptionLayout != null && descriptionLayout.getLineCount() != 0) {
                height += descriptionLayout.getLineBottom(descriptionLayout.getLineCount() - 1);
            }
            if (linkLayout != null && linkLayout.getLineCount() > 0) {
                height += linkLayout.getLineBottom(linkLayout.getLineCount() - 1);
            }
            height = Math.max(AndroidUtilities.dp(52), height);
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Math.max(AndroidUtilities.dp(68), height + AndroidUtilities.dp(16)) + (needDivider ? 1 : 0));

            int maxPhotoWidth = AndroidUtilities.dp(52);
            int x = LocaleController.isRTL ? MeasureSpec.getSize(widthMeasureSpec) - AndroidUtilities.dp(8) - maxPhotoWidth : AndroidUtilities.dp(8);
            letterDrawable.setBounds(x, AndroidUtilities.dp(8), x + maxPhotoWidth, AndroidUtilities.dp(60));
            linkImageView.setImageCoords(x, AndroidUtilities.dp(8), maxPhotoWidth, maxPhotoWidth);
        }
    }

    public void setGif(TLRPC.Document document, boolean divider) {
        needDivider = divider;
        needShadow = false;
        documentAttach = document;
        mediaWebpage = true;
        requestLayout();
        updateButtonState(false);
    }

    public boolean isSticker() {
        return documentAttachType == DOCUMENT_ATTACH_TYPE_STICKER;
    }

    public boolean showingBitmap() {
        return linkImageView.getBitmap() != null;
    }

    public TLRPC.Document getDocument() {
        return documentAttach;
    }

    public void setScaled(boolean value) {
        scaled = value;
        lastUpdateTime = System.currentTimeMillis();
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (drawLinkImageView) {
            linkImageView.onDetachedFromWindow();
        }
        MediaController.getInstance().removeLoadingFileObserver(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (drawLinkImageView) {
            if (linkImageView.onAttachedToWindow()) {
                updateButtonState(false);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (Build.VERSION.SDK_INT >= 21 && getBackground() != null) {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                getBackground().setHotspot(event.getX(), event.getY());
            }
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (titleLayout != null) {
            canvas.save();
            canvas.translate(AndroidUtilities.dp(LocaleController.isRTL ? 8 : AndroidUtilities.leftBaseline), titleY);
            titleLayout.draw(canvas);
            canvas.restore();
        }

        if (descriptionLayout != null) {
            descriptionTextPaint.setColor(0xff8a8a8a);
            canvas.save();
            canvas.translate(AndroidUtilities.dp(LocaleController.isRTL ? 8 : AndroidUtilities.leftBaseline), descriptionY);
            descriptionLayout.draw(canvas);
            canvas.restore();
        }

        if (linkLayout != null) {
            descriptionTextPaint.setColor(Theme.MSG_LINK_TEXT_COLOR);
            canvas.save();
            canvas.translate(AndroidUtilities.dp(LocaleController.isRTL ? 8 : AndroidUtilities.leftBaseline), linkY);
            linkLayout.draw(canvas);
            canvas.restore();
        }

        if (!mediaWebpage) {
                letterDrawable.draw(canvas);
        }
        if (drawLinkImageView) {
            canvas.save();
            if (scaled && scale != 0.8f || !scaled && scale != 1.0f) {
                long newTime = System.currentTimeMillis();
                long dt = (newTime - lastUpdateTime);
                lastUpdateTime = newTime;
                if (scaled && scale != 0.8f) {
                    scale -= dt / 400.0f;
                    if (scale < 0.8f) {
                        scale = 0.8f;
                    }
                } else {
                    scale += dt / 400.0f;
                    if (scale > 1.0f) {
                        scale = 1.0f;
                    }
                }
                invalidate();
            }
            canvas.scale(scale, scale, getMeasuredWidth() / 2, getMeasuredHeight() / 2);
            linkImageView.draw(canvas);
            canvas.restore();
        }
        if (mediaWebpage && (documentAttachType == DOCUMENT_ATTACH_TYPE_PHOTO || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF)) {
            radialProgress.draw(canvas);
        }

        if (needDivider && !mediaWebpage) {
            if (LocaleController.isRTL) {
                canvas.drawLine(0, getMeasuredHeight() - 1, getMeasuredWidth() - AndroidUtilities.dp(AndroidUtilities.leftBaseline), getMeasuredHeight() - 1, paint);
            } else {
                canvas.drawLine(AndroidUtilities.dp(AndroidUtilities.leftBaseline), getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight() - 1, paint);
            }
        }

    }

    private Drawable getDrawableForCurrentState() {
        return buttonState == 1 ? Theme.inlistDrawable[Theme.INLIST_FILE] : null;
    }

    public void updateButtonState(boolean animated) {
        if (!mediaWebpage) {
            return;
        }
        String fileName = null;
        File cacheFile = null;
        if (documentAttach != null) {
            fileName = FileLoader.getAttachFileName(documentAttach);
            cacheFile = FileLoader.getPathToAttach(documentAttach);
        }
        if (fileName == null) {
            radialProgress.setBackground(null, false, false);
            return;
        }
        if (cacheFile.exists() && cacheFile.length() == 0) {
            cacheFile.delete();
        }
        if (!cacheFile.exists()) {
            MediaController.getInstance().addLoadingFileObserver(fileName, this);
            boolean progressVisible = true;
            buttonState = 1;
            Float progress = ImageLoader.getInstance().getFileProgress(fileName);
            float setProgress = progress != null ? progress : 0;
            radialProgress.setProgress(setProgress, false);
            radialProgress.setBackground(getDrawableForCurrentState(), progressVisible, animated);
            invalidate();
        } else {
            MediaController.getInstance().removeLoadingFileObserver(this);
            buttonState = -1;
            radialProgress.setBackground(getDrawableForCurrentState(), false, animated);
            invalidate();
        }
    }

    public void setDelegate(ContextLinkCellDelegate contextLinkCellDelegate) {
        delegate = contextLinkCellDelegate;
    }

    @Override
    public void onFailedDownload(String fileName) {
        updateButtonState(false);
    }

    @Override
    public void onSuccessDownload(String fileName) {
        radialProgress.setProgress(1, true);
        updateButtonState(true);
    }

    @Override
    public void onProgressDownload(String fileName, float progress) {
        radialProgress.setProgress(progress, true);
        if (buttonState != 1) {
            updateButtonState(false);
        }
    }

    @Override
    public void onProgressUpload(String fileName, float progress, boolean isEncrypted) {

    }

    @Override
    public int getObserverTag() {
        return TAG;
    }
}
