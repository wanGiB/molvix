package com.molvix.android.ui.fragments;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.molvix.android.R;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.utils.UiUtils;
import com.morsebyte.shailesh.twostagerating.TwoStageRate;

public class MoreContentsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.more_content, rootKey);
        SwitchPreferenceCompat dailyMovieRecommendationSwitch = findPreference(getString(R.string.daily_movie_recommendation_key));
        Preference bitcoinDonationsPref = findPreference(getString(R.string.donation_key));
        if (bitcoinDonationsPref != null) {
            bitcoinDonationsPref.setWidgetLayoutResource(R.layout.bitcoin_donation_page);
            bitcoinDonationsPref.setOnPreferenceClickListener(preference -> {
                copyBitcoinAddress();
                UiUtils.showSafeToast("Bitcoin address copied to clip board");
                return true;
            });
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
        Preference thirdPartySoftwareAttrPref = findPreference(getString(R.string.third_party_software_attr));
        if (thirdPartySoftwareAttrPref != null) {
            thirdPartySoftwareAttrPref.setOnPreferenceClickListener(preference -> {
                //Display third party software used
                displayThirdPartySoftwareUsed();
                return true;
            });
        }
    }

    private void displayThirdPartySoftwareUsed() {

    }

    private void initAppRater() {
        if (getActivity() != null) {
            TwoStageRate twoStageRate = TwoStageRate.with(getActivity());
            twoStageRate.setShowAppIcon(true);
            twoStageRate.showRatePromptDialog();
        }
    }

    private void copyBitcoinAddress() {

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals(getString(R.string.daily_movie_recommendation_key))) {
            AppPrefs.setDailyMoviesRecommendability((Boolean) newValue);
        }
        return true;
    }

}
