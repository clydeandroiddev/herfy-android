package com.herfy.subscription.subhome;

import java.util.ArrayList;

public class SubHomeContract {

    public interface View {

        void showSubs(ArrayList<Integer> subs);

        void setSubItem(Integer item);

        void openNutritionFacts();

    }

    public interface Presenter {

        void getHomeSubs();

        void setSubItem(Integer item);

        void openNutritionFacts();

    }
}
