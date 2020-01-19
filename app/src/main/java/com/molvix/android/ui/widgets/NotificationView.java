package com.molvix.android.ui.widgets;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Notification;
import com.molvix.android.models.Season;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.io.File;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;

public class NotificationView extends FrameLayout {

    @BindView(R.id.notification_description_view)
    MolvixTextView notificationDescriptionView;

    @BindView(R.id.notification_time_view)
    MolvixTextView notificationTimeView;

    @BindView(R.id.notification_icon_view)
    ImageView notificationIconView;

    @BindView(R.id.notification_root_view)
    ImageView notificationRootView;

    private Realm realm;

    public NotificationView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public NotificationView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NotificationView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        View notificationView = LayoutInflater.from(context).inflate(R.layout.notificaton_view, null);
        ButterKnife.bind(this, notificationView);
        removeAllViews();
        addView(notificationView);
        requestLayout();
    }

    @SuppressWarnings("ConstantConditions")
    public void bindNotification(Notification notification) {
        realm = Realm.getDefaultInstance();
        notificationDescriptionView.setText(notification.getMessage());
        int notificationDestination = notification.getDestination();
        if (notificationDestination == AppConstants.DESTINATION_EPISODE) {
            VectorDrawableCompat downloadIcon = VectorDrawableCompat.create(getResources(), R.drawable.ic_file_download_white_24dp, null);
            notificationIconView.setImageDrawable(downloadIcon);
        } else {
            VectorDrawableCompat newReleaseIcon = VectorDrawableCompat.create(getResources(), R.drawable.ic_new_releases_black_24dp, null);
            notificationIconView.setImageDrawable(newReleaseIcon);
        }
        notificationTimeView.setText(AppConstants.DATE_FORMATTER_IN_12HRS.format(new Date(notification.getTimeStamp())));
        View.OnClickListener onClickListener = v -> {
            UiUtils.blinkView(notificationRootView);
            if (notificationDestination == AppConstants.DESTINATION_EPISODE) {
                Episode episode = realm.where(Episode.class).equalTo(AppConstants.EPISODE_ID, notification.getResolutionKey()).findFirst();
                Movie movie = realm.where(Movie.class).equalTo(AppConstants.MOVIE_ID, episode.getMovieId()).findFirst();
                Season season = realm.where(Season.class).equalTo(AppConstants.SEASON_ID, episode.getSeasonId()).findFirst();
                if (episode != null) {
                    String episodeName = episode.getEpisodeName();
                    //Open the download movie file
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
                        UiUtils.showSafeToast("Oops! Sorry, an error occurred while attempting to play video.The file must have being deleted or moved to another folder.");
                    }
                }
            } else if (notificationDestination == AppConstants.DESTINATION_NEW_EPISODE_AVAILABLE) {

            } else if (notificationDestination == AppConstants.DESTINATION_NEW_SEASON_AVAILABLE) {

            }
        };
        notificationRootView.setOnClickListener(onClickListener);
        notificationIconView.setOnClickListener(onClickListener);
        notificationDescriptionView.setOnClickListener(onClickListener);
        notificationTimeView.setOnClickListener(onClickListener);
    }

}
