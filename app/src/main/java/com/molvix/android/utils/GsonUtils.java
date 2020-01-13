package com.molvix.android.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonUtils {
    public static Gson getGSON(){
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setExclusionStrategies(new DBFlowExclusionStrategy());
        return gsonBuilder.create();
    }
}
