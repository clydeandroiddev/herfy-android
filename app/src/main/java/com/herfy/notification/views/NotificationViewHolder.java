package com.herfy.notification.views;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;
import com.herfy.R;
import com.herfy.notification.NotificationContract;
import com.herfy.utils.helper.DateFormatterUtil;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.notification.NotifData;
import com.skylinedynamics.solosdk.api.models.objects.notification.Notification;
import com.herfy.utils.helper.LocaleUtil;
import java.util.HashMap;
import java.util.LinkedList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NotificationViewHolder extends RecyclerView.ViewHolder implements NotificationContract.View {


    private Context context;
    private NotificationContract.Presenter notificationPresenter;

    @BindView(R.id.tvNotifTitle)
    TextView title;

    @BindView(R.id.txtMinutes)
    MaterialTextView txtMinutes;
    @BindView(R.id.notificationImage)
    ShapeableImageView notificationImage;

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
        String imageUrl = notification.getNotifData().getImageUri();
        title.setText(notification.getNotifData().getTitle().getValue(LocaleUtil.getInstance().getLanguage()));
        if (imageUrl != null && !imageUrl.equalsIgnoreCase("null") && !imageUrl.isEmpty() && !imageUrl.toLowerCase().contains("default-image.png")) {
            Glide.with(context).load(imageUrl).apply(RequestOptions.centerCropTransform()).placeholder(R.drawable.imagepicker_image_placeholder).diskCacheStrategy(DiskCacheStrategy.ALL).into(notificationImage);
            notificationImage.setVisibility(View.VISIBLE);
        } else {
            notificationImage.setVisibility(View.GONE);
        }
        txtMinutes.setText(DateFormatterUtil.INSTANCE.formatTimeAgo(notification
                .getNotifData().getCreatedAt()));
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

    @Override
    public void setIsLoading(boolean isLoading) {

    }

}
