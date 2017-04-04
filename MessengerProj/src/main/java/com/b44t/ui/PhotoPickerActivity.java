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


package com.b44t.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MediaController;
import com.b44t.messenger.NotificationCenter;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.R;
import com.b44t.messenger.TLRPC;
import com.b44t.messenger.MessageObject;
import com.b44t.ui.ActionBar.Theme;
import com.b44t.ui.Adapters.BaseFragmentAdapter;
import com.b44t.ui.ActionBar.ActionBar;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.Cells.PhotoPickerPhotoCell;
import com.b44t.ui.Components.BackupImageView;
import com.b44t.ui.Components.LayoutHelper;
import com.b44t.ui.Components.PickerBottomLayout;

import java.util.ArrayList;
import java.util.HashMap;

public class PhotoPickerActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, PhotoViewer.PhotoViewerProvider {

    public interface PhotoPickerActivityDelegate {
        void selectedPhotosChanged();

        void actionButtonPressed(boolean canceled);

        boolean didSelectVideo(String path);
    }

    private HashMap<Integer, MediaController.PhotoEntry> selectedPhotos;

    private MediaController.AlbumEntry selectedAlbum;

    private GridView listView;
    private ListAdapter listAdapter;
    private PickerBottomLayout pickerBottomLayout;
    private int itemWidth = 100;
    private boolean sendPressed;
    private boolean singlePhoto;
    private ChatActivity chatActivity;

    private PhotoPickerActivityDelegate delegate;

    public PhotoPickerActivity(MediaController.AlbumEntry selectedAlbum, HashMap<Integer, MediaController.PhotoEntry> selectedPhotos, boolean onlyOnePhoto, ChatActivity chatActivity) {
        super();
        this.selectedAlbum = selectedAlbum; // must never be null
        this.selectedPhotos = selectedPhotos;
        this.singlePhoto = onlyOnePhoto;
        this.chatActivity = chatActivity;
        if (selectedAlbum.isVideo) {
            singlePhoto = true;
        }
    }

    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.closeChats);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.closeChats);
        super.onFragmentDestroy();
    }

    @SuppressWarnings("unchecked")
    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(selectedAlbum.bucketName);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);

        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(0xffffffff);

        listView = new GridView(context);
        listView.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
        listView.setClipToPadding(false);
        listView.setDrawSelectorOnTop(true);
        listView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        listView.setHorizontalScrollBarEnabled(false);
        listView.setVerticalScrollBarEnabled(false);
        listView.setNumColumns(GridView.AUTO_FIT);
        listView.setVerticalSpacing(AndroidUtilities.dp(4));
        listView.setHorizontalSpacing(AndroidUtilities.dp(4));
        listView.setSelector(R.drawable.list_selector);
        frameLayout.addView(listView);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        layoutParams.bottomMargin = singlePhoto ? 0 : AndroidUtilities.dp(48);
        listView.setLayoutParams(layoutParams);
        listView.setAdapter(listAdapter = new ListAdapter(context));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (selectedAlbum.isVideo) {
                    if (i < 0 || i >= selectedAlbum.photos.size()) {
                        return;
                    }
                    if (delegate.didSelectVideo(selectedAlbum.photos.get(i).path)) {
                        finishFragment();
                    }
                } else {
                    ArrayList<Object> arrayList = (ArrayList) selectedAlbum.photos;
                    if (i < 0 || i >= arrayList.size()) {
                        return;
                    }
                    PhotoViewer.getInstance().setParentActivity(getParentActivity());
                    PhotoViewer.getInstance().openPhotoForSelect(arrayList, i, singlePhoto ? 1 : 0, PhotoPickerActivity.this, chatActivity);
                }
            }
        });

        TextView emptyView = new TextView(context);
        emptyView.setTextColor(0xff808080);
        emptyView.setTextSize(20);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        emptyView.setText(LocaleController.getString("NoPhotos", R.string.NoPhotos));

        frameLayout.addView(emptyView);
        layoutParams = (FrameLayout.LayoutParams) emptyView.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        layoutParams.bottomMargin = singlePhoto ? 0 : AndroidUtilities.dp(48);
        emptyView.setLayoutParams(layoutParams);
        emptyView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        pickerBottomLayout = new PickerBottomLayout(context, false);
        frameLayout.addView(pickerBottomLayout);
        layoutParams = (FrameLayout.LayoutParams) pickerBottomLayout.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = AndroidUtilities.dp(48);
        layoutParams.gravity = Gravity.BOTTOM;
        pickerBottomLayout.setLayoutParams(layoutParams);
        pickerBottomLayout.cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delegate.actionButtonPressed(true);
                finishFragment();
            }
        });
        pickerBottomLayout.doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendSelectedPhotos();
            }
        });

        listView.setEmptyView(emptyView);
        pickerBottomLayout.updateSelectedCount(selectedPhotos.size(), true);

        if(singlePhoto) {
            pickerBottomLayout.setVisibility(View.GONE);
        }
        else {
            View shadow = new View(context);
            shadow.setBackgroundResource(R.drawable.header_shadow_reverse);
            frameLayout.addView(shadow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 3, Gravity.LEFT | Gravity.BOTTOM, 0, 0, 0, 48));
        }

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
        fixLayout();
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fixLayout();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.closeChats) {
            removeSelfFromStack();
        }
    }

    private PhotoPickerPhotoCell getCellForIndex(int index) {
        int count = listView.getChildCount();

        for (int a = 0; a < count; a++) {
            View view = listView.getChildAt(a);
            if (view instanceof PhotoPickerPhotoCell) {
                PhotoPickerPhotoCell cell = (PhotoPickerPhotoCell) view;
                int num = (Integer) cell.photoImage.getTag();
                if (num < 0 || num >= selectedAlbum.photos.size()) {
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
        PhotoPickerPhotoCell cell = getCellForIndex(index);
        if (cell != null) {
            int coords[] = new int[2];
            cell.photoImage.getLocationInWindow(coords);
            PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
            object.viewX = coords[0];
            object.viewY = coords[1] - AndroidUtilities.statusBarHeight;
            object.parentView = listView;
            object.imageReceiver = cell.photoImage.getImageReceiver();
            object.thumb = object.imageReceiver.getBitmap();
            object.scale = cell.photoImage.getScaleX();
            cell.checkBox.setVisibility(View.GONE);
            return object;
        }
        return null;
    }

    @Override
    public void updatePhotoAtIndex(int index) {
        PhotoPickerPhotoCell cell = getCellForIndex(index);
        if (cell != null) {
                cell.photoImage.setOrientation(0, true);
                MediaController.PhotoEntry photoEntry = selectedAlbum.photos.get(index);
                if (photoEntry.thumbPath != null) {
                    cell.photoImage.setImage(photoEntry.thumbPath, null, cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                } else if (photoEntry.path != null) {
                    cell.photoImage.setOrientation(photoEntry.orientation, true);
                    if (photoEntry.isVideo) {
                        cell.photoImage.setImage("vthumb://" + photoEntry.imageId + ":" + photoEntry.path, null, cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                    } else {
                        cell.photoImage.setImage("thumb://" + photoEntry.imageId + ":" + photoEntry.path, null, cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                    }
                } else {
                    cell.photoImage.setImageResource(R.drawable.nophotos);
                }
        }
    }

    @Override
    public Bitmap getThumbForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        PhotoPickerPhotoCell cell = getCellForIndex(index);
        if (cell != null) {
            return cell.photoImage.getImageReceiver().getBitmap();
        }
        return null;
    }

    @Override
    public void willSwitchFromPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        int count = listView.getChildCount();
        for (int a = 0; a < count; a++) {
            View view = listView.getChildAt(a);
            if (view.getTag() == null) {
                continue;
            }
            PhotoPickerPhotoCell cell = (PhotoPickerPhotoCell) view;
            int num = (Integer) view.getTag();
                if (num < 0 || num >= selectedAlbum.photos.size()) {
                    continue;
                }
            if (num == index) {
                cell.checkBox.setVisibility(View.VISIBLE);
                break;
            }
        }
    }

    @Override
    public void willHidePhotoViewer() {
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean isPhotoChecked(int index) {
            return !(index < 0 || index >= selectedAlbum.photos.size()) && selectedPhotos.containsKey(selectedAlbum.photos.get(index).imageId);
    }

    @Override
    public void setPhotoChecked(int index) {
        boolean add = true;
            if (index < 0 || index >= selectedAlbum.photos.size()) {
                return;
            }
            MediaController.PhotoEntry photoEntry = selectedAlbum.photos.get(index);
            if (selectedPhotos.containsKey(photoEntry.imageId)) {
                selectedPhotos.remove(photoEntry.imageId);
                add = false;
            } else {
                selectedPhotos.put(photoEntry.imageId, photoEntry);
            }
        int count = listView.getChildCount();
        for (int a = 0; a < count; a++) {
            View view = listView.getChildAt(a);
            int num = (Integer) view.getTag();
            if (num == index) {
                ((PhotoPickerPhotoCell) view).setChecked(add, false);
                break;
            }
        }
        pickerBottomLayout.updateSelectedCount(selectedPhotos.size(), true);
        delegate.selectedPhotosChanged();
    }

    @Override
    public boolean cancelButtonPressed() {
        delegate.actionButtonPressed(true);
        finishFragment();
        return true;
    }

    @Override
    public void sendButtonPressed(int index) {
            if (selectedPhotos.isEmpty()) {
                if (index < 0 || index >= selectedAlbum.photos.size()) {
                    return;
                }
                MediaController.PhotoEntry photoEntry = selectedAlbum.photos.get(index);
                selectedPhotos.put(photoEntry.imageId, photoEntry);
            }
        sendSelectedPhotos();
    }

    @Override
    public int getSelectedCount() {
        return selectedPhotos.size();
    }

    public void setDelegate(PhotoPickerActivityDelegate delegate) {
        this.delegate = delegate;
    }

    private void sendSelectedPhotos() {
        if (selectedPhotos.isEmpty() || delegate == null || sendPressed) {
            return;
        }
        sendPressed = true;
        delegate.actionButtonPressed(false);
        finishFragment();
    }

    private void fixLayout() {
        if (listView != null) {
            ViewTreeObserver obs = listView.getViewTreeObserver();
            obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    fixLayoutInternal();
                    if (listView != null) {
                        listView.getViewTreeObserver().removeOnPreDrawListener(this);
                    }
                    return true;
                }
            });
        }
    }

    private void fixLayoutInternal() {
        if (getParentActivity() == null) {
            return;
        }
        int position = listView.getFirstVisiblePosition();
        WindowManager manager = (WindowManager) ApplicationLoader.applicationContext.getSystemService(Activity.WINDOW_SERVICE);
        int rotation = manager.getDefaultDisplay().getRotation();

        int columnsCount;
        if (AndroidUtilities.isTablet()) {
            columnsCount = 3;
        } else {
            if (rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90) {
                columnsCount = 5;
            } else {
                columnsCount = 3;
            }
        }
        listView.setNumColumns(columnsCount);
        if (AndroidUtilities.isTablet()) {
            itemWidth = (AndroidUtilities.dp(490) - ((columnsCount + 1) * AndroidUtilities.dp(4))) / columnsCount;
        } else {
            itemWidth = (AndroidUtilities.displaySize.x - ((columnsCount + 1) * AndroidUtilities.dp(4))) / columnsCount;
        }
        listView.setColumnWidth(itemWidth);

        listAdapter.notifyDataSetChanged();
        listView.setSelection(position);
    }

    private class ListAdapter extends BaseFragmentAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int i) {
            return true;
        }

        @Override
        public int getCount() {
            return selectedAlbum.photos.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
                PhotoPickerPhotoCell cell = (PhotoPickerPhotoCell) view;
                if (view == null) {
                    view = new PhotoPickerPhotoCell(mContext);
                    cell = (PhotoPickerPhotoCell) view;
                    cell.checkFrame.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int index = (Integer) ((View) v.getParent()).getTag();
                                MediaController.PhotoEntry photoEntry = selectedAlbum.photos.get(index);
                                if (selectedPhotos.containsKey(photoEntry.imageId)) {
                                    selectedPhotos.remove(photoEntry.imageId);
                                    photoEntry.imagePath = null;
                                    photoEntry.thumbPath = null;
                                    updatePhotoAtIndex(index);
                                } else {
                                    selectedPhotos.put(photoEntry.imageId, photoEntry);
                                }
                                ((PhotoPickerPhotoCell) v.getParent()).setChecked(selectedPhotos.containsKey(photoEntry.imageId), true);
                            pickerBottomLayout.updateSelectedCount(selectedPhotos.size(), true);
                            delegate.selectedPhotosChanged();
                        }
                    });
                    cell.checkFrame.setVisibility(singlePhoto ? View.GONE : View.VISIBLE);
                }
                cell.itemWidth = itemWidth;
                BackupImageView imageView = ((PhotoPickerPhotoCell) view).photoImage;
                imageView.setTag(i);
                view.setTag(i);
                boolean showing;
                imageView.setOrientation(0, true);

                    MediaController.PhotoEntry photoEntry = selectedAlbum.photos.get(i);
                    if (photoEntry.thumbPath != null) {
                        imageView.setImage(photoEntry.thumbPath, null, mContext.getResources().getDrawable(R.drawable.nophotos));
                    } else if (photoEntry.path != null) {
                        imageView.setOrientation(photoEntry.orientation, true);
                        if (photoEntry.isVideo) {
                            imageView.setImage("vthumb://" + photoEntry.imageId + ":" + photoEntry.path, null, mContext.getResources().getDrawable(R.drawable.nophotos));
                        } else {
                            imageView.setImage("thumb://" + photoEntry.imageId + ":" + photoEntry.path, null, mContext.getResources().getDrawable(R.drawable.nophotos));
                        }
                    } else {
                        imageView.setImageResource(R.drawable.nophotos);
                    }
                    cell.setChecked(selectedPhotos.containsKey(photoEntry.imageId), false);
                    showing = PhotoViewer.getInstance().isShowingImage(photoEntry.path);
                imageView.getImageReceiver().setVisible(!showing, true);
                cell.checkBox.setVisibility(singlePhoto || showing ? View.GONE : View.VISIBLE);
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return selectedAlbum.photos.isEmpty();
        }
    }
}
