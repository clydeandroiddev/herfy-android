package com.herfy.verification.views;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputFilter;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.capillary.hydra.android.sdk.HydraAPI;
import com.capillary.hydra.android.sdk.UserPreferences;
import com.herfy.R;
import com.herfy.auth.views.AuthFragment;
import com.herfy.base.BaseDialogFragment;
import com.herfy.utils.constants.Text;
import com.herfy.main.MainActivity;
import com.herfy.order.views.OrderTypeActivity;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.Concept;
import com.herfy.utils.helper.AnimUtil;
import com.herfy.util.StringUtil;
import com.herfy.verification.VerificationContract;
import com.herfy.verification.VerificationPresenter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VerifyCodeDialogFragment extends BaseDialogFragment implements VerificationContract.View, VerifyCodeAdapter.VerifyCodeListener {

    private VerificationContract.Presenter verificationPresenter;

    @BindView(R.id.closeButton)
    ImageView close;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.card)
    CardView card;
    @BindView(R.id.container)
    ConstraintLayout container;
    @BindView(R.id.content)
    ConstraintLayout content;
    @BindView(R.id.code_error)
    TextView codeError;
    @BindView(R.id.count_message)
    TextView countMessage;
    @BindView(R.id.count)
    TextView count;
    @BindView(R.id.count_container)
    LinearLayout countContainer;
    @BindView(R.id.resend_container)
    LinearLayout resendContainer;
    @BindView(R.id.resend_message)
    TextView resendMessage;
    @BindView(R.id.resend)
    TextView resend;
    @BindView(R.id.cancel)
    TextView cancel;
    @BindView(R.id.confirm)
    TextView confirm;
    @BindView(R.id.otp_list)
    RecyclerView otpList;
    @BindView(R.id.spaceTwo)
    View spaceTwo;
    @BindView(R.id.spaceOne)
    View spaceOne;

    @BindView(R.id.phone_number_container)
    ConstraintLayout phoneNumberContainer;
    @BindView(R.id.otp_list_layout)
    ConstraintLayout otpListLayout;
    @BindView(R.id.label)
    TextView label;
    @BindView(R.id.phone_number_prefix)
    TextView phoneNumberPrefix;
    @BindView(R.id.phone_number)
    EditText phoneNumber;
    @BindView(R.id.send_code)
    TextView sendCode;
    @BindView(R.id.buttons_container)
    ConstraintLayout buttonsContainer;

    private List<String> numbers;

    private GridLayoutManager gridLayoutManager;
    private VerifyCodeAdapter adapter;

    private boolean isCode = true;
    private boolean orderHistory = false;
    private boolean cart = false;
    private boolean orderType = false;
    private boolean favoriteHome = false;
    private boolean favoriteMenuItem = false;
    private boolean guest = false;
    private String customerId = "";
    private String email = "";
    private String password = "";
    private String phoneNumberValue = "";
    private int codeLength = 6;
    private CountDownTimer countDownTimer;
    private long countDownTime = 60000;

    public static VerifyCodeDialogFragment newInstance() {
        return new VerifyCodeDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        verificationPresenter = new VerificationPresenter(this);
        Bundle bundle = getArguments();
        if (bundle != null) {
            if (bundle.containsKey("order_history")) {
                orderHistory = bundle.getBoolean("order_history");
            }
            if (bundle.containsKey("cart")) {
                cart = bundle.getBoolean("cart");
            }
            if (bundle.containsKey("order_type")) {
                orderType = bundle.getBoolean("order_type");
            }
            if (bundle.containsKey("favorite_home")) {
                favoriteHome = bundle.getBoolean("favorite_home");
            }
            if (bundle.containsKey("favorite_menu_item")) {
                favoriteMenuItem = bundle.getBoolean("favorite_menu_item");
            }
            if (bundle.containsKey("guest")) {
                guest = bundle.getBoolean("guest");
            }
            if (bundle.containsKey("customer_id")) {
                customerId = bundle.getString("customer_id");
            }
            if (bundle.containsKey("email")) {
                email = bundle.getString("email");
            }
            if (bundle.containsKey("password")) {
                password = bundle.getString("password");
            }
            if (bundle.containsKey("phone_number")) {
                phoneNumberValue = bundle.getString("phone_number");
            }
        }
        isCode = phoneNumberValue != null && !phoneNumberValue.isEmpty();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_otp, container, false);
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
            TypedValue typedValue = new TypedValue();
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            dialog.getWindow().setStatusBarColor(typedValue.data);
        }
    }


    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
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
                VerifyCodeDialogFragment.super.onDismiss(dialog);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        card.startAnimation(animationSet);


    }

    @Override
    public void setPresenter(VerificationContract.Presenter presenter) {
        verificationPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {

    }

    @Override
    public void setCartExpiry() {

    }

    private void setupNumbersAndOTP() {
        Concept concept = CacheUtil.getInstance().getConcept();
        if (concept != null) {
            codeLength = concept.getOtpLength();
            if (codeLength == 0) {
                codeLength = 6;
            }
        }
        if (codeLength < 6) {
            spaceOne.setVisibility(View.VISIBLE);
            spaceTwo.setVisibility(View.VISIBLE);
        } else {
            spaceTwo.setVisibility(View.GONE);
            spaceOne.setVisibility(View.GONE);
        }
        numbers = new ArrayList<>(codeLength);
        for (int i = 0; i < codeLength; i++) {
            numbers.add("");
        }

        gridLayoutManager = new GridLayoutManager(requireContext(), codeLength);
//        if (LocaleUtil.getInstance().isArabic()) {
//            gridLayoutManager.setReverseLayout(true);
//        }
        otpList.setLayoutManager(gridLayoutManager);
        if (adapter == null) {
            adapter = new VerifyCodeAdapter(requireContext(), numbers, this);
        }
        adapter.notifyDataSetChanged();
        otpList.setAdapter(adapter);
    }

    @Override
    public void setupViews() {

       if(!isCode) {
           phoneNumberContainer.setVisibility(View.VISIBLE);
           otpListLayout.setVisibility(View.GONE);
           sendCode.setVisibility(View.VISIBLE);
           buttonsContainer.setVisibility(View.GONE);
       } else {
           phoneNumberContainer.setVisibility(View.GONE);
           otpListLayout.setVisibility(View.VISIBLE);
           sendCode.setVisibility(View.GONE);
           buttonsContainer.setVisibility(View.VISIBLE);
       }

        String prefix = CacheUtil.getInstance().getConcept().getDialingCode();
        phoneNumberPrefix.setText(StringUtil.localize(prefix.contains("+") ? prefix : "+" + prefix));

        phoneNumber.setFilters(new InputFilter[] { new InputFilter.LengthFilter(13 - prefix.replace("+", "").length()) });

        setupNumbersAndOTP();

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

        cancel.setOnClickListener(v -> onDismiss(requireDialog()));

        close.setOnClickListener(v -> onDismiss(requireDialog()));

        phoneNumber.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                try {
                    Activity activity = getActivity();
                    if(activity != null) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            sendCode.performClick();
                            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
                            inputMethodManager.hideSoftInputFromWindow(phoneNumber.getWindowToken(), 0);
                            return true;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

        sendCode.setOnClickListener(view -> {
            phoneNumberValue = (phoneNumberPrefix.getText().toString()).trim().replace("+", "") + phoneNumber.getText().toString().trim();
            verificationPresenter.send(customerId, StringUtil.toEnglish((phoneNumberValue.trim().replace("+", ""))));
        });

        confirm.setOnClickListener(v -> {
            loadDialog();
            if (isCode) {
                if (!guest) {
                    verificationPresenter.verify(customerId, email, password, StringUtil.toEnglish(phoneNumberValue.trim().replace("+", "")), StringUtil.toEnglish(getCode().trim()));
                } else {
                    verificationPresenter.guest(StringUtil.toEnglish(phoneNumberValue.trim().replace("+", "")), StringUtil.toEnglish(getCode().trim()));
                }
            }
        });

        countDownTimer = new CountDownTimer(countDownTime, 1000) {
            public void onTick(long millisUntilFinished) {
                countDownTime = millisUntilFinished;
                count.setText(CacheUtil.getInstance().getTranslations(Text.RESEND_SECONDS, "(x) seconds").replace("(x)", StringUtil.localize(String.valueOf(millisUntilFinished / 1000))));
            }

            public void onFinish() {
                countDownTime = 60000;
                countContainer.setVisibility(View.GONE);
                resendContainer.setVisibility(View.VISIBLE);
            }
        };

        if (isCode) {
            countDownTime = 60000;
            countMessage.setVisibility(View.VISIBLE);
            countContainer.setVisibility(View.VISIBLE);
            resendContainer.setVisibility(View.GONE);
            countDownTimer.start();
        }

        resend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (countContainer.getVisibility() == View.GONE) {
                    loadDialog();
                    resendContainer.setVisibility(View.GONE);
                    verificationPresenter.send(customerId, phoneNumberValue);
                }
            }
        });

    }

    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {
        if(!isCode) {
            title.setText(CacheUtil.getInstance().getTranslations(Text.ENTER_PHONE_NUMBER_CAPS));
        } else {
            title.setText(CacheUtil.getInstance().getTranslations(Text.VERIFY_PHONE_NUMBER_CAPS, "VERIFY PHONE NUMBER"));
        }
        label.setText(CacheUtil.getInstance().getTranslations(Text.WE_WILL_SEND_CODE_MESSAGE, "We will send a code to your number. We need your mobile number to verify your identity and to help use contact you in case of an order query. Standard data charge may apply."));
        sendCode.setText(CacheUtil.getInstance().getTranslations(Text.SEND_CODE_CAPS, "SEND CODE"));
        cancel.setText(CacheUtil.getInstance().getTranslations(Text.CANCEL, "CANCEL"));
        confirm.setText(CacheUtil.getInstance().getTranslations(Text.CONFIRM_CAPS, "CONFIRM"));
        countMessage.setText(CacheUtil.getInstance().getTranslations(Text.YOU_CAN_RESEND_THE_CODE, "You can resend the code in"));
        count.setText(CacheUtil.getInstance().getTranslations(Text.RESEND_SECONDS, "(x) seconds").replace("(x)", StringUtil.localize("60")));
        resendMessage.setText(CacheUtil.getInstance().getTranslations(Text.RESEND_VERIFICATION_CODE, "Didn't get the code?"));
        resend.setText(CacheUtil.getInstance().getTranslations(Text.RESEND, "Resend"));
    }

    @Override
    public void setCode(String codeValue) {
        try {
            Concept concept = CacheUtil.getInstance().getConcept();
            if (concept != null) {
                codeLength = concept.getOtpLength();
                if (codeLength == 0) {
                    codeLength = 6;
                }
            }
            numbers = new ArrayList<>(codeLength);
            for (int i = 0; i < codeLength; i++) {
                numbers.add("");
            }
            for (int i = 0; i < codeValue.length(); i++) {
                numbers.set(i, (codeValue.charAt(i) + ""));
            }
            if (checkIfAllIsFilled()) {
                confirm.performClick();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void showErrorCode(String error) {
        dismissDialogs();
        if (error.isEmpty()) {
            codeError.setVisibility(View.GONE);
        } else {
            codeError.setText(error);
            codeError.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void showErrorPhoneNumber(String error) {
        dismissDialogs();
        if (error.isEmpty()) {
            codeError.setVisibility(View.GONE);
        } else {
            codeError.setText(error);
            codeError.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void showMessage(String message) {
        dismissDialogs();
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showError(String error) {
        dismissDialogs();
        showAlertDialog("", error);
    }

    @Override
    public void showResendError(String error) {
        dismissDialogs();
        showAlertDialog("", error);
        resendContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void showCode() {
        if(!isCode) {
            try {
                AuthFragment authFragment = (AuthFragment) getParentFragment();
                authFragment.setupCodeBroadcastListener();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        isCode = true;
        phoneNumber.setText("");
        try {
            Activity activity = getActivity();
            if(activity != null) {
                Toast.makeText(activity, CacheUtil.getInstance().getTranslations(Text.VERIFICATION_CODE_SENT, "A verification code is sent to your number"), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        phoneNumberContainer.setVisibility(View.GONE);
        otpListLayout.setVisibility(View.VISIBLE);
        sendCode.setVisibility(View.GONE);
        buttonsContainer.setVisibility(View.VISIBLE);
        title.setText(CacheUtil.getInstance().getTranslations(Text.VERIFY_PHONE_NUMBER_CAPS, "VERIFY PHONE NUMBER"));
        countDownTime = 60000;
        countMessage.setVisibility(View.VISIBLE);
        countContainer.setVisibility(View.VISIBLE);
        resendContainer.setVisibility(View.GONE);
        countDownTimer.start();
        dismissDialogs();
    }

    @Override
    public void showVerified() {

        //HYDRA SIGN IN EVENT
        HydraAPI.Companion.getDefaultInstance(requireContext()).
                pushUserSignin(
                        AuthUtil.getInstance().getCustomer().getId(),
                        AuthUtil.getInstance().getCustomer().getAttributes().getFirstName(),
                        AuthUtil.getInstance().getCustomer().getAttributes().getLastName(),
                        AuthUtil.getInstance().getCustomer().getAttributes().getEmail(),
                        AuthUtil.getInstance().getCustomer().getAttributes().getMobile(),
                        Collections.emptyMap());

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            HydraAPI.Companion.getDefaultInstance(requireContext()).passUserPreferences(AuthUtil.getInstance().getCustomer().getAttributes().getMobile(), new UserPreferences(false,false));
        }else{
            HydraAPI.Companion.getDefaultInstance(requireContext()).passUserPreferences(AuthUtil.getInstance().getCustomer().getAttributes().getMobile(), new UserPreferences(true,true));

        }



        onDismiss(getDialog());

        Intent intent = new Intent(getActivity(), MainActivity.class);
        if (!orderType) {
            intent.putExtra("cart", cart);
            /*if (orderHistory) {
                intent = new Intent(getActivity(), OrdersActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            } else*/ if (favoriteHome) {
                intent = new Intent(getActivity(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            } else if (favoriteMenuItem) {
                //intent = new Intent(getActivity(), MenuItemActivity.class);
                intent = new Intent(getActivity(), MainActivity.class);
                intent.putExtra("menu", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            } else {
                intent.putExtra("home", true);
                intent.putExtra("menu", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }
        } else {
            intent = new Intent(getActivity(), OrderTypeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        }
        intent.putExtra("sign_in", true);
        startActivity(intent);
        getActivity().finish();
        getActivity().overridePendingTransition(AnimUtil.inF(), AnimUtil.inS());

    }

    private boolean checkIfAllIsFilled() {
        for (int i = 0; i < numbers.size(); i++) {
            if (numbers.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getCode() {
        StringBuilder builder = new StringBuilder();
        builder.append("");
        for (int i = 0; i < numbers.size(); i++) {
            builder.append("" + numbers.get(i));
        }
        return builder.toString();
    }

    @Override
    public void filled(String number, int position) {
        numbers.set(position, number);
        if (checkIfAllIsFilled()) {
            confirm.performClick();
        }
    }

}

