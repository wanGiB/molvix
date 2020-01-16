package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
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
import com.molvix.android.eventbuses.UpdateSeason;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.managers.SeasonsManager;
import com.molvix.android.models.Season;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.UiUtils;
import com.raizlabs.android.dbflow.runtime.DirectModelNotifier;
import com.raizlabs.android.dbflow.structure.BaseModel;

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
    private DirectModelNotifier.ModelChangedListener<Season> seasonModelChangedListener;

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
        registerSeasonModelChangeListener();
        View.OnClickListener onClickListener = v -> {
            UiUtils.blinkView(rootView);
            if (season.getEpisodes() != null && !season.getEpisodes().isEmpty()) {
                EventBus.getDefault().post(new LoadEpisodesForSeason(season));
            } else {
                if (ConnectivityUtils.isDeviceConnectedToTheInternet()) {
                    UiUtils.showSafeToast("Please wait...");
                    SeasonsManager.prepareSeason(season.getSeasonId(), true);
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

    private void registerSeasonModelChangeListener() {
        seasonModelChangedListener = new DirectModelNotifier.ModelChangedListener<Season>() {
            @Override
            public void onModelChanged(@NonNull Season model, @NonNull BaseModel.Action action) {
                if (action == BaseModel.Action.UPDATE) {
                    if (season.getSeasonId().equals(model.getSeasonId())) {
                        season.setEpisodes(model.getEpisodes());
                        EventBus.getDefault().post(new UpdateSeason(season));
                        if (SeasonsManager.wasSeasonUnderPreparation(season.getSeasonId())) {
                            SeasonsManager.prepareSeason(season.getSeasonId(), false);
                            EventBus.getDefault().post(new LoadEpisodesForSeason(season));
                        }
                    }
                }
            }

            @Override
            public void onTableChanged(@Nullable Class<?> tableChanged, @NonNull BaseModel.Action action) {

            }
        };
        DirectModelNotifier.get().registerForModelChanges(Season.class, seasonModelChangedListener);
    }

    private void unRegisterSeasonModelChangeListener() {
        DirectModelNotifier.get().unregisterForModelChanges(Season.class, seasonModelChangedListener);
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
        registerSeasonModelChangeListener();
        loadSeasonEpisodes();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unRegisterSeasonModelChangeListener();
    }

}
