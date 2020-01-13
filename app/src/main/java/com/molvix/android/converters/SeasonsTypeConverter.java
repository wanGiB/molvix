package com.molvix.android.converters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Season;
import com.molvix.android.utils.GsonUtils;
import com.raizlabs.android.dbflow.converter.TypeConverter;

import java.lang.reflect.Type;
import java.util.List;

public class SeasonsTypeConverter extends TypeConverter<String, List<Season>> {

    @Override
    public String getDBValue(List<Season> model) {
        Gson gson = GsonUtils.getGSON();
        Type seasonType = new TypeToken<List<Season>>() {
        }.getType();
        return gson.toJson(model, seasonType);
    }

    @Override
    public List<Season> getModelValue(String data) {
        Gson gson = GsonUtils.getGSON();
        Type seasonsType = new TypeToken<List<Season>>() {
        }.getType();
        return gson.fromJson(data, seasonsType);
    }
}

