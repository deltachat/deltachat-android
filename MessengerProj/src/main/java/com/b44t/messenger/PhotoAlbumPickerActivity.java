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


package com.b44t.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.b44t.messenger.ActionBar.ActionBar;
import com.b44t.messenger.ActionBar.ActionBarMenu;
import com.b44t.messenger.ActionBar.ActionBarMenuItem;
import com.b44t.messenger.ActionBar.BaseFragment;
import com.b44t.messenger.Components.BaseFragmentAdapter;
import com.b44t.messenger.Cells.PhotoPickerAlbumsCell;
import com.b44t.messenger.Components.LayoutHelper;
import com.b44t.messenger.Components.PickerBottomLayout;

import java.util.ArrayList;
import java.util.HashMap;

public class PhotoAlbumPickerActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    public interface PhotoAlbumPickerActivityDelegate {
        void didSelectPhotos(ArrayList<String> photos, ArrayList<String> captions);
        boolean didSelectVideo(String path);
        void startPhotoSelectActivity();
    }

    private ArrayList<MediaController.AlbumEntry> albumsSorted = null;
    private ArrayList<MediaController.AlbumEntry> videoAlbumsSorted = null;
    private HashMap<Integer, MediaController.PhotoEntry> selectedPhotos = new HashMap<>();
    private boolean loading = false;

    private int columnsCount = 2;
    private ListView listView;
    private ListAdapter listAdapter;
    private FrameLayout progressView;
    private TextView emptyView;
    private TextView dropDown;
    private ActionBarMenuItem dropDownContainer;
    private PickerBottomLayout pickerBottomLayout;
    private boolean sendPressed;
    private boolean singlePhoto;

    private final static int SELECTMODE0_PHOTOS = 0;
    private final static int SELECTMODE1_VIDEOS = 1;
    private int selectedMode;

    private ChatActivity chatActivity;

    private PhotoAlbumPickerActivityDelegate delegate;

    private final static int ID_OPEN_SYSTEM_SELECTOR = 1;
    private final static int ID_ITEM_PHOTOS = 2;
    private final static int ID_ITEM_VIDEO = 3;

    public PhotoAlbumPickerActivity(boolean singlePhoto, ChatActivity chatActivity) {
        super();
        this.chatActivity = chatActivity;
        this.singlePhoto = singlePhoto;
    }

    @Override
    public boolean onFragmentCreate() {
        loading = true;
        MediaController.loadGalleryPhotosAlbums(classGuid);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.albumsDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.closeChats);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.albumsDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.closeChats);
        super.onFragmentDestroy();
    }

    @SuppressWarnings("unchecked")
    @Override
    public View createView(final Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == ID_OPEN_SYSTEM_SELECTOR) {
                    if (delegate != null) {
                        finishFragment(false);
                        delegate.startPhotoSelectActivity();
                    }
                } else if (id == ID_ITEM_PHOTOS) {
                    if (selectedMode == SELECTMODE0_PHOTOS) {
                        return;
                    }
                    selectedMode = SELECTMODE0_PHOTOS;
                    dropDown.setText(context.getString(R.string.PickerPhotos));
                    emptyView.setText(context.getString(R.string.NoPhotos));
                    listAdapter.notifyDataSetChanged();
                } else if (id == ID_ITEM_VIDEO) {
                    if (selectedMode == SELECTMODE1_VIDEOS) {
                        return;
                    }
                    selectedMode = SELECTMODE1_VIDEOS;
                    dropDown.setText(context.getString(R.string.PickerVideo));
                    emptyView.setText(context.getString(R.string.NoVideo));
                    listAdapter.notifyDataSetChanged();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        //menu.addItem(ID_OPEN_SYSTEM_SELECTOR, R.drawable.ic_ab_other); -- fallback-selector not needed

        fragmentView = new FrameLayout(context);

        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(0xffffffff);

        if (!singlePhoto) {
            selectedMode = SELECTMODE0_PHOTOS;

            dropDownContainer = new ActionBarMenuItem(context, menu, 0);
            dropDownContainer.setSubMenuOpenSide(1);
            dropDownContainer.addSubItem(ID_ITEM_PHOTOS, context.getString(R.string.PickerPhotos));
            dropDownContainer.addSubItem(ID_ITEM_VIDEO, context.getString(R.string.PickerVideo));
            actionBar.addView(dropDownContainer);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) dropDownContainer.getLayoutParams();
            layoutParams.height = LayoutHelper.MATCH_PARENT;
            layoutParams.width = LayoutHelper.WRAP_CONTENT;
            layoutParams.rightMargin = AndroidUtilities.dp(40);
            layoutParams.leftMargin = AndroidUtilities.dp(56);
            layoutParams.gravity = Gravity.TOP | Gravity.START;
            dropDownContainer.setLayoutParams(layoutParams);
            dropDownContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dropDownContainer.toggleSubMenu();
                }
            });

            dropDown = new TextView(context);
            dropDown.setGravity(Gravity.START);
            dropDown.setSingleLine(true);
            dropDown.setLines(1);
            dropDown.setMaxLines(1);
            dropDown.setEllipsize(TextUtils.TruncateAt.END);
            dropDown.setTextColor(0xffffffff);
            dropDown.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0);
            dropDown.setCompoundDrawablePadding(AndroidUtilities.dp(4));
            dropDown.setPadding(0, 0, AndroidUtilities.dp(10), 0);
            dropDown.setText(context.getString(R.string.PickerPhotos));
            dropDownContainer.addView(dropDown);
            layoutParams = (FrameLayout.LayoutParams) dropDown.getLayoutParams();
            layoutParams.width = LayoutHelper.WRAP_CONTENT;
            layoutParams.height = LayoutHelper.WRAP_CONTENT;
            layoutParams.leftMargin = AndroidUtilities.dp(16);
            layoutParams.gravity = Gravity.CENTER_VERTICAL;
            dropDown.setLayoutParams(layoutParams);
        } else {
            actionBar.setTitle(context.getString(R.string.Gallery));
        }

        listView = new ListView(context);
        listView.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), AndroidUtilities.dp(4));
        listView.setClipToPadding(false);
        listView.setHorizontalScrollBarEnabled(false);
        listView.setVerticalScrollBarEnabled(false);
        listView.setSelector(new ColorDrawable(0));
        listView.setDividerHeight(0);
        listView.setDivider(null);
        listView.setDrawingCacheEnabled(false);
        listView.setScrollingCacheEnabled(false);
        frameLayout.addView(listView);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        layoutParams.bottomMargin = AndroidUtilities.dp(48);
        listView.setLayoutParams(layoutParams);
        listView.setAdapter(listAdapter = new ListAdapter(context));

        emptyView = new TextView(context);
        emptyView.setTextColor(0xff808080);
        emptyView.setTextSize(20);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        emptyView.setText(context.getString(R.string.NoPhotos));
        frameLayout.addView(emptyView);
        layoutParams = (FrameLayout.LayoutParams) emptyView.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        layoutParams.bottomMargin = AndroidUtilities.dp(48);
        emptyView.setLayoutParams(layoutParams);
        emptyView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        progressView = new FrameLayout(context);
        progressView.setVisibility(View.GONE);
        frameLayout.addView(progressView);
        layoutParams = (FrameLayout.LayoutParams) progressView.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        layoutParams.bottomMargin = AndroidUtilities.dp(48);
        progressView.setLayoutParams(layoutParams);

        ProgressBar progressBar = new ProgressBar(context);
        progressView.addView(progressBar);
        layoutParams = (FrameLayout.LayoutParams) progressView.getLayoutParams();
        layoutParams.width = LayoutHelper.WRAP_CONTENT;
        layoutParams.height = LayoutHelper.WRAP_CONTENT;
        layoutParams.gravity = Gravity.CENTER;
        progressView.setLayoutParams(layoutParams);

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
                finishFragment();
            }
        });
        pickerBottomLayout.doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendSelectedPhotos();
                finishFragment();
            }
        });

        if (loading && (albumsSorted == null || albumsSorted != null && albumsSorted.isEmpty())) {
            progressView.setVisibility(View.VISIBLE);
            listView.setEmptyView(null);
        } else {
            progressView.setVisibility(View.GONE);
            listView.setEmptyView(emptyView);
        }
        pickerBottomLayout.updateSelectedCount(selectedPhotos.size(), true);

        View shadow = new View(context);
        shadow.setBackgroundResource(R.drawable.header_shadow_reverse);
        frameLayout.addView(shadow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 3, Gravity.START | Gravity.BOTTOM, 0, 0, 0, 48));

        return fragmentView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (dropDownContainer != null) {
            dropDownContainer.closeSubMenu();
        }
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
        if (id == NotificationCenter.albumsDidLoaded) {
            int guid = (Integer) args[0];
            if (classGuid == guid) {
                albumsSorted = (ArrayList<MediaController.AlbumEntry>) args[1];
                videoAlbumsSorted = (ArrayList<MediaController.AlbumEntry>) args[3];
                if (progressView != null) {
                    progressView.setVisibility(View.GONE);
                }
                if (listView != null && listView.getEmptyView() == null) {
                    listView.setEmptyView(emptyView);
                }
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
                loading = false;
            }
        } else if (id == NotificationCenter.closeChats) {
            removeSelfFromStack();
        }
    }

    public void setDelegate(PhotoAlbumPickerActivityDelegate delegate) {
        this.delegate = delegate;
    }

    private void sendSelectedPhotos() {
        if (selectedPhotos.isEmpty() || delegate == null || sendPressed) {
            return;
        }
        sendPressed = true;
        ArrayList<String> photos = new ArrayList<>();
        ArrayList<String> captions = new ArrayList<>();
        for (HashMap.Entry<Integer, MediaController.PhotoEntry> entry : selectedPhotos.entrySet()) {
            MediaController.PhotoEntry photoEntry = entry.getValue();
            if (photoEntry.imagePath != null) {
                photos.add(photoEntry.imagePath);
                captions.add(photoEntry.caption != null ? photoEntry.caption.toString() : null);
            } else if (photoEntry.path != null) {
                photos.add(photoEntry.path);
                captions.add(photoEntry.caption != null ? photoEntry.caption.toString() : null);
            }
        }

        delegate.didSelectPhotos(photos, captions);
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

        WindowManager manager = (WindowManager) ApplicationLoader.applicationContext.getSystemService(Activity.WINDOW_SERVICE);
        int rotation = manager.getDefaultDisplay().getRotation();
        columnsCount = 2;
        if (rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90) {
            columnsCount = 4;
        }
        listAdapter.notifyDataSetChanged();

        if (dropDownContainer != null) {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) dropDownContainer.getLayoutParams();
            layoutParams.topMargin = (Build.VERSION.SDK_INT >= 21 ? AndroidUtilities.statusBarHeight : 0);
            dropDownContainer.setLayoutParams(layoutParams);

            if (ApplicationLoader.applicationContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                dropDown.setTextSize(18);
            } else {
                dropDown.setTextSize(20);
            }
        }
    }

    private void openPhotoPicker(MediaController.AlbumEntry albumEntry) {
        if( albumEntry == null ) {
            return;
        }
        PhotoPickerActivity fragment = new PhotoPickerActivity(albumEntry, selectedPhotos, singlePhoto, chatActivity);
        fragment.setDelegate(new PhotoPickerActivity.PhotoPickerActivityDelegate() {
            @Override
            public void selectedPhotosChanged() {
                if (pickerBottomLayout != null) {
                    pickerBottomLayout.updateSelectedCount(selectedPhotos.size(), true);
                }
            }

            @Override
            public void actionButtonPressed(boolean canceled) {
                removeSelfFromStack();
                if (!canceled) {
                    sendSelectedPhotos();
                }
            }

            @Override
            public boolean didSelectVideo(String path) {
                removeSelfFromStack();
                return delegate.didSelectVideo(path);
            }
        });
        presentFragment(fragment);
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
            if (singlePhoto || selectedMode == SELECTMODE0_PHOTOS) {
                return (albumsSorted != null ? (int) Math.ceil(albumsSorted.size() / (float) columnsCount) : 0);
            } else {
                return (videoAlbumsSorted != null ? (int) Math.ceil(videoAlbumsSorted.size() / (float) columnsCount) : 0);
            }
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
            int type = getItemViewType(i);
            if (type == 0) {
                PhotoPickerAlbumsCell photoPickerAlbumsCell;
                if (view == null) {
                    view = new PhotoPickerAlbumsCell(mContext);
                    photoPickerAlbumsCell = (PhotoPickerAlbumsCell) view;
                    photoPickerAlbumsCell.setDelegate(new PhotoPickerAlbumsCell.PhotoPickerAlbumsCellDelegate() {
                        @Override
                        public void didSelectAlbum(MediaController.AlbumEntry albumEntry) {
                            openPhotoPicker(albumEntry);
                        }
                    });
                } else {
                    photoPickerAlbumsCell = (PhotoPickerAlbumsCell) view;
                }
                photoPickerAlbumsCell.setAlbumsCount(columnsCount);
                for (int a = 0; a < columnsCount; a++) {
                    int index = i * columnsCount + a;
                    if (singlePhoto || selectedMode == SELECTMODE0_PHOTOS) {
                        if (index < albumsSorted.size()) {
                            MediaController.AlbumEntry albumEntry = albumsSorted.get(index);
                            photoPickerAlbumsCell.setAlbum(a, albumEntry);
                        } else {
                            photoPickerAlbumsCell.setAlbum(a, null);
                        }
                    } else {
                        if (index < videoAlbumsSorted.size()) {
                            MediaController.AlbumEntry albumEntry = videoAlbumsSorted.get(index);
                            photoPickerAlbumsCell.setAlbum(a, albumEntry);
                        } else {
                            photoPickerAlbumsCell.setAlbum(a, null);
                        }
                    }
                }
                photoPickerAlbumsCell.requestLayout();
            }
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
            return getCount() == 0;
        }
    }
}
