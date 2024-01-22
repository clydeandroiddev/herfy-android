package com.herfy.subscription.subhome;

import java.util.ArrayList;

public class SubHomePresenter implements SubHomeContract.Presenter {

    private final SubHomeContract.View contractView;

    public SubHomePresenter(SubHomeContract.View contractView) {
        this.contractView = contractView;
    }

    @Override
    public void getHomeSubs() {
        ArrayList<Integer> slides = new ArrayList<>();
        slides.add(1);
        slides.add(2);
        slides.add(3);
        contractView.showSubs(slides);
    }

    @Override
    public void setSubItem(Integer item) {
        contractView.setSubItem(item);
    }

    @Override
    public void openNutritionFacts() {
        contractView.openNutritionFacts();
    }

}
