package com.herfy.loyalty;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.utils.constants.Text;
import com.herfy.font.FontHandler;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.Concept;
import com.skylinedynamics.solosdk.api.models.objects.Loyalty;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyPointsPosRedemption;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransaction;
import com.skylinedynamics.solosdk.api.models.objects.Tier;
import com.herfy.utils.helper.CurrencyUtils;
import com.herfy.util.StringUtil;
import com.herfy.utils.helper.TimeUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LoyaltyPointsViewHolder extends RecyclerView.ViewHolder implements LoyaltyContract.View {

    private Context context;
    @BindView(R.id.tvDate)
    TextView tvDate;
    @BindView(R.id.tvSarValue)
    TextView tvSarValue;
    @BindView(R.id.tvPoints)
    TextView tvPoints;
    @BindView(R.id.tvAmountSpentLabel)
    TextView tvAmountSpentLabel;

    private LoyaltyHistoryContract.Presenter loyaltyPresenter;
    public LoyaltyPointsViewHolder(Context context, LoyaltyHistoryContract.Presenter loyaltyPresenter, View itemView) {
        super(itemView);

        ButterKnife.bind(this, itemView);
        this.context = context;
        this.loyaltyPresenter = loyaltyPresenter;

        setupViews();
        setupFonts();
        setupTranslations();

    }


    @Override
    public void setPresenter(LoyaltyContract.Presenter presenter) {

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
        tvSarValue.setTypeface(FontHandler.getSemiboldTypeface());
    }

    @Override
    public void setupTranslations() {
        tvAmountSpentLabel.setText(CacheUtil.getInstance().getTranslations(Text.LOYALTY_AMOUNT_SPENT, "Amount Spent"));
    }

    @Override
    public void showLoyalty(Loyalty loyalty, String name) {

    }

    @Override
    public void showLoyaltyError(String message) {

    }

    @Override
    public void showLoyaltyPointsHistory(boolean newestToOldest, ArrayList<LoyaltyTransaction> loyaltyTransactions) {

    }

    @Override
    public void setLoyaltyHistory(LoyaltyTransaction loyaltyTransaction) {
        Concept concept = CacheUtil.getInstance().getConcept();
        tvDate.setText(TimeUtil.formatDate(loyaltyTransaction.getCreatedAt()));
        DecimalFormat formatter = new DecimalFormat("#,###");
        if (loyaltyTransaction.getType() != null && loyaltyTransaction.getType().equalsIgnoreCase("collection")) {
            tvPoints.setText("+" + formatter.format(loyaltyTransaction.getPoints()));
            tvPoints.setTextColor(context.getResources().getColor(R.color.background_color_blue));
        } else {
            tvPoints.setText("-" + formatter.format(loyaltyTransaction.getPoints()));
            tvPoints.setTextColor(context.getResources().getColor(R.color.text_color_red_points));
        }
        if (loyaltyTransaction.getDetails() != null && loyaltyTransaction.getDetails().getAmount() != null) {
            tvSarValue.setText(StringUtil.localize(formatter.format(loyaltyTransaction.getDetails().getAmount()) + " " + CacheUtil.getInstance().getTranslations(concept.getCurrencySymbol())));
        } else {
            tvSarValue.setText(CurrencyUtils.Companion.updateCurrency(0.0, true));
        }
    }

    @Override
    public void showNextTier(Tier tier, int totalAccumulatedPoints, int pointsUntilNextTier, int startingPointsOfNextTier, boolean isMaxTier, Loyalty loyalty) {

    }

    @Override
    public void showQRPage(LoyaltyPointsPosRedemption loyalty) {

    }

    @Override
    public void showQrData(String data) {

    }
    @Override
    public void showTiers(Loyalty loyalty) {

    }

}
