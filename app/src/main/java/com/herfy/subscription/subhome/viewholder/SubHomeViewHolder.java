package com.herfy.subscription.subhome.viewholder;

import android.content.Context;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.subscription.subhome.SubHomeContract;
import com.herfy.util.StringUtil;
import com.herfy.utils.theme.DynamicTheme;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SubHomeViewHolder extends RecyclerView.ViewHolder implements SubHomeContract.View {

    @BindView(R.id.item_image)
    ImageView itemImage;
    @BindView(R.id.item_avg_cal)
    AppCompatTextView averageCal;
    @BindView(R.id.item_days)
    AppCompatTextView days;
    @BindView(R.id.item_name)
    AppCompatTextView itemName;
    @BindView(R.id.item_price)
    AppCompatTextView itemPrice;
    @BindView(R.id.item_description)
    AppCompatTextView itemDescription;
    @BindView(R.id.item_order_type)
    AppCompatTextView itemOrderType;
    @BindView(R.id.container)
    ConstraintLayout container;

    private Context context;
    private SubHomeContract.Presenter menuPresenter;

    public SubHomeViewHolder(Context context, SubHomeContract.Presenter menuPresenter, View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        this.context = context;
        this.menuPresenter = menuPresenter;
        setupViews();
        setupFonts();
        setupTranslations();
    }


    private void setupViews() {

    }

    private void setupFonts() {

    }

    private void setupTranslations() {

    }

    @Override
    public void showSubs(ArrayList<Integer> subs) {

    }

    @Override
    public void setSubItem(Integer item) {
        averageCal.setText("3200 avg cal/meal");
        days.setText("20 days");
        String colorStr = DynamicTheme.getStringColor(context);
        itemName.setText(Html.fromHtml("Keto Breakfast and Lunch Plan Lite " + "<font color=" + colorStr + ">(40 meals)</font>"));
        itemDescription.setText("2x meals per day  •  4x weeks  •  for 1x person");
        itemPrice.setText(StringUtil.getNewFormattedPrice(6000, true));
        container.setOnClickListener(v -> menuPresenter.setSubItem(item));
        averageCal.setOnClickListener(v -> menuPresenter.openNutritionFacts());
        itemOrderType.setText("Delivery and VAT are included");
    }

    @Override
    public void openNutritionFacts() {

    }
}