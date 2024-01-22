package com.herfy.notification.views;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.herfy.R;
import com.herfy.constants.Text;
import com.herfy.notification.NotificationContract;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.notification.NotifData;
import com.skylinedynamics.solosdk.api.models.objects.notification.Notification;
import com.herfy.util.CacheUtil;
import com.herfy.util.LocaleUtil;

import java.util.HashMap;
import java.util.LinkedList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NotificationViewHolder extends RecyclerView.ViewHolder implements NotificationContract.View {


    private Context context;
    private NotificationContract.Presenter notificationPresenter;

    @BindView(R.id.container)
    ConstraintLayout notifContainer;
    @BindView(R.id.tvNotifTitle)
    TextView title;
    @BindView(R.id.tvNotifDesc)
    TextView desc;
    @BindView(R.id.ivNewNotif)
    ImageView notifBadge;
    public @BindView(R.id.foregroundRow)
    MaterialCardView foregroundRow;
    @BindView(R.id.clDelete)
    public ConstraintLayout clDelete;
    @BindView(R.id.textView)
    public TextView tvDelete;


    public NotificationViewHolder(Context context, NotificationContract.Presenter notificationPresenter, View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);

        this.context = context;
        this.notificationPresenter = notificationPresenter;

        setupViews();
        setupFonts();
        setupTranslations();
    }

    @Override
    public void setPresenter(NotificationContract.Presenter presenter) {
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {

    }

    @Override
    public void setCartExpiry() {

    }

    @Override
    public void setupViews() {

    }

    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {

    }

    @Override
    public void showNotifications(LinkedList<Notification> notifications, int pageCount) {

    }

    @Override
    public void setupNotification(Notification notification) {
        title.setText(notification.getNotifData().getTitle().getValue(LocaleUtil.getInstance().getLanguage()));
        desc.setText(notification.getNotifData().getMessage().getValue(LocaleUtil.getInstance().getLanguage()));
        tvDelete.setText(CacheUtil.getInstance().getTranslations(Text.DELETE, "Delete"));

        if(notification.getNotifData().getReadAt() != null) {
            notifBadge.setVisibility(View.GONE);
        } else {
            notifBadge.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void showNotificationDetails(NotifData notifData) {

    }

    @Override
    public void orderDeleted() {

    }

    @Override
    public void showNotifError() {

    }

    @Override
    public void showDigitalCoupon(Campaign digitalCoupon) {

    }

    @Override
    public void showError(String message) {

    }

}
