package com.molvix.android.managers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.molvix.android.preferences.AppPrefs;

import java.util.ArrayList;
import java.util.List;

public class GenreManager {

    public static List<String> fetchAvailableGenres() {
        List<String>genresList =  new ArrayList<>();
        String genresString = AppPrefs.getGenresString();
        if (genresString!=null){
            genresList=new Gson().fromJson(genresString,new TypeToken<List<String>>(){}.getType());
        }
        return genresList;
    }

    public static void persistGenres(List<String> genresList) {
        if (!genresList.isEmpty()) {
            String genresString = new Gson().toJson(genresList, new TypeToken<List<String>>() {
            }.getType());
            AppPrefs.persistGenresString(genresString);
        }
    }
}
