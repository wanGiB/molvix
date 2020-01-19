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

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NotificationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements StickyRecyclerHeadersAdapter<RecyclerView.ViewHolder> {

    private List<Notification> notifications = Collections.emptyList();
    private Context context;

    public NotificationsAdapter(Context context) {
        this.context = context;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NotificationsItemViewHolder(LayoutInflater.from(context).inflate(R.layout.recycler_item_notification, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        NotificationsItemViewHolder notificationsItemViewHolder = (NotificationsItemViewHolder) holder;
        notificationsItemViewHolder.bindNotification(notifications.get(position));
    }

    @Override
    public String getHeaderId(int position) {
        return getDateHeaderValue(notifications.get(position).getTimeStamp());
    }

    @Override
    public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
        return new NotificationsSectionItemViewHolder(LayoutInflater.from(context).inflate(R.layout.notifications_section_view, parent, false));
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder, int position) {
        NotificationsSectionItemViewHolder notificationsSectionItemViewHolder = (NotificationsSectionItemViewHolder) holder;
        notificationsSectionItemViewHolder.bindDateLabel(notifications.get(position).getTimeStamp());
    }

    @Override
    public int getItemCount() {
        return notifications == null ? 0 : notifications.size();
    }

    private static String getDateHeaderValue(long timeStamp) {
        Date today = new Date();
        Date notificationDate = new Date(timeStamp);
        Calendar todayCalendar = Calendar.getInstance();
        Calendar notificationDateCalendar = Calendar.getInstance();
        notificationDateCalendar.setTime(notificationDate);
        int notificationDateValue = notificationDateCalendar.get(Calendar.DATE);
        int todayDateValue = todayCalendar.get(Calendar.DATE);
        String dateString;
        if (todayCalendar.get(Calendar.MONTH) == notificationDateCalendar.get(Calendar.MONTH)
                && todayCalendar.get(Calendar.DATE) == notificationDateCalendar.get(Calendar.DATE)
                && todayCalendar.get(Calendar.YEAR) == notificationDateCalendar.get(Calendar.YEAR)) {
            dateString = "Today";
        } else {
            int timeDiff = Math.abs(notificationDateValue - todayDateValue);
            if (timeDiff == 1 && todayCalendar.get(Calendar.MONTH) == notificationDateCalendar.get(Calendar.MONTH) && todayCalendar.get(Calendar.YEAR) == notificationDateCalendar.get(Calendar.YEAR)) {
                dateString = "Yesterday";
            } else {
                String currentYear = AppConstants.DATE_FORMATTER_IN_YEARS.format(today);
                dateString = AppConstants.DATE_FORMATTER_IN_BIRTHDAY_FORMAT.format(notificationDate).replace(currentYear, "");
            }
        }
        return dateString;
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

        void bindDateLabel(long timeStamp) {
            String dateString = getDateHeaderValue(timeStamp);
            dateLabelView.setText(dateString);
        }

    }

}
