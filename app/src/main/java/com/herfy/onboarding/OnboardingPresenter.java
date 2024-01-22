package com.herfy.onboarding;

public class OnboardingPresenter implements OnboardingContract.Presenter {

    private final OnboardingContract.View onboardingView;

    public OnboardingPresenter(OnboardingContract.View onboardingView) {
        this.onboardingView = onboardingView;
        this.onboardingView.setPresenter(this);
    }

    @Override
    public void start() {
        onboardingView.setupViews();
        onboardingView.setupFonts();
        onboardingView.setupTranslations();
    }

}
