package com.herfy.mukafaa_otp;

import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;

public interface MukafaaOTPContract {

    interface View extends BaseView<Presenter> {

        void setCode(String code);

        void showErrorCode(String error);

        void showErrorPhoneNumber(String error);

        void showMessage(String message);

        void showError(String error);

        void showCode();

        void showVerified();

        void showLoadingDialog();

        void hideLoadingDialog();

        void showInvalidOTPDialog(boolean closePageOnClose);

        void showInvalidOTPDialog(String message, boolean closePageOnClose);

        void closePage();

        void closePageWithTransactionID(String transactionId, String amount);

        void setCountdown(long millis);
    }

    interface Presenter extends BasePresenter {

        void sendOtp();
        void verify(String otp, String amount);
        String getFormattedTimeForCountdown(long millisUntilFinished);
    }

}
