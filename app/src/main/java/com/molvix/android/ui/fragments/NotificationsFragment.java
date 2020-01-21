package com.molvix.android.ui.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.molvix.android.R;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.models.Notification;
import com.molvix.android.observers.MolvixContentChangeObserver;
import com.molvix.android.ui.adapters.NotificationsAdapter;
import com.molvix.android.ui.rendering.StickyRecyclerHeadersDecoration;
import com.molvix.android.utils.MolvixDB;
import com.molvix.android.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NotificationsFragment extends Fragment {

    @BindView(R.id.notifications_recycler_view)
    RecyclerView notificationsRecyclerView;

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.loading_view)
    View loadingView;

    @BindView(R.id.content_loading_layout)
    View contentLoadingView;

    @BindView(R.id.notifications_empty_view)
    View notificationsEmptyView;

    private List<Notification> notifications = new ArrayList<>();
    private NotificationsAdapter notificationsAdapter;
    private StickyRecyclerHeadersDecoration stickyRecyclerHeadersDecoration;
    private NotificationsFetchTask notificationsFetchTask;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStop() {
        super.onStop();
        MolvixContentChangeObserver.removeNotificationsChangeListener();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);
        ButterKnife.bind(this, root);
        return root;
    }

    @SuppressWarnings("ConstantConditions")
    private void setupSwipeRefreshLayoutColorScheme() {
        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getActivity(), R.color.gplus_color_1),
                ContextCompat.getColor(getActivity(), R.color.gplus_color_2),
                ContextCompat.getColor(getActivity(), R.color.gplus_color_3),
                ContextCompat.getColor(getActivity(), R.color.gplus_color_4));
        swipeRefreshLayout.setOnRefreshListener(this::fetchNotifications);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupSwipeRefreshLayoutColorScheme();
        setupAdapter();
        fetchNotifications();
        fetchNotificationsFromRemoteResource();
    }

    private void fetchNotificationsFromRemoteResource() {
        if (notificationsFetchTask != null) {
            notificationsFetchTask.cancel(true);
            notificationsFetchTask = null;
        }
        notificationsFetchTask = new NotificationsFetchTask();
        notificationsFetchTask.execute();
    }

    private void setupAdapter() {
        notificationsAdapter = new NotificationsAdapter(getActivity());
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        stickyRecyclerHeadersDecoration = new StickyRecyclerHeadersDecoration(notificationsAdapter);
        notificationsRecyclerView.addItemDecoration(stickyRecyclerHeadersDecoration);
        notificationsRecyclerView.setAdapter(notificationsAdapter);
    }

    private void fetchNotifications() {
        MolvixDB.fetchNotifications((results, e) -> {
            if (!notifications.containsAll(results)) {
                notifications.addAll(results);
                notificationsAdapter.notifyDataSetChanged();
            }
            stickyRecyclerHeadersDecoration.invalidateHeaders();
            if (!results.isEmpty()) {
                for (Notification notification : results) {
                    notification.setSeen(true);
                    MolvixDB.updateNotification(notification);
                }
            }
            invalidateUI();
        });
    }

    private void invalidateUI() {
        if (notifications.isEmpty()) {
            UiUtils.toggleViewVisibility(notificationsEmptyView, true);
            UiUtils.toggleViewVisibility(loadingView, false);
        } else {
            UiUtils.toggleViewVisibility(contentLoadingView, false);
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    static class NotificationsFetchTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            ContentManager.fetchNotifications();
            return null;
        }
    }

}