package com.molvix.android.utils;

import com.molvix.android.models.Episode;
import com.molvix.android.models.Episode_Table;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Movie_Table;
import com.molvix.android.models.Season;
import com.molvix.android.models.Season_Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;

public class LocalDbUtils {
    public static Movie getMovie(String movieId) {
        return SQLite
                .select()
                .from(Movie.class)
                .where(Movie_Table.movieId.eq(movieId))
                .querySingle();
    }

    public static Season getSeason(String seasonId) {
        return SQLite
                .select()
                .from(Season.class)
                .where(Season_Table.seasonId.eq(seasonId))
                .querySingle();
    }

    public static Episode getEpisode(String episodeId) {
        return SQLite
                .select()
                .from(Episode.class)
                .where(Episode_Table.episodeId.eq(episodeId))
                .querySingle();
    }

}
