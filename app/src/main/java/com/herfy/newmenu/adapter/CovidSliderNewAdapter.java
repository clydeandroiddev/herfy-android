package com.herfy.newmenu.adapter;

import android.widget.ImageView;


import com.herfy.newmenu.SliderContract;
import com.herfy.utils.constants.Values;
import com.skylinedynamics.solosdk.api.models.objects.Slide;
import com.herfy.util.StringUtil;

import ss.com.bannerslider.adapters.SliderAdapter;
import ss.com.bannerslider.viewholder.ImageSlideViewHolder;

public class CovidSliderNewAdapter extends SliderAdapter {

    private final SliderContract.Presenter sliderPresenter;

    public CovidSliderNewAdapter(SliderContract.Presenter sliderPresenter) {
        this.sliderPresenter = sliderPresenter;
    }

    @Override
    public int getItemCount() {
        return sliderPresenter.getMidSlidesCount();
    }

    @Override
    public void onBindImageSlide(int position, ImageSlideViewHolder imageSlideViewHolder) {
        Slide slide = sliderPresenter.getMidSlide(position);

        //AppLogger.longError("midslider "+slide.getImageUri());
        if (slide != null) {
            String imageUrl = StringUtil.getCloudflareSliderUrl(Values.SLIDER_IMAGE_WIDTH, Values.SLIDER_IMAGE_HEIGHT, slide.getImageUri());
            imageSlideViewHolder.bindImageSlide(slide.getImageUri());
            imageSlideViewHolder.imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        }
    }
}
