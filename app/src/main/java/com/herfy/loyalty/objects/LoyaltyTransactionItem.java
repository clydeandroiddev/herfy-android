package com.herfy.loyalty.objects;

import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransaction;

public class LoyaltyTransactionItem extends ListItem {
    private LoyaltyTransaction loyaltyTransaction;
    public LoyaltyTransaction  getLoyaltyTransaction() {
        return loyaltyTransaction;
    }
    public void setLoyaltyTransaction(LoyaltyTransaction loyaltyTransaction) {
        this.loyaltyTransaction = loyaltyTransaction;
    }
    @Override
    public int getType() {
        return TYPE_LOYALTY;
    }
}
