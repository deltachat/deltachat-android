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

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MediaController;
import com.b44t.messenger.MessageObject;
import com.b44t.messenger.MrContact;
import com.b44t.messenger.NotificationCenter;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.FileLoader;
import com.b44t.messenger.R;
import com.b44t.messenger.TLRPC;
import com.b44t.messenger.Utilities;
import com.b44t.ui.ActionBar.ActionBar;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.Adapters.BaseFragmentAdapter;
import com.b44t.ui.Cells.AudioCell;
import com.b44t.ui.Components.EmptyTextProgressView;
import com.b44t.ui.Components.LayoutHelper;
import com.b44t.ui.Components.PickerBottomLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class AudioSelectActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private ListAdapter listViewAdapter;
    private EmptyTextProgressView progressView;
    private PickerBottomLayout bottomLayout;

    private boolean loadingAudio;

    private ArrayList<MediaController.AudioEntry> audioEntries = new ArrayList<>();
    private HashMap<Long, MediaController.AudioEntry> selectedAudios = new HashMap<>();

    private AudioSelectActivityDelegate delegate;

    private MessageObject playingAudio;

    public interface AudioSelectActivityDelegate {
        void didSelectAudio(ArrayList<MessageObject> audios);
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.closeChats);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioDidReset);
        loadAudio();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.closeChats);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioDidReset);
        if (playingAudio != null && MediaController.getInstance().isMessageOnAir(playingAudio)) {
            MediaController.getInstance().cleanupPlayer(true, true);
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(context.getString(R.string.AttachMusic));
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

        progressView = new EmptyTextProgressView(context);
        progressView.setText(context.getString(R.string.NoAudio));
        frameLayout.addView(progressView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        ListView listView = new ListView(context);
        listView.setEmptyView(progressView);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setAdapter(listViewAdapter = new ListAdapter(context));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.START | Gravity.TOP, 0, 0, 0, 48));

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                AudioCell audioCell = (AudioCell) view;
                MediaController.AudioEntry audioEntry = audioCell.getAudioEntry();
                if (selectedAudios.containsKey(audioEntry.id)) {
                    selectedAudios.remove(audioEntry.id);
                    audioCell.setChecked(false);
                } else {
                    selectedAudios.put(audioEntry.id, audioEntry);
                    audioCell.setChecked(true);
                }
                updateBottomLayoutCount();
            }
        });

        bottomLayout = new PickerBottomLayout(context, false);
        frameLayout.addView(bottomLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM));
        bottomLayout.cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishFragment();
            }
        });
        bottomLayout.doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (delegate != null) {
                    ArrayList<MessageObject> audios = new ArrayList<>();
                    for (HashMap.Entry<Long, MediaController.AudioEntry> entry : selectedAudios.entrySet()) {
                        audios.add(entry.getValue().messageObject);
                    }
                    delegate.didSelectAudio(audios);
                }
                finishFragment();
            }
        });

        View shadow = new View(context);
        shadow.setBackgroundResource(R.drawable.header_shadow_reverse);
        frameLayout.addView(shadow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 3, Gravity.START | Gravity.BOTTOM, 0, 0, 0, 48));

        if (loadingAudio) {
            progressView.showProgress();
        } else {
            progressView.showTextView();
        }
        updateBottomLayoutCount();
        return fragmentView;
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.closeChats) {
            removeSelfFromStack();
        } else if (id == NotificationCenter.audioDidReset) {
            if (listViewAdapter != null) {
                listViewAdapter.notifyDataSetChanged();
            }
        }
    }

    private void updateBottomLayoutCount() {
        bottomLayout.updateSelectedCount(selectedAudios.size(), true);
    }

    public void setDelegate(AudioSelectActivityDelegate audioSelectActivityDelegate) {
        delegate = audioSelectActivityDelegate;
    }

    private void loadAudio() {
        loadingAudio = true;
        if (progressView != null) {
            progressView.showProgress();
        }
        Utilities.globalQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                String[] projection = {
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.DURATION
                };

                final ArrayList<MediaController.AudioEntry> newAudioEntries = new ArrayList<>();
                Cursor cursor = null;
                try {
                    cursor = ApplicationLoader.applicationContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, MediaStore.Audio.Media.IS_MUSIC + " != 0", null, MediaStore.Audio.Media.TITLE);
                    int id = -2000000000;
                    while (cursor.moveToNext()) {
                        MediaController.AudioEntry audioEntry = new MediaController.AudioEntry();
                        audioEntry.id = cursor.getInt(0);
                        audioEntry.author = cursor.getString(1);
                        audioEntry.title = cursor.getString(2);
                        audioEntry.path = cursor.getString(3);
                        audioEntry.duration = (int) (cursor.getLong(4) / 1000);

                        File file = new File(audioEntry.path);

                        TLRPC.TL_message message = new TLRPC.TL_message();
                        message.out = true;
                        message.id = id;
                        message.to_id = new TLRPC.TL_peerUser();
                        message.to_id.user_id = message.from_id = MrContact.MR_CONTACT_ID_SELF;
                        message.date = (int) (System.currentTimeMillis() / 1000);
                        message.message = "-1";
                        message.attachPath = audioEntry.path;
                        message.media = new TLRPC.TL_messageMediaDocument();
                        message.media.document = new TLRPC.TL_document();
                        message.flags |= TLRPC.MESSAGE_FLAG_HAS_MEDIA | TLRPC.MESSAGE_FLAG_HAS_FROM_ID;

                        String ext = FileLoader.getFileExtension(file);

                        message.media.document.id = 0;
                        message.media.document.access_hash = 0;
                        message.media.document.date = message.date;
                        message.media.document.mime_type = "audio/" + (ext.length() > 0 ? ext : "mp3");
                        message.media.document.size = (int) file.length();
                        message.media.document.thumb = new TLRPC.TL_photoSizeEmpty();
                        message.media.document.thumb.type = "s";
                        message.media.document.dc_id = 0;

                        TLRPC.TL_documentAttributeAudio attributeAudio = new TLRPC.TL_documentAttributeAudio();
                        attributeAudio.duration = audioEntry.duration;
                        attributeAudio.title = audioEntry.title;
                        attributeAudio.performer = audioEntry.author;
                        message.media.document.attributes.add(attributeAudio);

                        TLRPC.TL_documentAttributeFilename fileName = new TLRPC.TL_documentAttributeFilename();
                        fileName.file_name = file.getName();
                        message.media.document.attributes.add(fileName);

                        audioEntry.messageObject = new MessageObject(message, false);

                        newAudioEntries.add(audioEntry);
                        id--;
                    }
                } catch (Exception e) {

                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        audioEntries = newAudioEntries;
                        progressView.showTextView();
                        listViewAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
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
            return audioEntries.size();
        }

        @Override
        public Object getItem(int i) {
            return audioEntries.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            //int type = getItemViewType(i);
            if (view == null) {
                view = new AudioCell(mContext);
                ((AudioCell) view).setDelegate(new AudioCell.AudioCellDelegate() {
                    @Override
                    public void startedPlayingAudio(MessageObject messageObject) {
                        playingAudio = messageObject;
                    }
                });
            }
            MediaController.AudioEntry audioEntry = audioEntries.get(i);
            ((AudioCell) view).setAudio(audioEntries.get(i), selectedAudios.containsKey(audioEntry.id));
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
            return audioEntries.isEmpty();
        }
    }
}
