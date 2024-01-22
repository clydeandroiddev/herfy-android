package com.herfy.ratings;

import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.herfy.ratings.views.RatingViewHolder;
import com.skylinedynamics.solosdk.api.models.objects.Rating;

import java.util.List;

public interface RatingsContract {

    interface View extends BaseView<Presenter> {

        void setRatings(List<Rating> ratings);

        void setRating(Rating rating);

    }

    interface Presenter extends BasePresenter {

        void setRatings(List<Rating> ratings);

        List<Rating> getRatings();

        void onBindRatingViewAtPosition(int position, RatingViewHolder ratingViewHolder);

        int getRatingsCount();

        void rate(int position, int value);

    }

}
