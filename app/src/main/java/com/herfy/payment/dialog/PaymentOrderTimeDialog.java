package com.herfy.payment.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.herfy.R;
import com.herfy.utils.constants.Text;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.herfy.utils.helper.LocaleUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PaymentOrderTimeDialog extends DialogFragment {

    private OnAction onAction;
    private OrderType orderType;
    private LatLng latLng;
    private int mins;
    private String address;
    private String branchUrl;

    public static PaymentOrderTimeDialog newInstance(OrderType orderType, int mins, String address, String branchUrl, LatLng latLng, OnAction onAction) {
        PaymentOrderTimeDialog fragment = new PaymentOrderTimeDialog();
        fragment.onAction = onAction;
        fragment.orderType = orderType;
        fragment.latLng = latLng;
        fragment.mins = mins;
        fragment.address = address;
        fragment.branchUrl = branchUrl;
        return fragment;
    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        View view = inflater.inflate(R.layout.fragment_dialog_promised_time, container, false);
        MaterialButton confirm = view.findViewById(R.id.confirm);
        TextView title = view.findViewById(R.id.title);
        ImageButton close = view.findViewById(R.id.buttonClose);
        ImageView homeIcon = view.findViewById(R.id.homeIcon);
        TextView labelReady = view.findViewById(R.id.labelReady);
        TextView minutesText = view.findViewById(R.id.mins);
        TextView type = view.findViewById(R.id.type);
        TextView addressLabel = view.findViewById(R.id.address);
        ImageView rectangleIcon = view.findViewById(R.id.rectangleIcon);
        ImageView staticMap = view.findViewById(R.id.staticMap);
        ImageView branchImage = view.findViewById(R.id.branchImage);
        MaterialButton change = view.findViewById(R.id.btn_change);
        MaterialCardView card = view.findViewById(R.id.card);
        card.setBackgroundResource(R.color.page_background_color);
        minutesText.setText(mins + " " + CacheUtil.getInstance().getTranslations("mins", "mins"));

        change.setText(CacheUtil.getInstance().getTranslations(Text.CHANGE));
        confirm.setText(CacheUtil.getInstance().getTranslations(Text.CONFIRM_CAPS, "CONFIRM"));
        addressLabel.setText(address);
        branchImage.setVisibility(View.GONE);
        homeIcon.setVisibility(View.VISIBLE);

        if (orderType != null && orderType.equals(OrderType.CURBSIDE)) {
            type.setText(CacheUtil.getInstance().getTranslations(Text.CURBSIDE_PICKUP_FROM_CAPS, "CURBSIDE PICKUP FROM"));
        } else if (orderType != null && orderType.equals(OrderType.DRIVE_THRU)) {
            type.setText(CacheUtil.getInstance().getTranslations(Text.DRIVE_THRU_PICKUP, "Drive Thru Pickup").toUpperCase());
        } else if (orderType != null && orderType.equals(OrderType.PICKUP)) {
            type.setText(CacheUtil.getInstance().getTranslations(Text.WALK_IN_PICKUP, "Walk In Pickup").toUpperCase());
        } else {
            type.setText(CacheUtil.getInstance().getTranslations(Text.PICKUP_FROM, "PICKUP FROM"));
        }

        switch (orderType) {
            case PICKUP:
            case DRIVE_THRU:
            case CURBSIDE:
                rectangleIcon.setVisibility(View.VISIBLE);
                labelReady.setText(CacheUtil.getInstance().getTranslations(Text.PICKUP_READY_LABEL, "Your order will be ready in"));

                homeIcon.setImageResource(R.drawable.branch_icon);
                if (branchUrl != null) {
                    if (!branchUrl.isEmpty()) {
                        homeIcon.setVisibility(View.GONE);
                        branchImage.setVisibility(View.VISIBLE);
                        Glide.with(requireActivity()).load(branchUrl)
                                .skipMemoryCache(false)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .transform(new CenterCrop(), new RoundedCorners(requireActivity().getResources().getDimensionPixelSize(R.dimen.category_icon_12dp)))
                                .into(branchImage);
                    }
                }
                break;
            case DINE_IN:
                rectangleIcon.setVisibility(View.VISIBLE);
                labelReady.setText(CacheUtil.getInstance().getTranslations(Text.PICKUP_READY_LABEL, "Your order will be ready in"));
                type.setText(CacheUtil.getInstance().getTranslations(Text.DINING_AT, "DINING AT"));
                homeIcon.setImageResource(R.drawable.branch_icon);
                if (branchUrl != null) {
                    if (!branchUrl.isEmpty()) {
                        branchImage.setVisibility(View.VISIBLE);
                        homeIcon.setVisibility(View.GONE);
                        Glide.with(requireActivity()).load(branchUrl)
                                .skipMemoryCache(false)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .transform(new CenterCrop(), new RoundedCorners(requireActivity().getResources().getDimensionPixelSize(R.dimen.category_icon_12dp)))
                                .into(branchImage);
                    }
                }
                break;
            case DELIVERY:
                rectangleIcon.setVisibility(View.GONE);
                labelReady.setText(CacheUtil.getInstance().getTranslations(Text.DELIVERY_READY_LABEL, "Your order will be delivered in"));
                type.setText(CacheUtil.getInstance().getTranslations(Text.DELIVER_TO_CAPS, "DELIVER TO"));
                homeIcon.setImageResource(R.drawable.home_icon);
                break;
        }

        String languageToLoad = "en";
        if (LocaleUtil.getInstance().getLanguage().equalsIgnoreCase("ar")) {
            languageToLoad = "ar";
        }
        String key = new String(Base64.decode(getResources().getString(R.string.google_maps_api_key_encoded), Base64.DEFAULT));
        String url = "https://maps.googleapis.com/maps/api/staticmap?language=" + languageToLoad + "&center=" + latLng.latitude + "," + latLng.longitude + "&zoom=15&size=400x400&key=" + key;
        Glide.with(requireActivity()).load(url)
                .skipMemoryCache(false)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(new CenterCrop())
                .into(staticMap);

        close.setOnClickListener(view1 -> {
            dismissAllowingStateLoss();
            onAction.close();
        });
        title.setText(CacheUtil.getInstance().getTranslations(Text.PROMISED_TIME, "PROMISED TIME").toUpperCase());
        confirm.setOnClickListener(v -> {
            dismissAllowingStateLoss();
            onAction.confirm();
        });

        change.setOnClickListener(v -> {
            dismissAllowingStateLoss();
            onAction.change();
        });

        return view;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        onAction.close();
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            TypedValue typedValue = new TypedValue();
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            dialog.getWindow().setStatusBarColor(typedValue.data);
        }
    }

    @Override
    public void onCancel(@NotNull DialogInterface dialog) {
        onAction.close();
    }

    public interface OnAction {
        void close();

        void change();

        void confirm();
    }

}


