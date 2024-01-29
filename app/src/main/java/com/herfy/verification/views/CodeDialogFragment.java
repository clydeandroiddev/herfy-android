package com.herfy.verification.views;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.transition.AutoTransition;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import com.herfy.R;
import com.herfy.base.BaseDialogFragment;
import com.herfy.data.constants.Text;
import com.herfy.main.MainActivity;
import com.herfy.order.views.OrderTypeActivity;
import com.herfy.util.AnimUtil;
import com.herfy.util.CacheUtil;
import com.herfy.util.StringUtil;
import com.herfy.verification.VerificationContract;
import com.herfy.verification.VerificationPresenter;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CodeDialogFragment extends BaseDialogFragment implements VerificationContract.View {

    private VerificationContract.Presenter verificationPresenter;

    @BindView(R.id.container)
    ConstraintLayout container;
    @BindView(R.id.content)
    ConstraintLayout content;

    @BindView(R.id.card)
    CardView card;
    @BindView(R.id.code_container)
    CardView codeContainer;

    @BindView(R.id.close)
    ImageButton close;

    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.code_error)
    TextView codeError;
    @BindView(R.id.count_message)
    TextView countMessage;
    @BindView(R.id.count)
    TextView count;
    @BindView(R.id.resend_message)
    TextView resendMessage;
    @BindView(R.id.resend)
    TextView resend;

    @BindView(R.id.phone_number)
    TextView phoneNumber;
    @BindView(R.id.code)
    EditText code;

    @BindView(R.id.back)
    Button back;
    @BindView(R.id.send_confirm)
    Button sendConfirm;

    @BindView(R.id.count_container)
    LinearLayout countContainer;
    @BindView(R.id.resend_container)
    LinearLayout resendContainer;

    @BindView(R.id.label)
    TextView label;

    private boolean isCode = false;
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

    private ConstraintSet codeConstraintSet;

    private CountDownTimer countDownTimer;
    private long countDownTime = 60000;

    public static CodeDialogFragment newInstance() {
        return new CodeDialogFragment();
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

        View view = inflater.inflate(R.layout.fragment_dialog_code, container, false);

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
                CodeDialogFragment.super.onDismiss(dialog);
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

//        phoneNumber.setText("");
//        phoneNumber.append(StringUtil.localize(phoneNumberValue));
//
//        TextWatcher phoneNumberTextWatcher = new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//                phoneNumber.removeTextChangedListener(this);
//                phoneNumber.setText(StringUtil.localize(phoneNumber.getText().toString()));
//                phoneNumber.setSelection(editable.length());
//                phoneNumber.addTextChangedListener(this);
//            }
//        };
//
//        phoneNumber.addTextChangedListener(phoneNumberTextWatcher);
//
//        phoneNumber.setOnEditorActionListener(new EditText.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if (actionId == EditorInfo.IME_ACTION_DONE) {
//                    sendConfirm.performClick();
//                    InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
//                    inputMethodManager.hideSoftInputFromWindow(phoneNumber.getWindowToken(), 0);
//                    return true;
//                }
//                return false;
//            }
//        });

        TextWatcher codeTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                code.removeTextChangedListener(this);
                code.setText(StringUtil.localize(code.getText().toString()));
                code.setSelection(editable.length());
                code.addTextChangedListener(this);
            }
        };

        code.addTextChangedListener(codeTextWatcher);

        code.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    sendConfirm.performClick();
                    InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(code.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        codeConstraintSet = new ConstraintSet();
        codeConstraintSet.clone(content);

        ConstraintSet phoneNumberConstraintSet = new ConstraintSet();
        phoneNumberConstraintSet.clone(content);
        phoneNumberConstraintSet.setGuidelinePercent(R.id.guideline, 0);
        //     phoneNumberConstraintSet.constrainWidth(R.id.phone_number_container, 0);
//        phoneNumberConstraintSet.connect(R.id.phone_number_container, ConstraintSet.START, R.id.content, ConstraintSet.START, ViewUtil.dpToPx(getActivity(), 4));
//        phoneNumberConstraintSet.connect(R.id.phone_number_container, ConstraintSet.END, R.id.content, ConstraintSet.END, ViewUtil.dpToPx(getActivity(), 4));
//        phoneNumberConstraintSet.connect(R.id.phone_number_error, ConstraintSet.START, R.id.content, ConstraintSet.START, 0);
//        phoneNumberConstraintSet.connect(R.id.phone_number_error, ConstraintSet.END, R.id.content, ConstraintSet.END, 0);
        phoneNumberConstraintSet.connect(R.id.code_container, ConstraintSet.START, R.id.content, ConstraintSet.END, 0);
        phoneNumberConstraintSet.connect(R.id.code_container, ConstraintSet.END, R.id.content, ConstraintSet.END, 0);
        //phoneNumberConstraintSet.connect(R.id.code_error, ConstraintSet.START, R.id.content, ConstraintSet.END, 0);
        phoneNumberConstraintSet.connect(R.id.back, ConstraintSet.START, R.id.content, ConstraintSet.START, 0);
        phoneNumberConstraintSet.connect(R.id.back, ConstraintSet.END, R.id.content, ConstraintSet.START, 0);
        phoneNumberConstraintSet.connect(R.id.send_confirm, ConstraintSet.START, R.id.content, ConstraintSet.START, 0);
        phoneNumberConstraintSet.connect(R.id.send_confirm, ConstraintSet.END, R.id.content, ConstraintSet.END, 0);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDismiss(getDialog());
//                if (!guest) {
//                    if (countDownTimer != null) {
//                        countDownTimer.cancel();
//                    }
//                    Transition transition = new AutoTransition();
//                    transition.addListener(new Transition.TransitionListener() {
//                        @Override
//                        public void onTransitionStart(@NonNull Transition transition) {
//
//                        }
//
//                        @Override
//                        public void onTransitionEnd(@NonNull Transition transition) {
//                            isCode = false;
//                            title.setText(CacheUtil.getInstance().getTranslations(Text.ENTER_PHONE_NUMBER));
//                            sendConfirm.setText(CacheUtil.getInstance().getTranslations(Text.SEND_CODE_CAPS, "SEND CODE"));
//                            phoneNumber.requestLayout();
//                            countContainer.setVisibility(View.GONE);
//                            resendContainer.setVisibility(View.GONE);
//                        }
//
//                        @Override
//                        public void onTransitionCancel(@NonNull Transition transition) {
//
//                        }
//
//                        @Override
//                        public void onTransitionPause(@NonNull Transition transition) {
//
//                        }
//
//                        @Override
//                        public void onTransitionResume(@NonNull Transition transition) {
//
//                        }
//                    });
//                    TransitionManager.beginDelayedTransition(content, transition);
//                    phoneNumberConstraintSet.applyTo(content);
//                } else {
//                    onDismiss(getDialog());
//                }
            }
        });

        sendConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadDialog();
                if (isCode) {
                    if (!guest) {
                        verificationPresenter.verify(customerId, email, password, StringUtil.toEnglish(phoneNumberValue.trim().replace("+", "")), StringUtil.toEnglish(code.getText().toString().trim()));
                    } else {
                        verificationPresenter.guest(StringUtil.toEnglish(phoneNumberValue.trim().replace("+", "")), StringUtil.toEnglish(code.getText().toString().trim()));
                    }
                } else {
                    phoneNumberValue = (phoneNumber.getText().toString() + code.getText().toString()).trim().replace("+", "");
                    verificationPresenter.send(customerId, StringUtil.toEnglish((phoneNumberValue.trim().replace("+", ""))));
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
            countDownTimer.start();
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
//                    back.performClick();
                    changeToAddNumber();
                }
            }, 100);
        }

        resend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (countContainer.getVisibility() == View.GONE) {
                    loadDialog();
                    verificationPresenter.send(customerId, phoneNumberValue);
                }
            }
        });

    }

    private void changeToAddNumber() {
        if (!guest) {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            isCode = false;
            title.setText(CacheUtil.getInstance().getTranslations(Text.ENTER_PHONE_NUMBER_CAPS));
            label.setVisibility(View.VISIBLE);
            sendConfirm.setText(CacheUtil.getInstance().getTranslations(Text.SEND_CODE_CAPS, "SEND CODE"));
            back.setVisibility(View.GONE);
            close.setVisibility(View.VISIBLE);
            addNumberStateForCodeButton();
            code.setHint(CacheUtil.getInstance().getTranslations(Text.ENTER_NUMBER_HERE, "Enter number here"));
            code.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            countMessage.setVisibility(View.GONE);
            phoneNumber.setVisibility(View.VISIBLE);
            code.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15)});
            countContainer.setVisibility(View.GONE);
            resendContainer.setVisibility(View.GONE);
            close.setOnClickListener(v -> {
                if (!isCode)
                    onDismiss(getDialog());
            });
        }
    }

    private void addNumberStateForCodeButton() {
        ConstraintLayout constraintLayout = getView().findViewById(R.id.content);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);
        constraintSet.connect(R.id.send_confirm, ConstraintSet.START, R.id.content, ConstraintSet.START, 0);
        constraintSet.connect(R.id.send_confirm, ConstraintSet.END, R.id.content, ConstraintSet.END, 0);
        constraintSet.applyTo(constraintLayout);
    }

    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {
        String prefix = CacheUtil.getInstance().getConcept().getDialingCode();
        phoneNumber.setText(StringUtil.localize(prefix.contains("+") ? prefix : "+" + prefix));
        title.setText(CacheUtil.getInstance().getTranslations(Text.VERIFY_PHONE_NUMBER_CAPS, "VERIFY PHONE NUMBER"));
        code.setHint(CacheUtil.getInstance().getTranslations(Text.ENTER_CODE_HERE, "Enter code here"));
        back.setText(CacheUtil.getInstance().getTranslations(Text.CANCEL, "CANCEL"));
        sendConfirm.setText(CacheUtil.getInstance().getTranslations(Text.CONFIRM_CAPS, "CONFIRM"));
        countMessage.setText(CacheUtil.getInstance().getTranslations(Text.YOU_CAN_RESEND_THE_CODE, "You can resend the code in"));
        count.setText(CacheUtil.getInstance().getTranslations(Text.RESEND_SECONDS, "(x) seconds").replace("(x)", StringUtil.localize("60")));
        resendMessage.setText(CacheUtil.getInstance().getTranslations(Text.RESEND_VERIFICATION_CODE, "Didn't get the code?"));
        resend.setText(CacheUtil.getInstance().getTranslations(Text.RESEND, "Resend"));
        label.setText(CacheUtil.getInstance().getTranslations(Text.WE_WILL_SEND_CODE_MESSAGE, "We will send a code to your number. We need your mobile number to verify your identity and to help use contact you in case of an order query. Standard data charge may apply."));
    }

    @Override
    public void setCode(String codeValue) {
        code.setText(codeValue);
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

    }

    @Override
    public void showCode() {
        code.setText("");
        dismissDialogs();
        try {
            Activity activity = getActivity();
            if(activity != null) {
                Toast.makeText(activity, CacheUtil.getInstance().getTranslations(Text.VERIFICATION_CODE_SENT, "A verification code is sent to your number"), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Transition transition = new AutoTransition();
        transition.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(@NonNull Transition transition) {

            }

            @Override
            public void onTransitionEnd(@NonNull Transition transition) {
                label.setVisibility(View.GONE);
                isCode = true;
                title.setText(CacheUtil.getInstance().getTranslations(Text.VERIFY_PHONE_NUMBER_CAPS, "VERIFY PHONE NUMBER"));
                sendConfirm.setText(CacheUtil.getInstance().getTranslations(Text.CONFIRM_CAPS, "CONFIRM"));
                code.requestLayout();
                countDownTime = 60000;
                close.setVisibility(View.INVISIBLE);
                close.setOnClickListener(null);
                countMessage.setVisibility(View.VISIBLE);
                countContainer.setVisibility(View.VISIBLE);
                resendContainer.setVisibility(View.GONE);
                countDownTimer.start();
                back.setVisibility(View.VISIBLE);
                code.setHint(CacheUtil.getInstance().getTranslations(Text.ENTER_CODE_HERE, "Enter code here"));
                code.setGravity(Gravity.CENTER);
                phoneNumber.setVisibility(View.GONE);
                code.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
            }

            @Override
            public void onTransitionCancel(@NonNull Transition transition) {

            }

            @Override
            public void onTransitionPause(@NonNull Transition transition) {

            }

            @Override
            public void onTransitionResume(@NonNull Transition transition) {

            }
        });
        TransitionManager.beginDelayedTransition(content, transition);
        codeConstraintSet.applyTo(content);
    }

    @Override
    public void showVerified() {
        onDismiss(getDialog());
        /*Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.putExtra("order_history", orderHistory);
        intent.putExtra("cart", cart);
        intent.putExtra("order_type", orderType);
        intent.putExtra("favorite_home", favoriteHome);
        intent.putExtra("favorite_menu_item", favoriteMenuItem);
        startActivity(intent);*/

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

}
