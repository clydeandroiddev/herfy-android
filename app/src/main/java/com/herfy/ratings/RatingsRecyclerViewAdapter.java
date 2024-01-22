package com.herfy.ratings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.ratings.views.RatingViewHolder;

public class RatingsRecyclerViewAdapter extends RecyclerView.Adapter<RatingViewHolder> {

    private Context context;
    private final RatingsContract.Presenter ratingsPresenter;

    public RatingsRecyclerViewAdapter(Context context, RatingsContract.Presenter ratingsPresenter) {
        this.context = context;
        this.ratingsPresenter = ratingsPresenter;
    }

    @NonNull
    @Override
    public RatingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RatingViewHolder(context, ratingsPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rating, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RatingViewHolder holder, int position) {
         ratingsPresenter.onBindRatingViewAtPosition(position, holder);
    }

    @Override
    public int getItemCount() {
        return ratingsPresenter.getRatingsCount();
    }

}
