package com.molvix.android.converters;

import com.google.gson.Gson;
import com.molvix.android.enums.EpisodeQuality;
import com.molvix.android.utils.GsonUtils;
import com.raizlabs.android.dbflow.converter.TypeConverter;

public class EpisodeQualityTypeConverter extends TypeConverter<String, EpisodeQuality> {

    @Override
    public String getDBValue(EpisodeQuality model) {
        Gson gson = GsonUtils.getGSON();
        return gson.toJson(model, EpisodeQuality.class);
    }

    @Override
    public EpisodeQuality getModelValue(String data) {
        Gson gson = GsonUtils.getGSON();
        return gson.fromJson(data, EpisodeQuality.class);
    }

}
