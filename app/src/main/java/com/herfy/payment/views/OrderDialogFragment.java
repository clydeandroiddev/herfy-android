package com.herfy.payment.views;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.airbnb.lottie.LottieAnimationView;
import com.herfy.R;
import com.herfy.base.BaseDialogFragment;
import com.herfy.utils.constants.Text;
import com.herfy.utils.helper.CacheUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

public class OrderDialogFragment extends BaseDialogFragment {

    @BindView(R.id.proper_label)
    AppCompatTextView properLabel;
    @BindView(R.id.animation_view)
    LottieAnimationView animationView;

    private boolean isSchedule;

    public OrderDialogFragment(boolean isSchedule) {
        this.isSchedule = isSchedule;
    }

    public static OrderDialogFragment newInstance(boolean isSchedule) {
        return new OrderDialogFragment(isSchedule);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialog_set, container, false);
        ButterKnife.bind(this, view);
        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        setupTranslations();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            Window window = dialog.getWindow();
            if(window != null) {
                window.setLayout(width, height);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                TypedValue typedValue = new TypedValue();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(typedValue.data);
            }
        }
    }

    private void setupViews() {
        if (isAdded()) { // Ensure that the fragment is added to fragment manager before dismissing it
            if (isSchedule) {
                properLabel.setText(CacheUtil.getInstance().getTranslations(Text.ORDER_SCHEDULED, "ORDER SCHEDULED").toUpperCase());
            } else {
                properLabel.setText(CacheUtil.getInstance().getTranslations(Text.ORDER_PLACED, "ORDER PLACED").toUpperCase());
            }
            try {
                new Handler().postDelayed(() -> {
                    if (getDialog() != null && getDialog().isShowing()) {
                        dismiss();
                    }
                }, 2250);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setupTranslations() {

    }

}

