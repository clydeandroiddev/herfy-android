package com.herfy.util;

import com.google.gson.reflect.TypeToken;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.util.GsonHelper;

import java.util.List;

public class CartUtil {

    public static boolean isSamePromoResponse(List<Campaign> digitalCoupons) {
        String promotionSaveString = CacheUtil.getInstance().getDigitalCoupons();
        if (!promotionSaveString.isEmpty()) {
            List<Campaign> savedDigitalCoupons = GsonHelper.gsonFinal().fromJson(promotionSaveString, new TypeToken<List<Campaign>>() {
            }.getType());
            if (savedDigitalCoupons.size() == digitalCoupons.size()) {
                int size = savedDigitalCoupons.size();
                int counter = 0;
                for (int a = 0; a < size; a++) {
                    if (digitalCoupons.get(a).getId().equals(digitalCoupons.get(a).getId())
                            && digitalCoupons.get(a).getPromotion().getId().equals(digitalCoupons.get(a).getPromotion().getId())
                            && digitalCoupons.get(a).getAttributes().getPromotionId().equals(digitalCoupons.get(a).getAttributes().getPromotionId())) {
                        counter = counter + 1;
                    }
                }
                return counter == size;
            }
        }
        return false;
    }
}
