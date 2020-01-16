package com.molvix.android.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.molvix.android.R;
import com.molvix.android.models.Episode;
import com.molvix.android.ui.widgets.EpisodeView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class EpisodesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Episode> episodes;
    private Context context;

    public EpisodesAdapter(Context context, List<Episode> episodes) {
        this.context = context;
        this.episodes = episodes;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.recycler_view_item_episode_view, parent, false);
        return new EpisodesViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        EpisodesViewHolder episodesViewHolder = (EpisodesViewHolder) holder;
        episodesViewHolder.bindEpisode(episodes.get(position));
    }

    @Override
    public int getItemCount() {
        return episodes.size();
    }

    static class EpisodesViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.episode_view)
        EpisodeView episodeView;

        EpisodesViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bindEpisode(Episode episode) {
            episodeView.bindEpisode(episode);
        }

    }

}
