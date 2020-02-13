package com.molvix.android.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.molvix.android.R;
import com.molvix.android.beans.DownloadedVideoItem;
import com.molvix.android.eventbuses.LoadDownloadedVideosFromFile;
import com.molvix.android.managers.DownloadedItemsPositionsManager;
import com.molvix.android.ui.adapters.DownloadedVideosAdapter;
import com.molvix.android.ui.decorators.MarginDecoration;
import com.molvix.android.ui.widgets.AutoFitRecyclerView;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.utils.UiUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DownloadedVideosFragment extends BaseFragment {

    @BindView(R.id.content_loading_layout)
    View emptyContentsParentView;

    @BindView(R.id.loading_view)
    View loadingView;

    @BindView(R.id.media_empty_view)
    View noMediaView;

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.downloaded_videos_recycler_view)
    AutoFitRecyclerView downloadedMoviesRecyclerView;

    @BindView(R.id.back_nav)
    FloatingActionButton backNav;

    public static List<DownloadedVideoItem> downloadedVideoItems = new ArrayList<>();
    private DownloadedVideosAdapter downloadedVideosAdapter;

    private Handler mUIHandler = new Handler();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View downloadedVideosView = inflater.inflate(R.layout.fragment_downloaded_videos, container, false);
        ButterKnife.bind(this, downloadedVideosView);
        return downloadedVideosView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        backNav.hide();
        backNav.setOnClickListener(v -> Objects.requireNonNull(getActivity()).onBackPressed());
        setupSwipeRefreshLayoutColorScheme();
        setupAdapter();
        File videosDir = FileUtils.getVideosDir();
        loadDownloadedVideos(videosDir.exists() ? videosDir.getName() : "", videosDir);
    }

    @SuppressWarnings("ConstantConditions")
    private void setupSwipeRefreshLayoutColorScheme() {
        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getActivity(), R.color.gplus_color_1),
                ContextCompat.getColor(getActivity(), R.color.gplus_color_2),
                ContextCompat.getColor(getActivity(), R.color.gplus_color_3),
                ContextCompat.getColor(getActivity(), R.color.gplus_color_4));
        swipeRefreshLayout.setOnRefreshListener(() -> {
            File videosDir = FileUtils.getVideosDir();
            loadDownloadedVideos(videosDir.exists() ? videosDir.getName() : "", videosDir);
        });
    }

    private void setupAdapter() {
        downloadedVideosAdapter = new DownloadedVideosAdapter(getActivity(), downloadedVideoItems);
        downloadedMoviesRecyclerView.addItemDecoration(new MarginDecoration(getActivity(), 0));
        downloadedMoviesRecyclerView.setAdapter(downloadedVideosAdapter);
    }

    private String getThumbnailPath(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null && children.length > 0) {
                return getThumbnailPath(children[0]);
            } else {
                return null;
            }
        } else {
            return file.getPath();
        }
    }

    private boolean atLeast10mB(File existingFile) {
        float existingFileLength = FileUtils.getFileSizeInMB(existingFile.length());
        return existingFileLength >= 10;
    }

    private void loadDownloadedVideos(String parentFolder, File dir) {
        if (dir.exists()) {
            downloadedVideoItems.clear();
            downloadedVideosAdapter.notifyDataSetChanged();
            File[] children = dir.listFiles();
            if (children != null && children.length > 0) {
                for (File file : children) {
                    String thumbnailPath = getThumbnailPath(file);
                    if (!file.isHidden() && thumbnailPath != null) {
                        if (!atLeast10mB(new File(thumbnailPath))) {
                            return;
                        }
                        DownloadedVideoItem downloadedVideoItem = new DownloadedVideoItem();
                        downloadedVideoItem.setDownloadedFile(file);
                        downloadedVideoItem.setParentFolderName(parentFolder);
                        downloadedVideoItems.add(downloadedVideoItem);
                        downloadedVideosAdapter.notifyItemInserted(downloadedVideoItems.size() - 1);
                    }
                }
                displayDownloadsAvailableView();
                if (DownloadedItemsPositionsManager.canShowBackNav()) {
                    backNav.show();
                } else {
                    backNav.hide();
                }
                if (!downloadedVideoItems.isEmpty()) {
                    if (downloadedVideoItems.get(0).getParentFolderName().equals(FileUtils.videoFolder())) {
                        backNav.hide();
                    }
                } else {
                    displayNoDownloadedVideosView();
                }
            }
        } else {
            displayNoDownloadedVideosView();
        }
        swipeRefreshLayout.setRefreshing(false);
    }

    private void scrollToLastPosition() {
        try {
            downloadedMoviesRecyclerView.scrollToPosition(DownloadedItemsPositionsManager.getLastPosition());
            DownloadedItemsPositionsManager.popLastPosition();
        } catch (Exception ignored) {

        }
    }

    private void displayDownloadsAvailableView() {
        UiUtils.toggleViewVisibility(emptyContentsParentView, false);
        UiUtils.toggleViewVisibility(loadingView, false);
        UiUtils.toggleViewVisibility(noMediaView, false);
    }

    private void displayNoDownloadedVideosView() {
        UiUtils.toggleViewVisibility(emptyContentsParentView, true);
        UiUtils.toggleViewVisibility(loadingView, false);
        UiUtils.toggleViewVisibility(noMediaView, true);
    }

    @Override
    public void onEvent(Object event) {
        mUIHandler.post(() -> {
            if (event instanceof LoadDownloadedVideosFromFile) {
                LoadDownloadedVideosFromFile downloadedVideosFromFile = (LoadDownloadedVideosFromFile) event;
                loadDownloadedVideos(downloadedVideosFromFile.getParentFolderName(), downloadedVideosFromFile.getParentFolder());
            }
        });
    }

    public boolean needsToNavigateBack() {
        if (downloadedVideoItems.isEmpty()) {
            return false;
        }
        DownloadedVideoItem downloadedVideoItem = downloadedVideoItems.get(0);
        return !downloadedVideoItem.getParentFolderName().equals(FileUtils.videoFolder());
    }

    public void navigateBack() {
        DownloadedVideoItem downloadedVideoItem = downloadedVideoItems.get(0);
        File file = downloadedVideoItem.getDownloadedFile();
        File parentFile = file.getParentFile();
        if (parentFile != null && parentFile.exists()) {
            File superParentFile = parentFile.getParentFile();
            if (superParentFile != null && superParentFile.exists()) {
                loadDownloadedVideos(superParentFile.getName(), superParentFile);
                scrollToLastPosition();
            }
        }
    }
}
