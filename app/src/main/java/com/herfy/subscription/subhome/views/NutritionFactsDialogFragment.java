package com.herfy.subscription.subhome.views;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.herfy.R;
import com.herfy.base.BaseDialogFragment;
import com.herfy.utils.constants.Text;
import com.herfy.utils.helper.CacheUtil;

import org.jetbrains.annotations.NotNull;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NutritionFactsDialogFragment extends BaseDialogFragment {

    @BindView(R.id.card)
    CardView card;
    @BindView(R.id.container)
    ConstraintLayout container;
    @BindView(R.id.carbs_value)
    AppCompatTextView carbsValue;
    @BindView(R.id.carbs_label)
    AppCompatTextView carbsLabel;
    @BindView(R.id.fats_value)
    AppCompatTextView fatsValue;
    @BindView(R.id.fats_label)
    AppCompatTextView fatsLabel;
    @BindView(R.id.protein_value)
    AppCompatTextView proteinValue;
    @BindView(R.id.protein_label)
    AppCompatTextView proteinLabel;
    @BindView(R.id.calories_value)
    AppCompatTextView caloriesValue;
    @BindView(R.id.calories_label)
    AppCompatTextView caloriesLabel;
    @BindView(R.id.title_nutrition)
    AppCompatTextView titleNutrition;

    private String carbs;
    private String fats;
    private String calories;
    private String protein;

    public static NutritionFactsDialogFragment newInstance() {
        return new NutritionFactsDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            if (bundle.containsKey("carbs")) {
                carbs = bundle.getString("carbs");
            }
            if (bundle.containsKey("fats")) {
                fats = bundle.getString("fats");
            }
            if (bundle.containsKey("calories")) {
                calories = bundle.getString("calories");
            }
            if (bundle.containsKey("protein")) {
                protein = bundle.getString("protein");
            }
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialog_nutrition_facts, container, false);
        ButterKnife.bind(this, view);
        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        setupTranslations();
    }

    @Override
    public @NotNull Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(getActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                onDismiss(getDialog());
            }
        };
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                TypedValue typedValue = new TypedValue();
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                dialog.getWindow().setStatusBarColor(typedValue.data);
            }
        }
    }


    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {

        AnimationSet animationSet = new AnimationSet(true);
        Animation animation = new ScaleAnimation(1f, 0f, 1f, 0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setDuration(300);
        animation.setFillBefore(true);
        animation.setFillAfter(true);
        animationSet.addAnimation(animation);

        animation = new AlphaAnimation(1f, 0f);
        animation.setDuration(300);
        animation.setFillBefore(true);
        animation.setFillAfter(true);
        animationSet.addAnimation(animation);

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                dialog.dismiss();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        card.startAnimation(animationSet);

    }


    public void setupViews() {
        carbsValue.setText(carbs);
        proteinValue.setText(protein);
        fatsValue.setText(fats);
        caloriesValue.setText(calories);
        container.setOnClickListener(v -> onDismiss(getDialog()));
        AnimationSet animationSet = new AnimationSet(true);
        Animation animation = new ScaleAnimation(0f, 1f, 0f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setDuration(300);
        animation.setFillBefore(true);
        animation.setFillAfter(true);
        animationSet.addAnimation(animation);
        animation = new AlphaAnimation(0f, 1f);
        animation.setDuration(300);
        animation.setFillBefore(true);
        animation.setFillAfter(true);
        animationSet.addAnimation(animation);
        card.startAnimation(animationSet);
    }

    public void setupTranslations() {
        titleNutrition.setText(CacheUtil.getInstance().getTranslations(Text.AVERAGE_DAILY_NUTRITION_FACTS, "Average daily nutrition facts"));
        carbsLabel.setText(CacheUtil.getInstance().getTranslations(Text.TOTAL_CARBS, "Total Carbs"));
        fatsLabel.setText(CacheUtil.getInstance().getTranslations(Text.TOTAL_FATS, "Total Fats"));
        proteinLabel.setText(CacheUtil.getInstance().getTranslations(Text.TOTAL_PROTEIN, "Total Protein"));
        caloriesLabel.setText(CacheUtil.getInstance().getTranslations(Text.TOTAL_CALORIES, "Total Calories"));
    }

}

