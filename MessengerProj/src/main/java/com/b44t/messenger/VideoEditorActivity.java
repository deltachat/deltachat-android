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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.media.MediaCodecInfo;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.b44t.messenger.VideoEditedInfo;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.MediaBox;
import com.coremedia.iso.boxes.MediaHeaderBox;
import com.coremedia.iso.boxes.SampleSizeBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.googlecode.mp4parser.util.Matrix;
import com.googlecode.mp4parser.util.Path;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.MediaController;
import com.b44t.messenger.NotificationCenter;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.R;
import com.b44t.messenger.ActionBar.ActionBar;
import com.b44t.messenger.ActionBar.ActionBarMenu;
import com.b44t.messenger.ActionBar.BaseFragment;
import com.b44t.messenger.Components.LayoutHelper;
import com.b44t.messenger.Components.VideoSeekBarView;
import com.b44t.messenger.Components.VideoTimelineView;

import java.util.List;

@TargetApi(16)
public class VideoEditorActivity extends BaseFragment implements TextureView.SurfaceTextureListener, NotificationCenter.NotificationCenterDelegate {

    private boolean created = false;
    private MediaPlayer videoPlayer = null;
    private VideoTimelineView videoTimelineView = null;
    private View videoContainerView = null;
    private ImageView playButton = null;
    private VideoSeekBarView videoSeekBarView = null;
    private TextureView textureView = null;
    private View controlView = null;
    private boolean playerPrepared = false;

    private String videoPath = null;
    private float lastProgress = 0;
    private boolean needSeek = false;
    private VideoEditorActivityDelegate delegate;

    private final Object sync = new Object();
    private Thread thread = null;

    private int originalWidth = 0;
    private int originalHeight = 0;
    private int originalRotationValue = 0;
    private int originalVideoBitrate = 0;
    private float originalDurationMs = 0;
    private long originalAudioBytes = 0;

    private long startTimeMs = 0;
    private long endTimeMs = 0;

    private int resultWidth = 0;
    private int resultHeight = 0;
    private int resultBitrate = 0;
    private long resultDurationMs = 0;
    private int estimatedBytes = 0;

    private long MAX_BYTES;

    public interface VideoEditorActivityDelegate {
        void didFinishEditVideo(VideoEditedInfo vei, long estimatedSize, long estimatedDuration);
    }

    private Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            boolean playerCheck;

            while (true) {
                synchronized (sync) {
                    try {
                        playerCheck = videoPlayer != null && videoPlayer.isPlaying();
                    } catch (Exception e) {
                        playerCheck = false;

                    }
                }
                if (!playerCheck) {
                    break;
                }
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if (videoPlayer != null && videoPlayer.isPlaying()) {
                            float startTime = videoTimelineView.getLeftProgress() * originalDurationMs;
                            float endTime = videoTimelineView.getRightProgress() * originalDurationMs;
                            if (startTime == endTime) {
                                startTime = endTime - 0.01f;
                            }
                            float progress = (videoPlayer.getCurrentPosition() - startTime) / (endTime - startTime);
                            float lrdiff = videoTimelineView.getRightProgress() - videoTimelineView.getLeftProgress();
                            progress = videoTimelineView.getLeftProgress() + lrdiff * progress;
                            if (progress > lastProgress) {
                                videoSeekBarView.setProgress(progress);
                                lastProgress = progress;
                            }
                            if (videoPlayer.getCurrentPosition() >= endTime) {
                                try {
                                    videoPlayer.pause();
                                    onPlayComplete();
                                } catch (Exception e) {

                                }
                            }
                        }
                    }
                });
                try {
                    Thread.sleep(50);
                } catch (Exception e) {

                }
            }
            synchronized (sync) {
                thread = null;
            }
        }
    };

    public VideoEditorActivity(Bundle args) {
        super(args);
        videoPath = args.getString("videoPath");
        MAX_BYTES = MrMailbox.getConfigInt("sys.msgsize_max_recommended", 10*1024*1024);
    }

    @Override
    public boolean onFragmentCreate() {
        if (created) {
            return true;
        }
        if (videoPath == null || !processOpenVideo()) {
            return false;
        }
        videoPlayer = new MediaPlayer();
        videoPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        onPlayComplete();
                    }
                });
            }
        });
        videoPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                playerPrepared = true;
                if (videoTimelineView != null && videoPlayer != null) {
                    videoPlayer.seekTo((int) (videoTimelineView.getLeftProgress() * originalDurationMs));
                }
            }
        });
        try {
            videoPlayer.setDataSource(videoPath);
            videoPlayer.prepareAsync();
        } catch (Exception e) {

            return false;
        }

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.closeChats);
        created = true;

        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        if (videoTimelineView != null) {
            videoTimelineView.destroy();
        }
        if (videoPlayer != null) {
            try {
                videoPlayer.stop();
                videoPlayer.release();
                videoPlayer = null;
            } catch (Exception e) {

            }
        }
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.closeChats);
        super.onFragmentDestroy();
    }

    @Override
    public View createView(final Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_close_white);
        actionBar.setTitle(ApplicationLoader.applicationContext.getString(R.string.SendVideo));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    synchronized (sync) {
                        if (videoPlayer != null) {
                            try {
                                videoPlayer.stop();
                                videoPlayer.release();
                                videoPlayer = null;
                            } catch (Exception e) {

                            }
                        }
                    }

                    if(estimatedBytes>MAX_BYTES) {
                        AndroidUtilities.showHint(getParentActivity(), String.format(context.getString(R.string.PleaseCutVideoToMaxSize), AndroidUtilities.formatFileSize(MAX_BYTES)));
                        return;
                    }

                    if (delegate != null) {
                        VideoEditedInfo vei = new VideoEditedInfo();
                        vei.originalPath    = videoPath;
                        vei.startTime       = startTimeMs;
                        vei.endTime         = endTimeMs;
                        vei.rotationValue   = originalRotationValue;
                        vei.originalWidth   = originalWidth;
                        vei.originalHeight  = originalHeight;
                        vei.originalBitrate = originalVideoBitrate;
                        vei.resultWidth     = resultWidth;
                        vei.resultHeight    = resultHeight;
                        vei.resultBitrate   = resultBitrate;
                        delegate.didFinishEditVideo(vei, estimatedBytes, resultDurationMs);
                    }
                    finishFragment();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItemWithWidth(1, R.drawable.ic_done, AndroidUtilities.dp(56));

        fragmentView = getParentActivity().getLayoutInflater().inflate(R.layout.video_editor_layout, null, false);
        videoContainerView = fragmentView.findViewById(R.id.video_container);
        controlView = fragmentView.findViewById(R.id.control_layout);

        videoTimelineView = (VideoTimelineView) fragmentView.findViewById(R.id.video_timeline_view);
        videoTimelineView.setVideoPath(videoPath);
        videoTimelineView.setDelegate(new VideoTimelineView.VideoTimelineViewDelegate() {
            @Override
            public void onLeftProgressChanged(float progress) {
                if (videoPlayer == null || !playerPrepared) {
                    return;
                }
                try {
                    if (videoPlayer.isPlaying()) {
                        videoPlayer.pause();
                        playButton.setImageResource(R.drawable.video_play);
                    }
                    videoPlayer.setOnSeekCompleteListener(null);
                    videoPlayer.seekTo((int) (originalDurationMs * progress));
                } catch (Exception e) {

                }
                needSeek = true;
                videoSeekBarView.setProgress(videoTimelineView.getLeftProgress());
                updateVideoEditedInfo();
            }

            @Override
            public void onRifhtProgressChanged(float progress) {
                if (videoPlayer == null || !playerPrepared) {
                    return;
                }
                try {
                    if (videoPlayer.isPlaying()) {
                        videoPlayer.pause();
                        playButton.setImageResource(R.drawable.video_play);
                    }
                    videoPlayer.setOnSeekCompleteListener(null);
                    videoPlayer.seekTo((int) (originalDurationMs * progress));
                } catch (Exception e) {

                }
                needSeek = true;
                videoSeekBarView.setProgress(videoTimelineView.getLeftProgress());
                updateVideoEditedInfo();
            }
        });

        videoSeekBarView = (VideoSeekBarView) fragmentView.findViewById(R.id.video_seekbar);
        videoSeekBarView.delegate = new VideoSeekBarView.SeekBarDelegate() {
            @Override
            public void onSeekBarDrag(float progress) {
                if (progress < videoTimelineView.getLeftProgress()) {
                    progress = videoTimelineView.getLeftProgress();
                    videoSeekBarView.setProgress(progress);
                } else if (progress > videoTimelineView.getRightProgress()) {
                    progress = videoTimelineView.getRightProgress();
                    videoSeekBarView.setProgress(progress);
                }
                if (videoPlayer == null || !playerPrepared) {
                    return;
                }
                if (videoPlayer.isPlaying()) {
                    try {
                        videoPlayer.seekTo((int) (originalDurationMs * progress));
                        lastProgress = progress;
                    } catch (Exception e) {

                    }
                } else {
                    lastProgress = progress;
                    needSeek = true;
                }
            }
        };

        playButton = (ImageView) fragmentView.findViewById(R.id.play_button);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });

        textureView = (TextureView) fragmentView.findViewById(R.id.video_view);
        textureView.setSurfaceTextureListener(this);

        updateVideoEditedInfo();

        return fragmentView;
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.closeChats) {
            removeSelfFromStack();
        }
    }

    private void setPlayerSurface() {
        if (textureView == null || !textureView.isAvailable() || videoPlayer == null) {
            return;
        }
        try {
            Surface s = new Surface(textureView.getSurfaceTexture());
            videoPlayer.setSurface(s);
            if (playerPrepared) {
                videoPlayer.seekTo((int) (videoTimelineView.getLeftProgress() * originalDurationMs));
            }
        } catch (Exception e) {

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        fixLayoutInternal();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fixLayout();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        setPlayerSurface();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (videoPlayer == null) {
            return true;
        }
        videoPlayer.setDisplay(null);
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private void onPlayComplete() {
        if (playButton != null) {
            playButton.setImageResource(R.drawable.video_play);
        }
        if (videoSeekBarView != null && videoTimelineView != null) {
            videoSeekBarView.setProgress(videoTimelineView.getLeftProgress());
        }
        try {
            if (videoPlayer != null) {
                if (videoTimelineView != null) {
                    videoPlayer.seekTo((int) (videoTimelineView.getLeftProgress() * originalDurationMs));
                }
            }
        } catch (Exception e) {

        }
    }

    private void fixVideoSize() {
        if (fragmentView == null || getParentActivity() == null) {
            return;
        }
        int viewHeight;
        viewHeight = AndroidUtilities.displaySize.y - AndroidUtilities.statusBarHeight - ActionBar.getCurrentActionBarHeight();

        int width;
        int height;
        if (getParentActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            width = AndroidUtilities.displaySize.x / 3 - AndroidUtilities.dp(24);
            height = viewHeight - AndroidUtilities.dp(32);
        } else {
            width = AndroidUtilities.displaySize.x;
            height = viewHeight - AndroidUtilities.dp(276);
        }

        int vwidth = originalRotationValue == 90 || originalRotationValue == 270 ? originalHeight : originalWidth;
        int vheight = originalRotationValue == 90 || originalRotationValue == 270 ? originalWidth : originalHeight;
        float wr = (float) width / (float) vwidth;
        float hr = (float) height / (float) vheight;
        float ar = (float) vwidth / (float) vheight;

        if (wr > hr) {
            width = (int) (height * ar);
        } else {
            height = (int) (width / ar);
        }

        if (textureView != null) {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) textureView.getLayoutParams();
            layoutParams.width = width;
            layoutParams.height = height;
            layoutParams.leftMargin = 0;
            layoutParams.topMargin = 0;
            textureView.setLayoutParams(layoutParams);
        }
    }

    private void fixLayoutInternal() {
        if (getParentActivity() == null) {
            return;
        }
        if (getParentActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // landscape orientation
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) videoContainerView.getLayoutParams();
            layoutParams.topMargin = AndroidUtilities.dp(16);
            layoutParams.bottomMargin = AndroidUtilities.dp(16);
            layoutParams.width = AndroidUtilities.displaySize.x / 3 - AndroidUtilities.dp(24);
            videoContainerView.setLayoutParams(layoutParams);

            layoutParams = (FrameLayout.LayoutParams) controlView.getLayoutParams();
            layoutParams.topMargin = AndroidUtilities.dp(16);
            layoutParams.bottomMargin = 0;
            layoutParams.width = AndroidUtilities.displaySize.x / 3 * 2 - AndroidUtilities.dp(32);
            layoutParams.leftMargin = AndroidUtilities.displaySize.x / 3 + AndroidUtilities.dp(16);
            layoutParams.gravity = Gravity.TOP;
            controlView.setLayoutParams(layoutParams);

        } else {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) videoContainerView.getLayoutParams();
            layoutParams.topMargin = AndroidUtilities.dp(16);
            layoutParams.bottomMargin = AndroidUtilities.dp(260);
            layoutParams.width = LayoutHelper.MATCH_PARENT;
            videoContainerView.setLayoutParams(layoutParams);

            layoutParams = (FrameLayout.LayoutParams) controlView.getLayoutParams();
            layoutParams.topMargin = 0;
            layoutParams.leftMargin = 0;
            layoutParams.bottomMargin = AndroidUtilities.dp(150);
            layoutParams.width = LayoutHelper.MATCH_PARENT;
            layoutParams.gravity = Gravity.BOTTOM;
            controlView.setLayoutParams(layoutParams);
        }
        fixVideoSize();
        videoTimelineView.clearFrames();
    }

    private void fixLayout() {
        if (fragmentView == null) {
            return;
        }
        fragmentView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                fixLayoutInternal();
                if (fragmentView != null) {
                    if (Build.VERSION.SDK_INT < 16) {
                        fragmentView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    } else {
                        fragmentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            }
        });
    }

    private void play() {
        if (videoPlayer == null || !playerPrepared) {
            return;
        }
        if (videoPlayer.isPlaying()) {
            videoPlayer.pause();
            playButton.setImageResource(R.drawable.video_play);
        } else {
            try {
                playButton.setImageDrawable(null);
                lastProgress = 0;
                if (needSeek) {
                    videoPlayer.seekTo((int) (originalDurationMs * videoSeekBarView.getProgress()));
                    needSeek = false;
                }
                videoPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        float startTime = videoTimelineView.getLeftProgress() * originalDurationMs;
                        float endTime = videoTimelineView.getRightProgress() * originalDurationMs;
                        if (startTime == endTime) {
                            startTime = endTime - 0.01f;
                        }
                        lastProgress = (videoPlayer.getCurrentPosition() - startTime) / (endTime - startTime);
                        float lrdiff = videoTimelineView.getRightProgress() - videoTimelineView.getLeftProgress();
                        lastProgress = videoTimelineView.getLeftProgress() + lrdiff * lastProgress;
                        videoSeekBarView.setProgress(lastProgress);
                    }
                });
                videoPlayer.start();
                synchronized (sync) {
                    if (thread == null) {
                        thread = new Thread(progressRunnable);
                        thread.start();
                    }
                }
            } catch (Exception e) {

            }
        }
    }

    public void setDelegate(VideoEditorActivityDelegate delegate) {
        this.delegate = delegate;
    }

    // open original
    //==============================================================================================

    private boolean processOpenVideo()
    {
        // check, if we can compress videos in common
        boolean canCompress = true;
        if (Build.VERSION.SDK_INT < 18 /*=4.3 Jelly Bean*/) {
            try {
                MediaCodecInfo codecInfo = MediaController.selectCodec(MediaController.MIME_TYPE);
                if (codecInfo == null) {
                    canCompress = false;
                } else {
                    String name = codecInfo.getName();
                    if (name.equals("OMX.google.h264.encoder") ||
                            name.equals("OMX.ST.VFM.H264Enc") ||
                            name.equals("OMX.Exynos.avc.enc") ||
                            name.equals("OMX.MARVELL.VIDEO.HW.CODA7542ENCODER") ||
                            name.equals("OMX.MARVELL.VIDEO.H264ENCODER") ||
                            name.equals("OMX.k3.video.encoder.avc") || //fix this later
                            name.equals("OMX.TI.DUCATI1.VIDEO.H264E")) { //fix this later
                        canCompress = false;
                    } else {
                        if (MediaController.selectColorFormat(codecInfo, MediaController.MIME_TYPE) == 0) {
                            canCompress = false;
                        }
                    }
                }
            } catch (Exception e) {
                canCompress = false;
            }
        }
        if( !canCompress ) {
            AndroidUtilities.showHint(ApplicationLoader.applicationContext, "Can't compress."); // should not happen
            return false;
        }

        // load information for the given video
        try {
            IsoFile isoFile = new IsoFile(videoPath);
            List<Box> boxes = Path.getPaths(isoFile, "/moov/trak/");
            TrackHeaderBox trackHeaderBox = null;

            Box boxTest = Path.getPath(isoFile, "/moov/trak/mdia/minf/stbl/stsd/mp4a/");
            if (boxTest == null) {
                return false; // non-mp4
            }

            for (Box box : boxes) {
                TrackBox trackBox = (TrackBox) box;
                long sampleSizes = 0;
                long trackBitrate = 0;
                try {
                    MediaBox mediaBox = trackBox.getMediaBox();
                    MediaHeaderBox mediaHeaderBox = mediaBox.getMediaHeaderBox();
                    SampleSizeBox sampleSizeBox = mediaBox.getMediaInformationBox().getSampleTableBox().getSampleSizeBox();
                    for (long size : sampleSizeBox.getSampleSizes()) {
                        sampleSizes += size;
                    }
                    float originalVideoSeconds = (float) mediaHeaderBox.getDuration() / (float) mediaHeaderBox.getTimescale();
                    trackBitrate = (int) (sampleSizes * 8 / originalVideoSeconds);
                    originalDurationMs = originalVideoSeconds * 1000;
                } catch (Exception e) {

                }
                TrackHeaderBox headerBox = trackBox.getTrackHeaderBox();
                if (headerBox.getWidth() != 0 && headerBox.getHeight() != 0) {
                    trackHeaderBox = headerBox;
                    originalVideoBitrate = (int) (trackBitrate / 100000 * 100000);
                } else {
                    originalAudioBytes += sampleSizes;
                }
            }
            if (trackHeaderBox == null) {
                return false;
            }

            Matrix matrix = trackHeaderBox.getMatrix();
            if (matrix.equals(Matrix.ROTATE_90)) {
                originalRotationValue = 90;
            } else if (matrix.equals(Matrix.ROTATE_180)) {
                originalRotationValue = 180;
            } else if (matrix.equals(Matrix.ROTATE_270)) {
                originalRotationValue = 270;
            }
            originalWidth = (int) trackHeaderBox.getWidth();
            originalHeight = (int) trackHeaderBox.getHeight();

        } catch (Exception e) {

            return false;
        }

        updateVideoEditedInfo();

        return true;
    }

    // calculate result
    //==============================================================================================

    private int calculateEstimatedSize(float timeDelta) {
        long videoFramesSize = (long) (resultBitrate / 8 * (originalDurationMs /1000));
        int size = (int) ((originalAudioBytes + videoFramesSize) * timeDelta);
        return size;
    }

    private void updateVideoEditedInfo() {
        if (videoTimelineView == null) {
            return;
        }

        // handle left and right locator
        resultDurationMs = (long) Math.ceil((videoTimelineView.getRightProgress() - videoTimelineView.getLeftProgress()) * originalDurationMs);
        if (videoTimelineView.getLeftProgress() == 0) {
            startTimeMs = -1;
        } else {
            startTimeMs = (long) (videoTimelineView.getLeftProgress() * originalDurationMs) * 1000;
        }
        if (videoTimelineView.getRightProgress() == 1) {
            endTimeMs = -1;
        } else {
            endTimeMs = (long) (videoTimelineView.getRightProgress() * originalDurationMs) * 1000;
        }

        // calculate video bitrate
        long maxVideoBytes = MAX_BYTES  -  originalAudioBytes  -  resultDurationMs/*10 kbps codec overhead*/;
        resultBitrate = (int)(maxVideoBytes/(resultDurationMs/1000)*8);

        if( resultBitrate < 200000) {
            resultBitrate = 200000;
        }
        else if (resultBitrate > 500000) {
            if( resultDurationMs<30*1000 ) {
                resultBitrate = 1500000; // ~ 12 MB/minute, plus Audio
            }
            else if( resultDurationMs<60*1000 ) {
                resultBitrate = 1000000; // ~ 8 MB/minute, plus Audio
            }
            else {
                resultBitrate = 500000; // ~ 3.7 MB/minute, plus Audio
            }
        }

        // get video dimensions
        int maxSide = resultBitrate>400000? 640 : 480;
        resultWidth = originalWidth;
        resultHeight = originalHeight;
        if (resultWidth > maxSide || resultHeight > maxSide) {
            float scale = resultWidth>resultHeight? (float)maxSide/resultWidth : (float)maxSide/resultHeight;
            resultWidth *= scale;
            resultHeight *= scale;
        }

        // calulate bytes
        estimatedBytes = calculateEstimatedSize((float) resultDurationMs / originalDurationMs);

        // update title
        int minutes = (int) (resultDurationMs / 1000 / 60);
        int seconds = (int) Math.ceil(resultDurationMs / 1000) - minutes * 60;
        String videoTimeSize = String.format("%d:%02d, ~%s", minutes, seconds, AndroidUtilities.formatFileSize(estimatedBytes));
        actionBar.setSubtitle(videoTimeSize);
    }
}
