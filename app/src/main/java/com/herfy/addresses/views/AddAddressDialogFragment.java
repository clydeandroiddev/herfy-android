package com.herfy.addresses.views;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.herfy.R;
import com.herfy.addresses.AddressesContract;
import com.herfy.addresses.AddressesPresenter;
import com.herfy.base.BaseActivity;
import com.herfy.base.BaseDialogFragment;
import com.herfy.data.constants.Text;
import com.herfy.font.FontHandler;
import com.herfy.util.AuthUtil;
import com.herfy.util.BuildUtil;
import com.herfy.util.CacheUtil;
import com.herfy.util.StringUtil;
import com.herfy.views.CustomTypefaceSpan;
import com.skylinedynamics.solosdk.api.models.objects.Address;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AddAddressDialogFragment extends BaseDialogFragment implements AddressesContract.View {

    private AddressesContract.Presenter addressesPresenter;

    private OnSubmit onSubmit;

    public interface OnSubmit {
        void onSubmit(Dialog dialog, String phoneNumber, String label, String note);
    }

    @BindView(R.id.phone_number) TextInputEditText phoneNumber;
    @BindView(R.id.note) TextInputEditText note;

    @BindView(R.id.label_container) MaterialCardView labelContainer;

    @BindView(R.id.label) TextView label;
    @BindView(R.id.submit) TextView submit;

    private Address address = null;

    public static AddAddressDialogFragment newInstance(OnSubmit onSubmit, Address address) {

        AddAddressDialogFragment addAddressDialogFragment = new AddAddressDialogFragment();
        addAddressDialogFragment.onSubmit = onSubmit;
        addAddressDialogFragment.address = address;

        return addAddressDialogFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addressesPresenter = new AddressesPresenter(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_dialog_add_address, container, false);

        ButterKnife.bind(this, view);

        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        addressesPresenter.start();
    }

    @Override
    public void setPresenter(AddressesContract.Presenter presenter) {
        addressesPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {
        if(getActivity() != null) {
            ((BaseActivity) getActivity()).logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
        }
    }

    @Override
    public void setCartExpiry() {

    }

    @Override
    public void setupViews() {

        phoneNumber.setText("");
        note.setText("");

        if(address != null) {
            phoneNumber.append(StringUtil.localize(address.getAttributes().getTelephone()));

            String labelValue = address.getAttributes().getLabel();
            if(labelValue.equalsIgnoreCase("Home") || labelValue.equalsIgnoreCase("الصفحة الرئيسية")) {
                label.setText(CacheUtil.getInstance().getTranslations(Text.HOME));
            } else if(labelValue.equalsIgnoreCase("Work") || labelValue.equalsIgnoreCase("عمل")) {
                label.setText(CacheUtil.getInstance().getTranslations(Text.WORK));
            } else {
                label.setText(CacheUtil.getInstance().getTranslations(Text.OTHER));
            }

            note.append(address.getAttributes().getInstructions());
        } else if(AuthUtil.getInstance().isSignedIn()) {
            label.setText(CacheUtil.getInstance().getTranslations(Text.HOME));
            //For Burgerizzr
            if(BuildUtil.soloAppKey().equalsIgnoreCase("SeBwUS2G9SEnkwty")) {
                phoneNumber.append(StringUtil.localize(CacheUtil.getInstance().getConcept().getDialingCode() + AuthUtil.getInstance().getCustomer().getAttributes().getMobile().substring(1)));
            } else {
                phoneNumber.append(StringUtil.localize(AuthUtil.getInstance().getCustomer().getAttributes().getMobile()));
            }
            note.setText("");
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

        labelContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(getActivity(), labelContainer);

                popup.getMenuInflater().inflate(R.menu.address_label, popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        label.setText(item.getTitle());
                        return true;
                    }
                });

                popup.show();

                Menu menu = popup.getMenu();
                for (int i = 0; i < menu.size(); i++) {
                    MenuItem mi = menu.getItem(i);

                    if(i == 0) {
                        mi.setTitle(CacheUtil.getInstance().getTranslations(Text.HOME));
                    } else if(i == 1) {
                        mi.setTitle(CacheUtil.getInstance().getTranslations(Text.WORK));
                    } else {
                        mi.setTitle(CacheUtil.getInstance().getTranslations(Text.OTHER));
                    }

                    Typeface typeface = FontHandler.getMediumTypeface();

                    SpannableString mNewTitle = new SpannableString(mi.getTitle());
                    mNewTitle.setSpan(new CustomTypefaceSpan("", typeface, ContextCompat.getColor(getActivity(), R.color.primary_text)), 0, mNewTitle.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    mi.setTitle(mNewTitle);

                }
            }
        });

        note.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if(actionId == EditorInfo.IME_ACTION_DONE) {
                    submit.performClick();
                    InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(note.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().dismiss();
                onSubmit.onSubmit(getDialog(), StringUtil.toEnglish(phoneNumber.getText().toString().trim().replace("+", "")), label.getText().toString(), note.getText().toString().trim());
            }
        });

    }

    @Override
    public void setupFonts() {
        phoneNumber.setTypeface(FontHandler.getMediumTypeface());
        label.setTypeface(FontHandler.getMediumTypeface());
        note.setTypeface(FontHandler.getMediumTypeface());
        submit.setTypeface(FontHandler.getSemiboldTypeface());
    }

    @Override
    public void setupTranslations() {
        phoneNumber.setHint(CacheUtil.getInstance().getTranslations(Text.ENTER_PHONE_NUMBER_CAPS));
        label.setHint(CacheUtil.getInstance().getTranslations(Text.HOME));
        note.setHint(CacheUtil.getInstance().getTranslations(Text.LANDMARK_OTHER_INFOS));
        submit.setText(CacheUtil.getInstance().getTranslations(Text.SUBMIT));
    }

    @Override
    public void initializeLocation() {

    }

    @Override
    public void getLocation() {

    }

    @Override
    public void showLocation(LatLng latLng) {

    }

    @Override
    public void showPlaces(ArrayList<AutocompletePrediction> places) {

    }

    @Override
    public void setPlace(int position, AutocompletePrediction place, boolean selected) {

    }

    @Override
    public void selectPlace(int position) {

    }

    @Override
    public void showAddresses(ArrayList<com.skylinedynamics.solosdk.api.models.objects.Address> homeAddresses, ArrayList<com.skylinedynamics.solosdk.api.models.objects.Address> workAddresses, ArrayList<com.skylinedynamics.solosdk.api.models.objects.Address> otherAddresses) {

    }

    @Override
    public void setAddress(com.skylinedynamics.solosdk.api.models.objects.Address address, boolean selected) {

    }

    @Override
    public void showPlace(Place place) {

    }

    @Override
    public void selectAddressHome(int position) {

    }

    @Override
    public void selectAddressWork(int position) {

    }

    @Override
    public void selectAddressOther(int position) {

    }

    @Override
    public void addAddress(Place place, android.location.Address address) {

    }

    @Override
    public void updateAddress(Place place, Address address) {

    }

    @Override
    public void saveAddress(Address address) {

    }

    @Override
    public void showOrderType() {

    }

    @Override
    public void showMessage(String message) {

    }

}
