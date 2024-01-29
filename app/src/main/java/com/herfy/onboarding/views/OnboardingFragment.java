package com.herfy.onboarding.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.herfy.BuildConfig;
import com.herfy.R;
import com.herfy.base.BaseFragment;
import com.herfy.data.constants.Text;
import com.herfy.onboarding.OnboardingContract;
import com.herfy.onboarding.OnboardingPresenter;
import com.herfy.onboarding.OnboardingUtils;
import com.herfy.util.CacheUtil;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class OnboardingFragment extends BaseFragment implements OnboardingContract.View {

    private OnboardingContract.Presenter onboardingPresenter;

    //@BindView(R.id.image_container) LinearLayout imageContainer;
    @BindView(R.id.dynamic_image_container)
    ConstraintLayout dynamic_image_container;
    @BindView(R.id.image)
    ImageView image;
    @BindView(R.id.dynamic_image_header)
    ImageView dynamicImageHeader;
    @BindView(R.id.dynamic_image_content)
    ImageView dynamicImageContent;

    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.subtitle)
    TextView subtitle;

    private int index = 0;

    public OnboardingFragment() {

    }

    public static OnboardingFragment newInstance(int index) {
        OnboardingFragment onboardingFragment = new OnboardingFragment();
        onboardingFragment.index = index;
        return onboardingFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        onboardingPresenter = new OnboardingPresenter(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        onboardingPresenter.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (BuildConfig.SOLO_APP_TYPE.isEmpty()) {
            animate(image);
        } else {
            if (index == 0) {
                animate(dynamic_image_container);
            } else {
                animate(image);
            }
        }
        animate(title);
        animate(subtitle);
    }

    @Override
    public void setPresenter(OnboardingContract.Presenter presenter) {
        onboardingPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {

    }

    @Override
    public void setCartExpiry() {

    }

    @Override
    public void setupViews() {
        OnboardingUtils.OnboardingData appType = OnboardingUtils.getData(BuildConfig.SOLO_APP_TYPE);

        switch (index) {
            case 0:
                if (appType == OnboardingUtils.OnboardingData.Burgerizzr) {
                    image.setVisibility(View.VISIBLE);
                    dynamic_image_container.setVisibility(View.GONE);
                    dynamicImageHeader.setVisibility(View.GONE);
                    dynamicImageContent.setVisibility(View.GONE);
                    image.setImageResource(R.drawable.onboarding_1);
                } else {
                    image.setVisibility(View.GONE);
                    dynamic_image_container.setVisibility(View.VISIBLE);
                    dynamicImageHeader.setImageResource(OnboardingUtils.getData(BuildConfig.SOLO_APP_TYPE).firstImage);
                    dynamicImageContent.setImageResource(OnboardingUtils.getData(BuildConfig.SOLO_APP_TYPE).secondImage);
                }

                title.setText(CacheUtil.getInstance().getTranslations(Text.EXPLORE_OUR_MENU_TITLE, "Explore our menu"));
                subtitle.setText(CacheUtil.getInstance().getTranslations(Text.EXPLORE_OUR_MENU_MESSAGE, "Swipe on the screen to check out our other categories."));
                break;
            case 1:
                image.setVisibility(View.VISIBLE);
                dynamic_image_container.setVisibility(View.GONE);
                dynamicImageHeader.setVisibility(View.GONE);
                dynamicImageContent.setVisibility(View.GONE);
                if (appType == OnboardingUtils.OnboardingData.Burgerizzr) {
                    image.setImageResource(R.drawable.onboarding_2);
                } else {
                    image.setImageResource(R.drawable.img_onboarding_dynamic_2);
                }
                title.setText(CacheUtil.getInstance().getTranslations(Text.GET_STATUS_UPDATES_TITLE, "Get status updates"));
                subtitle.setText(CacheUtil.getInstance().getTranslations(Text.GET_STATUS_UPDATES_MESSAGE, "Know the exact status of your order, and get notified when it arrives."));
                break;
        }
    }

    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {

    }

    public void animate(View view) {
        YoYo.with(Techniques.BounceInUp)
                .duration(700)
                .playOn(view);
    }

}
