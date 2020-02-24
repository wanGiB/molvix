package com.molvix.android.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.molvix.android.R;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.models.Notification;
import com.molvix.android.models.Notification_;
import com.molvix.android.ui.adapters.NotificationsAdapter;
import com.molvix.android.ui.rendering.StickyRecyclerHeadersDecoration;
import com.molvix.android.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NotificationsFragment extends BaseFragment {

    @BindView(R.id.notifications_recycler_view)
    RecyclerView notificationsRecyclerView;

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.loading_view)
    View loadingView;

    @BindView(R.id.content_loading_layout)
    View emptyContentParentView;

    @BindView(R.id.notifications_empty_view)
    View notificationsEmptyView;

    @BindView(R.id.btn_clear_all_notifications)
    FloatingActionButton clearNotificationsFab;

    @BindView(R.id.notifications_center_label)
    TextView notificationsCenterLabel;

    private List<Notification> notifications = new ArrayList<>();
    private NotificationsAdapter notificationsAdapter;

    private Handler mUIHandler = new Handler();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);
        ButterKnife.bind(this, root);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        clearNotificationsFab.hide();
        clearNotificationsFab.setOnClickListener(v -> {
            notifications.clear();
            notificationsAdapter.notifyDataSetChanged();
            MolvixDB.getNotificationBox().removeAll();
            invalidateUI();
            notificationsCenterLabel.setText(getString(R.string.all_caught_up));
        });
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
    }

    private void setupAdapter() {
        notificationsAdapter = new NotificationsAdapter(getActivity(), notifications);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        StickyRecyclerHeadersDecoration stickyRecyclerHeadersDecoration = new StickyRecyclerHeadersDecoration(notificationsAdapter);
        notificationsRecyclerView.addItemDecoration(stickyRecyclerHeadersDecoration);
        notificationsRecyclerView.setAdapter(notificationsAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (notificationsAdapter != null) {
            fetchNotifications();
        }
    }

    private void fetchNotifications() {
        new Thread(() -> {
            List<Notification> results = MolvixDB.getNotificationBox().query().orderDesc(Notification_.timeStamp).build().find();
            loadNotifications(results);
        }).start();
    }

    private void loadNotifications(List<Notification> results) {
        mUIHandler.post(() -> {
            if (!notifications.isEmpty()) {
                notifications.clear();
                notificationsAdapter.notifyDataSetChanged();
            }
            for (Notification notification : results) {
                if (!notifications.contains(notification)) {
                    notifications.add(notification);
                    notificationsAdapter.notifyItemInserted(notifications.size() - 1);
                } else {
                    int indexOfNotification = results.indexOf(notification);
                    if (indexOfNotification != -1) {
                        notifications.set(indexOfNotification, notification);
                        notificationsAdapter.notifyItemChanged(indexOfNotification);
                    }
                }
            }
            invalidateUI();
        });
    }

    private void invalidateUI() {
        if (notifications.isEmpty()) {
            UiUtils.toggleViewVisibility(emptyContentParentView, true);
            UiUtils.toggleViewVisibility(loadingView, false);
            UiUtils.toggleViewVisibility(notificationsEmptyView, true);
            clearNotificationsFab.hide();
        } else {
            UiUtils.toggleViewVisibility(emptyContentParentView, false);
            clearNotificationsFab.show();
        }
        swipeRefreshLayout.setRefreshing(false);
    }

}