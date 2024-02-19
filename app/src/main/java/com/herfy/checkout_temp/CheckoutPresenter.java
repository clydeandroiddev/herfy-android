package com.herfy.checkout_temp;

public class CheckoutPresenter implements CheckoutContract.CheckoutPresenter {

    private final CheckoutContract.View checkoutView;

    public CheckoutPresenter(CheckoutContract.View checkoutView) {
        this.checkoutView = checkoutView;
        this.checkoutView.setPresenter(this);
    }

    @Override
    public void start() {
        checkoutView.setupViews();
        getCards();
    }

    @Override
    public void getCards() {

    }
}
