package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.molvix.android.R;
import com.molvix.android.beans.DownloadedVideoItem;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.managers.EpisodesManager;
import com.molvix.android.managers.FileDownloadManager;
import com.molvix.android.managers.ThemeManager;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.ui.activities.MainActivity;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.utils.MolvixLogger;
import com.molvix.android.utils.ThumbNailUtils;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class EpisodeView extends FrameLayout {

    private static String TAG = EpisodeView.class.getSimpleName();

    @BindView(R.id.episode_name_view)
    TextView episodeNameView;

    @BindView(R.id.episode_download_options_spinner_view)
    Spinner episodeDownloadOptionsSpinner;

    @BindView(R.id.episode_download_button_view)
    TextView downloadOrPlayButton;

    @BindView(R.id.download_progress_container)
    View downloadProgressContainer;

    @BindView(R.id.downloadProgressBar)
    ProgressBar downloadProgressBar;

    @BindView(R.id.download_progress_text_view)
    TextView downloadProgressTextView;

    @BindView(R.id.cancel_download)
    ImageView cancelDownloadBtn;

    @BindView(R.id.pause_or_resume_download_btn)
    ImageView pauseOrResumeBtn;

    @BindView(R.id.clickable_dummy_view)
    View clickableDummyView;

    @BindView(R.id.download_directory_disclaimer)
    MolvixTextView downloadDirectoryFolderView;

    private Episode episode;
    private Season season;
    private Movie movie;

    public EpisodeView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public EpisodeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EpisodeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        removeAllViews();
        @SuppressLint("InflateParams") View rootView = LayoutInflater.from(context).inflate(R.layout.episode_view, null);
        ButterKnife.bind(this, rootView);
        addView(rootView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public void bindEpisode(Episode episode) {
        this.episode = episode;
        season = episode.getSeason();
        movie = season.getMovie();
        String episodeName = setupEpisodeName(episode);
        initSpinner(episode);
        checkEpisodeActiveDownloadStatus(episode);
        initDownloadOrPlayButtonEventListener(episode, episodeName);
        initCancelDownloadButton();
        initActionButtonsEventListener();
    }

    private void initCancelDownloadButton() {
        ThemeManager.ThemeSelection themeSelection = ThemeManager.getThemeSelection();
        VectorDrawableCompat cancelButtonIcon = VectorDrawableCompat.create(getResources(),
                themeSelection == ThemeManager.ThemeSelection.DARK
                        ? R.drawable.ic_cancel_download_white :
                        R.drawable.ic_cancel_download_dark, null);
        cancelDownloadBtn.setImageDrawable(cancelButtonIcon);
    }

    public void showDownloadDirInstr() {
        String message = "Videos would be downloaded to the <b>...\"/Molvix/Videos/" + WordUtils.capitalize(movie.getMovieName()) + "/" + season.getSeasonName() + "/" + "\"</b> folder";
        UiUtils.toggleViewVisibility(downloadDirectoryFolderView, true);
        downloadDirectoryFolderView.setText(UiUtils.fromHtml(message));
    }

    public void hideDownloadDir() {
        UiUtils.toggleViewVisibility(downloadDirectoryFolderView, false);
    }

    private void initActionButtonsEventListener() {
        cancelDownloadBtn.setOnClickListener(v -> {
            UiUtils.blinkView(v);
            cancelActiveDownload();
        });
        pauseOrResumeBtn.setOnClickListener(v -> {
            UiUtils.blinkView(v);
            boolean isPaused = AppPrefs.isPaused(episode.getEpisodeId());
            if (isPaused) {
                resumeDownload();
                showPauseButton();
            } else {
                pauseDownload();
                showResumeButton();
            }
        });
    }

    private void pauseDownload() {
        FileDownloadManager.pauseDownload(episode);
    }

    private void resumeDownload() {
        FileDownloadManager.downloadEpisode(episode);
        AppPrefs.setPaused(episode.getEpisodeId(), false);
    }

    private void showPauseButton() {
        ThemeManager.ThemeSelection themeSelection = ThemeManager.getThemeSelection();
        VectorDrawableCompat pauseBtn = VectorDrawableCompat.create(getResources(),
                themeSelection == ThemeManager.ThemeSelection.DARK
                        ? R.drawable.ic_pause_light :
                        R.drawable.ic_pause_dark, null);
        pauseOrResumeBtn.setImageDrawable(pauseBtn);
        downloadOrPlayButton.setText(getContext().getString(R.string.downloading));
    }

    private void showResumeButton() {
        ThemeManager.ThemeSelection themeSelection = ThemeManager.getThemeSelection();
        VectorDrawableCompat resumeBtn = VectorDrawableCompat.create(getResources(),
                themeSelection == ThemeManager.ThemeSelection.DARK ?
                        R.drawable.ic_resume_light :
                        R.drawable.ic_resume_dark, null);
        pauseOrResumeBtn.setImageDrawable(resumeBtn);
        downloadOrPlayButton.setText(getContext().getString(R.string.paused));
    }

    private void cancelActiveDownload() {
        FileDownloadManager.cancelDownload(episode);
    }

    private String setupEpisodeName(Episode episode) {
        String episodeName = episode.getEpisodeName();
        if (StringUtils.isNotEmpty(episodeName)) {
            episodeNameView.setText(episodeName);
        }
        return episodeName;
    }

    private void initPlayScope(File file) {
        AlertDialog.Builder filePlayScopeOptionsBuilder = new AlertDialog.Builder(getContext());
        filePlayScopeOptionsBuilder.setTitle("Play");
        filePlayScopeOptionsBuilder.setSingleChoiceItems(new CharSequence[]{"Within Molvix", "Outside Molvix"}, -1, (dialog, which) -> {
            dialog.dismiss();
            if (which == 0) {
                playWithinApp(file);
            } else if (which == 1) {
                playOutSideApp(file);
            }
        });
        filePlayScopeOptionsBuilder.create().show();
    }

    private void playWithinApp(File file) {
        File seasonDir = file.getParentFile();
        DownloadedVideoItem downloadedVideoItem = getDownloadedVideoItem(file, seasonDir);
        List<DownloadedVideoItem> downloadedVideoItems = new ArrayList<>();
        downloadedVideoItems.add(downloadedVideoItem);
        if (seasonDir != null) {
            File[] otherEpisodes = seasonDir.listFiles();
            if (otherEpisodes != null && otherEpisodes.length > 0) {
                for (File episode : otherEpisodes) {
                    String fileThumbNailPath = ThumbNailUtils.getThumbnailPath(episode);
                    if (!episode.isHidden() && fileThumbNailPath != null) {
                        if (FileUtils.isAtLeast10mB(new File(fileThumbNailPath))) {
                            File immediateParentDir = episode.getParentFile();
                            DownloadedVideoItem downloadedEpisodeItem = getDownloadedVideoItem(episode, immediateParentDir);
                            if (!downloadedVideoItems.contains(downloadedEpisodeItem)) {
                                downloadedVideoItems.add(downloadedEpisodeItem);
                            }
                        }
                    }
                }
            }
        }
        if (!downloadedVideoItems.isEmpty()) {
            if (getContext() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getContext();
                //Close the BottomSheetDialog before playing video
                mainActivity.onBackPressed();
                mainActivity.playVideo(downloadedVideoItems, downloadedVideoItem);
            }
        }
    }

    @NotNull
    private DownloadedVideoItem getDownloadedVideoItem(File file, File seasonDir) {
        String movieName = null;
        String episodeName = file.getName();
        DownloadedVideoItem downloadedVideoItem = new DownloadedVideoItem();
        downloadedVideoItem.setDownloadedFile(file);
        String parentFolderName = null;
        File movieDir = null;
        if (seasonDir != null) {
            parentFolderName = seasonDir.getName();
            movieDir = seasonDir.getParentFile();
        }
        if (parentFolderName != null) {
            downloadedVideoItem.setParentFolderName(parentFolderName);
        }
        if (movieDir != null) {
            movieName = movieDir.getName();
        }
        downloadedVideoItem.setTitle(movieName + ", " + parentFolderName + "-" + episodeName);
        return downloadedVideoItem;
    }

    private void initDownloadOrPlayButtonEventListener(Episode episode, String episodeName) {
        downloadOrPlayButton.setOnClickListener(v -> {
            UiUtils.blinkView(v);
            String text = downloadOrPlayButton.getText().toString().trim();
            if (text.equals(getContext().getString(R.string.play))) {
                String fileName = episodeName + ".mp4";
                File downloadedFile = FileUtils.getFilePath(fileName, WordUtils.capitalize(movie.getMovieName()), season.getSeasonName());
                if (downloadedFile.exists()) {
                    initPlayScope(downloadedFile);
                } else {
                    UiUtils.showSafeToast("Oops! Sorry, an error occurred while attempting to play video.");
                }
            } else {
                if (ConnectivityUtils.isDeviceConnectedToTheInternet()) {
                    if (AppPrefs.getEpisodeDownloadProgress(episode.getEpisodeId()) > -1) {
                        String progressMessage = AppPrefs.getEpisodeDownloadProgressText(episode.getEpisodeId());
                        if (StringUtils.isNotEmpty(progressMessage)) {
                            if (AppPrefs.isPaused(episode.getEpisodeId())) {
                                pauseOrResumeBtn.performClick();
                            } else {
                                return;
                            }
                        } else {
                            UiUtils.showSafeToast("Download already in progress");
                        }
                        return;
                    }
                    int episodeQualitySelection = episodeDownloadOptionsSpinner.getSelectedItemPosition();
                    if (episodeQualitySelection == 0) {
                        episode.setEpisodeQuality(AppConstants.HIGH_QUALITY);
                    } else if (episodeQualitySelection == 1) {
                        episode.setEpisodeQuality(AppConstants.STANDARD_QUALITY);
                    } else {
                        episode.setEpisodeQuality(AppConstants.LOW_QUALITY);
                    }
                    MolvixDB.updateEpisode(episode);
                    AppPrefs.updateEpisodeDownloadProgress(episode.getEpisodeId(), 0);
                    extractEpisodeDownloadOptions(episode);
                } else {
                    UiUtils.showSafeToast("Please connect to the internet and try again.");
                }
            }
        });
        clickableDummyView.setOnClickListener(v -> downloadOrPlayButton.performClick());
    }

    private void playOutSideApp(File downloadedFile) {
        Intent videoIntent = new Intent(Intent.ACTION_VIEW);
        Uri videoUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            videoUri = FileProvider.getUriForFile(getContext(),
                    getContext().getApplicationContext()
                            .getPackageName() + ".provider", downloadedFile);
        } else {
            videoUri = Uri.fromFile(downloadedFile);
        }
        videoIntent.setDataAndType(videoUri, "video/*");
        videoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        getContext().startActivity(videoIntent);
    }

    private void checkEpisodeActiveDownloadStatus(Episode episode) {
        int episodeActiveDownloadProgress = AppPrefs.getEpisodeDownloadProgress(episode.getEpisodeId());
        if (episodeActiveDownloadProgress != -1) {
            if (episodeActiveDownloadProgress == 0) {
                downloadOrPlayButton.setText(getContext().getString(R.string.preparing));
                downloadOrPlayButton.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                setEpisodeNameViewDefaultColor();
                downloadOrPlayButton.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                UiUtils.toggleViewVisibility(downloadProgressContainer, false);
                if (StringUtils.isNotEmpty(AppPrefs.getEpisodeDownloadProgressText(episode.getEpisodeId()))) {
                    setToDownloadInProgress(episode);
                }
            } else {
                setToDownloadInProgress(episode);
            }
        } else {
            downloadOrPlayButton.clearAnimation();
            UiUtils.toggleViewVisibility(downloadProgressContainer, false);
            checkToSeeIfEpisodeAlreadyDownloaded(episode.getEpisodeName());
        }
    }

    private void setEpisodeNameViewDefaultColor() {
        int activeTheme = AppCompatDelegate.getDefaultNightMode();
        if (activeTheme == AppCompatDelegate.MODE_NIGHT_NO) {
            episodeNameView.setTextColor(ContextCompat.getColor(getContext(), R.color.dracula_primary));
        } else {
            episodeNameView.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        }
    }

    private void setToDownloadInProgress(Episode episode) {
        setToDownloadable();
        boolean paused = AppPrefs.isPaused(episode.getEpisodeId());
        if (paused) {
            showResumeButton();
        } else {
            showPauseButton();
        }
        //Download has started
        UiUtils.toggleViewVisibility(downloadProgressContainer, true);
        downloadProgressBar.setProgress(AppPrefs.getEpisodeDownloadProgress(episode.getEpisodeId()));
        downloadProgressTextView.setText(AppPrefs.getEpisodeDownloadProgressText(episode.getEpisodeId()));
    }

    private void checkToSeeIfEpisodeAlreadyDownloaded(String episodeName) {
        String fileName = episodeName + ".mp4";
        File existingFile = FileUtils.getFilePath(fileName, WordUtils.capitalize(movie.getMovieName()), WordUtils.capitalize(season.getSeasonName()));
        if (existingFile.exists()) {
            setToPlayable(existingFile);
        } else {
            setToDownloadable();
        }
    }

    private void initSpinner(Episode episode) {
        checkAndSelectEpisodeQuality(episode);
    }

    private void checkAndSelectEpisodeQuality(Episode episode) {
        int existingEpisodeQuality = episode.getEpisodeQuality();
        if (existingEpisodeQuality != 0) {
            if (existingEpisodeQuality == AppConstants.STANDARD_QUALITY) {
                episodeDownloadOptionsSpinner.setSelection(1);
            } else if (existingEpisodeQuality == AppConstants.HIGH_QUALITY) {
                episodeDownloadOptionsSpinner.setSelection(0);
            } else {
                episodeDownloadOptionsSpinner.setSelection(2);
            }
        } else {
            episodeDownloadOptionsSpinner.setSelection(1);
        }
    }

    private void setToDownloadable() {
        setEpisodeNameViewDefaultColor();
        VectorDrawableCompat downloadIcon = VectorDrawableCompat.create(getResources(), R.drawable.file_download_in_progress, null);
        downloadOrPlayButton.setText(getContext().getString(R.string.download));
        downloadOrPlayButton.setCompoundDrawablesWithIntrinsicBounds(downloadIcon, null, null, null);
        downloadOrPlayButton.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
    }

    private void setToPlayable(File existingFile) {
        if (FileUtils.isAtLeast10mB(existingFile)) {
            downloadOrPlayButton.setText(getContext().getString(R.string.play));
            VectorDrawableCompat playIcon = VectorDrawableCompat.create(getResources(), R.drawable.ic_play_arrow, null);
            downloadOrPlayButton.setCompoundDrawablesWithIntrinsicBounds(playIcon, null, null, null);
            episodeNameView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorGreen));
            downloadOrPlayButton.setTextColor(ContextCompat.getColor(getContext(), R.color.colorGreen));
        } else {
            setToDownloadable();
        }
    }

    private void extractEpisodeDownloadOptions(Episode episode) {
        new EpisodeDownloadOptionsExtractionTask(episode).execute();
    }

    static class EpisodeDownloadOptionsExtractionTask extends AsyncTask<Void, Void, Void> {

        public Episode episode;

        EpisodeDownloadOptionsExtractionTask(Episode episode) {
            this.episode = episode;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            fetchDownloadOptionsForEpisode(episode);
            return null;
        }

        private void fetchDownloadOptionsForEpisode(Episode episode) {
            try {
                Document episodeDocument = Jsoup.connect(episode.getEpisodeLink()).get();
                //Bring out all href elements containing download
                Elements links = episodeDocument.select("a[href]");
                if (links != null && !links.isEmpty()) {
                    List<String> downloadOptions = new ArrayList<>();
                    for (Element link : links) {
                        String episodeFileName = link.text();
                        String episodeDownloadLink = link.attr("href");
                        if (episodeDownloadLink.contains(AppConstants.DOWNLOADABLE)) {
                            MolvixLogger.d(TAG, episodeFileName + ", " + episodeDownloadLink);
                            downloadOptions.add(episodeDownloadLink);
                        }
                    }
                    if (!downloadOptions.isEmpty()) {
                        String episodeCaptchaSolverLink = null;
                        if (downloadOptions.size() == 1) {
                            episodeCaptchaSolverLink = downloadOptions.get(0);
                        } else if (downloadOptions.size() == 2) {
                            try {
                                String standard = downloadOptions.get(0);
                                if (episode.getEpisodeQuality() == AppConstants.HIGH_QUALITY || episode.getEpisodeQuality() == AppConstants.STANDARD_QUALITY) {
                                    episodeCaptchaSolverLink = standard;
                                } else {
                                    episodeCaptchaSolverLink = standard;
                                }
                            } catch (Exception ignored) {

                            }
                        } else if (downloadOptions.size() == 3) {
                            try {
                                String standard = downloadOptions.get(0);
                                String highest = downloadOptions.get(1);
                                if (episode.getEpisodeQuality() == AppConstants.HIGH_QUALITY) {
                                    episodeCaptchaSolverLink = highest;
                                } else if (episode.getEpisodeQuality() == AppConstants.STANDARD_QUALITY) {
                                    episodeCaptchaSolverLink = standard;
                                } else {
                                    episodeCaptchaSolverLink = standard;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        String finalEpisodeCaptchaSolverLink = episodeCaptchaSolverLink;
                        if (finalEpisodeCaptchaSolverLink != null) {
                            episode.setEpisodeCaptchaSolverLink(finalEpisodeCaptchaSolverLink);
                            MolvixDB.updateEpisode(episode);
                            EpisodesManager.enqueDownloadableEpisode(episode);
                        } else {
                            UiUtils.showSafeToast("Sorry, failed to download " + episode.getEpisodeName() + ".Please try again.");
                            AppPrefs.updateEpisodeDownloadProgress(episode.getEpisodeId(), -1);
                            MolvixDB.updateEpisode(episode);
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                UiUtils.showSafeToast("Sorry, failed to download " + episode.getEpisodeName() + ".Please try again.");
                AppPrefs.updateEpisodeDownloadProgress(episode.getEpisodeId(), -1);
            }
        }
    }
}
