package com.herfy.ratings;

import com.herfy.ratings.views.RatingViewHolder;
import com.skylinedynamics.solosdk.api.models.objects.Rating;

import java.util.ArrayList;
import java.util.List;

public class RatingsPresenter implements RatingsContract.Presenter {

    private RatingsContract.View ratingsView;

    private List<Rating> ratings = new ArrayList<>();

    public RatingsPresenter(RatingsContract.View ratingsView) {
        this.ratingsView = ratingsView;
        this.ratingsView.setPresenter(this);
    }

    @Override
    public void start() {
        ratingsView.setupViews();
        ratingsView.setupFonts();
        ratingsView.setupTranslations();
    }


    @Override
    public void setRatings(List<Rating> ratings) {
        this.ratings = ratings;
    }

    @Override
    public List<Rating> getRatings() {
        return ratings;
    }

    @Override
    public void onBindRatingViewAtPosition(int position, RatingViewHolder ratingViewHolder) {
        ratingViewHolder.setRating(ratings.get(position));
    }

    @Override
    public int getRatingsCount() {
        return ratings.size();
    }

    @Override
    public void rate(int position, int value) {
        ratings.get(position).getAttributes().setRating(value);
    }
}
