package com.molvix.android.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.molvix.android.companions.AppConstants;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.managers.FileDownloadManager;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;
import com.molvix.android.utils.FileUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

public class EmptyContentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String episodeId = intent.getStringExtra(AppConstants.EPISODE_ID);
        if (episodeId != null) {
            DownloadableEpisode downloadableEpisode = MolvixDB.getDownloadableEpisode(episodeId);
            if (downloadableEpisode != null) {
                MolvixDB.deleteDownloadableEpisode(downloadableEpisode);
            }
            Episode episode = MolvixDB.getEpisode(episodeId);
            if (episode != null) {
                episode.setDownloadProgress(-1);
                episode.setProgressDisplayText("");
                MolvixDB.updateEpisode(episode);
                String movieName = WordUtils.capitalize(episode.getSeason().getMovie().getMovieName());
                String seasonName = WordUtils.capitalize(episode.getSeason().getSeasonName());
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
                int downloadId = Math.abs((dirPath + fileName).hashCode());
                FileDownloadManager.cancelDownload(downloadId);
            }
        }
        finish();
    }

}
