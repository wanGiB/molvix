package com.molvix.android.ui.fragments;

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
import com.molvix.android.models.Notification;
import com.molvix.android.ui.adapters.NotificationsAdapter;
import com.molvix.android.ui.rendering.StickyRecyclerHeadersDecoration;
import com.molvix.android.utils.UiUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.ImportFlag;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmResults;

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

    private RealmResults<Notification> notifications;
    private NotificationsAdapter notificationsAdapter;
    private Realm realm;
    private StickyRecyclerHeadersDecoration stickyRecyclerHeadersDecoration;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = Realm.getDefaultInstance();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (notifications != null) {
            notifications.removeAllChangeListeners();
        }
        realm.close();
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupSwipeRefreshLayoutColorScheme();
        setupAdapter();
        fetchNotifications();
    }

    private void setupAdapter() {
        notificationsAdapter = new NotificationsAdapter(getActivity());
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        stickyRecyclerHeadersDecoration = new StickyRecyclerHeadersDecoration(notificationsAdapter);
        notificationsRecyclerView.addItemDecoration(stickyRecyclerHeadersDecoration);
        notificationsRecyclerView.setAdapter(notificationsAdapter);
    }

    private void fetchNotifications() {
        notifications = realm.where(Notification.class).findAllAsync();
        OrderedRealmCollectionChangeListener<RealmResults<Notification>> notificationRealmChangeListener = (results, changeSet) -> {
            invalidateUI();
            notificationsAdapter.setNotifications(results);
            stickyRecyclerHeadersDecoration.invalidateHeaders();
            if (!results.isEmpty()) {
                realm.executeTransaction(r -> {
                    for (Notification notification : results) {
                        notification.setSeen(true);
                        r.copyToRealmOrUpdate(notification, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                    }
                });
            }
        };
        notifications.addChangeListener(notificationRealmChangeListener);
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

}