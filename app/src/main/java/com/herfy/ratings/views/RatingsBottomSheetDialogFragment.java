package com.herfy.ratings.views;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.base.BaseBottomSheetDialogFragment;
import com.herfy.constants.Text;
import com.herfy.font.FontHandler;
import com.herfy.ratings.RatingsContract;
import com.herfy.ratings.RatingsPresenter;
import com.herfy.ratings.RatingsRecyclerViewAdapter;
import com.herfy.util.CacheUtil;
import com.herfy.util.ViewUtil;
import com.skylinedynamics.solosdk.api.models.objects.Rating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RatingsBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements RatingsContract.View {

    private RatingsContract.Presenter ratingsPresenter;

    private RatingsBottomSheetDialogFragment.OnSubmit onSubmit;

    public interface OnSubmit {
        void onSubmit(Dialog dialog, List<Rating> ratings);
    }

    @BindView(R.id.title) TextView title;

    @BindView(R.id.close) ImageButton close;

    @BindView(R.id.recycler_view) RecyclerView recyclerView;

    @BindView(R.id.submit) Button submit;

    private List<Rating> ratings = new ArrayList<>();

    public static RatingsBottomSheetDialogFragment newInstance(RatingsBottomSheetDialogFragment.OnSubmit onSubmit, List<Rating> ratings) {

        RatingsBottomSheetDialogFragment ratingsBottomSheetDialogFragment = new RatingsBottomSheetDialogFragment();

        ratingsBottomSheetDialogFragment.onSubmit = onSubmit;

        ratingsBottomSheetDialogFragment.setRatings(ratings);

        return ratingsBottomSheetDialogFragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_dialog_ratings, container, false);

        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ratingsPresenter = new RatingsPresenter(this);
        ratingsPresenter.setRatings(ratings);
        ratingsPresenter.start();
    }

    @Override
    public void setPresenter(RatingsContract.Presenter presenter) {
        ratingsPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {
        if(getActivity() != null) {
            ((BaseActivity) getActivity()).logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
        }
    }

    @Override
    public void setCartExpiry() {

    }

    @Override
    public void setupViews() {
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        RatingsRecyclerViewAdapter adapter = new RatingsRecyclerViewAdapter(getActivity(), ratingsPresenter);
        recyclerView.setAdapter(adapter);

        boolean rated = true;

        for (Rating rating : ratingsPresenter.getRatings()) {
            if(rating.getAttributes().getRating() == 0) {
                rated = false;
                break;
            }
        }

        submit.setVisibility(rated ? View.GONE : View.VISIBLE);

        submit.setBackgroundColor(ViewUtil.getColorMain(getActivity()));

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSubmit.onSubmit(getDialog(), ratingsPresenter.getRatings());
            }
        });
    }

    @Override
    public void setupFonts() {
        title.setTypeface(FontHandler.getSemiboldTypeface());
        submit.setTypeface(FontHandler.getRegularTypeface());
    }

    @Override
    public void setupTranslations() {
        title.setText(CacheUtil.getInstance().getTranslations(Text.RATE_YOUR_EXPERIENCE, "Rate your experience"));
        submit.setText(CacheUtil.getInstance().getTranslations(Text.SUBMIT_RATING));
    }

    @Override
    public void setRatings(List<Rating> ratingsList) {
        ratings.clear();
        ratings.addAll(ratingsList);
    }

    @Override
    public void setRating(Rating rating) {

    }

}
