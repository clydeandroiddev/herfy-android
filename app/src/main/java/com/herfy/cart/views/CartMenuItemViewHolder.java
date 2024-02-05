package com.herfy.cart.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.TextWatcher;
import android.text.style.StrikethroughSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.herfy.R;
import com.herfy.cart.CartContract;
import com.herfy.utils.constants.Text;
import com.herfy.database.AppDatabase;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.Ingredient;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.ServiceCharge;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.skylinedynamics.solosdk.api.models.objects.Translated;
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfig;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CartMenuItemViewHolder extends RecyclerView.ViewHolder implements CartContract.View {

    private Context context;

    private CartContract.Presenter cartPresenter;

    @BindView(R.id.image)
    ImageView image;

    @BindView(R.id.quantity)
    AppCompatTextView quantity;
    @BindView(R.id.total_price)
    AppCompatTextView totalPrice;
    @BindView(R.id.name)
    AppCompatTextView name;
    @BindView(R.id.modifier_items)
    AppCompatTextView modifierItems;
    @BindView(R.id.ingredients)
    AppCompatTextView ingredients;

    @BindView(R.id.plus)
    FrameLayout plus;
    @BindView(R.id.minus)
    FrameLayout minus;

    @BindView(R.id.delete)
    AppCompatImageButton delete;

    @BindView(R.id.edit)
    FloatingActionButton edit;

    @BindView(R.id.notes_container)
    LinearLayout notesContainer;
    @BindView(R.id.notes)
    EditText notes;

    @BindView(R.id.old_price_view)
    AppCompatTextView oldPriceView;
    @BindView(R.id.old_price)
    AppCompatTextView oldPrice;

    public CartMenuItemViewHolder(Context context, CartContract.Presenter cartPresenter, View itemView) {

        super(itemView);

        ButterKnife.bind(this, itemView);

        this.context = context;
        this.cartPresenter = cartPresenter;

        setupViews();
        setupFonts();
        setupTranslations();

    }

    public CartMenuItemViewHolder(@NonNull View itemView, CartContract.Presenter cartPresenter) {
        super(itemView);
        this.cartPresenter = cartPresenter;
    }

    @Override
    public void setPresenter(CartContract.Presenter presenter) {

    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {

    }

    @Override
    public void setCartExpiry() {

    }

    @Override
    public void setupViews() {

    }

    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {
        notes.setHint(CacheUtil.getInstance().getTranslations(Text.ADD_NOTE, "Add note"));
    }

    @Override
    public void showProgressDialog() {

    }

    @Override
    public void dismissProgressDialog() {

    }

    @Override
    public void showStore(Store store) {

    }

    @Override
    public void showAddress(String address) {

    }

    @Override
    public void showMenuItems(List<MenuItem> menuItems, boolean fromPromo, boolean fromRemoveCoupon) {

    }

    @Override
    public void refreshMenuItems(int position, boolean fromChangeQuantity) {

    }

    @Override
    public void updateMenuItems(List<MenuItem> menuItems) {

    }

    @Override
    public void setMenuItem(MenuItem menuItem) {
        AppDatabase appDatabase = AppDatabase.getInstance();
        plus.setVisibility(menuItem.isPromo() ? View.INVISIBLE : View.VISIBLE);
        minus.setVisibility(menuItem.isPromo() ? View.INVISIBLE : View.VISIBLE);


        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cartPresenter.changeQuantity(getAdapterPosition(), true);
            }
        });

        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cartPresenter.changeQuantity(getAdapterPosition(), false);
            }
        });

        String imageUrl = menuItem.getImage();
        if (imageUrl == null || imageUrl.equalsIgnoreCase("null") || imageUrl.isEmpty() || imageUrl.toLowerCase().contains("default-image.png")) {
            imageUrl = Values.DEFAULT_IMAGE_URL;
        }

        Glide.with(context).load(imageUrl).skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.centerInsideTransform()).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                //image.setVisibility(View.GONE);

                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                image.setVisibility(View.VISIBLE);
                return false;
            }
        }).into(image);

        quantity.setText(StringUtil.localize(String.valueOf(menuItem.getAttributes().getQuantity())));

        String orderStoreId = null;
        if (CacheUtil.getInstance().getOrderStore() != null) {
            orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
        }

        double menuItemTotalPrice = cartPresenter.getMenuItemTotalPrice(menuItem, -1);
        if (!menuItem.isPromo()) {
            totalPrice.setText(StringUtil.getNewFormattedPrice(menuItemTotalPrice * menuItem.getAttributes().getQuantity(), true));
        } else {
            if (menuItemTotalPrice > 0) {
                totalPrice.setText(StringUtil.getNewFormattedPrice(menuItemTotalPrice * menuItem.getAttributes().getQuantity(), true));
            } else {
                totalPrice.setText(CacheUtil.getInstance().getTranslations(Text.FREE_CAPS, "FREE"));
            }
        }

        name.setText(menuItem.getName(LocaleUtil.getInstance().getLanguage()));

        boolean first = true;

        StringBuilder stringBuilder = new StringBuilder();

        int comboIndex = 0;
        int trueIndex = 0;
        for (ModifierItem size : menuItem.getSizes().getModifierItems()) {
            if (size.getAttributes().isSelected()) {
                if (!first) {
                    stringBuilder.append(", ");
                } else {
                    first = false;
                }
                trueIndex = comboIndex;
                stringBuilder.append(size.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()) + "(" + StringUtil.localize(String.valueOf(size.getAttributes().getQuantity()) + ")"));
            }
            comboIndex++;
        }

        for (ModifierItem drink : menuItem.getDrinks().getModifierItems()) {
            if (drink.getAttributes().isSelected()) {
                if (!first) {
                    stringBuilder.append(", ");
                } else {
                    first = false;
                }
                stringBuilder.append(drink.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()) + "(" + StringUtil.localize(String.valueOf(drink.getAttributes().getQuantity()) + ")"));
            }
        }

        for (ModifierItem fries : menuItem.getFries().getModifierItems()) {
            if (fries.getAttributes().isSelected()) {
                if (!first) {
                    stringBuilder.append(", ");
                } else {
                    first = false;
                }
                stringBuilder.append(fries.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()) + "(" + StringUtil.localize(String.valueOf(fries.getAttributes().getQuantity()) + ")"));
            }
        }

        for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
            VisibilityConfig visibilityConfig = modifierGroup.getAttributes().getVisibilityConfig();
            if (visibilityConfig == null || visibilityConfig.getInitialValue() == null || !visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
                for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                    if (modifierItem.getAttributes().isSelected()) {
                        if (!first) {
                            stringBuilder.append(", ");
                        } else {
                            first = false;
                        }
                        stringBuilder.append(modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()) + "(" + StringUtil.localize(String.valueOf(modifierItem.getAttributes().getQuantity()) + ")"));
                    }
                }

            }

        }

        for (ModifierItem modifierItem : menuItem.getModifierItems()) {
            if (modifierItem.getAttributes().isSelected()) {
                if (!first) {
                    stringBuilder.append(", ");
                } else {
                    first = false;
                }
                stringBuilder.append(modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()) + "(" + StringUtil.localize(String.valueOf(modifierItem.getAttributes().getQuantity())) + ")");
            }
        }

        try {
            if (menuItem.isCombo) {
                for (MenuItemComponents components : menuItem.combo.get(trueIndex).components) {
                    for (ComponentModifierItem item : components.items) {
                        if (item.selected) {
                            if (!menuItem.isSandwichOnly) {
                                if (!first) {
                                    stringBuilder.append(", ");
                                } else {
                                    first = false;
                                }
                                stringBuilder.append(translated(LocaleUtil.getInstance().getLanguage(), item.name) + "(" + StringUtil.localize(String.valueOf(1)) + ")");
                            }
                            if (!item.modifierGroups.isEmpty()) {
//                                if (components.name.getEnglish().toLowerCase().contains("sandwich") || components.name.getFrench().toLowerCase().contains("sandwich")) {
//                                    int getIndex = getYourWayModifier(item.modifierGroups);
//                                    if (getIndex != -1) {
//                                        for (int i = 0; i < item.modifierGroups.get(getIndex).getModifierItems().size(); i++) {
//                                            if (item.modifierGroups.get(getIndex).getModifierItems().get(i).getAttributes().isSelected()) {
//                                                Log.e("==$AAAAAAAAname", "AAAA:" + item.modifierGroups.get(getIndex).getModifierItems().get(i).getAttributes().getName());
//                                                if (!first) {
//                                                    stringBuilder.append(", ");
//                                                } else {
//                                                    first = false;
//                                                }
//                                                stringBuilder.append(item.modifierGroups.get(getIndex).getModifierItems().get(i).getAttributes().getName() + "(" + StringUtil.localize(String.valueOf(item.modifierGroups.get(getIndex).getModifierItems().get(i).getAttributes().getQuantity())) + ")");
//                                            }
//                                        }
//                                    }
//                                } else if ((!menuItem.isSandwichOnly && (!components.name.getEnglish().toLowerCase().contains("sandwich") && !components.name.getFrench().toLowerCase().contains("sandwich"))) || ((!components.name.getEnglish().toLowerCase().contains("sandwich") && !components.name.getFrench().toLowerCase().contains("sandwich")) && menuItem.customFieldObjectMap.isNotItemable)) {
//                                    for (ModifierGroup modifierGroup : item.modifierGroups) {
//                                        for (int i = 0; i < modifierGroup.getModifierItems().size(); i++) {
//                                            if (modifierGroup.getModifierItems().get(i).getAttributes().isSelected()) {
//                                                if (!first) {
//                                                    stringBuilder.append(", ");
//                                                } else {
//                                                    first = false;
//                                                }
//                                                stringBuilder.append(modifierGroup.getModifierItems().get(i).getAttributes().getName() + "(" + StringUtil.localize(String.valueOf(modifierGroup.getModifierItems().get(i).getAttributes().getQuantity())) + ")");
//                                            }
//                                        }
//                                    }
//                                }
                                for (ModifierGroup modifierGroup : item.modifierGroups) {
                                    for (int i = 0; i < modifierGroup.getModifierItems().size(); i++) {
                                        if (modifierGroup.getModifierItems().get(i).getAttributes().isSelected()) {
                                            if (!first) {
                                                stringBuilder.append(", ");
                                            } else {
                                                first = false;
                                            }
                                            stringBuilder.append(modifierGroup.getModifierItems().get(i).getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()) + "(" + StringUtil.localize(String.valueOf(modifierGroup.getModifierItems().get(i).getAttributes().getQuantity())) + ")");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        modifierItems.setText(stringBuilder.toString());

        first = true;

        int removedIngredients = 0;

        stringBuilder = new StringBuilder();

        for (Ingredient ingredient : menuItem.getIngredients()) {
            if (!ingredient.getAttributes().isSelected()) {
                removedIngredients += 1;
                if (!first) {
                    stringBuilder.append(", ");
                } else {
                    first = false;
                }
                stringBuilder.append(ingredient.getAttributes().getName());
            }
        }

        if (removedIngredients > 0) {
            ingredients.setText(CacheUtil.getInstance().getTranslations(Text.REMOVE) + " " + stringBuilder.toString());
            ingredients.setVisibility(View.VISIBLE);
        } else {
            ingredients.setVisibility(View.GONE);
        }

        if (menuItem.getListPrice() > 0.0) {
            oldPriceView.setVisibility(View.VISIBLE);
            oldPrice.setVisibility(View.VISIBLE);
            SpannedString formattedOldPrice = StringUtil.getNewFormattedPrice(menuItem.getListPrice());
            SpannableString oldPriceSpan = new SpannableString(formattedOldPrice);
            oldPriceSpan.setSpan(new StrikethroughSpan(), 0, formattedOldPrice.length(), 0);
            oldPrice.setText(oldPriceSpan);
        } else {
            oldPriceView.setVisibility(View.GONE);
            oldPrice.setVisibility(View.GONE);
        }

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                cartPresenter.deleteCartItem(false, getAdapterPosition());
            }
        });

        edit.setVisibility(menuItem.isPromo() ? View.INVISIBLE : View.VISIBLE);

        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cartPresenter.editCartItem(getAdapterPosition());
            }
        });

        if (!CacheUtil.getInstance().getConcept().isCartItemsNotesEnabled()) {
            notesContainer.setVisibility(View.GONE);
        } else {
            notesContainer.setVisibility(View.VISIBLE);
            String itemnote = menuItem.getAttributes().getNotes();
            if (itemnote.isEmpty()) {
                notes.setText("");
            } else {
                notes.setText(itemnote);
            }

            notes.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    cartPresenter.addNotes(getAdapterPosition(), s.toString());
                }
            });
        }


    }

    public String translated(String language, Translated name) {
        if (language.equalsIgnoreCase("en-us")) {
            return name.getEnglish();
        } else {
            return language.equalsIgnoreCase("fr") ? name.getFrench() : name.getArabic();
        }
    }


    private int getYourWayModifier(ArrayList<ModifierGroup> modifierGroups) {
        for (int i = 0; i < modifierGroups.size(); i++) {
            if (modifierGroups.get(i).getAttributes().customFieldObjectMap.label.equalsIgnoreCase("yourway")) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void setTotalPrice(boolean hasPromotion, double subtotalPrice, double discount, double totalPrice, boolean isDelivery, double deliveryCharge, ArrayList<ServiceCharge> charges, double alrajhiAmount) {

    }

    @Override
    public void setOriginalPrice(boolean hasPromotion, double originalPrice) {

    }

    @Override
    public void setTax(boolean inclusive, CharSequence taxAmount, double vatAmount) {

    }

    @Override
    public void deleteCartItem(int position) {

    }

    @Override
    public void editCartItem(int position) {

    }

    @Override
    public void showCoupon(String couponCode, String description) {

    }

    @Override
    public void removeCoupon() {

    }

    @Override
    public void proceed(boolean signIn) {

    }

    @Override
    public void showOrderType(boolean checkout) {

    }

    @Override
    public void showSignIn() {

    }

    @Override
    public void showMessage(String message) {

    }

    @Override
    public void showError(String error) {

    }

    @Override
    public void showValidateOrderError(List<String> errors) {

    }

    @Override
    public void showUnavailableMenuItems() {

    }

    @Override
    public void deleteUnavailableItems(int cartQuantity) {

    }

    @Override
    public void showUpsell() {

    }

    @Override
    public void showPayment() {

    }

    @Override
    public void refreshDigitalCoupons() {

    }

    @Override
    public void showDigitalCoupons(boolean loading, List<Campaign> digitalCoupons, int selected) {

    }

    @Override
    public void showAvailableDigitalCoupons(List<Campaign> digitalCoupons, int selected) {

    }


    @Override
    public void setDigitalCoupon(Campaign campaign, boolean selected) {

    }

    @Override
    public void selectDigitalCoupon(int position) {

    }

    @Override
    public void showDigitalCoupon(Campaign campaign) {

    }

    @Override
    public void addPromotionItem(MenuItem menuItem) {

    }

    @Override
    public void showPromotionItem() {

    }

    @Override
    public void showPromoError(String error) {

    }

    @Override
    public void removeMokafaaView() {

    }

    @Override
    public String getFromCache(String key) {
        return null;
    }

    @Override
    public void validatingOrder(double subtotal) {

    }

    @Override
    public void removeItemEvent(MenuItem menuItem, double totalPrice) {

    }

    @Override
    public void firebaseViewCartEvent() {

    }

    @Override
    public void firebaseCheckoutEvent() {

    }

    @Override
    public void setSchedule(boolean showAlert, String scheduleTime) {

    }
}