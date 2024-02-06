package com.herfy.subscription.subhome.views;

import android.animation.Animator;
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
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.herfy.R;
import com.herfy.base.BaseDialogFragment;
import com.herfy.utils.constants.Text;
import com.herfy.subscription.subhome.StubFilterModel;
import com.herfy.subscription.subhome.adapter.FilterOptionsAdapter;
import com.herfy.utils.helper.CacheUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FilterOptionsDialogFragment extends BaseDialogFragment implements FilterOptionsAdapter.FilterOptionsListener {

    @BindView(R.id.container)
    ConstraintLayout container;
    @BindView(R.id.category_image_card)
    CardView card;
    @BindView(R.id.apply)
    AppCompatTextView apply;
    @BindView(R.id.list_filters)
    RecyclerView filtersList;
    @BindView(R.id.show_following)
    AppCompatTextView showFollowing;
    @BindView(R.id.list_layout)
    CardView listLayout;
    @BindView(R.id.polygon)
    ImageView polygon;

    private FilterOptionsAdapter adapter;
    private List<StubFilterModel> filters;
    private LinearLayoutManager linearLayoutManager;

    public static FilterOptionsDialogFragment newInstance() {
        return new FilterOptionsDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filters = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialog_filter_home, container, false);
        ButterKnife.bind(this, view);
        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupAdapter();
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

        super.onDismiss(dialog);
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

        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                slideOutDown(listLayout, polygon);
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

    private void setupAdapter() {
        linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        filtersList.setLayoutManager(linearLayoutManager);

        if (adapter == null) {
            adapter = new FilterOptionsAdapter(getActivity(), filters, this);
        }
        filtersList.setAdapter(adapter);
        loadList();
    }

    private void loadList() {
        filters.clear();
        StubFilterModel model = new StubFilterModel();
        model.selected = true;
        model.name = "Ketogenic Plans";
        filters.add(model);
        StubFilterModel model1 = new StubFilterModel();
        model1.selected = true;
        model1.name = "Gluten-free Plans";
        filters.add(model1);
        StubFilterModel model2 = new StubFilterModel();
        model2.selected = true;
        model2.name = "Lactose-free Plans";
        filters.add(model2);
        StubFilterModel model3 = new StubFilterModel();
        model3.selected = true;
        model3.name = "Vegetarian Plans";
        filters.add(model3);
        StubFilterModel model4 = new StubFilterModel();
        model4.selected = true;
        model4.name = "Vegan Plans";
        filters.add(model4);
        adapter.notifyDataSetChanged();
    }

    private void setupViews() {

        container.setOnClickListener(v -> onDismiss(getDialog()));

        apply.setOnClickListener(v -> onDismiss(getDialog()));

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

        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                slideInDown(listLayout, polygon);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        card.startAnimation(animationSet);
    }

    public static void slideOutDown(View view, ImageView polygon) {
        YoYo.with(Techniques.SlideOutUp)
                .duration(200)
                .interpolate(new AccelerateDecelerateInterpolator())
                .withListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        polygon.setVisibility(View.GONE);
                        view.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {

                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                })
                .playOn(view);
    }

    public static void slideInDown(View view, ImageView polygon) {
        YoYo.with(Techniques.SlideInDown)
                .duration(200)
                .interpolate(new AccelerateDecelerateInterpolator())
                .withListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        view.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        polygon.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                })
                .playOn(view);
    }


    private void setupTranslations() {
        showFollowing.setText(CacheUtil.getInstance().getTranslations(Text.SHOW_THE_FOLLOWING, "Show the following:"));
        apply.setText(CacheUtil.getInstance().getTranslations(Text.APPLY, "APPLY").toUpperCase());
    }

    @Override
    public void select(StubFilterModel menu, int position) {
        if (menu.selected) {
            filters.get(position).selected = false;
        } else {
            filters.get(position).selected = true;
        }
        adapter.notifyItemChanged(position);
    }
}
