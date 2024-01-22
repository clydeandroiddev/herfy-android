package com.herfy.loyalty;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.constants.Text;
import com.skylinedynamics.solosdk.api.models.objects.Loyalty;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyPointsPosRedemption;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransaction;
import com.skylinedynamics.solosdk.api.models.objects.Tier;
import com.herfy.util.AnimUtil;
import com.herfy.util.CacheUtil;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LoyaltyPointsHistory extends BaseActivity implements LoyaltyContract.View  {

    private LoyaltyContract.Presenter loyaltyPresenter;
    private LoyaltyPointsRecyclerViewAdapter adapter;

    @BindView(R.id.back)MaterialButton back;
    @BindView(R.id.swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.recycler_view) RecyclerView recyclerView;
    @BindView(R.id.empty) ConstraintLayout emptyView;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.cart) ConstraintLayout cart;
    @BindView(R.id.btnGoToMenu) MaterialButton btnGoToMenu;
    @BindView(R.id.tvNoPoints) TextView tvNoPoints;
    @BindView(R.id.tvNoPointsDescription) TextView tvNoPointsDescription;
    @BindView(R.id.sort_label) TextView sortLabel;
    @BindView(R.id.sort_card) MaterialCardView sortCard;
    @BindView(R.id.sort_container) ConstraintLayout sortContainer;
    private boolean canLoadMoreItems = false;
    private boolean isLoadingHistory = false;
    private int page = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_loyalty_points_history);
        ButterKnife.bind(this);
        showLoadingDialog();
        loyaltyPresenter = new LoyaltyPresenter(this);
        loyaltyPresenter.start();
        loyaltyPresenter.getLoyaltyHistoryTransaction2(page);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(AnimUtil.outF(), AnimUtil.outS());
    }

    @Override
    public void setPresenter(LoyaltyContract.Presenter presenter) {
        loyaltyPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {
        logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
    }

    @Override
    public void setupViews() {
        cart.setVisibility(View.GONE);
        back.setVisibility(View.VISIBLE);
        back.setOnClickListener( new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        btnGoToMenu.setOnClickListener( new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                setResult(1024);
                onBackPressed();
            }
        });

        sortContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loyaltyPresenter.sortHistory();
            }
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                emptyView.setVisibility(View.GONE);
                sortCard.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
                page = 1;
                loyaltyPresenter.getLoyaltyHistoryTransaction2(page);
            }
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new LoyaltyPointsRecyclerViewAdapter(this, loyaltyPresenter);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!isLoadingHistory && canLoadMoreItems && linearLayoutManager.findLastCompletelyVisibleItemPosition() >= linearLayoutManager.getItemCount() - 1) {
                    isLoadingHistory = true;
                    loyaltyPresenter.getLoyaltyHistoryTransaction2(page);
                }
            }
        });
    }

    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {
        back.setText(CacheUtil.getInstance().getTranslations(Text.BACK, "Back"));
        title.setText(CacheUtil.getInstance().getTranslations(Text.POINTS_HISTORY, "POINTS HISTORY"));
        tvNoPoints.setText(CacheUtil.getInstance().getTranslations(Text.NO_POINTS_HISTORY, "No points history"));
        tvNoPointsDescription.setText(CacheUtil.getInstance().getTranslations(Text.START_EARNING_POINTS_LABEL, "You can start earning points by ordering anything in our menu."));
        btnGoToMenu.setText(CacheUtil.getInstance().getTranslations(Text.GO_TO_MENU, "GO TO MENU"));
        sortLabel.setText(CacheUtil.getInstance().getTranslations(Text.NEWEST_TO_OLDEST, "Newest to oldest"));
    }

    @Override
    public void showLoyalty(Loyalty loyalty, String name) {
        dismissDialogs();
    }

    @Override
    public void showLoyaltyError(String message) {
        isLoadingHistory = false;
        canLoadMoreItems = false;
        dismissDialogs();
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

    }

    @Override
    public void showLoyaltyPointsHistory(boolean newestToOldest, ArrayList<LoyaltyTransaction> loyaltyTransactions) {

        isLoadingHistory = false;

        page += 1;

        if (newestToOldest) {
            sortLabel.setText(CacheUtil.getInstance().getTranslations(Text.NEWEST_TO_OLDEST, "Newest to oldest"));
        } else {
            sortLabel.setText(CacheUtil. getInstance().getTranslations(Text.OLDEST_TO_NEWEST, "Oldest to newest"));
        }

        if (loyaltyTransactions.size() != 0 && loyaltyTransactions.size() % 10 == 0) {
            canLoadMoreItems = true;
        } else {
            canLoadMoreItems = false;
        }

        swipeRefreshLayout.setRefreshing(false);
        adapter.notifyDataSetChanged();

        if (loyaltyTransactions.size() > 0) {
            emptyView.setVisibility(View.GONE);
            sortCard.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.VISIBLE);
        } else {


            emptyView.setVisibility(View.VISIBLE);
            sortCard.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
        }

        dismissDialogs();
    }

    @Override
    public void setLoyaltyHistory(LoyaltyTransaction loyaltyTransaction) {

    }

    @Override
    public void showNextTier(Tier tier, int totalAccumulatedPoints, int pointsUntilNextTier, int startingPointsOfNextTier) {

    }

    @Override
    public void showQRPage(LoyaltyPointsPosRedemption loyalty) {

    }

    @Override
    public void showQrData(String data) {

    }
}
