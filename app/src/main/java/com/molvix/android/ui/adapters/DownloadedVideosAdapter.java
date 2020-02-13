package com.molvix.android.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.molvix.android.R;
import com.molvix.android.beans.DownloadedVideoItem;
import com.molvix.android.ui.widgets.DownloadedVideoItemView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DownloadedVideosAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<DownloadedVideoItem> downloadedFiles;
    private LayoutInflater layoutInflater;

    public DownloadedVideosAdapter(Context context, List<DownloadedVideoItem> downloadedFiles) {
        this.layoutInflater = LayoutInflater.from(context);
        this.downloadedFiles = downloadedFiles;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DownloadedVideoItemHolder(layoutInflater.inflate(R.layout.recycler_item_downloaded_video_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DownloadedVideoItemHolder downloadedVideoItemHolder = (DownloadedVideoItemHolder) holder;
        downloadedVideoItemHolder.bindData(downloadedFiles.get(position),position);
    }

    @Override
    public int getItemCount() {
        return downloadedFiles.size();
    }

    static class DownloadedVideoItemHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.downloaded_video_item)
        DownloadedVideoItemView downloadedVideoItemView;

        DownloadedVideoItemHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bindData(DownloadedVideoItem downloadedVideoItem, int position) {
            downloadedVideoItemView.bindDownloadedVideoItem(downloadedVideoItem,position);
        }

    }

}
