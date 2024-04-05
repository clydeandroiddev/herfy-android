package com.herfy.order.views;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.tabs.TabLayout;
import com.herfy.R;
import com.herfy.base.BaseDialogFragment;
import com.herfy.utils.constants.Text;
import com.herfy.order.OrderContract;
import com.herfy.order.OrderPresenter;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.Rating;
import com.skylinedynamics.solosdk.api.models.objects.Store;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BookmarkAddressDialogFragment extends BaseDialogFragment implements OrderContract.View {

    private OrderContract.Presenter orderPresenter;

    private OnAction onAction;

    public interface OnAction {
        void onSave(String label);
    }

    @BindView(R.id.container) ConstraintLayout container;
    @BindView(R.id.content) ConstraintLayout content;

    @BindView(R.id.card) CardView card;

    @BindView(R.id.title) TextView title;

    @BindView(R.id.cancel) Button cancel;
    @BindView(R.id.save) Button save;

    @BindView(R.id.ivHome)
    ImageView ivHome;

    @BindView(R.id.ivWork)
    ImageView ivWork;

    @BindView(R.id.ivFriendsHouse)
    ImageView ivFriendsHouse;

    @BindView(R.id.ivHangout)
    ImageView ivHangout;

    @BindView(R.id.tvHome)
    TextView tvHome;

    @BindView(R.id.tvWork)
    TextView tvWork;

    @BindView(R.id.tvFriendsHouse)
    TextView tvFriendsHouse;

    @BindView(R.id.tvHangout)
    TextView tvHangout;

    private String labelValue = "";
    private Address address = null;

    public static BookmarkAddressDialogFragment newInstance(OrderContract.Presenter orderPresenter, Address address, OnAction onAction) {
        BookmarkAddressDialogFragment addressDialogFragment = new BookmarkAddressDialogFragment();
        addressDialogFragment.orderPresenter = orderPresenter;
        addressDialogFragment.address = address;
        addressDialogFragment.onAction = onAction;
        return addressDialogFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        orderPresenter = new OrderPresenter(this);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_dialog_bookmark_address, container, false);

        ButterKnife.bind(this, view);

        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        orderPresenter.start();
    }

    @Override
    public @NotNull Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(getActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                onDismiss(getDialog());
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                TypedValue typedValue = new TypedValue();
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                dialog.getWindow().setStatusBarColor(typedValue.data);
            }
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {

        AnimationSet animationSet = new AnimationSet(true);
        Animation animation = new ScaleAnimation(1f, 0f, 1f, 0f, Animation.RELATIVE_TO_SELF,0.5f, Animation.RELATIVE_TO_SELF,0.5f);
        animation.setDuration(300);
        animation.setFillBefore(true);
        animation.setFillAfter(true);
        animationSet.addAnimation(animation);

        animation = new AlphaAnimation(1f, 0f);
        animation.setDuration(300);
        animation.setFillBefore(true);
        animation.setFillAfter(true);
        animationSet.addAnimation(animation);

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                dialog.dismiss();
                BookmarkAddressDialogFragment.super.onDismiss(dialog);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        card.startAnimation(animationSet);

    }

    @Override
    public void setPresenter(OrderContract.Presenter presenter) {
        orderPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {

    }

    @Override
    public void setCartExpiry() {

    }

    @Override
    public void setupViews() {

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDismiss(getDialog());
            }
        });

        AnimationSet animationSet = new AnimationSet(true);
        Animation animation = new ScaleAnimation(0f, 1f, 0f, 1f, Animation.RELATIVE_TO_SELF,0.5f, Animation.RELATIVE_TO_SELF,0.5f);
        animation.setDuration(300);
        animation.setFillBefore(true);
        animation.setFillAfter(true);
        animationSet.addAnimation(animation);

        animation = new AlphaAnimation(0f, 1f);
        animation.setDuration(300);
        animation.setFillBefore(true);
        animation.setFillAfter(true);
        animationSet.addAnimation(animation);

        card.startAnimation(animationSet);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDismiss(getDialog());
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAction.onSave(labelValue);
            }
        });
        ivHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                labelValue = "Home";
                ivHome.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
                ivWork.setColorFilter(R.color.black);
                ivFriendsHouse.setColorFilter(R.color.black);
                ivHangout.setColorFilter(R.color.black);
                ivHome.setBackground(AppCompatResources.getDrawable(requireContext(),R.drawable.background_circle_selected));
                ivWork.setBackground(AppCompatResources.getDrawable(requireContext(),R.drawable.background_circle_unselected));
                ivFriendsHouse.setBackground(AppCompatResources.getDrawable(requireContext(),R.drawable.background_circle_unselected));
                ivHangout.setBackground(AppCompatResources.getDrawable(requireContext(),R.drawable.background_circle_unselected));
            }
        });
        ivWork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                labelValue = "Work";
                ivWork.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
                ivHome.setColorFilter(R.color.black);
                ivFriendsHouse.setColorFilter(R.color.black);
                ivHangout.setColorFilter(R.color.black);
                ivWork.setBackground(AppCompatResources.getDrawable(requireContext(),R.drawable.background_circle_selected));
                ivHome.setBackground(AppCompatResources.getDrawable(requireContext(),R.drawable.background_circle_unselected));
                ivFriendsHouse.setBackground(AppCompatResources.getDrawable(requireContext(),R.drawable.background_circle_unselected));
                ivHangout.setBackground(AppCompatResources.getDrawable(requireContext(),R.drawable.background_circle_unselected));
            }
        });
        ivFriendsHouse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                labelValue = "Friends House";
                ivFriendsHouse.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
                ivHome.setColorFilter(R.color.black);
                ivWork.setColorFilter(R.color.black);
                ivHangout.setColorFilter(R.color.black);
                ivFriendsHouse.setBackground(AppCompatResources.getDrawable(requireContext(),R.drawable.background_circle_selected));
                ivHome.setBackground(AppCompatResources.getDrawable(requireContext(),R.drawable.background_circle_unselected));
                ivWork.setBackground(AppCompatResources.getDrawable(requireContext(),R.drawable.background_circle_unselected));
                ivHangout.setBackground(AppCompatResources.getDrawable(requireContext(),R.drawable.background_circle_unselected));
            }
        });
        ivHangout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                labelValue = "Hangout";
                ivHangout.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
                ivHome.setColorFilter(R.color.black);
                ivWork.setColorFilter(R.color.black);
                ivFriendsHouse.setColorFilter(R.color.black);
                ivHangout.setBackground(AppCompatResources.getDrawable(requireContext(),R.drawable.background_circle_selected));
                ivHome.setBackground(AppCompatResources.getDrawable(requireContext(),R.drawable.background_circle_unselected));
                ivWork.setBackground(AppCompatResources.getDrawable(requireContext(),R.drawable.background_circle_unselected));
                ivFriendsHouse.setBackground(AppCompatResources.getDrawable(requireContext(),R.drawable.background_circle_unselected));
            }
        });
        ivHome.performClick();
    }



    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {
        title.setText(CacheUtil.getInstance().getTranslations(Text.BOOKMARK_THIS_ADDRESS, "Bookmark this address"));
        cancel.setText(CacheUtil.getInstance().getTranslations(Text.CANCEL, "CANCEL"));
        save.setText(CacheUtil.getInstance().getTranslations(Text.SAVE_NOW_CAPS, "SAVE NOW"));
        tvHome.setText(CacheUtil.getInstance().getTranslations(Text.HOME, "Home"));
        tvWork.setText(CacheUtil.getInstance().getTranslations(Text.WORK, "Work"));
        tvFriendsHouse.setText(CacheUtil.getInstance().getTranslations(Text.FRIENDS_HOUSE, "Friends House"));
        tvHangout.setText(CacheUtil.getInstance().getTranslations(Text.HANGOUT, "Hangout"));
    }

    @Override
    public void showOrderTypes(Set<OrderType> orderTypes) {

    }

    @Override
    public void addToCartFavorite(MenuItem menuItem) {

    }

    @Override
    public void createOrderType(OrderType orderType) {

    }

    @Override
    public void selectOrderType(OrderType orderType) {

    }

    @Override
    public TabLayout.Tab createTab(String tag, String text, Drawable icon) {
        return null;
    }

    @Override
    public void initializeLocation() {

    }

    @Override
    public void getLocation() {

    }

    @Override
    public void showLocation(boolean isPlace, LatLng latLng, String address) {

    }

    @Override
    public void showAddresses(ArrayList<Address> bookmarkedAddresses, ArrayList<Address> otherAddresses) {

    }

    @Override
    public void setAddress(Address address, boolean selected) {

    }

    @Override
    public void selectAddressHome(Address address, boolean redownloadMenu) {

    }

    @Override
    public void selectAddressWork(int position) {

    }

    @Override
    public void selectAddressOther(Address address, boolean redownloadMenu) {

    }

    @Override
    public void updateAddAddress(@Nullable Address updateAddress, @Nullable android.location.Address saveAddress) {

    }

    @Override
    public void showStores(ArrayList<Store> pickupStores, ArrayList<Store> curbsideStores) {

    }

    @Override
    public void setStore(Store store, boolean selected) {

    }

    @Override
    public void callStore(Store store) {

    }

    @Override
    public void selectStore(int position, Store store, boolean initialLoad) {

    }

    @Override
    public void showStore(boolean markerClicked, int position) {

    }

    @Override
    public void showStore(boolean open, Store store, String error) {

    }

    @Override
    public void showOrderType(OrderType orderType) {

    }

    @Override
    public void selectDelivery() {

    }

    @Override
    public void selectPickup() {

    }

    @Override
    public void selectDineIn() {

    }

    @Override
    public void selectCurbside() {

    }

    @Override
    public void saveAddress(String address) {

    }

    @Override
    public void saveAddress(Address address) {

    }

    @Override
    public void updateAddress(String address) {

    }

    @Override
    public void logFirebaseEvent(String event) {

    }

    @Override
    public void showUnavailableMenuItems() {

    }

    @Override
    public void deleteUnavailableItems() {

    }

    @Override
    public void showPhoneNumberError(String error) {

    }

    @Override
    public void showError(String error) {

    }

    @Override
    public void updateMap(OrderType orderType, LatLng startLocation, LatLng endLocation, LatLngBounds bounds) {

    }

    @Override
    public void showOrder(OrderDetails order) {

    }

    @Override
    public void showOrderStatuses(ArrayList<OrderStatus> orderStatuses) {

    }

    @Override
    public void setTimeLine(OrderStatus orderStatus) {

    }

    @Override
    public void showStatus(OrderStatus orderStatus) {

    }

    @Override
    public void showDriver(LatLng latLng) {

    }

    @Override
    public void updateDriver(LatLng latLng) {

    }

    @Override
    public void showMessage(String message) {

    }

    @Override
    public void showRatings(OrderStatus orderStatus, List<Rating> ratings) {

    }

    @Override
    public void showOrders() {

    }

    @Override
    public void showMenuItem(String categoryId, String menuItemId) {

    }

    @Override
    public void enableConfirmButton(boolean enable, String storeName) {

    }

    @Override
    public void saveToCache(String key, String value) {

    }

    @Override
    public void preloadImage(String imageUrl, RequestOptions requestOptions) {

    }

    @Override
    public void diffMenuSaved(Address address) {

    }

    @Override
    public void showPromptChangeOrderType(Address address) {

    }

    @Override
    public void showMinimumDeliveryForAreaMessage(String error) {

    }

    @Override
    public void setCloseTime(String time) {

    }

}
