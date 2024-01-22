package com.herfy.ratings.views;

import android.content.Context;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.font.FontHandler;
import com.herfy.ratings.RatingsContract;
import com.herfy.util.ViewUtil;
import com.skylinedynamics.solosdk.api.models.objects.Rating;

import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RatingViewHolder extends RecyclerView.ViewHolder implements RatingsContract.View {

    private Context context;

    private RatingsContract.Presenter ratingsPresenter;

    @BindView(R.id.name) TextView name;

    @BindView(R.id.value) RatingBar value;

    public RatingViewHolder(Context context, RatingsContract.Presenter ratingsPresenter, View itemView) {

        super(itemView);

        ButterKnife.bind(this, itemView);

        this.context = context;
        this.ratingsPresenter = ratingsPresenter;

        setupViews();
        setupFonts();
        setupTranslations();

    }

    @Override
    public void setPresenter(RatingsContract.Presenter presenter) {

    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {

    }

    @Override
    public void setCartExpiry() {

    }

    @Override
    public void setupViews() {
        name.setTextColor(ViewUtil.getColorMain(context));
    }

    @Override
    public void setupFonts() {
        name.setTypeface(FontHandler.getRegularTypeface());
    }

    @Override
    public void setupTranslations() {

    }

    @Override
    public void setRatings(List<Rating> ratings) {
        
    }

    @Override
    public void setRating(Rating rating) {

        name.setText(rating.getAttributes().getName());

        value.setRating(rating.getAttributes().getRating());

        value.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                if(fromUser) {
                    ratingsPresenter.rate(getAdapterPosition(), (int) rating);
                }
            }
        });

    }


}
