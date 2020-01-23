package com.molvix.android.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.molvix.android.R;
import com.molvix.android.models.Season;
import com.molvix.android.ui.widgets.SeasonView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SeasonsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<Season> seasons;

    public SeasonsAdapter(Context context, List<Season> seasons) {
        this.context = context;
        this.seasons = seasons;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SeasonHeaderGroupViewHolder(LayoutInflater.from(context).inflate(R.layout.recycler_view_item_season, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SeasonHeaderGroupViewHolder seasonHeaderGroupViewHolder = (SeasonHeaderGroupViewHolder) holder;
        seasonHeaderGroupViewHolder.bindSeasonData(seasons.get(position));
    }

    @Override
    public int getItemCount() {
        return seasons.size();
    }

    static class SeasonHeaderGroupViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.season_view)
        SeasonView seasonView;

        SeasonHeaderGroupViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bindSeasonData(Season season) {
            seasonView.bindSeason(season);
        }
    }

}