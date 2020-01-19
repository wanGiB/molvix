package com.molvix.android.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.molvix.android.R;
import com.molvix.android.models.Notification;
import com.molvix.android.ui.adapters.NotificationsAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

public class NotificationsFragment extends Fragment {

    @BindView(R.id.notifications_recycler_view)
    RecyclerView notificationsRecyclerView;

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.content_loading_layout)
    View contentLoadingLayout;

    private RealmResults<Notification> notifications;
    private NotificationsAdapter notificationsAdapter;
    private Realm realm;
    private OrderedRealmCollectionChangeListener<RealmResults<Notification>> notificationRealmChangeListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = Realm.getDefaultInstance();
    }

    @Override
    public void onStop() {
        super.onStop();
        realm.close();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);
        ButterKnife.bind(this, root);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void fetchNotifications() {
        notifications = realm.where(Notification.class).findAllAsync();
        notificationRealmChangeListener = (notifications, changeSet) -> {
            notificationsAdapter.setNotifications(notifications);
//            invalidateUI();
        };
        notifications.addChangeListener(notificationRealmChangeListener);
    }

}