package com.herfy.cart.views;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.airbnb.lottie.LottieAnimationView;
import com.herfy.R;
import com.herfy.base.BaseDialogFragment;
import com.herfy.constants.Text;
import com.herfy.util.CacheUtil;

import org.jetbrains.annotations.NotNull;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CartSchedFragment extends BaseDialogFragment {

    @BindView(R.id.proper_label)
    AppCompatTextView properLabel;
    @BindView(R.id.animation_view)
    LottieAnimationView animationView;

    private boolean isSchedule;

    public CartSchedFragment(boolean isSchedule) {
        this.isSchedule = isSchedule;
    }

    public static CartSchedFragment newInstance(boolean isSchedule) {
        return new CartSchedFragment(isSchedule);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.payment_new_sliding_panel, container, false);
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
    public @NotNull Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(getActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                onDismiss(getDialog());
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                TypedValue typedValue = new TypedValue();
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                dialog.getWindow().setStatusBarColor(typedValue.data);
            }
        }
    }


    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
    }

    private void setupViews() {
        if (isSchedule) {
            properLabel.setText(CacheUtil.getInstance().getTranslations(Text.ORDER_SCHEDULED, "ORDER SCHEDULED").toUpperCase());
        } else {
            properLabel.setText(CacheUtil.getInstance().getTranslations(Text.ADDRESS_SET, "ORDER PLACED").toUpperCase());
        }
        try {
            new Handler().postDelayed(() -> dismiss(), 2250);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupTranslations() {

    }

}
