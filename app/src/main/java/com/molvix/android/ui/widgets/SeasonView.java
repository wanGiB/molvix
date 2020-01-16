package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.molvix.android.R;
import com.molvix.android.eventbuses.LoadEpisodesForSeason;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.managers.SeasonsManager;
import com.molvix.android.models.Season;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.LocalDbUtils;
import com.molvix.android.utils.UiUtils;

import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SeasonView extends FrameLayout {

    @BindView(R.id.list_item_season_name)
    MolvixTextView seasonNameView;

    @BindView(R.id.list_item_season_arrow)
    ImageView arrow;

    @BindView(R.id.root_view)
    View rootView;

    private Season season;
    private SeasonEpisodesExtractionTask seasonEpisodesExtractionTask;

    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;

    public SeasonView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public SeasonView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SeasonView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        @SuppressLint("InflateParams") View seasonView = LayoutInflater.from(context).inflate(R.layout.season_view, null);
        ButterKnife.bind(this, seasonView);
        removeAllViews();
        addView(seasonView);
        requestLayout();
    }

    public void bindSeason(Season season) {
        this.season = season;
        seasonNameView.setText(season.getSeasonName());
        loadSeasonEpisodes();
        initEventHandlers(season);
        registerSharedPreferenceChangeListener(season);
    }

    private void registerSharedPreferenceChangeListener(Season season) {
        unRegisterSharedPreferenceChangeListener();
        onSharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals(season.getSeasonId())) {
                Season newSeason = LocalDbUtils.getSeason(key);
                if (newSeason != null && newSeason.getEpisodes() != null && !newSeason.getEpisodes().isEmpty()) {
                    invalidateSeason(newSeason);
                }
            }
        };
        AppPrefs.getAppPreferences().registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    private void unRegisterSharedPreferenceChangeListener() {
        if (onSharedPreferenceChangeListener != null) {
            AppPrefs.getAppPreferences().unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
            onSharedPreferenceChangeListener = null;
        }
    }

    private void invalidateSeason(Season changedSeason) {
        bindSeason(changedSeason);
        if (SeasonsManager.wasSeasonEpisodesUnderPreparation(changedSeason.getSeasonId())) {
            SeasonsManager.prepareSeasonEpisodes(changedSeason.getSeasonId(),false);
            EventBus.getDefault().post(new LoadEpisodesForSeason(changedSeason));
        }
    }

    private void initEventHandlers(Season season) {
        OnClickListener onClickListener = v -> {
            UiUtils.blinkView(rootView);
            if (season.getEpisodes() != null && !season.getEpisodes().isEmpty()) {
                EventBus.getDefault().post(new LoadEpisodesForSeason(season));
            } else {
                if (ConnectivityUtils.isDeviceConnectedToTheInternet()) {
                    UiUtils.showSafeToast("Please wait...");
                    SeasonsManager.prepareSeasonEpisodes(season.getSeasonId(), true);
                    loadSeasonEpisodes();
                } else {
                    UiUtils.showSafeToast("Please connect to the internet and try again.");
                }
            }
        };
        rootView.setOnClickListener(onClickListener);
        seasonNameView.setOnClickListener(onClickListener);
        arrow.setOnClickListener(onClickListener);
    }

    private void loadSeasonEpisodes() {
        if (seasonEpisodesExtractionTask != null) {
            seasonEpisodesExtractionTask.cancel(true);
            seasonEpisodesExtractionTask = null;
        }
        seasonEpisodesExtractionTask = new SeasonEpisodesExtractionTask(season);
        seasonEpisodesExtractionTask.execute();
    }

    static class SeasonEpisodesExtractionTask extends AsyncTask<Void, Void, Void> {

        private Season season;

        SeasonEpisodesExtractionTask(Season season) {
            this.season = season;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ContentManager.extractMetaDataFromMovieSeasonLink(season);
            return null;
        }

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        loadSeasonEpisodes();
        registerSharedPreferenceChangeListener(season);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unRegisterSharedPreferenceChangeListener();
    }

}
