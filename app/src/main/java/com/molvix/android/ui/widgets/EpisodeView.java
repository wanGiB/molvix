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
import android.view.animation.Animation;
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

    private Episode episode;
    private Season season;
    private Movie movie;
    private Animation mFadeInFadeIn;

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
        mFadeInFadeIn = UiUtils.getAnimation(getContext(), android.R.anim.fade_in);
        @SuppressLint("InflateParams") View rootView = LayoutInflater.from(context).inflate(R.layout.episode_view, null);
        ButterKnife.bind(this, rootView);
        removeAllViews();
        addView(rootView);
        requestLayout();
    }

    public void bindEpisode(Episode episode) {
        this.episode = episode;
        season = episode.getSeason();
        movie = season.getMovie();
        String episodeName = setupEpisodeName(episode);
        initSpinner(episode);
        initDownloadOrPlayButtonEventListener(episode, episodeName);
        checkEpisodeActiveDownloadStatus(episode);
        initCancelActiveDownloadButtonEventListener();
    }

    private void initCancelActiveDownloadButtonEventListener() {
        cancelDownloadBtn.setOnClickListener(v -> cancelActiveDownload());
    }

    private void cancelActiveDownload() {
        FileDownloadManager.cancelDownload(getCurrentDownloadId());
    }

    private int getCurrentDownloadId() {
        String movieName = WordUtils.capitalize(movie.getMovieName());
        String seasonName = WordUtils.capitalize(season.getSeasonName());
        int downloadQuality = episode.getEpisodeQuality();
        String downloadUrl;
        if (downloadQuality == AppConstants.HIGH_QUALITY) {
            downloadUrl = episode.getHighQualityDownloadLink();
        } else if (downloadQuality == AppConstants.STANDARD_QUALITY) {
            downloadUrl = episode.getStandardQualityDownloadLink();
        } else {
            downloadUrl = episode.getLowQualityDownloadLink();
        }
        String fileExtension = StringUtils.substringAfter(downloadUrl, ".");
        String fileName = episode.getEpisodeName() + "." + fileExtension;
        String dirPath = FileUtils.getFilePath(movieName, seasonName).getPath();
        return (dirPath + fileName).hashCode();
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
                String fileExtension = StringUtils.substringAfter(downloadUrl, ".");
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
                    if (downloadButtonOrPlayButton.getAnimation() == null) {
                        int episodeQualitySelection = episodeDownloadOptionsSpinner.getSelectedItemPosition();
                        if (episodeQualitySelection == 0) {
                            episode.setEpisodeQuality(AppConstants.HIGH_QUALITY);
                        } else if (episodeQualitySelection == 1) {
                            episode.setEpisodeQuality(AppConstants.STANDARD_QUALITY);
                        } else {
                            episode.setEpisodeQuality(AppConstants.LOW_QUALITY);
                        }
                        episode.setDownloadProgress(0);
                        MolvixDB.updateEpisode(episode);
                        extractEpisodeDownloadOptions(episode);
                    }
                } else {
                    UiUtils.showSafeToast("Please connect to the internet and try again.");
                }
            }
        });
    }

    private void checkEpisodeActiveDownloadStatus(Episode episode) {
        int episodeActiveDownloadProgress = episode.getDownloadProgress();
        if (episodeActiveDownloadProgress != -1) {
            if (episodeActiveDownloadProgress == 0) {
                downloadButtonOrPlayButton.setText(getContext().getString(R.string.preparing));
                downloadButtonOrPlayButton.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                animateDownloadButton();
                UiUtils.toggleViewVisibility(downloadProgressContainer, false);
            } else {
                setToDownloadable();
                downloadButtonOrPlayButton.setText(getContext().getString(R.string.downloading));
                animateDownloadButton();
                //Download has started
                UiUtils.toggleViewVisibility(downloadProgressContainer, true);
                downloadProgressBar.setProgress(episode.getDownloadProgress());
                downloadProgressTextView.setText(episode.getProgressDisplayText());
            }
        } else {
            downloadButtonOrPlayButton.clearAnimation();
            UiUtils.toggleViewVisibility(downloadProgressContainer, false);
            checkToSeeIfEpisodeAlreadyDownloaded(episode, episode.getEpisodeName());
        }
    }

    private void animateDownloadButton() {
        mFadeInFadeIn.setDuration(800);
        mFadeInFadeIn.setRepeatMode(Animation.REVERSE);
        mFadeInFadeIn.setRepeatCount(Animation.INFINITE);
        downloadButtonOrPlayButton.clearAnimation();
        UiUtils.animateView(downloadButtonOrPlayButton, mFadeInFadeIn);
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
                String fileExtension = StringUtils.substringAfter(downloadUrl, ".");
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
        new EpisodeDownloadOptionsExtractionTask(episode.getEpisodeId()).execute();
    }

    static class EpisodeDownloadOptionsExtractionTask extends AsyncTask<Void, Void, Void> {

        private String episodeId;

        EpisodeDownloadOptionsExtractionTask(String episodeId) {
            this.episodeId = episodeId;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            fetchDownloadOptionsForEpisode(episodeId);
            return null;
        }

        private void fetchDownloadOptionsForEpisode(String episodeId) {
            try {
                Episode episode = MolvixDB.getEpisode(episodeId);
                if (episode != null) {
                    Document episodeDocument = Jsoup.connect(episode.getEpisodeLink()).get();
                    //Bring out all href elements containing
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
                                } catch (Exception ignored) {

                                }
                            }
                            String finalEpisodeCaptchaSolverLink = episodeCaptchaSolverLink;
                            if (finalEpisodeCaptchaSolverLink != null) {
                                episode.setEpisodeCaptchaSolverLink(finalEpisodeCaptchaSolverLink);
                                MolvixDB.updateEpisode(episode);
                                EpisodesManager.enqueEpisodeForDownload(episode);
                            } else {
                                UiUtils.showSafeToast("Sorry, failed to download " + episode.getEpisodeName() + ".Please try again.");
                                episode.setDownloadProgress(-1);
                                MolvixDB.updateEpisode(episode);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
