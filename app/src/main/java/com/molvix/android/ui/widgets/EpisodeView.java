package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.managers.EpisodesManager;
import com.molvix.android.managers.FileDownloadManager;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
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
    TextView downloadButtonOrPlayButton;

    @BindView(R.id.download_progress_container)
    View downloadProgressContainer;

    @BindView(R.id.downloadProgressBar)
    ProgressBar downloadProgressBar;

    @BindView(R.id.download_progress_text_view)
    TextView downloadProgressTextView;

    @BindView(R.id.cancel_download_btn)
    Button cancelDownloadBtn;

    @BindView(R.id.clickable_dummy_view)
    View clickableDummyView;

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
        initCancelActiveDownloadButtonEventListener();
    }

    private void initCancelActiveDownloadButtonEventListener() {
        cancelDownloadBtn.setOnClickListener(v -> cancelActiveDownload());
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

    private void initDownloadOrPlayButtonEventListener(Episode episode, String episodeName) {
        downloadButtonOrPlayButton.setOnClickListener(v -> {
            UiUtils.blinkView(v);
            String text = downloadButtonOrPlayButton.getText().toString().trim();
            if (text.equals(getContext().getString(R.string.play))) {
                String downloadUrl;
                int episodeQuality = episode.getEpisodeQuality();
                if (episodeQuality == AppConstants.STANDARD_QUALITY) {
                    downloadUrl = episode.getStandardQualityDownloadLink();
                } else if (episodeQuality == AppConstants.HIGH_QUALITY) {
                    downloadUrl = episode.getHighQualityDownloadLink();
                } else {
                    downloadUrl = episode.getLowQualityDownloadLink();
                }
                String fileExtension = StringUtils.substringAfterLast(downloadUrl, ".");
                String fileName = episodeName + "." + fileExtension;
                File downloadedFile = FileUtils.getFilePath(fileName, WordUtils.capitalize(movie.getMovieName()), season.getSeasonName());
                if (downloadedFile.exists()) {
                    Intent videoIntent = new Intent(Intent.ACTION_VIEW);
                    videoIntent.setDataAndType(Uri.fromFile(downloadedFile), "video/*");
                    getContext().startActivity(videoIntent);
                } else {
                    UiUtils.showSafeToast("Oops! Sorry, an error occurred while attempting to play video.");
                }
            } else {
                if (ConnectivityUtils.isDeviceConnectedToTheInternet()) {
                    if (AppPrefs.getEpisodeDownloadProgress(episode.getEpisodeId()) > -1) {
                        UiUtils.showSafeToast("Download already in progress");
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
        clickableDummyView.setOnClickListener(v -> downloadButtonOrPlayButton.performClick());
    }

    private void checkEpisodeActiveDownloadStatus(Episode episode) {
        int episodeActiveDownloadProgress = AppPrefs.getEpisodeDownloadProgress(episode.getEpisodeId());
        if (episodeActiveDownloadProgress != -1) {
            if (episodeActiveDownloadProgress == 0) {
                downloadButtonOrPlayButton.setText(getContext().getString(R.string.preparing));
                downloadButtonOrPlayButton.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                UiUtils.toggleViewVisibility(downloadProgressContainer, false);
                if (StringUtils.isNotEmpty(AppPrefs.getEpisodeDownloadProgressText(episode.getEpisodeId()))) {
                    setToDownloadInProgress(episode);
                }
            } else {
                setToDownloadInProgress(episode);
            }
        } else {
            downloadButtonOrPlayButton.clearAnimation();
            UiUtils.toggleViewVisibility(downloadProgressContainer, false);
            checkToSeeIfEpisodeAlreadyDownloaded(episode, episode.getEpisodeName());
        }
    }

    private void setToDownloadInProgress(Episode episode) {
        setToDownloadable();
        downloadButtonOrPlayButton.setText(getContext().getString(R.string.downloading));
        //Download has started
        UiUtils.toggleViewVisibility(downloadProgressContainer, true);
        downloadProgressBar.setProgress(AppPrefs.getEpisodeDownloadProgress(episode.getEpisodeId()));
        downloadProgressTextView.setText(AppPrefs.getEpisodeDownloadProgressText(episode.getEpisodeId()));
    }

    private void checkToSeeIfEpisodeAlreadyDownloaded(Episode episode, String episodeName) {
        int episodeQuality = episode.getEpisodeQuality();
        if (episodeQuality != 0) {
            String downloadUrl;
            if (episodeQuality == AppConstants.STANDARD_QUALITY) {
                downloadUrl = episode.getStandardQualityDownloadLink();
            } else if (episodeQuality == AppConstants.HIGH_QUALITY) {
                downloadUrl = episode.getHighQualityDownloadLink();
            } else {
                downloadUrl = episode.getLowQualityDownloadLink();
            }
            if (downloadUrl != null) {
                String fileExtension = StringUtils.substringAfterLast(downloadUrl, ".");
                String fileName = episodeName + "." + fileExtension;
                File existingFile = FileUtils.getFilePath(fileName, WordUtils.capitalize(movie.getMovieName()), WordUtils.capitalize(season.getSeasonName()));
                if (existingFile.exists()) {
                    setToPlayable();
                } else {
                    setToDownloadable();
                }
            } else {
                setToDownloadable();
            }
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
        episodeNameView.setTextColor(ContextCompat.getColor(getContext(), R.color.blue_grey_active));
        VectorDrawableCompat downloadIcon = VectorDrawableCompat.create(getResources(), R.drawable.ic_file_download_white_24dp, null);
        downloadButtonOrPlayButton.setText(getContext().getString(R.string.download));
        downloadButtonOrPlayButton.setCompoundDrawablesWithIntrinsicBounds(downloadIcon, null, null, null);
        downloadButtonOrPlayButton.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
    }

    private void setToPlayable() {
        downloadButtonOrPlayButton.setText(getContext().getString(R.string.play));
        VectorDrawableCompat playIcon = VectorDrawableCompat.create(getResources(), R.drawable.ic_play_arrow_blue_24dp, null);
        downloadButtonOrPlayButton.setCompoundDrawablesWithIntrinsicBounds(playIcon, null, null, null);
        episodeNameView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorGreen));
        downloadButtonOrPlayButton.setTextColor(ContextCompat.getColor(getContext(), R.color.colorGreen));
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
                            Log.d(TAG, episodeFileName + ", " + episodeDownloadLink);
                            downloadOptions.add(episodeDownloadLink);
                        }
                    }
                    if (!downloadOptions.isEmpty()) {
                        String episodeCaptchaSolverLink = null;
                        if (downloadOptions.size() == 2) {
                            try {
                                String standard = downloadOptions.get(0);
                                String lowest = downloadOptions.get(1);
                                if (episode.getEpisodeQuality() == AppConstants.HIGH_QUALITY || episode.getEpisodeQuality() == AppConstants.STANDARD_QUALITY) {
                                    episodeCaptchaSolverLink = standard;
                                } else {
                                    episodeCaptchaSolverLink = lowest;
                                }
                            } catch (Exception ignored) {

                            }
                        } else if (downloadOptions.size() == 3) {
                            try {
                                String standard = downloadOptions.get(0);
                                String highest = downloadOptions.get(1);
                                String lowest = downloadOptions.get(2);
                                if (episode.getEpisodeQuality() == AppConstants.HIGH_QUALITY) {
                                    episodeCaptchaSolverLink = highest;
                                } else if (episode.getEpisodeQuality() == AppConstants.STANDARD_QUALITY) {
                                    episodeCaptchaSolverLink = standard;
                                } else {
                                    episodeCaptchaSolverLink = lowest;
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
