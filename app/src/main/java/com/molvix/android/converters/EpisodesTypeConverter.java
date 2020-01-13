package com.molvix.android.converters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.molvix.android.models.Episode;
import com.molvix.android.utils.GsonUtils;
import com.raizlabs.android.dbflow.converter.TypeConverter;

import java.lang.reflect.Type;
import java.util.List;

public class EpisodesTypeConverter extends TypeConverter<String, List<Episode>> {

    @Override
    public String getDBValue(List<Episode> model) {
        Gson gson = GsonUtils.getGSON();
        Type episodeType = new TypeToken<List<Episode>>() {
        }.getType();
        return gson.toJson(model, episodeType);
    }

    @Override
    public List<Episode> getModelValue(String data) {
        Gson gson = GsonUtils.getGSON();
        Type episodesType = new TypeToken<List<Episode>>() {
        }.getType();
        return gson.fromJson(data, episodesType);
    }

}
