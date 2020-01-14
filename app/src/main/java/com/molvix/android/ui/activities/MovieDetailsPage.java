package com.molvix.android.ui.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.molvix.android.R;

public class MovieDetailsPage extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);
    }
}
