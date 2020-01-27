package com.molvix.android.ui.fragments;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.molvix.android.R;
import com.molvix.android.preferences.AppPrefs;
import com.morsebyte.shailesh.twostagerating.TwoStageRate;

public class MoreContentsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    private SwitchPreferenceCompat dailyMovieRecommendationSwitch;
    private SwitchPreferenceCompat downloadedMoviesUpdateSwitch;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.more_content, rootKey);
        dailyMovieRecommendationSwitch = findPreference(getString(R.string.daily_movie_recommendation_key));
        downloadedMoviesUpdateSwitch = findPreference(getString(R.string.downloaded_movies_update_key));
        if (downloadedMoviesUpdateSwitch != null) {
            downloadedMoviesUpdateSwitch.setDefaultValue(AppPrefs.canBeUpdatedOnDownloadedMovies());
            downloadedMoviesUpdateSwitch.setOnPreferenceChangeListener(this);
        }
        if (dailyMovieRecommendationSwitch != null) {
            dailyMovieRecommendationSwitch.setDefaultValue(AppPrefs.canDailyMoviesBeRecommended());
            dailyMovieRecommendationSwitch.setOnPreferenceChangeListener(this);
        }
        Preference feedBackPref = findPreference(getString(R.string.feedback_key));
        if (feedBackPref != null) {
            feedBackPref.setOnPreferenceClickListener(preference -> {
                initAppRater();
                return true;
            });
        }
    }

    private void initAppRater() {
        if (getActivity() != null) {
            TwoStageRate twoStageRate = TwoStageRate.with(getActivity());
            twoStageRate.setShowAppIcon(true);
            twoStageRate.showRatePromptDialog();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals(getString(R.string.daily_movie_recommendation_key))) {
            AppPrefs.setDailyMoviesRecommendability((Boolean) newValue);
            dailyMovieRecommendationSwitch.setDefaultValue(newValue);
        } else if (preference.getKey().equals(getString(R.string.downloaded_movies_update_key))) {
            AppPrefs.setDownloadedMoviesUpdatable((Boolean) newValue);
            downloadedMoviesUpdateSwitch.setDefaultValue(newValue);
        }
        return true;
    }

}
