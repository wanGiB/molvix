package com.molvix.android.ui.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.molvix.android.R;
import com.molvix.android.beans.DownloadedVideoItem;
import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.eventbuses.DownloadedFileDeletedEvent;
import com.molvix.android.eventbuses.LoadDownloadedVideosFromFile;
import com.molvix.android.managers.DownloadedItemsPositionsManager;
import com.molvix.android.ui.adapters.DownloadedVideosAdapter;
import com.molvix.android.ui.decorators.MarginDecoration;
import com.molvix.android.ui.widgets.AutoFitRecyclerView;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DownloadedVideosFragment extends BaseFragment {

    @BindView(R.id.content_loading_layout)
    View emptyContentsParentView;

    @BindView(R.id.downloaded_videos_center_label)
    TextView emptyContentMessageView;

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

    @BindView(R.id.nav_path_view)
    TextView navPathView;

    public static List<DownloadedVideoItem> downloadedVideoItems = new ArrayList<>();
    private DownloadedVideosAdapter downloadedVideosAdapter;
    private Handler mUIHandler = new Handler();
    private File lastParentFile;

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
        UiUtils.toggleViewVisibility(navPathView, false);
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
            if (downloadedVideoItems.isEmpty()) {
                File videosDir = FileUtils.getVideosDir();
                loadDownloadedVideos(videosDir.exists() ? videosDir.getName() : "", videosDir);
            } else {
                DownloadedVideoItem firstItemOnTheList = downloadedVideoItems.get(0);
                File firstFile = firstItemOnTheList.getDownloadedFile();
                if (firstFile != null) {
                    File parentFile = firstFile.getParentFile();
                    if (parentFile != null && parentFile.exists()) {
                        loadDownloadedVideos(parentFile.getName(), parentFile);
                    }
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void setupAdapter() {
        downloadedVideosAdapter = new DownloadedVideosAdapter(getActivity(), downloadedVideoItems);
        downloadedMoviesRecyclerView.addItemDecoration(new MarginDecoration(getActivity(), 0));
        downloadedMoviesRecyclerView.setAdapter(downloadedVideosAdapter);
    }

    private void loadDownloadedVideos(String parentFolder, File dir) {
        if (dir.exists()) {
            File[] children = dir.listFiles();
            downloadedVideoItems.clear();
            downloadedVideosAdapter.notifyDataSetChanged();
            if (children != null && children.length > 0) {
                for (File file : children) {
                    if (!file.isHidden()) {
                        DownloadedVideoItem downloadedVideoItem = new DownloadedVideoItem();
                        downloadedVideoItem.setDownloadedFile(file);
                        downloadedVideoItem.setParentFolderName(parentFolder);
                        downloadedVideoItems.add(downloadedVideoItem);
                        downloadedVideosAdapter.notifyItemInserted(downloadedVideoItems.size() - 1);
                        if (downloadedVideoItem.getParentFolderName().equals(FileUtils.videoFolder())) {
                            backNav.hide();
                            UiUtils.toggleViewVisibility(navPathView, false);
                        } else {
                            backNav.show();
                            drawPathString(downloadedVideoItem);
                        }
                    }
                }
                if (downloadedVideoItems.isEmpty()) {
                    displayNoDownloadedVideosView();
                } else {
                    displayDownloadsAvailableView();
                }
            } else {
                if (parentFolder.equals(FileUtils.videoFolder())) {
                    displayNoDownloadedVideosView();
                } else {
                    displayNoDownloadedVideosView();
                    lastParentFile = dir;
                    emptyContentMessageView.setText(getString(R.string.directory_empty_msg));
                }
            }
        } else {
            displayNoDownloadedVideosView();
        }
        swipeRefreshLayout.setRefreshing(false);
    }

    private void drawPathString(DownloadedVideoItem downloadedVideoItem) {
        File downloadedFileItem = downloadedVideoItem.getDownloadedFile();
        String fileName = downloadedFileItem.getName();
        String filePath = downloadedFileItem.getPath();
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File externalFilesDir = ApplicationLoader.getInstance().getExternalFilesDir(null);
            if (externalFilesDir != null && externalFilesDir.exists()) {
                String externalStoragePath = externalFilesDir.getPath();
                filePath = StringUtils.remove(filePath, externalStoragePath).replace("/" + FileUtils.getRootFolder(), "").replace("/" + FileUtils.videoFolder(), "");
            }
        }
        filePath = filePath.replace("/Android/data/com.molvix.android/files", "");
        filePath = StringUtils.remove(filePath, "/" + FileUtils.getRootFolder()).replace("/" + FileUtils.videoFolder(), "");
        filePath = filePath.replace(fileName, "");
        UiUtils.toggleViewVisibility(navPathView, true);
        navPathView.setText("...".concat(filePath));
        if (StringUtils.isEmpty(filePath)) {
            UiUtils.toggleViewVisibility(navPathView, false);
            backNav.hide();
        }
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
        lastParentFile = null;
    }

    private void displayNoDownloadedVideosView() {
        UiUtils.toggleViewVisibility(emptyContentsParentView, true);
        UiUtils.toggleViewVisibility(loadingView, false);
        UiUtils.toggleViewVisibility(noMediaView, true);
        emptyContentMessageView.setText(getString(R.string.no_downloaded_videos));
        lastParentFile = null;
    }

    @Override
    public void onEvent(Object event) {
        mUIHandler.post(() -> {
            if (event instanceof LoadDownloadedVideosFromFile) {
                LoadDownloadedVideosFromFile downloadedVideosFromFile = (LoadDownloadedVideosFromFile) event;
                loadDownloadedVideos(downloadedVideosFromFile.getParentFolderName(), downloadedVideosFromFile.getParentFolder());
            } else if (event instanceof DownloadedFileDeletedEvent) {
                DownloadedFileDeletedEvent downloadedFileDeletedEvent = (DownloadedFileDeletedEvent) event;
                DownloadedVideoItem downloadedVideoItem = downloadedFileDeletedEvent.getDownloadedVideoItem();
                File file = downloadedVideoItem.getDownloadedFile();
                File parentFile = file.getParentFile();
                downloadedVideoItems.remove(downloadedVideoItem);
                downloadedVideosAdapter.notifyDataSetChanged();
                if (downloadedVideoItems.isEmpty()) {
                    checkAndLoadParentFileContents(parentFile);
                }
            }
        });
    }

    private void checkAndLoadParentFileContents(File parentFile) {
        if (parentFile != null && parentFile.exists()) {
            File superParentFile = parentFile.getParentFile();
            if (superParentFile != null && superParentFile.exists()) {
                String superParentFileName = superParentFile.getName();
                loadDownloadedVideos(superParentFileName, superParentFile);
            }
        }
    }

    public boolean needsToNavigateBack() {
        if (downloadedVideoItems.isEmpty()) {
            return lastParentFile != null;
        }
        DownloadedVideoItem downloadedVideoItem = downloadedVideoItems.get(0);
        return !downloadedVideoItem.getParentFolderName().equals(FileUtils.videoFolder());
    }

    public void navigateBack() {
        if (lastParentFile != null && lastParentFile.exists()) {
            checkAndLoadParentFileContents(lastParentFile);
        } else {
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
}
