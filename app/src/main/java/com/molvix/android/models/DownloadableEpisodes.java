package com.molvix.android.models;

import androidx.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.molvix.android.converters.EpisodesTypeConverter;
import com.molvix.android.database.MolvixDB;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ConflictAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings({"WeakerAccess", "NullableProblems"})
@Table(database = MolvixDB.class,
        primaryKeyConflict = ConflictAction.REPLACE,
        insertConflict = ConflictAction.REPLACE,
        updateConflict = ConflictAction.REPLACE)
public class DownloadableEpisodes extends BaseModel implements Serializable {

    @PrimaryKey
    @Column
    @Expose
    public String episodesId;

    @Column(typeConverter = EpisodesTypeConverter.class)
    @Nullable
    public List<Episode> downloadableEpisodes;

    public void setEpisodesId(String episodesId) {
        this.episodesId = episodesId;
    }

    public String getEpisodesId() {
        return episodesId;
    }

    public void setDownloadableEpisodes(@Nullable List<Episode> downloadableEpisodes) {
        this.downloadableEpisodes = downloadableEpisodes;
    }

    @Nullable
    public List<Episode> getDownloadableEpisodes() {
        return downloadableEpisodes;
    }
}
