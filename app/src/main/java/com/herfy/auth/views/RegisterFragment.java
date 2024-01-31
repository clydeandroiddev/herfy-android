package com.herfy.auth.views;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.addisonelliott.segmentedbutton.SegmentedButton;
import com.addisonelliott.segmentedbutton.SegmentedButtonGroup;
import com.daimajia.androidanimations.library.Techniques;
import com.herfy.R;
import com.herfy.auth.AuthContract;
import com.herfy.auth.AuthPresenter;
import com.herfy.base.BaseActivity;
import com.herfy.base.BaseFragment;
import com.herfy.data.constants.Text;
import com.skylinedynamics.solosdk.api.models.objects.AllowedCountries;
import com.skylinedynamics.solosdk.api.models.objects.LoginOption;
import com.herfy.util.CacheUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RegisterFragment extends BaseFragment implements AuthContract.View {

    private AuthContract.Presenter authPresenter;

    @BindView(R.id.container) LinearLayout container;
    @BindView(R.id.full_name_label) TextView fullNameLabel;
    @BindView(R.id.full_name_error) TextView fullNameError;
    @BindView(R.id.phone_number_label) TextView phoneNumberLabel;
    @BindView(R.id.phone_number_prefix) Spinner phoneNumberPrefix;
    @BindView(R.id.phone_number_error) TextView phoneNumberError;
    @BindView(R.id.username_label) TextView emailAddressLabel;
    @BindView(R.id.username_error) TextView emailAddressError;
    @BindView(R.id.password_label) TextView passwordLabel;
    @BindView(R.id.password_error) TextView passwordError;
    @BindView(R.id.or) TextView or;

    @BindView(R.id.full_name_container) CardView fullNameContainer;
    @BindView(R.id.phone_number_container) CardView phoneNumberContainer;
    @BindView(R.id.username_container) CardView emailAddressContainer;
    @BindView(R.id.password_container) CardView passwordContainer;

    @BindView(R.id.first_name) EditText firstName;
    @BindView(R.id.phone_number) EditText phoneNumber;
    @BindView(R.id.username) EditText emailAddress;
    @BindView(R.id.password) EditText password;

    @BindView(R.id.register) Button register;
    @BindView(R.id.google) CardView google;
    @BindView(R.id.tvGoogle) TextView tvGoogle;

    @BindView(R.id.language_container) ConstraintLayout languageContainer;
    @BindView(R.id.language_segmented) SegmentedButtonGroup languageSegmented;


    @BindView(R.id.linearLayout2) LinearLayout linearLayout2;
    @BindView(R.id.ivHidePassword) ImageView ivHidePassword;
    @BindView(R.id.french)
    SegmentedButton french;
    @BindView(R.id.arabic)
    SegmentedButton arabic;
    @BindView(R.id.english)
    SegmentedButton english;

    private LoginOption loginOption;

    private boolean isPasswordHidden = true;

    private boolean firstAnimate = true;

    private ArrayList<AllowedCountries> allowedCountries = new ArrayList<>();
    private String selectedDialCode = "";
    private int mobileFormatIndex = -1;

    public RegisterFragment() {

    }

    public static RegisterFragment newInstance() {
        return new RegisterFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        authPresenter = new AuthPresenter(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_register, container, false);

        ButterKnife.bind(this, view);

        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loginOption = CacheUtil.getInstance().getConcept().getLoginOption();
        authPresenter.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(firstAnimate) {
            firstAnimate = false;
            container.setVisibility(View.VISIBLE);
            try {
                BaseActivity activity = (BaseActivity) getActivity();
                if (activity != null) {
                    activity.animateSpringView(Techniques.SlideInUp, 300, fullNameLabel, 8);
                    activity.animateSpringView(Techniques.SlideInUp, 350, fullNameContainer, 8);
                    activity.animateSpringView(Techniques.SlideInUp, 400, phoneNumberLabel, 8);
                    activity.animateSpringView(Techniques.SlideInUp, 450, phoneNumberContainer, 8);
                    if (loginOption == LoginOption.EMAIL) {
                        activity.animateSpringView(Techniques.SlideInUp, 500, emailAddressLabel, 8);
                        activity.animateSpringView(Techniques.SlideInUp, 550, emailAddressContainer, 8);
                    }
                    activity.animateSpringView(Techniques.SlideInUp, 600, passwordLabel, 8);
                    activity.animateSpringView(Techniques.SlideInUp, 650, passwordContainer, 8);
                    activity.animateSpringView(Techniques.SlideInUp, 750, register, 8);
                    activity.animateSpringView(Techniques.SlideInUp, 900, languageContainer, 8);
                    String socialLoginOption = CacheUtil.getInstance().getConcept().getSocialLoginOption();
                    if(socialLoginOption != null && !socialLoginOption.isEmpty() && !socialLoginOption.equalsIgnoreCase("apple")) {
                        activity.animateSpringView(Techniques.SlideInUp, 950, or, 8);
                        if (socialLoginOption.contains("google")){
                            activity.animateSpringView(Techniques.SlideInUp, 1000, google, 8);
                        } else google.setVisibility(View.GONE);
                    } else {
                        or.setVisibility(View.GONE);
                        google.setVisibility(View.GONE);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void setPresenter(AuthContract.Presenter presenter) {
        authPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {
        ((AuthFragment) getParentFragment()).logEvent(eventName, eventAttributes, eventMetricName, eventMetricValue);
    }

    @Override
    public void setCartExpiry() {

    }

    @Override
    public void setupViews() {

        String secondaryLanguage = CacheUtil.getInstance().getConcept().getSecondaryLanguage();

        if (secondaryLanguage == null || secondaryLanguage.isEmpty()) {
            languageContainer.setVisibility(View.INVISIBLE);
        }

        if(CacheUtil.getInstance().getConcept().getSocialLoginOption().contains("google")) {
            or.setVisibility(View.VISIBLE);
            google.setVisibility(View.VISIBLE);
        }

        if (loginOption == LoginOption.EMAIL) {
            emailAddressContainer.setVisibility(View.VISIBLE);
            emailAddressLabel.setVisibility(View.VISIBLE);
        } else {
            emailAddressContainer.setVisibility(View.GONE);
            emailAddressLabel.setVisibility(View.GONE);
        }

        if(CacheUtil.getInstance().getConcept().getAllowedCountries().size() == 1) {
            phoneNumberPrefix.setBackground(null);
            phoneNumberPrefix.setEnabled(false);
        }
        allowedCountries.addAll(CacheUtil.getInstance().getConcept().getAllowedCountries());

        if(selectedDialCode.isEmpty()) selectedDialCode = allowedCountries.get(0).getDialCode();
        if(mobileFormatIndex == -1 && allowedCountries.size() > 0) mobileFormatIndex = 0;

        phoneNumberPrefix.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDialCode = allowedCountries.get(position).getDialCode();
                mobileFormatIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        AllowedCountriesAdapter adapter = new AllowedCountriesAdapter(requireContext(), allowedCountries);
        phoneNumberPrefix.setAdapter(adapter);

        phoneNumber.setFilters(new InputFilter[] { new InputFilter.LengthFilter(13 - selectedDialCode.replace("+", "").length()) });

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

        TextWatcher passwordTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.toString().isEmpty()){
                    linearLayout2.setVisibility(View.GONE);
                    ivHidePassword.setVisibility(View.GONE);
                } else {
                    linearLayout2.setVisibility(View.VISIBLE);
                    ivHidePassword.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                password.removeTextChangedListener(this);
                password.setText(StringUtil.localize(password.getText().toString()));
                password.setSelection(editable.length());
                password.addTextChangedListener(this);
            }
        };

        phoneNumber.addTextChangedListener(phoneNumberTextWatcher);
        password.addTextChangedListener(passwordTextWatcher);

        ivHidePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPasswordHidden) {
                    isPasswordHidden = false;
                    password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    ivHidePassword.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.ic_eye));
                } else {
                    isPasswordHidden = true;
                    ivHidePassword.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.ic_eye_hidden));
                    password.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }
        });

        /*String key = CacheUtil.getInstance().getConceptKey();
        LoginOption loginOption = CacheUtil.getInstance().getConcept().getLoginOption();
        if ((loginOption == LoginOption.MOBILE_NUMBER && BuildConfig.FEATURE_PHASE >= 1.3f) || key.equalsIgnoreCase("CQcFAwgLDA0")) {
            emailAddress.setText(StringUtil.generateRandomEmail());
            emailAddressLabel.setVisibility(View.GONE);
            emailAddressContainer.setVisibility(View.GONE);
        }*/

//        confirmPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
//                if(i == EditorInfo.IME_ACTION_DONE) {
//                    register.performClick();
//                    InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
//                    inputMethodManager.hideSoftInputFromWindow(confirmPassword.getWindowToken(), 0);
//                    return true;
//                }
//                return false;
//            }
//        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = "";
                if(loginOption == LoginOption.EMAIL) {
                    username = emailAddress.getText().toString();
                }
                ((AuthFragment) getParentFragment()).signUp(firstName.getText().toString().trim(), StringUtil.toEnglish(selectedDialCode + phoneNumber.getText().toString().trim()), username, password.getText().toString().trim(), mobileFormatIndex);
            }
        });

//        facebook.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                ((AuthActivity) getActivity()).signInFacebook();
//            }
//        });

        google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((AuthFragment) getParentFragment()).signInGoogle();
            }
        });

        ArrayList<String> languages = new ArrayList<>(CacheUtil.getInstance().getConcept().getLanguages());
        if(languages.size() > 0){
            for(String lang : languages){
                if(lang.equalsIgnoreCase(LocaleUtil.FR)){
                    french.setVisibility(View.VISIBLE);
                }else if(lang.equalsIgnoreCase(LocaleUtil.EN_US)){
                    english.setVisibility(View.VISIBLE);
                } else if(lang.equalsIgnoreCase(LocaleUtil.AR_SA)){
                    arabic.setVisibility(View.VISIBLE);
                }
            }
        }

        int pos = 2;
        if(LocaleUtil.getInstance().isArabic()){
            pos = 0;
        }else if(LocaleUtil.getInstance().isFrench()){
            pos = 1;
        }

        languageSegmented.setPosition(pos, true);

        languageSegmented.setOnPositionChangedListener(new SegmentedButtonGroup.OnPositionChangedListener() {
            @Override
            public void onPositionChanged(int position) {
                SegmentedButton selected =    languageSegmented.getButton(position);
                if(selected == french){
                    if(!LocaleUtil.getInstance().isFrench()) {
                        ((AuthFragment) getParentFragment()).changeLanguage("fr");
                    }
                }else if(selected == english){
                    if(!LocaleUtil.getInstance().isEnglish()) {
                        ((AuthFragment) getParentFragment()).changeLanguage("en");
                    }
                }else if(selected == arabic){
                    if(!LocaleUtil.getInstance().isArabic()) {
                        ((AuthFragment) getParentFragment()).changeLanguage("ar");
                    }
                }
            }
        });

    }

    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {
        firstName.setHint(CacheUtil.getInstance().getTranslations(Text.ENTER_YOUR_FIRSTNAME_AND_LASTNAME, "Enter your first name and last name"));
        fullNameLabel.setText(CacheUtil.getInstance().getTranslations(Text.FULL_NAME, "FULL NAME"));
        phoneNumberLabel.setText(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER));
        emailAddressLabel.setText(CacheUtil.getInstance().getTranslations(Text.EMAIL_ADDRESS));
        passwordLabel.setText(CacheUtil.getInstance().getTranslations(Text.PASSWORD));
        password.setHint( CacheUtil.getInstance().getTranslations(Text.PLEASE_TYPE_YOUR_PASSWORD_HERE, "Please type your password here"));
        register.setText(CacheUtil.getInstance().getTranslations(Text.REGISTER, "REGISTER"));
        or.setText(CacheUtil.getInstance().getTranslations(Text.OR));
        tvGoogle.setText(CacheUtil.getInstance().getTranslations(Text.SIGN_IN_WITH_GOOGLE));
    }

    @Override
    public void signInEmail(String email, String password, int mobileFormatIndex) {

    }

    @Override
    public void forgotPassword(String phoneNumber, int mobileFormatIndex) {

    }

    @Override
    public void signInFacebook() {

    }

    @Override
    public void signInGoogle() {

    }

    @Override
    public void signUp(String fullName, String phoneNumber, String email, String password, int index) {

    }

    @Override
    public void showErrorEmail(String error) {
		try {
			if(error.isEmpty()){
				emailAddressError.setVisibility(View.GONE);
			} else {
				emailAddressError.setText(error);
				emailAddressError.setVisibility(View.VISIBLE);
			}
		} catch (NullPointerException e) {}
    }

    @Override
    public void showErrorPassword(String error) {
		try {
			if(error.isEmpty()){
				passwordError.setVisibility(View.GONE);
			} else {
				passwordError.setText(error);
				passwordError.setVisibility(View.VISIBLE);
			}
		} catch (NullPointerException e) {}
    }

    @Override
    public void showErrorUsername(String error) {
        try {
            if(error.isEmpty()){
                emailAddressError.setVisibility(View.GONE);
            } else {
                emailAddressError.setText(error);
                emailAddressError.setVisibility(View.VISIBLE);
            }
        } catch (NullPointerException e) {}
    }

    @Override
    public void showMessage(String message) {

    }

    @Override
    public void showError(String error) {

    }

    @Override
    public void showSuccess(String success) {

    }

    @Override
    public void showErrorFirstName(String error) {
    	try {
			if (error.isEmpty()) {
                fullNameError.setVisibility(View.GONE);
			} else {
                fullNameError.setText(error);
                fullNameError.setVisibility(View.VISIBLE);
			}
		} catch (NullPointerException e) {}
    }

    @Override
    public void showErrorPhoneNumber(String error) {
		try {
			if(error.isEmpty()){
				phoneNumberError.setVisibility(View.GONE);
			} else {
				phoneNumberError.setText(error);
				phoneNumberError.setVisibility(View.VISIBLE);
			}
		} catch (NullPointerException e) {}
    }

    @Override
    public void showGuest() {

    }

    @Override
    public void showPhoneNumber(String customerId, String email, String password) {
        ((AuthFragment) getParentFragment()).showPhoneNumber(customerId, email, password);
    }

    @Override
    public void setupCodeBroadcastListener() {

    }

    @Override
    public void showCode(boolean guest, String customerId, String email, String password, String phoneNumber) {
        ((AuthFragment) getParentFragment()).showCode(guest, customerId, email, password, phoneNumber);
    }

    @Override
    public void showMain() {
        ((AuthFragment) getParentFragment()).showMain();
    }

    @Override
    public void changeLanguage(String language) {

    }

    @Override
    public void logSigninEvent(String customerId) {

    }

    @Override
    public void logRegistrationEvent(String registrationMethod) { }

}
