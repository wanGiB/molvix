package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
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
import com.molvix.android.contracts.DoneCallback;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.eventbuses.LoadEpisodesForSeason;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.managers.SeasonsManager;
import com.molvix.android.models.Season;
import com.molvix.android.models.Season_;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.UiUtils;

import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.objectbox.reactive.DataSubscription;

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
    private DataSubscription seasonSubscription;
    private ProgressDialog progressDialog;

    private CallableSeasonEpisodesExtractionTask callableSeasonEpisodesExtractionTask;

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
        addSeasonChangeListener(season);
    }

    private void addSeasonChangeListener(Season season) {
        seasonSubscription = MolvixDB.getSeasonBox()
                .query()
                .equal(Season_.seasonId, season.getSeasonId())
                .build()
                .subscribe()
                .observer(data -> {
                    if (!data.isEmpty()) {
                        Season updatedSeason = data.get(0);
                        if (updatedSeason != null && updatedSeason.equals(season)) {
                            bindUpdatedSeason(updatedSeason);
                        }
                    }
                });
    }

    public void removeSeasonChangeListener() {
        if (seasonSubscription != null && !seasonSubscription.isCanceled()) {
            seasonSubscription.cancel();
            seasonSubscription = null;
        }
    }

    private void bindUpdatedSeason(Season newSeason) {
        if (newSeason.getEpisodes() != null && !newSeason.getEpisodes().isEmpty()) {
            setNewSeason(newSeason);
            seasonNameView.setText(newSeason.getSeasonName());
        }
    }

    private void setNewSeason(Season newSeason) {
        this.season = newSeason;
    }

    private void initEventHandlers(Season season) {
        rootView.setOnClickListener(v -> {
            UiUtils.blinkView(rootView);
            if (season.getEpisodes() != null && !season.getEpisodes().isEmpty()) {
                EventBus.getDefault().post(new LoadEpisodesForSeason(season));
            } else {
                if (ConnectivityUtils.isDeviceConnectedToTheInternet()) {
                    SeasonsManager.setSeasonRefreshable(season.getSeasonId());
                    showProgressDialog();
                    callableSeasonEpisodesExtractionTask = new CallableSeasonEpisodesExtractionTask((result, e) -> UiUtils.runOnMain(() -> {
                        dismissProgressDialog();
                        if (e == null && result != null) {
                            EventBus.getDefault().post(new LoadEpisodesForSeason(result));
                        } else {
                            UiUtils.showSafeToast("Sorry,an error occurred while fetching episodes for " + season.getSeasonName() + ".Please try again");
                        }
                    }));
                    callableSeasonEpisodesExtractionTask.execute(season.getSeasonLink(), season.getSeasonId());
                } else {
                    UiUtils.showSafeToast("Please connect to the internet and try again.");
                }
            }
        });
        OnClickListener onClickListener = v -> rootView.performClick();
        seasonNameView.setOnClickListener(onClickListener);
        arrow.setOnClickListener(onClickListener);
        subRootView.setOnClickListener(onClickListener);
    }

    private void showProgressDialog() {
        progressDialog = ProgressDialog.show(getContext(), "Fetching " + season.getSeasonName() + " Episodes", "Please wait...", true, true);
        progressDialog.setOnCancelListener(dialog -> {
            if (callableSeasonEpisodesExtractionTask != null && !callableSeasonEpisodesExtractionTask.isCancelled()) {
                callableSeasonEpisodesExtractionTask.cancel(true);
                callableSeasonEpisodesExtractionTask = null;
            }
        });
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog.cancel();
            progressDialog = null;
        }
    }

    private void loadSeasonEpisodes() {
        if (SeasonsManager.canRefreshSeason(season.getSeasonId())) {
            SeasonEpisodesExtractionTask seasonEpisodesExtractionTask = new SeasonEpisodesExtractionTask(season.getSeasonLink(), season.getSeasonId());
            seasonEpisodesExtractionTask.execute();
        }
    }

    static class CallableSeasonEpisodesExtractionTask extends AsyncTask<String, Void, Void> {

        private DoneCallback<Season> seasonDoneCallback;

        CallableSeasonEpisodesExtractionTask(DoneCallback<Season> seasonDoneCallback) {
            this.seasonDoneCallback = seasonDoneCallback;
        }

        @Override
        protected Void doInBackground(String... input) {
            String seasonLink = input[0];
            String seasonId = input[1];
            ContentManager.extractMetaDataFromMovieSeasonLink(seasonLink, seasonId, (result, e) -> seasonDoneCallback.done(result, e));
            return null;
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
        addSeasonChangeListener(season);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeSeasonChangeListener();
    }

}
