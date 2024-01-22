package com.herfy.verification;

import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;

public interface VerificationContract {

    interface View extends BaseView<Presenter> {

        void setCode(String code);

        void showErrorCode(String error);

        void showErrorPhoneNumber(String error);

        void showMessage(String message);

        void showError(String error);

        void showResendError(String error);

        void showCode();

        void showVerified();

    }

    interface Presenter extends BasePresenter {

        void send(String customerId, String phoneNumber);

        void verify(String customerId, String email, String password, String phoneNumber, String code);

        void guest(String phoneNumber, String code);

        void update(String customerId, String email, String phoneNumber);

        void signInEmail(String email, String password);

        void signInFacebook(String email);

        void signInGoogle(String email);

        void signInTwitter(String email);

        void getCustomer(String id);

        void getFavorites(int page);

    }

}
