package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.eventbuses.UpdateNotification;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Notification;
import com.molvix.android.models.Season;
import com.molvix.android.ui.activities.MainActivity;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.text.WordUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NotificationView extends FrameLayout {

    @BindView(R.id.notification_description_view)
    MolvixTextView notificationDescriptionView;

    @BindView(R.id.notification_time_view)
    MolvixTextView notificationTimeView;

    @BindView(R.id.notification_icon_view)
    ImageView notificationIconView;

    @BindView(R.id.notification_root_view)
    LinearLayout notificationRootView;

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
        removeAllViews();
        @SuppressLint("InflateParams") View notificationView = LayoutInflater.from(context).inflate(R.layout.notificaton_view, null);
        ButterKnife.bind(this, notificationView);
        addView(notificationView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @SuppressWarnings("ConstantConditions")
    public void bindNotification(Notification notification) {
        notificationDescriptionView.setText(UiUtils.fromHtml(notification.getMessage()));
        int notificationDestination = notification.getDestination();
        if (notificationDestination == AppConstants.DESTINATION_DOWNLOADED_EPISODE) {
            VectorDrawableCompat downloadIcon = VectorDrawableCompat.create(getResources(), R.drawable.file_download_in_progress, null);
            notificationIconView.setImageDrawable(downloadIcon);
        } else {
            VectorDrawableCompat newReleaseIcon = VectorDrawableCompat.create(getResources(), R.drawable.ic_new_releases, null);
            notificationIconView.setImageDrawable(newReleaseIcon);
        }
        notificationTimeView.setText(AppConstants.DATE_FORMATTER_IN_12HRS.format(new Date(notification.getTimeStamp())));
        View.OnClickListener onClickListener = v -> {
            UiUtils.blinkView(notificationRootView);
            if (notificationDestination == AppConstants.DESTINATION_DOWNLOADED_EPISODE) {
                Episode episode = MolvixDB.getEpisode(notification.getDestinationKey());
                Movie movie = episode.getSeason().getMovie();
                Season season = episode.getSeason();
                if (episode != null) {
                    String episodeName = episode.getEpisodeName();
                    String fileName = episodeName + ".mp4";
                    File downloadedFile = FileUtils.getFilePath(fileName, WordUtils.capitalize(movie.getMovieName()), WordUtils.capitalize(season.getSeasonName()));
                    if (downloadedFile.exists()) {
                        notification.setSeen(true);
                        EventBus.getDefault().post(new UpdateNotification(notification));
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
                    } else {
                        UiUtils.showSafeToast("Oops! Sorry, an error occurred while attempting to play video.The file must have being deleted or moved to another folder.");
                    }
                }
            } else {
                String destinationKey = notification.getDestinationKey();
                if (getContext() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getContext();
                    mainActivity.loadMovieDetails(destinationKey);
                }
            }
        };
        notificationRootView.setOnClickListener(onClickListener);
        notificationIconView.setOnClickListener(onClickListener);
        notificationDescriptionView.setOnClickListener(onClickListener);
        notificationTimeView.setOnClickListener(onClickListener);
    }

}
