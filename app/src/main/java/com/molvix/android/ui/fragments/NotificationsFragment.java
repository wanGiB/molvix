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
import com.molvix.android.ui.adapters.NotificationsAdapter;
import com.molvix.android.ui.rendering.StickyRecyclerHeadersDecoration;
import com.molvix.android.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.objectbox.reactive.DataSubscription;

public class NotificationsFragment extends BaseFragment {

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

    @BindView(R.id.btn_clear_all_notifications)
    FloatingActionButton clearNotificationsFab;

    @BindView(R.id.notifications_center_label)
    TextView notificationsCenterLabel;

    private List<Notification> notifications = new ArrayList<>();
    private NotificationsAdapter notificationsAdapter;
    private DataSubscription notificationsSubScription;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);
        ButterKnife.bind(this, root);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        clearNotificationsFab.setOnClickListener(v -> {
            notifications.clear();
            notificationsAdapter.notifyDataSetChanged();
            MolvixDB.getNotificationBox().removeAll();
            notificationsCenterLabel.setText(getString(R.string.all_caught_up));
            invalidateUI();
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

    private void removeNotificationsChangeListener() {
        if (notificationsSubScription != null && !notificationsSubScription.isCanceled()) {
            notificationsSubScription.cancel();
            notificationsSubScription = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        removeNotificationsChangeListener();
    }

    private void setupAdapter() {
        notificationsAdapter = new NotificationsAdapter(getActivity());
        notificationsAdapter.setNotifications(notifications);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        StickyRecyclerHeadersDecoration stickyRecyclerHeadersDecoration = new StickyRecyclerHeadersDecoration(notificationsAdapter);
        notificationsRecyclerView.addItemDecoration(stickyRecyclerHeadersDecoration);
        notificationsRecyclerView.setAdapter(notificationsAdapter);
    }

    private void fetchNotifications() {
        try {
            notificationsSubScription = MolvixDB.getNotificationBox().query().build().subscribe().observer(this::loadNotifications);
            new Handler().postDelayed(this::invalidateUI, 5000);
        } catch (Exception ignored) {

        }
    }

    private void loadNotifications(List<Notification> results) {
        for (Notification notification : results) {
            if (!notifications.contains(notification)) {
                notifications.add(notification);
                if (notifications.size() == 1) {
                    notificationsAdapter.notifyDataSetChanged();
                } else {
                    notificationsAdapter.notifyItemInserted(notifications.size() - 1);
                }
            } else {
                int indexOfNotification = results.indexOf(notification);
                if (indexOfNotification != -1) {
                    notifications.set(indexOfNotification, notification);
                    notificationsAdapter.notifyItemChanged(indexOfNotification);
                }
            }
        }
        invalidateUI();
    }

    private void invalidateUI() {
        if (notifications.isEmpty()) {
            UiUtils.toggleViewVisibility(notificationsEmptyView, true);
            UiUtils.toggleViewVisibility(loadingView, false);
            UiUtils.toggleViewVisibility(clearNotificationsFab, false);
        } else {
            UiUtils.toggleViewVisibility(contentLoadingView, false);
            UiUtils.toggleViewVisibility(clearNotificationsFab, true);
        }
        swipeRefreshLayout.setRefreshing(false);
    }

}