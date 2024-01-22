package com.herfy.checkout;

public class CheckoutPresenter implements CheckoutContract.Presenter {

    private final CheckoutContract.View checkoutView;

    public CheckoutPresenter(CheckoutContract.View checkoutView) {
        this.checkoutView = checkoutView;
        this.checkoutView.setPresenter(this);
    }

    @Override
    public void start() {
        checkoutView.setupViews();
        checkoutView.setupFonts();
        checkoutView.setupTranslations();
    }

}
