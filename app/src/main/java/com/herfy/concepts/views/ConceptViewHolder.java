package com.herfy.concepts.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.herfy.R;
import com.herfy.concepts.ConceptsContract;
import com.herfy.font.FontHandler;
import com.herfy.util.ViewUtil;
import com.skylinedynamics.solosdk.api.models.objects.Concept;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ConceptViewHolder extends RecyclerView.ViewHolder implements ConceptsContract.View {

    private Context context;

    private ConceptsContract.Presenter conceptsPresenter;

    @BindView(R.id.card) MaterialCardView card;

    @BindView(R.id.container) ConstraintLayout container;

    @BindView(R.id.image) ImageView image;
    @BindView(R.id.icon) ImageView icon;

    @BindView(R.id.name) TextView name;

    public ConceptViewHolder(Context context, ConceptsContract.Presenter changeStorePresenter, View itemView) {

        super(itemView);

        ButterKnife.bind(this, itemView);

        this.context = context;
        this.conceptsPresenter = changeStorePresenter;

        setupViews();
        setupFonts();
        setupTranslations();

    }

    public ConceptViewHolder(@NonNull View itemView, Context context) {
        super(itemView);
        this.context = context;
    }


    @Override
    public void setPresenter(ConceptsContract.Presenter presenter) {
        conceptsPresenter = presenter;
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
        name.setTypeface(FontHandler.getMediumTypeface());
    }

    @Override
    public void setupTranslations() {

    }

    @Override
    public void setConcept(Concept concept, boolean selected) {
        if (selected) {
            card.setStrokeColor(ViewUtil.getColorMain(context));
            card.setStrokeWidth(ViewUtil.dpToPx(context, 2));
            icon.setVisibility(View.VISIBLE);
        } else {
            card.setStrokeColor(Color.parseColor("#BEBEBE"));
            card.setStrokeWidth(ViewUtil.dpToPx(context, 1));
            icon.setVisibility(View.GONE);
        }

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                conceptsPresenter.selectConcept(getAdapterPosition());
            }
        });

        Resources resources = context.getResources();
        image.setImageDrawable(resources.getDrawable(resources.getIdentifier(concept.getLogo(), "drawable", context.getPackageName())));

        name.setText(concept.getLabel());
    }

    @Override
    public void selectConcept(int selected, boolean newConcept) {

    }

    @Override
    public void showError(String error) {

    }

    @Override
    public void initSolo() {

    }

    @Override
    public void showSplash() {

    }

}