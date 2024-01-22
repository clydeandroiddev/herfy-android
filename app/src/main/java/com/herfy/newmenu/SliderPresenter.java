package com.herfy.newmenu;

import android.net.Uri;

import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.CmsApiCall;
import com.skylinedynamics.solosdk.api.models.objects.Slide;
import com.herfy.util.AppLogger;
import com.herfy.util.CacheUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SliderPresenter implements SliderContract.Presenter {

    private final SliderContract.View sliderView;

    private ArrayList<Slide> slides = new ArrayList<>();
    private ArrayList<Slide> midBanners = new ArrayList<>();

    public SliderPresenter(SliderContract.View sliderView) {
        this.sliderView = sliderView;
    }


    @Override
    public ArrayList<Slide> getSliders() {
        return slides;
    }

    @Override
    public void getSlider() {

        CmsApiCall.getInstance().getSlider(CacheUtil.getInstance().getSliderId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {

                slides = new ArrayList<>();

                try {

                    JSONObject object = new JSONObject(o.toString());

                    JSONArray dataArray = object.getJSONArray("data");

                    for (int i = 0; i < dataArray.length(); i++) {

                        JSONObject slideObject = dataArray.getJSONObject(i);

                        Slide slide = Slide.parse(slideObject);
                        slides.add(slide);

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                sliderView.showSlides(slides);

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                sliderView.showSlidesError();
            }
        });

    }

    @Override
    public ArrayList<Slide> getMidSliders() {
        return midBanners;
    }

    @Override
    public void getMidSlider() {
        if(!CacheUtil.getInstance().getMidSliderId().isEmpty()) {
            CmsApiCall.getInstance().getSlider(CacheUtil.getInstance().getMidSliderId(), new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    midBanners = new ArrayList<>();
                    try {
                        JSONObject object = new JSONObject(o.toString());
                        JSONArray dataArray = object.getJSONArray("data");
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject slideObject = dataArray.getJSONObject(i);
                            Slide slide = Slide.parse(slideObject);
                            midBanners.add(slide);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    sliderView.showMidBanner(midBanners);

                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    midBanners.clear();
                    sliderView.showMidBanner(midBanners);
                }
            });
        } else {
            midBanners.clear();
            sliderView.showMidBanner(midBanners);
        }
    }

    @Override
    public int getSlidesCount() {
        return slides.size();
    }

    @Override
    public Slide getSlide(int position) {
        if (slides.size() > position) {
            return slides.get(position);
        }
        return null;
    }

    @Override
    public void selectSlide(Slide slide) {

       if (slide != null && slide.getLinkValue() != null && !slide.getLinkValue().isEmpty()) {
            if (slide.getLinkType().equals("url")) {
                sliderView.showLink(Uri.parse(slide.getLinkValue()));
            } else if (slide.getLinkType().equals("category")) {
                sliderView.showMenuCategory(slide.getLinkValue());
            } else if (slide.getLinkType().equals("item")) {
                sliderView.showMenuItem(slide.getLinkValue());
            }
        }

    }

    @Override
    public int getMidSlidesCount() {
        return midBanners.size();
    }

    @Override
    public Slide getMidSlide(int position) {
        return midBanners.get(position);
    }

    @Override
    public void selectMidSlide(int position) {
        Slide slide;

        if (midBanners.size() == 1) {
            slide = midBanners.get(0);
        } else {
            slide = midBanners.get(position);
        }

        if (slide.getLinkValue() != null && !slide.getLinkValue().isEmpty()) {
            if (slide.getLinkType().equals("url")) {
                sliderView.showLink(Uri.parse(slide.getLinkValue()));
            } else if (slide.getLinkType().equals("category")) {
                sliderView.showMenuCategory(slide.getLinkValue());
            } else if (slide.getLinkType().equals("item")) {
                sliderView.showMenuItem(slide.getLinkValue());
            }

        }
    }
}
