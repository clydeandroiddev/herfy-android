package com.herfy.cart.views;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.herfy.R;
import com.herfy.cart.CartContract;
import com.herfy.cart.adapters.CartPromoAdapter;
import com.herfy.data.constants.Text;
import com.herfy.main.MainActivity;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.ServiceCharge;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.herfy.util.CacheUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;

public class CartAvailablePromoDialogFragment extends DialogFragment implements CartContract.View, CartPromoAdapter.CartPromoListener {

    private CartAvailablePromoDialogFragment.OnAction onAction;

    protected CompositeDisposable disposable = new CompositeDisposable();

    private CartPromoAdapter adapter;
    private List<Campaign> campaigns;

    private MaterialButton skip;
    private MaterialButton avail;
    private MaterialButton remove;
    private boolean donTriggerClose;
    private int selected;

    private LinearLayout empty;
    private RecyclerView recyclerView;
    private TextView selectPromo;

    public static CartAvailablePromoDialogFragment newInstance(int selected, List<Campaign> campaigns, OnAction onAction) {
        CartAvailablePromoDialogFragment fragment = new CartAvailablePromoDialogFragment();
        fragment.campaigns = campaigns;
        fragment.onAction = onAction;
        fragment.selected = selected;
        return fragment;
    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.dialog_fragment_cart_promo, container, false);

        AppCompatImageButton close = view.findViewById(R.id.close);

        TextView title = view.findViewById(R.id.title);

        empty = view.findViewById(R.id.empty);
        TextView searchEmptyPromotion = view.findViewById(R.id.search_empty_promotions);
        TextView searchEmptyTextDesc = view.findViewById(R.id.search_empty_text_desc);

        selectPromo = view.findViewById(R.id.select_promo);

        skip = view.findViewById(R.id.skip);
        avail = view.findViewById(R.id.avail);
        remove = view.findViewById(R.id.remove);

        title.setText(CacheUtil.getInstance().getTranslations(Text.PROMOS, "PROMOS").toUpperCase());

        skip.setText(CacheUtil.getInstance().getTranslations(Text.SKIP, "SKIP"));
        avail.setText(CacheUtil.getInstance().getTranslations(Text.AVAIL_THIS_PROMO_CAPS, "AVAIL THIS PROMO"));
        remove.setText(CacheUtil.getInstance().getTranslations(Text.REMOVE_PROMO_CAPS, "REMOVE PROMO"));

        searchEmptyPromotion.setText(CacheUtil.getInstance().getTranslations(Text.NO_PROMO_IS_AVAILABLE, "No promo is available"));

        searchEmptyTextDesc.setText(CacheUtil.getInstance().getTranslations(Text.NO_PROMO_AVAILABLE_MESSAGE, "Sorry, there is no promo available right now, try checking next time."));

        selectPromo.setText(CacheUtil.getInstance().getTranslations(Text.YOU_CAN_CHOOSE_ONE_OF_THE_FOLLOWING_ITEMS_AT_A_DISCOUNTED_PRICE, "You can choose one of the following items at a discounted price"));

        recyclerView = view.findViewById(R.id.recyclerView);

        if (adapter == null) {
            adapter = new CartPromoAdapter(getActivity(), campaigns, this);
            adapter.selected = selected;
        }

        recyclerView.setAdapter(adapter);

        close.setOnClickListener(v -> dismiss());

        skip.setOnClickListener(v -> {
            donTriggerClose = false;
            close.performClick();
            if(CacheUtil.getInstance().getCampaign() != null) {
                onAction.onRemove();
            }
        });

        avail.setOnClickListener(v -> {
            if (adapter.selected != -1) {
                donTriggerClose = true;
                if (CacheUtil.getInstance().getCouponCode() != null) {
                    // TODO: Remove activity reference
                    try {
                        MainActivity activity = (MainActivity) getActivity();
                        if(activity != null) {
                            activity.showPromptAlertDialog("", CacheUtil.getInstance().getTranslations(Text.ONE_DISCOUNT_ONLY_MESSAGE, "You can only apply one discount at a time. Currently applied discount will be removed"), CacheUtil.getInstance().getTranslations(Text.YES_CAPS), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    onAction.onAvail(true, adapter.selected);
                                }
                            }, CacheUtil.getInstance().getTranslations(Text.NO_CAPS), (dialogInterface, i) -> dialogInterface.dismiss());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    onAction.onAvail(false, adapter.selected);
                }
            }
        });

        remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.selected = -1;
                adapter.notifyDataSetChanged();
                skip.setVisibility(View.VISIBLE);
                avail.setVisibility(View.GONE);
                remove.setVisibility(View.GONE);
                onAction.onRemove();
            }
        });

        updateCampaigns(true, selected, campaigns);

        return view;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (!donTriggerClose)
            onAction.onClose();
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                TypedValue typedValue = new TypedValue();
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                dialog.getWindow().setStatusBarColor(typedValue.data);
            }
        }
    }

    @Override
    public void onCancel(@NotNull DialogInterface dialog) {
        onAction.onClose();
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
    public void refreshMenuItems(int position,   boolean fromChangeQuantity) {

    }

    @Override
    public void updateMenuItems(List<MenuItem> menuItems) {

    }

    @Override
    public void setMenuItem(MenuItem menuItem) {

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

    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {

    }

    @Override
    public void select(Campaign campaign, int position) {
        if (adapter.selected == position) {
            adapter.selected = -1;
            skip.setVisibility(View.VISIBLE);
            avail.setVisibility(View.GONE);
            remove.setVisibility(View.GONE);
        } else {
            skip.setVisibility(View.GONE);
            if(CacheUtil.getInstance().getCampaign() != null && CacheUtil.getInstance().getCampaign().getId().equalsIgnoreCase(campaign.getId())) {
                avail.setVisibility(View.GONE);
                remove.setVisibility(View.VISIBLE);
            } else {
                avail.setVisibility(View.VISIBLE);
                remove.setVisibility(View.GONE);
            }
            adapter.selected = position;
        }
        adapter.notifyDataSetChanged();
    }

    public void updateCampaigns(boolean initial, int selected, List<Campaign> campaigns) {
        if(CacheUtil.getInstance().getCampaign() == null) {
            skip.setVisibility(View.VISIBLE);
            avail.setVisibility(View.GONE);
            remove.setVisibility(View.GONE);
        } else {
            skip.setVisibility(View.GONE);
            avail.setVisibility(View.GONE);
            remove.setVisibility(View.VISIBLE);
        }

        if (campaigns != null && campaigns.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
            selectPromo.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            skip.setVisibility(View.VISIBLE);
            avail.setVisibility(View.GONE);
            remove.setVisibility(View.GONE);
        } else {
            empty.setVisibility(View.GONE);
            selectPromo.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        if(!initial) {
            adapter.updateCampaigns(selected, campaigns);
        }

    }

    public interface OnAction {
        void onClose();
        void onAvail(boolean removeCoupon, int selected);
        void onRemove();
    }

}

