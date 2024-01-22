package com.herfy.views;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import ss.com.bannerslider.ImageLoadingService;

public class SliderImageLoadingService implements ImageLoadingService {

    @Override
    public void loadImage(String url, ImageView imageView) {

        imageView.setScaleType(ImageView.ScaleType.FIT_XY);

        Glide.with(imageView.getContext()).load(url).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.ALL).into(imageView);

    }

    @Override
    public void loadImage(int resource, ImageView imageView) {

        imageView.setScaleType(ImageView.ScaleType.FIT_XY);

        Glide.with(imageView.getContext()).load(resource).skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.ALL).into(imageView);

    }

    @Override
    public void loadImage(String url, int placeHolder, int errorDrawable, ImageView imageView) {

        imageView.setScaleType(ImageView.ScaleType.FIT_XY);

        Glide.with(imageView.getContext()).load(url).skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.ALL).apply(new RequestOptions().placeholder(placeHolder).error(errorDrawable)).into(imageView);

    }
}
