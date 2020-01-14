package com.molvix.android.ui.adapters;

import android.view.ViewGroup;

import com.molvix.android.beans.MovieContentItem;
import com.thoughtbot.expandablerecyclerview.MultiTypeExpandableRecyclerViewAdapter;
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;
import com.thoughtbot.expandablerecyclerview.viewholders.ChildViewHolder;
import com.thoughtbot.expandablerecyclerview.viewholders.GroupViewHolder;

import java.util.List;

public class SeasonsWithEpisodesAdapter extends MultiTypeExpandableRecyclerViewAdapter<GroupViewHolder, ChildViewHolder> {

    private List<MovieContentItem> contentItems;

    public SeasonsWithEpisodesAdapter(List<MovieContentItem> contentItems) {
        super(contentItems);
        this.contentItems = contentItems;
    }

    @Override
    public GroupViewHolder onCreateGroupViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public ChildViewHolder onCreateChildViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindChildViewHolder(ChildViewHolder holder, int flatPosition, ExpandableGroup group, int childIndex) {

    }

    @Override
    public void onBindGroupViewHolder(GroupViewHolder holder, int flatPosition, ExpandableGroup group) {

    }

    @Override
    public int getGroupViewType(int position, ExpandableGroup group) {
        return super.getGroupViewType(position, group);
    }

    @Override
    public int getChildViewType(int position, ExpandableGroup group, int childIndex) {
        return super.getChildViewType(position, group, childIndex);
    }

}