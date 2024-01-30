package com.herfy.cart.views;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.base.BaseDialogFragment;
import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.herfy.cart.CartContract;
import com.herfy.data.constants.Text;
import com.herfy.font.FontHandler;
import com.herfy.order.OrderContract;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.herfy.util.CacheUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.ViewUtil;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CartDialogFragment extends BaseDialogFragment implements BaseView {

    private BasePresenter presenter;

    @BindView(R.id.message)
    TextView message;

    @BindView(R.id.menu_items_container)
    LinearLayout menuItemsContainer;

    @BindView(R.id.no)
    MaterialButton no;
    @BindView(R.id.yes)
    MaterialButton yes;
    @BindView(R.id.main_layout)
    ConstraintLayout mainLayout;

    public static CartDialogFragment newInstance(CartContract.Presenter cartPresenter) {
        CartDialogFragment cartDialogFragment = new CartDialogFragment();
        cartDialogFragment.presenter = cartPresenter;
        return cartDialogFragment;
    }

    public static CartDialogFragment newInstance(OrderContract.Presenter orderPresenter) {
        CartDialogFragment cartDialogFragment = new CartDialogFragment();
        cartDialogFragment.presenter = orderPresenter;
        return cartDialogFragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialog_cart, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        setupFonts();
        setupTranslations();
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
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                TypedValue typedValue = new TypedValue();
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                dialog.getWindow().setStatusBarColor(typedValue.data);
            }
        }
    }

    @Override
    public void setPresenter(Object presenter) {
        if (presenter instanceof BasePresenter) {
            this.presenter = (BasePresenter) presenter;
        }
    }

    @Override
    public void setCartExpiry() {

    }

    @Override
    public void logEvent(String eventName, HashMap eventAttributes, String eventMetricName, double eventMetricValue) {
        if (getActivity() != null) {
            ((BaseActivity) getActivity()).logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
        }
    }

    @Override
    public void setupViews() {


        menuItemsContainer.removeAllViews();

        if (presenter instanceof CartContract.Presenter) {
            message.setText(((CartContract.Presenter) presenter).getUnavailableMenuItemsMessage());
            for (MenuItem menuItem : ((CartContract.Presenter) presenter).getUnavailableMenuItems()) {
                TextView name = new TextView(getActivity());
                name.setTextColor(ContextCompat.getColor(getActivity(), R.color.primary_text));
                name.setTypeface(FontHandler.getRegularTypeface());
                name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                name.setText("- " + menuItem.getName(LocaleUtil.getInstance().getLanguage()));
                name.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                name.setGravity(View.TEXT_ALIGNMENT_CENTER);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(ViewUtil.dpToPx(getActivity(), 16), 0, ViewUtil.dpToPx(getActivity(), 16), 0);
                name.setLayoutParams(layoutParams);
                menuItemsContainer.addView(name);
            }
        } else if (presenter instanceof OrderContract.Presenter) {
            message.setText(((OrderContract.Presenter) presenter).getUnavailableMenuItemsMessage());
            for (MenuItem menuItem : ((OrderContract.Presenter) presenter).getUnavailableMenuItems()) {
                TextView name = new TextView(getActivity());
                name.setTextColor(ContextCompat.getColor(getActivity(), R.color.primary_text));
                name.setTypeface(FontHandler.getRegularTypeface());
                name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                name.setText("- " + (menuItem.getName(LocaleUtil.getInstance().getLanguage())));
                name.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                name.setGravity(View.TEXT_ALIGNMENT_CENTER);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(ViewUtil.dpToPx(getActivity(), 16), 0, ViewUtil.dpToPx(getActivity(), 16), 0);
                name.setLayoutParams(layoutParams);
                menuItemsContainer.addView(name);
            }
        }

        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((BaseActivity) getActivity()).dismissDialogs();
                dismiss();
            }
        });

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (presenter instanceof CartContract.Presenter) {
                    ((CartContract.Presenter) presenter).deleteUnavailableItems();
                } else if (presenter instanceof OrderContract.Presenter) {
                    ((OrderContract.Presenter) presenter).deleteUnavailableItems();
                }
                dismiss();
            }
        });

    }

    @Override
    public void setupFonts() {
        message.setTypeface(FontHandler.getMediumTypeface());
    }

    @Override
    public void setupTranslations() {

        no.setText(CacheUtil.getInstance().getTranslations(Text.NO_CAPS));
        yes.setText(CacheUtil.getInstance().getTranslations(Text.YES_CAPS));
    }

}
