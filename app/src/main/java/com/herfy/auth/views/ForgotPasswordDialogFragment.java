package com.herfy.auth.views;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.herfy.R;
import com.herfy.auth.AuthContract;
import com.herfy.auth.AuthPresenter;
import com.herfy.base.BaseDialogFragment;
import com.herfy.data.constants.Text;
import com.skylinedynamics.solosdk.api.models.objects.AllowedCountries;
import com.herfy.util.CacheUtil;
import com.herfy.util.StringUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ForgotPasswordDialogFragment extends BaseDialogFragment implements AuthContract.View {

    private AuthContract.Presenter authPresenter;

    @BindView(R.id.container)
    ConstraintLayout container;
    @BindView(R.id.content)
    ConstraintLayout content;
    @BindView(R.id.phone_number_success_container)
    ConstraintLayout phoneNumberSuccessContainer;

    @BindView(R.id.card)
    CardView card;

    @BindView(R.id.close)
    ImageButton close;

    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.subtitle)
    TextView subtitle;
    //@BindView(R.id.email_address_error) TextView emailAddressError;
    @BindView(R.id.phone_number_error)
    TextView phoneNumberError;
    @BindView(R.id.phone_number_success)
    TextView phoneNumberSuccess;

    //@BindView(R.id.email_address) EditText emailAddress;
    @BindView(R.id.phone_number_prefix)
    Spinner phoneNumberPrefix;
    @BindView(R.id.phone_number)
    EditText phoneNumber;

    @BindView(R.id.submit)
    Button submit;

    private ArrayList<AllowedCountries> allowedCountries = new ArrayList<AllowedCountries>();
    private String selectedDialCode = "";
    private int mobileFormatIndex = -1;

    public static ForgotPasswordDialogFragment newInstance(AuthContract.Presenter authPresenter) {
        ForgotPasswordDialogFragment forgotPasswordDialogFragment = new ForgotPasswordDialogFragment();
        forgotPasswordDialogFragment.authPresenter = authPresenter;
        return forgotPasswordDialogFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authPresenter = new AuthPresenter(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialog_forgot_password, container, false);
        ButterKnife.bind(this, view);
        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authPresenter.start();
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
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        card.startAnimation(animationSet);

    }

    @Override
    public void setPresenter(AuthContract.Presenter presenter) {
        authPresenter = presenter;
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

        /*TextWatcher emailAddressTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                submit.setFocusable(editable.length() > 0);
                submit.setClickable(editable.length() > 0);
                submit.setBackgroundColor(editable.length() > 0 ? Color.parseColor(CacheUtil.getInstance().getColorMain()) : Color.parseColor("#C2C2C2"));
            }
        };

        emailAddress.addTextChangedListener(emailAddressTextWatcher);

        emailAddress.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submit.performClick();
                    InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(emailAddress.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });*/

        if(CacheUtil.getInstance().getConcept().getAllowedCountries() .size() == 1) {
            phoneNumberPrefix.setBackground(null);
            phoneNumberPrefix.setEnabled(false);
        }
        allowedCountries.addAll(CacheUtil.getInstance().getConcept().getAllowedCountries());

        if (selectedDialCode.isEmpty()) selectedDialCode = allowedCountries.get(0).getDialCode();
        if (mobileFormatIndex == -1 && allowedCountries.size() > 0) mobileFormatIndex = 0;

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
                String phoneNumberValue = phoneNumber.getText().toString().trim().replace("+", "");
                if (!phoneNumberValue.isEmpty() && StringUtil.isValidPhoneNumber(StringUtil.toEnglish((selectedDialCode.replace("+", "")+phoneNumberValue.trim())),mobileFormatIndex)) {
                    showErrorPhoneNumber("");
                    submit.setBackgroundColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
                    submit.setEnabled(true);
                } else {
                    showErrorPhoneNumber(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_INVALID, "The phone number you entered is invalid"));
                    submit.setBackgroundColor(Color.parseColor("#C2C2C2"));
                    submit.setEnabled(false);
                }
            }
        };

        phoneNumber.addTextChangedListener(phoneNumberTextWatcher);

        phoneNumber.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submit.performClick();
                    InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(phoneNumber.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //((SignInFragment) getParentFragment()).forgotPassword(emailAddress.getText().toString().trim());
                phoneNumber.setEnabled(false);
                InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(phoneNumber.getWindowToken(), 0);
                String phoneNumberValue = selectedDialCode.replace("+", "") + phoneNumber.getText().toString().trim();
                ((SignInFragment) getParentFragment()).forgotPassword(StringUtil.toEnglish((phoneNumberValue.trim().replace("+", ""))),mobileFormatIndex);
            }
        });

    }

    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {
        title.setText(CacheUtil.getInstance().getTranslations(Text.FORGOT_PASSWORD));
        subtitle.setText(CacheUtil.getInstance().getTranslations(Text.FORGOT_PASSWORD_MOBILE_MESSAGE, "We will send you a new password as soon as you submit your registered phone number."));
        //emailAddress.setHint(CacheUtil.getInstance().getTranslations(Text.ENTER_EMAIL, "Enter Email"));
        phoneNumber.setHint(CacheUtil.getInstance().getTranslations(Text.ENTER_PHONE_NUMBER, "Enter phone number"));
        phoneNumberSuccess.setText(CacheUtil.getInstance().getTranslations(Text.WE_SENT_YOUR_NEW_PASSWORD, "We've sent your new password"));
        submit.setText(CacheUtil.getInstance().getTranslations(Text.SUBMIT));
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

    }

    @Override
    public void showErrorPassword(String error) {
        phoneNumber.setEnabled(true);
        phoneNumberSuccessContainer.setVisibility(View.GONE);
        if (error.isEmpty()) {
            phoneNumberError.setVisibility(View.GONE);
        } else {
            phoneNumberError.setText(error);
            phoneNumberError.setVisibility(View.VISIBLE);
            dismissDialogs();
        }
    }

    @Override
    public void showErrorUsername(String error) {

    }

    @Override
    public void showMessage(String message) {

    }

    @Override
    public void showError(String error) {
        phoneNumber.setEnabled(true);
    }

    @Override
    public void showSuccess(String success) {

    }

    @Override
    public void showErrorFirstName(String error) {

    }

    @Override
    public void showErrorPhoneNumber(String error) {

    }

    @Override
    public void showGuest() {

    }

    @Override
    public void showPhoneNumber(String customerId, String email, String password) {

    }

    @Override
    public void setupCodeBroadcastListener() {

    }

    @Override
    public void showCode(boolean guest, String customerId, String email, String password, String phoneNumber) {

    }

    @Override
    public void showMain() {

    }

    @Override
    public void changeLanguage(String language) {

    }

    @Override
    public void logSigninEvent(String customerId) {

    }

    @Override
    public void logRegistrationEvent(String registrationMethod) {}
}
