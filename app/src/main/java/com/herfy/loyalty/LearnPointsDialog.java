package com.herfy.loyalty;

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
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.herfy.R;
import com.herfy.constants.Text;
import com.skylinedynamics.solosdk.api.models.objects.CollectRules;
import com.skylinedynamics.solosdk.api.models.objects.Loyalty;
import com.skylinedynamics.solosdk.api.models.objects.Tier;
import com.herfy.util.CacheUtil;
import com.herfy.util.CurrencyUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LearnPointsDialog extends DialogFragment {

    @BindView(R.id.btnOkay)
    MaterialButton btnOkay;

    @BindView(R.id.title)
    TextView title;

    @BindView(R.id.description)
    TextView description;

    @BindView(R.id.expiry)
    TextView expiry;

    @BindView(R.id.promotional_products)
    TextView promotionalProducts;

    Tier currentTier;

    String tier = "";
    List<Tier> tiers = new ArrayList<>();

    Loyalty loyalty;

    public static LearnPointsDialog newInstance(Loyalty loyalty, String tier, List<Tier> tiers) {
        LearnPointsDialog fragment = new LearnPointsDialog();
        fragment.tier = tier;
        fragment.tiers = tiers;
        fragment.loyalty = loyalty;
        return fragment;
    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialog_learn_points, container, false);
        ButterKnife.bind(this, view);
        return view;
    }


    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        title.setText(CacheUtil.getInstance().getTranslations(Text.HOW_DO_I_EARN_POINTS, "HOW DO I EARN POINTS?"));
        btnOkay.setText(CacheUtil.getInstance().getTranslations(Text.OKAY_CAPS, "OKAY"));
        promotionalProducts.setText(CacheUtil.getInstance().getTranslations(Text.PROMOTIONAL_PRODUCTS_MAY, "Promotional products may not be included in the loyalty points collection."));
        expiry.setText(CacheUtil.getInstance().getTranslations(Text.THE_POINTS_EXPIRE_IN, "The points expire in (x) day(s).").replace("(x)",String.valueOf(loyalty.getAttributes().getPointsExpireInDays())));

        for (Tier thisTier : tiers) {
            if (thisTier.getName().equalsIgnoreCase(tier)) {
                currentTier = thisTier;
            }
        }

        if (currentTier == null) {
            currentTier = tiers.get(tiers.size() - 1);
        }
        CollectRules collectRules = currentTier.getCollectRules();
        if (collectRules != null) {
            Integer earnPoint = collectRules.getEarnPoint();
            String transactions = CacheUtil.getInstance().getTranslations(
                    Text.YOU_CAN_EARN_POINTS,
                    "You can earn points by ordering food from this app. You will earn (x) points for every (y) (z) you spend."
            );

            if (earnPoint != null) {
                transactions = transactions.replace("(x)", String.valueOf(earnPoint));
            }

            transactions = transactions
                    .replace("(x)", String.valueOf(collectRules.getEarnPoint()))
                    .replace("(y)", CurrencyUtils.Companion.updateCurrency(collectRules.getPerSar(), true))
                    .replace(" (z)", "");
            description.setText(transactions);
        }
        btnOkay.setOnClickListener(view1 -> onDismiss(Objects.requireNonNull(getDialog())));
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


}

