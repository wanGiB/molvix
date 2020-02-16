package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.molvix.android.R;
import com.molvix.android.beans.DownloadedVideoItem;
import com.molvix.android.eventbuses.DownloadedFileDeletedEvent;
import com.molvix.android.eventbuses.LoadDownloadedVideosFromFile;
import com.molvix.android.managers.DownloadedItemsPositionsManager;
import com.molvix.android.managers.ThemeManager;
import com.molvix.android.ui.activities.MainActivity;
import com.molvix.android.ui.fragments.DownloadedVideosFragment;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.utils.ThumbNailUtils;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DownloadedVideoItemView extends FrameLayout {

    @BindView(R.id.video_preview)
    ImageView videoPreview;

    @BindView(R.id.bottom_title_view)
    TextView bottomTitleView;

    @BindView(R.id.duration_text_view)
    TextView durationTextView;

    public DownloadedVideoItemView(@NonNull Context context) {
        super(context);
        initUI(context);
    }

    public DownloadedVideoItemView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initUI(context);
    }

    public DownloadedVideoItemView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initUI(context);
    }

    private void initUI(Context context) {
        removeAllViews();
        @SuppressLint("InflateParams") View downloadedVideoItemView = LayoutInflater.from(context).inflate(R.layout.downloaded_video_item_view, null);
        ButterKnife.bind(this, downloadedVideoItemView);
        addView(downloadedVideoItemView);
    }

    public void bindDownloadedVideoItem(DownloadedVideoItem downloadedVideoItem, int position) {
        File downloadedFile = downloadedVideoItem.getDownloadedFile();
        String parentFolderName = downloadedVideoItem.getParentFolderName();
        if (parentFolderName.equals(FileUtils.videoFolder())) {
            UiUtils.toggleViewVisibility(durationTextView, false);
            //this is the video name folder
            String videoName = downloadedFile.getName();
            bottomTitleView.setText(videoName.concat(enumerateSeasonsInFolder(downloadedFile)));
        } else {
            if (downloadedFile.isDirectory()) {
                UiUtils.toggleViewVisibility(durationTextView, false);
                bottomTitleView.setText(downloadedFile.getName());
            } else {
                UiUtils.toggleViewVisibility(durationTextView, true);
                String downloadedFilePath = downloadedFile.getPath();
                downloadedFilePath = StringUtils.remove(downloadedFilePath, File.separator + parentFolderName);
                downloadedFilePath = StringUtils.remove(downloadedFilePath, File.separator + downloadedFile.getName());
                String movieName = StringUtils.substringAfterLast(downloadedFilePath, File.separator);
                String episodeName = downloadedFile.getName();
                bottomTitleView.setText(episodeName);
                downloadedVideoItem.setTitle(movieName + ", " + parentFolderName + "-" + episodeName);
                try {
                    int videoDuration = MediaPlayer.create(getContext(), Uri.fromFile(downloadedFile)).getDuration();
                    String durationText = DateUtils.formatElapsedTime(videoDuration / 1000);
                    durationTextView.setText(durationText);
                } catch (Exception ignored) {

                }
            }
        }
        String thumbnailPath = ThumbNailUtils.getThumbnailPath(downloadedFile);
        if (thumbnailPath != null) {
            UiUtils.loadVideoThumbNailIntoView(videoPreview, thumbnailPath);
        } else {
            ThemeManager.ThemeSelection themeSelection = ThemeManager.getThemeSelection();
            videoPreview.setImageDrawable(new ColorDrawable(ContextCompat.getColor(getContext(), themeSelection == ThemeManager.ThemeSelection.DARK ? R.color.dracula_surface_color : R.color.ease_gray)));
        }
        attachClickEventListener(downloadedVideoItem, position, downloadedFile);
        attachDeleteEventListener(downloadedVideoItem, downloadedFile);
    }

    private void attachClickEventListener(DownloadedVideoItem downloadedVideoItem, int position, File downloadedFile) {
        OnClickListener onClickListener = v -> {
            UiUtils.blinkView(videoPreview);
            if (downloadedFile.isDirectory()) {
                EventBus.getDefault().post(new LoadDownloadedVideosFromFile(downloadedFile.getName(), downloadedFile));
                DownloadedItemsPositionsManager.enquePosition(position);
            } else {
                //Play Video Here
                if (getContext() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getContext();
                    mainActivity.playVideo(DownloadedVideosFragment.downloadedVideoItems, downloadedVideoItem);
                }
            }
        };
        setOnClickListener(onClickListener);
        bottomTitleView.setOnClickListener(onClickListener);
        videoPreview.setOnClickListener(onClickListener);
        durationTextView.setOnClickListener(onClickListener);
    }

    private void attachDeleteEventListener(DownloadedVideoItem downloadedVideoItem, File downloadedFile) {
        OnLongClickListener onLongClickListener = v -> {
            AlertDialog.Builder deletePromptDialogBuilder = new AlertDialog.Builder(getContext());
            deletePromptDialogBuilder.setTitle("Attention!");
            deletePromptDialogBuilder.setMessage("Delete " + downloadedFile.getName() + "?");
            deletePromptDialogBuilder.setPositiveButton("DELETE", (dialog, which) -> {
                dialog.dismiss();
                if (!downloadedFile.isDirectory()) {
                    boolean deleted = downloadedFile.delete();
                    if (deleted) {
                        EventBus.getDefault().post(new DownloadedFileDeletedEvent(downloadedVideoItem));
                    } else {
                        UiUtils.showSafeToast("Sorry, we couldn't delete the episode at this point in time. Please try again later.");
                    }
                } else {
                    try {
                        boolean deleted = FileUtils.deleteDirectory(downloadedFile);
                        if (deleted) {
                            EventBus.getDefault().post(new DownloadedFileDeletedEvent(downloadedVideoItem));
                        } else {
                            UiUtils.showSafeToast("Sorry, failed to delete directory.Please try again");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        UiUtils.showSafeToast("Sorry, failed to delete directory.Please try again");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        UiUtils.showSafeToast("Sorry, failed to delete directory.Please try again");
                    }
                }
            });
            deletePromptDialogBuilder.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
            deletePromptDialogBuilder.create().show();
            return true;
        };
        setOnLongClickListener(onLongClickListener);
        bottomTitleView.setOnLongClickListener(onLongClickListener);
        videoPreview.setOnLongClickListener(onLongClickListener);
        durationTextView.setOnLongClickListener(onLongClickListener);
    }

    private String enumerateSeasonsInFolder(File downloadedFile) {
        File[] seasons = downloadedFile.listFiles();
        if (seasons != null && seasons.length > 0) {
            int seasonsLength = seasons.length;
            StringBuilder seasonsCountBuilder = new StringBuilder();
            int threshold = 3;
            for (int i = 0; i < seasonsLength; i++) {
                int seasonValue = i + 1;
                if (i > threshold) {
                    seasonsCountBuilder.append("...").append(seasonsLength);
                    break;
                } else {
                    seasonsCountBuilder.append(seasonValue).append((i + 1 > threshold) ? "" : ",");
                }
            }
            return "(" + StringUtils.stripEnd(seasonsCountBuilder.toString(), ",") + ")";
        }
        return "";
    }

}
