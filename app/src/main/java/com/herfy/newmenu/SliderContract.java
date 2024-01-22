package com.herfy.newmenu;

import android.net.Uri;

import com.skylinedynamics.solosdk.api.models.objects.Slide;

import java.util.ArrayList;

public class SliderContract {

    public interface View {

        void showSlides(ArrayList<Slide> slides);

        void showSlidesError();

        void showMidBanner(ArrayList<Slide> banners);

        void showLink(Uri uri);

        void showMenuItem(String menuItemId);

        void showMenuCategory(String menuCategoryId);

    }

    public interface Presenter {

        ArrayList<Slide> getSliders();

        void getSlider();

        ArrayList<Slide> getMidSliders();

        void getMidSlider();

        int getSlidesCount();

        Slide getSlide(int position);

        void selectSlide(Slide slide);

        int getMidSlidesCount();

        Slide getMidSlide(int position);

        void selectMidSlide(int position);

    }
}
