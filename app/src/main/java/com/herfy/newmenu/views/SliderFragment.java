package com.herfy.newmenu.views;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.herfy.R;
import com.herfy.base.BaseFragment;
import com.herfy.data.constants.Values;
import com.herfy.newmenu.SliderContract;
import com.skylinedynamics.solosdk.api.models.objects.Slide;
import com.herfy.util.StringUtil;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SliderFragment extends BaseFragment implements SliderContract.View {

    private SliderContract.Presenter sliderPresenter;

    @BindView(R.id.container) RelativeLayout container;

    @BindView(R.id.loader) FrameLayout loader;

    @BindView(R.id.image) ImageView image;

    private Slide slide;

    public SliderFragment() {

    }

    public static SliderFragment newInstance(SliderContract.Presenter sliderPresenter, Slide slide) {
        SliderFragment sliderFragment = new SliderFragment();
        sliderFragment.sliderPresenter = sliderPresenter;
        sliderFragment.slide = slide;
        return sliderFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_slider, container, false);

        ButterKnife.bind(this, view);

        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sliderPresenter.selectSlide(slide);
            }
        });

        try {
            Activity activity = getActivity();
            if (activity != null) {
                String imageUrl = StringUtil.getCloudflareSliderUrl(Values.SLIDER_IMAGE_WIDTH, Values.SLIDER_IMAGE_HEIGHT, slide.getImageUri());
                Glide.with(activity).load(imageUrl)
                        .centerCrop()
                        .skipMemoryCache(false)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                loader.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                loader.setVisibility(View.GONE);
                                return false;
                            }
                        }).into(image);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void showSlides(ArrayList<Slide> slides) {

    }

    @Override
    public void showSlidesError() {

    }

    @Override
    public void showMidBanner(ArrayList<Slide> banners) {

    }

    @Override
    public void showLink(Uri uri) {

    }

    @Override
    public void showMenuItem(String menuItemId) {

    }

    @Override
    public void showMenuCategory(String menuCategoryId) {

    }

}
