package com.molvix.android.managers;

import com.molvix.android.eventbuses.CheckForDownloadableEpisodes;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.utils.MolvixLogger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.greenrobot.eventbus.EventBus;

public class EpisodesManager {

    public static void enqueDownloadableEpisode(Episode episode) {
        DownloadableEpisode existingDownloadableEpisode = MolvixDB.getDownloadableEpisode(episode.getEpisodeId());
        if (existingDownloadableEpisode != null) {
            MolvixLogger.d(ContentManager.class.getSimpleName(), "An already existing downloadable was found for " + getEpisodeFullName(episode) + ".Nothing to do further");
            EventBus.getDefault().post(new CheckForDownloadableEpisodes());
            return;
        }
        DownloadableEpisode newDownloadableEpisode = new DownloadableEpisode();
        newDownloadableEpisode.setDownloadableEpisodeId(episode.getEpisodeId());
        newDownloadableEpisode.episode.setTarget(episode);
        MolvixDB.createNewDownloadableEpisode(newDownloadableEpisode);
        MolvixLogger.d(ContentManager.class.getSimpleName(), getEpisodeFullName(episode) + " has being made downloadable");
        EventBus.getDefault().post(new CheckForDownloadableEpisodes());
    }

    public static void popDownloadableEpisode(Episode episode) {
        DownloadableEpisode downloadableEpisode = MolvixDB.getDownloadableEpisode(episode.getEpisodeId());
        if (downloadableEpisode != null) {
            MolvixDB.deleteDownloadableEpisode(downloadableEpisode);
            unLockCaptchaSolver();
        }
        EventBus.getDefault().post(new CheckForDownloadableEpisodes());
    }

    public static void lockCaptchaSolver(String episodeId) {
        AppPrefs.lockCaptchaSolver(episodeId);
    }

    public static void unLockCaptchaSolver() {
        AppPrefs.unLockCaptchaSolver();
    }

    public static boolean isCaptchaSolvable() {
        return AppPrefs.isCaptchaSolvable();
    }

    public static String getEpisodeFullName(Episode episode) {
        Season episodeSeason = episode.getSeason();
        Movie episodeMovie = episodeSeason.getMovie();
        return episode.getEpisodeName() + ", " + episodeSeason.getSeasonName() + " of " + WordUtils.capitalize(episodeMovie.getMovieName());
    }

    public static String getEpisodeAndSeasonDescr(Episode episode) {
        Season episodeSeason = episode.getSeason();
        return episode.getEpisodeName() + " of " + episodeSeason.getSeasonName();
    }

    public static String getEpisodeAbbrev(Episode episode) {
        Season season = episode.getSeason();
        Movie movie = season.getMovie();
        String seasonName = season.getSeasonName();
        String episodeName = episode.getEpisodeName();
        String abbrevedSeasonName = seasonName.charAt(0) + "-" + StringUtils.substringAfterLast(seasonName, "-");
        String abbrevedEpisodeName = episodeName.charAt(0) + "-" + StringUtils.substringAfterLast(episodeName, "-");
        return abbrevedEpisodeName + "/" + abbrevedSeasonName + " of " + WordUtils.capitalize(movie.getMovieName());
    }

}
