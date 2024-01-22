package com.herfy.mukafaa_otp;

import com.google.gson.JsonObject;
import com.herfy.constants.Text;
import com.herfy.constants.Values;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.models.requestbodies.AuthorizeProviderBody;
import com.skylinedynamics.solosdk.api.models.requestbodies.RedeemProviderBody;
import com.skylinedynamics.solosdk.util.GsonHelper;
import com.herfy.util.ApiLogLevel;
import com.herfy.util.ApiLogUtil;
import com.herfy.util.AppLogger;
import com.herfy.util.AuthUtil;
import com.herfy.util.CacheUtil;
import com.herfy.util.ErrorHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class MukafaaOTPPresenter implements MukafaaOTPContract.Presenter {

    private final MukafaaOTPContract.View otpView;
    private final String number;
    private JsonObject cartBody;

    public MukafaaOTPPresenter(MukafaaOTPContract.View otpView, String number, String validatedBody) {
        this.otpView = otpView;
        this.otpView.setPresenter(this);
        this.number = number;
        if (validatedBody != null) {
            this.cartBody = GsonHelper.gsonFinal().fromJson(validatedBody, JsonObject.class);
        } else {
            this.cartBody = null;
        }
    }

    @Override
    public void start() {
        otpView.setupViews();
        otpView.setupFonts();
        otpView.setupTranslations();
        sendOtp();
    }

    @Override
    public void sendOtp() {

        otpView.showLoadingDialog();
        int customerId = Integer.parseInt(AuthUtil.getInstance().getCustomer().getId());
        AuthorizeProviderBody body = new AuthorizeProviderBody(customerId, number);
        CustomersApiCall.getInstance().authorizeProvider(body, Values.MUKAFAA_PROVIDER, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                otpView.hideLoadingDialog();
                if (o instanceof JSONObject) {
                    otpView.setCountdown(3 * 60 * 1000);
                } else {
                    otpView.showInvalidOTPDialog(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), true);
                }
            }

            @Override
            public void onError(Object o) {
                otpView.hideLoadingDialog();
                AppLogger.printRetrofitError(o);
                if (o instanceof JSONObject) {
                    try {
                        JSONObject object = (JSONObject) o;
                        ApiLogUtil.sendMessageLog(ApiLogLevel.ERROR, object.toString());
                        Object error = object.get("error");
                        Object messages = object.get("messages");
                        String detail = ErrorHandler.getErrorMessage(error, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null);
                        if (messages instanceof JSONObject) {
                            detail = ((JSONObject) messages).getString("message");
                            String errorCode = ((JSONObject) messages).getString("errorCode");
                            if (errorCode.equals("210")) {
                                otpView.showInvalidOTPDialog(detail, false);
                                return;
                            } else {
                                otpView.showInvalidOTPDialog(detail, true);
                            }
                        } else {
                            otpView.showInvalidOTPDialog(detail, true);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        otpView.showInvalidOTPDialog(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), true);
                    }
                } else {
                    otpView.showInvalidOTPDialog(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), true);
                }
            }
        });

    }

    @Override
    public void verify(String otp, String amount) {

        boolean valid = true;

        if (otp.isEmpty()) {
            valid = false;
            otpView.showErrorCode(CacheUtil.getInstance().getTranslations(Text.CODE_REQUIRED));
        }

        if (valid) {
            otpView.showLoadingDialog();
            int customerId = Integer.parseInt(AuthUtil.getInstance().getCustomer().getId());
            RedeemProviderBody body = new RedeemProviderBody(customerId, otp, amount, cartBody);
            CustomersApiCall.getInstance().redeemProvider(body, Values.MUKAFAA_PROVIDER, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    otpView.hideLoadingDialog();
                    if (o instanceof JSONObject) {
                        try {
                            String transactionId = ((JSONObject) o).getString("transaction-id");
                            otpView.closePageWithTransactionID(transactionId, amount);
                        } catch (JSONException e) {
                            onError(e);
                        }
                    } else {
                        otpView.showInvalidOTPDialog(false);
                    }
                }

                @Override
                public void onError(Object o) {
                    otpView.hideLoadingDialog();
                    if (o instanceof JSONObject) {
                        try {
                            JSONObject obj = (JSONObject) o;
                            ApiLogUtil.sendMessageLog(ApiLogLevel.ERROR, obj.toString());
                            Object error = obj.get("error");
                            Object messages = obj.get("messages");
                            String detail = ErrorHandler.getErrorMessage(error, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null);
                            if (error instanceof JSONObject) {
                                JSONObject jsonError = (JSONObject) error;
                                String errorCode = jsonError.getString("code");
                                if (errorCode.equals("1062")) {
                                    otpView.showInvalidOTPDialog(false);
                                } else {
                                    otpView.showInvalidOTPDialog(detail, true);
                                }
                            } else if (error instanceof JSONArray) {
                                JSONObject errorObject = ((JSONArray) error).getJSONObject(0);
                                String errorCode = errorObject.getString("code");
                                if (errorCode.equals("1062")) {
                                    otpView.showInvalidOTPDialog(false);
                                } else {
                                    otpView.showInvalidOTPDialog(detail, true);
                                }
                            } else if (messages instanceof JSONObject) {
                                JSONObject jsonMessages = (JSONObject) messages;
                                String msg = jsonMessages.getString("message");
                                String errorCode = jsonMessages.getString("errorCode");
                                if (!errorCode.equals("301")) {
                                    otpView.showInvalidOTPDialog(msg, true);
                                } else {
                                    otpView.showInvalidOTPDialog(false);
                                }
                            } else {
                                otpView.showInvalidOTPDialog(false);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            otpView.showInvalidOTPDialog(false);
                        }
                    } else {
                        otpView.showInvalidOTPDialog(false);
                    }
                }
            });

        }
    }

    @Override
    public String getFormattedTimeForCountdown(long millisUntilFinished) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
        millisUntilFinished -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished);
        String countdownTime = "";
        if (minutes > 0) {
            String minlabel = minutes == 1 ? CacheUtil.getInstance().getTranslations(Text.MINUTE) : CacheUtil.getInstance().getTranslations(Text.MINUTES);
            countdownTime += minutes + " " + minlabel + " ";
        }
        if (seconds > 0) {
            String seclabel = seconds == 1 ? CacheUtil.getInstance().getTranslations(Text.SECOND) : CacheUtil.getInstance().getTranslations(Text.SECONDS);
            countdownTime += seconds + " " + seclabel;
        }

        return countdownTime;
    }
}