package com.herfy.country;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.herfy.BuildConfig;
import com.herfy.R;
import com.herfy.base.BaseDialogFragment;
import com.herfy.data.constants.Text;
import com.skylinedynamics.solosdk.api.SoloApi;
import com.skylinedynamics.solosdk.api.SoloAuth;
import com.herfy.util.CacheUtil;
import com.herfy.util.EnvUtil;
import com.herfy.utils.helper.LocaleUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CountriesDialogFragment extends BaseDialogFragment implements CountriesContract.View, ConfirmConceptSelectionDialog.OnDialogAction {

    private OnAction onAction;

    @BindView(R.id.card)
    ConstraintLayout card;
    @BindView(R.id.left_layout)
    LinearLayout leftLayout;
    @BindView(R.id.left_label)
    TextView leftLabel;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.confirm)
    MaterialButton confirm;
    @BindView(R.id.countries)
    RecyclerView countries;

    private SupportedCountry country;

    private CountriesContract.Presenter mainPresenter;
    private boolean hideClose;
    private int size;
    private boolean onRestart;
    private CountriesAdapterNew countriesAdapter;
    private ConfirmConceptSelectionDialog confirmConceptSelectionDialog;

    public static CountriesDialogFragment newInstance(OnAction onAction, int size, boolean hideClose) {
        CountriesDialogFragment countriesDialogFragment = new CountriesDialogFragment();
        countriesDialogFragment.onAction = onAction;
        countriesDialogFragment.size = size;
        countriesDialogFragment.hideClose = hideClose;
        return countriesDialogFragment;
    }

    public static CountriesDialogFragment newInstance(OnAction onAction, int size) {
        return newInstance(onAction, size, false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_country_selector, container, false);
        ButterKnife.bind(this, view);
        mainPresenter = new CountriesPresenter(this);
        mainPresenter.start();
        setupViews();
        setupTranslations();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
            TypedValue typedValue = new TypedValue();
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            dialog.getWindow().setStatusBarColor(typedValue.data);
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        onAction.onConfirm(countriesAdapter.getSelectedCountry(), onRestart);
    }

    @Override
    public void setPresenter(CountriesContract.Presenter presenter) {

    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {

    }

    @Override
    public void setCartExpiry() {

    }

    public void setupViews() {

        List<SupportedCountry> countriesList = new ArrayList<>();
        Activity activity = getActivity();
        if(activity != null) {
            countriesList = CacheUtil.getSupportedCountries(BuildConfig.APPLICATION_ID, getActivity());
        }

        country = CacheUtil.getInstance().getCountry();
        if (country == null) {
            for (int i = 0; i < countriesList.size(); i++) {
                if (CacheUtil.getInstance().getApplication().getAttributes().getKey().equalsIgnoreCase(countriesList.get(i).applicationKey)) {
                    country = countriesList.get(i);
                    CacheUtil.getInstance().setCountry(country);
                }
            }
        }
        if (countriesAdapter == null) {
            countriesAdapter = new CountriesAdapterNew(size, countriesList, country);
            countries.setAdapter(countriesAdapter);
        }

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean proccedChange = false;
                country = countriesAdapter.getSelectedCountry();
                if (CacheUtil.getInstance().getCountry() != null) {
                    if (!country.applicationId.equalsIgnoreCase(CacheUtil.getInstance().getCountry().applicationId)) {
                        proccedChange = true;
                    }
                } else {
                    proccedChange = !country.applicationId.equalsIgnoreCase(BuildConfig.APPLICATION_ID);
                }

                if (proccedChange) {
                    if (CacheUtil.getInstance().getCartQuantity() != 0) {
                        if (confirmConceptSelectionDialog != null) {
                            confirmConceptSelectionDialog.dismiss();
                        }
                        confirmConceptSelectionDialog = ConfirmConceptSelectionDialog.Companion.newInstance();
                        confirmConceptSelectionDialog.show(getChildFragmentManager(), ConfirmConceptSelectionDialog.class.getSimpleName());
                    } else {
                        onYes();
                    }
                } else {
                    onRestart = false;
                    onDismiss(requireDialog());
                }
            }
        });

        if (hideClose) {
            leftLayout.setVisibility(View.INVISIBLE);
            leftLayout.setOnClickListener(null);
        } else {
            leftLayout.setVisibility(View.VISIBLE);
            leftLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onAction.onClose();
                }
            });
        }

    }

    @Override
    public void setupFonts() {

    }

    public void setupTranslations() {
        title.setText(CacheUtil.getInstance().getTranslations(Text.RESTAURANT_CAPS, "RESTAURANT").toUpperCase());
        leftLabel.setText(CacheUtil.getInstance().getTranslations(Text.CLOSE, "Close"));
        confirm.setText(CacheUtil.getInstance().getTranslations(Text.CONFIRM_CAPS, "CONFIRM"));
    }

    @Override
    public void proceedRestart() {
        onRestart = true;
        onDismiss(requireDialog());
    }

    @Override
    public void preloadImage(String imageUrl, RequestOptions requestOptions) {

    }

    @Override
    public void onYes() {
        onAction.showLoading();
        mainPresenter.setOldCountry(CacheUtil.getInstance().getCountry());
        CacheUtil.getInstance().setCountry(country);
        CacheUtil.getInstance().setApplicationKey(country.applicationKey);
        CacheUtil.getInstance().setConceptKey(country.applicationConcept);
        SoloApi.init(requireActivity(), EnvUtil.getEnvironmentUrl(), CacheUtil.getInstance().getApplicationKey(), CacheUtil.getInstance().getConceptKey());
        SoloAuth.getInstance().setLanguage(LocaleUtil.getInstance().getLanguageSymbol());
        SoloApi.getInstance().setDebug(BuildConfig.BUILD_TYPE.equals("debug"));
        mainPresenter.getApplication();
    }

    @Override
    public void onBack() {
        if (confirmConceptSelectionDialog != null) {
            confirmConceptSelectionDialog.dismiss();
        }
    }

    public interface OnAction {
        void onConfirm(SupportedCountry country, boolean restart);

        void showLoading();

        void onClose();
    }

}

