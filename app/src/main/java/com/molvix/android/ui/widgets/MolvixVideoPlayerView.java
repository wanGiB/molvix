package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.devbrackets.android.exomedia.listener.VideoControlsButtonListener;
import com.devbrackets.android.exomedia.listener.VideoControlsVisibilityListener;
import com.devbrackets.android.exomedia.ui.widget.VideoControls;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.molvix.android.R;
import com.molvix.android.beans.DownloadedVideoItem;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.ui.activities.MainActivity;
import com.molvix.android.utils.UiUtils;

import java.io.File;
import java.util.List;
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

    private AtomicReference<File> activeFileReference = new AtomicReference<>();

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

    public void loadVideo(List<DownloadedVideoItem> videoItemList, int startIndex) {
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
        ((ViewGroup) getParent()).removeView(MolvixVideoPlayerView.this);
    }

    @SuppressWarnings("deprecation")
    private void configureVideoControls(List<DownloadedVideoItem> downloadedVideoItems, int startIndex) {
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
        }
    }

    private void listenToControlsVisibilityChange(VideoControls videoControls) {
        videoControls.setVisibilityListener(new VideoControlsVisibilityListener() {
            @Override
            public void onControlsShown() {
                showTitleViewContainer();
            }

            @Override
            public void onControlsHidden() {
                hideTitleViewContainer();
            }
        });
    }

    private void hideTitleViewContainer() {
        Animation slideUpAnim = UiUtils.getAnimation(getContext(), R.anim.slide_up);
        slideUpAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                UiUtils.toggleViewVisibility(titleViewContainer, false);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        titleViewContainer.clearAnimation();
        titleViewContainer.startAnimation(slideUpAnim);
    }

    // Shows the system bars by removing all the flags// except for the ones that make the content appear under the system bars.
    private void leaveImmersiveMode() {
        if (getContext() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getContext();
            View decorView = mainActivity.getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
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

    private void showTitleViewContainer() {
        Animation slideDownAnim = UiUtils.getAnimation(getContext(), R.anim.slide_down);
        slideDownAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                UiUtils.toggleViewVisibility(titleViewContainer, true);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        titleViewContainer.clearAnimation();
        titleViewContainer.startAnimation(slideDownAnim);
    }

    private void listenToControlButtonClicks(List<DownloadedVideoItem> downloadedVideoItems, int startIndex, VideoControls videoControls) {
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

    private void tryShowPreviousButton(List<DownloadedVideoItem> downloadedVideoItems, int startIndex, VideoControls videoControls) {
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

    private void tryShowNextButton(List<DownloadedVideoItem> downloadedVideoItems, int startIndex, VideoControls videoControls) {
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

    private void tryPlayPreviousEpisode(List<DownloadedVideoItem> downloadedVideoItems, int startIndex) {
        try {
            DownloadedVideoItem previous = downloadedVideoItems.get(startIndex - 1);
            if (previous != null) {
                loadVideo(downloadedVideoItems, downloadedVideoItems.indexOf(previous));
            }
        } catch (Exception e) {
            UiUtils.showSafeToast("Error in playback.Please try again later");
        }
    }

    private void tryPlayNextEpisode(List<DownloadedVideoItem> downloadedVideoItems, int startIndex) {
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
