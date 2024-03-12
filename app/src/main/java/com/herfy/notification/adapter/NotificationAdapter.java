package com.herfy.notification.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.notification.NotificationContract;
import com.herfy.notification.views.NotificationViewHolder;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationViewHolder> {

    private Context context;
    private final NotificationContract.Presenter presenter;

    public NotificationAdapter(Context context, NotificationContract.Presenter presenter) {
        this.context = context;
        this.presenter = presenter;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NotificationViewHolder(context, presenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_new_notification, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        presenter.onBindNotificationViewAtPosition(position, holder);
    }

    @Override
    public int getItemCount() { return presenter.getNotificationsCount();}
}
