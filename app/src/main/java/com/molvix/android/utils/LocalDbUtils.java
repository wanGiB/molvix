package com.molvix.android.utils;

import com.molvix.android.database.MolvixDB;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Episode_Table;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Movie_Table;
import com.molvix.android.models.Season;
import com.molvix.android.models.Season_Table;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.database.transaction.ProcessModelTransaction;
import com.raizlabs.android.dbflow.structure.database.transaction.Transaction;

import java.util.List;


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

    public static void performBulkInsertionOfMovies(List<Movie> movies) {
        ProcessModelTransaction<Movie> processModelTransaction =
                new ProcessModelTransaction.Builder<>((ProcessModelTransaction.ProcessModel<Movie>) (movie, wrapper) -> {
                    try {
                        movie.save();
                    } catch (IllegalStateException ignored) {

                    }
                }).addAll(movies).build();
        Transaction transaction = FlowManager.getDatabase(MolvixDB.class).beginTransactionAsync(processModelTransaction).build();
        transaction.execute();
    }

}
