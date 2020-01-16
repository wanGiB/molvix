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
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.enums.EpisodeQuality;
import com.molvix.android.managers.EpisodesManager;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.utils.LocalDbUtils;
import com.molvix.android.utils.UiUtils;
import com.raizlabs.android.dbflow.runtime.DirectModelNotifier;
import com.raizlabs.android.dbflow.structure.BaseModel;

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
    TextView downloadButton;

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
        season = LocalDbUtils.getSeason(episode.getSeasonId());
        movie = LocalDbUtils.getMovie(episode.getMovieId());
        String episodeName = episode.getEpisodeName();
        if (StringUtils.isNotEmpty(episodeName)) {
            episodeNameView.setText(episodeName);
        }
        initSpinner(episode);
        checkToSeeIfEpisodeAlreadyDownloaded(episode, episodeName);
        initDownloadEventListener(episode, episodeName);
        checkEpisodeActiveDownloadStatus(episode);
    }

    private void initDownloadEventListener(Episode episode, String episodeName) {
        downloadButton.setOnClickListener(v -> {
            UiUtils.blinkView(v);
            String text = downloadButton.getText().toString().trim();
            if (text.equals(getContext().getString(R.string.play))) {
                String downloadUrl;
                EpisodeQuality episodeQuality = episode.getEpisodeQuality();
                if (episodeQuality == EpisodeQuality.STANDARD_QUALITY) {
                    downloadUrl = episode.getStandardQualityDownloadLink();
                } else if (episodeQuality == EpisodeQuality.HIGH_QUALITY) {
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
                    episode.setDownloadProgress(0);
                    episode.update();
                    extractEpisodeDownloadOptions(episode);
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
                downloadButton.setText(getContext().getString(R.string.preparing));
                downloadButton.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                animateDownloadButton();
            } else {
                setToDownloadable();
                downloadButton.setText(getContext().getString(R.string.downloading));
                animateDownloadButton();
            }
        }
    }

    private void animateDownloadButton() {
        mFadeInFadeIn.setDuration(3000);
        mFadeInFadeIn.setRepeatMode(Animation.REVERSE);
        mFadeInFadeIn.setRepeatCount(Animation.INFINITE);
        downloadButton.clearAnimation();
        UiUtils.animateView(downloadButton, mFadeInFadeIn);
    }

    private void checkToSeeIfEpisodeAlreadyDownloaded(Episode episode, String episodeName) {
        EpisodeQuality episodeQuality = episode.getEpisodeQuality();
        if (episodeQuality != null) {
            String downloadUrl;
            if (episodeQuality == EpisodeQuality.STANDARD_QUALITY) {
                downloadUrl = episode.getStandardQualityDownloadLink();
            } else if (episodeQuality == EpisodeQuality.HIGH_QUALITY) {
                downloadUrl = episode.getHighQualityDownloadLink();
            } else {
                downloadUrl = episode.getLowQualityDownloadLink();
            }
            if (downloadUrl != null) {
                String fileExtension = StringUtils.substringAfter(downloadUrl, ".");
                String fileName = episodeName + "." + fileExtension;
                UiUtils.showSafeToast("FileName=" + fileName);
                File existingFile = FileUtils.getFilePath(fileName, WordUtils.capitalize(movie.getMovieName()), season.getSeasonName());
                if (existingFile.exists()) {
                    downloadButton.setText(getContext().getString(R.string.play));
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
        episodeDownloadOptionsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    episode.setEpisodeQuality(EpisodeQuality.HIGH_QUALITY);
                } else if (position == 1) {
                    episode.setEpisodeQuality(EpisodeQuality.STANDARD_QUALITY);
                } else {
                    episode.setEpisodeQuality(EpisodeQuality.LOW_QUALITY);
                }
                episode.update();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

        });

    }

    private void checkAndSelectEpisodeQuality(Episode episode) {
        EpisodeQuality existingEpisodeQuality = episode.getEpisodeQuality();
        if (existingEpisodeQuality != null) {
            if (existingEpisodeQuality == EpisodeQuality.STANDARD_QUALITY) {
                episodeDownloadOptionsSpinner.setSelection(1);
            } else if (existingEpisodeQuality == EpisodeQuality.HIGH_QUALITY) {
                episodeDownloadOptionsSpinner.setSelection(0);
            } else {
                episodeDownloadOptionsSpinner.setSelection(2);
            }
        } else {
            episodeDownloadOptionsSpinner.setSelection(1);
        }
    }

    private void setToDownloadable() {
        VectorDrawableCompat downloadIcon = VectorDrawableCompat.create(getResources(), R.drawable.ic_file_download_white_24dp, null);
        downloadButton.setText(getContext().getString(R.string.download));
        downloadButton.setCompoundDrawablesWithIntrinsicBounds(downloadIcon, null, null, null);
    }

    private void setToPlayable() {
        VectorDrawableCompat playIcon = VectorDrawableCompat.create(getResources(), R.drawable.ic_play_arrow_blue_24dp, null);
        downloadButton.setCompoundDrawablesWithIntrinsicBounds(playIcon, null, null, null);
    }

    private void extractEpisodeDownloadOptions(Episode episode) {
        new EpisodeDownloadOptionsExtractionTask(episode).execute();
    }

    static class EpisodeDownloadOptionsExtractionTask extends AsyncTask<Void, Void, Void> {

        private Episode episode;

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
                                if (episode.getEpisodeQuality() == EpisodeQuality.HIGH_QUALITY || episode.getEpisodeQuality() == EpisodeQuality.STANDARD_QUALITY) {
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
                                if (episode.getEpisodeQuality() == EpisodeQuality.HIGH_QUALITY) {
                                    episodeCaptchaSolverLink = highest;
                                } else if (episode.getEpisodeQuality() == EpisodeQuality.STANDARD_QUALITY) {
                                    episodeCaptchaSolverLink = standard;
                                } else {
                                    episodeCaptchaSolverLink = lowest;
                                }
                            } catch (Exception ignored) {

                            }
                        }
                        if (episodeCaptchaSolverLink != null) {
                            episode.setEpisodeCaptchaSolverLink(episodeCaptchaSolverLink);
                            episode.update();
                            EpisodesManager.enqueEpisodeForDownload(episode);
                        } else {
                            UiUtils.showSafeToast("Sorry, failed to download " + episode.getEpisodeName() + ".Please try again.");
                            episode.setDownloadProgress(-1);
                            episode.update();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
