package com.molvix.android.jobs;

import com.molvix.android.preferences.AppPrefs;

import java.util.List;

public class EpisodeDownloadOptionsResolutionManager {

    public static void captureOptions(List<String> downloadLinks, String episodeLink) {
        AppPrefs.persistDownloadOptionsForEpisodeLink(downloadLinks, episodeLink);
    }

    public static List<String> getDownloadOptions(String episodeLink) {
        return AppPrefs.getDownloadOptionsForEpisodeLink(episodeLink);
    }

    public static void captureTargetLink(String targetLink, String episodeLink) {
        AppPrefs.persistDownloadableEpisodeTargetLink(targetLink, episodeLink);
    }

    public static String getTargetLinkForEpisodeLink(String episodeLink) {
        return AppPrefs.getTargetLinkOfEpisode(episodeLink);
    }

}
