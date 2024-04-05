package com.herfy.order.views;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.Rating;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.herfy.util.StringUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class UpdateAddAddressDialogFragment extends BaseDialogFragment implements OrderContract.View {

    private OrderContract.Presenter orderPresenter;

    private OnAction onAction;

    public interface OnAction {
        void onConfirm(String phoneNumber, String note, String label, boolean isSkip, boolean isExistingAddress);
    }

    @BindView(R.id.container)
    ConstraintLayout container;
    @BindView(R.id.content)
    ConstraintLayout content;

    @BindView(R.id.card)
    CardView card;
    @BindView(R.id.phone_number_container)
    CardView phoneNumberContainer;

    @BindView(R.id.close)
    ImageButton close;

    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.phone_number_label)
    TextView phoneNumberLabel;
    @BindView(R.id.phone_number_prefix)
    TextView phoneNumberPrefix;
    @BindView(R.id.phone_number_error)
    TextView phoneNumberError;
    @BindView(R.id.note_label)
    TextView noteLabel;

    @BindView(R.id.phone_number)
    EditText phoneNumber;
    @BindView(R.id.note)
    EditText note;

    @BindView(R.id.save)
    Button save;

    @BindView(R.id.skip)
    Button skip;
    @BindView(R.id.confirm)
    Button confirm;

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

    private boolean isAddressExist = false;

    private static boolean isFromAccount;
    private Address updateAddress = null;
    private android.location.Address saveAddress = null;

    private String labelValue = "";

    public static UpdateAddAddressDialogFragment newInstance(OrderContract.Presenter orderPresenter, Address updateAddress, android.location.Address saveAddress, boolean isFromAccountPage, OnAction onAction) {
        UpdateAddAddressDialogFragment updateAddAddressDialogFragment = new UpdateAddAddressDialogFragment();
        updateAddAddressDialogFragment.orderPresenter = orderPresenter;
        updateAddAddressDialogFragment.updateAddress = updateAddress;
        updateAddAddressDialogFragment.saveAddress = saveAddress;
        isFromAccount = isFromAccountPage;
        updateAddAddressDialogFragment.onAction = onAction;
        return updateAddAddressDialogFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        orderPresenter = new OrderPresenter(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialog_update_add_address, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        setupFonts();
        setupTranslations();
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
        Animation animation = new ScaleAnimation(1f, 0f, 1f, 0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
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
                UpdateAddAddressDialogFragment.super.onDismiss(dialog);
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
        Animation animation = new ScaleAnimation(0f, 1f, 0f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
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

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDismiss(getDialog());
            }
        });

        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissDialogs();
                onAction.onConfirm(phoneNumberPrefix.getText().toString().trim().replace("+", "") + phoneNumber.getText().toString().trim(), "", "", true, isAddressExist);
                InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(note.getWindowToken(), 0);
            }
        });

        String prefix = CacheUtil.getInstance().getConcept().getDialingCode();
        phoneNumberPrefix.setText(StringUtil.localize(prefix.contains("+") ? prefix : "+" + prefix));

        if (AuthUtil.getInstance().isSignedIn()) {
            phoneNumberLabel.setVisibility(View.GONE);
            skip.setVisibility(View.VISIBLE);
            confirm.setVisibility(View.VISIBLE);
            save.setVisibility(View.GONE);
            phoneNumberContainer.setVisibility(View.GONE);
            String key = CacheUtil.getInstance().getConceptKey();
            if(key.equalsIgnoreCase("CQcFAwgLDA0")) {
                phoneNumber.setText(StringUtil.localize(AuthUtil.getInstance().getCustomer().getAttributes().getMobile().replace("+", "").substring(1)));
            } else {
                phoneNumber.setText(StringUtil.localize(AuthUtil.getInstance().getCustomer().getAttributes().getMobile().replace("+", "").replace(prefix, "")));
            }
            noteLabel.setVisibility(View.GONE);
        } else {
            skip.setVisibility(View.GONE);
            confirm.setVisibility(View.GONE);
            save.setVisibility(View.VISIBLE);
            phoneNumberLabel.setVisibility(View.VISIBLE);
            phoneNumberContainer.setVisibility(View.VISIBLE);
            noteLabel.setVisibility(View.VISIBLE);

        }

        TextWatcher phoneNumberTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                phoneNumber.removeTextChangedListener(this);
                phoneNumber.setText(StringUtil.localize(phoneNumber.getText().toString()));
                phoneNumber.setSelection(editable.length());
                phoneNumber.addTextChangedListener(this);
            }
        };

        phoneNumber.addTextChangedListener(phoneNumberTextWatcher);

        if (updateAddress != null) {
            isAddressExist = true;
            note.setText(updateAddress.getAttributes().getInstructions());
        }

        note.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    confirm.performClick();
                    InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(note.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	try {
					onAction.onConfirm(phoneNumberPrefix.getText().toString().trim().replace("+", "") + phoneNumber.getText().toString().trim(), note.getText().toString(), labelValue, false,isAddressExist);
					InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
					inputMethodManager.hideSoftInputFromWindow(note.getWindowToken(), 0);
				} catch (NullPointerException e) {}
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
				try {
					onAction.onConfirm(phoneNumber.getText().toString().trim().replace("+", ""), note.getText().toString(), labelValue, false, isAddressExist);
				} catch (NullPointerException e) {}
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

        if (isFromAccount){ ivHome.performClick(); }
    }

    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {
        title.setText(CacheUtil.getInstance().getTranslations(AuthUtil.getInstance().isSignedIn() ? Text.NOTE_TO_DRIVER_CAPS : Text.SAVE_ADDRESS, AuthUtil.getInstance().isSignedIn() ? "NOTE TO DRIVER" : "SAVE ADDRESS"));
        noteLabel.setText(CacheUtil.getInstance().getTranslations(Text.NOTE_TO_DRIVER, "Note to driver"));
        phoneNumberLabel.setText(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER, "Phone number"));
        note.setHint(CacheUtil.getInstance().getTranslations(Text.NOTE_PLACEHOLDER, "House number, floor, door color, landmarks, cars outside, etc."));
        skip.setText(CacheUtil.getInstance().getTranslations(Text.SKIP));
        //phoneNumber.setHint(StringUtil.localize("eg: 966551234567"));
        save.setText(CacheUtil.getInstance().getTranslations(Text.SAVE, "SAVE").toUpperCase());
        confirm.setText(CacheUtil.getInstance().getTranslations(Text.CONFIRM_CAPS, "CONFIRM"));
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
        if (error.isEmpty()) {
            phoneNumberError.setVisibility(View.GONE);
        } else {
            if (phoneNumberContainer.getVisibility() == View.VISIBLE) {
                phoneNumberError.setText(error);
                phoneNumberError.setVisibility(View.VISIBLE);
            }
        }
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
