package com.herfy.newmenu.adapter;

import android.widget.ImageView;

import com.herfy.newmenu.SliderContract;

import ss.com.bannerslider.adapters.SliderAdapter;
import ss.com.bannerslider.viewholder.ImageSlideViewHolder;

public class SlidesSliderNewAdapter extends SliderAdapter {

    private final SliderContract.Presenter sliderPresenter;

    public SlidesSliderNewAdapter(SliderContract.Presenter sliderPresenter) {
        this.sliderPresenter = sliderPresenter;
    }

    @Override
    public int getItemCount() {
        return sliderPresenter.getSlidesCount();
    }

    @Override
    public void onBindImageSlide(int position, ImageSlideViewHolder imageSlideViewHolder) {
        imageSlideViewHolder.bindImageSlide(sliderPresenter.getSlide(position).getImageUri());
        imageSlideViewHolder.imageView.setScaleType(ImageView.ScaleType.FIT_XY);
    }
}

