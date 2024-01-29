package com.herfy.onboarding.views;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.core.app.ActivityOptionsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.herfy.BuildConfig;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.data.constants.Text;
import com.herfy.country.CountriesActivity;
import com.herfy.main.MainActivity;
import com.herfy.onboarding.OnboardingContract;
import com.herfy.onboarding.OnboardingFragmentPagerAdapter;
import com.herfy.onboarding.OnboardingPresenter;
import com.herfy.selection.SelectionActivity;
import com.herfy.util.CacheUtil;
import com.herfy.analytics.FacebookAppEventUtil;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class OnboardingActivity extends BaseActivity implements OnboardingContract.View {

    private OnboardingContract.Presenter onboardingPresenter;

    @BindView(R.id.view_pager)
    ViewPager2 viewPager;

    @BindView(R.id.proceed)
    Button proceed;

    @BindView(R.id.dots_indicator)
    DotsIndicator dotsIndicator;

    private OnboardingFragmentPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        ButterKnife.bind(this);
        onboardingPresenter = new OnboardingPresenter(this);
        onboardingPresenter.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setStatusBarColor();
        initBase();
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == adapter.getItemCount() - 1) {
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        } else {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    public void setPresenter(OnboardingContract.Presenter presenter) {
        onboardingPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {
        logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
    }

    @Override
    public void setupViews() {
        adapter = new OnboardingFragmentPagerAdapter(this);
        viewPager.setAdapter(adapter);
        dotsIndicator.setViewPager2(viewPager);
        int colorMain = Color.parseColor(CacheUtil.getInstance().getColorMain());
        dotsIndicator.setSelectedDotColor(colorMain);

        proceed.setBackgroundTintList(ColorStateList.valueOf(colorMain));

        proceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewPager.getCurrentItem() != adapter.getItemCount() - 1) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                } else {
                    FacebookAppEventUtil.Companion.completeTutorialEvent(OnboardingActivity.this);
                    if (BuildConfig.MULTI_COUNTRY_ENABLED) {
                        CacheUtil.getInstance().setOnboarding(true);
                        Intent countriesIntent = new Intent(OnboardingActivity.this, CountriesActivity.class);
                        countriesIntent.putExtra("home", true);
                        countriesIntent.putExtra("forceLocation", getIntent().getExtras().getBoolean("forceLocation", false));
                        startActivity(countriesIntent);
                        finish();
                    } else {
                        showLoadingDialog();
                        viewPager.setVisibility(View.INVISIBLE);
                        proceed.setVisibility(View.INVISIBLE);
                        dotsIndicator.setVisibility(View.INVISIBLE);
                        CacheUtil.getInstance().setOnboarding(true);
                        Intent intent;
                        if (BuildConfig.SUBSCRIPTION_ENABLED && CacheUtil.getInstance().getMode() == 0) {
                            intent = new Intent(OnboardingActivity.this, SelectionActivity.class);
                        } else {
                            intent = new Intent(OnboardingActivity.this, MainActivity.class);
                        }
                        intent.putExtra("home", true);
                        intent.putExtra("forceLocation", getIntent().getExtras().getBoolean("forceLocation", false));
                        Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(OnboardingActivity.this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
                        startActivity(intent, bundle);
                        finish();
                    }
                }
            }
        });

    }

    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {
        proceed.setText(CacheUtil.getInstance().getTranslations(Text.CONTINUE_TITLE));
    }

}
