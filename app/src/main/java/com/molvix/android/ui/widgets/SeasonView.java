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
import com.molvix.android.managers.ContentManager;
import com.molvix.android.managers.SeasonsManager;
import com.molvix.android.models.Season;
import com.molvix.android.observers.MolvixContentChangeObserver;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.UiUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SeasonView extends FrameLayout {

    @BindView(R.id.list_item_season_name)
    MolvixTextView seasonNameView;

    @BindView(R.id.list_item_season_arrow)
    ImageView arrow;

    @BindView(R.id.root_view)
    View rootView;

    @BindView(R.id.sub_root_view)
    View subRootView;

    private Season season;
    private AtomicBoolean pendingEpisodesLoadOperation = new AtomicBoolean();

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
        registerModelChangeListener(season);
    }

    private void registerModelChangeListener(Season season) {
        MolvixContentChangeObserver.addSeasonChangeListener(season, newSeason -> {
            if (newSeason.getEpisodes() != null && !newSeason.getEpisodes().isEmpty()) {
                setNewSeason(newSeason);
                seasonNameView.setText(newSeason.getSeasonName());
                if (pendingEpisodesLoadOperation.get()) {
                    EventBus.getDefault().post(new LoadEpisodesForSeason(newSeason.getSeasonId()));
                    pendingEpisodesLoadOperation.set(false);
                }
            }
        });
    }

    private void setNewSeason(Season newSeason) {
        this.season = newSeason;
    }

    private void unRegisterModelChangeListener() {
        MolvixContentChangeObserver.removeSeasonChangeListener(season);
    }

    private void initEventHandlers(Season season) {
        OnClickListener onClickListener = v -> {
            UiUtils.blinkView(rootView);
            if (season.getEpisodes() != null && !season.getEpisodes().isEmpty()) {
                EventBus.getDefault().post(new LoadEpisodesForSeason(season.getSeasonId()));
            } else {
                if (ConnectivityUtils.isDeviceConnectedToTheInternet()) {
                    SeasonsManager.setSeasonRefreshable(season.getSeasonId());
                    UiUtils.showSafeToast("Please wait...");
                    pendingEpisodesLoadOperation.set(true);
                    loadSeasonEpisodes();
                } else {
                    UiUtils.showSafeToast("Please connect to the internet and try again.");
                }
            }
        };
        rootView.setOnClickListener(onClickListener);
        seasonNameView.setOnClickListener(onClickListener);
        arrow.setOnClickListener(onClickListener);
        subRootView.setOnClickListener(onClickListener);
    }

    private void loadSeasonEpisodes() {
        if (SeasonsManager.canRefreshSeason(season.getSeasonId())) {
            SeasonEpisodesExtractionTask seasonEpisodesExtractionTask = new SeasonEpisodesExtractionTask(season.getSeasonLink(), season.getSeasonId());
            seasonEpisodesExtractionTask.execute();
        }
    }

    static class SeasonEpisodesExtractionTask extends AsyncTask<Void, Void, Void> {

        private String seasonId, seasonLink;

        SeasonEpisodesExtractionTask(String seasonLink, String seasonId) {
            this.seasonId = seasonId;
            this.seasonLink = seasonLink;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ContentManager.extractMetaDataFromMovieSeasonLink(seasonLink, seasonId);
            return null;
        }

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        loadSeasonEpisodes();
        registerModelChangeListener(season);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unRegisterModelChangeListener();
    }

}
