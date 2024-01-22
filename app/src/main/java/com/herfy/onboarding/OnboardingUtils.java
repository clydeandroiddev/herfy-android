package com.herfy.onboarding;

import com.herfy.R;

public class OnboardingUtils {

    public enum OnboardingData {
        Restaurant(R.drawable.img_onboarding_restaurant_1, R.drawable.img_onboarding_restaurant_2),
        CoffeeShop(R.drawable.img_onboarding_coffee_shop_1, R.drawable.img_onboarding_coffee_shop_2),
        Fastfood(R.drawable.img_onboarding_fastfood_burger_1, R.drawable.img_onboarding_fastfood_burger_2),
        Healthy(R.drawable.img_onboarding_healthy_food_1, R.drawable.img_onboarding_healthy_food_2),
        Shawarma(R.drawable.img_onboarding_shawarma_place_1, R.drawable.img_onboarding_shawarma_place_2),
        Burgerizzr(R.drawable.onboarding_1, R.drawable.onboarding_2);

        public final int firstImage;
        public final int secondImage;

        OnboardingData(int firstImage, int secondImage) {
            this.firstImage = firstImage;
            this.secondImage = secondImage;
        }
    }

    public static OnboardingData getData(String appType) {
        OnboardingUtils.OnboardingData[] type = OnboardingUtils.OnboardingData.values();
        if(!appType.equals("")){
            for(OnboardingData data : type) {
                if(appType.equalsIgnoreCase(data.name().toString())) {
                    return data;
                }
            }
        }
        return OnboardingData.Restaurant;
    }
}
