package com.molvix.android.ui.widgets;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.molvix.android.R;
import com.molvix.android.enums.EpisodeQuality;
import com.molvix.android.eventbuses.DownloadEpisodeEvent;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.utils.LocalDbUtils;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;

public class EpisodeView extends FrameLayout {

    @BindView(R.id.episode_name_view)
    TextView episodeNameView;

    @BindView(R.id.episode_download_options_spinner_view)
    Spinner episodeDownloadOptionsSpinner;

    @BindView(R.id.episode_download_button_view)
    TextView downloadButton;

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
        View rootView = LayoutInflater.from(context).inflate(R.layout.episode_view, null);
        ButterKnife.bind(this, rootView);
        removeAllViews();
        addView(rootView);
        requestLayout();
    }

    void bindEpisode(Episode episode) {
        season = LocalDbUtils.getSeason(episode.getSeasonId());
        movie = LocalDbUtils.getMovie(episode.getMovieId());
        String episodeName = episode.getEpisodeName();
        if (StringUtils.isNotEmpty(episodeName)) {
            episodeNameView.setText(episodeName);
        }
        initSpinner(episode);
        checkToSeeIfEpisodeAlreadyDownloaded(episode, episodeName);
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
                File existingFile = FileUtils.getFilePath(fileName, WordUtils.capitalize(movie.getMovieName()), season.getSeasonName());
                if (existingFile.exists()) {
                    Intent videoIntent = new Intent(Intent.ACTION_VIEW);
                    videoIntent.setDataAndType(Uri.fromFile(existingFile), "video/*");
                    getContext().startActivity(videoIntent);
                } else {
                    UiUtils.showSafeToast("Oops! Sorry, an error occurred while attempting to play video.");
                }
            } else {
                EventBus.getDefault().post(new DownloadEpisodeEvent(episode));
            }
        });
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
            String fileExtension = StringUtils.substringAfter(downloadUrl, ".");
            String fileName = episodeName + "." + fileExtension;
            File existingFile = FileUtils.getFilePath(fileName, WordUtils.capitalize(movie.getMovieName()), season.getSeasonName());
            if (existingFile.exists()) {
                downloadButton.setText(getContext().getString(R.string.play));
            } else {
                setToDownloadable();
            }
        } else {
            setToDownloadable();
        }
    }

    private void initSpinner(Episode episode) {
        episodeDownloadOptionsSpinner.setSelection(1);
        episodeDownloadOptionsSpinner.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {
                episode.setEpisodeQuality(EpisodeQuality.HIGH_QUALITY);
            } else if (position == 1) {
                episode.setEpisodeQuality(EpisodeQuality.STANDARD_QUALITY);
            } else {
                episode.setEpisodeQuality(EpisodeQuality.LOW_QUALITY);
            }
            episode.update();
        });
    }

    private void setToDownloadable() {
        VectorDrawableCompat downloadIcon = VectorDrawableCompat.create(getResources(), R.drawable.ic_file_download_white_24dp, null);
        downloadButton.setText(getContext().getString(R.string.download));
        downloadButton.setCompoundDrawablesWithIntrinsicBounds(downloadIcon, null, null, null);
    }

}
