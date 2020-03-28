package com.molvix.android.ui.fragments;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.molvix.android.R;
import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.eventbuses.DownloadCoinsUpdatedEvent;
import com.molvix.android.managers.ThemeManager;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.ui.activities.SplashActivity;
import com.molvix.android.utils.Gamification;
import com.molvix.android.utils.UiUtils;
import com.morsebyte.shailesh.twostagerating.TwoStageRate;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MoreContentsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    private SwitchPreferenceCompat dailyMovieRecommendationSwitch;
    private SwitchPreferenceCompat downloadedMoviesUpdateSwitch;
    private SwitchPreferenceCompat themePref;
    private Preference downloadCoinsPref;
    private Handler mUIHandler = new Handler();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.more_content, rootKey);
        checkAndRegisterEventBus();
        dailyMovieRecommendationSwitch = findPreference(getString(R.string.daily_movie_recommendation_key));
        downloadedMoviesUpdateSwitch = findPreference(getString(R.string.downloaded_movies_update_key));
        if (downloadedMoviesUpdateSwitch != null) {
            downloadedMoviesUpdateSwitch.setChecked(AppPrefs.canBeUpdatedOnDownloadedMovies());
            downloadedMoviesUpdateSwitch.setOnPreferenceChangeListener(this);
        }
        if (dailyMovieRecommendationSwitch != null) {
            dailyMovieRecommendationSwitch.setChecked(AppPrefs.canDailyMoviesBeRecommended());
            dailyMovieRecommendationSwitch.setOnPreferenceChangeListener(this);
        }
        Preference feedBackPref = findPreference(getString(R.string.feedback_key));
        if (feedBackPref != null) {
            feedBackPref.setOnPreferenceClickListener(preference -> {
                initAppRater();
                return true;
            });
        }
        Preference molvixAppVersionPref = findPreference(getString(R.string.molvix_app_version));
        if (molvixAppVersionPref != null) {
            try {
                PackageManager packageManager = ApplicationLoader.getInstance().getPackageManager();
                if (packageManager != null) {
                    PackageInfo packageInfo = packageManager.getPackageInfo(ApplicationLoader.getInstance().getPackageName(), 0);
                    if (packageInfo != null) {
                        String versionName = packageInfo.versionName;
                        molvixAppVersionPref.setSummary("Version " + versionName);
                        molvixAppVersionPref.setOnPreferenceClickListener(preference -> {
                            if (getContext() != null) {
                                AlertDialog.Builder aboutAppDialogBuilder = new AlertDialog.Builder(getContext());
                                aboutAppDialogBuilder.setTitle("Molvix");
                                aboutAppDialogBuilder.setMessage(UiUtils.fromHtml("You are currently using Version <b>" + versionName + "</b> of Molvix.<br/><br/><b>Molvix</b>, an acronym for <b>Mobile Videos Extension</b>, finds and Downloads all your favorite TV Series and TV Shows for FREE."));
                                aboutAppDialogBuilder.setPositiveButton("OK", (dialogInterface, i) -> {
                                    dialogInterface.dismiss();
                                    dialogInterface.cancel();
                                });
                                aboutAppDialogBuilder.create().show();
                            }
                            return true;
                        });
                    }
                }
            } catch (Exception ignored) {

            }
        }
        themePref = findPreference(getString(R.string.theme_key));
        if (themePref != null) {
            ThemeManager.ThemeSelection themeSelection = ThemeManager.getThemeSelection();
            themePref.setDefaultValue(themeSelection == ThemeManager.ThemeSelection.DARK);
            themePref.setChecked(themeSelection == ThemeManager.ThemeSelection.DARK);
            themePref.setOnPreferenceChangeListener(this);
        }
        downloadCoinsPref = findPreference(getString(R.string.download_coins));
        if (downloadCoinsPref != null) {
            downloadCoinsPref.setSummary(UiUtils.fromHtml("You currently have <b>" + AppPrefs.getAvailableDownloadCoins() + "</b> download coins"));
            downloadCoinsPref.setOnPreferenceChangeListener(this);
            downloadCoinsPref.setOnPreferenceClickListener(preference -> {
                Gamification.displayCoinEssence(getContext(), "Download Coins");
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (downloadCoinsPref != null) {
            downloadCoinsPref.setSummary(UiUtils.fromHtml("You currently have <b>" + AppPrefs.getAvailableDownloadCoins() + "</b> download coins"));
        }
        checkAndRegisterEventBus();
    }

    @Override
    public void onPause() {
        super.onPause();
        checkAndUnRegisterEventBus();
    }

    @Override
    public void onStart() {
        super.onStart();
        checkAndRegisterEventBus();
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
        } else if (preference.getKey().equals(getString(R.string.theme_key))) {
            if (((Boolean) newValue)) {
                ThemeManager.setThemeSelection(ThemeManager.ThemeSelection.DARK);
                themePref.setDefaultValue(true);
            } else {
                ThemeManager.setThemeSelection(ThemeManager.ThemeSelection.LIGHT);
                themePref.setDefaultValue(false);
            }
            UiUtils.snackMessage("Switching theme.Please wait...", getView(), false, null, null);
            restartApp();
        }
        return true;
    }

    private void restartApp() {
        new Handler().postDelayed(() -> {
            Intent splashIntent = new Intent(getContext(), SplashActivity.class);
            startActivity(splashIntent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        }, 1000);
    }

    private void checkAndRegisterEventBus() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    private void checkAndUnRegisterEventBus() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEvent(Object event) {
        mUIHandler.post(() -> {
            if (event instanceof DownloadCoinsUpdatedEvent) {
                DownloadCoinsUpdatedEvent downloadCoinsUpdatedEvent = (DownloadCoinsUpdatedEvent) event;
                if (downloadCoinsPref != null) {
                    downloadCoinsPref.setSummary(UiUtils.fromHtml("You currently have <b>" + downloadCoinsUpdatedEvent.getCoins() + "</b> download coins"));
                }
            }
        });
    }

}