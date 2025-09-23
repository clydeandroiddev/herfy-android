package com.herfy.language;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.herfy.BuildConfig;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.country.CountriesActivity;
import com.herfy.main.MainActivity;
import com.herfy.onboarding.views.OnboardingActivity;
import com.herfy.selection.SelectionActivity;
import com.herfy.utils.helper.BuildUtil;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.theme.DynamicTheme;
import com.skylinedynamics.solosdk.api.SoloAuth;
import com.herfy.utils.helper.AnimUtil;
import com.herfy.utils.helper.LocaleUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LanguageActivity extends BaseActivity implements LanguageContract.View {

    private LanguageContract.Presenter languagePresenter;

    @BindView(R.id.tvTitle)
    TextView title;
    @BindView(R.id.message)
    TextView message;
    @BindView(R.id.arabic_label)
    TextView arabicLabel;
    @BindView(R.id.english_label)
    TextView englishLabel;
    @BindView(R.id.french_label)
    TextView frenchLabel;

    @BindView(R.id.arabic_card)
    MaterialCardView arabicCard;
    @BindView(R.id.english_card)
    MaterialCardView englishCard;
    @BindView(R.id.french_card)
    MaterialCardView frenchCard;

    @BindView(R.id.arabic)
    ConstraintLayout arabic;
    @BindView(R.id.english)
    ConstraintLayout english;
    @BindView(R.id.french)
    ConstraintLayout french;

    @BindView(R.id.arabic_icon)
    ImageView arabicIcon;
    @BindView(R.id.english_icon)
    ImageView englishIcon;
    @BindView(R.id.french_icon)
    ImageView frenchIcon;

    @BindView(R.id.proceed)
    MaterialButton proceed;

    private YoYo.YoYoString rope;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_language);
        ButterKnife.bind(this);
        languagePresenter = new LanguagePresenter(this);
        languagePresenter.start();

    }

    @Override
    protected void onResume() {
        super.onResume();
        setStatusBarColor(DynamicTheme.getDefaultPrimaryColor());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void setPresenter(LanguageContract.Presenter presenter) {
        languagePresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {
        logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
    }

    @Override
    public void setupViews() {

        proceed.getBackground().setTint(Color.parseColor(CacheUtil.getInstance().getColorMain()));
        arabicIcon.setColorFilter(Color.parseColor(CacheUtil.getInstance().getColorMain()));
        englishIcon.setColorFilter(Color.parseColor(CacheUtil.getInstance().getColorMain()));
        frenchIcon.setColorFilter(Color.parseColor(CacheUtil.getInstance().getColorMain()));

        arabic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectArabic();
            }
        });

        english.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectEnglish();
            }
        });

        french.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFrench();
            }
        });

        proceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoadingDialog();
                HashMap<String, String> eventAttributes = new HashMap<>();
                if (arabicIcon.getVisibility() == View.VISIBLE) {
                    LocaleUtil.getInstance().setLanguage("ar");
                    SoloAuth.getInstance().setLanguage(LocaleUtil.AR_SA);
                    eventAttributes.put("ChangeLanguage", "AR");
                } else if(frenchIcon.getVisibility() == View.VISIBLE){
                    LocaleUtil.getInstance().setLanguage("fr");
                    SoloAuth.getInstance().setLanguage(LocaleUtil.FR);
                    eventAttributes.put("ChangeLanguage", "FR");
                }else {
                    LocaleUtil.getInstance().setLanguage("en");
                    SoloAuth.getInstance().setLanguage(LocaleUtil.EN_US);
                    eventAttributes.put("ChangeLanguage", "EN");
                }
                logEvent("ChangeLanguage", eventAttributes, null, 0);
                languagePresenter.changeLanguage();
                Intent intent;
                boolean goToMain;
                if (!CacheUtil.getInstance().getOnboarding() && BuildUtil.splashOnboardingApp()) {
                    goToMain = false;
                    intent = new Intent(LanguageActivity.this, OnboardingActivity.class);
                } else if(BuildConfig.SUBSCRIPTION_ENABLED && CacheUtil.getInstance().getMode() == 0) {
                    goToMain = false;
                    intent = new Intent(LanguageActivity.this, SelectionActivity.class);
                } else {
                    goToMain = true;
                    intent = new Intent(LanguageActivity.this, MainActivity.class);
                    intent.putExtra("home", true);
                }
                intent.putExtra("forceLocation", getIntent().getExtras().getBoolean("forceLocation", false));

                if (goToMain && BuildConfig.MULTI_COUNTRY_ENABLED) {
                    Intent countriesIntent = new Intent(LanguageActivity.this, CountriesActivity.class);
                    countriesIntent.putExtra("home", true);
                    countriesIntent.putExtra("forceLocation", getIntent().getExtras().getBoolean("forceLocation", false));
                    startActivity(countriesIntent);
                    finish();
                } else {
                    startActivity(intent);
                    overridePendingTransition(AnimUtil.inF(), AnimUtil.inS());
                    finish();
                }
            }
        });

        animateSpringView(Techniques.FadeIn, 100, title, -16);
        animateSpringView(Techniques.FadeIn, 150, message, -16);
        List<String> languages = new ArrayList<>(CacheUtil.getInstance().getConcept().getLanguages());
        for(String lng : languages){
            if(lng.equalsIgnoreCase(LocaleUtil.EN_US)){
                animateSpringView(Techniques.FadeIn, 250, englishCard, -16);
                english.setVisibility(View.VISIBLE);
            }else if(lng.equalsIgnoreCase(LocaleUtil.AR_SA)){
                animateSpringView(Techniques.FadeIn, 200, arabicCard, -16);
                arabic.setVisibility(View.VISIBLE);
            }else if(lng.equalsIgnoreCase(LocaleUtil.FR)){
                animateSpringView(Techniques.FadeIn, 200, frenchCard, -16);
                french.setVisibility(View.VISIBLE);
            }
        }
        animateSpringView(Techniques.FadeIn, 300, proceed, -16);
        /*new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                animateView(Techniques.SlideInUp, 150, message);
                animateView(Techniques.SlideInUp, 200, arabicCard);
                animateView(Techniques.SlideInUp, 250, englishCard);
                animateView(Techniques.ZoomIn, 300, proceed);
            }
        }, 200);*/

    }



    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {

    }

    @Override
    public void selectArabic() {
        title.setText(getString(R.string.please_pick_a_language_title_ar));
        message.setText(getString(R.string.please_pick_a_language_message_ar));
        arabicCard.setStrokeColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
        englishCard.setStrokeColor(ContextCompat.getColor(this, android.R.color.transparent));
        frenchCard.setStrokeColor(ContextCompat.getColor(this, android.R.color.transparent));
        arabicLabel.setTextColor(Color.parseColor("#171717"));
        englishLabel.setTextColor(Color.parseColor("#000000"));
        frenchLabel.setTextColor(Color.parseColor("#000000"));
        arabicIcon.setVisibility(View.VISIBLE);
        englishIcon.setVisibility(View.GONE);
        frenchIcon.setVisibility(View.GONE);
        proceed.setText(getString(R.string.proceed_ar));
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    }

    @Override
    public void selectEnglish() {
        title.setText(getString(R.string.please_pick_a_language_title_en));
        message.setText(getString(R.string.please_pick_a_language_message_en));
        arabicCard.setStrokeColor(ContextCompat.getColor(this, android.R.color.transparent));
        englishCard.setStrokeColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
        frenchCard.setStrokeColor(ContextCompat.getColor(this, android.R.color.transparent));
        arabicLabel.setTextColor(Color.parseColor("#000000"));
        englishLabel.setTextColor(Color.parseColor("#171717"));
        frenchLabel.setTextColor(Color.parseColor("#000000"));
        arabicIcon.setVisibility(View.GONE);
        englishIcon.setVisibility(View.VISIBLE);
        frenchIcon.setVisibility(View.GONE);
        proceed.setText(getString(R.string.proceed_en));
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
    }

    private void selectFrench() {
        title.setText(getString(R.string.please_pick_a_language_title_fr));
        message.setText(getString(R.string.please_pick_a_language_message_fr));
        arabicCard.setStrokeColor(ContextCompat.getColor(this, android.R.color.transparent));
        englishCard.setStrokeColor(ContextCompat.getColor(this, android.R.color.transparent));
        frenchCard.setStrokeColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
        arabicLabel.setTextColor(Color.parseColor("#000000"));
        englishLabel.setTextColor(Color.parseColor("#000000"));
        frenchLabel.setTextColor(Color.parseColor("#171717"));
        arabicIcon.setVisibility(View.GONE);
        englishIcon.setVisibility(View.GONE);
        frenchIcon.setVisibility(View.VISIBLE);
        proceed.setText(getString(R.string.proceed_fr));
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
    }

}
