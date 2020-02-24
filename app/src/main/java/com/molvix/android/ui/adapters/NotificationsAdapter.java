package com.molvix.android.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.models.Notification;
import com.molvix.android.ui.widgets.NotificationView;
import com.molvix.android.utils.DateUtils;
import com.molvix.android.utils.MolvixGenUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NotificationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements StickyRecyclerHeadersAdapter<RecyclerView.ViewHolder> {

    private List<Notification> notifications;
    private Context context;

    public NotificationsAdapter(Context context, List<Notification> notifications) {
        this.context = context;
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NotificationsItemViewHolder(LayoutInflater.from(context).inflate(R.layout.recycler_view_item_notification, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        NotificationsItemViewHolder notificationsItemViewHolder = (NotificationsItemViewHolder) holder;
        notificationsItemViewHolder.bindNotification(notifications.get(position));
    }

    @Override
    public String getHeaderId(int position) {
        Notification notification = notifications.get(position);
        Date createdAt = new Date(notification.getTimeStamp());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(createdAt);
        return String.valueOf(MolvixGenUtils.hashCode(calendar.get(Calendar.YEAR), calendar.get(Calendar.DAY_OF_YEAR)));
    }

    @Override
    public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
        return new NotificationsSectionItemViewHolder(LayoutInflater.from(context).inflate(R.layout.notifications_section_view, parent, false));
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder, int position) {
        NotificationsSectionItemViewHolder notificationsSectionItemViewHolder = (NotificationsSectionItemViewHolder) holder;
        Notification notification = notifications.get(position);
        if (notification != null) {
            Date createdAt = new Date(notification.getTimeStamp());
            notificationsSectionItemViewHolder.bindDate(DateUtils.getRelativeDate(context, Locale.getDefault(), createdAt.getTime()));
        }
    }

    @Override
    public int getItemCount() {
        return notifications == null ? 0 : notifications.size();
    }

    static class NotificationsItemViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.notification_view)
        NotificationView notificationView;

        NotificationsItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bindNotification(Notification notification) {
            notificationView.bindNotification(notification);
        }

    }

    static class NotificationsSectionItemViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.date_label)
        TextView dateLabelView;

        NotificationsSectionItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bindDate(String dateString) {
            String currentYear = AppConstants.DATE_FORMATTER_IN_YEARS.format(new Date());
            dateLabelView.setText(StringUtils.removeEnd(dateString.replace(currentYear, ""),","));
        }

    }

}
