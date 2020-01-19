package com.molvix.android.ui.rendering;

import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.molvix.android.ui.adapters.StickyRecyclerHeadersAdapter;

import java.util.HashMap;


/**
 * An implementation of {@link HeaderProvider} that creates and caches header views
 */
public class HeaderViewCache implements HeaderProvider {

    private final StickyRecyclerHeadersAdapter mAdapter;
    private final HashMap<String, View> mHeaderViews = new HashMap<>();
    private final OrientationProvider mOrientationProvider;

    public HeaderViewCache(StickyRecyclerHeadersAdapter adapter,
                           OrientationProvider orientationProvider) {
        mAdapter = adapter;
        mOrientationProvider = orientationProvider;
    }

    @Override
    @SuppressWarnings("unchecked")
    public View getHeader(RecyclerView parent, int position) {
        String headerId = mAdapter.getHeaderId(position);

        View header = mHeaderViews.get(headerId);

        if (header == null) {
            //TODO - recycle views
            RecyclerView.ViewHolder viewHolder = mAdapter.onCreateHeaderViewHolder(parent);
            mAdapter.onBindHeaderViewHolder(viewHolder, position);
            header = viewHolder.itemView;
            if (header.getLayoutParams() == null) {
                header.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            int widthSpec;
            int heightSpec;

            if (mOrientationProvider.getOrientation(parent) == LinearLayoutManager.VERTICAL) {
                widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY);
                heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.UNSPECIFIED);
            } else {
                widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.UNSPECIFIED);
                heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.EXACTLY);
            }

            int childWidth = ViewGroup.getChildMeasureSpec(widthSpec,
                    parent.getPaddingLeft() + parent.getPaddingRight(), header.getLayoutParams().width);
            int childHeight = ViewGroup.getChildMeasureSpec(heightSpec,
                    parent.getPaddingTop() + parent.getPaddingBottom(), header.getLayoutParams().height);
            header.measure(childWidth, childHeight);
            header.layout(0, 0, header.getMeasuredWidth(), header.getMeasuredHeight());
            mHeaderViews.put(headerId, header);
        }
        return header;
    }

    @Override
    public void invalidate() {
        mHeaderViews.clear();
    }
}
