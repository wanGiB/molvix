package com.molvix.android.ui.rendering;


import androidx.recyclerview.widget.RecyclerView;

/**
 * Interface for getting the orientation of a RecyclerView from its LayoutManager
 */
public interface OrientationProvider {

  int getOrientation(RecyclerView recyclerView);

  boolean isReverseLayout(RecyclerView recyclerView);
}
