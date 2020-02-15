package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Settings.System;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.devbrackets.android.exomedia.listener.VideoControlsButtonListener;
import com.devbrackets.android.exomedia.listener.VideoControlsVisibilityListener;
import com.devbrackets.android.exomedia.ui.animation.TopViewHideShowAnimation;
import com.devbrackets.android.exomedia.ui.widget.VideoControls;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.molvix.android.R;
import com.molvix.android.beans.DownloadedVideoItem;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.ui.activities.MainActivity;
import com.molvix.android.utils.MolvixLogger;
import com.molvix.android.utils.UiUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MolvixVideoPlayerView extends FrameLayout {

    @BindView(R.id.video_title_view)
    TextView videoTitleView;

    @BindView(R.id.video_nav_back)
    ImageView videoNavBackView;

    @BindView(R.id.video_view)
    VideoView videoView;

    @BindView(R.id.title_view_container)
    View titleViewContainer;

    @BindView(R.id.volume_controller)
    CircleSeekBar volumeController;

    @BindView(R.id.brightness_controller)
    CircleSeekBar brightnessController;

    private AtomicReference<File> activeFileReference = new AtomicReference<>();
    private AtomicInteger currentOrientation = new AtomicInteger(1);
    private AtomicBoolean controlsShown = new AtomicBoolean(false);
    private AtomicBoolean titleContainerVisible = new AtomicBoolean(true);
    private AtomicBoolean brightnessHandleAvailable = new AtomicBoolean(true);
    private AudioManager audioManager;

    private AtomicInteger initialBrightness = new AtomicInteger(0);
    private AtomicInteger initialVolume = new AtomicInteger(0);

    private int brightness;
    private ContentResolver cResolver;
    private Window window;

    public MolvixVideoPlayerView(@NonNull Context context) {
        super(context);
        initUI(context);
    }

    public MolvixVideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initUI(context);
    }

    public MolvixVideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initUI(context);
    }

    private void initUI(Context context) {
        removeAllViews();
        @SuppressLint("InflateParams") View playerView = LayoutInflater.from(context).inflate(R.layout.video_player_view, null);
        ButterKnife.bind(this, playerView);
        addView(playerView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void initVolumeAndBrightnessController() {
        audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            volumeController.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            initialVolume.set(volume);
            volumeController.setSeekBarChangeListener(new CircleSeekBar.OnSeekBarChangedListener() {
                @Override
                public void onPointsChanged(CircleSeekBar circleSeekBar, int points, boolean fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, points,
                            0);
                }

                @Override
                public void onStartTrackingTouch(CircleSeekBar circleSeekBar) {

                }

                @Override
                public void onStopTrackingTouch(CircleSeekBar circleSeekBar) {

                }

            });
            volumeController.setProgressDisplayAndInvalidate(volume);
            cResolver = getContext().getContentResolver();
            window = ((MainActivity) getContext()).getWindow();
            brightnessController.setMax(255);
            brightnessController.setStep(1);
            try {
                brightness = System.getInt(cResolver, System.SCREEN_BRIGHTNESS);
                initialBrightness.set(brightness);
                brightnessHandleAvailable.set(true);
            } catch (Settings.SettingNotFoundException e) {
                UiUtils.toggleViewVisibility(brightnessController, false);
                brightnessHandleAvailable.set(false);
                MolvixLogger.d("Error", "Cannot access system brightness");
                e.printStackTrace();
            }
            brightnessController.setSeekBarChangeListener(new CircleSeekBar.OnSeekBarChangedListener() {
                @Override
                public void onPointsChanged(CircleSeekBar circleSeekBar, int points, boolean fromUser) {
                    if (points <= 20) {
                        brightness = 20;
                    } else {
                        brightness = points;
                    }
                    System.putInt(cResolver, System.SCREEN_BRIGHTNESS, brightness);
                    WindowManager.LayoutParams windowLayoutParams = window.getAttributes();
                    windowLayoutParams.screenBrightness = brightness / (float) 255;
                    window.setAttributes(windowLayoutParams);
                }

                @Override
                public void onStartTrackingTouch(CircleSeekBar circleSeekBar) {

                }

                @Override
                public void onStopTrackingTouch(CircleSeekBar circleSeekBar) {

                }
            });
            brightnessController.setProgressDisplayAndInvalidate(brightness);
        }
    }

    private void resetBrightnessAndVolumeToInitials() {
        try {
            if (initialVolume.get() != 0 && audioManager != null) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, initialVolume.get(),
                        0);
            }
            if (brightnessHandleAvailable.get() && initialBrightness.get() != 0) {
                System.putInt(cResolver, System.SCREEN_BRIGHTNESS, brightness);
                WindowManager.LayoutParams windowLayoutParams = window.getAttributes();
                windowLayoutParams.screenBrightness = initialBrightness.get() / (float) 255;
                window.setAttributes(windowLayoutParams);
            }
        } catch (Exception ignored) {

        }
    }

    public void loadVideo(List<DownloadedVideoItem> videoItemList, int startIndex) {
        initVolumeAndBrightnessController();
        DownloadedVideoItem downloadedVideoItem = videoItemList.get(startIndex);
        configureVideoControls(videoItemList, startIndex);
        videoTitleView.setText(downloadedVideoItem.getTitle());
        videoView.setVideoPath(downloadedVideoItem.getDownloadedFile().getPath());
        videoView.setOnPreparedListener(() -> {
            try {
                long lastPlayBackPosition = AppPrefs.getLastMediaPlayBackPositionFor(downloadedVideoItem.getDownloadedFile());
                if (lastPlayBackPosition > 0) {
                    videoView.seekTo(lastPlayBackPosition);
                }
                videoView.start();
                activeFileReference.set(downloadedVideoItem.getDownloadedFile());
            } catch (Exception ignored) {

            }
        });
        videoView.setOnErrorListener(e -> {
            AlertDialog.Builder errorBuilder = new AlertDialog.Builder(getContext());
            errorBuilder.setMessage("Sorry, couldn't play video.It seems this video is corrupt or not fully downloaded.");
            errorBuilder.setPositiveButton("OK", (dialog, which) -> {
                dialog.dismiss();
                removePlayer();
            });
            errorBuilder.create().show();
            return true;
        });
        videoNavBackView.setOnClickListener(v -> {
            UiUtils.blinkView(videoNavBackView);
            removePlayer();
        });
        videoTitleView.setOnClickListener(v -> {
            //Do nothing, just don't let the search box behind take focus
        });
    }

    private void removePlayer() {
        cleanUpVideoView();
        resetBrightnessAndVolumeToInitials();
        ((ViewGroup) getParent()).removeView(MolvixVideoPlayerView.this);
    }

    @SuppressWarnings("deprecation")
    private void configureVideoControls(List<DownloadedVideoItem> downloadedVideoItems,
                                        int startIndex) {
        VideoControls videoControls = videoView.getVideoControls();
        if (videoControls != null) {
            tryShowNextButton(downloadedVideoItems, startIndex, videoControls);
            tryShowPreviousButton(downloadedVideoItems, startIndex, videoControls);
            listenToControlButtonClicks(downloadedVideoItems, startIndex, videoControls);
            listenToControlsVisibilityChange(videoControls);
            videoView.setOnCompletionListener(() -> {
                AppPrefs.persistMediaPlayBackPositionFor(downloadedVideoItems.get(startIndex).getDownloadedFile(), 0);
                int nextIndex = startIndex + 1;
                try {
                    DownloadedVideoItem nextOnList = downloadedVideoItems.get(nextIndex);
                    if (nextOnList != null) {
                        loadVideo(downloadedVideoItems, downloadedVideoItems.indexOf(nextOnList));
                    } else {
                        removePlayer();
                    }
                } catch (Exception ignored) {
                    removePlayer();
                }
            });
            setOnSystemUiVisibilityChangeListener(visibility -> {
                if (visibility == View.SYSTEM_UI_FLAG_VISIBLE && currentOrientation.get() == Configuration.ORIENTATION_LANDSCAPE) {
                    if (!controlsShown.get()) {
                        reEnterImmersiveModeDelayed();
                    }
                }
            });
        }
    }

    private void reEnterImmersiveModeDelayed() {
        new Handler().postDelayed(this::enterImmersiveMode, (long) 3000);
    }

    private void listenToControlsVisibilityChange(VideoControls videoControls) {
        videoControls.setVisibilityListener(new VideoControlsVisibilityListener() {

            @Override
            public void onControlsShown() {
                animateTitleContainerVisibility(true);
                animateVolumeAndBrightnessContainerVisibility(true);
                controlsShown.set(true);
            }

            @Override
            public void onControlsHidden() {
                animateTitleContainerVisibility(false);
                animateVolumeAndBrightnessContainerVisibility(false);
                if (currentOrientation.get() == Configuration.ORIENTATION_LANDSCAPE) {
                    enterImmersiveMode();
                }
                controlsShown.set(false);
            }
        });
    }

    private void animateVolumeAndBrightnessContainerVisibility(boolean toVisible) {
        UiUtils.toggleViewVisibility(volumeController, toVisible);
        if (brightnessHandleAvailable.get()) {
            UiUtils.toggleViewVisibility(volumeController, toVisible);
        }
        initVolumeAndBrightnessController();
    }

    private void animateTitleContainerVisibility(boolean toVisible) {
        if (titleContainerVisible.get() == toVisible) {
            return;
        }
        titleViewContainer.startAnimation(new TopViewHideShowAnimation(titleViewContainer, toVisible, 300L));
        titleContainerVisible.set(toVisible);
    }

    private void leaveImmersiveMode() {
        if (getContext() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getContext();
            View decorView = mainActivity.getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    private void enterImmersiveMode() {
        if (getContext() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getContext();
            View decorView = mainActivity.getWindow().getDecorView();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE
                                // Set the content to appear under the system bars so that the
                                // content doesn't resize when the system bars hide and show.
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                // Hide the nav bar and status bar
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        leaveImmersiveMode();
    }

    private void listenToControlButtonClicks(List<DownloadedVideoItem> downloadedVideoItems,
                                             int startIndex, VideoControls videoControls) {
        videoControls.setButtonListener(new VideoControlsButtonListener() {
            @Override
            public boolean onPlayPauseClicked() {
                return false;
            }

            @Override
            public boolean onPreviousClicked() {
                tryPlayPreviousEpisode(downloadedVideoItems, startIndex);
                return true;
            }

            @Override
            public boolean onNextClicked() {
                tryPlayNextEpisode(downloadedVideoItems, startIndex);
                return true;
            }

            @Override
            public boolean onRewindClicked() {
                return false;
            }

            @Override
            public boolean onFastForwardClicked() {
                return false;
            }

        });
    }

    private void tryShowPreviousButton(List<DownloadedVideoItem> downloadedVideoItems,
                                       int startIndex, VideoControls videoControls) {
        int previousIndex = startIndex - 1;
        try {
            DownloadedVideoItem previousOnList = downloadedVideoItems.get(previousIndex);
            if (previousOnList != null) {
                videoControls.setPreviousButtonRemoved(false);
            } else {
                videoControls.setPreviousButtonRemoved(true);
            }
        } catch (Exception e) {
            videoControls.setPreviousButtonRemoved(true);
        }
    }

    private void tryShowNextButton(List<DownloadedVideoItem> downloadedVideoItems,
                                   int startIndex, VideoControls videoControls) {
        int nextIndex = startIndex + 1;
        try {
            DownloadedVideoItem nextOnList = downloadedVideoItems.get(nextIndex);
            if (nextOnList != null) {
                videoControls.setNextButtonRemoved(false);
            } else {
                videoControls.setNextButtonRemoved(true);
            }
        } catch (Exception e) {
            videoControls.setNextButtonRemoved(true);
        }
    }

    private void tryPlayPreviousEpisode(List<DownloadedVideoItem> downloadedVideoItems,
                                        int startIndex) {
        try {
            DownloadedVideoItem previous = downloadedVideoItems.get(startIndex - 1);
            if (previous != null) {
                loadVideo(downloadedVideoItems, downloadedVideoItems.indexOf(previous));
            }
        } catch (Exception e) {
            UiUtils.showSafeToast("Error in playback.Please try again later");
        }
    }

    private void tryPlayNextEpisode(List<DownloadedVideoItem> downloadedVideoItems,
                                    int startIndex) {
        try {
            DownloadedVideoItem next = downloadedVideoItems.get(startIndex + 1);
            if (next != null) {
                loadVideo(downloadedVideoItems, downloadedVideoItems.indexOf(next));
            }
        } catch (Exception e) {
            UiUtils.showSafeToast("Error in playback.Please try again later");
        }
    }

    public void cleanUpVideoView() {
        try {
            if (videoView.isPlaying()) {
                videoView.stopPlayback();
            }
            videoView.release();
        } catch (Exception ignored) {

        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int newOrientation = newConfig.orientation;
        currentOrientation.set(newOrientation);
        if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            enterImmersiveMode();
        } else {
            leaveImmersiveMode();
        }
    }

    public void trySaveCurrentPlayerPosition() {
        File activeFile = activeFileReference.get();
        if (activeFile != null) {
            try {
                long currentPosition = videoView.getCurrentPosition();
                if (currentPosition != 0) {
                    AppPrefs.persistMediaPlayBackPositionFor(activeFile, currentPosition);
                }
            } catch (Exception ignored) {

            }
        }
    }

}
