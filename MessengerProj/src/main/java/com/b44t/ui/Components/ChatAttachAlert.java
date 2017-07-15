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


package com.b44t.ui.Components;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MediaController;
import com.b44t.messenger.MessageObject;
import com.b44t.messenger.NotificationCenter;
import com.b44t.messenger.support.widget.LinearLayoutManager;
import com.b44t.messenger.R;
import com.b44t.messenger.support.widget.RecyclerView;
import com.b44t.messenger.TLRPC;
import com.b44t.ui.ActionBar.BottomSheet;
import com.b44t.ui.ActionBar.Theme;
import com.b44t.ui.Cells.PhotoAttachPhotoCell;
import com.b44t.ui.ChatActivity;
import com.b44t.ui.PhotoViewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class ChatAttachAlert extends BottomSheet implements NotificationCenter.NotificationCenterDelegate, PhotoViewer.PhotoViewerProvider, BottomSheet.BottomSheetDelegateInterface {

    public final static int ATTACH_BUTTON_IDX_CAMERA       = 0;
    public final static int ATTACH_BUTTON_IDX_VIDEO        = 1;
    public final static int ATTACH_BUTTON_IDX_GALLERY      = 2;
    public final static int ATTACH_BUTTON_IDX_EMPTY        = 3;
    public final static int ATTACH_BUTTON_IDX_FILE         = 4;
    public final static int ATTACH_BUTTON_IDX_CONTACT      = 5;
    public final static int ATTACH_BUTTON_IDX_MUSIC        = 6;
    public final static int ATTACH_BUTTON_IDX_SENDSELECTED = 7;
    public final static int ATTACH_BUTTON_COUNT            = 8;

    public interface ChatAttachViewDelegate {
        void didPressedButton(int button);
        View getRevealView();
    }

    private class InnerAnimator {
        private AnimatorSet animatorSet;
        private float startRadius;
    }

    private LinearLayoutManager attachPhotoLayoutManager;
    private PhotoAttachAdapter photoAttachAdapter;
    private ChatActivity baseFragment;
    private AttachButton sendPhotosButton;
    private View views[] = new View[20];
    private RecyclerListView attachPhotoRecyclerView;
    private View lineView;
    private EmptyTextProgressView progressView;
    private ArrayList<Holder> viewsCache = new ArrayList<>(8);
    private RecyclerListView listView;
    private LinearLayoutManager layoutManager;
    private Drawable shadowDrawable;
    private ViewGroup attachView;
    private ArrayList<InnerAnimator> innerAnimators = new ArrayList<>();

    private DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();

    private boolean loading = true;

    private ChatAttachViewDelegate delegate;

    private int scrollOffsetY;
    private boolean ignoreLayout;

    private boolean useRevealAnimation;
    private float revealRadius;
    private int revealX;
    private int revealY;
    private boolean revealAnimationInProgress;

    private class AttachButton extends FrameLayout {

        private TextView textView;
        private ImageView imageView;

        public AttachButton(Context context) {
            super(context);

            imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            addView(imageView, LayoutHelper.createFrame(64, 64, Gravity.CENTER_HORIZONTAL | Gravity.TOP));

            textView = new TextView(context);
            textView.setLines(1);
            textView.setSingleLine(true);
            textView.setGravity(Gravity.CENTER_HORIZONTAL);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setTextColor(Theme.ATTACH_SHEET_TEXT_COLOR);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.TOP, 0, 64, 0, 0));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(85), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(90), MeasureSpec.EXACTLY));
        }

        public void setTextAndIcon(CharSequence text, Drawable drawable) {
            textView.setText(text);
            imageView.setBackgroundDrawable(drawable);
        }

        @Override
        public boolean hasOverlappingRendering() {
            return false;
        }
    }

    public ChatAttachAlert(Context context) {
        super(context, false);
        setDelegate(this);
        setUseRevealAnimation(true);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.albumsDidLoaded);
        shadowDrawable = context.getResources().getDrawable(R.drawable.sheet_shadow);

        containerView = listView = new RecyclerListView(context) {
            @Override
            public void requestLayout() {
                if (ignoreLayout) {
                    return;
                }
                super.requestLayout();
            }

            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                if (ev.getAction() == MotionEvent.ACTION_DOWN && scrollOffsetY != 0 && ev.getY() < scrollOffsetY) {
                    dismiss();
                    return true;
                }
                return super.onInterceptTouchEvent(ev);
            }

            @Override
            public boolean onTouchEvent(MotionEvent e) {
                return !isDismissed() && super.onTouchEvent(e);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int height = MeasureSpec.getSize(heightMeasureSpec);
                if (Build.VERSION.SDK_INT >= 21) {
                    height -= AndroidUtilities.statusBarHeight;
                }
                int contentSize = backgroundPaddingTop + AndroidUtilities.dp(294);
                int padding = contentSize == AndroidUtilities.dp(294) ? 0 : (height - AndroidUtilities.dp(294));
                if (padding != 0 && contentSize < height) {
                    padding -= (height - contentSize);
                }
                if (padding == 0) {
                    padding = backgroundPaddingTop;
                }
                if (getPaddingTop() != padding) {
                    ignoreLayout = true;
                    setPadding(backgroundPaddingLeft, padding, backgroundPaddingLeft, 0);
                    ignoreLayout = false;
                }
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(Math.min(contentSize, height), MeasureSpec.EXACTLY));
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                updateLayout();
            }

            @Override
            public void onDraw(Canvas canvas) {
                if (useRevealAnimation && Build.VERSION.SDK_INT <= 19) {
                    canvas.save();
                    canvas.clipRect(backgroundPaddingLeft, scrollOffsetY, getMeasuredWidth() - backgroundPaddingLeft, getMeasuredHeight());
                    if (revealAnimationInProgress) {
                        canvas.drawCircle(revealX, revealY, revealRadius, ciclePaint);
                    } else {
                        canvas.drawRect(backgroundPaddingLeft, scrollOffsetY, getMeasuredWidth() - backgroundPaddingLeft, getMeasuredHeight(), ciclePaint);
                    }
                    canvas.restore();
                } else {
                    shadowDrawable.setBounds(0, scrollOffsetY - backgroundPaddingTop, getMeasuredWidth(), getMeasuredHeight());
                    shadowDrawable.draw(canvas);
                }
            }
        };

        listView.setTag(10);
        containerView.setWillNotDraw(false);
        listView.setClipToPadding(false);
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(getContext()));
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        listView.setAdapter(new ListAdapter(context));
        listView.setVerticalScrollBarEnabled(false);
        listView.setEnabled(true);
        listView.setGlowColor(0xfff5f6f7);
        listView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                outRect.left = 0;
                outRect.right = 0;
                outRect.top = 0;
                outRect.bottom = 0;
            }
        });

        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (listView.getChildCount() <= 0) {
                    return;
                }
                updateLayout();
            }
        });
        containerView.setPadding(backgroundPaddingLeft, 0, backgroundPaddingLeft, 0);

        attachView = new FrameLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(294), MeasureSpec.EXACTLY));
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                int width = right - left;
                int height = bottom - top;
                int t = AndroidUtilities.dp(8);
                attachPhotoRecyclerView.layout(0, t, width, t + attachPhotoRecyclerView.getMeasuredHeight());
                progressView.layout(0, t, width, t + progressView.getMeasuredHeight());
                lineView.layout(0, AndroidUtilities.dp(96), width, AndroidUtilities.dp(96) + lineView.getMeasuredHeight());

                int diff = (width - AndroidUtilities.dp(85 * 4 + 20)) / 3;
                for (int a = 0; a < 8; a++) {
                    int y = AndroidUtilities.dp(105 + 95 * (a / 4));
                    int x = AndroidUtilities.dp(10) + (a % 4) * (AndroidUtilities.dp(85) + diff);
                    views[a].layout(x, y, x + views[a].getMeasuredWidth(), y + views[a].getMeasuredHeight());
                }
            }
        };

        views[8] = attachPhotoRecyclerView = new RecyclerListView(context);
        attachPhotoRecyclerView.setVerticalScrollBarEnabled(true);
        attachPhotoRecyclerView.setAdapter(photoAttachAdapter = new PhotoAttachAdapter(context));
        attachPhotoRecyclerView.setClipToPadding(false);
        attachPhotoRecyclerView.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), 0);
        attachPhotoRecyclerView.setItemAnimator(null);
        attachPhotoRecyclerView.setLayoutAnimation(null);
        attachPhotoRecyclerView.setOverScrollMode(RecyclerListView.OVER_SCROLL_NEVER);
        attachView.addView(attachPhotoRecyclerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 80));
        attachPhotoLayoutManager = new LinearLayoutManager(context) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };
        attachPhotoLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        attachPhotoRecyclerView.setLayoutManager(attachPhotoLayoutManager);
        attachPhotoRecyclerView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void onItemClick(View view, int position) {
                if (baseFragment == null || baseFragment.getParentActivity() == null) {
                    return;
                }
                ArrayList<Object> arrayList = (ArrayList) MediaController.allPhotosAlbumEntry.photos;
                if (position < 0 || position >= arrayList.size()) {
                    return;
                }
                PhotoViewer.getInstance().setParentActivity(baseFragment.getParentActivity());
                PhotoViewer.getInstance().openPhotoForSelect(arrayList, position, 0, ChatAttachAlert.this, baseFragment);
                AndroidUtilities.hideKeyboard(baseFragment.getFragmentView().findFocus());
            }
        });

        views[9] = progressView = new EmptyTextProgressView(context);
        if (Build.VERSION.SDK_INT >= 23 && getContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            progressView.setText(context.getString(R.string.PermissionStorage));
            progressView.setTextSize(16);
        } else {
            progressView.setText(context.getString(R.string.NoPhotos));
            progressView.setTextSize(20);
        }
        attachView.addView(progressView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 80));
        attachPhotoRecyclerView.setEmptyView(progressView);

        views[10] = lineView = new View(getContext()) {
            @Override
            public boolean hasOverlappingRendering() {
                return false;
            }
        };
        lineView.setBackgroundColor(0xffd2d2d2);
        attachView.addView(lineView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1, Gravity.TOP | Gravity.START));

        CharSequence[] labels = new CharSequence[ATTACH_BUTTON_COUNT];
        labels[ATTACH_BUTTON_IDX_CAMERA]      = ApplicationLoader.applicationContext.getString(R.string.ChatCamera);
        labels[ATTACH_BUTTON_IDX_GALLERY]     = ApplicationLoader.applicationContext.getString(R.string.ChatGallery);
        labels[ATTACH_BUTTON_IDX_VIDEO]       = ApplicationLoader.applicationContext.getString(R.string.AttachVideo);
        labels[ATTACH_BUTTON_IDX_MUSIC]       = ApplicationLoader.applicationContext.getString(R.string.AttachMusic);
        labels[ATTACH_BUTTON_IDX_FILE]        = ApplicationLoader.applicationContext.getString(R.string.AttachDocument);
        labels[ATTACH_BUTTON_IDX_CONTACT]     = ApplicationLoader.applicationContext.getString(R.string.AttachContact);
        labels[ATTACH_BUTTON_IDX_EMPTY]       = "";
        labels[ATTACH_BUTTON_IDX_SENDSELECTED]= "";

        for (int a = 0; a < ATTACH_BUTTON_COUNT; a++) {
            AttachButton attachButton = new AttachButton(context);
            attachButton.setTextAndIcon(labels[a], Theme.attachButtonDrawables[a]);
            attachView.addView(attachButton, LayoutHelper.createFrame(85, 90, Gravity.START | Gravity.TOP));
            attachButton.setTag(a);
            views[a] = attachButton;
            if (a == ATTACH_BUTTON_IDX_SENDSELECTED) {
                sendPhotosButton = attachButton;
                sendPhotosButton.imageView.setPadding(0, AndroidUtilities.dp(4), 0, 0);
            }
            attachButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    delegate.didPressedButton((Integer) v.getTag());
                }
            });
        }

        for (int a = 0; a < 8; a++) {
            viewsCache.add(photoAttachAdapter.createHolder());
        }

        if (loading) {
            progressView.showProgress();
        } else {
            progressView.showTextView();
        }
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.albumsDidLoaded) {
            if (photoAttachAdapter != null) {
                loading = false;
                progressView.showTextView();
                photoAttachAdapter.notifyDataSetChanged();
            }
        }
    }

    @SuppressLint("NewApi")
    private void updateLayout() {
        if (listView.getChildCount() <= 0) {
            listView.setTopGlowOffset(scrollOffsetY = listView.getPaddingTop());
            containerView.invalidate();
            return;
        }
        View child = listView.getChildAt(0);
        Holder holder = (Holder) listView.findContainingViewHolder(child);
        int top = child.getTop();
        int newOffset = 0;
        if (top >= 0 && holder != null && holder.getAdapterPosition() == 0) {
            newOffset = top;
        }
        if (scrollOffsetY != newOffset) {
            listView.setTopGlowOffset(scrollOffsetY = newOffset);
            containerView.invalidate();
        }
    }

    @Override
    protected boolean canDismissWithSwipe() {
        return false;
    }

    public void updatePhotosButton() {
        int count = photoAttachAdapter.getSelectedPhotos().size();
        if (count == 0) {
            sendPhotosButton.imageView.setPadding(0, AndroidUtilities.dp(4), 0, 0);
            sendPhotosButton.imageView.setBackgroundResource(R.drawable.attach_hide_states);
            sendPhotosButton.textView.setText("");
        } else {
            sendPhotosButton.imageView.setPadding(AndroidUtilities.dp(2), 0, 0, 0);
            sendPhotosButton.imageView.setBackgroundResource(R.drawable.attach_send_states);
            sendPhotosButton.textView.setText(String.format(ApplicationLoader.applicationContext.getString(R.string.SendItems), String.format(Locale.getDefault(), "%d", count)));
        }

        if (Build.VERSION.SDK_INT >= 23 && getContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            progressView.setText(ApplicationLoader.applicationContext.getString(R.string.PermissionStorage));
            progressView.setTextSize(16);
        } else {
            progressView.setText(ApplicationLoader.applicationContext.getString(R.string.NoPhotos));
            progressView.setTextSize(20);
        }
    }

    public void setDelegate(ChatAttachViewDelegate chatAttachViewDelegate) {
        delegate = chatAttachViewDelegate;
    }

    public void loadGalleryPhotos() {
        if (MediaController.allPhotosAlbumEntry == null && Build.VERSION.SDK_INT >= 21) {
            MediaController.loadGalleryPhotosAlbums(0);
        }
    }

    public void init(ChatActivity parentFragment) {
        if (MediaController.allPhotosAlbumEntry != null) {
            for (int a = 0; a < Math.min(100, MediaController.allPhotosAlbumEntry.photos.size()); a++) {
                MediaController.PhotoEntry photoEntry = MediaController.allPhotosAlbumEntry.photos.get(a);
                photoEntry.caption = null;
                photoEntry.imagePath = null;
                photoEntry.thumbPath = null;
            }
        }

        attachPhotoLayoutManager.scrollToPositionWithOffset(0, 1000000);
        photoAttachAdapter.clearSelectedPhotos();
        baseFragment = parentFragment;
        layoutManager.scrollToPositionWithOffset(0, 1000000);
        updatePhotosButton();
    }

    public HashMap<Integer, MediaController.PhotoEntry> getSelectedPhotos() {
        return photoAttachAdapter.getSelectedPhotos();
    }

    public void onDestroy() {
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.albumsDidLoaded);
        baseFragment = null;
    }

    private PhotoAttachPhotoCell getCellForIndex(int index) {
        if (MediaController.allPhotosAlbumEntry == null) {
            return null;
        }
        int count = attachPhotoRecyclerView.getChildCount();
        for (int a = 0; a < count; a++) {
            View view = attachPhotoRecyclerView.getChildAt(a);
            if (view instanceof PhotoAttachPhotoCell) {
                PhotoAttachPhotoCell cell = (PhotoAttachPhotoCell) view;
                int num = (Integer) cell.getImageView().getTag();
                if (num < 0 || num >= MediaController.allPhotosAlbumEntry.photos.size()) {
                    continue;
                }
                if (num == index) {
                    return cell;
                }
            }
        }
        return null;
    }

    @Override
    public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        PhotoAttachPhotoCell cell = getCellForIndex(index);
        if (cell != null) {
            int coords[] = new int[2];
            cell.getImageView().getLocationInWindow(coords);
            PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
            object.viewX = coords[0];
            object.viewY = coords[1] - (Build.VERSION.SDK_INT >= 21 ? AndroidUtilities.statusBarHeight : 0);
            object.parentView = attachPhotoRecyclerView;
            object.imageReceiver = cell.getImageView().getImageReceiver();
            object.thumb = object.imageReceiver.getBitmap();
            object.scale = cell.getImageView().getScaleX();
            object.clipBottomAddition = (Build.VERSION.SDK_INT >= 21 ? 0 : -AndroidUtilities.statusBarHeight);
            cell.getCheckBox().setVisibility(View.GONE);
            return object;
        }
        return null;
    }

    @Override
    public void updatePhotoAtIndex(int index) {
        PhotoAttachPhotoCell cell = getCellForIndex(index);
        if (cell != null) {
            cell.getImageView().setOrientation(0, true);
            MediaController.PhotoEntry photoEntry = MediaController.allPhotosAlbumEntry.photos.get(index);
            if (photoEntry.thumbPath != null) {
                cell.getImageView().setImage(photoEntry.thumbPath, null, cell.getContext().getResources().getDrawable(R.drawable.nophotos));
            } else if (photoEntry.path != null) {
                cell.getImageView().setOrientation(photoEntry.orientation, true);
                cell.getImageView().setImage("thumb://" + photoEntry.imageId + ":" + photoEntry.path, null, cell.getContext().getResources().getDrawable(R.drawable.nophotos));
            } else {
                cell.getImageView().setImageResource(R.drawable.nophotos);
            }
        }
    }

    @Override
    public Bitmap getThumbForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        PhotoAttachPhotoCell cell = getCellForIndex(index);
        if (cell != null) {
            return cell.getImageView().getImageReceiver().getBitmap();
        }
        return null;
    }

    @Override
    public void willSwitchFromPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        PhotoAttachPhotoCell cell = getCellForIndex(index);
        if (cell != null) {
            cell.getCheckBox().setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void willHidePhotoViewer() {
        int count = attachPhotoRecyclerView.getChildCount();
        for (int a = 0; a < count; a++) {
            View view = attachPhotoRecyclerView.getChildAt(a);
            if (view instanceof PhotoAttachPhotoCell) {
                PhotoAttachPhotoCell cell = (PhotoAttachPhotoCell) view;
                if (cell.getCheckBox().getVisibility() != View.VISIBLE) {
                    cell.getCheckBox().setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public boolean isPhotoChecked(int index) {
        return !(index < 0 || index >= MediaController.allPhotosAlbumEntry.photos.size()) && photoAttachAdapter.getSelectedPhotos().containsKey(MediaController.allPhotosAlbumEntry.photos.get(index).imageId);
    }

    @Override
    public void setPhotoChecked(int index) {
        boolean add = true;
        if (index < 0 || index >= MediaController.allPhotosAlbumEntry.photos.size()) {
            return;
        }
        MediaController.PhotoEntry photoEntry = MediaController.allPhotosAlbumEntry.photos.get(index);
        if (photoAttachAdapter.getSelectedPhotos().containsKey(photoEntry.imageId)) {
            photoAttachAdapter.getSelectedPhotos().remove(photoEntry.imageId);
            add = false;
        } else {
            photoAttachAdapter.getSelectedPhotos().put(photoEntry.imageId, photoEntry);
        }
        int count = attachPhotoRecyclerView.getChildCount();
        for (int a = 0; a < count; a++) {
            View view = attachPhotoRecyclerView.getChildAt(a);
            int num = (Integer) view.getTag();
            if (num == index) {
                ((PhotoAttachPhotoCell) view).setChecked(add, false);
                break;
            }
        }
        updatePhotosButton();
    }

    @Override
    public boolean cancelButtonPressed() {
        return false;
    }

    @Override
    public void sendButtonPressed(int index) {
        if (photoAttachAdapter.getSelectedPhotos().isEmpty()) {
            if (index < 0 || index >= MediaController.allPhotosAlbumEntry.photos.size()) {
                return;
            }
            MediaController.PhotoEntry photoEntry = MediaController.allPhotosAlbumEntry.photos.get(index);
            photoAttachAdapter.getSelectedPhotos().put(photoEntry.imageId, photoEntry);
        }
        delegate.didPressedButton(ATTACH_BUTTON_IDX_SENDSELECTED);
    }

    @Override
    public int getSelectedCount() {
        return photoAttachAdapter.getSelectedPhotos().size();
    }

    private void onRevealAnimationEnd(boolean open) {
        NotificationCenter.getInstance().setAnimationInProgress(false);
        revealAnimationInProgress = false;
        if (open && Build.VERSION.SDK_INT <= 19 && MediaController.allPhotosAlbumEntry == null) {
            MediaController.loadGalleryPhotosAlbums(0);
        }
    }

    @Override
    public void onOpenAnimationEnd() {
        onRevealAnimationEnd(true);
    }

    @Override
    public void onOpenAnimationStart() {

    }

    private class Holder extends RecyclerView.ViewHolder {

        public Holder(View itemView) {
            super(itemView);
        }
    }

    private class ListAdapter extends RecyclerView.Adapter {

        private final int TYPE_ATTACH_CELLS = 0;

        public ListAdapter(Context context) {
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // currently, we only use one viewType, TYPE_ATTACH_CELLS
            return new Holder(attachView);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        }

        @Override
        public int getItemCount() {
            return 1;
        }

        @Override
        public int getItemViewType(int position) {
            return TYPE_ATTACH_CELLS;
        }
    }

    private class PhotoAttachAdapter extends RecyclerView.Adapter {

        private final int TYPE_ATTACH_PHOTO_CELL = 0;

        private Context mContext;
        private HashMap<Integer, MediaController.PhotoEntry> selectedPhotos = new HashMap<>();

        public PhotoAttachAdapter(Context context) {
            mContext = context;
        }

        public void clearSelectedPhotos() {
            if (!selectedPhotos.isEmpty()) {
                for (HashMap.Entry<Integer, MediaController.PhotoEntry> entry : selectedPhotos.entrySet()) {
                    MediaController.PhotoEntry photoEntry = entry.getValue();
                    photoEntry.imagePath = null;
                    photoEntry.thumbPath = null;
                    photoEntry.caption = null;
                }
                selectedPhotos.clear();
                updatePhotosButton();
                notifyDataSetChanged();
            }
        }

        public Holder createHolder() {
            PhotoAttachPhotoCell cell = new PhotoAttachPhotoCell(mContext);
            cell.setDelegate(new PhotoAttachPhotoCell.PhotoAttachPhotoCellDelegate() {
                @Override
                public void onCheckClick(PhotoAttachPhotoCell v) {
                    MediaController.PhotoEntry photoEntry = v.getPhotoEntry();
                    if (selectedPhotos.containsKey(photoEntry.imageId)) {
                        selectedPhotos.remove(photoEntry.imageId);
                        v.setChecked(false, true);
                        photoEntry.imagePath = null;
                        photoEntry.thumbPath = null;
                        v.setPhotoEntry(photoEntry, (Integer) v.getTag() == MediaController.allPhotosAlbumEntry.photos.size() - 1);
                    } else {
                        selectedPhotos.put(photoEntry.imageId, photoEntry);
                        v.setChecked(true, true);
                    }
                    updatePhotosButton();
                }
            });
            return new Holder(cell);
        }

        public HashMap<Integer, MediaController.PhotoEntry> getSelectedPhotos() {
            return selectedPhotos;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            PhotoAttachPhotoCell cell = (PhotoAttachPhotoCell) holder.itemView;
            MediaController.PhotoEntry photoEntry = MediaController.allPhotosAlbumEntry.photos.get(position);
            cell.setPhotoEntry(photoEntry, position == MediaController.allPhotosAlbumEntry.photos.size() - 1);
            cell.setChecked(selectedPhotos.containsKey(photoEntry.imageId), false);
            cell.getImageView().setTag(position);
            cell.setTag(position);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // currently, we only use one viewType, TYPE_ATTACH_PHOTO_CELL
            Holder holder;
            if (!viewsCache.isEmpty()) {
                holder = viewsCache.get(0);
                viewsCache.remove(0);
            } else {
                holder = createHolder();
            }
            return holder;
        }

        @Override
        public int getItemCount() {
            int count = 0;
            if (MediaController.allPhotosAlbumEntry != null) {
                count += MediaController.allPhotosAlbumEntry.photos.size();
            }
            return count;
        }

        @Override
        public int getItemViewType(int position) {
            return TYPE_ATTACH_PHOTO_CELL;
        }
    }

    private void setUseRevealAnimation(boolean value) {
        if (!value || value && Build.VERSION.SDK_INT >= 18) {
            useRevealAnimation = value;
        }
    }

    @SuppressLint("NewApi")
    protected void setRevealRadius(float radius) {
        revealRadius = radius;
        if (Build.VERSION.SDK_INT <= 19) {
            containerView.invalidate();
        }
        if (!isDismissed()) {
            for (int a = 0; a < innerAnimators.size(); a++) {
                InnerAnimator innerAnimator = innerAnimators.get(a);
                if (innerAnimator.startRadius > radius) {
                    continue;
                }
                innerAnimator.animatorSet.start();
                innerAnimators.remove(a);
                a--;
            }
        }
    }

    protected float getRevealRadius() {
        return revealRadius;
    }

    @SuppressLint("NewApi")
    private void startRevealAnimation(final boolean open) {
        containerView.setTranslationY(0);

        final AnimatorSet animatorSet = new AnimatorSet();

        View view = delegate.getRevealView();
        if (view.getVisibility() == View.VISIBLE && ((ViewGroup) view.getParent()).getVisibility() == View.VISIBLE) {
            final int coords[] = new int[2];
            view.getLocationInWindow(coords);
            float top;
            if (Build.VERSION.SDK_INT <= 19) {
                top = AndroidUtilities.displaySize.y - containerView.getMeasuredHeight() - AndroidUtilities.statusBarHeight;
            } else {
                top = containerView.getY();
            }
            revealX = coords[0] + view.getMeasuredWidth() / 2;
            revealY = (int) (coords[1] + view.getMeasuredHeight() / 2 - top);
            if (Build.VERSION.SDK_INT <= 19) {
                revealY -= AndroidUtilities.statusBarHeight;
            }
        } else {
            revealX = AndroidUtilities.displaySize.x / 2 + backgroundPaddingLeft;
            revealY = (int) (AndroidUtilities.displaySize.y - containerView.getY());
        }

        int corners[][] = new int[][]{
                {0, 0},
                {0, AndroidUtilities.dp(304)},
                {containerView.getMeasuredWidth(), 0},
                {containerView.getMeasuredWidth(), AndroidUtilities.dp(304)}
        };
        int finalRevealRadius = 0;
        int y = revealY - scrollOffsetY + backgroundPaddingTop;
        for (int a = 0; a < 4; a++) {
            finalRevealRadius = Math.max(finalRevealRadius, (int) Math.ceil(Math.sqrt((revealX - corners[a][0]) * (revealX - corners[a][0]) + (y - corners[a][1]) * (y - corners[a][1]))));
        }
        int finalRevealX = revealX <= containerView.getMeasuredWidth() ? revealX : containerView.getMeasuredWidth();

        ArrayList<Animator> animators = new ArrayList<>(3);
        animators.add(ObjectAnimator.ofFloat(this, "revealRadius", open ? 0 : finalRevealRadius, open ? finalRevealRadius : 0));
        animators.add(ObjectAnimator.ofInt(backDrawable, "alpha", open ? 51 : 0));
        if (Build.VERSION.SDK_INT >= 21) {
            containerView.setElevation(AndroidUtilities.dp(10));
            try {
                animators.add(ViewAnimationUtils.createCircularReveal(containerView, finalRevealX, revealY, open ? 0 : finalRevealRadius, open ? finalRevealRadius : 0));
            } catch (Exception e) {

            }
            animatorSet.setDuration(320);
        } else {
            if (!open) {
                animatorSet.setDuration(200);
                containerView.setPivotX(revealX <= containerView.getMeasuredWidth() ? revealX : containerView.getMeasuredWidth());
                containerView.setPivotY(revealY);
                animators.add(ObjectAnimator.ofFloat(containerView, "scaleX", 0.0f));
                animators.add(ObjectAnimator.ofFloat(containerView, "scaleY", 0.0f));
                animators.add(ObjectAnimator.ofFloat(containerView, "alpha", 0.0f));
            } else {
                animatorSet.setDuration(250);
                containerView.setScaleX(1);
                containerView.setScaleY(1);
                containerView.setAlpha(1);
                if (Build.VERSION.SDK_INT <= 19) {
                    animatorSet.setStartDelay(20);
                }
            }
        }
        animatorSet.playTogether(animators);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (currentSheetAnimation != null && currentSheetAnimation.equals(animation)) {
                    currentSheetAnimation = null;
                    onRevealAnimationEnd(open);
                    containerView.invalidate();
                    containerView.setLayerType(View.LAYER_TYPE_NONE, null);
                    if (!open) {
                        containerView.setVisibility(View.INVISIBLE);
                        try {
                            dismissInternal();
                        } catch (Exception e) {

                        }
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (currentSheetAnimation != null && animatorSet.equals(animation)) {
                    currentSheetAnimation = null;
                }
            }
        });

        if (open) {
            innerAnimators.clear();
            NotificationCenter.getInstance().setAllowedNotificationsDutingAnimation(new int[]{NotificationCenter.dialogsNeedReload});
            NotificationCenter.getInstance().setAnimationInProgress(true);
            revealAnimationInProgress = true;

            int count = Build.VERSION.SDK_INT <= 19 ? 11 : 8;
            for (int a = 0; a < count; a++) {
                if (Build.VERSION.SDK_INT <= 19) {
                    if (a < 8) {
                        views[a].setScaleX(0.1f);
                        views[a].setScaleY(0.1f);
                    }
                    views[a].setAlpha(0.0f);
                } else {
                    views[a].setScaleX(0.7f);
                    views[a].setScaleY(0.7f);
                }

                InnerAnimator innerAnimator = new InnerAnimator();

                int buttonX = views[a].getLeft() + views[a].getMeasuredWidth() / 2;
                int buttonY = views[a].getTop() + attachView.getTop() + views[a].getMeasuredHeight() / 2;
                float dist = (float) Math.sqrt((revealX - buttonX) * (revealX - buttonX) + (revealY - buttonY) * (revealY - buttonY));
                float vecX = (revealX - buttonX) / dist;
                float vecY = (revealY - buttonY) / dist;
                views[a].setPivotX(views[a].getMeasuredWidth() / 2 + vecX * AndroidUtilities.dp(20));
                views[a].setPivotY(views[a].getMeasuredHeight() / 2 + vecY * AndroidUtilities.dp(20));
                innerAnimator.startRadius = dist - AndroidUtilities.dp(27 * 3);

                views[a].setTag(R.string.AppName, 1);
                animators = new ArrayList<>();
                final AnimatorSet animatorSetInner;
                if (a < 8) {
                    animators.add(ObjectAnimator.ofFloat(views[a], "scaleX", 0.7f, 1.05f));
                    animators.add(ObjectAnimator.ofFloat(views[a], "scaleY", 0.7f, 1.05f));

                    animatorSetInner = new AnimatorSet();
                    animatorSetInner.playTogether(
                            ObjectAnimator.ofFloat(views[a], "scaleX", 1.0f),
                            ObjectAnimator.ofFloat(views[a], "scaleY", 1.0f));
                    animatorSetInner.setDuration(100);
                    animatorSetInner.setInterpolator(decelerateInterpolator);
                } else {
                    animatorSetInner = null;
                }
                if (Build.VERSION.SDK_INT <= 19) {
                    animators.add(ObjectAnimator.ofFloat(views[a], "alpha", 1.0f));
                }
                innerAnimator.animatorSet = new AnimatorSet();
                innerAnimator.animatorSet.playTogether(animators);
                innerAnimator.animatorSet.setDuration(150);
                innerAnimator.animatorSet.setInterpolator(decelerateInterpolator);
                innerAnimator.animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (animatorSetInner != null) {
                            animatorSetInner.start();
                        }
                    }
                });
                innerAnimators.add(innerAnimator);
            }
        }
        currentSheetAnimation = animatorSet;
        animatorSet.start();
    }

    @Override
    protected boolean onCustomOpenAnimation() {
        if (useRevealAnimation) {
            startRevealAnimation(true);
            return true;
        }
        return false;
    }

    @Override
    protected boolean onCustomCloseAnimation() {
        if (useRevealAnimation) {
            backDrawable.setAlpha(51);
            startRevealAnimation(false);
            return true;
        }
        return false;
    }
}
